package app.appgrove.commons.privacy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import jakarta.persistence.Entity;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Check "stile ArchUnit" annotazioni ↔ manifesto dati (UC 0030, #13 C16): incrocia i campi
 * {@link PersonalData} delle entità JPA del servizio con le voci <em>entity-backed</em>
 * (chiavi {@code entity}+{@code field}) del manifesto YAML in {@code docs/compliance/manifests/}.
 * Bloccante nelle due direzioni:
 * <ul>
 *   <li>campo annotato {@code @PersonalData} senza voce nel manifesto → build rossa;</li>
 *   <li>voce entity-backed che punta a classe/campo inesistente o non annotato → build rossa
 *       (il RoPA non deve restare stantio).</li>
 * </ul>
 * Le voci senza {@code entity} (es. Cognito, log) sono fuori dal check Java; la parità lingue e
 * l'assemblaggio RoPA sono di {@code tools/compliance} (area {@code compliance} di run-tests.sh).
 * <p>Come in {@code FattureDataContract}, si considerano i soli campi dichiarati dall'entità
 * (le basi comuni {@code BaseEntity}/{@code BaseTenantEntity} non portano dati personali).
 */
public final class PersonalDataManifestVerifier {

    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());

    private PersonalDataManifestVerifier() {}

    /**
     * Verifica il manifesto {@code docs/compliance/manifests/<manifestId>.yaml} (risolto risalendo
     * alla root del repo) contro le entità dei package indicati. Fallisce con {@link AssertionError}.
     */
    public static void verify(String manifestId, String... packages) {
        verify(manifestFile(manifestId), packages);
    }

    /** Variante testabile: manifesto da un path esplicito. */
    public static void verify(Path manifestFile, String... packages) {
        Set<String> declared = entityBackedEntries(manifestFile);
        List<String> violations = new ArrayList<>();

        Set<String> annotated = new HashSet<>();
        for (JavaClass javaClass : new ClassFileImporter().importPackages(packages)) {
            if (!javaClass.isAnnotatedWith(Entity.class)) {
                continue;
            }
            Class<?> entity = javaClass.reflect();
            for (Field field : entity.getDeclaredFields()) {
                if (field.getAnnotation(PersonalData.class) == null) {
                    continue;
                }
                String key = entity.getName() + "#" + field.getName();
                annotated.add(key);
                if (!declared.contains(key)) {
                    violations.add("campo @PersonalData NON dichiarato nel manifesto " + manifestFile.getFileName()
                            + ": " + key);
                }
            }
        }

        for (String entry : declared) {
            if (!annotated.contains(entry) && !resolvesToAnnotatedField(entry)) {
                violations.add("voce entity-backed del manifesto " + manifestFile.getFileName()
                        + " senza campo @PersonalData corrispondente (stantia?): " + entry);
            }
        }

        if (!violations.isEmpty()) {
            throw new AssertionError(
                    "Manifesto dati e annotazioni @PersonalData NON allineati (#13 C16, UC 0030):\n  - "
                            + String.join("\n  - ", violations));
        }
    }

    /** Voci con `entity`+`field` del manifesto, come chiavi {@code fqcn#field}. */
    private static Set<String> entityBackedEntries(Path manifestFile) {
        JsonNode manifest;
        try {
            manifest = YAML.readTree(Files.newBufferedReader(manifestFile));
        } catch (IOException e) {
            throw new UncheckedIOException("manifesto non leggibile: " + manifestFile, e);
        }
        Set<String> entries = new HashSet<>();
        for (JsonNode entry : manifest.path("entries")) {
            if (entry.hasNonNull("entity") && entry.hasNonNull("field")) {
                entries.add(entry.get("entity").asText() + "#" + entry.get("field").asText());
            }
        }
        return entries;
    }

    /** Fallback per voci fuori dai package scanditi: la classe/campo deve comunque esistere ed essere annotato. */
    private static boolean resolvesToAnnotatedField(String entry) {
        String[] parts = entry.split("#", 2);
        try {
            Field field = Class.forName(parts[0]).getDeclaredField(parts[1]);
            return field.getAnnotation(PersonalData.class) != null;
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }

    /** Risale da {@code user.dir} fino alla root del monorepo (dove esiste docs/compliance/manifests). */
    private static Path manifestFile(String manifestId) {
        Path dir = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while (dir != null) {
            Path manifests = dir.resolve("docs/compliance/manifests");
            if (Files.isDirectory(manifests)) {
                return manifests.resolve(manifestId + ".yaml");
            }
            dir = dir.getParent();
        }
        throw new IllegalStateException("docs/compliance/manifests non trovata risalendo da " + System.getProperty("user.dir"));
    }
}
