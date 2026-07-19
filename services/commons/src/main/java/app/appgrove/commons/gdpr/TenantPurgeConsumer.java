package app.appgrove.commons.gdpr;

import app.appgrove.commons.entitlement.projection.EntitlementProjectionStore;
import app.appgrove.commons.messaging.MessageQueues;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import java.util.LinkedHashMap;
import java.util.Map;
import org.jboss.logging.Logger;

/**
 * Consumer riusabile della purge per-tenant (#06 H-19, UC 0032): ogni servizio con
 * {@link AppDataContract} consuma la <b>propria</b> coda {@code tenant-purge-<app_id>} e invoca il
 * proprio {@code purgeData} (erasure fisica), registrando l'<b>audit</b> come prova (#13 L70).
 * In locale i messaggi arrivano dal fan-out diretto del core; nel cloud dalla regola EventBridge
 * (evento {@code tenant.offboarded} → code per-servizio, UC 0004).
 *
 * <p>Semantica errori: purge o audit falliti → messaggio NON confermato → redrive → DLQ (la purge è
 * idempotente: un secondo passaggio cancella 0 righe). Messaggio malformato → redrive/DLQ.
 */
@ApplicationScoped
public class TenantPurgeConsumer {

    private static final Logger LOG = Logger.getLogger(TenantPurgeConsumer.class);
    private static final int BATCH = 10;

    @Inject
    Instance<AppDataContract> contract;

    // Instance<> (lazy): nei servizi senza AppDataContract il consumer è inerte e non deve
    // richiedere le code (nel profilo test esistono solo come @Mock dei servizi partecipanti).
    @Inject
    Instance<MessageQueues> queuesInstance;

    @Inject
    GdprPurgeAuditWriter audit;

    @Inject
    EntitlementProjectionStore projection;

    @Inject
    ObjectMapper mapper;

    /**
     * Aggiunge alla purge del dominio la cancellazione della <b>proiezione entitlement</b> (UC 0046),
     * che è infrastruttura di {@code commons} e non appartiene al contratto dell'app.
     *
     * <p>Va fatta <b>qui</b> e non in ogni {@code AppDataContract}: la proiezione contiene
     * l'identificativo dell'account e il piano che aveva, e lasciarla in piedi dopo un'erasure
     * significherebbe conservare la traccia di chi ha chiesto di sparire. Se dipendesse dalla
     * diligenza di ogni app, prima o poi una se ne dimenticherebbe — in silenzio.
     *
     * <p>Il conteggio entra nell'audit: l'audit è la <b>prova</b> dell'erasure, e una prova
     * incompleta non è una prova.
     */
    private PurgeResult withProjection(PurgeResult result, String tenantId) {
        if (!projection.enabled()) {
            return result;
        }
        int purged = projection.purgeTenant(tenantId);
        Map<String, Integer> deleted = new LinkedHashMap<>(result.deletedByEntity());
        deleted.put("entitlement_projection", purged);
        return new PurgeResult(result.appId(), deleted);
    }

    @Scheduled(every = "2s", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void poll() {
        try {
            drain();
        } catch (RuntimeException e) {
            LOG.debugf(e, "gdpr.purge.poll coda non raggiungibile");
        }
    }

    /** Elabora i messaggi disponibili; ritorna quanti ne ha confermati. Pubblico per i test. */
    public int drain() {
        if (contract.isUnsatisfied()) {
            return 0;
        }
        AppDataContract dataContract = contract.get();
        MessageQueues queues = queuesInstance.get();
        String queue = GdprQueues.purgeQueue(dataContract.appId());
        int processed = 0;
        for (MessageQueues.Message message : queues.receive(queue, BATCH)) {
            TenantPurgeMessage purge;
            try {
                purge = mapper.readValue(message.body(), TenantPurgeMessage.class);
            } catch (JsonProcessingException e) {
                LOG.errorf(e, "gdpr.purge messaggio malformato app_id=%s → redrive/DLQ", dataContract.appId());
                continue;
            }
            try {
                PurgeResult result = withProjection(
                        dataContract.purgeData(new GdprScope(purge.tenantId())), purge.tenantId());
                audit.record(purge.tenantId(), purge.reason(), result);
                LOG.infof("gdpr.purge completata tenant_id=%s app_id=%s reason=%s cancellate=%d",
                        purge.tenantId(), dataContract.appId(), purge.reason(), result.total());
                queues.delete(queue, message);
                processed++;
            } catch (RuntimeException e) {
                // NON confermare → redrive → DLQ; la purge è idempotente, il retry è sicuro.
                LOG.errorf(e, "gdpr.purge fallita tenant_id=%s app_id=%s → messaggio non confermato (redrive/DLQ)",
                        purge.tenantId(), dataContract.appId());
            }
        }
        return processed;
    }
}
