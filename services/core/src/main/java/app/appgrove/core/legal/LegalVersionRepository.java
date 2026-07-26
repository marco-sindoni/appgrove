package app.appgrove.core.legal;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Accesso in lettura alle versioni legali correnti (platform-level, UC 0056). Le scritture (upsert) le fa {@link LegalVersionSyncService} in SQL nativo. */
@ApplicationScoped
public class LegalVersionRepository implements PanacheRepositoryBase<LegalVersion, UUID> {

    public Optional<LegalVersion> findByComponent(LegalComponent component) {
        return find("component", component).firstResultOptional();
    }

    public List<LegalVersion> listCurrent() {
        return listAll();
    }
}
