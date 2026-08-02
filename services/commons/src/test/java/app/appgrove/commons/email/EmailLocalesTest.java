package app.appgrove.commons.email;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.LinkedHashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Set di lingue delle email (UC 0085): normalizzazione, ripiego e coerenza della dichiarazione. */
class EmailLocalesTest {

    private final EmailLocales locales = new EmailLocales("en", Set.of("en", "it"));

    @Test
    void knownLanguagesPassThrough() {
        assertEquals("it", locales.normalize("it"));
        assertEquals("en", locales.normalize("en"));
    }

    @Test
    void regionalAndUntidyFormsAreRecognised() {
        for (String raw : new String[] {"it-IT", "it_IT", "IT", " it ", "IT-it"}) {
            assertEquals("it", locales.normalize(raw), "forma riconosciuta: " + raw);
        }
    }

    @Test
    void missingOrUnknownLanguageFallsBack() {
        for (String raw : new String[] {null, "", "   ", "de", "fr-FR", "spazzatura"}) {
            assertEquals("en", locales.normalize(raw), "ripiego per: " + raw);
        }
    }

    @Test
    void theFallbackMustBeOneOfTheSupportedLanguages() {
        // Un ripiego fuori dall'insieme sarebbe una lingua senza catalogo: si scopre alla
        // costruzione, non la prima volta che un utente scrive in una lingua non prevista.
        assertThrows(IllegalArgumentException.class, () -> new EmailLocales("de", Set.of("en", "it")));
    }

    @Test
    void aFallbackIsMandatory() {
        assertThrows(IllegalArgumentException.class, () -> new EmailLocales(null, Set.of("en")));
        assertThrows(IllegalArgumentException.class, () -> new EmailLocales("  ", Set.of("en")));
    }

    @Test
    void theSupportedSetIsDefensivelyCopied() {
        Set<String> mutable = new LinkedHashSet<>(Set.of("en", "it"));
        EmailLocales built = new EmailLocales("en", mutable);
        mutable.add("de");
        assertEquals("en", built.normalize("de"), "aggiungere lingue al set originale non cambia il renderer");
    }
}
