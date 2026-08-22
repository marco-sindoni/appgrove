package app.appgrove.core;

import app.appgrove.core.billing.seats.SeatPricingLoader;
import app.appgrove.core.catalog.PricingSyncService;
import app.appgrove.core.gdpr.AppOffboarding;
import app.appgrove.core.legal.LegalVersionSyncService;
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
 *   <li>{@code sync-legal} — sync delle versioni legali una tantum e termina (UC 0056): legge i frontmatter di
 *       {@code content/legal/} e riconcilia {@code platform.legal_version}. La pipeline la invoca <b>dopo il
 *       migrate</b> al deploy dei legali; in locale/test gira allo startup ({@code appgrove.legal.sync-on-startup});</li>
 *   <li>{@code seed-seat-pricing} — crea la <b>prima</b> versione del listino dei posti dal file di risorse
 *       {@code pricing/seats.yaml}, se non esiste ancora, e termina (UC 0102). La pipeline la invoca
 *       <b>dopo il migrate</b>; in locale/test gira allo startup
 *       ({@code appgrove.seat-pricing.seed-on-startup}). <b>Semina, non sincronizza</b>: se una versione
 *       esiste già non scrive nulla, perché dal primo cambio di tariffa da console (UC 0105) la verità è
 *       la banca dati e risincronizzare dal file annullerebbe quel cambio;</li>
 *   <li>{@code migrate} — applica le migrazioni Flyway (schema {@code platform}) e termina; è il task ECS
 *       one-shot in VPC della pipeline (UC 0005, #07 14/15: {@code build → test → migrate → deploy}),
 *       connessione diretta Agroal (il Proxy è solo per le Lambda, #05 dec.3).</li>
 *   <li>{@code offboard-app <app_id>} — dismissione dati di un'app: accoda la purge per ogni tenant
 *       dell'app e termina (skill {@code drop-application}, UC 0048). È l'atto <b>irreversibile</b>
 *       della dismissione: lanciato a mano/CI dal runbook quando la dismissione è confermata, mai
 *       dalla skill (change 0043).</li>
 * </ul>
 */
@QuarkusMain
public class CoreMain implements QuarkusApplication {

    private static final Logger LOG = Logger.getLogger(CoreMain.class);

    static final String SYNC_PRICING = "sync-pricing";
    static final String SYNC_LEGAL = "sync-legal";
    static final String SEED_SEAT_PRICING = "seed-seat-pricing";
    static final String MIGRATE = "migrate";
    static final String OFFBOARD_APP = "offboard-app";

    @Inject
    PricingSyncService pricingSync;

    @Inject
    LegalVersionSyncService legalSync;

    @Inject
    SeatPricingLoader seatPricingLoader;

    @Inject
    Flyway flyway;

    @Inject
    AppOffboarding appOffboarding;

    @Override
    public int run(String... args) {
        if (args.length > 0 && SYNC_PRICING.equals(args[0])) {
            PricingSyncService.Report report = pricingSync.sync();
            LOG.infof(
                    "sync-pricing completata: apps=%d tiers=%d prices=%d archived=%d",
                    report.apps(), report.tiers(), report.prices(), report.archived());
            return 0;
        }
        if (args.length > 0 && SYNC_LEGAL.equals(args[0])) {
            LegalVersionSyncService.Report report = legalSync.sync();
            LOG.infof("sync-legal completata: componenti=%d", report.components());
            return 0;
        }
        if (args.length > 0 && SEED_SEAT_PRICING.equals(args[0])) {
            boolean created = seatPricingLoader.ensureInitialVersion();
            LOG.infof(
                    "seed-seat-pricing completata: %s",
                    created ? "prima versione del listino dei posti creata dal file" : "listino già presente");
            return 0;
        }
        if (args.length > 0 && MIGRATE.equals(args[0])) {
            var result = flyway.migrate();
            LOG.infof(
                    "migrate completata: %d migrazioni applicate (schema %s)",
                    result.migrationsExecuted, String.join(",", flyway.getConfiguration().getSchemas()));
            return 0;
        }
        if (args.length > 0 && OFFBOARD_APP.equals(args[0])) {
            if (args.length < 2 || args[1].isBlank()) {
                LOG.error("offboard-app richiede l'app_id: offboard-app <app_id>");
                return 1;
            }
            String appId = args[1];
            var tenants = appOffboarding.offboardApp(appId, AppOffboarding.REASON_APP_OFFBOARDED);
            LOG.infof("offboard-app completata: app_id=%s tenants=%d (purge accodata)", appId, tenants.size());
            return 0;
        }
        Quarkus.waitForExit();
        return 0;
    }
}
