package app.appgrove.@@APP_ID@@;

import app.appgrove.commons.access.AppOperation;
import app.appgrove.commons.access.AppOperationsContract;
import app.appgrove.commons.access.AppRole;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

/**
 * Documento delle operazioni dell'app @@APP_NAME@@ (UC 0101): che cosa l'applicazione espone e quanto
 * potere serve per ognuna. La coerenza fra questo documento e il codice è verificata da
 * {@code AppOperationsContractTest}, che diventa rosso se un'operazione compare nel codice e non qui —
 * o se una scrittura non chiede almeno {@code editor}.
 *
 * <p><b>Da riempire col dominio vero</b> quando le operazioni segnaposto verranno sostituite. La regola
 * di classificazione è una sola, in cascata (dettaglio in {@link AppOperationsContract}):
 *
 * <ol>
 *   <li>l'operazione <b>cambia dati, invia qualcosa fuori o consuma quota</b>? → almeno
 *       {@link AppRole#editor};</li>
 *   <li>governa <b>chi</b> usa l'applicazione (abilitazione, revoca, cambio di ruolo)? →
 *       {@link AppRole#admin};</li>
 *   <li>altrimenti → {@link AppRole#viewer}.</li>
 * </ol>
 *
 * <p>Le <b>esenzioni</b> sono poche e vanno dichiarate col loro motivo: i diritti dell'interessato sui
 * propri dati personali e lo stato di quota informativo. Non aggiungerne altre per comodità: un'operazione
 * dichiarata esente si sottrae al controllo, ed è l'unico modo di sottrarvisi.
 *
 * <p>Se questa applicazione avesse bisogno di <b>poteri intermedi</b> fra {@code viewer} ed {@code editor},
 * non si inventa un quarto ruolo: si dichiara come punto aperto della sua storia (UC 0101 §5).
 */
@ApplicationScoped
public class @@APP_CLASS@@OperationsContract implements AppOperationsContract {

    @Override
    public String appId() {
        return @@APP_CLASS@@DataContract.APP_ID;
    }

    @Override
    public List<AppOperation> operations() {
        return List.of(
                // ── Letture: cascata §4.3 ────────────────────────────────────
                AppOperation.requiring(
                        "items.list",
                        "Elenca gli elementi del gruppo di lavoro",
                        "List the workspace items",
                        ItemResource.class,
                        "list",
                        AppRole.viewer),
                AppOperation.requiring(
                        "items.get",
                        "Apre il dettaglio di un elemento",
                        "Open the detail of one item",
                        ItemResource.class,
                        "get",
                        AppRole.viewer),

                // ── Operazioni dispositive: cascata §4.1 ─────────────────────
                // La creazione consuma quota, quindi sarebbe dispositiva anche se non scrivesse nulla.
                AppOperation.requiring(
                        "items.create",
                        "Crea un elemento (consuma quota)",
                        "Create an item (consumes quota)",
                        ItemResource.class,
                        "create",
                        AppRole.editor),
                AppOperation.requiring(
                        "items.update",
                        "Modifica un elemento, incluso il suo stato",
                        "Update an item, including its status",
                        ItemResource.class,
                        "update",
                        AppRole.editor),
                AppOperation.requiring(
                        "items.delete",
                        "Cancella un elemento",
                        "Delete an item",
                        ItemResource.class,
                        "delete",
                        AppRole.editor),

                // ── Esente dai ruoli, di proposito ───────────────────────────
                AppOperation.exempt(
                        "quota.status",
                        "Legge uso e tetto della quota",
                        "Read quota usage and cap",
                        QuotaResource.class,
                        "status",
                        "Stato di quota informativo: resta raggiungibile anche da chi non ha ancora un ruolo"
                                + " sull'applicazione, così che il banner del consumo non diventi un rifiuto"
                                + " (scelta già assunta in UC 0099, dove il varco è volutamente opt-in)"));
    }
}
