# 0033 — Suggerimento di articoli nel modulo

**Applicazione**: 12 — DeskGrove Support (`helpdesk`) · **Epica**: 06 — Base di conoscenza e portale del richiedente
**Storia**: `0033` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0013`, `0029`, `0031`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che riceve ogni giorno tre richieste a cui la mia base di conoscenza risponde già
> voglio che il cliente veda la risposta **mentre** scrive la domanda, prima di premere invia
> così da ricevere meno richieste a parità di clienti serviti, e da poterlo misurare invece di sperarci.

**Contesto.** È l'ultima storia dell'epica e ne chiude il ragionamento: gli articoli esistono (`0029`), l'operatore
li usa rispondendo (`0030`), il pubblico li legge (`0031`) — resta il caso più prezioso, cioè la richiesta che non
viene mai aperta. Il valore non è teorico: ogni richiesta evitata è tempo che nessuno spende, su un prodotto venduto
a posti operatore. La forma però è delicata: il suggerimento deve **aiutare**, non ostacolare. Un modulo che ti
mette una barriera fra te e il pulsante «invia» è un modulo che il cliente odia, e il fastidio si scarica poi
sull'azienda. Per questo il suggerimento qui è passivo e misurato, non un varco da superare.

## 2. Requisiti funzionali

1. **RF-1** — Mentre il visitatore scrive l'**oggetto** nel modulo di contatto, superata una lunghezza minima, il
   modulo propone fino a cinque articoli **pubblicati** dello stesso account, pertinenti e nella lingua del modulo.
2. **RF-2** — Il suggerimento è **passivo**: non blocca l'invio, non obbliga a leggere nulla, si può chiudere e
   ignorare, e il pulsante di invio resta sempre attivo e nella stessa posizione.
3. **RF-3** — Aprire un articolo suggerito lo mostra sul portale pubblico (storia `0031`) **senza perdere** quanto già
   scritto nel modulo: tornando indietro, il testo è ancora lì.
4. **RF-4** — Se il visitatore apre un articolo suggerito e **non** invia la richiesta entro la sessione, l'episodio è
   contato come **richiesta evitata**; il conteggio del periodo è visibile all'account come un numero, senza alcun
   dato della persona.
5. **RF-5** — I suggerimenti passano dalla **stessa superficie pubblica in sola lettura** del portale: solo articoli
   pubblicati, solo di quell'account, con lo stesso limite di frequenza. Non si apre una seconda porta.
6. **RF-6** — Il **testo digitato non si conserva**: serve a interrogare e non finisce in nessuna tabella e in nessun
   registro. Se la richiesta poi viene inviata, l'oggetto si conserva perché è la richiesta, non perché è stato
   cercato.
7. **RF-7** — Se il portale è disattivato, se non ci sono articoli pubblicati o se la ricerca non risponde, il modulo
   funziona **esattamente come prima**: nessuno spazio vuoto, nessun messaggio d'errore, nessun ritardo dell'invio.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1) — deviazione già dichiarata dalla storia `0031`.** Il visitatore non ha un
  token: il `tenant_id` si ricava dall'**identificativo pubblico opaco** del portale associato al modulo, verificato
  a ogni richiesta. Un `tenant_id` che arrivasse dai parametri, dal corpo o da un'intestazione viene **ignorato**.
  Valgono le stesse tre condizioni cumulative: **sola lettura**, **solo articoli**, **solo quelli in stato
  `published` e non cancellati**. Nessuna estensione della deviazione: questa storia la **riusa**, non la allarga.
- **RT-2 — Interfaccia di programmazione (§2).** Rotta pubblica
  `GET /api/helpdesk/v1/public/{portale}/articles/suggest?q=<oggetto>&locale=<lingua>`, che restituisce al più
  cinque risultati **minimizzati** (titolo, categoria, indirizzo dell'articolo sul portale) e **non** il corpo:
  meno dati escono da una superficie pubblica, meglio è. Nessun verbo di scrittura sul prefisso `/public/`; errori
  in `application/problem+json`; limitazione di frequenza per indirizzo di rete con risposta `429`; definizione
  OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V<N>__deflection_event.sql` sullo schema `app_helpdesk`: tabella
  `deflection_event` con `tenant_id`, chiave primaria UUID versione 7, riferimento all'articolo aperto, momento,
  esito (`richiesta evitata` / `richiesta inviata comunque`), colonne di controllo e cancellazione logica.
  **Nessun identificativo della persona, nessun indirizzo di rete, nessun testo digitato**: la riga serve a contare,
  non a ricostruire chi ha fatto cosa. La ricerca usa gli stessi indici della storia `0030`: nessun motore esterno.
- **RT-4 — Modulo frontend (§3, §5).** Il suggerimento vive **dentro il modulo di contatto** della storia `0013`,
  che è una superficie pubblica a sé e non il backoffice: usa gli stessi token del sistema di design con il
  colore-categoria `teal`, funziona in tema chiaro e scuro ed è leggibile da telefono. Il numero delle richieste
  evitate compare invece nella sezione del backoffice, letto con il client generato.
- **RT-5 — Cinque lingue (§4).** **Due elenchi da non confondere.** Le etichette del pannello di suggerimento
  seguono la **lingua del modulo**, cioè quella scelta dal cliente per il proprio sito e proposta al visitatore
  (storia `0013`), fra `en, it, fr, es, de` con ricaduta su `en`; il **titolo dell'articolo** è invece nella lingua
  dell'articolo e non viene tradotto. L'interfaccia dell'operatore che mostra il conteggio passa dallo spazio-nomi
  `helpdesk` ed è presente in tutte e cinque le lingue.
- **RT-6 — Varchi e quota (§6, §7).** Il suggerimento **non consuma** la metrica `agents`. Non attraversa la catena
  dei varchi dell'utente, perché non c'è un utente: al suo posto valgono l'esistenza del portale, il suo stato attivo
  e il limite di frequenza. Se il portale è spento (per disattivazione o per abbonamento non attivo, secondo la
  decisione della storia `0031`) i suggerimenti non compaiono e il modulo resta pienamente funzionante: **la
  ricezione di una richiesta di assistenza non si degrada mai** a causa di una funzione accessoria.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento nuovo: `cerca_articoli` è già dichiarato dalla
  storia `0030` e opera sugli articoli dell'account, non su questa superficie. Il numero delle richieste evitate
  entra fra i valori restituiti da `stato_del_servizio`, dichiarato dalla storia `0034`: qui si dichiara **il dato**,
  non lo strumento. Il contratto vive dentro il servizio; il server conversazionale è di piattaforma e non ancora
  implementato (UC 0061-0063).
- **RT-8 — Dati personali (§10).** **Nessun dato personale nuovo, a due condizioni che sono requisiti**: il testo
  digitato non si conserva (RF-6) e l'evento di richiesta evitata non porta alcun identificativo della persona né
  l'indirizzo di rete (RT-3). Con queste due condizioni `deflection_event` è un dato **statistico dell'account** e
  non un dato personale, e non genera voci nuove nel manifesto
  `docs/compliance/manifests/helpdesk.yaml`; va comunque inclusa in `purgeData` del contratto
  `HelpdeskDataContract` per la chiusura dell'account. Se in implementazione servisse un identificativo di sessione
  per riconoscere «stessa visita», deve essere **volatile e locale al browser** (memoria di sessione, non un cookie
  di tracciamento, nessun banner di consenso): è comunque una scelta da confermare (§8). **Nessun tracciamento**:
  niente strumenti di analisi dentro il modulo, nessun uso secondario. Su questa app appgrove è **responsabile del
  trattamento** per conto dell'azienda cliente, non titolare.
- **RT-9 — Registrazione eventi (§14).** Gli eventi `suggerimento mostrato`, `articolo suggerito aperto`, `richiesta
  evitata` e `suggerimento respinto per frequenza` sono registrati con `tenant_id`, `app_id`, identificativo
  dell'articolo e identificativo di correlazione, **senza il testo digitato, senza l'indirizzo di rete e senza alcun
  dato del visitatore**. Sulle rotte pubbliche non c'è `user_id`: il campo resta vuoto.

## 4. Criteri di accettazione

**CA-1 — Il suggerimento compare mentre si scrive**
- **Dato** un account con portale attivo e articoli pubblicati sui tempi di consegna
- **Quando** un visitatore scrive nell'oggetto del modulo di contatto una frase sui tempi di consegna
- **Allora** compaiono al più cinque articoli pertinenti con titolo e categoria, il pulsante di invio resta attivo e
  nella stessa posizione, e il pannello si può chiudere

**CA-2 — Nessun articolo, nessun danno**
- **Dato** un account con il portale disattivato, oppure senza alcun articolo pubblicato
- **Quando** un visitatore compila il modulo
- **Allora** il modulo si comporta esattamente come prima di questa storia: nessuno spazio vuoto, nessun errore,
  nessun ritardo, e la richiesta si invia normalmente

**CA-3 — Isolamento fra account e nessuna bozza**
- **Dato** due account `A` e `B`, ciascuno con articoli pubblicati e bozze
- **Quando** dal modulo di `A` si sostituisce l'identificativo pubblico con quello di `B`, oppure si aggiunge un
  `tenant_id` nei parametri, oppure si cerca una parola che compare **solo** in una bozza
- **Allora** il parametro forzato è ignorato, nessuna bozza compare mai fra i suggerimenti, e non si raggiunge
  nessun'altra entità dell'applicazione

**CA-4 — La richiesta evitata si conta**
- **Dato** un visitatore che apre un articolo suggerito e poi abbandona il modulo senza inviare
- **Quando** l'account guarda il numero del periodo
- **Allora** il conteggio delle richieste evitate è aumentato di uno, e la riga registrata non contiene alcun dato
  della persona né il testo che aveva digitato

**CA-5 — Limite di frequenza e degrado morbido**
- **Dato** un indirizzo di rete che supera la soglia delle richieste di suggerimento
- **Quando** continua a scrivere nell'oggetto
- **Allora** la chiamata riceve `429`, il pannello dei suggerimenti semplicemente non si aggiorna, e **l'invio della
  richiesta continua a funzionare**

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend`, `frontend`, `compliance`; l'intera suite prima del
      commit);
- [ ] prove di **unità** sulla minimizzazione dei risultati e sul conteggio della richiesta evitata, e di
      **integrazione** sulla rotta pubblica di suggerimento, con database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** in forma di **prova di sicurezza sulla superficie pubblica**:
      identificativo di un altro account, `tenant_id` forzato nei parametri, parola presente solo in una bozza,
      tentativo di ottenere il corpo dell'articolo — tutti respinti allo stesso modo;
- [ ] **prova end-to-end**: *coprire ora* — passo «scrivi l'oggetto nel modulo, vedi il suggerimento e apri
      l'articolo» del percorso `[J-HELPDESK]`, e registro di copertura
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) aggiornato;
- [ ] **traduzioni**: pannello del modulo nella lingua del modulo fra tutte e cinque (`en, it, fr, es, de`) con
      ricaduta su `en`; interfaccia dell'operatore in tutte e cinque;
- [ ] **manifesto dei dati**: nessuna voce nuova, **a condizione** che RF-6 e RT-3 siano rispettati;
      `deflection_event` presente in `purgeData`; la verifica va fatta, non data per scontata;
- [ ] **registro delle decisioni** `changes/NNNN-*/decisions.json` compilato, con annotate la scelta di non
      conservare il testo digitato e la forma dell'eventuale identificativo di sessione;
- [ ] contratto degli **strumenti conversazionali**: nessuno nuovo, con la nota che il conteggio entra in
      `stato_del_servizio` (storia `0034`);
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0013` — modulo web di contatto | Il suggerimento vive dentro quel modulo: senza, non c'è dove metterlo |
| storia `0029` — articoli della base di conoscenza | Si suggerisce solo ciò che è pubblicato: la distinzione bozza/pubblicato è la regola su cui si fonda RF-1 |
| storia `0031` — portale pubblico degli articoli | Fornisce la superficie pubblica in sola lettura e l'indirizzo a cui puntano i suggerimenti. Questa storia la riusa e non la allarga |
| storia `0034` — strumenti di lettura | Ospiterà il numero delle richieste evitate dentro `stato_del_servizio`; qui si produce il dato, non lo strumento |

## 7. Fuori ambito

- **La scrittura e la pubblicazione degli articoli**: storia `0029`.
- **La ricerca dell'articolo mentre l'operatore risponde**: storia `0030`.
- **La pagina che mostra l'articolo al visitatore**: storia `0031`. Qui si suggerisce e si rimanda, non si mostra il
  corpo.
- **Il suggerimento mentre si scrive il corpo del messaggio, e non solo l'oggetto**: rimandato. Sarebbe più efficace
  ma anche più invasivo e più costoso in chiamate; ha senso valutarlo dopo aver misurato quanto funziona sull'oggetto,
  che è il motivo per cui RF-4 esiste.
- **Un assistente che scrive una risposta automatica al visitatore prima dell'invio**: fuori ambito, ed è una
  esclusione di merito. La risposta al cliente esce solo dopo che una persona l'ha approvata (§7 della descrizione
  dell'applicazione): un suggerimento è un rimando a un testo già scritto e approvato, non una risposta generata.
- **Il confronto fra richieste evitate e richieste ricevute come indicatore di efficacia**: il cruscotto è della
  storia `0028`; qui si fornisce il numero grezzo.

## 8. Punti aperti

- **Come si riconosce «stessa visita» senza tracciare.** Per contare una richiesta evitata bisogna sapere che chi ha
  aperto l'articolo è la stessa persona che non ha inviato. La proposta è un identificativo **volatile e locale al
  browser** (memoria di sessione, cancellato alla chiusura), che non è un cookie di tracciamento e non richiede
  banner. È una scelta con effetti sulla postura privacy della piattaforma e **la conferma è dello sviluppatore**;
  se la conferma non arriva, il ripiego onesto è contare solo «articoli suggeriti aperti» e dirlo per quello che è,
  invece di dichiarare una misura che non regge.
- **La lunghezza minima dell'oggetto prima di suggerire, e ogni quanto interrogare.** Troppo presto si suggerisce a
  caso, troppo tardi il cliente ha già finito di scrivere. La proposta è una soglia di caratteri più una pausa di
  digitazione, tarata in implementazione su dati di prova realistici. **La chiude chi implementa**, e la scelta va
  nel registro delle decisioni.
- **Se le richieste evitate debbano comparire nel cruscotto o restare un numero a parte.** È direzione di prodotto e
  tocca la storia `0028`: annotata qui perché non si perda, **la chiude lo sviluppatore**.
