package app.appgrove.commons.tenancy;

/**
 * Sollevata quando non è possibile risolvere {@code tenant_id} dal JWT verificato.
 * Rende esecutivo il fail-closed: nessun default tenant, accesso negato (→ 403 via mapper).
 */
public class TenantNotResolvedException extends RuntimeException {

    public TenantNotResolvedException() {
        super("tenant_id assente nel token: accesso negato (fail-closed)");
    }
}
