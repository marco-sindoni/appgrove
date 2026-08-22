package app.appgrove.core.billing.seats;

import java.util.List;

/**
 * Il calcolo del dovuto per i posti (UC 0102): funzioni <b>pure</b>, senza stato e senza banca dati.
 *
 * <p><b>Il calcolo è a scaglioni progressivi</b>: ogni posto paga la tariffa della fascia in cui cade
 * <i>quel</i> posto, non la tariffa dell'ultima fascia raggiunta. Con 12 posti si paga
 * {@code 7 × 2,99 + 2 × 1,99 = 24,91 €}, non {@code 9 ×} una tariffa unica.
 *
 * <p><b>Perché progressivo</b> (decisione dello sviluppatore, epica E22.2). Il modello a tariffa unica di
 * fascia — quello scartato — faceva <b>scendere il totale</b> ai confini: undici posti costavano meno di
 * dieci. Un prezzo che cala quando cresci è indifendibile davanti a un cliente anche quando è a suo
 * favore, perché sembra un errore di conteggio. Con la progressività il dovuto è <b>monotono crescente</b>
 * e a scendere è il {@link #nextSeatCents(int, SeatPricingVersion) costo del posto successivo}: la stessa
 * convenienza, detta in un modo che si capisce.
 *
 * <p><b>Nessuna fascia è scritta qui.</b> Le fasce stanno in banca dati; il file di risorse serve solo al
 * primo popolamento. Cablarle nel codice «per i collaudi» le farebbe divergere il giorno del primo cambio.
 *
 * <p><b>Nessun caso speciale per la franchigia.</b> I primi tre posti sono gratuiti perché la prima fascia
 * ha tariffa zero, non perché il codice li conti a parte.
 *
 * <p>Tutto in <b>centesimi interi</b>: il denaro non si calcola in virgola mobile, e il dovuto non si
 * arrotonda per riga.
 */
public final class SeatPricing {

    private SeatPricing() {}

    /**
     * Il dovuto mensile, in centesimi, per {@code seats} posti occupati.
     *
     * @param seats numero di posti occupati; {@code 0} è ammesso e vale zero (nella realtà non accade, c'è
     *     sempre l'owner, ma una funzione totale non ha buchi da spiegare)
     * @throws IncoherentSeatPricingException se il listino non è utilizzabile
     */
    public static long dueCents(int seats, SeatPricingVersion version) {
        if (seats < 0) {
            throw new IllegalArgumentException("numero di posti negativo: " + seats);
        }
        List<SeatPricingBand> bands = requireCoherent(version);
        long total = 0;
        for (SeatPricingBand band : bands) {
            int upper = band.isOpenEnded() ? seats : Math.min(seats, band.getToSeat());
            int inBand = upper - band.getFromSeat() + 1;
            if (inBand > 0) {
                total += (long) inBand * band.getUnitPriceCents();
            }
        }
        return total;
    }

    /**
     * Quanto costerebbe il posto <b>successivo</b> a quelli già occupati, in centesimi: la tariffa della
     * fascia in cui cade il posto {@code seats + 1}.
     *
     * <p>È il secondo numero che le interfacce mostrano accanto al totale (UC 0103, UC 0106), e appartiene
     * al calcolo e non alla presentazione. Ai tre confini di fascia scende — è lì che si vede che la
     * persona in più costa meno della precedente.
     */
    public static int nextSeatCents(int seats, SeatPricingVersion version) {
        if (seats < 0) {
            throw new IllegalArgumentException("numero di posti negativo: " + seats);
        }
        return bandFor(seats + 1, version).getUnitPriceCents();
    }

    /**
     * La fascia in cui cade un singolo posto.
     *
     * @param seat numero d'ordine del posto, a partire da 1
     * @throws IncoherentSeatPricingException se il listino non è utilizzabile (su un listino coerente ogni
     *     posto ≥ 1 cade in una fascia, per costruzione)
     */
    public static SeatPricingBand bandFor(int seat, SeatPricingVersion version) {
        if (seat < 1) {
            throw new IllegalArgumentException("il primo posto è il numero 1, richiesto: " + seat);
        }
        for (SeatPricingBand band : requireCoherent(version)) {
            if (band.contains(seat)) {
                return band;
            }
        }
        throw new IncoherentSeatPricingException("nessuna fascia copre il posto " + seat);
    }

    /**
     * Quanti posti sono <b>compresi nella franchigia</b>: i posti coperti dalle fasce iniziali a tariffa
     * zero. Col listino iniziale sono tre, owner compreso.
     *
     * <p>Si <b>deriva</b> dal listino e non è una costante: la franchigia è una riga di listino, non una
     * regola di codice (UC 0102), e il giorno in cui cambia deve cambiare in un posto solo. Serve alle
     * interfacce, che devono poter dire «tre posti sono compresi» senza saperlo da sé.
     *
     * <p>Una fascia a tariffa zero <b>aperta</b> significherebbe posti illimitati gratis: il caso non è
     * escluso dalla coerenza del listino, e qui si legge come {@link Integer#MAX_VALUE} invece di far
     * fallire un conto che sarebbe comunque corretto (il dovuto resterebbe zero).
     */
    public static int freeSeats(SeatPricingVersion version) {
        int free = 0;
        for (SeatPricingBand band : requireCoherent(version)) {
            if (band.getUnitPriceCents() != 0) {
                return free;
            }
            if (band.isOpenEnded()) {
                return Integer.MAX_VALUE;
            }
            free = band.getToSeat();
        }
        return free;
    }

    /**
     * Verifica che il listino sia utilizzabile e restituisce le sue fasce ordinate: fasce presenti, prima
     * fascia dal posto 1, contigue senza buchi né sovrapposizioni, <b>solo l'ultima</b> aperta verso
     * l'alto, tariffe non negative.
     *
     * <p>La contiguità non è esprimibile come vincolo di riga in banca dati (la migrazione lo dichiara):
     * il presidio è qui, ed è attraversato sia dal caricamento iniziale sia da ogni calcolo. Costa un
     * passaggio su cinque elementi e chiude la classe di guasto «il conto torna ma il listino ha un buco».
     */
    public static List<SeatPricingBand> requireCoherent(SeatPricingVersion version) {
        List<SeatPricingBand> bands = version.getBands();
        if (bands.isEmpty()) {
            throw new IncoherentSeatPricingException("listino senza fasce");
        }
        SeatPricingBand first = bands.get(0);
        if (first.getFromSeat() != 1) {
            throw new IncoherentSeatPricingException(
                    "la prima fascia deve partire dal posto 1, parte da " + first.getFromSeat());
        }
        for (int i = 0; i < bands.size(); i++) {
            SeatPricingBand band = bands.get(i);
            boolean last = i == bands.size() - 1;
            if (band.getUnitPriceCents() < 0) {
                throw new IncoherentSeatPricingException(
                        "tariffa negativa nella fascia che parte dal posto " + band.getFromSeat());
            }
            if (last) {
                if (!band.isOpenEnded()) {
                    throw new IncoherentSeatPricingException(
                            "l'ultima fascia deve essere aperta (posto finale vuoto), finisce al posto "
                                    + band.getToSeat());
                }
                continue;
            }
            if (band.isOpenEnded()) {
                throw new IncoherentSeatPricingException(
                        "solo l'ultima fascia può essere aperta: quella dal posto " + band.getFromSeat()
                                + " non ha posto finale ma non è l'ultima");
            }
            int expected = band.getToSeat() + 1;
            int actual = bands.get(i + 1).getFromSeat();
            if (actual != expected) {
                throw new IncoherentSeatPricingException(
                        "fasce non contigue: dopo il posto " + band.getToSeat() + " la fascia seguente"
                                + " dovrebbe partire dal posto " + expected + ", parte dal posto " + actual);
            }
        }
        return bands;
    }
}
