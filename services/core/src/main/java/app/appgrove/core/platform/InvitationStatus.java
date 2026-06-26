package app.appgrove.core.platform;

/** Ciclo di vita di un invito. Persistito come stringa. */
public enum InvitationStatus {
    pending,
    accepted,
    revoked,
    expired
}
