package app.appgrove.core.billing;

/**
 * Esito di una transazione di fatturazione, come lo racconta lo storico all'utente (UC 0096). Sono gli
 * esiti che il set di eventi sottoscritto (#09 D21) sa distinguere; nulla di più, per non promettere una
 * precisione che non abbiamo.
 *
 * <p>{@link #disputed} e {@link #refunded} sono i due modi in cui il denaro <b>torna indietro</b>, e la
 * riconciliazione (UC 0071) li tratta insieme come <i>storni</i>: l'importo esce dall'incassato e il netto
 * della riga va a zero. Restano però distinti, perché una contestazione del cliente e un rimborso deciso
 * da noi non sono la stessa cosa da guardare.
 */
public enum BillingTransactionStatus {
    /** Pagamento riuscito ({@code transaction.completed}). */
    paid,
    /** Pagamento fallito ({@code transaction.payment_failed}): dunning in corso, UC 0026. */
    failed,
    /** Contestato dal titolare della carta ({@code transaction.disputed}). */
    disputed,
    /** Rimborsato integralmente ({@code transaction.refunded}), UC 0071. */
    refunded
}
