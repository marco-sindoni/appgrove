package app.appgrove.core.billing;

/**
 * Esito di una transazione di fatturazione, come lo racconta lo storico all'utente (UC 0096). Sono i tre
 * esiti che il set di eventi sottoscritto (#09 D21) sa distinguere; nulla di più, per non promettere una
 * precisione che non abbiamo.
 */
public enum BillingTransactionStatus {
    /** Pagamento riuscito ({@code transaction.completed}). */
    paid,
    /** Pagamento fallito ({@code transaction.payment_failed}): dunning in corso, UC 0026. */
    failed,
    /** Contestato dal titolare della carta ({@code transaction.disputed}). */
    disputed
}
