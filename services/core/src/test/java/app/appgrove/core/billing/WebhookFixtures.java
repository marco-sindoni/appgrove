package app.appgrove.core.billing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Instant;
import java.util.UUID;

/**
 * Costruttore di payload webhook sintetici per l'L1 esaustivo (UC 0025): controllo preciso di
 * {@code event_id}, {@code event_type}, {@code occurred_at}, {@code status} → permette di esercitare
 * dedup, out-of-order, set eventi completo e casi limite, cosa che gli scenari dello stub (event_id/ts
 * generati) non consentono. La firma è applicata dal test via {@code PaddleSignature}.
 */
final class WebhookFixtures {

    private static final ObjectMapper M = new ObjectMapper();

    private WebhookFixtures() {}

    /** Evento subscription/transaction (snapshot). {@code status}/{@code tierId} possono essere null. */
    static String subscription(
            String eventId,
            String type,
            String status,
            Instant occurredAt,
            String tenantId,
            UUID appId,
            UUID tierId,
            Instant periodEnd) {
        ObjectNode root = M.createObjectNode();
        root.put("event_id", eventId);
        root.put("event_type", type);
        root.put("occurred_at", occurredAt.toString());
        ObjectNode data = root.putObject("data");
        data.put("paddle_subscription_id", "sub_" + eventId);
        if (status != null) {
            data.put("status", status);
        }
        data.put("current_period_start", occurredAt.toString());
        if (periodEnd != null) {
            data.put("current_period_end", periodEnd.toString());
        }
        ObjectNode custom = data.putObject("custom_data");
        custom.put("tenant_id", tenantId);
        custom.put("app_id", appId.toString());
        if (tierId != null) {
            custom.put("app_tier_id", tierId.toString());
        }
        return write(root);
    }

    /**
     * Evento {@code transaction.*} con i dati economici (UC 0096): oltre allo snapshot di subscription
     * porta importo, valuta, ciclo, riferimento della transazione e — quando c'è — la ricevuta. Con
     * {@code receiptUrl} a {@code null} si esercita il caso "ricevuta non ancora disponibile".
     */
    static String transaction(
            String eventId,
            String type,
            Instant occurredAt,
            String tenantId,
            UUID appId,
            UUID tierId,
            String paddleTransactionId,
            int amount,
            String currency,
            String billingCycle,
            String receiptUrl,
            Instant billedAt) {
        ObjectNode root = M.createObjectNode();
        root.put("event_id", eventId);
        root.put("event_type", type);
        root.put("occurred_at", occurredAt.toString());
        ObjectNode data = root.putObject("data");
        data.put("paddle_subscription_id", "sub_" + eventId);
        data.put("status", "active");
        data.put("current_period_start", occurredAt.toString());
        data.put("paddle_transaction_id", paddleTransactionId);
        data.put("amount", amount);
        if (currency != null) {
            data.put("currency", currency);
        }
        if (billingCycle != null) {
            data.put("billing_cycle", billingCycle);
        }
        if (receiptUrl != null) {
            data.put("receipt_url", receiptUrl);
        }
        if (billedAt != null) {
            data.put("billed_at", billedAt.toString());
        }
        ObjectNode custom = data.putObject("custom_data");
        custom.put("tenant_id", tenantId);
        custom.put("app_id", appId.toString());
        if (tierId != null) {
            custom.put("app_tier_id", tierId.toString());
        }
        return write(root);
    }

    /**
     * Variante che dichiara anche la <b>commissione</b> trattenuta dal fornitore (UC 0071): è il caso in cui
     * il netto NON viene stimato ma preso per buono da chi ha incassato.
     */
    static String transactionWithFee(
            String eventId,
            String type,
            Instant occurredAt,
            String tenantId,
            UUID appId,
            UUID tierId,
            String paddleTransactionId,
            int amount,
            String currency,
            Instant billedAt,
            int fee) {
        try {
            ObjectNode root = (ObjectNode) M.readTree(transaction(
                    eventId, type, occurredAt, tenantId, appId, tierId,
                    paddleTransactionId, amount, currency, "monthly", null, billedAt));
            ((ObjectNode) root.get("data")).put("fee", fee);
            return write(root);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /** Una riga di dettaglio di un accredito: transazione, netto accreditato (con segno), valuta. */
    record PayoutLine(String paddleTransactionId, int netAmount, String currency) {}

    /**
     * Evento {@code payout.paid} (UC 0071): l'accredito periodico del fornitore, con il dettaglio delle
     * transazioni che lo compongono. Non ha {@code custom_data}: un accredito raccoglie transazioni di conti
     * diversi ed è un dato economico della piattaforma.
     */
    static String payout(
            String eventId,
            Instant occurredAt,
            String paddlePayoutId,
            int amount,
            String currency,
            Instant paidAt,
            PayoutLine... lines) {
        ObjectNode root = M.createObjectNode();
        root.put("event_id", eventId);
        root.put("event_type", "payout.paid");
        root.put("occurred_at", occurredAt.toString());
        ObjectNode data = root.putObject("data");
        data.put("paddle_payout_id", paddlePayoutId);
        data.put("amount", amount);
        data.put("currency", currency);
        data.put("paid_at", paidAt.toString());
        var array = data.putArray("lines");
        for (PayoutLine line : lines) {
            ObjectNode node = array.addObject();
            node.put("paddle_transaction_id", line.paddleTransactionId());
            node.put("net_amount", line.netAmount());
            node.put("currency", line.currency());
        }
        return write(root);
    }

    /** Evento {@code customer.updated} (cattura {@code paddle_customer_id}). */
    static String customer(String eventId, Instant occurredAt, String tenantId, String paddleCustomerId) {
        ObjectNode root = M.createObjectNode();
        root.put("event_id", eventId);
        root.put("event_type", "customer.updated");
        root.put("occurred_at", occurredAt.toString());
        ObjectNode data = root.putObject("data");
        data.put("paddle_customer_id", paddleCustomerId);
        data.putObject("custom_data").put("tenant_id", tenantId);
        return write(root);
    }

    private static String write(ObjectNode node) {
        try {
            return M.writeValueAsString(node);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
