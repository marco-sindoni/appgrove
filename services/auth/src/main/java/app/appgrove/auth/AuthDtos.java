package app.appgrove.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** DTO degli endpoint auth. */
public final class AuthDtos {

    private AuthDtos() {}

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}

    /** Risposta token (access/id nel body; il refresh è nel cookie HttpOnly). */
    public record TokenResponse(String access_token, String id_token, String token_type, long expires_in) {}

    // ── flussi (UC 0058) ─────────────────────────────────────────────────────
    /** {@code locale} (UC 0018): lingua dell'interfaccia al momento della registrazione; assente → EN. */
    public record SignupRequest(
            @NotBlank @Email String email, @NotBlank String password, String displayName, String locale) {}

    public record SignupResponse(String status) {}

    /**
     * Verifica indirizzo, in <b>due forme alternative</b> (UC 0018):
     * <ul>
     *   <li>{@code token} — collegamento del provider locale, che conia i propri token;
     *   <li>{@code email} + {@code code} — collegamento generato dal Custom Message Lambda, dove il
     *       codice non esiste ancora quando il messaggio viene composto e lo sostituisce Cognito.
     * </ul>
     * La validazione "una delle due" è nel resource: Bean Validation da sola non la esprime.
     */
    public record VerifyRequest(String token, @Email String email, String code) {}

    /** Risposta del verify quando il provider non può auto-loggare (Cognito): solo conferma. */
    public record VerifiedResponse(String status) {}

    public record EmailRequest(@NotBlank @Email String email) {}

    /** Reimpostazione password: stesse due forme di {@link VerifyRequest}. */
    public record ResetRequest(String token, @Email String email, String code, @NotBlank String password) {}

    /** {@code locale}: lingua scelta dall'invitato mentre accetta (è il suo primo contatto). */
    public record AcceptInviteRequest(
            @NotBlank String token, @NotBlank String password, String displayName, String locale) {}

    /** {@code locale}: lingua dell'interfaccia di chi invita — l'invitato non è ancora conosciuto. */
    public record InviteSendRequest(
            @NotBlank @Email String email, @NotBlank String token, String role, String locale) {}

    public record LoginTwoFaRequest(@NotBlank String challenge_token, @NotBlank String code) {}

    public record TwoFaCodeRequest(@NotBlank String code) {}

    /** Risposta del login quando il 2FA è attivo: niente token finché non si supera la challenge. */
    public record MfaChallenge(boolean mfa_required, String challenge_token) {}

    /** Un account fra cui scegliere all'accesso: identificativo e nome (nessuna etichetta di ruolo). */
    public record AccountOption(String account_id, String account_name) {}

    /**
     * Risposta dell'accesso quando la persona appartiene a più account e nessuno è indicato come
     * attivo (UC 0118): niente token finché non ha scelto. Stessa forma della sfida del secondo
     * fattore — un indicatore, un token breve — più l'elenco fra cui scegliere, che senza il nome degli
     * account sarebbe una scelta alla cieca.
     */
    public record AccountSelectionChallenge(
            boolean account_selection_required, String choice_token, java.util.List<AccountOption> accounts) {}

    /** Scelta dell'account con cui aprire la sessione (UC 0118). */
    public record ChooseAccountRequest(@NotBlank String choice_token, @NotBlank String account_id) {}

    /**
     * Ispezione di un invito prima di mostrarne il modulo (UC 0118): la risposta la vede solo chi ha in
     * mano il token dell'invito, cioè la persona invitata — non l'account che ha invitato.
     *
     * @param mode {@code register} se quell'indirizzo non ha ancora un'identità (serve la parola
     *     d'accesso), {@code signin} se ce l'ha già (serve solo che si autentichi: una parola d'accesso
     *     nuova su un'identità esistente sarebbe una seconda identità mascherata)
     * @param email l'indirizzo invitato — è già suo, l'ha ricevuto per posta
     */
    public record InviteLookupResponse(String mode, String email) {}

    /** Il token dell'invito da ispezionare. */
    public record InviteLookupRequest(@NotBlank String token) {}

    public record EnrollResponse(String secret, String otpauth_uri) {}

    /** Stato del secondo fattore dell'utente del token: attivo o no. Nessun segreto, nessun dettaglio. */
    public record TwoFaStatusResponse(boolean enabled) {}
}
