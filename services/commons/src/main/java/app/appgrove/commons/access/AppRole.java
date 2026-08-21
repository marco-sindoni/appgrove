package app.appgrove.commons.access;

import java.util.Optional;

/**
 * Ruolo di una persona <b>su una applicazione</b> (UC 0098). È il livello in cui il potere ha senso in
 * un modello centralizzato: il ruolo di piattaforma dice soltanto chi possiede l'account
 * ({@code MembershipRole} in core), mentre <i>cosa</i> una persona può fare lo dice questo.
 *
 * <p><b>Perché vive in {@code commons} e non in core</b> (UC 0099): il varco dichiarativo dei servizi
 * ({@link RequiresAppRole}) deve poter <b>nominare</b> il ruolo minimo richiesto, e {@code commons} è
 * l'unico posto che sia core sia i servizi delle applicazioni possono vedere ({@code commons} non
 * dipende da core, e non può). Duplicare l'enumerazione avrebbe duplicato l'<b>ordinamento</b>, che è
 * la cosa che meno di ogni altra va scritta due volte.
 *
 * <p>I tre valori sono <b>ordinati</b>, dal meno al più capace, e l'ordinamento è la dichiarazione
 * dell'ordine: {@code viewer} &lt; {@code editor} &lt; {@code admin}. Il confronto vive qui in
 * {@link #atLeast(AppRole)} e <b>non va duplicato altrove</b> — lo usano il varco riusabile dei servizi
 * e le schermate (UC 0101, UC 0111). Una seconda copia dell'ordinamento è una seconda occasione di
 * sbagliarlo.
 *
 * <p>Il <b>significato di comportamento</b> dei tre ruoli — cosa esattamente può fare un {@code editor}
 * in una applicazione qualunque — è il contratto di UC 0101: qui c'è soltanto l'ordine.
 *
 * <p><b>L'owner dell'account non compare fra questi valori</b>, e non è una dimenticanza: il suo accesso
 * è implicito su tutte le applicazioni dell'account e vale come {@link #admin} (UC 0098 §5). Sta sopra
 * tutti perché la fonte di verità gli attribuisce il valore massimo, non perché esista un quarto valore.
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

    /**
     * Il ruolo scritto nel dato, o vuoto se il valore è assente o non riconosciuto. Un valore ignoto
     * <b>non</b> è un errore da propagare: è un'informazione che non sappiamo interpretare, e in
     * assenza di informazione non si concede (fail-closed). Serve a chi legge una copia locale o una
     * risposta di rete, dove il valore arriva come stringa.
     */
    public static Optional<AppRole> parse(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(AppRole.valueOf(value));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
