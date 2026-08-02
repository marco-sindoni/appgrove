package app.appgrove.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.appgrove.commons.email.EmailTemplateRenderer;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Email di autenticazione (UC 0018) rese davvero dal servizio: il bean è cablato sul renderer unico
 * di {@code services/commons} (UC 0085) con le lingue di questo servizio, e i template sono
 * <b>nell'artefatto di auth</b> — se la copia da {@code shared/email-templates} configurata nel
 * {@code pom.xml} smettesse di funzionare, questi test fallirebbero invece che l'avvio in produzione.
 *
 * <p>Il comportamento della resa (sostituzioni, escape, guardia sui segnaposto, ripiego di lingua,
 * messaggio sconosciuto) è coperto una volta sola dove vive il codice:
 * {@code app.appgrove.commons.email.EmailTemplateRendererTest}.
 */
@QuarkusTest
class EmailTemplatesTest {

    private static final String URL = "https://app.local.appgrove.app/verify?email=a%40b.test&code=123456";

    @Inject
    EmailTemplates templates;

    @Test
    void italianUserGetsItalianCopy() {
        EmailTemplateRenderer.Rendered r = templates.render("it", "verify", Map.of("actionUrl", URL));
        assertEquals("Conferma il tuo indirizzo email", r.subject());
        assertTrue(r.text().contains("Benvenuto su appgrove"), "corpo testuale in italiano");
        assertTrue(r.html().contains("Conferma l&#39;indirizzo"), "corpo grafico in italiano");
    }

    @Test
    void englishIsTheDefaultForAnythingElse() {
        // Le lingue del servizio sono quelle dichiarate in Locales: tutto il resto ripiega su EN.
        assertEquals("Confirm your email address",
                templates.render("de", "verify", Map.of("actionUrl", URL)).subject());
    }

    @Test
    void everyAuthMessageIsAvailableInTheArtifact() {
        for (String messageKey : new String[] {"verify", "reset", "invite"}) {
            EmailTemplateRenderer.Rendered r = templates.render(
                    "en", messageKey, Map.of("actionUrl", URL, "role", "owner"));
            assertTrue(!r.subject().isBlank(), "oggetto presente per il messaggio " + messageKey);
            assertTrue(r.text().contains(URL), "collegamento nel corpo testuale di " + messageKey);
            assertTrue(r.html().contains("&amp;code="), "collegamento con escape nel corpo grafico di " + messageKey);
        }
    }
}
