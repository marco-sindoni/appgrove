package app.appgrove.auth.local;

import app.appgrove.auth.AuthUser;
import app.appgrove.auth.EmailService;
import app.appgrove.auth.IdentityProvider;
import app.appgrove.auth.PlatformWriter;
import app.appgrove.auth.PlatformWriter.CreatedUser;
import app.appgrove.auth.PlatformWriter.InviteRow;
import app.appgrove.auth.local.CredentialsRepository.Cred;
import io.quarkus.arc.lookup.LookupIfProperty;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Provider Local (UC 0010 security-core + UC 0058 flussi): identità sul Postgres locale, JWT
 * firmati in proprio (claim dal DB replicando il Pre-Token-Gen), TOTP con lib reale, email su
 * Mailpit. Comportamento invariato rispetto a prima dell'estrazione della porta (change 0037).
 */
@LookupIfProperty(name = "auth.provider", stringValue = "local", lookupIfMissing = true)
@ApplicationScoped
public class LocalIdentityProvider implements IdentityProvider {

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

    @Override
    public LoginResult login(String emailAddr, String password) {
        AuthUser user = directory.findByEmail(emailAddr).filter(AuthUser::isActive)
                .orElseThrow(() -> unauthorized("Credenziali non valide."));
        Optional<Cred> credOpt = credentials.find(user.sub());
        if (credOpt.isPresent()) {
            Cred cred = credOpt.get();
            if (!Passwords.verify(password, cred.passwordHash())) {
                throw unauthorized("Credenziali non valide.");
            }
            if (!cred.emailVerified()) {
                throw status(Response.Status.FORBIDDEN, "Email non verificata.");
            }
            if (cred.totpEnabled() && !totpBypass) {
                return new LoginResult.MfaRequired(tokens.mfaChallengeToken(user.sub()));
            }
        } else if (!constantTimeEquals(devPassword, password)) {
            // utente del seed (nessuna riga credenziali) → fallback password dev universale
            throw unauthorized("Credenziali non valide.");
        }
        return new LoginResult.Ok(session(user));
    }

    @Override
    public Session loginMfa(String challengeToken, String code) {
        String sub = tokens.verifyTokenSubject(challengeToken, "mfa_challenge");
        Cred cred = credentials.find(sub).orElseThrow(() -> unauthorized("Challenge non valido."));
        if (!cred.totpEnabled() || !totp.verify(cred.totpSecret(), code)) {
            throw unauthorized("Codice 2FA non valido.");
        }
        return session(activeUser(sub, "Utente non valido."));
    }

    @Override
    public Session refresh(String refreshValue) {
        String sub = tokens.verifyRefreshSubject(refreshValue);
        return session(activeUser(sub, "Utente non più valido."));
    }

    @Override
    public void logout(String refreshValue) {
        // Nessuno stato server-side per il refresh token locale: basta il clear del cookie.
    }

    @Override
    public void signup(String emailAddr, String password, String displayName) {
        if (directory.findByEmail(emailAddr).isPresent()) {
            throw status(Response.Status.CONFLICT, "Email già registrata.");
        }
        CreatedUser created = platform.createAccountWithOwner(localSub(), emailAddr, displayName);
        credentials.create(created.user().sub(), Passwords.hash(password), false);
        email.sendVerify(emailAddr, tokens.emailVerifyToken(created.user().sub()));
    }

    @Override
    public Optional<Session> verifyEmail(String token) {
        String sub = verifyOrBadRequest(token, "email_verify", "Token di verifica non valido o scaduto.");
        credentials.setEmailVerified(sub);
        return Optional.of(session(activeUser(sub, "Utente non valido."))); // auto-login post-verifica (UC1 step 4)
    }

    @Override
    public void resendVerification(String emailAddr) {
        directory.findByEmail(emailAddr).ifPresent(u ->
                credentials.find(u.sub()).filter(c -> !c.emailVerified())
                        .ifPresent(c -> email.sendVerify(u.email(), tokens.emailVerifyToken(u.sub()))));
    }

    @Override
    public void forgotPassword(String emailAddr) {
        directory.findByEmail(emailAddr)
                .ifPresent(u -> email.sendReset(u.email(), tokens.passwordResetToken(u.sub())));
    }

    @Override
    public void resetPassword(String token, String newPassword) {
        String sub = verifyOrBadRequest(token, "pwd_reset", "Token di reset non valido o scaduto.");
        credentials.create(sub, Passwords.hash(newPassword), true); // upsert: reset implica email verificata
    }

    @Override
    public Session acceptInvitation(InviteRow invite, String password, String displayName) {
        CreatedUser created =
                platform.createUserInTenant(localSub(), invite.tenantId(), invite.email(), displayName, invite.role());
        credentials.create(created.user().sub(), Passwords.hash(password), true); // link prova l'email
        platform.markInvitationAccepted(invite.id(), created.id());
        return session(created.user()); // auto-login come membro
    }

    @Override
    public Enrollment startTotpEnrollment(String bearerToken, String sub) {
        AuthUser user = directory.findBySub(sub).orElseThrow(() -> unauthorized("Utente non valido."));
        credentials.find(sub).orElseThrow(() ->
                status(Response.Status.CONFLICT, "2FA non disponibile per account senza credenziali (utente seed)."));
        String secret = totp.newSecret();
        credentials.setTotp(sub, secret, false);
        return new Enrollment(secret, totp.otpauthUri(secret, user.email()));
    }

    @Override
    public void confirmTotpEnrollment(String bearerToken, String sub, String code) {
        Cred cred = credentials.find(sub)
                .orElseThrow(() -> status(Response.Status.CONFLICT, "Nessuna iscrizione 2FA in corso."));
        if (cred.totpSecret() == null || !totp.verify(cred.totpSecret(), code)) {
            throw unauthorized("Codice 2FA non valido.");
        }
        credentials.setTotp(sub, cred.totpSecret(), true);
    }

    @Override
    public Optional<String> jwks() {
        return Optional.of(tokens.jwks());
    }

    // ── helper ─────────────────────────────────────────────────────────────
    private Session session(AuthUser user) {
        Set<String> groups = tokens.groupsFor(user);
        return new Session(
                tokens.accessToken(user, groups), tokens.idToken(user), tokens.accessTtlSeconds(),
                tokens.refreshToken(user), tokens.refreshTtlSeconds());
    }

    private AuthUser activeUser(String sub, String message) {
        return directory.findBySub(sub).filter(AuthUser::isActive)
                .orElseThrow(() -> unauthorized(message));
    }

    private String verifyOrBadRequest(String token, String use, String message) {
        try {
            return tokens.verifyTokenSubject(token, use);
        } catch (RuntimeException e) {
            throw status(Response.Status.BAD_REQUEST, message);
        }
    }

    private static String localSub() {
        return "local-" + UUID.randomUUID();
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
}
