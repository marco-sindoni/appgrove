package app.appgrove.core.billing;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository della subscription. Le letture passano dal discriminator (tenant dal JWT): ogni query
 * è automaticamente {@code WHERE tenant_id = :tid} (invariante #2). La <b>scrittura</b> dal consumer
 * webhook — che gira fuori da una richiesta autenticata — usa invece {@code SubscriptionWriter}
 * (SQL nativo, tenant esplicito dal payload firmato).
 */
@ApplicationScoped
public class SubscriptionRepository implements PanacheRepositoryBase<Subscription, UUID> {

    /** Subscription del tenant corrente per l'app indicata (al più una, vincolo unico per-app). */
    public Optional<Subscription> findByApp(UUID appId) {
        return find("appId", appId).firstResultOptional();
    }
}
