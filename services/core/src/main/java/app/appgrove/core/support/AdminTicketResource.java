package app.appgrove.core.support;

import app.appgrove.core.observability.AwsConsoleLinks;
import app.appgrove.core.platform.CallerContext;
import app.appgrove.core.platform.Roles;
import app.appgrove.core.support.TicketDtos.AdminMessageView;
import app.appgrove.core.support.TicketDtos.AdminTicketDetailView;
import app.appgrove.core.support.TicketDtos.AdminTicketView;
import app.appgrove.core.support.TicketDtos.PostMessage;
import app.appgrove.core.support.TicketDtos.UpdateTicket;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.ResponseStatus;

/**
 * Sezione «Ticket» della console di amministrazione (UC 0075): la <b>coda cross-account</b> delle
 * richieste di assistenza, il filo di conversazione e le due sole scritture ammesse — rispondere e
 * cambiare stato/priorità.
 *
 * <p>Questi endpoint stavano sotto {@code /admin/gdpr/tickets} perché il ticketing è nato come
 * strumento della console dei diritti (UC 0034). Quel percorso raccontava una falsità: una richiesta
 * di assistenza generica non ha nulla a che vedere con l'esercizio dei diritti. Con questa storia il
 * ticketing ha una casa propria; la console dei diritti continua ad <b>aggregare</b> i soli ticket
 * privacy e a rimandare qui.
 *
 * <p>Lettura cross-tenant: è l'<b>eccezione documentata</b> all'invariante del filtro per conto,
 * ammessa solo per il ruolo {@code platform-admin} e solo attraverso {@link TicketStore}, che porta
 * il conto in modo esplicito. Nessuna impersonation (#03 15): il contenuto dei messaggi non è mai
 * modificabile, e ogni operazione lascia una riga nei registri.
 */
@Path("/api/platform/v1/admin/tickets")
@RolesAllowed(Roles.PLATFORM_ADMIN)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AdminTicketResource {

    private static final Logger LOG = Logger.getLogger(AdminTicketResource.class);

    @Inject
    TicketStore tickets;

    @Inject
    TicketNotifier notifier;

    @Inject
    AwsConsoleLinks links;

    @Inject
    CallerContext caller;

    /** Coda cross-account, filtri opzionali; ordinamento per scadenza deciso da {@link TicketStore}. */
    @GET
    public List<AdminTicketView> list(
            @QueryParam("type") TicketType type,
            @QueryParam("status") TicketStatus status,
            @QueryParam("priority") TicketPriority priority) {
        return tickets.list(type, status, priority).stream().map(this::view).toList();
    }

    @GET
    @Path("/{id}")
    public AdminTicketDetailView get(@PathParam("id") UUID id) {
        TicketStore.TicketRow row = load(id);
        List<AdminMessageView> thread = tickets.thread(id).stream()
                .map(m -> new AdminMessageView(m.id(), m.author(), m.body(), m.createdAt()))
                .toList();
        return new AdminTicketDetailView(view(row), thread);
    }

    /**
     * Risposta di chi assiste: il ticket passa <b>in attesa dell'utente</b> e chi l'ha aperto riceve
     * l'avviso. È la transizione automatica del ciclo di vita (UC 0075): {@code in_progress} resta
     * lo stato che l'operatore mette a mano quando ci sta lavorando senza aver ancora risposto.
     */
    @POST
    @Path("/{id}/messages")
    @ResponseStatus(201)
    public AdminMessageView reply(@PathParam("id") UUID id, @Valid PostMessage body) {
        TicketStore.TicketRow row = load(id);
        if (row.status() == TicketStatus.closed) {
            throw new ClientErrorException("Ticket chiuso", Response.Status.CONFLICT);
        }
        TicketStore.MessageRow message =
                tickets.addMessage(row, TicketAuthor.admin, caller.subject(), body.body());
        tickets.update(id, TicketStatus.waiting_user, row.priority(), caller.subject(), Instant.now());
        LOG.infof("ticket.admin-reply ticket_id=%s tenant_id=%s actor=%s", id, row.tenantId(), caller.subject());
        notifier.notifyRequester(TicketNotifier.TicketRef.of(load(id)));
        return new AdminMessageView(message.id(), message.author(), message.body(), message.createdAt());
    }

    /** Cambio stato/priorità (ops sicure: mai editing del contenuto). Chi ha aperto è avvisato. */
    @PATCH
    @Path("/{id}")
    public AdminTicketView update(@PathParam("id") UUID id, @Valid UpdateTicket body) {
        load(id);
        tickets.update(id, body.status(), body.priority(), caller.subject(), Instant.now());
        TicketStore.TicketRow updated = load(id);
        LOG.infof("ticket.admin-update ticket_id=%s status=%s priority=%s tenant_id=%s actor=%s",
                id, body.status(), body.priority(), updated.tenantId(), caller.subject());
        notifier.notifyRequester(TicketNotifier.TicketRef.of(updated));
        return view(updated);
    }

    private AdminTicketView view(TicketStore.TicketRow row) {
        return new AdminTicketView(
                row.id(), row.tenantId(), row.accountName(), row.type(), row.source(), row.subject(),
                row.priority(), row.status(), row.flaggedForReview(), row.dueAt(), row.exportJobId(),
                row.closedAt(), row.createdAt(),
                links.logsInsightsUrl(Map.of("ticket_id", row.id().toString())).orElse(null));
    }

    private TicketStore.TicketRow load(UUID id) {
        return tickets.find(id).orElseThrow(() -> new NotFoundException("Ticket non trovato"));
    }
}
