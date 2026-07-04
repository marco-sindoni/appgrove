package app.appgrove.commons.gdpr;

import java.util.List;

/**
 * Messaggio sulla coda {@link GdprQueues#EXPORT_RESULTS}: esito dell'export del servizio
 * {@code appId} per il job {@code jobId}. Su successo porta gli <b>step dichiarati</b> dal
 * contratto (#13 D22, progress) e la chiave S3 del frammento; su fallimento l'errore.
 */
public record ExportResultMessage(
        String jobId,
        String appId,
        boolean success,
        List<String> steps,
        String fragmentKey,
        String error) {

    public static ExportResultMessage completed(String jobId, String appId, List<String> steps, String fragmentKey) {
        return new ExportResultMessage(jobId, appId, true, steps, fragmentKey, null);
    }

    public static ExportResultMessage failed(String jobId, String appId, String error) {
        return new ExportResultMessage(jobId, appId, false, List.of(), null, error);
    }
}
