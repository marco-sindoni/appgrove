package app.appgrove.core.billing.seats;

import app.appgrove.commons.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLRestriction;

/**
 * Una fascia del listino dei posti (UC 0102): dal posto {@code fromSeat} al posto {@code toSeat} incluso,
 * ogni posto costa {@code unitPriceCents} centesimi al mese.
 *
 * <p>{@code toSeat} nullo significa <b>fascia aperta</b>, e solo l'ultima fascia lo è: un listino la cui
 * ultima fascia è chiusa non saprebbe che prezzo dare al posto successivo, e viene rifiutato.
 *
 * <p>La <b>franchigia</b> è una fascia come le altre — la prima, da 1 a 3, a tariffa zero. Non c'è nulla
 * nel calcolo che la riconosca: è esattamente il punto in cui il codice resta semplice.
 */
@Entity
@Table(schema = "platform", name = "seat_pricing_band")
@SQLRestriction("deleted_at is null")
public class SeatPricingBand extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "version_id", nullable = false, updatable = false)
    private SeatPricingVersion version;

    @Column(name = "from_seat", nullable = false, updatable = false)
    private int fromSeat;

    @Column(name = "to_seat", updatable = false)
    private Integer toSeat;

    @Column(name = "unit_price_cents", nullable = false, updatable = false)
    private int unitPriceCents;

    protected SeatPricingBand() {
        // richiesto da JPA
    }

    SeatPricingBand(SeatPricingVersion version, int fromSeat, Integer toSeat, int unitPriceCents) {
        this.version = version;
        this.fromSeat = fromSeat;
        this.toSeat = toSeat;
        this.unitPriceCents = unitPriceCents;
    }

    public SeatPricingVersion getVersion() {
        return version;
    }

    public int getFromSeat() {
        return fromSeat;
    }

    /** Ultimo posto della fascia, oppure {@code null} se la fascia è aperta verso l'alto. */
    public Integer getToSeat() {
        return toSeat;
    }

    public int getUnitPriceCents() {
        return unitPriceCents;
    }

    /** La fascia è quella aperta verso l'alto (l'ultima del listino). */
    public boolean isOpenEnded() {
        return toSeat == null;
    }

    /** Il posto indicato cade in questa fascia. */
    public boolean contains(int seat) {
        return seat >= fromSeat && (toSeat == null || seat <= toSeat);
    }
}
