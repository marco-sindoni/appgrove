package app.appgrove.core.billing.seats;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Il collaudo del calcolo del dovuto (UC 0102 §9): <b>tabellare</b>, con tutti i casi della storia §4.
 * È il collaudo più importante della sotto-epica dei posti a pagamento — quello che nessuno perdonerebbe
 * sbagliato — e per questo è un collaudo di <b>unità</b>: nessuna banca dati, nessun avvio, esito immediato.
 *
 * <p><b>Le tariffe non sono scritte qui.</b> Le fasce arrivano dal file {@code pricing/seats.yaml}, cioè dal
 * listino vero; scritti nel collaudo sono soltanto i <b>risultati attesi</b>, che vengono dalla tabella
 * della storia. È la differenza fra provare il listino del prodotto e provare una sua copia: una copia
 * divergerebbe al primo cambio di tariffa, restando verde.
 */
class SeatPricingTest {

    /** Il listino iniziale letto dal file, come oggetto di dominio in memoria. */
    private static SeatPricingVersion listinoDelFile() {
        ObjectMapper yaml = new ObjectMapper(new YAMLFactory()).registerModule(new JavaTimeModule());
        try (InputStream in = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream(SeatPricingLoader.RESOURCE)) {
            assertNotNull(in, "risorsa mancante nel classpath: " + SeatPricingLoader.RESOURCE);
            return yaml.readValue(in, SeatPricingDefinition.class).asVersion();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Un listino costruito a mano, per i soli casi di <b>incoerenza</b>: qui le fasce sono l'oggetto del test. */
    private static SeatPricingVersion listino() {
        return new SeatPricingVersion(Instant.EPOCH, "EUR", "listino di collaudo");
    }

    // ── La tabella della storia §4 ────────────────────────────────────────────
    // posti, dovuto in centesimi, costo del posto successivo in centesimi.
    // I dovuti sono esattamente i valori della tabella dello use case; il costo del posto successivo è la
    // tariffa della fascia in cui cade il posto (posti + 1).
    @ParameterizedTest(name = "{0} posti → dovuto {1} centesimi, posto successivo {2} centesimi")
    @CsvSource({
        "  0,      0,   0", // caso definito anche se irraggiungibile: c'è sempre l'owner
        "  1,      0,   0", // franchigia: la seconda persona è gratis
        "  2,      0,   0",
        "  3,      0, 299", // ultimo posto gratuito: il prossimo è il primo a pagamento
        "  4,    299, 299",
        "  5,    598, 299",
        "  8,   1495, 299",
        " 10,   2093, 199", // confine: il posto successivo cambia fascia e costa MENO
        " 11,   2292, 199",
        " 12,   2491, 199",
        " 50,  10053,  99", // confine
        " 51,  10152,  99",
        " 52,  10251,  99", // l'esempio svolto della storia: 20,93 + 79,60 + 1,98 = 102,51 €
        " 55,  10548,  99",
        "100,  15003,  49", // confine
        "101,  15052,  49",
        "120,  15983,  49",
    })
    void tabellaDelDovutoEDelPostoSuccessivo(int posti, long dovutoCentesimi, int postoSuccessivoCentesimi) {
        SeatPricingVersion listino = listinoDelFile();

        assertEquals(dovutoCentesimi, SeatPricing.dueCents(posti, listino), "dovuto per " + posti + " posti");
        assertEquals(
                postoSuccessivoCentesimi,
                SeatPricing.nextSeatCents(posti, listino),
                "costo del posto successivo con " + posti + " posti");
    }

    /**
     * <b>Il dovuto è monotono crescente.</b> È la proprietà che il modello a tariffa unica di fascia — quello
     * scartato — non aveva: là undici posti costavano meno di dieci. Con gli scaglioni progressivi il totale
     * non cala mai, e questo collaudo è ciò che distingue il modello adottato da quello scartato: se qualcuno
     * riscrivesse il calcolo «a tariffa di fascia», diventerebbe rosso qui e non in produzione.
     */
    @Test
    void ilDovutoNonScendeMaiAlCrescereDeiPosti() {
        SeatPricingVersion listino = listinoDelFile();

        long precedente = -1;
        for (int posti = 0; posti <= 150; posti++) {
            long dovuto = SeatPricing.dueCents(posti, listino);
            assertTrue(
                    dovuto >= precedente,
                    "il dovuto per " + posti + " posti (" + dovuto + ") è inferiore a quello per "
                            + (posti - 1) + " (" + precedente + ")");
            precedente = dovuto;
        }
    }

    /**
     * A scendere è il <b>costo del posto successivo</b>, e scende esattamente ai tre confini di fascia. È
     * l'altra metà della frase «il totale cresce sempre, a scendere è il costo del prossimo posto»: senza
     * questo collaudo, un listino con quattro fasce identiche passerebbe quello di monotonia.
     */
    @Test
    void ilCostoDelPostoSuccessivoScendeAiTreConfini() {
        SeatPricingVersion listino = listinoDelFile();

        assertEquals(299, SeatPricing.nextSeatCents(9, listino));
        assertEquals(199, SeatPricing.nextSeatCents(10, listino));

        assertEquals(199, SeatPricing.nextSeatCents(49, listino));
        assertEquals(99, SeatPricing.nextSeatCents(50, listino));

        assertEquals(99, SeatPricing.nextSeatCents(99, listino));
        assertEquals(49, SeatPricing.nextSeatCents(100, listino));
    }

    /**
     * La franchigia è la <b>prima fascia a tariffa zero</b>, non un caso speciale del codice: si vede
     * interrogando il listino, non leggendo il calcolo.
     */
    @Test
    void laFranchigiaEUnaFasciaATariffaZeroDaUnoATre() {
        SeatPricingVersion listino = listinoDelFile();

        assertEquals(0, SeatPricing.bandFor(1, listino).getUnitPriceCents());
        assertEquals(0, SeatPricing.bandFor(3, listino).getUnitPriceCents());
        assertEquals(1, SeatPricing.bandFor(3, listino).getFromSeat());
        assertEquals(3, SeatPricing.bandFor(3, listino).getToSeat().intValue());
        assertEquals(299, SeatPricing.bandFor(4, listino).getUnitPriceCents());
    }

    /** L'ultima fascia è aperta: c'è sempre un prezzo per il posto successivo, quanti che siano. */
    @Test
    void laFasciaAltaEApertaEValeAnchePerNumeriGrandi() {
        SeatPricingVersion listino = listinoDelFile();

        assertTrue(SeatPricing.bandFor(100_000, listino).isOpenEnded());
        assertEquals(49, SeatPricing.nextSeatCents(1_000_000, listino));
        // 15003 (primi 100) + 999_900 posti oltre il centesimo a 0,49 €
        assertEquals(15_003L + 999_900L * 49L, SeatPricing.dueCents(1_000_000, listino));
    }

    // ── Coerenza del listino ──────────────────────────────────────────────────

    @Test
    void unListinoSenzaFasceERifiutato() {
        IncoherentSeatPricingException e =
                assertThrows(IncoherentSeatPricingException.class, () -> SeatPricing.dueCents(5, listino()));
        assertTrue(e.getMessage().contains("senza fasce"), e.getMessage());
    }

    @Test
    void unListinoCheNonParteDalPostoUnoERifiutato() {
        SeatPricingVersion listino = listino().addBand(2, null, 299);

        IncoherentSeatPricingException e =
                assertThrows(IncoherentSeatPricingException.class, () -> SeatPricing.dueCents(5, listino));
        assertTrue(e.getMessage().contains("deve partire dal posto 1"), e.getMessage());
    }

    @Test
    void unListinoConUnBucoFraLeFasceERifiutato() {
        SeatPricingVersion listino = listino().addBand(1, 3, 0).addBand(5, null, 299);

        IncoherentSeatPricingException e =
                assertThrows(IncoherentSeatPricingException.class, () -> SeatPricing.dueCents(6, listino));
        assertTrue(e.getMessage().contains("non contigue"), e.getMessage());
    }

    @Test
    void unListinoConFasceSovrapposteERifiutato() {
        SeatPricingVersion listino = listino().addBand(1, 3, 0).addBand(3, null, 299);

        IncoherentSeatPricingException e =
                assertThrows(IncoherentSeatPricingException.class, () -> SeatPricing.dueCents(6, listino));
        assertTrue(e.getMessage().contains("non contigue"), e.getMessage());
    }

    @Test
    void unListinoConUltimaFasciaChiusaERifiutato() {
        SeatPricingVersion listino = listino().addBand(1, 3, 0).addBand(4, 10, 299);

        IncoherentSeatPricingException e =
                assertThrows(IncoherentSeatPricingException.class, () -> SeatPricing.dueCents(6, listino));
        assertTrue(e.getMessage().contains("l'ultima fascia deve essere aperta"), e.getMessage());
    }

    @Test
    void unListinoConUnaFasciaApertaInMezzoERifiutato() {
        SeatPricingVersion listino = listino().addBand(1, null, 0).addBand(4, null, 299);

        IncoherentSeatPricingException e =
                assertThrows(IncoherentSeatPricingException.class, () -> SeatPricing.dueCents(6, listino));
        assertTrue(e.getMessage().contains("solo l'ultima fascia può essere aperta"), e.getMessage());
    }

    @Test
    void unaTariffaNegativaERifiutata() {
        SeatPricingVersion listino = listino().addBand(1, 3, 0).addBand(4, null, -1);

        IncoherentSeatPricingException e =
                assertThrows(IncoherentSeatPricingException.class, () -> SeatPricing.dueCents(6, listino));
        assertTrue(e.getMessage().contains("tariffa negativa"), e.getMessage());
    }

    // ── Confini degli argomenti ───────────────────────────────────────────────

    @Test
    void unNumeroDiPostiNegativoNonEUnCasoDaCalcolare() {
        SeatPricingVersion listino = listinoDelFile();

        assertThrows(IllegalArgumentException.class, () -> SeatPricing.dueCents(-1, listino));
        assertThrows(IllegalArgumentException.class, () -> SeatPricing.nextSeatCents(-1, listino));
    }

    @Test
    void ilPrimoPostoEIlNumeroUno() {
        SeatPricingVersion listino = listinoDelFile();

        assertThrows(IllegalArgumentException.class, () -> SeatPricing.bandFor(0, listino));
    }
}
