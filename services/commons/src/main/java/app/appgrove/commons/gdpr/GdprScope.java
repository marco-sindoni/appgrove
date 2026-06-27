package app.appgrove.commons.gdpr;

/**
 * Ambito di un'operazione GDPR per-app. Il {@code tenantId} è <b>esplicito</b> (non derivato dal JWT):
 * export/purge sono orchestrati dal core (UC 0032), tipicamente <b>fuori</b> da una richiesta utente
 * (es. EventBridge purge), quindi il tenant arriva nello scope, non dal discriminator.
 */
public record GdprScope(String tenantId) {

    public GdprScope {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("GdprScope richiede un tenantId");
        }
    }
}
