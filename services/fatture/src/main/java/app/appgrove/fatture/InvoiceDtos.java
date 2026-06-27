package app.appgrove.fatture;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** DTO delle fatture. {@code tenantId} è derivato (JWT), in sola lettura; il {@code number} è server-side. */
public final class InvoiceDtos {

    private InvoiceDtos() {}

    /** Creazione fattura: il cliente e le righe; numero/totale/stato sono server-side. */
    public record CreateInvoice(
            @NotBlank @Size(max = 255) String customerName,
            @Size(max = 320) String customerEmail,
            LocalDate issueDate,
            @Size(max = 3) String currency,
            @Valid List<CreateLine> lines) {}

    public record CreateLine(
            @NotBlank @Size(max = 500) String description,
            BigDecimal quantity,
            BigDecimal unitAmount) {}

    /** Patch fattura: campi opzionali (null = invariato). Le righe non sono modificabili (app #1). */
    public record UpdateInvoice(
            @Size(max = 255) String customerName,
            @Size(max = 320) String customerEmail,
            String status) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record InvoiceView(
            UUID id,
            String number,
            String customerName,
            String customerEmail,
            LocalDate issueDate,
            String status,
            String currency,
            BigDecimal totalAmount,
            String tenantId,
            List<LineView> lines) {

        public static InvoiceView from(Invoice i) {
            return new InvoiceView(
                    i.getId(),
                    i.getNumber(),
                    i.getCustomerName(),
                    i.getCustomerEmail(),
                    i.getIssueDate(),
                    i.getStatus().name(),
                    i.getCurrency(),
                    i.getTotalAmount(),
                    i.getTenantId(),
                    i.getLines().stream().map(LineView::from).toList());
        }
    }

    public record LineView(
            UUID id, String description, BigDecimal quantity, BigDecimal unitAmount, BigDecimal lineAmount) {

        public static LineView from(InvoiceLine l) {
            return new LineView(l.getId(), l.getDescription(), l.getQuantity(), l.getUnitAmount(), l.getLineAmount());
        }
    }
}
