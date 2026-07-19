package app.appgrove.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Resa dei template email EN/IT (UC 0018): scelta lingua, sostituzioni, escape della versione grafica. */
@QuarkusTest
class EmailTemplatesTest {

    private static final String URL = "https://app.local.appgrove.app/verify?email=a%40b.test&code=123456";

    @Inject
    EmailTemplates templates;

    @Test
    void italianUserGetsItalianCopy() {
        EmailTemplates.Rendered r = templates.render("it", "verify", Map.of("actionUrl", URL));
        assertEquals("Conferma il tuo indirizzo email", r.subject());
        assertTrue(r.text().contains("Benvenuto su appgrove"), "corpo testuale in italiano");
        assertTrue(r.html().contains("Conferma l&#39;indirizzo"), "corpo grafico in italiano");
    }

    @Test
    void englishIsTheDefaultForAnythingElse() {
        // Assente, sconosciuta, o una variante regionale che non gestiamo: sempre inglese.
        for (String locale : new String[] {null, "", "de", "fr-FR", "spazzatura"}) {
            assertEquals("Confirm your email address",
                    templates.render(locale, "verify", Map.of("actionUrl", URL)).subject(),
                    "ripiego su EN per la lingua: " + locale);
        }
    }

    @Test
    void regionalVariantsResolveToTheirLanguage() {
        for (String locale : new String[] {"it-IT", "it_IT", "IT", " it "}) {
            assertEquals("Conferma il tuo indirizzo email",
                    templates.render(locale, "verify", Map.of("actionUrl", URL)).subject(),
                    "variante regionale riconosciuta: " + locale);
        }
    }

    @Test
    void dynamicValuesAreSubstitutedInBothVersions() {
        EmailTemplates.Rendered r =
                templates.render("it", "invite", Map.of("actionUrl", URL, "role", "amministratore"));
        assertTrue(r.text().contains("amministratore"), "ruolo nel corpo testuale");
        assertTrue(r.html().contains("amministratore"), "ruolo nel corpo grafico");
    }

    /**
     * Senza escape il collegamento arriverebbe rotto: l'indirizzo di verifica contiene {@code &} fra
     * i parametri, e un lettore di posta lo interpreterebbe come inizio di entità HTML.
     */
    @Test
    void htmlVersionEscapesTheLink() {
        EmailTemplates.Rendered r = templates.render("en", "verify", Map.of("actionUrl", URL));
        assertTrue(r.html().contains("code=123456"), "il collegamento c'è");
        assertTrue(r.html().contains("&amp;code="), "la e commerciale è sottoposta a escape");
        assertFalse(r.html().contains("\"" + URL + "\""), "l'URL grezzo non deve finire nell'attributo");
        assertTrue(r.text().contains(URL), "la versione testuale porta invece l'URL così com'è");
    }

    @Test
    void unresolvedPlaceholdersFailLoudly() {
        // Manca `role`: meglio un errore qui che un'email con dentro "{{role}}".
        assertThrows(IllegalStateException.class,
                () -> templates.render("it", "invite", Map.of("actionUrl", URL)));
    }

    @Test
    void unknownMessageIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> templates.render("en", "messaggio-inesistente", Map.of("actionUrl", URL)));
    }
}
