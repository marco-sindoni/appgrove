package app.appgrove.core.catalog;

import io.agroal.api.AgroalDataSource;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Repository dei price di catalogo. */
@ApplicationScoped
public class AppPriceRepository implements PanacheRepositoryBase<AppPrice, UUID> {

    @Inject
    AgroalDataSource ds;

    /** Letture in-richiesta (entità JPA, soggette al persistence-unit multitenant). */
    public List<AppPrice> listByTier(UUID appTierId) {
        return list("appTierId", appTierId);
    }

    /**
     * La fascia ha almeno un price vivo? È la domanda del <b>tier free di baseline</b> (UC 0027): interessa
     * solo <i>se</i> un prezzo esiste, non quale sia.
     *
     * <p>Conta invece di caricare, e la differenza non è di prestazioni ma di <b>robustezza</b>: caricando
     * le entità, una singola riga con un {@code billing_cycle} fuori dai valori dell'enum — un dato storico,
     * una riga scritta da un'integrazione — farebbe fallire l'intera lettura degli entitlement, cioè il
     * percorso che governa l'accesso. Un conteggio non converte nulla e non può rompersi così (change 0076).
     */
    public boolean existsForTier(UUID appTierId) {
        return count("appTierId", appTierId) > 0;
    }

    /**
     * Risolve il <b>tier</b> dal {@code paddle_price_id} — il mapping "come in prod" abilitato da UC 0022
     * (sbloccava il punto differito di UC 0023: lo stub passava l'{@code app_tier_id} esplicito perché
     * mancavano le entità di catalogo).
     *
     * <p>Implementato in <b>SQL nativo</b> (non Panache): il consumatore reale è la <b>pipeline webhook</b>
     * (UC 0025), che gira <b>fuori da una richiesta autenticata</b> → niente JWT → il {@code TenantResolver}
     * (DISCRIMINATOR) è fail-closed e una query Hibernate fallirebbe. Stesso pattern di {@code SubscriptionWriter}.
     * Il catalogo è platform-level (nessun {@code tenant_id} da filtrare).
     */
    public Optional<UUID> findTierIdByPaddlePriceId(String paddlePriceId) {
        try (Connection c = ds.getConnection();
                PreparedStatement ps = c.prepareStatement(
                        "select app_tier_id from platform.app_price where paddle_price_id = ? and deleted_at is null")) {
            ps.setString(1, paddlePriceId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(rs.getObject(1, UUID.class)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("lookup tier da paddle_price_id fallito", e);
        }
    }
}
