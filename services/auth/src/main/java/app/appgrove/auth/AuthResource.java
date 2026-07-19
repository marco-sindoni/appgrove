package app.appgrove.auth;

import app.appgrove.auth.AuthDtos.AcceptInviteRequest;
import app.appgrove.auth.AuthDtos.EmailRequest;
import app.appgrove.auth.AuthDtos.EnrollResponse;
import app.appgrove.auth.AuthDtos.InviteSendRequest;
import app.appgrove.auth.AuthDtos.LoginRequest;
import app.appgrove.auth.AuthDtos.LoginTwoFaRequest;
import app.appgrove.auth.AuthDtos.MfaChallenge;
import app.appgrove.auth.AuthDtos.ResetRequest;
import app.appgrove.auth.AuthDtos.SignupRequest;
import app.appgrove.auth.AuthDtos.SignupResponse;
import app.appgrove.auth.AuthDtos.TokenResponse;
import app.appgrove.auth.AuthDtos.TwoFaCodeRequest;
import app.appgrove.auth.AuthDtos.VerifiedResponse;
import app.appgrove.auth.AuthDtos.VerifyRequest;
import app.appgrove.auth.IdentityProvider.Enrollment;
import app.appgrove.auth.IdentityProvider.LoginResult;
import app.appgrove.auth.IdentityProvider.Session;
import app.appgrove.auth.PlatformWriter.InviteRow;
import io.quarkus.security.Authenticated;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import java.time.Instant;
import java.util.Optional;
import org.eclipse.microprofile.jwt.JsonWebToken;

/**
 * Strato HTTP unico del BFF auth (UC 0010/0058/0015): route, validazione, cookie refresh HttpOnly,
 * status code. La logica di identità sta dietro la porta {@link IdentityProvider} (Local in dev,
 * Cognito in test/prod) — il contratto degli endpoint è identico tra gli ambienti (#02).
 * Endpoint pubblici (l'autenticazione è il loro scopo); il 2FA enroll/verify usa il Bearer access token.
 */
@Path("/api/auth")
@Produces(MediaType.APPLICATION_JSON)
public class AuthResource {

    static final String REFRESH_COOKIE = "appgrove_refresh";

    // Instance<>: i provider sono beans condizionali (@LookupIfProperty su auth.provider).
    @Inject
    Instance<IdentityProvider> providers;

    @Inject
    PlatformWriter platform;

    @Inject
    EmailService email;

    @Inject
    JsonWebToken jwt;

    private IdentityProvider provider() {
        return providers.get();
    }

    // ── security-core (UC 0010) ───────────────────────────────────────────────

    @POST
    @Path("/login")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response login(@Valid LoginRequest body) {
        LoginResult result = provider().login(body.email(), body.password());
        if (result instanceof LoginResult.MfaRequired mfa) {
            return Response.ok(new MfaChallenge(true, mfa.challengeToken())).build();
        }
        return tokenResponse(((LoginResult.Ok) result).session());
    }

    @POST
    @Path("/login/2fa")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response loginTwoFa(@Valid LoginTwoFaRequest body) {
        return tokenResponse(provider().loginMfa(body.challenge_token(), body.code()));
    }

    @POST
    @Path("/refresh")
    public Response refresh(@CookieParam(REFRESH_COOKIE) String refreshCookie) {
        if (refreshCookie == null || refreshCookie.isBlank()) {
            throw unauthorized("Refresh token assente.");
        }
        return tokenResponse(provider().refresh(refreshCookie));
    }

    @POST
    @Path("/logout")
    public Response logout(@CookieParam(REFRESH_COOKIE) String refreshCookie) {
        if (refreshCookie != null && !refreshCookie.isBlank()) {
            provider().logout(refreshCookie);
        }
        return Response.noContent().cookie(refreshCookie("", 0)).build();
    }

    @GET
    @Path("/jwks")
    @Produces(MediaType.APPLICATION_JSON)
    public String jwks() {
        // In cloud i servizi validano sul JWKS Cognito: qui 404 (endpoint del solo provider Local).
        return provider().jwks()
                .orElseThrow(() -> status(Response.Status.NOT_FOUND, "JWKS non esposto da questo ambiente."));
    }

    // ── flussi (UC 0058) ──────────────────────────────────────────────────────

    @POST
    @Path("/signup")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response signup(@Valid SignupRequest body) {
        PasswordPolicy.validate(body.password());
        provider().signup(body.email(), body.password(), body.displayName(), body.locale());
        return Response.status(Response.Status.CREATED).entity(new SignupResponse("verification_required")).build();
    }

    @POST
    @Path("/verify")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response verify(@Valid VerifyRequest body) {
        Optional<Session> session =
                provider().verifyEmail(emailActionToken(body.token(), body.email(), body.code()));
        // Local: auto-login post-verifica (UC1 step 4). Cognito: conferma senza credenziali →
        // la SPA rimanda al login (nessun token nella risposta).
        return session.map(this::tokenResponse)
                .orElseGet(() -> Response.ok(new VerifiedResponse("confirmed")).build());
    }

    @POST
    @Path("/verify/resend")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response resendVerification(@Valid EmailRequest body) {
        provider().resendVerification(body.email());
        return Response.accepted().build(); // risposta neutra
    }

    @POST
    @Path("/password/forgot")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response forgotPassword(@Valid EmailRequest body) {
        provider().forgotPassword(body.email());
        return Response.accepted().build(); // neutra (anti-enumeration)
    }

    @POST
    @Path("/password/reset")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response resetPassword(@Valid ResetRequest body) {
        PasswordPolicy.validate(body.password());
        provider().resetPassword(emailActionToken(body.token(), body.email(), body.code()), body.password());
        return Response.noContent().build();
    }

    @POST
    @Path("/invitations/accept")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response acceptInvitation(@Valid AcceptInviteRequest body) {
        PasswordPolicy.validate(body.password());
        InviteRow inv = platform.findInvitationByTokenHash(TokenHashes.sha256Hex(body.token()))
                .orElseThrow(() -> status(Response.Status.BAD_REQUEST, "Invito non valido."));
        if (!inv.isPending()) {
            throw status(Response.Status.GONE, "Invito non più valido.");
        }
        if (inv.isExpired(Instant.now())) {
            throw status(Response.Status.GONE, "Invito scaduto.");
        }
        return tokenResponse(provider().acceptInvitation(inv, body.password(), body.displayName(), body.locale()));
    }

    @POST
    @Path("/invitations/send")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response sendInvitation(@Valid InviteSendRequest body) {
        String role = body.role() != null && !body.role().isBlank() ? body.role() : "member";
        // Lingua di chi invita: l'invitato non è ancora un utente, non ha una preferenza da leggere.
        email.sendInvite(body.email(), body.locale(), body.token(), role);
        return Response.accepted().build();
    }

    @POST
    @Path("/2fa/enroll")
    @Authenticated
    public EnrollResponse enroll2fa(@HeaderParam("Authorization") String authorization) {
        Enrollment enrollment = provider().startTotpEnrollment(bearerToken(authorization), jwt.getSubject());
        return new EnrollResponse(enrollment.secret(), enrollment.otpauthUri());
    }

    @POST
    @Path("/2fa/verify")
    @Authenticated
    @Consumes(MediaType.APPLICATION_JSON)
    public Response verify2fa(@HeaderParam("Authorization") String authorization, @Valid TwoFaCodeRequest body) {
        provider().confirmTotpEnrollment(bearerToken(authorization), jwt.getSubject(), body.code());
        return Response.noContent().build();
    }

    // ── helper ─────────────────────────────────────────────────────────────

    /**
     * Riconduce a un token unico le <b>due forme</b> del collegamento di verifica/reimpostazione
     * (UC 0018): {@code token} da solo, oppure {@code email} + {@code code}.
     *
     * <p>La seconda esiste perché il Custom Message Lambda compone il messaggio <b>prima</b> che il
     * codice esista (Cognito sostituisce il segnaposto dopo): non può quindi produrre un token
     * unico. La ricomposizione la fa il provider, così il formato del token resta un suo dettaglio.
     *
     * <p>Il messaggio d'errore è il medesimo delle altre condizioni di token non valido: non
     * distinguere "forma sbagliata" da "codice sbagliato" evita di dare indizi a chi tenta.
     */
    private String emailActionToken(String token, String emailAddr, String code) {
        if (token != null && !token.isBlank()) {
            return token;
        }
        if (emailAddr == null || emailAddr.isBlank() || code == null || code.isBlank()) {
            throw status(Response.Status.BAD_REQUEST, "Token di verifica non valido o scaduto.");
        }
        return provider().emailActionToken(emailAddr, code);
    }

    private Response tokenResponse(Session session) {
        TokenResponse body = new TokenResponse(
                session.accessToken(), session.idToken(), "Bearer", session.expiresInSeconds());
        return Response.ok(body)
                .cookie(refreshCookie(session.refreshValue(), (int) session.refreshTtlSeconds()))
                .build();
    }

    /** Cookie host-only (#02 17): Secure/HttpOnly/SameSite=Lax/Path=/api/auth, nessun Domain. */
    private NewCookie refreshCookie(String value, int maxAge) {
        return new NewCookie.Builder(REFRESH_COOKIE)
                .value(value).path("/api/auth").httpOnly(true).secure(true)
                .sameSite(NewCookie.SameSite.LAX).maxAge(maxAge).build();
    }

    private static String bearerToken(String authorization) {
        if (authorization == null || !authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
            throw unauthorized("Access token assente.");
        }
        return authorization.substring(7).trim();
    }

    private static WebApplicationException unauthorized(String detail) {
        return new WebApplicationException(detail, Response.Status.UNAUTHORIZED);
    }

    private static WebApplicationException status(Response.Status s, String detail) {
        return new WebApplicationException(detail, s);
    }
}
