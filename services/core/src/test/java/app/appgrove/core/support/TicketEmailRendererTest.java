package app.appgrove.core.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.appgrove.commons.email.EmailTemplateRenderer;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Email del ticketing (UC 0075) rese davvero dal servizio: i quattro messaggi esistono in entrambe
 * le lingue, sono nell'artefatto del core (copia da {@code shared/email-templates} configurata nel
 * {@code pom.xml}) e non lasciano segnaposto irrisolti.
 *
 * <p>Il comportamento della resa (sostituzioni, escape, guardia sui segnaposto, ripiego di lingua)
 * è coperto una volta sola dove vive il codice, in {@code services/commons}.
 */
@QuarkusTest
class TicketEmailRendererTest {

    private static final String URL = "https://app.local.appgrove.app/support";

    @Inject
    TicketEmailRenderer renderer;

    @Test
    void italianRequesterGetsItalianCopyWithTheSubjectOfTheirRequest() {
        EmailTemplateRenderer.Rendered r = renderer.render("it", TicketEmailRenderer.OPENED,
                Map.of("ticketSubject", "Fattura sbagliata", "actionUrl", URL));
        assertEquals("Abbiamo ricevuto la tua richiesta: Fattura sbagliata", r.subject());
        assertTrue(r.text().contains("Fattura sbagliata"), "il corpo deve riportare l'oggetto");
        assertTrue(r.text().contains(URL), "il corpo deve portare alla pagina Supporto");
    }

    /** L'istanza privacy dice il termine di legge e la data: è la prova che il cliente è informato. */
    @Test
    void privacyConfirmationStatesTheOneMonthDeadline() {
        EmailTemplateRenderer.Rendered it = renderer.render("it", TicketEmailRenderer.OPENED_PRIVACY,
                Map.of("ticketSubject", "Voglio i miei dati", "dueDate", "01/09/2026", "actionUrl", URL));
        assertTrue(it.text().contains("un mese"), "il termine di legge dev'essere esplicito");
        assertTrue(it.text().contains("01/09/2026"), "la data del termine dev'essere nel testo");

        EmailTemplateRenderer.Rendered en = renderer.render("en", TicketEmailRenderer.OPENED_PRIVACY,
                Map.of("ticketSubject", "My data please", "dueDate", "01/09/2026", "actionUrl", URL));
        assertTrue(en.text().contains("within one month"), "stessa promessa in inglese: " + en.text());
    }

    /**
     * Minimizzazione (UC 0075): l'avviso di aggiornamento porta alla pagina, non il contenuto della
     * conversazione — una richiesta delicata non deve finire in chiaro in una casella di posta.
     */
    @Test
    void updateNoticeCarriesNoThreadContent() {
        EmailTemplateRenderer.Rendered r = renderer.render("it", TicketEmailRenderer.UPDATED,
                Map.of("ticketSubject", "Istanza", "actionUrl", URL));
        assertTrue(r.text().contains("non viaggia mai per email"),
                "il testo deve dire che il contenuto resta nello spazio di lavoro: " + r.text());
    }

    @Test
    void everyTicketMessageIsAvailableInBothLanguages() {
        Map<String, String> values = Map.of(
                "ticketSubject", "Oggetto", "ticketId", "abc", "tenantId", "t1",
                "ticketType", "privacy", "ticketStatus", "open", "dueDate", "01/09/2026",
                "actionUrl", URL);
        for (String locale : new String[] {"it", "en"}) {
            for (String key : new String[] {
                TicketEmailRenderer.OPENED, TicketEmailRenderer.OPENED_PRIVACY,
                TicketEmailRenderer.UPDATED, TicketEmailRenderer.INBOX
            }) {
                EmailTemplateRenderer.Rendered r = renderer.render(locale, key, values);
                assertFalse(r.subject().isBlank(), "oggetto presente per " + key + " (" + locale + ")");
                assertFalse(r.text().isBlank(), "testo presente per " + key + " (" + locale + ")");
                assertFalse(r.html().isBlank(), "versione grafica presente per " + key + " (" + locale + ")");
            }
        }
    }
}
