package app.appgrove.core.observability;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Deep-link console AWS (UC 0034, #13 L75): senza regione configurata (locale) nessun link;
 * con la config, URL di Logs Insights con la query filtrata codificata e URL dell'oggetto S3.
 * Test unitario puro (i campi di config sono package-private).
 */
class AwsConsoleLinksTest {

    @Test
    void withoutRegionNoLinksAreBuilt() {
        AwsConsoleLinks links = new AwsConsoleLinks();
        links.region = Optional.empty();
        links.logGroup = Optional.empty();
        assertTrue(links.logsInsightsUrl(Map.of("tenant_id", "t1")).isEmpty());
        assertTrue(links.s3ObjectUrl("bucket", "key").isEmpty());
    }

    @Test
    void logsInsightsUrlCarriesRegionGroupAndFilter() {
        AwsConsoleLinks links = new AwsConsoleLinks();
        links.region = Optional.of("eu-south-1");
        links.logGroup = Optional.of("/ecs/appgrove");
        String url = links.logsInsightsUrl(Map.of("job_id", "abc-123")).orElseThrow();
        assertTrue(url.startsWith("https://eu-south-1.console.aws.amazon.com/cloudwatch/"),
                "regione nel dominio: " + url);
        assertTrue(url.contains("logs-insights"), url);
        // la query filtrata è nel payload (codifica a stella: %XX → *XX)
        assertTrue(url.contains("job_id*3Dabc-123"), "filtro codificato nel payload: " + url);
        assertTrue(url.contains("*2Fecs*2Fappgrove"), "log group codificato nel payload: " + url);
    }

    @Test
    void s3ObjectUrlPointsToBucketAndKey() {
        AwsConsoleLinks links = new AwsConsoleLinks();
        links.region = Optional.of("eu-south-1");
        links.logGroup = Optional.empty();
        String url = links.s3ObjectUrl("gdpr-export", "jobs/j1/export.zip").orElseThrow();
        assertTrue(url.contains("s3/object/gdpr-export"), url);
        assertTrue(url.contains("prefix=jobs%2Fj1%2Fexport.zip"), url);
    }
}
