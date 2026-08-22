package app.appgrove.core.billing.seats;

import app.appgrove.core.platform.InvitationRepository;
import app.appgrove.core.platform.InvitationStatus;
import app.appgrove.core.platform.MembershipRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Instant;

/**
 * Quanti posti occupa l'account corrente (UC 0102 §4). <b>Definizione unica</b>: se questo conteggio
 * vivesse in due posti, i due divergerebbero — e la divergenza si manifesterebbe come una fattura sbagliata.
 *
 * <h2>La regola, in una frase</h2>
 *
 * Occupa un posto chi ha un'<b>appartenenza viva</b> all'account, più ogni <b>invito in attesa non
 * scaduto</b>.
 *
 * <p>Scritta così — e non come elenco degli stati che occupano posto — copre da sé tutti i casi dello use
 * case:
 *
 * <ul>
 *   <li>l'<b>owner</b> rientra per costruzione, perché è un'appartenenza con ruolo owner: non va sommato a
 *       parte, sarebbe un doppio conteggio. È anche il senso della franchigia «di tre persone in tutto»,
 *       che comprende l'owner;</li>
 *   <li>le persone <b>attive</b> rientrano;</li>
 *   <li>le persone <b>sospese</b> rientrano: la sospensione è un provvedimento reversibile, non una
 *       riduzione. Chi vuole liberare il posto indica la persona per la cessazione;</li>
 *   <li>le persone <b>indicate per la cessazione</b> rientreranno da sé quando UC 0104 introdurrà quello
 *       stato, <b>senza modificare questo conteggio</b>: la loro appartenenza è viva fino alla scadenza del
 *       periodo. È la ragione per cui la regola è scritta sull'<i>esistenza</i> dell'appartenenza e non
 *       sull'elenco dei suoi stati — un elenco andrebbe aggiornato a ogni stato nuovo, e un aggiornamento
 *       dimenticato è un posto non pagato;</li>
 *   <li>le persone <b>rimosse</b> non rientrano: la loro appartenenza è cancellata, e il filtro
 *       {@code deleted_at is null} dell'entità le esclude.</li>
 * </ul>
 *
 * <p>Lo stato dell'<b>identità</b> della persona non c'entra: una sospensione di piattaforma (per esempio
 * la limitazione del trattamento dell'art. 18) è una leva del titolare, non una riduzione decisa
 * dall'account, e non libera il posto.
 *
 * <p>Fra gli <b>inviti</b> occupano posto solo quelli in attesa e non ancora scaduti. Le due condizioni
 * servono entrambe: «scaduto» è uno stato che qualcuno deve scrivere, e fra la scadenza dell'ora e il
 * passaggio che la registra esistono righe ancora {@code pending} con la data già passata. Contarle
 * significherebbe far pagare posti che nessuno può più occupare. Gli inviti accettati non si contano qui
 * perché l'accettazione ha già prodotto l'appartenenza, che è contata: contarli entrambi raddoppierebbe la
 * persona.
 *
 * <h2>Perché due interrogazioni e non una</h2>
 *
 * Il piano di lavoro chiedeva «una sola interrogazione con unione, non cinque». Cinque sarebbe il difetto —
 * un conteggio per stato — e qui non ce ne sono cinque: ce ne sono <b>due</b>, una per tabella, che è il
 * minimo possibile senza scrivere SQL nativo. E scriverlo nativo avrebbe avuto un prezzo alto: il perimetro
 * dell'account andrebbe passato a mano, mentre con le entità già separate per account il filtro
 * {@code WHERE tenant_id} lo aggiunge Hibernate (invariante #2 mantenuto <b>per costruzione</b>, non per
 * disciplina). Su un conto che decide quanto si paga, quel filtro non è un dettaglio di prestazioni.
 */
@ApplicationScoped
public class SeatCount {

    @Inject
    MembershipRepository memberships;

    @Inject
    InvitationRepository invitations;

    /**
     * I posti occupati dall'account corrente. Il perimetro viene dal claim {@code tenant_id} del token
     * verificato attraverso il discriminatore delle entità (invarianti #1 e #2): nessun identificativo di
     * account arriva da parametro.
     */
    public int occupiedSeats() {
        return occupiedSeatsAt(Instant.now());
    }

    /**
     * I posti occupati dall'account corrente a un dato istante. L'istante è esplicito perché la scadenza
     * degli inviti dipende da esso: un collaudo che non potesse fissarlo proverebbe qualcosa che cambia da
     * un'esecuzione all'altra.
     */
    public int occupiedSeatsAt(Instant when) {
        long people = memberships.count();
        long pendingInvitations =
                invitations.count("status = ?1 and expiresAt > ?2", InvitationStatus.pending, when);
        return (int) (people + pendingInvitations);
    }
}
