package app.appgrove.core.catalog;

import app.appgrove.core.billing.PaymentProvider;
import app.appgrove.core.billing.PaymentProvider.PriceSync;
import app.appgrove.core.billing.PaymentProvider.PricingSyncRequest;
import app.appgrove.core.billing.PaymentProvider.PricingSyncResult;
import app.appgrove.core.billing.PaymentProvider.ProductSync;
import app.appgrove.core.catalog.PricingDefinition.AppDef;
import app.appgrove.core.catalog.PricingDefinition.PriceDef;
import app.appgrove.core.catalog.PricingDefinition.TierDef;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.agroal.api.AgroalDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.jboss.logging.Logger;

/**
 * Motore della <b>sync pricing-as-code</b> (UC 0022, #09 H37). Riconcilia, in modo <b>idempotente</b>:
 * <ol>
 *   <li><b>carica</b> la definizione dagli YAML (loader);</li>
 *   <li><b>upsert</b> di {@code app}/{@code app_tier}/{@code app_price} nel catalogo DB dell'ambiente, con
 *       ID <b>deterministici</b> dalla chiave ({@link CatalogIds}) — campi env-agnostici (importi/limiti/feature);</li>
 *   <li><b>archivia</b> i rimossi (soft-delete) rispettando il <b>grandfathering</b> (un price/tier con
 *       subscription attiva non si archivia) e <b>rifiuta</b> di mutare l'importo di un price <b>vivo</b>
 *       (già con {@code paddle_price_id}); </li>
 *   <li><b>riconcilia con Paddle</b> via il port {@link PaymentProvider} (stub in dev/test) e scrive gli ID
 *       Paddle per-ambiente nel catalogo.</li>
 * </ol>
 *
 * <p>Scritture in <b>SQL nativo</b> con ID espliciti deterministici (stesso pattern di {@code SubscriptionWriter}:
 * opera fuori da una richiesta autenticata, niente {@code TenantResolver}; il catalogo è platform-level, non
 * tenant-scoped). L'archiviazione è <b>scoped per-app</b>: rimuovere un price/tier dallo YAML di un'app lo
 * archivia; la rimozione di un'<b>intera app</b> è fuori scope qui (tracciata nei Punti aperti di UC 0022).
 */
@ApplicationScoped
public class PricingSyncService {

    private static final Logger LOG = Logger.getLogger(PricingSyncService.class);

    private static final Set<String> ACTIVE_SUB_STATUSES = Set.of("active", "trialing", "past_due");

    private static final String UPSERT_APP =
            """
            insert into platform.app
              (id, slug, name, user_model, status, paddle_product_id, created_at, updated_at, created_by, updated_by)
            values (?, ?, ?, ?, ?, null, now(), now(), 'sync', 'sync')
            on conflict (id) do update set
              slug = excluded.slug, name = excluded.name, user_model = excluded.user_model,
              status = excluded.status, updated_at = now(), updated_by = 'sync', deleted_at = null
            """;

    private static final String UPSERT_TIER =
            """
            insert into platform.app_tier
              (id, app_id, key, name, limits, features, trial_days, created_at, updated_at, created_by, updated_by)
            values (?, ?, ?, ?, ?::jsonb, ?::jsonb, ?, now(), now(), 'sync', 'sync')
            on conflict (id) do update set
              app_id = excluded.app_id, key = excluded.key, name = excluded.name,
              limits = excluded.limits, features = excluded.features, trial_days = excluded.trial_days,
              updated_at = now(), updated_by = 'sync', deleted_at = null
            """;

    private static final String UPSERT_PRICE =
            """
            insert into platform.app_price
              (id, app_tier_id, billing_cycle, paddle_price_id, amount, currency, created_at, updated_at, created_by, updated_by)
            values (?, ?, ?, null, ?, ?, now(), now(), 'sync', 'sync')
            on conflict (id) do update set
              app_tier_id = excluded.app_tier_id, billing_cycle = excluded.billing_cycle,
              amount = excluded.amount, currency = excluded.currency,
              updated_at = now(), updated_by = 'sync', deleted_at = null
            """;

    @Inject
    AgroalDataSource ds;

    @Inject
    PaymentProvider provider;

    @Inject
    PricingCatalogLoader loader;

    @Inject
    ObjectMapper json;

    /** Esito sintetico della sync (per logging/entrypoint). */
    public record Report(int apps, int tiers, int prices, int archived) {}

    /** Carica il pricing-as-code dagli YAML e sincronizza. */
    public Report sync() {
        return sync(loader.load());
    }

    /** Sincronizza la definizione fornita (usato dai test con cataloghi sintetici). */
    @Transactional
    public Report sync(List<AppDef> defs) {
        int tiers = 0;
        int prices = 0;
        int archived = 0;
        try (Connection c = ds.getConnection()) {
            for (AppDef app : defs) {
                upsertApp(c, app);
                Set<UUID> desiredTierIds = new HashSet<>();
                Set<UUID> desiredPriceIds = new HashSet<>();
                for (TierDef tier : app.tiers()) {
                    UUID tierId = CatalogIds.tierId(app.slug(), tier.key());
                    desiredTierIds.add(tierId);
                    upsertTier(c, tierId, app, tier);
                    tiers++;
                    for (PriceDef price : tier.prices()) {
                        UUID priceId = CatalogIds.priceId(app.slug(), tier.key(), price.billingCycle().name());
                        desiredPriceIds.add(priceId);
                        guardImmutability(c, priceId, price.amount());
                        upsertPrice(c, priceId, tierId, price);
                        prices++;
                    }
                }
                archived += archiveRemoved(c, app.slug(), desiredTierIds, desiredPriceIds);
            }
            reconcilePaddle(c, defs);
            LOG.infof(
                    "pricing.sync apps=%d tiers=%d prices=%d archived=%d", defs.size(), tiers, prices, archived);
            return new Report(defs.size(), tiers, prices, archived);
        } catch (SQLException e) {
            throw new IllegalStateException("sync pricing-as-code fallita", e);
        }
    }

    // ── upsert ────────────────────────────────────────────────────────────────

    private void upsertApp(Connection c, AppDef app) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(UPSERT_APP)) {
            ps.setObject(1, CatalogIds.appId(app.slug()));
            ps.setString(2, app.slug());
            ps.setString(3, app.name());
            ps.setString(4, app.userModel().name());
            ps.setString(5, app.status().name());
            ps.executeUpdate();
        }
    }

    private void upsertTier(Connection c, UUID tierId, AppDef app, TierDef tier) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(UPSERT_TIER)) {
            ps.setObject(1, tierId);
            ps.setObject(2, CatalogIds.appId(app.slug()));
            ps.setString(3, tier.key());
            ps.setString(4, tier.name());
            ps.setString(5, jsonb(tier.limits()));
            ps.setString(6, jsonb(tier.features()));
            ps.setInt(7, tier.trialDays());
            ps.executeUpdate();
        }
    }

    private void upsertPrice(Connection c, UUID priceId, UUID tierId, PriceDef price) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(UPSERT_PRICE)) {
            ps.setObject(1, priceId);
            ps.setObject(2, tierId);
            ps.setString(3, price.billingCycle().name());
            ps.setLong(4, price.amount());
            ps.setString(5, price.currency());
            ps.executeUpdate();
        }
    }

    /**
     * Immutabilità (#09 H37): un price <b>vivo</b> (già pubblicato su Paddle → {@code paddle_price_id} valorizzato)
     * non può cambiare importo. Il cambio prezzo è "nuovo Price + archivia il vecchio", gestito da {@code pricing-change}
     * (UC 0047). Un price non ancora sincronizzato (draft, senza ID) può invece variare.
     */
    private void guardImmutability(Connection c, UUID priceId, long desiredAmount) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "select amount, paddle_price_id from platform.app_price where id = ? and deleted_at is null")) {
            ps.setObject(1, priceId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    long currentAmount = rs.getLong("amount");
                    String paddlePriceId = rs.getString("paddle_price_id");
                    if (paddlePriceId != null && currentAmount != desiredAmount) {
                        throw new ImmutablePriceException(priceId, currentAmount, desiredAmount);
                    }
                }
            }
        }
    }

    // ── archiviazione dei rimossi (grandfathering) ──────────────────────────────

    private int archiveRemoved(Connection c, String slug, Set<UUID> desiredTierIds, Set<UUID> desiredPriceIds)
            throws SQLException {
        UUID appId = CatalogIds.appId(slug);
        int archived = 0;
        // price rimossi (tier ancora esistente ma price non più desiderato)
        for (UUID tierId : currentTierIds(c, appId)) {
            for (UUID priceId : currentPriceIds(c, tierId)) {
                if (!desiredPriceIds.contains(priceId) && !hasActiveSubscription(c, tierId)) {
                    archived += archiveRow(c, "platform.app_price", priceId);
                }
            }
            // tier interamente rimosso → archivia tier + i suoi price (se non grandfathered)
            if (!desiredTierIds.contains(tierId) && !hasActiveSubscription(c, tierId)) {
                for (UUID priceId : currentPriceIds(c, tierId)) {
                    archived += archiveRow(c, "platform.app_price", priceId);
                }
                archived += archiveRow(c, "platform.app_tier", tierId);
            }
        }
        return archived;
    }

    private boolean hasActiveSubscription(Connection c, UUID tierId) throws SQLException {
        String inClause = "?, ?, ?";
        try (PreparedStatement ps = c.prepareStatement(
                "select count(*) from platform.subscription where app_tier_id = ? and deleted_at is null"
                        + " and status in (" + inClause + ")")) {
            ps.setObject(1, tierId);
            int i = 2;
            for (String status : ACTIVE_SUB_STATUSES) {
                ps.setString(i++, status);
            }
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1) > 0;
            }
        }
    }

    private int archiveRow(Connection c, String table, UUID id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "update " + table + " set deleted_at = now(), updated_at = now(), updated_by = 'sync'"
                        + " where id = ? and deleted_at is null")) {
            ps.setObject(1, id);
            return ps.executeUpdate();
        }
    }

    // ── riconciliazione Paddle (via port) ───────────────────────────────────────

    private void reconcilePaddle(Connection c, List<AppDef> defs) throws SQLException {
        List<ProductSync> products = new ArrayList<>();
        for (AppDef app : defs) {
            UUID appId = CatalogIds.appId(app.slug());
            List<PriceSync> prices = new ArrayList<>();
            for (TierDef tier : app.tiers()) {
                for (PriceDef price : tier.prices()) {
                    UUID priceId = CatalogIds.priceId(app.slug(), tier.key(), price.billingCycle().name());
                    prices.add(new PriceSync(
                            priceKey(app.slug(), tier.key(), price.billingCycle().name()),
                            price.amount(),
                            price.currency(),
                            price.billingCycle().name(),
                            currentPaddleId(c, "platform.app_price", "paddle_price_id", priceId)));
                }
            }
            products.add(new ProductSync(
                    app.slug(),
                    app.name(),
                    currentPaddleId(c, "platform.app", "paddle_product_id", appId),
                    prices));
        }

        PricingSyncResult result = provider.syncPricing(new PricingSyncRequest(products));

        for (AppDef app : defs) {
            String productId = result.productIdByKey().get(app.slug());
            if (productId != null) {
                writePaddleId(c, "platform.app", "paddle_product_id", CatalogIds.appId(app.slug()), productId);
            }
            for (TierDef tier : app.tiers()) {
                for (PriceDef price : tier.prices()) {
                    String key = priceKey(app.slug(), tier.key(), price.billingCycle().name());
                    String priceId = result.priceIdByKey().get(key);
                    if (priceId != null) {
                        writePaddleId(
                                c,
                                "platform.app_price",
                                "paddle_price_id",
                                CatalogIds.priceId(app.slug(), tier.key(), price.billingCycle().name()),
                                priceId);
                    }
                }
            }
        }
    }

    private void writePaddleId(Connection c, String table, String column, UUID id, String paddleId)
            throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "update " + table + " set " + column + " = ?, updated_at = now(), updated_by = 'sync'"
                        + " where id = ? and " + column + " is distinct from ?")) {
            ps.setString(1, paddleId);
            ps.setObject(2, id);
            ps.setString(3, paddleId);
            ps.executeUpdate();
        }
    }

    // ── helper di lettura ───────────────────────────────────────────────────────

    private List<UUID> currentTierIds(Connection c, UUID appId) throws SQLException {
        return idList(c, "select id from platform.app_tier where app_id = ? and deleted_at is null", appId);
    }

    private List<UUID> currentPriceIds(Connection c, UUID tierId) throws SQLException {
        return idList(c, "select id from platform.app_price where app_tier_id = ? and deleted_at is null", tierId);
    }

    private List<UUID> idList(Connection c, String sql, UUID param) throws SQLException {
        List<UUID> ids = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getObject(1, UUID.class));
                }
            }
        }
        return ids;
    }

    private String currentPaddleId(Connection c, String table, String column, UUID id) throws SQLException {
        try (PreparedStatement ps =
                c.prepareStatement("select " + column + " from " + table + " where id = ? and deleted_at is null")) {
            ps.setObject(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }

    private static String priceKey(String slug, String tierKey, String cycle) {
        return slug + ":" + tierKey + ":" + cycle;
    }

    private String jsonb(Map<String, Object> value) {
        if (value == null) {
            return null;
        }
        try {
            return json.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("serializzazione JSON del catalogo fallita", e);
        }
    }

    /** Violazione di immutabilità prezzi: tentata mutazione dell'importo di un price vivo (#09 H37). */
    public static class ImmutablePriceException extends RuntimeException {
        public ImmutablePriceException(UUID priceId, long currentAmount, long desiredAmount) {
            super("immutabilità prezzi: il price " + priceId + " è vivo (sincronizzato su Paddle), non si può"
                    + " mutare l'importo da " + currentAmount + " a " + desiredAmount
                    + " — usare 'nuovo Price + archivia' (pricing-change, UC 0047)");
        }
    }
}
