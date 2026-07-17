package app.appgrove.commons.gdpr;

import app.appgrove.commons.messaging.MessageQueues;
import app.appgrove.commons.storage.ExportStorage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

/**
 * Worker riusabile dell'export GDPR (UC 0032): ogni servizio che implementa {@link AppDataContract}
 * consuma la <b>propria</b> coda {@code gdpr-export-<app_id>}, esegue {@code exportData} in-process,
 * carica il frammento su storage ({@code jobs/<job_id>/<app_id>.json}) e notifica l'esito sulla coda
 * risultati consumata dal core. Nessuna chiamata HTTP tra servizi (decisione change 0028).
 *
 * <p>Semantica errori: messaggio malformato → non confermato → redrive/DLQ; <b>export fallito</b> →
 * esito {@code FAILED} al core (job FAILED, #13 D22) e messaggio confermato; invio esito fallito
 * (coda giù) → non confermato → redrive (l'export è idempotente: il frammento si sovrascrive).
 * Nei servizi senza contratto (es. auth col provider locale) il worker è inerte. Pattern poller/drain di
 * {@code PaddleWebhookConsumer} (in test lo scheduler è spento e {@link #drain()} è invocato).
 */
@ApplicationScoped
public class GdprExportWorker {

    private static final Logger LOG = Logger.getLogger(GdprExportWorker.class);
    private static final int BATCH = 10;

    @Inject
    Instance<AppDataContract> contract;

    // Instance<> (lazy): nei servizi SENZA AppDataContract (es. auth col provider locale) il worker è inerte e
    // non deve richiedere code/storage — che nel profilo test esistono solo come @Mock dei servizi
    // che partecipano al framework GDPR.
    @Inject
    Instance<MessageQueues> queuesInstance;

    @Inject
    Instance<ExportStorage> storageInstance;

    @Inject
    ObjectMapper mapper;

    @Scheduled(every = "2s", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void poll() {
        try {
            drain();
        } catch (RuntimeException e) {
            // es. ElasticMQ non in esecuzione in locale: non intasare i log dello scheduler.
            LOG.debugf(e, "gdpr.export.poll coda non raggiungibile");
        }
    }

    /** Elabora i messaggi disponibili; ritorna quanti ne ha confermati. Pubblico per i test. */
    public int drain() {
        if (contract.isUnsatisfied()) {
            return 0;
        }
        AppDataContract dataContract = contract.get();
        MessageQueues queues = queuesInstance.get();
        String queue = GdprQueues.exportQueue(dataContract.appId());
        int processed = 0;
        for (MessageQueues.Message message : queues.receive(queue, BATCH)) {
            ExportRequestMessage request;
            try {
                request = mapper.readValue(message.body(), ExportRequestMessage.class);
            } catch (JsonProcessingException e) {
                // messaggio velenoso: NON confermare → redrive → DLQ
                LOG.errorf(e, "gdpr.export messaggio malformato app_id=%s → redrive/DLQ", dataContract.appId());
                continue;
            }
            ExportResultMessage result = export(dataContract, request);
            try {
                queues.send(GdprQueues.EXPORT_RESULTS, mapper.writeValueAsString(result));
            } catch (JsonProcessingException e) {
                throw new IllegalStateException("serializzazione esito export fallita", e);
            }
            queues.delete(queue, message);
            processed++;
        }
        return processed;
    }

    private ExportResultMessage export(AppDataContract dataContract, ExportRequestMessage request) {
        String appId = dataContract.appId();
        try {
            ExportResult exportResult = dataContract.exportData(new GdprScope(request.tenantId()));
            String fragmentKey = GdprQueues.fragmentKey(request.jobId(), appId);
            storageInstance.get().put(fragmentKey, mapper.writeValueAsBytes(exportResult), "application/json");
            LOG.infof("gdpr.export completato tenant_id=%s app_id=%s job_id=%s step=%d",
                    request.tenantId(), appId, request.jobId(), exportResult.steps().size());
            return ExportResultMessage.completed(request.jobId(), appId, exportResult.steps(), fragmentKey);
        } catch (Exception e) {
            LOG.errorf(e, "gdpr.export fallito tenant_id=%s app_id=%s job_id=%s",
                    request.tenantId(), appId, request.jobId());
            return ExportResultMessage.failed(request.jobId(), appId, e.getMessage());
        }
    }
}
