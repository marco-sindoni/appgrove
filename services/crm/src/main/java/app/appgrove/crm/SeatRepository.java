package app.appgrove.crm;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository dei posti. Tenant-scoped automatico: {@code count()} e le ricerche vedono solo i posti
 * <b>attivi</b> del tenant corrente (il {@code @SQLRestriction} esclude quelli revocati), ed è
 * esattamente la giacenza che la quota {@code seats} deve contare.
 */
@ApplicationScoped
public class SeatRepository implements PanacheRepositoryBase<Seat, UUID> {

    /** Posto attivo di un utente nel tenant corrente, se esiste. */
    public Optional<Seat> activeForUser(String userId) {
        return find("userId", userId).firstResultOptional();
    }

    /** Vero se l'utente ha un posto attivo nel tenant corrente. */
    public boolean hasSeat(String userId) {
        return count("userId", userId) > 0;
    }
}
