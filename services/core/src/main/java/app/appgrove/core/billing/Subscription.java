package app.appgrove.core.billing;

import app.appgrove.commons.persistence.BaseTenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.SQLRestriction;

/**
 * Subscription per {@code (tenant, app)} — unica fonte di verità dello stato di billing (#09 B11/B12).
 * Entità tenant-scoped: estende {@link BaseTenantEntity} → filtro {@code WHERE tenant_id} automatico
 * (invariante #2). DDL in {@code V2__core_domain.sql} (UC 0013).
 *
 * <p>Scope/proprietà: il <b>mapping JPA + repository</b> nasce con UC 0023 (change 0018), primo
 * consumatore che scrive la subscription via la pipeline webhook locale; il consumer applica gli
 * eventi con tenant esplicito dal payload firmato (vedi {@code SubscriptionWriter}). La lettura per
 * l'entitlement derivato è di UC 0027. L'irrobustimento prod (Lambda/dedup/out-of-order/DLQ) è UC 0025.
 */
@Entity
@Table(schema = "platform", name = "subscription")
@SQLRestriction("deleted_at is null")
public class Subscription extends BaseTenantEntity {

    @Column(name = "app_id", nullable = false, columnDefinition = "uuid")
    private UUID appId;

    @Column(name = "app_tier_id", columnDefinition = "uuid")
    private UUID appTierId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private SubscriptionStatus status;

    @Column(name = "current_period_start")
    private Instant currentPeriodStart;

    @Column(name = "current_period_end")
    private Instant currentPeriodEnd;

    @Column(name = "cancel_at")
    private Instant cancelAt;

    @Column(name = "trial_end")
    private Instant trialEnd;

    @Column(name = "paddle_subscription_id")
    private String paddleSubscriptionId;

    /**
     * Cambio tier <b>schedulato</b> a fine periodo (downgrade): tier di destinazione. Null se nessun
     * cambio programmato. Persiste ciò che la derivazione lifecycle non può inferire (a parità di accesso
     * un downgrade schedulato è {@code ACTIVE}); popolato/azzerato dal consumer webhook (UC 0028).
     */
    @Column(name = "scheduled_tier_id", columnDefinition = "uuid")
    private UUID scheduledTierId;

    /** Istante in cui il cambio tier schedulato diventa effettivo (tipicamente {@code current_period_end}). */
    @Column(name = "scheduled_change_at")
    private Instant scheduledChangeAt;

    /**
     * Timestamp dell'ultimo evento applicato (guardia out-of-order, UC 0025). Sola lettura: la scrittura è
     * del {@code SubscriptionWriter} in SQL nativo; qui è mappata per far emettere alle mutazioni
     * self-service (UC 0028) un webhook con {@code occurred_at} monotòno rispetto allo stato corrente.
     */
    @Column(name = "last_event_occurred_at", insertable = false, updatable = false)
    private Instant lastEventOccurredAt;

    protected Subscription() {
        // richiesto da JPA
    }

    public UUID getAppId() {
        return appId;
    }

    public UUID getAppTierId() {
        return appTierId;
    }

    public SubscriptionStatus getStatus() {
        return status;
    }

    public Instant getCurrentPeriodStart() {
        return currentPeriodStart;
    }

    public Instant getCurrentPeriodEnd() {
        return currentPeriodEnd;
    }

    public Instant getCancelAt() {
        return cancelAt;
    }

    public Instant getTrialEnd() {
        return trialEnd;
    }

    public String getPaddleSubscriptionId() {
        return paddleSubscriptionId;
    }

    public UUID getScheduledTierId() {
        return scheduledTierId;
    }

    public Instant getScheduledChangeAt() {
        return scheduledChangeAt;
    }

    public Instant getLastEventOccurredAt() {
        return lastEventOccurredAt;
    }
}
