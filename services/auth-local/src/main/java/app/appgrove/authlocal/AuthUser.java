package app.appgrove.authlocal;

/** Identità letta dal DB (schema platform) per costruire i claim. {@code sub} = cognito_sub, {@code tenantId} = account id. */
public record AuthUser(
        String sub, String tenantId, String role, String status, String email, String displayName) {

    public boolean isActive() {
        return "active".equals(status);
    }
}
