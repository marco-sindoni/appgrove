package app.appgrove.core.platform;

import app.appgrove.commons.access.AppRole;
import app.appgrove.commons.access.MyAppAccessView;
import app.appgrove.commons.entitlement.EntitlementView;
import app.appgrove.core.billing.EntitlementReadModel;
import app.appgrove.core.catalog.App;
import app.appgrove.core.catalog.AppRepository;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.jboss.logging.Logger;

/**
 * «Dove posso entrare, e con che ruolo» (UC 0099): {@code GET /api/platform/v1/me/app-access}. La lettura
 * che il menu laterale usa per sapere quali applicazioni mostrare (UC 0107) e che ogni servizio di
 * applicazione usa, attraverso il varco condiviso, per sapere che potere ha chi sta chiamando.
 *
 * <p><b>Il ruolo per applicazione non è nel token</b>, ed è la decisione centrale della storia: nel token
 * un cambio di ruolo avrebbe effetto solo al rinnovo, e un account con dieci applicazioni gonfierebbe ogni
 * richiesta. Questa lettura è il prezzo di quella scelta; il ricavo è che una revoca si sente in pochi
 * secondi.
 *
 * <p><b>Nessun parametro identifica la persona</b>, e non è una semplificazione: dice dove può entrare
 * <b>chi chiama</b>, sempre. Account e persona arrivano dal JWT verificato (invariante #1). La domanda
 * «dove può entrare quella <i>altra</i> persona» è un'altra lettura, che appartiene alla schermata di
 * gestione (UC 0111) e ha bisogno del proprio controllo di permessi.
 *
 * <p>Compaiono solo le applicazioni che hanno <b>insieme</b> il diritto dell'account e l'accesso della
 * persona: due condizioni, entrambe necessarie. Un accesso a una applicazione che l'account non ha più non
 * apre nulla, e il diritto dell'account non basta a far entrare chi non è stato abilitato — tranne
 * l'<b>owner</b>, che ha accesso implicito a tutte, col ruolo massimo.
 */
@Path("/api/platform/v1/me")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
public class MeAppAccessResource {

    private static final Logger LOG = Logger.getLogger(MeAppAccessResource.class);

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

    @GET
    @Path("/app-access")
    @Transactional
    public List<MyAppAccessView> myAppAccess() {
        Membership me = currentMembership();
        Set<String> entitled = entitledSlugs();

        List<MyAppAccessView> out = new ArrayList<>();
        if (me.getRole() == MembershipRole.owner) {
            // L'owner non ha righe di accesso: le ha tutte per costruzione, col ruolo massimo (UC 0098 §5).
            // Deriva dal diritto dell'account, quindi un'applicazione appena acquistata gli compare subito.
            for (String slug : entitled) {
                appBySlug(slug).ifPresent(app -> out.add(view(app, AppRole.admin)));
            }
        } else {
            for (AppAccess access : accesses.findByIdentity(me.getIdentityId())) {
                App app = apps.findById(access.getAppId());
                // Applicazione sparita dal catalogo, o diritto dell'account decaduto: la riga di accesso
                // resta (riattivando, gli accessi tornano validi senza ricostruirli) ma non apre nulla.
                if (app != null && entitled.contains(app.getSlug())) {
                    out.add(view(app, access.getRole()));
                }
            }
        }
        LOG.debugf("me.app-access risolte %d applicazioni per user_id=%s", out.size(), caller.subject());
        return out;
    }

    /**
     * Slug delle applicazioni a cui l'<b>account</b> ha diritto, dal read-model degli entitlement: la
     * regola è quella già unica di {@code EntitlementAccess} e qui si <b>consuma</b>, non si riscrive
     * (stessa scelta di {@link AppAccessResource}). Una seconda copia della condizione divergerebbe.
     *
     * <p><b>La voce di catalogo di piattaforma dei posti è esclusa</b> (UC 0103), e lo è <b>a monte</b>:
     * il read-model degli entitlement elenca soltanto le righe di catalogo con {@code kind = application},
     * quindi la voce dei posti non arriva mai fin qui. È il motivo per cui in questo metodo non si vede
     * alcuna condizione sui posti, e non è una dimenticanza: la regola sta nel punto unico che governa
     * l'accesso, com'è già per la condizione di accesso stessa. Una seconda copia qui divergerebbe.
     *
     * <p>L'esclusione, però, va <b>provata su questa superficie</b>, non solo su quella a monte: è il menu
     * laterale del cliente a essere sbagliato se la voce compare, e un collaudo che si accontenti di
     * provare il read-model non se ne accorgerebbe.
     */
    private Set<String> entitledSlugs() {
        Set<String> slugs = new LinkedHashSet<>();
        for (EntitlementView entitlement : entitlements.forCurrentTenant().entitlements()) {
            slugs.add(entitlement.appSlug());
        }
        return slugs;
    }

    private Optional<App> appBySlug(String slug) {
        return apps.findBySlug(slug);
    }

    private static MyAppAccessView view(App app, AppRole role) {
        return new MyAppAccessView(app.getId(), app.getSlug(), app.getName(), role.name());
    }

    private Membership currentMembership() {
        UUID identityId = identities.findByCognitoSub(caller.subject())
                .orElseThrow(() -> new NotFoundException("Profilo utente non trovato"))
                .getId();
        return memberships.findByIdentity(identityId)
                .orElseThrow(() -> new NotFoundException("Profilo utente non trovato"));
    }
}
