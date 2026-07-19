package app.appgrove.commons.entitlement.projection;

import app.appgrove.commons.entitlement.EntitlementEvents;
import app.appgrove.commons.messaging.MessageQueues;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import java.time.Instant;
import java.util.Optional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * Consuma la coda {@code entitlement-<app_id>} e marca come <b>da rinfrescare</b> la proiezione del
 * tenant indicato (UC 0046). Stesso impianto del consumer di purge GDPR: polling schedulato,
 * conferma solo a esito riuscito, redrive → coda degli scarti in caso di errore.
 *
 * <p><b>Marca, non cancella.</b> L'invalidazione lascia in piedi il valore vecchio: se al momento
 * del rinfresco core non risponde, {@code ProjectedEntitlementService} continua a servire l'ultima
 * verità nota invece di negare l'accesso. Cancellare la riga trasformerebbe ogni disdetta in una
 * finestra di potenziale blocco totale per quel tenant.
 *
 * <p><b>Idempotente</b>: marcare una riga già marcata non cambia nulla, quindi una doppia consegna
 * (semantica "almeno una volta" delle code) è innocua. Un messaggio malformato non viene confermato
 * e finisce negli scarti: non lo si scarta in silenzio, perché significherebbe perdere per sempre
 * un'invalidazione e servire dati vecchi senza saperlo.
 */
@ApplicationScoped
public class EntitlementInvalidationConsumer {

    private static final Logger LOG = Logger.getLogger(EntitlementInvalidationConsumer.class);
    private static final int BATCH = 10;

    @Inject
    EntitlementProjectionStore store;

    // Instance<> lazy: nei servizi senza proiezione (core, auth) e nei test senza code il consumer
    // è inerte e non deve richiedere bean che non esistono.
    @Inject
    Instance<MessageQueues> queuesInstance;

    @Inject
    EntitlementProjectionMetrics metrics;

    @Inject
    ObjectMapper mapper;

    @ConfigProperty(name = "appgrove.app-id")
    Optional<String> appId;

    @Scheduled(every = "2s", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void poll() {
        try {
            drain();
        } catch (RuntimeException e) {
            LOG.debugf(e, "entitlement.invalidation coda non raggiungibile");
        }
    }

    /** Elabora i messaggi disponibili; ritorna quanti ne ha confermati. Pubblico per i test. */
    public int drain() {
        if (!store.enabled() || appId.isEmpty() || queuesInstance.isUnsatisfied()) {
            return 0;
        }
        MessageQueues queues = queuesInstance.get();
        String queue = EntitlementEvents.invalidationQueue(appId.get());
        int processed = 0;
        for (MessageQueues.Message message : queues.receive(queue, BATCH)) {
            EntitlementEvents.InvalidationMessage event;
            try {
                event = mapper.readValue(message.body(), EntitlementEvents.InvalidationMessage.class);
            } catch (JsonProcessingException e) {
                LOG.errorf(e, "entitlement.invalidation messaggio malformato app_id=%s → redrive/scarti", appId.get());
                continue;
            }
            if (event.tenantId() == null || event.tenantId().isBlank()) {
                LOG.errorf("entitlement.invalidation messaggio senza tenant_id app_id=%s → redrive/scarti", appId.get());
                continue;
            }
            try {
                int marked = store.markStale(event.tenantId());
                metrics.invalidationLag(parseInstant(event.occurredAt()));
                LOG.infof(
                        "entitlement.invalidation tenant_id=%s app_id=%s reason=%s righe_marcate=%d",
                        event.tenantId(), appId.get(), event.reason(), marked);
                queues.delete(queue, message);
                processed++;
            } catch (RuntimeException e) {
                // NON confermare → redrive → scarti. Marcare è idempotente, il ritentativo è sicuro.
                LOG.errorf(
                        e,
                        "entitlement.invalidation fallita tenant_id=%s app_id=%s → messaggio non confermato",
                        event.tenantId(), appId.get());
            }
        }
        return processed;
    }

    private static Instant parseInstant(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(value);
        } catch (RuntimeException e) {
            return null; // diagnostica non parsabile: non è una ragione per rifiutare l'invalidazione
        }
    }
}
