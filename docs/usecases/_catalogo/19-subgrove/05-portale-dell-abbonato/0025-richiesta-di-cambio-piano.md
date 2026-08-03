# 0025 — Richiesta di cambio piano

**Applicazione**: 19 — SubGrove (`abbonati`) · **Epica**: 05 — Portale dell'abbonato
**Storia**: `0025` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0023`, `0014`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come persona abbonata che vorrebbe passare alla formula completa
> voglio poterlo chiedere dalla pagina del mio abbonamento
> così da non dover aspettare di passare in reception in un orario in cui c'è qualcuno.

**Contesto.** È la funzione che rende il portale utile anche a chi **non** vuole andarsene, ed è il modo più
naturale per far crescere il ricavo ricorrente del cliente. La differenza con la disdetta è netta e va rispettata
nel disegno: la disdetta è un **diritto** dell'abbonato e non si può ostacolare (storia `0024`); il cambio di
piano è una **richiesta** rivolta al cliente, che può accettarla o no — magari perché quel piano ha un numero
chiuso, o perché ci sono condizioni da discutere. Un cambio che si applicasse da solo toglierebbe al cliente il
controllo sui propri prezzi.

## 2. Requisiti funzionali

1. **RF-1** — Dalla pagina pubblica l'abbonato vede i piani **pubblicabili** dell'account (quelli attivi e
   marcati come mostrabili all'esterno) con nome, condizioni e canone, e può chiederne uno.
2. **RF-2** — La richiesta mostra prima di inviare, in modo indicativo, cosa comporterebbe: canone nuovo,
   quando avrebbe effetto, ed eventuale conguaglio — con l'avvertenza che l'importo definitivo lo conferma il
   cliente.
3. **RF-3** — La richiesta **non** cambia nulla: arriva al cliente in un elenco «richieste degli abbonati», che
   la può **accettare** (e allora si applica il cambio della storia `0014`) o **rifiutare** con un motivo.
4. **RF-4** — L'abbonato riceve un riscontro scritto sia all'invio sia all'esito, e la pagina mostra sempre lo
   stato della richiesta in corso.
5. **RF-5** — Un abbonato non può avere più di una richiesta aperta per abbonamento: la seconda sostituisce la
   prima, e l'interfaccia lo dice.
6. **RF-6** — Un piano si può marcare **non pubblicabile**: resta sottoscrivibile dalla reception ma non compare
   nel portale.

## 3. Requisiti tecnici

- **RT-1 — Superficie pubblica.** Nessuna credenziale; solo il gettone firmato della storia `0023`.
- **RT-2 — Isolamento fra account (§1).** I piani mostrati sono solo quelli dell'account dell'abbonamento; la
  richiesta nasce con lo stesso `tenant_id`.
- **RT-3 — Interfaccia di programmazione (§2).** Rotte pubbliche `GET /api/abbonati/v1/pubblico/{gettone}/piani`
  e `POST .../richiesta-cambio-piano`; rotte interne `GET /api/abbonati/v1/richieste` e
  `POST /api/abbonati/v1/richieste/{id}/{accetta|rifiuta}`; errori in `problem+json`; OpenAPI aggiornata.
- **RT-4 — Persistenza (§8).** Nessuna tabella nuova: si usa `richiesta_dell_abbonato` della storia `0024`, con
  il tipo che distingue disdetta e cambio piano. Sul piano si aggiunge il campo «pubblicabile».
- **RT-5 — Modulo frontend (§3, §5).** Sulla pagina pubblica, l'elenco dei piani con il piano attuale marcato;
  nel backoffice, l'elenco delle richieste con le due azioni; solo token del sistema di design.
- **RT-6 — Cinque lingue (§4).** Elenco dei piani, avvertenza sull'indicatività e riscontri in
  `en, it, fr, es, de`.
- **RT-7 — Ciclo di vita.** L'accettazione **riusa** interamente la storia `0014`: nessuna logica di cambio piano
  duplicata qui, altrimenti i due percorsi divergeranno.
- **RT-8 — Esposizione conversazionale (§12).** Nessuno strumento nuovo per l'invio; l'accettazione da chat
  passerebbe da `cambia_piano` (storia `0014`), che ha già la sua conferma.
- **RT-9 — Dati personali (§10).** La richiesta è già coperta dalla voce di manifesto della storia `0024`; va
  aggiunto che il tipo distingue le due.
- **RT-10 — Registrazione eventi (§14).** `richiesta di cambio piano ricevuta`, `accettata`, `rifiutata`, con
  `tenant_id`, `app_id`, `user_id` (per le due azioni interne) e correlazione, senza nomi.

## 4. Criteri di accettazione

**CA-1 — Richiesta inviata**
- **Dato** un abbonato sulla pagina pubblica
- **Quando** sceglie un piano superiore e invia la richiesta
- **Allora** nulla del suo abbonamento è cambiato, la pagina mostra «richiesta in attesa» e arriva il riscontro
  scritto

**CA-2 — Accettazione**
- **Dato** la richiesta nell'elenco del cliente · **Quando** il cliente la accetta
- **Allora** si applica il cambio piano della storia `0014`, con le sue regole e il suo conguaglio, e l'abbonato
  riceve l'esito

**CA-3 — Rifiuto con motivo**
- **Dato** la stessa richiesta · **Quando** il cliente la rifiuta indicando un motivo
- **Allora** l'abbonamento resta com'è e l'abbonato riceve l'esito con il motivo

**CA-4 — Piani non pubblicabili**
- **Dato** un piano marcato non pubblicabile · **Quando** l'abbonato apre la pagina
- **Allora** quel piano non compare, pur restando sottoscrivibile dalla reception

**CA-5 — Una sola richiesta aperta**
- **Dato** una richiesta già in attesa · **Quando** l'abbonato ne invia un'altra
- **Allora** la nuova sostituisce la precedente e l'interfaccia lo dichiara

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend` e `frontend`);
- [ ] prove di **unità** sulla sostituzione della richiesta aperta; **integrazione** sul percorso richiesta →
      accettazione → cambio applicato;
- [ ] prova di **isolamento fra account** su piani mostrati e richieste;
- [ ] **prova end-to-end**: *rimando* — il percorso `[J-ABBONATI-PUBBLICO]` della storia `0034` copre la
      disdetta; il cambio piano ha voce `da-coprire` nel registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml);
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato con il tipo di richiesta;
- [ ] **registro delle decisioni** compilato: la richiesta non applica nulla, e perché (il cliente resta padrone
      dei propri prezzi); riuso integrale della logica di cambio piano;
- [ ] documentazione aggiornata.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0023` | serve la pagina pubblica e il gettone |
| storia `0014` | l'accettazione applica quella logica, senza duplicarla |
| storia `0024` | riusa la tabella delle richieste e l'impianto dei riscontri |

## 7. Fuori ambito

- il pagamento del conguaglio dal portale: **mai** (§5.2 della descrizione);
- l'accettazione automatica: esclusa per scelta;
- la sospensione chiesta dall'abbonato: non prevista, perché ha quasi sempre condizioni da discutere a voce.

## 8. Punti aperti

**Automatismo per i passaggi verso l'alto.** Accettare da soli le richieste che aumentano il canone sarebbe
comodo e commercialmente sensato, e il rischio è basso. Resta però una decisione sui prezzi del cliente, presa
senza di lui. **Proposta**: impostazione per account, spenta di default, che il cliente accende se vuole.
Chiude: lo sviluppatore, con la direzione di prodotto.
