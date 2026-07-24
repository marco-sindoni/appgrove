package app.appgrove.crm;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.UUID;

/** Repository delle interazioni. Tenant-scoped automatico (discriminator). */
@ApplicationScoped
public class InteractionRepository implements PanacheRepositoryBase<Interaction, UUID> {

    /** Interazioni di un contatto, dalla più recente. Il filtro per tenant è già implicito. */
    public PanacheQuery<Interaction> forContact(UUID contactId) {
        return find("contact.id = ?1 order by occurredOn desc, createdAt desc", contactId);
    }
}
