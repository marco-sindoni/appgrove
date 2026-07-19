package app.appgrove.commons.entitlement.projection;

import app.appgrove.commons.entitlement.EntitlementService;
import app.appgrove.commons.entitlement.EntitlementView;
import app.appgrove.commons.entitlement.MetricLimit;
import app.appgrove.commons.entitlement.SafetyNet;
import app.appgrove.commons.quota.QuotaNature;
import app.appgrove.commons.entitlement.EntitlementViewSource;
import app.appgrove.commons.tenancy.TenantNotResolvedException;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.logging.Logger;

/**
 * Implementazione <b>predefinita</b> di {@link EntitlementService} (UC 0046): legge i diritti dalla
 * <b>proiezione locale</b> dell'app invece di chiamare core a ogni richiesta (UC 0027). Il codice di
 * dominio delle app non cambia: continua a iniettare {@code EntitlementService}.
 *
 * <h2>Postura: cache con rete di sicurezza</h2>
 *
 * La proiezione <b>non è la fonte del permesso, è la sua cache</b>. Tre situazioni, tre risposte:
 *
 * <ol>
 *   <li><b>Riga fresca</b> → si usa, senza soglia di scadenza. Un abbonamento cambia di rado: far
 *       scadere la proiezione dopo N ore introdurrebbe un blocco a orologeria senza aggiungere
 *       sicurezza reale, perché è l'<b>evento</b> — non il tempo — a dire che qualcosa è cambiato.</li>
 *   <li><b>Riga da rinfrescare</b> (un evento l'ha invalidata) → si tenta il rinfresco dalla rete di
 *       sicurezza; se core non risponde <b>si continua a usare il valore vecchio</b>, che resta
 *       l'ultima verità nota, emettendo la misura di scostamento. Un guasto di core non deve
 *       trasformarsi nel blocco di tutti i clienti paganti — è esattamente il disastro che la
 *       proiezione esiste per evitare.</li>
 *   <li><b>Riga assente</b> (tenant mai visto) → il rinfresco è obbligatorio: non abbiamo nulla su
 *       cui decidere. Se anche core è irraggiungibile si <b>nega</b>, ed è l'unico caso in cui un
 *       utente legittimo può essere respinto.</li>
 * </ol>
 *
 * <p>La finestra di incoerenza esiste ed è nota: fra la disdetta e il consumo dell'evento passano
 * secondi, durante i quali l'accesso resta concesso. È un rischio accettato consapevolmente —
 * riguarda l'uso di un servizio già erogato, mentre il denaro è governato dal fornitore di pagamento.
 *
 * <p><b>Invarianti.</b> Il {@code tenant_id} arriva dal <b>JWT verificato</b> (#1) e vincola ogni
 * lettura della proiezione (#2); {@code @RequestScoped} memoizza l'esito per richiesta, così più gate
 * nella stessa richiesta non ripetono né la query né l'eventuale chiamata di rete.
 */
@RequestScoped
public class ProjectedEntitlementService implements EntitlementService {

    private static final Logger LOG = Logger.getLogger(ProjectedEntitlementService.class);

    @Inject
    EntitlementProjectionStore store;

    @Inject
    @SafetyNet
    EntitlementService safetyNet;

    @Inject
    EntitlementProjectionMetrics metrics;

    @Inject
    JsonWebToken jwt;

    /** Memoizzazione per-richiesta: {@code appSlug → esito}. */
    private final Map<String, Resolution> perRequest = new HashMap<>();

    @Override
    public boolean hasAccess(String appSlug) {
        return resolve(appSlug).view() != null;
    }

    @Override
    public long capFor(String appSlug, String metric) {
        return limit(appSlug, metric).map(MetricLimit::cap).orElse(-1L);
    }

    @Override
    public QuotaNature natureOf(String appSlug, String metric) {
        return limit(appSlug, metric)
                .map(MetricLimit::nature)
                .filter(n -> n != null && !n.isBlank())
                .map(n -> QuotaNature.valueOf(n.toUpperCase(Locale.ROOT)))
                .orElse(null);
    }

    private Optional<MetricLimit> limit(String appSlug, String metric) {
        EntitlementView view = resolve(appSlug).view();
        if (view == null || view.limits() == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(view.limits().get(metric));
    }

    /** Esito della risoluzione per un'app, memoizzato per richiesta. */
    private Resolution resolve(String appSlug) {
        return perRequest.computeIfAbsent(appSlug, this::resolveUncached);
    }

    private Resolution resolveUncached(String appSlug) {
        if (!store.enabled()) {
            // Servizio senza proiezione (core, auth, o app con la proiezione disattivata):
            // comportamento identico a UC 0027, nessuna regressione.
            return new Resolution(safetyNetView(appSlug), Source.SAFETY_NET_ONLY);
        }
        String tenantId = tenantId();
        Optional<EntitlementProjectionStore.ProjectedEntitlement> row = store.find(tenantId, appSlug);

        if (row.isPresent() && !row.get().stale()) {
            metrics.hit();
            return new Resolution(row.get().view(), Source.PROJECTION);
        }

        String motivo = row.isPresent() ? "da_rinfrescare" : "assente";
        try {
            EntitlementView fresh = safetyNetView(appSlug);
            store.save(tenantId, appSlug, fresh);
            metrics.safetyNet(motivo);
            return new Resolution(fresh, Source.SAFETY_NET);
        } catch (RuntimeException e) {
            if (row.isPresent()) {
                // Ultima verità nota: meglio di un blocco. Scostamento misurato e loggato.
                metrics.staleServed(tenantId, appSlug, row.get().refreshedAt());
                LOG.debugf(e, "entitlement.projection rinfresco fallito, si prosegue con la proiezione vecchia");
                return new Resolution(row.get().view(), Source.STALE);
            }
            metrics.deniedUnknown(tenantId, appSlug);
            return new Resolution(null, Source.DENIED_UNKNOWN);
        }
    }

    /** Vista dell'app dalla rete di sicurezza; {@code null} = nessun accesso (diniego noto). */
    private EntitlementView safetyNetView(String appSlug) {
        // La rete di sicurezza espone la vista completa: la si riusa così com'è, senza ri-derivare
        // nulla in locale (il calcolo degli entitlement resta in un solo posto, in core).
        if (safetyNet instanceof EntitlementViewSource source) {
            return source.viewFor(appSlug).orElse(null);
        }
        // Sorgente puramente booleana (test/implementazioni ridotte): si conserva almeno l'accesso.
        return safetyNet.hasAccess(appSlug) ? new EntitlementView(appSlug, null, null, null, Map.of()) : null;
    }

    private String tenantId() {
        Object claim = jwt.getClaim("tenant_id");
        String tenantId = claim == null ? null : claim.toString();
        if (tenantId == null || tenantId.isBlank()) {
            // Fail-closed come JwtTenantResolver: senza tenant verificato non si legge nulla.
            throw new TenantNotResolvedException();
        }
        return tenantId;
    }

    /** Provenienza dell'esito, utile a test e diagnostica. */
    enum Source {
        /** Proiezione fresca. */
        PROJECTION,
        /** Rinfrescata dalla rete di sicurezza. */
        SAFETY_NET,
        /** Servizio senza proiezione: solo rete di sicurezza. */
        SAFETY_NET_ONLY,
        /** Proiezione vecchia servita perché il rinfresco è fallito. */
        STALE,
        /** Negato: nessuna proiezione e core irraggiungibile. */
        DENIED_UNKNOWN
    }

    private record Resolution(EntitlementView view, Source source) {}
}
