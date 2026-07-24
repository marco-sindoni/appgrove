package app.appgrove.crm;

import app.appgrove.commons.messaging.MessageQueues;
import app.appgrove.commons.usage.UsageEvents;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import java.time.Instant;
import org.jboss.logging.Logger;

/**
 * Dichiara al core l'uso a giacenza dei posti del tenant (UC 0054): pubblica sulla coda condivisa
 * {@code app-usage} il messaggio "il tenant T dell'app crm occupa N posti", così che il gate del
 * downgrade in core ({@code TierChangePolicy}) possa rifiutare il passaggio a un piano con meno posti
 * di quelli occupati.
 *
 * <p><b>Verso invertito e asincrono, di proposito</b> (UC 0054): è l'app a spingere il proprio uso verso
 * core, non core a leggerlo dall'app. Reintrodurre una lettura sincrona {@code core → app} qui sarebbe
 * il passo indietro che UC 0046 ha appena evitato nella direzione opposta.
 *
 * <p><b>La pubblicazione non è mai bloccante.</b> Un errore sul bus viene loggato e ingoiato: assegnare o
 * revocare un posto è già avvenuto e non va annullato perché una notifica non è partita. La conseguenza —
 * una giacenza lato core temporaneamente vecchia — al più lascia passare un downgrade che andava bloccato
 * fino al prossimo report, ed è meno grave che rompere l'operazione dell'utente. Ogni cambiamento dei
 * posti ripubblica il valore assoluto, quindi un report perso viene sanato dal successivo.
 */
@ApplicationScoped
public class SeatUsagePublisher {

    private static final Logger LOG = Logger.getLogger(SeatUsagePublisher.class);

    // Instance<> lazy: nei test senza code il publisher resta inerte invece di far fallire il boot.
    @Inject
    Instance<MessageQueues> queuesInstance;

    @Inject
    ObjectMapper mapper;

    /** Pubblica la giacenza attuale dei posti del tenant. Best-effort: non solleva mai verso il chiamante. */
    public void publish(String tenantId, long seatCount) {
        if (queuesInstance.isUnsatisfied()) {
            return;
        }
        try {
            String body = mapper.writeValueAsString(new UsageEvents.UsageReport(
                    CrmDataContract.APP_ID, tenantId, CrmQuotaService.METRIC, seatCount, Instant.now().toString()));
            queuesInstance.get().send(UsageEvents.USAGE_QUEUE, body);
            LOG.infof("app-usage pubblicato app=%s tenant_id=%s seats=%d", CrmDataContract.APP_ID, tenantId, seatCount);
        } catch (JsonProcessingException | RuntimeException e) {
            LOG.errorf(e, "app-usage pubblicazione fallita tenant_id=%s seats=%d → ignorata", tenantId, seatCount);
        }
    }
}
