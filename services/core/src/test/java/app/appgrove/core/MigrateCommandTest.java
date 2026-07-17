package app.appgrove.core;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.test.junit.main.Launch;
import io.quarkus.test.junit.main.LaunchResult;
import io.quarkus.test.junit.main.QuarkusMainTest;
import org.junit.jupiter.api.Test;

/**
 * Command-mode {@code migrate} (UC 0005): il task ECS one-shot della pipeline applica le migrazioni
 * Flyway e termina con exit code 0. In test lo schema è già migrato all'avvio
 * ({@code migrate-at-start=true}) → il comando è idempotente (0 nuove migrazioni) ma esercita
 * l'intero percorso arg → Flyway → exit.
 */
@QuarkusMainTest
class MigrateCommandTest {

    @Test
    @Launch("migrate")
    void migrateApplicaFlywayETermina(LaunchResult result) {
        assertTrue(
                result.getOutput().contains("migrate completata"),
                "output atteso: 'migrate completata' — output reale:\n" + result.getOutput());
    }
}
