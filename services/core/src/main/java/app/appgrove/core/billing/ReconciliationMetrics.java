package app.appgrove.core.billing;

import app.appgrove.core.billing.ReconciliationDtos.ReconciliationTotals;
import io.micrometer.core.instrument.MeterRegistry;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

/**
 * Osservabilità del netto incassato (UC 0071, #08 8-9). Rende visibile fuori dalla console ciò che la
 * console mostra a chi la apre: quanto è entrato davvero, quanto pesano le commissioni, quanto denaro è
 * ancora fermo dal fornitore, e — soprattutto — se un accredito atteso <b>non è arrivato</b>.
 *
 * <p>Le misure sono <b>di piattaforma</b>: nessuna dimensione per conto o utente (regola dei due piani,
 * #08 30-31 — i valori ad alta cardinalità vivono nei log, mai come dimensioni di una metrica).
 *
 * <p>{@link Instance} lazy sul registro delle misure: dove l'estensione non c'è (test, servizi senza
 * Micrometer) la strumentazione è <b>inerte</b>. Una misura mancante non è mai una buona ragione per far
 * fallire l'elaborazione dei pagamenti.
 */
@ApplicationScoped
public class ReconciliationMetrics {

    private static final Logger LOG = Logger.getLogger(ReconciliationMetrics.class);
    private static final String PREFIX = "appgrove.billing.reconciliation.";

    @Inject
    Instance<MeterRegistry> registry;

    @Inject
    ReconciliationService reconciliation;

    /**
     * Rilevazione periodica. Ogni ora è la cadenza giusta: gli accrediti arrivano ogni due settimane e il
     * ritardo si misura in giorni — misurarlo al minuto costerebbe query senza aggiungere una sola
     * informazione. In test lo scheduler è spento e il metodo si invoca direttamente.
     */
    @Scheduled(every = "1h", concurrentExecution = Scheduled.ConcurrentExecution.SKIP, delayed = "30s")
    void sample() {
        try {
            publish(reconciliation.totals());
        } catch (RuntimeException e) {
            LOG.debugf(e, "billing.reconciliation rilevazione non riuscita");
        }
    }

    /**
     * Pubblica le misure dei totali e segnala l'accredito in ritardo. Ritorna se l'accredito atteso risulta
     * in ritardo, così il chiamante (e i test) possano verificarlo senza passare dal registro delle misure.
     */
    public boolean publish(ReconciliationTotals totals) {
        boolean overdue = reconciliation.isPayoutOverdue(totals);
        record("gross", totals.gross());
        record("fee", totals.fee());
        record("net", totals.net());
        record("reversed", totals.reversed());
        record("unsettled", totals.unsettled());
        record("fee_percent", (long) ReconciliationService.feePercent(totals.gross(), totals.fee()));
        record("payout_overdue", overdue ? 1 : 0);
        if (overdue) {
            // A WARN e non solo come misura: un accredito che non arriva è una decisione da prendere, e
            // deve lasciare traccia anche per chi guarda i log e non le dashboard.
            LOG.warnf(
                    "billing.reconciliation accredito atteso non ricevuto: netto non accreditato=%d dal %s",
                    totals.unsettled(), totals.oldestUnsettledAt());
        }
        return overdue;
    }

    private void record(String name, long value) {
        if (registry.isUnsatisfied()) {
            return;
        }
        try {
            registry.get().summary(PREFIX + name).record(value);
        } catch (RuntimeException e) {
            LOG.debugf(e, "billing.reconciliation registrazione misura fallita");
        }
    }
}
