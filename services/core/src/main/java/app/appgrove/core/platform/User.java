package app.appgrove.core.platform;

import app.appgrove.commons.persistence.BaseTenantEntity;
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

    @Column(name = "cognito_sub", nullable = false, updatable = false)
    private String cognitoSub;

    @Column(nullable = false)
    private String email;

    @Column(name = "display_name")
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status = UserStatus.active;

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
}
