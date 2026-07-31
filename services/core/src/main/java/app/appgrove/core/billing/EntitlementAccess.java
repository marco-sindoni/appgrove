package app.appgrove.core.billing;

import app.appgrove.core.catalog.AppStatus;
import app.appgrove.core.platform.AccountStatus;

/**
 * Regola <b>unica</b> che decide se un account ha accesso a un'app (UC 0077). Fino a questa change la
 * stessa condizione viveva in due copie — in Java dentro {@link EntitlementReadModel}, in SQL dentro la
 * matrice della console admin — e le due <b>divergevano</b>: la matrice partiva dalle righe di
 * subscription, quindi non vedeva mai un'app abilitata dal <b>tier free di baseline</b>, e ignorava
 * l'account in attesa di eliminazione. Chi apriva la console per sapere "cosa vede questo cliente"
 * poteva leggere una risposta falsa.
 *
 * <p>La regola (#09 dec.30, UC 0027): l'accesso c'è quando l'account non è in attesa di eliminazione
 * (UC 0033, #13 E25: disattivazione immediata = zero entitlement) <b>e</b> l'app è {@code active}
 * (gate 2, alimentato da UC 0076) <b>e</b> — se una subscription esiste — quella subscription concede
 * accesso ({@link SubscriptionStatus#grantsAccess()}, UC 0026); se non esiste, l'accesso resta quello
 * del <b>tier free</b> dell'app, quando c'è.
 *
 * <p>Classe di sola decisione, senza stato e senza accesso al database: i chiamanti raccolgono gli
 * ingredienti dove è naturale per loro (entità JPA nel percorso tenant-scoped, SQL nativo nella
 * superficie admin cross-account) e chiedono qui il verdetto. È il punto in cui la coerenza fra le
 * viste è garantita per costruzione invece che per disciplina.
 */
public final class EntitlementAccess {

    private EntitlementAccess() {}

    /**
     * Verdetto di accesso per la coppia (account, app).
     *
     * @param accountStatus stato dell'account; {@code null} quando il chiamante non lo conosce e vale
     *     quanto uno stato che non blocca (l'account inesistente non ha comunque subscription)
     * @param appStatus stato dell'app di catalogo
     * @param subscriptionStatus stato della subscription del tenant su quell'app, {@code null} se non
     *     esiste alcuna subscription
     * @param freeTierAvailable se l'app offre un tier free di baseline; consultato <b>solo</b> quando
     *     non esiste subscription
     */
    public static boolean granted(
            AccountStatus accountStatus,
            AppStatus appStatus,
            SubscriptionStatus subscriptionStatus,
            boolean freeTierAvailable) {
        if (accountStatus == AccountStatus.pending_deletion) {
            return false;
        }
        if (appStatus != AppStatus.active) {
            return false;
        }
        return subscriptionStatus != null ? subscriptionStatus.grantsAccess() : freeTierAvailable;
    }
}
