package app.appgrove.commons.audit;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.Map;
import java.util.TreeMap;
import org.jboss.logging.Logger;
import org.jboss.logmanager.MDC;

/**
 * Eventi audit/sicurezza come log strutturati (UC 0006, #08 28-29): un record INFO sul logger
 * {@code appgrove.audit} con la chiave MDC {@code log_type=audit}, che il subscription filter
 * Terraform ({@code { $.mdc.log_type = "audit" }}) instrada verso l'archivio 12 mesi
 * (Firehose → S3 → Glacier). Gli identificativi di contesto (tenant/user/correlation) arrivano
 * dall'MDC di richiesta ({@code MdcRequestFilter}); per i flussi fuori richiesta (job, code)
 * vanno passati nei {@code details}.
 *
 * <p><b>Niente dati personali in chiaro</b> (#08/5): i {@code details} devono contenere SOLO
 * identificativi opachi (UUID, slug, subject/sub, valori enum) — mai email, nomi, testo libero
 * inserito dall'utente. L'archivio conserva 12 mesi: ripulire dopo è costoso.
 */
@ApplicationScoped
public class AuditLogger {

    /** Chiave MDC che marca il record come audit (pattern del subscription filter). */
    public static final String LOG_TYPE_KEY = "log_type";
    /** Valore della chiave {@link #LOG_TYPE_KEY} per gli eventi audit. */
    public static final String LOG_TYPE_AUDIT = "audit";
    /** Nome del logger dedicato agli eventi audit. */
    public static final String LOGGER_NAME = "appgrove.audit";

    private static final Logger LOG = Logger.getLogger(LOGGER_NAME);

    /**
     * Registra un evento audit: {@code action} in notazione puntata (es. {@code gdpr.export.requested},
     * {@code tenant.offboarded}, {@code admin.app.status-changed}), esito e dettagli (soli
     * identificativi opachi, chiavi ordinate per output deterministico).
     */
    public void log(String action, AuditOutcome outcome, Map<String, String> details) {
        // log_type vive nell'MDC solo per la durata della chiamata: il valore precedente
        // (se mai presente) viene ripristinato, i log ordinari non devono finire in archivio.
        String previous = MDC.get(LOG_TYPE_KEY);
        MDC.put(LOG_TYPE_KEY, LOG_TYPE_AUDIT);
        try {
            // messaggio già composto (niente format parametrico): il testo in archivio è letterale
            LOG.info(action + " outcome=" + outcome + format(details));
        } finally {
            if (previous == null) {
                MDC.remove(LOG_TYPE_KEY);
            } else {
                MDC.put(LOG_TYPE_KEY, previous);
            }
        }
    }

    /** Scorciatoia per l'esito più comune. */
    public void success(String action, Map<String, String> details) {
        log(action, AuditOutcome.SUCCESS, details);
    }

    private static String format(Map<String, String> details) {
        if (details == null || details.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        new TreeMap<>(details).forEach((key, value) -> {
            if (value != null) {
                sb.append(' ').append(key).append('=').append(value);
            }
        });
        return sb.toString();
    }
}
