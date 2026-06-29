package app.appgrove.commons.entitlement;

/**
 * Accesso non abilitato per l'app: gate entitlement non superato (gate 3, #09 dec.30). Mappata a
 * <b>402 Payment Required</b> problem+json da {@code EntitlementRequiredMapper}. Messaggio azionabile
 * ("riattiva o esporta i tuoi dati", #09 F31 — i diritti GDPR restano comunque esercitabili).
 */
public class EntitlementRequiredException extends RuntimeException {

    private final String appSlug;

    public EntitlementRequiredException(String appSlug) {
        super("Abbonamento richiesto per l'app '" + appSlug
                + "': riattiva l'abbonamento oppure esporta/elimina i tuoi dati.");
        this.appSlug = appSlug;
    }

    public String getAppSlug() {
        return appSlug;
    }
}
