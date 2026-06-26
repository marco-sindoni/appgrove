package app.appgrove.core.platform;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Optional;
import java.util.UUID;

/** Repository utenti. Tenant-scoped automatico (discriminator): nessun filtro manuale su tenant. */
@ApplicationScoped
public class UserRepository implements PanacheRepositoryBase<User, UUID> {

    /** L'utente del tenant corrente con quel {@code cognito_sub} (identità del chiamante). */
    public Optional<User> findByCognitoSub(String cognitoSub) {
        return find("cognitoSub", cognitoSub).firstResultOptional();
    }
}
