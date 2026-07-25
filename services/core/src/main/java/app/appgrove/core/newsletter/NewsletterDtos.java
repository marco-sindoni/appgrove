package app.appgrove.core.newsletter;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Contratti REST della newsletter (UC 0039). */
public final class NewsletterDtos {

    private NewsletterDtos() {}

    /**
     * Iscrizione dall'endpoint pubblico (sito vetrina / checkbox al signup).
     *
     * @param email     indirizzo da iscrivere
     * @param locale    lingua preferita per l'email di conferma (facoltativa, ripiego su en)
     * @param consent   deve essere esplicitamente {@code true} (checkbox non pre-spuntata)
     * @param channel   {@code site} o {@code signup} (default {@code site})
     * @param website   campo esca ("honeypot"): i browser umani lo lasciano vuoto, i bot lo riempiono
     */
    public record SubscribeRequest(
            @NotBlank @Email String email,
            String locale,
            Boolean consent,
            String channel,
            String website) {}

    /** Stato della preferenza newsletter dell'utente autenticato. */
    public record PreferenceView(boolean subscribed) {}

    /** Aggiornamento della preferenza newsletter dell'utente autenticato. */
    public record PreferenceRequest(boolean subscribed) {}
}
