package app.appgrove.core.support;

import app.appgrove.commons.persistence.BaseTenantEntity;
import app.appgrove.commons.privacy.PersonalData;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.SQLRestriction;

/**
 * Ticket di supporto in-house (UC 0034, #13 D21). Tenant-scoped (discriminator → l'utente vede solo
 * i ticket del proprio tenant); chi lo ha aperto = {@code created_by} (sub del JWT, via
 * AuditListener). Le scritture di sistema/admin — fuori dal tenant del chiamante — passano da
 * {@link TicketStore} (JDBC, tenant esplicito). Il tipo {@code privacy} porta la scadenza legale
 * {@code due_at} (art. 12: riscontro entro 1 mese) e può essere auto-creato dagli eventi
 * (export FAILED → {@code export_job_id}, indice unico = idempotenza).
 */
@Entity
@Table(schema = "platform", name = "support_ticket")
@SQLRestriction("deleted_at is null")
public class SupportTicket extends BaseTenantEntity {

    /** Scadenza legale del riscontro sui ticket privacy: 1 mese (art. 12 GDPR). */
    public static final Duration PRIVACY_SLA = Duration.ofDays(30);

    /** Retention dei ticket chiusi (#13 E): 24 mesi dalla chiusura, poi hard-delete dello sweeper. */
    public static final Duration RETENTION = Duration.ofDays(730);

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private TicketType type;

    @PersonalData(
            category = "contenuto libero (oggetto della richiesta di supporto/privacy)",
            purpose = "gestione delle richieste di supporto e di esercizio dei diritti (#13 D21)",
            retention = "24 mesi dalla chiusura del ticket (#13 E)")
    @Column(nullable = false, length = 200)
    private String subject;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private TicketPriority priority = TicketPriority.normal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private TicketStatus status = TicketStatus.open;

    /** Scadenza legale (solo ticket privacy): creazione + 1 mese (art. 12). */
    @Column(name = "due_at")
    private Instant dueAt;

    /** Export job che ha generato l'auto-ticket (export FAILED); null per i ticket aperti a mano. */
    @Column(name = "export_job_id")
    private UUID exportJobId;

    /** Istante di chiusura/risoluzione: da qui decorre la retention di 24 mesi. */
    @Column(name = "closed_at")
    private Instant closedAt;

    protected SupportTicket() {
        // richiesto da JPA
    }

    public SupportTicket(TicketType type, String subject) {
        this.type = type;
        this.subject = subject;
    }

    public TicketType getType() {
        return type;
    }

    public String getSubject() {
        return subject;
    }

    public TicketPriority getPriority() {
        return priority;
    }

    public void setPriority(TicketPriority priority) {
        this.priority = priority;
    }

    public TicketStatus getStatus() {
        return status;
    }

    /** Cambia stato mantenendo coerente {@code closed_at} (decorrenza retention). */
    public void moveTo(TicketStatus next, Instant now) {
        this.status = next;
        this.closedAt = next.isTerminal() ? now : null;
    }

    public Instant getDueAt() {
        return dueAt;
    }

    public void setDueAt(Instant dueAt) {
        this.dueAt = dueAt;
    }

    public UUID getExportJobId() {
        return exportJobId;
    }

    public Instant getClosedAt() {
        return closedAt;
    }
}
