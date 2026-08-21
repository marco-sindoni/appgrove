package app.appgrove.commons.access;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.appgrove.commons.access.projection.AppRoleProjectionStore.ProjectedAppRole;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/**
 * La <b>scadenza</b> della copia locale del ruolo (UC 0099 §9). È la differenza voluta rispetto alla copia
 * dei diritti d'accesso, che non scade: qui la durata massima è la rete che tiene quando il canale degli
 * eventi è rotto — l'unico caso in cui l'invalidazione, da sola, non protegge nulla e un permesso revocato
 * sopravviverebbe.
 */
class AppRoleProjectionExpiryTest {

    private static final Duration MAX_AGE = Duration.ofSeconds(60);
    private static final Instant NOW = Instant.parse("2026-08-21T10:00:00Z");

    private static ProjectedAppRole row(boolean stale, Duration age) {
        return new ProjectedAppRole(AppRole.editor, stale, NOW.minus(age));
    }

    @Test
    void aFreshRowIsUsedWithoutTouchingTheNetwork() {
        assertTrue(row(false, Duration.ofSeconds(1)).usable(MAX_AGE, NOW));
        assertTrue(row(false, Duration.ZERO).usable(MAX_AGE, NOW));
    }

    @Test
    void anInvalidatedRowIsNeverUsableHoweverRecent() {
        assertFalse(row(true, Duration.ZERO).usable(MAX_AGE, NOW));
    }

    @Test
    void aRowOlderThanTheMaxAgeIsRefreshedEvenWithoutAnyEvent() {
        assertFalse(row(false, Duration.ofSeconds(61)).usable(MAX_AGE, NOW));
    }

    @Test
    void theBoundaryItselfIsStillUsable() {
        // Esattamente alla durata massima la riga vale ancora: la soglia è "più vecchia di", non "vecchia
        // quanto". Fissarlo in un collaudo evita che l'inclusione dell'estremo cambi per distrazione.
        assertTrue(row(false, MAX_AGE).usable(MAX_AGE, NOW));
        assertFalse(row(false, MAX_AGE.plusMillis(1)).usable(MAX_AGE, NOW));
    }

    @Test
    void aZeroOrAbsentMaxAgeDisablesTheCopyInsteadOfMakingItEternal() {
        // Un valore di configurazione degenere deve togliere la copia dal percorso, non renderla
        // definitiva: in assenza di una durata sensata si preferisce chiedere al core.
        assertFalse(row(false, Duration.ZERO).usable(Duration.ZERO, NOW));
        assertFalse(row(false, Duration.ZERO).usable(null, NOW));
        assertFalse(row(false, Duration.ZERO).usable(Duration.ofSeconds(-1), NOW));
    }

    @Test
    void aKnownDenialIsAnAnswerAndExpiresLikeAnyOther() {
        ProjectedAppRole denial = new ProjectedAppRole(null, false, NOW.minusSeconds(10));
        assertTrue(denial.usable(MAX_AGE, NOW), "il diniego noto risparmia una chiamata di rete");
        ProjectedAppRole old = new ProjectedAppRole(null, false, NOW.minusSeconds(120));
        assertFalse(old.usable(MAX_AGE, NOW), "ma scade come tutto il resto: un accesso appena concesso deve arrivare");
    }
}
