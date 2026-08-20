package app.appgrove.core.platform;

import app.appgrove.commons.audit.AuditLogger;
import app.appgrove.commons.membership.ActiveAccount;
import app.appgrove.commons.membership.ActiveAccount.Candidate;
import app.appgrove.commons.membership.ActiveAccount.Choice;
import app.appgrove.core.platform.MembershipRepository.AccountOfIdentity;
import app.appgrove.core.platform.MembershipDtos.MembershipRef;
import app.appgrove.core.platform.MembershipDtos.MyMembershipsView;
import app.appgrove.core.platform.MembershipDtos.SetActiveAccount;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jboss.logging.Logger;

/**
 * Le appartenenze della <b>persona che sta chiamando</b> e il cambio del suo account attivo
 * (UC 0117). È la sola interfaccia dell'account attivo: il claim continua a essere calcolato dal
 * solo lato server, alla creazione del token, e <b>l'invariante 1 non si tocca</b> — cambia la
 * funzione che lo calcola, non chi se ne fida.
 *
 * <p><b>Perché queste letture attraversano gli account, e perché è corretto.</b> La domanda «a quali
 * account appartengo io?» ha per soggetto la persona, non l'account: per costruzione attraversa gli
 * account. Il perimetro è la persona del token ({@code sub}), mai un identificativo che arrivi dal
 * chiamante — e nessuna interfaccia di account usa queste letture, perché nessuna deve poter dedurre
 * le altre appartenenze di qualcun altro (UC 0116 §8).
 *
 * <p><b>Il cambio non restituisce token.</b> Scrive la scelta e nulla più: il rinnovo passa dal
 * percorso di rinnovo esistente, così la costruzione del claim resta in un solo posto. Un endpoint
 * che restituisse un token sarebbe un secondo posto in cui l'account si stabilisce, cioè un secondo
 * posto in cui si può sbagliare.
 */
@Path("/api/platform/v1/me")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MeMembershipsResource {

    private static final Logger LOG = Logger.getLogger(MeMembershipsResource.class);

    @Inject
    IdentityRepository identities;

    @Inject
    MembershipRepository memberships;

    @Inject
    CallerContext caller;

    @Inject
    AuditLogger audit;

    @Inject
    EntityManager em;

    /**
     * Le appartenenze attive della persona in sessione, con il nome dell'account, e quale è
     * l'account <b>attivo</b> secondo la stessa regola che compone il token
     * ({@link ActiveAccount#choose}).
     *
     * <p>{@code activeAccountId} è quindi la verità corrente lato server, non il valore grezzo
     * conservato. Serve anche a un secondo scopo: l'interfaccia lo confronta con l'account del token
     * che ha in mano e si accorge se l'account attivo è cambiato in un'altra scheda (UC 0117 §6).
     * Nullo quando la persona ha più appartenenze e nessuna scelta valida — lo stesso caso in cui il
     * token nasce senza claim.
     */
    @GET
    @Path("/memberships")
    public MyMembershipsView memberships() {
        Identity identity = currentIdentity();
        List<AccountOfIdentity> accounts = memberships.activeAccountsOf(identity.getId());
        Choice choice = ActiveAccount.choose(candidates(accounts), identity.getActiveMembershipId());
        String activeAccountId =
                choice instanceof Choice.Chosen chosen ? chosen.candidate().tenantId() : null;
        return new MyMembershipsView(
                activeAccountId,
                accounts.stream()
                        .map(a -> new MembershipRef(a.tenantId(), a.accountName()))
                        .toList());
    }

    /**
     * Cambia l'account attivo della persona in sessione.
     *
     * <p>L'account arriva dal corpo della richiesta ed è un <b>candidato</b>, non una fonte di
     * verità: viene accettato solo se corrisponde a un'appartenenza <b>attiva</b> della persona del
     * token, riverificata adesso. Non è una violazione dell'invariante 1 — nessun claim viene
     * costruito qui: si scrive un suggerimento che la funzione del token riverificherà a sua volta.
     *
     * <p>Rifiuto <b>404</b> e non 403 quando l'appartenenza non esiste: un 403 direbbe «quell'account
     * esiste, ma non è tuo», e l'esistenza di un account non è un'informazione che appartiene a chi
     * chiede. Indistinguibile da «non esiste».
     *
     * <p>Chiedere l'account su cui si è già è <b>idempotente</b> e non lascia riga di registro: una
     * prova di cambio senza cambio sarebbe una prova falsa (stessa regola di UC 0076).
     */
    @POST
    @Path("/active-account")
    @Transactional
    public Response setActiveAccount(@Valid SetActiveAccount body) {
        Identity identity = currentIdentity();
        List<AccountOfIdentity> accounts = memberships.activeAccountsOf(identity.getId());
        AccountOfIdentity target = accounts.stream()
                .filter(a -> a.tenantId().equals(body.accountId()))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Account non trovato"));

        Choice before = ActiveAccount.choose(candidates(accounts), identity.getActiveMembershipId());
        String fromTenantId = before instanceof Choice.Chosen chosen ? chosen.candidate().tenantId() : null;
        if (target.tenantId().equals(fromTenantId)) {
            return Response.noContent().build(); // già lì: nessuna scrittura, nessuna prova falsa
        }

        identity.setActiveMembershipId(target.membershipId());
        em.createNativeQuery("insert into platform.active_account_audit"
                        + " (id, identity_id, from_tenant_id, to_tenant_id, executed_at)"
                        + " values (:id, :identityId, :from, :to, :at)")
                .setParameter("id", UUID.randomUUID())
                .setParameter("identityId", identity.getId())
                .setParameter("from", fromTenantId)
                .setParameter("to", target.tenantId())
                .setParameter("at", Instant.now())
                .executeUpdate();

        LOG.infof("account.switched user_id=%s tenant_id=%s from_tenant_id=%s",
                caller.subject(), target.tenantId(), fromTenantId);
        // Soli identificativi opachi: il cambio di account è l'informazione utile in caso di
        // contestazione su «chi ha fatto cosa e per conto di chi».
        Map<String, String> details = new java.util.HashMap<>();
        details.put("user_id", identity.getId().toString());
        details.put("actor", caller.subject());
        details.put("tenant_id", target.tenantId());
        if (fromTenantId != null) {
            details.put("from_tenant_id", fromTenantId);
        }
        audit.success("account.switched", details);
        return Response.noContent().build();
    }

    // ── helper ───────────────────────────────────────────────────────────────

    /**
     * La persona del token. Il {@code sub} è l'unica chiave ammessa: mai un identificativo che
     * arrivi dal chiamante, altrimenti questa interfaccia diventerebbe il modo di leggere le
     * appartenenze di qualcun altro.
     */
    private Identity currentIdentity() {
        return identities.findByCognitoSub(caller.subject())
                .orElseThrow(() -> new NotFoundException("Profilo utente non trovato"));
    }

    private static List<Candidate> candidates(List<AccountOfIdentity> accounts) {
        return accounts.stream()
                .map(a -> new Candidate(a.membershipId(), a.tenantId(), a.role()))
                .toList();
    }
}
