package app.appgrove.auth;

/**
 * Trasporto delle email funzionali del BFF ({@link EmailService} costruisce il testo): in locale
 * quarkus-mailer → Mailpit, in cloud SES. Selezione con {@code auth.provider} come il provider.
 */
public interface MailSender {

    void send(String to, String subject, String textBody);
}
