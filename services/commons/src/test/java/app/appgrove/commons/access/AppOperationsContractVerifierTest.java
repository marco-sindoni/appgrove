package app.appgrove.commons.access;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * <b>Auto-collaudo del verificatore</b> (UC 0101 §8): dimostra che il collaudo del contratto dei tre ruoli
 * <b>fallisce davvero</b> davanti ai difetti che esiste per cogliere. È il collaudo che rende credibili gli
 * altri: un verificatore mai visto fallire non è una prova, è una speranza.
 *
 * <p>Le risorse-campione sono classi annidate di questo file, non un'applicazione finta: le annotazioni si
 * leggono per riflessione e restare accanto alle asserzioni è ciò che rende ogni caso leggibile in dieci
 * righe. In {@code commons} non gira alcuna augmentation Quarkus, quindi nessuna di queste diventa una rotta.
 */
class AppOperationsContractVerifierTest {

    // ── Risorse-campione ─────────────────────────────────────────────────────

    /** Il caso corretto: letture {@code viewer} dalla classe, scritture {@code editor} dal metodo. */
    @Path("/sample/items")
    @RequiresAppRole(AppRole.viewer)
    static class GuardedResource {
        @GET
        public String read() {
            return "";
        }

        @POST
        @RequiresAppRole(AppRole.editor)
        public String write() {
            return "";
        }

        @DELETE
        @RequiresAppRole(AppRole.admin)
        public String remove() {
            return "";
        }

        /** Aiutante non esposto: la direzione «reale → dichiarato» non deve pretenderlo nel documento. */
        String helper() {
            return "";
        }
    }

    /** Risorsa senza varco: è la forma delle vie esenti (diritti dell'interessato, stato di quota). */
    @Path("/sample/quota")
    static class ExemptResource {
        @GET
        public String status() {
            return "";
        }
    }

    /** Una scrittura che nessuno protegge: il difetto centrale che il collaudo esiste per cogliere. */
    @Path("/sample/unprotected")
    static class UnprotectedResource {
        @POST
        public String write() {
            return "";
        }
    }

    /** Una scrittura protetta troppo poco: il varco c'è, ma chiede solo {@code viewer}. */
    @Path("/sample/weak")
    @RequiresAppRole(AppRole.viewer)
    static class WeakResource {
        @POST
        public String write() {
            return "";
        }
    }

    // ── Il caso corretto passa ───────────────────────────────────────────────

    @Test
    void aWellClassifiedApplicationPasses() {
        assertDoesNotThrow(() -> AppOperationsContractVerifier.verify(
                contract(
                        AppOperation.requiring("items.read", "Elenca", "List", GuardedResource.class, "read",
                                AppRole.viewer),
                        AppOperation.requiring("items.write", "Crea", "Create", GuardedResource.class, "write",
                                AppRole.editor),
                        AppOperation.requiring("items.remove", "Cancella", "Delete", GuardedResource.class, "remove",
                                AppRole.admin),
                        AppOperation.exempt("quota.status", "Stato quota", "Quota status", ExemptResource.class,
                                "status", "informativo: resta raggiungibile anche senza ruolo (UC 0099)")),
                List.of(GuardedResource.class, ExemptResource.class)));
    }

    // ── I quattro difetti che devono diventare rossi ─────────────────────────

    /** Definition of Done §3 della storia: «esiste il collaudo che coglie una scrittura non protetta». */
    @Test
    void anUnprotectedWriteMakesTheSuiteRed() {
        AssertionError error = assertThrows(AssertionError.class, () -> AppOperationsContractVerifier.verify(
                contract(AppOperation.requiring("unprotected.write", "Scrive", "Writes", UnprotectedResource.class,
                        "write", AppRole.editor)),
                List.of(UnprotectedResource.class)));
        assertTrue(error.getMessage().contains("non è protetto"),
                "il messaggio deve dire che manca il varco, non solo che qualcosa non torna: " + error.getMessage());
    }

    @Test
    void aWriteThatOnlyAsksForViewerMakesTheSuiteRed() {
        AssertionError error = assertThrows(AssertionError.class, () -> AppOperationsContractVerifier.verify(
                contract(AppOperation.requiring("weak.write", "Scrive", "Writes", WeakResource.class, "write",
                        AppRole.viewer)),
                List.of(WeakResource.class)));
        assertTrue(error.getMessage().contains("DISPOSITIVA"),
                "il messaggio deve nominare la cascata: " + error.getMessage());
    }

    /** La direzione che coglie l'operazione aggiunta domani e dimenticata nel documento. */
    @Test
    void anExposedOperationMissingFromTheDocumentMakesTheSuiteRed() {
        AssertionError error = assertThrows(AssertionError.class, () -> AppOperationsContractVerifier.verify(
                contract(AppOperation.requiring("items.read", "Elenca", "List", GuardedResource.class, "read",
                        AppRole.viewer)),
                List.of(GuardedResource.class)));
        assertTrue(error.getMessage().contains("ESPOSTA e non dichiarata"),
                "devono comparire le due operazioni non dichiarate: " + error.getMessage());
        assertTrue(error.getMessage().contains("write") && error.getMessage().contains("remove"),
                "entrambe, non solo la prima: " + error.getMessage());
    }

    /** Trappola 3 del piano di lavoro: proteggere una via di conformità «per coerenza» rompe un diritto. */
    @Test
    void anExemptOperationThatIsActuallyGuardedMakesTheSuiteRed() {
        AssertionError error = assertThrows(AssertionError.class, () -> AppOperationsContractVerifier.verify(
                contract(
                        AppOperation.exempt("items.read", "Elenca", "List", GuardedResource.class, "read",
                                "sarebbe una via di conformità"),
                        AppOperation.requiring("items.write", "Crea", "Create", GuardedResource.class, "write",
                                AppRole.editor),
                        AppOperation.requiring("items.remove", "Cancella", "Delete", GuardedResource.class, "remove",
                                AppRole.admin)),
                List.of(GuardedResource.class)));
        assertTrue(error.getMessage().contains("esenzione protetta è un diritto rotto"),
                "il messaggio deve spiegare il danno, non solo l'incoerenza: " + error.getMessage());
    }

    // ── Gli altri disallineamenti ────────────────────────────────────────────

    @Test
    void aDocumentThatNamesAMethodWhichNoLongerExistsMakesTheSuiteRed() {
        AssertionError error = assertThrows(AssertionError.class, () -> AppOperationsContractVerifier.verify(
                contract(AppOperation.requiring("items.rinominata", "Elenca", "List", GuardedResource.class,
                        "readAll", AppRole.viewer)),
                List.of()));
        assertTrue(error.getMessage().contains("non esiste"), error.getMessage());
    }

    @Test
    void twoTruthsAboutTheSamePowerMakeTheSuiteRed() {
        AssertionError error = assertThrows(AssertionError.class, () -> AppOperationsContractVerifier.verify(
                contract(AppOperation.requiring("items.read", "Elenca", "List", GuardedResource.class, "read",
                        AppRole.admin)),
                List.of()));
        assertTrue(error.getMessage().contains("il varco applica viewer"), error.getMessage());
    }

    @Test
    void aDuplicatedIdentifierMakesTheSuiteRed() {
        AssertionError error = assertThrows(AssertionError.class, () -> AppOperationsContractVerifier.verify(
                contract(
                        AppOperation.requiring("items.same", "Elenca", "List", GuardedResource.class, "read",
                                AppRole.viewer),
                        AppOperation.requiring("items.same", "Crea", "Create", GuardedResource.class, "write",
                                AppRole.editor)),
                List.of()));
        assertTrue(error.getMessage().contains("identificativo duplicato"), error.getMessage());
    }

    /** Un metodo senza verbo HTTP non è un'operazione: dichiararlo è un errore, non una precisazione. */
    @Test
    void aMethodWithoutAnHttpVerbIsNotAnOperation() {
        AssertionError error = assertThrows(AssertionError.class, () -> AppOperationsContractVerifier.verify(
                new AppOperationsContract() {
                    @Override
                    public String appId() {
                        return "sample";
                    }

                    @Override
                    public List<AppOperation> operations() {
                        return List.of(AppOperation.exempt("quota.helper", "Aiutante", "Helper",
                                Helper.class, "notAnOperation", "non è nemmeno un'operazione"));
                    }
                },
                List.of()));
        assertTrue(error.getMessage().contains("non porta un verbo HTTP"), error.getMessage());
    }

    @Path("/sample/helper")
    static class Helper {
        public String notAnOperation() {
            return "";
        }
    }

    // ── helper ───────────────────────────────────────────────────────────────

    private static AppOperationsContract contract(AppOperation... operations) {
        return new AppOperationsContract() {
            @Override
            public String appId() {
                return "sample";
            }

            @Override
            public List<AppOperation> operations() {
                return List.of(operations);
            }
        };
    }
}
