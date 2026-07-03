package app.appgrove.core;

import app.appgrove.commons.privacy.PersonalDataManifestVerifier;
import org.junit.jupiter.api.Test;

/**
 * Gate compliance (UC 0030, #13 C16): le entità core con campi {@code @PersonalData} devono essere
 * dichiarate nel manifesto piattaforma ({@code docs/compliance/manifests/platform.yaml}) e viceversa.
 */
class PersonalDataManifestTest {

    @Test
    void campiPersonalDataAllineatiAlManifestoPlatform() {
        PersonalDataManifestVerifier.verify("platform", "app.appgrove.core");
    }
}
