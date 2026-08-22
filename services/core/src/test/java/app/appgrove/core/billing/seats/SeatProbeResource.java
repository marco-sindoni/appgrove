package app.appgrove.core.billing.seats;

import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.Instant;
import java.util.Map;

/**
 * Endpoint di collaudo (<b>solo classpath di test</b>), sullo stampo di {@code MdcProbeResource}.
 *
 * <p><b>Ridotto da UC 0103.</b> Alla nascita (UC 0102) esponeva anche il <b>conteggio dei posti</b>, perché
 * quel conteggio non aveva ancora una superficie di prodotto. Ora ce l'ha — {@code GET
 * /api/platform/v1/me/seats}, il riquadro dei posti — e il collaudo del conteggio è stato agganciato
 * all'operazione <b>vera</b>: un endpoint di collaudo che sopravvive alla superficie che doveva anticipare
 * diventa l'unica cosa che qualcuno prova, e da quel momento la superficie vera non è più coperta da nulla.
 *
 * <p>Resta la <b>selezione della versione per data</b>, e resta per una ragione che non scade: ha bisogno di
 * una data scelta dal collaudo, mentre ogni operazione di prodotto serve — per scelta, UC 0102 §8 — il
 * listino vigente <b>adesso</b>. La data passata è la domanda di UC 0106 («quanto pagava in marzo?»); il
 * giorno in cui quella superficie esisterà, anche questo probe andrà ritirato.
 *
 * <p>Non è un'operazione di prodotto e non finisce nell'artefatto: sta in {@code src/test/java}.
 */
@Path("/test/seats")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
public class SeatProbeResource {

    @Inject
    SeatPricingRepository repository;

    /** La versione vigente a una data: 200 con la sua decorrenza e nota, 404 se nessuna vigeva. */
    @GET
    @Path("/vigente")
    @Transactional
    public Response vigente(@QueryParam("at") String at) {
        return repository
                .findVigenteAl(Instant.parse(at))
                .map(version -> Response.ok(Map.of(
                                "effectiveFrom", version.getEffectiveFrom().toString(),
                                "note", version.getNote() == null ? "" : version.getNote(),
                                "bands", version.getBands().size()))
                        .build())
                .orElseGet(() -> Response.status(Response.Status.NOT_FOUND).build());
    }

    /**
     * La versione vigente a una data <b>pretesa</b>: 200, oppure 409 con il tipo e il messaggio del rifiuto.
     * Serve a provare che il calcolo si <b>nega</b> invece di inventare una tariffa — rifiuto che oggi non ha
     * ancora un consumatore di prodotto (arriverà con UC 0103).
     */
    @GET
    @Path("/vigente-obbligatoria")
    @Transactional
    public Response vigenteObbligatoria(@QueryParam("at") String at) {
        try {
            return Response.ok(Map.of(
                            "effectiveFrom",
                            repository.requireVigenteAl(Instant.parse(at)).getEffectiveFrom().toString()))
                    .build();
        } catch (NoSeatPricingVersionException e) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(Map.of("error", e.getClass().getSimpleName(), "message", e.getMessage()))
                    .build();
        }
    }
}
