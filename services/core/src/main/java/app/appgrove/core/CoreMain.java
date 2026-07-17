package app.appgrove.core;

import app.appgrove.core.catalog.PricingSyncService;
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import jakarta.inject.Inject;
import org.flywaydb.core.Flyway;
import org.jboss.logging.Logger;

/**
 * Entrypoint del servizio core. Default: avvia il server HTTP ({@code waitForExit}). In <b>command-mode</b>:
 * <ul>
 *   <li>{@code sync-pricing} — sync pricing-as-code una tantum e termina; la pipeline (UC 0005) la invoca
 *       <b>dopo il Flyway migrate</b> (deploy test → sync sandbox, tag→prod → sync production, #09 H37) e
 *       il flusso {@code dev seed} la esegue in locale prima di caricare le subscription del seed;</li>
 *   <li>{@code migrate} — applica le migrazioni Flyway (schema {@code platform}) e termina; è il task ECS
 *       one-shot in VPC della pipeline (UC 0005, #07 14/15: {@code build → test → migrate → deploy}),
 *       connessione diretta Agroal (il Proxy è solo per le Lambda, #05 dec.3).</li>
 * </ul>
 */
@QuarkusMain
public class CoreMain implements QuarkusApplication {

    private static final Logger LOG = Logger.getLogger(CoreMain.class);

    static final String SYNC_PRICING = "sync-pricing";
    static final String MIGRATE = "migrate";

    @Inject
    PricingSyncService pricingSync;

    @Inject
    Flyway flyway;

    @Override
    public int run(String... args) {
        if (args.length > 0 && SYNC_PRICING.equals(args[0])) {
            PricingSyncService.Report report = pricingSync.sync();
            LOG.infof(
                    "sync-pricing completata: apps=%d tiers=%d prices=%d archived=%d",
                    report.apps(), report.tiers(), report.prices(), report.archived());
            return 0;
        }
        if (args.length > 0 && MIGRATE.equals(args[0])) {
            var result = flyway.migrate();
            LOG.infof(
                    "migrate completata: %d migrazioni applicate (schema %s)",
                    result.migrationsExecuted, String.join(",", flyway.getConfiguration().getSchemas()));
            return 0;
        }
        Quarkus.waitForExit();
        return 0;
    }
}
