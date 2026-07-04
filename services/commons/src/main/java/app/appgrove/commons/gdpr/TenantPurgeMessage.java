package app.appgrove.commons.gdpr;

/**
 * Messaggio sulla coda {@code tenant-purge-<app_id>} (#06 H-19): il servizio destinatario invoca il
 * proprio {@code purgeData} per il tenant. In locale il publisher del core invia direttamente alle
 * code; nel cloud lo stesso messaggio arriva dal bus EventBridge (evento {@code tenant.offboarded},
 * regola → code per-servizio, UC 0004). {@code reason} finisce nell'audit (prova #13 L70).
 */
public record TenantPurgeMessage(String tenantId, String reason) {

    /** Motivo canonico dell'offboarding account (eliminazione account, #13 L71). */
    public static final String REASON_OFFBOARDED = "tenant.offboarded";
}
