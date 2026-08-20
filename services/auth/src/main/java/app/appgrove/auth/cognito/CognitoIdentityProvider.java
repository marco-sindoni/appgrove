package app.appgrove.auth.cognito;

import app.appgrove.auth.EmailService;
import app.appgrove.auth.IdentityProvider;
import app.appgrove.auth.InvalidTokenException;
import app.appgrove.auth.Locales;
import app.appgrove.auth.PlatformWriter;
import app.appgrove.auth.PlatformWriter.CreatedUser;
import app.appgrove.auth.PlatformWriter.InviteRow;
import io.quarkus.arc.lookup.LookupIfProperty;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthFlowType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthenticationResultType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ChallengeNameType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.CodeMismatchException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ExpiredCodeException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.InitiateAuthResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.MessageActionType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.NotAuthorizedException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.RespondToAuthChallengeResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.TooManyRequestsException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserNotConfirmedException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserNotFoundException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UsernameExistsException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.VerifySoftwareTokenResponseType;

/**
 * Provider Cognito (UC 0015): identità su Amazon Cognito, membership nello schema {@code platform}
 * (stesse scritture del provider locale, via RDS Proxy in cloud). Stesso contratto HTTP e stessi
 * messaggi problem+json del provider Local; i claim {@code tenant_id}/{@code roles} nei JWT
 * arrivano dal Pre-Token-Gen (UC 0016) leggendo le righe scritte qui.
 */
@LookupIfProperty(name = "auth.provider", stringValue = "cognito")
@ApplicationScoped
public class CognitoIdentityProvider implements IdentityProvider {

    @Inject
    CognitoConfig config;

    // Instance<>: client prodotto lazy (mai istanziato in profilo locale, mock nei test).
    @Inject
    Instance<CognitoIdentityProviderClient> cognito;

    @Inject
    PlatformWriter platform;

    @Inject
    EmailService email;

    @Override
    public LoginResult login(String emailAddr, String password) {
        InitiateAuthResponse res;
        try {
            res = client().initiateAuth(b -> b
                    .clientId(config.clientId())
                    .authFlow(AuthFlowType.USER_PASSWORD_AUTH)
                    .authParameters(Map.of(
                            "USERNAME", emailAddr,
                            "PASSWORD", password,
                            "SECRET_HASH", secretHash(emailAddr))));
        } catch (NotAuthorizedException | UserNotFoundException e) {
            throw unauthorized("Credenziali non valide.");
        } catch (UserNotConfirmedException e) {
            throw status(Response.Status.FORBIDDEN, "Email non verificata.");
        } catch (TooManyRequestsException e) {
            throw status(Response.Status.TOO_MANY_REQUESTS, "Troppi tentativi: riprova più tardi.");
        }
        if (res.challengeName() == ChallengeNameType.SOFTWARE_TOKEN_MFA) {
            return new LoginResult.MfaRequired(OpaqueTokens.join(emailAddr, res.session()));
        }
        if (res.authenticationResult() == null) {
            // Challenge diversa dal TOTP (es. NEW_PASSWORD_REQUIRED): non prevista dai nostri flussi.
            Log.warnf("Challenge Cognito non gestita al login: %s", res.challengeNameAsString());
            throw unauthorized("Credenziali non valide.");
        }
        return sessionOrAccountChoice(res.authenticationResult());
    }

    @Override
    public LoginResult loginMfa(String challengeToken, String code) {
        String[] parts = OpaqueTokens.split(challengeToken);
        String username = parts[0];
        String cognitoSession = parts[1];
        RespondToAuthChallengeResponse res;
        try {
            res = client().respondToAuthChallenge(b -> b
                    .clientId(config.clientId())
                    .challengeName(ChallengeNameType.SOFTWARE_TOKEN_MFA)
                    .session(cognitoSession)
                    .challengeResponses(Map.of(
                            "USERNAME", username,
                            "SOFTWARE_TOKEN_MFA_CODE", code,
                            "SECRET_HASH", secretHash(username))));
        } catch (CodeMismatchException | ExpiredCodeException e) {
            throw unauthorized("Codice 2FA non valido.");
        } catch (NotAuthorizedException e) {
            throw unauthorized("Challenge non valido.");
        }
        if (res.authenticationResult() == null) {
            throw unauthorized("Challenge non valido.");
        }
        return sessionOrAccountChoice(res.authenticationResult());
    }

    @Override
    public Session chooseAccount(String choiceToken, String accountId) {
        // Il token di scelta è la coppia (sub, refresh token) già usata per il cookie di rinnovo: qui è
        // esattamente ciò che serve, perché la sessione con il claim nuovo si ottiene RINNOVANDO dopo
        // aver conservato la scelta. Nessun token nuovo da inventare, nessuna password da ricordare.
        String[] parts = OpaqueTokens.split(choiceToken);
        if (!platform.chooseActiveAccount(parts[0], accountId)) {
            // 404 e non 403: l'esistenza di un account non è un'informazione di chi chiede.
            throw status(Response.Status.NOT_FOUND, "Account non trovato.");
        }
        // Il rinnovo fa girare di nuovo la funzione che compone il token, che rilegge la scelta appena
        // conservata e la riverifica: il claim continua a nascere in un posto solo.
        return refresh(choiceToken);
    }

    @Override
    public Session refresh(String refreshValue) {
        String[] parts = OpaqueTokens.split(refreshValue);
        String sub = parts[0];
        String refreshToken = parts[1];
        InitiateAuthResponse res;
        try {
            // SECRET_HASH del flusso refresh: username = sub (per questo il cookie porta il sub).
            res = client().initiateAuth(b -> b
                    .clientId(config.clientId())
                    .authFlow(AuthFlowType.REFRESH_TOKEN_AUTH)
                    .authParameters(Map.of(
                            "REFRESH_TOKEN", refreshToken,
                            "SECRET_HASH", secretHash(sub))));
        } catch (NotAuthorizedException | UserNotFoundException e) {
            throw new InvalidTokenException();
        }
        AuthenticationResultType result = res.authenticationResult();
        if (result == null) {
            throw new InvalidTokenException();
        }
        // Rotazione: con la refresh token rotation Cognito ritorna un nuovo refresh token;
        // in sua assenza si ripropone il precedente (stessa scadenza lato pool).
        String rotated = result.refreshToken() != null ? result.refreshToken() : refreshToken;
        return session(result, sub, rotated);
    }

    @Override
    public void logout(String refreshValue) {
        try {
            String refreshToken = OpaqueTokens.split(refreshValue)[1];
            client().revokeToken(b -> b
                    .clientId(config.clientId())
                    .clientSecret(config.clientSecret())
                    .token(refreshToken));
        } catch (RuntimeException e) {
            // Il logout non deve fallire per un token già revocato/malformato: il cookie viene
            // comunque cancellato dal resource (fail-safe, non fail-closed: qui non si autentica).
            Log.debugf("RevokeToken fallita al logout: %s", e.toString());
        }
    }

    @Override
    public void signup(String emailAddr, String password, String displayName, String locale) {
        String lang = Locales.normalize(locale);
        String sub;
        try {
            sub = client().signUp(b -> {
                b.clientId(config.clientId())
                        .secretHash(secretHash(emailAddr))
                        .username(emailAddr)
                        .password(password)
                        .userAttributes(userAttributes(emailAddr, displayName))
                        // La lingua deve viaggiare QUI: Cognito manda l'email di verifica durante
                        // questa chiamata, quando la riga in platform.identity non esiste ancora.
                        .clientMetadata(localeMetadata(lang));
            }).userSub();
        } catch (UsernameExistsException e) {
            throw status(Response.Status.CONFLICT, "Email già registrata.");
        }
        try {
            platform.createAccountWithOwner(sub, emailAddr, displayName, lang);
        } catch (RuntimeException e) {
            // Compensazione: niente utente Cognito orfano se la membership non è scrivibile.
            deleteQuietly(emailAddr);
            throw e;
        }
        // L'email col codice di verifica la manda Cognito, resa dal Custom Message Lambda (UC 0018).
    }

    @Override
    public String emailActionToken(String emailAddr, String code) {
        return OpaqueTokens.join(emailAddr, code);
    }

    @Override
    public Optional<Session> verifyEmail(String token) {
        String[] parts = emailToken(token, "Token di verifica non valido o scaduto.");
        try {
            client().confirmSignUp(b -> b
                    .clientId(config.clientId())
                    .secretHash(secretHash(parts[0]))
                    .username(parts[0])
                    .confirmationCode(parts[1]));
        } catch (CodeMismatchException | ExpiredCodeException | UserNotFoundException e) {
            throw status(Response.Status.BAD_REQUEST, "Token di verifica non valido o scaduto.");
        }
        // Niente auto-login: Cognito non emette token alla conferma (serve la password) → la SPA
        // rimanda al login. Divergenza consapevole dal provider Local (vedi UC 0017 UC2).
        return Optional.empty();
    }

    @Override
    public void resendVerification(String emailAddr) {
        Map<String, String> metadata = localeMetadata(platform.localeOf(emailAddr));
        try {
            client().resendConfirmationCode(b -> b
                    .clientId(config.clientId())
                    .secretHash(secretHash(emailAddr))
                    .username(emailAddr)
                    .clientMetadata(metadata));
        } catch (UserNotFoundException | NotAuthorizedException e) {
            // risposta neutra anti-enumeration (il resource risponde comunque 202)
        }
    }

    @Override
    public void forgotPassword(String emailAddr) {
        Map<String, String> metadata = localeMetadata(platform.localeOf(emailAddr));
        try {
            client().forgotPassword(b -> b
                    .clientId(config.clientId())
                    .secretHash(secretHash(emailAddr))
                    .username(emailAddr)
                    .clientMetadata(metadata));
        } catch (UserNotFoundException | NotAuthorizedException e) {
            // neutra (anti-enumeration)
        }
    }

    @Override
    public void resetPassword(String token, String newPassword) {
        String[] parts = emailToken(token, "Token di reset non valido o scaduto.");
        try {
            client().confirmForgotPassword(b -> b
                    .clientId(config.clientId())
                    .secretHash(secretHash(parts[0]))
                    .username(parts[0])
                    .confirmationCode(parts[1])
                    .password(newPassword));
        } catch (CodeMismatchException | ExpiredCodeException | UserNotFoundException e) {
            throw status(Response.Status.BAD_REQUEST, "Token di reset non valido o scaduto.");
        }
    }

    @Override
    public Session acceptInvitation(InviteRow invite, String password, String displayName, String locale) {
        String sub;
        try {
            // Email provata dal link d'invito (#02 / UC 0017 UC7) → utente creato già confermato,
            // nessuna email Cognito (MessageAction=SUPPRESS).
            var created = client().adminCreateUser(b -> b
                    .userPoolId(config.userPoolId())
                    .username(invite.email())
                    .messageAction(MessageActionType.SUPPRESS)
                    .userAttributes(userAttributes(invite.email(), displayName)));
            sub = created.user().attributes().stream()
                    .filter(a -> "sub".equals(a.name()))
                    .map(AttributeType::value)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Cognito non ha ritornato il sub"));
            client().adminSetUserPassword(b -> b
                    .userPoolId(config.userPoolId())
                    .username(invite.email())
                    .password(password)
                    .permanent(true));
        } catch (UsernameExistsException e) {
            // Parità col provider locale (UC 0118): chi ha già un'identità NON entra da qui — questo
            // percorso conierebbe una password nuova su una persona che ne ha già una. Il percorso vero
            // è la sezione degli inviti in testa al proprio cruscotto, e l'interfaccia ci arriva prima
            // interrogando /invitations/lookup: questo rifiuto è la rete di sicurezza dietro di essa.
            throw status(Response.Status.CONFLICT,
                    "Questo indirizzo è già registrato su appgrove: accedi con le tue credenziali "
                            + "e accetta l'invito dalla sezione in testa al tuo cruscotto.");
        }
        CreatedUser createdUser = platform.createUserInTenant(
                sub, invite.tenantId(), invite.email(), displayName, invite.role(), locale);
        platform.markInvitationAccepted(invite.id(), createdUser.id());
        LoginResult login = login(invite.email(), password); // auto-login come membro
        if (login instanceof LoginResult.Ok ok) {
            return ok.session();
        }
        throw unauthorized("Login post-invito non riuscito.");
    }

    @Override
    public Enrollment startTotpEnrollment(String bearerToken, String sub) {
        String secret = client().associateSoftwareToken(b -> b.accessToken(bearerToken)).secretCode();
        String label = client().getUser(b -> b.accessToken(bearerToken)).userAttributes().stream()
                .filter(a -> "email".equals(a.name()))
                .map(AttributeType::value)
                .findFirst()
                .orElse(sub);
        return new Enrollment(secret, otpauthUri(secret, label));
    }

    @Override
    public void confirmTotpEnrollment(String bearerToken, String sub, String code) {
        VerifySoftwareTokenResponseType status;
        try {
            status = client().verifySoftwareToken(b -> b.accessToken(bearerToken).userCode(code)).status();
        } catch (CodeMismatchException | software.amazon.awssdk.services.cognitoidentityprovider.model
                .EnableSoftwareTokenMfaException e) {
            throw unauthorized("Codice 2FA non valido.");
        }
        if (status != VerifySoftwareTokenResponseType.SUCCESS) {
            throw unauthorized("Codice 2FA non valido.");
        }
        client().setUserMFAPreference(b -> b
                .accessToken(bearerToken)
                .softwareTokenMfaSettings(s -> s.enabled(true).preferredMfa(true)));
    }

    @Override
    public boolean totpEnabled(String bearerToken, String sub) {
        // `userMFASettingList` elenca i fattori CONFERMATI: un'iscrizione avviata e mai confermata non
        // vi compare, che è esattamente la semantica che la lettura promette.
        return !client().getUser(b -> b.accessToken(bearerToken)).userMFASettingList().isEmpty();
    }

    @Override
    public Optional<String> jwks() {
        return Optional.empty(); // in cloud i servizi validano sul JWKS Cognito (issuer del pool)
    }

    // ── helper ─────────────────────────────────────────────────────────────
    private CognitoIdentityProviderClient client() {
        return cognito.get();
    }

    private Session session(AuthenticationResultType result) {
        String sub = subFromAccessToken(result.accessToken());
        return session(result, sub, result.refreshToken());
    }

    /**
     * Sessione, oppure la <b>sfida di scelta dell'account</b> (UC 0118).
     *
     * <p>Su Cognito il caso si riconosce dall'<b>assenza del claim {@code tenant_id}</b> nell'access
     * token appena emesso: la funzione che compone il token non solleva eccezioni quando non riesce a
     * scegliere — restituisce l'evento inalterato — quindi l'accesso riesce e il token esce senza
     * claim, e i servizi lo rifiutano (a chiusura, UC 0012). È l'unico segnale disponibile qui, ed è
     * affidabile perché è lo stesso che i servizi useranno.
     *
     * <p>La sfida si offre <b>solo</b> quando gli account fra cui scegliere sono più di uno. Un'unica
     * appartenenza attiva sarebbe stata scelta dalla funzione del token, quindi se il claim manca con
     * zero o una appartenenza il motivo è un altro (persona senza appartenenze, o sospesa): non è una
     * scelta da fare, ed è un token che i servizi rifiutano. Comportamento invariato rispetto a prima.
     */
    private LoginResult sessionOrAccountChoice(AuthenticationResultType result) {
        String sub = subFromAccessToken(result.accessToken());
        if (!hasTenantClaim(result.accessToken())) {
            var accounts = platform.activeAccountsOf(sub);
            if (accounts.size() > 1) {
                return new LoginResult.AccountSelectionRequired(
                        OpaqueTokens.join(sub, result.refreshToken()), accounts);
            }
            Log.warnf("Token senza tenant_id e %d appartenenze: nessuna scelta da offrire", accounts.size());
        }
        return new LoginResult.Ok(session(result, sub, result.refreshToken()));
    }

    /**
     * Il claim {@code tenant_id} è presente nell'access token? Stessa decodifica di
     * {@link #subFromAccessToken(String)} — token appena ricevuto da Cognito su TLS, fonte fidata e
     * non input del client, quindi nessuna verifica di firma necessaria.
     */
    static boolean hasTenantClaim(String accessToken) {
        try {
            String[] segments = accessToken.split("\\.");
            String payload = new String(Base64.getUrlDecoder().decode(segments[1]), StandardCharsets.UTF_8);
            return java.util.regex.Pattern.compile("\"tenant_id\"\\s*:\\s*\"[^\"]+\"")
                    .matcher(payload)
                    .find();
        } catch (RuntimeException e) {
            throw new IllegalStateException("Access token Cognito non decodificabile", e);
        }
    }

    private Session session(AuthenticationResultType result, String sub, String refreshToken) {
        return new Session(
                result.accessToken(), result.idToken(), result.expiresIn(),
                OpaqueTokens.join(sub, refreshToken), config.refreshTtlSeconds());
    }

    /**
     * Estrae il {@code sub} dal payload dell'access token appena ricevuto da Cognito su TLS:
     * nessuna verifica di firma necessaria (fonte fidata, non input del client).
     */
    static String subFromAccessToken(String accessToken) {
        try {
            String[] segments = accessToken.split("\\.");
            String payload = new String(Base64.getUrlDecoder().decode(segments[1]), StandardCharsets.UTF_8);
            var matcher = java.util.regex.Pattern.compile("\"sub\"\\s*:\\s*\"([^\"]+)\"").matcher(payload);
            if (!matcher.find()) {
                throw new IllegalStateException("claim sub assente");
            }
            return matcher.group(1);
        } catch (RuntimeException e) {
            throw new IllegalStateException("Access token Cognito non decodificabile", e);
        }
    }

    private String[] emailToken(String token, String message) {
        try {
            return OpaqueTokens.split(token);
        } catch (InvalidTokenException e) {
            throw status(Response.Status.BAD_REQUEST, message);
        }
    }

    private void deleteQuietly(String username) {
        try {
            client().adminDeleteUser(b -> b.userPoolId(config.userPoolId()).username(username));
        } catch (RuntimeException e) {
            Log.warnf("Compensazione signup: AdminDeleteUser fallita per %s: %s", username, e.toString());
        }
    }

    /**
     * Contesto applicativo passato a Cognito e inoltrato tale e quale al Custom Message Lambda
     * (UC 0018), che ne ricava la lingua del template.
     *
     * <p>Cognito <b>non memorizza</b> questi valori: valgono per la singola chiamata. Se un
     * messaggio venisse generato senza una nostra chiamata, la Lambda non li troverebbe e
     * ripiegherebbe sull'inglese — comportamento voluto, non una svista.
     */
    private static Map<String, String> localeMetadata(String locale) {
        return Map.of("locale", Locales.normalize(locale));
    }

    private java.util.List<AttributeType> userAttributes(String emailAddr, String displayName) {
        var attrs = new java.util.ArrayList<AttributeType>();
        attrs.add(AttributeType.builder().name("email").value(emailAddr).build());
        if (displayName != null && !displayName.isBlank()) {
            attrs.add(AttributeType.builder().name("name").value(displayName).build());
        }
        return attrs;
    }

    /** SECRET_HASH Cognito: Base64(HMAC-SHA256(client secret, username + client id)). */
    private String secretHash(String username) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(config.clientSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getEncoder()
                    .encodeToString(mac.doFinal((username + config.clientId()).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Calcolo SECRET_HASH fallito", e);
        }
    }

    private static String otpauthUri(String secret, String label) {
        String encodedLabel = URLEncoder.encode(label, StandardCharsets.UTF_8).replace("+", "%20");
        return "otpauth://totp/appgrove:" + encodedLabel
                + "?secret=" + secret + "&issuer=appgrove&algorithm=SHA1&digits=6&period=30";
    }

    private static WebApplicationException unauthorized(String detail) {
        return new WebApplicationException(detail, Response.Status.UNAUTHORIZED);
    }

    private static WebApplicationException status(Response.Status s, String detail) {
        return new WebApplicationException(detail, s);
    }
}
