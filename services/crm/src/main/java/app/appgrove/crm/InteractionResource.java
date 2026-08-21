package app.appgrove.crm;

import app.appgrove.commons.access.AppRole;
import app.appgrove.commons.access.RequiresAppRole;
import app.appgrove.commons.entitlement.RequiresEntitlement;
import app.appgrove.crm.CrmDtos.CreateInteraction;
import app.appgrove.crm.CrmDtos.InteractionView;
import io.quarkus.security.Authenticated;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

/**
 * API delle interazioni di un contatto (UC 0054): storico di telefonate, email, incontri e note.
 * Stessi varchi concentrici della {@link ContactResource} — diritto dell'account (402), <b>ruolo sulla
 * applicazione</b> ({@code viewer} per leggere, {@code editor} per scrivere — UC 0099), posto (403) — e
 * nessun confronto fra ruoli scritto qui. Le interazioni sono annidate sotto il contatto e ne condividono
 * l'isolamento per account.
 */
@Path("/api/crm/v1/contacts/{contactId}/interactions")
@Authenticated
@RequiresEntitlement
@RequiresAppRole(AppRole.viewer)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class InteractionResource {

    @Inject
    ContactRepository contacts;

    @Inject
    InteractionRepository interactions;

    @Inject
    SeatAccess seatAccess;

    @GET
    @RolesAllowed({Roles.OWNER, Roles.ADMIN, Roles.MEMBER})
    public List<InteractionView> list(@PathParam("contactId") UUID contactId) {
        seatAccess.requireActiveSeat();
        requireContact(contactId);
        return interactions.forContact(contactId).list().stream().map(InteractionView::from).toList();
    }

    @POST
    @RequiresAppRole(AppRole.editor)
    @RolesAllowed({Roles.OWNER, Roles.ADMIN, Roles.MEMBER})
    @Transactional
    public Response create(@PathParam("contactId") UUID contactId, @Valid CreateInteraction body) {
        seatAccess.requireActiveSeat();
        Contact contact = requireContact(contactId);
        LocalDate occurredOn = body.occurredOn() != null ? body.occurredOn() : LocalDate.now(ZoneOffset.UTC);
        Interaction interaction = new Interaction(contact, parseKind(body.kind()), occurredOn, body.note());
        interactions.persist(interaction);
        return Response.status(Response.Status.CREATED).entity(InteractionView.from(interaction)).build();
    }

    private Contact requireContact(UUID contactId) {
        Contact contact = contacts.findById(contactId);
        if (contact == null) {
            throw new NotFoundException("Contatto non trovato");
        }
        return contact;
    }

    /** Tipo interazione: null/blank = {@code note} di default; valore ignoto = 400. */
    private static InteractionKind parseKind(String value) {
        if (value == null || value.isBlank()) {
            return InteractionKind.note;
        }
        try {
            return InteractionKind.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Tipo di interazione non valido: " + value);
        }
    }
}
