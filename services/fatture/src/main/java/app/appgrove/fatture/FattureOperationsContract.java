package app.appgrove.fatture;

import app.appgrove.commons.access.AppOperation;
import app.appgrove.commons.access.AppOperationsContract;
import app.appgrove.commons.access.AppRole;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

/**
 * Documento delle operazioni dell'app <b>fatture</b> (UC 0101): che cosa l'applicazione espone e quanto
 * potere serve per ognuna. La classificazione segue la cascata del contratto di piattaforma
 * ({@link AppOperationsContract}) e non ammette eccezioni per convenienza: la coerenza fra questo
 * documento e il codice è verificata da {@code AppOperationsContractTest}.
 *
 * <p><b>Nota di lettura.</b> Fino a UC 0099 questa applicazione non dichiarava alcun ruolo minimo, perché
 * era «a utente singolo» e la domanda non si poneva. La categoria B2C/B2B si ritira (UC 0114) e l'accesso
 * per applicazione vale per tutte: qui le letture sono {@code viewer} e le scritture {@code editor}, come
 * in qualunque altra applicazione. Non esistono operazioni di governo degli accessi dentro `fatture`: la
 * schermata che le porterà è di UC 0111.
 */
@ApplicationScoped
public class FattureOperationsContract implements AppOperationsContract {

    @Override
    public String appId() {
        return FattureDataContract.APP_ID;
    }

    @Override
    public List<AppOperation> operations() {
        return List.of(
                // ── Letture: cascata §4.3 ────────────────────────────────────
                AppOperation.requiring(
                        "invoices.list",
                        "Elenca le fatture del gruppo di lavoro",
                        "List the workspace invoices",
                        InvoiceResource.class,
                        "list",
                        AppRole.viewer),
                AppOperation.requiring(
                        "invoices.get",
                        "Apre il dettaglio di una fattura",
                        "Open the detail of one invoice",
                        InvoiceResource.class,
                        "get",
                        AppRole.viewer),

                // ── Operazioni dispositive: cascata §4.1 ─────────────────────
                // La creazione consuma quota, quindi sarebbe dispositiva anche se non scrivesse nulla.
                AppOperation.requiring(
                        "invoices.create",
                        "Crea una fattura (consuma quota)",
                        "Create an invoice (consumes quota)",
                        InvoiceResource.class,
                        "create",
                        AppRole.editor),
                AppOperation.requiring(
                        "invoices.update",
                        "Modifica una fattura, incluso il suo stato",
                        "Update an invoice, including its status",
                        InvoiceResource.class,
                        "update",
                        AppRole.editor),
                AppOperation.requiring(
                        "invoices.delete",
                        "Cancella una fattura",
                        "Delete an invoice",
                        InvoiceResource.class,
                        "delete",
                        AppRole.editor),

                // ── Esente dai ruoli, di proposito ───────────────────────────
                AppOperation.exempt(
                        "quota.status",
                        "Legge uso e tetto della quota",
                        "Read quota usage and cap",
                        QuotaResource.class,
                        "fatture",
                        "Stato di quota informativo: resta raggiungibile anche da chi non ha ancora un ruolo"
                                + " sull'applicazione, così che il banner del consumo non diventi un rifiuto"
                                + " (scelta già assunta in UC 0099, dove il varco è volutamente opt-in)"));
    }
}
