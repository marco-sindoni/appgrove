package app.appgrove.core.platform;

/**
 * Nomi di ruolo usati da {@code @RolesAllowed}, letti dal claim JWT {@code roles} (UC 0016,
 * #02 10: {@code smallrye.jwt.path.groups=roles}). Livello account (owner/member) + livello
 * piattaforma (platform-admin).
 */
public final class Roles {

    public static final String OWNER = "owner";

    /**
     * <b>Non è più un ruolo di appartenenza</b> (UC 0098: {@link MembershipRole} ammette due soli
     * valori) e nessun token nuovo lo porterà mai più. Resta qui, e resta nelle annotazioni che lo
     * nominano, come <b>tolleranza dei token già emessi</b>: un token coniato prima del rilascio
     * contiene ancora {@code admin} e deve continuare a funzionare fino alla sua scadenza naturale
     * (UC 0113 §6, piano di rilascio).
     *
     * <p><b>Non toglierlo qui.</b> La riduzione del claim dei ruoli è di UC 0099 e il ritiro della
     * tolleranza — con la sua data — è di UC 0113: rimuoverlo prima significherebbe stringere
     * l'autorizzazione di venti operazioni nella storia sbagliata, e chiudere fuori chi ha in mano un
     * token ancora valido.
     */
    public static final String ADMIN = "admin";
    public static final String MEMBER = "member";
    public static final String PLATFORM_ADMIN = "platform-admin";

    private Roles() {}
}
