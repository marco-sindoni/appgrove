package app.appgrove.commons.gdpr;

/**
 * Messaggio sulla coda {@code gdpr-export-<app_id>}: il core chiede al servizio {@code appId} di
 * esportare i dati del tenant per il job {@code jobId} (UC 0032). Il {@code tenantId} è esplicito
 * ma <b>derivato dal JWT</b> del richiedente al momento della creazione del job (invariante #1).
 */
public record ExportRequestMessage(String jobId, String tenantId, String appId) {}
