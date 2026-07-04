package app.appgrove.core.gdpr;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** DTO dell'API export GDPR (UC 0032). La UI self-service che li consuma è UC 0033. */
public final class GdprDtos {

    private GdprDtos() {}

    /** Richiesta di export: tutto l'account, oppure una singola app ({@code appId} = slug). */
    public record StartExport(@NotNull GdprExportKind kind, String appId) {}

    /** Avanzamento aggregato: servizi completati su totale (gli step per-servizio negli item). */
    public record ProgressView(int completed, int total) {}

    public record ItemView(String appId, GdprExportStatus status, List<String> steps, String error) {

        static ItemView from(GdprExportJobItem item) {
            return new ItemView(item.getAppId(), item.getStatus(), item.getSteps(), item.getError());
        }
    }

    public record JobView(
            UUID id,
            GdprExportKind kind,
            String appId,
            GdprExportStatus status,
            ProgressView progress,
            List<ItemView> items,
            Instant requestedAt,
            Instant completedAt,
            String error) {

        static JobView from(GdprExportJob job, List<GdprExportJobItem> items) {
            int completed = (int) items.stream()
                    .filter(i -> i.getStatus() == GdprExportStatus.COMPLETED)
                    .count();
            return new JobView(
                    job.getId(),
                    job.getKind(),
                    job.getAppId(),
                    job.getStatus(),
                    new ProgressView(completed, items.size()),
                    items.stream().map(ItemView::from).toList(),
                    job.getCreatedAt(),
                    job.getCompletedAt(),
                    job.getError());
        }
    }

    /** Link di download firmato con scadenza (7 giorni, #13 D22): mostrato solo in-app. */
    public record DownloadView(String url, Instant expiresAt) {}

    /**
     * Recesso per-app (UC 0033, #13 D19/E23): la conferma porta l'export per-app già COMPLETED
     * come prova del passo "esporta" — il server la verifica prima di cancellare.
     */
    public record StartWithdrawal(@NotBlank String exportJobId) {}

    /** Esito del recesso: purge dell'app richiesta sulla coda, attivazione rimossa. */
    public record WithdrawalView(String appId, String status) {}
}
