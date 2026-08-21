package app.appgrove.core.platform;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * DTO degli accessi per applicazione (UC 0098). L'identificativo della persona è quello
 * dell'<b>identità</b> — lo stesso che l'API delle persone dell'account già espone come {@code id} —
 * così che chi legge le due superfici parli della stessa persona con lo stesso numero.
 */
public final class AppAccessDtos {

    private AppAccessDtos() {}

    /**
     * Una persona che ha accesso all'applicazione.
     *
     * <p>{@code implicit} distingue l'<b>owner</b>, che non ha riga propria e non è revocabile, dalle
     * persone abilitate una per una: senza quel campo l'interfaccia dovrebbe dedurlo, e dedurre un
     * permesso è il modo di sbagliarlo.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record AppAccessView(
            UUID identityId, String email, String displayName, String role, boolean implicit) {}

    /**
     * Concessione. Il ruolo è <b>obbligatorio</b>: nessun valore predefinito lato servizio, perché un
     * potere concesso per omissione di un campo è il modo peggiore di concederlo. Il predefinito
     * prudente da proporre nell'interfaccia è materia di UC 0111.
     */
    public record GrantAccess(@NotNull UUID identityId, @NotBlank String role) {}

    /** Cambio di ruolo: solo il ruolo, la terna non si modifica. */
    public record ChangeRole(@NotBlank String role) {}
}
