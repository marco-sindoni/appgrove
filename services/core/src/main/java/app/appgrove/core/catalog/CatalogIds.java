package app.appgrove.core.catalog;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * ID <b>deterministici</b> del catalogo, derivati dalla <b>chiave interna stabile</b> (UC 0022, #09 H37).
 * Stesso pricing-as-code → stessi UUID in ogni ambiente: il loader può fare upsert idempotente e il seed
 * (subscription) può referenziare il catalogo con FK stabili senza dipendere dall'ordine di creazione.
 *
 * <p>Algoritmo: UUID name-based (versione 3, {@link UUID#nameUUIDFromBytes}) sul nome
 * {@code "<tipo>:<chiavi>"}. Replicabile altrove (es. il seed.sql) con lo stesso schema di nomi.
 */
public final class CatalogIds {

    private CatalogIds() {}

    /** {@code app:<slug>} */
    public static UUID appId(String slug) {
        return det("app:" + slug);
    }

    /** {@code tier:<slug>:<tierKey>} */
    public static UUID tierId(String slug, String tierKey) {
        return det("tier:" + slug + ":" + tierKey);
    }

    /** {@code price:<slug>:<tierKey>:<billingCycle>} */
    public static UUID priceId(String slug, String tierKey, String billingCycle) {
        return det("price:" + slug + ":" + tierKey + ":" + billingCycle);
    }

    private static UUID det(String name) {
        return UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8));
    }
}
