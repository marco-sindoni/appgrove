package app.appgrove.fatture;

import app.appgrove.commons.quota.QuotaLimitSource;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Sorgente del tetto di quota <b>config-driven</b> (locale, UC 0051): il tetto della metrica
 * {@code fatture} arriva dalla configurazione ({@code app.fatture.quota.fatture.cap}, default 10).
 * <p><b>Decisione differita (UC 0027)</b>: la risoluzione del tetto dall'entitlement
 * (subscription → tier → {@code app_tier.limits}) sostituirà questa implementazione senza toccare il
 * resto del codice. Vedi i punti aperti di UC 0027.
 */
@ApplicationScoped
public class ConfigQuotaLimitSource implements QuotaLimitSource {

    @ConfigProperty(name = "app.fatture.quota.fatture.cap", defaultValue = "10")
    long fattureCap;

    @Override
    public long capFor(String tenantId, String metric) {
        if ("fatture".equals(metric)) {
            return fattureCap;
        }
        return -1; // metrica sconosciuta → nessun limite applicato qui
    }
}
