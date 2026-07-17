package app.appgrove.auth;

import app.appgrove.auth.PlatformWriter.InviteRow;
import java.util.Optional;

/**
 * Porta del BFF auth (UC 0010 §6 / UC 0015): lo strato HTTP ({@link AuthResource}) è unico e
 * dietro questa interfaccia stanno le due implementazioni — {@code Local} (Postgres, profilo dev,
 * UC 0010/0058) e {@code Cognito} (test/prod, UC 0015). Selezione a runtime con la config
 * {@code auth.provider} (default {@code local}).
 *
 * <p>Contratto comune: errori come {@code WebApplicationException} con messaggio utente (mappati
 * problem+json da commons), {@link InvalidTokenException} per token non validi (401 fail-closed).
 * I valori {@code refreshValue} sono opachi per il chiamante: finiscono SOLO nel cookie HttpOnly.
 */
public interface IdentityProvider {

    /** Sessione emessa: access/id nel body, refresh nel cookie (rotazione a ogni refresh). */
    record Session(
            String accessToken, String idToken, long expiresInSeconds,
            String refreshValue, long refreshTtlSeconds) {}

    /** Esito del login: sessione, oppure challenge 2FA da completare con {@code loginMfa}. */
    sealed interface LoginResult {
        record Ok(Session session) implements LoginResult {}

        record MfaRequired(String challengeToken) implements LoginResult {}
    }

    /** Iscrizione TOTP avviata: segreto + URI otpauth:// per l'app authenticator. */
    record Enrollment(String secret, String otpauthUri) {}

    LoginResult login(String email, String password);

    Session loginMfa(String challengeToken, String code);

    Session refresh(String refreshValue);

    /** Revoca il refresh token (Cognito {@code RevokeToken}); il clear del cookie è del resource. */
    void logout(String refreshValue);

    /** Crea identità + account/owner nello schema platform; invia (o fa inviare) l'email di verifica. */
    void signup(String email, String password, String displayName);

    /**
     * Conferma l'email. Ritorna la sessione se il provider può auto-loggare l'utente (Local);
     * vuoto se la conferma avviene senza credenziali disponibili (Cognito → la SPA rimanda al login).
     */
    Optional<Session> verifyEmail(String token);

    void resendVerification(String email);

    void forgotPassword(String email);

    void resetPassword(String token, String newPassword);

    /**
     * Accept invito: crea l'identità (email già provata dal link → confermata), l'utente nel tenant
     * invitante col ruolo dell'invito, marca l'invito accettato e auto-logga.
     */
    Session acceptInvitation(InviteRow invite, String password, String displayName);

    Enrollment startTotpEnrollment(String bearerToken, String sub);

    void confirmTotpEnrollment(String bearerToken, String sub, String code);

    /** JWKS locale (solo provider Local: in cloud i servizi validano sul JWKS Cognito). */
    Optional<String> jwks();
}
