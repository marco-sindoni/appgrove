package app.appgrove.core.support;

import app.appgrove.core.platform.CallerContext;
import app.appgrove.core.platform.Roles;
import app.appgrove.core.support.TicketDtos.MessageView;
import app.appgrove.core.support.TicketDtos.OpenTicket;
import app.appgrove.core.support.TicketDtos.PostMessage;
import app.appgrove.core.support.TicketDtos.TicketDetailView;
import app.appgrove.core.support.TicketDtos.TicketView;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.ResponseStatus;

/**
 * API ticket lato utente (UC 0034, #13 D21): apri ticket, lista dei propri, thread con risposta.
 * Aperta a tutti i ruoli del tenant (il supporto serve anche ai member). Il tenant arriva dal JWT
 * (invariante #1) e le letture sono tenant-filtered dal discriminator (#2) → i ticket di un altro
 * tenant sono un 404. Canale di supporto, quindi <b>esente dai gate di enforcement</b> come i
 * diritti GDPR (#09 F31): niente {@code @RequiresEntitlement} — deve funzionare anche con
 * subscription scaduta (es. per esercitare i diritti).
 */
@Path("/api/platform/v1/tickets")
@RolesAllowed({Roles.OWNER, Roles.ADMIN, Roles.MEMBER})
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TicketResource {

    private static final Logger LOG = Logger.getLogger(TicketResource.class);

    @Inject
    SupportTicketRepository tickets;

    @Inject
    SupportTicketMessageRepository messages;

    @Inject
    TicketNotifier notifier;

    @Inject
    CallerContext caller;

    @POST
    @Transactional
    @ResponseStatus(201)
    public TicketView open(@Valid OpenTicket body) {
        SupportTicket ticket = new SupportTicket(body.type(), body.subject());
        if (body.type() == TicketType.privacy) {
            ticket.setDueAt(Instant.now().plus(SupportTicket.PRIVACY_SLA));
        }
        tickets.persist(ticket);
        messages.persist(new SupportTicketMessage(ticket.getId(), TicketAuthor.user, body.message()));
        LOG.infof("ticket.opened ticket_id=%s type=%s tenant_id=%s user_id=%s",
                ticket.getId(), ticket.getType(), caller.tenantId(), caller.subject());
        notifier.notifySupportInbox(ref(ticket), body.message());
        return TicketView.from(ticket);
    }

    @GET
    public List<TicketView> list() {
        return tickets.listRecent().stream().map(TicketView::from).toList();
    }

    @GET
    @Path("/{id}")
    public TicketDetailView get(@PathParam("id") UUID id) {
        SupportTicket ticket = load(id);
        return new TicketDetailView(
                TicketView.from(ticket),
                messages.byTicket(ticket.getId()).stream().map(MessageView::from).toList());
    }

    @POST
    @Path("/{id}/messages")
    @Transactional
    @ResponseStatus(201)
    public MessageView reply(@PathParam("id") UUID id, @Valid PostMessage body) {
        SupportTicket ticket = load(id);
        if (ticket.getStatus() == TicketStatus.closed) {
            throw new ClientErrorException(
                    "Ticket chiuso: aprine uno nuovo per continuare", Response.Status.CONFLICT);
        }
        SupportTicketMessage message =
                new SupportTicketMessage(ticket.getId(), TicketAuthor.user, body.body());
        messages.persist(message);
        // una risposta dell'utente su un ticket risolto lo riapre (il thread non è concluso)
        if (ticket.getStatus() == TicketStatus.resolved) {
            ticket.moveTo(TicketStatus.open, Instant.now());
        }
        LOG.infof("ticket.user-reply ticket_id=%s tenant_id=%s user_id=%s",
                ticket.getId(), caller.tenantId(), caller.subject());
        notifier.notifySupportInbox(ref(ticket), body.body());
        return MessageView.from(message);
    }

    private SupportTicket load(UUID id) {
        SupportTicket ticket = tickets.findById(id);
        if (ticket == null) {
            throw new NotFoundException("Ticket non trovato");
        }
        return ticket;
    }

    /**
     * Riferimento per il notifier costruito da JWT + entità: tenant e utente arrivano dal
     * {@link CallerContext} (invariante #1) e NON dall'entità — il discriminator {@code @TenantId}
     * viene valorizzato da Hibernate solo all'insert (flush), quindi su un ticket appena
     * {@code persist()} sarebbe ancora null (bug "tenant null" trovato in collaudo).
     */
    private TicketNotifier.TicketRef ref(SupportTicket ticket) {
        return new TicketNotifier.TicketRef(
                ticket.getId(),
                caller.tenantId().toString(),
                ticket.getType(),
                ticket.getSubject(),
                ticket.getStatus(),
                caller.subject());
    }
}
