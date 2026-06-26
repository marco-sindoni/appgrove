package app.appgrove.authlocal;

import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Email transazionali → Mailpit (dev). Email <b>funzionali</b>: portano il link/token; il copy
 * localizzato EN/IT definitivo è UC 0018. In test usa MockMailbox (mailer mock).
 */
@ApplicationScoped
public class EmailService {

    @Inject
    Mailer mailer;

    @ConfigProperty(name = "auth.local.app-base-url")
    String baseUrl;

    public void sendVerify(String email, String token) {
        send(email, "Verifica la tua email appgrove",
                "Conferma il tuo indirizzo: " + link("/verify", token));
    }

    public void sendReset(String email, String token) {
        send(email, "Reimposta la password appgrove",
                "Reimposta la password: " + link("/reset", token));
    }

    public void sendInvite(String email, String token, String role) {
        send(email, "Sei stato invitato su appgrove",
                "Sei stato invitato come " + role + ". Accetta l'invito: " + link("/accept", token));
    }

    private String link(String path, String token) {
        return baseUrl + path + "?token=" + token;
    }

    private void send(String to, String subject, String body) {
        mailer.send(Mail.withText(to, subject, body));
    }
}
