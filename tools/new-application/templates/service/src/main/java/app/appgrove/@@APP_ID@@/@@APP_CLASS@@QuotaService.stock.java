package app.appgrove.@@APP_ID@@;

import app.appgrove.commons.quota.QuotaExceededException;
import app.appgrove.commons.quota.QuotaLimitSource;
import app.appgrove.commons.quota.QuotaService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Implementazione della quota per l'app @@APP_NAME@@. Metrica {@code @@METRIC@@} di natura
 * <b>stock</b>: l'uso è quanti record <b>esistono ora</b> per il tenant (conteggio tenant-scoped
 * automatico via discriminator, righe cancellate logicamente escluse), <b>senza alcuna finestra
 * temporale</b>; il tetto arriva da {@link QuotaLimitSource}, cioè — da UC 0046 — dalla proiezione
 * locale degli entitlement, non da una chiamata a core.
 *
 * <p>La conseguenza pratica che distingue questa natura dall'altra: il conteggio <b>scende</b> quando
 * un record viene cancellato, quindi il tetto si libera subito. Nessun calendario, nessun azzeramento
 * a inizio mese: chiedersi "da quando conto?" qui è già il sintomo di una natura sbagliata.
 *
 * <p>Se la metrica dell'app fosse invece di natura <b>flow</b> (a consumo: quanti record si sono
 * creati nel periodo), {@link #currentUsage} andrebbe cambiato in un conteggio sulla finestra — e il
 * file di listino dovrebbe dichiarare {@code type: flow} con la sua {@code window}. Le due cose vanno
 * cambiate insieme: un tetto a giacenza contato a consumo non si libera mai.
 */
@ApplicationScoped
public class @@APP_CLASS@@QuotaService implements QuotaService {

    public static final String METRIC = "@@METRIC@@";

    @Inject
    ItemRepository items;

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
        return items.count();
    }
}
