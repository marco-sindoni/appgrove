package app.appgrove.commons.entitlement;

import jakarta.ws.rs.NameBinding;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca un endpoint (o un'intera risorsa) come <b>soggetto al gate entitlement (402)</b>: prima di
 * eseguirlo, {@link EntitlementGateFilter} verifica che il tenant del JWT abbia accesso all'app
 * corrente, altrimenti risponde <b>402</b> (#09 dec.30 gate 3, difesa in profondità lato servizio).
 *
 * <p><b>Gate opt-in di proposito.</b> Solo gli endpoint annotati passano dal gate: gli endpoint
 * <b>non</b> annotati (es. i diritti GDPR export/erasure, #09 F31, o lo stato di quota informativo)
 * restano raggiungibili con solo authN+ownership anche senza accesso/subscription. L'esenzione GDPR è
 * così garantita <b>per costruzione</b>, senza liste di esclusione.
 */
@NameBinding
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresEntitlement {}
