package app.appgrove.core.platform;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

/**
 * DTO dei percorsi «di me stesso» introdotti da UC 0118: gli inviti ricevuti dalla persona in
 * sessione e l'apertura di un proprio account.
 *
 * <p><b>Nessun ruolo esce da qui.</b> Il ruolo dell'invito è dell'account che invita e non ha
 * significato per chi lo riceve — «ti invitano come collaboratore» sarebbe un'etichetta di ruolo
 * globale, che UC 0117 §4.6 vieta perché il ruolo è per applicazione. Chi decide se accettare guarda
 * <b>chi</b> lo invita, non con che etichetta.
 */
public final class MeDtos {

    private MeDtos() {}

    /**
     * Un invito ricevuto: chi invita e fino a quando vale.
     *
     * @param accountName il nome dell'azienda che invita — è ciò che rende consapevole il consenso
     */
    public record MyInvitationView(String id, String accountId, String accountName, Instant expiresAt) {}

    /** Gli inviti in attesa della persona in sessione; lista vuota quando non ce n'è nessuno. */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record MyInvitationsView(List<MyInvitationView> invitations) {}

    /**
     * Richiesta di apertura di un proprio account (UC 0118, percorso B). Serve <b>solo</b> il nome:
     * indirizzo, nome della persona e parola d'accesso ci sono già — chiederli di nuovo produrrebbe
     * una seconda identità con un indirizzo diverso, che è il difetto che questa storia esiste per
     * evitare.
     */
    public record CreateOwnAccount(@NotBlank @Size(max = 255) String name) {}

    /** L'account appena aperto: identificativo e nome. */
    public record OwnAccountView(String accountId, String name) {}
}
