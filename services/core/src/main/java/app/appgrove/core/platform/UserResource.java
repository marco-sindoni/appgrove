package app.appgrove.core.platform;

import app.appgrove.commons.web.Page;
import app.appgrove.commons.web.PageRequest;
import app.appgrove.core.platform.UserDtos.UpdateUser;
import app.appgrove.core.platform.UserDtos.UserView;
import io.quarkus.security.Authenticated;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.UUID;

/**
 * API utenti del tenant. Tenant-scoped automatico (discriminator): ogni query filtra
 * {@code WHERE tenant_id = ?} senza codice manuale. Gestione riservata a owner/admin.
 */
@Path("/api/platform/v1/users")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserResource {

    @Inject
    UserRepository repository;

    @Inject
    CallerContext caller;

    @GET
    @RolesAllowed({Roles.OWNER, Roles.ADMIN})
    public Page<UserView> list(@QueryParam("page") Integer page, @QueryParam("size") Integer size) {
        PageRequest pr = PageRequest.of(page, size);
        List<UserView> content = repository.findAll()
                .page(io.quarkus.panache.common.Page.of(pr.page(), pr.size()))
                .list()
                .stream()
                .map(UserView::from)
                .toList();
        return Page.of(content, pr, repository.count());
    }

    @GET
    @Path("/me")
    public UserView me() {
        return repository.findByCognitoSub(caller.subject())
                .map(UserView::from)
                .orElseThrow(() -> new NotFoundException("Profilo utente non trovato"));
    }

    @GET
    @Path("/{id}")
    @RolesAllowed({Roles.OWNER, Roles.ADMIN})
    public UserView get(@PathParam("id") UUID id) {
        return UserView.from(require(id));
    }

    @PATCH
    @Path("/{id}")
    @RolesAllowed({Roles.OWNER, Roles.ADMIN})
    @Transactional
    public UserView update(@PathParam("id") UUID id, UpdateUser body) {
        User user = require(id);
        if (body.role() != null) {
            user.setRole(parseRole(body.role()));
        }
        if (body.status() != null) {
            user.setStatus(parseStatus(body.status()));
        }
        if (body.displayName() != null) {
            user.setDisplayName(body.displayName());
        }
        return UserView.from(user);
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed({Roles.OWNER, Roles.ADMIN})
    @Transactional
    public Response delete(@PathParam("id") UUID id) {
        User user = require(id);
        user.markDeleted();
        return Response.noContent().build();
    }

    private User require(UUID id) {
        User user = repository.findById(id);
        if (user == null) {
            throw new NotFoundException("Utente non trovato");
        }
        return user;
    }

    private static UserRole parseRole(String value) {
        try {
            return UserRole.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Ruolo non valido: " + value);
        }
    }

    private static UserStatus parseStatus(String value) {
        try {
            return UserStatus.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Stato non valido: " + value);
        }
    }
}
