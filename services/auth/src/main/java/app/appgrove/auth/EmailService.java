package app.appgrove.auth;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Email funzionali del BFF (portano il link/token); il copy localizzato EN/IT definitivo è UC 0018.
 * Il trasporto è la porta {@link MailSender}: Mailpit in locale, SES in cloud. Le email di
 * verifica/reset in cloud le manda Cognito (qui passa solo l'invito).
 */
@ApplicationScoped
public class EmailService {

    // Instance<>: i trasporti sono beans condizionali (@LookupIfProperty su auth.provider).
    @Inject
    Instance<MailSender> sender;

    @ConfigProperty(name = "auth.app-base-url")
    String baseUrl;

    public void sendVerify(String email, String token) {
        sender.get().send(email, "Verifica la tua email appgrove",
                "Conferma il tuo indirizzo: " + link("/verify", token));
    }

    public void sendReset(String email, String token) {
        sender.get().send(email, "Reimposta la password appgrove",
                "Reimposta la password: " + link("/reset", token));
    }

    public void sendInvite(String email, String token, String role) {
        sender.get().send(email, "Sei stato invitato su appgrove",
                "Sei stato invitato come " + role + ". Accetta l'invito: " + link("/accept", token));
    }

    private String link(String path, String token) {
        return baseUrl + path + "?token=" + token;
    }
}
