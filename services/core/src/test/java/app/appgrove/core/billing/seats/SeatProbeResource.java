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
 * <p>Esiste perché due pezzi di questa storia sono corretti solo <b>dentro una richiesta autenticata</b> e
 * non hanno ancora una superficie di prodotto che li esponga:
 *
 * <ul>
 *   <li>il <b>conteggio dei posti</b> prende il perimetro dal claim {@code tenant_id} attraverso il
 *       discriminatore; fuori da una richiesta il risolutore del tenant è fail-closed, quindi non c'è modo
 *       di provarlo da un collaudo di unità. La superficie vera arriva con UC 0103;</li>
 *   <li>la <b>selezione della versione per data</b> ha bisogno di una data scelta dal collaudo, mentre
 *       l'operazione di prodotto serve sempre il listino vigente adesso (per scelta, UC 0102 §8).</li>
 * </ul>
 *
 * <p>Non è un'operazione di prodotto e non finisce nell'artefatto: sta in {@code src/test/java}.
 */
@Path("/test/seats")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
public class SeatProbeResource {

    @Inject
    SeatCount seatCount;

    @Inject
    SeatPricingRepository repository;

    /** I posti occupati dall'account del token, a un istante scelto (la scadenza degli inviti dipende da esso). */
    @GET
    @Path("/count")
    @Transactional
    public Map<String, Object> count(@QueryParam("at") String at) {
        Instant when = at == null ? Instant.now() : Instant.parse(at);
        return Map.of("seats", seatCount.occupiedSeatsAt(when));
    }

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
