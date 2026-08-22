package app.appgrove.@@APP_ID@@;

import static org.junit.jupiter.api.Assertions.assertEquals;

import app.appgrove.commons.access.AppOperationsContractVerifier;
import org.junit.jupiter.api.Test;

/**
 * Il contratto dei tre ruoli, reso <b>vero</b> per l'app @@APP_NAME@@ (UC 0101 §8): il documento delle
 * operazioni e il codice devono dire la stessa cosa, nelle due direzioni.
 *
 * <p>La direzione che conta è «reale → dichiarato»: chi aggiungerà domani una rotta a questa applicazione
 * e si dimenticherà il varco troverà questo collaudo rosso, con scritto quale operazione manca. Senza di
 * esso la dimenticanza si scoprirebbe in produzione, o mai.
 */
class AppOperationsContractTest {

    private final @@APP_CLASS@@OperationsContract contract = new @@APP_CLASS@@OperationsContract();

    @Test
    void ilDocumentoDelleOperazioniCoincideConIlCodice() {
        AppOperationsContractVerifier.verify(contract, "app.appgrove.@@APP_ID@@");
    }

    /** L'identificativo del documento è quello dell'applicazione: il varco chiede il ruolo su sé stessa. */
    @Test
    void ilDocumentoParlaDiQuestaApplicazione() {
        assertEquals("@@APP_ID@@", contract.appId());
    }
}
