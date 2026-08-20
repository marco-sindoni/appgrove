package app.appgrove.auth.local;

import app.appgrove.auth.AuthUser;
import app.appgrove.auth.EmailService;
import app.appgrove.auth.IdentityProvider;
import app.appgrove.auth.PlatformWriter;
import app.appgrove.auth.PlatformWriter.CreatedUser;
import app.appgrove.auth.PlatformWriter.InviteRow;
import app.appgrove.auth.local.CredentialsRepository.Cred;
import app.appgrove.auth.local.UserDirectory.Resolution;
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
        Resolution resolution = directory.resolveByEmail(emailAddr)
                .filter(r -> r.user().isActive())
                .orElseThrow(() -> unauthorized("Credenziali non valide."));
        AuthUser user = resolution.user();
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
        // La verifica dell'account attivo (UC 0117) sta ESATTAMENTE dove nascerebbe la sessione: le
        // credenziali sono già provate, quindi il messaggio non rivela nulla a chi non è la persona.
        requireChosenAccount(resolution);
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
    public void signup(String emailAddr, String password, String displayName, String locale) {
        // Dopo UC 0116 la domanda giusta è «esiste già questa PERSONA?», non «esiste già una persona con
        // questo indirizzo in qualche account?»: un'identità rimasta senza appartenenze non comparirebbe
        // nella prima lettura, e il rifiuto arriverebbe più tardi come violazione di indice.
        if (directory.findByEmail(emailAddr).isPresent() || platform.identityExists(emailAddr)) {
            throw status(Response.Status.CONFLICT, "Email già registrata.");
        }
        CreatedUser created = platform.createAccountWithOwner(localSub(), emailAddr, displayName, locale);
        credentials.create(created.user().sub(), Passwords.hash(password), false);
        email.sendVerify(emailAddr, created.user().locale(), tokens.emailVerifyToken(created.user().sub()));
    }

    @Override
    public String emailActionToken(String emailAddr, String code) {
        // Il provider locale conia i propri token e li mette per intero nel collegamento: la forma
        // indirizzo+codice esiste solo per i messaggi generati da Cognito (UC 0018).
        throw status(Response.Status.BAD_REQUEST, "Token di verifica non valido o scaduto.");
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
                        .ifPresent(c -> email.sendVerify(u.email(), u.locale(), tokens.emailVerifyToken(u.sub()))));
    }

    @Override
    public void forgotPassword(String emailAddr) {
        directory.findByEmail(emailAddr)
                .ifPresent(u -> email.sendReset(u.email(), u.locale(), tokens.passwordResetToken(u.sub())));
    }

    @Override
    public void resetPassword(String token, String newPassword) {
        String sub = verifyOrBadRequest(token, "pwd_reset", "Token di reset non valido o scaduto.");
        credentials.create(sub, Passwords.hash(newPassword), true); // upsert: reset implica email verificata
    }

    @Override
    public Session acceptInvitation(InviteRow invite, String password, String displayName, String locale) {
        // UC 0116 — il modello ora ammette che quella persona esista già (l'appartenenza sarebbe una in
        // più, non una identità in più). Questo percorso però conia un NUOVO identificativo di
        // autenticazione e una NUOVA password: applicarli a un'identità esistente significherebbe
        // permettere a un invito di rimettere in gioco le credenziali di qualcun altro. Quindi qui si
        // rifiuta, in modo comprensibile e non con una violazione di indice — e far entrare davvero chi
        // esiste già, dalla sua sessione e senza toccarne la password, è di UC 0118.
        // Il rifiuto lo vede solo l'invitato (ha in mano il proprio token d'invito), mai chi ha invitato:
        // nessuna interfaccia dell'account apprende da qui che quella persona esiste.
        if (platform.identityExists(invite.email())) {
            throw status(Response.Status.CONFLICT,
                    "Questo indirizzo è già registrato su appgrove: accedi con le tue credenziali "
                            + "e accetta l'invito dalla tua sessione.");
        }
        CreatedUser created = platform.createUserInTenant(
                localSub(), invite.tenantId(), invite.email(), displayName, invite.role(), locale);
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
    public boolean totpEnabled(String bearerToken, String sub) {
        // Un utente senza riga di credenziali (utente del seed) non ha 2FA: falso, non un errore —
        // questa lettura serve a decidere se mostrare un invito, non a negare un accesso.
        return credentials.find(sub).map(Cred::totpEnabled).orElse(false);
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

    /**
     * La persona del subject, pronta a ricevere una sessione. Rifiuta con {@code 409} quando ha più
     * appartenenze attive e nessuna scelta valida (UC 0117): nessun token può portare un account che
     * nessuno ha scelto, e dire «credenziali non valide» in quel caso sarebbe una bugia.
     */
    private AuthUser activeUser(String sub, String message) {
        Resolution resolution = directory.resolveBySub(sub)
                .filter(r -> r.user().isActive())
                .orElseThrow(() -> unauthorized(message));
        requireChosenAccount(resolution);
        return resolution.user();
    }

    /**
     * A chiusura in caso di dubbio: se l'account attivo non è determinato, non si emette nulla.
     *
     * <p>La <b>superficie</b> per rispondere a questa richiesta senza avere una sessione appartiene a
     * UC 0118 (la storia che rende raggiungibile il caso, perché è quella che crea la seconda
     * appartenenza): qui si garantisce l'esito tipizzato, il rifiuto e un messaggio che dice cosa è
     * successo. Il caso è comunque raro per costruzione — con una sola appartenenza residua la regola
     * la sceglie da sé, e ogni appartenenza nuova nasce già indicata come attiva.
     */
    private static void requireChosenAccount(Resolution resolution) {
        if (resolution.mustChooseAccount()) {
            throw status(Response.Status.CONFLICT,
                    "Appartieni a più account e nessuno è impostato come attivo: "
                            + "scegli l'account su cui vuoi lavorare.");
        }
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
