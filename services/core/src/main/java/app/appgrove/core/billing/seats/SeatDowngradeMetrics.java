package app.appgrove.core.billing.seats;

import io.micrometer.core.instrument.MeterRegistry;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import java.time.Instant;
import org.jboss.logging.Logger;

/**
 * La misura che rende visibile <b>il guasto che costa al cliente</b>: le riduzioni dei posti
 * <b>scadute e non ancora eseguite</b> (UC 0104 §5, trappola nota n. 3 del piano di lavoro).
 *
 * <p>Perché questa misura non è un extra. Una riduzione non eseguita è invisibile da ogni altro punto di
 * osservazione: il cliente non riceve alcun errore, la schermata mostra un'attesa con una data passata, e
 * l'unica conseguenza è che continua a pagare posti che credeva chiusi. Nessun collaudo la intercetta,
 * perché non è un difetto del codice ma un'esecuzione che non è avvenuta. Senza una misura, il primo a
 * scoprirlo è il cliente, dalla fattura.
 *
 * <p><b>In condizioni normali vale zero</b>: lo spazzino gira ogni ora e la data di esecuzione è la fine di
 * un periodo mensile, quindi il valore è diverso da zero solo per i minuti fra la scadenza e il giro
 * successivo. Un valore che resta positivo per più di un periodo di controllo è un allarme.
 *
 * <p>Misura <b>di piattaforma</b>: nessuna dimensione per account (regola dei due piani, #08 30-31 — i
 * valori ad alta cardinalità vivono nei log). Quale account è in ritardo lo dice il messaggio di severità
 * alta che lo spazzino scrive quando l'esecuzione fallisce.
 *
 * <p>{@link Instance} pigra sul registro delle misure: dove l'estensione non c'è (collaudi) la
 * strumentazione è <b>inerte</b>. Una misura mancante non è mai una buona ragione per far fallire nulla.
 */
@ApplicationScoped
public class SeatDowngradeMetrics {

    private static final Logger LOG = Logger.getLogger(SeatDowngradeMetrics.class);
    private static final String OVERDUE = "appgrove.seats.reduction.overdue";

    @Inject
    Instance<MeterRegistry> registry;

    @Inject
    SeatDowngradeExecutor executor;

    /**
     * Rilevazione periodica, sfasata rispetto allo spazzino: si misura <b>dopo</b> che il giro di
     * esecuzione ha avuto la sua occasione, altrimenti si misurerebbe la coda invece del ritardo.
     */
    @Scheduled(every = "1h", concurrentExecution = Scheduled.ConcurrentExecution.SKIP, delayed = "5m")
    void sample() {
        try {
            publish(Instant.now());
        } catch (RuntimeException e) {
            LOG.debugf(e, "seats.reduction rilevazione non riuscita");
        }
    }

    /**
     * Pubblica la misura e restituisce il numero di riduzioni scadute e non eseguite, così che i collaudi
     * possano verificarlo senza passare dal registro delle misure.
     */
    public long publish(Instant now) {
        long overdue = executor.overdueCount(now);
        if (!registry.isUnsatisfied()) {
            try {
                registry.get().summary(OVERDUE).record(overdue);
            } catch (RuntimeException e) {
                LOG.debugf(e, "seats.reduction registrazione misura fallita");
            }
        }
        if (overdue > 0) {
            // A WARN e non solo come misura: chi guarda i log deve poterlo vedere senza una dashboard, e
            // il testo deve dire la conseguenza — non «contatore diverso da zero», ma «qualcuno paga».
            LOG.warnf(
                    "seats.reduction.overdue riduzioni scadute e non eseguite=%d — quegli account stanno"
                            + " pagando posti che credevano chiusi",
                    overdue);
        }
        return overdue;
    }
}
