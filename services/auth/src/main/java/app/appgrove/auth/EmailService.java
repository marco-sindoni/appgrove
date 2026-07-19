package app.appgrove.auth;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Email di autenticazione del BFF: sceglie la lingua, rende il template (UC 0018) e costruisce il
 * collegamento. Il trasporto è la porta {@link MailSender}: Mailpit in locale, SES in cloud.
 *
 * <p>In cloud passa di qui il <b>solo invito</b>: verifica e reimpostazione password le compone il
 * Custom Message Lambda dentro Cognito. I testi però sono gli stessi — vengono dalla medesima
 * sorgente {@code shared/email-templates}.
 */
@ApplicationScoped
public class EmailService {

    // Instance<>: i trasporti sono beans condizionali (@LookupIfProperty su auth.provider).
    @Inject
    Instance<MailSender> sender;

    @Inject
    EmailTemplates templates;

    @ConfigProperty(name = "auth.app-base-url")
    String baseUrl;

    public void sendVerify(String email, String locale, String token) {
        send(email, locale, "verify", Map.of("actionUrl", tokenLink("/verify", token)));
    }

    public void sendReset(String email, String locale, String token) {
        send(email, locale, "reset", Map.of("actionUrl", tokenLink("/reset", token)));
    }

    public void sendInvite(String email, String locale, String token, String role) {
        send(email, locale, "invite", Map.of("actionUrl", tokenLink("/accept", token), "role", role));
    }

    private void send(String to, String locale, String messageKey, Map<String, String> values) {
        EmailTemplates.Rendered rendered = templates.render(locale, messageKey, values);
        sender.get().send(to, rendered.subject(), rendered.text(), rendered.html());
    }

    /**
     * Collegamento a token unico: è la forma del provider locale, che conia i propri token e li
     * conosce già quando compone l'email. In cloud verifica e reimpostazione usano invece la forma a
     * due parametri ({@code ?email=…&code={####}}), perché quando il Custom Message Lambda compone
     * il messaggio <b>il codice non esiste ancora</b>. Entrambe le forme sono accettate dagli
     * endpoint (vedi {@link AuthResource}).
     */
    private String tokenLink(String path, String token) {
        return baseUrl + path + "?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
    }
}
