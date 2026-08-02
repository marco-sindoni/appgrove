package app.appgrove.core.newsletter;

import app.appgrove.commons.email.EmailLocales;
import app.appgrove.commons.email.EmailTemplateRenderer;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Map;
import java.util.Set;

/**
 * Email di conferma della newsletter (UC 0039): adattatore sottile sul <b>renderer unico</b> della
 * piattaforma ({@link EmailTemplateRenderer} in {@code services/commons}, UC 0085).
 *
 * <p>Era "il gemello compatto" del renderer del servizio di autenticazione — stessi due passaggi,
 * stesso escape, stessa guardia sui segnaposto — e la duplicazione è stata chiusa: qui restano solo
 * le <b>lingue della newsletter</b> (inglese e italiano, ripiego sull'inglese, convenzione UC 0018)
 * e la chiave del messaggio. I testi restano nella sorgente unica {@code shared/email-templates},
 * copiata nell'artefatto a build time (vedi {@code pom.xml}).
 */
@ApplicationScoped
public class NewsletterEmailRenderer {

    private static final String MESSAGE_KEY = "newsletter-confirm";

    /** Lingue della newsletter. Passare alle cinque lingue del sito è una scelta di UC 0039. */
    static final EmailLocales LOCALES = new EmailLocales("en", Set.of("en", "it"));

    private EmailTemplateRenderer renderer;

    @PostConstruct
    void load() {
        renderer = new EmailTemplateRenderer(LOCALES);
    }

    /** Rende il messaggio di conferma con il collegamento {@code actionUrl}. */
    public EmailTemplateRenderer.Rendered renderConfirm(String locale, String actionUrl) {
        return renderer.render(locale, MESSAGE_KEY, Map.of("actionUrl", actionUrl));
    }

    /**
     * Riconduce una lingua qualsiasi a una delle lingue della newsletter. Usata anche fuori dalle
     * email: è la stessa scelta di lingua con cui si scrive l'iscritto a database e si rende la
     * pagina di esito della conferma.
     */
    static String normalize(String raw) {
        return LOCALES.normalize(raw);
    }
}
