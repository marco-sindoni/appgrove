package app.appgrove.core.catalog;

import app.appgrove.commons.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

/**
 * Tier (piano) di un'app: chiave interna stabile {@code key} (unica per app), {@code limits}/{@code features}
 * come JSON (vivono nel <b>nostro</b> DB, non in Paddle — #09 B8). {@code appId} è un riferimento per UUID
 * (coerente con {@code Subscription}), non una relazione JPA. Read-model; scritture dal {@link PricingSyncService}.
 */
@Entity
@Table(schema = "platform", name = "app_tier")
@SQLRestriction("deleted_at is null")
public class AppTier extends BaseEntity {

    @Column(name = "app_id", columnDefinition = "uuid", nullable = false)
    private UUID appId;

    // proprietà 'tierKey' (colonna "key"): 'key' è parola riservata JPQL → evitarla come nome di proprietà.
    @Column(name = "key", nullable = false, length = 64)
    private String tierKey;

    @Column(nullable = false)
    private String name;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> limits;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> features;

    @Column(name = "trial_days", nullable = false)
    private int trialDays;

    protected AppTier() {
        // richiesto da JPA
    }

    public UUID getAppId() {
        return appId;
    }

    public String getKey() {
        return tierKey;
    }

    public String getName() {
        return name;
    }

    public Map<String, Object> getLimits() {
        return limits;
    }

    public Map<String, Object> getFeatures() {
        return features;
    }

    public int getTrialDays() {
        return trialDays;
    }
}
