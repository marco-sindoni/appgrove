package app.appgrove.auth.local;

import app.appgrove.auth.MailSender;
import io.quarkus.arc.lookup.LookupIfProperty;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/** Trasporto email locale: quarkus-mailer → Mailpit (dev) / MockMailbox (test). */
@LookupIfProperty(name = "auth.provider", stringValue = "local", lookupIfMissing = true)
@ApplicationScoped
public class MailerMailSender implements MailSender {

    @Inject
    Mailer mailer;

    @Override
    public void send(String to, String subject, String textBody) {
        mailer.send(Mail.withText(to, subject, textBody));
    }
}
