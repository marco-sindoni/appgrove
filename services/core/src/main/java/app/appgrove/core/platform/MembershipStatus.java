package app.appgrove.core.platform;

/**
 * Stato dell'appartenenza a un account (UC 0116): {@code suspended} = la persona non si presenta più
 * come persona di <b>quell'</b>account (leva dell'owner), mentre resta attiva altrove. Distinto da
 * {@link IdentityStatus}, che è la leva del titolare sulla persona. Persistito come stringa.
 */
public enum MembershipStatus {
    active,
    suspended
}
