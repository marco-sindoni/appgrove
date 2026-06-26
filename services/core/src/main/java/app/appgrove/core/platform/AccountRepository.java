package app.appgrove.core.platform;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.UUID;

/** Repository dell'account (radice tenant). Accesso per {@code id = tenant_id} (non discriminator). */
@ApplicationScoped
public class AccountRepository implements PanacheRepositoryBase<Account, UUID> {
}
