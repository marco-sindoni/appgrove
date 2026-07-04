package app.appgrove.core.support;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** DTO dell'API ticket lato utente (UC 0034, #13 D21). I DTO admin sono in {@code AdminGdprDtos}. */
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
                    ticket.getSubject(),
                    ticket.getPriority(),
                    ticket.getStatus(),
                    ticket.getDueAt(),
                    ticket.getCreatedAt(),
                    ticket.getClosedAt());
        }
    }

    public record TicketDetailView(TicketView ticket, List<MessageView> thread) {}
}
