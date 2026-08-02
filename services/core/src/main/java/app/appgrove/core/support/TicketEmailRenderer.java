package app.appgrove.core.support;

import app.appgrove.commons.email.EmailLocales;
import app.appgrove.commons.email.EmailTemplateRenderer;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Map;
import java.util.Set;

/**
 * Email del ticketing (UC 0075): adattatore sottile sul <b>renderer unico</b> della piattaforma
 * ({@link EmailTemplateRenderer} in {@code services/commons}, UC 0085).
 *
 * <p>Prima di questa change le email dei ticket erano stringhe concatenate a mano dentro
 * {@code TicketNotifier}: solo testo, solo italiano, senza impaginazione e fuori dalla sorgente
 * unica {@code shared/email-templates}. Erano l'unico canale email della piattaforma rimasto
 * indietro, e la duplicazione che UC 0085 esiste per chiudere.
 *
 * <p>Le lingue sono le due delle email transazionali (inglese e italiano, ripiego sull'inglese,
 * convenzione UC 0018): è la stessa scelta della newsletter, e corrisponde alla colonna
 * {@code users.locale}, che ammette solo quei due valori.
 */
@ApplicationScoped
public class TicketEmailRenderer {

    /** Conferma di apertura di un ticket di supporto generico. */
    static final String OPENED = "ticket-opened";

    /** Conferma di apertura di un'istanza privacy: dice il termine di legge e la data. */
    static final String OPENED_PRIVACY = "ticket-opened-privacy";

    /** Avviso di aggiornamento (risposta o cambio stato) a chi ha aperto la richiesta. */
    static final String UPDATED = "ticket-updated";

    /** Avviso alla casella di assistenza della piattaforma. */
    static final String INBOX = "ticket-inbox";

    /** Lingue delle email transazionali (#13 G38): inglese e italiano, ripiego sull'inglese. */
    static final EmailLocales LOCALES = new EmailLocales("en", Set.of("en", "it"));

    private EmailTemplateRenderer renderer;

    @PostConstruct
    void load() {
        renderer = new EmailTemplateRenderer(LOCALES);
    }

    /** Rende il messaggio {@code key} nella lingua indicata, ricondotta a una supportata. */
    public EmailTemplateRenderer.Rendered render(String locale, String key, Map<String, String> values) {
        return renderer.render(locale, key, values);
    }
}
