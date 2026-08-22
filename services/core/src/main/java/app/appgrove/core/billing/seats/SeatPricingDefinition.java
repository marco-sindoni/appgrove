package app.appgrove.core.billing.seats;

import java.time.Instant;
import java.util.List;

/**
 * Il listino dei posti come <b>codice</b>: la forma del file {@code resources/pricing/seats.yaml}, che
 * fornisce il <b>valore iniziale</b> della prima versione (UC 0102 §7).
 *
 * <p>Da lì in poi la verità è la banca dati: il file non viene riletto per aggiornare nulla, e le versioni
 * successive nascono dalla console di piattaforma (UC 0105). È la differenza con il listino delle
 * applicazioni, che invece si <b>sincronizza</b> dai file a ogni avvio.
 *
 * @param effectiveFrom decorrenza della prima versione
 * @param currency valuta (oggi sempre {@code EUR})
 * @param note perché questa versione esiste, per chi la rilegge fra un anno
 * @param bands le fasce, dalla prima all'ultima
 */
public record SeatPricingDefinition(
        Instant effectiveFrom, String currency, String note, List<BandDef> bands) {

    /**
     * Una fascia del file.
     *
     * @param toSeat ultimo posto della fascia; assente (nullo) per l'ultima fascia, che è aperta
     */
    public record BandDef(int fromSeat, Integer toSeat, int unitPriceCents) {}

    /**
     * Il listino del file come oggetto di dominio, <b>in memoria</b>, senza banca dati.
     *
     * <p>È l'unico punto in cui il file diventa un listino, e serve a due usi che devono restare d'accordo:
     * il controllo di coerenza prima della semina e il collaudo tabellare del calcolo, che così prova le
     * tariffe <b>del file</b> e non tariffe ricopiate nel codice del collaudo — dove sarebbero divergute
     * al primo cambio.
     */
    public SeatPricingVersion asVersion() {
        SeatPricingVersion version = new SeatPricingVersion(effectiveFrom, currency, note);
        for (BandDef band : bands) {
            version.addBand(band.fromSeat(), band.toSeat(), band.unitPriceCents());
        }
        return version;
    }
}
