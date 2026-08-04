# 0028 — Cruscotto del servizio

**Applicazione**: 12 — DeskGrove Support (`helpdesk`) · **Epica**: 05 — Tempi di risposta e livello di servizio
**Storia**: `0028` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0010`, `0025`, `0027`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che ha cinque minuti la mattina
> voglio quattro numeri che mi dicano se stiamo rispondendo in tempo e se i clienti sono contenti
> così da accorgermi che qualcosa non va senza dover leggere le richieste una per una.

**Contesto.** È l'ultima storia dell'epica e la sua ragion d'essere: tutto ciò che le sta prima — calendario,
politiche, orologi, avvisi, soddisfazione — produce dati che nessuno vede in forma d'insieme. Il
[documento capofila](../application-description.md) §2.5 è netto sulla forma che deve avere: *un numero solo che
dica se si sta rispondendo in tempo, non un cruscotto con dodici grafici*. Quattro numeri, quindi, e nessun
grafico: sono la stessa cosa che lo strumento conversazionale `stato_del_servizio` restituisce (capofila §7), ed è
volutamente lo stesso calcolo, perché due calcoli che rispondono alla stessa domanda finiscono sempre per dare due
risposte diverse.

## 2. Requisiti funzionali

1. **RF-1** — Il cruscotto mostra quattro numeri su un periodo scelto: richieste aperte alla fine del periodo,
   tempo medio di prima risposta in ore lavorative, numero di scadenze violate, voto medio di soddisfazione.
2. **RF-2** — Il periodo si sceglie fra intervalli predefiniti — ultimi 7 giorni, ultimi 30, ultimi 90, mese
   corrente — e ogni numero dichiara a quale periodo si riferisce.
3. **RF-3** — Il cruscotto si può restringere a una coda; non esiste alcun filtro per singolo operatore.
4. **RF-4** — Ogni numero porta all'elenco delle richieste che lo compongono, con i filtri della vista già
   applicati (storia `0010`).
5. **RF-5** — Il tempo medio di prima risposta e le violazioni si calcolano con le stesse funzioni di ore
   lavorative delle storie `0023` e `0025`: nessun secondo calcolo del tempo vive dentro il cruscotto.
6. **RF-6** — Il voto medio non viene mostrato finché le risposte del periodo non superano una soglia minima
   (proposta: cinque), per evitare che una media risalga a una singola persona; sotto la soglia il riquadro dice
   che le risposte sono troppo poche.
7. **RF-7** — Il cruscotto è di **sola lettura**: nessuna azione, nessuna modifica, nessun dato personale nei
   conteggi.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni aggregazione filtra per `tenant_id` preso dal token verificato; un
  `tenant_id` che arrivasse dal corpo della richiesta o dai parametri viene ignorato. Nessuna interrogazione
  aggrega fra account: la coda della console di amministrazione che legge fra tutti gli account è l'eccezione
  dell'assistenza **interna** della piattaforma e **non** si applica qui (capofila §10, separazione n. 2).
- **RT-2 — Interfaccia di programmazione (§2).** Rotta `GET /api/helpdesk/v1/cruscotto?periodo=&coda=` che
  restituisce i quattro numeri più il conteggio delle risposte all'indagine (per la soglia di RF-6); parametri
  validati contro l'elenco chiuso dei periodi; errori in `application/problem+json`; definizione OpenAPI aggiornata
  nello stesso commit. Il calcolo vive in un unico componente di servizio, condiviso con lo strumento
  conversazionale.
- **RT-3 — Persistenza (§8).** Nessuna tabella nuova: i numeri si ricavano per aggregazione dalle tabelle
  esistenti sullo schema `app_helpdesk` — richiesta, `service_breach`, `satisfaction_survey`. Si aggiungono gli
  indici necessari a rendere le aggregazioni sostenibili su (`tenant_id`, coda, istante di apertura) e
  (`tenant_id`, istante della violazione), in una migrazione `V<N>__dashboard_indexes.sql`. Nessuna vista
  materializzata e nessuna tabella di riepilogo: sui volumi del segmento sarebbero complessità senza contropartita.
- **RT-4 — Modulo frontend (§3, §5).** Sezione *Cruscotto* del modulo `helpdesk`: quattro riquadri, il selettore
  del periodo, il selettore della coda. Dati letti con il client generato; solo token del sistema di design,
  colore-categoria `teal`; funziona in tema chiaro e scuro; i riquadri restano leggibili sullo schermo di un
  telefono; controllo automatico di accessibilità sulla schermata.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe visibili — nomi dei quattro numeri, etichette dei periodi, il
  messaggio «risposte troppo poche», l'unità «ore lavorative» — passano dallo spazio-nomi `helpdesk` e sono
  presenti in `en, it, fr, es, de`. I numeri e le date si formattano secondo la lingua scelta.
- **RT-6 — Varchi e quota (§6, §7).** Il cruscotto non consuma la metrica `agents` (natura `stock`). Restano i
  varchi a monte: `401` senza token valido, `402` con abbonamento `canceled`, `403` per ruolo insufficiente. La
  lettura del cruscotto è consentita anche a chi **non occupa un posto operatore**, perché chi guarda i rapporti non
  consuma quota (capofila §3, voce «metrica di quota»): è la prova che quella definizione è stata implementata
  davvero.
- **RT-7 — Esposizione conversazionale (§12).** Questa storia realizza il contratto di
  `stato_del_servizio(periodo) → richieste aperte, tempo medio di prima risposta, scadenze violate, soddisfazione
  media`, marcato **lettura**, nessuna conferma umana, idempotente. Restituisce gli stessi aggregati della rotta e
  **nessun** elenco di richieste, nessun nome, nessun contenuto. Il contratto vive dentro il servizio `helpdesk`;
  il server conversazionale è di piattaforma e non è ancora implementato (UC 0061-0063).
- **RT-8 — Dati personali (§10).** Nessun dato personale nuovo e nessuna tabella nuova: il cruscotto legge dati già
  dichiarati nel manifesto e restituisce solo aggregati. Due presidi espliciti: la soglia minima sul voto medio
  (RF-6), che impedisce di risalire alla singola persona, e l'assenza del filtro per operatore (RF-3), che tiene
  il cruscotto fuori dalla sorveglianza dei dipendenti dell'azienda cliente. Nessuna risposta della rotta contiene
  il corpo di un messaggio o un commento dell'indagine.
- **RT-9 — Registrazione eventi (§14).** L'evento «cruscotto consultato» è registrato con `tenant_id`, `app_id`,
  `user_id`, periodo e coda richiesti, e identificativo di correlazione; nessun dato personale e nessun valore
  aggregato nei registri.

## 4. Criteri di accettazione

**CA-1 — I quattro numeri**
- **Dato** un account con dieci richieste aperte, quattro chiuse nel periodo con prima risposta media di 3 ore
  lavorative, due violazioni registrate e sei voti dell'indagine
- **Quando** il titolare apre il cruscotto sugli ultimi 30 giorni
- **Allora** vede esattamente quei quattro valori, ciascuno con l'indicazione del periodo a cui si riferisce

**CA-2 — Periodo senza dati**
- **Dato** un account appena attivato · **Quando** apre il cruscotto sugli ultimi 7 giorni · **Allora** vede zeri e
  un messaggio che spiega che non ci sono ancora dati, non un errore e non un riquadro vuoto

**CA-3 — Soglia minima sulla soddisfazione**
- **Dato** un periodo con tre soli voti raccolti
- **Quando** si apre il cruscotto
- **Allora** il riquadro della soddisfazione non mostra alcuna media e dichiara che le risposte sono troppo poche,
  mentre gli altri tre numeri restano visibili

**CA-4 — Nessun dato personale negli aggregati**
- **Dato** un periodo con richieste contenenti nomi e commenti dell'indagine
- **Quando** si esamina la risposta della rotta del cruscotto
- **Allora** contiene solo numeri, e nessun nome, recapito, oggetto di richiesta o commento; non esiste alcun
  filtro per singolo operatore

**CA-5 — Dal numero all'elenco**
- **Dato** il riquadro «scadenze violate» che mostra 2 · **Quando** l'utente lo apre · **Allora** raggiunge
  l'elenco delle due richieste violate di quel periodo, con i filtri già applicati e coerenti col numero mostrato

**CA-6 — Isolamento fra account**
- **Dato** due account `A` e `B` con richieste, violazioni e voti nello stesso periodo
- **Quando** un utente di `A` apre il cruscotto, anche forzando l'identificativo di una coda di `B` nel parametro
  del filtro
- **Allora** i quattro numeri riguardano soltanto `A`, e il filtro su una coda di `B` risponde come se quella coda
  non esistesse

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (backend e frontend; l'intera suite prima del commit);
- [ ] prove di **unità** sulle quattro aggregazioni — periodo vuoto, soglia della soddisfazione, media calcolata in
      ore lavorative, violazioni contate una sola volta — e di **integrazione** sulla rotta con database effimero e
      migrazioni vere;
- [ ] prova di **isolamento fra account** sulle aggregazioni, con dati di due account nello stesso periodo;
- [ ] **prova end-to-end**: *rimando* alla storia `0037`, proprietaria del percorso `[J-HELPDESK]`, che si chiude
      con la lettura del cruscotto dopo il percorso completo; motivo e storia proprietaria annotati nel registro di
      copertura;
- [ ] **traduzioni** presenti in tutte e cinque le lingue (`en, it, fr, es, de`), con formattazione di numeri e date
      per lingua;
- [ ] **manifesto dei dati** non richiede voci nuove: annotare esplicitamente nel registro delle decisioni che il
      cruscotto restituisce solo aggregati;
- [ ] **registro delle decisioni** `changes/NNNN-*/decisions.json` compilato, in particolare sull'assenza del filtro
      per operatore, sulla soglia minima della soddisfazione e sulla rinuncia ai grafici;
- [ ] contratto degli **strumenti conversazionali**: `stato_del_servizio` dichiarato come lettura, con lo stesso
      calcolo della rotta;
- [ ] controllo automatico di **accessibilità** verde sulla schermata;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0025` (orologi e violazioni) | Due dei quattro numeri sono il tempo di prima risposta e le violazioni |
| storia `0027` (indagine di soddisfazione) | Il quarto numero è il voto medio |
| storia `0010` (elenco, ricerca e viste) | Ogni numero porta all'elenco filtrato corrispondente |
| epica di piattaforma non implementata (UC 0061-0063) | `stato_del_servizio` sarà richiamabile solo quando il livello conversazionale esisterà: qui se ne dichiara il contratto |

## 7. Fuori ambito

- **I grafici e gli andamenti nel tempo**: esclusi di proposito, per la ragione scritta nel capofila §2.5.
  Aggiungerli sarebbe l'inizio dell'analisi avanzata che il segmento non usa.
- **Il confronto fra periodi** («questo mese contro il mese scorso»): rimandato per la stessa ragione; se
  arrivasse, andrebbe scritto come una storia sua, non nascosto qui.
- **L'esportazione dei numeri in un file**: rimandata; il cruscotto è di sola lettura sullo schermo.
- **Il filtro e il confronto per singolo operatore**: escluso, ed è una scelta consapevole — misurare le persone
  una a una è un altro prodotto, con conseguenze sul rapporto di lavoro dentro l'azienda cliente. Se lo
  sviluppatore volesse cambiarlo, è una decisione di direzione di prodotto.
- **Il rapporto periodico mandato per posta elettronica** («il tuo lunedì mattina»): rimandato, è una funzione con
  un proprio ciclo di invii.
- **Il numero di richieste per canale o per etichetta**: rimandato, non è fra i quattro numeri che il capofila
  indica come necessari.

## 8. Punti aperti

- **La soglia minima di risposte per mostrare la media** (proposta: cinque) è un compromesso fra utilità e tutela
  della singola persona che ha votato. **Decide lo sviluppatore.**
- **Se il tempo medio di prima risposta debba essere una media o una mediana**: su volumi piccoli una sola risposta
  arrivata dopo tre giorni sposta la media e rende il numero poco rappresentativo; la mediana è più onesta ma è
  meno immediata da spiegare. **Decide lo sviluppatore.**
- **Se il cruscotto debba essere visibile a tutti gli operatori o ai soli ruoli `owner` e `admin`**: mostrare le
  violazioni a tutta la squadra può motivare oppure mettere sotto pressione. È una scelta sul rapporto fra le
  persone dell'azienda cliente, non una scelta tecnica. **Decide lo sviluppatore.**
