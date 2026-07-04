package app.appgrove.core.gdpr;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.UUID;

/** Repository degli item per-servizio (letture REST, tenant-filtered dal discriminator). */
@ApplicationScoped
public class GdprExportJobItemRepository implements PanacheRepositoryBase<GdprExportJobItem, UUID> {

    public List<GdprExportJobItem> byJob(UUID jobId) {
        return list("jobId = ?1 order by appId", jobId);
    }
}
