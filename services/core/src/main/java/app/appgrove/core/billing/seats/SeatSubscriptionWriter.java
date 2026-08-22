package app.appgrove.core.billing.seats;

import app.appgrove.core.catalog.PlatformCatalog;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Scrive l'abbonamento di piattaforma dei <b>posti</b> (UC 0103): lo crea al quarto posto e ne alza la
 * quantità a ogni posto a pagamento in più.
 *
 * <p><b>Perché una scrittura sincrona, quando l'invariante dice «l'abbonamento lo muove solo il
 * webhook».</b> L'invariante (#09 C16) esiste per gli abbonamenti delle <b>applicazioni</b>, dove nessuno
 * aspetta: si avvia un acquisto, il fornitore conferma quando vuole, l'accesso si apre al webhook. Qui
 * l'ordine degli atti è l'opposto e non è negoziabile: l'invito <b>non deve nascere</b> se l'addebito non
 * è riuscito, quindi l'esito serve <i>adesso</i>. Un abbonamento che arrivasse via webhook mezzo secondo
 * dopo lascerebbe l'invito senza nessuno che possa deciderne la sorte.
 *
 * <p>La scrittura resta comunque <b>a valle di una conferma del fornitore</b> — mai «ottimistica» — e il
 * webhook che seguirà è idempotente su questa riga: la guardia {@code last_event_occurred_at} resta nulla
 * qui, così un evento successivo del fornitore può riscriverla senza essere scartato come vecchio.
 *
 * <p><b>SQL nativo</b>, come {@code SubscriptionWriter}: l'entità {@code Subscription} è di sola lettura
 * per contratto (nessun costruttore pubblico, nessun setter) e va tenuta così — è la fonte di verità dello
 * stato di fatturazione, e il numero di punti che la scrivono deve restare contabile a mano.
 */
@ApplicationScoped
public class SeatSubscriptionWriter {

    /**
     * Durata del periodo dei posti alla creazione. È la <b>permanenza minima mensile</b> dell'epica E22.2:
     * il posto pagato resta a disposizione dell'account per un mese, ed è la ragione per cui un invito
     * scaduto non produce un rimborso ma un posto riutilizzabile.
     */
    private static final int PERIOD_MONTHS = 1;

    private static final String UPSERT =
            """
            insert into platform.subscription
              (id, tenant_id, app_id, app_tier_id, status, quantity,
               current_period_start, current_period_end, paddle_subscription_id,
               created_at, updated_at, created_by, updated_by)
            values (:id, :tenant, :app, null, 'active', :quantity,
                    :periodStart, :periodEnd, :paddleSubscriptionId,
                    now(), now(), 'seats', 'seats')
            on conflict (tenant_id, app_id) where deleted_at is null
            do update set
              status                 = 'active',
              quantity               = :quantity,
              paddle_subscription_id = coalesce(
                  platform.subscription.paddle_subscription_id, excluded.paddle_subscription_id),
              updated_at             = now(),
              updated_by             = 'seats'
            """;

    @Inject
    EntityManager em;

    /**
     * Porta l'abbonamento dei posti dell'account alla quantità indicata, creandolo se non c'è.
     *
     * <p>Il periodo si scrive <b>solo alla creazione</b>: sull'aggiornamento non viene toccato, perché
     * aggiungere una persona non fa ripartire il mese — se lo facesse, un account potrebbe rimandare il
     * rinnovo all'infinito invitando qualcuno il giorno prima della scadenza.
     *
     * @param tenantId account, ricavato dal token verificato dal chiamante (mai da un parametro di rete)
     * @param quantity posti a pagamento pagati per il periodo in corso
     * @param paddleSubscriptionId identificativo dell'abbonamento presso il fornitore
     */
    public void upsert(UUID tenantId, int quantity, String paddleSubscriptionId) {
        Instant now = Instant.now();
        em.createNativeQuery(UPSERT)
                .setParameter("id", UUID.randomUUID())
                .setParameter("tenant", tenantId.toString())
                .setParameter("app", PlatformCatalog.seatsAppId())
                .setParameter("quantity", quantity)
                .setParameter("periodStart", now)
                .setParameter("periodEnd", now.plus(PERIOD_MONTHS * 30L, ChronoUnit.DAYS))
                .setParameter("paddleSubscriptionId", paddleSubscriptionId)
                .executeUpdate();
    }

    /**
     * Riporta la quantità al valore precedente: la rettifica del caso in cui l'addebito riesce e la
     * creazione dell'invito fallisce subito dopo (UC 0103 §5).
     *
     * <p>Nella pratica la transazione che fallisce annulla da sé anche questa scrittura, e proprio per
     * questo il metodo esiste: il ripristino <b>non</b> deve dipendere dal fatto che chi chiama sia dentro
     * la stessa unità di lavoro. Un giorno la sequenza sarà spezzata in due transazioni con l'addebito nel
     * mezzo (vedi i punti aperti di UC 0103) e questo è il punto che continuerà a funzionare.
     */
    public void restoreQuantity(UUID tenantId, int previousQuantity) {
        em.createNativeQuery(
                        "update platform.subscription set quantity = :quantity, updated_at = now(),"
                                + " updated_by = 'seats' where tenant_id = :tenant and app_id = :app"
                                + " and deleted_at is null")
                .setParameter("quantity", previousQuantity)
                .setParameter("tenant", tenantId.toString())
                .setParameter("app", PlatformCatalog.seatsAppId())
                .executeUpdate();
    }
}
