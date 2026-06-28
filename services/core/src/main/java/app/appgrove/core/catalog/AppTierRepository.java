package app.appgrove.core.catalog;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Repository dei tier di catalogo. */
@ApplicationScoped
public class AppTierRepository implements PanacheRepositoryBase<AppTier, UUID> {

    public List<AppTier> listByApp(UUID appId) {
        return list("appId", appId);
    }

    public Optional<AppTier> findByAppAndKey(UUID appId, String key) {
        return find("appId = ?1 and tierKey = ?2", appId, key).firstResultOptional();
    }
}
