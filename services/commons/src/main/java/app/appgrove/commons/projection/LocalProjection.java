package app.appgrove.commons.projection;

/**
 * Una <b>copia locale</b> che un servizio di applicazione tiene nel proprio schema per non chiedere al
 * core a ogni richiesta: i diritti d'accesso dell'account (UC 0046) e il ruolo della persona su questa
 * applicazione (UC 0099). Due copie, un solo meccanismo.
 *
 * <p><b>Perché esiste questa interfaccia.</b> Le copie locali hanno due doveri che non appartengono al
 * dominio di nessuna applicazione e che, se dipendessero dalla diligenza di chi ne aggiunge una, prima o
 * poi verrebbero dimenticati — in silenzio:
 *
 * <ol>
 *   <li><b>farsi marcare da rinfrescare</b> quando arriva un evento che dice «qualcosa è cambiato per
 *       l'account T»: c'è <b>una sola coda per servizio</b> e il messaggio è sottile di proposito, quindi
 *       chi lo consuma non può sapere quali copie esistano — le interpella tutte;</li>
 *   <li><b>farsi cancellare</b> quando quell'account esercita il diritto di cancellazione: una copia
 *       superstite conserverebbe la traccia di chi ha chiesto di sparire, e il conteggio della
 *       cancellazione è la <b>prova</b> dell'erasure (#13 L70) — una prova incompleta non è una prova.</li>
 * </ol>
 *
 * <p>Chi aggiunge una terza copia locale implementa questa interfaccia e viene incluso da solo: non deve
 * ricordarsi di modificare il consumatore dell'invalidazione e quello della purga. È il modo di non avere
 * due meccanismi che divergono.
 *
 * <p>Le attuazioni girano <b>fuori</b> da una richiesta autenticata (i consumatori non hanno un token):
 * l'account è sempre <b>esplicito</b> nelle interrogazioni e arriva dal contenuto dell'evento pubblicato
 * dal core, mai da un input di un client.
 */
public interface LocalProjection {

    /**
     * Nome della copia nella traccia di controllo della cancellazione (es. {@code
     * entitlement_projection}). È un identificativo, non un'etichetta da mostrare.
     */
    String name();

    /**
     * {@code true} se questa copia è configurata in questo servizio. <b>Configurazione assente = copia
     * inerte</b>: i servizi che non sono applicazioni di marketplace (core, auth) non hanno la tabella e
     * non devono fallire all'avvio né essere interpellati.
     */
    boolean enabled();

    /**
     * Marca come <b>da rinfrescare</b> tutte le righe dell'account, <b>senza cancellarle</b>. Idempotente:
     * rimarcare una riga già marcata non cambia nulla, quindi una doppia consegna del messaggio è innocua.
     *
     * @return righe marcate (0 = nessuna copia per quell'account: la prima richiesta la creerà)
     */
    int markStale(String tenantId);

    /**
     * Cancella <b>fisicamente</b> ogni riga dell'account: fa parte dell'erasure, non è un dettaglio di
     * cache.
     *
     * @return righe cancellate (entra nell'audit della purga come prova)
     */
    int purgeTenant(String tenantId);
}
