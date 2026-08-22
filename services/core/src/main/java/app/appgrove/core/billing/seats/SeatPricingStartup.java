package app.appgrove.core.billing.seats;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * In {@code %dev}/{@code %test} semina il listino dei posti <b>allo startup</b>
 * ({@code appgrove.seat-pricing.seed-on-startup}), così l'ambiente locale ha il listino senza passi
 * manuali (invariante CLAUDE.md «Avvio locale»). In prod resta <b>OFF</b>: la semina gira come passo di
 * distribuzione, col comando {@code seed-seat-pricing} di {@code CoreMain}, dopo il {@code migrate}.
 *
 * <p><b>Perché OFF in prod, e non «tanto è idempotente».</b> L'artefatto di spedizione deve arrivare in
 * ascolto <b>senza toccare la banca dati</b>: è la regola che lo smoke di avvio (`tools/smoke`) verifica
 * lanciando i servizi nel profilo di spedizione con una banca dati <b>irraggiungibile di proposito</b>. Un
 * bean che scrive allo startup fa fallire quell'avvio — ed è la stessa ragione per cui
 * {@code PricingSyncStartup} e la sincronizzazione dei documenti legali sono gated allo stesso modo. La
 * scoperta è della change 0097: la prima stesura seminava in ogni profilo.
 *
 * <p>È un bean separato dal {@link SeatPricingLoader} anche per una ragione tecnica: il metodo che scrive è
 * {@code @Transactional}, e un osservatore che lo chiamasse su sé stesso salterebbe l'intercettore
 * (auto-invocazione) restando senza transazione. La chiamata fra bean diversi passa dal proxy.
 */
@ApplicationScoped
public class SeatPricingStartup {

    private static final Logger LOG = Logger.getLogger(SeatPricingStartup.class);

    @Inject
    SeatPricingLoader loader;

    @ConfigProperty(name = "appgrove.seat-pricing.seed-on-startup", defaultValue = "false")
    boolean seedOnStartup;

    void onStart(@Observes StartupEvent event) {
        if (!seedOnStartup) {
            return;
        }
        if (loader.ensureInitialVersion()) {
            LOG.info("seat-pricing.seed@startup prima versione del listino dei posti creata dal file");
        }
    }
}
