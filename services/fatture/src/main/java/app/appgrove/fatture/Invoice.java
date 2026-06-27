package app.appgrove.fatture;

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
 * Fattura (app #1, B2C single-user). Tenant-scoped: il filtro {@code WHERE tenant_id = ?} è automatico
 * (discriminator). {@code customerName}/{@code customerEmail} sono dati personali del cliente
 * (base giuridica: contratto, #13 A2) — dichiarati con {@link PersonalData} e nel manifesto dati.
 */
@Entity
@Table(schema = "app_fatture", name = "invoice")
@SQLRestriction("deleted_at is null")
public class Invoice extends BaseTenantEntity {

    @Column(nullable = false, updatable = false)
    private String number;

    @PersonalData(
            category = "identità cliente",
            purpose = "emissione e gestione fatture",
            legalBasis = "contratto",
            retention = "10 anni dall'emissione (obblighi fiscali)")
    @Column(name = "customer_name", nullable = false)
    private String customerName;

    @PersonalData(
            category = "contatto cliente",
            purpose = "recapito e invio fattura",
            legalBasis = "contratto",
            retention = "10 anni dall'emissione (obblighi fiscali)")
    @Column(name = "customer_email")
    private String customerEmail;

    @Column(name = "issue_date", nullable = false)
    private LocalDate issueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvoiceStatus status = InvoiceStatus.draft;

    @Column(nullable = false)
    private String currency = "EUR";

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL)
    private List<InvoiceLine> lines = new ArrayList<>();

    protected Invoice() {
        // richiesto da JPA
    }

    public Invoice(String number, String customerName, String customerEmail, LocalDate issueDate, String currency) {
        this.number = number;
        this.customerName = customerName;
        this.customerEmail = customerEmail;
        this.issueDate = issueDate;
        if (currency != null && !currency.isBlank()) {
            this.currency = currency;
        }
    }

    /** Aggiunge una riga mantenendo la relazione bidirezionale; ricalcola il totale. */
    public void addLine(InvoiceLine line) {
        line.setInvoice(this);
        lines.add(line);
        recomputeTotal();
    }

    public void recomputeTotal() {
        this.totalAmount = lines.stream()
                .map(InvoiceLine::getLineAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public String getNumber() {
        return number;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public LocalDate getIssueDate() {
        return issueDate;
    }

    public InvoiceStatus getStatus() {
        return status;
    }

    public void setStatus(InvoiceStatus status) {
        this.status = status;
    }

    public String getCurrency() {
        return currency;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public List<InvoiceLine> getLines() {
        return lines;
    }
}
