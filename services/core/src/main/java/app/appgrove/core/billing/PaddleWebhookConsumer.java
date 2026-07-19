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
 * <p>Hardening UC 0025: dedup su {@code event_id}, out-of-order via {@code occurred_at} e set eventi
 * completo sono gestiti nel {@link SubscriptionWriter} (transazionale). Qui il consumer governa il
 * <b>redrive/DLQ</b>: su esito andato a buon fine (applicato, duplicato o stale) conferma ed elimina il
 * messaggio; su <b>errore</b> NON conferma → la coda lo ri-consegna e, dopo {@code maxReceiveCount}
 * (ElasticMQ/SQS), lo instrada in <b>DLQ</b>. L'<b>allarme</b> sulla DLQ è cloud (UC 0025 "Punti aperti").
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

    @Inject
    EntitlementInvalidationPublisher invalidation;

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

    /** Elabora i messaggi disponibili; ritorna quanti ne ha confermati. Pubblico per i test. */
    public int drain() {
        int processed = 0;
        for (WebhookQueue.Message message : queue.receive(BATCH)) {
            try {
                PaddleWebhookEvent event = PaddleWebhookEvent.from(mapper, message.body());
                SubscriptionWriter.Outcome outcome = writer.apply(event);
                // Invalidazione delle proiezioni entitlement (UC 0046) SOLO su PROCESSED: un evento
                // duplicato o out-of-order non ha cambiato lo stato, quindi non c'è nulla da
                // rinfrescare — pubblicare comunque genererebbe rinfreschi inutili e falserebbe la
                // misura di ricorso alla rete di sicurezza, che è la spia di un bus rotto.
                if (outcome == SubscriptionWriter.Outcome.PROCESSED && !event.isCustomerEvent()) {
                    invalidation.invalidate(event.tenantId(), event.appId(), event.eventType());
                }
                queue.delete(message); // esito di successo (applicato/duplicato/stale) → conferma
                processed++;
                LOG.debugf("webhook.consume outcome=%s", outcome);
            } catch (RuntimeException e) {
                // Errore di elaborazione: NON confermare → redrive della coda → dopo maxReceiveCount → DLQ.
                // Log strutturato (invariante #4: MDC valorizzato dal writer/commons) come aggancio allarme.
                LOG.errorf(e, "webhook.consume errore di elaborazione → messaggio non confermato (redrive/DLQ)");
            }
        }
        return processed;
    }
}
