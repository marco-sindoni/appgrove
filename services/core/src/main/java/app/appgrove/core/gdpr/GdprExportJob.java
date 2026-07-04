package app.appgrove.core.gdpr;

import app.appgrove.commons.persistence.BaseTenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.SQLRestriction;

/**
 * Job di export GDPR (#13 D22): record con stato QUEUED→RUNNING→COMPLETED/FAILED. Tenant-scoped
 * (discriminator → ownership row-level automatica sulle letture REST). Chi ha richiesto =
 * {@code created_by} (sub del JWT, via AuditListener). Le scritture del consumer risultati —
 * fuori da una richiesta autenticata — passano da {@link GdprJobStore} (JDBC, tenant esplicito).
 */
@Entity
@Table(schema = "platform", name = "gdpr_export_job")
@SQLRestriction("deleted_at is null")
public class GdprExportJob extends BaseTenantEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private GdprExportKind kind;

    /** Slug dell'app per {@code kind=app}; null per l'export dell'intero account. */
    @Column(name = "app_id", length = 64)
    private String appId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private GdprExportStatus status = GdprExportStatus.QUEUED;

    /** Chiave S3 dello ZIP aggregato; valorizzata a COMPLETED. */
    @Column(name = "zip_key")
    private String zipKey;

    @Column
    private String error;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected GdprExportJob() {
        // richiesto da JPA
    }

    public GdprExportJob(GdprExportKind kind, String appId) {
        this.kind = kind;
        this.appId = appId;
    }

    public GdprExportKind getKind() {
        return kind;
    }

    public String getAppId() {
        return appId;
    }

    public GdprExportStatus getStatus() {
        return status;
    }

    public void setStatus(GdprExportStatus status) {
        this.status = status;
    }

    public String getZipKey() {
        return zipKey;
    }

    public String getError() {
        return error;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }
}
