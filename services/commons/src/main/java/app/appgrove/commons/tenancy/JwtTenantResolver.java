package app.appgrove.commons.tenancy;

import io.quarkus.hibernate.orm.PersistenceUnitExtension;
import io.quarkus.hibernate.orm.runtime.tenant.TenantResolver;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.jwt.JsonWebToken;

/**
 * Risolve il {@code tenant_id} per Hibernate multitenancy (DISCRIMINATOR) leggendolo
 * <b>solo dal JWT verificato</b> (claim {@code tenant_id}) — invariante #1. Mai da body/param.
 * Fail-closed: se il claim manca solleva {@link TenantNotResolvedException} (niente default tenant).
 */
@PersistenceUnitExtension
@ApplicationScoped
public class JwtTenantResolver implements TenantResolver {

    /** Sentinella usata fuori da una richiesta autenticata (es. boot); nessun dato tenant vi è associato. */
    public static final String NO_TENANT = "__no_tenant__";

    @Inject
    JsonWebToken jwt;

    @Override
    public String getDefaultTenantId() {
        return NO_TENANT;
    }

    @Override
    public String resolveTenantId() {
        String tenantId = null;
        try {
            Object claim = jwt.getClaim("tenant_id");
            tenantId = claim == null ? null : claim.toString();
        } catch (RuntimeException ignored) {
            // nessun contesto/token attivo → trattato come assente (fail-closed sotto)
        }
        if (tenantId == null || tenantId.isBlank()) {
            throw new TenantNotResolvedException();
        }
        return tenantId;
    }
}
