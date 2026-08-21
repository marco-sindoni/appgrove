package app.appgrove.commons.access;

import app.appgrove.commons.access.projection.AppRoleProjectionStore;
import app.appgrove.commons.access.projection.AppRoleProjectionStore.ProjectedAppRole;
import app.appgrove.commons.entitlement.SafetyNet;
import app.appgrove.commons.tenancy.TenantNotResolvedException;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.logging.Logger;

/**
 * Attuazione <b>predefinita</b> di {@link AppRoleService} (UC 0099): legge il ruolo dalla <b>copia
 * locale</b> del servizio e interpella il core solo quando la copia non basta a decidere. Il codice di
 * dominio delle applicazioni non la nomina: inietta {@code AppRoleService} — o, meglio ancora, non inietta
 * nulla e mette {@code @RequiresAppRole} sull'operazione.
 *
 * <h2>Postura: copia con rete di sicurezza, e una scadenza</h2>
 *
 * Quattro situazioni, quattro risposte:
 *
 * <ol>
 *   <li><b>Copia usabile</b> (non invalidata e non scaduta) → si usa, senza toccare la rete. È il caso
 *       normale;</li>
 *   <li><b>Copia da rinfrescare</b> (un evento l'ha invalidata, o è passata la durata massima) → si tenta
 *       il rinfresco; se il core non risponde si continua a usare il valore vecchio, che resta l'ultima
 *       verità nota. Un guasto del core non deve bloccare tutte le persone di tutti gli account: il rischio
 *       accettato è che una revoca decisa <i>durante</i> il guasto arrivi in ritardo, e dura quanto il
 *       guasto;</li>
 *   <li><b>Copia assente</b> → il rinfresco è obbligatorio: non abbiamo nulla su cui decidere. Se anche il
 *       core è irraggiungibile <b>non si concede</b> e si risponde «non decidibile» (UC 0099 §5);</li>
 *   <li><b>Rilettura obbligatoria</b> ({@link #roleFresh}, operazioni irreversibili) → la copia non si usa
 *       <b>mai</b>, nemmeno come ripiego: se il core non risponde, l'operazione non parte. È il senso di
 *       quella rilettura — chiudere la finestra in cui una revoca appena decisa non è ancora arrivata.</li>
 * </ol>
 *
 * <p><b>Invarianti.</b> Account e persona arrivano dal <b>JWT verificato</b> (#1) e vincolano ogni lettura
 * della copia (#2): non si legge mai il ruolo di un'altra coppia account-persona. {@code @RequestScoped}
 * memoizza l'esito per richiesta, così più varchi nella stessa richiesta non ripetono né la query né la
 * chiamata di rete.
 */
@RequestScoped
public class ProjectedAppRoleService implements AppRoleService {

    private static final Logger LOG = Logger.getLogger(ProjectedAppRoleService.class);

    @Inject
    AppRoleProjectionStore store;

    @Inject
    @SafetyNet
    AppRoleService safetyNet;

    @Inject
    JsonWebToken jwt;

    /**
     * Durata massima della copia locale. Sessanta secondi è il valore proposto dalla storia: abbastanza
     * poco perché una revoca arrivi comunque in fretta se il canale degli eventi è rotto, abbastanza da
     * togliere il core dal percorso caldo. Si cambia per ambiente, non nel codice.
     */
    @ConfigProperty(name = "appgrove.app-role.projection.max-age", defaultValue = "60s")
    Duration maxAge;

    /** Memoizzazione per-richiesta: {@code appSlug → esito}. */
    private final Map<String, AppRoleOutcome> perRequest = new HashMap<>();

    @Override
    public AppRoleOutcome roleOf(String appSlug) {
        AppRoleOutcome memoized = perRequest.get(appSlug);
        if (memoized != null) {
            return memoized;
        }
        AppRoleOutcome outcome = resolve(appSlug);
        perRequest.put(appSlug, outcome);
        return outcome;
    }

    @Override
    public AppRoleOutcome roleFresh(String appSlug) {
        if (!store.enabled()) {
            return fromSafetyNet(appSlug).orElseGet(AppRoleOutcome.Unavailable::new);
        }
        Optional<AppRoleOutcome> fresh = refresh(appSlug);
        if (fresh.isEmpty()) {
            LOG.errorf(
                    "app_role.fresh rilettura obbligatoria fallita tenant_id=%s user_id=%s app_id=%s:"
                            + " l'operazione non parte (nessun ripiego sulla copia locale)",
                    tenantId(), subject(), appSlug);
            return new AppRoleOutcome.Unavailable();
        }
        // La rilettura aggiorna anche la memoizzazione: dopo di essa, nella stessa richiesta, la
        // verità è quella appena letta.
        perRequest.put(appSlug, fresh.get());
        return fresh.get();
    }

    private AppRoleOutcome resolve(String appSlug) {
        if (!store.enabled()) {
            // Servizio senza copia locale (core, auth, o applicazione con la copia disattivata):
            // si interpella direttamente la fonte di verità.
            return fromSafetyNet(appSlug).orElseGet(AppRoleOutcome.Unavailable::new);
        }
        Optional<ProjectedAppRole> row = store.find(tenantId(), subject(), appSlug);
        if (row.isPresent() && row.get().usable(maxAge, Instant.now())) {
            return outcomeOf(row.get());
        }

        Optional<AppRoleOutcome> fresh = refresh(appSlug);
        if (fresh.isPresent()) {
            return fresh.get();
        }
        if (row.isPresent()) {
            // Ultima verità nota: meglio di un blocco totale. Va vista, quindi WARN e non DEBUG.
            LOG.warnf(
                    "app_role.projection servita copia vecchia tenant_id=%s user_id=%s app_id=%s eta_secondi=%d"
                            + " (rinfresco fallito: core irraggiungibile)",
                    tenantId(), subject(), appSlug,
                    Math.max(0L, Duration.between(row.get().refreshedAt(), Instant.now()).toSeconds()));
            return outcomeOf(row.get());
        }
        LOG.errorf(
                "app_role.projection nessuna base per decidere tenant_id=%s user_id=%s app_id=%s:"
                        + " copia assente e core irraggiungibile → si nega",
                tenantId(), subject(), appSlug);
        return new AppRoleOutcome.Unavailable();
    }

    /**
     * Rinfresco dalla fonte di verità, con scrittura della copia. Vuoto = il core non ha risposto: la
     * decisione su cosa farne è del chiamante, che è l'unico a sapere se ha una copia vecchia da usare.
     */
    private Optional<AppRoleOutcome> refresh(String appSlug) {
        Optional<AppRoleOutcome> outcome = fromSafetyNet(appSlug);
        if (outcome.isEmpty()) {
            return Optional.empty();
        }
        if (store.enabled()) {
            // Anche il diniego si scrive: sapere che una persona NON ha accesso vale quanto sapere che
            // ce l'ha, e risparmia una chiamata di rete a ogni sua richiesta successiva.
            store.save(tenantId(), subject(), appSlug, outcome.get().roleOrNull());
        }
        return outcome;
    }

    /** Esito dalla fonte di verità, o vuoto se non ha risposto. */
    private Optional<AppRoleOutcome> fromSafetyNet(String appSlug) {
        try {
            return Optional.of(safetyNet.roleOf(appSlug));
        } catch (RuntimeException e) {
            LOG.debugf(e, "app_role.projection lettura dal core fallita app_id=%s", appSlug);
            return Optional.empty();
        }
    }

    private static AppRoleOutcome outcomeOf(ProjectedAppRole row) {
        return row.role() == null ? new AppRoleOutcome.NoAccess() : new AppRoleOutcome.Granted(row.role());
    }

    private String tenantId() {
        Object claim = jwt.getClaim("tenant_id");
        String tenantId = claim == null ? null : claim.toString();
        if (tenantId == null || tenantId.isBlank()) {
            // Fail-closed come JwtTenantResolver: senza account verificato non si legge nulla.
            throw new TenantNotResolvedException();
        }
        return tenantId;
    }

    private String subject() {
        String subject = jwt.getSubject();
        if (subject == null || subject.isBlank()) {
            throw new TenantNotResolvedException();
        }
        return subject;
    }
}
