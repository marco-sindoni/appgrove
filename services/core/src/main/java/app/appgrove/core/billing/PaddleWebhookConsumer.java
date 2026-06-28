package app.appgrove.core.billing;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

/**
 * Consumer dei webhook (#09 D19): legge dalla coda e applica gli eventi alla {@code subscription} in
 * modo idempotente. In dev il drain è schedulato (poller); nei test lo scheduler è disabilitato e il
 * drain è invocato esplicitamente (deterministico).
 *
 * <p>Rigore <b>Minimo</b> (UC 0023): HMAC (a monte, ingest) + idempotenza (upsert). La <b>dedup</b> su
 * {@code event_id}, l'<b>out-of-order</b> via {@code occurred_at} e <b>DLQ + allarmi</b> sono di UC 0025
 * (qui un errore lascia il messaggio in coda, senza DLQ).
 */
@ApplicationScoped
public class PaddleWebhookConsumer {

    private static final Logger LOG = Logger.getLogger(PaddleWebhookConsumer.class);
    private static final int BATCH = 10;

    @Inject
    WebhookQueue queue;

    @Inject
    SubscriptionWriter writer;

    @Inject
    ObjectMapper mapper;

    /** Poller schedulato (solo dev/prod; in test {@code quarkus.scheduler.enabled=false}). */
    @Scheduled(every = "2s", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void poll() {
        try {
            drain();
        } catch (RuntimeException e) {
            // es. ElasticMQ non in esecuzione in locale: non intasare i log dello scheduler.
            LOG.debugf(e, "webhook.poll coda non raggiungibile");
        }
    }

    /** Elabora i messaggi disponibili; ritorna quanti ne ha applicati. Pubblico per i test. */
    public int drain() {
        int processed = 0;
        for (WebhookQueue.Message message : queue.receive(BATCH)) {
            try {
                writer.apply(PaddleWebhookEvent.from(mapper, message.body()));
                queue.delete(message);
                processed++;
            } catch (RuntimeException e) {
                // UC 0023 (Minimo): logga e lascia il messaggio in coda. Retry/DLQ/allarme → UC 0025.
                LOG.errorf(e, "webhook.consume errore di elaborazione → messaggio non confermato");
            }
        }
        return processed;
    }
}
