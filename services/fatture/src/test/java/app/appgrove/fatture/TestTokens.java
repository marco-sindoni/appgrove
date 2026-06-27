package app.appgrove.fatture;

import io.smallrye.jwt.build.Jwt;
import java.util.Set;

/** Harness identità: firma JWT reali (smallrye-jwt) con i claim tenant_id/roles. Niente Cognito. */
public final class TestTokens {

    public static final String ISSUER = "https://local.appgrove.app";

    private TestTokens() {}

    public static String withTenant(String tenantId, String... roles) {
        return Jwt.issuer(ISSUER)
                .upn("user-" + tenantId)
                .subject("sub-" + tenantId)
                .claim("tenant_id", tenantId)
                .groups(Set.of(roles))
                .sign();
    }

    /** Token autenticato CON ruoli ma SENZA {@code tenant_id}: esercita il fail-closed del resolver. */
    public static String withRolesNoTenant(String... roles) {
        return Jwt.issuer(ISSUER)
                .upn("user-anon")
                .subject("sub-anon")
                .groups(Set.of(roles))
                .sign();
    }
}
