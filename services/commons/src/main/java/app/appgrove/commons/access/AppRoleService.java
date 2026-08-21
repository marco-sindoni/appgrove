package app.appgrove.commons.access;

/**
 * Faccia di piattaforma per il <b>ruolo della persona su questa applicazione</b> (UC 0099), consumata dal
 * varco {@link RequiresAppRole}. Astrae <b>come</b> il ruolo si ottiene — copia locale del servizio, con
 * il core come fonte di verità — così che il codice di dominio dell'applicazione non lo sappia e non debba
 * saperlo. Gemella di {@code EntitlementService}, che risponde all'altra domanda: non «che potere ha questa
 * persona» ma «l'account ha diritto a questa applicazione».
 *
 * <p>Il ruolo <b>non è nel token</b>, ed è la decisione centrale della storia: un cambio di ruolo avrebbe
 * effetto solo al rinnovo del token, e un account con dieci applicazioni gonfierebbe ogni richiesta. Il
 * prezzo è questo strato; il ricavo è che una revoca si sente in pochi secondi.
 */
public interface AppRoleService {

    /**
     * Ruolo della persona del token su quella applicazione, dalla <b>copia locale</b> quando è usabile.
     * È il percorso normale: nessuna chiamata di rete quando la copia è fresca.
     */
    AppRoleOutcome roleOf(String appSlug);

    /**
     * Come {@link #roleOf}, ma <b>salta la copia locale</b> e rilegge dalla fonte di verità.
     *
     * <p><b>Solo per le operazioni irreversibili</b> — cancellazioni di massa, cambi di ruolo, revoche
     * (UC 0099 §5). Costa una chiamata di rete: usarlo «per sicurezza» su tutto riporta il core sul
     * percorso caldo di ogni richiesta di ogni applicazione, che è la situazione che la copia locale
     * esiste per evitare. La domanda da farsi è una sola: se questa operazione partisse con un ruolo
     * revocato tre secondi fa, si potrebbe tornare indietro?
     *
     * <p>Il risultato <b>aggiorna</b> la copia locale: una rilettura non è mai sprecata.
     */
    AppRoleOutcome roleFresh(String appSlug);
}
