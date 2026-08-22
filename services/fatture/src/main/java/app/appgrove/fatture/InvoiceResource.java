package app.appgrove.fatture;

import app.appgrove.commons.access.AppRole;
import app.appgrove.commons.access.RequiresAppRole;
import app.appgrove.commons.entitlement.RequiresEntitlement;
import app.appgrove.commons.web.Page;
import app.appgrove.commons.web.PageRequest;
import app.appgrove.fatture.InvoiceDtos.CreateInvoice;
import app.appgrove.fatture.InvoiceDtos.CreateLine;
import app.appgrove.fatture.InvoiceDtos.InvoiceView;
import app.appgrove.fatture.InvoiceDtos.UpdateInvoice;
import io.quarkus.security.Authenticated;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
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
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

/**
 * API fatture del tenant. Tenant-scoped automatico (discriminator): ogni query filtra
 * {@code WHERE tenant_id = ?} senza codice manuale. La creazione consuma quota (metrica {@code fatture}).
 *
 * <p>Due varchi, nell'ordine in cui una persona può rimediare:
 * <ul>
 *   <li>{@code @RequiresEntitlement} (UC 0027): diritto dell'<b>account</b> all'applicazione (402 se
 *       l'abbonamento è disdetto o sospeso, o se l'app è disabilitata) — prima ancora del gate quota;</li>
 *   <li>{@link RequiresAppRole} (UC 0099, classificato da UC 0101): <b>ruolo della persona su questa
 *       applicazione</b> — letture {@code viewer} dalla classe, operazioni dispositive {@code editor} dal
 *       metodo. La creazione consuma quota, quindi sarebbe dispositiva anche se non scrivesse nulla.</li>
 * </ul>
 *
 * <p>{@code @RolesAllowed} elenca invece i ruoli di <b>piattaforma</b> e comprende tutti quelli esistenti:
 * dice soltanto «appartieni a un account» e non decide nulla qui (vedi {@link Roles}). La classificazione
 * completa delle operazioni sta in {@link FattureOperationsContract}, e {@code AppOperationsContractTest}
 * diventa rosso se una rotta nuova non la dichiara.
 *
 * <p>L'endpoint di stato quota ({@code QuotaResource}) resta volutamente <b>fuori</b> dai varchi: è
 * informativo ed è dichiarato esente nel documento delle operazioni.
 */
@Path("/api/fatture/v1/invoices")
@Authenticated
@RequiresEntitlement
@RequiresAppRole(AppRole.viewer)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class InvoiceResource {

    @Inject
    InvoiceRepository repository;

    @Inject
    FattureQuotaService quota;

    @Inject
    CallerContext caller;

    @GET
    @RolesAllowed({Roles.OWNER, Roles.ADMIN, Roles.MEMBER})
    public Page<InvoiceView> list(@QueryParam("page") Integer page, @QueryParam("size") Integer size) {
        PageRequest pr = PageRequest.of(page, size);
        List<InvoiceView> content = repository.findAll()
                .page(io.quarkus.panache.common.Page.of(pr.page(), pr.size()))
                .list()
                .stream()
                .map(InvoiceView::from)
                .toList();
        return Page.of(content, pr, repository.count());
    }

    @GET
    @Path("/{id}")
    @RolesAllowed({Roles.OWNER, Roles.ADMIN, Roles.MEMBER})
    public InvoiceView get(@PathParam("id") UUID id) {
        return InvoiceView.from(require(id));
    }

    @POST
    @RequiresAppRole(AppRole.editor)
    @RolesAllowed({Roles.OWNER, Roles.ADMIN, Roles.MEMBER})
    @Transactional
    public Response create(@Valid CreateInvoice body) {
        // Gate quota PRIMA dell'azione che consuma quota (#09 A5/F30): a tetto raggiunto → 429.
        quota.checkAndReserve(FattureQuotaService.METRIC);

        LocalDate issueDate = body.issueDate() != null ? body.issueDate() : LocalDate.now(ZoneOffset.UTC);
        String number = repository.nextNumber(caller.tenantId().toString(), issueDate.getYear());

        Invoice invoice = new Invoice(number, body.customerName(), body.customerEmail(), issueDate, body.currency());
        if (body.lines() != null) {
            for (CreateLine line : body.lines()) {
                invoice.addLine(new InvoiceLine(line.description(), line.quantity(), line.unitAmount()));
            }
        }
        repository.persist(invoice);
        return Response.status(Response.Status.CREATED).entity(InvoiceView.from(invoice)).build();
    }

    @PATCH
    @RequiresAppRole(AppRole.editor)
    @Path("/{id}")
    @RolesAllowed({Roles.OWNER, Roles.ADMIN, Roles.MEMBER})
    @Transactional
    public InvoiceView update(@PathParam("id") UUID id, @Valid UpdateInvoice body) {
        Invoice invoice = require(id);
        if (body.customerName() != null) {
            invoice.setCustomerName(body.customerName());
        }
        if (body.customerEmail() != null) {
            invoice.setCustomerEmail(body.customerEmail());
        }
        if (body.status() != null) {
            invoice.setStatus(parseStatus(body.status()));
        }
        return InvoiceView.from(invoice);
    }

    @DELETE
    @RequiresAppRole(AppRole.editor)
    @Path("/{id}")
    @RolesAllowed({Roles.OWNER, Roles.ADMIN, Roles.MEMBER})
    @Transactional
    public Response delete(@PathParam("id") UUID id) {
        require(id).markDeleted();
        return Response.noContent().build();
    }

    private Invoice require(UUID id) {
        Invoice invoice = repository.findById(id);
        if (invoice == null) {
            throw new NotFoundException("Fattura non trovata");
        }
        return invoice;
    }

    private static InvoiceStatus parseStatus(String value) {
        try {
            return InvoiceStatus.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Stato non valido: " + value);
        }
    }
}
