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

    /** Un account fra cui scegliere: identificativo e nome, nulla di più (nessuna etichetta di ruolo). */
    record AccountRef(String accountId, String accountName) {}

    /**
     * Esito del login: sessione, challenge 2FA da completare con {@code loginMfa}, oppure
     * <b>scelta dell'account</b> da completare con {@code chooseAccount} (UC 0118).
     *
     * <p>Il terzo esito esiste perché dopo UC 0116 una persona può appartenere a più account e il
     * token deve portarne <b>uno</b>: quando le appartenenze attive sono più di una e nessuna è
     * indicata come attiva, nessuno può decidere al posto suo. Prima di UC 0118 quel caso era un
     * rifiuto {@code 409} con un messaggio comprensibile ma senza superficie per rispondere; ora è
     * una sfida, additiva ed esattamente sul modello di quella del secondo fattore.
     */
    sealed interface LoginResult {
        record Ok(Session session) implements LoginResult {}

        record MfaRequired(String challengeToken) implements LoginResult {}

        record AccountSelectionRequired(String choiceToken, java.util.List<AccountRef> accounts)
                implements LoginResult {}
    }

    /** Iscrizione TOTP avviata: segreto + URI otpauth:// per l'app authenticator. */
    record Enrollment(String secret, String otpauthUri) {}

    LoginResult login(String email, String password);

    /**
     * Completa la sfida del secondo fattore. Ritorna un {@link LoginResult} e non direttamente una
     * sessione perché la scelta dell'account può servire <b>anche dopo</b> il secondo fattore: sono
     * due passaggi indipendenti, e fonderli avrebbe reso irraggiungibile la scelta per chi ha il
     * secondo fattore attivo.
     */
    LoginResult loginMfa(String challengeToken, String code);

    /**
     * Conserva l'account scelto dalla persona ed emette la sessione (UC 0118).
     *
     * <p>Il {@code choiceToken} è la prova che le credenziali sono già state verificate: senza di
     * esso questa operazione sarebbe il modo di scrivere l'account attivo di chiunque.
     * L'{@code accountId} è un <b>candidato</b> e viene accettato solo se corrisponde a
     * un'appartenenza attiva della persona del token, riverificata adesso — e il claim continua a
     * essere calcolato dalla sola funzione che compone il token, che a sua volta riverifica.
     *
     * @throws jakarta.ws.rs.WebApplicationException 404 se l'account non è un'appartenenza attiva
     *     della persona: non 403, perché l'esistenza di un account non è informazione di chi chiede
     */
    Session chooseAccount(String choiceToken, String accountId);

    Session refresh(String refreshValue);

    /** Revoca il refresh token (Cognito {@code RevokeToken}); il clear del cookie è del resource. */
    void logout(String refreshValue);

    /**
     * Crea identità + account/owner nello schema platform; invia (o fa inviare) l'email di verifica.
     *
     * @param locale lingua dell'email di verifica (UC 0018). Serve come parametro e non si legge dal
     *     DB perché al momento dell'invio l'utente <b>non è ancora stato scritto</b>: col provider
     *     Cognito l'email parte dalla chiamata di registrazione stessa.
     */
    void signup(String email, String password, String displayName, String locale);

    /**
     * Ricompone il token di verifica/reimpostazione dalla coppia indirizzo + codice (UC 0018).
     *
     * <p>Esiste perché il collegamento generato dal Custom Message Lambda porta i due valori
     * separati: quando la Lambda compone il messaggio il codice non esiste ancora (Cognito
     * sostituisce il segnaposto dopo), quindi non può produrre un token unico. Il formato del token
     * resta un dettaglio interno del provider — il resource non lo conosce.
     *
     * @throws jakarta.ws.rs.WebApplicationException 400 se il provider non usa questa forma
     */
    String emailActionToken(String email, String code);

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
    Session acceptInvitation(InviteRow invite, String password, String displayName, String locale);

    Enrollment startTotpEnrollment(String bearerToken, String sub);

    void confirmTotpEnrollment(String bearerToken, String sub, String code);

    /**
     * Vero se l'utente del token ha il secondo fattore <b>attivo</b> (iscrizione confermata), falso
     * altrimenti — anche quando l'iscrizione è stata avviata ma mai confermata.
     *
     * <p>Esiste perché il prodotto deve poter <b>dire la verità</b> sullo stato del secondo fattore
     * (UC 0097): un invito ad attivarlo mostrato a chi l'ha già attivo è peggio di nessun invito.
     * È una sola lettura: non avvia, non conferma e non disattiva nulla.
     */
    boolean totpEnabled(String bearerToken, String sub);

    /** JWKS locale (solo provider Local: in cloud i servizi validano sul JWKS Cognito). */
    Optional<String> jwks();
}
