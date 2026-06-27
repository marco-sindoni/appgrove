package app.appgrove.fatture;

/** Nomi di ruolo (gruppi JWT) usati da {@code @RolesAllowed}. In B2C single-user l'utente è owner. */
public final class Roles {

    public static final String OWNER = "owner";
    public static final String ADMIN = "admin";

    private Roles() {}
}
