package app.appgrove.crm;

/** Nomi di ruolo (gruppi JWT) usati da {@code @RolesAllowed}. Modello utente: multi-utente (B2B): piu utenti per account, con ruoli. */
public final class Roles {

    public static final String OWNER = "owner";
    public static final String ADMIN = "admin";
    /** Membro semplice dell'account (solo modello multi-utente). */
    public static final String MEMBER = "member";

    private Roles() {}
}
