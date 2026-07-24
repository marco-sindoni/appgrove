package app.appgrove.crm;

import app.appgrove.commons.entitlement.RequiresEntitlement;
import app.appgrove.commons.quota.QuotaLimitSource;
import app.appgrove.crm.CrmDtos.AssignSeat;
import app.appgrove.crm.CrmDtos.SeatSummary;
import app.appgrove.crm.CrmDtos.SeatView;
import io.quarkus.security.Authenticated;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.jboss.logging.Logger;

/**
 * Gestione dei <b>posti</b> del mini-CRM (UC 0054): chi, dentro l'account, può usare l'app. È la
 * schermata «Membri» vista dal backend.
 *
 * <p><b>Solo owner e admin</b> assegnano e revocano posti ({@code @RolesAllowed}); un member non li
 * gestisce. A differenza del dominio, queste rotte <b>non</b> passano dal varco {@link SeatAccess}: un
 * titolare deve poter liberare un posto anche quando sono tutti occupati (compreso il caso in cui non
 * ne ha uno lui stesso), altrimenti un account pieno resterebbe bloccato.
 *
 * <p>L'assegnazione è l'<b>unico</b> punto che consuma quota: prima di occupare un posto si fa
 * {@code checkAndReserve("seats")} e, a tetto raggiunto, si risponde 429. È qui che l'«invito oltre il
 * posto» di UC 0054 §5 diventa concreto: l'invito al tenant resta libero (è di core), ma il posto non si
 * assegna. La revoca libera immediatamente la giacenza (metrica a giacenza). Ogni cambiamento ripubblica
 * la giacenza verso core ({@link SeatUsagePublisher}) per il gate del downgrade.
 */
@Path("/api/crm/v1/seats")
@Authenticated
@RequiresEntitlement
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SeatResource {

    private static final Logger LOG = Logger.getLogger(SeatResource.class);

    @Inject
    SeatRepository seats;

    @Inject
    CrmQuotaService quota;

    @Inject
    QuotaLimitSource limits;

    @Inject
    CallerContext caller;

    @Inject
    SeatUsagePublisher usage;

    /** Riepilogo posti (occupati / tetto / rimanenti) + elenco, per la schermata «Membri». */
    @GET
    @RolesAllowed({Roles.OWNER, Roles.ADMIN, Roles.MEMBER})
    public SeatSummary summary() {
        long used = seats.count();
        long cap = limits.capFor(caller.tenantId().toString(), CrmQuotaService.METRIC);
        List<SeatView> list = seats.listAll().stream().map(SeatView::from).toList();
        return SeatSummary.of(used, cap, list);
    }

    /** Assegna un posto a un utente dell'account. OWNER/ADMIN. A tetto raggiunto → 429. */
    @POST
    @RolesAllowed({Roles.OWNER, Roles.ADMIN})
    @Transactional
    public Response assign(@Valid AssignSeat body) {
        // Già assegnato? Operazione idempotente: 200 col posto esistente, senza consumare quota.
        var existing = seats.activeForUser(body.userId());
        if (existing.isPresent()) {
            return Response.ok(SeatView.from(existing.get())).build();
        }
        // Gate quota PRIMA di occupare il posto (#09 A5): a tetto raggiunto → 429 problem+json.
        quota.checkAndReserve(CrmQuotaService.METRIC);

        Seat seat = new Seat(body.userId());
        seats.persist(seat);
        seats.flush(); // rende il nuovo posto conteggiabile subito, prima di leggere la giacenza
        publishUsage();
        LOG.infof("crm.seat.assign tenant_id=%s user_id=%s", caller.tenantId(), body.userId());
        return Response.status(Response.Status.CREATED).entity(SeatView.from(seat)).build();
    }

    /** Revoca il posto di un utente. OWNER/ADMIN. Libera subito la giacenza. */
    @DELETE
    @Path("/{userId}")
    @RolesAllowed({Roles.OWNER, Roles.ADMIN})
    @Transactional
    public Response revoke(@PathParam("userId") String userId) {
        Seat seat = seats.activeForUser(userId)
                .orElseThrow(() -> new NotFoundException("Nessun posto attivo per questo utente"));
        seat.markDeleted();
        seats.flush(); // la giacenza scende subito: la ripubblichiamo aggiornata
        publishUsage();
        LOG.infof("crm.seat.revoke tenant_id=%s user_id=%s", caller.tenantId(), userId);
        return Response.noContent().build();
    }

    private void publishUsage() {
        usage.publish(caller.tenantId().toString(), seats.count());
    }
}
