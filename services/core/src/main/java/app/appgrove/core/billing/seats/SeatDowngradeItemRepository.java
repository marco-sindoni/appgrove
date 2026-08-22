package app.appgrove.core.billing.seats;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.UUID;

/**
 * Le persone indicate per la cessazione, dentro l'account corrente (UC 0104). Tenant-scoped automatico.
 */
@ApplicationScoped
public class SeatDowngradeItemRepository implements PanacheRepositoryBase<SeatDowngradeItem, UUID> {

    /** Le persone indicate in una riduzione, in ordine di indicazione. */
    public List<SeatDowngradeItem> of(UUID downgradeId) {
        return find("downgradeId = ?1 order by createdAt, id", downgradeId).list();
    }

    /** Le identità indicate in una riduzione. */
    public List<UUID> identitiesOf(UUID downgradeId) {
        return of(downgradeId).stream().map(SeatDowngradeItem::getIdentityId).toList();
    }
}
