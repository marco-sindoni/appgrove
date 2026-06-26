package app.appgrove.authlocal;

import app.appgrove.authlocal.AuthDtos.LoginRequest;
import app.appgrove.authlocal.AuthDtos.TokenResponse;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Set;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Provider auth locale (UC 0010, security-core). Emette JWT con claim dal DB, gestisce refresh cookie
 * ed espone il JWKS. Fail-closed: credenziali errate / refresh non valido → 401. Tutti gli endpoint
 * sono pubblici (l'autenticazione è il loro scopo).
 */
@Path("/api/auth")
@Produces(MediaType.APPLICATION_JSON)
public class AuthResource {

    /** Cookie del refresh token: HttpOnly, host-only, Path=/api/auth (#02 dec.17). */
    static final String REFRESH_COOKIE = "appgrove_refresh";

    @ConfigProperty(name = "auth.local.dev-password")
    String devPassword;

    @Inject
    UserDirectory directory;

    @Inject
    TokenService tokens;

    @POST
    @Path("/login")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response login(@Valid LoginRequest body) {
        AuthUser user = directory.findByEmail(body.email())
                .filter(AuthUser::isActive)
                .orElse(null);
        // confronto password a tempo costante; messaggio generico (anti-enumeration)
        boolean ok = user != null && constantTimeEquals(devPassword, body.password());
        if (!ok) {
            throw unauthorized("Credenziali non valide.");
        }
        return tokenResponse(user);
    }

    @POST
    @Path("/refresh")
    public Response refresh(@CookieParam(REFRESH_COOKIE) String refreshCookie) {
        if (refreshCookie == null || refreshCookie.isBlank()) {
            throw unauthorized("Refresh token assente.");
        }
        String sub;
        try {
            sub = tokens.verifyRefreshSubject(refreshCookie);
        } catch (RuntimeException e) {
            throw unauthorized("Refresh token non valido.");
        }
        AuthUser user = directory.findBySub(sub)
                .filter(AuthUser::isActive)
                .orElseThrow(() -> unauthorized("Utente non più valido."));
        return tokenResponse(user);
    }

    @POST
    @Path("/logout")
    public Response logout() {
        return Response.noContent().cookie(refreshCookie("", 0)).build();
    }

    @GET
    @Path("/jwks")
    @Produces(MediaType.APPLICATION_JSON)
    public String jwks() {
        return tokens.jwks();
    }

    // ── helper ─────────────────────────────────────────────────────────────
    private Response tokenResponse(AuthUser user) {
        Set<String> groups = tokens.groupsFor(user);
        TokenResponse body = new TokenResponse(
                tokens.accessToken(user, groups),
                tokens.idToken(user),
                "Bearer",
                tokens.accessTtlSeconds());
        NewCookie cookie = refreshCookie(tokens.refreshToken(user), (int) tokens.refreshTtlSeconds());
        return Response.ok(body).cookie(cookie).build();
    }

    private NewCookie refreshCookie(String value, int maxAge) {
        return new NewCookie.Builder(REFRESH_COOKIE)
                .value(value)
                .path("/api/auth")
                .httpOnly(true)
                .secure(true)
                .sameSite(NewCookie.SameSite.LAX)
                .maxAge(maxAge)
                .build();
    }

    private static WebApplicationException unauthorized(String detail) {
        return new WebApplicationException(detail, Response.Status.UNAUTHORIZED);
    }

    private static boolean constantTimeEquals(String a, String b) {
        return MessageDigest.isEqual(
                a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}
