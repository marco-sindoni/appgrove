package app.appgrove.core.billing.seats;

import app.appgrove.commons.web.ProblemDetail;
import app.appgrove.core.billing.seats.SeatDowngradeDtos.ReductionPreview;
import app.appgrove.core.billing.seats.SeatDowngradeDtos.ReductionView;
import app.appgrove.core.billing.seats.SeatDowngradeDtos.RequestReduction;
import app.appgrove.core.platform.Roles;
import io.quarkus.security.Authenticated;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jboss.logging.Logger;

/**
 * La <b>riduzione dei posti in attesa</b> vista dalla rete (UC 0104):
 * {@code /api/platform/v1/me/seats/reduction}.
 *
 * <p>Tutto riservato all'<b>owner</b>, come il riquadro dei posti e come l'invito: qui si decide chi
 * cesserà di far parte dell'account e quanto l'account pagherà dal mese prossimo. Un collaboratore che
 * potesse leggere l'elenco degli indicati saprebbe di un licenziamento prima della persona interessata.
 *
 * <p>Nessun parametro di account: viene dal claim {@code tenant_id} del token verificato (invariante #1).
 *
 * <p><b>I rifiuti sono leciti e tipizzati.</b> Ognuno porta un identificativo stabile nel campo
 * {@code type} del corpo problem+json, perché l'interfaccia deve poter mostrare cinque testi diversi in
 * cinque lingue senza interpretare un messaggio italiano. Il codice è {@code 409}: sono conflitti con lo
 * stato dell'account, non mancanze di permesso — chi chiede è proprio l'owner, che di permessi ne ha tutti.
 */
@Path("/api/platform/v1/me/seats/reduction")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SeatDowngradeResource {

    private static final Logger LOG = Logger.getLogger(SeatDowngradeResource.class);

    @Inject
    SeatDowngradeService reductions;

    /**
     * Lo stato dell'attesa. {@code 204} quando non c'è nulla in attesa: è uno stato <b>normale</b>, non un
     * «non trovato» — la maggior parte degli account non ha alcuna riduzione programmata, e rispondere
     * {@code 404} costringerebbe ogni interfaccia a trattare la normalità come un errore.
     */
    @GET
    @RolesAllowed(Roles.OWNER)
    @Transactional
    public Response current() {
        Optional<ReductionView> pending = reductions.pending();
        return pending.map(view -> Response.ok(view).build())
                .orElseGet(() -> Response.noContent().build());
    }

    /**
     * L'effetto <b>prima della conferma</b> (UC 0104 §4.2): «cesseranno il 14 settembre; dal 15 pagherai
     * 17,94 € invece di 24,91 €», col numero di posti risultante e la composizione degli scaglioni.
     *
     * <p>Le persone arrivano come parametri ripetuti ({@code ?userId=…&userId=…}) e non in un corpo, perché
     * questa è una <b>lettura</b>: si ripete a ogni casella spuntata e non crea nulla. Un {@code POST} per
     * ottenere una stima avrebbe reso impossibile riconoscerla come innocua da fuori.
     */
    @GET
    @jakarta.ws.rs.Path("/preview")
    @RolesAllowed(Roles.OWNER)
    @Transactional
    public ReductionPreview preview(@QueryParam("userId") List<UUID> userIds) {
        return reductions.preview(userIds == null ? List.of() : userIds);
    }

    /** Indica le persone da cessare: un atto unico, con una data di esecuzione comune. */
    @POST
    @RolesAllowed(Roles.OWNER)
    @Transactional
    public Response request(@Valid RequestReduction body) {
        try {
            ReductionView view = reductions.request(body.userIds());
            LOG.infof(
                    "seats.reduction.created people=%d execute_at=%s seats_after=%d due_after_cents=%d",
                    view.people().size(), view.executeAt(), view.seatsAfter(), view.dueCentsAfter());
            return Response.status(Response.Status.CREATED).entity(view).build();
        } catch (SeatDowngradeRefusedException e) {
            return refused(e);
        }
    }

    /** Annulla l'intera attesa. Nessun effetto contabile: la quantità non era mai stata cambiata. */
    @DELETE
    @RolesAllowed(Roles.OWNER)
    @Transactional
    public Response cancel() {
        try {
            reductions.cancel();
            return Response.noContent().build();
        } catch (SeatDowngradeRefusedException e) {
            return refused(e);
        }
    }

    /**
     * Toglie una singola persona dall'elenco degli indicati. Se non ne resta nessuna, l'attesa si chiude
     * da sé — e la risposta è la stessa: da fuori «ho tolto la persona» è riuscito in entrambi i casi, e la
     * schermata rilegge lo stato comunque.
     */
    @DELETE
    @jakarta.ws.rs.Path("/people/{userId}")
    @RolesAllowed(Roles.OWNER)
    @Transactional
    public Response removePerson(@PathParam("userId") UUID userId) {
        try {
            boolean stillPending = reductions.removeItem(userId);
            LOG.infof("seats.reduction.person-removed user_id=%s still_pending=%s", userId, stillPending);
            return Response.noContent().build();
        } catch (SeatDowngradeRefusedException e) {
            return refused(e);
        }
    }

    /**
     * Rifiuto lecito, restituito come {@code Response} e non sollevato come eccezione: il mappatore delle
     * eccezioni web riscrive sempre {@code type} ad {@code about:blank}, e quel campo è l'unico modo di dire
     * a un programma <i>quale</i> rifiuto è stato.
     */
    private static Response refused(SeatDowngradeRefusedException e) {
        int status = Response.Status.CONFLICT.getStatusCode();
        return Response.status(status)
                .type(ProblemDetail.MEDIA_TYPE)
                .entity(new ProblemDetail(e.getType(), "Conflict", status, e.getMessage(), null, null))
                .build();
    }
}
