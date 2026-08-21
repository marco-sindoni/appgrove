package app.appgrove.commons.access;

/**
 * Varco del ruolo per applicazione non superato (UC 0099). Mappata a problem+json da
 * {@code AppRoleRequiredMapper}.
 *
 * <p><b>Tre casi, non uno.</b> «Non entri», «non puoi fare <i>questo</i>» e «non lo so» sono tre cose
 * diverse e vanno dette in modo diverso: chi non ha accesso deve sapere a chi chiederlo, chi ha un ruolo
 * troppo basso deve sapere quale serve, e chi incontra un guasto nostro non va accusato di non avere
 * permessi che invece ha. Ogni caso porta un identificativo <b>stabile</b> nel campo {@code type} del
 * corpo dell'errore, perché l'interfaccia distingua i casi <b>senza interpretare un messaggio</b>: il
 * messaggio del server è in italiano, l'interfaccia parla cinque lingue. Sono contratto.
 */
public class AppRoleRequiredException extends RuntimeException {

    /** Nessun accesso all'applicazione: serve l'abilitazione dell'owner o di un {@code admin}. */
    public static final String TYPE_NO_ACCESS = "urn:appgrove:app-role:no-access";

    /** Accesso c'è, ruolo insufficiente per <b>questa</b> operazione. */
    public static final String TYPE_INSUFFICIENT = "urn:appgrove:app-role:insufficient";

    /** Non decidibile: guasto nostro, non permesso mancante. */
    public static final String TYPE_UNAVAILABLE = "urn:appgrove:app-role:unavailable";

    /** Il caso, che determina codice di stato, titolo e identificativo stabile. */
    public enum Case {
        NO_ACCESS(403, TYPE_NO_ACCESS, "Accesso all'applicazione non abilitato"),
        INSUFFICIENT_ROLE(403, TYPE_INSUFFICIENT, "Ruolo insufficiente"),
        UNAVAILABLE(503, TYPE_UNAVAILABLE, "Servizio momentaneamente non disponibile");

        private final int status;
        private final String type;
        private final String title;

        Case(int status, String type, String title) {
            this.status = status;
            this.type = type;
            this.title = title;
        }

        public int status() {
            return status;
        }

        public String type() {
            return type;
        }

        public String title() {
            return title;
        }
    }

    private final Case kind;
    private final String appSlug;
    private final AppRole required;
    private final AppRole held;

    private AppRoleRequiredException(Case kind, String appSlug, AppRole required, AppRole held, String message) {
        super(message);
        this.kind = kind;
        this.appSlug = appSlug;
        this.required = required;
        this.held = held;
    }

    /** «Non entri»: nessun accesso all'applicazione. Dice a chi chiedere l'abilitazione. */
    public static AppRoleRequiredException noAccess(String appSlug, AppRole required) {
        return new AppRoleRequiredException(
                Case.NO_ACCESS,
                appSlug,
                required,
                null,
                "Non hai accesso all'applicazione '" + appSlug
                        + "': chiedi al titolare dell'account o a un amministratore dell'applicazione di"
                        + " abilitarti.");
    }

    /** «Non puoi fare questo»: il ruolo c'è ma non basta. <b>Nomina</b> il ruolo che serve. */
    public static AppRoleRequiredException insufficient(String appSlug, AppRole required, AppRole held) {
        return new AppRoleRequiredException(
                Case.INSUFFICIENT_ROLE,
                appSlug,
                required,
                held,
                "Per questa operazione su '" + appSlug + "' serve almeno il ruolo '" + required
                        + "': il tuo ruolo è '" + held + "'.");
    }

    /**
     * «Non lo so»: nessuna copia locale del ruolo e la fonte di verità non risponde. Si nega — in assenza
     * di informazione non si concede — ma il testo parla di un guasto, non di un permesso mancante.
     */
    public static AppRoleRequiredException unavailable(String appSlug) {
        return new AppRoleRequiredException(
                Case.UNAVAILABLE,
                appSlug,
                null,
                null,
                "Non è stato possibile verificare i tuoi permessi su '" + appSlug
                        + "' in questo momento: riprova fra qualche istante. Non è un problema dei tuoi"
                        + " permessi.");
    }

    public Case kind() {
        return kind;
    }

    public String appSlug() {
        return appSlug;
    }

    /** Ruolo minimo richiesto dall'operazione; {@code null} nel caso del guasto. */
    public AppRole required() {
        return required;
    }

    /** Ruolo effettivamente posseduto; {@code null} se nessuno o non noto. */
    public AppRole held() {
        return held;
    }
}
