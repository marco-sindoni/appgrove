package app.appgrove.commons.privacy;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URISyntaxException;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Test del check annotazioni ↔ manifesto (UC 0030 §9) su fixture dedicate. */
class PersonalDataManifestVerifierTest {

    private static final String FIXTURES_PKG = "app.appgrove.commons.privacy.fixtures";

    @Test
    void manifestoAllineatoPassa() {
        assertDoesNotThrow(() -> PersonalDataManifestVerifier.verify(fixture("fixture-ok.yaml"), FIXTURES_PKG));
    }

    @Test
    void campoAnnotatoNonDichiaratoFallisce() {
        AssertionError error = assertThrows(
                AssertionError.class,
                () -> PersonalDataManifestVerifier.verify(fixture("fixture-missing-entry.yaml"), FIXTURES_PKG));
        assertTrue(error.getMessage().contains("NON dichiarato"), error.getMessage());
        assertTrue(error.getMessage().contains("FixtureCustomer#email"), error.getMessage());
    }

    @Test
    void voceStantiaSenzaCampoAnnotatoFallisce() {
        AssertionError error = assertThrows(
                AssertionError.class,
                () -> PersonalDataManifestVerifier.verify(fixture("fixture-stale-entry.yaml"), FIXTURES_PKG));
        assertTrue(error.getMessage().contains("stantia"), error.getMessage());
        assertTrue(error.getMessage().contains("FixtureCustomer#notes"), error.getMessage());
    }

    private static Path fixture(String name) throws URISyntaxException {
        return Path.of(PersonalDataManifestVerifierTest.class
                .getClassLoader()
                .getResource("manifests/" + name)
                .toURI());
    }
}
