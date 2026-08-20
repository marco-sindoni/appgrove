package app.appgrove.core.platform;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository dell'identità della persona (UC 0116). <b>NON</b> è tenant-scoped: l'identità è
 * un'entità di piattaforma, quindi qui non c'è (e non deve esserci) alcun filtro per account —
 * l'assenza è deliberata, non una dimenticanza.
 *
 * <p>Conseguenza operativa: dentro un percorso di account l'identità si raggiunge <b>solo</b>
 * passando per un'appartenenza dello stesso account ({@link MembershipRepository}). Interrogare
 * direttamente per indirizzo di posta o per identificativo di autenticazione è ammesso ai soli
 * percorsi di accesso e di piattaforma.
 */
@ApplicationScoped
public class IdentityRepository implements PanacheRepositoryBase<Identity, UUID> {

    /** L'identità con quell'identificativo di autenticazione (unico globalmente). */
    public Optional<Identity> findByCognitoSub(String cognitoSub) {
        return find("cognitoSub", cognitoSub).firstResultOptional();
    }

    /** L'identità con quell'indirizzo di posta (unico globalmente, confronto in minuscolo). */
    public Optional<Identity> findByEmail(String email) {
        return find("lower(email) = ?1", email == null ? null : email.toLowerCase())
                .firstResultOptional();
    }
}
