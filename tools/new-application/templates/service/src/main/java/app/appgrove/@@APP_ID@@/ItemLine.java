package app.appgrove.@@APP_ID@@;

import app.appgrove.commons.persistence.BaseTenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import org.hibernate.annotations.SQLRestriction;

/**
 * Riga di un {@link Item} (tenant-scoped, entità figlia). {@code lineAmount} è derivato server-side
 * ({@code quantity * unitAmount}). Nessun dato personale diretto sulla riga.
 *
 * <p>Esiste anche per dimostrare, nella suite generata, che l'erasure GDPR cancella in ordine
 * FK-safe e non lascia orfani: se il dominio reale non ha entità figlie, questa classe si elimina.
 */
@Entity
@Table(schema = "@@SCHEMA@@", name = "item_line")
@SQLRestriction("deleted_at is null")
public class ItemLine extends BaseTenantEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "item_id", nullable = false, updatable = false)
    private Item item;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private BigDecimal quantity = BigDecimal.ONE;

    @Column(name = "unit_amount", nullable = false)
    private BigDecimal unitAmount = BigDecimal.ZERO;

    @Column(name = "line_amount", nullable = false)
    private BigDecimal lineAmount = BigDecimal.ZERO;

    protected ItemLine() {
        // richiesto da JPA
    }

    public ItemLine(String description, BigDecimal quantity, BigDecimal unitAmount) {
        this.description = description;
        if (quantity != null) {
            this.quantity = quantity;
        }
        if (unitAmount != null) {
            this.unitAmount = unitAmount;
        }
        this.lineAmount = this.quantity.multiply(this.unitAmount);
    }

    void setItem(Item item) {
        this.item = item;
    }

    public Item getItem() {
        return item;
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
