package app.appgrove.core.platform;

import app.appgrove.commons.audit.AuditLogger;
import app.appgrove.commons.web.Page;
import app.appgrove.commons.web.PageRequest;
import app.appgrove.commons.web.ProblemDetail;
import app.appgrove.core.platform.InvitationDtos.CreateInvitation;
import app.appgrove.core.platform.InvitationDtos.InvitationView;
import io.quarkus.security.Authenticated;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * API inviti del tenant. Tenant-scoped automatico (discriminator). Gestione riservata all'<b>owner</b>,
 * e da UC 0100 <b>soltanto</b> a lui: la tolleranza per i token già emessi che portano {@code admin}
 * (UC 0098) è ritirata <b>qui</b>, in anticipo su UC 0113, perché invitare una persona nell'account è
 * esattamente il potere che questa storia riserva all'owner. La difesa vera è questa annotazione, non la
 * guardia della rotta nel backoffice.
 *
 * <p>Il token grezzo è restituito SOLO alla creazione; su DB resta solo il suo hash (single-use).
 *
 * <p><b>Tre esiti all'invio, di cui due leciti</b> (UC 0118 §5). «Questa persona è già membro di
 * questo account» e «c'è già un invito in attesa per questo indirizzo» sono informazioni
 * dell'<b>account</b>: si dicono, con due identificativi distinti perché l'interfaccia possa
 * mostrare due testi diversi nelle cinque lingue. «Questa persona ha già un'identità appgrove»
 * <b>non</b> lo è: rivelerebbe a un'azienda l'esistenza di un rapporto fra quella persona e la
 * piattaforma, che non le appartiene — quindi l'esito è <b>identico</b>, esista l'identità o no.
 */
@Path("/api/platform/v1/invitations")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class InvitationResource {

    private static final Duration TTL = Duration.ofDays(7);

    /**
     * Identificativi stabili delle due collisioni <b>lecite</b>, nel campo {@code type} del corpo
     * problem+json (RFC 9457). Servono perché l'interfaccia distingua i due casi <b>senza
     * interpretare un messaggio</b>: il messaggio del server è in italiano, l'interfaccia parla
     * cinque lingue. Sono contratto: cambiarli è un cambio di contratto.
     */
    static final String TYPE_ALREADY_MEMBER = "urn:appgrove:invitation:already-member";

    static final String TYPE_ALREADY_INVITED = "urn:appgrove:invitation:already-invited";

    @Inject
    InvitationRepository repository;

    @Inject
    IdentityRepository identities;

    @Inject
    MembershipRepository memberships;

    @Inject
    CallerContext caller;

    @Inject
    AuditLogger audit;

    /**
     * Invio di un invito. Non c'è alcun ruolo da scegliere (UC 0100): si entra sempre come
     * {@code member}, e i poteri si concedono dopo, una applicazione alla volta.
     */
    @POST
    @RolesAllowed(Roles.OWNER)
    @Transactional
    public Response create(@Valid CreateInvitation body) {
        String email = body.email().trim();

        // La lettura dell'identità si esegue SEMPRE, prima di ogni ramo, e il suo risultato cambia solo
        // il valore scritto in `identity_id`: mai il codice di stato, mai il corpo, mai lavoro in più su
        // un ramo e non sull'altro. È così che l'esito resta indistinguibile per costruzione, e non per
        // una simmetria di codice che la prossima modifica romperebbe senza accorgersene (UC 0118 §5).
        // Serve comunque per il controllo lecito qui sotto: «è già membro di QUESTO account?» è una
        // domanda sull'appartenenza, e l'appartenenza si raggiunge solo passando dall'identità.
        Identity identity = identities.findByEmail(email).orElse(null);

        if (identity != null && memberships.findByIdentity(identity.getId()).isPresent()) {
            // Informazione dell'account: lecita, e la più utile delle tre.
            return conflict(TYPE_ALREADY_MEMBER, "Questa persona è già membro di questo account.");
        }
        if (repository.existsPendingForEmail(email)) {
            return conflict(TYPE_ALREADY_INVITED, "Esiste già un invito in attesa per " + email + ".");
        }
        String token = InvitationTokens.newToken();
        // "chi ha invitato" è l'identità della persona (UC 0116): l'appartenenza cambia nel tempo,
        // l'identità no — ed è l'identificativo che le altre tabelle già conservano.
        UUID invitedBy = identities.findByCognitoSub(caller.subject()).map(Identity::getId).orElse(null);
        Invitation invitation =
                new Invitation(email, InvitationTokens.hash(token), Instant.now().plus(TTL), invitedBy);
        // Il collegamento all'identità che esiste già (UC 0118): serve all'accettazione, e NON esce mai
        // verso chi ha invitato — InvitationView non lo porta, e nessuna interfaccia di account lo vede.
        invitation.setIdentityId(identity == null ? null : identity.getId());
        repository.persist(invitation);
        repository.flush(); // forza INSERT: id e tenant_id valorizzati prima della response
        // evento audit (UC 0006): id invito, MAI l'email dell'invitato (nessun dato personale, #08/5).
        // Il ruolo non si registra più perché non è più una scelta di chi invita (UC 0100).
        audit.success("member.invited", Map.of(
                "invitation_id", invitation.getId().toString(),
                "actor", caller.subject()));
        return Response.status(Response.Status.CREATED)
                .entity(InvitationView.created(invitation, token))
                .build();
    }

    @GET
    @RolesAllowed(Roles.OWNER)
    public Page<InvitationView> listPending(@QueryParam("page") Integer page, @QueryParam("size") Integer size) {
        PageRequest pr = PageRequest.of(page, size);
        List<InvitationView> content = repository.find("status", InvitationStatus.pending)
                .page(io.quarkus.panache.common.Page.of(pr.page(), pr.size()))
                .list()
                .stream()
                .map(InvitationView::from)
                .toList();
        return Page.of(content, pr, repository.count("status", InvitationStatus.pending));
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed(Roles.OWNER)
    @Transactional
    public Response revoke(@PathParam("id") UUID id) {
        Invitation invitation = repository.findById(id);
        if (invitation == null) {
            throw new NotFoundException("Invito non trovato");
        }
        invitation.setStatus(InvitationStatus.revoked);
        audit.success("member.invitation.revoked", Map.of(
                "invitation_id", invitation.getId().toString(),
                "actor", caller.subject()));
        return Response.noContent().build();
    }

    /**
     * Rifiuto lecito, restituito come <b>Response</b> e non sollevato come eccezione: il mappatore
     * delle {@code WebApplicationException} riscrive sempre {@code type} ad {@code about:blank}, e
     * quel campo è l'unico modo di dire a un programma <i>quale</i> delle due collisioni è stata.
     */
    private static Response conflict(String type, String detail) {
        int status = Response.Status.CONFLICT.getStatusCode();
        return Response.status(status)
                .type(ProblemDetail.MEDIA_TYPE)
                .entity(new ProblemDetail(type, "Conflict", status, detail, null, null))
                .build();
    }
}
