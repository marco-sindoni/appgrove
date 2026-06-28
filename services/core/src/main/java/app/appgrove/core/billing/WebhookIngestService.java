package app.appgrove.core.billing;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

/**
 * Ingest dei webhook (#09 D18a/D19): verifica la firma HMAC e, se valida, accoda sulla coda webhook
 * rispondendo subito (il consumer elabora in modo asincrono). Firma non valida → eccezione → 401,
 * nessun processing. Condiviso dall'endpoint HTTP ({@link PaddleWebhookResource}) e dall'emettitore di
 * scenari ({@code StubScenarioEmitter}), così entrambi passano per la <b>stessa</b> verifica.
 */
@ApplicationScoped
public class WebhookIngestService {

    private static final Logger LOG = Logger.getLogger(WebhookIngestService.class);

    @Inject
    PaddleSignature signature;

    @Inject
    WebhookQueue queue;

    /**
     * Verifica la firma e accoda il corpo grezzo.
     *
     * @throws InvalidWebhookSignatureException se la firma è assente o errata
     */
    public void ingest(String body, String signatureHeader) {
        if (!signature.verify(body, signatureHeader)) {
            LOG.warn("webhook.ingest firma non valida → scartato (401)");
            throw new InvalidWebhookSignatureException();
        }
        queue.send(body);
    }
}
