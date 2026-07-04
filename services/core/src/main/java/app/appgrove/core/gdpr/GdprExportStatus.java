package app.appgrove.core.gdpr;

/** Stato del job di export (#13 D22) e dei suoi item per-servizio. */
public enum GdprExportStatus {
    QUEUED,
    RUNNING,
    COMPLETED,
    FAILED
}
