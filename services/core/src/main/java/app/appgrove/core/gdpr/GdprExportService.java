package app.appgrove.core.gdpr;

import app.appgrove.commons.gdpr.ExportRequestMessage;
import app.appgrove.commons.gdpr.GdprQueues;
import app.appgrove.commons.messaging.MessageQueues;
import app.appgrove.core.catalog.AppRepository;
import app.appgrove.core.billing.SubscriptionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.jboss.logging.Logger;

/**
 * Orchestratore dell'export GDPR (#13 D22, lato richiesta): crea il record {@code gdpr_export_job}
 * con un item per servizio coinvolto e pubblica la richiesta sulle code per-servizio
 * {@code gdpr-export-<app_id>}. Gira <b>in contesto REST</b> (JWT presente): il tenant nei messaggi
 * è quello del discriminator (invariante #1); l'aggregazione degli esiti è del
 * {@link GdprExportResultsConsumer}.
 *
 * <p>Servizi coinvolti: {@code platform} (sempre) + le app <b>attivate</b> dal tenant (una
 * subscription in qualunque stato, anche canceled: i diritti valgono per tutta la retention, #09 F31).
 */
@ApplicationScoped
public class GdprExportService {

    private static final Logger LOG = Logger.getLogger(GdprExportService.class);

    @Inject
    GdprExportJobRepository jobs;

    @Inject
    GdprExportJobItemRepository items;

    @Inject
    SubscriptionRepository subscriptions;

    @Inject
    AppRepository apps;

    @Inject
    MessageQueues queues;

    @Inject
    ObjectMapper mapper;

    @Transactional
    public GdprExportJob start(GdprExportKind kind, String appSlug, String tenantId, String subject) {
        List<String> targets = targetsFor(kind, appSlug);

        GdprExportJob job = new GdprExportJob(kind, kind == GdprExportKind.app ? appSlug : null);
        jobs.persist(job);
        for (String target : targets) {
            items.persist(new GdprExportJobItem(job.getId(), target));
        }
        jobs.flush();

        // Publish dopo la persist: se un send fallisce la transazione fa rollback (niente job
        // fantasma); un messaggio già inviato produce al più un frammento orfano che il consumer
        // risultati scarta (job inesistente) — innocuo, lo storage si auto-pulisce (lifecycle 7gg).
        for (String target : targets) {
            queues.send(GdprQueues.exportQueue(target),
                    serialize(new ExportRequestMessage(job.getId().toString(), tenantId, target)));
        }
        job.setStatus(GdprExportStatus.RUNNING);

        LOG.infof("gdpr.export.start tenant_id=%s user_id=%s job_id=%s kind=%s targets=%s",
                tenantId, subject, job.getId(), kind, targets);
        return job;
    }

    /** Servizi da interpellare; per {@code kind=app} valida che l'app sia stata attivata dal tenant. */
    private List<String> targetsFor(GdprExportKind kind, String appSlug) {
        List<String> activated = activatedAppSlugs();
        if (kind == GdprExportKind.app) {
            if (!activated.contains(appSlug)) {
                throw new NotFoundException("Nessuna app '" + appSlug + "' attivata per questo account");
            }
            return List.of(appSlug);
        }
        List<String> targets = new ArrayList<>();
        targets.add(PlatformDataContract.APP_ID);
        targets.addAll(activated);
        return targets;
    }

    /** Slug delle app con una subscription del tenant corrente (lettura tenant-filtered, #2). */
    private List<String> activatedAppSlugs() {
        return subscriptions.listAll().stream()
                .map(s -> s.getAppId())
                .distinct()
                .map(appId -> apps.findById(appId))
                .filter(app -> app != null)
                .map(app -> app.getSlug())
                .sorted()
                .toList();
    }

    private String serialize(ExportRequestMessage message) {
        try {
            return mapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("serializzazione richiesta export fallita", e);
        }
    }

    /** UUID del job da path param, o 404 (mai 500 su input malformato). */
    public static UUID jobId(String raw) {
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException e) {
            throw new NotFoundException("Export job non trovato");
        }
    }
}
