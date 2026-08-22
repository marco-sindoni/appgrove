package app.appgrove.core.billing;

import io.quarkus.arc.properties.IfBuildProperty;
import jakarta.enterprise.context.ApplicationScoped;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Provider di pagamento finto per dev/test (#09 I39): "API Paddle finta" che ritorna ID plausibili,
 * senza alcuna rete esterna né account Paddle. Attivo quando {@code appgrove.payments.provider=stub}
 * (default in dev/test). I webhook sintetici firmati che muovono la {@code subscription} sono prodotti
 * dallo {@code StubScenarioEmitter}.
 */
@ApplicationScoped
@IfBuildProperty(name = "appgrove.payments.provider", stringValue = "stub", enableIfMissing = true)
public class StubPaymentProvider implements PaymentProvider {

    /**
     * Motivo con cui rifiutare gli addebiti dei posti (UC 0103); assente o vuoto = si accetta sempre.
     * Proprietà di <b>esecuzione</b>, non di compilazione: un collaudo la valorizza con il proprio profilo.
     */
    @ConfigProperty(name = "appgrove.seats.stub-decline-reason")
    Optional<String> declineReason;

    @Override
    public CheckoutInit startCheckout(StartCheckoutCommand command) {
        // Customer lazy (#09 C15): se l'account ne ha già uno lo riusa, altrimenti ne "crea" uno finto.
        String customerId = command.existingPaddleCustomerId() != null
                ? command.existingPaddleCustomerId()
                : "ctm_" + token();
        return new CheckoutInit("chk_" + token(), customerId, "txn_" + token(), "sub_" + token());
    }

    /**
     * Sync pricing finta: ritorna ID Paddle <b>deterministici</b> dalla chiave interna stabile (così il
     * ri-sync è idempotente, offline). Se un ID è già noto lo riconferma (mai rigenera) — modella
     * "esiste già su Paddle". Non muta importi: l'immutabilità è già garantita dal {@code PricingSyncService}.
     */
    @Override
    public PricingSyncResult syncPricing(PricingSyncRequest request) {
        Map<String, String> productIds = new LinkedHashMap<>();
        Map<String, String> priceIds = new LinkedHashMap<>();
        for (ProductSync product : request.products()) {
            productIds.put(
                    product.productKey(),
                    product.existingProductId() != null
                            ? product.existingProductId()
                            : "pro_" + det(product.productKey()));
            for (PriceSync price : product.prices()) {
                priceIds.put(
                        price.priceKey(),
                        price.existingPriceId() != null
                                ? price.existingPriceId()
                                : "pri_" + det(price.priceKey()));
            }
        }
        return new PricingSyncResult(productIds, priceIds);
    }

    /**
     * Cambio piano nello stub: <b>no-op</b>. L'effetto sulla {@code subscription} arriva dal webhook
     * sintetico emesso dallo {@link StubSubscriptionActivation} (osservatore di {@link SubscriptionChangeRequested}),
     * così l'attivazione passa sempre dalla pipeline webhook (invariante #09 C16), come per il checkout.
     */
    @Override
    public void changeSubscriptionTier(SubscriptionTierChange change) {
        // no-op: il webhook lo emette lo stub observer.
    }

    @Override
    public void cancelSubscription(SubscriptionRef ref) {
        // no-op: il webhook lo emette lo stub observer.
    }

    @Override
    public void resumeSubscription(SubscriptionRef ref) {
        // no-op: il webhook lo emette lo stub observer.
    }

    /** Portal finto (offline): URL plausibile e deterministico dal customer id, nessuna rete esterna. */
    @Override
    public CustomerPortalSession createCustomerPortalSession(String paddleCustomerId) {
        return new CustomerPortalSession(
                "https://sandbox-customer-portal.paddle.com/stub/" + det(paddleCustomerId));
    }

    /**
     * Addebito dei posti finto (UC 0103): <b>accetta</b> e restituisce identificativi plausibili, riusando
     * l'abbonamento e il cliente quando già esistono (customer lazy, come il checkout).
     *
     * <p><b>Il rifiuto si simula da configurazione</b> ({@code appgrove.seats.stub-decline-reason}): con un
     * motivo valorizzato ogni addebito dei posti viene rifiutato con quel motivo. Serve a provare il caso
     * che conta di più della storia — l'invito che <b>non nasce</b> perché l'addebito non è andato — e la
     * via della configurazione è preferibile a una regola cablata sui dati (un indirizzo «magico», un
     * importo «magico»): una regola cablata verrebbe inciampata da un collaudo qualunque che usa per caso
     * quel valore, e nessuno capirebbe perché.
     */
    @Override
    public SeatChargeResult chargeSeats(SeatChargeCommand command) {
        if (declineReason.isPresent() && !declineReason.get().isBlank()) {
            return SeatChargeResult.declined(declineReason.get());
        }
        String subscriptionId = command.paddleSubscriptionId() != null
                ? command.paddleSubscriptionId()
                : "sub_" + token();
        String customerId = command.existingPaddleCustomerId() != null
                ? command.existingPaddleCustomerId()
                : "ctm_" + token();
        return SeatChargeResult.accepted(subscriptionId, customerId, "txn_" + token());
    }

    /**
     * Storno finto: <b>no-op</b>, come ogni altra mutazione dello stub. Non c'è stato presso il fornitore da
     * riportare indietro, perché non c'è un fornitore: l'effetto che conta e che si può provare è che
     * l'unità di lavoro locale non lasci nulla dietro di sé.
     */
    @Override
    public void releaseSeatCharge(SeatChargeReversal reversal) {
        // no-op: nessuno stato finto da stornare.
    }

    /** ID plausibile e opaco (stile Paddle), deterministicamente unico. */
    private static String token() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 24);
    }

    /** Token deterministico (24 hex) dalla chiave: stabile tra ri-sync → idempotenza offline. */
    private static String det(String key) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(key.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(24);
            for (int i = 0; i < 12; i++) {
                sb.append(Character.forDigit((digest[i] >> 4) & 0xf, 16));
                sb.append(Character.forDigit(digest[i] & 0xf, 16));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 non disponibile", e);
        }
    }
}
