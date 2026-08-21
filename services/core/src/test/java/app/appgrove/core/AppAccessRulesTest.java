package app.appgrove.core;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.appgrove.core.platform.AppAccessRules;
import app.appgrove.core.platform.AppRole;
import app.appgrove.core.platform.MembershipRole;
import org.junit.jupiter.api.Test;

/**
 * La regola di chi-può-cosa come <b>funzione pura</b> (UC 0098 §9): è il collaudo più importante della
 * storia, perché è il punto in cui la regola resta una sola invece di essere ripetuta in ogni
 * operazione. Nessuna banca dati, nessun contesto di richiesta: solo gli ingredienti e il verdetto.
 */
class AppAccessRulesTest {

    // ── scrittura ────────────────────────────────────────────────────────────

    @Test
    void ownerManagesEveryApplicationEvenWithoutAnyAccessRow() {
        // L'accesso dell'owner è implicito: non ha righe, e non gli servono.
        assertTrue(AppAccessRules.canManage(MembershipRole.owner, null));
        assertTrue(AppAccessRules.canManage(MembershipRole.owner, AppRole.viewer));
    }

    @Test
    void appAdminManagesTheApplicationItIsAdminOf() {
        assertTrue(AppAccessRules.canManage(MembershipRole.member, AppRole.admin));
    }

    @Test
    void appAdminOfAnotherApplicationDoesNotManageThisOne() {
        // Su un'altra applicazione il suo ruolo è ASSENTE: è così che il potere resta circoscritto.
        assertFalse(AppAccessRules.canManage(MembershipRole.member, null));
    }

    @Test
    void editorAndViewerDoNotWrite() {
        assertFalse(AppAccessRules.canManage(MembershipRole.member, AppRole.editor));
        assertFalse(AppAccessRules.canManage(MembershipRole.member, AppRole.viewer));
    }

    // ── lettura ──────────────────────────────────────────────────────────────

    @Test
    void everyoneWithAccessReadsWhoHasAccessIncludingViewer() {
        assertTrue(AppAccessRules.canRead(MembershipRole.owner, null));
        assertTrue(AppAccessRules.canRead(MembershipRole.member, AppRole.admin));
        assertTrue(AppAccessRules.canRead(MembershipRole.member, AppRole.editor));
        assertTrue(AppAccessRules.canRead(MembershipRole.member, AppRole.viewer));
    }

    @Test
    void withoutAccessTheApplicationsPeopleAreNotVisible() {
        assertFalse(AppAccessRules.canRead(MembershipRole.member, null));
    }

    // ── ordinamento dei ruoli di applicazione ────────────────────────────────

    @Test
    void appRolesAreOrderedFromViewerToAdmin() {
        assertTrue(AppRole.admin.atLeast(AppRole.editor));
        assertTrue(AppRole.admin.atLeast(AppRole.admin));
        assertTrue(AppRole.editor.atLeast(AppRole.viewer));
        assertFalse(AppRole.editor.atLeast(AppRole.admin));
        assertFalse(AppRole.viewer.atLeast(AppRole.editor));
    }
}
