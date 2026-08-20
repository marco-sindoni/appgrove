package app.appgrove.core.platform;

import app.appgrove.commons.audit.AuditLogger;
import app.appgrove.core.platform.MeDtos.CreateOwnAccount;
import app.appgrove.core.platform.MeDtos.OwnAccountView;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Map;
import java.util.UUID;
import org.jboss.logging.Logger;

/**
 * Apertura di un <b>proprio</b> account da parte di chi è già una persona della piattaforma
 * (UC 0118, percorso B).
 *
 * <p>È il percorso che prima non esisteva: chi era stato invitato da un'azienda non poteva aprire un
 * account per sé, perché la registrazione con quell'indirizzo rispondeva «email già registrata» e
 * finiva lì. La soluzione <b>non</b> è permettere alla registrazione di accettare un indirizzo che
 * esiste — nessuno può creare un account per conto di un'identità senza autenticarla — ma spostare il
 * percorso <b>dentro la sessione</b>: sei già tu, non serve dirci di nuovo chi sei.
 *
 * <p>Per questo qui non si chiede né parola d'accesso né nome della persona: si chiede <b>solo</b> il
 * nome del nuovo account. Chiedere di nuovo quei campi è il modo in cui una persona finisce per
 * crearsi una seconda identità con un altro indirizzo — e unire due identità è lavoro manuale e
 * sgradevole, fuori scope per costruzione.
 *
 * <p>Nessun ruolo da scegliere: chi apre un account ne è l'{@code owner}. Un solo owner per account
 * resta un requisito dichiarato.
 */
@Path("/api/platform/v1/me/accounts")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MeAccountsResource {

    private static final Logger LOG = Logger.getLogger(MeAccountsResource.class);

    @Inject
    AccountRepository accounts;

    @Inject
    IdentityRepository identities;

    @Inject
    MembershipRepository memberships;

    @Inject
    CallerContext caller;

    @Inject
    AuditLogger audit;

    /**
     * Crea un account nuovo con l'appartenenza {@code owner} della persona in sessione, e lo rende il
     * suo <b>account attivo</b>: chi apre un account vuole andarci: non ci si apre un account per
     * restare dove si era. L'interfaccia ricarica, il token nasce con il claim nuovo e la persona vi
     * si trova dentro.
     *
     * <p>L'appartenenza si crea con una scrittura di piattaforma e non con l'entità tenant-scoped:
     * l'account è nuovo e <b>non è</b> quello del token, quindi Hibernate scriverebbe il
     * {@code tenant_id} sbagliato. L'account di destinazione non arriva mai dal chiamante — è quello
     * appena creato in questa stessa transazione.
     *
     * <p>Il gate legale del nuovo account sarà pendente al primo ingresso, ed è corretto:
     * l'accettazione dei documenti è per <b>account</b>, non per persona (UC 0056) — ogni account è un
     * contratto a sé.
     */
    @POST
    @Transactional
    public Response create(@Valid CreateOwnAccount body) {
        Identity identity = identities.findByCognitoSub(caller.subject())
                .orElseThrow(() -> new NotFoundException("Profilo utente non trovato"));

        Account account = new Account(body.name().trim());
        accounts.persist(account);
        accounts.flush(); // forza l'INSERT: l'id serve subito per l'appartenenza

        UUID membershipId = memberships.createMembership(
                account.getId().toString(), identity.getId(), MembershipRole.owner, caller.subject());
        identity.setActiveMembershipId(membershipId);

        LOG.infof("account.created user_id=%s tenant_id=%s", identity.getId(), account.getId());
        audit.success("account.self-created", Map.of(
                "tenant_id", account.getId().toString(),
                "user_id", identity.getId().toString(),
                "actor", caller.subject()));
        return Response.status(Response.Status.CREATED)
                .entity(new OwnAccountView(account.getId().toString(), account.getName()))
                .build();
    }
}
