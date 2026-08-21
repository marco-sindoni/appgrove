package app.appgrove.core.platform;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository degli accessi per applicazione (UC 0098). Tenant-scoped automatico (discriminatore): ogni
 * metodo filtra {@code WHERE tenant_id = ?} senza codice manuale — <b>non</b> va riscritto a mano, e non
 * esiste qui alcuna lettura trasversale agli account.
 *
 * <p>Le righe cancellate sono fuori da ogni lettura ({@code @SQLRestriction} sull'entità): revocare e
 * riconcedere è quindi possibile, e la riconcessione crea una riga nuova invece di resuscitare la vecchia.
 */
@ApplicationScoped
public class AppAccessRepository implements PanacheRepositoryBase<AppAccess, UUID> {

    /** «Quali applicazioni vede questa persona?» — la domanda del menu laterale (UC 0099). */
    public List<AppAccess> findByIdentity(UUID identityId) {
        return list("identityId", identityId);
    }

    /** «Chi ha accesso a questa applicazione?» — senza l'owner, che non ha righe (UC 0111). */
    public List<AppAccess> findByApp(UUID appId) {
        return list("appId", appId);
    }

    /** L'accesso vivo di quella persona a quella applicazione, se esiste. */
    public Optional<AppAccess> findOne(UUID appId, UUID identityId) {
        return find("appId = ?1 and identityId = ?2", appId, identityId).firstResultOptional();
    }

    /**
     * Il ruolo di quella persona su quella applicazione, o vuoto se non ha accesso. È l'ingrediente che
     * {@link AppAccessRules} pretende dal chiamante: la regola non tocca la banca dati, la lettura sì.
     */
    public Optional<AppRole> roleOf(UUID appId, UUID identityId) {
        return findOne(appId, identityId).map(AppAccess::getRole);
    }
}
