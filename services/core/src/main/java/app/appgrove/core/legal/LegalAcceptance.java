package app.appgrove.core.legal;

import app.appgrove.commons.persistence.BaseTenantEntity;
import app.appgrove.commons.privacy.PersonalData;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.SQLRestriction;

/**
 * Prova di accettazione/presa d'atto di un documento legale da parte di un utente (UC 0056).
 * Registro <b>append-only</b>, tenant/utente-scoped: il filtro {@code WHERE tenant_id = ?} è automatico
 * (discriminator). Minimizzato — nessun IP/user-agent. Finalità <b>accountability</b> (art. 5.2) e
 * base contrattuale (art. 6.1.b): è la prova di chi ha accettato quale versione e quando.
 */
@Entity
@Table(schema = "platform", name = "legal_acceptance")
@SQLRestriction("deleted_at is null")
public class LegalAcceptance extends BaseTenantEntity {

    @PersonalData(
            category = "identificativo online (subject Cognito)",
            purpose = "prova dell'accettazione/presa d'atto dei documenti legali (accountability, art. 5.2)",
            legalBasis = "contratto (art. 6.1.b) e obbligo di accountability (art. 5.2)",
            retention = "vita dell'account + periodo di prescrizione applicabile (prova del consenso contrattuale, #13 E)")
    @Column(name = "user_id", nullable = false, updatable = false, length = 255)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 32)
    private LegalComponent component;

    @Column(nullable = false, updatable = false, length = 32)
    private String version;

    @Column(nullable = false, updatable = false)
    private int major;

    @Enumerated(EnumType.STRING)
    @Column(name = "act_type", nullable = false, updatable = false, length = 16)
    private LegalActType actType;

    @Column(name = "accepted_at", nullable = false, updatable = false)
    private Instant acceptedAt;

    @Column(name = "commit_hash", updatable = false, length = 64)
    private String commitHash;

    protected LegalAcceptance() {
        // richiesto da JPA
    }

    public LegalAcceptance(
            String userId,
            LegalComponent component,
            String version,
            int major,
            LegalActType actType,
            Instant acceptedAt,
            String commitHash) {
        this.userId = userId;
        this.component = component;
        this.version = version;
        this.major = major;
        this.actType = actType;
        this.acceptedAt = acceptedAt;
        this.commitHash = commitHash;
    }

    public String getUserId() {
        return userId;
    }

    public LegalComponent getComponent() {
        return component;
    }

    public String getVersion() {
        return version;
    }

    public int getMajor() {
        return major;
    }

    public LegalActType getActType() {
        return actType;
    }

    public Instant getAcceptedAt() {
        return acceptedAt;
    }

    public String getCommitHash() {
        return commitHash;
    }
}
