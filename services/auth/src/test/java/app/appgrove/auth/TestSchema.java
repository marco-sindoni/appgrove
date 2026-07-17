package app.appgrove.auth;

import io.agroal.api.AgroalDataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Prepara la DB di test (Dev Services): applica le migrazioni del core (V1+V2) una volta e carica il
 * seed condiviso ({@code dev/seed/seed.sql}, idempotente). il provider locale non possiede migrazioni platform: in dev
 * le applica {@code dev migrate}/{@code dev seed}, nei test questo helper.
 */
public final class TestSchema {

    private TestSchema() {}

    public static synchronized void ensure(AgroalDataSource ds) {
        Path base = Path.of(System.getProperty("user.dir")); // services/auth
        try (Connection c = ds.getConnection(); Statement st = c.createStatement()) {
            if (!tableExists(st)) {
                st.execute(read(base, "../core/src/main/resources/db/migration/V1__create_platform_schema.sql"));
                st.execute(read(base, "../core/src/main/resources/db/migration/V2__core_domain.sql"));
            }
            st.execute(read(base, "../../dev/seed/seed.sql")); // idempotente (ON CONFLICT)
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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
