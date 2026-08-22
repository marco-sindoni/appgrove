package app.appgrove.@@APP_ID@@;

/**
 * Nomi di ruolo di <b>piattaforma</b> (gruppi JWT) usati da {@code @RolesAllowed}.
 *
 * <p><b>Non sono i ruoli che decidono cosa si può fare qui.</b> Dopo UC 0098/0099 il potere dentro una
 * applicazione lo dice il ruolo <i>sull'applicazione</i> ({@code AppRole}: viewer, editor, admin), letto
 * dalla fonte di verità e applicato dal varco condiviso, e la classificazione delle operazioni sta in
 * {@code @@APP_CLASS@@OperationsContract} (UC 0101). Questa lista dice soltanto «appartieni a un account»
 * — la stessa cosa che dicono già {@code @Authenticated} e il claim {@code tenant_id}.
 *
 * <p>Comprenderli tutti è necessario, non cosmetico: Quarkus applica {@code @RolesAllowed} <b>prima</b>
 * dei filtri JAX-RS (sicurezza «eager», a livello di rotta), quindi una lista che escludesse
 * {@code member} risponderebbe {@code 403} senza corpo a ogni collaboratore — e il varco del ruolo non
 * arriverebbe mai a decidere.
 *
 * <p>La <b>rimozione</b> di queste annotazioni — che a quel punto non aggiungono nulla — appartiene al
 * ritiro del modello vecchio: UC 0111 e UC 0114.
 */
public final class Roles {

    public static final String OWNER = "owner";
    public static final String ADMIN = "admin";
@@ROLES_EXTRA_CONSTANTS@@
    private Roles() {}
}
