package app.appgrove.core.platform;

import app.appgrove.core.catalog.AppStatusService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * DTO della console admin (UC 0021). Viste **cross-tenant** read-only (più il solo write {@code app.status}).
 * Alimentate da query native (bypassano deliberatamente il filtro {@code @TenantId}: superficie gated
 * {@code platform-admin}, eccezione esplicita all'invariante #2).
 */
public final class AdminDtos {

    private AdminDtos() {}

    /** KPI base dell'Overview. */
    public record OverviewView(long accounts, long users, long activeSubscriptions, long disabledApps) {}

    /**
     * App del catalogo (platform-level).
     *
     * <p>{@code kind} distingue una <b>applicazione</b> del marketplace da una <b>voce di piattaforma</b>
     * (UC 0103): la console è l'unica superficie in cui la voce dei posti <b>si vede</b>, perché chi
     * amministra deve poter constatare che esiste — ma va vista per quello che è, non confusa con una
     * applicazione da vendere. Delle cinque esclusioni della scelta strutturale, questa è l'unica che non
     * è una sparizione: è un'etichetta.
     */
    public record AppView(UUID id, String slug, String name, String userModel, String status, String kind) {}

    /** Riga della lista account (cross-tenant) con conteggi. */
    public record AdminAccountView(UUID id, String name, String status, long users, long activeSubscriptions) {}

    /** Utente nella vista admin (cross-tenant), con il tenant di appartenenza. */
    public record AdminUserView(
            UUID id, String email, String displayName, String role, String status, String tenantId, String tenantName) {}

    /** Cella della matrice entitlement (tenant×app, **derivata** da subscription + app.status). */
    public record EntitlementCell(
            String tenantId,
            String tenantName,
            UUID appId,
            String appSlug,
            String appName,
            String subscriptionStatus,
            boolean appActive,
            boolean entitled) {}

    /** Riga billing read-only (stato locale; drift Paddle reale → UC 0025). */
    public record BillingRow(
            String tenantId,
            String tenantName,
            String appSlug,
            String appName,
            String tierKey,
            String status,
            String currentPeriodStart,
            String currentPeriodEnd) {}

    /** Dettaglio account: anagrafica + utenti + entitlement derivato del tenant. */
    public record AccountDetailView(
            UUID id, String name, String status, List<AdminUserView> users, List<EntitlementCell> entitlements) {}

    /**
     * Body del toggle disable-app (unico write della console, UC 0076). La <b>motivazione</b> è
     * facoltativa — lo use case la vuole tale, e i chiamanti automatici esistenti (la suite
     * end-to-end di piattaforma attiva l'app in preparazione) non la passano.
     */
    public record UpdateAppStatus(
            @NotBlank String status, @Size(max = AppStatusService.REASON_MAX_LENGTH) String reason) {}

    /**
     * Riga del registro delle disabilitazioni/riabilitazioni (UC 0076): chi, quando, quale app, da
     * quale stato a quale, con la motivazione se è stata data. Platform-level: nessun tenant.
     */
    public record AppStatusAuditView(
            UUID id,
            UUID appId,
            String appSlug,
            String appName,
            String fromStatus,
            String toStatus,
            String actor,
            String reason,
            Instant executedAt) {}
}
