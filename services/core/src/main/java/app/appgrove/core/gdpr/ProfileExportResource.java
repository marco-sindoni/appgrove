package app.appgrove.core.gdpr;

import app.appgrove.core.platform.Account;
import app.appgrove.core.platform.AccountRepository;
import app.appgrove.core.platform.CallerContext;
import app.appgrove.core.platform.Invitation;
import app.appgrove.core.platform.InvitationRepository;
import app.appgrove.core.platform.User;
import app.appgrove.core.platform.UserRepository;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jboss.logging.Logger;

/**
 * Export del profilo del <b>singolo utente</b> (artt. 15/20, UC 0033): i dati personali
 * dell'utente <i>in quanto persona</i> vivono nel core (profilo, email, inviti) — capability della
 * piattaforma, <b>senza interpellare le app</b> (decisione UC 0032: i dati delle app appartengono
 * all'account, non al singolo operatore; quell'export è {@code /gdpr/exports}, owner/admin).
 * Download JSON sincrono, ogni ruolo, solo i propri dati ({@code sub} dal JWT, invariante #1).
 * Diritto esente dai gate (#09 F31): niente {@code @RequiresEntitlement}.
 */
@Path("/api/platform/v1/users/me/export")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
public class ProfileExportResource {

    private static final Logger LOG = Logger.getLogger(ProfileExportResource.class);

    @Inject
    UserRepository users;

    @Inject
    AccountRepository accounts;

    @Inject
    InvitationRepository invitations;

    @Inject
    CallerContext caller;

    @GET
    public Response export() {
        User user = users.findByCognitoSub(caller.subject())
                .orElseThrow(() -> new NotFoundException("Profilo utente non trovato"));
        Account account = accounts.findById(caller.tenantId());

        Map<String, Object> document = new LinkedHashMap<>();
        document.put("generatedAt", Instant.now().toString());
        document.put("profile", profile(user));
        document.put("account", account == null ? null : Map.of(
                "id", account.getId().toString(),
                "name", account.getName()));
        document.put("invitations", invitationsFor(user.getEmail()));

        LOG.infof("gdpr.profile-export tenant_id=%s user_id=%s", user.getTenantId(), caller.subject());
        return Response.ok(document)
                .header("Content-Disposition",
                        "attachment; filename=\"appgrove-profilo-" + user.getId() + ".json\"")
                .build();
    }

    private static Map<String, Object> profile(User user) {
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("id", user.getId().toString());
        profile.put("cognitoSub", user.getCognitoSub());
        profile.put("email", user.getEmail());
        profile.put("displayName", user.getDisplayName());
        profile.put("role", user.getRole().name());
        profile.put("status", user.getStatus().name());
        profile.put("createdAt", user.getCreatedAt());
        return profile;
    }

    /** Inviti indirizzati all'email dell'utente nel tenant corrente (lettura tenant-scoped, #2). */
    private List<Map<String, Object>> invitationsFor(String email) {
        return invitations.list("lower(email) = ?1", email.toLowerCase()).stream()
                .map(ProfileExportResource::invitation)
                .toList();
    }

    private static Map<String, Object> invitation(Invitation inv) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", inv.getId().toString());
        row.put("email", inv.getEmail());
        row.put("role", inv.getRole().name());
        row.put("status", inv.getStatus().name());
        row.put("expiresAt", inv.getExpiresAt());
        row.put("createdAt", inv.getCreatedAt());
        return row;
    }
}
