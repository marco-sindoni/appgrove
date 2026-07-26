package app.appgrove.core.platform;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.appgrove.core.TestData;
import app.appgrove.core.TestTokens;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.time.Instant;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

/**
 * Timbro dell'ultima attività (UC 0035): il tracker throttla le scritture di {@code last_active_at}
 * nella finestra configurata e il filtro lo aggiorna solo sulle richieste autenticate (tenant dal
 * JWT verificato, invariante #1). "Adesso" iniettabile nel tracker: niente attese reali.
 */
@QuarkusTest
class AccountActivityTrackerTest {

    private static final String TENANT_TOUCH = "22222222-0000-0000-0000-0000000035a1";
    private static final String TENANT_FILTER = "22222222-0000-0000-0000-0000000035a2";

    @Inject
    TestData data;

    @Inject
    AccountActivityTracker tracker;

    /** La finestra di throttle nega i timbri ripetuti e li riammette una volta scaduta. */
    @Test
    void throttleGatesRepeatedStampsWithinWindow() {
        String tenant = "throttle-" + TENANT_TOUCH;
        Instant t0 = Instant.parse("2026-01-02T03:04:05Z");
        assertTrue(tracker.shouldStamp(tenant, t0), "primo timbro: da scrivere");
        assertFalse(tracker.shouldStamp(tenant, t0.plusSeconds(60)), "entro la finestra: niente riscrittura");
        assertTrue(tracker.shouldStamp(tenant, t0.plusSeconds(7 * 3600)), "oltre la finestra: si riscrive");
    }

    /** {@code touch} scrive {@code last_active_at} e, entro la finestra, non lo riscrive. */
    @Test
    void touchStampsLastActiveThrottled() {
        data.account(TENANT_TOUCH, "Attività tracker");
        Instant t0 = Instant.parse("2026-02-01T00:00:00Z");

        tracker.touch(TENANT_TOUCH, t0);
        assertEquals(t0, data.accountLastActiveAt(TENANT_TOUCH), "il primo touch scrive l'istante");

        tracker.touch(TENANT_TOUCH, t0.plusSeconds(60));
        assertEquals(t0, data.accountLastActiveAt(TENANT_TOUCH), "entro la finestra resta invariato");

        Instant later = t0.plusSeconds(7 * 3600);
        tracker.touch(TENANT_TOUCH, later);
        assertEquals(later, data.accountLastActiveAt(TENANT_TOUCH), "oltre la finestra si aggiorna");
    }

    /** Il filtro timbra l'attività solo su richiesta autenticata; una richiesta anonima non tocca nulla. */
    @Test
    void filterStampsOnlyAuthenticatedRequests() {
        data.account(TENANT_FILTER, "Attività filtro");
        OffsetDateTime old = OffsetDateTime.parse("2026-01-01T00:00:00Z");
        data.backdateAccountActivity(TENANT_FILTER, old);

        // Anonima → 401 e nessun timbro.
        given().when().get("/api/platform/v1/accounts/me").then().statusCode(401);
        assertEquals(old.toInstant(), data.accountLastActiveAt(TENANT_FILTER),
                "una richiesta non autenticata non aggiorna last_active_at");

        // Autenticata per quel tenant → 200 e attività timbrata a "adesso".
        given().header("Authorization", "Bearer " + TestTokens.withTenant(TENANT_FILTER, "owner"))
                .when().get("/api/platform/v1/accounts/me").then().statusCode(200);
        assertTrue(data.accountLastActiveAt(TENANT_FILTER).isAfter(old.toInstant().plusSeconds(1)),
                "una richiesta autenticata aggiorna last_active_at");
    }
}
