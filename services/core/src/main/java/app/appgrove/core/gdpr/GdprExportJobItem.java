package app.appgrove.core.gdpr;

import app.appgrove.commons.persistence.BaseTenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

/**
 * Avanzamento per-servizio di un {@link GdprExportJob}: un item per ogni servizio interpellato
 * ({@code platform} + app attivate). Gli {@code steps} sono le etichette dichiarate dal contratto
 * per il progress (#13 D22). Aggiornato dal consumer risultati via {@link GdprJobStore} (JDBC).
 */
@Entity
@Table(schema = "platform", name = "gdpr_export_job_item")
@SQLRestriction("deleted_at is null")
public class GdprExportJobItem extends BaseTenantEntity {

    @Column(name = "job_id", nullable = false, columnDefinition = "uuid", updatable = false)
    private UUID jobId;

    /** {@code platform} o slug dell'app (id del contratto per-servizio). */
    @Column(name = "app_id", nullable = false, length = 64, updatable = false)
    private String appId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private GdprExportStatus status = GdprExportStatus.QUEUED;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> steps;

    @Column(name = "fragment_key")
    private String fragmentKey;

    @Column
    private String error;

    protected GdprExportJobItem() {
        // richiesto da JPA
    }

    public GdprExportJobItem(UUID jobId, String appId) {
        this.jobId = jobId;
        this.appId = appId;
    }

    public UUID getJobId() {
        return jobId;
    }

    public String getAppId() {
        return appId;
    }

    public GdprExportStatus getStatus() {
        return status;
    }

    public List<String> getSteps() {
        return steps;
    }

    public String getFragmentKey() {
        return fragmentKey;
    }

    public String getError() {
        return error;
    }
}
