package app.appgrove.core.newsletter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resa dell'email di conferma della newsletter dalla sorgente unica {@code shared/email-templates}
 * (messaggio {@code newsletter-confirm}), copiata nell'artefatto a build time (vedi {@code pom.xml}).
 *
 * <p>È il gemello compatto di {@code EmailTemplates} del servizio auth: stessi due passaggi (stringhe
 * della lingua → impaginazione condivisa), stesso escape dei valori, stessa guardia sui segnaposto
 * non risolti. Lingue en/it con ripiego su en (convenzione UC 0018). L'unificazione dei due renderer
 * in {@code services/commons} è tracciata nel backlog: qui si evita di rifattorizzare auth.
 */
@ApplicationScoped
public class NewsletterEmailRenderer {

    private static final String BASE = "email-templates/";
    private static final String MESSAGE_KEY = "newsletter-confirm";
    private static final String DEFAULT_LOCALE = "en";
    private static final List<String> SUPPORTED = List.of("en", "it");
    private static final List<String> SLOTS = List.of("heading", "intro", "actionLabel", "fallback", "footer");
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{([a-zA-Z][a-zA-Z0-9_]*)}}");

    private final ObjectMapper mapper = new ObjectMapper();

    private String layoutHtml;
    private String layoutText;
    private Map<String, JsonNode> catalogs;

    /** Email resa nelle due versioni (grafica + testuale). */
    public record Rendered(String subject, String text, String html) {}

    @PostConstruct
    void load() {
        layoutHtml = readResource(BASE + "layout.html");
        layoutText = readResource(BASE + "layout.txt");
        Map<String, JsonNode> loaded = new LinkedHashMap<>();
        for (String locale : SUPPORTED) {
            try {
                loaded.put(locale, mapper.readTree(readResource(BASE + locale + ".json")));
            } catch (IOException e) {
                throw new IllegalStateException("Catalogo email non leggibile: " + locale, e);
            }
        }
        catalogs = Map.copyOf(loaded);
    }

    /** Rende il messaggio di conferma con il collegamento {@code actionUrl}. */
    public Rendered renderConfirm(String locale, String actionUrl) {
        JsonNode catalog = catalogs.get(normalize(locale));
        JsonNode message = catalog.path("messages").path(MESSAGE_KEY);
        if (message.isMissingNode()) {
            throw new IllegalStateException("Messaggio email assente: " + MESSAGE_KEY);
        }
        Map<String, String> values = Map.of("actionUrl", actionUrl);

        Map<String, String> slots = new LinkedHashMap<>();
        slots.put("brand", catalog.path("brand").asText("appgrove"));
        for (String slot : SLOTS) {
            slots.put(slot, substitute(message.path(slot).asText(""), values));
        }
        slots.put("actionUrl", actionUrl);

        Map<String, String> escaped = new LinkedHashMap<>();
        slots.forEach((k, v) -> escaped.put(k, escapeHtml(v)));

        Rendered rendered = new Rendered(
                substitute(message.path("subject").asText(""), values),
                substitute(layoutText, slots),
                substitute(layoutHtml, escaped));

        if (hasPlaceholder(rendered.subject()) || hasPlaceholder(rendered.text()) || hasPlaceholder(rendered.html())) {
            throw new IllegalStateException("Segnaposto non risolto nel messaggio newsletter-confirm (lingua " + locale + ")");
        }
        return rendered;
    }

    static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return DEFAULT_LOCALE;
        }
        String lang = raw.trim().toLowerCase(Locale.ROOT).split("[-_]", 2)[0];
        return SUPPORTED.contains(lang) ? lang : DEFAULT_LOCALE;
    }

    private static boolean hasPlaceholder(String text) {
        return PLACEHOLDER.matcher(text).find();
    }

    private static String substitute(String template, Map<String, String> values) {
        Matcher matcher = PLACEHOLDER.matcher(template);
        StringBuilder out = new StringBuilder();
        while (matcher.find()) {
            String value = values.get(matcher.group(1));
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
            found = NewsletterEmailRenderer.class.getClassLoader().getResourceAsStream(path);
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
