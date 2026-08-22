package app.appgrove.core.billing.seats;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Optional;
import java.util.UUID;

/**
 * Le riduzioni dei posti dell'account corrente (UC 0104). Tenant-scoped automatico (discriminatore):
 * ogni metodo Panache filtra {@code WHERE tenant_id = ?} senza codice manuale.
 *
 * <p>Questo repository serve <b>solo</b> il percorso dentro una richiesta autenticata (richiesta,
 * annullamento, rimozione di una persona, lettura del riquadro). L'<b>esecuzione</b> alla scadenza gira
 * fuori da una richiesta — lavoro periodico, consumatore degli eventi del fornitore — dove non c'è token e
 * il risolutore del perimetro di Hibernate è chiuso: quella vive in {@link SeatDowngradeExecutor}, a SQL
 * nativo con l'account esplicito.
 */
@ApplicationScoped
public class SeatDowngradeRepository implements PanacheRepositoryBase<SeatDowngrade, UUID> {

    /**
     * La riduzione <b>in attesa</b> dell'account corrente, se c'è. Ce n'è al massimo una: lo garantisce
     * l'indice unico parziale {@code ux_seat_downgrade_pending}, non questo metodo.
     */
    public Optional<SeatDowngrade> pending() {
        return find("status", SeatDowngradeStatus.pending).firstResultOptional();
    }
}
