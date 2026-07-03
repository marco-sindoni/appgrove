package app.appgrove.fatture;

import app.appgrove.commons.privacy.PersonalDataManifestVerifier;
import org.junit.jupiter.api.Test;

/**
 * Gate compliance (UC 0030, #13 C16): le entità fatture con campi {@code @PersonalData} devono essere
 * dichiarate nel manifesto dell'app ({@code docs/compliance/manifests/fatture.yaml}) e viceversa.
 */
class PersonalDataManifestTest {

    @Test
    void campiPersonalDataAllineatiAlManifestoFatture() {
        PersonalDataManifestVerifier.verify("fatture", "app.appgrove.fatture");
    }
}
