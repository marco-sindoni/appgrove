package app.appgrove.core.platform;

/** Stato di un account (tenant). Persistito come stringa (#05 7). */
public enum AccountStatus {
    active,
    suspended,
    /** Eliminazione richiesta (UC 0033): disattivato subito, hard-purge a fine grace (#13 E25). */
    pending_deletion
}
