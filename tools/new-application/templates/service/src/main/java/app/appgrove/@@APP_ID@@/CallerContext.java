package app.appgrove.@@APP_ID@@;

import app.appgrove.commons.tenancy.TenantNotResolvedException;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import java.util.UUID;
import org.eclipse.microprofile.jwt.JsonWebToken;

/**
 * Identità del chiamante dal JWT verificato (invariante #1): {@code tenant_id} (= account id) e
 * {@code sub} (= user id). Mai da body/param. Fail-closed se il tenant non è risolvibile.
 */
@RequestScoped
public class CallerContext {

    @Inject
    JsonWebToken jwt;

    /** {@code tenant_id} come UUID (= account id). Fail-closed: assente/malformato → 403. */
    public UUID tenantId() {
        Object claim = jwt.getClaim("tenant_id");
        String raw = claim == null ? null : claim.toString();
        if (raw == null || raw.isBlank()) {
            throw new TenantNotResolvedException();
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException e) {
            throw new TenantNotResolvedException();
        }
    }

    /** {@code sub} = identità utente. */
    public String subject() {
        return jwt.getSubject();
    }
}
