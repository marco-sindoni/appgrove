package app.appgrove.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.test.junit.main.Launch;
import io.quarkus.test.junit.main.LaunchResult;
import io.quarkus.test.junit.main.QuarkusMainTest;
import org.junit.jupiter.api.Test;

/**
 * Command-mode {@code offboard-app} (UC 0048): esercita il percorso arg → {@code AppOffboarding} →
 * exit. Con un app_id senza tenant l'orchestrazione è un no-op che completa (exit 0); senza app_id il
 * comando rifiuta con exit 1. Il fan-out vero è verificato in unità da {@code AppOffboardingTest}.
 */
@QuarkusMainTest
class OffboardAppCommandTest {

    @Test
    @Launch(value = "offboard-app", exitCode = 1)
    void senzaAppIdEsceConErrore(LaunchResult result) {
        assertEquals(1, result.exitCode(), "offboard-app senza app_id deve uscire con codice 1");
    }

    @Test
    @Launch({"offboard-app", "appinesistente"})
    void conAppIdCompletaEEsceZero(LaunchResult result) {
        assertTrue(
                result.getOutput().contains("offboard-app completata"),
                "output atteso: 'offboard-app completata' — output reale:\n" + result.getOutput());
    }
}
