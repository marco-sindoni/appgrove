package app.appgrove.core.platform;

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
 * Invito a entrare in un tenant. Tenant-scoped (discriminator). Il token grezzo NON è persistito:
 * si salva solo {@code tokenHash} (single-use). L'accettazione (→ creazione utente) è UC 0017.
 */
@Entity
@Table(schema = "platform", name = "invitations")
@SQLRestriction("deleted_at is null")
public class Invitation extends BaseTenantEntity {

    @PersonalData(
            category = "contatto (email dell'invitato)",
            purpose = "recapito e gestione dell'invito",
            legalBasis = "misure precontrattuali/contratto",
            retention = "fino a scadenza/accettazione dell'invito")
    @Column(nullable = false)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Column(name = "token_hash", nullable = false)
    private String tokenHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvitationStatus status = InvitationStatus.pending;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "invited_by")
    private UUID invitedBy;

    @Column(name = "accepted_user_id")
    private UUID acceptedUserId;

    protected Invitation() {
        // richiesto da JPA
    }

    public Invitation(String email, UserRole role, String tokenHash, Instant expiresAt, UUID invitedBy) {
        this.email = email;
        this.role = role;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.invitedBy = invitedBy;
    }

    public String getEmail() {
        return email;
    }

    public UserRole getRole() {
        return role;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public InvitationStatus getStatus() {
        return status;
    }

    public void setStatus(InvitationStatus status) {
        this.status = status;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public UUID getInvitedBy() {
        return invitedBy;
    }

    public UUID getAcceptedUserId() {
        return acceptedUserId;
    }

    public void setAcceptedUserId(UUID acceptedUserId) {
        this.acceptedUserId = acceptedUserId;
    }
}
