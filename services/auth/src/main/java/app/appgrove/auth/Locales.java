package app.appgrove.auth;

import app.appgrove.commons.email.EmailLocales;
import java.util.Set;

/**
 * Lingue delle email transazionali (UC 0018): inglese e italiano, con <b>ripiego sull'inglese</b>.
 *
 * <p>Il ripiego non è difensivo per abitudine: la lingua può mancare davvero (utenti creati prima di
 * questa colonna, chiamate senza il parametro, valori che non riconosciamo). In tutti quei casi
 * l'email deve partire lo stesso — una verifica indirizzo non spedita blocca una registrazione.
 *
 * <p>L'algoritmo di normalizzazione non vive più qui: sta in {@link EmailLocales}
 * ({@code services/commons}), unica sede da UC 0085. Questa classe resta il punto in cui il servizio
 * di autenticazione dichiara <b>quali</b> lingue copre, ed è usata anche fuori dalle email (lingua
 * dell'utente scritta a database, attributo passato a Cognito).
 *
 * <p>Le altre lingue (#13 G38) riguardano i contenuti pubblici del sito, non queste email.
 */
public final class Locales {

    public static final String DEFAULT = "en";

    public static final Set<String> SUPPORTED = Set.of("en", "it");

    /** Le lingue di questo servizio nella forma attesa dal renderer condiviso. */
    public static final EmailLocales EMAIL = new EmailLocales(DEFAULT, SUPPORTED);

    private Locales() {}

    /**
     * Riconduce una lingua qualsiasi a una supportata. Accetta le forme comuni ({@code it},
     * {@code it-IT}, {@code IT_it}) guardando il solo prefisso di lingua; tutto il resto → inglese.
     */
    public static String normalize(String raw) {
        return EMAIL.normalize(raw);
    }
}
