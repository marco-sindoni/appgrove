package app.appgrove.core.support;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** DTO dell'API ticket, lato utente e lato amministrazione (UC 0034 · UC 0075). */
public final class TicketDtos {

    private TicketDtos() {}

    /** Apertura di un ticket: oggetto + primo messaggio del thread. */
    public record OpenTicket(
            @NotNull TicketType type,
            @NotBlank @Size(max = 200) String subject,
            @NotBlank @Size(max = 4000) String message) {}

    /** Risposta nel thread. */
    public record PostMessage(@NotBlank @Size(max = 4000) String body) {}

    public record MessageView(UUID id, TicketAuthor author, String body, Instant createdAt) {

        static MessageView from(SupportTicketMessage message) {
            return new MessageView(
                    message.getId(), message.getAuthor(), message.getBody(), message.getCreatedAt());
        }
    }

    public record TicketView(
            UUID id,
            TicketType type,
            TicketSource source,
            String subject,
            TicketPriority priority,
            TicketStatus status,
            Instant dueAt,
            Instant createdAt,
            Instant closedAt) {

        static TicketView from(SupportTicket ticket) {
            return new TicketView(
                    ticket.getId(),
                    ticket.getType(),
                    ticket.getSource(),
                    ticket.getSubject(),
                    ticket.getPriority(),
                    ticket.getStatus(),
                    ticket.getDueAt(),
                    ticket.getCreatedAt(),
                    ticket.getClosedAt());
        }
    }

    public record TicketDetailView(TicketView ticket, List<MessageView> thread) {}

    // ── Vista di amministrazione (UC 0075) ───────────────────────────────────
    // Stava in AdminGdprDtos perché il ticketing è nato come strumento della console privacy;
    // con la sezione «Ticket» autonoma torna dove appartiene, accanto al proprio dominio.

    /**
     * Riga della coda cross-account. {@code flaggedForReview} è il contrassegno «da rivedere»
     * (categorie particolari, UC 0075); {@code logsUrl} è il collegamento profondo ai registri
     * (null se non configurato, come in locale).
     */
    public record AdminTicketView(
            UUID id,
            String tenantId,
            String accountName,
            TicketType type,
            TicketSource source,
            String subject,
            TicketPriority priority,
            TicketStatus status,
            boolean flaggedForReview,
            Instant dueAt,
            UUID exportJobId,
            Instant closedAt,
            Instant createdAt,
            String logsUrl) {}

    public record AdminMessageView(UUID id, TicketAuthor author, String body, Instant createdAt) {}

    public record AdminTicketDetailView(AdminTicketView ticket, List<AdminMessageView> thread) {}

    /** Cambio stato/priorità del ticket (chi assiste non edita mai il contenuto: ops sicure). */
    public record UpdateTicket(@NotNull TicketStatus status, @NotNull TicketPriority priority) {}
}
