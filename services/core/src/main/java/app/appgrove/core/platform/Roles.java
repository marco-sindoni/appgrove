package app.appgrove.core.platform;

/** Nomi di ruolo (gruppi JWT) usati da {@code @RolesAllowed}. Tenant-level + platform-level. */
public final class Roles {

    public static final String OWNER = "owner";
    public static final String ADMIN = "admin";
    public static final String MEMBER = "member";
    public static final String PLATFORM_ADMIN = "platform-admin";

    private Roles() {}
}
