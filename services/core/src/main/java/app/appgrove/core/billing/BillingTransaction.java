package app.appgrove.core.billing;

import app.appgrove.commons.persistence.BaseTenantEntity;
import app.appgrove.commons.privacy.PersonalData;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.SQLRestriction;

/**
 * Una transazione di fatturazione del conto — quello che la pagina Billing chiama "pagamento" (UC 0096).
 * È la copia locale di ciò che il fornitore di pagamento (Paddle, venditore di record) ci ha comunicato
 * via webhook: importo, valuta, esito, ricevuta.
 *
 * <p><b>Perché la teniamo noi.</b> Lo storico deve elencare <b>tutte</b> le transazioni del conto, anche
 * quelle fallite, e deve poter essere aperto anche quando il fornitore non risponde: leggerlo su richiesta
 * dalla sua API metterebbe una chiamata di rete verso l'esterno dentro il rendering di una pagina, e per un
 * conto senza un identificativo cliente presso il fornitore non ci sarebbe nemmeno nulla da interrogare
 * (decisione 3, change 0077).
 *
 * <p>Entità tenant-scoped: estende {@link BaseTenantEntity} → filtro {@code WHERE tenant_id} automatico in
 * lettura (invariante #2). La <b>scrittura</b> è del {@link SubscriptionWriter}, che gira fuori da una
 * richiesta autenticata e usa SQL nativo con il tenant esplicito preso dal payload firmato.
 *
 * <p>{@code billingCycle} è mappato come <b>testo</b> e non come enum di proposito: qui il ciclo è
 * un'etichetta da mostrare, e una riga storica con un valore fuori catalogo non deve poter spegnere
 * l'intera pagina (stessa lezione della vetrina, change 0076 decisione 22).
 */
@Entity
@Table(schema = "platform", name = "billing_transaction")
@SQLRestriction("deleted_at is null")
public class BillingTransaction extends BaseTenantEntity {

    @Column(name = "app_id", columnDefinition = "uuid")
    private UUID appId;

    @Column(name = "app_tier_id", columnDefinition = "uuid")
    private UUID appTierId;

    @PersonalData(
            category = "identificativo online (id transazione presso Paddle)",
            purpose = "storico pagamenti e ricevute del conto; riconciliazione con Paddle (MoR, titolare autonomo)",
            retention = "account attivo + grace 14gg; retention fiscale in capo a Paddle")
    @Column(name = "paddle_transaction_id", nullable = false, length = 64)
    private String paddleTransactionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private BillingTransactionStatus status;

    /** Importo in unità minori (centesimi), come tutto il listino. */
    @PersonalData(
            category = "dati di fatturazione (importo e valuta della transazione)",
            purpose = "mostrare al titolare del conto quanto e quando ha pagato (esecuzione del contratto)",
            retention = "account attivo + grace 14gg; retention fiscale in capo a Paddle")
    @Column(nullable = false)
    private int amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "billing_cycle", length = 16)
    private String billingCycle;

    /** Ricevuta presso il fornitore; {@code null} quando non è (ancora) disponibile. */
    @PersonalData(
            category = "collegamento a documento di pagamento ospitato da Paddle (ricevuta del cliente)",
            purpose = "consentire al titolare del conto di aprire la ricevuta del proprio pagamento",
            retention = "account attivo + grace 14gg; il documento è conservato da Paddle (MoR)")
    @Column(name = "receipt_url")
    private String receiptUrl;

    @Column(name = "billed_at", nullable = false)
    private Instant billedAt;

    /**
     * Commissione trattenuta dal fornitore su questa transazione (UC 0071), in unità minori. {@code null}
     * sulle righe per cui non è applicabile (tentativo fallito) e su quelle registrate prima che il dato
     * esistesse: inventarlo a posteriori scriverebbe un numero falso proprio nella vista che serve a dire
     * la verità sui soldi.
     */
    @PersonalData(
            category = "dati di fatturazione (commissione trattenuta sulla transazione)",
            purpose = "riconciliare il ricavo lordo con il denaro davvero accreditato (osservabilità economica)",
            retention = "account attivo + grace 14gg; retention fiscale in capo a Paddle")
    @Column(name = "fee_amount")
    private Integer feeAmount;

    /** Netto residuo dopo la commissione; 0 su storni e tentativi falliti. */
    @PersonalData(
            category = "dati di fatturazione (netto residuo della transazione)",
            purpose = "riconciliare il ricavo lordo con il denaro davvero accreditato (osservabilità economica)",
            retention = "account attivo + grace 14gg; retention fiscale in capo a Paddle")
    @Column(name = "net_amount")
    private Integer netAmount;

    /**
     * Provenienza della commissione: {@code provider} se dichiarata dal fornitore, {@code estimated} se
     * stimata da noi. Testo e non enum per la stessa ragione del ciclo di fatturazione: un valore
     * inatteso non deve poter spegnere una pagina. Non è un dato personale — descrive il dato, non la persona.
     */
    @Column(name = "fee_source", length = 16)
    private String feeSource;

    /** Guardia out-of-order (come su {@link Subscription}): scritta in SQL nativo, qui in sola lettura. */
    @Column(name = "last_event_occurred_at", insertable = false, updatable = false)
    private Instant lastEventOccurredAt;

    protected BillingTransaction() {
        // richiesto da JPA
    }

    public UUID getAppId() {
        return appId;
    }

    public UUID getAppTierId() {
        return appTierId;
    }

    public String getPaddleTransactionId() {
        return paddleTransactionId;
    }

    public BillingTransactionStatus getStatus() {
        return status;
    }

    public int getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getBillingCycle() {
        return billingCycle;
    }

    public String getReceiptUrl() {
        return receiptUrl;
    }

    public Instant getBilledAt() {
        return billedAt;
    }

    public Integer getFeeAmount() {
        return feeAmount;
    }

    public Integer getNetAmount() {
        return netAmount;
    }

    public String getFeeSource() {
        return feeSource;
    }

    public Instant getLastEventOccurredAt() {
        return lastEventOccurredAt;
    }
}
