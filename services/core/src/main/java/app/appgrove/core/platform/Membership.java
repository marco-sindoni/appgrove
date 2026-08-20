package app.appgrove.core.platform;

import app.appgrove.commons.persistence.BaseTenantEntity;
import app.appgrove.commons.privacy.PersonalData;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.SQLRestriction;

/**
 * Appartenenza di una persona a un account (UC 0116): la coppia (account, identità) con il suo ruolo
 * e il suo stato. Entità di <b>account</b>, tenant-scoped: il filtro {@code WHERE tenant_id = ?} è
 * automatico (discriminator, invariante #2).
 *
 * <p>Una persona può averne più di una; per ogni account ne ha <b>al massimo una viva</b> — vincolo
 * esplicito {@code ux_membership_tenant_identity} sulle righe vive (V17). Uscire da un account
 * significa cancellare l'appartenenza, <b>non</b> l'identità: le altre appartenenze non ne sanno nulla.
 *
 * <p>I dati personali diretti (indirizzo, nome, lingua) stanno tutti sull'{@link Identity}. Qui c'è
 * il <b>legame</b> fra una persona e un'azienda, che è a sua volta un dato personale — e il titolare
 * che ne risponde è l'account, non la piattaforma. È esattamente il cambio di titolarità che questa
 * storia introduce.
 */
@Entity
@Table(schema = "platform", name = "membership")
@SQLRestriction("deleted_at is null")
public class Membership extends BaseTenantEntity {

    @PersonalData(
            category = "identificativo online (riferimento all'identità della persona)",
            purpose = "legame fra la persona e questo account (appartenenza, ruolo, accesso ai dati dell'account)",
            retention = "finché l'appartenenza è viva; eliminata con l'account (#13 E25)")
    @Column(name = "identity_id", nullable = false, updatable = false)
    private UUID identityId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MembershipRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MembershipStatus status = MembershipStatus.active;

    protected Membership() {
        // richiesto da JPA
    }

    public Membership(UUID identityId, MembershipRole role) {
        this.identityId = identityId;
        this.role = role;
    }

    public UUID getIdentityId() {
        return identityId;
    }

    public MembershipRole getRole() {
        return role;
    }

    public void setRole(MembershipRole role) {
        this.role = role;
    }

    public MembershipStatus getStatus() {
        return status;
    }

    public void setStatus(MembershipStatus status) {
        this.status = status;
    }
}
