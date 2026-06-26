package app.appgrove.authlocal;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** DTO degli endpoint auth. */
public final class AuthDtos {

    private AuthDtos() {}

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}

    /** Risposta token (access/id nel body; il refresh è nel cookie HttpOnly). */
    public record TokenResponse(String access_token, String id_token, String token_type, long expires_in) {}
}
