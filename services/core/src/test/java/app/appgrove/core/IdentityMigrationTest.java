package app.appgrove.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.agroal.api.AgroalDataSource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Travaso di UC 0116 (migrazione {@code V17__identity_membership.sql}), provato con <b>conteggi a
 * confronto</b> e non a occhio — è il collaudo che permette di dormire.
 *
 * <p>Il travaso vero gira una volta sola, su dati che in un ambiente di collaudo non ci sono: qui lo
 * si riesegue <b>davvero</b>, prendendo il SQL dal file di migrazione (non una copia che può
 * divergere) e rimappandolo su uno schema-sonda usa-e-getta. Così il collaudo prova due cose che
 * contano: che il travaso non perde righe, e che la <b>guardia dentro la migrazione</b> fallisce
 * quando qualcosa si perde.
 */
@QuarkusTest
class IdentityMigrationTest {

    private static final String MIGRATION = "db/migration/V17__identity_membership.sql";
    private static final String PROBE = "v17_probe";

    @Inject
    AgroalDataSource ds;

    // ── il vincolo di troppo se n'è andato, e quello giusto è arrivato ────────

    @Test
    void gliIndiciUniciGlobaliSuUsersNonEsistonoPiu() {
        assertFalse(indexExists("ux_users_cognito_sub"),
                "l'indice che imponeva «1 utente → 1 account» dev'essere caduto");
        assertFalse(indexExists("ux_users_email"),
                "l'unicità dell'indirizzo non vive più su una tabella interna all'account");
    }

    @Test
    void unicitaGlobaleVivesuIdentita() {
        assertTrue(indexExists("ux_identity_cognito_sub"));
        assertTrue(indexExists("ux_identity_email"));
    }

    @Test
    void ilVincoloCheServeVivesullAppartenenzaEdEParziale() {
        String definition = indexDefinition("ux_membership_tenant_identity");
        assertTrue(definition != null && definition.contains("UNIQUE"),
                "unicità su (tenant_id, identity_id)");
        assertTrue(definition.contains("deleted_at IS NULL"),
                "limitata alle righe vive: chi esce da un account deve poterci rientrare");
    }

    // ── il travaso, rieseguito con conteggi a confronto ──────────────────────

    @Test
    void travasoConvertreOgniUtenteVivoInUnaIdentitaPiuUnaAppartenenza() throws Exception {
        withProbeSchema(st -> {
            seedProbeUser(st, "probe-a", "probe-a@example.test", "t-1", "owner", false);
            seedProbeUser(st, "probe-b", "probe-b@example.test", "t-1", "member", false);
            seedProbeUser(st, "probe-c", "probe-c@example.test", "t-2", "owner", false);
            // riga già cancellata: non è una persona viva e non deve produrre nulla
            seedProbeUser(st, "probe-d", "probe-d@example.test", "t-2", "member", true);

            st.execute(probe(travasoSql()));

            assertEquals(3, count(st, "identity"), "una identità per ogni utente vivo");
            assertEquals(3, count(st, "membership"), "una appartenenza per ogni utente vivo");
            assertEquals(3, scalar(st, "select count(*) from " + PROBE + ".identity i"
                            + " join " + PROBE + ".users u on u.id = i.id"
                            + " where lower(u.email) = lower(i.email) and u.cognito_sub = i.cognito_sub"),
                    "stesso indirizzo e stesso identificativo di autenticazione, riga per riga");
            assertEquals(3, scalar(st, "select count(*) from " + PROBE + ".membership m"
                            + " join " + PROBE + ".users u on u.id = m.identity_id"
                            + " where m.tenant_id = u.tenant_id and m.role = u.role and m.status = u.status"),
                    "stesso account, stesso ruolo, stesso stato");
            assertEquals(1, scalar(st, "select count(distinct id) from " + PROBE + ".identity"
                            + " where id in (select id from " + PROBE + ".users where cognito_sub = 'probe-a')"),
                    "l'identità conserva l'identificativo della riga utente");

            // la guardia della migrazione è verde quando i conteggi tornano
            st.execute(probe(guardiaSql()));
        });
    }

    @Test
    void laGuardiaDentroLaMigrazioneFallisceSeIlTravasoPerdeRighe() throws Exception {
        withProbeSchema(st -> {
            seedProbeUser(st, "perso-a", "perso-a@example.test", "t-9", "owner", false);
            seedProbeUser(st, "perso-b", "perso-b@example.test", "t-9", "member", false);
            st.execute(probe(travasoSql()));

            // qualcuno si perde per strada: è esattamente il difetto che la guardia esiste per cogliere
            st.execute("delete from " + PROBE + ".identity where cognito_sub = 'perso-b'");
            SQLException error = assertThrows(SQLException.class, () -> st.execute(probe(guardiaSql())));
            assertTrue(error.getMessage().contains("V17"),
                    "la migrazione deve fallire a voce alta: " + error.getMessage());
        });
    }

    // ── helper ───────────────────────────────────────────────────────────────

    /**
     * Il SQL del travaso, letto dal file di migrazione fra i due marcatori di sezione: se la
     * migrazione cambia, questo collaudo prova la versione nuova e non una copia invecchiata.
     */
    private static String travasoSql() {
        String sql = migrationSource();
        int from = sql.indexOf("INSERT INTO platform.identity");
        int to = sql.indexOf("DROP INDEX platform.ux_users_cognito_sub");
        return sql.substring(from, to);
    }

    /** Il blocco di guardia dei conteggi, letto dallo stesso file. */
    private static String guardiaSql() {
        String sql = migrationSource();
        return sql.substring(sql.indexOf("DO $$"));
    }

    private static String migrationSource() {
        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(MIGRATION)) {
            if (in == null) {
                throw new IllegalStateException("migrazione non trovata nel classpath: " + MIGRATION);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** Riscrive il SQL della migrazione sullo schema-sonda: stesse istruzioni, tabelle usa-e-getta. */
    private static String probe(String sql) {
        return sql.replace("platform.", PROBE + ".");
    }

    private interface ProbeWork {
        void run(Statement st) throws Exception;
    }

    /** Schema-sonda con la stessa forma delle tre tabelle vere, ricreato e distrutto a ogni collaudo. */
    private void withProbeSchema(ProbeWork work) throws Exception {
        try (Connection c = ds.getConnection(); Statement st = c.createStatement()) {
            st.execute("drop schema if exists " + PROBE + " cascade");
            st.execute("create schema " + PROBE);
            st.execute("create table " + PROBE + ".users as select * from platform.users where false");
            st.execute("create table " + PROBE + ".identity as select * from platform.identity where false");
            st.execute("create table " + PROBE + ".membership as select * from platform.membership where false");
            try {
                work.run(st);
            } finally {
                st.execute("drop schema if exists " + PROBE + " cascade");
            }
        }
    }

    private void seedProbeUser(Statement st, String sub, String email, String tenantId, String role,
            boolean deleted) throws SQLException {
        st.execute("insert into " + PROBE + ".users"
                + "(id, tenant_id, cognito_sub, email, display_name, role, status, locale,"
                + " created_at, updated_at, created_by, deleted_at) values ('"
                + UUID.randomUUID() + "', '" + tenantId + "', '" + sub + "', '" + email + "', '"
                + sub + "', '" + role + "', 'active', 'en', now(), now(), 'probe', "
                + (deleted ? "'" + OffsetDateTime.now() + "'" : "null") + ")");
    }

    private long count(Statement st, String table) throws SQLException {
        return scalar(st, "select count(*) from " + PROBE + "." + table);
    }

    private long scalar(Statement st, String sql) throws SQLException {
        try (ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getLong(1);
        }
    }

    private boolean indexExists(String name) {
        return indexDefinition(name) != null;
    }

    private String indexDefinition(String name) {
        try (Connection c = ds.getConnection(); Statement st = c.createStatement();
                ResultSet rs = st.executeQuery(
                        "select indexdef from pg_indexes where schemaname = 'platform'"
                                + " and indexname = '" + name + "'")) {
            return rs.next() ? rs.getString(1) : null;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
