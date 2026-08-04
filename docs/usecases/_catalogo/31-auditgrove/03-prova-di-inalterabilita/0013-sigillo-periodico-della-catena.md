# 0013 — Sigillo periodico della catena

**Applicazione**: 31 — AuditGrove (`agentaudit`) · **Epica**: 03 — Prova di inalterabilità
**Storia**: `0013` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0002`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come persona che risponde di ciò che fanno gli agenti della propria azienda
> voglio che a intervalli regolari venga prodotta una fotografia firmata dello stato del registro
> così da avere un punto fermo nel tempo a cui riferire ogni verifica successiva, invece di dover credere sulla
> parola a chi custodisce il registro.

**Contesto.** La storia 0002 ha costruito la catena delle impronte: ogni riga porta l'impronta della precedente, e
alterarne una obbliga a ricalcolare tutte le successive. Sembra sufficiente, e non lo è. **Chi ha accesso alla
base di dati può riscrivere le righe e poi ricalcolare l'intera catena**: il risultato è una catena perfettamente
integra che racconta una storia diversa da quella vera. Una catena verificabile solo da noi non dimostra nulla —
dimostra solo che noi siamo coerenti con noi stessi (§4.3 della descrizione dell'applicazione).

Il sigillo è la risposta: a cadenza regolare si fissa un punto — *a questa data la catena aveva questa testa,
questo numero di righe, queste sequenze* — e lo si firma. Da quel momento, riscrivere il passato significa
contraddire un sigillo. Questa storia produce e custodisce il sigillo; è la storia 0017 a **consegnarlo fuori dal
nostro perimetro**, che è ciò che lo trasforma da annotazione a prova. Le due vanno lette insieme, e questa da
sola non è ancora sufficiente.

## 2. Requisiti funzionali

1. **RF-1** — Una lavorazione programmata produce, per ogni account con almeno un'azione nuova, un **sigillo
   giornaliero** che dichiara: periodo coperto, prima e ultima sequenza incluse, conteggio delle azioni, impronta
   di testa della catena al momento del sigillo, algoritmo usato, momento di produzione.
2. **RF-2** — Il sigillo è **firmato** con la chiave di firma della piattaforma, e la firma è verificabile da chi
   possieda la sola parte pubblica corrispondente, senza accesso ai nostri sistemi.
3. **RF-3** — Il sigillo è a sua volta **una riga della catena**: il sigillo del giorno seguente copre anche
   l'evento che ha registrato il sigillo precedente, così che nessun sigillo possa essere fatto sparire senza
   spezzare la catena.
4. **RF-4** — Un sigillo si può produrre **su richiesta** di una persona dell'account, oltre che a cadenza
   programmata: serve a fissare un punto prima di un'operazione delicata o a chiudere un contenzioso.
5. **RF-5** — I sigilli **non si cancellano e non si modificano**, mai: né dall'applicazione, né dalla console di
   amministrazione, né da una lavorazione di manutenzione. La sola cancellazione ammessa nell'intera piattaforma
   — quella per l'esercizio dei diritti dell'interessato — non tocca i sigilli, perché un sigillo non contiene
   dati di persone.
6. **RF-6** — Un account senza azioni nuove nel periodo non produce un sigillo vuoto: produce un sigillo di
   **continuità** che dichiara «nessuna azione fra la sequenza X e la data Y», perché il silenzio va dimostrato
   quanto l'attività.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni sigillo appartiene a un account e ogni lettura filtra per
  `tenant_id` preso dal token verificato; un `tenant_id` che arrivasse dal corpo della richiesta o dai parametri
  viene ignorato. La lavorazione programmata itera sugli account e non mescola mai due catene: l'impronta di testa
  di un account non compare mai in un sigillo di un altro.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `GET /api/agentaudit/v1/seals` (elenco paginato) e
  `POST /api/agentaudit/v1/seals` (produzione su richiesta); corpo validato; errori in
  `application/problem+json`; definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V<N>__sigilli.sql` sullo schema `app_agentaudit`: tabella `seals` con
  `tenant_id`, chiave primaria UUID versione 7, colonne di controllo, periodo, prima e ultima sequenza, conteggio,
  impronta di testa, algoritmo, firma. **Stessa deroga consapevole della storia 0002**: la colonna `deleted_at`
  esiste per uniformità e **non viene mai valorizzata**; il ruolo di database del servizio ha sulla tabella i soli
  privilegi di inserimento e lettura.
- **RT-4 — Modulo frontend (§3, §5).** Nessuna schermata propria in questa storia: l'elenco dei sigilli compare
  nella schermata «Integrità» della storia 0014. Qui si costruisce solo ciò che quella schermata leggerà.
- **RT-5 — Cinque lingue (§4).** Nessun testo visibile introdotto in questa storia; i testi della schermata
  arrivano con la 0014 e quelli del messaggio di recapito con la 0017.
- **RT-6 — Varchi e quota (§6, §7).** La produzione del sigillo **non consuma** la metrica `actions` quando è
  automatica: è un atto della piattaforma, non un'azione dell'agente, e far pagare la produzione della prova
  sarebbe la stessa stortura descritta al §3 della descrizione dell'applicazione. Il sigillo prodotto **su
  richiesta** consuma una unità, perché è un'azione voluta da una persona; a quota esaurita risponde `429` con
  l'indicazione del rimedio, e il sigillo automatico continua comunque a essere prodotto — un account che ha
  finito la quota non deve perdere la prova di ciò che ha già registrato. Con abbonamento non attivo risponde
  `402`, ma i sigilli già prodotti restano leggibili.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento nuovo qui: la lettura dei sigilli entra in
  `verifica_integrita` alla storia 0034, marcato **lettura**. Il contratto vive dentro il servizio; il server
  conversazionale è di piattaforma e non ancora implementato (UC 0061-0063).
- **RT-8 — Dati personali (§10).** **Nessun dato personale nuovo**: un sigillo contiene numeri, date e impronte,
  e per costruzione non contiene identificativi di persone. È una proprietà da preservare, non un caso: se un
  giorno si volesse aggiungere al sigillo «chi l'ha richiesto», quella diventerebbe una voce di manifesto e
  cambierebbe il regime di conservazione del sigillo. Il richiedente di un sigillo su richiesta viene registrato
  **nella riga di catena** che accompagna il sigillo, non dentro il sigillo firmato.
- **RT-9 — Registrazione eventi (§14).** Gli eventi `sigillo prodotto`, `sigillo su richiesta respinto per quota`
  e `lavorazione dei sigilli fallita` sono registrati con `tenant_id`, `app_id`, `user_id` e identificativo di
  correlazione, senza dati personali.

## 4. Criteri di accettazione

**CA-1 — Il sigillo giornaliero si produce e si incatena**
- **Dato** un account con dodici azioni registrate nella giornata
- **Quando** la lavorazione programmata dei sigilli viene eseguita
- **Allora** esiste un sigillo che dichiara prima e ultima sequenza corrette e conteggio pari a dodici, la sua
  impronta di testa coincide con l'impronta dell'ultima azione, ed esiste nella catena una riga che registra la
  produzione del sigillo

**CA-2 — La firma è verificabile dall'esterno**
- **Dato** un sigillo prodotto e la sola parte pubblica della chiave di firma
- **Quando** un verificatore che non ha accesso ai nostri sistemi controlla la firma
- **Allora** la verifica riesce, e riesce anche dopo che il sigillo è stato copiato altrove

**CA-3 — Un sigillo non si può togliere di mezzo**
- **Dato** una successione di tre sigilli giornalieri
- **Quando** si tenta di cancellare o modificare il secondo, sia dall'applicazione sia direttamente sulla base di
  dati con il ruolo del servizio
- **Allora** l'operazione viene respinta dal database, non esiste nel codice alcun percorso che possa emetterla, e
  la rimozione della riga di catena corrispondente renderebbe comunque non integra la catena dal punto successivo

**CA-4 — Il silenzio è dimostrato**
- **Dato** un account che non registra azioni per tre giorni
- **Quando** la lavorazione programmata viene eseguita in quei giorni
- **Allora** vengono prodotti sigilli di continuità che dichiarano l'assenza di azioni fra la sequenza raggiunta e
  la data corrente, invece di non produrre nulla

**CA-5 — Isolamento fra account**
- **Dato** due account `A` e `B`, ciascuno con le proprie azioni e i propri sigilli
- **Quando** un utente di `A` chiede l'elenco dei sigilli
- **Allora** vede solo i propri, anche se forza l'identificativo dell'altro account nella richiesta, e nessuna
  impronta di testa di `B` compare in un sigillo di `A`

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno le aree toccate; l'intera suite prima del commit);
- [ ] prove di **unità** sul calcolo del sigillo e sulla firma, e di **integrazione** sulla lavorazione
      programmata e sulla rotta di produzione su richiesta, con database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** sui sigilli, compreso il tentativo di forzare l'identificativo
      dell'account dall'esterno;
- [ ] **prova end-to-end**: risposta «rimando» — la superficie utente dei sigilli nasce con la schermata
      «Integrità» della storia 0014, che è la storia proprietaria del passo di percorso; il registro di copertura
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) riceve la voce `da-coprire` con
      questo motivo;
- [ ] **traduzioni**: nessun testo visibile introdotto, e il fatto è dichiarato;
- [ ] **manifesto dei dati**: nessuna voce nuova — un sigillo non contiene dati di persone, e la proprietà è
      verificata da una prova, non solo affermata;
- [ ] **registro delle decisioni** `changes/NNNN-*/decisions.json` compilato, con **due voci obbligatorie**: il
      sigillo automatico che non consuma quota, e la scelta di produrre sigilli di continuità anche in assenza di
      attività;
- [ ] contratto degli **strumenti conversazionali**: nessuno in questa storia, dichiarato;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0002` | Il sigillo fotografa la catena: senza catena non c'è testa da fissare |
| Chiave di firma della piattaforma | La firma deve poter essere verificata da terzi: serve una coppia di chiavi con una parte pubblica pubblicabile, e una custodia della parte privata che non stia nella stessa base di dati che il sigillo protegge |

## 7. Fuori ambito

- **la consegna del sigillo fuori dal nostro perimetro**: storia 0017. È bene ripeterlo: senza quella storia,
  questa produce un'annotazione ben fatta e non ancora una prova;
- la verifica dell'integrità esposta all'utente e la schermata che mostra i sigilli: storia 0014;
- il pacchetto verificabile da terzi: storia 0015;
- l'ancoraggio dell'impronta a un registro pubblico di terzi e la marca temporale qualificata: deliberatamente
  rimandati, sono un punto aperto di prodotto (§11 della descrizione dell'applicazione).

## 8. Punti aperti

- **Dove sta la parte privata della chiave di firma.** Se sta nella stessa base di dati che il sigillo protegge,
  chi può riscrivere le righe può anche rifirmare i sigilli, e il presidio vale molto meno. Serve una custodia
  separata, con rotazione: è una decisione di infrastruttura e di sicurezza, non di questa storia. Chi chiude:
  sviluppatore, insieme a chi presidia la sicurezza.
- **Rotazione della chiave di firma.** I sigilli firmati con una chiave ritirata devono restare verificabili: la
  parte pubblica di ogni chiave usata va conservata e pubblicata insieme al periodo di validità. Il campo che
  dichiara quale chiave ha firmato **va previsto adesso**, perché aggiungerlo dopo lascerebbe sigilli che non lo
  dichiarano.
- **Cadenza del sigillo automatico.** Giornaliera è una proposta. Più fitta significa punti fermi più ravvicinati
  e più righe; più rada significa finestre più larghe in cui una riscrittura resta indimostrabile. Chi chiude:
  sviluppatore.
