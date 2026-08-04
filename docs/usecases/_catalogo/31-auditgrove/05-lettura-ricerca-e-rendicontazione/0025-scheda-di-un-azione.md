# 0025 — Scheda di un'azione

**Applicazione**: 31 — AuditGrove (`agentaudit`) · **Epica**: 05 — Lettura, ricerca e rendicontazione
**Storia**: `0025` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0024`, `0002`, `0014`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come persona che deve ricostruire un fatto
> voglio aprire una singola azione e vedere tutto ciò che si sa di lei, compresa la sua posizione nella catena
> così da poter dire con precisione chi ha chiesto cosa, chi l'ha approvato, e che quella riga non è stata toccata.

**Contesto.** L'elenco (storia 0024) risponde alla domanda «cosa è successo?»; questa risponde a «cosa è successo
**esattamente**, e come faccio a fidarmene?». È la schermata che si guarda quando qualcuno ha chiesto conto, e per
questo deve contenere due cose che le schermate di dettaglio di solito non contengono: le **sei domande** del
contratto dell'azione (storia 0007) in forma leggibile, e la **posizione nella catena** con l'esito dell'ultima
verifica di integrità (storia 0014). Una scheda che mostra i fatti senza mostrare la prova che i fatti non sono
stati riscritti è a metà del lavoro.

Il requisito più importante non è tecnico ma di composizione: **l'assenza di approvazione deve saltare all'occhio**.
Un'azione senza nulla osta che appare identica a una approvata è un difetto di prodotto, non di grafica.

## 2. Requisiti funzionali

1. **RF-1** — Esiste la rotta `GET /api/agentaudit/v1/actions/{id}` che restituisce la scheda completa di una
   azione dell'account.
2. **RF-2** — La scheda risponde alle sei domande del contratto dell'azione: **chi** l'ha chiesta (richiedente e
   agente), **quale strumento**, **con quali parametri** (nome, tipo, lunghezza, se vuoto, e impronta del valore —
   mai il valore, salvo il caso del RF-6), **quale effetto** (classe di effetto ed esito dichiarato), **quale
   approvazione** c'era o non c'era, **cosa è stato letto e cosa scritto** (risorse dichiarate).
3. **RF-3** — La scheda mostra la **posizione nella catena**: numero di sequenza dell'account, numero di sequenza
   della sorgente, impronta dell'azione, impronta dell'azione precedente, sigillo che la copre (se esiste) ed
   **esito dell'ultima verifica** dell'intervallo in cui ricade.
4. **RF-4** — Quando l'azione è collegata a un nulla osta, la scheda ne mostra lo stato, chi ha deciso, quando e
   il motivo scritto, con il rimando alla scheda del nulla osta.
5. **RF-5** — Quando l'azione **non** ha un nulla osta e la regola dello strumento ne prevedeva uno, la scheda lo
   dichiara in modo evidente e non sopprimibile, con la formula «eseguita senza approvazione, quando era richiesta»
   e il rimando allo scostamento rilevato (storia 0023).
6. **RF-6** — Quando per quello strumento la conservazione dei contenuti è attiva (storia 0031), la scheda mostra
   un comando esplicito per rivelare il contenuto, riservato al ruolo che ne ha diritto; la rivelazione è
   registrata.
7. **RF-7** — La scheda offre il rimando diretto alla produzione del pacchetto di prova (storia 0015) per
   l'intervallo che contiene l'azione.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** La lettura della singola azione filtra per `tenant_id` preso dal token
  verificato: un identificativo di azione di un altro account risponde `404` e non `403`, per non rivelare
  l'esistenza della riga altrui.
- **RT-2 — Interfaccia di programmazione (§2).** Rotta `GET /api/agentaudit/v1/actions/{id}`; errori in
  `application/problem+json`; definizione OpenAPI aggiornata nello stesso commit. Il contenuto conservato, quando
  esiste, viaggia su una rotta distinta (`GET …/actions/{id}/content`) così che la scheda ordinaria non lo porti
  mai con sé per errore.
- **RT-3 — Persistenza (§8).** Nessuna tabella nuova e nessuna migrazione di struttura: la scheda legge le tabelle
  delle azioni, dei nulla osta e dei sigilli. La tabella delle azioni resta in sola aggiunta, e **aprire una
  scheda non scrive niente su di essa** — la traccia della consultazione è un evento a sé.
- **RT-4 — Modulo frontend (§3, §5).** Schermata di dettaglio raggiungibile dalla cronologia; dati letti con il
  client generato; solo token del sistema di design; funziona in tema chiaro e scuro. La marcatura dell'azione
  senza approvazione usa il colore funzionale d'allarme del sistema di design **e** un testo esplicito: il colore
  da solo non è un'informazione accessibile.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe visibili passano dallo spazio-nomi `agentaudit` e sono
  presenti in `en, it, fr, es, de`, comprese le formule delicate — «eseguita senza approvazione, quando era
  richiesta» va tradotta con cura, perché è la frase che qualcuno leggerà in una contestazione.
- **RT-6 — Varchi e quota (§6, §7).** La consultazione **non consuma** la metrica `actions`. La rivelazione del
  contenuto conservato richiede un ruolo superiore a quello della sola lettura: a ruolo insufficiente risponde
  `403`.
- **RT-7 — Esposizione conversazionale (§12).** Questa storia non dichiara strumenti; lo strumento di lettura
  `dettaglio_azione` è dichiarato alla storia 0034 e restituisce **la stessa scheda in forma minimizzata**, senza
  mai il contenuto conservato — che verso un assistente non esce in nessun caso.
- **RT-8 — Dati personali (§10).** Nessun campo nuovo. La scheda è però il punto di massima esposizione dei dati
  già dichiarati: vale il principio di minimizzazione, e il contenuto conservato si mostra solo su azione
  esplicita. La **rivelazione del contenuto** è essa stessa un trattamento e va dichiarata nel manifesto come
  finalità di consultazione.
- **RT-9 — Registrazione eventi (§14).** L'apertura di una scheda è registrata nel registro tecnico con
  `tenant_id`, `app_id`, `user_id`, identificativo di correlazione e identificativo dell'azione consultata, senza
  dati personali. La **rivelazione del contenuto** è invece una riga del registro dell'applicazione, perché è un
  atto di cui deve restare prova.

## 4. Criteri di accettazione

**CA-1 — La scheda risponde alle sei domande**
- **Dato** un'azione dichiarata con richiedente, agente, strumento, tre parametri, classe di effetto
  «cancellazione», esito «riuscita» e due risorse scritte
- **Quando** un utente apre la scheda
- **Allora** vede tutti e sei gli elementi, con i parametri in forma di nome, tipo, lunghezza e impronta, e
  nessun valore in chiaro

**CA-2 — L'assenza di approvazione è evidente**
- **Dato** un'azione su uno strumento la cui regola richiede approvazione, dichiarata senza nulla osta
- **Quando** un utente apre la scheda
- **Allora** legge in evidenza «eseguita senza approvazione, quando era richiesta», con il rimando allo
  scostamento, e la marcatura non si può nascondere

**CA-3 — La posizione nella catena è verificabile**
- **Dato** un'azione che ricade in un intervallo già sigillato e verificato integro
- **Quando** un utente apre la scheda
- **Allora** vede numero di sequenza, impronta, impronta precedente, il sigillo che la copre e l'esito «integra»
  con la data della verifica

**CA-4 — Il contenuto conservato non si mostra da solo**
- **Dato** un'azione su uno strumento con conservazione dei contenuti attiva
- **Quando** un utente con ruolo di sola lettura apre la scheda
- **Allora** vede che un contenuto esiste ma non lo vede, e il comando di rivelazione risponde `403`

**CA-5 — Isolamento fra account**
- **Dato** due account `A` e `B` e l'identificativo di un'azione di `B`
- **Quando** un utente di `A` chiede quella scheda
- **Allora** riceve `404`, senza alcuna indicazione che quell'azione esista altrove

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno le aree toccate; l'intera suite prima del commit);
- [ ] prove di **unità** sulla composizione della scheda e di **integrazione** sulle due rotte (scheda e
      contenuto), con database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** sulla lettura della singola azione, compreso il `404` invece del `403`;
- [ ] prova di **matrice dei ruoli** sulla rivelazione del contenuto;
- [ ] **prova end-to-end**: **rimando** — il percorso `[J-AGENTAUDIT]` della storia 0037 attraversa la scheda di
      dettaglio; voce `da-coprire` nel registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) con storia proprietaria `0037`;
- [ ] prova di **accessibilità** automatica, con verifica che la marcatura «senza approvazione» sia leggibile
      anche senza percepire il colore;
- [ ] **traduzioni** presenti in tutte e cinque le lingue (`en, it, fr, es, de`);
- [ ] **manifesto dei dati** aggiornato in italiano e inglese con la finalità di consultazione e la rivelazione
      del contenuto;
- [ ] **registro delle decisioni** compilato, con la voce sul `404` invece del `403` e sul perché la rivelazione
      del contenuto è una riga del registro e non un semplice evento tecnico;
- [ ] contratto degli **strumenti conversazionali**: nessuno in questa storia, dichiarato (è la 0034);
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0024` | La scheda si raggiunge dall'elenco, che deve esistere |
| storia `0002` | La posizione nella catena esiste solo se la catena esiste |
| storia `0014` | L'esito della verifica di integrità mostrato nella scheda viene da lì |
| storia `0023` | Il rimando allo scostamento presuppone che gli scostamenti siano rilevati |
| storia `0031` | La rivelazione del contenuto ha senso solo dove la conservazione è attiva; finché la 0031 non c'è, quella parte della scheda resta assente e la cosa va dichiarata |

## 7. Fuori ambito

- **la produzione del pacchetto di prova**: qui c'è solo il rimando; la produzione è la storia 0015;
- **la correzione di un'azione**: non esiste e non esisterà. Se il dato dichiarato era sbagliato, si dichiara una
  nuova azione che lo corregge — la scheda non ha e non avrà un comando di modifica;
- **la cancellazione del contenuto**: storia 0032;
- **il confronto fra due azioni** simili: utile ma non richiesto da nessuna fonte dell'analisi, rimandato.

## 8. Punti aperti

- **Quale ruolo può rivelare il contenuto conservato.** Propongo il ruolo di amministratore dell'account e non
  quello di membro, ma è una scelta che interseca la matrice dei ruoli della storia 0029 e va decisa una volta
  sola per tutte e due. Chi chiude: sviluppatore, insieme alla 0029.
- **Se la scheda debba mostrare l'impronta per intero o abbreviata.** Un'impronta completa è illeggibile e occupa
  una riga; abbreviarla nella schermata è comodo ma chi verifica ha bisogno di quella intera. Propongo abbreviata
  con possibilità di copiare il valore completo, ma è una decisione di usabilità da verificare con chi userà
  davvero la verifica.
