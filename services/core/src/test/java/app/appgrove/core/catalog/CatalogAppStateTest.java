package app.appgrove.core.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;

import app.appgrove.core.billing.SubscriptionStatus;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/**
 * Derivazione dei sei stati della card di catalogo (UC 0095 §9, "unit: derivazione dello stato dalle
 * combinazioni"). Test <b>puro</b>: nessun contenitore, nessun database — la regola è una funzione e va
 * verificata come tale, combinazione per combinazione.
 */
class CatalogAppStateTest {

    private static final Instant CANCEL_AT = Instant.parse("2030-01-01T00:00:00Z");

    @Test
    void appSpentaDallaPiattaformaVinceSuTutto() {
        // È la coerenza che UC 0076 aveva lasciato aperta: con l'app spenta non conta che l'abbonamento
        // sia formalmente attivo, né che una fascia gratuita esista.
        assertEquals(
                CatalogAppState.disabled_by_platform,
                CatalogAppState.derive(AppStatus.inactive, SubscriptionStatus.active, null, false));
        assertEquals(
                CatalogAppState.disabled_by_platform,
                CatalogAppState.derive(AppStatus.inactive, SubscriptionStatus.trialing, null, true));
        assertEquals(
                CatalogAppState.disabled_by_platform,
                CatalogAppState.derive(AppStatus.inactive, null, null, true));
    }

    @Test
    void abbonamentoVivoDettaLoStato() {
        assertEquals(
                CatalogAppState.active,
                CatalogAppState.derive(AppStatus.active, SubscriptionStatus.active, null, true));
        assertEquals(
                CatalogAppState.trial,
                CatalogAppState.derive(AppStatus.active, SubscriptionStatus.trialing, null, true));
        assertEquals(
                CatalogAppState.payment_pending,
                CatalogAppState.derive(AppStatus.active, SubscriptionStatus.past_due, null, true));
        assertEquals(
                CatalogAppState.cancellation_scheduled,
                CatalogAppState.derive(AppStatus.active, SubscriptionStatus.active, CANCEL_AT, true));
    }

    @Test
    void abbonamentoTerminatoRiportaLAppFraLeProposte() {
        // Disdetto o in pausa: nessun accesso (la regola unica ignora la fascia gratuita quando un
        // abbonamento esiste), quindi la card torna acquistabile.
        assertEquals(
                CatalogAppState.available,
                CatalogAppState.derive(AppStatus.active, SubscriptionStatus.canceled, CANCEL_AT, false));
        assertEquals(
                CatalogAppState.available,
                CatalogAppState.derive(AppStatus.active, SubscriptionStatus.paused, null, false));
    }

    @Test
    void senzaAbbonamentoComandaLaRegolaUnicaDiAccesso() {
        // Fascia gratuita che concede accesso: l'app è già in uso (compare nel menu laterale), quindi
        // active — offrire "Subscribe" sarebbe una bugia.
        assertEquals(CatalogAppState.active, CatalogAppState.derive(AppStatus.active, null, null, true));
        // Nessun accesso e nessun abbonamento: è una proposta d'acquisto.
        assertEquals(CatalogAppState.available, CatalogAppState.derive(AppStatus.active, null, null, false));
    }
}
