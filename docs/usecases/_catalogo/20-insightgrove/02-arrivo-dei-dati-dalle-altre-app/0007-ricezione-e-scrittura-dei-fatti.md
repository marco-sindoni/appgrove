# 0007 — Ricezione e scrittura dei fatti

**Applicazione**: 20 — InsightGrove (`insights`) · **Epica**: 02 — Arrivo dei dati dalle altre app
**Storia**: `0007` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0002`, `0006`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che ha collegato la fatturazione a InsightGrove
> voglio che i numeri arrivino da soli, senza che io faccia niente, e che non arrivino due volte
> così da poter guardare il cruscotto sapendo che riflette quello che è successo davvero.

**Contesto.** Il contratto esiste (0006), il magazzino esiste (0002): manca chi porta i fatti dall'uno all'altro.
Questa storia costruisce il consumatore della coda dedicata a `insights`. È il punto in cui il `tenant_id` entra
nell'applicazione **da una strada diversa dal gettone**, ed è quindi il punto in cui l'isolamento fra account
potrebbe rompersi: per questo il codice che scrive i fatti è separato da quello che li legge, e non condivide
alcun meccanismo con il percorso delle richieste HTTP.

## 2. Requisiti funzionali

1. **RF-1** — Il servizio consuma i messaggi dalla coda dedicata, valida ogni fatto contro il contratto (0006) e
   scrive quelli validi nella tabella `fatto`, con il `tenant_id` **preso dal messaggio**.
2. **RF-2** — La consegna è **idempotente**: lo stesso fatto consegnato più volte produce una sola riga. Un
   fatto con la stessa chiave di idempotenza e un valore diverso è una **correzione**: sostituisce il valore e
   conserva traccia del fatto che è stato corretto.
3. **RF-3** — Un fatto non valido viene **scartato** e finisce nella coda di scarto, con la regola violata; non
   blocca la coda e non fa fallire i fatti successivi.
4. **RF-4** — Un fatto il cui `tenant_id` non corrisponde ad alcun account noto alla piattaforma viene scartato.
5. **RF-5** — Un fatto proveniente da una fonte che quell'account **non ha collegato** viene scartato senza
   errore, con un conteggio: è il caso normale, non un'anomalia (storia 0008).
6. **RF-6** — Il consumo è osservabile: si sa quanti fatti sono stati scritti, quanti scartati e per quale
   motivo, per account e per fonte.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Il `tenant_id` con cui il fatto viene scritto proviene
  **esclusivamente** dal messaggio dell'evento, e il messaggio proviene dal bus interno della piattaforma, non
  dall'esterno. Il percorso di scrittura dei fatti **non condivide codice** con il percorso di lettura HTTP: non
  esiste un punto in cui un `tenant_id` di una richiesta possa finire in una scrittura di fatti, né viceversa.
  Va verificato con una prova, non solo con una convenzione.
- **RT-2 — Interfaccia di programmazione (§2).** Nessuna rotta pubblica nuova: il consumo avviene dalla coda.
  L'unica superficie HTTP è quella diagnostica della storia 0010.
- **RT-3 — Persistenza (§8).** Scrittura sulla tabella `fatto` (in sola aggiunta) rispettando il vincolo di
  unicità su `(tenant_id, app_origine, chiave_idempotenza)`; la correzione è una nuova riga che rende
  «superata» la precedente, non un aggiornamento.
- **RT-8 — Dati personali (§10).** Nessun dato personale nuovo, per costruzione: il contratto lo garantisce.
  Se la via (A) delle etichette è stata scelta, le etichette arrivano su un evento **separato** e sono trattate
  da un consumatore distinto.
- **RT-14 — Registrazione eventi (§14).** «Fatto scritto», «fatto corretto», «fatto scartato» si registrano con
  `tenant_id`, `app_id` d'origine, identificativo di correlazione e la regola violata quando c'è; **mai** il
  contenuto del fatto.
- **RT-11 — Prove (§11).** Prove di integrazione con database effimero: consegna doppia, correzione, fatto
  malformato, account sconosciuto, fonte non collegata. Prova di isolamento: due account che pubblicano fatti
  contemporaneamente non si mescolano.

## 4. Criteri di accettazione

**CA-1 — Un fatto valido arriva**
- **Dato** un account che ha collegato la fonte «fatturazione»
- **Quando** quella fonte pubblica un fatto valido
- **Allora** entro pochi secondi il fatto è nel magazzino, intestato a quell'account, e il momento dell'ultimo
  fatto della fonte è aggiornato

**CA-2 — Consegna doppia**
- **Dato** un fatto già scritto
- **Quando** lo stesso messaggio viene consegnato una seconda volta
- **Allora** nel magazzino c'è ancora una sola riga e il consumo non segnala errore

**CA-3 — Correzione**
- **Dato** un fatto con valore `820000` e chiave di idempotenza `k`
- **Quando** arriva un fatto con la stessa `k` e valore `790000`
- **Allora** il valore corrente diventa `790000`, resta traccia che è stato corretto, e i calcoli successivi
  usano il valore nuovo

**CA-4 — Fatto malformato**
- **Dato** un fatto che viola una regola del contratto
- **Quando** viene consegnato
- **Allora** finisce nella coda di scarto con la regola violata, la coda continua a scorrere e il registro non
  contiene il contenuto del fatto

**CA-5 — Isolamento fra account**
- **Dato** due account `A` e `B` che ricevono fatti sulla stessa misura nello stesso momento
- **Quando** un utente di `A` legge i propri fatti
- **Allora** vede solo i propri, e nessuna manipolazione della richiesta gli fa vedere quelli di `B`

**CA-6 — Fonte non collegata**
- **Dato** un account che **non** ha collegato la fonte «magazzino»
- **Quando** quella fonte pubblica un fatto per quell'account
- **Allora** il fatto viene scartato senza errore, il conteggio degli scarti per fonte non collegata aumenta, e
  nel magazzino non compare nulla

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno le aree toccate; l'intera suite prima del commit);
- [ ] prove di **unità** sull'idempotenza e sulla correzione, e di **integrazione** sul consumo dalla coda, con
      database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** sul percorso di scrittura **e** su quello di lettura, con la verifica
      esplicita che i due percorsi non condividono la sorgente del `tenant_id`;
- [ ] **prova end-to-end**: *rimando* alla storia 0034; voce `da-coprire` nel registro di copertura;
- [ ] **traduzioni**: non applicabile (nessuna superficie utente);
- [ ] **manifesto dei dati**: nessuna voce nuova; va confermato che il consumo non introduce dati personali;
- [ ] **registro delle decisioni** compilato, con la separazione dei due percorsi del `tenant_id` e il perché;
- [ ] contratto degli **strumenti conversazionali**: nessuna funzione introdotta;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0002` | serve il magazzino |
| storia `0006` | serve il contratto da far rispettare |
| storia `0008` (reciproca) | il filtro «fonte non collegata» ha senso solo quando esiste il collegamento: le due storie si completano e vanno collaudate insieme |

## 7. Fuori ambito

- il collegamento e la revoca di una fonte: storia 0008;
- il ripopolamento dello storico: storia 0009;
- il rilevamento delle fonti silenti: storia 0010.

## 8. Punti aperti

- **Quanto si conserva la coda di scarto?** Contiene fatti rifiutati che, se il rifiuto è dovuto a testo libero,
  potrebbero contenere il dato che non doveva entrare. Raccomandazione: **conservazione breve, quattordici
  giorni, e nessuna esposizione del contenuto** — nemmeno alla console di amministrazione. Chiude:
  **sviluppatore**.
- **Cosa succede se una fonte pubblica un `tenant_id` sbagliato?** L'app scriverebbe il fatto sotto l'account
  sbagliato, e nessun controllo di questa app può accorgersene: dal suo punto di vista è un fatto valido.
  È un difetto **della fonte**, che le prove di isolamento della fonte devono coprire. La lacuna va dichiarata,
  non nascosta: qui non è risolvibile.
