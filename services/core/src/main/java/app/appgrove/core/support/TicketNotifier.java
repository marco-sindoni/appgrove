package app.appgrove.core.support;

import io.agroal.api.AgroalDataSource;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * Notifiche email del ticketing (UC 0034, #13 D21): alla casella di supporto quando nasce un
 * ticket o l'utente risponde; a chi ha aperto il ticket quando l'admin risponde o cambia lo stato.
 * In dev le email vanno a Mailpit (come il servizio auth in locale); il relay SES nel cloud è infrastruttura
 * tracciata (UC 0034 "Punti aperti"). <b>Fail-soft</b>: l'email è best-effort e non deve mai far
 * fallire l'operazione sul ticket — errori loggati e inghiottiti.
 */
@ApplicationScoped
public class TicketNotifier {

    private static final Logger LOG = Logger.getLogger(TicketNotifier.class);

    /**
     * Riferimento minimo al ticket per le notifiche. Va costruito da dati <b>certi</b>: la riga
     * letta dal DB (admin/consumer) o JWT + entità lato utente ({@code TicketResource#ref}) — MAI
     * da un'entità appena {@code persist()}: il discriminator {@code @TenantId} è valorizzato solo
     * all'insert e risulterebbe null.
     */
    public record TicketRef(UUID id, String tenantId, TicketType type, String subject, TicketStatus status,
            String createdBy) {

        public static TicketRef of(TicketStore.TicketRow row) {
            return new TicketRef(row.id(), row.tenantId(), row.type(), row.subject(), row.status(),
                    row.createdBy());
        }
    }

    @Inject
    Mailer mailer;

    @Inject
    AgroalDataSource ds;

    /** Casella della piattaforma che riceve i nuovi ticket/le risposte degli utenti. */
    @ConfigProperty(name = "appgrove.support.inbox")
    String supportInbox;

    /** Nuovo ticket o risposta dell'utente → casella di supporto della piattaforma. */
    public void notifySupportInbox(TicketRef ticket, String excerpt) {
        send(supportInbox,
                "[appgrove] Ticket " + ticket.type() + ": " + ticket.subject(),
                "Ticket " + ticket.id() + " (tenant " + ticket.tenantId() + ", stato "
                        + ticket.status() + ")\n\n" + excerpt);
    }

    /** Risposta/cambio stato dell'admin → email a chi ha aperto il ticket (se ha un'email nota). */
    public void notifyRequester(TicketRef ticket, String excerpt) {
        String email = requesterEmail(ticket);
        if (email == null) {
            LOG.warnf("ticket.notify nessuna email per il richiedente ticket_id=%s tenant_id=%s",
                    ticket.id(), ticket.tenantId());
            return;
        }
        send(email,
                "[appgrove] Aggiornamento sul tuo ticket: " + ticket.subject(),
                "Il tuo ticket " + ticket.id() + " è ora \"" + ticket.status() + "\".\n\n"
                        + excerpt + "\n\nPuoi rispondere dalla pagina Supporto del backoffice.");
    }

    /**
     * Email di chi ha aperto il ticket: {@code created_by} = sub del JWT → {@code users.cognito_sub}
     * (lookup JDBC con tenant esplicito: chi chiama può essere l'admin, fuori dal tenant del ticket).
     */
    private String requesterEmail(TicketRef ticket) {
        if (ticket.createdBy() == null) {
            return null;
        }
        try (Connection c = ds.getConnection();
                PreparedStatement ps = c.prepareStatement(
                        "select email from platform.users"
                                + " where tenant_id = ? and cognito_sub = ? and deleted_at is null")) {
            ps.setString(1, ticket.tenantId());
            ps.setString(2, ticket.createdBy());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        } catch (SQLException e) {
            LOG.warnf(e, "ticket.notify lookup email fallito ticket_id=%s", ticket.id());
            return null;
        }
    }

    private void send(String to, String subject, String body) {
        try {
            mailer.send(Mail.withText(to, subject, body));
        } catch (RuntimeException e) {
            LOG.warnf(e, "ticket.notify invio email fallito to=%s (best-effort, si prosegue)", to);
        }
    }
}
