package app.appgrove.core.billing;

import java.util.List;

/**
 * Coda dei webhook (≈ SQS): disaccoppia ricezione (ingest, 200 subito) da elaborazione (consumer)
 * (#09 D19). In dev/prod = {@code SqsWebhookQueue} (ElasticMQ/SQS); nei test è sostituita da una
 * implementazione in-memory ({@code @Mock}) per restare deterministici e offline.
 */
public interface WebhookQueue {

    /** Accoda il corpo grezzo (già verificato in firma) del webhook. */
    void send(String body);

    /** Riceve fino a {@code max} messaggi (può essere vuoto). */
    List<Message> receive(int max);

    /** Conferma l'elaborazione di un messaggio (lo rimuove dalla coda). */
    void delete(Message message);

    /** Messaggio in coda: {@code handle} per la delete, {@code body} = payload webhook grezzo. */
    record Message(String handle, String body) {}
}
