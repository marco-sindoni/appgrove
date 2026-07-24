package app.appgrove.crm;

import app.appgrove.commons.persistence.BaseTenantEntity;
import app.appgrove.commons.privacy.PersonalData;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLRestriction;

/**
 * Contatto del mini-CRM (UC 0054): una persona di un'organizzazione cliente, i cui dati sono immessi
 * dal tenant. Tenant-scoped: il filtro {@code WHERE tenant_id = ?} è automatico (discriminator).
 *
 * <p>{@code displayName}, {@code email}, {@code phone} e {@code notes} sono <b>dati personali di
 * terzi</b> (base giuridica: contratto verso il tenant titolare, #13 A2) — dichiarati con
 * {@link PersonalData} e, obbligatoriamente, nel manifesto dati {@code docs/compliance/manifests/crm.yaml}.
 * {@code notes} è testo libero: è il campo dove un utente può inavvertitamente scrivere categorie
 * particolari; l'informativa del tenant titolare deve coprirlo (vedi manifesto).
 */
@Entity
@Table(schema = "app_crm", name = "contact")
@SQLRestriction("deleted_at is null")
public class Contact extends BaseTenantEntity {

    @PersonalData(
            category = "identità del contatto",
            purpose = "gestione della relazione commerciale con i contatti del tenant",
            legalBasis = "contratto",
            retention = "fino a cancellazione da parte del tenant o chiusura dell'account")
    @Column(name = "display_name", nullable = false)
    private String displayName;

    @PersonalData(
            category = "recapito del contatto (email)",
            purpose = "gestione della relazione commerciale con i contatti del tenant",
            legalBasis = "contratto",
            retention = "fino a cancellazione da parte del tenant o chiusura dell'account")
    @Column(name = "email")
    private String email;

    @PersonalData(
            category = "recapito del contatto (telefono)",
            purpose = "gestione della relazione commerciale con i contatti del tenant",
            legalBasis = "contratto",
            retention = "fino a cancellazione da parte del tenant o chiusura dell'account")
    @Column(name = "phone")
    private String phone;

    @Column(name = "organization")
    private String organization;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContactStage stage = ContactStage.lead;

    @PersonalData(
            category = "annotazioni sul contatto (testo libero)",
            purpose = "gestione della relazione commerciale con i contatti del tenant",
            legalBasis = "contratto",
            retention = "fino a cancellazione da parte del tenant o chiusura dell'account")
    @Column(name = "notes")
    private String notes;

    protected Contact() {
        // richiesto da JPA
    }

    public Contact(String displayName, String email, String phone, String organization, String notes) {
        this.displayName = displayName;
        this.email = email;
        this.phone = phone;
        this.organization = organization;
        this.notes = notes;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getOrganization() {
        return organization;
    }

    public void setOrganization(String organization) {
        this.organization = organization;
    }

    public ContactStage getStage() {
        return stage;
    }

    public void setStage(ContactStage stage) {
        this.stage = stage;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
