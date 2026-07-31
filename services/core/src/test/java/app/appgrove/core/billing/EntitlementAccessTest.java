package app.appgrove.core.billing;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.appgrove.core.catalog.AppStatus;
import app.appgrove.core.platform.AccountStatus;
import org.junit.jupiter.api.Test;

/**
 * Regola unica di accesso (UC 0077): il punto in cui backoffice, matrice admin e polling post-checkout
 * si accordano. Test puro — nessun contesto Quarkus, nessun database: qui si verifica la <b>decisione</b>,
 * non la sua raccolta di ingredienti.
 */
class EntitlementAccessTest {

    @Test
    void subscriptionCheConcedeAccessoSuAppAttivaDaAccesso() {
        assertTrue(EntitlementAccess.granted(
                AccountStatus.active, AppStatus.active, SubscriptionStatus.active, false));
        // dunning/grace: past_due concede ancora accesso (UC 0026, #09 E29)
        assertTrue(EntitlementAccess.granted(
                AccountStatus.active, AppStatus.active, SubscriptionStatus.past_due, false));
    }

    @Test
    void subscriptionCheNonConcedeAccessoNonDaAccessoNeanchePerLaBaseline() {
        // canceled/paused: la subscription presente SOVRASCRIVE la baseline free, non vi ricade.
        assertFalse(EntitlementAccess.granted(
                AccountStatus.active, AppStatus.active, SubscriptionStatus.canceled, true));
        assertFalse(EntitlementAccess.granted(
                AccountStatus.active, AppStatus.active, SubscriptionStatus.paused, true));
    }

    @Test
    void senzaSubscriptionDecideIlTierFreeDiBaseline() {
        assertTrue(EntitlementAccess.granted(AccountStatus.active, AppStatus.active, null, true));
        assertFalse(EntitlementAccess.granted(AccountStatus.active, AppStatus.active, null, false));
    }

    @Test
    void appDisabilitataEsclude() {
        // gate 2 (UC 0076): app inactive → nessun accesso, nemmeno con subscription valida.
        assertFalse(EntitlementAccess.granted(
                AccountStatus.active, AppStatus.inactive, SubscriptionStatus.active, false));
        assertFalse(EntitlementAccess.granted(AccountStatus.active, AppStatus.inactive, null, true));
    }

    @Test
    void accountInAttesaDiEliminazioneAzzeraTutto() {
        // UC 0033, #13 E25: disattivazione immediata, prima di ogni altro gate.
        assertFalse(EntitlementAccess.granted(
                AccountStatus.pending_deletion, AppStatus.active, SubscriptionStatus.active, true));
        assertFalse(EntitlementAccess.granted(AccountStatus.pending_deletion, AppStatus.active, null, true));
    }

    @Test
    void accountSconosciutoNonBlocca() {
        // Stato ignoto (account non trovato): non è un motivo di diniego — senza account non ci sono
        // comunque subscription, e la baseline free resta quella del catalogo.
        assertTrue(EntitlementAccess.granted(null, AppStatus.active, null, true));
        assertTrue(EntitlementAccess.granted(null, AppStatus.active, SubscriptionStatus.active, false));
    }
}
