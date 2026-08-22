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

    /**
     * Invio di un invito: <b>solo l'indirizzo</b> (UC 0100). Il ruolo non si chiede più — non era una
     * scelta, perché il ruolo di piattaforma ha due soli valori e l'owner nasce con l'account (UC 0098):
     * chi entra entra come persona dell'account, e i poteri si concedono dopo, una applicazione alla
     * volta ({@code platform.app_access}).
     *
     * <p>Un {@code role} inviato da un chiamante vecchio viene <b>ignorato</b> (le proprietà sconosciute
     * non sono un errore) e non concede nulla: l'invito nasce sempre {@code member}. La garanzia che
     * conta è il valore fisso, non il codice di stato del rifiuto.
     */
    public record CreateInvitation(@NotBlank @Email @Size(max = 320) String email) {}

    /**
     * Vista di un invito per l'account che lo ha mandato. <b>Senza ruolo</b> (UC 0100): l'elenco unico
     * delle persone non ha una colonna dove metterlo, e un campo che vale sempre lo stesso valore è
     * rumore nel contratto.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record InvitationView(UUID id, String email, String status, Instant expiresAt, String token) {

        /** Vista senza token (list/get). */
        public static InvitationView from(Invitation i) {
            return new InvitationView(i.getId(), i.getEmail(), i.getStatus().name(), i.getExpiresAt(), null);
        }

        /** Vista con token grezzo (solo creazione). */
        public static InvitationView created(Invitation i, String token) {
            return new InvitationView(i.getId(), i.getEmail(), i.getStatus().name(), i.getExpiresAt(), token);
        }
    }
}
