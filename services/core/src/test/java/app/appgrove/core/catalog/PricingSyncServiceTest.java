package app.appgrove.core.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.appgrove.core.catalog.PricingDefinition.AppDef;
import app.appgrove.core.catalog.PricingDefinition.PriceDef;
import app.appgrove.core.catalog.PricingDefinition.TierDef;
import io.agroal.api.AgroalDataSource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Motore di sync pricing-as-code (UC 0022), esercitato <b>offline</b> contro lo stub (#09 I39). Usa app
 * sintetiche ({@code uc22-*}) per non interferire col catalogo reale sincronizzato allo startup. Copre i
 * "Requisiti di test" della UC: idempotenza, mappa chiave→ID env, immutabilità, grandfathering, archiviazione.
 */
@QuarkusTest
class PricingSyncServiceTest {

    @Inject
    PricingSyncService sync;

    @Inject
    AppPriceRepository priceRepo;

    @Inject
    AgroalDataSource ds;

    private static AppDef app(String slug, List<TierDef> tiers) {
        return new AppDef(slug, slug, AppUserModel.single_user, AppStatus.active, tiers);
    }

    private static TierDef tier(String key, List<PriceDef> prices) {
        return new TierDef(key, key, 0, Map.of("metric", "x", "cap", 1), Map.of(), prices);
    }

    private static PriceDef price(long amount) {
        return new PriceDef(BillingCycle.monthly, amount, "EUR");
    }

    @Test
    void idempotentSyncFillsStablePaddleIdsAndMapsKeyToTier() {
        List<AppDef> defs = List.of(app("uc22-idem", List.of(tier("pro", List.of(price(1500))))));
        sync.sync(defs);

        UUID priceId = CatalogIds.priceId("uc22-idem", "pro", "monthly");
        String paddleId1 = paddlePriceId(priceId);
        assertNotNull(paddleId1, "la sync riempie paddle_price_id (via stub)");
        assertTrue(paddleId1.startsWith("pri_"), "ID Paddle plausibile dallo stub");

        // mappa chiave → ID ambiente: dal paddle_price_id risalgo al tier (come in prod). Lookup in SQL
        // nativo → invocabile anche fuori da una richiesta (consumer webhook, UC 0025).
        UUID resolvedTier = priceRepo.findTierIdByPaddlePriceId(paddleId1).orElseThrow();
        assertEquals(CatalogIds.tierId("uc22-idem", "pro"), resolvedTier);

        // ri-sync: idempotente (stesso ID, nessun duplicato)
        sync.sync(defs);
        assertEquals(paddleId1, paddlePriceId(priceId), "ID Paddle stabile tra ri-sync");
        assertEquals(1, count("select count(*) from platform.app where slug = 'uc22-idem' and deleted_at is null"));
        assertEquals(1, count("select count(*) from platform.app_price where id = '" + priceId
                + "' and deleted_at is null"));
    }

    @Test
    void rejectsAmountChangeOnLivePrice() {
        List<AppDef> at900 = List.of(app("uc22-imm", List.of(tier("pro", List.of(price(900))))));
        sync.sync(at900);
        UUID priceId = CatalogIds.priceId("uc22-imm", "pro", "monthly");
        assertNotNull(paddlePriceId(priceId), "il price è vivo (sincronizzato su Paddle)");

        List<AppDef> at1900 = List.of(app("uc22-imm", List.of(tier("pro", List.of(price(1900))))));
        assertThrows(PricingSyncService.ImmutablePriceException.class, () -> sync.sync(at1900));
        assertEquals(900, amount(priceId), "l'importo del price vivo resta invariato (rollback)");
    }

    @Test
    void grandfathersRemovedPriceWhenTierHasActiveSubscription() {
        List<AppDef> withPrice = List.of(app("uc22-gf", List.of(tier("pro", List.of(price(1000))))));
        sync.sync(withPrice);

        UUID priceId = CatalogIds.priceId("uc22-gf", "pro", "monthly");
        insertActiveSubscription("tenant-gf", CatalogIds.appId("uc22-gf"), CatalogIds.tierId("uc22-gf", "pro"));

        List<AppDef> withoutPrice = List.of(app("uc22-gf", List.of(tier("pro", List.of()))));
        sync.sync(withoutPrice);
        assertFalse(isArchived(priceId), "grandfathering: price NON archiviato (tier con subscription attiva)");
    }

    @Test
    void archivesRemovedPriceWithoutActiveSubscription() {
        List<AppDef> withPrice = List.of(app("uc22-arch", List.of(tier("pro", List.of(price(1000))))));
        sync.sync(withPrice);
        UUID priceId = CatalogIds.priceId("uc22-arch", "pro", "monthly");

        List<AppDef> withoutPrice = List.of(app("uc22-arch", List.of(tier("pro", List.of()))));
        sync.sync(withoutPrice);
        assertTrue(isArchived(priceId), "price rimosso e senza subscription → archiviato (soft-delete)");
    }

    // ── helper JDBC ─────────────────────────────────────────────────────────────

    private void insertActiveSubscription(String tenantId, UUID appId, UUID tierId) {
        exec(
                "insert into platform.subscription(id, tenant_id, app_id, app_tier_id, status, created_at, updated_at)"
                        + " values (?, ?, ?, ?, 'active', ?, ?)",
                UUID.randomUUID(), tenantId, appId, tierId, OffsetDateTime.now(), OffsetDateTime.now());
    }

    private String paddlePriceId(UUID id) {
        return queryString("select paddle_price_id from platform.app_price where id = ?", id);
    }

    private long amount(UUID id) {
        return count("select amount from platform.app_price where id = '" + id + "'");
    }

    private boolean isArchived(UUID id) {
        return count("select case when deleted_at is null then 0 else 1 end from platform.app_price where id = '"
                + id + "'") == 1;
    }

    private long count(String sql) {
        try (Connection c = ds.getConnection();
                PreparedStatement ps = c.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getLong(1);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String queryString(String sql, UUID param) {
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void exec(String sql, Object... params) {
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
