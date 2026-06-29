package app.appgrove.core.platform;

import app.appgrove.core.platform.AdminDtos.AccountDetailView;
import app.appgrove.core.platform.AdminDtos.AdminAccountView;
import app.appgrove.core.platform.AdminDtos.AdminUserView;
import app.appgrove.core.platform.AdminDtos.AppView;
import app.appgrove.core.billing.SubscriptionStatus;
import app.appgrove.core.platform.AdminDtos.BillingRow;
import app.appgrove.core.platform.AdminDtos.EntitlementCell;
import app.appgrove.core.platform.AdminDtos.OverviewView;
import app.appgrove.core.platform.AdminDtos.UpdateAppStatus;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
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
import java.util.Set;
import java.util.UUID;
import org.jboss.logging.Logger;

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

    private static final Logger LOG = Logger.getLogger(AdminResource.class);
    private static final Set<String> APP_STATUSES = Set.of("active", "inactive");

    @Inject
    EntityManager em;

    @Inject
    CallerContext caller;

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
        return rows(ENTITLEMENTS_SQL + " order by a.name, app.slug")
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

    @PATCH
    @Path("/apps/{id}")
    @Transactional
    public AppView setAppStatus(@PathParam("id") UUID id, @Valid UpdateAppStatus body) {
        if (!APP_STATUSES.contains(body.status())) {
            throw new BadRequestException("Stato app non valido: " + body.status());
        }
        int updated = em.createNativeQuery(
                "update platform.app set status = :status, updated_at = now(), updated_by = :actor "
                        + "where id = :id and deleted_at is null")
                .setParameter("status", body.status())
                .setParameter("actor", caller.subject())
                .setParameter("id", id)
                .executeUpdate();
        if (updated == 0) {
            throw new NotFoundException("App non trovata");
        }
        // logging strutturato dell'azione admin (invariante #4): app_id + attore + esito
        LOG.infof("admin.disable-app app_id=%s status=%s actor=%s", id, body.status(), caller.subject());
        List<Object[]> r = rows(
                "select id, slug, name, user_model, status from platform.app where id = :id", "id", id);
        Object[] a = r.get(0);
        return new AppView((UUID) a[0], str(a[1]), str(a[2]), str(a[3]), str(a[4]));
    }

    // ── helper ───────────────────────────────────────────────────────────────

    // entitled NON è nel SQL: si deriva in Java dalla mappa canonica status→accesso
    // (SubscriptionStatus.grantsAccess(), UC 0026) ∧ app abilitata → fonte di verità unica, niente
    // predicato duplicato. app_active resta in SQL (è uno stato dell'app, non della subscription).
    private static final String ENTITLEMENTS_SQL = """
            select s.tenant_id, a.name, app.id, app.slug, app.name, s.status,
                   (app.status = 'active') as app_active
            from platform.subscription s
            join platform.accounts a on a.id::text = s.tenant_id
            join platform.app app on app.id = s.app_id
            where s.deleted_at is null
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
        return rows(ENTITLEMENTS_SQL + " and s.tenant_id = :tid order by app.slug", "tid", tenantId)
                .stream()
                .map(this::toEntitlement)
                .toList();
    }

    private AdminUserView toUser(Object[] r) {
        return new AdminUserView((UUID) r[0], str(r[1]), str(r[2]), str(r[3]), str(r[4]), str(r[5]), str(r[6]));
    }

    private EntitlementCell toEntitlement(Object[] r) {
        String status = str(r[5]);
        boolean appActive = bool(r[6]);
        // entitled = mappa canonica status→accesso (UC 0026) ∧ app abilitata (gate 2).
        boolean entitled = SubscriptionStatus.valueOf(status).grantsAccess() && appActive;
        return new EntitlementCell(
                str(r[0]), str(r[1]), (UUID) r[2], str(r[3]), str(r[4]), status, appActive, entitled);
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
