package app.appgrove.core.platform;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

/** DTO degli inviti. Il {@code token} grezzo è esposto SOLO nella risposta di creazione. */
public final class InvitationDtos {

    private InvitationDtos() {}

    public record CreateInvitation(
            @NotBlank @Email @Size(max = 320) String email,
            @NotBlank String role) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record InvitationView(
            UUID id, String email, String role, String status, Instant expiresAt, String token) {

        /** Vista senza token (list/get). */
        public static InvitationView from(Invitation i) {
            return new InvitationView(
                    i.getId(), i.getEmail(), i.getRole().name(), i.getStatus().name(), i.getExpiresAt(), null);
        }

        /** Vista con token grezzo (solo creazione). */
        public static InvitationView created(Invitation i, String token) {
            return new InvitationView(
                    i.getId(), i.getEmail(), i.getRole().name(), i.getStatus().name(), i.getExpiresAt(), token);
        }
    }
}
