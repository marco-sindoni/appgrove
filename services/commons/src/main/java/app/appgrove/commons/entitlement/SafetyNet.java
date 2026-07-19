package app.appgrove.commons.entitlement;

import jakarta.inject.Qualifier;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Qualifica l'implementazione di {@link EntitlementService} usata come <b>rete di sicurezza</b>:
 * la lettura sincrona da core ({@link RestEntitlementService}), invocata solo quando la proiezione
 * locale non basta a decidere (riga assente, o riga da rinfrescare — UC 0046).
 *
 * <p>Il bean {@code @Default} è invece la lettura dalla <b>proiezione locale</b>
 * ({@code ProjectedEntitlementService}): il codice di dominio delle app inietta
 * {@code EntitlementService} senza qualificatori e ottiene il percorso disaccoppiato, senza
 * modifiche. Questo qualificatore esiste per rendere la rete di sicurezza <b>esplicita</b>: chi la
 * inietta sta dichiarando di volere una chiamata di rete sul percorso caldo.
 */
@Qualifier
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
public @interface SafetyNet {}
