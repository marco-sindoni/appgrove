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
