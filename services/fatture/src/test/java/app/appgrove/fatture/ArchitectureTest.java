package app.appgrove.fatture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;

import app.appgrove.commons.persistence.BaseEntity;
import app.appgrove.commons.persistence.BaseTenantEntity;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import jakarta.persistence.Entity;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;

/** Guardie statiche degli invarianti: base entity ereditata e nessuna mutazione del tenant. */
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

    @Test
    void nessunConfigPropertyConDefaultValueVuoto() {
        classes()
                .should(nonUsareConfigPropertyConDefaultVuoto())
                .because("defaultValue=\"\" fa fallire l'avvio (SRCFG00014) nei profili dove il bean "
                        + "è attivo: usare Optional<String> + orElse(\"\") — regressione change 0036")
                .check(CLASSES);
    }

    /** Controlla campi e parametri (costruttori/metodi) annotati {@code @ConfigProperty}. */
    private static ArchCondition<JavaClass> nonUsareConfigPropertyConDefaultVuoto() {
        return new ArchCondition<>("non usare @ConfigProperty(defaultValue = \"\")") {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                javaClass.getFields().forEach(field ->
                        field.tryGetAnnotationOfType(ConfigProperty.class).ifPresent(cp -> {
                            if (cp.defaultValue().isEmpty()) {
                                events.add(SimpleConditionEvent.violated(field,
                                        field.getFullName() + " usa @ConfigProperty(defaultValue = \"\")"));
                            }
                        }));
                javaClass.getCodeUnits().forEach(codeUnit ->
                        codeUnit.getParameters().forEach(parameter ->
                                parameter.tryGetAnnotationOfType(ConfigProperty.class)
                                        .ifPresent(cp -> {
                                            if (cp.defaultValue().isEmpty()) {
                                                events.add(SimpleConditionEvent.violated(codeUnit,
                                                        codeUnit.getFullName()
                                                                + " ha un parametro @ConfigProperty(defaultValue = \"\")"));
                                            }
                                        })));
            }
        };
    }
}
