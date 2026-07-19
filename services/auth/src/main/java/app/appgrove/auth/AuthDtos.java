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

    public record EnrollResponse(String secret, String otpauth_uri) {}
}
