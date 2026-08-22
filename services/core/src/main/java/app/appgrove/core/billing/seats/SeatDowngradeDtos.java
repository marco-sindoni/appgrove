package app.appgrove.core.billing.seats;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotEmpty;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Contratto di rete della <b>riduzione dei posti in attesa</b> (UC 0104): quello che l'owner vede prima di
 * confermare, e quello che vede finché l'attesa dura.
 *
 * <p><b>Ogni numero è calcolato dal servizio</b>, come per il riquadro dei posti: la data di esecuzione, i
 * posti che resteranno, il dovuto prima e dopo, e la composizione degli scaglioni. L'interfaccia non fa
 * aritmetica — è la sola forma in cui cinque traduzioni e un servizio dicono lo stesso importo.
 */
public final class SeatDowngradeDtos {

    private SeatDowngradeDtos() {}

    /** Corpo della richiesta: le persone da cessare, come <b>elenco</b>. */
    public record RequestReduction(
            @NotEmpty(message = "indica almeno una persona") List<UUID> userIds) {}

    /**
     * Una persona indicata per la cessazione.
     *
     * <p>Porta indirizzo e nome perché il riquadro di avviso deve poter dire <i>chi</i> cesserà senza una
     * seconda chiamata: sono le stesse informazioni che l'owner già vede nella riga dell'elenco delle
     * persone, quindi non si rivela nulla di nuovo.
     */
    public record ReductionPersonView(UUID userId, String email, String displayName) {}

    /**
     * Una riga della composizione degli scaglioni: «6 posti a 2,99 € = 17,94 €».
     *
     * @param toSeat limite superiore della fascia; assente per l'ultima, aperta verso l'alto
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ReductionBandView(
            int fromSeat, Integer toSeat, int unitPriceCents, int seats, long subtotalCents) {}

    /**
     * La riduzione in attesa, così come la mostra il riquadro di avviso.
     *
     * @param id identificativo della riduzione
     * @param executeAt quando si eseguirà: la fine del periodo già pagato
     * @param requestedAt quando è stata chiesta
     * @param overdue la data è passata e l'esecuzione non è ancora avvenuta. Va detto a schermo con
     *     onestà («la riduzione è in corso di esecuzione»): un cliente che vede una data passata e nessun
     *     cambiamento pensa che il sistema si sia dimenticato di lui, e ha ragione a pensarlo
     * @param people le persone indicate
     * @param seatsAfter posti che resteranno occupati dopo l'esecuzione
     * @param dueCentsNow dovuto mensile <b>oggi</b>
     * @param dueCentsAfter dovuto mensile <b>dopo</b> l'esecuzione
     * @param currency valuta del listino vigente
     * @param bandsAfter composizione degli scaglioni che si applicherà dopo l'esecuzione
     */
    public record ReductionView(
            UUID id,
            Instant executeAt,
            Instant requestedAt,
            boolean overdue,
            List<ReductionPersonView> people,
            int seatsAfter,
            long dueCentsNow,
            long dueCentsAfter,
            String currency,
            List<ReductionBandView> bandsAfter) {}

    /**
     * L'effetto <b>prima della conferma</b> (UC 0104 §4.2): «cesseranno il 14 settembre; dal 15 pagherai
     * 17,94 € invece di 24,91 €», col numero di posti risultante e la composizione degli scaglioni.
     *
     * <p>È una lettura a sé e non un pezzo del riquadro, perché dipende da <b>chi</b> l'owner ha appena
     * selezionato: è la sola informazione della sezione che cambia a ogni casella spuntata.
     *
     * @param executeAt la data che la riduzione avrebbe
     * @param people le persone che si sta per indicare
     * @param seatsNow posti occupati oggi
     * @param seatsAfter posti che resterebbero
     * @param dueCentsNow dovuto mensile oggi
     * @param dueCentsAfter dovuto mensile dopo
     * @param currency valuta del listino vigente
     * @param bandsNow composizione degli scaglioni oggi
     * @param bandsAfter composizione degli scaglioni dopo
     */
    public record ReductionPreview(
            Instant executeAt,
            List<ReductionPersonView> people,
            int seatsNow,
            int seatsAfter,
            long dueCentsNow,
            long dueCentsAfter,
            String currency,
            List<ReductionBandView> bandsNow,
            List<ReductionBandView> bandsAfter) {}

    /** Converte la composizione calcolata dal listino nella forma di rete. */
    static List<ReductionBandView> bands(List<SeatPricing.SeatBandUsage> usage) {
        return usage.stream()
                .map(u -> new ReductionBandView(
                        u.fromSeat(), u.toSeat(), u.unitPriceCents(), u.seats(), u.subtotalCents()))
                .toList();
    }
}
