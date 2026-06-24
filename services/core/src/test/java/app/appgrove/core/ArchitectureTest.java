package app.appgrove.core;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;

import app.appgrove.commons.persistence.BaseEntity;
import app.appgrove.commons.persistence.BaseTenantEntity;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import jakarta.persistence.Entity;
import org.junit.jupiter.api.Test;

/** Guardie statiche degli invarianti (UC 0012 §8): uso della base entity e nessuna mutazione del tenant. */
class ArchitectureTest {

    private static final JavaClasses CLASSES =
            new ClassFileImporter().importPackages("app.appgrove");

    @Test
    void entitiesExtendBaseEntity() {
        classes()
                .that().areAnnotatedWith(Entity.class)
                .should().beAssignableTo(BaseEntity.class)
                .because("ogni entity deve ereditare UUID v7 + audit + soft-delete")
                .check(CLASSES);
    }

    @Test
    void tenantScopedEntitiesDoNotExposeTenantSetter() {
        noMethods()
                .that().areDeclaredInClassesThat().areAssignableTo(BaseTenantEntity.class)
                .should().haveNameMatching("setTenant.*")
                .because("tenant_id è gestito dal discriminator (JWT), non mutabile dall'applicazione")
                .check(CLASSES);
    }
}
