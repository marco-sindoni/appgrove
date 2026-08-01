package app.appgrove.core.catalog;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Contratto della <b>vetrina per questo account</b> (UC 0095): quali app esistono, come si presentano e
 * in che stato sono per chi sta chiedendo. Nessun identificativo di account entra o esce da qui — il
 * tenant viene dal token verificato e basta (invariante #1).
 */
public final class CatalogDtos {

    private CatalogDtos() {}

    /** L'intera vetrina, nell'ordine deciso dal read-model. */
    public record CatalogView(List<CatalogAppView> apps) {}

    /**
     * Una app della vetrina.
     *
     * @param appSlug chiave stabile dell'app, la stessa degli entitlement e delle rotte dei moduli
     * @param name nome commerciale
     * @param category tinta/icona di categoria dichiarata a listino; {@code null} se non dichiarata (il
     *     frontend ne deriva una stabile dallo slug)
     * @param descriptions descrizione breve per lingua; il frontend sceglie quella attiva e ripiega
     *     sull'inglese. Servite tutte insieme perché il cambio lingua non debba ricaricare il catalogo
     * @param state stato della card per questo account
     * @param planName nome della fascia in corso, quando ce n'è una (abbonamento o baseline gratuita)
     * @param trialEndsAt fine della prova, valorizzato solo in {@code trial}
     * @param cancelAt fine dell'accesso per disdetta programmata, valorizzato solo in
     *     {@code cancellation_scheduled}
     * @param startingPrice prezzo di partenza; assente per un'app con sole fasce gratuite
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record CatalogAppView(
            String appSlug,
            String name,
            String category,
            Map<String, String> descriptions,
            CatalogAppState state,
            String planName,
            Instant trialEndsAt,
            Instant cancelAt,
            StartingPrice startingPrice) {}

    /** Il prezzo più basso da cui si parte, in unità minori (centesimi), come il resto del listino. */
    public record StartingPrice(int amount, String currency, String billingCycle) {}
}
