package app.appgrove.@@APP_ID@@;

import app.appgrove.commons.entitlement.RequiresEntitlement;
import app.appgrove.commons.web.Page;
import app.appgrove.commons.web.PageRequest;
import app.appgrove.@@APP_ID@@.ItemDtos.CreateItem;
import app.appgrove.@@APP_ID@@.ItemDtos.CreateLine;
import app.appgrove.@@APP_ID@@.ItemDtos.ItemView;
import app.appgrove.@@APP_ID@@.ItemDtos.UpdateItem;
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
 * API del dominio segnaposto dell'app @@APP_NAME@@. Tenant-scoped automatico (discriminator): ogni
 * query filtra {@code WHERE tenant_id = ?} senza codice manuale. La creazione consuma quota
 * (metrica {@code @@METRIC@@}).
 *
 * <p>{@code @RequiresEntitlement} (UC 0027): la risorsa passa dal gate entitlement (402) — accesso
 * negato (subscription canceled/paused, app disabilitata) → 402 prima ancora del gate quota.
 * L'endpoint di stato quota ({@code QuotaResource}) resta volutamente <b>fuori</b> dal gate
 * (informativo): togliergli l'esenzione renderebbe illeggibile il consumo proprio a chi ha perso
 * l'accesso e deve capire perché.
 */
@Path("/api/@@APP_ID@@/v1/items")
@Authenticated
@RequiresEntitlement
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ItemResource {

    @Inject
    ItemRepository repository;

    @Inject
    @@APP_CLASS@@QuotaService quota;

    @Inject
    CallerContext caller;

    @GET
    @RolesAllowed({@@ROLES_ALLOWED@@})
    public Page<ItemView> list(@QueryParam("page") Integer page, @QueryParam("size") Integer size) {
        PageRequest pr = PageRequest.of(page, size);
        List<ItemView> content = repository.findAll()
                .page(io.quarkus.panache.common.Page.of(pr.page(), pr.size()))
                .list()
                .stream()
                .map(ItemView::from)
                .toList();
        return Page.of(content, pr, repository.count());
    }

    @GET
    @Path("/{id}")
    @RolesAllowed({@@ROLES_ALLOWED@@})
    public ItemView get(@PathParam("id") UUID id) {
        return ItemView.from(require(id));
    }

    @POST
    @RolesAllowed({@@ROLES_ALLOWED@@})
    @Transactional
    public Response create(@Valid CreateItem body) {
        // Gate quota PRIMA dell'azione che consuma quota (#09 A5/F30): a tetto raggiunto → 429.
        quota.checkAndReserve(@@APP_CLASS@@QuotaService.METRIC);

        LocalDate recordedOn = body.recordedOn() != null ? body.recordedOn() : LocalDate.now(ZoneOffset.UTC);
        String code = repository.nextCode(caller.tenantId().toString(), recordedOn.getYear());

        Item item = new Item(code, body.contactName(), body.contactEmail(), recordedOn, body.currency());
        if (body.lines() != null) {
            for (CreateLine line : body.lines()) {
                item.addLine(new ItemLine(line.description(), line.quantity(), line.unitAmount()));
            }
        }
        repository.persist(item);
        return Response.status(Response.Status.CREATED).entity(ItemView.from(item)).build();
    }

    @PATCH
    @Path("/{id}")
    @RolesAllowed({@@ROLES_ALLOWED@@})
    @Transactional
    public ItemView update(@PathParam("id") UUID id, @Valid UpdateItem body) {
        Item item = require(id);
        if (body.contactName() != null) {
            item.setContactName(body.contactName());
        }
        if (body.contactEmail() != null) {
            item.setContactEmail(body.contactEmail());
        }
        if (body.status() != null) {
            item.setStatus(parseStatus(body.status()));
        }
        return ItemView.from(item);
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed({@@ROLES_ALLOWED@@})
    @Transactional
    public Response delete(@PathParam("id") UUID id) {
        require(id).markDeleted();
        return Response.noContent().build();
    }

    private Item require(UUID id) {
        Item item = repository.findById(id);
        if (item == null) {
            throw new NotFoundException("Record non trovato");
        }
        return item;
    }

    private static ItemStatus parseStatus(String value) {
        try {
            return ItemStatus.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Stato non valido: " + value);
        }
    }
}
