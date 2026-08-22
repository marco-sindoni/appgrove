package app.appgrove.commons.access;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import org.junit.jupiter.api.Test;

/**
 * La riga del documento delle operazioni (UC 0101): <b>ruolo minimo ed esenzione sono alternativi</b>, e
 * lo stato illegale non deve essere rappresentabile. Se lo fosse, il verificatore dovrebbe inseguirlo — e
 * un'operazione «senza ruolo e senza motivo» passerebbe per dichiarata mentre è dimenticata.
 */
class AppOperationTest {

    @Path("/x")
    static class Sample {
        @GET
        public String read() {
            return "";
        }
    }

    @Test
    void anOperationEitherRequiresARoleOrSaysWhyItIsExempt() {
        AppOperation guarded =
                AppOperation.requiring("x.read", "Legge", "Reads", Sample.class, "read", AppRole.viewer);
        assertFalse(guarded.exemptFromRoles());
        assertEquals(AppRole.viewer, guarded.minimumRole());

        AppOperation exempt =
                AppOperation.exempt("x.read", "Legge", "Reads", Sample.class, "read", "informativo");
        assertTrue(exempt.exemptFromRoles());
        assertEquals("informativo", exempt.exemptionReason());
    }

    @Test
    void neitherARoleNorAReasonIsRejected() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> new AppOperation("x.read", "Legge", "Reads", Sample.class, "read", null, null));
        assertTrue(error.getMessage().contains("dimenticata"), error.getMessage());
    }

    @Test
    void bothARoleAndAReasonIsRejected() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> new AppOperation("x.read", "Legge", "Reads", Sample.class, "read", AppRole.viewer, "perché sì"));
        assertTrue(error.getMessage().contains("alternativi"), error.getMessage());
    }

    /** Un'esenzione senza motivo è la forma più comoda di dimenticanza: si rifiuta. */
    @Test
    void anExemptionWithoutAReasonIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> AppOperation.exempt("x.read", "Legge", "Reads", Sample.class, "read", "  "));
    }

    /** Le descrizioni servono a chi scrive la documentazione utente: entrambe le lingue, obbligatorie. */
    @Test
    void bothDescriptionsAreMandatory() {
        assertThrows(IllegalArgumentException.class,
                () -> AppOperation.requiring("x.read", "", "Reads", Sample.class, "read", AppRole.viewer));
        assertThrows(IllegalArgumentException.class,
                () -> AppOperation.requiring("x.read", "Legge", null, Sample.class, "read", AppRole.viewer));
    }

    @Test
    void theIdentifierAndTheMethodAreMandatory() {
        assertThrows(IllegalArgumentException.class,
                () -> AppOperation.requiring(" ", "Legge", "Reads", Sample.class, "read", AppRole.viewer));
        assertThrows(IllegalArgumentException.class,
                () -> AppOperation.requiring("x.read", "Legge", "Reads", Sample.class, null, AppRole.viewer));
    }

    @Test
    void requiringWithoutARoleTellsYouToUseTheExemptFactory() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> AppOperation.requiring("x.read", "Legge", "Reads", Sample.class, "read", null));
        assertTrue(error.getMessage().contains("exempt"), error.getMessage());
    }
}
