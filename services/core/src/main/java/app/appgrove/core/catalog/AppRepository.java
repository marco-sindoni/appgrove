package app.appgrove.core.catalog;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Optional;
import java.util.UUID;

/** Repository del catalogo app (platform-level, non tenant-scoped). */
@ApplicationScoped
public class AppRepository implements PanacheRepositoryBase<App, UUID> {

    public Optional<App> findBySlug(String slug) {
        return find("slug", slug).firstResultOptional();
    }
}
