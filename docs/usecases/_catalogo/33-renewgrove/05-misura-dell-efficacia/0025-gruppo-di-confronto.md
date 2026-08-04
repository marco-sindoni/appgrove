# 0025 — Gruppo di confronto

**Applicazione**: 33 — RenewGrove (`fidelizzazione`) · **Epica**: 05 — Misura dell'efficacia
**Storia**: `0025` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0024` — il gruppo si forma sugli stessi esiti, con la stessa finestra e la stessa regola
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che legge «hai trattenuto il 78% dei clienti su cui sei intervenuto»
> voglio sapere quanti ne avrei trattenuti **senza fare niente**
> così da capire se quel 78% è merito del lavoro fatto o è semplicemente il ritmo normale della mia attività.

**Contesto.** Un numero sui soli clienti su cui si è intervenuti non misura nulla, e va detto proprio così: la
maggior parte dei clienti resta comunque. Se in un'attività ne resta il 75% ogni anno da sempre, «ne ho trattenuti
il 78%» è una notizia che non c'è. Serve un termine di paragone, e serve **formato prima di sapere come è andata**,
perché scegliere il gruppo a posteriori significa scegliere il risultato. Questa storia costruisce il paragone
migliore che si può costruire senza fare un esperimento — e scrive a schermo, senza girarci intorno, che un
esperimento non è. È il momento giusto per farla adesso perché la `0024` ha appena reso disponibili gli ingredienti:
una finestra dichiarata e una regola di perdita congelata.

## 2. Requisiti funzionali

1. **RF-1** — Alla conferma di un intervento (`0019`), **nello stesso istante** in cui nasce l'esito del gruppo
   `intervenuto` (`0024`), il sistema forma una **coorte di confronto**: i rapporti che in quel momento stanno nella
   **stessa fascia di rischio** e nella **stessa fascia di anzianità** del rapporto intervenuto, e su cui **non**
   risulta alcun intervento confermato. L'elenco si **congela**: è una fotografia, non un'interrogazione che si
   rieseguirà.
2. **RF-2** — Per ogni rapporto della coorte nasce un `EsitoDelRapporto` con la **stessa durata di finestra** e la
   **stessa regola di perdita** dell'esito intervenuto, gruppo `di confronto`. I due gruppi si misurano con lo stesso
   metro o non si confrontano.
3. **RF-3** — La coorte si forma **solo** se raggiunge un numero minimo di rapporti idonei (proposta: 5). Sotto
   quella soglia la coorte **non nasce**, l'esito intervenuto resta valido e la scheda dice «rapporti idonei
   insufficienti per un confronto» al posto di una percentuale calcolata su due casi.
4. **RF-4** — Se durante la finestra un rapporto della coorte riceve un **intervento confermato**, il suo esito di
   confronto esce dal conteggio ed è marcato **contaminato**, con il momento e la ragione. I contaminati si contano e
   si mostrano: un gruppo che si svuota in silenzio è peggio di un gruppo assente.
5. **RF-5** — La composizione della coorte è **consultabile e immodificabile**: si vede quanti rapporti, in che
   fascia di rischio e di anzianità, formata quando, con quale soglia minima; non esiste alcuna via — interfaccia web
   o interfaccia di programmazione — per aggiungere, togliere o sostituire un rapporto dopo la formazione.
6. **RF-6** — Accanto a ogni confronto compaiono, sempre e a schermo, i **tre limiti dichiarati**: (a) i rapporti non
   sono stati assegnati **a caso** ai due gruppi; (b) chi decide su chi intervenire sceglie di norma i clienti che
   conosce meglio, e questo **sposta** il confronto (effetto di selezione); (c) con numeri piccoli la differenza fra i
   due gruppi può essere solo caso. Il testo dice che si tratta di un **confronto indicativo, non di un esperimento**.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** La coorte si forma **dentro un solo account**: ogni interrogazione di
  formazione filtra per `tenant_id` preso dal token verificato e nessun rapporto di un altro account può entrarvi,
  nemmeno per errore di ambito. Un `tenant_id` che arrivasse dal corpo della richiesta viene ignorato.
- **RT-2 — Interfaccia di programmazione (§2).** `GET /api/fidelizzazione/v1/coorti/{id}` (composizione e limiti) e
  `GET /api/fidelizzazione/v1/coorti` con paginazione a pagina/dimensione e totale; **nessuna rotta di modifica**,
  per costruzione; errori in `application/problem+json`; definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V<N>__coorte_di_confronto.sql` sullo schema `app_fidelizzazione`: tabella
  `coorte_di_confronto` con `tenant_id`, chiave primaria UUID versione 7, colonne di controllo e cancellazione
  logica; porta l'intestazione della coorte (momento della formazione, fascia di rischio, fascia di anzianità,
  durata della finestra, regola di perdita, soglia minima, numerosità). L'appartenenza si esprime con le righe di
  `esito_del_rapporto` che portano gruppo `di confronto` e il riferimento alla coorte: **nessuna tabella di legame
  nuova con dati riferiti a persone**. Nessuna chiave esterna verso altri schemi.
- **RT-4 — Modulo frontend (§3, §5).** Sezione **Confronto** nel modulo `fidelizzazione`, raggiungibile dall'esito e
  dal rendiconto; dati letti con il client generato; solo token del sistema di design; tema chiaro e scuro. I tre
  limiti del **RF-6** stanno **nel corpo della pagina**, non in un suggerimento a comparsa.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe visibili — compresi i tre testi dei limiti dichiarati, che sono
  la parte che conta — passano dallo spazio-nomi `fidelizzazione` e sono presenti in `en, it, fr, es, de`.
- **RT-6 — Varchi e quota (§6, §7).** Nessun consumo di quota: la coorte non crea rapporti sorvegliati, li
  **riferisce**. Con abbonamento `canceled` le rotte rispondono `402`; la lettura è aperta a `owner`, `admin` e
  `member`.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento nuovo: il confronto esce dentro
  `efficacia_degli_interventi`, dichiarato nella storia `0028`, che ne restituisce anche i limiti — un numero di
  confronto senza i suoi limiti, letto in una chat, è più pericoloso che a schermo. Il server conversazionale è di
  piattaforma e non ancora implementato (UC 0061-0063).
- **RT-8 — Dati personali (§10).** La storia **non introduce campi personali nuovi**: `coorte_di_confronto` contiene
  criteri e conteggi, non righe riconducibili a persone, ed è dichiarata nel manifesto come **esclusione motivata**,
  accanto a `fonte`, `modello_di_punteggio` e `piano_di_intervento` (§6 della descrizione). L'appartenenza al gruppo
  è invece un dato riferito a una persona, e vive nel campo `gruppo` di `esito_del_rapporto`, già dichiarato,
  esportato e cancellato dalla `0024`.
- **RT-9 — Registrazione eventi (§14).** `coorte formata (numerosità, fascia)`, `coorte non formata (sotto soglia)`,
  `esito di confronto contaminato`, con `tenant_id`, `app_id`, `user_id` e identificativo di correlazione, senza
  etichette di rapporti.
- **RT-10 — Prove (§11).** Unità sui criteri di idoneità e sulla soglia minima; integrazione sulla formazione con
  database effimero e migrazioni vere, che verifica il **congelamento** (formata la coorte, cambiare la fascia di un
  rapporto non ne cambia la composizione); isolamento fra due account.

## 4. Criteri di accettazione

**CA-1 — La coorte si forma alla conferma, con lo stesso metro**
- **Dato** un account con dodici rapporti in fascia alta e anzianità simile, e un intervento confermato su uno di
  essi con finestra di 90 giorni
- **Quando** la conferma va a buon fine
- **Allora** nasce una coorte con gli undici rapporti non intervenuti idonei, e per ciascuno un
  `EsitoDelRapporto` *ancora aperto* di gruppo `di confronto`, con la stessa finestra di 90 giorni e la stessa
  regola di perdita

**CA-2 — Sotto soglia non si confronta**
- **Dato** un account con soli tre rapporti idonei oltre a quello intervenuto
- **Quando** l'intervento viene confermato
- **Allora** nessuna coorte nasce, l'esito intervenuto è comunque creato, e la scheda mostra «rapporti idonei
  insufficienti per un confronto» al posto di qualunque percentuale

**CA-3 — La composizione è congelata**
- **Dato** una coorte formata con undici rapporti
- **Quando** due di quei rapporti cambiano fascia di rischio e uno viene archiviato, e si ricarica la scheda della
  coorte
- **Allora** la composizione è ancora di undici rapporti, invariata, con il momento di formazione originale

**CA-4 — Contaminazione dichiarata, non nascosta**
- **Dato** una coorte di undici rapporti, su uno dei quali viene confermato un intervento dentro la finestra
- **Quando** si apre il confronto
- **Allora** quel rapporto è marcato **contaminato** con momento e ragione, è escluso dai conteggi, e la scheda dice
  «1 di 11 escluso perché è stato oggetto di un intervento»

**CA-5 — I limiti sono a schermo**
- **Dato** un confronto con entrambi i gruppi valutati
- **Quando** si apre la scheda in una qualsiasi delle cinque lingue
- **Allora** i tre limiti — assegnazione non casuale, effetto di selezione, numeri piccoli — sono visibili nel corpo
  della pagina insieme ai numeri, e il testo dichiara che è un confronto indicativo e non un esperimento

**CA-6 — Isolamento fra account**
- **Dato** due account `A` e `B` con rapporti nella stessa fascia
- **Quando** in `A` si conferma un intervento e si forma la coorte
- **Allora** nessun rapporto di `B` vi compare, e un utente di `B` che forzasse l'identificativo della coorte di `A`
  riceve `404`

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend` e `frontend`; l'intera suite prima del commit);
- [ ] prove di **unità** sui criteri di idoneità e sulla soglia minima, e di **integrazione** sulla formazione e sul
      congelamento, con database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** sulla formazione della coorte e sulla sua lettura;
- [ ] **prova end-to-end**: *rimando* — il confronto compare nel percorso `[J-FIDELIZZAZIONE]` della storia `0030`
      solo come lettura del rendiconto; il registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) porta la voce `da-coprire` con
      motivo («richiede una finestra di osservazione conclusa, non riproducibile in un percorso breve senza
      pilotare l'orologio») e storia proprietaria `0030`;
- [ ] **traduzioni** presenti in `en, it, fr, es, de`, testi dei limiti compresi;
- [ ] **manifesto dei dati**: nessuna voce nuova; `coorte_di_confronto` dichiarata come esclusione motivata;
- [ ] **registro delle decisioni** compilato: criteri di idoneità, soglia minima scelta e perché, congelamento
      invece di ricalcolo, trattamento dei contaminati;
- [ ] contratto degli **strumenti conversazionali**: nessuno strumento nuovo; i limiti viaggiano dentro
      `efficacia_degli_interventi` della `0028`;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la descrizione parla della misura di efficacia (§8, epica 05).

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0024` (esito del rapporto) | la coorte è fatta di esiti: stessa finestra, stessa regola, gruppo diverso |
| storia `0013` (calcolo e storico del punteggio) | l'idoneità si decide sulla **fascia di rischio del giorno della formazione**, che è un valore della serie storica |
| storia `0009` (il rapporto sorvegliato) | l'anzianità del rapporto è un criterio di idoneità |

## 7. Fuori ambito

- l'**assegnazione casuale** dei rapporti ai due gruppi, cioè un esperimento vero: significherebbe **non intervenire
  di proposito** su clienti che si stanno perdendo, per motivi statistici. Non è una funzione che questo prodotto
  può proporre a una micro-impresa, e non la propone;
- qualunque calcolo di significatività statistica: con le numerosità di questo segmento produrrebbe una cifra
  autorevole su una base che non la regge. Al suo posto ci sono la soglia minima (**RF-3**) e i limiti dichiarati
  (**RF-6**);
- la **presentazione** dei conteggi aggregati per periodo: storia `0027`;
- il confronto fra **tipi diversi** di intervento (telefonata contro messaggio contro offerta): richiede numerosità
  che questo segmento non ha; se ne riparla quando i dati esistono.

## 8. Punti aperti

- **La soglia minima di 5 rapporti idonei** è una convenzione scelta per prudenza, non una soglia statistica: con
  cinque casi per gruppo una differenza resta compatibile con il caso. Alzarla rende il confronto più onesto e più
  spesso assente; abbassarla fa il contrario. Chiude: **sviluppatore** — direzione di prodotto.
- **Se i rapporti di confronto debbano consumare qualcosa nel rendiconto della prova gratuita.** Formare coorti
  significa aprire esiti su rapporti su cui non si è fatto nulla: sono lavoro dell'app, non del cliente, e qui non
  costano quota. Se un giorno la quota si spostasse dai rapporti sorvegliati agli esiti valutati, questa scelta
  andrebbe rifatta. Chiude: **sviluppatore** — prezzi e quote.
- **Effetto di selezione: dichiararlo basta?** L'app dice che il confronto è distorto, ma non lo corregge — nessuna
  ponderazione, nessun appaiamento oltre fascia e anzianità. È una scelta consapevole (correggere richiederebbe
  metodi che nessuno qui potrebbe spiegare a un titolare), e va confermata come tale invece di essere scoperta
  dopo. Chiude: **sviluppatore** — direzione di prodotto.
