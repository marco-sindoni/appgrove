package app.appgrove.core.platform;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
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

    /**
     * Una applicazione su cui la persona è abilitata, con il ruolo che vi ha (UC 0100). È
     * un'informazione in <b>sola lettura</b>: il ruolo su una applicazione si cambia dalla gestione
     * utenti dell'applicazione (UC 0111), non da qui.
     *
     * <p>{@code implicit} distingue l'accesso dell'<b>owner</b> — che non ha righe di permesso e ce
     * l'ha su tutte le applicazioni a cui l'account ha diritto (UC 0098 §5) — da quello di chi è stato
     * abilitato una applicazione alla volta. Per l'owner {@code role} è quindi nullo: non ha un ruolo
     * <i>su</i> quella applicazione, ce l'ha sull'account. Stessa forma già usata da
     * {@link AppAccessDtos.AppAccessView}, perché le due superfici parlano della stessa cosa.
     *
     * @param appId identificativo dell'applicazione nel catalogo
     * @param app nome breve stabile dell'applicazione (lo stesso che il frontend usa come chiave)
     * @param role ruolo sull'applicazione ({@code admin}/{@code editor}/{@code viewer}), nullo se implicito
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record UserAppView(UUID appId, String app, String role, boolean implicit) {}

    /**
     * Vista di una persona dell'account.
     *
     * <p>{@code apps} e {@code joinedAt} servono all'<b>elenco unico</b> di UC 0100: quante e quali
     * applicazioni la persona usa, e da quando fa parte del gruppo di lavoro. {@code apps} è
     * valorizzato solo nelle letture di <b>governo</b> dell'account (elenco delle persone e lettura per
     * identificativo) e resta assente su {@code /users/me}: quella è la lettura più calda
     * dell'applicazione — parte a ogni caricamento di pagina, per ogni ruolo — e caricarvi i diritti
     * dell'account e gli accessi per applicazione ne farebbe pagare il costo a chi non li usa.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record UserView(
            UUID id,
            String email,
            String displayName,
            String role,
            String status,
            String tenantId,
            Instant joinedAt,
            List<UserAppView> apps) {

        /**
         * Lo stato mostrato è quello dell'appartenenza, tranne quando la persona è sospesa sulla
         * piattaforma (limitazione del trattamento, art. 18): in quel caso vince la sospensione
         * dell'identità, perché è la più forte delle due e nasconderla mostrerebbe come attiva una
         * persona che non può accedere.
         */
        public static UserView from(Membership membership, Identity identity) {
            return from(membership, identity, null);
        }

        /** Come sopra, con l'elenco delle applicazioni della persona (letture di governo dell'account). */
        public static UserView from(Membership membership, Identity identity, List<UserAppView> apps) {
            String status = identity.getStatus() == IdentityStatus.suspended
                    ? IdentityStatus.suspended.name()
                    : membership.getStatus().name();
            return new UserView(
                    identity.getId(),
                    identity.getEmail(),
                    identity.getDisplayName(),
                    membership.getRole().name(),
                    status,
                    membership.getTenantId(),
                    membership.getCreatedAt(),
                    apps);
        }
    }

    /** Patch di una persona dell'account: campi opzionali (null = invariato). Gli enum sono validati nel resource. */
    public record UpdateUser(String role, String status, @Size(max = 255) String displayName) {}

    /** Rettifica self-service del proprio profilo (art. 16, UC 0033): solo il nome visualizzato. */
    public record UpdateMe(@NotBlank @Size(max = 255) String displayName) {}
}
