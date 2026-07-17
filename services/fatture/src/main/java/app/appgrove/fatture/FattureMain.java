package app.appgrove.fatture;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import jakarta.inject.Inject;
import org.flywaydb.core.Flyway;
import org.jboss.logging.Logger;

/**
 * Entrypoint del servizio fatture. Default: avvia il server HTTP ({@code waitForExit}). In
 * <b>command-mode</b> {@code migrate} applica le migrazioni Flyway (schema {@code app_fatture}) e
 * termina — è il task ECS one-shot in VPC della pipeline (UC 0005, #07 14/15: {@code build → test →
 * migrate → deploy}), connessione diretta Agroal (il Proxy è solo per le Lambda, #05 dec.3). Stesso
 * pattern di {@code CoreMain}.
 */
@QuarkusMain
public class FattureMain implements QuarkusApplication {

    private static final Logger LOG = Logger.getLogger(FattureMain.class);

    static final String MIGRATE = "migrate";

    @Inject
    Flyway flyway;

    @Override
    public int run(String... args) {
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
