package app.appgrove.fatture;

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

    private static final String TABLE = "app_fatture.entitlement_projection";

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

    /**
     * Invecchia le righe del tenant di {@code seconds} secondi, spostandone indietro la data di
     * rinfresco. Serve a provare la <b>scadenza</b> (change 0094) senza attese reali e senza toccare la
     * configurazione: abbassare la durata massima nel profilo di test farebbe scadere anche le righe dei
     * test che dimostrano il disaccoppiamento, e passerebbero — o falliranno — per la ragione sbagliata.
     *
     * <p>Non tocca {@code stale}: una riga invecchiata è diversa da una invalidata, ed è proprio quella
     * distinzione che i test devono poter esercitare separatamente.
     */
    public void ageBySeconds(String tenantId, int seconds) {
        try (Connection c = ds.getConnection();
                PreparedStatement ps = c.prepareStatement("update " + TABLE
                        + " set refreshed_at = refreshed_at - make_interval(secs => ?) where tenant_id = ?")) {
            ps.setInt(1, seconds);
            ps.setString(2, tenantId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("invecchiamento della proiezione di test fallito", e);
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
