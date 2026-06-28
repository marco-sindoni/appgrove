package app.appgrove.core.billing;

/** Firma HMAC del webhook non valida → l'ingest risponde 401, nessun processing (#09 D18a). */
public class InvalidWebhookSignatureException extends RuntimeException {
    public InvalidWebhookSignatureException() {
        super("Firma webhook non valida");
    }
}
