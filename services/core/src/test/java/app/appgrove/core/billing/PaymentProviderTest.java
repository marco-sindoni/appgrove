package app.appgrove.core.billing;

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
                "aaaaaaaa-0000-0000-0000-000000000001", UUID.randomUUID(), null, "annual", "owner@acme.test"));

        assertTrue(init.checkoutToken().startsWith("chk_"), "checkout token plausibile");
        assertTrue(init.paddleCustomerId().startsWith("ctm_"), "customer id plausibile");
        assertTrue(init.paddleTransactionId().startsWith("txn_"), "transaction id plausibile");
        assertTrue(init.paddleSubscriptionId().startsWith("sub_"), "subscription id plausibile");
    }

    @Test
    void stubGeneratesDistinctTokensPerCheckout() {
        var a = provider.startCheckout(new PaymentProvider.StartCheckoutCommand(
                "t", UUID.randomUUID(), null, "annual", "a@test"));
        var b = provider.startCheckout(new PaymentProvider.StartCheckoutCommand(
                "t", UUID.randomUUID(), null, "annual", "b@test"));
        assertNotEquals(a.checkoutToken(), b.checkoutToken());
    }
}
