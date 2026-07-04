package app.appgrove.commons.messaging;

import java.util.List;

/**
 * Accesso alle code di messaggi (≈ SQS) indirizzate <b>per nome</b>: usato dal framework GDPR
 * (UC 0032) per le code per-servizio {@code gdpr-export-<app_id>} / {@code tenant-purge-<app_id>}
 * e per la coda risultati condivisa. In dev/prod = {@link SqsMessageQueues} (ElasticMQ/SQS); nei
 * test un'implementazione in-memory ({@code @Mock} nel servizio) per restare deterministici e
 * offline — stesso pattern della {@code WebhookQueue} di core (UC 0023).
 */
public interface MessageQueues {

    /** Accoda {@code body} sulla coda {@code queueName}. */
    void send(String queueName, String body);

    /** Riceve fino a {@code max} messaggi dalla coda (può essere vuoto). */
    List<Message> receive(String queueName, int max);

    /** Conferma l'elaborazione di un messaggio (lo rimuove dalla coda). */
    void delete(String queueName, Message message);

    /** Messaggio in coda: {@code handle} per la delete, {@code body} = payload JSON. */
    record Message(String handle, String body) {}
}
