package app.appgrove.core.billing;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Endpoint di ingest dei webhook Paddle (#09 D19). <b>Pubblico</b>, fuori dall'authorizer di business:
 * è un canale server-to-server autenticato dalla <b>firma HMAC</b> (#09 D18a / UC 0025 §8), non dal JWT.
 * Il tenant non arriva mai da input client non firmato: viaggia nei {@code custom_data} del payload
 * firmato (invariante #1 preservato nello spirito — tenant non falsificabile).
 *
 * <p>UC 0023 fornisce la versione <b>locale</b>; UC 0025 ne farà la Lambda di ingest dietro API GW con
 * dedup {@code event_id}/out-of-order/DLQ.
 */
@Path("/api/platform/v1/webhooks/paddle")
public class PaddleWebhookResource {

    @Inject
    WebhookIngestService ingest;

    /** Firma valida → 200 + accodato; firma assente/errata → 401, nessun processing. */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response receive(String body, @HeaderParam("Paddle-Signature") String signature) {
        try {
            ingest.ingest(body, signature);
            return Response.ok().build();
        } catch (InvalidWebhookSignatureException e) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
    }
}
