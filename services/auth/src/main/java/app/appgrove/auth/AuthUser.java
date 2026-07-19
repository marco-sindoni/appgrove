package app.appgrove.auth;

/**
 * Identità letta dal DB (schema platform) per costruire i claim. {@code sub} = cognito_sub,
 * {@code tenantId} = account id, {@code locale} = lingua delle email transazionali (UC 0018).
 */
public record AuthUser(
        String sub, String tenantId, String role, String status, String email, String displayName, String locale) {

    public boolean isActive() {
        return "active".equals(status);
    }
}
