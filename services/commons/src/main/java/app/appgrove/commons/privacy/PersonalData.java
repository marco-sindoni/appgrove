package app.appgrove.commons.privacy;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca un campo di entità come <b>dato personale</b> (#13 C/L). Dichiara la classificazione
 * (categoria / finalità / base giuridica / retention) che alimenta il manifesto dati e la RoPA.
 * <p>L'enforcement bloccante ("ogni campo personale deve essere dichiarato" + copertura export/erasure)
 * è industrializzato da <b>UC 0030/0031</b>; qui l'annotazione è la sorgente di verità per-app.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface PersonalData {

    /** Categoria del dato (es. "identità cliente", "contatto"). */
    String category();

    /** Finalità del trattamento (es. "emissione e gestione fatture"). */
    String purpose();

    /** Base giuridica GDPR (default: esecuzione del contratto). */
    String legalBasis() default "contratto";

    /** Politica di conservazione (es. "10 anni dall'emissione" per obblighi fiscali). */
    String retention() default "";
}
