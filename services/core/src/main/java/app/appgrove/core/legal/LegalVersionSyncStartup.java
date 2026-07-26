package app.appgrove.core.legal;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * In {@code %dev}/{@code %test} esegue la sync delle versioni legali <b>allo startup</b>
 * ({@code appgrove.legal.sync-on-startup}), così in locale/test {@code legal_version} è sempre allineata
 * ai frontmatter di {@code content/legal/} senza passi manuali (come {@link app.appgrove.core.catalog.PricingSyncStartup}).
 * In prod resta OFF: la sync gira come step di deploy via l'entrypoint command-mode {@code sync-legal}. Idempotente.
 */
@ApplicationScoped
public class LegalVersionSyncStartup {

    private static final Logger LOG = Logger.getLogger(LegalVersionSyncStartup.class);

    @Inject
    LegalVersionSyncService sync;

    @ConfigProperty(name = "appgrove.legal.sync-on-startup", defaultValue = "false")
    boolean syncOnStartup;

    void onStart(@Observes StartupEvent event) {
        if (!syncOnStartup) {
            return;
        }
        LegalVersionSyncService.Report report = sync.sync();
        LOG.infof("legal.sync@startup componenti=%d", report.components());
    }
}
