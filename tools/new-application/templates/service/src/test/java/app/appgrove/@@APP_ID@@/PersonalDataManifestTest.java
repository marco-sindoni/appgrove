package app.appgrove.@@APP_ID@@;

import app.appgrove.commons.privacy.PersonalDataManifestVerifier;
import org.junit.jupiter.api.Test;

/**
 * Gate compliance (UC 0030, #13 C16): le entità dell'app con campi {@code @PersonalData} devono
 * essere dichiarate nel manifesto dati ({@code docs/compliance/manifests/@@APP_ID@@.yaml}) e
 * viceversa. Bloccante nelle due direzioni: un campo personale non dichiarato rende la build rossa,
 * e una voce di manifesto senza campo corrispondente pure (il registro dei trattamenti non deve
 * restare stantio).
 */
class PersonalDataManifestTest {

    @Test
    void campiPersonalDataAllineatiAlManifesto() {
        PersonalDataManifestVerifier.verify("@@APP_ID@@", "app.appgrove.@@APP_ID@@");
    }
}
