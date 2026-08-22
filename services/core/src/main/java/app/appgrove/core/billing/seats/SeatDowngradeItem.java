package app.appgrove.core.billing.seats;

import app.appgrove.commons.persistence.BaseTenantEntity;
import app.appgrove.commons.privacy.PersonalData;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.SQLRestriction;

/**
 * Una <b>persona indicata</b> per la cessazione dentro una {@link SeatDowngrade riduzione} (UC 0104).
 *
 * <p>La riga dice «questa persona <b>uscirà</b>», non «questa persona è uscita»: fino alla data di
 * esecuzione la persona resta attiva, con lo stesso accesso e gli stessi ruoli, e continua a occupare —
 * e a costare — il suo posto. È la ragione per cui {@code SeatCount} non è stato toccato da questa
 * storia: il conteggio dei posti è scritto sull'<i>esistenza</i> dell'appartenenza, non sull'elenco dei
 * suoi stati, quindi lo stato «in cessazione» non fa sparire posti dal conto.
 *
 * <p>Il riferimento è all'<b>identità</b> della persona e non alla sua appartenenza, come
 * {@code app_access.identity_id}: l'appartenenza a un account può chiudersi e riaprirsi, l'identità no.
 */
@Entity
@Table(schema = "platform", name = "seat_downgrade_item")
@SQLRestriction("deleted_at is null")
public class SeatDowngradeItem extends BaseTenantEntity {

    @Column(name = "downgrade_id", nullable = false, updatable = false, columnDefinition = "uuid")
    private UUID downgradeId;

    /**
     * Dato personale, come {@code membership.identity_id} e per la stessa ragione: non contiene né nome
     * né indirizzo, ma <b>dice qualcosa di una persona</b> — e qui dice una cosa delicata, che il suo
     * rapporto con quell'account è stato messo in scadenza.
     */
    @PersonalData(
            category = "identificativo online (riferimento all'identità della persona indicata per la cessazione)",
            purpose = "sapere quali persone l'account ha indicato per la cessazione alla fine del periodo pagato, "
                    + "così da eseguire la riduzione e adeguare i posti a pagamento",
            retention = "finché la riduzione è conservata; eliminata con l'account (#13 E25)")
    @Column(name = "identity_id", nullable = false, updatable = false, columnDefinition = "uuid")
    private UUID identityId;

    protected SeatDowngradeItem() {
        // richiesto da JPA
    }

    public SeatDowngradeItem(UUID downgradeId, UUID identityId) {
        this.downgradeId = downgradeId;
        this.identityId = identityId;
    }

    public UUID getDowngradeId() {
        return downgradeId;
    }

    public UUID getIdentityId() {
        return identityId;
    }
}
