package app.appgrove.core.platform;

import app.appgrove.commons.persistence.BaseTenantEntity;
import app.appgrove.commons.privacy.PersonalData;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLRestriction;

/**
 * Utente del tenant (membership foldata: 1 utente→1 tenant, nessuna tabella memberships).
 * Tenant-scoped: il filtro {@code WHERE tenant_id = ?} è automatico (discriminator).
 * {@code email}/{@code displayName} sono dati personali (#13 C) — finalità gestione account.
 */
@Entity
@Table(schema = "platform", name = "users")
@SQLRestriction("deleted_at is null")
public class User extends BaseTenantEntity {

    @PersonalData(
            category = "identificativo online (subject Cognito)",
            purpose = "collegamento identità di autenticazione ↔ profilo applicativo",
            retention = "account attivo + grace 14gg (#13 E25)")
    @Column(name = "cognito_sub", nullable = false, updatable = false)
    private String cognitoSub;

    @PersonalData(
            category = "contatto",
            purpose = "erogazione e gestione account (login, comunicazioni di servizio)",
            retention = "account attivo + grace 14gg (#13 E25)")
    @Column(nullable = false)
    private String email;

    @PersonalData(
            category = "identità (nome visualizzato)",
            purpose = "identificazione dell'utente nella UI e nel tenant",
            retention = "account attivo + grace 14gg (#13 E25)")
    @Column(name = "display_name")
    private String displayName;

    /**
     * Lingua dell'utente per le email transazionali (UC 0018): fonte di verità unica, letta dal
     * servizio auth per scegliere il template EN/IT. Mai nulla: chi non l'ha espressa è {@code en}.
     */
    @PersonalData(
            category = "preferenza (lingua)",
            purpose = "lingua delle email transazionali di autenticazione",
            retention = "account attivo + grace 14gg (#13 E25)")
    @Column(nullable = false, length = 8)
    private String locale = "en";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status = UserStatus.active;

    /**
     * Causale della sospensione (UC 0034): {@code gdpr_restriction} = limitazione del trattamento
     * (art. 18, #13 D19), distinta da una sospensione amministrativa. Null se non sospeso.
     */
    @Column(name = "suspended_reason", length = 32)
    private String suspendedReason;

    protected User() {
        // richiesto da JPA
    }

    public User(String cognitoSub, String email, String displayName, UserRole role) {
        this.cognitoSub = cognitoSub;
        this.email = email;
        this.displayName = displayName;
        this.role = role;
    }

    public String getCognitoSub() {
        return cognitoSub;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public String getSuspendedReason() {
        return suspendedReason;
    }

    public void setSuspendedReason(String suspendedReason) {
        this.suspendedReason = suspendedReason;
    }
}
