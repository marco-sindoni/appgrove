package app.appgrove.core.platform;

import app.appgrove.commons.audit.AuditLogger;
import app.appgrove.commons.web.Page;
import app.appgrove.commons.web.PageRequest;
import app.appgrove.core.billing.EntitlementInvalidationPublisher;
import app.appgrove.core.billing.EntitlementReadModel;
import app.appgrove.core.catalog.App;
import app.appgrove.core.catalog.AppRepository;
import app.appgrove.core.platform.UserDtos.UpdateMe;
import app.appgrove.core.platform.UserDtos.UpdateUser;
import app.appgrove.core.platform.UserDtos.UserAppView;
import app.appgrove.core.platform.UserDtos.UserView;
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
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jboss.logging.Logger;

/**
 * API delle persone dell'account. Dopo UC 0116 la riga di partenza è l'<b>appartenenza</b>
 * (tenant-scoped automatico: ogni query filtra {@code WHERE tenant_id = ?} senza codice manuale) e
 * i dati della persona arrivano dall'<b>identità</b> collegata. Il percorso è sempre
 * appartenenza → identità, mai identità → appartenenze: <b>nessuna risposta di questa API rivela a
 * quali altri account una persona appartenga</b> (UC 0116 §8).
 *
 * <p>Il contratto esposto non cambia (stessi percorsi, stessi campi, stesso {@code id} della
 * persona): cambia solo da dove vengono i campi.
 *
 * <p>Gestione riservata all'<b>owner</b>, e da UC 0100 <b>soltanto</b> a lui: la tolleranza per i token
 * già emessi che portano {@code admin} (UC 0098) è ritirata <b>qui</b>, in anticipo su UC 0113, perché
 * governare le persone dell'account è esattamente il potere che questa storia riserva all'owner. La
 * difesa vera è questa annotazione, non la guardia della rotta nel backoffice. Le operazioni su
 * {@code /me} restano invece aperte a chiunque appartenga a un account: sono i propri dati.
 */
@Path("/api/platform/v1/users")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserResource {

    private static final Logger LOG = Logger.getLogger(UserResource.class);

    @Inject
    MembershipRepository memberships;

    @Inject
    IdentityRepository identities;

    @Inject
    CallerContext caller;

    @Inject
    AppAccessRepository accesses;

    @Inject
    AppRepository apps;

    @Inject
    EntitlementReadModel entitlements;

    @Inject
    EntitlementInvalidationPublisher invalidation;

    @Inject
    AuditLogger audit;

    /**
     * Le persone dell'account, ciascuna con le applicazioni su cui è abilitata (UC 0100): è la lettura
     * che alimenta l'elenco unico della schermata «Members».
     */
    @GET
    @RolesAllowed(Roles.OWNER)
    public Page<UserView> list(@QueryParam("page") Integer page, @QueryParam("size") Integer size) {
        PageRequest pr = PageRequest.of(page, size);
        AccountApps accountApps = new AccountApps();
        List<UserView> content = memberships.findAll()
                .page(io.quarkus.panache.common.Page.of(pr.page(), pr.size()))
                .list()
                .stream()
                .map(m -> view(m, accountApps))
                .filter(java.util.Objects::nonNull)
                .toList();
        return Page.of(content, pr, memberships.count());
    }

    @GET
    @Path("/me")
    public UserView me() {
        Membership membership = currentMembership();
        return UserView.from(membership, requireIdentity(membership));
    }

    /**
     * Rettifica self-service del proprio profilo (art. 16, UC 0033): ogni persona, qualunque ruolo,
     * corregge il proprio nome visualizzato. Il nome sta sull'identità, quindi la correzione vale in
     * tutti gli account a cui la persona appartiene — è il suo nome, non quello che un account le
     * assegna. Il cambio indirizzo è dei flussi auth (UC 0017, differito).
     */
    @PATCH
    @Path("/me")
    @Transactional
    public UserView updateMe(@Valid UpdateMe body) {
        Membership membership = currentMembership();
        Identity identity = requireIdentity(membership);
        identity.setDisplayName(body.displayName());
        LOG.infof("user.rectify tenant_id=%s user_id=%s", membership.getTenantId(), caller.subject());
        return UserView.from(membership, identity);
    }

    @GET
    @Path("/{id}")
    @RolesAllowed(Roles.OWNER)
    public UserView get(@PathParam("id") UUID id) {
        Membership membership = requireMembership(id);
        // Stessa forma della riga dell'elenco, applicazioni comprese: una lettura di dettaglio che
        // mostrasse meno della riga da cui si arriva sarebbe una trappola per chi la consuma.
        return UserView.from(membership, requireIdentity(membership), new AccountApps().of(membership));
    }

    @PATCH
    @Path("/{id}")
    @RolesAllowed(Roles.OWNER)
    @Transactional
    public UserView update(@PathParam("id") UUID id, UpdateUser body) {
        Membership membership = requireMembership(id);
        Identity identity = requireIdentity(membership);
        if (body.role() != null) {
            MembershipRole role = parseRole(body.role());
            if (role != MembershipRole.owner) {
                requireNotLastOwner(membership, "retrocedere");
            }
            membership.setRole(role);
        }
        if (body.status() != null) {
            // L'owner governa l'appartenenza al PROPRIO account, non l'identità della persona: una
            // sospensione decisa qui non la tocca negli altri account a cui appartiene.
            MembershipStatus status = parseStatus(body.status());
            if (status != MembershipStatus.active) {
                requireNotLastOwner(membership, "sospendere");
            }
            membership.setStatus(status);
        }
        if (body.displayName() != null) {
            identity.setDisplayName(body.displayName());
        }
        // evento audit (UC 0006) solo per i cambi privilegiati (ruolo/stato), non per la
        // rettifica del nome; details con soli identificativi opachi (mai email/nome).
        if (body.role() != null || body.status() != null) {
            Map<String, String> details = new HashMap<>();
            details.put("user_id", identity.getId().toString());
            details.put("actor", caller.subject());
            if (body.role() != null) {
                details.put("role", body.role());
            }
            if (body.status() != null) {
                details.put("status", body.status());
            }
            audit.success("member.updated", details);
        }
        return UserView.from(membership, identity);
    }

    /**
     * Uscita della persona da <b>questo</b> account: si chiude l'<b>appartenenza</b>, non l'identità.
     * Le altre appartenenze della persona non ne sanno nulla, e l'identità resta cancellabile solo
     * quando resta senza appartenenze (UC 0116 §6, ciclo di conservazione di UC 0033).
     */
    @DELETE
    @Path("/{id}")
    @RolesAllowed(Roles.OWNER)
    @Transactional
    public Response delete(@PathParam("id") UUID id) {
        Membership membership = requireMembership(id);
        requireNotLastOwner(membership, "rimuovere");
        membership.markDeleted();
        // Gli accessi alle applicazioni escono con la persona (UC 0098 §5): un permesso che sopravvive a
        // chi non fa più parte dell'account tornerebbe valido il giorno in cui quella persona rientra —
        // silenziosamente, e con i poteri di prima. La cancellazione è logica, come quella
        // dell'appartenenza: la storia resta leggibile, il permesso no.
        int revoked = 0;
        for (AppAccess access : accesses.findByIdentity(membership.getIdentityId())) {
            access.markDeleted();
            revoked++;
        }
        if (revoked > 0) {
            // I servizi delle applicazioni tengono una copia locale del ruolo (UC 0099): senza questo
            // evento la persona appena uscita continuerebbe a lavorare per la durata della copia. Si
            // invalida su TUTTE le applicazioni, perché i suoi accessi potevano essere su più di una.
            invalidation.invalidateAllApps(membership.getTenantId(), "member.removed");
        }
        audit.success("member.removed", Map.of(
                "user_id", membership.getIdentityId().toString(),
                "actor", caller.subject(),
                "app_accesses_revoked", Integer.toString(revoked)));
        return Response.noContent().build();
    }

    // ── helper ───────────────────────────────────────────────────────────────

    /** L'appartenenza della persona che sta chiamando, dentro l'account del token. */
    private Membership currentMembership() {
        Identity identity = identities.findByCognitoSub(caller.subject())
                .orElseThrow(() -> new NotFoundException("Profilo utente non trovato"));
        return memberships.findByIdentity(identity.getId())
                .orElseThrow(() -> new NotFoundException("Profilo utente non trovato"));
    }

    /**
     * L'appartenenza di questo account per la persona indicata. {@code id} è l'identificativo della
     * <b>persona</b> (l'identità): un identificativo di una persona che non appartiene a questo
     * account non trova nulla — nessun 404 distinguibile da «non esiste», così l'assenza non dice
     * se quella persona esiste altrove.
     */
    private Membership requireMembership(UUID identityId) {
        return memberships.findByIdentity(identityId)
                .orElseThrow(() -> new NotFoundException("Utente non trovato"));
    }

    private Identity requireIdentity(Membership membership) {
        Identity identity = identities.findById(membership.getIdentityId());
        if (identity == null) {
            throw new NotFoundException("Profilo utente non trovato");
        }
        return identity;
    }

    /** Vista di un'appartenenza; {@code null} se l'identità collegata non è più viva (riga orfana). */
    private UserView view(Membership membership, AccountApps accountApps) {
        Identity identity = identities.findById(membership.getIdentityId());
        return identity == null ? null : UserView.from(membership, identity, accountApps.of(membership));
    }

    /**
     * Indice «persona → applicazioni su cui è abilitata», costruito <b>una volta per richiesta</b>.
     *
     * <p>Il vincolo che ne dettava la forma: il costo della lettura <b>non deve crescere con il numero
     * di persone</b>. Con trenta persone in elenco, una interrogazione per riga sono trenta
     * interrogazioni — e l'elenco delle persone è la schermata che si apre per prima quando qualcosa
     * non va. Qui si leggono <b>tutte</b> le righe di accesso dell'account in una volta (sono al più
     * persone × applicazioni, e la lettura è già filtrata per account dal discriminatore) e si
     * raggruppano in memoria.
     *
     * <p>I diritti dell'account si leggono <b>solo se</b> nell'elenco compare un owner, perché servono
     * soltanto a lui: il suo accesso è implicito su tutte le applicazioni a cui l'account ha diritto e
     * non ha righe di permesso da leggere (UC 0098 §5).
     */
    private final class AccountApps {

        private final Map<UUID, String> nameByApp = new HashMap<>();
        private final Map<UUID, List<UserAppView>> byIdentity = new HashMap<>();
        private List<UserAppView> ownerApps;

        private AccountApps() {
            // Solo le APPLICAZIONI (UC 0103): la colonna «applicazioni» dell'elenco delle persone dice su
            // che cosa ognuno è abilitato a lavorare. L'owner ha accesso implicito a tutto ciò che
            // l'account ha diritto di usare, quindi senza questa esclusione la voce di piattaforma dei
            // posti comparirebbe fra le sue applicazioni — e chi legge la schermata dei membri leggerebbe
            // «l'owner è abilitato ai Posti dell'account», che non vuol dire niente.
            for (App app : apps.listApplications()) {
                nameByApp.put(app.getId(), app.getSlug());
            }
            for (AppAccess access : accesses.listAll()) {
                String name = nameByApp.get(access.getAppId());
                if (name == null) {
                    // Applicazione uscita dal catalogo: la riga di permesso non nomina più nulla, e
                    // mostrare un identificativo nudo non aiuterebbe nessuno.
                    continue;
                }
                byIdentity
                        .computeIfAbsent(access.getIdentityId(), k -> new ArrayList<>())
                        .add(new UserAppView(access.getAppId(), name, access.getRole().name(), false));
            }
            byIdentity.values().forEach(list -> list.sort(Comparator.comparing(UserAppView::app)));
        }

        /** Le applicazioni di quella persona, mai {@code null} (l'elenco vuoto è uno stato legittimo). */
        List<UserAppView> of(Membership membership) {
            if (membership.getRole() == MembershipRole.owner) {
                return ownerApps();
            }
            return byIdentity.getOrDefault(membership.getIdentityId(), List.of());
        }

        private List<UserAppView> ownerApps() {
            if (ownerApps == null) {
                Map<String, UUID> idByName = new HashMap<>();
                nameByApp.forEach((id, name) -> idByName.put(name, id));
                ownerApps = entitlements.forCurrentTenant().entitlements().stream()
                        .map(e -> new UserAppView(idByName.get(e.appSlug()), e.appSlug(), null, true))
                        .sorted(Comparator.comparing(UserAppView::app))
                        .toList();
            }
            return ownerApps;
        }
    }

    /**
     * <b>L'ultimo owner è intoccabile</b>: non si rimuove, non si retrocede, non si sospende (UC 0098
     * §5). Fino a questa change il divieto viveva soltanto nell'interfaccia, come comando disabilitato
     * ({@code isLastOwner} nella schermata dei membri): un divieto che sta solo lì è un divieto che si
     * aggira con una richiesta diretta, e lascerebbe un account senza nessuno che possa governarlo —
     * uno stato da cui non si torna indietro senza intervento manuale. Ora il rifiuto arriva dal
     * servizio, e l'interfaccia resta una cortesia.
     *
     * <p>Rifiuto {@code 409}: è un conflitto con lo stato dell'account, non una mancanza di permessi
     * (chi chiede è tipicamente proprio l'owner, che di permessi ne ha tutti).
     */
    private void requireNotLastOwner(Membership membership, String verb) {
        if (membership.getRole() == MembershipRole.owner && memberships.countOwners() <= 1) {
            throw new ClientErrorException(
                    "Non si può " + verb + " l'ultimo owner dell'account.", Response.Status.CONFLICT);
        }
    }

    private static MembershipRole parseRole(String value) {
        try {
            return MembershipRole.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Ruolo non valido: " + value);
        }
    }

    private static MembershipStatus parseStatus(String value) {
        try {
            return MembershipStatus.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Stato non valido: " + value);
        }
    }
}
