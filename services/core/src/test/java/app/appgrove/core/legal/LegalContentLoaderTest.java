package app.appgrove.core.legal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;
import org.junit.jupiter.api.Test;

/** Risoluzione dei token {@code {{titolare.*}}} (UC 0056): stessa semantica di site/src/lib/legal.ts. */
class LegalContentLoaderTest {

    @Test
    void substitutesKnownTokens() {
        String out = LegalContentLoader.substituteTokens(
                "Titolare: {{titolare.ragione_sociale}} — P.IVA {{ titolare.piva }}.",
                Map.of("titolare.ragione_sociale", "Acme S.r.l.", "titolare.piva", "IT01234567890"));
        assertEquals("Titolare: Acme S.r.l. — P.IVA IT01234567890.", out);
    }

    @Test
    void throwsOnUnknownToken() {
        var ex = assertThrows(IllegalStateException.class, () ->
                LegalContentLoader.substituteTokens("{{titolare.mancante}}", Map.of()));
        org.junit.jupiter.api.Assertions.assertTrue(ex.getMessage().contains("titolare.mancante"));
    }

    @Test
    void majorIsFirstSemverDigit() {
        assertEquals(2, LegalContentLoader.majorOf("2.3.1"));
        assertEquals(1, LegalContentLoader.majorOf("1.0.0"));
    }
}
