package app.appgrove.authlocal;

import app.appgrove.authlocal.AuthDtos.AcceptInviteRequest;
import app.appgrove.authlocal.AuthDtos.EmailRequest;
import app.appgrove.authlocal.AuthDtos.EnrollResponse;
import app.appgrove.authlocal.AuthDtos.InviteSendRequest;
import app.appgrove.authlocal.AuthDtos.LoginRequest;
import app.appgrove.authlocal.AuthDtos.LoginTwoFaRequest;
import app.appgrove.authlocal.AuthDtos.MfaChallenge;
import app.appgrove.authlocal.AuthDtos.ResetRequest;
import app.appgrove.authlocal.AuthDtos.SignupRequest;
import app.appgrove.authlocal.AuthDtos.SignupResponse;
import app.appgrove.authlocal.AuthDtos.TokenResponse;
import app.appgrove.authlocal.AuthDtos.TwoFaCodeRequest;
import app.appgrove.authlocal.AuthDtos.VerifyRequest;
import app.appgrove.authlocal.CredentialsRepository.Cred;
import app.appgrove.authlocal.PlatformWriter.CreatedUser;
import app.appgrove.authlocal.PlatformWriter.InviteRow;
import io.quarkus.security.Authenticated;
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
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;

/**
 * Provider auth locale (UC 0010 security-core + UC 0058 flussi completi). Login/refresh/logout/JWKS +
 * signup/verifica, reset password, accept invito, 2FA TOTP. Fail-closed; email funzionali su Mailpit.
 * Endpoint pubblici (l'autenticazione è il loro scopo); il 2FA enroll/verify usa il Bearer access token.
 */
@Path("/api/auth")
@Produces(MediaType.APPLICATION_JSON)
public class AuthResource {

    static final String REFRESH_COOKIE = "appgrove_refresh";

    @ConfigProperty(name = "auth.local.dev-password")
    String devPassword;

    @ConfigProperty(name = "auth.local.totp-bypass")
    boolean totpBypass;

    @Inject
    UserDirectory directory;

    @Inject
    CredentialsRepository credentials;

    @Inject
    PlatformWriter platform;

    @Inject
    TokenService tokens;

    @Inject
    TotpService totp;

    @Inject
    EmailService email;

    @Inject
    JsonWebToken jwt;

    // ── security-core (UC 0010) ───────────────────────────────────────────────

    @POST
    @Path("/login")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response login(@Valid LoginRequest body) {
        AuthUser user = directory.findByEmail(body.email()).filter(AuthUser::isActive)
                .orElseThrow(() -> unauthorized("Credenziali non valide."));
        Optional<Cred> credOpt = credentials.find(user.sub());
        if (credOpt.isPresent()) {
            Cred cred = credOpt.get();
            if (!Passwords.verify(body.password(), cred.passwordHash())) {
                throw unauthorized("Credenziali non valide.");
            }
            if (!cred.emailVerified()) {
                throw status(Response.Status.FORBIDDEN, "Email non verificata.");
            }
            if (cred.totpEnabled() && !totpBypass) {
                return Response.ok(new MfaChallenge(true, tokens.mfaChallengeToken(user.sub()))).build();
            }
        } else if (!constantTimeEquals(devPassword, body.password())) {
            // utente del seed (nessuna riga credenziali) → fallback password dev universale
            throw unauthorized("Credenziali non valide.");
        }
        return tokenResponse(user);
    }

    @POST
    @Path("/login/2fa")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response loginTwoFa(@Valid LoginTwoFaRequest body) {
        String sub = tokens.verifyTokenSubject(body.challenge_token(), "mfa_challenge");
        Cred cred = credentials.find(sub).orElseThrow(() -> unauthorized("Challenge non valido."));
        if (!cred.totpEnabled() || !totp.verify(cred.totpSecret(), body.code())) {
            throw unauthorized("Codice 2FA non valido.");
        }
        AuthUser user = directory.findBySub(sub).filter(AuthUser::isActive)
                .orElseThrow(() -> unauthorized("Utente non valido."));
        return tokenResponse(user);
    }

    @POST
    @Path("/refresh")
    public Response refresh(@CookieParam(REFRESH_COOKIE) String refreshCookie) {
        if (refreshCookie == null || refreshCookie.isBlank()) {
            throw unauthorized("Refresh token assente.");
        }
        String sub = tokens.verifyRefreshSubject(refreshCookie);
        AuthUser user = directory.findBySub(sub).filter(AuthUser::isActive)
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

    // ── flussi (UC 0058) ──────────────────────────────────────────────────────

    @POST
    @Path("/signup")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response signup(@Valid SignupRequest body) {
        PasswordPolicy.validate(body.password());
        if (directory.findByEmail(body.email()).isPresent()) {
            throw status(Response.Status.CONFLICT, "Email già registrata.");
        }
        CreatedUser created = platform.createAccountWithOwner(body.email(), body.displayName());
        credentials.create(created.user().sub(), Passwords.hash(body.password()), false);
        email.sendVerify(body.email(), tokens.emailVerifyToken(created.user().sub()));
        return Response.status(Response.Status.CREATED).entity(new SignupResponse("verification_required")).build();
    }

    @POST
    @Path("/verify")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response verify(@Valid VerifyRequest body) {
        String sub = verifyOrBadRequest(body.token(), "email_verify", "Token di verifica non valido o scaduto.");
        credentials.setEmailVerified(sub);
        AuthUser user = directory.findBySub(sub).filter(AuthUser::isActive)
                .orElseThrow(() -> unauthorized("Utente non valido."));
        return tokenResponse(user); // auto-login post-verifica (UC1 step 4)
    }

    @POST
    @Path("/verify/resend")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response resendVerification(@Valid EmailRequest body) {
        directory.findByEmail(body.email()).ifPresent(u ->
                credentials.find(u.sub()).filter(c -> !c.emailVerified())
                        .ifPresent(c -> email.sendVerify(u.email(), tokens.emailVerifyToken(u.sub()))));
        return Response.accepted().build(); // risposta neutra
    }

    @POST
    @Path("/password/forgot")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response forgotPassword(@Valid EmailRequest body) {
        directory.findByEmail(body.email())
                .ifPresent(u -> email.sendReset(u.email(), tokens.passwordResetToken(u.sub())));
        return Response.accepted().build(); // neutra (anti-enumeration)
    }

    @POST
    @Path("/password/reset")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response resetPassword(@Valid ResetRequest body) {
        PasswordPolicy.validate(body.password());
        String sub = verifyOrBadRequest(body.token(), "pwd_reset", "Token di reset non valido o scaduto.");
        credentials.create(sub, Passwords.hash(body.password()), true); // upsert: reset implica email verificata
        return Response.noContent().build();
    }

    @POST
    @Path("/invitations/accept")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response acceptInvitation(@Valid AcceptInviteRequest body) {
        PasswordPolicy.validate(body.password());
        InviteRow inv = platform.findInvitationByTokenHash(sha256Hex(body.token()))
                .orElseThrow(() -> status(Response.Status.BAD_REQUEST, "Invito non valido."));
        if (!inv.isPending()) {
            throw status(Response.Status.GONE, "Invito non più valido.");
        }
        if (inv.isExpired(Instant.now())) {
            throw status(Response.Status.GONE, "Invito scaduto.");
        }
        CreatedUser created = platform.createUserInTenant(inv.tenantId(), inv.email(), body.displayName(), inv.role());
        credentials.create(created.user().sub(), Passwords.hash(body.password()), true); // link prova l'email
        platform.markInvitationAccepted(inv.id(), created.id());
        return tokenResponse(created.user()); // auto-login come membro
    }

    @POST
    @Path("/invitations/send")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response sendInvitation(@Valid InviteSendRequest body) {
        String role = body.role() != null && !body.role().isBlank() ? body.role() : "member";
        email.sendInvite(body.email(), body.token(), role);
        return Response.accepted().build();
    }

    @POST
    @Path("/2fa/enroll")
    @Authenticated
    public EnrollResponse enroll2fa() {
        String sub = jwt.getSubject();
        AuthUser user = directory.findBySub(sub).orElseThrow(() -> unauthorized("Utente non valido."));
        credentials.find(sub).orElseThrow(() ->
                status(Response.Status.CONFLICT, "2FA non disponibile per account senza credenziali (utente seed)."));
        String secret = totp.newSecret();
        credentials.setTotp(sub, secret, false);
        return new EnrollResponse(secret, totp.otpauthUri(secret, user.email()));
    }

    @POST
    @Path("/2fa/verify")
    @Authenticated
    @Consumes(MediaType.APPLICATION_JSON)
    public Response verify2fa(@Valid TwoFaCodeRequest body) {
        String sub = jwt.getSubject();
        Cred cred = credentials.find(sub)
                .orElseThrow(() -> status(Response.Status.CONFLICT, "Nessuna iscrizione 2FA in corso."));
        if (cred.totpSecret() == null || !totp.verify(cred.totpSecret(), body.code())) {
            throw unauthorized("Codice 2FA non valido.");
        }
        credentials.setTotp(sub, cred.totpSecret(), true);
        return Response.noContent().build();
    }

    // ── helper ─────────────────────────────────────────────────────────────
    private Response tokenResponse(AuthUser user) {
        Set<String> groups = tokens.groupsFor(user);
        TokenResponse body = new TokenResponse(
                tokens.accessToken(user, groups), tokens.idToken(user), "Bearer", tokens.accessTtlSeconds());
        return Response.ok(body).cookie(refreshCookie(tokens.refreshToken(user), (int) tokens.refreshTtlSeconds())).build();
    }

    private String verifyOrBadRequest(String token, String use, String message) {
        try {
            return tokens.verifyTokenSubject(token, use);
        } catch (RuntimeException e) {
            throw status(Response.Status.BAD_REQUEST, message);
        }
    }

    private NewCookie refreshCookie(String value, int maxAge) {
        return new NewCookie.Builder(REFRESH_COOKIE)
                .value(value).path("/api/auth").httpOnly(true).secure(true)
                .sameSite(NewCookie.SameSite.LAX).maxAge(maxAge).build();
    }

    private static WebApplicationException unauthorized(String detail) {
        return new WebApplicationException(detail, Response.Status.UNAUTHORIZED);
    }

    private static WebApplicationException status(Response.Status s, String detail) {
        return new WebApplicationException(detail, s);
    }

    private static boolean constantTimeEquals(String a, String b) {
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }

    private static String sha256Hex(String token) {
        return TokenHashes.sha256Hex(token);
    }
}
