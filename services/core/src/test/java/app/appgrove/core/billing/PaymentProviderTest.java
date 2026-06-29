package app.appgrove.core.billing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Port {@code PaymentProvider}: in dev/test è risolto allo stub (#09 I39), che ritorna ID plausibili. */
@QuarkusTest
class PaymentProviderTest {

    @Inject
    PaymentProvider provider;

    @Test
    void stubStartCheckoutReturnsPlausibleIds() {
        var init = provider.startCheckout(new PaymentProvider.StartCheckoutCommand(
                "aaaaaaaa-0000-0000-0000-000000000001", UUID.randomUUID(), UUID.randomUUID(),
                "pri_x", "annual", "owner@acme.test", null));

        assertTrue(init.checkoutToken().startsWith("chk_"), "checkout token plausibile");
        assertTrue(init.paddleCustomerId().startsWith("ctm_"), "customer id plausibile");
        assertTrue(init.paddleTransactionId().startsWith("txn_"), "transaction id plausibile");
        assertTrue(init.paddleSubscriptionId().startsWith("sub_"), "subscription id plausibile");
    }

    @Test
    void stubGeneratesDistinctTokensPerCheckout() {
        var a = provider.startCheckout(new PaymentProvider.StartCheckoutCommand(
                "t", UUID.randomUUID(), UUID.randomUUID(), "pri_a", "annual", "a@test", null));
        var b = provider.startCheckout(new PaymentProvider.StartCheckoutCommand(
                "t", UUID.randomUUID(), UUID.randomUUID(), "pri_b", "annual", "b@test", null));
        assertNotEquals(a.checkoutToken(), b.checkoutToken());

        // customer lazy: se passato un id esistente, lo stub lo riusa
        var reuse = provider.startCheckout(new PaymentProvider.StartCheckoutCommand(
                "t", UUID.randomUUID(), UUID.randomUUID(), "pri_c", "annual", "c@test", "ctm_existing"));
        assertEquals("ctm_existing", reuse.paddleCustomerId());
    }
}
