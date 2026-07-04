package app.appgrove.core.gdpr;

import app.appgrove.commons.gdpr.GdprQueues;
import app.appgrove.commons.gdpr.TenantPurgeMessage;
import app.appgrove.commons.messaging.MessageQueues;
import app.appgrove.core.billing.PaymentProvider;
import app.appgrove.core.billing.Subscription;
import app.appgrove.core.billing.SubscriptionRepository;
import app.appgrove.core.catalog.App;
import app.appgrove.core.catalog.AppRepository;
import app.appgrove.core.gdpr.GdprDtos.StartWithdrawal;
import app.appgrove.core.gdpr.GdprDtos.WithdrawalView;
import app.appgrove.core.platform.CallerContext;
import app.appgrove.core.platform.Roles;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.ResponseStatus;

/**
 * Recesso per-app (art. 17, UC 0033, #13 D19/E23): flusso <b>esporta → conferma → cancella
 * immediata</b>. La conferma richiede la prova del passo "esporta": un export job <b>per-app</b>
 * dell'app indicata già {@code COMPLETED} (letto tenant-scoped: il job di un altro tenant è un
 * 404). Alla conferma: attivazione rimossa (soft-delete delle subscription della coppia
 * tenant/app) e purge dei dati dell'app pubblicata sulla coda {@code tenant-purge-<app>} (consumer
 * e audit della change 0028). Diritto esente dai gate (#09 F31): niente
 * {@code @RequiresEntitlement}. OWNER/ADMIN come l'export account (decisione 6, change 0028: i
 * dati delle app appartengono all'account).
 */
@Path("/api/platform/v1/gdpr/apps/{appSlug}/withdrawal")
@RolesAllowed({Roles.OWNER, Roles.ADMIN})
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class GdprWithdrawalResource {

    private static final Logger LOG = Logger.getLogger(GdprWithdrawalResource.class);

    @Inject
    AppRepository apps;

    @Inject
    SubscriptionRepository subscriptions;

    @Inject
    GdprExportJobRepository exportJobs;

    @Inject
    PaymentProvider provider;

    @Inject
    MessageQueues queues;

    @Inject
    ObjectMapper mapper;

    @Inject
    CallerContext caller;

    @POST
    @Transactional
    @ResponseStatus(202)
    public WithdrawalView withdraw(@PathParam("appSlug") String appSlug, @Valid StartWithdrawal body) {
        App app = apps.findBySlug(appSlug)
                .orElseThrow(() -> new NotFoundException("App non trovata: " + appSlug));
        List<Subscription> activations = subscriptions.list("appId", app.getId());
        if (activations.isEmpty()) {
            throw new NotFoundException("Nessuna app '" + appSlug + "' attivata per questo account");
        }
        requireCompletedExport(body.exportJobId(), appSlug);

        // Billing best-effort: se esiste una subscription reale attiva la si disdice anche presso
        // il provider (in dev lo stub è un no-op; semantica con Paddle reale tracciata su UC 0025).
        for (Subscription sub : activations) {
            if (sub.getPaddleSubscriptionId() != null && sub.getStatus().grantsAccess()) {
                provider.cancelSubscription(new PaymentProvider.SubscriptionRef(
                        caller.tenantId().toString(), app.getId(), sub.getPaddleSubscriptionId()));
            }
            sub.markDeleted(); // attivazione rimossa: l'app sparisce da entitlement/export account
        }

        queues.send(GdprQueues.purgeQueue(appSlug),
                serialize(new TenantPurgeMessage(caller.tenantId().toString(), "app-withdrawal")));

        LOG.infof("gdpr.withdrawal tenant_id=%s app_id=%s user_id=%s export_job=%s",
                caller.tenantId(), appSlug, caller.subject(), body.exportJobId());
        return new WithdrawalView(appSlug, "PURGE_REQUESTED");
    }

    /** Prova del passo "esporta": job per-app dell'app indicata, del tenant corrente, COMPLETED. */
    private void requireCompletedExport(String exportJobId, String appSlug) {
        GdprExportJob job = exportJobs.findById(GdprExportService.jobId(exportJobId));
        if (job == null) {
            throw new NotFoundException("Export job non trovato");
        }
        if (job.getKind() != GdprExportKind.app || !appSlug.equals(job.getAppId())) {
            throw new ClientErrorException(
                    "L'export indicato non è l'export per-app di '" + appSlug + "'",
                    Response.Status.CONFLICT);
        }
        if (job.getStatus() != GdprExportStatus.COMPLETED) {
            throw new ClientErrorException(
                    "Export non completato (stato " + job.getStatus() + "): completa prima l'export",
                    Response.Status.CONFLICT);
        }
    }

    private String serialize(TenantPurgeMessage message) {
        try {
            return mapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("serializzazione messaggio purge fallita", e);
        }
    }
}
