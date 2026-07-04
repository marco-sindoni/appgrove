package app.appgrove.core.support;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.UUID;

/** Repository dei messaggi di thread (letture REST utente, tenant-filtered dal discriminator). */
@ApplicationScoped
public class SupportTicketMessageRepository implements PanacheRepositoryBase<SupportTicketMessage, UUID> {

    public List<SupportTicketMessage> byTicket(UUID ticketId) {
        return list("ticketId = ?1 order by createdAt", ticketId);
    }
}
