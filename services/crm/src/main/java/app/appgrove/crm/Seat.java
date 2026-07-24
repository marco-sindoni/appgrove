package app.appgrove.crm;

import app.appgrove.commons.persistence.BaseTenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLRestriction;

/**
 * Un <b>posto</b> occupato nell'app Mini-CRM (UC 0054): l'abilitazione di un utente dell'account a
 * usare l'app. {@code userId} è il claim {@code sub} del membro (identità dell'utente, non dato di
 * terzi). Tenant-scoped come tutto il resto.
 *
 * <p>È la <b>giacenza</b> contata dalla quota {@code seats} (natura stock): {@link SeatRepository}
 * conta le righe attive per il tenant, il tetto arriva dall'entitlement. Revocare un posto è una
 * cancellazione logica ({@link #markDeleted()}) e libera immediatamente la giacenza — nessuna finestra
 * temporale da attendere. Non porta dati personali di terzi: solo l'identità di un membro del tenant.
 */
@Entity
@Table(schema = "app_crm", name = "seat")
@SQLRestriction("deleted_at is null")
public class Seat extends BaseTenantEntity {

    @Column(name = "user_id", nullable = false, updatable = false)
    private String userId;

    protected Seat() {
        // richiesto da JPA
    }

    public Seat(String userId) {
        this.userId = userId;
    }

    public String getUserId() {
        return userId;
    }
}
