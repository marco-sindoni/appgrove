package app.appgrove.core.catalog;

import app.appgrove.core.billing.EntitlementAccess;
import app.appgrove.core.billing.Subscription;
import app.appgrove.core.billing.SubscriptionRepository;
import app.appgrove.core.catalog.CatalogDtos.CatalogAppView;
import app.appgrove.core.catalog.CatalogDtos.CatalogView;
import app.appgrove.core.catalog.CatalogDtos.StartingPrice;
import app.appgrove.core.catalog.PricingDefinition.AppDef;
import app.appgrove.core.platform.Account;
import app.appgrove.core.platform.AccountRepository;
import app.appgrove.core.platform.AccountStatus;
import app.appgrove.core.platform.CallerContext;
import io.agroal.api.AgroalDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Read-model della <b>vetrina per il tenant corrente</b> (UC 0095): il catalogo reale del backend, con
 * per ogni app lo stato che ha <b>per questo account</b>.
 *
 * <p><b>Perché esiste.</b> Fino a questa storia l'elenco delle app acquistabili lo costruiva il frontend
 * dai moduli che si trovava impacchettati dentro: un'app pubblicata a listino ma senza modulo non era
 * comprabile, e lo stato dell'app per l'account non si vedeva affatto. Questo è il rovescio della
 * medaglia di {@code /me/entitlements} (UC 0027), che elenca soltanto ciò a cui si ha accesso: qui si
 * elenca <b>tutto ciò che si può vedere</b>, accesso incluso o meno.
 *
 * <p><b>Che cosa entra nella vetrina</b> (decisione 6, change 0076): le app pubblicate ({@code active})
 * più le app spente dalla piattaforma per cui questo account ha un abbonamento non cancellato — chi paga
 * deve poter leggere perché non può usarla. Un'app spenta e mai sottoscritta resta invisibile: non si fa
 * vetrina di ciò che la piattaforma ha deciso di non vendere.
 *
 * <p><b>Coerenza per costruzione</b>: lo stato lo decide {@link CatalogAppState#derive}, che a sua volta
 * consuma {@link EntitlementAccess} (regola unica di accesso, UC 0077) e il ciclo di vita
 * dell'abbonamento (UC 0026). Qui non si ri-deriva nulla a mano.
 *
 * <p><b>Parte descrittiva</b> (categoria e descrizione nelle 5 lingue): viene dal pricing-as-code letto
 * in-processo, non dal database — è contenuto di presentazione env-agnostico e non c'è ragione di
 * duplicarlo in una colonna e in un ramo di sincronizzazione. Un'app presente nel database ma non più
 * nella definizione resta presentabile, senza descrizione.
 *
 * <p>Invarianti: gli abbonamenti sono letti tenant-scoped (discriminator dal JWT verificato, #1/#2); il
 * catalogo è dato di piattaforma. Nessun parametro consente al chiamante di indicare un account.
 */
@ApplicationScoped
public class CatalogReadModel {

    @Inject
    AppRepository apps;

    @Inject
    AppTierRepository tiers;

    @Inject
    SubscriptionRepository subscriptions;

    @Inject
    AccountRepository accounts;

    @Inject
    CallerContext caller;

    @Inject
    PricingCatalogLoader pricing;

    @Inject
    AgroalDataSource ds;

    /** La vetrina del tenant del JWT, in ordine alfabetico di nome (stabile fra due letture). */
    public CatalogView forCurrentTenant() {
        Account account = accounts.findById(caller.tenantId());
        AccountStatus accountStatus = account != null ? account.getStatus() : null;

        Map<UUID, Subscription> byApp = new HashMap<>();
        for (Subscription s : subscriptions.listAll()) {
            byApp.put(s.getAppId(), s);
        }
        Map<String, AppDef> definitions = definitionsBySlug();
        Map<UUID, StartingPrice> startingPrices = startingPrices();
        Map<UUID, String> freeTiers = freeTierNames();

        List<CatalogAppView> view = new ArrayList<>();
        // Solo le APPLICAZIONI (UC 0103): la vetrina è l'elenco di ciò che si può comprare e aprire, e la
        // voce di piattaforma dei posti non è né l'uno né l'altro — i posti si comprano invitando una
        // persona, non da una card del catalogo. Nota che l'esclusione va fatta PRIMA della condizione
        // sull'abbonamento qui sotto: un account che ha comprato dei posti ha un abbonamento su quella
        // voce, quindi la regola «spenta e mai sottoscritta resta invisibile» non basterebbe a nasconderla.
        for (App app : apps.listApplications()) {
            Subscription sub = byApp.get(app.getId());
            if (app.getStatus() != AppStatus.active && sub == null) {
                continue; // spenta e mai sottoscritta: fuori dalla vetrina
            }

            String freeTierName = sub == null ? freeTiers.get(app.getId()) : null;
            boolean accessGranted = EntitlementAccess.granted(
                    accountStatus, app.getStatus(), sub != null ? sub.getStatus() : null, freeTierName != null);
            CatalogAppState state = CatalogAppState.derive(
                    app.getStatus(), sub != null ? sub.getStatus() : null, sub != null ? sub.getCancelAt() : null,
                    accessGranted);

            // Via all'acquisto per un'app freemium (UC 0096, punto aperto lasciato da UC 0095): l'app è
            // usabile grazie alla sola fascia gratuita, quindi la card dice "attiva" — ma un piano a
            // pagamento esiste e finora non c'era modo di comprarlo da nessuna parte, perché in Billing
            // non c'è alcuna card di abbonamento su cui agire.
            boolean upgradeAvailable = state == CatalogAppState.active
                    && sub == null
                    && startingPrices.containsKey(app.getId());

            AppDef def = definitions.get(app.getSlug());
            view.add(new CatalogAppView(
                    app.getSlug(),
                    app.getName(),
                    def != null ? def.category() : null,
                    def != null ? def.descriptions() : null,
                    state,
                    planName(sub, freeTierName),
                    state == CatalogAppState.trial && sub != null ? sub.getTrialEnd() : null,
                    state == CatalogAppState.cancellation_scheduled && sub != null ? sub.getCancelAt() : null,
                    startingPrices.get(app.getId()),
                    upgradeAvailable));
        }
        view.sort(Comparator.comparing(CatalogAppView::name, String.CASE_INSENSITIVE_ORDER));
        return new CatalogView(view);
    }

    /**
     * Prezzo di partenza per app: l'importo più basso fra i price vivi; a parità di importo vince il ciclo
     * mensile, perché è quello che la card annuncia ("from €N/mo"). Un'app con sole fasce gratuite non
     * compare nella mappa — la card dirà "gratis", non "€0": non sono la stessa promessa.
     *
     * <p><b>Perché in SQL nativo e non con le entità.</b> Due ragioni, entrambe concrete. La prima è il
     * costo: una vetrina di venti app con tre fasce l'una farebbe decine di letture; qui è una sola.
     * La seconda è la <b>robustezza</b>, ed è quella che conta di più: il ciclo di fatturazione qui è
     * un'<b>etichetta</b> da mostrare, non un valore su cui decidere. Caricarlo come enum farebbe
     * fallire l'intera pagina per una singola riga con un valore fuori catalogo — un dato storico, una
     * riga scritta da un'integrazione — mentre la vetrina deve degradare, non spegnersi.
     */
    private Map<UUID, StartingPrice> startingPrices() {
        Map<UUID, StartingPrice> cheapest = new HashMap<>();
        String sql =
                """
                select t.app_id, p.amount, p.currency, p.billing_cycle
                  from platform.app_price p
                  join platform.app_tier t on t.id = p.app_tier_id
                 where p.deleted_at is null and t.deleted_at is null
                 order by t.app_id,
                          p.amount asc,
                          case when p.billing_cycle = 'monthly' then 0 else 1 end
                """;
        try (Connection c = ds.getConnection();
                PreparedStatement ps = c.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                UUID appId = rs.getObject(1, UUID.class);
                // L'ordinamento mette per primo il migliore di ogni app: le righe successive si scartano.
                if (!cheapest.containsKey(appId)) {
                    cheapest.put(appId, new StartingPrice(rs.getInt(2), rs.getString(3), rs.getString(4)));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("lettura dei prezzi di partenza del catalogo fallita", e);
        }
        return cheapest;
    }

    /**
     * Nome della fascia gratuita di baseline per app: la prima fascia <b>senza alcun prezzo vivo</b>
     * (stessa definizione di UC 0027). Una sola lettura per tutta la vetrina, e senza toccare le entità
     * dei price — vale la stessa ragione di robustezza spiegata sopra.
     */
    private Map<UUID, String> freeTierNames() {
        Map<UUID, String> byApp = new HashMap<>();
        String sql =
                """
                select t.app_id, t.name
                  from platform.app_tier t
                 where t.deleted_at is null
                   and not exists (select 1 from platform.app_price p
                                    where p.app_tier_id = t.id and p.deleted_at is null)
                 order by t.app_id, t.created_at, t.id
                """;
        try (Connection c = ds.getConnection();
                PreparedStatement ps = c.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                UUID appId = rs.getObject(1, UUID.class);
                String name = rs.getString(2);
                byApp.putIfAbsent(appId, name);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("lettura delle fasce gratuite del catalogo fallita", e);
        }
        return byApp;
    }

    /** Nome della fascia in corso: quella dell'abbonamento, o la gratuita di baseline quando è lei ad agire. */
    private String planName(Subscription sub, String freeTierName) {
        if (sub != null && sub.getAppTierId() != null) {
            AppTier tier = tiers.findById(sub.getAppTierId());
            return tier != null ? tier.getName() : null;
        }
        return freeTierName;
    }

    private Map<String, AppDef> definitionsBySlug() {
        Map<String, AppDef> bySlug = new HashMap<>();
        for (AppDef def : pricing.load()) {
            bySlug.put(def.slug(), def);
        }
        return bySlug;
    }
}
