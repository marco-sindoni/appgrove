package app.appgrove.core.platform;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * DTO delle persone dell'account. La <b>forma non cambia</b> con UC 0116: la schermata dei membri
 * continua a vedere un elenco di persone dell'account. Cambia da dove vengono i campi — ruolo,
 * stato e account dall'{@link Membership appartenenza}, indirizzo e nome dall'{@link Identity} — e
 * {@code id} resta l'identificativo della <b>persona</b> (l'identità), che è lo stesso di prima.
 * {@code tenantId} è derivato (JWT), in sola lettura.
 */
public final class UserDtos {

    private UserDtos() {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record UserView(
            UUID id, String email, String displayName, String role, String status, String tenantId) {

        /**
         * Lo stato mostrato è quello dell'appartenenza, tranne quando la persona è sospesa sulla
         * piattaforma (limitazione del trattamento, art. 18): in quel caso vince la sospensione
         * dell'identità, perché è la più forte delle due e nasconderla mostrerebbe come attiva una
         * persona che non può accedere.
         */
        public static UserView from(Membership membership, Identity identity) {
            String status = identity.getStatus() == IdentityStatus.suspended
                    ? IdentityStatus.suspended.name()
                    : membership.getStatus().name();
            return new UserView(
                    identity.getId(),
                    identity.getEmail(),
                    identity.getDisplayName(),
                    membership.getRole().name(),
                    status,
                    membership.getTenantId());
        }
    }

    /** Patch di una persona dell'account: campi opzionali (null = invariato). Gli enum sono validati nel resource. */
    public record UpdateUser(String role, String status, @Size(max = 255) String displayName) {}

    /** Rettifica self-service del proprio profilo (art. 16, UC 0033): solo il nome visualizzato. */
    public record UpdateMe(@NotBlank @Size(max = 255) String displayName) {}
}
