package app.appgrove.commons.quota;

import app.appgrove.commons.entitlement.EntitlementService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Implementazione <b>reale</b> di {@link QuotaLimitSource} (UC 0027): risolve il tetto della metrica
 * <b>dall'entitlement</b> ({@code subscription → app_tier.limits}) via {@link EntitlementService},
 * sostituendo le implementazioni config-driven locali (UC 0051). Vive in {@code commons}: ogni app la
 * eredita senza codice, identificandosi col proprio {@code quarkus.application.name} (= slug).
 *
 * <p>Il {@code tenantId} della firma è ignorato di proposito: l'entitlement è derivato dal <b>JWT
 * verificato</b> nella chiamata a core (invariante #1), non da un parametro.
 */
@ApplicationScoped
public class EntitlementQuotaLimitSource implements QuotaLimitSource {

    @Inject
    EntitlementService entitlements;

    @ConfigProperty(name = "quarkus.application.name")
    String appSlug;

    @Override
    public long capFor(String tenantId, String metric) {
        return entitlements.capFor(appSlug, metric);
    }
}
