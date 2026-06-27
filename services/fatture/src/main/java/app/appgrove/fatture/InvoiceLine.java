package app.appgrove.fatture;

import app.appgrove.commons.persistence.BaseTenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import org.hibernate.annotations.SQLRestriction;

/**
 * Riga di una fattura (tenant-scoped, figlia di {@link Invoice}). {@code lineAmount} è derivato
 * server-side ({@code quantity * unitAmount}). Nessun dato personale diretto sulla riga.
 */
@Entity
@Table(schema = "app_fatture", name = "invoice_line")
@SQLRestriction("deleted_at is null")
public class InvoiceLine extends BaseTenantEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "invoice_id", nullable = false, updatable = false)
    private Invoice invoice;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private BigDecimal quantity = BigDecimal.ONE;

    @Column(name = "unit_amount", nullable = false)
    private BigDecimal unitAmount = BigDecimal.ZERO;

    @Column(name = "line_amount", nullable = false)
    private BigDecimal lineAmount = BigDecimal.ZERO;

    protected InvoiceLine() {
        // richiesto da JPA
    }

    public InvoiceLine(String description, BigDecimal quantity, BigDecimal unitAmount) {
        this.description = description;
        if (quantity != null) {
            this.quantity = quantity;
        }
        if (unitAmount != null) {
            this.unitAmount = unitAmount;
        }
        this.lineAmount = this.quantity.multiply(this.unitAmount);
    }

    void setInvoice(Invoice invoice) {
        this.invoice = invoice;
    }

    public Invoice getInvoice() {
        return invoice;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitAmount() {
        return unitAmount;
    }

    public BigDecimal getLineAmount() {
        return lineAmount;
    }
}
