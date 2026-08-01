package app.appgrove.core.billing;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.UUID;

/**
 * Repository dello storico pagamenti. Le letture passano dal discriminator di multi-tenancy (tenant dal
 * token verificato): ogni query è automaticamente {@code WHERE tenant_id = :tid} (invariante #2). La
 * <b>scrittura</b> è del {@link SubscriptionWriter}, che gira fuori da una richiesta autenticata.
 */
@ApplicationScoped
public class BillingTransactionRepository implements PanacheRepositoryBase<BillingTransaction, UUID> {

    /** Le transazioni del tenant corrente, dalla più recente: è l'ordine in cui la pagina le mostra. */
    public List<BillingTransaction> listRecentFirst() {
        return list("order by billedAt desc, id desc");
    }
}
