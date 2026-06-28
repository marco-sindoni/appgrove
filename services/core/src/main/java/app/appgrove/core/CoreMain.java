package app.appgrove.core;

import app.appgrove.core.catalog.PricingSyncService;
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

/**
 * Entrypoint del servizio core. Default: avvia il server HTTP ({@code waitForExit}). In <b>command-mode</b>
 * {@code sync-pricing} esegue la sync pricing-as-code una tantum e termina — è ciò che la pipeline (UC 0005)
 * invocherà <b>dopo il Flyway migrate</b> (deploy test → sync sandbox, tag→prod → sync production, #09 H37) e
 * che il flusso {@code dev seed} esegue in locale prima di caricare le subscription del seed.
 */
@QuarkusMain
public class CoreMain implements QuarkusApplication {

    private static final Logger LOG = Logger.getLogger(CoreMain.class);

    static final String SYNC_PRICING = "sync-pricing";

    @Inject
    PricingSyncService pricingSync;

    @Override
    public int run(String... args) {
        if (args.length > 0 && SYNC_PRICING.equals(args[0])) {
            PricingSyncService.Report report = pricingSync.sync();
            LOG.infof(
                    "sync-pricing completata: apps=%d tiers=%d prices=%d archived=%d",
                    report.apps(), report.tiers(), report.prices(), report.archived());
            return 0;
        }
        Quarkus.waitForExit();
        return 0;
    }
}
