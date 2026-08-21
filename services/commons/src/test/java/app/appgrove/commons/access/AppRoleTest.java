package app.appgrove.commons.access;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * L'ordinamento dei ruoli di applicazione (UC 0099 §9): {@code viewer} &lt; {@code editor} &lt;
 * {@code admin}. È il collaudo più importante della storia, perché l'ordine è la cosa che tutto il resto
 * assume e nessun altro posto ricontrolla: un {@code editor} che passasse per un varco {@code admin}
 * sarebbe un difetto silenzioso e generalizzato a ogni applicazione.
 */
class AppRoleTest {

    @Test
    void everyRoleIsEnoughForItself() {
        for (AppRole role : AppRole.values()) {
            assertTrue(role.atLeast(role), role + " deve bastare per sé stesso");
        }
    }

    @Test
    void theOrderIsViewerThenEditorThenAdmin() {
        assertEquals(List.of(AppRole.viewer, AppRole.editor, AppRole.admin), List.of(AppRole.values()));
    }

    @Test
    void aHigherRoleIsEnoughForALowerRequirement() {
        assertTrue(AppRole.editor.atLeast(AppRole.viewer));
        assertTrue(AppRole.admin.atLeast(AppRole.viewer));
        assertTrue(AppRole.admin.atLeast(AppRole.editor));
    }

    @Test
    void aLowerRoleIsNeverEnoughForAHigherRequirement() {
        assertFalse(AppRole.viewer.atLeast(AppRole.editor));
        assertFalse(AppRole.viewer.atLeast(AppRole.admin));
        assertFalse(AppRole.editor.atLeast(AppRole.admin));
    }

    /**
     * L'owner dell'account sta sopra tutti perché la fonte di verità gli attribuisce {@code admin}, non
     * perché esista un quarto valore: il varco non deve conoscere il concetto di owner.
     */
    @Test
    void theOwnerIsRepresentedAsAdminAndSitsAboveEveryone() {
        AppRole owner = AppRole.admin;
        for (AppRole required : AppRole.values()) {
            assertTrue(owner.atLeast(required), "l'owner deve passare il varco " + required);
        }
    }

    @Test
    void anUnknownOrMissingValueIsNeverAPermission() {
        assertEquals(Optional.empty(), AppRole.parse(null));
        assertEquals(Optional.empty(), AppRole.parse(""));
        assertEquals(Optional.empty(), AppRole.parse("  "));
        // Valore ritirato dal ruolo di PIATTAFORMA: non è un ruolo di applicazione e non deve diventarlo
        // per somiglianza del nome.
        assertEquals(Optional.empty(), AppRole.parse("owner"));
        assertEquals(Optional.empty(), AppRole.parse("ADMIN"));
        assertEquals(Optional.of(AppRole.admin), AppRole.parse("admin"));
    }
}
