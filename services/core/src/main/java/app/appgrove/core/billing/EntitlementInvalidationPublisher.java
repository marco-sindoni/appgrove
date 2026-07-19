package app.appgrove.core.billing;

import app.appgrove.commons.entitlement.EntitlementEvents;
import app.appgrove.commons.messaging.MessageQueues;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.agroal.api.AgroalDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.jboss.logging.Logger;

/**
 * Pubblica sul bus interno l'invalidazione degli entitlement quando lo stato di billing di un tenant
 * cambia (UC 0046): una riga sulla coda {@code entitlement-<slug>} dell'app interessata.
 *
 * <p><b>Perché l'evento è sottile.</b> Non trasporta i diritti calcolati: dire "i diritti del tenant
 * T sono cambiati" basta all'app per marcare la propria proiezione e rinfrescarla alla prima
 * richiesta utile. Pubblicare i diritti calcolati costringerebbe core a ri-derivare l'entitlement
 * <b>fuori</b> da una richiesta autenticata — duplicando la logica di {@link EntitlementReadModel} e
 * aggirando il filtro per tenant di Hibernate. Il rischio di due derivazioni che divergono nel tempo
 * è molto peggiore del costo di una chiamata di rinfresco.
 *
 * <p>Gira <b>fuori</b> da una richiesta autenticata (consumer webhook): niente JWT, quindi SQL nativo
 * con {@code tenant_id} esplicito preso dallo stato di core, mai da input client (stessa postura di
 * {@link SubscriptionWriter}).
 *
 * <p><b>La pubblicazione non è mai bloccante.</b> Un errore sul bus viene loggato e ingoiato: la
 * mutazione di billing è già stata applicata e non va annullata perché una notifica non è partita.
 * La conseguenza — proiezioni che restano vecchie — è coperta dalle misure di scostamento lato app,
 * che è il posto giusto per accorgersene.
 */
@ApplicationScoped
public class EntitlementInvalidationPublisher {

    private static final Logger LOG = Logger.getLogger(EntitlementInvalidationPublisher.class);

    private static final String SLUG_BY_ID =
            "select slug from platform.app where id = ? and deleted_at is null";

    private static final String ACTIVE_SLUGS =
            "select slug from platform.app where status = 'active' and deleted_at is null";

    @Inject
    AgroalDataSource ds;

    // Instance<> lazy: nei test senza code il publisher resta inerte invece di far fallire il boot.
    @Inject
    Instance<MessageQueues> queuesInstance;

    @Inject
    ObjectMapper mapper;

    /**
     * Invalida i diritti del tenant sull'app indicata (per UUID di catalogo). Usato dopo l'applicazione
     * di un evento di billing che ha davvero modificato la subscription.
     */
    public void invalidate(String tenantId, UUID appId, String reason) {
        if (tenantId == null || tenantId.isBlank() || appId == null) {
            return;
        }
        String slug = slugOf(appId);
        if (slug == null) {
            LOG.warnf("entitlement.invalidation app_id=%s senza slug di catalogo → nessuna pubblicazione", appId);
            return;
        }
        publish(tenantId, slug, reason);
    }

    /**
     * Invalida i diritti del tenant su <b>tutte</b> le app attive: serve per i cambiamenti che non
     * riguardano una singola subscription ma l'account nel suo insieme (es. account messo in attesa di
     * eliminazione, UC 0033, che azzera ogni entitlement).
     */
    public void invalidateAllApps(String tenantId, String reason) {
        if (tenantId == null || tenantId.isBlank()) {
            return;
        }
        for (String slug : activeSlugs()) {
            publish(tenantId, slug, reason);
        }
    }

    private void publish(String tenantId, String slug, String reason) {
        if (queuesInstance.isUnsatisfied()) {
            return;
        }
        String queue = EntitlementEvents.invalidationQueue(slug);
        try {
            String body = mapper.writeValueAsString(new EntitlementEvents.InvalidationMessage(
                    tenantId, reason, Instant.now().toString()));
            queuesInstance.get().send(queue, body);
            LOG.infof("entitlement.invalidation pubblicata tenant_id=%s app_id=%s reason=%s", tenantId, slug, reason);
        } catch (Exception e) {
            // Non rilanciare: vedi nota di classe. La proiezione resterà vecchia e le misure di
            // scostamento lato app lo renderanno visibile.
            LOG.errorf(
                    e,
                    "entitlement.invalidation pubblicazione fallita tenant_id=%s app_id=%s reason=%s"
                            + " → la proiezione dell'app resterà vecchia fino al prossimo evento",
                    tenantId, slug, reason);
        }
    }

    private String slugOf(UUID appId) {
        try (Connection c = ds.getConnection();
                PreparedStatement ps = c.prepareStatement(SLUG_BY_ID)) {
            ps.setObject(1, appId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        } catch (SQLException e) {
            LOG.errorf(e, "entitlement.invalidation risoluzione slug fallita app_id=%s", appId);
            return null;
        }
    }

    private List<String> activeSlugs() {
        List<String> slugs = new ArrayList<>();
        try (Connection c = ds.getConnection();
                PreparedStatement ps = c.prepareStatement(ACTIVE_SLUGS);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                slugs.add(rs.getString(1));
            }
        } catch (SQLException e) {
            LOG.errorf(e, "entitlement.invalidation elenco app attive non leggibile → nessuna pubblicazione");
        }
        return slugs;
    }
}
