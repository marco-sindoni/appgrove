package app.appgrove.core.catalog;

import app.appgrove.commons.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.SQLRestriction;

/**
 * Price di un tier per ciclo: mapping <b>(tier × ciclo) = 1 Price</b> Paddle (#09 B10). {@code amount} è in
 * <b>minor units</b> (es. 900 = €9,00); il prezzo è uguale per tutti gli ambienti (vive nel codice), mentre
 * {@code paddlePriceId} è <b>per-ambiente</b> e lo riempie la sync (#09 H37). Read-model; scritture dal
 * {@link PricingSyncService}.
 */
@Entity
@Table(schema = "platform", name = "app_price")
@SQLRestriction("deleted_at is null")
public class AppPrice extends BaseEntity {

    @Column(name = "app_tier_id", columnDefinition = "uuid", nullable = false)
    private UUID appTierId;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_cycle", nullable = false, length = 16)
    private BillingCycle billingCycle;

    @Column(name = "paddle_price_id")
    private String paddlePriceId;

    @Column(nullable = false)
    private int amount;

    @Column(nullable = false, length = 3)
    private String currency;

    protected AppPrice() {
        // richiesto da JPA
    }

    public UUID getAppTierId() {
        return appTierId;
    }

    public BillingCycle getBillingCycle() {
        return billingCycle;
    }

    public String getPaddlePriceId() {
        return paddlePriceId;
    }

    public int getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }
}
