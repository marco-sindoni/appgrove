package app.appgrove.commons.membership;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

/**
 * Il ruolo di piattaforma come finisce nel claim del token (UC 0099). <b>Gemello</b> di
 * {@code _claim_role} in {@code infra/modules/platform_shared/lambda/pre_token_gen/handler.py}: la stessa
 * tabella di casi è eseguita anche da {@code test_handler.py}. Se una delle due attuazioni cambia, l'altra
 * cambia con essa — altrimenti i collaudi locali dicono una cosa e l'ambiente reale un'altra.
 */
class PlatformRolesTest {

    @Test
    void theTwoLivingValuesPassUnchanged() {
        assertEquals("owner", PlatformRoles.claimRole("owner"));
        assertEquals("member", PlatformRoles.claimRole("member"));
    }

    /**
     * Il valore ritirato da UC 0098 diventa {@code member}: una persona di un ambiente non ancora
     * convertito accede col potere MINORE, non con nessun potere — il valore vecchio significava «membro
     * con poteri in più», non «persona sconosciuta». La tolleranza la ritira UC 0113.
     */
    @Test
    void theRetiredAdminValueBecomesMember() {
        assertEquals("member", PlatformRoles.claimRole("admin"));
    }

    @Test
    void theConversionIsExactAndNotBySimilarity() {
        // Solo il valore esatto: un ruolo sconosciuto non va tradotto in un permesso per assonanza.
        assertEquals("Admin", PlatformRoles.claimRole("Admin"));
        assertEquals("administrator", PlatformRoles.claimRole("administrator"));
    }

    @Test
    void anAbsentRoleStaysAbsent() {
        // Decidere se un token si possa emettere non è compito di questa funzione: a chiusura in caso di
        // dubbio, quella decisione è di chi compone il token.
        assertNull(PlatformRoles.claimRole(null));
    }
}
