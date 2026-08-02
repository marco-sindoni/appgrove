package app.appgrove.commons.email;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Resa dei template email dal renderer unico (UC 0085): scelta della lingua, sostituzioni, escape
 * della versione grafica, guardia sui segnaposto, set di lingue parametrizzabile.
 *
 * <p>Copre insieme i messaggi che prima stavano in due renderer separati: quelli di autenticazione
 * (UC 0018) e la conferma della newsletter (UC 0039).
 */
class EmailTemplateRendererTest {

    private static final String URL = "https://app.local.appgrove.app/verify?email=a%40b.test&code=123456";

    /** Le lingue delle email transazionali: inglese e italiano, ripiego sull'inglese. */
    private final EmailTemplateRenderer renderer =
            new EmailTemplateRenderer(new EmailLocales("en", Set.of("en", "it")));

    @Test
    void italianUserGetsItalianCopy() {
        EmailTemplateRenderer.Rendered r = renderer.render("it", "verify", Map.of("actionUrl", URL));
        assertEquals("Conferma il tuo indirizzo email", r.subject());
        assertTrue(r.text().contains("Benvenuto su appgrove"), "corpo testuale in italiano");
        assertTrue(r.html().contains("Conferma l&#39;indirizzo"), "corpo grafico in italiano");
    }

    @Test
    void fallbackLanguageForAnythingElse() {
        // Assente, sconosciuta, o una variante regionale che non gestiamo: sempre la lingua di ripiego.
        for (String locale : new String[] {null, "", "de", "fr-FR", "spazzatura"}) {
            assertEquals("Confirm your email address",
                    renderer.render(locale, "verify", Map.of("actionUrl", URL)).subject(),
                    "ripiego su EN per la lingua: " + locale);
        }
    }

    @Test
    void regionalVariantsResolveToTheirLanguage() {
        for (String locale : new String[] {"it-IT", "it_IT", "IT", " it "}) {
            assertEquals("Conferma il tuo indirizzo email",
                    renderer.render(locale, "verify", Map.of("actionUrl", URL)).subject(),
                    "variante regionale riconosciuta: " + locale);
        }
    }

    @Test
    void dynamicValuesAreSubstitutedInBothVersions() {
        EmailTemplateRenderer.Rendered r =
                renderer.render("it", "invite", Map.of("actionUrl", URL, "role", "amministratore"));
        assertTrue(r.text().contains("amministratore"), "ruolo nel corpo testuale");
        assertTrue(r.html().contains("amministratore"), "ruolo nel corpo grafico");
    }

    /**
     * Senza escape il collegamento arriverebbe rotto: l'indirizzo di verifica contiene {@code &} fra
     * i parametri, e un lettore di posta lo interpreterebbe come inizio di entità HTML.
     */
    @Test
    void htmlVersionEscapesTheLink() {
        EmailTemplateRenderer.Rendered r = renderer.render("en", "verify", Map.of("actionUrl", URL));
        assertTrue(r.html().contains("code=123456"), "il collegamento c'è");
        assertTrue(r.html().contains("&amp;code="), "la e commerciale è sottoposta a escape");
        assertFalse(r.html().contains("\"" + URL + "\""), "l'URL grezzo non deve finire nell'attributo");
        assertTrue(r.text().contains(URL), "la versione testuale porta invece l'URL così com'è");
    }

    @Test
    void htmlVersionEscapesDynamicValuesToo() {
        EmailTemplateRenderer.Rendered r = renderer.render(
                "en", "invite", Map.of("actionUrl", URL, "role", "<b>admin</b> & \"owner\""));
        assertTrue(r.html().contains("&lt;b&gt;admin&lt;/b&gt; &amp; &quot;owner&quot;"),
                "i valori dinamici finiscono con escape nella versione grafica");
        assertTrue(r.text().contains("<b>admin</b> & \"owner\""),
                "la versione testuale porta il valore così com'è");
    }

    @Test
    void newsletterConfirmIsRenderedByTheSameRenderer() {
        EmailTemplateRenderer.Rendered r =
                renderer.render("it", "newsletter-confirm", Map.of("actionUrl", URL));
        assertFalse(r.subject().isBlank(), "l'oggetto della conferma newsletter c'è");
        assertTrue(r.text().contains(URL), "il collegamento di conferma è nel corpo testuale");
        assertTrue(r.html().contains("&amp;code="), "e con escape nel corpo grafico");
    }

    @Test
    void unresolvedPlaceholdersFailLoudly() {
        // Manca `role`: meglio un errore qui che un'email con dentro "{{role}}".
        assertThrows(IllegalStateException.class,
                () -> renderer.render("it", "invite", Map.of("actionUrl", URL)));
    }

    @Test
    void unknownMessageIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> renderer.render("en", "messaggio-inesistente", Map.of("actionUrl", URL)));
    }

    /**
     * Il senso del set di lingue parametrizzabile (UC 0085): con un insieme più stretto la stessa
     * classe serve un servizio che copre meno lingue, senza toccare il codice della resa.
     */
    @Test
    void aNarrowerLanguageSetFallsBackToItsOwnDefault() {
        EmailTemplateRenderer onlyEnglish =
                new EmailTemplateRenderer(new EmailLocales("en", Set.of("en")));
        assertEquals("Confirm your email address",
                onlyEnglish.render("it", "verify", Map.of("actionUrl", URL)).subject(),
                "italiano non coperto → ripiego sull'inglese");
        assertEquals(Set.of("en"), onlyEnglish.locales().supported());
    }

    @Test
    void aDifferentDefaultLanguageIsHonoured() {
        EmailTemplateRenderer italianFirst =
                new EmailTemplateRenderer(new EmailLocales("it", Set.of("en", "it")));
        assertEquals("Conferma il tuo indirizzo email",
                italianFirst.render("de", "verify", Map.of("actionUrl", URL)).subject(),
                "lingua sconosciuta → ripiego sull'italiano, che qui è il default del set");
    }
}
