package app.appgrove.core.platform;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.UUID;

/** Repository inviti. Tenant-scoped automatico (discriminator). */
@ApplicationScoped
public class InvitationRepository implements PanacheRepositoryBase<Invitation, UUID> {

    /** True se esiste già un invito pending per quell'email nel tenant corrente. */
    public boolean existsPendingForEmail(String email) {
        return count("lower(email) = ?1 and status = ?2", email.toLowerCase(), InvitationStatus.pending) > 0;
    }
}
