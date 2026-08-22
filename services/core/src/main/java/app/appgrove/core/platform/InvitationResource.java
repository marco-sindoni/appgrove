package app.appgrove.core.platform;

import app.appgrove.commons.audit.AuditLogger;
import app.appgrove.commons.audit.AuditOutcome;
import app.appgrove.commons.web.Page;
import app.appgrove.commons.web.PageRequest;
import app.appgrove.commons.web.ProblemDetail;
import app.appgrove.core.billing.seats.SeatChargeDeclinedException;
import app.appgrove.core.billing.seats.SeatDowngradeService;
import app.appgrove.core.billing.seats.SeatSubscriptionService;
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
 * <p><b>Da UC 0103 l'invito passa dalla cassa.</b> La creazione è una <b>sequenza ordinata</b>, e l'ordine
 * è la garanzia: <i>blocco dell'account → stato dell'account → riduzione in attesa → indirizzo duplicato →
 * addebito del posto → creazione dell'invito → invio dell'email</i>. Se l'addebito non riesce l'invito
 * <b>non nasce</b>: è preferibile un invito mancato a un posto attivo non pagato. Se la creazione fallisce
 * dopo un addebito riuscito, l'addebito viene <b>annullato</b> nella stessa unità di lavoro.
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

    /**
     * Il fornitore di pagamento ha rifiutato l'addebito del posto (UC 0103 §5): l'invito non è stato
     * creato. Il motivo del fornitore viaggia nel campo {@code detail}, perché è l'unica informazione con
     * cui chi ha invitato può rimediare.
     */
    static final String TYPE_SEAT_CHARGE_DECLINED = "urn:appgrove:seats:charge-declined";

    /** L'account è in attesa di eliminazione: nessun invito ammesso (UC 0103 §5). */
    static final String TYPE_ACCOUNT_PENDING_DELETION = "urn:appgrove:account:pending-deletion";

    /**
     * C'è una <b>riduzione dei posti in attesa</b>: nessuna aggiunta finché non si chiude (UC 0104 §8).
     * L'identificativo serve all'interfaccia per mostrare il testo con le <b>due vie d'uscita</b> nella
     * lingua di chi legge: annullare la riduzione, oppure attendere la data.
     */
    static final String TYPE_REDUCTION_PENDING = "urn:appgrove:seats:reduction-pending";

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

    @Inject
    AccountRepository accounts;

    @Inject
    SeatSubscriptionService seats;

    @Inject
    SeatDowngradeService reductions;

    /**
     * Invio di un invito. Non c'è alcun ruolo da scegliere (UC 0100): si entra sempre come
     * {@code member}, e i poteri si concedono dopo, una applicazione alla volta.
     */
    @POST
    @RolesAllowed(Roles.OWNER)
    @Transactional
    public Response create(@Valid CreateInvitation body) {
        String email = body.email().trim();

        // (1) BLOCCO DELL'ACCOUNT, prima di qualunque lettura che decida del denaro. Serializza gli inviti
        // dello stesso account: senza, due clic simultanei leggono entrambi «tre posti» e addebitano due
        // volte lo stesso salto di fascia (UC 0103 §5). Il costo è una riga bloccata per la durata di un
        // invito; il beneficio è che il conto non può sbagliare.
        seats.lockAccount();

        // (2) STATO DELL'ACCOUNT: un conto in attesa di eliminazione non invita nessuno. Aggiungere una
        // persona a un account che sta chiudendo significherebbe farle occupare — e pagare — un posto che
        // sparisce fra pochi giorni.
        Account account = accounts.findById(caller.tenantId());
        if (account != null && account.getStatus() == AccountStatus.pending_deletion) {
            return conflict(
                    TYPE_ACCOUNT_PENDING_DELETION,
                    "L'account è in attesa di eliminazione: non è possibile invitare nuove persone.");
        }

        // (3) RIDUZIONE IN ATTESA → nessun invito ammesso (UC 0104 §8). Il gate sta QUI, subito dopo lo
        // stato dell'account e PRIMA di ogni calcolo e di ogni addebito: un rifiuto che si conosce in
        // anticipo non deve costare denaro. La ragione del divieto è pratica: sommare un'aggiunta e una
        // riduzione dentro lo stesso periodo renderebbe il conto del periodo indecidibile — il posto
        // liberato è pagato fino a scadenza, quello nuovo si paga adesso, e i due non si compensano — e la
        // fattura risultante sarebbe inspiegabile a chi la legge.
        //
        // Il presidio è QUI, nel servizio, e non nel comando spento a schermo: un divieto che vive solo
        // nell'interfaccia si aggira con una richiesta diretta. Il testo offre DUE VIE D'USCITA, ed è la
        // parte che conta — un rifiuto senza uscita è un vicolo cieco, e questo non lo è.
        if (reductions.blocksAdditions()) {
            return conflict(
                    TYPE_REDUCTION_PENDING,
                    "C'è una riduzione dei posti programmata: fino alla sua esecuzione non è possibile"
                            + " aggiungere persone. Annulla la riduzione, oppure attendi la data prevista.");
        }

        // La lettura dell'identità si esegue SEMPRE, prima di ogni ramo, e il suo risultato cambia solo
        // il valore scritto in `identity_id`: mai il codice di stato, mai il corpo, mai lavoro in più su
        // un ramo e non sull'altro. È così che l'esito resta indistinguibile per costruzione, e non per
        // una simmetria di codice che la prossima modifica romperebbe senza accorgersene (UC 0118 §5).
        // Serve comunque per il controllo lecito qui sotto: «è già membro di QUESTO account?» è una
        // domanda sull'appartenenza, e l'appartenenza si raggiunge solo passando dall'identità.
        Identity identity = identities.findByEmail(email).orElse(null);

        // (4) INDIRIZZO DUPLICATO, prima dell'addebito: un rifiuto lecito non deve costare denaro.
        if (identity != null && memberships.findByIdentity(identity.getId()).isPresent()) {
            // Informazione dell'account: lecita, e la più utile delle tre.
            return conflict(TYPE_ALREADY_MEMBER, "Questa persona è già membro di questo account.");
        }
        if (repository.existsPendingForEmail(email)) {
            return conflict(TYPE_ALREADY_INVITED, "Esiste già un invito in attesa per " + email + ".");
        }

        // (5) ADDEBITO DEL POSTO. Restituisce «nessun addebito» quando il posto è dentro la franchigia o
        // era già pagato in questo periodo; solleva quando il fornitore rifiuta, e allora non si crea nulla.
        SeatSubscriptionService.SeatCharge charge;
        try {
            charge = seats.chargeOneMoreSeat();
        } catch (SeatChargeDeclinedException e) {
            audit.log("member.invite.declined", AuditOutcome.FAILURE, Map.of(
                    "actor", caller.subject(),
                    "reason", "seat-charge-declined"));
            int status = 402; // Payment Required: manca il pagamento, non il permesso.
            return Response.status(status)
                    .type(ProblemDetail.MEDIA_TYPE)
                    .entity(new ProblemDetail(
                            TYPE_SEAT_CHARGE_DECLINED, "Payment Required", status, e.getReason(), null, null))
                    .build();
        }

        try {
            return createInvitation(email, identity, charge);
        } catch (RuntimeException e) {
            // (7) L'invito non è nato dopo un addebito riuscito: si annulla l'addebito. È il caso in cui
            // qualcuno avrebbe pagato un posto che nessuno occupa, e va rettificato subito.
            seats.release(charge);
            throw e;
        }
    }

    /**
     * La creazione vera della riga di invito, <b>dopo</b> che il posto è stato pagato (UC 0103, passo 6).
     *
     * <p>Sta in un metodo suo perché il chiamante deve poter distinguere «è fallita la creazione» da «è
     * fallito qualcosa prima»: solo nel primo caso c'è un addebito da annullare, e un {@code try} che
     * abbracciasse anche i passi precedenti annullerebbe un addebito mai fatto.
     */
    private Response createInvitation(
            String email, Identity identity, SeatSubscriptionService.SeatCharge charge) {
        String token = InvitationTokens.newToken();
        // "chi ha invitato" è l'identità della persona (UC 0116): l'appartenenza cambia nel tempo,
        // l'identità no — ed è l'identificativo che le altre tabelle già conservano.
        UUID invitedBy = identities.findByCognitoSub(caller.subject()).map(Identity::getId).orElse(null);
        Invitation invitation =
                new Invitation(email, InvitationTokens.hash(token), Instant.now().plus(TTL), invitedBy);
        // Il collegamento all'identità che esiste già (UC 0118): serve all'accettazione, e NON esce mai
        // verso chi ha invitato — InvitationView non lo porta, e nessuna interfaccia di account lo vede.
        invitation.setIdentityId(identity == null ? null : identity.getId());
        // Il posto di questo invito è pagato, e qui si scrive con che cosa. Nullo = non era dovuto nulla.
        invitation.setSeatChargeRef(charge.chargeRef());
        repository.persist(invitation);
        repository.flush(); // forza INSERT: id e tenant_id valorizzati prima della response
        // evento audit (UC 0006): id invito, MAI l'email dell'invitato (nessun dato personale, #08/5).
        // Il ruolo non si registra più perché non è più una scelta di chi invita (UC 0100).
        audit.success("member.invited", Map.of(
                "invitation_id", invitation.getId().toString(),
                "actor", caller.subject(),
                // Se il posto è costato: quanti posti a pagamento ha ora l'account. Mai l'importo e mai
                // l'indirizzo — il registro di audit non è il posto della contabilità né dei dati personali.
                "seat_quantity", String.valueOf(charge.newQuantity())));
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
