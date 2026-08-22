package app.appgrove.core.catalog;

import app.appgrove.commons.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLRestriction;

/**
 * App di catalogo (platform-level, #09 B10): mapping <b>1 app = 1 Product</b> Paddle.
 * NON estende {@link app.appgrove.commons.persistence.BaseTenantEntity}: il catalogo è di piattaforma,
 * non tenant-scoped (nessun {@code WHERE tenant_id}). DDL in {@code V2__core_domain.sql} (UC 0013);
 * entità JPA + repository nascono qui (UC 0022), primo consumatore reale del catalogo.
 *
 * <p>Le scritture passano dal {@link PricingSyncService} (pricing-as-code → catalogo, via SQL nativo con
 * ID deterministici {@link CatalogIds}); questa entità è il <b>read-model</b>. {@code paddleProductId} è
 * per-ambiente, riempito dalla sync contro Paddle (stub in dev/test), mai dal codice (#09 H37).
 */
@Entity
@Table(schema = "platform", name = "app")
@SQLRestriction("deleted_at is null")
public class App extends BaseEntity {

    @Column(nullable = false)
    private String slug;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_model", nullable = false, length = 32)
    private AppUserModel userModel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AppStatus status;

    /**
     * Applicazione del marketplace o voce di piattaforma (UC 0103). Il valore predefinito in banca dati è
     * {@code application}: la sincronizzazione del listino non nomina questa colonna, quindi ogni app che
     * nasce dal pricing-as-code nasce applicazione senza che il motore di sincronizzazione debba saperlo.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AppKind kind = AppKind.application;

    @Column(name = "paddle_product_id")
    private String paddleProductId;

    protected App() {
        // richiesto da JPA
    }

    public String getSlug() {
        return slug;
    }

    public String getName() {
        return name;
    }

    public AppUserModel getUserModel() {
        return userModel;
    }

    public AppStatus getStatus() {
        return status;
    }

    public AppKind getKind() {
        return kind;
    }

    /**
     * Vero se questa riga è una <b>applicazione</b> del marketplace, cioè qualcosa che si vende e si apre.
     * Le superfici che elencano applicazioni chiedono questo, non «lo slug non è quello dei posti».
     */
    public boolean isApplication() {
        return kind == AppKind.application;
    }

    public String getPaddleProductId() {
        return paddleProductId;
    }
}
