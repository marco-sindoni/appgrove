package app.appgrove.commons.entitlement.projection;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import java.time.Duration;
import java.time.Instant;
import org.jboss.logging.Logger;

/**
 * Strumentazione degli <b>scostamenti</b> della proiezione entitlement (UC 0046). È il presidio che
 * rende visibile il prezzo della postura scelta: la proiezione può essere vecchia, e senza misure un
 * canale di eventi rotto resterebbe invisibile finché qualcuno non se ne accorge dai reclami.
 *
 * <p>Le misure alimentano gli allarmi definiti nel modulo Terraform {@code microsaas_app}, così ogni
 * app li eredita senza codice. I contatori che contano davvero:
 *
 * <ul>
 *   <li>{@code safety_net} — ricorso alla chiamata sincrona a core. Fisiologico se raro (primo
 *       accesso di un tenant, o subito dopo un cambio di abbonamento); se <b>cresce stabilmente</b>
 *       significa che gli eventi non arrivano e siamo tornati di fatto al comportamento sincrono;</li>
 *   <li>{@code stale_served} — servita una proiezione vecchia perché core non rispondeva. È il caso
 *       in cui stiamo <b>consapevolmente</b> decidendo su dati non freschi: va visto;</li>
 *   <li>{@code denied_unknown} — accesso negato per assenza di qualunque base (proiezione assente e
 *       core irraggiungibile). È l'unico caso in cui un utente legittimo può essere respinto;</li>
 *   <li>{@code invalidation_lag} — ritardo fra l'evento a monte e il consumo, in secondi.</li>
 * </ul>
 *
 * <p>{@link Instance} lazy sul {@link MeterRegistry}: nei servizi/test senza estensione Micrometer la
 * strumentazione è inerte e non deve far fallire nulla — una misura mancante non è mai una buona
 * ragione per negare o concedere un accesso.
 */
@ApplicationScoped
public class EntitlementProjectionMetrics {

    private static final Logger LOG = Logger.getLogger(EntitlementProjectionMetrics.class);
    private static final String PREFIX = "appgrove.entitlement.projection.";

    @Inject
    Instance<MeterRegistry> registry;

    /** Proiezione fresca usata senza toccare la rete: il caso normale. */
    public void hit() {
        count("hit", "esito", "fresca");
    }

    /** Rinfresco riuscito tramite la rete di sicurezza (riga assente o da rinfrescare). */
    public void safetyNet(String motivo) {
        count("safety_net", "motivo", motivo);
    }

    /**
     * Servita una proiezione vecchia perché il rinfresco è fallito. Loggato a {@code WARN}: è una
     * decisione presa su dati non freschi e deve lasciare traccia anche fuori dalle metriche.
     */
    public void staleServed(String tenantId, String appSlug, Instant refreshedAt) {
        count("stale_served", "esito", "vecchia");
        gaugeAge(refreshedAt);
        LOG.warnf(
                "entitlement.projection servita proiezione vecchia tenant_id=%s app_id=%s eta_secondi=%d"
                        + " (rinfresco fallito: core irraggiungibile)",
                tenantId, appSlug, ageSeconds(refreshedAt));
    }

    /** Accesso negato: nessuna proiezione e core irraggiungibile. Nessuna base per decidere. */
    public void deniedUnknown(String tenantId, String appSlug) {
        count("denied_unknown", "esito", "negato");
        LOG.errorf(
                "entitlement.projection accesso negato tenant_id=%s app_id=%s: proiezione assente e core"
                        + " irraggiungibile — nessuna base per decidere",
                tenantId, appSlug);
    }

    /** Ritardo di propagazione fra l'evento a monte e il suo consumo. */
    public void invalidationLag(Instant occurredAt) {
        if (occurredAt == null || registry.isUnsatisfied()) {
            return;
        }
        long seconds = ageSeconds(occurredAt);
        try {
            registry.get().timer(PREFIX + "invalidation_lag").record(Duration.ofSeconds(seconds));
        } catch (RuntimeException e) {
            LOG.debugf(e, "entitlement.projection registrazione metrica fallita");
        }
    }

    private void gaugeAge(Instant refreshedAt) {
        if (refreshedAt == null || registry.isUnsatisfied()) {
            return;
        }
        try {
            registry.get().summary(PREFIX + "stale_age_seconds").record(ageSeconds(refreshedAt));
        } catch (RuntimeException e) {
            LOG.debugf(e, "entitlement.projection registrazione metrica fallita");
        }
    }

    private void count(String name, String tagKey, String tagValue) {
        if (registry.isUnsatisfied()) {
            return;
        }
        try {
            registry.get().counter(PREFIX + name, tagKey, tagValue).increment();
        } catch (RuntimeException e) {
            LOG.debugf(e, "entitlement.projection registrazione metrica fallita");
        }
    }

    private static long ageSeconds(Instant since) {
        return since == null ? 0L : Math.max(0L, Duration.between(since, Instant.now()).toSeconds());
    }
}
