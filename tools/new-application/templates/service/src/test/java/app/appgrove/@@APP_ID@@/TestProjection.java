package app.appgrove.@@APP_ID@@;

import io.agroal.api.AgroalDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Utilità di test sulle <b>copie locali</b> del servizio — diritti d'accesso (UC 0046) e ruolo per
 * applicazione (UC 0099/0101): azzeramento e ispezione diretta.
 *
 * <p>Serve perché quelle copie <b>sopravvivono fra i test</b>: sono cache su tabella, non stato in
 * memoria. Un test che cambia l'esito della rete di sicurezza senza azzerarle continuerebbe a leggere il
 * valore memorizzato dal test precedente — e passerebbe o fallirebbe per la ragione sbagliata.
 */
@ApplicationScoped
public class TestProjection {

    private static final String TABLE = "@@SCHEMA@@.entitlement_projection";
    private static final String ROLE_TABLE = "@@SCHEMA@@.app_role_projection";

    @Inject
    AgroalDataSource ds;

    /** Svuota <b>entrambe</b> le copie: il prossimo accesso ricadrà sulla rete di sicurezza. */
    public void clear() {
        execute("delete from " + TABLE);
        execute("delete from " + ROLE_TABLE);
    }

    /** Righe della copia del ruolo per il tenant (per distinguere «assente» da «diniego noto»). */
    public int roleRowsFor(String tenantId) {
        return count(ROLE_TABLE, tenantId);
    }

    /** Ruolo copiato per quella persona su questa applicazione, o {@code null} se assente/diniego. */
    public String roleOf(String tenantId, String subject) {
        try (Connection c = ds.getConnection();
                PreparedStatement ps = c.prepareStatement(
                        "select role from " + ROLE_TABLE + " where tenant_id = ? and subject = ? and app_slug = ?")) {
            ps.setString(1, tenantId);
            ps.setString(2, subject);
            ps.setString(3, "@@APP_ID@@");
            try (var rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("lettura copia del ruolo di test fallita", e);
        }
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
        return count(TABLE, tenantId);
    }

    private int count(String table, String tenantId) {
        try (Connection c = ds.getConnection();
                PreparedStatement ps = c.prepareStatement("select count(*) from " + table + " where tenant_id = ?")) {
            ps.setString(1, tenantId);
            try (var rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("conteggio copia locale di test fallito", e);
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
