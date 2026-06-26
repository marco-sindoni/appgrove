package app.appgrove.commons.persistence;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import org.eclipse.microprofile.jwt.JsonWebToken;

/**
 * Audit-attore: valorizza {@code created_by}/{@code updated_by} con il {@code sub} del JWT verificato
 * (= user_id), in scrittura. Lookup CDI programmatico (Arc): fuori da una richiesta autenticata
 * (boot, job, fixture) il token non è disponibile e l'attore resta {@code null} — fail-soft, mai blocca.
 */
public class AuditListener {

    @PrePersist
    void onPrePersist(BaseEntity entity) {
        String actor = currentActor();
        if (entity.getCreatedBy() == null) {
            entity.setCreatedBy(actor);
        }
        entity.setUpdatedBy(actor);
    }

    @PreUpdate
    void onPreUpdate(BaseEntity entity) {
        entity.setUpdatedBy(currentActor());
    }

    private static String currentActor() {
        try {
            if (Arc.container() == null) {
                return null;
            }
            try (InstanceHandle<JsonWebToken> handle = Arc.container().instance(JsonWebToken.class)) {
                if (handle == null || !handle.isAvailable()) {
                    return null;
                }
                String sub = handle.get().getSubject();
                return (sub == null || sub.isBlank()) ? null : sub;
            }
        } catch (RuntimeException ignored) {
            // nessun contesto di richiesta/token → attore sconosciuto (fail-soft)
            return null;
        }
    }
}
