package app.appgrove.core.platform;

/**
 * Ruolo di una persona <b>su una applicazione</b> (UC 0098). È il livello in cui il potere ha senso in
 * un modello centralizzato: il ruolo di piattaforma dice soltanto chi possiede l'account
 * ({@link MembershipRole}), mentre <i>cosa</i> una persona può fare lo dice questo.
 *
 * <p>I tre valori sono <b>ordinati</b>, dal meno al più capace, e l'ordinamento è la dichiarazione
 * dell'ordine: {@code viewer} &lt; {@code editor} &lt; {@code admin}. Il confronto vive qui in
 * {@link #atLeast(AppRole)} e <b>non va duplicato altrove</b> — lo useranno il varco riusabile dei
 * servizi (UC 0099) e le schermate (UC 0101, UC 0111). Una seconda copia dell'ordinamento è una
 * seconda occasione di sbagliarlo.
 *
 * <p>Il <b>significato di comportamento</b> dei tre ruoli — cosa esattamente può fare un {@code editor}
 * in una applicazione qualunque — è il contratto di UC 0101: qui c'è soltanto l'ordine.
 */
public enum AppRole {

    /** Legge e non scrive. */
    viewer,

    /** Legge e modifica i dati dell'applicazione. */
    editor,

    /** Come {@code editor}, e in più governa gli accessi a <b>quella</b> applicazione (UC 0098 §2). */
    admin;

    /**
     * Questo ruolo è almeno tanto capace di quello richiesto? È il confronto che ogni operazione fa
     * prima di procedere: {@code caller.atLeast(EDITOR)}.
     */
    public boolean atLeast(AppRole required) {
        return ordinal() >= required.ordinal();
    }
}
