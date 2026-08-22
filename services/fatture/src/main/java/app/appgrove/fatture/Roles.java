package app.appgrove.fatture;

/**
 * Nomi di ruolo di <b>piattaforma</b> (gruppi JWT) usati da {@code @RolesAllowed}.
 *
 * <p><b>Non sono i ruoli che decidono cosa si può fare qui.</b> Dopo UC 0098/0099 il potere dentro una
 * applicazione lo dice il ruolo <i>sull'applicazione</i> ({@code AppRole}: viewer, editor, admin), letto
 * dalla fonte di verità e applicato dal varco condiviso. Questa lista dice soltanto «appartieni a un
 * account» — la stessa cosa che dicono già {@code @Authenticated} e il claim {@code tenant_id} — e
 * comprende quindi <b>tutti</b> i ruoli di piattaforma esistenti (UC 0098 li ha ridotti a {@code owner} e
 * {@code member}; {@code admin} è tollerato per i gettoni vecchi).
 *
 * <p>Comprenderli tutti è necessario, non cosmetico: Quarkus applica {@code @RolesAllowed} <b>prima</b>
 * dei filtri JAX-RS (sicurezza «eager», a livello di rotta), quindi una lista che escludesse
 * {@code member} risponderebbe {@code 403} senza corpo a ogni collaboratore — e il varco del ruolo non
 * arriverebbe mai a decidere, rendendo la classificazione di UC 0101 una dichiarazione senza effetto.
 *
 * <p>La <b>rimozione</b> di queste annotazioni — che a quel punto non aggiungono nulla — appartiene al
 * ritiro del modello vecchio: UC 0111 (gestione utenti dentro l'applicazione) e UC 0114.
 */
public final class Roles {

    public static final String OWNER = "owner";
    public static final String ADMIN = "admin";
    /** Collaboratore dell'account: il suo potere sull'applicazione lo dice {@code AppRole}, non questo. */
    public static final String MEMBER = "member";

    private Roles() {}
}
