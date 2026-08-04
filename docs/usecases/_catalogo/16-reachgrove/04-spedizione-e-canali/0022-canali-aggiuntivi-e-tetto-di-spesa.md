# 0022 — Canali aggiuntivi e tetto di spesa

**Applicazione**: 16 — ReachGrove (`campaigns`) · **Epica**: 04 — Spedizione e canali
**Storia**: `0022` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0007`, `0019`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che ogni tanto ha bisogno di un canale diverso dalla posta elettronica
> voglio poter attivare un canale collegando il mio contratto con un fornitore e darmi un tetto di spesa
> così da usarlo quando serve senza rischiare una bolletta che non mi aspetto.

**Contesto.** La scheda di catalogo dà a ReachGrove tre canali, ma il catalogo stesso ha **escluso** l'app 05
ChatGrove per tre ragioni: trasferimento di dati verso un paese terzo, regole imposte dal fornitore, e un costo
variabile per messaggio che erode il margine. Le tre ragioni valgono identiche qui, e i numeri lo confermano:
0,0927 $ per un messaggio breve verso l'Italia contro circa 0,0001 $ per un messaggio di posta elettronica — mille
volte tanto ([application-description.md](../application-description.md) §2.6 fonte 8 e §11.1). Rivenderli dentro
un piano da 39 € al mese significa che poche centinaia di messaggi bruciano l'intero canone.

La via scelta è quella che tiene in piedi il prodotto senza ripetere l'errore: **appgrove non rivende invii**. La
posta elettronica parte dalla nostra infrastruttura; i canali aggiuntivi partono dal contratto che **il cliente**
ha con il proprio fornitore. Questa storia costruisce il posto in cui quei canali si innestano — l'astrazione del
canale, l'attivazione per account, il tetto di spesa — e le due storie successive collegano i fornitori concreti.

## 2. Requisiti funzionali

1. **RF-1** — Esiste un'**astrazione di canale** con tre tipi: `posta_elettronica` (nostro, sempre disponibile),
   `messaggi_brevi` e `messaggistica` (facoltativi, del cliente). Campagne, modelli, consensi e percorsi automatici
   parlano di «canale», non di fornitore.
2. **RF-2** — Un canale aggiuntivo si attiva **per account** collegando un contratto del cliente (storie `0023` e
   `0024`); finché non è attivo, il canale non compare come scelta nella composizione della campagna — non compare
   disabilitato con un invito a comprare: **non compare**.
3. **RF-3** — Per ogni canale attivo il cliente si dà un **tetto di spesa** per finestra (mensile) e un tetto per
   singola campagna, espressi nel numero di messaggi e nella valuta del suo contratto quando il costo unitario è
   noto.
4. **RF-4** — Raggiunto il tetto, l'invio **su quel canale** si ferma: le campagne in corso si mettono in pausa,
   quelle programmate su quel canale passano a `bloccata`. La posta elettronica **non** è toccata: un canale che si
   ferma non ferma gli altri.
5. **RF-5** — Il consenso è **per canale**: un iscritto che ha acconsentito alla posta elettronica **non** è
   contattabile sui messaggi brevi, e viceversa. Non esiste alcuna forma di estensione automatica del consenso da
   un canale all'altro.
6. **RF-6** — L'invio su canale aggiuntivo **consuma comunque** la metrica `messages_sent`, anche se il messaggio
   lo paga il cliente al proprio fornitore. Il motivo va **scritto nell'interfaccia**, non solo nel listino: la
   segmentazione, la verifica del consenso, la soppressione e il tracciamento li facciamo noi, ed è quello che il
   piano copre.
7. **RF-7** — I canali aggiuntivi sono disponibili **solo** sul piano alto, coerentemente con la proposta di
   listino (§5 della descrizione); sugli altri piani la sezione spiega cosa sono e non li attiva.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** `channel_connection` e i tetti di spesa filtrano per `tenant_id` preso
  dal token verificato. Il consumo di un account non è mai visibile né imputabile a un altro.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `GET|POST /api/campaigns/v1/channels`,
  `PUT /api/campaigns/v1/channels/{tipo}/spending-cap`, `POST /api/campaigns/v1/channels/{tipo}/disable`. Corpo
  validato, errori in `application/problem+json`, definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Tabella `channel_connection` (storia `0002`) con tipo, stato, riferimento **cifrato**
  alle credenziali, tetto di spesa e consumo della finestra; le campagne acquisiscono la colonna del canale. Schema
  `app_campaigns`, chiave primaria UUID versione 7, colonne di controllo, cancellazione logica. **Le credenziali
  non stanno in chiaro nella tabella**: si conserva un riferimento a un deposito di segreti.
- **RT-4 — Modulo frontend (§3, §5).** Sezione «Canali» del modulo `campaigns`: elenco dei canali con stato,
  impostazione del tetto, consumo della finestra, e il riquadro che spiega **perché** un invio su canale del
  cliente consuma comunque la quota. Solo token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Nomi dei canali, stati, testo del tetto di spesa e la spiegazione del doppio
  conteggio, tutti dallo spazio-nomi `campaigns` e presenti in `en, it, fr, es, de`. La spiegazione del doppio
  conteggio è un testo delicato: tradotto male sembra un doppio addebito.
- **RT-6 — Varchi e quota (§6, §7).** Ogni invio su qualunque canale prenota una unità di `messages_sent` (natura
  `flow`); a quota esaurita `429`. Il **tetto di spesa** è un limite **del cliente su sé stesso**, distinto dalla
  quota: al suo raggiungimento la risposta è `409` con motivo «tetto di spesa raggiunto», non `429`, perché il
  rimedio è diverso (alzare il proprio tetto, non cambiare piano). L'attivazione dei canali aggiuntivi richiede il
  piano che li comprende, altrimenti `402`.
- **RT-7 — Esposizione conversazionale (§12).** `elenca_campagne` (lettura) restituisce anche il canale.
  L'attivazione di un canale e la modifica del tetto di spesa **non** sono esposte alla chat: comportano credenziali
  e impegni di spesa verso l'esterno, e si fanno dove si vede cosa si sta firmando. Scelta dichiarata.
- **RT-8 — Dati personali (§10).** Le credenziali del fornitore **non sono dati personali** dell'iscritto ma sono
  segreti dell'account: si dichiarano nel manifesto come voce di sicurezza e non compaiono mai in chiaro
  nell'esportazione. Ciò che cambia per i dati delle persone è il **destinatario**: attivando un canale il cliente
  autorizza la trasmissione del **numero di telefono** dei suoi iscritti a un fornitore esterno; l'app deve dirlo
  con chiarezza al momento dell'attivazione, e la voce corrispondente va nel manifesto in italiano e inglese.
- **RT-9 — Registrazione eventi (§14).** «Canale attivato», «canale disattivato», «tetto di spesa modificato»,
  «invio fermato per tetto di spesa» con `tenant_id`, `app_id`, `user_id` e identificativo di correlazione.
  **Mai** le credenziali, nemmeno in forma parziale, e mai i numeri di telefono.

## 4. Criteri di accettazione

**CA-1 — Un canale non attivo non si sceglie**
- **Dato** un account che non ha collegato nessun contratto per i messaggi brevi
- **Quando** compone una campagna
- **Allora** fra i canali disponibili c'è solo la posta elettronica; il canale non attivo **non compare** nella
  scelta

**CA-2 — Tetto di spesa raggiunto**
- **Dato** un account con canale messaggi brevi attivo e tetto mensile di 500 messaggi, già consumati
- **Quando** una campagna su quel canale tenta di partire
- **Allora** la campagna si ferma con motivo «tetto di spesa raggiunto» (`409`), e una campagna di posta
  elettronica programmata nello stesso momento **parte regolarmente**

**CA-3 — Il consenso non si estende da un canale all'altro**
- **Dato** un iscritto con consenso registrato per la posta elettronica e nessun consenso per i messaggi brevi, che
  ha un numero di telefono in scheda
- **Quando** viene incluso in un segmento e si tenta una campagna su messaggi brevi
- **Allora** risulta **non inviabile** su quel canale e viene saltato; nessun parametro produce un esito diverso

**CA-4 — Il doppio conteggio è spiegato e applicato**
- **Dato** un account che invia 100 messaggi brevi con il proprio contratto
- **Quando** la spedizione si conclude
- **Allora** la metrica `messages_sent` risulta consumata di 100 unità, e l'interfaccia mostra accanto al consumo
  la spiegazione del perché

**CA-5 — Isolamento fra account**
- **Dato** due account `A` e `B` con canali attivi
- **Quando** un utente di `A` chiede la configurazione dei canali o prova a leggere il riferimento alle credenziali
  di `B`
- **Allora** vede solo i propri, e le credenziali non sono leggibili nemmeno per il proprio account: si vede lo
  stato della connessione, non il segreto

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (`backend`, `frontend`, `compliance`; l'intera suite prima del commit);
- [ ] prove di **unità** sulla contabilità del tetto di spesa e sulla separazione fra tetto e quota; prove di
      **integrazione** sull'attivazione, sul blocco per tetto e sull'indipendenza fra canali;
- [ ] prova di **isolamento fra account** su canali, tetti e consumi;
- [ ] prova esplicita che le credenziali **non** sono leggibili da nessuna rotta e non compaiono nei registri;
- [ ] **prova end-to-end**: rimando motivato — il percorso `[J-CAMPAIGNS]` (storia `0037`) copre il canale di posta
      elettronica; i canali aggiuntivi richiedono un contratto esterno del cliente e restano coperti da prove di
      integrazione con fornitore simulato. Voce `da-coprire` nel registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml), con motivo e storia proprietaria
      `0023`;
- [ ] **traduzioni** in tutte e cinque le lingue, con revisione attenta del testo sul doppio conteggio;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese: `channel_connection` come segreto dell'account e la
      trasmissione del numero di telefono al fornitore esterno come nuova destinazione dei dati;
- [ ] **registro delle decisioni** compilato, con annotato che appgrove **non rivende invii** e perché l'invio su
      canale del cliente consuma comunque la quota;
- [ ] contratto degli **strumenti conversazionali**: nessuno nuovo; attivazione e tetto **non** esposti, con la
      motivazione scritta;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storia `0007` | Il consenso è registrato **per canale**: senza quella struttura questa storia non ha su cosa poggiare |
| Storia `0019` | La spedizione è il percorso in cui il canale viene scelto e il tetto applicato |
| Conferma del listino ([application-description.md](../application-description.md) §5) | Il fatto che i canali aggiuntivi stiano solo nel piano alto e che consumino la quota è una decisione di prezzo, fermata di escalation dello sviluppatore |

## 7. Fuori ambito

- il collegamento del fornitore di **messaggi brevi**: è la storia `0023`;
- il collegamento del fornitore di **messaggistica**: è la storia `0024`;
- la rivendita di invii su canali di terzi: **esclusa per scelta di prodotto**, non rimandata
  ([application-description.md](../application-description.md) §1 e §11.1);
- la fatturazione del consumo del cliente presso il suo fornitore: non passa da noi, non la vediamo e non la
  riconciliamo;
- i messaggi **transazionali** (conferme, promemoria, fatture): non passano da qui, sono delle rispettive app.

## 8. Punti aperti

- **Il tetto di spesa espresso in denaro.** Conoscere il costo unitario del contratto del cliente richiederebbe di
  leggerlo dal fornitore, e non tutti lo espongono. La proposta è un tetto **in numero di messaggi**, con
  l'importo mostrato solo quando il fornitore fornisce una tariffa affidabile. Chiude lo sviluppatore.
- **Che cosa fare del consumo residuo quando il cliente stacca il contratto a metà campagna.** La campagna si ferma:
  ma se ha già consumato quota per messaggi mai partiti, quella quota va restituita, con la stessa regola della
  storia `0020`. Va confermato che valga anche qui.
- **Prezzi e collocazione nei piani**: fermata di escalation dello sviluppatore.
