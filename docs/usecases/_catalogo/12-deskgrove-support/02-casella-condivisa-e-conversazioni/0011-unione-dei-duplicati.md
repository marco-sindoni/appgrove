# 0011 — Unione dei duplicati

**Applicazione**: 12 — DeskGrove Support (`helpdesk`) · **Epica**: 02 — Casella condivisa e conversazioni
**Storia**: `0011` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0007`, `0009`, `0010`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come persona che risponde ai clienti
> voglio unire due richieste che parlano dello stesso problema dello stesso cliente
> così da avere una conversazione sola invece di due mezze conversazioni, e da non far rispondere due colleghi alla
> stessa domanda credendo di lavorare su cose diverse.

**Contesto.** È il difetto tipico di ogni casella condivisa e si moltiplicherà con i canali automatici dell'epica
03: il cliente scrive dal modulo del sito, non riceve risposta in mezz'ora e riscrive per posta elettronica; oppure
manda per errore lo stesso messaggio due volte. Nascono due richieste, e con esse due numeri, due orologi, due
persone che rispondono. Costruire l'unione **adesso**, mentre il dominio è ancora piccolo, costa una giornata; farlo
dopo che priorità, code, scadenze e soddisfazione si sono attaccate alla richiesta significa dover decidere cosa
succede a ciascuna di quelle cose. La regola che tiene tutto in piedi è una sola, ed è la stessa della riapertura
(storia `0009`): **niente si riscrive e niente si perde**.

## 2. Requisiti funzionali

1. **RF-1** — Da una richiesta l'operatore sceglie **un'altra richiesta dello stesso richiedente** come
   destinazione dell'unione; l'operazione richiede una **conferma esplicita** che dica quali due richieste verranno
   unite, quanti messaggi si sposteranno e che l'operazione non si disfa.
2. **RF-2** — I messaggi della richiesta assorbita passano nella destinazione **mantenendo verso, autore e data
   originali**, note interne comprese; il filo risultante è in ordine cronologico e nessun messaggio viene riscritto,
   rietichettato o riattribuito.
3. **RF-3** — La richiesta assorbita passa a `chiusa` con motivo «unita alla richiesta N», **conserva il proprio
   numero**, resta consultabile in sola lettura e porta il rimando alla destinazione; la destinazione porta il
   rimando inverso e l'elenco delle richieste che ha assorbito.
4. **RF-4** — L'unione è ammessa **solo** fra due richieste dello stesso account e dello stesso richiedente, ed è
   **vietata** se una delle due è già chiusa, già assorbita da un'altra o coincide con l'altra; ogni caso vietato è
   rifiutato con `409` e la spiegazione del motivo.
5. **RF-5** — L'unione avviene in **una sola transazione**: o passa tutto o non passa niente. Il numero di messaggi
   della destinazione dopo l'unione è esattamente la somma dei messaggi delle due richieste prima: nessun doppione,
   nessuna perdita.
6. **RF-6** — L'unione registra **chi** l'ha eseguita e **quando**, come evento di sistema visibile nel filo di
   **entrambe** le richieste.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Entrambe le richieste vengono risolte filtrando per `tenant_id` preso dal
  token verificato: se una delle due appartiene a un altro account la risposta è `404`, mai `403`, e nulla viene
  scritto. Un `tenant_id` che arrivasse dal corpo o dai parametri viene ignorato. Prova di isolamento dedicata, con
  tentativo di unione fra account diversi.
- **RT-2 — Interfaccia di programmazione (§2).**
  `POST /api/helpdesk/v1/tickets/{id}/merge` con l'identificativo della richiesta di destinazione e una conferma
  esplicita nel corpo; corpo validato in modo dichiarativo; errori in `application/problem+json` (`409` per
  richiedente diverso, richiesta chiusa, richiesta già assorbita o coincidente; `404` per richiesta altrui o
  inesistente); definizione OpenAPI aggiornata nello stesso commit. L'operazione è **idempotente per esito**:
  ripeterla sulla stessa coppia risponde `409` con la spiegazione «già unita», mai una seconda migrazione di
  messaggi.
- **RT-3 — Persistenza (§8).** Migrazione `V8__ticket_merge.sql` sullo schema `app_helpdesk`: colonna
  `merged_into_ticket_id` su `ticket` come **riferimento logico** senza chiave esterna, e tipo di evento «unione»
  nella tabella `ticket_event` introdotta dalla storia `0009`. Lo spostamento dei messaggi, il cambio di stato della
  richiesta assorbita e i due eventi stanno **nella stessa transazione**, con blocco sulle due righe di `ticket` in
  ordine deterministico di identificativo per non incrociare due unioni simultanee. Chiavi primarie UUID versione 7,
  colonne di controllo, cancellazione logica.
- **RT-4 — Modulo frontend (§3, §5).** Nella schermata di dettaglio del modulo `helpdesk`: azione «Unisci a un'altra
  richiesta», scelta della destinazione fra le sole richieste dello stesso richiedente (riusando la ricerca della
  storia `0010`), finestra di conferma che dice cosa succederà e che l'operazione non si disfa, e rimando visibile
  fra le due richieste dopo l'unione. Solo token del sistema di design; tema chiaro e scuro; controllo automatico di
  accessibilità sulla finestra di conferma.
- **RT-5 — Cinque lingue (§4).** Testo della conferma, motivo di chiusura «unita alla richiesta N», messaggi di
  rifiuto e testo dell'evento di sistema passano dallo spazio-nomi `helpdesk` e sono presenti in `en, it, fr, es,
  de`. Il motivo si memorizza come **valore tecnico con il numero**, non come frase tradotta.
- **RT-6 — Varchi e quota (§6, §7).** Unire **non consuma quota**: la metrica unica dell'app è `agents` (posti
  operatore, natura `stock`), consumata dalla storia `0018`, e un'unione **riduce** il numero di richieste, non lo
  aumenta. Restano i varchi a monte: `401`, `402` con abbonamento non attivo, `403` per ruolo insufficiente.
  Trattandosi di un'operazione difficilmente reversibile si propone di richiedere almeno il ruolo `admin` (vedi
  punti aperti). La storia non fissa prezzi: consuma il tetto pubblicato dall'abilitazione.
- **RT-7 — Esposizione conversazionale (§12).** Strumento dichiarato:
  `unisci_richieste(numero_origine, numero_destinazione) → proposta di unione`, marcato **scrittura** con
  **conferma umana obbligatoria**: lo strumento produce una **proposta** che dice quali messaggi si sposterebbero, e
  l'unione avviene solo dopo l'approvazione della persona. È la stessa regola che separa `prepara_risposta` da
  `invia_risposta` (§7 della descrizione), applicata a un'operazione che non si disfa. Il contratto vive dentro il
  servizio; il server conversazionale è di piattaforma e non ancora implementato (UC 0061-0063).
- **RT-8 — Dati personali (§10).** Nessun campo nuovo e nessuna tabella nuova di persone: l'unione **sposta** righe
  già dichiarate nel manifesto `docs/compliance/manifests/helpdesk.yaml`. Poiché è ammessa **solo** a parità di
  richiedente, l'insieme dei dati riferibili a quella persona non cambia: cambia solo la richiesta che li contiene.
  Va però verificato — e scritto nel registro delle decisioni — che l'esportazione e la cancellazione per singolo
  richiedente (storia `0036`) seguano i messaggi nella loro **nuova** collocazione e non nella vecchia: una riga
  spostata e dimenticata è il difetto di conformità più probabile di questa storia.
- **RT-9 — Registrazione eventi (§14).** Gli eventi `richieste unite` (con i due identificativi e il numero di
  messaggi spostati) e `unione rifiutata` (con il motivo tecnico) sono registrati con `tenant_id`, `app_id`,
  `user_id` e identificativo di correlazione, senza dati personali.

## 4. Criteri di accettazione

**CA-1 — Unione di due richieste**
- **Dato** due richieste dello stesso richiedente nello stesso account, una con tre messaggi e una con due, e un
  utente abilitato con il ruolo richiesto
- **Quando** unisce la prima nella seconda e conferma
- **Allora** la destinazione ha cinque messaggi in ordine cronologico con verso, autore e data originali; la
  richiesta assorbita è `chiusa` con motivo «unita alla richiesta N», conserva il proprio numero ed è consultabile
  in sola lettura; entrambe portano il rimando all'altra e l'evento di sistema con chi e quando

**CA-2 — Richiedente diverso**
- **Dato** due richieste dello stesso account ma di due richiedenti diversi · **Quando** si tenta di unirle
- **Allora** la risposta è `409` con la spiegazione, e nessun messaggio viene spostato

**CA-3 — Unione ripetuta**
- **Dato** una richiesta già assorbita da un'altra · **Quando** si tenta di unirla una seconda volta, alla stessa
  destinazione o a un'altra · **Allora** la risposta è `409` con la spiegazione «già unita», e i messaggi restano
  dove sono: nessuna seconda migrazione

**CA-4 — O tutto o niente**
- **Dato** un'unione che fallisce a metà (ad esempio per un errore di scrittura simulato dopo lo spostamento del
  primo messaggio) · **Quando** la transazione viene annullata
- **Allora** entrambe le richieste tornano esattamente allo stato iniziale, con i conteggi dei messaggi di partenza e
  nessun evento di unione registrato

**CA-5 — Isolamento fra account**
- **Dato** due account `A` e `B`, ciascuno con richieste dello stesso nome di richiedente
- **Quando** un utente di `A` tenta di unire una propria richiesta a una richiesta di `B`, anche forzando il
  `tenant_id` nel corpo · **Allora** riceve `404` e nulla viene scritto né in `A` né in `B`

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend`, `frontend`, `compliance`; l'intera suite prima del
      commit);
- [ ] prove di **unità** sulle condizioni di ammissibilità dell'unione e di **integrazione** sullo spostamento
      transazionale dei messaggi, compreso l'annullamento a metà e due unioni simultanee, con database effimero e
      migrazioni vere;
- [ ] prova di **isolamento fra account** con tentativo di unione fra account diversi;
- [ ] **prova end-to-end**: *rimandare* — il percorso `[J-HELPDESK]` copre già apertura, risposta e chiusura, e
      l'unione è un'operazione occasionale che allungherebbe il percorso senza aggiungere rischio non già coperto
      dalle prove di integrazione. Si apre nel registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) una voce `da-coprire` con motivo
      «operazione occasionale, rischio coperto a livello di integrazione» e storia proprietaria `0037`;
- [ ] **traduzioni** presenti in tutte e cinque le lingue (`en, it, fr, es, de`);
- [ ] **manifesto dei dati**: nessuna voce nuova, ma verifica scritta che esportazione e cancellazione per singolo
      richiedente seguano i messaggi nella loro nuova collocazione;
- [ ] **registro delle decisioni** compilato, con annotate la transazione unica, l'ordine deterministico dei blocchi
      e la scelta di **non** prevedere l'annullamento dell'unione;
- [ ] contratto degli **strumenti conversazionali** dichiarato per `unisci_richieste`, con conferma obbligatoria su
      proposta;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0007` | Servono i messaggi del filo: sono ciò che l'unione sposta, e la loro immutabilità è la regola che l'unione deve rispettare |
| storia `0009` | Servono la macchina a stati e la tabella degli eventi: la richiesta assorbita si chiude con un motivo e l'unione si registra come evento |
| storia `0010` | Serve la ricerca per scegliere la richiesta di destinazione senza doverne conoscere l'identificativo |
| epica di piattaforma non implementata (UC 0061-0063) | Il livello conversazionale non esiste: qui si dichiara solo il contratto di `unisci_richieste` |

## 7. Fuori ambito

- l'**annullamento dell'unione** («separa di nuovo le due richieste»): **non esiste** ed è una scelta, non una
  mancanza — vedi i punti aperti;
- la **fusione di due richiedenti** che sono la stessa persona con due indirizzi diversi, che è il caso in cui
  l'unione qui è vietata: storia `0012`, che possiede l'anagrafica del richiedente e la deduplicazione;
- il **riconoscimento automatico dei duplicati** e il suggerimento «forse questa è la stessa richiesta»: non lo fa
  nessuna storia di questa stesura; l'unione resta un'azione decisa da una persona;
- il destino di **priorità, coda, etichette, scadenze e soddisfazione** delle due richieste unite: quei campi non
  esistono ancora e la domanda va ripresa dalle storie che li introducono (`0019`, `0021`, `0025`, `0027`);
- gli **allegati** che seguono i messaggi spostati: storia `0016`, che li introduce e dovrà rispettare la regola
  «niente si perde» scritta qui.

## 8. Punti aperti

- **Serve poter annullare un'unione?** La proposta di questa storia è **no**: l'annullamento richiederebbe di
  ricordare la provenienza di ogni singolo messaggio e riaprirebbe una richiesta chiusa, cioè esattamente ciò che la
  storia `0009` vieta. Il rimedio a un'unione sbagliata è aprire una richiesta nuova, non disfare la storia della
  conversazione. Ha però un costo per l'operatore che sbaglia, ed è una decisione di direzione di prodotto: chiude
  lo **sviluppatore**.
- **Quale ruolo può unire?** Qui si propone `admin`, perché l'operazione non si disfa; ma in un'azienda di tre
  persone dove tutti rispondono, un permesso in più è un attrito che porta a non usare la funzione e a lasciare i
  duplicati in coda. La matrice dei ruoli dell'app si consolida con la storia `0018` (operatori e posti): chiude lo
  **sviluppatore**, insieme a quella storia.
