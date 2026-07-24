package app.appgrove.crm;

import app.appgrove.commons.persistence.BaseTenantEntity;
import app.appgrove.commons.privacy.PersonalData;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import org.hibernate.annotations.SQLRestriction;

/**
 * Interazione datata con un {@link Contact} (tenant-scoped, entità figlia): una telefonata, una email,
 * un incontro o una semplice nota. {@code note} è testo libero e <b>dato personale</b> (può riferirsi
 * al contatto o a terzi citati): dichiarato con {@link PersonalData} e nel manifesto dati.
 */
@Entity
@Table(schema = "app_crm", name = "interaction")
@SQLRestriction("deleted_at is null")
public class Interaction extends BaseTenantEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "contact_id", nullable = false, updatable = false)
    private Contact contact;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InteractionKind kind = InteractionKind.note;

    @Column(name = "occurred_on", nullable = false)
    private LocalDate occurredOn;

    @PersonalData(
            category = "contenuto dell'interazione (testo libero)",
            purpose = "storico della relazione commerciale con i contatti del tenant",
            legalBasis = "contratto",
            retention = "fino a cancellazione da parte del tenant o chiusura dell'account")
    @Column(name = "note")
    private String note;

    protected Interaction() {
        // richiesto da JPA
    }

    public Interaction(Contact contact, InteractionKind kind, LocalDate occurredOn, String note) {
        this.contact = contact;
        if (kind != null) {
            this.kind = kind;
        }
        this.occurredOn = occurredOn;
        this.note = note;
    }

    public Contact getContact() {
        return contact;
    }

    public InteractionKind getKind() {
        return kind;
    }

    public LocalDate getOccurredOn() {
        return occurredOn;
    }

    public String getNote() {
        return note;
    }
}
