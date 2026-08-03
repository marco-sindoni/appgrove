# 0014 — Invio della nota spese

**Applicazione**: 08 — SpendGrove (`notespese`) · **Epica**: 03 — Note spese e approvazione
**Storia**: `0014` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0013`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come collaboratore che ha finito di comporre la nota di luglio
> voglio inviarla all'approvazione sapendo esattamente cosa sto mandando e cosa non potrò più cambiare
> così da non dover rincorrere il titolare per correggere una riga dopo avergliela già messa sul tavolo.

**Contesto.** L'invio è la prima azione **irreversibile** dell'applicazione: chiama in causa un'altra persona e fa
uscire il fascicolo dalla sfera di chi lo ha composto. Merita una storia a sé, separata dalla composizione, proprio
perché la sua natura è diversa: la composizione è un lavoro, l'invio è un impegno. È anche il punto in cui gli
avvisi accumulati (spese senza giustificativo, pagamenti non tracciabili, massimali sforati) vanno mostrati per
l'ultima volta prima che sia troppo tardi.

## 2. Requisiti funzionali

1. **RF-1** — Una nota in `bozza` con almeno una spesa si può inviare; passa in `inviata` e non è più modificabile
   da chi l'ha composta.
2. **RF-2** — Prima dell'invio l'app mostra un **riepilogo di conferma** con totale, numero di spese e l'elenco
   degli avvisi aperti; l'invio richiede un gesto esplicito dopo averlo visto.
3. **RF-3** — All'invio la nota è **congelata**: le spese che contiene non si possono più modificare né togliere
   finché non torna indietro (rifiuto, storia `0015`).
4. **RF-4** — L'app individua l'approvatore competente in base alle assegnazioni della storia `0012`; se non ce n'è
   nessuno, l'invio è respinto con un messaggio che dice cosa configurare, non con un errore tecnico.
5. **RF-5** — L'approvatore riceve una notifica e vede la nota nel proprio elenco di cose da fare; il collaboratore
   vede lo stato «in attesa di approvazione» con la data di invio.
6. **RF-6** — Una nota inviata non si può inviare di nuovo, e il tentativo lo dice chiaramente.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** L'invio filtra per `tenant_id` preso dal token verificato; l'approvatore
  individuato appartiene per costruzione allo stesso account, e il servizio lo verifica invece di fidarsi.
- **RT-2 — Interfaccia di programmazione (§2).** `POST /api/notespese/v1/note-spese/{id}/invia`, idempotente per
  identificativo di richiesta: un doppio tocco sul pulsante non produce due invii. Errori in
  `application/problem+json` con `409` per nota già inviata e `422` per approvatore mancante; definizione OpenAPI
  aggiornata.
- **RT-3 — Persistenza (§8).** Migrazione `V11__invio_nota.sql`: colonne `inviata_da`, `inviata_il`,
  `approvatore_atteso` sulla tabella `nota_spese`; il congelamento si ottiene con il controllo di stato nel
  servizio, non con un blocco a livello di riga.
- **RT-4 — Modulo frontend (§3, §5).** Finestra di conferma con il riepilogo e gli avvisi; stato «inviata» visibile
  nell'elenco. Solo token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Riepilogo, avvisi e messaggi di errore passano dallo spazio-nomi `notespese` e sono
  presenti in `en, it, fr, es, de`.
- **RT-6 — Varchi e quota (§6, §7).** L'invio non consuma quota. Con abbonamento `past_due` l'invio resta possibile
  (periodo di tolleranza); con `canceled` risponde `402`.
- **RT-7 — Esposizione conversazionale (§12).** La storia dichiara `invia_nota_spese(id_nota) → esito dell'invio`,
  marcato **scrittura irreversibile**: produce una bozza di azione e richiede **conferma umana obbligatoria**, perché
  chiama in causa un'altra persona e non si annulla. Dipendenza: UC 0061-0063.
- **RT-8 — Dati personali (§10).** Nessuna categoria nuova; si aggiungono i campi «chi ha inviato e quando», che
  sono dati di attività lavorativa: voce aggiornata nel manifesto in italiano e inglese.
- **RT-9 — Registrazione eventi (§14).** L'evento `nota inviata` porta `tenant_id`, `app_id`, `user_id`,
  identificativo di correlazione, identificativo della nota e numero di spese — mai il totale né i nomi.

## 4. Criteri di accettazione

**CA-1 — Invio con riepilogo**
- **Dato** una nota in `bozza` con sei spese, di cui una senza giustificativo
- **Quando** il collaboratore preme «Invia»
- **Allora** vede il riepilogo con il totale e l'avviso sulla spesa scoperta, e solo dopo la conferma esplicita la
  nota passa a `inviata`

**CA-2 — Congelamento**
- **Dato** una nota `inviata` · **Quando** il collaboratore tenta di togliere una spesa o di modificarne l'importo
- **Allora** l'operazione è respinta con `409` e il messaggio spiega che la nota è in approvazione

**CA-3 — Approvatore mancante**
- **Dato** un collaboratore senza approvatore assegnato · **Quando** invia la nota
- **Allora** riceve `422` con l'indicazione di configurare l'approvatore, e la nota resta in `bozza`

**CA-4 — Doppio invio**
- **Dato** una nota `inviata` · **Quando** si ripete la stessa richiesta di invio
- **Allora** la risposta è `409` (o l'esito idempotente della prima, se porta lo stesso identificativo di richiesta)
  e non esiste un secondo invio

**CA-5 — Isolamento fra account**
- **Dato** due account `A` e `B` · **Quando** un utente di `A` tenta di inviare una nota di `B`
- **Allora** riceve `404`

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno backend e frontend; l'intera suite prima del commit);
- [ ] prove di **unità** sulla scelta dell'approvatore; di **integrazione** sull'invio con database effimero e
      migrazioni vere, compresa la prova di idempotenza;
- [ ] prova di **isolamento fra account** sull'invio;
- [ ] **prova end-to-end**: *coprire ora* il passo «invio la nota e non posso più modificarla» nel percorso
      `[J-NOTESPESE]`; registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) aggiornato;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese;
- [ ] **registro delle decisioni** compilato, con la scelta di rendere l'invio irreversibile e idempotente;
- [ ] contratto dello strumento `invia_nota_spese` dichiarato, marcato scrittura irreversibile con conferma
      obbligatoria;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| `0013` | Serve una nota composta da inviare |
| `0012` | Serve l'assegnazione approvatore → collaboratori |

## 7. Fuori ambito

- L'atto di approvare o respingere: storia `0015`.
- Il canale della notifica (posta elettronica, avviso dentro l'app): è di piattaforma; qui si dichiara che
  l'approvatore va avvisato, non con quale mezzo.

## 8. Punti aperti

- **Se il collaboratore possa ritirare una nota inviata** prima che l'approvatore la guardi: sarebbe comodo, ma
  toglierebbe all'invio la sua natura di impegno e complicherebbe le corse fra i due. La proposta è di no; è una
  decisione di prodotto.
