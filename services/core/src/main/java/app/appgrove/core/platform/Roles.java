package app.appgrove.core.platform;

/**
 * Nomi di ruolo usati da {@code @RolesAllowed}, letti dal claim JWT {@code roles} (UC 0016,
 * #02 10: {@code smallrye.jwt.path.groups=roles}). Tenant-level (owner/admin/member) +
 * platform-level (platform-admin).
 */
public final class Roles {

    public static final String OWNER = "owner";
    public static final String ADMIN = "admin";
    public static final String MEMBER = "member";
    public static final String PLATFORM_ADMIN = "platform-admin";

    private Roles() {}
}
