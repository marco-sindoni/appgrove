package app.appgrove.core.gdpr;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.appgrove.commons.gdpr.DataManifest;
import app.appgrove.commons.gdpr.ExportResult;
import app.appgrove.commons.gdpr.GdprScope;
import app.appgrove.commons.gdpr.PurgeResult;
import app.appgrove.core.TestData;
import io.quarkus.test.junit.QuarkusTest;
import java.util.List;
import java.util.Map;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

/**
 * Contratto GDPR della piattaforma (#13 L74, UC 0032 §9 compliance): l'export copre OGNI entità con
 * campi {@code @PersonalData} (metro: il manifesto derivato) e la purge è fisica, scoped al tenant e
 * senza orfani. Una futura entità col manifesto ma fuori dall'export fa fallire questo test.
 */
@QuarkusTest
class PlatformGdprContractTest {

    private static final String TENANT_A = "77777777-0000-0000-0000-0000000000c1";
    private static final String TENANT_B = "77777777-0000-0000-0000-0000000000c2";

    @Inject
    PlatformDataContract contract;

    @Inject
    TestData data;

    @Test
    void exportCoversEveryManifestEntity() {
        data.account(TENANT_A, "Acme GDPR");
        data.user(TENANT_A, "sub-gdpr-a", "gdpr-a@example.test", "owner");
        data.invitation(TENANT_A, "invitato-a@example.test", "member");
        var ticketId = data.ticket(TENANT_A, "support", "Domanda di prova", "open");
        data.ticketMessage(TENANT_A, ticketId, "user", "Testo del messaggio");
        // Newsletter (UC 0039): iscritto con l'email dell'utente del tenant → coperto per email.
        data.newsletterSubscriber("gdpr-a@example.test", "confirmed");
        // Accettazione legale (UC 0056): prova che l'export copre il log accettazioni.
        data.legalAcceptance(TENANT_A, "sub-gdpr-a", "terms", "1.0.0", 1, "accept");
        // Storico pagamenti (UC 0096): l'export deve restituire anche quello che il conto ha pagato.
        java.util.UUID gdprApp = java.util.UUID.randomUUID();
        data.app(gdprApp, "gdpr-app-" + gdprApp.toString().substring(0, 8));
        data.billingTransaction(TENANT_A, gdprApp, "txn_gdpr_" + gdprApp, 1500);

        ExportResult export = contract.exportData(new GdprScope(TENANT_A));
        assertEquals("platform", export.appId());
        assertFalse(export.steps().isEmpty(), "il contratto deve dichiarare gli step di progress");

        DataManifest manifest = contract.manifest();
        assertFalse(manifest.entries().isEmpty());
        for (DataManifest.Entry entry : manifest.entries()) {
            List<Map<String, Object>> rows = export.entities().get(entry.entity());
            assertTrue(rows != null && !rows.isEmpty(),
                    "entità del manifesto assente dall'export: " + entry.entity());
            assertTrue(rows.get(0).containsKey(entry.field()),
                    "campo personale non coperto dall'export: " + entry.entity() + "." + entry.field());
        }
    }

    @Test
    void purgeIsPhysicalScopedToTenantAndLeavesNoOrphans() {
        data.account(TENANT_A, "Da cancellare");
        data.user(TENANT_A, "sub-purge-a", "purge-a@example.test", "owner");
        data.invitation(TENANT_A, "invitato-purge-a@example.test", "member");
        var ticketA = data.ticket(TENANT_A, "privacy", "Ticket da purgare", "open");
        data.ticketMessage(TENANT_A, ticketA, "user", "Contenuto da purgare");
        data.account(TENANT_B, "Da preservare");
        data.user(TENANT_B, "sub-purge-b", "purge-b@example.test", "owner");

        PurgeResult result = contract.purgeData(new GdprScope(TENANT_A));
        assertTrue(result.total() > 0, "la purge deve cancellare almeno una riga");

        // A: nessun dato residuo in nessuna entità esportabile
        ExportResult afterA = contract.exportData(new GdprScope(TENANT_A));
        afterA.entities().forEach((entity, rows) ->
                assertTrue(rows.isEmpty(), "dati residui per il tenant A in " + entity));

        // B: intatto
        ExportResult afterB = contract.exportData(new GdprScope(TENANT_B));
        assertFalse(afterB.entities().get("accounts").isEmpty(), "l'account di B non va toccato");
        assertFalse(afterB.entities().get("identities").isEmpty(), "le persone di B non vanno toccate");
    }

    /**
     * UC 0116 — la stretta più delicata verso la conformità, provata in <b>entrambi i versi</b>:
     * cancellare l'account A non cancella l'identità di chi appartiene anche a B (sarebbe cancellare
     * dati di un altro titolare), e cancellare l'ultimo account di una persona la rende cancellabile.
     */
    @Test
    void purgeKeepsTheIdentityWhenAnotherMembershipSurvives() {
        String tenantUno = "77777777-0000-0000-0000-0000000000d1";
        String tenantDue = "77777777-0000-0000-0000-0000000000d2";
        data.account(tenantUno, "Conto uno");
        data.account(tenantDue, "Conto due");
        java.util.UUID condivisa = data.identity("sub-condivisa", "condivisa@example.test", "Condivisa");
        data.membership(tenantUno, condivisa, "member");
        data.membership(tenantDue, condivisa, "owner");
        // persona presente SOLO nel conto uno: con la purga deve sparire del tutto
        java.util.UUID soloUno = data.identity("sub-solo-uno", "solo-uno@example.test", "Solo Uno");
        data.membership(tenantUno, soloUno, "member");

        contract.purgeData(new GdprScope(tenantUno));

        assertEquals(1, data.identityCount("condivisa@example.test"),
                "l'identità sopravvive: appartiene ancora al conto due");
        assertEquals(List.of(tenantDue), data.tenantsOf(condivisa),
                "resta solo l'appartenenza al conto due");
        assertEquals(0, data.identityCount("solo-uno@example.test"),
                "l'identità rimasta orfana va cancellata con l'account");

        // secondo verso: cancellato anche l'ultimo account, la persona non resta in giro
        contract.purgeData(new GdprScope(tenantDue));
        assertEquals(0, data.identityCount("condivisa@example.test"),
                "cancellata l'ultima appartenenza, l'identità è cancellabile e viene cancellata");
    }
}
