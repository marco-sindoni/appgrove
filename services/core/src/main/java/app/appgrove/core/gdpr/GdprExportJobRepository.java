package app.appgrove.core.gdpr;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.UUID;

/**
 * Repository dei job di export (letture REST): il discriminator aggiunge {@code WHERE tenant_id}
 * a ogni query (invariante #2) → un tenant non vede mai i job di un altro (404 naturale).
 */
@ApplicationScoped
public class GdprExportJobRepository implements PanacheRepositoryBase<GdprExportJob, UUID> {

    public List<GdprExportJob> listRecent() {
        return list("order by createdAt desc");
    }
}
