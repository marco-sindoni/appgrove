package app.appgrove.core.billing.seats;

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
 * La <b>riduzione dei posti come atto</b> (UC 0104): l'owner indica le persone da cessare e l'account
 * entra in «riduzione in attesa» fino alla fine del periodo già pagato. Entità di <b>account</b>,
 * tenant-scoped: il filtro {@code WHERE tenant_id = ?} è automatico (discriminatore, invariante #2).
 *
 * <p><b>Un atto, non N contrassegni.</b> La data di esecuzione è <b>una sola</b> e sta qui, non ripetuta
 * sulle righe delle persone: tre persone indicate con tre date diverse sarebbero tre riduzioni, e
 * l'account non ne ha chiesta nessuna delle tre. Per la stessa ragione l'annullamento è una riga che
 * cambia stato e non un'operazione che può riuscire a metà.
 *
 * <p><b>La quantità dell'abbonamento non è qui, e non cambia adesso.</b> La riduzione è <i>programmata</i>:
 * il posto è stato pagato per tutto il mese (permanenza minima mensile, epica E22.2), quindi
 * {@code platform.subscription.quantity} scende solo all'esecuzione. È la ragione per cui annullare non ha
 * alcun effetto contabile: non c'è nulla da rimborsare perché nulla era stato cambiato.
 */
@Entity
@Table(schema = "platform", name = "seat_downgrade")
@SQLRestriction("deleted_at is null")
public class SeatDowngrade extends BaseTenantEntity {

    @Column(name = "execute_at", nullable = false)
    private Instant executeAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private SeatDowngradeStatus status = SeatDowngradeStatus.pending;

    /**
     * Chi ha chiesto la riduzione. Dato personale come {@code app_access.granted_by} e per la stessa
     * ragione: non contiene né nome né indirizzo, ma dice che <b>quella persona</b> ha deciso la
     * cessazione di un collega. Serve alla traccia di controllo, non all'autorizzazione.
     */
    @PersonalData(
            category = "identificativo online (riferimento all'identità di chi ha chiesto la riduzione dei posti)",
            purpose = "sapere chi, dentro l'account, ha deciso la cessazione di una o più persone (traccia di controllo)",
            retention = "finché la riduzione è conservata; eliminata con l'account (#13 E25)")
    @Column(name = "requested_by", updatable = false, columnDefinition = "uuid")
    private UUID requestedBy;

    @Column(name = "executed_at")
    private Instant executedAt;

    protected SeatDowngrade() {
        // richiesto da JPA
    }

    public SeatDowngrade(Instant executeAt, UUID requestedBy) {
        this.executeAt = executeAt;
        this.requestedBy = requestedBy;
    }

    public Instant getExecuteAt() {
        return executeAt;
    }

    public SeatDowngradeStatus getStatus() {
        return status;
    }

    public void setStatus(SeatDowngradeStatus status) {
        this.status = status;
    }

    public UUID getRequestedBy() {
        return requestedBy;
    }

    public Instant getExecutedAt() {
        return executedAt;
    }

    public void setExecutedAt(Instant executedAt) {
        this.executedAt = executedAt;
    }

    /**
     * L'attesa è <b>scaduta e non ancora eseguita</b>: la condizione che va misurata e allarmata
     * (UC 0104 §5). Non è uno stato a sé — resta {@code pending} — perché non è un fatto normale: è il
     * guasto in cui un cliente paga posti che credeva chiusi.
     */
    public boolean isOverdue(Instant now) {
        return status == SeatDowngradeStatus.pending && !executeAt.isAfter(now);
    }
}
