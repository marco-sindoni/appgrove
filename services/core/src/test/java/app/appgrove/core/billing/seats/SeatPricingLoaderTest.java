package app.appgrove.core.billing.seats;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.appgrove.core.TestData;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Il caricamento iniziale del listino dei posti (UC 0102 §7, requisito di test «creazione della prima
 * versione dal file all'avvio, senza duplicarla ai riavvii successivi»).
 *
 * <p>La prima versione <b>esiste già</b> quando questo collaudo parte: l'ha creata
 * {@link SeatPricingStartup} all'avvio dell'applicazione di test. È esattamente ciò che si vuole provare —
 * che l'avvio la crei — e insieme il primo dei due riavvii della prova di idempotenza.
 */
@QuarkusTest
class SeatPricingLoaderTest {

    @Inject
    SeatPricingLoader loader;

    @Inject
    TestData data;

    @Test
    void laPrimaVersioneNasceAllAvvioDalFile() {
        SeatPricingDefinition file = loader.read();

        assertEquals(1, data.seatPricingVersionCount(), "una sola versione dopo l'avvio");

        List<int[]> bands = data.seatPricingBands(SeatPricingLoader.versionId(file.effectiveFrom().toString()));
        assertEquals(file.bands().size(), bands.size(), "tante fasce quante quelle del file");
        for (int i = 0; i < bands.size(); i++) {
            SeatPricingDefinition.BandDef atteso = file.bands().get(i);
            int[] scritto = bands.get(i);
            assertEquals(atteso.fromSeat(), scritto[0], "posto iniziale della fascia " + i);
            // -1 è la convenzione dell'aiutante di collaudo per «posto finale vuoto» (fascia aperta).
            assertEquals(atteso.toSeat() == null ? -1 : atteso.toSeat(), scritto[1], "posto finale " + i);
            assertEquals(atteso.unitPriceCents(), scritto[2], "tariffa della fascia " + i);
        }
    }

    /**
     * Il secondo avvio non crea nulla. È la prova che conta davvero: un caricamento non idempotente
     * moltiplicherebbe i listini a ogni riavvio, e con due versioni alla stessa decorrenza la domanda
     * «quale vigeva quel giorno» non avrebbe più risposta.
     */
    @Test
    void ilSecondoAvvioNonDuplicaNulla() {
        int prima = data.seatPricingVersionCount();

        assertFalse(loader.ensureInitialVersion(), "il secondo avvio non deve creare nulla");
        assertFalse(loader.ensureInitialVersion(), "e nemmeno il terzo");

        assertEquals(prima, data.seatPricingVersionCount());
    }

    /** Il file del prodotto è un listino coerente: se non lo fosse, l'avvio non sarebbe nemmeno arrivato qui. */
    @Test
    void ilFileDelProdottoEUnListinoCoerente() {
        SeatPricingDefinition file = loader.read();

        SeatPricing.requireCoherent(file.asVersion());

        assertEquals("EUR", file.currency());
        assertTrue(file.bands().size() >= 2, "un listino a scaglioni ha almeno due fasce");
        assertEquals(0, file.bands().get(0).unitPriceCents(), "la prima fascia è la franchigia, a tariffa zero");
    }
}
