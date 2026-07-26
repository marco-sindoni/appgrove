package app.appgrove.core.legal;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.UUID;

/**
 * Log accettazioni legali (tenant/utente-scoped, UC 0056). Il filtro {@code WHERE tenant_id = ?} è
 * automatico (discriminator): questi metodi vanno usati dentro una richiesta autenticata.
 */
@ApplicationScoped
public class LegalAcceptanceRepository implements PanacheRepositoryBase<LegalAcceptance, UUID> {

    /** Tutte le accettazioni dell'utente nel tenant corrente (per derivare lo stato). */
    public List<LegalAcceptance> findByUser(String userId) {
        return list("userId", userId);
    }

    /** Idempotenza del POST: esiste già un'accettazione per (utente, componente, versione)? */
    public boolean exists(String userId, LegalComponent component, String version) {
        return count("userId = ?1 and component = ?2 and version = ?3", userId, component, version) > 0;
    }
}
