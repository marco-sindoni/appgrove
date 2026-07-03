package app.appgrove.commons.privacy.fixtures;

import app.appgrove.commons.persistence.BaseEntity;
import app.appgrove.commons.privacy.PersonalData;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;

/**
 * Entità fixture per i test di {@link app.appgrove.commons.privacy.PersonalDataManifestVerifier}:
 * un campo personale annotato e uno neutro. Estende {@link BaseEntity} per restare conforme alle
 * regole ArchUnit dei servizi (il test-jar finisce sul loro classpath di test).
 */
@Entity
public class FixtureCustomer extends BaseEntity {

    @PersonalData(category = "contatto", purpose = "fixture di test")
    @Column
    private String email;

    @Column
    private String notes;
}
