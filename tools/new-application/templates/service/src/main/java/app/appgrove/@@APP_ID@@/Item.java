package app.appgrove.@@APP_ID@@;

import app.appgrove.commons.persistence.BaseTenantEntity;
import app.appgrove.commons.privacy.PersonalData;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.SQLRestriction;

/**
 * Record principale dell'app @@APP_NAME@@ — <b>dominio segnaposto</b> generato dallo scaffolding
 * (UC 0046): va sostituito con l'entità reale dell'app. Tenant-scoped: il filtro
 * {@code WHERE tenant_id = ?} è automatico (discriminator).
 *
 * <p>{@code contactName}/{@code contactEmail} sono dati personali (base giuridica: contratto,
 * #13 A2) — dichiarati con {@link PersonalData} e, obbligatoriamente, nel manifesto dati
 * {@code docs/compliance/manifests/@@APP_ID@@.yaml}. Le due dichiarazioni sono incrociate da
 * {@code PersonalDataManifestTest}: aggiungere un campo personale senza aggiornare il manifesto
 * rende la build rossa, ed è voluto.
 */
@Entity
@Table(schema = "@@SCHEMA@@", name = "item")
@SQLRestriction("deleted_at is null")
public class Item extends BaseTenantEntity {

    @Column(nullable = false, updatable = false)
    private String code;

    @PersonalData(
            category = "identità del contatto",
            purpose = "@@PD_PURPOSE@@",
            legalBasis = "contratto",
            retention = "@@PD_RETENTION@@")
    @Column(name = "contact_name", nullable = false)
    private String contactName;

    @PersonalData(
            category = "recapito del contatto",
            purpose = "@@PD_PURPOSE@@",
            legalBasis = "contratto",
            retention = "@@PD_RETENTION@@")
    @Column(name = "contact_email")
    private String contactEmail;

    @Column(name = "recorded_on", nullable = false)
    private LocalDate recordedOn;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ItemStatus status = ItemStatus.draft;

    @Column(nullable = false)
    private String currency = "EUR";

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @OneToMany(mappedBy = "item", cascade = CascadeType.ALL)
    private List<ItemLine> lines = new ArrayList<>();

    protected Item() {
        // richiesto da JPA
    }

    public Item(String code, String contactName, String contactEmail, LocalDate recordedOn, String currency) {
        this.code = code;
        this.contactName = contactName;
        this.contactEmail = contactEmail;
        this.recordedOn = recordedOn;
        if (currency != null && !currency.isBlank()) {
            this.currency = currency;
        }
    }

    /** Aggiunge una riga mantenendo la relazione bidirezionale; ricalcola il totale. */
    public void addLine(ItemLine line) {
        line.setItem(this);
        lines.add(line);
        recomputeTotal();
    }

    public void recomputeTotal() {
        this.totalAmount = lines.stream()
                .map(ItemLine::getLineAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public String getCode() {
        return code;
    }

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    public LocalDate getRecordedOn() {
        return recordedOn;
    }

    public ItemStatus getStatus() {
        return status;
    }

    public void setStatus(ItemStatus status) {
        this.status = status;
    }

    public String getCurrency() {
        return currency;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public List<ItemLine> getLines() {
        return lines;
    }
}
