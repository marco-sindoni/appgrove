package app.appgrove.core.observability;

import jakarta.enterprise.context.ApplicationScoped;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Deep-link alla console AWS per la console "Diritti GDPR" (UC 0034, #13 L75): CloudWatch Logs
 * Insights pre-filtrato ({@code request_id}/{@code tenant_id}/{@code job_id} — i campi che il
 * logging strutturato porta sempre, invariante #4) e oggetto S3 dell'export. Costruiti da
 * <b>config per-ambiente</b>: senza {@code appgrove.aws-console.region} (locale: niente CloudWatch)
 * i link non vengono prodotti e la UI non li mostra. Formato URL best-effort sul formato documentato
 * della console AWS; la validazione sull'ambiente cloud reale è tracciata (UC 0034 "Punti aperti").
 */
@ApplicationScoped
public class AwsConsoleLinks {

    /** Regione della console AWS (es. {@code eu-south-1}); assente in locale → nessun link. */
    @ConfigProperty(name = "appgrove.aws-console.region")
    Optional<String> region;

    /** Log group interrogato da Logs Insights (es. {@code /ecs/appgrove}). */
    @ConfigProperty(name = "appgrove.aws-console.log-group")
    Optional<String> logGroup;

    /** Finestra temporale relativa della query (default 7 giorni). */
    static final long QUERY_WINDOW_SECONDS = 7 * 24 * 3600L;

    /**
     * URL di Logs Insights con query pre-filtrata sui valori dati (match sul messaggio: robusto sia
     * col log JSON sia col log testuale). Vuoto se regione o log group non sono configurati.
     */
    public Optional<String> logsInsightsUrl(Map<String, String> filters) {
        if (region.isEmpty() || logGroup.isEmpty() || filters.isEmpty()) {
            return Optional.empty();
        }
        String conditions = filters.entrySet().stream()
                .map(f -> "@message like \"" + f.getKey() + "=" + f.getValue() + "\"")
                .collect(Collectors.joining(" and "));
        String query = "fields @timestamp, @message | filter " + conditions
                + " | sort @timestamp desc | limit 200";
        String payload = "~(end~0~start~-" + QUERY_WINDOW_SECONDS
                + "~timeType~'RELATIVE~unit~'seconds~editorString~'" + jazz(query)
                + "~source~(~'" + jazz(logGroup.get()) + "))";
        String fragment = "logsV2:logs-insights$3FqueryDetail$3D"
                + URLEncoder.encode(payload, StandardCharsets.UTF_8).replace("%", "$");
        return Optional.of("https://" + region.get() + ".console.aws.amazon.com/cloudwatch/home?region="
                + region.get() + "#" + fragment);
    }

    /** URL dell'oggetto S3 nella console AWS (puntatore, non il contenuto). Vuoto senza regione. */
    public Optional<String> s3ObjectUrl(String bucket, String key) {
        if (region.isEmpty() || bucket == null || key == null) {
            return Optional.empty();
        }
        return Optional.of("https://" + region.get() + ".console.aws.amazon.com/s3/object/" + bucket
                + "?region=" + region.get() + "&prefix=" + URLEncoder.encode(key, StandardCharsets.UTF_8));
    }

    /** Codifica "a stella" della console CloudWatch: percent-encoding con {@code %} → {@code *}. */
    private static String jazz(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8)
                .replace("+", "*20")
                .replace("%", "*");
    }
}
