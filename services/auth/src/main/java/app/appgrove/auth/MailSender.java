package app.appgrove.auth;

/**
 * Trasporto delle email funzionali del BFF ({@link EmailService} costruisce il messaggio): in locale
 * quarkus-mailer → Mailpit, in cloud SES. Selezione con {@code auth.provider} come il provider.
 *
 * <p>Doppia versione (UC 0018): quella grafica è ciò che l'utente vede normalmente, quella testuale
 * è il ripiego per i lettori che non mostrano la grafica — e resta il modo in cui i test estraggono
 * il collegamento senza dipendere dal markup.
 */
public interface MailSender {

    void send(String to, String subject, String textBody, String htmlBody);
}
