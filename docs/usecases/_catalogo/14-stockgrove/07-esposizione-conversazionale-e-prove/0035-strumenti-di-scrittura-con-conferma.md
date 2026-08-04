# 0035 — Strumenti di scrittura con bozza e conferma

**Applicazione**: 14 — StockGrove (`magazzino`) · **Epica**: 07 — Esposizione conversazionale e prove end-to-end
**Storia**: `0035` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0034`, `0016`, `0017`, `0021`, `0023`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come addetto che ha le mani occupate
> voglio poter dire «scaricane due per il lavoro di stamattina» e vedere cosa succederà **prima** che succeda
> così da muovere il magazzino a voce senza il rischio che un fraintendimento diventi un movimento vero.

**Contesto.** La storia `0034` ha aperto la lettura, che è innocua. La scrittura no: qui un fraintendimento non
produce una risposta sbagliata, produce un **fatto sbagliato dentro un registro che non si cancella**. La regola
di piattaforma è netta e vale come vincolo di progetto — l'intelligenza artificiale prepara, la persona approva
([PRINCIPI-APPGROVE.md](../../_kit/PRINCIPI-APPGROVE.md) §12) — e in questa applicazione ha una ragione in più:
l'unica correzione ammessa è lo storno, quindi ogni movimento sbagliato resta per sempre nella storia, sia pure
annullato. Questa storia dichiara i sei strumenti di scrittura in **due tempi**: lo strumento produce una bozza
con l'effetto atteso, una seconda invocazione la conferma, e solo allora il movimento entra nel registro.

## 2. Requisiti funzionali

1. **RF-1** — Il servizio dichiara sei strumenti marcati `scrittura`: `registra_carico`, `registra_scarico`,
   `trasferisci`, `rettifica_giacenza`, `storna_movimento`, `chiudi_inventario`. **Nessuno** di essi esegue
   direttamente: ognuno produce una **bozza**.
2. **RF-2** — La bozza mostra l'**effetto atteso** in termini che una persona possa verificare in due secondi:
   articolo (codice e descrizione), deposito, quantità del movimento, **quantità prima e quantità dopo** per ogni
   coppia articolo-deposito toccata, e il motivo quando c'è. La bozza **non** ha ancora toccato nulla.
3. **RF-3** — Ogni bozza porta un identificativo, una **chiave di idempotenza** e una **scadenza** breve: scaduta,
   la conferma è rifiutata con l'invito a rifare la richiesta, perché nel frattempo la giacenza può essere
   cambiata e l'effetto mostrato non descrive più la realtà.
4. **RF-4** — La conferma è una invocazione **esplicita e separata** (`conferma_bozza(id_bozza)`) da parte della
   persona, mai dall'assistente per proprio conto; esegue con la chiave di idempotenza della bozza, così che una
   doppia conferma — la rete che ritenta, la persona che tocca due volte — restituisca **lo stesso** movimento già
   creato e non un secondo movimento.
5. **RF-5** — `rettifica_giacenza` pretende un **motivo scritto** non vuoto e la sua bozza dichiara a parole cosa
   sta per accadere: che il saldo cambia perché si sta affermando che il registro era sbagliato. Senza motivo la
   bozza non si crea nemmeno.
6. **RF-6** — `chiudi_inventario` mostra nella bozza l'**elenco completo delle differenze** riga per riga, con il
   conteggio delle righe che genereranno una rettifica, perché una sola conferma produce molti movimenti; se
   l'elenco è più lungo di quanto stia in una risposta, la bozza dichiara il totale e rimanda alla schermata
   dell'inventario per la lettura integrale.
7. **RF-7** — Il contratto dichiara esplicitamente le operazioni che **non sono e non saranno** strumenti, e la
   dichiarazione è verificata da una prova automatica che fallisce se un giorno comparissero.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** La bozza è creata, letta, confermata e scartata **solo** dentro il
  `tenant_id` del token verificato; una bozza di un account non è confermabile da un altro nemmeno conoscendone
  l'identificativo. Prova di isolamento fra due account su creazione e conferma.
- **RT-2 — Interfaccia di programmazione (§2).** La conferma non reimplementa nulla: chiama gli stessi servizi
  applicativi di `POST /api/magazzino/v1/movimenti`, `/trasferimenti`, `/rettifiche`, `/storni` e della chiusura
  dell'inventario, quindi eredita l'aritmetica nella base di dati e l'aggiornamento condizionato in una sola
  transazione. Errori in `application/problem+json`, tradotti nell'esito d'errore dello strumento con lo stesso
  codice; definizione OpenAPI aggiornata nello stesso commit per la risorsa delle bozze.
- **RT-3 — Persistenza (§8).** Migrazione `V21__bozze_strumenti.sql` sullo schema `app_magazzino`: tabella
  `bozza_strumento` con `tenant_id`, chiave primaria UUID versione 7, strumento invocato, parametri, effetto
  calcolato, chiave di idempotenza, scadenza, stato (`aperta`, `confermata`, `scaduta`, `scartata`), colonne di
  controllo e cancellazione logica. Le bozze scadute si potano; i movimenti che ne nascono restano per sempre,
  perché il registro è in sola aggiunta e nessuna storia può violarlo.
- **RT-4 — Modulo frontend (§3, §5).** Nessuna schermata nuova in questa storia: la conferma nell'interfaccia
  passa dalle schermate dei movimenti già esistenti. Se e quando la piattaforma offrirà un riquadro di conferma
  comune, il modulo vi si aggancerà con i soli token del sistema di design, in tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** I testi della bozza — la frase che descrive l'effetto, l'avvertimento della
  rettifica, il messaggio di scadenza, il messaggio di conferma già avvenuta — passano dallo spazio-nomi
  `magazzino` e sono presenti in `en, it, fr, es, de`.
- **RT-6 — Varchi e quota (§6, §7).** La creazione della bozza e la conferma attraversano la catena completa
  (`401` / `403` / `402` / `403` / `429`) con il ruolo richiesto dall'operazione corrispondente. **I movimenti non
  consumano mai la metrica `articoli_gestiti`** e non vengono mai respinti con `429`: il tetto colpisce solo la
  creazione di un articolo nuovo (§5 della descrizione). Uno strumento di scrittura che rifiutasse uno scarico per
  quota esaurita corromperebbe il saldo del cliente ed è vietato.
- **RT-7 — Esposizione conversazionale (§12).** Sei strumenti marcati `scrittura`, **tutti** con bozza e conferma
  umana esplicita, nessuno con effetti verso l'esterno. `rettifica_giacenza` e `chiudi_inventario` sono i due con
  effetto ampio o non annullabile se non con un altro movimento, e portano l'avvertimento rafforzato. Il server
  conversazionale è di piattaforma e **non è ancora implementato** (UC 0061-0063).
- **RT-8 — Dati personali (§10).** Nessun campo personale nuovo. La bozza conserva i parametri ricevuti, che per
  `rettifica_giacenza` e `storna_movimento` includono un **motivo a testo libero**: è lo stesso ingresso non
  presidiato descritto al §6 della descrizione, quindi la tabella `bozza_strumento` va aggiunta a `exportData` e
  `purgeData` del contratto `MagazzinoDataContract` e il campo del motivo va annotato `@PersonalData` come già lo
  è la nota del movimento.
- **RT-9 — Registrazione eventi (§14).** Si registrano `bozza creata`, `bozza confermata`, `bozza scaduta`,
  `conferma ripetuta ignorata` con nome dello strumento, esito, `tenant_id`, `app_id`, `user_id` e identificativo
  di correlazione — **senza** il testo del motivo e senza le descrizioni degli articoli.

## 4. Criteri di accettazione

**CA-1 — La bozza non tocca niente**
- **Dato** l'articolo `RIC-014` con 9 pezzi nel deposito «Magazzino»
- **Quando** viene invocato `registra_scarico(articolo: "RIC-014", deposito: "Magazzino", quantità: 2)`
- **Allora** torna una bozza che dichiara «prima 9, dopo 7», la giacenza è **ancora 9** e nel registro non c'è
  alcun movimento nuovo

**CA-2 — Doppia conferma, un solo movimento**
- **Dato** una bozza aperta di scarico da 2 pezzi
- **Quando** `conferma_bozza` viene invocata due volte con lo stesso identificativo
- **Allora** la prima crea il movimento e porta la giacenza a 7, la seconda restituisce **lo stesso** movimento con
  l'indicazione che era già stato confermato, e nel registro c'è **una** riga sola

**CA-3 — Rettifica senza motivo**
- **Dato** un utente che invoca `rettifica_giacenza(articolo: "RIC-014", deposito: "Magazzino", quantità_reale: 6)`
  senza motivo
- **Allora** la bozza **non** viene creata, l'esito è `400` in `application/problem+json` con un messaggio che
  spiega che una rettifica senza motivo non è ammessa, e nulla cambia

**CA-4 — Bozza scaduta perché la realtà è cambiata**
- **Dato** una bozza di scarico da 3 pezzi creata quando ce n'erano 5, e un altro utente che nel frattempo ne ha
  scaricati 4
- **Quando** la bozza viene confermata dopo la scadenza
- **Allora** l'esito è `409` con l'invito a rifare la richiesta, nessun movimento viene creato e la giacenza resta
  quella vera

**CA-5 — Chiusura dell'inventario: prima l'elenco, poi la conferma**
- **Dato** una sessione di inventario con 12 righe contate di cui 3 in differenza
- **Quando** viene invocato `chiudi_inventario`
- **Allora** la bozza elenca le 3 differenze con atteso, contato e scarto, dichiara che la conferma genererà 3
  rettifiche, e finché non arriva la conferma l'inventario resta aperto e nessuna rettifica esiste

**CA-6 — Le operazioni che non esistono**
- **Dato** il contratto degli strumenti pubblicato
- **Quando** lo si interroga cercando «cancella movimento», «imposta giacenza» o «invia ordine al fornitore»
- **Allora** nessuno dei tre esiste, e la prova automatica di contratto fallisce se uno strumento con quegli
  effetti viene introdotto

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno backend, frontend e compliance; l'intera suite prima del commit);
- [ ] prove di **unità** sul calcolo dell'effetto atteso e sulla scadenza, di **integrazione** sulla risorsa delle
      bozze e sulla conferma idempotente, con database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** su creazione e conferma della bozza;
- [ ] **prova end-to-end**: *rimando* — la conferma degli strumenti di scrittura non ha ancora un server che li
      esponga; il percorso `[J-MAGAZZINO]` copre le stesse operazioni dalle rotte pubbliche nelle storie `0036` e
      `0037`, ed è lì che il registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) riceve le voci;
- [ ] **traduzioni** dei testi della bozza presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese per il motivo a testo libero della bozza, campo
      annotato, tabella `bozza_strumento` presente in esportazione e cancellazione;
- [ ] **registro delle decisioni** compilato, con la scelta dei due tempi bozza-conferma e della scadenza breve;
- [ ] contratto degli **strumenti conversazionali** dichiarato, con le tre operazioni escluse messe per iscritto;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata: il §7 della descrizione dell'applicazione resta la fonte del contratto.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| `0034` | Il contratto degli strumenti e il descrittore dentro il servizio esistono già |
| `0016`, `0017` | Trasferimento e storno devono esistere come operazioni prima di essere esposti |
| `0021` | La rettifica con motivo obbligatorio è l'operazione che `rettifica_giacenza` prepara |
| `0023` | La chiusura dell'inventario con l'elenco delle differenze è ciò che `chiudi_inventario` prepara |
| UC 0061-0063 (piattaforma, non implementati) | Server conversazionale, autenticazione delegata e mappatura operazioni → strumenti. Nel frattempo bozza e conferma sono raggiungibili come risorsa dell'app e provate come tali |

## 7. Fuori ambito

- **Gli strumenti di lettura**: storia `0034`.
- **L'invio dell'ordine al fornitore**: non è uno strumento e non lo diventerà — l'app non manda niente a nessuno
  fuori dall'azienda (§1 della descrizione); l'ordine è di ProcureGrove (48). È anche il motivo per cui questa app
  è insolitamente sicura da esporre a un assistente: **nessuno strumento ha effetti verso l'esterno**, il peggio
  che può accadere è un movimento sbagliato dentro il registro del cliente, correggibile con uno storno.
- **La cancellazione di un movimento e l'impostazione diretta della giacenza**: non esistono come operazioni
  dell'applicazione, né dalla chat né dall'interfaccia (§4 e §7 della descrizione).
- **Il consenso delegato e la traccia di chi ha autorizzato l'assistente**: sono di piattaforma (UC 0062, 0065).

## 8. Punti aperti

- **Quanto deve durare una bozza.** Troppo breve è un fastidio, troppo lunga mostra un effetto che non è più vero.
  La proposta è pochi minuti, ma dipende da come la piattaforma gestirà il giro di conferma (UC 0061-0063) e non è
  una decisione di questa app da sola.
- **Chi può confermare.** Se la conferma richieda lo stesso ruolo dell'operazione corrispondente o un ruolo
  superiore quando l'effetto è ampio (la chiusura di un inventario che genera decine di rettifiche) è una scelta di
  prodotto: la chiude lo sviluppatore.
- **La giacenza negativa dagli strumenti**: `registra_scarico` segue la regola dell'applicazione — rifiuto con
  `409` e quantità residua — ma la regola stessa è il punto aperto 3 della descrizione e non la chiude questa
  storia.
