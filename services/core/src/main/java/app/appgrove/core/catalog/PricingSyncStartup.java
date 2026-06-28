package app.appgrove.core.catalog;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * In {@code %dev}/{@code %test} esegue la sync pricing-as-code <b>allo startup</b> ({@code appgrove.pricing.sync-on-startup}),
 * così il catalogo locale è sempre = pricing-as-code senza passi manuali (invariante CLAUDE.md "Avvio locale").
 * In prod resta OFF: la sync gira come step di deploy via l'entrypoint command-mode {@code sync-pricing}
 * (cablaggio CI a UC 0005). Idempotente: ri-avviare l'app non altera lo stato.
 */
@ApplicationScoped
public class PricingSyncStartup {

    private static final Logger LOG = Logger.getLogger(PricingSyncStartup.class);

    @Inject
    PricingSyncService sync;

    @ConfigProperty(name = "appgrove.pricing.sync-on-startup", defaultValue = "false")
    boolean syncOnStartup;

    void onStart(@Observes StartupEvent event) {
        if (!syncOnStartup) {
            return;
        }
        PricingSyncService.Report report = sync.sync();
        LOG.infof(
                "pricing.sync@startup apps=%d tiers=%d prices=%d archived=%d",
                report.apps(), report.tiers(), report.prices(), report.archived());
    }
}
