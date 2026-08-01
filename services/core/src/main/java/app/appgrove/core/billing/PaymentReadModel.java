package app.appgrove.core.billing;

import app.appgrove.core.billing.PaymentDtos.PaymentView;
import app.appgrove.core.billing.PaymentDtos.PaymentsView;
import app.appgrove.core.catalog.App;
import app.appgrove.core.catalog.AppRepository;
import app.appgrove.core.catalog.AppTier;
import app.appgrove.core.catalog.AppTierRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;

/**
 * Read-model dello <b>storico pagamenti</b> del conto corrente (UC 0096): la sezione "Payments &amp;
 * receipts" della pagina Billing.
 *
 * <p><b>Completezza prima di tutto</b>: entrano <b>tutte</b> le transazioni del conto, comprese quelle
 * fallite e contestate. Un pagamento fallito è esattamente ciò che l'utente deve poter vedere, ed è la
 * riga da cui parte per rimettere a posto il metodo di pagamento.
 *
 * <p><b>Una riga sopravvive al suo contorno</b>: se l'app o la fascia pagata non sono più a catalogo la
 * riga resta, con il nome mancante al posto di un'eccezione. Uno storico di fatturazione che sparisce
 * perché il listino è cambiato sarebbe un difetto, non una pulizia.
 *
 * <p>Invarianti: le transazioni sono lette tenant-scoped (discriminator dal token verificato, #1/#2);
 * catalogo e listino sono dati di piattaforma.
 */
@ApplicationScoped
public class PaymentReadModel {

    @Inject
    BillingTransactionRepository transactions;

    @Inject
    AppRepository apps;

    @Inject
    AppTierRepository tiers;

    /** Lo storico del tenant del token, dalla transazione più recente. */
    public PaymentsView forCurrentTenant() {
        List<PaymentView> views = new ArrayList<>();
        for (BillingTransaction tx : transactions.listRecentFirst()) {
            App app = tx.getAppId() != null ? apps.findById(tx.getAppId()) : null;
            AppTier tier = tx.getAppTierId() != null ? tiers.findById(tx.getAppTierId()) : null;
            views.add(new PaymentView(
                    tx.getBilledAt(),
                    app != null ? app.getSlug() : null,
                    app != null ? app.getName() : null,
                    tier != null ? tier.getName() : null,
                    tx.getBillingCycle(),
                    tx.getAmount(),
                    tx.getCurrency(),
                    tx.getStatus(),
                    tx.getReceiptUrl()));
        }
        return new PaymentsView(views);
    }
}
