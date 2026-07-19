package app.appgrove.@@APP_ID@@;

import io.agroal.api.AgroalDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Utilità di test sulla proiezione entitlement (UC 0046): azzeramento e ispezione diretta.
 *
 * <p>Serve perché la proiezione <b>sopravvive fra i test</b>: è una cache su tabella, non uno stato
 * in memoria. Un test che cambia l'esito della rete di sicurezza senza azzerare la proiezione
 * continuerebbe a leggere il valore memorizzato dal test precedente — e passerebbe o fallirebbe per
 * la ragione sbagliata.
 */
@ApplicationScoped
public class TestProjection {

    private static final String TABLE = "@@SCHEMA@@.entitlement_projection";

    @Inject
    AgroalDataSource ds;

    /** Svuota la proiezione: il prossimo accesso ricadrà sulla rete di sicurezza. */
    public void clear() {
        execute("delete from " + TABLE);
    }

    /** Marca da rinfrescare le righe del tenant, come farebbe il consumer di invalidazione. */
    public void markStale(String tenantId) {
        try (Connection c = ds.getConnection();
                PreparedStatement ps =
                        c.prepareStatement("update " + TABLE + " set stale = true, invalidated_at = now()"
                                + " where tenant_id = ?")) {
            ps.setString(1, tenantId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("markStale di test fallito", e);
        }
    }

    /** Righe presenti per il tenant (per distinguere "assente" da "presente ma senza accesso"). */
    public int rowsFor(String tenantId) {
        try (Connection c = ds.getConnection();
                PreparedStatement ps = c.prepareStatement("select count(*) from " + TABLE + " where tenant_id = ?")) {
            ps.setString(1, tenantId);
            try (var rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("conteggio proiezione di test fallito", e);
        }
    }

    private void execute(String sql) {
        try (Connection c = ds.getConnection();
                PreparedStatement ps = c.prepareStatement(sql)) {
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("pulizia proiezione di test fallita", e);
        }
    }
}
