package app.appgrove.fatture;

import app.appgrove.commons.quota.QuotaExceededException;
import app.appgrove.commons.quota.QuotaLimitSource;
import app.appgrove.commons.quota.QuotaService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

/**
 * Implementazione della quota per l'app fatture. Metrica {@code fatture} di natura <b>flow</b>:
 * l'uso è il numero di fatture create dal tenant nel <b>mese di calendario</b> corrente (conteggio
 * tenant-scoped automatico via discriminator); il tetto arriva da {@link QuotaLimitSource}.
 */
@ApplicationScoped
public class FattureQuotaService implements QuotaService {

    public static final String METRIC = "fatture";

    @Inject
    InvoiceRepository invoices;

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
        return invoices.countCreatedSince(startOfCurrentMonthUtc());
    }

    private static Instant startOfCurrentMonthUtc() {
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        return now.toLocalDate()
                .withDayOfMonth(1)
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant();
    }
}
