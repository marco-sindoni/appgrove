package app.appgrove.commons.gdpr;

/**
 * Contratto SPI GDPR per-app (#13 L69/L70): ogni app espone export ed erasure dei dati personali
 * di un tenant. Invocato dal framework export/erasure del core (UC 0032). L'implementazione opera
 * su <b>tenant esplicito</b> ({@link GdprScope}) perché gira anche fuori da una richiesta utente.
 */
public interface AppDataContract {

    /** Identificativo dell'app (es. {@code "fatture"}). */
    String appId();

    /** Esporta tutti i dati personali del tenant (con step di progress). */
    ExportResult exportData(GdprScope scope);

    /** Cancella fisicamente tutti i dati del tenant, senza lasciare orfani. */
    PurgeResult purgeData(GdprScope scope);

    /** Manifesto dati dell'app (campi personali + classificazione). */
    DataManifest manifest();
}
