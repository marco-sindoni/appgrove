# 0024 — Contratto del cliente per la messaggistica

**Applicazione**: 16 — ReachGrove (`campaigns`) · **Epica**: 04 — Spedizione e canali
**Storia**: `0024` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore — **scritta e NON implementabile**
**Dipende da**: `0007`, `0022`, `0023`, **revisione legale (§11.1 della descrizione)**
**Ultimo aggiornamento**: 2026-08-03

> ⚠️ **Storia sospesa per decisione dichiarata.** Questa storia **non si implementa** finché la revisione legale non
> ha chiuso la questione del trasferimento di numeri di telefono verso un paese terzo
> ([application-description.md](../application-description.md) §11.1 e §6 punto 3). Il fatto che il contratto col
> fornitore sia del cliente **non fa sparire il trasferimento**: sposta chi ne risponde, non se avviene, e la
> trasmissione materiale la eseguiamo noi. È il punto aperto più serio dell'app. La storia è scritta perché sia
> pronta e perché il confine sia chiaro, non perché sia in coda di lavorazione.

## 1. Narrazione

> Come titolare i cui clienti rispondono più volentieri sulla messaggistica che alla posta elettronica
> voglio collegare il mio contratto con il fornitore e mandare i messaggi da lì
> così da raggiungerli dove leggono, restando io il titolare del rapporto col fornitore.

**Contesto.** È il canale che i clienti chiedono più spesso e insieme quello che il catalogo ha già valutato e
scartato una volta: l'app 05 ChatGrove è stata **esclusa** proprio per questo canale
([_escluse/README.md](../../_escluse/README.md)). Portando il contratto in capo al cliente cadono due delle tre
ragioni dell'esclusione — il costo variabile non è nostro, le regole del fornitore le accetta chi firma con lui.
**La terza non cade.**

Il fornitore dominante di questo canale è extra-europeo, la tariffa si paga a messaggio consegnato e dipende dalla
categoria del modello e dal prefisso del destinatario (§2.6 fonte 9). Ma il problema che ferma la storia non è il
prezzo: è che per far partire un messaggio dobbiamo trasmettere un numero di telefono fuori dall'Unione europea, e
la piattaforma vuole i dati personali a riposo in Europa. Chi ne risponde, a che titolo e con quale garanzia è una
domanda che non si chiude in un documento di catalogo.

## 2. Requisiti funzionali

1. **RF-1** — L'account collega il proprio contratto col fornitore di messaggistica: credenziali sue, numero
   registrato suo, fattura sua. Come per i messaggi brevi, appgrove non rivende invii.
2. **RF-2** — Su questo canale si possono inviare **solo modelli approvati** dal fornitore: l'app importa i modelli
   approvati dell'account e non consente di comporre testo libero per il primo messaggio, perché il fornitore lo
   rifiuterebbe. Lo stato di approvazione di ogni modello è visibile.
3. **RF-3** — Al momento del collegamento l'app mostra un **avviso esplicito e non nascondibile**: attivando questo
   canale i numeri di telefono degli iscritti selezionati vengono trasmessi a un fornitore **fuori dall'Unione
   europea**; l'avviso dice chi è il fornitore, cosa riceve e che il cliente è titolare del trattamento verso i
   propri iscritti. Il collegamento richiede una **presa d'atto esplicita**, registrata con momento e utente.
4. **RF-4** — È inviabile **solo** l'iscritto con consenso registrato **per il canale messaggistica**. Come per i
   messaggi brevi, la base giuridica `soft_spam` **non abilita** questo canale.
5. **RF-5** — Gli errori e i rifiuti del fornitore (modello non approvato, finestra di contatto chiusa, numero non
   valido) sono riportati al cliente con il codice originale e la spiegazione in lingua.
6. **RF-6** — Il consumo si conta sul tetto di spesa (storia `0022`) e sulla metrica `messages_sent`, con la stessa
   regola del doppio conteggio già spiegata.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Connessione, modelli approvati e consumi filtrano per `tenant_id` preso
  dal token verificato; l'invio usa sempre le credenziali dell'account proprietario della campagna.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte
  `POST|DELETE /api/campaigns/v1/channels/messaging/connection`,
  `GET /api/campaigns/v1/channels/messaging/templates` (modelli approvati, in sola lettura dal fornitore). Le
  credenziali si accettano solo in scrittura. Errori in `application/problem+json` con il codice del fornitore;
  definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Riga di `channel_connection` con tipo `messaggistica`; segreto nel deposito dei
  segreti e solo il riferimento in tabella; tabella di appoggio per i modelli approvati con il loro stato. Schema
  `app_campaigns`, chiave primaria UUID versione 7, colonne di controllo, cancellazione logica. Va conservata anche
  la **presa d'atto** del trasferimento (chi, quando, quale testo), perché è una prova.
- **RT-4 — Modulo frontend (§3, §5).** Scheda del canale nella sezione «Canali», con l'avviso sul trasferimento in
  posizione dominante — non un riquadro informativo fra gli altri — e la casella di presa d'atto obbligatoria.
  Elenco dei modelli approvati con lo stato. Solo token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe dallo spazio-nomi `campaigns` in `en, it, fr, es, de`. Il
  **testo della presa d'atto** è un testo con valore probatorio: si conserva nella lingua in cui è stato accettato
  e non si traduce a posteriori, esattamente come i testi di consenso.
- **RT-6 — Varchi e quota (§6, §7).** Una unità di `messages_sent` (natura `flow`) per messaggio, più il tetto di
  spesa; canale disponibile solo sul piano che lo comprende, altrimenti `402`.
- **RT-7 — Esposizione conversazionale (§12).** `stato_iscritto` risponde anche per questo canale. Collegamento,
  presa d'atto e invio su questo canale **non** sono esposti alla chat: c'è un trasferimento verso un paese terzo
  che una persona deve accettare guardandolo. Scelta dichiarata.
- **RT-8 — Dati personali (§10).** È la voce più delicata del manifesto
  `docs/compliance/manifests/campaigns.yaml`: trasmissione del numero di telefono a un fornitore **fuori
  dall'Unione europea**, da dichiarare in italiano e inglese con la destinazione, la garanzia applicabile e la
  presa d'atto del cliente. Il fornitore va elencato fra quelli che trattano dati (§6 della descrizione,
  integrazione 3). La **classificazione della change è sostanziale**: va aggiornata la valutazione dei rischi
  **prima** che il canale venga acceso, non dopo.
- **RT-9 — Registrazione eventi (§14).** «Canale messaggistica collegato», «presa d'atto del trasferimento
  registrata», «modelli sincronizzati», «invio rifiutato dal fornitore con codice», con `tenant_id`, `app_id`,
  `user_id` e identificativo di correlazione. Mai credenziali, mai numeri di telefono.

## 4. Criteri di accettazione

**CA-1 — Non si collega senza presa d'atto**
- **Dato** un account che inserisce credenziali valide ma non accetta l'avviso sul trasferimento
- **Quando** tenta di completare il collegamento
- **Allora** il canale **non** si attiva; la presa d'atto è un requisito, non un consiglio

**CA-2 — Solo modelli approvati**
- **Dato** un canale attivo e una campagna composta con testo libero
- **Quando** si esegue il controllo pre-volo
- **Allora** il controllo è rosso con motivo «su questo canale servono modelli approvati dal fornitore», e la
  campagna non si programma

**CA-3 — Il consenso è per canale**
- **Dato** un iscritto con consenso alla posta elettronica e senza consenso alla messaggistica
- **Quando** viene incluso in una campagna su questo canale
- **Allora** risulta non inviabile e viene saltato, con motivo esplicito

**CA-4 — Rifiuto del fornitore leggibile**
- **Dato** un invio rifiutato dal fornitore perché il modello non è più approvato
- **Quando** il ritorno viene elaborato
- **Allora** la consegna si chiude in errore con il codice originale e la spiegazione in lingua, e il modello
  risulta «non più approvato» nell'elenco

**CA-5 — Isolamento fra account**
- **Dato** due account con il canale collegato
- **Quando** si spedisce per conto di uno
- **Allora** vengono usate le sue credenziali e i suoi modelli; quelli dell'altro non sono raggiungibili né visibili

## 5. Definizione di fatto

- [ ] **precondizione bloccante**: la revisione legale ha chiuso la questione del trasferimento verso un paese
      terzo ([application-description.md](../application-description.md) §11.1) e la valutazione dei rischi è
      aggiornata. **Finché questa casella è vuota, la storia non si implementa**, e le altre caselle non si
      spuntano;
- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (`backend`, `frontend`, `compliance`; l'intera suite prima del commit);
- [ ] prove di **unità** sulla regola dei modelli approvati e sul consenso per canale; prove di **integrazione**
      con fornitore simulato, compresi i rifiuti;
- [ ] prova di **isolamento fra account** su connessione, modelli e invio;
- [ ] prova che il collegamento **non** avvenga senza presa d'atto registrata;
- [ ] **prova end-to-end**: rimando motivato — richiede un contratto esterno e un fornitore extra-europeo; coperta
      da prove di integrazione con fornitore simulato. Voce `da-coprire` nel registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml), con motivo e questa storia come
      proprietaria;
- [ ] **traduzioni** in tutte e cinque le lingue, con il testo della presa d'atto conservato nella lingua di
      accettazione;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese con il trasferimento verso il paese terzo, la
      garanzia applicabile e il fornitore fra quelli che trattano dati;
- [ ] **registro delle decisioni** compilato, con annotato il motivo della sospensione e la data in cui è stata
      sciolta;
- [ ] contratto degli **strumenti conversazionali**: nessuno nuovo, con la motivazione scritta;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| **Revisione legale** sul trasferimento di numeri di telefono verso un paese terzo | È la precondizione bloccante: senza, la storia non si implementa ([application-description.md](../application-description.md) §11.1) |
| Storia `0007` | Il consenso per canale |
| Storia `0022` | Astrazione del canale, attivazione, tetto di spesa |
| Storia `0023` | Ne riusa il percorso di collegamento delle credenziali e la gestione degli errori del fornitore: farla prima evita di scrivere due volte la stessa struttura |

## 7. Fuori ambito

- la **conversazione in entrata**: una risposta su questo canale non apre niente qui. Il caso d'uso conversazionale
  dell'app 05 ChatGrove **non viene assorbito**: qui c'è solo l'invio
  ([application-description.md](../application-description.md) §10);
- l'approvazione dei modelli presso il fornitore: la chiede il cliente al fornitore, noi li leggiamo;
- la rivendita di messaggi: esclusa per scelta di prodotto;
- la scelta di un fornitore di messaggistica europeo alternativo: non ne ho valutati, ed è un punto aperto.

## 8. Punti aperti

- **Trasferimento verso un paese terzo** — il punto che sospende la storia. Il contratto è del cliente, ma la
  trasmissione la eseguiamo noi: chi è titolare, chi responsabile, quale garanzia si applica e cosa va scritto
  nell'informativa dell'iscritto. **Chiude la revisione legale**, con lo sviluppatore.
- **Se il canale debba esistere affatto.** L'alternativa onesta è rinunciarvi, come il catalogo ha già fatto con
  l'app 05. Le prime sei epiche dell'app stanno in piedi da sole e la posta elettronica è il canale che il cliente
  tipo usa davvero: rinunciare a questa storia **non toglie la ragione d'essere al prodotto**. È una decisione di
  prodotto dello sviluppatore.
- **Fornitore europeo alternativo per la messaggistica**: non valutato in questa analisi. Se esistesse, cadrebbe
  la ragione che tiene ferma la storia.
