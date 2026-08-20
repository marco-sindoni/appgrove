package app.appgrove.core.platform;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO delle appartenenze della persona in sessione e del cambio di account attivo (UC 0117).
 *
 * <p><b>Nessuna etichetta di ruolo</b> (UC 0117 §4.6): il ruolo è per applicazione, e una etichetta
 * globale sarebbe falsa appena una persona è abilitata a più di una applicazione. Qui si dice a quali
 * account la persona appartiene e come si chiamano — il ruolo si legge dove è vero.
 */
public final class MembershipDtos {

    private MembershipDtos() {}

    /** Un account a cui la persona appartiene: identificativo e nome, nulla di più. */
    public record MembershipRef(String accountId, String accountName) {}

    /**
     * Le appartenenze della persona in sessione.
     *
     * @param activeAccountId account attivo secondo la regola che compone il token; {@code null}
     *     quando le appartenenze sono più di una e nessuna scelta è valida — lo stesso caso in cui il
     *     token nasce senza claim
     * @param memberships gli account, in ordine di anzianità dell'appartenenza
     */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record MyMembershipsView(String activeAccountId, java.util.List<MembershipRef> memberships) {}

    /** Richiesta di cambio: l'account su cui la persona vuole lavorare. */
    public record SetActiveAccount(@NotBlank String accountId) {}
}
