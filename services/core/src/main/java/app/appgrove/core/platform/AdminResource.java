package app.appgrove.core.platform;

import app.appgrove.core.platform.AdminDtos.AccountDetailView;
import app.appgrove.core.platform.AdminDtos.AdminAccountView;
import app.appgrove.core.platform.AdminDtos.AdminUserView;
import app.appgrove.core.platform.AdminDtos.AppStatusAuditView;
import app.appgrove.core.platform.AdminDtos.AppView;
import app.appgrove.core.billing.EntitlementAccess;
import app.appgrove.core.billing.ReconciliationDtos.ReconciliationView;
import app.appgrove.core.billing.ReconciliationService;
import app.appgrove.core.billing.SubscriptionStatus;
import app.appgrove.core.catalog.AppStatus;
import app.appgrove.core.catalog.AppStatusService;
import app.appgrove.core.platform.AdminDtos.BillingRow;
import app.appgrove.core.platform.AdminDtos.EntitlementCell;
import app.appgrove.core.platform.AdminDtos.OverviewView;
import app.appgrove.core.platform.AdminDtos.UpdateAppStatus;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.validation.Valid;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import java.util.UUID;

/**
 * Console admin (UC 0021): superficie **cross-tenant** read-only riservata a {@code platform-admin}.
 *
 * <p>A differenza degli altri resource (tenant-scoped via {@code @TenantId}), qui le letture spaziano su
 * <b>tutti</b> i tenant: usiamo <b>query native</b> (che non passano dal filtro tenant di Hibernate) ed è una
 * <b>eccezione esplicita e documentata</b> all'invariante #2, ammessa solo perché gated {@code platform-admin}.
 * L'unico write è il toggle {@code app.status} (disable-app, gate 2 — l'<i>enforcement</i> runtime è di UC 0014/0027).
 */
@Path("/api/platform/v1/admin")
@RolesAllowed(Roles.PLATFORM_ADMIN)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AdminResource {

    @Inject
    EntityManager em;

    @Inject
    CallerContext caller;

    @Inject
    AppStatusService appStatus;

    @Inject
    ReconciliationService reconciliation;

    @GET
    @Path("/overview")
    public OverviewView overview() {
        Object[] r = (Object[]) em.createNativeQuery(
                """
                select
                  (select count(*) from platform.accounts where deleted_at is null),
                  (select count(*) from platform.users where deleted_at is null),
                  (select count(*) from platform.subscription where deleted_at is null and status in ('active','trialing')),
                  (select count(*) from platform.app where deleted_at is null and status = 'inactive')
                """)
                .getSingleResult();
        return new OverviewView(num(r[0]), num(r[1]), num(r[2]), num(r[3]));
    }

    @GET
    @Path("/apps")
    public List<AppView> apps() {
        return rows("""
                select id, slug, name, user_model, status
                from platform.app where deleted_at is null order by slug
                """)
                .stream()
                .map(r -> new AppView((UUID) r[0], str(r[1]), str(r[2]), str(r[3]), str(r[4])))
                .toList();
    }

    @GET
    @Path("/accounts")
    public List<AdminAccountView> accounts() {
        return rows("""
                select a.id, a.name, a.status,
                  (select count(*) from platform.users u
                     where u.tenant_id = a.id::text and u.deleted_at is null),
                  (select count(*) from platform.subscription s
                     where s.tenant_id = a.id::text and s.deleted_at is null and s.status in ('active','trialing'))
                from platform.accounts a
                where a.deleted_at is null
                order by a.name
                """)
                .stream()
                .map(r -> new AdminAccountView((UUID) r[0], str(r[1]), str(r[2]), num(r[3]), num(r[4])))
                .toList();
    }

    @GET
    @Path("/accounts/{id}")
    public AccountDetailView account(@PathParam("id") UUID id) {
        List<Object[]> head = rows(
                "select id, name, status from platform.accounts where id = :id and deleted_at is null",
                "id", id);
        if (head.isEmpty()) {
            throw new NotFoundException("Account non trovato");
        }
        Object[] a = head.get(0);
        return new AccountDetailView(
                (UUID) a[0], str(a[1]), str(a[2]), usersOf(id.toString()), entitlementsOf(id.toString()));
    }

    @GET
    @Path("/users")
    public List<AdminUserView> users() {
        return rows("""
                select u.id, u.email, u.display_name, u.role, u.status, u.tenant_id, a.name
                from platform.users u
                join platform.accounts a on a.id::text = u.tenant_id
                where u.deleted_at is null
                order by a.name, u.email
                """)
                .stream()
                .map(this::toUser)
                .toList();
    }

    @GET
    @Path("/entitlements")
    public List<EntitlementCell> entitlements() {
        return rows(ENTITLEMENTS_SQL + " order by acc.name, app.slug")
                .stream()
                .map(this::toEntitlement)
                .toList();
    }

    @GET
    @Path("/billing")
    public List<BillingRow> billing() {
        return rows("""
                select s.tenant_id, acc.name, app.slug, app.name, t.key, s.status,
                       s.current_period_start::text, s.current_period_end::text
                from platform.subscription s
                join platform.accounts acc on acc.id::text = s.tenant_id
                join platform.app app on app.id = s.app_id
                left join platform.app_tier t on t.id = s.app_tier_id
                where s.deleted_at is null
                order by acc.name, app.slug
                """)
                .stream()
                .map(r -> new BillingRow(
                        str(r[0]), str(r[1]), str(r[2]), str(r[3]), str(r[4]), str(r[5]), str(r[6]), str(r[7])))
                .toList();
    }

    /**
     * Riconciliazione fra ricavo lordo e denaro davvero accreditato (UC 0071): lordo → commissioni → netto →
     * accredito, per periodo, con la quadratura di ogni accredito. Sta qui e non nella pagina di
     * fatturazione del cliente perché il netto è un dato economico della piattaforma: al cliente interessa
     * quanto ha pagato, a noi quanto è entrato.
     */
    @GET
    @Path("/reconciliation")
    public ReconciliationView reconciliation() {
        return reconciliation.view();
    }

    /**
     * Disabilita/riabilita un'app (UC 0076): unico write della console. Idempotente — chiedere lo stato
     * corrente risponde 200 senza scrivere né sporcare il registro. La regola a valle (l'app non
     * {@code active} sparisce dagli entitlement) è già di UC 0027/0077 e non viene qui duplicata.
     */
    @PATCH
    @Path("/apps/{id}")
    public AppView setAppStatus(@PathParam("id") UUID id, @Valid UpdateAppStatus body) {
        AppStatus target;
        try {
            target = AppStatus.valueOf(body.status());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Stato app non valido: " + body.status());
        }
        AppStatusService.Result result = appStatus.setStatus(id, target, body.reason(), caller.subject());
        if (result == null) {
            throw new NotFoundException("App non trovata");
        }
        return new AppView(
                result.appId(), result.slug(), result.name(), result.userModel(), result.status().name());
    }

    /** Registro delle transizioni di stato delle app (UC 0076): chi, quando, quale app, perché. */
    @GET
    @Path("/apps/audit")
    public List<AppStatusAuditView> appStatusAudit() {
        return appStatus.auditTrail().stream()
                .map(e -> new AppStatusAuditView(
                        e.id(),
                        e.appId(),
                        e.appSlug(),
                        e.appName(),
                        e.fromStatus().name(),
                        e.toStatus().name(),
                        e.actor(),
                        e.reason(),
                        e.executedAt()))
                .toList();
    }

    // ── helper ───────────────────────────────────────────────────────────────

    // entitled NON è nel SQL: si deriva in Java con EntitlementAccess (UC 0077), la stessa regola che
    // serve il read-model del tenant e il polling post-checkout → niente predicato duplicato che nel
    // tempo diverge. Il SQL porta soltanto gli ingredienti: stato dell'account, stato dell'app,
    // eventuale stato della subscription, disponibilità del tier free.
    //
    // La matrice parte da (account × app) e NON dalle sole righe di subscription: altrimenti un'app
    // abilitata dal tier free di baseline — che il backoffice mostra — resterebbe invisibile alla
    // console. Il filtro tiene le righe che hanno una subscription (come prima) più quelle abilitabili
    // dalla baseline gratuita; le coppie senza né l'una né l'altra non sono entitlement e non entrano.
    private static final String ENTITLEMENTS_SQL =
            """
            select acc.id::text, acc.name, acc.status, app.id, app.slug, app.name, app.status,
                   s.status, app.free_tier
            from platform.accounts acc
            cross join (
                select app.id, app.slug, app.name, app.status,
                       exists (
                           select 1 from platform.app_tier t
                           where t.app_id = app.id and t.deleted_at is null
                             and not exists (
                                 select 1 from platform.app_price p
                                 where p.app_tier_id = t.id and p.deleted_at is null)
                       ) as free_tier
                from platform.app app
                where app.deleted_at is null
            ) app
            left join platform.subscription s
                   on s.tenant_id = acc.id::text and s.app_id = app.id and s.deleted_at is null
            where acc.deleted_at is null
              and (s.status is not null or (app.status = 'active' and app.free_tier))
            """;

    private List<AdminUserView> usersOf(String tenantId) {
        return rows("""
                select u.id, u.email, u.display_name, u.role, u.status, u.tenant_id, a.name
                from platform.users u
                join platform.accounts a on a.id::text = u.tenant_id
                where u.tenant_id = :tid and u.deleted_at is null
                order by u.email
                """, "tid", tenantId)
                .stream()
                .map(this::toUser)
                .toList();
    }

    private List<EntitlementCell> entitlementsOf(String tenantId) {
        return rows(ENTITLEMENTS_SQL + " and acc.id::text = :tid order by app.slug", "tid", tenantId)
                .stream()
                .map(this::toEntitlement)
                .toList();
    }

    private AdminUserView toUser(Object[] r) {
        return new AdminUserView((UUID) r[0], str(r[1]), str(r[2]), str(r[3]), str(r[4]), str(r[5]), str(r[6]));
    }

    private EntitlementCell toEntitlement(Object[] r) {
        AccountStatus accountStatus = AccountStatus.valueOf(str(r[2]));
        AppStatus appStatus = AppStatus.valueOf(str(r[6]));
        String subStatus = str(r[7]); // null sulle righe di baseline gratuita (nessuna subscription)
        SubscriptionStatus subscriptionStatus = subStatus != null ? SubscriptionStatus.valueOf(subStatus) : null;
        // entitled = regola unica condivisa (UC 0077), la stessa che vede l'utente nel backoffice.
        boolean entitled = EntitlementAccess.granted(accountStatus, appStatus, subscriptionStatus, bool(r[8]));
        return new EntitlementCell(
                str(r[0]),
                str(r[1]),
                (UUID) r[3],
                str(r[4]),
                str(r[5]),
                subStatus,
                appStatus == AppStatus.active,
                entitled);
    }

    @SuppressWarnings("unchecked")
    private List<Object[]> rows(String sql, Object... params) {
        var q = em.createNativeQuery(sql);
        for (int i = 0; i + 1 < params.length; i += 2) {
            q.setParameter((String) params[i], params[i + 1]);
        }
        return q.getResultList();
    }

    private static long num(Object o) {
        return ((Number) o).longValue();
    }

    private static String str(Object o) {
        return o == null ? null : o.toString();
    }

    private static boolean bool(Object o) {
        return Boolean.TRUE.equals(o);
    }
}
