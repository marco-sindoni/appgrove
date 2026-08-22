package app.appgrove.core.billing.seats;

import app.appgrove.core.billing.seats.SeatPricingDtos.SeatBandView;
import app.appgrove.core.billing.seats.SeatPricingDtos.SeatPricingView;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.time.Instant;
import org.jboss.logging.Logger;

/**
 * Il listino dei posti vigente: {@code GET /api/platform/v1/seat-pricing} (UC 0102 §8).
 *
 * <p>Aperta a <b>qualunque</b> persona autenticata, non solo all'owner: leggere quanto costa un posto non
 * richiede il diritto di comprarlo, e la pagina dei prezzi la vede chiunque. Il divieto vero sta a valle —
 * comprare è di UC 0103, cambiare le tariffe è dell'amministratore di piattaforma (UC 0105) — e nulla di
 * ciò che si serve qui lo aggira: sono tariffe, uguali per tutti gli account.
 *
 * <p>Nessun parametro, e in particolare nessuna data: si serve il listino <b>vigente adesso</b>. Il listino
 * di una data passata serve a ricostruire una fattura, cioè a UC 0106, e una data che arriva dal chiamante
 * sarebbe il modo di farsi dire che prezzo si pagava prima — informazione senza pericolo, ma senza nemmeno
 * un consumatore: si aggiunge quando qualcuno ne ha bisogno.
 */
@Path("/api/platform/v1/seat-pricing")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
public class SeatPricingResource {

    private static final Logger LOG = Logger.getLogger(SeatPricingResource.class);

    @Inject
    SeatPricingRepository repository;

    @GET
    @Transactional
    public SeatPricingView current() {
        SeatPricingVersion version = repository.requireVigenteAl(Instant.now());
        SeatPricingView view = new SeatPricingView(
                version.getCurrency(),
                version.getEffectiveFrom(),
                version.getBands().stream()
                        .map(band -> new SeatBandView(
                                band.getFromSeat(), band.getToSeat(), band.getUnitPriceCents()))
                        .toList());
        LOG.infof("seat-pricing.read effective_from=%s bands=%d", view.effectiveFrom(), view.bands().size());
        return view;
    }
}
