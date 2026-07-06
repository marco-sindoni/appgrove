package app.appgrove.commons.metrics;

import io.micrometer.core.instrument.Clock;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import java.time.Duration;
import java.util.Optional;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Produce l'{@link EmfMeterRegistry} come bean CDI: quarkus-micrometer lo aggancia al registry
 * composito, così le metriche Micrometer dei servizi (incluse le {@code http.server.requests}
 * built-in) fluiscono nel bridge EMF senza codice per-servizio.
 *
 * <p>La pubblicazione parte SOLO con {@code appgrove.metrics.emf.enabled=true} (test/prod, dove
 * stdout è raccolto dal driver awslogs); altrimenti il registry resta inerte — i meter si
 * registrano ma nessuna riga EMF viene emessa (in locale sarebbe solo rumore).
 */
@ApplicationScoped
public class EmfMeterRegistryProducer {

    @Produces
    @Singleton
    public EmfMeterRegistry emfMeterRegistry(
            @ConfigProperty(name = "appgrove.metrics.emf.enabled", defaultValue = "false") boolean enabled,
            @ConfigProperty(name = "appgrove.metrics.emf.step", defaultValue = "60s") Duration step,
            @ConfigProperty(name = "quarkus.application.name") Optional<String> serviceName,
            @ConfigProperty(name = "appgrove.app-id") Optional<String> appId,
            @ConfigProperty(name = "appgrove.env") Optional<String> env) {
        EmfMeterRegistry registry = new EmfMeterRegistry(
                EmfMeterRegistry.config(step), Clock.SYSTEM, EmfMeterRegistry.stdoutWriter());
        // dimensioni comuni a bassa cardinalità (whitelist #08/9), valorizzate dalla config del
        // servizio/ambiente quando presenti: service = nome applicazione, app_id/env espliciti.
        serviceName.ifPresent(value -> registry.config().commonTags("service", value));
        appId.ifPresent(value -> registry.config().commonTags("app_id", value));
        env.ifPresent(value -> registry.config().commonTags("env", value));
        if (enabled) {
            registry.start(runnable -> {
                Thread thread = new Thread(runnable, "appgrove-emf-metrics");
                thread.setDaemon(true);
                return thread;
            });
        }
        return registry;
    }

    void close(@Disposes EmfMeterRegistry registry) {
        registry.close();
    }
}
