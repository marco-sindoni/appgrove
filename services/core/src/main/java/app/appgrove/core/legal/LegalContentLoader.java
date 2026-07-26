package app.appgrove.core.legal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import jakarta.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Carica i documenti legali <b>bundlati nella classpath</b> ({@code /legal/*.md} + {@code /legal/entity.yaml},
 * copiati da {@code content/legal/} dal maven-resources-plugin) e ne risolve i token {@code {{titolare.<campo>}}}
 * da {@code entity.yaml} — stessa logica di {@code site/src/lib/legal.ts}, così sito e app non duplicano i testi
 * (UC 0056, chiude il punto aperto "sostituzione token nel rendering in-app").
 *
 * <p>La lingua <b>IT è facente fede</b> per la versione (frontmatter): la major di ri-accettazione si deriva
 * da lì. Un token senza chiave in {@code entity.yaml} è un errore (integrità referenziale, come il check CI).
 */
@ApplicationScoped
public class LegalContentLoader {

    /** Lingua di riferimento per il versioning (content/legal/README: IT facente fede). */
    public static final String AUTHORITATIVE_LANG = "it";

    private static final Pattern TOKEN_RE = Pattern.compile("\\{\\{\\s*([a-zA-Z0-9_.]+)\\s*}}");
    private static final Pattern FRONTMATTER_RE =
            Pattern.compile("^---\\s*\\R(.*?)\\R---\\s*\\R(.*)$", Pattern.DOTALL);

    private final YAMLMapper yaml = new YAMLMapper();

    /** Contenuto di un documento legale col testo (markdown) coi token già risolti. */
    public record LegalDoc(LegalComponent component, String lang, String version, int major, LocalDate effectiveDate, String markdown) {}

    /** Solo la testata di versione (frontmatter), lingua IT — usata dalla sync per popolare legal_version. */
    public record LegalMeta(LegalComponent component, String version, int major, LocalDate effectiveDate) {}

    /** Frontmatter del componente nella lingua facente fede (IT). Lancia se il file manca. */
    public LegalMeta metaOf(LegalComponent component) {
        LegalDoc doc = load(component, AUTHORITATIVE_LANG);
        return new LegalMeta(component, doc.version(), doc.major(), doc.effectiveDate());
    }

    /** Carica un componente in una lingua: frontmatter + markdown coi token risolti. */
    public LegalDoc load(LegalComponent component, String lang) {
        String resource = "/legal/" + component.name() + "." + lang + ".md";
        String raw = readResource(resource);
        Matcher m = FRONTMATTER_RE.matcher(raw);
        if (!m.matches()) {
            throw new IllegalStateException("Frontmatter mancante o malformato in " + resource);
        }
        JsonNode fm = parseYaml(m.group(1), resource);
        String body = m.group(2);
        String version = requireText(fm, "version", resource);
        LocalDate effective = LocalDate.parse(requireText(fm, "effective_date", resource));
        int major = majorOf(version);
        String resolved = substituteTokens(body, loadEntity());
        return new LegalDoc(component, lang, version, major, effective, resolved);
    }

    /** Prima cifra del semver = soglia di ri-accettazione (README content/legal). */
    public static int majorOf(String version) {
        return Integer.parseInt(version.split("\\.")[0].trim());
    }

    /** Valori del titolare come mappa piatta {@code titolare.<campo>} → stringa (da /legal/entity.yaml). */
    public Map<String, String> loadEntity() {
        JsonNode root = parseYaml(readResource("/legal/entity.yaml"), "/legal/entity.yaml");
        Map<String, String> flat = new HashMap<>();
        flatten(root, "", flat);
        return flat;
    }

    private void flatten(JsonNode node, String prefix, Map<String, String> out) {
        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> it = node.fields();
            while (it.hasNext()) {
                Map.Entry<String, JsonNode> e = it.next();
                String key = prefix.isEmpty() ? e.getKey() : prefix + "." + e.getKey();
                flatten(e.getValue(), key, out);
            }
        } else {
            out.put(prefix, node.asText());
        }
    }

    /** Sostituisce {@code {{titolare.<campo>}}} coi valori di {@code entity}; lancia se un token non risolve. */
    public static String substituteTokens(String markdown, Map<String, String> entity) {
        Matcher m = TOKEN_RE.matcher(markdown);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String key = m.group(1);
            String value = entity.get(key);
            if (value == null) {
                throw new IllegalStateException(
                        "Token legale non risolto: {{" + key + "}} — nessuna chiave \"" + key + "\" in content/legal/entity.yaml");
            }
            m.appendReplacement(sb, Matcher.quoteReplacement(value));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private JsonNode parseYaml(String text, String where) {
        try {
            return yaml.readTree(text);
        } catch (IOException e) {
            throw new UncheckedIOException("YAML non valido in " + where, e);
        }
    }

    private String requireText(JsonNode node, String field, String where) {
        JsonNode v = node.get(field);
        if (v == null || v.isNull() || v.asText().isBlank()) {
            throw new IllegalStateException("Campo frontmatter '" + field + "' mancante in " + where);
        }
        return v.asText();
    }

    private String readResource(String path) {
        try (InputStream in = LegalContentLoader.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("Risorsa legale non trovata nella classpath: " + path
                        + " (il maven-resources-plugin copia content/legal in /legal?)");
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Lettura risorsa legale fallita: " + path, e);
        }
    }
}
