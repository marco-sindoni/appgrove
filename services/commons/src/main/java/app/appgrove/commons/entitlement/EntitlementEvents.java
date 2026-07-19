package app.appgrove.commons.entitlement;

/**
 * Convenzioni del canale di <b>invalidazione</b> degli entitlement (UC 0046): una coda per servizio,
 * simmetrica alle code GDPR ({@code GdprQueues}).
 *
 * <p><b>L'evento è sottile di proposito.</b> Non trasporta i diritti calcolati, ma solo la notizia
 * "i diritti del tenant T sono cambiati". Il ricalcolo resta in <b>un solo posto</b> (il read-model
 * di core, letto dentro una richiesta autenticata tramite la rete di sicurezza): un evento grasso
 * imporrebbe a core di ri-derivare gli entitlement fuori da una richiesta — duplicando la logica e
 * aggirando il filtro per tenant — con il rischio, molto peggiore di una chiamata in più, che le due
 * derivazioni divergano nel tempo.
 */
public final class EntitlementEvents {

    private EntitlementEvents() {}

    /**
     * Coda di invalidazione del servizio {@code appId} (es. {@code entitlement-fatture}). Dichiarata
     * in {@code dev/elasticmq.conf} (locale) e nel modulo Terraform {@code microsaas_app} (cloud).
     */
    public static String invalidationQueue(String appId) {
        return "entitlement-" + appId;
    }

    /**
     * Messaggio di invalidazione: identifica il <b>tenant</b> i cui diritti sono cambiati e perché.
     *
     * <p>{@code tenantId} proviene dallo stato di core (payload webhook firmato o mutazione
     * applicata da core), <b>mai</b> da input client non autenticato: il consumer gira fuori da una
     * richiesta con JWT e non ha altro modo di sapere di chi si tratta (stesso ragionamento del
     * consumer di purge GDPR).
     *
     * @param tenantId tenant i cui diritti sono cambiati
     * @param reason causa (es. {@code subscription.updated}, {@code account.pending_deletion}) — solo
     *     per diagnostica: il consumer non differenzia il comportamento in base alla causa
     * @param occurredAt istante dell'evento a monte (diagnostica del ritardo di propagazione)
     */
    public record InvalidationMessage(String tenantId, String reason, String occurredAt) {}
}
