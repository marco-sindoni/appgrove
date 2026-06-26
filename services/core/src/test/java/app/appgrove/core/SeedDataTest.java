package app.appgrove.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.agroal.api.AgroalDataSource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import org.junit.jupiter.api.Test;

/**
 * Valida il seed deterministico (UC 0011): applica lo STESSO {@code dev/seed/seed.sql} due volte
 * contro il Postgres di test (Testcontainers + Flyway già migrato) e verifica cast/stati e
 * idempotenza. Le righe del seed sono individuabili da {@code created_by = 'seed'}, così le
 * asserzioni restano scoped al seed anche con la DB di test condivisa con le altre suite.
 */
@QuarkusTest
class SeedDataTest {

    @Inject
    AgroalDataSource ds;

    private static Path seedFile() {
        // user.dir = services/core → ../../ = repo root
        return Path.of(System.getProperty("user.dir"))
                .getParent().getParent()
                .resolve("dev/seed/seed.sql");
    }

    private void applySeed() throws Exception {
        String sql = Files.readString(seedFile());
        try (Connection c = ds.getConnection(); Statement st = c.createStatement()) {
            st.execute(sql); // pgjdbc esegue più statement separati da ';'
        }
    }

    private long scalar(String sql) {
        try (Connection c = ds.getConnection();
                Statement st = c.createStatement();
                ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getLong(1);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String text(String sql) {
        try (Connection c = ds.getConnection();
                Statement st = c.createStatement();
                ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getString(1);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void seedIsIdempotentAndCoversTheCast() throws Exception {
        // doppia applicazione: deve completare senza errori e senza duplicare (ON CONFLICT DO UPDATE)
        applySeed();
        applySeed();

        // ── cast multi-tenant ────────────────────────────────────────────────
        assertEquals(3, scalar("select count(*) from platform.accounts where created_by = 'seed'"),
                "3 account: Acme (B2B), Bob (B2C), Platform");
        assertEquals(5, scalar("select count(*) from platform.users where created_by = 'seed'"),
                "5 utenti: Acme owner/admin/member, Bob, Platform admin");
        assertEquals(2, scalar(
                "select count(*) from platform.invitations where created_by = 'seed' and status = 'pending'"),
                "2 inviti pending (Acme)");

        // ── ruoli (B2B) ──────────────────────────────────────────────────────
        assertEquals(1, scalar("select count(*) from platform.users where created_by = 'seed' and role = 'owner' and tenant_id = 'a0000000-0000-4000-8000-000000000001'"));
        assertEquals(1, scalar("select count(*) from platform.users where created_by = 'seed' and role = 'admin'"));
        assertEquals(1, scalar("select count(*) from platform.users where created_by = 'seed' and role = 'member'"));

        // ── catalogo (single/multi/disabled) ─────────────────────────────────
        assertEquals(3, scalar("select count(*) from platform.app where created_by = 'seed'"));
        assertEquals(4, scalar("select count(*) from platform.app_tier where created_by = 'seed'"));
        assertEquals(4, scalar("select count(*) from platform.app_price where created_by = 'seed'"));
        assertEquals("inactive", text("select status from platform.app where slug = 'legacy'"),
                "l'app 'legacy' è disabilitata dall'admin (esercita il gate app-abilitata)");
        assertEquals("single_user", text("select user_model from platform.app where slug = 'notes'"));
        assertEquals("multi_user", text("select user_model from platform.app where slug = 'teams'"));

        // ── subscription: stati di lifecycle vari (entitlement derivato) ──────
        assertEquals(5, scalar("select count(*) from platform.subscription where created_by = 'seed'"));
        assertEquals(1, scalar("select count(*) from platform.subscription where created_by = 'seed' and status = 'past_due'"));
        assertEquals(1, scalar("select count(*) from platform.subscription where created_by = 'seed' and status = 'trialing'"));
        assertEquals(1, scalar("select count(*) from platform.subscription where created_by = 'seed' and status = 'canceled'"));
        assertEquals(2, scalar("select count(*) from platform.subscription where created_by = 'seed' and status = 'active'"));

        // ── ≥2 tenant per la matrice cross-tenant ────────────────────────────
        assertEquals(2, scalar("select count(distinct tenant_id) from platform.subscription where created_by = 'seed'"));

        // ── dati 100% sintetici (no PII): ogni email del seed è *.test ────────
        assertEquals(0, scalar("select count(*) from platform.users where created_by = 'seed' and email not like '%.test'"),
                "tutte le email del seed sono sintetiche (*.test)");
    }
}
