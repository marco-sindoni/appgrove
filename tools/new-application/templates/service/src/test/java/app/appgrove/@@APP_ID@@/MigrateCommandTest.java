package app.appgrove.@@APP_ID@@;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.test.junit.main.Launch;
import io.quarkus.test.junit.main.LaunchResult;
import io.quarkus.test.junit.main.QuarkusMainTest;
import org.junit.jupiter.api.Test;

/**
 * Command-mode {@code migrate} (UC 0005): il task ECS one-shot della pipeline applica le migrazioni
 * Flyway (schema {@code @@SCHEMA@@}) e termina con exit code 0 — stesso pattern del core.
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
