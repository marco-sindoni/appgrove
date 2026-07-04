package app.appgrove.core.gdpr;

import app.appgrove.commons.gdpr.ExportResultMessage;
import app.appgrove.commons.gdpr.GdprQueues;
import app.appgrove.commons.messaging.MessageQueues;
import app.appgrove.commons.storage.ExportStorage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.jboss.logging.Logger;

/**
 * Consumer della coda risultati (solo core, UC 0032): aggiorna item/job man mano che i servizi
 * finiscono e, quando <b>tutti</b> hanno completato, aggrega i frammenti nello <b>ZIP</b> finale
 * ({@code jobs/<job_id>/export.zip}) e chiude il job (#13 D22). Un esito FAILED marca il job FAILED
 * (il ticket privacy automatico dipende dal ticketing → UC 0034).
 *
 * <p>Errori: esito orfano (job inesistente) → scartato; errore di elaborazione → messaggio NON
 * confermato → redrive/DLQ (aggiornamenti idempotenti, il retry è sicuro).
 */
@ApplicationScoped
public class GdprExportResultsConsumer {

    private static final Logger LOG = Logger.getLogger(GdprExportResultsConsumer.class);
    private static final int BATCH = 10;

    @Inject
    MessageQueues queues;

    @Inject
    ExportStorage storage;

    @Inject
    GdprJobStore store;

    @Inject
    ObjectMapper mapper;

    @Scheduled(every = "2s", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void poll() {
        try {
            drain();
        } catch (RuntimeException e) {
            LOG.debugf(e, "gdpr.results.poll coda non raggiungibile");
        }
    }

    /** Elabora gli esiti disponibili; ritorna quanti ne ha confermati. Pubblico per i test. */
    public int drain() {
        int processed = 0;
        for (MessageQueues.Message message : queues.receive(GdprQueues.EXPORT_RESULTS, BATCH)) {
            ExportResultMessage result;
            try {
                result = mapper.readValue(message.body(), ExportResultMessage.class);
            } catch (JsonProcessingException e) {
                LOG.errorf(e, "gdpr.results messaggio malformato → redrive/DLQ");
                continue;
            }
            try {
                GdprJobStore.Outcome outcome = store.applyResult(result);
                switch (outcome) {
                    case ALL_COMPLETED -> completeJob(result);
                    case UNKNOWN_JOB -> LOG.warnf(
                            "gdpr.results esito orfano scartato job_id=%s app_id=%s", result.jobId(), result.appId());
                    case JOB_FAILED -> LOG.errorf(
                            "gdpr.results job FAILED job_id=%s app_id=%s error=%s",
                            result.jobId(), result.appId(), result.error());
                    case UPDATED -> LOG.debugf(
                            "gdpr.results progress job_id=%s app_id=%s", result.jobId(), result.appId());
                }
                queues.delete(GdprQueues.EXPORT_RESULTS, message);
                processed++;
            } catch (RuntimeException e) {
                LOG.errorf(e, "gdpr.results errore di elaborazione job_id=%s → messaggio non confermato (redrive/DLQ)",
                        result.jobId());
            }
        }
        return processed;
    }

    private void completeJob(ExportResultMessage result) {
        UUID jobId = UUID.fromString(result.jobId());
        byte[] zip = buildZip(store.fragments(jobId));
        String zipKey = GdprQueues.zipKey(result.jobId());
        storage.put(zipKey, zip, "application/zip");
        store.markCompleted(jobId, zipKey);
        LOG.infof("gdpr.export job COMPLETED job_id=%s zip=%s", jobId, zipKey);
    }

    /** ZIP con un file {@code <app_id>.json} per ogni frammento (i JSON prodotti dai contratti). */
    private byte[] buildZip(Map<String, String> fragmentsByApp) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            for (Map.Entry<String, String> fragment : fragmentsByApp.entrySet()) {
                zip.putNextEntry(new ZipEntry(fragment.getKey() + ".json"));
                zip.write(storage.get(fragment.getValue()));
                zip.closeEntry();
            }
        } catch (IOException e) {
            throw new UncheckedIOException("assemblaggio ZIP export fallito", e);
        }
        return out.toByteArray();
    }
}
