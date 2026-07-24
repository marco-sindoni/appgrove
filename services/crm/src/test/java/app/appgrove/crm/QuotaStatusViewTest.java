package app.appgrove.crm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import app.appgrove.crm.QuotaDtos.QuotaStatusView;
import org.junit.jupiter.api.Test;

/** Unit della mappatura uso/tetto → vista quota (rami: tetto presente, illimitato, oltre il tetto). */
class QuotaStatusViewTest {

    @Test
    void withCapComputesRemaining() {
        QuotaStatusView v = QuotaStatusView.of("seats", 3, 10);
        assertEquals(10L, v.limit());
        assertEquals(7L, v.remaining());
    }

    @Test
    void negativeCapMeansUnlimited() {
        QuotaStatusView v = QuotaStatusView.of("seats", 5, -1);
        assertNull(v.limit());
        assertNull(v.remaining());
    }

    @Test
    void remainingNeverGoesNegative() {
        QuotaStatusView v = QuotaStatusView.of("seats", 12, 10);
        assertEquals(0L, v.remaining());
    }
}
