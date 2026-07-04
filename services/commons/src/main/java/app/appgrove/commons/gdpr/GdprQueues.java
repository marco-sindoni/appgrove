package app.appgrove.commons.gdpr;

/**
 * Convenzioni di naming del messaging GDPR (UC 0032): una coda export e una coda purge <b>per
 * servizio</b> (simmetria con #06 H-19) più la coda risultati condivisa consumata dal core.
 * Gli stessi nomi sono dichiarati in {@code dev/elasticmq.conf} (locale) e — quando l'infra
 * nascerà — nel Terraform (UC 0003/0004, vedi "Punti aperti" di quegli use case).
 */
public final class GdprQueues {

    /** Coda condivisa dei risultati di export, consumata dal core (aggregazione job). */
    public static final String EXPORT_RESULTS = "gdpr-export-results";

    private GdprQueues() {}

    /** Coda delle richieste di export del servizio {@code appId} (es. {@code gdpr-export-fatture}). */
    public static String exportQueue(String appId) {
        return "gdpr-export-" + appId;
    }

    /** Coda purge per-tenant del servizio {@code appId} (es. {@code tenant-purge-fatture}, #06 H). */
    public static String purgeQueue(String appId) {
        return "tenant-purge-" + appId;
    }

    /** Chiave S3 del frammento di export prodotto da {@code appId} per il job {@code jobId}. */
    public static String fragmentKey(String jobId, String appId) {
        return "jobs/" + jobId + "/" + appId + ".json";
    }

    /** Chiave S3 dello ZIP finale del job {@code jobId} (aggregato dal core). */
    public static String zipKey(String jobId) {
        return "jobs/" + jobId + "/export.zip";
    }
}
