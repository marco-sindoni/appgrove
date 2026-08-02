package app.appgrove.core.billing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.agroal.api.AgroalDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Emette i webhook sintetici <b>firmati</b> di uno scenario lifecycle (UC 0023). Ogni evento è uno
 * snapshot completo dello stato di subscription, firmato HMAC e passato per la <b>stessa</b> pipeline
 * di ingest dei webhook reali ({@link WebhookIngestService}: verifica firma → coda → consumer). Il
 * linkage tenant è impostato server-side nei {@code custom_data} (#09 C14/D21).
 *
 * <p>Copre il set di scenari lifecycle (#09 I39): happy/past_due/canceled/upgrade/downgrade/paused/
 * resumed/renewal/chargeback/customer, più i due della riconciliazione (UC 0071): {@code payout} accredita
 * il netto non ancora accreditato e {@code refund} rimborsa l'ultimo pagamento e lo restituisce in un
 * accredito successivo. I casi limite per L1 (firma errata/replay, duplicato,
 * out-of-order) sono costruiti puntualmente nei test ({@code WebhookFixtures}); la gestione lato
 * consumer (dedup/out-of-order/DLQ) è ora completa (UC 0025).
 */
@ApplicationScoped
public class StubScenarioEmitter {

    @Inject
    WebhookIngestService ingest;

    @Inject
    PaddleSignature signature;

    @Inject
    ObjectMapper mapper;

    @Inject
    AgroalDataSource ds;

    /** Istante dell'evento più avanti emesso finora: tiene monotòno il tempo compresso dello stub. */
    private Instant lastEmittedAt;

    /** Evento emesso, per la risposta dell'endpoint dev. */
    public record EmittedEvent(String eventType, String status) {}

    /**
     * Emette la sequenza dello scenario per {@code (tenant, app)}. Per upgrade/downgrade
     * {@code targetTierId} è il tier di destinazione (per gli altri scenari è ignorato).
     */
    public List<EmittedEvent> emit(
            LifecycleScenario scenario, String tenantId, UUID appId, UUID appTierId, UUID targetTierId) {
        String paddleSubId = "sub_" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
        Instant base = nextBase();
        List<EmittedEvent> out = new ArrayList<>();
        switch (scenario) {
            case happy_path -> {
                out.add(send("subscription.created", SubscriptionStatus.trialing, tenantId, appId, appTierId,
                        paddleSubId, base, base, base.plus(14, ChronoUnit.DAYS), null, base.plus(14, ChronoUnit.DAYS)));
                out.add(send("subscription.activated", SubscriptionStatus.active, tenantId, appId, appTierId,
                        paddleSubId, base.plusSeconds(1), base, base.plus(365, ChronoUnit.DAYS), null, null));
                // Il pagamento che ha attivato l'abbonamento (UC 0096): senza di lui lo storico dei
                // pagamenti resterebbe vuoto anche dopo un acquisto, e la pagina Billing non sarebbe
                // verificabile in locale. Stesso periodo dell'attivazione: non muove nulla, racconta.
                out.add(send("transaction.completed", SubscriptionStatus.active, tenantId, appId, appTierId,
                        paddleSubId, base.plusSeconds(2), base, base.plus(365, ChronoUnit.DAYS), null, null));
            }
            case past_due -> {
                out.add(send("subscription.activated", SubscriptionStatus.active, tenantId, appId, appTierId,
                        paddleSubId, base, base, base.plus(365, ChronoUnit.DAYS), null, null));
                out.add(send("transaction.payment_failed", SubscriptionStatus.past_due, tenantId, appId, appTierId,
                        paddleSubId, base.plusSeconds(1), base, base.plus(365, ChronoUnit.DAYS), null, null));
            }
            case canceled -> {
                out.add(send("subscription.activated", SubscriptionStatus.active, tenantId, appId, appTierId,
                        paddleSubId, base, base, base.plus(365, ChronoUnit.DAYS), null, null));
                out.add(send("subscription.canceled", SubscriptionStatus.canceled, tenantId, appId, appTierId,
                        paddleSubId, base.plusSeconds(1), base, base.plus(365, ChronoUnit.DAYS),
                        base.plus(365, ChronoUnit.DAYS), null));
            }
            case upgrade -> {
                out.add(send("subscription.activated", SubscriptionStatus.active, tenantId, appId, appTierId,
                        paddleSubId, base, base, base.plus(365, ChronoUnit.DAYS), null, null));
                out.add(send("subscription.updated", SubscriptionStatus.active, tenantId, appId,
                        require(targetTierId, "targetTierId richiesto per upgrade"),
                        paddleSubId, base.plusSeconds(1), base, base.plus(365, ChronoUnit.DAYS), null, null));
            }
            case downgrade -> {
                out.add(send("subscription.activated", SubscriptionStatus.active, tenantId, appId, appTierId,
                        paddleSubId, base, base, base.plus(365, ChronoUnit.DAYS), null, null));
                out.add(send("subscription.updated", SubscriptionStatus.active, tenantId, appId,
                        require(targetTierId, "targetTierId richiesto per downgrade"),
                        paddleSubId, base.plusSeconds(1), base, base.plus(365, ChronoUnit.DAYS), null, null));
            }
            case paused -> {
                out.add(send("subscription.activated", SubscriptionStatus.active, tenantId, appId, appTierId,
                        paddleSubId, base, base, base.plus(365, ChronoUnit.DAYS), null, null));
                out.add(send("subscription.paused", SubscriptionStatus.paused, tenantId, appId, appTierId,
                        paddleSubId, base.plusSeconds(1), base, base.plus(365, ChronoUnit.DAYS), null, null));
            }
            case resumed -> {
                out.add(send("subscription.activated", SubscriptionStatus.active, tenantId, appId, appTierId,
                        paddleSubId, base, base, base.plus(365, ChronoUnit.DAYS), null, null));
                out.add(send("subscription.paused", SubscriptionStatus.paused, tenantId, appId, appTierId,
                        paddleSubId, base.plusSeconds(1), base, base.plus(365, ChronoUnit.DAYS), null, null));
                out.add(send("subscription.resumed", SubscriptionStatus.active, tenantId, appId, appTierId,
                        paddleSubId, base.plusSeconds(2), base, base.plus(365, ChronoUnit.DAYS), null, null));
            }
            case renewal -> {
                out.add(send("subscription.activated", SubscriptionStatus.active, tenantId, appId, appTierId,
                        paddleSubId, base, base, base.plus(365, ChronoUnit.DAYS), null, null));
                // rinnovo: il periodo avanza di un anno (transaction.completed → active)
                out.add(send("transaction.completed", SubscriptionStatus.active, tenantId, appId, appTierId,
                        paddleSubId, base.plusSeconds(1), base.plus(365, ChronoUnit.DAYS),
                        base.plus(730, ChronoUnit.DAYS), null, null));
            }
            case chargeback -> {
                out.add(send("subscription.activated", SubscriptionStatus.active, tenantId, appId, appTierId,
                        paddleSubId, base, base, base.plus(365, ChronoUnit.DAYS), null, null));
                out.add(send("transaction.disputed", SubscriptionStatus.past_due, tenantId, appId, appTierId,
                        paddleSubId, base.plusSeconds(1), base, base.plus(365, ChronoUnit.DAYS), null, null));
            }
            case customer -> {
                String paddleCustomerId = "ctm_" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
                out.add(sendCustomer("customer.updated", tenantId, paddleCustomerId, base));
            }
            case payout -> out.add(sendPayout(pendingNet(tenantId, appId), base));
            case refund -> {
                SettledTransaction refunded = lastPaid(tenantId, appId);
                out.add(sendRefund(refunded, tenantId, appId, appTierId, base));
                // La restituzione arriva nell'accredito SUCCESSIVO, con segno negativo: è così che il
                // fornitore la comunica, ed è la ragione per cui la quadratura di un accredito già
                // registrato non deve essere ricalcolata (decisione 5, change 0083).
                out.add(sendPayout(
                        List.of(new SettledTransaction(
                                refunded.paddleTransactionId(),
                                refunded.amount(),
                                -refunded.netAmount(),
                                refunded.currency())),
                        base.plusSeconds(1)));
            }
        }
        return out;
    }

    /**
     * Emette un singolo {@code subscription.updated} per una mutazione self-service (UC 0028): cambio tier
     * immediato (upgrade), downgrade schedulato ({@code scheduledTierId}+{@code scheduledChangeAt}),
     * disdetta ({@code cancelAt}) o riattivazione ({@code cancelAt=null}). Lo snapshot è calcolato dal
     * resource e passato per la <b>stessa</b> pipeline webhook firmata; lo stato resta {@code active}
     * (una disdetta programmata è {@code active}+{@code cancel_at}, semantica UC 0026).
     */
    public EmittedEvent emitSubscriptionUpdate(
            String tenantId, UUID appId, UUID resultTierId, String paddleSubId,
            Instant periodStart, Instant periodEnd, Instant cancelAt,
            UUID scheduledTierId, Instant scheduledChangeAt, Instant afterOccurredAt) {
        // monotòno rispetto all'ultimo evento applicato: evita che la guardia out-of-order (UC 0025) scarti
        // il cambio quando arriva nello stesso secondo dell'attivazione (tempo compresso dello stub).
        Instant occurredAt = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        if (afterOccurredAt != null && !occurredAt.isAfter(afterOccurredAt)) {
            occurredAt = afterOccurredAt.plusSeconds(1);
        }
        return send("subscription.updated", SubscriptionStatus.active, tenantId, appId, resultTierId,
                paddleSubId, occurredAt, periodStart, periodEnd, cancelAt, null,
                scheduledTierId, scheduledChangeAt);
    }

    /**
     * Istante di partenza del prossimo scenario, <b>monotòno</b> rispetto a tutto ciò che questo
     * simulatore ha già emesso.
     *
     * <p>Serve perché il tempo dello stub è compresso: due scenari lanciati nello stesso secondo — cosa
     * normalissima in un test — partirebbero dallo stesso {@code occurred_at}, e la guardia out-of-order
     * del consumer (UC 0025) scarterebbe come "vecchio" il secondo, che invece è il più recente. È lo
     * stesso accorgimento già applicato alle mutazioni self-service, esteso agli scenari perché uno di
     * essi ora dura tre eventi (il pagamento del percorso felice, UC 0096) invece di due.
     */
    private synchronized Instant nextBase() {
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        return lastEmittedAt == null || now.isAfter(lastEmittedAt) ? now : lastEmittedAt.plusSeconds(1);
    }

    /** Ricorda l'istante più avanti fra quelli emessi, per la monotonia di {@link #nextBase()}. */
    private synchronized void remember(Instant occurredAt) {
        if (occurredAt != null && (lastEmittedAt == null || occurredAt.isAfter(lastEmittedAt))) {
            lastEmittedAt = occurredAt;
        }
    }

    private EmittedEvent send(
            String eventType, SubscriptionStatus status, String tenantId, UUID appId, UUID appTierId,
            String paddleSubId, Instant occurredAt, Instant periodStart, Instant periodEnd,
            Instant cancelAt, Instant trialEnd) {
        return send(eventType, status, tenantId, appId, appTierId, paddleSubId, occurredAt,
                periodStart, periodEnd, cancelAt, trialEnd, null, null);
    }

    private EmittedEvent send(
            String eventType, SubscriptionStatus status, String tenantId, UUID appId, UUID appTierId,
            String paddleSubId, Instant occurredAt, Instant periodStart, Instant periodEnd,
            Instant cancelAt, Instant trialEnd, UUID scheduledTierId, Instant scheduledChangeAt) {
        String body = build(eventType, status, tenantId, appId, appTierId, paddleSubId, occurredAt,
                periodStart, periodEnd, cancelAt, trialEnd, scheduledTierId, scheduledChangeAt);
        ingest.ingest(body, signature.sign(body));
        remember(occurredAt);
        return new EmittedEvent(eventType, status.name());
    }

    /** Una transazione da accreditare: riferimento presso il fornitore, lordo, netto e valuta (UC 0071). */
    private record SettledTransaction(String paddleTransactionId, int amount, int netAmount, String currency) {}

    /**
     * Le transazioni riuscite del conto e dell'app che nessun accredito ha ancora citato — cioè il denaro
     * che il fornitore ci deve. Lo stub non le inventa: accredita quelle che ci sono, così l'accredito
     * simulato quadra davvero con lo storico invece di essere un numero scollegato.
     */
    private List<SettledTransaction> pendingNet(String tenantId, UUID appId) {
        String sql =
                """
                select t.paddle_transaction_id, t.amount, coalesce(t.net_amount, 0), t.currency
                  from platform.billing_transaction t
                 where t.tenant_id = ? and t.app_id = ? and t.deleted_at is null and t.status = 'paid'
                   and not exists (
                       select 1 from platform.payout_line l
                        where l.paddle_transaction_id = t.paddle_transaction_id)
                 order by t.billed_at
                """;
        List<SettledTransaction> out = new ArrayList<>();
        try (Connection c = ds.getConnection();
                PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, tenantId);
            ps.setObject(2, appId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new SettledTransaction(
                            rs.getString(1), rs.getInt(2), rs.getInt(3), rs.getString(4)));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("lettura delle transazioni da accreditare fallita", e);
        }
        if (out.isEmpty()) {
            throw new IllegalArgumentException(
                    "nessuna transazione da accreditare per questa app: esegui prima uno scenario che"
                            + " produca un pagamento (happy_path o renewal)");
        }
        return out;
    }

    /** L'ultimo pagamento riuscito del conto e dell'app: quello che lo scenario di rimborso restituisce. */
    private SettledTransaction lastPaid(String tenantId, UUID appId) {
        String sql =
                """
                select t.paddle_transaction_id, t.amount, coalesce(t.net_amount, 0), t.currency
                  from platform.billing_transaction t
                 where t.tenant_id = ? and t.app_id = ? and t.deleted_at is null and t.status = 'paid'
                 order by t.billed_at desc
                 limit 1
                """;
        try (Connection c = ds.getConnection();
                PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, tenantId);
            ps.setObject(2, appId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new SettledTransaction(
                            rs.getString(1), rs.getInt(2), rs.getInt(3), rs.getString(4));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("lettura dell'ultimo pagamento fallita", e);
        }
        throw new IllegalArgumentException(
                "nessun pagamento riuscito da rimborsare per questa app: esegui prima happy_path");
    }

    /** Emette un {@code payout.paid} che accredita le righe indicate; l'importo è la somma dei loro netti. */
    private EmittedEvent sendPayout(List<SettledTransaction> lines, Instant occurredAt) {
        ObjectNode root = mapper.createObjectNode();
        root.put("event_id", "evt_" + UUID.randomUUID().toString().replace("-", "").substring(0, 20));
        root.put("event_type", "payout.paid");
        root.put("occurred_at", occurredAt.toString());
        ObjectNode data = root.putObject("data");
        data.put("paddle_payout_id", "pay_" + UUID.randomUUID().toString().replace("-", "").substring(0, 24));
        data.put("paid_at", occurredAt.toString());
        int total = 0;
        String currency = "EUR";
        var array = data.putArray("lines");
        for (SettledTransaction line : lines) {
            ObjectNode node = array.addObject();
            node.put("paddle_transaction_id", line.paddleTransactionId());
            node.put("net_amount", line.netAmount());
            node.put("currency", line.currency());
            total += line.netAmount();
            currency = line.currency();
        }
        data.put("amount", total);
        data.put("currency", currency);
        send(root, occurredAt);
        return new EmittedEvent("payout.paid", null);
    }

    /**
     * Emette un {@code transaction.refunded} sulla <b>stessa</b> transazione: il riferimento presso il
     * fornitore è la chiave di idempotenza, quindi il rimborso cambia lo stato della riga esistente invece
     * di aggiungerne una nuova (decisione 6, change 0083).
     */
    private EmittedEvent sendRefund(
            SettledTransaction original, String tenantId, UUID appId, UUID appTierId, Instant occurredAt) {
        ObjectNode root = mapper.createObjectNode();
        root.put("event_id", "evt_" + UUID.randomUUID().toString().replace("-", "").substring(0, 20));
        root.put("event_type", "transaction.refunded");
        root.put("occurred_at", occurredAt.toString());
        ObjectNode data = root.putObject("data");
        ObjectNode custom = data.putObject("custom_data");
        custom.put("tenant_id", tenantId);
        custom.put("app_id", appId.toString());
        if (appTierId != null) {
            custom.put("app_tier_id", appTierId.toString());
        }
        putTransaction(data, appTierId, occurredAt);
        // Stesso riferimento e stesso importo della transazione originale: è un cambio di stato della riga
        // esistente, non un nuovo pagamento. Riscriverli qui evita che un listino nel frattempo cambiato
        // riscriva l'importo storico di un pagamento già avvenuto.
        data.put("paddle_transaction_id", original.paddleTransactionId());
        data.put("amount", original.amount());
        data.put("currency", original.currency());
        send(root, occurredAt);
        return new EmittedEvent("transaction.refunded", null);
    }

    /** Firma e immette nella stessa pipeline dei webhook reali. */
    private void send(ObjectNode root, Instant occurredAt) {
        String body;
        try {
            body = mapper.writeValueAsString(root);
        } catch (Exception e) {
            throw new IllegalStateException("Serializzazione webhook fallita", e);
        }
        ingest.ingest(body, signature.sign(body));
        remember(occurredAt);
    }

    /** Emette un evento {@code customer.*} (cattura {@code paddle_customer_id} su accounts). */
    private EmittedEvent sendCustomer(
            String eventType, String tenantId, String paddleCustomerId, Instant occurredAt) {
        ObjectNode root = mapper.createObjectNode();
        root.put("event_id", "evt_" + UUID.randomUUID().toString().replace("-", "").substring(0, 20));
        root.put("event_type", eventType);
        root.put("occurred_at", occurredAt.toString());
        ObjectNode data = root.putObject("data");
        data.put("paddle_customer_id", paddleCustomerId);
        data.putObject("custom_data").put("tenant_id", tenantId);
        String body;
        try {
            body = mapper.writeValueAsString(root);
        } catch (Exception e) {
            throw new IllegalStateException("Serializzazione webhook customer fallita", e);
        }
        ingest.ingest(body, signature.sign(body));
        return new EmittedEvent(eventType, null);
    }

    /** Serializza un evento nella forma attesa da {@link PaddleWebhookEvent}. */
    String build(
            String eventType, SubscriptionStatus status, String tenantId, UUID appId, UUID appTierId,
            String paddleSubId, Instant occurredAt, Instant periodStart, Instant periodEnd,
            Instant cancelAt, Instant trialEnd) {
        return build(eventType, status, tenantId, appId, appTierId, paddleSubId, occurredAt,
                periodStart, periodEnd, cancelAt, trialEnd, null, null);
    }

    /** Variante con cambio tier schedulato (downgrade a fine periodo, UC 0028). */
    String build(
            String eventType, SubscriptionStatus status, String tenantId, UUID appId, UUID appTierId,
            String paddleSubId, Instant occurredAt, Instant periodStart, Instant periodEnd,
            Instant cancelAt, Instant trialEnd, UUID scheduledTierId, Instant scheduledChangeAt) {
        ObjectNode root = mapper.createObjectNode();
        root.put("event_id", "evt_" + UUID.randomUUID().toString().replace("-", "").substring(0, 20));
        root.put("event_type", eventType);
        root.put("occurred_at", occurredAt.toString());
        ObjectNode data = root.putObject("data");
        data.put("paddle_subscription_id", paddleSubId);
        data.put("status", status.name());
        putInstant(data, "current_period_start", periodStart);
        putInstant(data, "current_period_end", periodEnd);
        putInstant(data, "cancel_at", cancelAt);
        putInstant(data, "trial_end", trialEnd);
        putInstant(data, "scheduled_change_at", scheduledChangeAt);
        ObjectNode custom = data.putObject("custom_data");
        custom.put("tenant_id", tenantId);
        custom.put("app_id", appId.toString());
        if (appTierId != null) {
            custom.put("app_tier_id", appTierId.toString());
        }
        if (scheduledTierId != null) {
            custom.put("scheduled_tier_id", scheduledTierId.toString());
        }
        if (eventType != null && eventType.startsWith("transaction.")) {
            putTransaction(data, appTierId, occurredAt);
        }
        try {
            return mapper.writeValueAsString(root);
        } catch (Exception e) {
            throw new IllegalStateException("Serializzazione webhook fallita", e);
        }
    }

    /**
     * Dati economici di una transazione (UC 0096), che nel mondo vero arrivano dal fornitore: qui li
     * ricaviamo dal <b>listino</b> della fascia in gioco, così che lo storico locale contenga gli importi
     * veri dell'app e non numeri inventati. Senza questi campi la pagina "Payments &amp; receipts"
     * resterebbe per sempre vuota in locale e non sarebbe verificabile né a mano né da un percorso
     * end-to-end.
     *
     * <p>Sta nello stub — cioè in dev/test — e non nel consumer: in produzione l'importo effettivamente
     * pagato lo sa solo chi ha incassato, e un backend che se lo calcolasse da sé scriverebbe un dato
     * falso nella pagina di fatturazione.
     *
     * <p>Il prezzo si legge in SQL nativo, non come entità: il ciclo di fatturazione qui è un'etichetta
     * da riportare, e una riga di listino con un valore fuori catalogo non deve far fallire l'emissione
     * (stessa ragione della vetrina, change 0076).
     */
    private void putTransaction(ObjectNode data, UUID appTierId, Instant occurredAt) {
        data.put(
                "paddle_transaction_id",
                "txn_" + UUID.randomUUID().toString().replace("-", "").substring(0, 24));
        data.put("billed_at", occurredAt.toString());
        // La ricevuta del fornitore: URL plausibile, mai raggiunto dai test (nessuna chiamata in uscita).
        data.put("receipt_url", "https://sandbox-my.paddle.com/receipts/" + UUID.randomUUID());
        int amount = 0;
        String currency = "EUR";
        String cycle = null;
        if (appTierId != null) {
            String sql =
                    """
                    select p.amount, p.currency, p.billing_cycle
                      from platform.app_price p
                     where p.app_tier_id = ? and p.deleted_at is null
                     order by case when p.billing_cycle = 'monthly' then 0 else 1 end, p.amount
                     limit 1
                    """;
            try (Connection c = ds.getConnection();
                    PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setObject(1, appTierId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        amount = rs.getInt(1);
                        currency = rs.getString(2);
                        cycle = rs.getString(3);
                    }
                }
            } catch (SQLException e) {
                throw new IllegalStateException("lettura del listino per la transazione finta fallita", e);
            }
        }
        data.put("amount", amount);
        data.put("currency", currency);
        if (cycle != null) {
            data.put("billing_cycle", cycle);
        }
    }

    private static void putInstant(ObjectNode node, String field, Instant value) {
        if (value == null) {
            node.putNull(field);
        } else {
            node.put(field, value.toString());
        }
    }

    private static UUID require(UUID value, String message) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }
}
