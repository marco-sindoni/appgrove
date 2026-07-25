package app.appgrove.core.newsletter;

import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * Invio dell'email di conferma del double opt-in (UC 0039). Usa il {@link Mailer} quarkus-mailer
 * già cablato nel core per il ticketing (Mailpit in dev, MockMailbox in test; il relay SES cloud è
 * infrastruttura differita come per il supporto). <b>Fail-soft</b> come {@code TicketNotifier}:
 * l'invio non deve mai far fallire l'iscrizione — errori loggati e inghiottiti.
 */
@ApplicationScoped
public class NewsletterMailer {

    private static final Logger LOG = Logger.getLogger(NewsletterMailer.class);

    @Inject
    Mailer mailer;

    @Inject
    NewsletterEmailRenderer renderer;

    /** Base pubblica raggiungibile dal browser su cui vive l'endpoint di conferma del core. */
    @ConfigProperty(name = "appgrove.newsletter.base-url")
    String baseUrl;

    /** Manda a {@code email} il link di conferma per {@code subscriberId} con token grezzo. */
    public void sendConfirmation(String email, String locale, UUID subscriberId, String rawToken) {
        String actionUrl = baseUrl + "/api/platform/v1/newsletter/confirm?token="
                + URLEncoder.encode(rawToken, StandardCharsets.UTF_8);
        try {
            NewsletterEmailRenderer.Rendered r = renderer.renderConfirm(locale, actionUrl);
            mailer.send(Mail.withText(email, r.subject(), r.text()).setHtml(r.html()));
            LOG.infof("newsletter.confirm.sent subscriber_id=%s", subscriberId);
        } catch (RuntimeException e) {
            LOG.warnf(e, "newsletter.confirm invio email fallito subscriber_id=%s (best-effort, si prosegue)",
                    subscriberId);
        }
    }
}
