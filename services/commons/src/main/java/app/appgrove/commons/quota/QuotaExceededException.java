package app.appgrove.commons.quota;

/**
 * Quota della metrica esaurita: hard-limit raggiunto (#09 A6). Mappata a <b>429</b> problem+json
 * da {@code QuotaExceededMapper} nel commons. Porta la metrica e il tetto per il dettaglio dell'errore.
 */
public class QuotaExceededException extends RuntimeException {

    private final String metric;
    private final long cap;

    public QuotaExceededException(String metric, long cap) {
        super("Quota '" + metric + "' esaurita (tetto " + cap + ")");
        this.metric = metric;
        this.cap = cap;
    }

    public String getMetric() {
        return metric;
    }

    public long getCap() {
        return cap;
    }
}
