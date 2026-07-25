package app.appgrove.core.newsletter;

/**
 * Ciclo di vita di un iscritto alla newsletter (UC 0039): {@code pending} finché il double opt-in
 * non è confermato (nessun invio marketing), {@code confirmed} dopo la conferma, {@code unsubscribed}
 * dopo la revoca. La transizione governa se l'indirizzo può ricevere comunicazioni di marketing.
 */
public enum SubscriberStatus {
    pending,
    confirmed,
    unsubscribed
}
