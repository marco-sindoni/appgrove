package app.appgrove.auth.local;

import app.appgrove.auth.AuthUser;
import io.agroal.api.AgroalDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Lettura delle persone dallo schema {@code platform} via JDBC diretto. Il login è <b>pre-tenant</b>:
 * NON si usa l'entità {@code Membership} tenant-scoped del core (il discriminator richiederebbe un
 * tenant già noto). Replica la lettura DB del Pre-Token-Gen (#02 dec.9).
 *
 * <p>Dopo UC 0116 la lettura è identità ⋈ appartenenza: si cerca la <b>persona</b> (unica
 * globalmente per indirizzo e per identificativo di autenticazione) e se ne prende
 * l'<b>appartenenza attiva</b>. Lo stato risultante è {@code suspended} se lo è una qualsiasi delle
 * due — a chiusura in caso di dubbio: la sospensione della persona (limitazione del trattamento) e
 * quella decisa dall'owner del singolo account valgono entrambe.
 *
 * <p><b>Parità col Pre-Token-Gen</b> (infra/modules/platform_shared/lambda/pre_token_gen/handler.py):
 * con più appartenenze attive si prende la <b>più antica</b>, in modo deterministico e identico nei
 * due linguaggi. È un ripiego dichiarato e non una scelta di prodotto: la scelta dell'account attivo
 * di una sessione è di UC 0117. Se una delle due implementazioni cambia, l'altra cambia con essa.
 */
@ApplicationScoped
public class UserDirectory {

    /**
     * Una riga per persona: l'appartenenza più antica fra quelle vive. Lo stato è il peggiore dei
     * due (sospesa la persona OPPURE sospesa l'appartenenza → sospesa).
     */
    private static final String SELECT =
            "select i.cognito_sub, m.tenant_id, m.role,"
                    + " case when i.status = 'suspended' or m.status = 'suspended'"
                    + "      then 'suspended' else 'active' end as status,"
                    + " i.email, i.display_name, i.locale"
                    + " from platform.identity i"
                    + " join platform.membership m on m.identity_id = i.id and m.deleted_at is null"
                    + " where %s and i.deleted_at is null"
                    + " order by m.created_at, m.id limit 1";

    @Inject
    AgroalDataSource ds;

    public Optional<AuthUser> findByEmail(String email) {
        return query("lower(i.email) = lower(?)", email);
    }

    public Optional<AuthUser> findBySub(String sub) {
        return query("i.cognito_sub = ?", sub);
    }

    private Optional<AuthUser> query(String condition, String value) {
        String sql = SELECT.formatted(condition);
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, value);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(new AuthUser(
                        rs.getString("cognito_sub"),
                        rs.getString("tenant_id"),
                        rs.getString("role"),
                        rs.getString("status"),
                        rs.getString("email"),
                        rs.getString("display_name"),
                        rs.getString("locale")));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
