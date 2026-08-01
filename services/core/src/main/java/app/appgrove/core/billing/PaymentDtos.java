package app.appgrove.core.billing;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;

/**
 * Contratto dello <b>storico pagamenti e ricevute</b> del conto corrente (UC 0096). Nessun identificativo
 * di conto entra o esce da qui: il tenant viene dal token verificato e basta (invariante #1).
 */
public final class PaymentDtos {

    private PaymentDtos() {}

    /** Lo storico, dalla transazione più recente. */
    public record PaymentsView(List<PaymentView> payments) {}

    /**
     * Una riga dello storico.
     *
     * @param billedAt data dell'addebito — la prima colonna della tabella
     * @param appSlug chiave stabile dell'app, {@code null} per una transazione non riferita a un'app
     * @param appName nome dell'app; {@code null} se l'app non è più a catalogo
     * @param planName nome della fascia pagata; {@code null} se la fascia non esiste più
     * @param billingCycle etichetta del ciclo così come l'ha comunicata il fornitore ({@code monthly}…)
     * @param amount importo in unità minori (centesimi), come tutto il listino
     * @param status esito: pagato, fallito, contestato
     * @param receiptUrl ricevuta del fornitore; <b>assente</b> quando non è ancora disponibile — la riga
     *     esiste comunque, senza collegamento, invece di sparire o di promettere un documento che non c'è
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record PaymentView(
            Instant billedAt,
            String appSlug,
            String appName,
            String planName,
            String billingCycle,
            int amount,
            String currency,
            BillingTransactionStatus status,
            String receiptUrl) {}
}
