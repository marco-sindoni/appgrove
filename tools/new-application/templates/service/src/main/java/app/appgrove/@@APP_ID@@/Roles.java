package app.appgrove.@@APP_ID@@;

/** Nomi di ruolo (gruppi JWT) usati da {@code @RolesAllowed}. Modello utente: @@USER_MODEL_NOTE@@. */
public final class Roles {

    public static final String OWNER = "owner";
    public static final String ADMIN = "admin";
@@ROLES_EXTRA_CONSTANTS@@
    private Roles() {}
}
