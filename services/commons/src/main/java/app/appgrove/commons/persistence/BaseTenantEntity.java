package app.appgrove.commons.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import org.hibernate.annotations.TenantId;

/**
 * Base delle entity tenant-scoped. Il campo {@code tenantId} è il discriminatore di multitenancy:
 * Hibernate lo valorizza dal {@code TenantResolver} (JWT) in scrittura e aggiunge il filtro
 * {@code WHERE tenant_id = ?} a <b>ogni</b> query in lettura — invariante #2 reso automatico.
 */
@MappedSuperclass
public abstract class BaseTenantEntity extends BaseEntity {

    @TenantId
    @Column(name = "tenant_id", nullable = false, updatable = false, length = 64)
    private String tenantId;

    public String getTenantId() {
        return tenantId;
    }
}
