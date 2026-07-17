package app.appgrove.auth.cognito;

import app.appgrove.auth.MailSender;
import io.quarkus.arc.lookup.LookupIfProperty;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Trasporto email cloud via SES (inviti; verifica/reset in cloud le manda Cognito). Testo semplice:
 * template EN/IT e identità di dominio SES arrivano con UC 0018 — finché l'identità non è
 * verificata l'invio fallisce e l'errore emerge nei log (fail visibile, non silenzioso).
 */
@LookupIfProperty(name = "auth.provider", stringValue = "cognito")
@ApplicationScoped
public class SesMailSender implements MailSender {

    @ConfigProperty(name = "auth.mail-from", defaultValue = "noreply@appgrove.app")
    String from;

    @Inject
    Instance<software.amazon.awssdk.services.sesv2.SesV2Client> ses;

    @Override
    public void send(String to, String subject, String textBody) {
        ses.get().sendEmail(b -> b
                .fromEmailAddress(from)
                .destination(d -> d.toAddresses(to))
                .content(c -> c.simple(m -> m
                        .subject(s -> s.data(subject))
                        .body(body -> body.text(t -> t.data(textBody))))));
    }
}
