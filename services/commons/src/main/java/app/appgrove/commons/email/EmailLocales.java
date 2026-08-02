package app.appgrove.commons.email;

import java.util.Locale;
import java.util.Set;

/**
 * Insieme delle lingue in cui un servizio spedisce le sue email, con la <b>lingua di ripiego</b>.
 *
 * <p>È un parametro, non una costante di piattaforma: le email di autenticazione e quelle della
 * newsletter possono coprire lingue diverse senza che questo costringa a due renderer diversi
 * (UC 0085). Il ripiego non è difensivo per abitudine: la lingua può mancare davvero (utenti creati
 * prima della colonna, chiamate senza il parametro, valori che non riconosciamo) e in tutti quei
 * casi l'email deve partire lo stesso — una verifica indirizzo non spedita blocca una registrazione.
 *
 * @param fallback lingua usata quando quella richiesta manca o non è supportata
 * @param supported lingue effettivamente disponibili (deve contenere {@code fallback})
 */
public record EmailLocales(String fallback, Set<String> supported) {

    public EmailLocales {
        if (fallback == null || fallback.isBlank()) {
            throw new IllegalArgumentException("La lingua di ripiego è obbligatoria");
        }
        supported = Set.copyOf(supported);
        if (!supported.contains(fallback)) {
            throw new IllegalArgumentException(
                    "La lingua di ripiego '" + fallback + "' non è fra quelle supportate: " + supported);
        }
    }

    /**
     * Riconduce una lingua qualsiasi a una supportata. Accetta le forme comuni ({@code it},
     * {@code it-IT}, {@code IT_it}) guardando il solo prefisso di lingua; tutto il resto → ripiego.
     */
    public String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        String language = raw.trim().toLowerCase(Locale.ROOT).split("[-_]", 2)[0];
        return supported.contains(language) ? language : fallback;
    }
}
