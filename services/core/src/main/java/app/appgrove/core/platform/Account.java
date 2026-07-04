package app.appgrove.core.platform;

import app.appgrove.commons.persistence.BaseEntity;
import app.appgrove.commons.privacy.PersonalData;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;
import org.hibernate.annotations.SQLRestriction;

/**
 * Account = tenant. {@code id} è il {@code tenant_id} iniettato nel JWT (invariante #1).
 * NON estende {@link app.appgrove.commons.persistence.BaseTenantEntity}: è la radice del tenant,
 * non una riga tenant-scoped; l'accesso è filtrato per {@code id = tenant_id} (equivalente #2).
 */
@Entity
@Table(schema = "platform", name = "accounts")
@SQLRestriction("deleted_at is null")
public class Account extends BaseEntity {

    /** Grace di cancellazione account (#13 E25): disattivazione subito, hard-purge dopo 14 giorni. */
    public static final Duration DELETION_GRACE = Duration.ofDays(14);

    @PersonalData(
            category = "identità/anagrafica account (nei B2C è il nome della persona)",
            purpose = "identificazione dell'account/tenant nella piattaforma",
            retention = "account attivo + grace 14gg (#13 E25)")
    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountStatus status = AccountStatus.active;

    @PersonalData(
            category = "identificativo online (customer id Paddle)",
            purpose = "riconciliazione abbonamenti/pagamenti con Paddle (MoR, titolare autonomo)",
            retention = "account attivo + grace 14gg; retention fiscale in capo a Paddle")
    @Column(name = "paddle_customer_id")
    private String paddleCustomerId;

    /** Istante della richiesta di eliminazione (UC 0033); null se nessuna eliminazione in corso. */
    @Column(name = "deletion_requested_at")
    private Instant deletionRequestedAt;

    /**
     * Causale della sospensione (UC 0034): {@code gdpr_restriction} = limitazione del trattamento
     * (art. 18, #13 D19), distinta da una sospensione amministrativa. Null se non sospeso.
     */
    @Column(name = "suspended_reason", length = 32)
    private String suspendedReason;

    protected Account() {
        // richiesto da JPA
    }

    public Account(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public void setStatus(AccountStatus status) {
        this.status = status;
    }

    public String getPaddleCustomerId() {
        return paddleCustomerId;
    }

    public void setPaddleCustomerId(String paddleCustomerId) {
        this.paddleCustomerId = paddleCustomerId;
    }

    public Instant getDeletionRequestedAt() {
        return deletionRequestedAt;
    }

    public String getSuspendedReason() {
        return suspendedReason;
    }

    public void setSuspendedReason(String suspendedReason) {
        this.suspendedReason = suspendedReason;
    }

    public void setDeletionRequestedAt(Instant deletionRequestedAt) {
        this.deletionRequestedAt = deletionRequestedAt;
    }

    /** Scadenza della grace (richiesta + 14gg), o null se nessuna eliminazione in corso. */
    public Instant deletionEffectiveAt() {
        return deletionRequestedAt == null ? null : deletionRequestedAt.plus(DELETION_GRACE);
    }
}
