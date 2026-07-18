package app.appgrove.fatture;

import io.smallrye.jwt.build.Jwt;
import io.smallrye.jwt.build.JwtClaimsBuilder;
import java.util.List;

/**
 * Harness identità: firma JWT reali (smallrye-jwt) a specchio dell'access token di produzione
 * (UC 0016) — ruoli nel claim {@code roles}, {@code token_use=access}, {@code client_id}. Niente
 * Cognito. Espone anche varianti "cattive" per i test di sicurezza.
 */
public final class TestTokens {

    public static final String ISSUER = "https://local.appgrove.app";
    public static final String CLIENT_ID = "appgrove-local-bff";

    private TestTokens() {}

    private static JwtClaimsBuilder access() {
        return Jwt.issuer(ISSUER).claim("token_use", "access").claim("client_id", CLIENT_ID);
    }

    public static String withTenant(String tenantId, String... roles) {
        return access()
                .upn("user-" + tenantId)
                .subject("sub-" + tenantId)
                .claim("tenant_id", tenantId)
                .claim("roles", List.of(roles))
                .sign();
    }

    /** Token autenticato CON ruoli ma SENZA {@code tenant_id}: esercita il fail-closed del resolver. */
    public static String withRolesNoTenant(String... roles) {
        return access().upn("user-anon").subject("sub-anon").claim("roles", List.of(roles)).sign();
    }

    /** Id token (token_use=id): firma valida ma NON access token → dev'essere rifiutato. */
    public static String idToken(String tenantId, String... roles) {
        return Jwt.issuer(ISSUER)
                .upn("user-" + tenantId)
                .subject("sub-" + tenantId)
                .claim("tenant_id", tenantId)
                .claim("roles", List.of(roles))
                .claim("token_use", "id")
                .claim("client_id", CLIENT_ID)
                .sign();
    }

    /** Access token con client_id diverso da quello atteso → dev'essere rifiutato. */
    public static String withWrongClientId(String tenantId, String... roles) {
        return Jwt.issuer(ISSUER)
                .upn("user-" + tenantId)
                .subject("sub-" + tenantId)
                .claim("tenant_id", tenantId)
                .claim("roles", List.of(roles))
                .claim("token_use", "access")
                .claim("client_id", "un-altro-client")
                .sign();
    }
}
