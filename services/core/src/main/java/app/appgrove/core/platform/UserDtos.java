package app.appgrove.core.platform;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/** DTO degli utenti. {@code tenantId} è derivato (JWT), in sola lettura. */
public final class UserDtos {

    private UserDtos() {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record UserView(
            UUID id, String email, String displayName, String role, String status, String tenantId) {
        public static UserView from(User u) {
            return new UserView(
                    u.getId(),
                    u.getEmail(),
                    u.getDisplayName(),
                    u.getRole().name(),
                    u.getStatus().name(),
                    u.getTenantId());
        }
    }

    /** Patch di un utente: campi opzionali (null = invariato). I valori enum sono validati nel resource. */
    public record UpdateUser(String role, String status, @Size(max = 255) String displayName) {}

    /** Rettifica self-service del proprio profilo (art. 16, UC 0033): solo il nome visualizzato. */
    public record UpdateMe(@NotBlank @Size(max = 255) String displayName) {}
}
