package app.appgrove.core.billing;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Commissione trattenuta dal fornitore di pagamento su una transazione, e netto che ne resta (UC 0071).
 *
 * <p><b>Chi decide il numero.</b> Il fornitore è venditore ufficiale verso il cliente: la commissione vera
 * la conosce solo lui. Quando la comunica nel payload dell'evento, quella <b>vince sempre</b>. Quando non la
 * comunica — ed è il caso di oggi, senza account reale (bloccato da #14) — il backend la <b>stima</b> con la
 * stessa formula usata dal co-pilota prezzi ({@code tools/pricing-change/lib/fee.mjs}): una percentuale
 * dell'importo più una quota fissa per transazione. La stima è marcata come tale riga per riga
 * ({@link Source}) e la vista lo mostra: una stima scambiata per un dato dichiarato sarebbe peggio di un
 * dato mancante, perché nessuno saprebbe di doverne dubitare.
 *
 * <p>La quota fissa è la ragione per cui la commissione <b>non</b> è una percentuale costante: su una
 * transazione da 5 euro pesa il 10%, su una da 50 l'1%. È esattamente il segnale che questo use case esiste
 * per rendere visibile.
 */
@ApplicationScoped
public class PaymentFees {

    /** Provenienza del dato di commissione: dichiarato dal fornitore o stimato da noi. */
    public enum Source {
        provider,
        estimated
    }

    /**
     * Commissione e netto di una transazione.
     *
     * @param feeAmount commissione in unità minori (mai negativa, mai superiore all'importo)
     * @param netAmount netto residuo in unità minori
     * @param source da dove viene il numero
     */
    public record Fee(int feeAmount, int netAmount, Source source) {}

    /** Percentuale trattenuta dal fornitore (5% = il valore di listino Paddle, #09 K46). */
    @ConfigProperty(name = "appgrove.payments.fee-percent", defaultValue = "5.0")
    double feePercent;

    /** Quota fissa per transazione in unità minori (50 = $0,50 ≈ €0,50, tasso assunto 1,0 come #09 K46). */
    @ConfigProperty(name = "appgrove.payments.fee-fixed-minor", defaultValue = "50")
    int feeFixedMinor;

    /**
     * Commissione e netto di una transazione riuscita.
     *
     * @param amount importo lordo in unità minori
     * @param declaredFee commissione dichiarata dal fornitore, o {@code null} se il payload non la porta
     */
    public Fee of(int amount, Integer declaredFee) {
        if (declaredFee != null) {
            int fee = clamp(declaredFee, amount);
            return new Fee(fee, amount - fee, Source.provider);
        }
        int fee = clamp(estimate(amount), amount);
        return new Fee(fee, amount - fee, Source.estimated);
    }

    /** Stima deterministica: parte percentuale arrotondata più quota fissa. */
    int estimate(int amount) {
        if (amount <= 0) {
            return 0;
        }
        return (int) Math.round(amount * feePercent / 100.0) + feeFixedMinor;
    }

    /**
     * La commissione non può essere negativa né superare l'importo: un netto negativo su una singola
     * transazione sarebbe un dato senza senso che si propagherebbe in tutti gli aggregati a valle.
     */
    private static int clamp(int fee, int amount) {
        return Math.max(0, Math.min(fee, Math.max(0, amount)));
    }
}
