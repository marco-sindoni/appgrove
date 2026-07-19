package app.appgrove.auth;

import io.agroal.api.AgroalDataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Prepara la DB di test (Dev Services): applica <b>tutte</b> le migrazioni del core in ordine di
 * versione, una volta sola, e carica il seed condiviso ({@code dev/seed/seed.sql}, idempotente).
 * Il provider locale non possiede migrazioni platform: in dev le applica {@code dev migrate}/
 * {@code dev seed}, nei test questo helper.
 *
 * <p>Applica l'intera cartella e non un elenco cablato (era V1+V2): così una nuova migrazione che
 * tocca {@code platform.users} — come {@code V8__user_locale.sql} (UC 0018) — arriva qui da sola,
 * invece di far fallire questi test con un "colonna inesistente" a ogni giro.
 */
public final class TestSchema {

    private TestSchema() {}

    public static synchronized void ensure(AgroalDataSource ds) {
        Path base = Path.of(System.getProperty("user.dir")); // services/auth
        try (Connection c = ds.getConnection(); Statement st = c.createStatement()) {
            if (!tableExists(st)) {
                for (Path migration : coreMigrations(base)) {
                    st.execute(Files.readString(migration));
                }
            }
            st.execute(read(base, "../../dev/seed/seed.sql")); // idempotente (ON CONFLICT)
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** Migrazioni del core ordinate per numero di versione (V2 prima di V10, non alfabetico). */
    private static List<Path> coreMigrations(Path base) throws Exception {
        Path dir = base.resolve("../core/src/main/resources/db/migration").normalize();
        try (Stream<Path> files = Files.list(dir)) {
            return files.filter(p -> p.getFileName().toString().matches("V\\d+__.*\\.sql"))
                    .sorted(Comparator.comparingInt(TestSchema::version))
                    .toList();
        }
    }

    private static int version(Path migration) {
        String name = migration.getFileName().toString();
        return Integer.parseInt(name.substring(1, name.indexOf("__")));
    }

    private static boolean tableExists(Statement st) throws Exception {
        try (ResultSet rs = st.executeQuery("select to_regclass('platform.users')")) {
            rs.next();
            return rs.getString(1) != null;
        }
    }

    private static String read(Path base, String rel) throws Exception {
        return Files.readString(base.resolve(rel).normalize());
    }
}
