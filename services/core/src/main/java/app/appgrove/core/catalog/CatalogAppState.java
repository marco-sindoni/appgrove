package app.appgrove.core.catalog;

import app.appgrove.core.billing.SubscriptionLifecycle;
import app.appgrove.core.billing.SubscriptionStatus;

/**
 * I <b>sei stati</b> che una card del catalogo può assumere per un dato account (UC 0095 §4). È la
 * traduzione, in linguaggio di vetrina, di ciò che il dominio già sa: stato dell'app di catalogo
 * (UC 0076), fase del ciclo di vita dell'abbonamento (UC 0026), regola unica di accesso (UC 0077).
 *
 * <p><b>Perché è un tipo e non una stringa sparsa</b>: la stessa combinazione deve dare lo stesso
 * risultato ovunque, e l'incoerenza che questa storia esiste per chiudere — app spenta dalla piattaforma
 * mostrata "Active" in Billing e assente dal menu laterale — nasceva esattamente da due derivazioni
 * scritte a mano in due posti diversi.
 *
 * <p><b>Derivazione pura</b>: nessuna dipendenza, nessun accesso al database, quindi verificabile per
 * combinazioni. Gli ingredienti li raccoglie {@link CatalogReadModel}.
 */
public enum CatalogAppState {

    /** Attivabile: nessun accesso in corso, l'azione offerta è l'acquisto. */
    available,
    /** In uso: abbonamento che concede accesso, oppure fascia gratuita di baseline. */
    active,
    /** Prova in corso ({@code trialing}): in uso, con una scadenza da mostrare. */
    trial,
    /** Pagamento in sospeso ({@code past_due}): accesso ancora concesso, ma va sanato. */
    payment_pending,
    /** Disdetta programmata ({@code active} con {@code cancel_at}): revocabile fino alla scadenza. */
    cancellation_scheduled,
    /** Spenta dalla piattaforma ({@code app.status != active}): nessuna azione d'acquisto. */
    disabled_by_platform;

    /**
     * Stato della card per la coppia (account, app).
     *
     * <p>L'ordine delle domande è significativo: la disabilitazione di piattaforma <b>vince su tutto</b>,
     * anche su un abbonamento formalmente attivo — è la coerenza che UC 0076 aveva lasciato aperta come
     * punto da chiudere. Poi comanda l'abbonamento, quando c'è e non è terminato. Solo in ultimo si guarda
     * la fascia gratuita: un'app freemium senza abbonamento è già in uso, e offrirle "Subscribe" sarebbe
     * una bugia (compare nel menu laterale).
     *
     * @param appStatus stato dell'app di catalogo
     * @param subscriptionStatus stato dell'abbonamento del tenant su quell'app, {@code null} se assente
     * @param canceledAt istante di disdetta programmata ({@code cancel_at}), {@code null} se non disdetta
     * @param accessGranted verdetto della regola unica di accesso (UC 0077) per la coppia, usato per il
     *     solo caso "nessun abbonamento": tiene dentro anche lo stato dell'account (in attesa di
     *     eliminazione = zero accesso) senza doverlo ri-derivare qui
     */
    public static CatalogAppState derive(
            AppStatus appStatus,
            SubscriptionStatus subscriptionStatus,
            java.time.Instant canceledAt,
            boolean accessGranted) {
        if (appStatus != AppStatus.active) {
            return disabled_by_platform;
        }
        if (subscriptionStatus != null) {
            SubscriptionLifecycle.Phase phase =
                    SubscriptionLifecycle.of(subscriptionStatus, canceledAt, null).phase();
            switch (phase) {
                case TRIAL:
                    return trial;
                case GRACE:
                    return payment_pending;
                case CANCELING:
                    return cancellation_scheduled;
                case ACTIVE:
                    return active;
                case ENDED:
                    // Disdetto o in pausa: l'app torna a essere una proposta d'acquisto. Si prosegue
                    // sotto, perché una fascia gratuita non riprende comunque il sopravvento (la regola
                    // unica di accesso ignora il tier free quando un abbonamento esiste).
                    break;
            }
        }
        return accessGranted ? active : available;
    }
}
