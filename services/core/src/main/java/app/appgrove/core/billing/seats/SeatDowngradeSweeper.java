package app.appgrove.core.billing.seats;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Instant;
import java.util.List;
import org.jboss.logging.Logger;

/**
 * Il <b>lavoro periodico</b> che esegue le riduzioni dei posti alla scadenza (UC 0104 §4.5), sul modello
 * degli spazzini già presenti nel core ({@code AccountDeletionSweeper}).
 *
 * <p><b>Ogni ora</b> è la cadenza giusta e vale la pena dire perché: la data di esecuzione è la fine di un
 * periodo di fatturazione mensile, quindi un ritardo di qualche decina di minuti non cambia nulla per
 * nessuno — né per il cliente, che non ha un posto in più da usare, né per il conto, che si calcola sullo
 * stato del momento. Girare al minuto costerebbe una interrogazione su tutta la piattaforma senza aggiungere
 * una sola informazione.
 *
 * <p><b>Una transazione per account</b>, dentro {@link SeatDowngradeExecutor#executeDue(Instant)}: un
 * guasto su un account non blocca gli altri. E l'esecuzione è idempotente, quindi una sovrapposizione fra
 * due giri non rimuove nessuno due volte — {@code SKIP} sulla concorrenza è comunque la postura giusta, per
 * non moltiplicare il lavoro inutilmente.
 *
 * <p>In locale e in test gira sullo scheduler applicativo di Quarkus; nei collaudi lo scheduler è spento e
 * il metodo si invoca direttamente con un «adesso» scelto, così non si aspetta nulla di reale. Il richiamo
 * dal cloud (temporizzatore gestito) è di UC 0035, come per gli altri spazzini.
 */
@ApplicationScoped
public class SeatDowngradeSweeper {

    private static final Logger LOG = Logger.getLogger(SeatDowngradeSweeper.class);

    @Inject
    SeatDowngradeExecutor executor;

    @Scheduled(every = "1h", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void run() {
        try {
            sweep(Instant.now());
        } catch (RuntimeException e) {
            // La lettura dell'elenco è fallita (banca dati non raggiungibile in locale, per esempio): non
            // intasare i log dello scheduler. Le riduzioni restano in attesa e la misura le rende visibili.
            LOG.debugf(e, "seats.reduction.sweep non eseguito");
        }
    }

    /**
     * Esegue le riduzioni dovute rispetto a {@code now}. L'istante è un parametro perché i collaudi
     * devono poterlo scegliere: un collaudo che aspettasse la fine di un periodo vero non sarebbe un
     * collaudo.
     *
     * @return gli account su cui la riduzione è stata eseguita
     */
    public List<String> sweep(Instant now) {
        List<String> done = executor.executeDue(now);
        if (!done.isEmpty()) {
            LOG.infof("seats.reduction.sweep eseguite=%d", done.size());
        }
        return done;
    }
}
