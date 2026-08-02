package app.appgrove.commons.email;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
 * Resa delle email transazionali dalla <b>sorgente unica</b> {@code shared/email-templates}, copiata
 * nell'artefatto di ogni servizio a tempo di build (vedi il {@code pom.xml} del servizio).
 *
 * <p>È il <b>renderer unico</b> della piattaforma (UC 0085): prima esisteva in due copie quasi
 * identiche, una nel servizio di autenticazione (verifica indirizzo, reimpostazione password,
 * invito — UC 0018) e una nel core per la conferma della newsletter (UC 0039). Due copie divergono
 * in silenzio — si corregge un escape da una parte e non dall'altra — ed è il difetto che questa
 * classe chiude.
 *
 * <p>La stessa cartella è resa anche dal Custom Message Lambda in Python
 * ({@code infra/modules/platform_shared/lambda/custom_message}), che implementa <b>gli stessi due
 * passaggi</b>. È il motivo per cui i testi non stanno nel codice: verifica e reimpostazione
 * password partono da Cognito in cloud e dal servizio in locale, e due copie del copy
 * divergerebbero in silenzio.
 *
 * <p>I due passaggi:
 * <ol>
 *   <li>le stringhe della lingua vengono risolte contro i valori dinamici ({@code {{role}}}, …);
 *   <li>le stringhe risolte riempiono i buchi dell'impaginazione condivisa
 *       ({@code layout.html} / {@code layout.txt}).
 * </ol>
 *
 * <p>Nella versione grafica ogni valore inserito è sottoposto a <b>escape</b>. Non è formalismo:
 * l'indirizzo di verifica contiene {@code &} fra i parametri e senza escape il collegamento
 * arriverebbe rotto nel lettore di posta.
 *
 * <p>Le lingue coperte sono un <b>parametro</b> ({@link EmailLocales}), non una costante: un
 * servizio può spedire in due lingue e un altro in cinque senza duplicare questa classe.
 *
 * <p>Non è un bean gestito dal contenitore di iniezione: ogni servizio lo costruisce una volta
 * dentro il proprio bean, passando l'insieme di lingue che gli serve. Il caricamento dei template
 * avviene alla costruzione, così un artefatto senza template fallisce all'avvio e non alla prima
 * email da spedire.
 */
public class EmailTemplateRenderer {

    private static final String BASE = "email-templates/";

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{([a-zA-Z][a-zA-Z0-9_]*)}}");

    /** Buchi dell'impaginazione riempiti dalle stringhe della lingua. */
    private static final List<String> SLOTS = List.of("heading", "intro", "actionLabel", "fallback", "footer");

    private final EmailLocales locales;
    private final String layoutHtml;
    private final String layoutText;
    private final Map<String, JsonNode> catalogs;

    /** Email resa nelle due versioni: la testuale è anche ciò che tiene i link leggibili ovunque. */
    public record Rendered(String subject, String text, String html) {}

    /** Carica impaginazione e cataloghi delle lingue richieste dal classpath del servizio. */
    public EmailTemplateRenderer(EmailLocales locales) {
        this.locales = locales;
        this.layoutHtml = readResource(BASE + "layout.html");
        this.layoutText = readResource(BASE + "layout.txt");
        ObjectMapper mapper = new ObjectMapper();
        Map<String, JsonNode> loaded = new LinkedHashMap<>();
        for (String locale : new TreeSet<>(locales.supported())) {
            try {
                loaded.put(locale, mapper.readTree(readResource(BASE + locale + ".json")));
            } catch (IOException e) {
                throw new IllegalStateException("Catalogo email non leggibile: " + locale, e);
            }
        }
        this.catalogs = Map.copyOf(loaded);
    }

    /** Le lingue coperte da questo renderer, con il loro ripiego. */
    public EmailLocales locales() {
        return locales;
    }

    /**
     * Rende il messaggio {@code messageKey} ({@code verify} | {@code reset} | {@code invite} |
     * {@code newsletter-confirm}) nella lingua indicata, ricondotta a una supportata
     * ({@link EmailLocales#normalize}).
     *
     * @param values valori dinamici; {@code actionUrl} è il collegamento del messaggio
     * @throws IllegalArgumentException se il messaggio non esiste nel catalogo
     * @throws IllegalStateException se resta un segnaposto non risolto
     */
    public Rendered render(String locale, String messageKey, Map<String, String> values) {
        JsonNode catalog = catalogs.get(locales.normalize(locale));
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
            found = EmailTemplateRenderer.class.getClassLoader().getResourceAsStream(path);
        }
        if (found == null) {
            throw new IllegalStateException(
                    "Template email assente dal classpath: " + path
                            + " — la copia da shared/email-templates è configurata nel pom del servizio.");
        }
        try (InputStream in = found) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Template email non leggibile: " + path, e);
        }
    }
}
