package app.appgrove.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resa delle email di autenticazione dalla <b>sorgente unica</b> {@code shared/email-templates}
 * (UC 0018), copiata nell'artefatto a build time (vedi {@code pom.xml}).
 *
 * <p>La stessa cartella è resa anche dal Custom Message Lambda in Python
 * ({@code infra/modules/platform_shared/lambda/custom_message}), che implementa <b>gli stessi due
 * passaggi</b> di questa classe. È il motivo per cui i testi non stanno nel codice: verifica e
 * reimpostazione password partono da Cognito in cloud e dal servizio in locale, e due copie del copy
 * divergerebbero in silenzio.
 *
 * <p>I due passaggi:
 * <ol>
 *   <li>le stringhe della lingua vengono risolte contro i valori dinamici ({@code {{role}}}, …);
 *   <li>le stringhe risolte riempiono i buchi dell'impaginazione condivisa.
 * </ol>
 *
 * <p>Nella versione grafica ogni valore inserito è sottoposto a <b>escape</b>. Non è formalismo:
 * l'indirizzo di verifica contiene {@code &} fra i parametri e senza escape il collegamento
 * arriverebbe rotto nel lettore di posta.
 */
@ApplicationScoped
public class EmailTemplates {

    private static final String BASE = "email-templates/";

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{([a-zA-Z][a-zA-Z0-9_]*)}}");

    /** Buchi dell'impaginazione riempiti dalle stringhe della lingua. */
    private static final List<String> SLOTS = List.of("heading", "intro", "actionLabel", "fallback", "footer");

    private final ObjectMapper mapper = new ObjectMapper();

    private String layoutHtml;
    private String layoutText;
    private Map<String, JsonNode> catalogs;

    /** Email resa nelle due versioni: la testuale è anche ciò che tiene i link leggibili ovunque. */
    public record Rendered(String subject, String text, String html) {}

    @PostConstruct
    void load() {
        layoutHtml = readResource(BASE + "layout.html");
        layoutText = readResource(BASE + "layout.txt");
        Map<String, JsonNode> loaded = new LinkedHashMap<>();
        for (String locale : new TreeSet<>(Locales.SUPPORTED)) {
            try {
                loaded.put(locale, mapper.readTree(readResource(BASE + locale + ".json")));
            } catch (IOException e) {
                throw new IllegalStateException("Catalogo email non leggibile: " + locale, e);
            }
        }
        catalogs = Map.copyOf(loaded);
    }

    /**
     * Rende il messaggio {@code messageKey} ({@code verify} | {@code reset} | {@code invite}) nella
     * lingua indicata (ricondotta da {@link Locales#normalize}).
     *
     * @param values valori dinamici; {@code actionUrl} è il collegamento del messaggio
     */
    public Rendered render(String locale, String messageKey, Map<String, String> values) {
        JsonNode catalog = catalogs.get(Locales.normalize(locale));
        JsonNode message = catalog.path("messages").path(messageKey);
        if (message.isMissingNode()) {
            throw new IllegalArgumentException("Messaggio email sconosciuto: " + messageKey);
        }

        Map<String, String> slots = new LinkedHashMap<>();
        slots.put("brand", catalog.path("brand").asText("appgrove"));
        for (String slot : SLOTS) {
            slots.put(slot, substitute(message.path(slot).asText(""), values));
        }
        slots.put("actionUrl", values.getOrDefault("actionUrl", ""));

        Map<String, String> escaped = new LinkedHashMap<>();
        slots.forEach((k, v) -> escaped.put(k, escapeHtml(v)));

        Rendered rendered = new Rendered(
                substitute(message.path("subject").asText(""), values),
                substitute(layoutText, slots),
                substitute(layoutHtml, escaped));

        // Un segnaposto rimasto è un template incoerente: meglio fallire qui che spedire a un utente
        // un'email con dentro il nome di un buco. Si cerca un segnaposto VERO (stesso schema della
        // sostituzione), non una parentesi qualsiasi: nei template ci sono anche commenti e stili.
        if (hasPlaceholder(rendered.subject()) || hasPlaceholder(rendered.text()) || hasPlaceholder(rendered.html())) {
            throw new IllegalStateException(
                    "Segnaposto non risolto nel messaggio '" + messageKey + "' (lingua " + locale + ")");
        }
        return rendered;
    }

    private static boolean hasPlaceholder(String text) {
        return PLACEHOLDER.matcher(text).find();
    }

    private static String substitute(String template, Map<String, String> values) {
        Matcher matcher = PLACEHOLDER.matcher(template);
        StringBuilder out = new StringBuilder();
        while (matcher.find()) {
            String value = values.get(matcher.group(1));
            // Segnaposto sconosciuto: lasciato intatto di proposito, così la guardia in render()
            // lo intercetta invece di sostituirlo con una stringa vuota silenziosa.
            matcher.appendReplacement(out, Matcher.quoteReplacement(value != null ? value : matcher.group()));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    private static String escapeHtml(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private static String readResource(String path) {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        InputStream found = loader != null ? loader.getResourceAsStream(path) : null;
        if (found == null) {
            found = EmailTemplates.class.getClassLoader().getResourceAsStream(path);
        }
        if (found == null) {
            throw new IllegalStateException(
                    "Template email assente dal classpath: " + path
                            + " — la copia da shared/email-templates è configurata nel pom.");
        }
        try (InputStream in = found) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Template email non leggibile: " + path, e);
        }
    }
}
