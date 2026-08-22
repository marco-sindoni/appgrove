package app.appgrove.core.billing.seats;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.test.junit.main.Launch;
import io.quarkus.test.junit.main.LaunchResult;
import io.quarkus.test.junit.main.QuarkusMainTest;
import org.junit.jupiter.api.Test;

/**
 * Command-mode {@code seed-seat-pricing} (UC 0102): è il passo con cui la pipeline semina il listino dei
 * posti in produzione, dopo il {@code migrate}, perché in prod la semina allo startup è <b>spenta</b> —
 * l'artefatto di spedizione non deve toccare la banca dati per arrivare in ascolto.
 *
 * <p>Questo collaudo esercita l'intero percorso argomento → caricamento → uscita. In test il listino è già
 * seminato allo startup, quindi l'esito atteso è «listino già presente»: cioè la prova che il comando è
 * <b>idempotente</b>, che è esattamente ciò che serve a un passo di distribuzione rieseguibile.
 */
@QuarkusMainTest
class SeedSeatPricingCommandTest {

    @Test
    @Launch("seed-seat-pricing")
    void ilComandoSeminaIlListinoETermina(LaunchResult result) {
        assertTrue(
                result.getOutput().contains("seed-seat-pricing completata"),
                "output atteso: 'seed-seat-pricing completata' — output reale:\n" + result.getOutput());
        assertTrue(
                result.getOutput().contains("listino già presente"),
                "in test il listino è seminato allo startup: il comando deve trovarlo e non riscriverlo"
                        + " — output reale:\n" + result.getOutput());
    }
}
