package app.appgrove.core.platform;

import jakarta.validation.constraints.NotBlank;
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

    /** App del catalogo (platform-level). */
    public record AppView(UUID id, String slug, String name, String userModel, String status) {}

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

    /** Body del toggle disable-app (unico write della console). */
    public record UpdateAppStatus(@NotBlank String status) {}
}
