package app.appgrove.auth.cognito;

import io.agroal.api.AgroalDataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/** Query scalari di verifica sullo schema platform nei test Cognito. */
final class TestDb {

    private TestDb() {}

    static long count(AgroalDataSource ds, String sql) {
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getLong(1);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static String text(AgroalDataSource ds, String sql) {
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getString(1);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
