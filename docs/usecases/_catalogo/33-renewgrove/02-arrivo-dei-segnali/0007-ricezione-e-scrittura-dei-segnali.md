# 0007 — Ricezione e scrittura dei segnali

**Applicazione**: 33 — RenewGrove (`fidelizzazione`) · **Epica**: 02 — Arrivo dei segnali dalle altre app
**Storia**: `0007` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0006`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che ha collegato la fatturazione e l'assistenza a RenewGrove
> voglio che i fatti arrivino da soli, senza che io faccia niente, e che non arrivino due volte
> così da poter guardare un punteggio sapendo che riflette quello che è successo davvero.

**Contesto.** Il contratto esiste (`0006`), le tabelle esistono (`0002`): manca chi porta i segnali dall'uno alle
altre. Questa storia costruisce il consumatore della coda dedicata a `fidelizzazione`. È il punto in cui il
`tenant_id` entra nell'applicazione **da una strada diversa dal token**, ed è quindi il punto esatto in cui
l'isolamento fra account potrebbe rompersi. Per questo il codice che scrive i segnali è separato da quello che li
legge e non condivide con esso alcun meccanismo: non è una preferenza di stile, è il presidio più forte del §4.2
della [descrizione](../application-description.md), e va **verificato con una prova, non con una convenzione**.

## 2. Requisiti funzionali

1. **RF-1** — Il servizio consuma i messaggi dalla coda dedicata, valida ogni segnale contro il contratto (`0006`)
   e scrive quelli validi nella tabella `segnale`, con il `tenant_id` **copiato dall'evento**.
2. **RF-2** — Il `tenant_id` non si deduce mai — non dall'app d'origine, non dal riferimento del rapporto, non da
   una tabella di corrispondenza — e non si accetta **mai** da una richiesta. Un segnale senza `tenant_id`, o con un
   `tenant_id` sconosciuto alla piattaforma, viene scartato.
3. **RF-3** — La consegna è **idempotente** su `(tenant_id, app_origine, chiave_idempotenza)`: lo stesso segnale
   consegnato più volte produce una sola riga. Poiché `segnale` è in sola aggiunta (`0002`), una correzione dello
   stesso fatto è una **riga nuova** che rende superata la precedente, mai un aggiornamento.
4. **RF-4** — Un segnale proveniente da una fonte che quell'account **non ha collegato** viene scartato senza
   errore, con un conteggio: è il caso normale, non un'anomalia (storia `0008`).
5. **RF-5** — Un segnale malformato viene scartato e finisce in una **coda di scarto**, con la regola violata; non
   blocca la coda e non fa fallire i segnali successivi.
6. **RF-6** — Il consumo è osservabile: si sa quanti segnali sono stati scritti, quanti scartati e per quale motivo,
   per account e per fonte. È il dato che la storia `0011` mostrerà all'utente.
7. **RF-7** — **Nessun contenuto del segnale finisce nei registri**: si registrano identificativi, tipo e regola
   violata, mai i valori del segnale rifiutato.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Il `tenant_id` con cui il segnale viene scritto proviene
  **esclusivamente** dal messaggio dell'evento, e il messaggio proviene dal canale interno della piattaforma, non
  dall'esterno. Il percorso di scrittura dei segnali **non condivide codice** con il percorso di lettura via
  interfaccia web: non esiste un punto in cui un `tenant_id` di una richiesta possa finire in una scrittura, né
  viceversa. È il requisito centrale della storia e va dimostrato con una prova dedicata.
- **RT-2 — Nessuna chiamata fra app (§2).** Il consumatore non interroga l'app d'origine per completare un segnale
  incompleto: un segnale incompleto si scarta. Chiedere sarebbe la scorciatoia numero due del §4.2 — una credenziale
  che scavalca abilitazione e ruolo.
- **RT-3 — Interfaccia di programmazione (§2).** Nessuna rotta pubblica nuova: il consumo avviene dalla coda.
  L'unica superficie visibile è quella diagnostica della storia `0011`.
- **RT-4 — Persistenza (§8).** Scrittura sulla tabella `segnale` (in sola aggiunta) rispettando il vincolo di
  unicità su `(tenant_id, app_origine, chiave_idempotenza)`; nessuna chiave esterna verso altri schemi.
- **RT-5 — Dati personali (§10).** Nessun campo personale **nuovo**: il contratto (`0006`) circoscrive già che cosa
  entra, e la voce `segnale.fatto` è dichiarata nel manifesto dalla storia `0002`. Va però scritto nel registro
  delle decisioni un punto che riguarda i dati personali e non è ovvio: la **coda di scarto** può contenere segnali
  rifiutati proprio perché portavano un dato che non doveva entrare, e quindi va trattata come materiale sensibile —
  conservazione breve e nessuna esposizione del contenuto.
- **RT-6 — Esposizione conversazionale (§12).** Nessuno strumento introdotto: il consumo non ha superficie
  comandabile. Il contratto degli strumenti vive dentro il servizio; il server conversazionale è di piattaforma e
  non ancora implementato (UC 0061-0063).
- **RT-7 — Registrazione eventi (§14).** «segnale scritto», «segnale superato da una correzione», «segnale
  scartato» si registrano con `tenant_id`, `app_id` d'origine, `user_id` quando esiste, identificativo di
  correlazione e la regola violata quando c'è; **mai** il contenuto del segnale.
- **RT-8 — Prove (§11).** Integrazione con database effimero e migrazioni vere: consegna doppia, correzione, segnale
  malformato, account sconosciuto, fonte non collegata. Prova di isolamento: due account che ricevono segnali
  contemporaneamente non si mescolano. Prova strutturale che i due percorsi del `tenant_id` non condividono codice.

## 4. Criteri di accettazione

**CA-1 — Un segnale valido arriva**
- **Dato** un account che ha collegato la fonte `abbonati`
- **Quando** quella fonte pubblica un segnale conforme al contratto
- **Allora** entro pochi secondi il segnale è scritto, intestato a quell'account, e il momento dell'ultimo segnale
  della fonte è aggiornato

**CA-2 — Consegna doppia**
- **Dato** un segnale già scritto
- **Quando** lo stesso messaggio viene consegnato una seconda volta
- **Allora** resta una sola riga e il consumo non segnala errore

**CA-3 — Il `tenant_id` si copia, non si deduce**
- **Dato** un segnale il cui riferimento di rapporto esiste già sotto l'account `A`, ma il cui `tenant_id`
  nell'evento è quello dell'account `B`
- **Quando** viene consegnato
- **Allora** il segnale è scritto sotto `B`, non sotto `A`: l'app non deduce l'account da nessun altro campo

**CA-4 — Account sconosciuto e segnale malformato**
- **Dato** un segnale con `tenant_id` assente o sconosciuto, e un secondo segnale che viola una regola del contratto
- **Quando** vengono consegnati
- **Allora** entrambi sono scartati, il secondo finisce nella coda di scarto con la regola violata, la coda continua
  a scorrere e il registro **non contiene** il contenuto dei segnali

**CA-5 — Fonte non collegata**
- **Dato** un account che **non** ha collegato la fonte `assistenza`
- **Quando** quella fonte pubblica un segnale per quell'account
- **Allora** il segnale è scartato senza errore, il conteggio degli scarti per fonte non collegata aumenta, e nulla
  viene scritto

**CA-6 — I due percorsi non si toccano**
- **Dato** due account `A` e `B` che ricevono segnali nello stesso momento
- **Quando** un utente di `A` legge i propri segnali attraverso l'interfaccia, forzando l'identificativo di `B` nella
  richiesta
- **Allora** vede solo i propri, e la prova strutturale conferma che il `tenant_id` della richiesta non raggiunge
  mai il percorso di scrittura

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend`; l'intera suite prima del commit);
- [ ] prove di **unità** sull'idempotenza e sulla correzione come riga nuova, e di **integrazione** sul consumo
      dalla coda, con database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** sul percorso di scrittura **e** su quello di lettura, con la verifica
      esplicita che i due percorsi non condividono la sorgente del `tenant_id`;
- [ ] **prova end-to-end**: *rimando* alla storia `0030`, che dovrà coprire «fonte pubblica → segnale scritto →
      visibile sul rapporto» sullo stack locale reale; voce `da-coprire` nel registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml);
- [ ] **traduzioni**: non applicabile — nessuna superficie utente in questa storia;
- [ ] **manifesto dei dati**: nessuna voce nuova; va confermato per iscritto che il consumo non introduce campi
      personali oltre quelli già dichiarati, e va aggiunta la nota sulla coda di scarto;
- [ ] **registro delle decisioni** compilato: separazione dei due percorsi del `tenant_id` e perché, trattamento
      della coda di scarto, correzione come riga nuova;
- [ ] contratto degli **strumenti conversazionali**: nessuna funzione introdotta;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0006` | serve il contratto da far rispettare, e la tabella `segnale` che ne rispecchia la forma |
| storia `0008` (reciproca) | il filtro «fonte non collegata» ha senso solo quando il collegamento esiste: le due storie si completano e vanno collaudate insieme |

## 7. Fuori ambito

- il collegamento e la revoca di una fonte: storia `0008`;
- l'aggregazione dei segnali su un rapporto e l'arrivo dell'etichetta leggibile: storia `0009`;
- i segnali registrati a mano e l'importazione da file: storia `0010`;
- il rilevamento delle fonti in silenzio: storia `0011`.

## 8. Punti aperti

- 🛑 **Lacuna dichiarata: se una fonte pubblicasse un `tenant_id` sbagliato, qui non è rilevabile.**
  `fidelizzazione` scriverebbe il segnale sotto l'account sbagliato e non potrebbe accorgersene, perché dal suo punto
  di vista è un segnale valido — e in quest'app il danno sarebbe più grave che in InsightGrove, perché non si tratta
  di un numero aggregato ma di un fatto riferito a un cliente identificabile. È un difetto **della fonte**, coperto
  dalle prove di isolamento della fonte. Qui non è risolvibile, e nasconderlo sarebbe peggio che scriverlo.
- **Quanto si conserva la coda di scarto?** Contiene segnali rifiutati che, se il rifiuto è dovuto a testo libero o
  a un campo anagrafico, portano dentro proprio il dato che non doveva entrare. Raccomandazione: **conservazione
  breve, quattordici giorni, e nessuna esposizione del contenuto**, nemmeno alla console di amministrazione.
  Chiude: lo sviluppatore.
