package app.appgrove.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * Parità fra le lingue dei template email (UC 0018) — è la rete che impedisce la divergenza.
 *
 * <p>Il rischio concreto: si aggiunge una frase all'inglese e ci si dimentica dell'italiano, oppure
 * si rinomina un segnaposto in una lingua sola. Nessuno se ne accorge finché un utente non riceve
 * un'email monca — e siccome i testi sono resi da <b>due programmi diversi</b> (il servizio Java e
 * la Lambda Python), il difetto può manifestarsi solo in cloud.
 *
 * <p>Verifica anche che i file siano davvero <b>nell'artefatto</b>: se la copia da
 * {@code shared/email-templates} configurata nel {@code pom.xml} smettesse di funzionare, questo
 * test fallirebbe qui invece che all'avvio in produzione.
 */
class EmailTemplatesParityTest {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{([a-zA-Z][a-zA-Z0-9_]*)}}");

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void everyLanguageHasTheSameMessagesAndPlaceholders() {
        JsonNode en = catalog("en");
        JsonNode it = catalog("it");

        assertEquals(keys(en.get("messages")), keys(it.get("messages")),
                "i messaggi definiti devono essere gli stessi in tutte le lingue");

        for (String message : keys(en.get("messages"))) {
            JsonNode enMessage = en.get("messages").get(message);
            JsonNode itMessage = it.get("messages").get(message);

            assertEquals(keys(enMessage), keys(itMessage),
                    "campi diversi nel messaggio '" + message + "'");

            for (String field : keys(enMessage)) {
                assertEquals(placeholders(enMessage.get(field).asText()),
                        placeholders(itMessage.get(field).asText()),
                        "segnaposto diversi in '" + message + "." + field + "'");
                assertTrue(!itMessage.get(field).asText().isBlank(),
                        "testo vuoto in '" + message + "." + field + "' (it)");
            }
        }
    }

    @Test
    void layoutsAreOnTheClasspathAndDeclareTheirSlots() {
        String html = resource("email-templates/layout.html");
        String text = resource("email-templates/layout.txt");
        // Il collegamento è l'unica cosa senza la quale l'email non serve a niente.
        assertTrue(placeholders(html).contains("actionUrl"), "l'impaginazione grafica deve avere actionUrl");
        assertTrue(placeholders(text).contains("actionUrl"), "l'impaginazione testuale deve avere actionUrl");
    }

    private static JsonNode catalog(String locale) {
        try {
            JsonNode root = MAPPER.readTree(resource("email-templates/" + locale + ".json"));
            assertEquals(locale, root.path("language").asText(), "campo `language` incoerente col nome del file");
            return root;
        } catch (Exception e) {
            throw new AssertionError("catalogo email non leggibile: " + locale, e);
        }
    }

    private static Set<String> keys(JsonNode node) {
        assertNotNull(node, "nodo assente nel catalogo");
        Set<String> keys = new TreeSet<>();
        for (Iterator<String> it = node.fieldNames(); it.hasNext(); ) {
            keys.add(it.next());
        }
        return keys;
    }

    private static Set<String> placeholders(String text) {
        Set<String> found = new LinkedHashSet<>();
        Matcher m = PLACEHOLDER.matcher(text);
        while (m.find()) {
            found.add(m.group(1));
        }
        return found;
    }

    private static String resource(String path) {
        try (InputStream in = EmailTemplatesParityTest.class.getClassLoader().getResourceAsStream(path)) {
            assertNotNull(in, "risorsa assente dall'artefatto: " + path
                    + " — controlla la copia da shared/email-templates nel pom.xml");
            return new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (java.io.IOException e) {
            throw new AssertionError("risorsa non leggibile: " + path, e);
        }
    }
}
