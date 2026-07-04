package app.appgrove.core.support;

import app.appgrove.commons.persistence.BaseTenantEntity;
import app.appgrove.commons.privacy.PersonalData;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.SQLRestriction;

/**
 * Messaggio del thread di un ticket (UC 0034, #13 D21). Tenant-scoped come il ticket; l'autore
 * applicativo è {@code created_by} (sub del JWT), {@code author} distingue il lato del thread
 * (utente / admin / sistema).
 */
@Entity
@Table(schema = "platform", name = "support_ticket_message")
@SQLRestriction("deleted_at is null")
public class SupportTicketMessage extends BaseTenantEntity {

    @Column(name = "ticket_id", nullable = false, updatable = false)
    private UUID ticketId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private TicketAuthor author;

    @PersonalData(
            category = "contenuto libero (testo dei messaggi del thread di supporto)",
            purpose = "gestione delle richieste di supporto e di esercizio dei diritti (#13 D21)",
            retention = "24 mesi dalla chiusura del ticket (#13 E)")
    @Column(nullable = false, length = 4000)
    private String body;

    protected SupportTicketMessage() {
        // richiesto da JPA
    }

    public SupportTicketMessage(UUID ticketId, TicketAuthor author, String body) {
        this.ticketId = ticketId;
        this.author = author;
        this.body = body;
    }

    public UUID getTicketId() {
        return ticketId;
    }

    public TicketAuthor getAuthor() {
        return author;
    }

    public String getBody() {
        return body;
    }
}
