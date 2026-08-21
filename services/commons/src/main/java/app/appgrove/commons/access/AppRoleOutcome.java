package app.appgrove.commons.access;

/**
 * Esito della domanda «che ruolo ha questa persona su questa applicazione?» (UC 0099). Tre esiti, mai un
 * quarto implicito — e la distinzione fra il secondo e il terzo è il punto di questa classe:
 *
 * <ul>
 *   <li>{@link Granted} — la persona ha un ruolo, e lo sappiamo;</li>
 *   <li>{@link NoAccess} — la persona <b>non ha accesso</b>, e lo sappiamo: è un'informazione, non
 *       un'assenza di informazione;</li>
 *   <li>{@link Unavailable} — <b>non lo sappiamo</b>: nessuna copia locale e il core non risponde. Si nega
 *       comunque (in assenza di informazione non si concede), ma con un messaggio diverso: dire «non hai i
 *       permessi» a chi li ha, per un guasto nostro, è accusare l'utente di un problema che è nostro.</li>
 * </ul>
 *
 * <p>Un tipo con tre casi invece di un {@code Optional} vuoto per due ragioni diverse: chi legge il codice
 * è costretto a decidere cosa fare del terzo caso, e non può dimenticarselo.
 */
public sealed interface AppRoleOutcome {

    /** La persona ha questo ruolo sull'applicazione (l'owner dell'account arriva qui come {@code admin}). */
    record Granted(AppRole role) implements AppRoleOutcome {}

    /** Diniego <b>noto</b>: la persona non ha accesso a questa applicazione. */
    record NoAccess() implements AppRoleOutcome {}

    /** Non decidibile: nessuna copia locale e la fonte di verità non è raggiungibile. */
    record Unavailable() implements AppRoleOutcome {}

    /** Il ruolo se c'è, altrimenti {@code null}. Comodità per chi ha già trattato gli altri due casi. */
    default AppRole roleOrNull() {
        return this instanceof Granted granted ? granted.role() : null;
    }
}
