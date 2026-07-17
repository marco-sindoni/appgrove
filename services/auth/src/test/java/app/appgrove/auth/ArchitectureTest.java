package app.appgrove.auth;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;

/**
 * Guardie statiche del servizio auth. La regola sui default vuoti nasce dalla regressione della
 * change 0036 (bug `queue-prefix`, corretto in change 0037): {@code @ConfigProperty(defaultValue =
 * "")} NON supera la validazione config di Quarkus all'avvio ({@code SRCFG00014}) — la stringa
 * vuota equivale a "assente". Per un default "vuoto" si usa {@code Optional<String>} +
 * {@code orElse("")}.
 */
class ArchitectureTest {

    private static final JavaClasses CLASSES =
            new ClassFileImporter().importPackages("app.appgrove");

    @Test
    void nessunConfigPropertyConDefaultValueVuoto() {
        classes()
                .should(nonUsareConfigPropertyConDefaultVuoto())
                .because("defaultValue=\"\" fa fallire l'avvio (SRCFG00014) nei profili dove il bean "
                        + "è attivo: usare Optional<String> + orElse(\"\") — regressione change 0036")
                .check(CLASSES);
    }

    /** Controlla campi e parametri (costruttori/metodi) annotati {@code @ConfigProperty}. */
    static ArchCondition<JavaClass> nonUsareConfigPropertyConDefaultVuoto() {
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
