package app.appgrove.core.gdpr;

import app.appgrove.commons.storage.ExportStorage;
import app.appgrove.core.gdpr.GdprDtos.DownloadView;
import app.appgrove.core.gdpr.GdprDtos.JobView;
import app.appgrove.core.gdpr.GdprDtos.StartExport;
import app.appgrove.core.platform.CallerContext;
import app.appgrove.core.platform.Roles;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.Duration;

/**
 * API dei diritti GDPR — export (accesso/portabilità, #13 D22). Capability di piattaforma
 * <b>esente dai gate di enforcement</b> (#09 F31): NIENTE {@code @RequiresEntitlement} — deve
 * rispondere anche con subscription canceled/paused o quota esaurita, per tutta la retention
 * (guardia: {@code GdprGateExemptionTest}). Solo authN + ownership: il tenant arriva dal JWT
 * (invariante #1) e le letture sono tenant-filtered dal discriminator (#2) → i job di un altro
 * tenant sono un 404. Gli endpoint di <b>cancellazione</b> non esistono qui per scelta: recesso
 * per-app → UC 0033, eliminazione account con grace 14gg → UC 0035 (vedi requirements change 0028).
 */
@Path("/api/platform/v1/gdpr/exports")
@RolesAllowed({Roles.OWNER, Roles.ADMIN})
@Produces(MediaType.APPLICATION_JSON)
public class GdprResource {

    /** Validità del link di download (#13 D22: presigned 7 giorni, come il lifecycle del bucket). */
    static final Duration LINK_TTL = Duration.ofDays(7);

    @Inject
    GdprExportService service;

    @Inject
    GdprExportJobRepository jobs;

    @Inject
    GdprExportJobItemRepository items;

    @Inject
    ExportStorage storage;

    @Inject
    CallerContext caller;

    @POST
    @Transactional
    public Response start(@Valid StartExport body) {
        if (body.kind() == GdprExportKind.app && (body.appId() == null || body.appId().isBlank())) {
            throw new BadRequestException("appId obbligatorio per l'export di una singola app");
        }
        GdprExportJob job = service.start(
                body.kind(), body.appId(), caller.tenantId().toString(), caller.subject());
        return Response.accepted(view(job)).build();
    }

    @GET
    @Path("/{id}")
    public JobView get(@PathParam("id") String id) {
        return view(load(id));
    }

    @GET
    @Path("/{id}/download")
    public DownloadView download(@PathParam("id") String id) {
        GdprExportJob job = load(id);
        if (job.getStatus() != GdprExportStatus.COMPLETED || job.getZipKey() == null) {
            throw new WebApplicationException(
                    "export non pronto (stato " + job.getStatus() + ")", Response.Status.CONFLICT);
        }
        ExportStorage.PresignedLink link = storage.presignGet(job.getZipKey(), LINK_TTL);
        return new DownloadView(link.url(), link.expiresAt());
    }

    private GdprExportJob load(String id) {
        GdprExportJob job = jobs.findById(GdprExportService.jobId(id));
        if (job == null) {
            throw new NotFoundException("Export job non trovato");
        }
        return job;
    }

    private JobView view(GdprExportJob job) {
        return JobView.from(job, items.byJob(job.getId()));
    }
}
