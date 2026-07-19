package app.appgrove.auth;

import java.util.Locale;
import java.util.Set;

/**
 * Lingue delle email transazionali (UC 0018): inglese e italiano, con <b>ripiego sull'inglese</b>.
 *
 * <p>Il ripiego non è difensivo per abitudine: la lingua può mancare davvero (utenti creati prima di
 * questa colonna, chiamate senza il parametro, valori che non riconosciamo). In tutti quei casi
 * l'email deve partire lo stesso — una verifica indirizzo non spedita blocca una registrazione.
 *
 * <p>Le altre lingue (#13 G38) riguardano i contenuti pubblici del sito, non queste email.
 */
public final class Locales {

    public static final String DEFAULT = "en";

    public static final Set<String> SUPPORTED = Set.of("en", "it");

    private Locales() {}

    /**
     * Riconduce una lingua qualsiasi a una supportata. Accetta le forme comuni ({@code it},
     * {@code it-IT}, {@code IT_it}) guardando il solo prefisso di lingua; tutto il resto → inglese.
     */
    public static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return DEFAULT;
        }
        String language = raw.trim().toLowerCase(Locale.ROOT).split("[-_]", 2)[0];
        return SUPPORTED.contains(language) ? language : DEFAULT;
    }
}
