package app.appgrove.core.platform;

import app.appgrove.commons.web.Page;
import app.appgrove.commons.web.PageRequest;
import app.appgrove.core.platform.InvitationDtos.CreateInvitation;
import app.appgrove.core.platform.InvitationDtos.InvitationView;
import io.quarkus.security.Authenticated;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * API inviti del tenant. Tenant-scoped automatico (discriminator). Gestione riservata a owner/admin.
 * Il token grezzo è restituito SOLO alla creazione; su DB resta solo il suo hash (single-use).
 */
@Path("/api/platform/v1/invitations")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class InvitationResource {

    /** Si possono invitare solo admin/member: l'owner nasce con l'account (signup). */
    private static final Set<UserRole> INVITABLE = EnumSet.of(UserRole.admin, UserRole.member);
    private static final Duration TTL = Duration.ofDays(7);

    @Inject
    InvitationRepository repository;

    @Inject
    UserRepository users;

    @Inject
    CallerContext caller;

    @POST
    @RolesAllowed({Roles.OWNER, Roles.ADMIN})
    @Transactional
    public Response create(@Valid CreateInvitation body) {
        UserRole role = parseInvitableRole(body.role());
        String email = body.email().trim();
        if (repository.existsPendingForEmail(email)) {
            throw new ClientErrorException("Esiste già un invito pending per " + email, Response.Status.CONFLICT);
        }
        String token = InvitationTokens.newToken();
        UUID invitedBy = users.findByCognitoSub(caller.subject()).map(User::getId).orElse(null);
        Invitation invitation = new Invitation(
                email, role, InvitationTokens.hash(token), Instant.now().plus(TTL), invitedBy);
        repository.persist(invitation);
        repository.flush(); // forza INSERT: id e tenant_id valorizzati prima della response
        return Response.status(Response.Status.CREATED)
                .entity(InvitationView.created(invitation, token))
                .build();
    }

    @GET
    @RolesAllowed({Roles.OWNER, Roles.ADMIN})
    public Page<InvitationView> listPending(@QueryParam("page") Integer page, @QueryParam("size") Integer size) {
        PageRequest pr = PageRequest.of(page, size);
        List<InvitationView> content = repository.find("status", InvitationStatus.pending)
                .page(io.quarkus.panache.common.Page.of(pr.page(), pr.size()))
                .list()
                .stream()
                .map(InvitationView::from)
                .toList();
        return Page.of(content, pr, repository.count("status", InvitationStatus.pending));
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed({Roles.OWNER, Roles.ADMIN})
    @Transactional
    public Response revoke(@PathParam("id") UUID id) {
        Invitation invitation = repository.findById(id);
        if (invitation == null) {
            throw new NotFoundException("Invito non trovato");
        }
        invitation.setStatus(InvitationStatus.revoked);
        return Response.noContent().build();
    }

    private static UserRole parseInvitableRole(String value) {
        UserRole role;
        try {
            role = UserRole.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Ruolo non valido: " + value);
        }
        if (!INVITABLE.contains(role)) {
            throw new BadRequestException("Ruolo non invitabile: " + value);
        }
        return role;
    }
}
