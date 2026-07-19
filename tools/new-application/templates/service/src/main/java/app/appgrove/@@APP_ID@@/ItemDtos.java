package app.appgrove.@@APP_ID@@;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** DTO del dominio segnaposto. {@code tenantId} è derivato (JWT), in sola lettura; il {@code code} è server-side. */
public final class ItemDtos {

    private ItemDtos() {}

    /** Creazione record: contatto e righe; codice/totale/stato sono server-side. */
    public record CreateItem(
            @NotBlank @Size(max = 255) String contactName,
            @Size(max = 320) String contactEmail,
            LocalDate recordedOn,
            @Size(max = 3) String currency,
            @Valid List<CreateLine> lines) {}

    public record CreateLine(
            @NotBlank @Size(max = 500) String description,
            BigDecimal quantity,
            BigDecimal unitAmount) {}

    /** Patch record: campi opzionali (null = invariato). Le righe non sono modificabili nello scaffold. */
    public record UpdateItem(
            @Size(max = 255) String contactName,
            @Size(max = 320) String contactEmail,
            String status) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ItemView(
            UUID id,
            String code,
            String contactName,
            String contactEmail,
            LocalDate recordedOn,
            String status,
            String currency,
            BigDecimal totalAmount,
            String tenantId,
            List<LineView> lines) {

        public static ItemView from(Item i) {
            return new ItemView(
                    i.getId(),
                    i.getCode(),
                    i.getContactName(),
                    i.getContactEmail(),
                    i.getRecordedOn(),
                    i.getStatus().name(),
                    i.getCurrency(),
                    i.getTotalAmount(),
                    i.getTenantId(),
                    i.getLines().stream().map(LineView::from).toList());
        }
    }

    public record LineView(
            UUID id, String description, BigDecimal quantity, BigDecimal unitAmount, BigDecimal lineAmount) {

        public static LineView from(ItemLine l) {
            return new LineView(l.getId(), l.getDescription(), l.getQuantity(), l.getUnitAmount(), l.getLineAmount());
        }
    }
}
