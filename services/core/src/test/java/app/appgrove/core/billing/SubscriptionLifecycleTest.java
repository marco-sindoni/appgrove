package app.appgrove.core.billing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.appgrove.core.billing.SubscriptionLifecycle.Phase;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * L1 puro della semantica di ciclo di vita (UC 0026): mappa status→accesso (#09 E29) e derivazione
 * {@link SubscriptionLifecycle}. Nessun DB — è logica di dominio. Verifica che {@code access} e
 * {@code phase} siano coerenti per costruzione e che {@code accessUntil} segua le regole (cancel =
 * fino a {@code cancel_at}; altrimenti fine periodo; null se ENDED).
 */
class SubscriptionLifecycleTest {

    private static final Instant PERIOD_END = Instant.parse("2026-07-01T00:00:00Z");
    private static final Instant CANCEL_AT = Instant.parse("2026-06-20T00:00:00Z");

    // ── Mappa status→accesso: unica fonte di verità (gate 3) ────────────────────
    @ParameterizedTest
    @CsvSource({
        "trialing,true",
        "active,true",
        "past_due,true", // dunning/grace: accesso mantenuto
        "canceled,false",
        "paused,false"
    })
    void grantsAccessMapIsCanonical(SubscriptionStatus status, boolean expected) {
        assertEquals(expected, status.grantsAccess());
    }

    @Test
    void trialingGrantsAccessUntilPeriodEnd() {
        SubscriptionLifecycle lc = SubscriptionLifecycle.of(SubscriptionStatus.trialing, null, PERIOD_END);
        assertEquals(Phase.TRIAL, lc.phase());
        assertTrue(lc.access());
        assertEquals(PERIOD_END, lc.accessUntil());
    }

    @Test
    void activeWithoutCancelIsActiveUntilPeriodEnd() {
        SubscriptionLifecycle lc = SubscriptionLifecycle.of(SubscriptionStatus.active, null, PERIOD_END);
        assertEquals(Phase.ACTIVE, lc.phase());
        assertTrue(lc.access());
        assertEquals(PERIOD_END, lc.accessUntil());
    }

    @Test
    void activeWithCancelAtIsCancelingWithAccessUntilCancelAt() {
        // Disdetta = accesso fino a fine periodo (status resta active finché il periodo non scade, #09 E34).
        SubscriptionLifecycle lc = SubscriptionLifecycle.of(SubscriptionStatus.active, CANCEL_AT, PERIOD_END);
        assertEquals(Phase.CANCELING, lc.phase());
        assertTrue(lc.access());
        assertEquals(CANCEL_AT, lc.accessUntil());
    }

    @Test
    void clearingCancelAtReturnsToActive() {
        // Riattivazione prima della scadenza: azzerato cancel_at → torna ACTIVE (#09 E25).
        SubscriptionLifecycle lc = SubscriptionLifecycle.of(SubscriptionStatus.active, null, PERIOD_END);
        assertEquals(Phase.ACTIVE, lc.phase());
    }

    @Test
    void pastDueIsGraceWithAccess() {
        SubscriptionLifecycle lc = SubscriptionLifecycle.of(SubscriptionStatus.past_due, null, PERIOD_END);
        assertEquals(Phase.GRACE, lc.phase());
        assertTrue(lc.access());
        assertEquals(PERIOD_END, lc.accessUntil());
    }

    @Test
    void canceledIsEndedNoAccess() {
        // Anche con cancel_at presente: status canceled = periodo già scaduto → nessun accesso.
        SubscriptionLifecycle lc = SubscriptionLifecycle.of(SubscriptionStatus.canceled, CANCEL_AT, PERIOD_END);
        assertEquals(Phase.ENDED, lc.phase());
        assertFalse(lc.access());
        assertNull(lc.accessUntil());
    }

    @Test
    void pausedIsEndedNoAccess() {
        SubscriptionLifecycle lc = SubscriptionLifecycle.of(SubscriptionStatus.paused, null, PERIOD_END);
        assertEquals(Phase.ENDED, lc.phase());
        assertFalse(lc.access());
        assertNull(lc.accessUntil());
    }

    @Test
    void accessAlwaysMatchesCanonicalMap() {
        // invariante di coerenza: access del lifecycle == mappa canonica, per ogni stato.
        for (SubscriptionStatus s : SubscriptionStatus.values()) {
            assertEquals(
                    s.grantsAccess(),
                    SubscriptionLifecycle.of(s, null, PERIOD_END).access(),
                    "incoerenza access/grantsAccess per " + s);
        }
    }
}
