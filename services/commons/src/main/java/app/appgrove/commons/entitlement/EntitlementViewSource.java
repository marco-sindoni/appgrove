package app.appgrove.commons.entitlement;

import java.util.Optional;

/**
 * Capacità opzionale di una {@link EntitlementService} di esporre la <b>vista completa</b> di un'app
 * (tier, fase, tetti), non solo le sue proiezioni scalari.
 *
 * <p>Serve alla proiezione locale (UC 0046) per <b>salvare ciò che ha letto</b> dalla rete di
 * sicurezza: senza questa capacità potrebbe memorizzare solo "ha accesso sì/no", perdendo i tetti di
 * quota e costringendo a una seconda chiamata. È tenuta separata da {@link EntitlementService}
 * perché non tutte le implementazioni hanno una vista da esporre (una sorgente puramente booleana
 * resta legittima).
 */
public interface EntitlementViewSource {

    /** Vista dell'app per il tenant corrente; vuoto = nessun accesso. */
    Optional<EntitlementView> viewFor(String appSlug);
}
