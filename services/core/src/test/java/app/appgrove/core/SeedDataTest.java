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

    private static Path repoFile(String rel) {
        // user.dir = services/core → ../../ = repo root
        return Path.of(System.getProperty("user.dir")).getParent().getParent().resolve(rel);
    }

    private void applySeed() throws Exception {
        // identità + subscription: il catalogo è già presente (loader allo startup), quindi le FK risolvono.
        String sql = Files.readString(repoFile("dev/seed/seed.sql"))
                + "\n"
                + Files.readString(repoFile("dev/seed/seed-subscriptions.sql"));
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
        assertEquals(5, scalar("select count(*) from platform.identity where created_by = 'seed'"),
                "5 persone: Acme owner + due member, Bob, Platform admin");
        assertEquals(5, scalar("select count(*) from platform.membership where created_by = 'seed'"),
                "5 appartenenze: una per persona (il caso normale, UC 0116)");
        assertEquals(2, scalar(
                "select count(*) from platform.invitations where created_by = 'seed' and status = 'pending'"),
                "2 inviti pending (Acme)");

        // ── ruoli di piattaforma: DUE valori (UC 0098) ───────────────────────
        assertEquals(1, scalar("select count(*) from platform.membership where created_by = 'seed' and role = 'owner' and tenant_id = 'a0000000-0000-4000-8000-000000000001'"));
        assertEquals(2, scalar("select count(*) from platform.membership where created_by = 'seed' and role = 'member' and tenant_id = 'a0000000-0000-4000-8000-000000000001'"),
                "Acme: due member (chi era admin di piattaforma ora è member con ruolo admin sul crm)");
        assertEquals(0, scalar("select count(*) from platform.membership where role = 'admin'"),
                "il ruolo di piattaforma 'admin' è stato ritirato: nessuna riga può portarlo");

        // ── accessi per applicazione (UC 0098) ───────────────────────────────
        // Il potere sta sull'applicazione: `admin@acme.test` è admin del crm, `member@acme.test` editor.
        // L'owner non ha righe: l'accesso gli è implicito.
        assertEquals(2, scalar("select count(*) from platform.app_access where created_by = 'seed'"),
                "2 accessi al crm: uno admin, uno editor");
        assertEquals("admin", text("select aa.role from platform.app_access aa"
                + " join platform.app app on app.id = aa.app_id"
                + " where app.slug = 'crm' and aa.identity_id = 'b0000000-0000-4000-8000-000000000002'"));
        assertEquals("editor", text("select aa.role from platform.app_access aa"
                + " join platform.app app on app.id = aa.app_id"
                + " where app.slug = 'crm' and aa.identity_id = 'b0000000-0000-4000-8000-000000000003'"));
        assertEquals(0, scalar("select count(*) from platform.app_access"
                + " where identity_id = 'b0000000-0000-4000-8000-000000000001'"),
                "l'owner non ha righe di accesso: gli è implicito");

        // ── catalogo (single/multi/disabled) ─────────────────────────────────
        // Non più nel seed: con il pricing-as-code (UC 0022) il catalogo è prodotto dal loader allo startup
        // (created_by = 'sync'), dagli YAML in services/core/.../pricing/. Le subscription del seed lo
        // referenziano via UUID deterministici (CatalogIds) → la doppia applicazione del seed non duplica.
        // 5 app reali+fixture: fatture, crm (app #2 reale, UC 0054), notes, teams, legacy.
        assertEquals(5, scalar("select count(*) from platform.app where created_by = 'sync'"));
        // 7 tier: fatture(free) + crm(free,team) + notes(free,pro) + teams(team) + legacy(std).
        assertEquals(7, scalar("select count(*) from platform.app_tier where created_by = 'sync'"));
        // 6 prezzi: notes pro (mensile+annuale) + teams (mensile+annuale) + crm team (mensile+annuale).
        assertEquals(6, scalar("select count(*) from platform.app_price where created_by = 'sync'"));
        assertEquals("inactive", text("select status from platform.app where slug = 'legacy'"),
                "l'app 'legacy' è disabilitata dall'admin (esercita il gate app-abilitata)");
        assertEquals("inactive", text("select status from platform.app where slug = 'crm'"),
                "crm è disabilitata di default (change 0042): veicolo di validazione, non prodotto in vendita");
        assertEquals("single_user", text("select user_model from platform.app where slug = 'notes'"));
        assertEquals("multi_user", text("select user_model from platform.app where slug = 'teams'"));
        // app #1 reale (UC 0051): fatture, single-user, con tier free cap 10 fatture/mese
        assertEquals("single_user", text("select user_model from platform.app where slug = 'fatture'"));
        assertEquals(10, scalar("select (limits->>'cap')::int from platform.app_tier"
                + " where app_id = (select id from platform.app where slug = 'fatture')"));

        // ── subscription: stati di lifecycle vari (entitlement derivato) ──────
        assertEquals(5, scalar("select count(*) from platform.subscription where created_by = 'seed'"));
        assertEquals(1, scalar("select count(*) from platform.subscription where created_by = 'seed' and status = 'past_due'"));
        assertEquals(1, scalar("select count(*) from platform.subscription where created_by = 'seed' and status = 'trialing'"));
        assertEquals(1, scalar("select count(*) from platform.subscription where created_by = 'seed' and status = 'canceled'"));
        assertEquals(2, scalar("select count(*) from platform.subscription where created_by = 'seed' and status = 'active'"));

        // ── ≥2 tenant per la matrice cross-tenant ────────────────────────────
        assertEquals(2, scalar("select count(distinct tenant_id) from platform.subscription where created_by = 'seed'"));

        // ── dati 100% sintetici (no PII): ogni email del seed è *.test ────────
        assertEquals(0, scalar("select count(*) from platform.identity where created_by = 'seed' and email not like '%.test'"),
                "tutte le email del seed sono sintetiche (*.test)");

        // ── platform.users è FREDDA (UC 0116, change 0088) ────────────────────
        // Il seme non la popola più: se qualcuno la ripopolasse, avremmo due verità sulla stessa
        // persona e nessun modo di sapere quale vince.
        assertEquals(0, scalar("select count(*) from platform.users where created_by = 'seed'"),
                "platform.users è la rete di ritorno del travaso: il seme non la scrive");
    }
}
