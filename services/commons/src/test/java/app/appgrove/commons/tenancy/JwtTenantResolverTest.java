package app.appgrove.commons.tenancy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;
import java.util.Set;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.junit.jupiter.api.Test;

/** Unit test del TenantResolver: legge solo dal JWT, fail-closed se il claim manca. */
class JwtTenantResolverTest {

    /** Stub minimale di JsonWebToken con claim arbitrari. */
    private static JsonWebToken jwtWith(Map<String, Object> claims) {
        return new JsonWebToken() {
            @Override
            public String getName() {
                return (String) claims.get("upn");
            }

            @Override
            public Set<String> getClaimNames() {
                return claims.keySet();
            }

            @Override
            @SuppressWarnings("unchecked")
            public <T> T getClaim(String claimName) {
                return (T) claims.get(claimName);
            }
        };
    }

    @Test
    void readsTenantIdFromJwt() {
        JwtTenantResolver resolver = new JwtTenantResolver();
        resolver.jwt = jwtWith(Map.of("tenant_id", "acme"));
        assertEquals("acme", resolver.resolveTenantId());
    }

    @Test
    void failClosedWhenTenantMissing() {
        JwtTenantResolver resolver = new JwtTenantResolver();
        resolver.jwt = jwtWith(Map.of());
        assertThrows(TenantNotResolvedException.class, resolver::resolveTenantId);
    }

    @Test
    void defaultTenantIsSentinel() {
        assertEquals(JwtTenantResolver.NO_TENANT, new JwtTenantResolver().getDefaultTenantId());
    }
}
