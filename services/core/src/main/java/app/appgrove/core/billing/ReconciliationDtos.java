package app.appgrove.core.billing;

import java.time.Instant;
import java.util.List;

/**
 * Vista di riconciliazione fra ricavo lordo e denaro davvero accreditato (UC 0071). Superficie
 * <b>amministrativa</b> e cross-tenant: sono dati economici della piattaforma, non del singolo cliente.
 *
 * <p>Tutti gli importi sono in <b>unità minori</b> (centesimi), come tutto il resto del listino: la
 * formattazione è del lato che li mostra.
 */
public final class ReconciliationDtos {

    private ReconciliationDtos() {}

    /**
     * La vista completa.
     *
     * @param currency valuta prevalente della finestra osservata; {@code null} quando non c'è alcun dato
     * @param totals totali della finestra
     * @param periods righe per mese, dal più recente
     * @param payouts accrediti, dal più recente
     * @param feeAlertPercent soglia oltre la quale il peso delle commissioni è da guardare
     * @param payoutOverdue vero se esiste netto non accreditato più vecchio della soglia di attesa
     * @param payoutMaxAgeDays soglia di attesa di un accredito, in giorni
     */
    public record ReconciliationView(
            String currency,
            ReconciliationTotals totals,
            List<ReconciliationPeriod> periods,
            List<PayoutView> payouts,
            double feeAlertPercent,
            boolean payoutOverdue,
            long payoutMaxAgeDays) {}

    /**
     * Totali della finestra osservata.
     *
     * @param gross lordo incassato (solo transazioni riuscite)
     * @param fee commissioni trattenute dal fornitore
     * @param net netto residuo
     * @param reversed importo tornato indietro (rimborsi e contestazioni)
     * @param settled netto già accreditato sul conto
     * @param unsettled netto non ancora accreditato
     * @param transactions numero di transazioni riuscite
     * @param estimatedFeeTransactions quante di quelle hanno la commissione <b>stimata</b> e non dichiarata
     * @param oldestUnsettledAt addebito più vecchio fra quelli non ancora accreditati; {@code null} se
     *     è tutto accreditato
     */
    public record ReconciliationTotals(
            long gross,
            long fee,
            long net,
            long reversed,
            long settled,
            long unsettled,
            long transactions,
            long estimatedFeeTransactions,
            Instant oldestUnsettledAt) {}

    /**
     * Una riga per mese di <b>addebito</b> (mai di accredito: il contrario farebbe rimbalzare i ricavi di un
     * mese sul successivo a ogni cambio di calendario del fornitore).
     *
     * @param period mese nella forma {@code AAAA-MM}
     * @param feePercent peso delle commissioni sul lordo, in percentuale
     * @param feeOverThreshold vero quando quel peso supera la soglia di attenzione
     */
    public record ReconciliationPeriod(
            String period,
            long gross,
            long fee,
            long net,
            long reversed,
            long transactions,
            double feePercent,
            boolean feeOverThreshold) {}

    /**
     * Un accredito e la sua quadratura.
     *
     * @param amount importo accreditato dal fornitore
     * @param linesNet somma dei netti delle righe di dettaglio
     * @param difference scostamento ({@code amount - linesNet}); {@code null} quando non è calcolabile
     * @param status esito della quadratura: {@code matched}, {@code mismatch} o {@code mixed_currency}
     * @param coveredFrom addebito più vecchio fra quelli collegati che conosciamo; {@code null} se nessuno
     * @param coveredTo addebito più recente fra quelli collegati che conosciamo; {@code null} se nessuno
     */
    public record PayoutView(
            String paddlePayoutId,
            Instant paidAt,
            String currency,
            long amount,
            long linesNet,
            Long difference,
            String status,
            long lines,
            Instant coveredFrom,
            Instant coveredTo) {}
}
