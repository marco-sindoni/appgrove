package app.appgrove.crm;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

/** DTO del dominio mini-CRM. {@code tenantId} è derivato dal token (sola lettura); mai da body/param. */
public final class CrmDtos {

    private CrmDtos() {}

    // ── Contatti ─────────────────────────────────────────────────────────────

    public record CreateContact(
            @NotBlank @Size(max = 255) String displayName,
            @Size(max = 320) String email,
            @Size(max = 64) String phone,
            @Size(max = 255) String organization,
            @Size(max = 2000) String notes) {}

    /** Patch contatto: campi opzionali (null = invariato). Lo stato passa per {@code stage}. */
    public record UpdateContact(
            @Size(max = 255) String displayName,
            @Size(max = 320) String email,
            @Size(max = 64) String phone,
            @Size(max = 255) String organization,
            String stage,
            @Size(max = 2000) String notes) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ContactView(
            UUID id,
            String displayName,
            String email,
            String phone,
            String organization,
            String stage,
            String notes,
            String tenantId) {

        public static ContactView from(Contact c) {
            return new ContactView(
                    c.getId(),
                    c.getDisplayName(),
                    c.getEmail(),
                    c.getPhone(),
                    c.getOrganization(),
                    c.getStage().name(),
                    c.getNotes(),
                    c.getTenantId());
        }
    }

    // ── Interazioni ──────────────────────────────────────────────────────────

    public record CreateInteraction(
            String kind,
            LocalDate occurredOn,
            @Size(max = 2000) String note) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record InteractionView(UUID id, UUID contactId, String kind, LocalDate occurredOn, String note) {

        public static InteractionView from(Interaction i) {
            return new InteractionView(
                    i.getId(),
                    i.getContact().getId(),
                    i.getKind().name(),
                    i.getOccurredOn(),
                    i.getNote());
        }
    }

    // ── Posti ────────────────────────────────────────────────────────────────

    public record AssignSeat(@NotBlank @Size(max = 64) String userId) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SeatView(UUID id, String userId) {

        public static SeatView from(Seat s) {
            return new SeatView(s.getId(), s.getUserId());
        }
    }

    /** Riepilogo posti per la schermata «Membri»: quanti occupati, quale tetto (null = illimitato). */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SeatSummary(long used, Long limit, Long remaining, java.util.List<SeatView> seats) {

        public static SeatSummary of(long used, long cap, java.util.List<SeatView> seats) {
            if (cap < 0) {
                return new SeatSummary(used, null, null, seats);
            }
            return new SeatSummary(used, cap, Math.max(0, cap - used), seats);
        }
    }
}
