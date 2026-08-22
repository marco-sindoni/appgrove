package app.appgrove.core.billing.seats;

/**
 * Stato di una riduzione dei posti (UC 0104). Tre valori e nessun altro: una riduzione è in attesa,
 * oppure è stata eseguita, oppure è stata annullata.
 *
 * <p><b>Non esiste uno stato «scaduta»</b>, ed è deliberato: un'attesa la cui data di esecuzione è passata
 * è ancora {@link #pending}, ed è esattamente la condizione che va <b>misurata e allarmata</b>
 * (UC 0104 §5). Uno stato dedicato la renderebbe un fatto normale invece di un guasto — e un cliente che
 * paga posti che credeva chiusi non è un fatto normale.
 */
public enum SeatDowngradeStatus {
    pending,
    executed,
    cancelled
}
