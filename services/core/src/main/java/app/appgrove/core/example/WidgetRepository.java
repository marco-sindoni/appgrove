package app.appgrove.core.example;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.UUID;

/** Repository Panache per l'harness multitenancy. Nessun filtro tenant manuale: lo aggiunge il discriminator. */
@ApplicationScoped
public class WidgetRepository implements PanacheRepositoryBase<Widget, UUID> {
}
