package app.appgrove.commons.access;

import java.util.List;

/**
 * Il <b>documento delle operazioni</b> di una applicazione (UC 0101): dichiara che cosa l'applicazione
 * espone e <b>quanto potere serve</b> per ognuna. È la parte del contratto dei tre ruoli che una macchina
 * può leggere, e quindi l'unica che non invecchia in silenzio.
 *
 * <p>Precedente identico nella forma: {@link app.appgrove.commons.gdpr.AppDataContract} — un contratto che
 * ogni applicazione realizza e che un collaudo verifica. Qui il collaudo è
 * {@code AppOperationsContractVerifier} (test-jar di {@code commons}), invocato da un test di una riga in
 * ogni servizio.
 *
 * <p><b>Perché esiste un documento se le annotazioni sono già nel codice.</b> Le annotazioni dicono che
 * cosa il varco fa; non dicono che l'elenco sia <b>completo</b>. Un metodo nuovo senza annotazione è
 * indistinguibile da un metodo che di proposito non ne ha bisogno, e nessuno se ne accorge. Con il
 * documento le due cose divergono e il collaudo lo dice. Serve inoltre a due lettori che non compilano
 * Java: il copilota della skill {@code new-application} (UC 0112) e chi scrive la documentazione utente.
 *
 * <p><b>La regola di classificazione</b> — una sola, valida per tutte le applicazioni, in cascata:
 *
 * <ol>
 *   <li>l'operazione <b>cambia dati, invia qualcosa fuori o consuma quota</b>? → almeno
 *       {@link AppRole#editor}. Sono le operazioni <i>dispositive</i>: creazione, modifica, cancellazione,
 *       invio, esportazione che genera un documento, importazione, cambio di stato;</li>
 *   <li>l'operazione governa <b>chi</b> usa l'applicazione? → {@link AppRole#admin}. Abilitazione, revoca,
 *       cambio di ruolo dentro quella applicazione;</li>
 *   <li>altrimenti → {@link AppRole#viewer}. Elenchi, dettagli, ricerche, riepiloghi.</li>
 * </ol>
 *
 * <p>Tre chiarimenti che evitano le discussioni ricorrenti: scaricare in foglio di calcolo ciò che si vede
 * già è una <b>lettura</b> (diventa dispositiva solo se produce un effetto verso l'esterno); le
 * <b>preferenze personali</b> — tema, lingua, colonne visibili — non sono dati dell'applicazione e le
 * cambia chiunque; il {@code viewer} vede <b>tutti</b> i dati che l'ambito dell'applicazione gli
 * attribuisce (UC 0115), e nascondergliene una parte sarebbe un ruolo nuovo, non una restrizione
 * silenziosa.
 *
 * <p><b>Le operazioni esenti dai ruoli</b> sono poche e vanno dichiarate col loro motivo
 * ({@link AppOperation#exempt}): i diritti dell'interessato sui propri dati personali e lo stato di quota
 * informativo. Sono raggiungibili anche da chi non ha alcun ruolo — per costruzione, perché
 * {@link RequiresAppRole} è volutamente <i>opt-in</i> (UC 0099) — e il collaudo pretende che
 * <b>non</b> portino l'annotazione del varco: un'esenzione protetta è un diritto rotto.
 */
public interface AppOperationsContract {

    /** Identificativo dell'app (es. {@code "fatture"}), lo stesso di {@code quarkus.application.name}. */
    String appId();

    /**
     * Tutte le operazioni esposte dall'applicazione, letture comprese. <b>Tutte</b>: il collaudo verifica
     * anche la direzione inversa — un'operazione esposta e non dichiarata rende rossa la suite — e quella è
     * la direzione che coglie ciò che qualcuno aggiunge domani.
     */
    List<AppOperation> operations();
}
