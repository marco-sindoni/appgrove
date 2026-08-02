package app.appgrove.auth;

import app.appgrove.commons.email.EmailTemplateRenderer;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Map;

/**
 * Email di autenticazione (UC 0018): adattatore sottile sul <b>renderer unico</b> della piattaforma
 * ({@link EmailTemplateRenderer} in {@code services/commons}, UC 0085).
 *
 * <p>Qui non c'è più logica di resa: la classe esiste solo per costruire il renderer con le lingue
 * delle email di autenticazione ({@link Locales#EMAIL}) e per restare il punto di iniezione già
 * usato da {@link EmailService}. I testi restano nella sorgente unica
 * {@code shared/email-templates}, copiata nell'artefatto a build time (vedi {@code pom.xml}).
 */
@ApplicationScoped
public class EmailTemplates {

    private EmailTemplateRenderer renderer;

    @PostConstruct
    void load() {
        renderer = new EmailTemplateRenderer(Locales.EMAIL);
    }

    /**
     * Rende il messaggio {@code messageKey} ({@code verify} | {@code reset} | {@code invite}) nella
     * lingua indicata (ricondotta da {@link Locales#normalize}).
     *
     * @param values valori dinamici; {@code actionUrl} è il collegamento del messaggio
     */
    public EmailTemplateRenderer.Rendered render(String locale, String messageKey, Map<String, String> values) {
        return renderer.render(locale, messageKey, values);
    }
}
