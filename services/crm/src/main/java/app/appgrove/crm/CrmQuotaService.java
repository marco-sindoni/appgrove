package app.appgrove.crm;

import app.appgrove.commons.quota.QuotaExceededException;
import app.appgrove.commons.quota.QuotaLimitSource;
import app.appgrove.commons.quota.QuotaService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Quota dell'app Mini-CRM. Metrica {@code seats} di natura <b>stock</b>: l'uso è quanti <b>posti</b>
 * (utenti abilitati) esistono ORA per il tenant, <b>senza alcuna finestra temporale</b>; il tetto arriva
 * da {@link QuotaLimitSource}, cioè — da UC 0046 — dalla proiezione locale degli entitlement, non da una
 * chiamata a core.
 *
 * <p><b>La giacenza è quella dei posti, non dei contatti.</b> Un tenant può avere migliaia di contatti e
 * due posti: ciò che il piano limita è quante <b>persone</b> possono usare l'app (UC 0054), non quanti
 * dati vi immettono. Per questo si conta {@link SeatRepository}, non i contatti — sbagliare tabella qui
 * sarebbe far pagare al cliente la cosa sbagliata.
 *
 * <p>Conseguenza della natura a giacenza: revocare un posto lo fa <b>scendere</b> subito e libera il
 * tetto. Nessun calendario, nessun azzeramento a inizio mese.
 */
@ApplicationScoped
public class CrmQuotaService implements QuotaService {

    public static final String METRIC = "seats";

    @Inject
    SeatRepository seats;

    @Inject
    QuotaLimitSource limits;

    @Inject
    CallerContext caller;

    @Override
    public void checkAndReserve(String metric) {
        long cap = limits.capFor(caller.tenantId().toString(), metric);
        if (cap < 0) {
            return; // nessun limite per questa metrica
        }
        if (currentUsage(metric) >= cap) {
            throw new QuotaExceededException(metric, cap);
        }
    }

    @Override
    public long currentUsage(String metric) {
        return seats.count();
    }
}
