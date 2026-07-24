package app.appgrove.crm;

import app.appgrove.commons.entitlement.RequiresEntitlement;
import app.appgrove.commons.web.Page;
import app.appgrove.commons.web.PageRequest;
import app.appgrove.crm.CrmDtos.ContactView;
import app.appgrove.crm.CrmDtos.CreateContact;
import app.appgrove.crm.CrmDtos.UpdateContact;
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
import java.util.List;
import java.util.UUID;

/**
 * API dei contatti del mini-CRM (UC 0054). Tenant-scoped automatico (discriminator): ogni query filtra
 * {@code WHERE tenant_id = ?} senza codice manuale.
 *
 * <p>Due gate concentrici, in ordine:
 * <ol>
 *   <li>{@code @RequiresEntitlement} (UC 0027): accesso dell'account all'app (402 se scaduto/disabilitato);</li>
 *   <li>{@link SeatAccess}: possesso di un <b>posto</b> da parte dell'utente (403 se non ne ha uno).</li>
 * </ol>
 * Tutti e tre i ruoli ({@code owner/admin/member}) possono gestire i contatti: la differenza fra ruoli
 * riguarda la gestione dei posti ({@link SeatResource}), non l'uso del CRM. I contatti <b>non</b> consumano
 * quota: la metrica {@code seats} conta le persone abilitate, non i dati che immettono.
 */
@Path("/api/crm/v1/contacts")
@Authenticated
@RequiresEntitlement
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ContactResource {

    @Inject
    ContactRepository repository;

    @Inject
    SeatAccess seatAccess;

    @GET
    @RolesAllowed({Roles.OWNER, Roles.ADMIN, Roles.MEMBER})
    public Page<ContactView> list(
            @QueryParam("q") String q,
            @QueryParam("stage") String stage,
            @QueryParam("page") Integer page,
            @QueryParam("size") Integer size) {
        seatAccess.requireActiveSeat();
        PageRequest pr = PageRequest.of(page, size);
        var query = repository.search(q, parseStage(stage));
        List<ContactView> content = query
                .page(io.quarkus.panache.common.Page.of(pr.page(), pr.size()))
                .list()
                .stream()
                .map(ContactView::from)
                .toList();
        return Page.of(content, pr, query.count());
    }

    @GET
    @Path("/{id}")
    @RolesAllowed({Roles.OWNER, Roles.ADMIN, Roles.MEMBER})
    public ContactView get(@PathParam("id") UUID id) {
        seatAccess.requireActiveSeat();
        return ContactView.from(require(id));
    }

    @POST
    @RolesAllowed({Roles.OWNER, Roles.ADMIN, Roles.MEMBER})
    @Transactional
    public Response create(@Valid CreateContact body) {
        seatAccess.requireActiveSeat();
        Contact contact = new Contact(
                body.displayName(), body.email(), body.phone(), body.organization(), body.notes());
        repository.persist(contact);
        return Response.status(Response.Status.CREATED).entity(ContactView.from(contact)).build();
    }

    @PATCH
    @Path("/{id}")
    @RolesAllowed({Roles.OWNER, Roles.ADMIN, Roles.MEMBER})
    @Transactional
    public ContactView update(@PathParam("id") UUID id, @Valid UpdateContact body) {
        seatAccess.requireActiveSeat();
        Contact contact = require(id);
        if (body.displayName() != null) {
            contact.setDisplayName(body.displayName());
        }
        if (body.email() != null) {
            contact.setEmail(body.email());
        }
        if (body.phone() != null) {
            contact.setPhone(body.phone());
        }
        if (body.organization() != null) {
            contact.setOrganization(body.organization());
        }
        if (body.stage() != null) {
            contact.setStage(parseStageRequired(body.stage()));
        }
        if (body.notes() != null) {
            contact.setNotes(body.notes());
        }
        return ContactView.from(contact);
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed({Roles.OWNER, Roles.ADMIN, Roles.MEMBER})
    @Transactional
    public Response delete(@PathParam("id") UUID id) {
        seatAccess.requireActiveSeat();
        require(id).markDeleted();
        return Response.noContent().build();
    }

    private Contact require(UUID id) {
        Contact contact = repository.findById(id);
        if (contact == null) {
            throw new NotFoundException("Contatto non trovato");
        }
        return contact;
    }

    /** Stato per il filtro: null/blank = nessun filtro; valore ignoto = 400. */
    private static ContactStage parseStage(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return parseStageRequired(value);
    }

    private static ContactStage parseStageRequired(String value) {
        try {
            return ContactStage.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Stato non valido: " + value);
        }
    }
}
