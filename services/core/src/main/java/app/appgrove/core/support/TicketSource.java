package app.appgrove.core.support;

/**
 * Provenienza di un ticket (UC 0075). Persistita come stringa (#05 7).
 *
 * <p>Serve a chi assiste per capire il contesto senza aprire il filo, e a chi legge i registri per
 * sapere quale innesco ha prodotto la richiesta. È un metadato operativo: non è un dato personale e
 * non compare nel manifesto dei trattamenti.
 */
public enum TicketSource {
    /** Modulo di apertura dentro il backoffice: la strada normale. */
    form,
    /** Evento di sistema: oggi solo l'esportazione GDPR fallita, che apre un ticket privacy. */
    event,
    /**
     * Messaggio arrivato alle caselle {@code privacy@}/{@code support@}. Predisposto: la ricezione
     * (SES → funzione Lambda) è rimandata perché dipende dall'uscita di SES dalla modalità di prova
     * e dalla verifica del dominio (UC 0018/0078). Il valore esiste da subito così che il giorno in
     * cui arriva non serva una migrazione.
     */
    email
}
