package app.appgrove.core.support;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Instant;

/**
 * Retention dei ticket (UC 0034, #13 E): hard-delete dei ticket chiusi/risolti da oltre 24 mesi
 * (minimizzazione; la prova di evasione resta nei registri audit, non nel ticket). Stesso pattern
 * di {@code AccountDeletionSweeper}: "adesso" iniettabile nei test, scheduler applicativo in
 * locale, trigger cloud (EventBridge/cron) di UC 0035.
 */
@ApplicationScoped
public class TicketRetentionSweeper {

    @Inject
    TicketStore store;

    @Scheduled(every = "1h", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void run() {
        sweep(Instant.now());
    }

    /** Elimina i ticket oltre retention rispetto a {@code now}; ritorna quanti ne ha eliminati. */
    public int sweep(Instant now) {
        return store.sweepExpired(now);
    }
}
