package app.appgrove.core.gdpr;

import app.appgrove.core.support.TicketAuthor;
import app.appgrove.core.support.TicketPriority;
import app.appgrove.core.support.TicketStatus;
import app.appgrove.core.support.TicketType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** DTO della console "Diritti GDPR" (UC 0034): aggregazione + ticket admin + limitazione art. 18. */
public final class AdminGdprDtos {

    private AdminGdprDtos() {}

    /**
     * Riga della tabella aggregata (#13 L75): una richiesta di esercizio diritti, qualunque sia il
     * tipo ({@code export} | {@code withdrawal} | {@code account_deletion} | {@code privacy_ticket}).
     * {@code dueAt} = scadenza rilevante (link export, fine grace, scadenza legale ticket);
     * {@code logsUrl} = deep-link Logs Insights (null se non configurato, es. in locale).
     */
    public record RequestView(
            String type,
            UUID refId,
            String tenantId,
            String accountName,
            String appId,
            String subjectId,
            String status,
            Instant requestedAt,
            Instant completedAt,
            Instant dueAt,
            String error,
            String logsUrl) {}

    public record ExportItemView(String appId, String status, String error) {}

    /** Dettaglio export: job + item per-servizio + puntatore all'oggetto S3 (chiave + console). */
    public record ExportDetailView(
            RequestView request, List<ExportItemView> items, String zipKey, String s3ConsoleUrl) {}

    public record AdminTicketView(
            UUID id,
            String tenantId,
            String accountName,
            TicketType type,
            String subject,
            TicketPriority priority,
            TicketStatus status,
            Instant dueAt,
            UUID exportJobId,
            Instant closedAt,
            Instant createdAt,
            String logsUrl) {}

    public record AdminMessageView(UUID id, TicketAuthor author, String body, Instant createdAt) {}

    public record AdminTicketDetailView(AdminTicketView ticket, List<AdminMessageView> thread) {}

    /** Cambio stato/priorità del ticket (l'admin non edita mai il contenuto: ops sicure, #13 L75). */
    public record UpdateTicket(@NotNull TicketStatus status, @NotNull TicketPriority priority) {}

    /** Applicazione della limitazione art. 18 a un account o a un singolo utente. */
    public record ApplyRestriction(
            @NotNull GdprRestrictionService.TargetKind targetKind,
            @NotNull UUID targetId,
            UUID ticketId,
            @Size(max = 512) String note) {}

    /** Esito di applica/rimuovi limitazione. */
    public record RestrictionResult(String outcome) {}

    /** Limitazioni attive + registro prove (audit #13 D19). */
    public record RestrictionsView(
            List<GdprRestrictionService.RestrictionView> active,
            List<GdprRestrictionService.RestrictionAuditView> auditTrail) {}

    /** Riga del registro prove di erasure ({@code gdpr_purge_audit}, #13 L70). */
    public record PurgeAuditView(
            UUID id, String tenantId, String appId, String reason, int total, Instant executedAt) {}
}
