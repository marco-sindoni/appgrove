package app.appgrove.core.platform;

/**
 * Stato dell'identità della persona sulla piattaforma (UC 0116): {@code suspended} = la persona non
 * accede, indipendentemente dai suoi account (leva del titolare, art. 18). Persistito come stringa.
 */
public enum IdentityStatus {
    active,
    suspended
}
