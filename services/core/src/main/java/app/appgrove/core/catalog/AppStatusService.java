package app.appgrove.core.catalog;

import app.appgrove.commons.audit.AuditLogger;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jboss.logging.Logger;

/**
 * Leva <b>reversibile</b> di disponibilità di un'app di catalogo (UC 0076): porta {@code app.status}
 * da {@code active} a {@code inactive} e ritorno, lasciando in {@code platform.app_status_audit} la
 * prova di chi ha agito, quando e perché.
 *
 * <p><b>Cosa NON fa</b>, e va tenuto ben distinto: non cancella dati, non tocca abbonamenti, non
 * modifica infrastruttura. La dismissione definitiva di un'app è tutt'altra cosa (UC 0048, skill
 * {@code drop-application}). Non applica nemmeno la regola a valle: l'esclusione dell'app dagli
 * entitlement effettivi è già di {@link app.appgrove.core.billing.EntitlementAccess} (UC 0027/0077),
 * che scarta ogni app non {@code active}. Questo servizio <b>alimenta</b> quella regola, non la duplica.
 *
 * <p><b>Idempotenza.</b> Chiedere lo stato in cui l'app già si trova non è un errore: non si scrive
 * il catalogo e — soprattutto — non si scrive una riga di registro. Una riga di audit senza
 * transizione sarebbe una prova falsa, ed è esattamente ciò che lo use case vieta.
 *
 * <p><b>Motivazione.</b> Testo libero dell'operatore: vive solo nella riga di database (conservazione
 * 12 mesi, {@code AuditRetentionSweeper}) e non entra mai nell'evento di {@link AuditLogger}, che
 * finisce nell'archivio a 12 mesi e ammette soli identificativi opachi (#08/5).
 *
 * <p>Scrive con SQL nativo, come già faceva la console: {@link App} è il read-model del catalogo
 * (le scritture di sincronizzazione passano da {@link PricingSyncService}) e non espone modificatori.
 */
@ApplicationScoped
public class AppStatusService {

    /** Lunghezza massima della motivazione (allineata alla colonna {@code reason}). */
    public static final int REASON_MAX_LENGTH = 512;

    private static final Logger LOG = Logger.getLogger(AppStatusService.class);

    /**
     * Esito del comando: l'app <b>dopo</b> il comando, più l'informazione se c'è stata davvero una
     * transizione ({@code false} = comando idempotente, nessuna scrittura, nessun audit).
     */
    public record Result(
            UUID appId, String slug, String name, String userModel, AppStatus status, boolean changed) {}

    /** Riga del registro delle transizioni, con l'app già risolta per la lettura umana. */
    public record AuditEntry(
            UUID id,
            UUID appId,
            String appSlug,
            String appName,
            AppStatus fromStatus,
            AppStatus toStatus,
            String actor,
            String reason,
            Instant executedAt) {}

    @Inject
    EntityManager em;

    @Inject
    AuditLogger auditLog;

    /**
     * Porta l'app allo stato richiesto. Ritorna {@code null} se l'app non esiste (o è cancellata):
     * la traduzione in risposta di errore è del chiamante, che conosce il protocollo.
     *
     * @param reason motivazione facoltativa dell'operatore ({@code null} ammesso), troncata a
     *     {@value #REASON_MAX_LENGTH} caratteri per difendere la colonna anche se un chiamante
     *     aggirasse la validazione del corpo
     * @param actor identificativo dell'operatore di piattaforma ({@code sub} del JWT verificato)
     */
    @Transactional
    public Result setStatus(UUID appId, AppStatus target, String reason, String actor) {
        Object[] before = appRow(appId);
        if (before == null) {
            return null;
        }
        AppStatus current = AppStatus.valueOf((String) before[4]);
        if (current == target) {
            // Idempotente: nessuna scrittura sul catalogo, nessuna riga di registro.
            LOG.debugf("admin.app.status-unchanged app_id=%s status=%s actor=%s", appId, target, actor);
            return result(before, current, false);
        }

        em.createNativeQuery("update platform.app set status = :status, updated_at = now(), updated_by = :actor"
                        + " where id = :id and deleted_at is null")
                .setParameter("status", target.name())
                .setParameter("actor", actor)
                .setParameter("id", appId)
                .executeUpdate();

        em.createNativeQuery("insert into platform.app_status_audit"
                        + " (id, app_id, from_status, to_status, actor, reason, executed_at)"
                        + " values (:id, :appId, :from, :to, :actor, :reason, :at)")
                .setParameter("id", UUID.randomUUID())
                .setParameter("appId", appId)
                .setParameter("from", current.name())
                .setParameter("to", target.name())
                .setParameter("actor", actor)
                .setParameter("reason", truncate(reason))
                .setParameter("at", Instant.now())
                .executeUpdate();

        // Logging strutturato dell'azione admin (invariante #4): app_id + transizione + attore.
        LOG.infof("admin.app.status-changed app_id=%s from=%s to=%s actor=%s", appId, current, target, actor);
        // Evento audit (UC 0006): la transizione cambia l'entitlement effettivo di TUTTI gli account.
        // Soli identificativi opachi — la motivazione NON entra qui (può contenere testo libero).
        auditLog.success("admin.app.status-changed", Map.of(
                "app_id", appId.toString(),
                "from_status", current.name(),
                "status", target.name(),
                "actor", actor));

        return result(before, target, true);
    }

    /** Registro delle transizioni, più recenti prima. Lettura di piattaforma (gated dal chiamante). */
    @SuppressWarnings("unchecked")
    public List<AuditEntry> auditTrail() {
        List<Object[]> rows = em.createNativeQuery("""
                select a.id, a.app_id, app.slug, app.name, a.from_status, a.to_status, a.actor, a.reason,
                       a.executed_at
                from platform.app_status_audit a
                join platform.app app on app.id = a.app_id
                order by a.executed_at desc, a.id desc
                """)
                .getResultList();
        return rows.stream()
                .map(r -> new AuditEntry(
                        (UUID) r[0],
                        (UUID) r[1],
                        (String) r[2],
                        (String) r[3],
                        AppStatus.valueOf((String) r[4]),
                        AppStatus.valueOf((String) r[5]),
                        (String) r[6],
                        (String) r[7],
                        (Instant) r[8]))
                .toList();
    }

    // ── helper ───────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private Object[] appRow(UUID appId) {
        List<Object[]> rows = em.createNativeQuery(
                        "select id, slug, name, user_model, status from platform.app"
                                + " where id = :id and deleted_at is null")
                .setParameter("id", appId)
                .getResultList();
        return rows.isEmpty() ? null : rows.get(0);
    }

    private static Result result(Object[] row, AppStatus status, boolean changed) {
        return new Result((UUID) row[0], (String) row[1], (String) row[2], (String) row[3], status, changed);
    }

    private static String truncate(String reason) {
        if (reason == null) {
            return null;
        }
        String trimmed = reason.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.length() <= REASON_MAX_LENGTH ? trimmed : trimmed.substring(0, REASON_MAX_LENGTH);
    }
}
