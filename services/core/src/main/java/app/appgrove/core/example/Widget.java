package app.appgrove.core.example;

import app.appgrove.commons.persistence.BaseTenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLRestriction;

/**
 * HARNESS multitenancy (UC 0012). Entity tenant-scoped minima usata solo per <b>esercitare e testare</b>
 * il pattern (discriminator, base entity, soft-delete) finché non arrivano le entità di dominio (UC 0013).
 * La sua tabella è creata <b>solo da una migration di test</b> ({@code core/src/test/.../V2__example_widget.sql}):
 * lo schema {@code platform} di produzione resta vuoto. Rimovibile quando UC 0013 introduce le entità reali.
 */
@Entity
@Table(schema = "platform", name = "widget")
@SQLRestriction("deleted_at is null")
public class Widget extends BaseTenantEntity {

    @Column(nullable = false)
    private String name;

    protected Widget() {
        // richiesto da JPA
    }

    public Widget(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
