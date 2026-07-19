package app.appgrove.@@APP_ID@@;

import app.appgrove.commons.quota.QuotaExceededException;
import app.appgrove.commons.quota.QuotaLimitSource;
import app.appgrove.commons.quota.QuotaService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

/**
 * Implementazione della quota per l'app @@APP_NAME@@. Metrica {@code @@METRIC@@} di natura
 * <b>flow</b>: l'uso è il numero di record creati dal tenant nel <b>mese di calendario</b> corrente
 * (conteggio tenant-scoped automatico via discriminator); il tetto arriva da {@link QuotaLimitSource},
 * cioè — da UC 0046 — dalla proiezione locale degli entitlement, non da una chiamata a core.
 *
 * <p>Se la metrica dell'app è di natura <b>stock</b> (giacenza: quanti oggetti esistono ORA, non
 * quanti se ne sono creati nel periodo), {@link #currentUsage} va cambiato in un conteggio totale
 * senza finestra temporale — e il file di listino deve dichiarare {@code type: stock}. Le due cose
 * vanno cambiate insieme: un tetto "a giacenza" contato "a consumo" non si libera mai.
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
        return items.countCreatedSince(startOfCurrentMonthUtc());
    }

    private static Instant startOfCurrentMonthUtc() {
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        return now.toLocalDate()
                .withDayOfMonth(1)
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant();
    }
}
