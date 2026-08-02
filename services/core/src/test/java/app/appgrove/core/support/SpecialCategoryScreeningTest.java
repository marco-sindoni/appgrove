package app.appgrove.core.support;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Riconoscimento delle categorie particolari (art. 9) nel testo di un ticket — UC 0075 §9.
 *
 * <p>Il riconoscitore ammette volentieri i falsi positivi (segnalare in più costa una lettura), ma
 * ci sono due difetti che deve escludere: <b>mancare</b> un testo palesemente delicato, e
 * <b>segnalare</b> una richiesta ordinaria solo perché una parola-spia compare dentro un'altra
 * parola ("terrazza" contiene "razza").
 */
class SpecialCategoryScreeningTest {

    @ParameterizedTest
    @ValueSource(strings = {
        "Vi ho mandato il referto della mia malattia",
        "Sono in terapia e vorrei cancellare i dati",
        "I attached my medical record by mistake",
        "Riguarda la mia appartenenza sindacale",
        "This concerns my religious beliefs",
        "Ho caricato l'impronta digitale per sbaglio",
        "Please delete data about my sexual orientation",
        "Si tratta della mia origine etnica",
        "Riguarda la MALATTIA di mio figlio",
        "Ho scritto della mia gravidanza"
    })
    void flagsTextTouchingSpecialCategories(String text) {
        assertTrue(SpecialCategoryScreening.flags(text), "doveva essere segnalato: " + text);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "Il totale della fattura di marzo non torna",
        "Non riesco a invitare un collega nello spazio di lavoro",
        "Ho un problema con la terrazza dell'ufficio e la fatturazione",
        "The invoice PDF does not download",
        "Vorrei cambiare il piano di abbonamento",
        "La ricerca nel catalogo non trova nulla"
    })
    void doesNotFlagOrdinaryRequests(String text) {
        assertFalse(SpecialCategoryScreening.flags(text), "non doveva essere segnalato: " + text);
    }

    @Test
    void ignoresNullAndBlankFragmentsAndScansAllOfThem() {
        assertFalse(SpecialCategoryScreening.flags(null, "   ", "tutto a posto"));
        // oggetto ordinario ma messaggio delicato: basta un frammento a far scattare la segnalazione
        assertTrue(SpecialCategoryScreening.flags("Richiesta di cancellazione", null,
                "riguarda dati sulla mia salute"));
    }
}
