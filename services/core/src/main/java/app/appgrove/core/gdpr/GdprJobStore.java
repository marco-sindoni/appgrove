package app.appgrove.core.gdpr;

import app.appgrove.commons.gdpr.ExportResultMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.agroal.api.AgroalDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Scritture dello stato dei job di export eseguite dal consumer risultati, che gira <b>fuori</b> da
 * una richiesta autenticata: JDBC diretto con chiavi esplicite ({@code job_id}), stesso razionale di
 * {@code SubscriptionWriter}. Idempotente sotto redelivery (gli update riapplicano gli stessi valori;
 * i marcatori di stato finale non regrediscono).
 */
@ApplicationScoped
public class GdprJobStore {

    /** Effetto dell'esito ricevuto sullo stato del job. */
    public enum Outcome {
        /** Il job non esiste (mai creato o già purgato): esito orfano da scartare. */
        UNKNOWN_JOB,
        /** Item aggiornato; il job resta in corso. */
        UPDATED,
        /** Item aggiornato e un item è FAILED → job marcato FAILED (#13 D22). */
        JOB_FAILED,
        /** Item aggiornato e tutti COMPLETED → assemblare lo ZIP e chiamare {@link #markCompleted}. */
        ALL_COMPLETED
    }

    @Inject
    AgroalDataSource ds;

    @Inject
    ObjectMapper mapper;

    /** Applica l'esito di un servizio all'item e ricalcola lo stato del job. */
    public Outcome applyResult(ExportResultMessage result) {
        UUID jobId = UUID.fromString(result.jobId());
        try (Connection c = ds.getConnection()) {
            c.setAutoCommit(false);
            int updated;
            try (PreparedStatement ps = c.prepareStatement(
                    "update platform.gdpr_export_job_item"
                            + " set status = ?, steps = ?::jsonb, fragment_key = ?, error = ?, updated_at = ?"
                            + " where job_id = ? and app_id = ?")) {
                ps.setString(1, result.success()
                        ? GdprExportStatus.COMPLETED.name()
                        : GdprExportStatus.FAILED.name());
                ps.setString(2, mapper.writeValueAsString(result.steps()));
                ps.setString(3, result.fragmentKey());
                ps.setString(4, result.error());
                ps.setObject(5, OffsetDateTime.now());
                ps.setObject(6, jobId);
                ps.setString(7, result.appId());
                updated = ps.executeUpdate();
            }
            if (updated == 0) {
                c.rollback();
                return Outcome.UNKNOWN_JOB;
            }

            boolean anyFailed = false;
            boolean allCompleted = true;
            try (PreparedStatement ps = c.prepareStatement(
                    "select status from platform.gdpr_export_job_item where job_id = ?")) {
                ps.setObject(1, jobId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String status = rs.getString(1);
                        anyFailed |= GdprExportStatus.FAILED.name().equals(status);
                        allCompleted &= GdprExportStatus.COMPLETED.name().equals(status);
                    }
                }
            }
            if (anyFailed) {
                try (PreparedStatement ps = c.prepareStatement(
                        "update platform.gdpr_export_job"
                                + " set status = ?, error = ?, completed_at = ?, updated_at = ?"
                                + " where id = ? and status not in ('COMPLETED','FAILED')")) {
                    ps.setString(1, GdprExportStatus.FAILED.name());
                    ps.setString(2, "export fallito per il servizio " + result.appId());
                    ps.setObject(3, OffsetDateTime.now());
                    ps.setObject(4, OffsetDateTime.now());
                    ps.setObject(5, jobId);
                    ps.executeUpdate();
                }
            }
            c.commit();
            if (anyFailed) {
                return Outcome.JOB_FAILED;
            }
            return allCompleted ? Outcome.ALL_COMPLETED : Outcome.UPDATED;
        } catch (SQLException | JsonProcessingException e) {
            throw new RuntimeException("aggiornamento export job " + jobId + " fallito", e);
        }
    }

    /** Frammenti del job ({@code app_id → fragment_key}), per l'assemblaggio dello ZIP. */
    public Map<String, String> fragments(UUID jobId) {
        Map<String, String> fragments = new LinkedHashMap<>();
        try (Connection c = ds.getConnection();
                PreparedStatement ps = c.prepareStatement(
                        "select app_id, fragment_key from platform.gdpr_export_job_item"
                                + " where job_id = ? order by app_id")) {
            ps.setObject(1, jobId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    fragments.put(rs.getString(1), rs.getString(2));
                }
            }
            return fragments;
        } catch (SQLException e) {
            throw new RuntimeException("lettura frammenti export job " + jobId + " fallita", e);
        }
    }

    /** Marca il job COMPLETED con la chiave dello ZIP aggregato. */
    public void markCompleted(UUID jobId, String zipKey) {
        try (Connection c = ds.getConnection();
                PreparedStatement ps = c.prepareStatement(
                        "update platform.gdpr_export_job"
                                + " set status = ?, zip_key = ?, completed_at = ?, updated_at = ?"
                                + " where id = ? and status not in ('COMPLETED','FAILED')")) {
            ps.setString(1, GdprExportStatus.COMPLETED.name());
            ps.setString(2, zipKey);
            ps.setObject(3, OffsetDateTime.now());
            ps.setObject(4, OffsetDateTime.now());
            ps.setObject(5, jobId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("chiusura export job " + jobId + " fallita", e);
        }
    }
}
