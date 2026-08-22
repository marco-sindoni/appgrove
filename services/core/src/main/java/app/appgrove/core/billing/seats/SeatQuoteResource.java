package app.appgrove.core.billing.seats;

import app.appgrove.core.billing.seats.SeatDtos.SeatSummaryView;
import app.appgrove.core.platform.Roles;
import io.quarkus.security.Authenticated;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.jboss.logging.Logger;

/**
 * Il riquadro dei posti dell'account: {@code GET /api/platform/v1/me/seats} (UC 0103 §6).
 *
 * <p>Riservata all'<b>owner</b>, come tutta la sezione «Members» e come l'invio dell'invito: qui non si
 * leggono tariffe (quelle sono pubbliche dentro il prodotto, {@link SeatPricingResource}) ma <b>quanto
 * paga questo account e quante persone ha</b> — informazione di chi governa il conto. Un collaboratore che
 * la leggesse conoscerebbe il costo della propria azienda senza averne titolo.
 *
 * <p>Nessun parametro: l'account viene dal claim {@code tenant_id} del token verificato (invariante #1) e
 * il conteggio è per costruzione quello di chi chiama.
 *
 * <p><b>Listino assente = errore, non un dovuto zero.</b> Se nessuna versione del listino è vigente,
 * {@link NoSeatPricingVersionException} <b>propaga</b> e la risposta è un errore: un riquadro che dicesse
 * «stai pagando 0,00 €» perché il listino non c'è sarebbe indistinguibile da un account entro la
 * franchigia, e sulla base di quello zero qualcuno inviterebbe una persona senza pagarla. L'interfaccia
 * tratta <b>qualunque</b> errore di questa lettura allo stesso modo — riquadro in errore e invito spento —
 * che è una garanzia più forte del distinguere la causa: non serve indovinare quale guasto sia per sapere
 * che non si può invitare alla cieca.
 *
 * <p>Il tipo di ritorno è la vista e non un {@code Response} di proposito: è così che il contratto
 * pubblicato porta lo schema del corpo, e da quello schema nasce il client dell'interfaccia.
 */
@Path("/api/platform/v1/me/seats")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
public class SeatQuoteResource {

    private static final Logger LOG = Logger.getLogger(SeatQuoteResource.class);

    @Inject
    SeatSubscriptionService seats;

    @GET
    @RolesAllowed(Roles.OWNER)
    @Transactional
    public SeatSummaryView summary() {
        SeatSummaryView view = seats.summary();
        LOG.infof(
                "seats.read used=%d paid=%d due_cents=%d next_seat_cents=%d",
                view.usedSeats(), view.paidSeats(), view.dueCents(), view.next().unitPriceCents());
        return view;
    }
}
