package app.appgrove.core.support;

import app.appgrove.commons.email.EmailTemplateRenderer;
import io.agroal.api.AgroalDataSource;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * Notifiche email del ticketing (UC 0034 · UC 0075). Tre messaggi, tutti resi dal renderer unico
 * della piattaforma ({@link TicketEmailRenderer}, che poggia su {@code services/commons}, UC 0085):
 *
 * <ul>
 *   <li><b>conferma di apertura</b> a chi ha aperto la richiesta — con il termine di legge quando è
 *       un'istanza privacy;
 *   <li><b>avviso di aggiornamento</b> a chi ha aperto la richiesta, quando la piattaforma risponde
 *       o cambia stato;
 *   <li><b>avviso alla casella di assistenza</b> quando nasce un ticket o il cliente scrive.
 * </ul>
 *
 * <p><b>Il contenuto del filo non viaggia mai per posta.</b> Le email dicono che c'è un
 * aggiornamento e portano alla pagina, dove l'accesso è già protetto: è minimizzazione, ed evita
 * che una richiesta delicata finisca in chiaro nella casella di posta di chi l'ha scritta. Fa
 * eccezione l'oggetto della richiesta, che serve a riconoscerla.
 *
 * <p>In dev le email vanno a Mailpit (come il servizio auth in locale); il relay SES nel cloud è
 * infrastruttura tracciata (UC 0034 "Punti aperti"). <b>Fail-soft</b>: l'email è best-effort e non
 * deve mai far fallire l'operazione sul ticket — errori registrati e inghiottiti.
 */
@ApplicationScoped
public class TicketNotifier {

    private static final Logger LOG = Logger.getLogger(TicketNotifier.class);

    /** Data della scadenza legale nelle email: giorno, non istante — è un termine, non un orario. */
    private static final DateTimeFormatter DUE_DATE =
            DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(ZoneOffset.UTC);

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

    /** Recapito di chi ha aperto la richiesta: indirizzo e lingua, letti insieme. */
    private record Recipient(String email, String locale) {}

    @Inject
    Mailer mailer;

    @Inject
    AgroalDataSource ds;

    @Inject
    TicketEmailRenderer renderer;

    /** Casella della piattaforma che riceve i nuovi ticket/le risposte degli utenti. */
    @ConfigProperty(name = "appgrove.support.inbox")
    String supportInbox;

    /** Base pubblica del backoffice: la pagina Supporto è la destinazione delle email al cliente. */
    @ConfigProperty(name = "appgrove.support.backoffice-url")
    String backofficeUrl;

    /** Base pubblica della console di amministrazione: destinazione delle email alla casella. */
    @ConfigProperty(name = "appgrove.support.admin-url")
    String adminUrl;

    /** Nuovo ticket o risposta dell'utente → casella di assistenza della piattaforma. */
    public void notifySupportInbox(TicketRef ticket) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("ticketId", ticket.id().toString());
        values.put("tenantId", ticket.tenantId());
        values.put("ticketType", ticket.type().name());
        values.put("ticketSubject", ticket.subject());
        values.put("ticketStatus", ticket.status().name());
        values.put("actionUrl", adminUrl + "/tickets/" + ticket.id());
        // La casella di assistenza è interna: si scrive sempre in italiano, la lingua del progetto.
        send(supportInbox, "it", TicketEmailRenderer.INBOX, values, ticket.id());
    }

    /**
     * Conferma di apertura a chi ha aperto la richiesta (UC 0075 §4.3). Sui ticket privacy il testo
     * dice il termine di legge e la sua data: è anche la prova che il cliente è stato informato.
     */
    public void notifyTicketOpened(TicketRef ticket, Instant dueAt) {
        Recipient recipient = requester(ticket);
        if (recipient == null) {
            return;
        }
        boolean privacy = ticket.type() == TicketType.privacy && dueAt != null;
        Map<String, String> values = new LinkedHashMap<>();
        values.put("ticketSubject", ticket.subject());
        values.put("actionUrl", backofficeUrl + "/support");
        if (privacy) {
            values.put("dueDate", DUE_DATE.format(dueAt));
        }
        send(recipient.email(), recipient.locale(),
                privacy ? TicketEmailRenderer.OPENED_PRIVACY : TicketEmailRenderer.OPENED,
                values, ticket.id());
    }

    /** Risposta/cambio stato dalla piattaforma → email a chi ha aperto il ticket. */
    public void notifyRequester(TicketRef ticket) {
        Recipient recipient = requester(ticket);
        if (recipient == null) {
            return;
        }
        Map<String, String> values = new LinkedHashMap<>();
        values.put("ticketSubject", ticket.subject());
        values.put("actionUrl", backofficeUrl + "/support");
        send(recipient.email(), recipient.locale(), TicketEmailRenderer.UPDATED, values, ticket.id());
    }

    /**
     * Recapito di chi ha aperto il ticket: {@code created_by} = sub del JWT → riga
     * identità ⋈ appartenenza (lettura JDBC con conto esplicito, perché chi chiama può essere
     * l'operatore di piattaforma, fuori dal conto del ticket).
     */
    private Recipient requester(TicketRef ticket) {
        if (ticket.createdBy() == null) {
            LOG.warnf("ticket.notify nessun richiedente noto ticket_id=%s tenant_id=%s",
                    ticket.id(), ticket.tenantId());
            return null;
        }
        try (Connection c = ds.getConnection();
                PreparedStatement ps = c.prepareStatement(
                        // Il recapito sta sull'identità; l'appartenenza è la prova che quella persona
                        // è del conto del biglietto (UC 0116). Il biglietto resta dell'account in cui
                        // è stato aperto: nessuna riassegnazione se la persona appartiene anche altrove.
                        "select i.email, i.locale from platform.membership m"
                                + " join platform.identity i on i.id = m.identity_id"
                                + " where m.tenant_id = ? and i.cognito_sub = ?"
                                + " and m.deleted_at is null and i.deleted_at is null")) {
            ps.setString(1, ticket.tenantId());
            ps.setString(2, ticket.createdBy());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    LOG.warnf("ticket.notify nessuna email per il richiedente ticket_id=%s tenant_id=%s",
                            ticket.id(), ticket.tenantId());
                    return null;
                }
                return new Recipient(rs.getString(1), rs.getString(2));
            }
        } catch (SQLException e) {
            LOG.warnf(e, "ticket.notify lettura recapito fallita ticket_id=%s", ticket.id());
            return null;
        }
    }

    private void send(String to, String locale, String messageKey, Map<String, String> values, UUID ticketId) {
        try {
            EmailTemplateRenderer.Rendered r = renderer.render(locale, messageKey, values);
            mailer.send(Mail.withText(to, r.subject(), r.text()).setHtml(r.html()));
            LOG.infof("ticket.notify.sent ticket_id=%s message=%s", ticketId, messageKey);
        } catch (RuntimeException e) {
            LOG.warnf(e, "ticket.notify invio email fallito ticket_id=%s message=%s (best-effort, si prosegue)",
                    ticketId, messageKey);
        }
    }
}
