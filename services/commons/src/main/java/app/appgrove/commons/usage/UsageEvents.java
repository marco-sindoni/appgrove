package app.appgrove.commons.usage;

/**
 * Convenzioni del canale con cui un'app dichiara al core il proprio <b>uso a giacenza</b> (UC 0054).
 *
 * <p><b>A cosa serve.</b> Il gate del downgrade (UC 0028, {@code TierChangePolicy.evaluateDowngrade})
 * deve sapere quanti oggetti "a giacenza" — per il mini-CRM: quanti posti occupati — esistono ORA per
 * un tenant, per rifiutare il passaggio a un piano con un tetto inferiore. Quel dato è <b>applicativo</b>
 * e vive nello schema dell'app, non in core: farlo leggere a core con una chiamata sincrona
 * significherebbe insegnare a core gli indirizzi di rete di ogni app e introdurre una dipendenza
 * sincrona nella direzione opposta a quella che UC 0046 ha appena rimosso.
 *
 * <p><b>Quindi il verso è invertito e asincrono.</b> È l'app che, quando la propria giacenza cambia,
 * pubblica "il tenant T dell'app A usa N unità della metrica M"; core la materializza in un read-model
 * e la legge quando serve valutare un downgrade. Simmetrico alla coda condivisa dei risultati di export
 * GDPR ({@code GdprQueues}): molti produttori (le app), un solo consumatore (core).
 *
 * <p><b>La coda è unica e condivisa</b>, non una per app: il consumatore è sempre e solo core, e il
 * messaggio porta con sé lo slug dell'app, quindi una coda per app aggiungerebbe solo nomi da scoprire
 * senza alcun vantaggio.
 */
public final class UsageEvents {

    private UsageEvents() {}

    /**
     * Coda condivisa dei report d'uso app → core. Dichiarata in {@code dev/elasticmq.conf} (locale) e in
     * {@code platform_shared} (cloud), come la coda dei risultati di export.
     */
    public static final String USAGE_QUEUE = "app-usage";

    /**
     * Report d'uso a giacenza di un tenant per una metrica di un'app.
     *
     * <p>{@code tenantId} proviene <b>sempre</b> dallo stato dell'app (la giacenza appena contata dentro
     * una richiesta autenticata o un'operazione già validata), mai da input client non autenticato: il
     * consumer gira in core fuori da una richiesta con token e si fida del mittente per il solo fatto che
     * ha potuto scrivere sulla coda interna.
     *
     * @param appSlug slug di catalogo dell'app che riporta (es. {@code crm})
     * @param tenantId account il cui uso è cambiato
     * @param metric metrica a giacenza (es. {@code seats})
     * @param value giacenza attuale: quante unità esistono ORA (mai negativa)
     * @param occurredAt istante della misura (diagnostica del ritardo di propagazione)
     */
    public record UsageReport(String appSlug, String tenantId, String metric, long value, String occurredAt) {}
}
