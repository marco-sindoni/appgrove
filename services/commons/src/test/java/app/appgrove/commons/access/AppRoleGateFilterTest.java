package app.appgrove.commons.access;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.ws.rs.container.ResourceInfo;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;

/**
 * Il varco del ruolo per applicazione (UC 0099): passa con ruolo sufficiente, rifiuta con ruolo
 * insufficiente <b>nominando</b> quello che serve, rifiuta diversamente chi non ha accesso, e distingue il
 * guasto nostro dal permesso mancante. Sono i tre esiti che tutta la storia esiste per tenere distinti.
 */
class AppRoleGateFilterTest {

    private static final String APP = "crm";

    /** Sorgente del ruolo a verdetto fisso, che conta quante volte è stata interpellata e come. */
    private static final class FixedRole implements AppRoleService {
        private final AppRoleOutcome outcome;
        int cachedCalls;
        int freshCalls;

        FixedRole(AppRoleOutcome outcome) {
            this.outcome = outcome;
        }

        @Override
        public AppRoleOutcome roleOf(String appSlug) {
            cachedCalls++;
            return outcome;
        }

        @Override
        public AppRoleOutcome roleFresh(String appSlug) {
            freshCalls++;
            return outcome;
        }
    }

    /** Risorsa finta: dichiara {@code viewer} sulla classe e alza l'asta sui singoli metodi. */
    @RequiresAppRole(AppRole.viewer)
    static class FakeResource {

        public void read() {}

        @RequiresAppRole(AppRole.editor)
        public void write() {}

        @RequiresAppRole(value = AppRole.admin, fresh = true)
        public void destroy() {}
    }

    /** Risorsa senza alcuna dichiarazione: il caso che non dovrebbe capitare. */
    static class UnannotatedResource {
        public void read() {}
    }

    private static AppRoleGateFilter filter(AppRoleService roles, Class<?> resource, String method) {
        AppRoleGateFilter filter = new AppRoleGateFilter();
        filter.roles = roles;
        filter.appSlug = APP;
        filter.resourceInfo = resourceInfo(resource, method);
        return filter;
    }

    private static ResourceInfo resourceInfo(Class<?> resource, String methodName) {
        Method method;
        try {
            method = resource.getMethod(methodName);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
        return new ResourceInfo() {
            @Override
            public Method getResourceMethod() {
                return method;
            }

            @Override
            public Class<?> getResourceClass() {
                return resource;
            }
        };
    }

    // ── ruolo sufficiente ────────────────────────────────────────────────────

    @Test
    void aSufficientRolePasses() {
        FixedRole roles = new FixedRole(new AppRoleOutcome.Granted(AppRole.editor));
        assertDoesNotThrow(() -> filter(roles, FakeResource.class, "write").filter(null));
    }

    @Test
    void aHigherRoleAlsoPasses() {
        FixedRole roles = new FixedRole(new AppRoleOutcome.Granted(AppRole.admin));
        assertDoesNotThrow(() -> filter(roles, FakeResource.class, "read").filter(null));
    }

    // ── ruolo insufficiente: «non puoi fare QUESTO» ──────────────────────────

    @Test
    void anInsufficientRoleIsRefusedNamingTheRoleRequired() {
        FixedRole roles = new FixedRole(new AppRoleOutcome.Granted(AppRole.viewer));
        AppRoleRequiredException refusal = assertThrows(
                AppRoleRequiredException.class, () -> filter(roles, FakeResource.class, "write").filter(null));
        assertEquals(AppRoleRequiredException.Case.INSUFFICIENT_ROLE, refusal.kind());
        assertEquals(AppRole.editor, refusal.required());
        assertEquals(AppRole.viewer, refusal.held());
        assertTrue(refusal.getMessage().contains("editor"), "il messaggio deve dire quale ruolo serve");
    }

    // ── nessun accesso: «non entri» ──────────────────────────────────────────

    @Test
    void noAccessIsADifferentRefusalFromAnInsufficientRole() {
        FixedRole roles = new FixedRole(new AppRoleOutcome.NoAccess());
        AppRoleRequiredException refusal = assertThrows(
                AppRoleRequiredException.class, () -> filter(roles, FakeResource.class, "read").filter(null));
        assertEquals(AppRoleRequiredException.Case.NO_ACCESS, refusal.kind());
        assertEquals(403, refusal.kind().status());
        assertTrue(
                refusal.getMessage().contains("abilitarti"),
                "chi non ha accesso deve sapere a chi chiedere l'abilitazione");
    }

    // ── non decidibile: guasto nostro, non permesso mancante ─────────────────

    @Test
    void anUndecidableOutcomeIsRefusedAsAFaultAndNotAsAPermission() {
        FixedRole roles = new FixedRole(new AppRoleOutcome.Unavailable());
        AppRoleRequiredException refusal = assertThrows(
                AppRoleRequiredException.class, () -> filter(roles, FakeResource.class, "read").filter(null));
        assertEquals(AppRoleRequiredException.Case.UNAVAILABLE, refusal.kind());
        assertEquals(503, refusal.kind().status(), "un guasto nostro non è un 403");
        assertTrue(
                refusal.getMessage().contains("Non è un problema dei tuoi"),
                "il testo non deve accusare l'utente di un guasto nostro");
    }

    // ── dichiarazione: vince il metodo, e la rilettura si attiva solo dove è chiesta ──

    @Test
    void theMethodDeclarationWinsOverTheClassOne() {
        FixedRole roles = new FixedRole(new AppRoleOutcome.Granted(AppRole.editor));
        // Sulla classe c'è viewer, sul metodo admin: con editor non si passa.
        AppRoleRequiredException refusal = assertThrows(
                AppRoleRequiredException.class,
                () -> filter(roles, FakeResource.class, "destroy").filter(null));
        assertEquals(AppRole.admin, refusal.required());
    }

    @Test
    void onlyAnOperationThatAsksForItRereadsFromTheSourceOfTruth() {
        FixedRole cached = new FixedRole(new AppRoleOutcome.Granted(AppRole.admin));
        filter(cached, FakeResource.class, "read").filter(null);
        assertEquals(1, cached.cachedCalls, "il percorso normale legge la copia locale");
        assertEquals(0, cached.freshCalls);

        FixedRole fresh = new FixedRole(new AppRoleOutcome.Granted(AppRole.admin));
        filter(fresh, FakeResource.class, "destroy").filter(null);
        assertEquals(1, fresh.freshCalls, "l'operazione irreversibile rilegge dalla fonte di verità");
        assertEquals(0, fresh.cachedCalls, "e NON si accontenta della copia locale");
    }

    /**
     * Se l'annotazione non fosse risolvibile, un varco che non sa cosa chiedere non lascia passare: è la
     * differenza fra un varco e una decorazione.
     */
    @Test
    void aGateThatCannotReadItsRequirementDenies() {
        FixedRole roles = new FixedRole(new AppRoleOutcome.Granted(AppRole.admin));
        AppRoleRequiredException refusal = assertThrows(
                AppRoleRequiredException.class,
                () -> filter(roles, UnannotatedResource.class, "read").filter(null));
        assertSame(AppRoleRequiredException.Case.UNAVAILABLE, refusal.kind());
    }
}
