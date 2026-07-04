package app.appgrove.core.gdpr;

import app.appgrove.core.gdpr.AdminGdprDtos.AdminMessageView;
import app.appgrove.core.gdpr.AdminGdprDtos.AdminTicketDetailView;
import app.appgrove.core.gdpr.AdminGdprDtos.AdminTicketView;
import app.appgrove.core.gdpr.AdminGdprDtos.ApplyRestriction;
import app.appgrove.core.gdpr.AdminGdprDtos.ExportDetailView;
import app.appgrove.core.gdpr.AdminGdprDtos.ExportItemView;
import app.appgrove.core.gdpr.AdminGdprDtos.PurgeAuditView;
import app.appgrove.core.gdpr.AdminGdprDtos.RequestView;
import app.appgrove.core.gdpr.AdminGdprDtos.RestrictionResult;
import app.appgrove.core.gdpr.AdminGdprDtos.RestrictionsView;
import app.appgrove.core.gdpr.AdminGdprDtos.UpdateTicket;
import app.appgrove.core.observability.AwsConsoleLinks;
import app.appgrove.core.platform.Account;
import app.appgrove.core.platform.CallerContext;
import app.appgrove.core.platform.Roles;
import app.appgrove.core.support.TicketAuthor;
import app.appgrove.core.support.TicketDtos.PostMessage;
import app.appgrove.core.support.TicketNotifier;
import app.appgrove.core.support.TicketStatus;
import app.appgrove.core.support.TicketStore;
import app.appgrove.core.support.TicketType;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.validation.Valid;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.ResponseStatus;

/**
 * Console "Diritti GDPR" (UC 0034, #13 L75): single pane <b>in aggregazione</b> — non un nuovo
 * store — su export, recessi per-app, eliminazioni account e ticket privacy, con puntatori
 * all'accessorio (Logs Insights, oggetto S3, registri prove). Come {@code AdminResource}: letture
 * cross-tenant via query native, eccezione documentata all'invariante #2 ammessa solo perché gated
 * {@code platform-admin}. Nessuna impersonation (#03 15): le uniche scritture sono le ops sicure
 * del ticketing (risposta/stato) e la limitazione art. 18 (reversibile, con prova in audit).
 */
@Path("/api/platform/v1/admin/gdpr")
@RolesAllowed(Roles.PLATFORM_ADMIN)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AdminGdprResource {

    private static final Logger LOG = Logger.getLogger(AdminGdprResource.class);

    @Inject
    EntityManager em;

    @Inject
    TicketStore tickets;

    @Inject
    TicketNotifier notifier;

    @Inject
    GdprRestrictionService restrictions;

    @Inject
    AwsConsoleLinks links;

    @Inject
    CallerContext caller;

    @ConfigProperty(name = "appgrove.gdpr.export-bucket")
    String exportBucket;

    // ── Aggregazione (#13 L75) ────────────────────────────────────────────────

    /** Tabella aggregata delle richieste diritti; {@code type} opzionale per filtrare. */
    @GET
    @Path("/requests")
    public List<RequestView> requests(@QueryParam("type") String type) {
        List<RequestView> merged = new ArrayList<>();
        merged.addAll(exportRequests());
        merged.addAll(withdrawalRequests());
        merged.addAll(accountDeletionRequests());
        merged.addAll(privacyTicketRequests());
        return merged.stream()
                .filter(r -> type == null || type.isBlank() || r.type().equals(type))
                .sorted(Comparator.comparing(RequestView::requestedAt).reversed())
                .toList();
    }

    /** Dettaglio di un export: item per-servizio + puntatore all'oggetto S3 (chiave + console). */
    @GET
    @Path("/exports/{id}")
    public ExportDetailView export(@PathParam("id") UUID id) {
        RequestView request = exportRequests().stream()
                .filter(r -> r.refId().equals(id))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Export job non trovato"));
        List<ExportItemView> items = rows("""
                select app_id, status, error from platform.gdpr_export_job_item
                where job_id = :job and deleted_at is null order by app_id
                """, "job", id)
                .stream()
                .map(r -> new ExportItemView(str(r[0]), str(r[1]), str(r[2])))
                .toList();
        String zipKey = (String) em.createNativeQuery(
                        "select zip_key from platform.gdpr_export_job where id = :id")
                .setParameter("id", id)
                .getSingleResult();
        return new ExportDetailView(
                request, items, zipKey, links.s3ObjectUrl(exportBucket, zipKey).orElse(null));
    }

    /** Registro prove di erasure ({@code gdpr_purge_audit} di piattaforma, #13 L70). */
    @GET
    @Path("/purge-audit")
    public List<PurgeAuditView> purgeAudit() {
        return rows("""
                select id, tenant_id, app_id, reason, total, executed_at
                from platform.gdpr_purge_audit order by executed_at desc
                """)
                .stream()
                .map(r -> new PurgeAuditView(
                        (UUID) r[0], str(r[1]), str(r[2]), str(r[3]),
                        ((Number) r[4]).intValue(), instant(r[5])))
                .toList();
    }

    // ── Ticket (vista admin, #13 D21) ─────────────────────────────────────────

    @GET
    @Path("/tickets")
    public List<AdminTicketView> ticketList(
            @QueryParam("type") TicketType type, @QueryParam("status") TicketStatus status) {
        return tickets.list(type, status).stream().map(this::ticketView).toList();
    }

    @GET
    @Path("/tickets/{id}")
    public AdminTicketDetailView ticket(@PathParam("id") UUID id) {
        TicketStore.TicketRow row = loadTicket(id);
        List<AdminMessageView> thread = tickets.thread(id).stream()
                .map(m -> new AdminMessageView(m.id(), m.author(), m.body(), m.createdAt()))
                .toList();
        return new AdminTicketDetailView(ticketView(row), thread);
    }

    /** Risposta dell'admin nel thread: un ticket {@code open} passa in lavorazione, l'utente è avvisato. */
    @POST
    @Path("/tickets/{id}/messages")
    @ResponseStatus(201)
    public AdminMessageView reply(@PathParam("id") UUID id, @Valid PostMessage body) {
        TicketStore.TicketRow row = loadTicket(id);
        if (row.status() == TicketStatus.closed) {
            throw new ClientErrorException("Ticket chiuso", Response.Status.CONFLICT);
        }
        TicketStore.MessageRow message =
                tickets.addMessage(row, TicketAuthor.admin, caller.subject(), body.body());
        if (row.status() == TicketStatus.open) {
            tickets.update(id, TicketStatus.in_progress, row.priority(), caller.subject(), Instant.now());
        }
        LOG.infof("ticket.admin-reply ticket_id=%s tenant_id=%s actor=%s", id, row.tenantId(), caller.subject());
        notifier.notifyRequester(TicketNotifier.TicketRef.of(loadTicket(id)), body.body());
        return new AdminMessageView(message.id(), message.author(), message.body(), message.createdAt());
    }

    /** Cambio stato/priorità (ops sicure: mai editing del contenuto). L'utente è avvisato. */
    @PATCH
    @Path("/tickets/{id}")
    public AdminTicketView update(@PathParam("id") UUID id, @Valid UpdateTicket body) {
        loadTicket(id);
        tickets.update(id, body.status(), body.priority(), caller.subject(), Instant.now());
        TicketStore.TicketRow updated = loadTicket(id);
        LOG.infof("ticket.admin-update ticket_id=%s status=%s priority=%s tenant_id=%s actor=%s",
                id, body.status(), body.priority(), updated.tenantId(), caller.subject());
        notifier.notifyRequester(TicketNotifier.TicketRef.of(updated),
                "Lo stato del ticket è stato aggiornato dal supporto.");
        return ticketView(updated);
    }

    // ── Limitazione del trattamento (art. 18, #13 D19) ────────────────────────

    @GET
    @Path("/restrictions")
    public RestrictionsView restrictionList() {
        return new RestrictionsView(restrictions.active(), restrictions.auditTrail());
    }

    @POST
    @Path("/restrictions")
    @ResponseStatus(201)
    public RestrictionResult applyRestriction(@Valid ApplyRestriction body) {
        GdprRestrictionService.Outcome outcome = restrictions.apply(
                body.targetKind(), body.targetId(), body.ticketId(), body.note(), caller.subject());
        return result(outcome, "già sospeso (limitato o sospensione amministrativa)");
    }

    @DELETE
    @Path("/restrictions/{targetKind}/{targetId}")
    public RestrictionResult removeRestriction(
            @PathParam("targetKind") String targetKind, @PathParam("targetId") UUID targetId) {
        GdprRestrictionService.TargetKind kind = parseKind(targetKind);
        GdprRestrictionService.Outcome outcome =
                restrictions.remove(kind, targetId, null, caller.subject());
        return result(outcome, "il bersaglio non è sotto limitazione art. 18");
    }

    // ── helper ───────────────────────────────────────────────────────────────

    private List<RequestView> exportRequests() {
        return rows("""
                select j.id, j.tenant_id, a.name, j.app_id, j.status, j.error,
                       j.created_at, j.completed_at, j.zip_key, j.created_by
                from platform.gdpr_export_job j
                left join platform.accounts a on a.id::text = j.tenant_id
                where j.deleted_at is null
                """)
                .stream()
                .map(r -> {
                    Instant completed = instant(r[7]);
                    boolean hasZip = r[8] != null;
                    UUID id = (UUID) r[0];
                    return new RequestView(
                            "export", id, str(r[1]), str(r[2]), str(r[3]), str(r[9]), str(r[4]),
                            instant(r[6]), completed,
                            hasZip && completed != null ? completed.plus(GdprResource.LINK_TTL) : null,
                            str(r[5]),
                            logsUrl("job_id", id.toString()));
                })
                .toList();
    }

    /**
     * Recessi per-app (UC 0033): derivati dalle attivazioni soft-deleted di account vivi — l'unico
     * flusso che le produce è il recesso ({@code GdprWithdrawalResource}); gli account offboardati
     * compaiono già come eliminazioni. Dedup per tenant+app (più subscription della stessa coppia).
     */
    private List<RequestView> withdrawalRequests() {
        Map<String, RequestView> byTenantApp = new LinkedHashMap<>();
        rows("""
                select s.id, s.tenant_id, acc.name, app.slug, s.deleted_at
                from platform.subscription s
                join platform.app app on app.id = s.app_id
                join platform.accounts acc on acc.id::text = s.tenant_id
                where s.deleted_at is not null and acc.deleted_at is null
                order by s.deleted_at desc
                """)
                .forEach(r -> byTenantApp.putIfAbsent(
                        str(r[1]) + "/" + str(r[3]),
                        new RequestView(
                                "withdrawal", (UUID) r[0], str(r[1]), str(r[2]), str(r[3]), null,
                                "PURGE_REQUESTED", instant(r[4]), null, null, null,
                                logsUrl("tenant_id", str(r[1])))));
        return new ArrayList<>(byTenantApp.values());
    }

    private List<RequestView> accountDeletionRequests() {
        return rows("""
                select a.id, a.name, a.status, a.deletion_requested_at, a.deleted_at
                from platform.accounts a
                where a.deletion_requested_at is not null
                  and (a.status = 'pending_deletion' or a.deleted_at is not null)
                """)
                .stream()
                .map(r -> {
                    UUID id = (UUID) r[0];
                    boolean pending = r[4] == null;
                    Instant requested = instant(r[3]);
                    return new RequestView(
                            "account_deletion", id, id.toString(), str(r[1]), null, null,
                            pending ? "GRACE_PENDING" : "OFFBOARDED",
                            requested,
                            instant(r[4]),
                            pending ? requested.plus(Account.DELETION_GRACE) : null,
                            null,
                            logsUrl("tenant_id", id.toString()));
                })
                .toList();
    }

    private List<RequestView> privacyTicketRequests() {
        return tickets.list(TicketType.privacy, null).stream()
                .map(t -> new RequestView(
                        "privacy_ticket", t.id(), t.tenantId(), t.accountName(), null,
                        t.createdBy(), t.status().name(), t.createdAt(), t.closedAt(), t.dueAt(),
                        null, logsUrl("ticket_id", t.id().toString())))
                .toList();
    }

    private AdminTicketView ticketView(TicketStore.TicketRow row) {
        return new AdminTicketView(
                row.id(), row.tenantId(), row.accountName(), row.type(), row.subject(),
                row.priority(), row.status(), row.dueAt(), row.exportJobId(), row.closedAt(),
                row.createdAt(), logsUrl("ticket_id", row.id().toString()));
    }

    private TicketStore.TicketRow loadTicket(UUID id) {
        return tickets.find(id).orElseThrow(() -> new NotFoundException("Ticket non trovato"));
    }

    private RestrictionResult result(GdprRestrictionService.Outcome outcome, String conflictHint) {
        return switch (outcome) {
            case NOT_FOUND -> throw new NotFoundException("Bersaglio non trovato");
            case CONFLICT -> throw new ClientErrorException(
                    "Operazione non applicabile: " + conflictHint, Response.Status.CONFLICT);
            case APPLIED, REMOVED -> new RestrictionResult(outcome.name());
        };
    }

    private static GdprRestrictionService.TargetKind parseKind(String raw) {
        try {
            return GdprRestrictionService.TargetKind.valueOf(raw);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("targetKind non valido: " + raw);
        }
    }

    private String logsUrl(String key, String value) {
        return links.logsInsightsUrl(Map.of(key, value)).orElse(null);
    }

    @SuppressWarnings("unchecked")
    private List<Object[]> rows(String sql, Object... params) {
        var q = em.createNativeQuery(sql);
        for (int i = 0; i + 1 < params.length; i += 2) {
            q.setParameter((String) params[i], params[i + 1]);
        }
        return q.getResultList();
    }

    private static String str(Object o) {
        return o == null ? null : o.toString();
    }

    private static Instant instant(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof Timestamp ts) {
            return ts.toInstant();
        }
        return (Instant) o;
    }
}
