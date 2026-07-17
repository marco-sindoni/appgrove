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
    public record SignupRequest(@NotBlank @Email String email, @NotBlank String password, String displayName) {}

    public record SignupResponse(String status) {}

    public record VerifyRequest(@NotBlank String token) {}

    /** Risposta del verify quando il provider non può auto-loggare (Cognito): solo conferma. */
    public record VerifiedResponse(String status) {}

    public record EmailRequest(@NotBlank @Email String email) {}

    public record ResetRequest(@NotBlank String token, @NotBlank String password) {}

    public record AcceptInviteRequest(@NotBlank String token, @NotBlank String password, String displayName) {}

    public record InviteSendRequest(@NotBlank @Email String email, @NotBlank String token, String role) {}

    public record LoginTwoFaRequest(@NotBlank String challenge_token, @NotBlank String code) {}

    public record TwoFaCodeRequest(@NotBlank String code) {}

    /** Risposta del login quando il 2FA è attivo: niente token finché non si supera la challenge. */
    public record MfaChallenge(boolean mfa_required, String challenge_token) {}

    public record EnrollResponse(String secret, String otpauth_uri) {}
}
