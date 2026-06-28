package app.appgrove.core.catalog;

import app.appgrove.core.catalog.PricingDefinition.AppDef;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Carica il <b>pricing-as-code</b> dagli YAML in {@code resources/pricing/}: un file per app
 * ({@code <slug>.yaml}) elencato in {@code pricing/index.yaml}. L'indice è il registro esplicito delle app
 * finché l'<b>auto-discovery multi-servizio</b> (UC 0046) non lo industrializza.
 *
 * <p>Le app <b>reali</b> (vanno in prod e su Paddle) stanno in {@code pricing/}; le app <b>fixture</b>
 * (catalogo sintetico per dev/test/E2E) stanno in {@code pricing/fixtures/} e si caricano <b>solo</b> con
 * {@code appgrove.pricing.include-fixtures=true} (true in {@code %dev}/{@code %test}, false in prod) → le
 * fixture non vengono <b>mai</b> sincronizzate sul vero Paddle.
 *
 * <p>Riusa l'{@link ObjectMapper} configurato da Quarkus (moduli inclusi: binding dei record per nome
 * parametro), ricreato su {@link YAMLFactory} → coerenza col resto del servizio.
 */
@ApplicationScoped
public class PricingCatalogLoader {

    static final String INDEX_RESOURCE = "pricing/index.yaml";
    static final String FIXTURES_INDEX_RESOURCE = "pricing/fixtures/index.yaml";

    @Inject
    ObjectMapper json;

    @ConfigProperty(name = "appgrove.pricing.include-fixtures", defaultValue = "false")
    boolean includeFixtures;

    private ObjectMapper yaml;

    @PostConstruct
    void init() {
        yaml = json.copyWith(new YAMLFactory());
    }

    /** Indice delle app: {@code apps: [notes, teams, …]}. */
    record Index(List<String> apps) {}

    /** Le app reali (sempre) + le fixture (solo se {@code include-fixtures}), nell'ordine dichiarato. */
    public List<AppDef> load() {
        List<AppDef> defs = new ArrayList<>(loadFrom(INDEX_RESOURCE, "pricing/"));
        if (includeFixtures) {
            defs.addAll(loadFrom(FIXTURES_INDEX_RESOURCE, "pricing/fixtures/"));
        }
        return defs;
    }

    private List<AppDef> loadFrom(String indexResource, String dir) {
        Index index = read(indexResource, Index.class);
        if (index == null || index.apps() == null) {
            throw new IllegalStateException(indexResource + " mancante o senza 'apps'");
        }
        List<AppDef> defs = new ArrayList<>(index.apps().size());
        for (String slug : index.apps()) {
            defs.add(read(dir + slug + ".yaml", AppDef.class));
        }
        return defs;
    }

    private <T> T read(String resource, Class<T> type) {
        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("risorsa pricing-as-code mancante: " + resource);
            }
            return yaml.readValue(in, type);
        } catch (IOException e) {
            throw new UncheckedIOException("lettura pricing-as-code fallita: " + resource, e);
        }
    }
}
