package app.appgrove.commons.access;

/**
 * Una operazione dichiarata nel documento delle operazioni di una applicazione (UC 0101), col
 * <b>ruolo minimo</b> che richiede — oppure col <b>motivo per cui è esente</b> dai ruoli.
 *
 * <p><b>Ruolo minimo ed esenzione sono alternativi, e il record lo impone.</b> Un'operazione «esente ma con
 * ruolo minimo {@code admin}» è una contraddizione, e un'operazione senza né l'uno né l'altro è una
 * dimenticanza travestita da dichiarazione: entrambe le forme sono rifiutate dal costruttore, così lo stato
 * illegale non è rappresentabile e il collaudo non deve inseguirlo. Si costruisce solo con
 * {@link #requiring} o {@link #exempt}.
 *
 * <p><b>Perché la dichiarazione nomina la classe e il metodo Java</b> e non il percorso HTTP: perché il
 * collaudo deve poter legare la riga del documento al <b>codice reale</b> — leggere l'annotazione effettiva
 * del varco, vedere che il metodo esiste ancora, accorgersi che è stato rinominato. Un confronto fra
 * stringhe di percorso sarebbe più bello da leggere e non dimostrerebbe niente.
 *
 * @param id identificativo stabile dell'operazione, es. {@code "invoices.create"}. Stabile significa che
 *     non cambia quando cambia il percorso HTTP: ci si riferisce a questo nella documentazione utente
 * @param descrizione descrizione breve in <b>italiano</b>
 * @param description descrizione breve in <b>inglese</b>
 * @param resource classe della risorsa JAX-RS che espone l'operazione
 * @param javaMethod nome del metodo Java che la realizza
 * @param minimumRole ruolo minimo richiesto, oppure {@code null} se l'operazione è esente dai ruoli
 * @param exemptionReason motivo dell'esenzione, oppure {@code null} se l'operazione richiede un ruolo
 */
public record AppOperation(
        String id,
        String descrizione,
        String description,
        Class<?> resource,
        String javaMethod,
        AppRole minimumRole,
        String exemptionReason) {

    public AppOperation {
        requireText(id, "id");
        requireText(descrizione, "descrizione (italiano)");
        requireText(description, "description (inglese)");
        if (resource == null) {
            throw new IllegalArgumentException("operazione " + id + ": la classe della risorsa è obbligatoria");
        }
        requireText(javaMethod, "javaMethod");
        boolean exempt = exemptionReason != null && !exemptionReason.isBlank();
        if (minimumRole == null && !exempt) {
            throw new IllegalArgumentException(
                    "operazione " + id + ": serve un ruolo minimo, oppure il motivo dell'esenzione dai ruoli."
                            + " Un'operazione senza né l'uno né l'altro non è dichiarata: è dimenticata");
        }
        if (minimumRole != null && exempt) {
            throw new IllegalArgumentException(
                    "operazione " + id + ": ruolo minimo ed esenzione sono alternativi. Un'operazione esente"
                            + " dai ruoli non ha un ruolo minimo, altrimenti non è esente");
        }
    }

    /** Operazione che richiede un ruolo: il caso normale, classificato con la cascata di UC 0101 §4. */
    public static AppOperation requiring(
            String id, String descrizione, String description, Class<?> resource, String javaMethod, AppRole role) {
        if (role == null) {
            throw new IllegalArgumentException("operazione " + id + ": il ruolo minimo è obbligatorio qui;"
                    + " per un'operazione esente si usa AppOperation.exempt(…) col motivo");
        }
        return new AppOperation(id, descrizione, description, resource, javaMethod, role, null);
    }

    /**
     * Operazione <b>esente dai ruoli</b>, col motivo per cui lo è. Le sole esenzioni ammesse dalla storia
     * sono i diritti dell'interessato sui propri dati personali e lo stato di quota informativo: il motivo
     * è obbligatorio perché è ciò che distingue un'esenzione voluta da una dimenticanza, e perché va letto
     * da chi rivede la classificazione fra un anno.
     */
    public static AppOperation exempt(
            String id, String descrizione, String description, Class<?> resource, String javaMethod, String reason) {
        requireText(reason, "motivo dell'esenzione");
        return new AppOperation(id, descrizione, description, resource, javaMethod, null, reason);
    }

    /** L'operazione è raggiungibile senza alcun ruolo sull'applicazione. */
    public boolean exemptFromRoles() {
        return minimumRole == null;
    }

    private static void requireText(String value, String what) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("operazione: " + what + " è obbligatorio e non può essere vuoto");
        }
    }
}
