package app.appgrove.auth.cognito;

import app.appgrove.auth.MailSender;
import io.quarkus.arc.lookup.LookupIfProperty;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Trasporto email cloud via SES. In cloud passa di qui il <b>solo invito</b>: verifica e
 * reimpostazione password le compone il Custom Message Lambda e le spedisce Cognito (UC 0018).
 *
 * <p>È anche l'unica email che parte dalla rete privata, dove non c'è uscita a internet: richiede
 * l'accesso di rete dedicato verso SES (endpoint {@code email}, {@code endpoints.tf}). Se manca,
 * l'invio fallisce con un errore di rete visibile nei log — non in silenzio.
 */
@LookupIfProperty(name = "auth.provider", stringValue = "cognito")
@ApplicationScoped
public class SesMailSender implements MailSender {

    @ConfigProperty(name = "auth.mail-from", defaultValue = "noreply@appgrove.app")
    String from;

    @Inject
    Instance<software.amazon.awssdk.services.sesv2.SesV2Client> ses;

    @Override
    public void send(String to, String subject, String textBody, String htmlBody) {
        ses.get().sendEmail(b -> b
                .fromEmailAddress(from)
                .destination(d -> d.toAddresses(to))
                .content(c -> c.simple(m -> m
                        .subject(s -> s.data(subject))
                        .body(body -> body
                                .text(t -> t.data(textBody))
                                .html(h -> h.data(htmlBody))))));
    }
}
