package app.appgrove.core.support;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.UUID;

/**
 * Repository dei ticket (letture REST utente): il discriminator aggiunge {@code WHERE tenant_id}
 * a ogni query (invariante #2) → un tenant non vede mai i ticket di un altro (404 naturale).
 */
@ApplicationScoped
public class SupportTicketRepository implements PanacheRepositoryBase<SupportTicket, UUID> {

    public List<SupportTicket> listRecent() {
        return list("order by createdAt desc");
    }
}
