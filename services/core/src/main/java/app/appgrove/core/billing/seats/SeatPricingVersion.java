package app.appgrove.core.billing.seats;

import app.appgrove.commons.persistence.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.hibernate.annotations.SQLRestriction;

/**
 * Una versione del listino dei posti (UC 0102): la sua decorrenza, la sua valuta e le sue fasce.
 *
 * <p>Entità di <b>piattaforma</b>: NON estende {@code BaseTenantEntity} e non porta {@code tenant_id},
 * perché il listino è di tutti — come {@code platform.app}. Un listino per account sarebbe una tariffa
 * negoziata, che l'epica 22 esclude.
 *
 * <p><b>Immutabile per contratto.</b> Non ci sono metodi per cambiare decorrenza, valuta o fasce dopo la
 * creazione: cambiare una tariffa significa creare una versione nuova (UC 0105). È la sola forma in cui la
 * domanda «quanto pagava questo cliente in marzo?» ha una risposta.
 *
 * <p>Si costruisce anche <b>in memoria</b>, senza banca dati: è ciò che permette al collaudo tabellare del
 * calcolo di essere un collaudo di unità e non un collaudo di integrazione.
 */
@Entity
@Table(schema = "platform", name = "seat_pricing_version")
@SQLRestriction("deleted_at is null")
public class SeatPricingVersion extends BaseEntity {

    @Column(name = "effective_from", nullable = false, updatable = false)
    private Instant effectiveFrom;

    @Column(nullable = false, updatable = false, length = 3)
    private String currency;

    @Column(length = 500)
    private String note;

    /**
     * Le fasce, <b>ordinate dal primo posto</b>. L'ordine non è un dettaglio di presentazione: la
     * coerenza del listino (contiguità, ultima fascia aperta) e il calcolo a scaglioni lo presuppongono,
     * e affidarlo all'ordine di inserzione avrebbe funzionato in memoria e non dalla banca dati.
     */
    @OneToMany(mappedBy = "version", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("fromSeat ASC")
    private List<SeatPricingBand> bands = new ArrayList<>();

    protected SeatPricingVersion() {
        // richiesto da JPA
    }

    public SeatPricingVersion(Instant effectiveFrom, String currency, String note) {
        this.effectiveFrom = effectiveFrom;
        this.currency = currency;
        this.note = note;
    }

    /**
     * Aggiunge una fascia in coda. Si usa solo in costruzione — dal caricamento iniziale, dalla console di
     * piattaforma (UC 0105) e dai collaudi: una versione già in banca dati non si arricchisce di fasce.
     *
     * @param toSeat ultimo posto della fascia, oppure {@code null} per la fascia aperta (l'ultima)
     */
    public SeatPricingVersion addBand(int fromSeat, Integer toSeat, int unitPriceCents) {
        bands.add(new SeatPricingBand(this, fromSeat, toSeat, unitPriceCents));
        return this;
    }

    public Instant getEffectiveFrom() {
        return effectiveFrom;
    }

    public String getCurrency() {
        return currency;
    }

    public String getNote() {
        return note;
    }

    /** Le fasce in sola lettura, ordinate dal primo posto. */
    public List<SeatPricingBand> getBands() {
        return Collections.unmodifiableList(bands);
    }

    /**
     * Forza il caricamento delle fasce dalla banca dati, così che la versione sia utilizzabile <b>fuori</b>
     * dalla sessione che l'ha letta. La chiama il repository subito dopo la lettura: una versione del
     * listino è un valore che viaggia (verso il calcolo, verso il contratto di rete), e restituirla con la
     * collezione pigra significherebbe consegnare un oggetto che funziona o no a seconda di dove lo si usa.
     */
    void loadBands() {
        bands.size();
    }
}
