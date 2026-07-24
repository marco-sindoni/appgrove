package app.appgrove.core.billing;

import app.appgrove.commons.messaging.MessageQueues;
import app.appgrove.commons.usage.UsageEvents;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import java.time.Instant;
import org.jboss.logging.Logger;

/**
 * Consuma la coda condivisa {@code app-usage} e materializza in {@link AppUsageStore} l'uso a giacenza
 * riportato dalle app (UC 0054). Stesso impianto del consumer di invalidazione entitlement: polling
 * schedulato, conferma solo a esito riuscito, redrive → scarti in caso di errore.
 *
 * <p><b>Idempotente</b>: l'upsert scrive il valore riportato e ignora i report più vecchi dell'ultimo
 * applicato, quindi una doppia consegna (semantica "almeno una volta" delle code) o un riordino sono
 * innocui. Un messaggio malformato non viene confermato e finisce negli scarti: scartarlo in silenzio
 * significherebbe valutare un downgrade su una giacenza sbagliata.
 */
@ApplicationScoped
public class AppUsageConsumer {

    private static final Logger LOG = Logger.getLogger(AppUsageConsumer.class);
    private static final int BATCH = 10;

    @Inject
    AppUsageStore store;

    // Instance<> lazy: nei test senza code il consumer è inerte e non richiede un bean che non esiste.
    @Inject
    Instance<MessageQueues> queuesInstance;

    @Inject
    ObjectMapper mapper;

    @Scheduled(every = "2s", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void poll() {
        try {
            drain();
        } catch (RuntimeException e) {
            LOG.debugf(e, "app-usage coda non raggiungibile");
        }
    }

    /** Elabora i messaggi disponibili; ritorna quanti ne ha confermati. Pubblico per i test. */
    public int drain() {
        if (queuesInstance.isUnsatisfied()) {
            return 0;
        }
        MessageQueues queues = queuesInstance.get();
        int processed = 0;
        for (MessageQueues.Message message : queues.receive(UsageEvents.USAGE_QUEUE, BATCH)) {
            UsageEvents.UsageReport report;
            try {
                report = mapper.readValue(message.body(), UsageEvents.UsageReport.class);
            } catch (JsonProcessingException e) {
                LOG.errorf(e, "app-usage messaggio malformato → redrive/scarti");
                continue;
            }
            if (isBlank(report.appSlug()) || isBlank(report.tenantId()) || isBlank(report.metric())) {
                LOG.errorf("app-usage messaggio incompleto (app/tenant/metrica) → redrive/scarti");
                continue;
            }
            try {
                store.record(
                        report.appSlug(), report.tenantId(), report.metric(),
                        report.value(), parseInstant(report.occurredAt()));
                LOG.infof(
                        "app-usage applicato app=%s tenant_id=%s metrica=%s valore=%d",
                        report.appSlug(), report.tenantId(), report.metric(), report.value());
                queues.delete(UsageEvents.USAGE_QUEUE, message);
                processed++;
            } catch (RuntimeException e) {
                // NON confermare → redrive → scarti. L'upsert è idempotente, il ritentativo è sicuro.
                LOG.errorf(e, "app-usage applicazione fallita app=%s tenant_id=%s → non confermato",
                        report.appSlug(), report.tenantId());
            }
        }
        return processed;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static Instant parseInstant(String value) {
        if (value == null || value.isBlank()) {
            return Instant.EPOCH; // report senza istante: il più vecchio possibile, non "riesuma" nulla
        }
        try {
            return Instant.parse(value);
        } catch (RuntimeException e) {
            return Instant.EPOCH;
        }
    }
}
