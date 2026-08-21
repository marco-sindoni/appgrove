package app.appgrove.commons.entitlement;

import jakarta.inject.Qualifier;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Qualifica l'attuazione che legge da core <b>sincrono</b>, usata come <b>rete di sicurezza</b> quando la
 * copia locale del servizio non basta a decidere (riga assente, scaduta o da rinfrescare). Vale per i
 * diritti d'accesso ({@link RestEntitlementService}, UC 0046) e per il ruolo sull'applicazione
 * ({@code RestAppRoleService}, UC 0099): due domande diverse, la stessa postura, quindi un solo
 * qualificatore — moltiplicarlo per pacchetto significherebbe suggerire che le due posture siano diverse.
 *
 * <p>Il bean {@code @Default} è invece la lettura dalla <b>copia locale</b>
 * ({@code ProjectedEntitlementService}, {@code ProjectedAppRoleService}): il codice di dominio delle app
 * inietta l'interfaccia senza qualificatori e ottiene il percorso disaccoppiato, senza modifiche. Questo
 * qualificatore esiste per rendere la rete di sicurezza <b>esplicita</b>: chi la inietta sta dichiarando
 * di volere una chiamata di rete sul percorso caldo.
 */
@Qualifier
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
public @interface SafetyNet {}
