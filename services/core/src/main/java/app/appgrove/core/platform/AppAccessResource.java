package app.appgrove.core.platform;

import app.appgrove.commons.audit.AuditLogger;
import app.appgrove.commons.entitlement.EntitlementView;
import app.appgrove.commons.web.ProblemDetail;
import app.appgrove.core.billing.EntitlementReadModel;
import app.appgrove.core.catalog.App;
import app.appgrove.core.catalog.AppRepository;
import app.appgrove.core.platform.AppAccessDtos.AppAccessView;
import app.appgrove.core.platform.AppAccessDtos.ChangeRole;
import app.appgrove.core.platform.AppAccessDtos.GrantAccess;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.persistence.PersistenceException;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.jboss.logging.Logger;

/**
 * Accessi delle persone dell'account a una applicazione (UC 0098): chi ha accesso, a chi si concede, a
 * chi si cambia ruolo, a chi si revoca. Nessuna schermata la consuma ancora — le superfici sono di
 * UC 0100 (elenco unico) e UC 0111 (gestione utenti dentro l'applicazione).
 *
 * <p><b>Attenzione: qui {@code @RolesAllowed} NON basta, ed è deliberato.</b> Il ruolo su una
 * applicazione <b>non è nel token</b> (UC 0099: un cambio di ruolo avrebbe effetto solo al rinnovo, e
 * dieci applicazioni gonfierebbero ogni richiesta), quindi la protezione è {@code @Authenticated} più la
 * verifica <b>esplicita</b> dentro il metodo, dentro la transazione, con {@link AppAccessRules}. Chi
 * "semplificasse" rimettendo {@code @RolesAllowed} aprirebbe un varco: l'annotazione conosce solo il
 * ruolo di piattaforma, che qui non è sufficiente a decidere.
 *
 * <p>L'<b>owner</b> non ha righe di accesso: gliele si aggiunge in testa in lettura, e ogni tentativo di
 * concedergli, cambiargli o revocargli l'accesso è rifiutato — non è un ruolo di applicazione.
 *
 * <p><b>Nessun evento viene emesso.</b> L'invalidazione della copia locale del ruolo nei servizi delle
 * applicazioni è il meccanismo di UC 0099: i tre punti di scrittura di questa classe sono i luoghi da cui
 * andrà emessa, e lo dicono a voce alta perché quella storia non debba cercarli.
 */
@Path("/api/platform/v1/apps/{appId}/access")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AppAccessResource {

    private static final Logger LOG = Logger.getLogger(AppAccessResource.class);

    /**
     * Identificativi stabili dei rifiuti tipizzati, nel campo {@code type} del corpo problem+json
     * (RFC 9457). Servono perché l'interfaccia distingua i casi <b>senza interpretare un messaggio</b>:
     * il messaggio del server è in italiano, l'interfaccia parla cinque lingue. Sono contratto.
     */
    static final String TYPE_NOT_ENTITLED = "urn:appgrove:app-access:not-entitled";

    static final String TYPE_PERSON_NOT_ACTIVE = "urn:appgrove:app-access:person-not-active";

    static final String TYPE_OWNER_IMPLICIT = "urn:appgrove:app-access:owner-implicit";

    @Inject
    AppAccessRepository accesses;

    @Inject
    MembershipRepository memberships;

    @Inject
    IdentityRepository identities;

    @Inject
    AppRepository apps;

    @Inject
    EntitlementReadModel entitlements;

    @Inject
    CallerContext caller;

    @Inject
    AuditLogger audit;

    // ── lettura ──────────────────────────────────────────────────────────────

    /**
     * Chi ha accesso a questa applicazione, con l'owner <b>in testa</b>. Aperta a chiunque abbia un
     * accesso all'applicazione, anche {@code viewer}: sono le persone del proprio gruppo di lavoro, non
     * un elenco riservato — ma chi non ha accesso all'applicazione non ne conosce nemmeno le persone.
     */
    @GET
    @Transactional
    public Response list(@PathParam("appId") UUID appId) {
        App app = requireApp(appId);
        Membership me = currentMembership();
        Optional<Response> refusal = requireEntitled(app);
        if (refusal.isPresent()) {
            return refusal.get();
        }
        if (!AppAccessRules.canRead(me.getRole(), accesses.roleOf(appId, me.getIdentityId()).orElse(null))) {
            return forbidden("Non hai accesso a questa applicazione.");
        }

        List<AppAccessView> out = new ArrayList<>();
        // L'owner per primo: non ha riga, ma è la persona che ha sempre accesso. Ometterlo darebbe
        // l'impressione che l'applicazione non abbia nessun responsabile.
        for (Membership owner : memberships.owners()) {
            Identity identity = identities.findById(owner.getIdentityId());
            if (identity != null) {
                out.add(new AppAccessView(
                        identity.getId(),
                        identity.getEmail(),
                        identity.getDisplayName(),
                        AppRole.admin.name(),
                        true));
            }
        }
        for (AppAccess access : accesses.findByApp(appId)) {
            Identity identity = identities.findById(access.getIdentityId());
            if (identity != null) {
                out.add(new AppAccessView(
                        identity.getId(),
                        identity.getEmail(),
                        identity.getDisplayName(),
                        access.getRole().name(),
                        false));
            }
        }
        return Response.ok(out).build();
    }

    // ── scrittura ────────────────────────────────────────────────────────────

    /**
     * Concede l'accesso. Un accesso <b>già esistente</b> non è un errore ma un <b>cambio di ruolo</b>
     * (UC 0098 §5): chiedere due operazioni per una intenzione sola scaricherebbe sull'interfaccia una
     * distinzione che il servizio conosce meglio di lei.
     */
    @POST
    @Transactional
    public Response grant(@PathParam("appId") UUID appId, @Valid GrantAccess body) {
        App app = requireApp(appId);
        AppRole role = parseRole(body.role());
        Membership me = currentMembership();
        Optional<Response> refusal = requireEntitled(app);
        if (refusal.isPresent()) {
            return refusal.get();
        }
        if (!AppAccessRules.canManage(me.getRole(), accesses.roleOf(appId, me.getIdentityId()).orElse(null))) {
            return forbidden("Solo l'owner o un admin di questa applicazione possono concedere l'accesso.");
        }

        Membership target = requireActiveTarget(body.identityId());
        if (target == null) {
            return typed(Response.Status.CONFLICT, TYPE_PERSON_NOT_ACTIVE,
                    "La persona non è attiva: l'accesso si concede solo a persone attive.");
        }
        if (target.getRole() == MembershipRole.owner) {
            return typed(Response.Status.CONFLICT, TYPE_OWNER_IMPLICIT,
                    "L'owner ha già accesso a tutte le applicazioni dell'account.");
        }

        Optional<AppAccess> existing = accesses.findOne(appId, target.getIdentityId());
        if (existing.isPresent()) {
            return changeRoleOf(existing.get(), role, appId);
        }

        AppAccess access = new AppAccess(appId, target.getIdentityId(), role, identityOfCaller().getId());
        try {
            // Flush esplicito: l'arbitro dell'unicità della terna è la banca dati, non la lettura
            // appena fatta. Due concessioni simultanee arrivano entrambe qui; una sola vince, l'altra
            // riceve un rifiuto invece di creare una seconda riga.
            accesses.persistAndFlush(access);
        } catch (PersistenceException e) {
            throw new ClientErrorException(
                    "Accesso già presente per questa persona su questa applicazione.", Response.Status.CONFLICT, e);
        }
        // UC 0099: da qui andrà emesso l'evento di invalidazione della copia locale del ruolo.
        audit.success("app_access.granted", Map.of(
                "app_id", appId.toString(),
                "user_id", target.getIdentityId().toString(),
                "role", role.name(),
                "actor", caller.subject()));
        LOG.infof("app_access.granted tenant_id=%s app_id=%s user_id=%s role=%s",
                me.getTenantId(), appId, target.getIdentityId(), role);
        return Response.status(Response.Status.CREATED)
                .entity(view(access))
                .build();
    }

    /** Cambia il ruolo di una persona su questa applicazione. */
    @PUT
    @Path("/{identityId}")
    @Transactional
    public Response changeRole(
            @PathParam("appId") UUID appId, @PathParam("identityId") UUID identityId, @Valid ChangeRole body) {
        App app = requireApp(appId);
        AppRole role = parseRole(body.role());
        Membership me = currentMembership();
        Optional<Response> refusal = requireEntitled(app);
        if (refusal.isPresent()) {
            return refusal.get();
        }
        if (!AppAccessRules.canManage(me.getRole(), accesses.roleOf(appId, me.getIdentityId()).orElse(null))) {
            return forbidden("Solo l'owner o un admin di questa applicazione possono cambiare il ruolo.");
        }
        if (isOwner(identityId)) {
            return typed(Response.Status.CONFLICT, TYPE_OWNER_IMPLICIT,
                    "Il ruolo dell'owner su una applicazione non si cambia: l'accesso gli è implicito.");
        }
        AppAccess access = accesses.findOne(appId, identityId)
                .orElseThrow(() -> new NotFoundException("Accesso non trovato"));
        return changeRoleOf(access, role, appId);
    }

    /** Revoca l'accesso (cancellazione logica). */
    @DELETE
    @Path("/{identityId}")
    @Transactional
    public Response revoke(@PathParam("appId") UUID appId, @PathParam("identityId") UUID identityId) {
        App app = requireApp(appId);
        Membership me = currentMembership();
        Optional<Response> refusal = requireEntitled(app);
        if (refusal.isPresent()) {
            return refusal.get();
        }
        if (!AppAccessRules.canManage(me.getRole(), accesses.roleOf(appId, me.getIdentityId()).orElse(null))) {
            return forbidden("Solo l'owner o un admin di questa applicazione possono revocare l'accesso.");
        }
        if (isOwner(identityId)) {
            return typed(Response.Status.CONFLICT, TYPE_OWNER_IMPLICIT,
                    "L'accesso dell'owner non è revocabile: gli è implicito su tutte le applicazioni.");
        }
        AppAccess access = accesses.findOne(appId, identityId)
                .orElseThrow(() -> new NotFoundException("Accesso non trovato"));
        access.markDeleted();
        // UC 0099: da qui andrà emesso l'evento di invalidazione della copia locale del ruolo.
        audit.success("app_access.revoked", Map.of(
                "app_id", appId.toString(),
                "user_id", identityId.toString(),
                "actor", caller.subject()));
        LOG.infof("app_access.revoked tenant_id=%s app_id=%s user_id=%s",
                me.getTenantId(), appId, identityId);
        return Response.noContent().build();
    }

    // ── helper ───────────────────────────────────────────────────────────────

    private Response changeRoleOf(AppAccess access, AppRole role, UUID appId) {
        AppRole before = access.getRole();
        access.setRole(role);
        if (before != role) {
            // UC 0099: da qui andrà emesso l'evento di invalidazione della copia locale del ruolo.
            audit.success("app_access.role_changed", Map.of(
                    "app_id", appId.toString(),
                    "user_id", access.getIdentityId().toString(),
                    "role", role.name(),
                    "previous_role", before.name(),
                    "actor", caller.subject()));
            LOG.infof("app_access.role_changed tenant_id=%s app_id=%s user_id=%s role=%s",
                    access.getTenantId(), appId, access.getIdentityId(), role);
        }
        return Response.ok(view(access)).build();
    }

    private AppAccessView view(AppAccess access) {
        Identity identity = identities.findById(access.getIdentityId());
        return new AppAccessView(
                access.getIdentityId(),
                identity == null ? null : identity.getEmail(),
                identity == null ? null : identity.getDisplayName(),
                access.getRole().name(),
                false);
    }

    private App requireApp(UUID appId) {
        App app = apps.findById(appId);
        if (app == null) {
            throw new NotFoundException("Applicazione non trovata");
        }
        return app;
    }

    /**
     * L'account ha <b>diritto</b> a questa applicazione? Un accesso a una applicazione che l'account non
     * ha non deve poter esistere. La regola è quella già unica di {@code EntitlementAccess}, consumata
     * qui attraverso il read-model: non se ne scrive una seconda copia.
     */
    private Optional<Response> requireEntitled(App app) {
        boolean entitled = entitlements.forCurrentTenant().entitlements().stream()
                .map(EntitlementView::appSlug)
                .anyMatch(slug -> slug.equals(app.getSlug()));
        if (entitled) {
            return Optional.empty();
        }
        return Optional.of(typed(Response.Status.CONFLICT, TYPE_NOT_ENTITLED,
                "Il tuo account non ha diritto a questa applicazione."));
    }

    /**
     * L'appartenenza <b>attiva</b> della persona bersaglio in <b>questo</b> account, o {@code null} se
     * esiste ma non è attiva. Una persona di un altro account non trova nulla: risposta «non trovato» e
     * non «vietato», perché l'esistenza di utenti di altri account non è un'informazione di chi chiede
     * (UC 0098 §5). L'identificativo arrivato dal chiamante non è mai una prova di appartenenza:
     * l'appartenenza si legge dal modello, dentro l'account del token.
     */
    private Membership requireActiveTarget(UUID identityId) {
        Membership membership = memberships.findByIdentity(identityId)
                .orElseThrow(() -> new NotFoundException("Utente non trovato"));
        Identity identity = identities.findById(membership.getIdentityId());
        if (identity == null) {
            throw new NotFoundException("Utente non trovato");
        }
        boolean active = membership.getStatus() == MembershipStatus.active
                && identity.getStatus() == IdentityStatus.active;
        return active ? membership : null;
    }

    private boolean isOwner(UUID identityId) {
        return memberships.findByIdentity(identityId)
                .map(m -> m.getRole() == MembershipRole.owner)
                .orElse(false);
    }

    private Membership currentMembership() {
        Identity identity = identityOfCaller();
        return memberships.findByIdentity(identity.getId())
                .orElseThrow(() -> new NotFoundException("Profilo utente non trovato"));
    }

    private Identity identityOfCaller() {
        return identities.findByCognitoSub(caller.subject())
                .orElseThrow(() -> new NotFoundException("Profilo utente non trovato"));
    }

    private static AppRole parseRole(String value) {
        try {
            return AppRole.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Ruolo non valido: " + value);
        }
    }

    private static Response forbidden(String detail) {
        return Response.status(Response.Status.FORBIDDEN)
                .type(ProblemDetail.MEDIA_TYPE)
                .entity(ProblemDetail.of(Response.Status.FORBIDDEN.getStatusCode(), "Forbidden", detail))
                .build();
    }

    /**
     * Rifiuto con {@code type} stabile. Restituito come {@link Response} e <b>non</b> sollevato come
     * eccezione perché il mapper delle eccezioni riscrive sempre {@code type} ad {@code about:blank}
     * (stessa ragione, e stesso precedente, degli esiti tipizzati dell'invito — UC 0118).
     */
    private static Response typed(Response.Status status, String type, String detail) {
        return Response.status(status)
                .type(ProblemDetail.MEDIA_TYPE)
                .entity(new ProblemDetail(type, status.getReasonPhrase(), status.getStatusCode(), detail, null, null))
                .build();
    }
}
