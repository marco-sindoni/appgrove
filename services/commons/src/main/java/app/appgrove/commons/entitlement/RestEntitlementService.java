package app.appgrove.commons.entitlement;

import app.appgrove.commons.quota.QuotaNature;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.rest.client.inject.RestClient;

/**
 * Implementazione di {@link EntitlementService} che legge il read-model da <b>core</b> via
 * {@link EntitlementClient}, propagando il JWT del chiamante (invariante #1). {@code @RequestScoped}:
 * il risultato è <b>memoizzato per richiesta</b> (più gate/quota nella stessa richiesta = una sola
 * chiamata a core).
 *
 * <p><b>Da UC 0027 a UC 0046: da percorso principale a rete di sicurezza.</b> Questa era
 * l'implementazione predefinita, invocata a ogni gate di ogni richiesta di ogni app — un fan-in
 * sincrono che metteva core sul percorso caldo. Ora è qualificata {@link SafetyNet} e il bean
 * predefinito è {@code ProjectedEntitlementService}, che legge la proiezione locale dell'app e
 * ricade qui <b>solo</b> quando la proiezione non basta a decidere (riga assente o da rinfrescare).
 * Il codice di dominio delle app non è cambiato: continua a iniettare {@code EntitlementService}.
 */
@RequestScoped
@SafetyNet
public class RestEntitlementService implements EntitlementService, EntitlementViewSource {

    @Inject
    @RestClient
    EntitlementClient client;

    @Inject
    JsonWebToken jwt;

    private MeEntitlementsView cache;

    /** Lettura memoizzata per-richiesta del read-model. {@code protected} per i test (override). */
    protected MeEntitlementsView entitlements() {
        if (cache == null) {
            cache = client.getMyEntitlements("Bearer " + jwt.getRawToken());
        }
        return cache;
    }

    @Override
    public Optional<EntitlementView> viewFor(String appSlug) {
        return find(appSlug);
    }

    private Optional<EntitlementView> find(String appSlug) {
        List<EntitlementView> all = entitlements().entitlements();
        if (all == null) {
            return Optional.empty();
        }
        return all.stream().filter(e -> e.appSlug() != null && e.appSlug().equals(appSlug)).findFirst();
    }

    private Optional<MetricLimit> limit(String appSlug, String metric) {
        return find(appSlug)
                .map(EntitlementView::limits)
                .filter(l -> l != null)
                .map(l -> l.get(metric));
    }

    @Override
    public boolean hasAccess(String appSlug) {
        return find(appSlug).isPresent();
    }

    @Override
    public long capFor(String appSlug, String metric) {
        return limit(appSlug, metric).map(MetricLimit::cap).orElse(-1L);
    }

    @Override
    public QuotaNature natureOf(String appSlug, String metric) {
        return limit(appSlug, metric)
                .map(MetricLimit::nature)
                .filter(n -> n != null && !n.isBlank())
                .map(n -> QuotaNature.valueOf(n.toUpperCase(Locale.ROOT)))
                .orElse(null);
    }
}
