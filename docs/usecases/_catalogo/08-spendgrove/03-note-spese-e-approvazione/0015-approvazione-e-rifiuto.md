# 0015 — Approvazione e rifiuto

**Applicazione**: 08 — SpendGrove (`notespese`) · **Epica**: 03 — Note spese e approvazione
**Storia**: `0015` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0014`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che deve autorizzare i rimborsi
> voglio vedere in una schermata tutto quello che sto approvando, con evidenziato ciò che non torna, e poter
> respingere spiegando perché
> così da mettere la firma con cognizione di causa in due minuti, invece di sfogliare un plico di fogli A4.

**Contesto.** È l'atto che dà senso a tutto il resto: senza un'approvazione identificata e datata, la nota spese è
un elenco di buone intenzioni. Il rischio da attenuare è l'approvazione distratta — quella in cui si preme «sì» su
tutto perché è tardi — e si attenua mostrando gli avvisi **dentro** la schermata di approvazione, non in una
pagina che nessuno apre. È anche il punto in cui va scritto un divieto: l'approvazione **non è un'azione
dell'assistente conversazionale** (descrizione, §7).

## 2. Requisiti funzionali

1. **RF-1** — Chi ha ruolo `approva` vede l'elenco delle note in attesa a lui assegnate, con totale, collaboratore,
   periodo e numero di avvisi.
2. **RF-2** — La schermata di approvazione mostra tutte le spese della nota con i loro giustificativi consultabili
   e gli avvisi in evidenza (giustificativo mancante, pagamento non tracciabile, massimale sforato, coerenza).
3. **RF-3** — L'approvazione porta la nota in `approvata` e registra **chi** ha approvato e **quando**; da quel
   momento la nota non si modifica più.
4. **RF-4** — Il rifiuto richiede un **motivo obbligatorio**, riporta la nota in `bozza` e libera le spese, così che
   il collaboratore possa correggerle e reinviarla.
5. **RF-5** — È possibile respingere **una singola spesa** togliendola dalla nota con un motivo, approvando il
   resto: è il caso più frequente e altrimenti costringerebbe a rifiutare tutto.
6. **RF-6** — Chi ha sostenuto la spesa **non può approvare la propria nota**, salvo che l'account abbia un solo
   membro con ruolo `approva` e sia la stessa persona — caso del professionista solo, che va ammesso e reso
   evidente nella traccia («autoapprovata»).

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Approvazione e rifiuto filtrano per `tenant_id` preso dal token
  verificato; in più il servizio verifica che chi approva sia **l'approvatore assegnato** di quel collaboratore:
  non basta avere il ruolo, bisogna essere il destinatario.
- **RT-2 — Interfaccia di programmazione (§2).** `POST /api/notespese/v1/note-spese/{id}/approva`,
  `POST .../respingi` con motivo obbligatorio, `POST .../spese/{idSpesa}/respingi`; errori in
  `application/problem+json` con `403` distinto per «non sei l'approvatore» e `409` per stato non compatibile;
  definizione OpenAPI aggiornata.
- **RT-3 — Persistenza (§8).** Migrazione `V12__approvazione.sql`: colonne `approvata_da`, `approvata_il`,
  `respinta_da`, `respinta_il`, `motivo_rifiuto`, `autoapprovata` sulla tabella `nota_spese`; tabella della
  cronologia degli atti, in **sola aggiunta**, perché la storia di chi ha deciso cosa non si riscrive.
- **RT-4 — Modulo frontend (§3, §5).** Sezione *Da approvare*; schermata con l'elenco delle spese, il visualizzatore
  del giustificativo e le due azioni; il rifiuto apre una finestra con il campo del motivo. Solo token del sistema di
  design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Testi degli avvisi, delle azioni e dei messaggi passano dallo spazio-nomi
  `notespese` e sono presenti in `en, it, fr, es, de`.
- **RT-6 — Varchi e quota (§6, §7).** Nessun consumo di quota. Con abbonamento `past_due` l'approvazione resta
  possibile; con `canceled` risponde `402`.
- **RT-7 — Esposizione conversazionale (§12).** 🛑 **Divieto esplicito**: l'approvazione **non è esposta come
  strumento**. L'assistente può preparare — `elenca_da_approvare` è una lettura ammessa — ma l'atto di approvare è
  un atto di una persona verso un'altra persona e resta un gesto compiuto nell'interfaccia, con l'identità
  dell'approvatore. Va scritto nel contratto degli strumenti, non solo qui.
- **RT-8 — Dati personali (§10).** Voci nuove nel manifesto in italiano e inglese per «chi ha approvato o respinto e
  perché»: sono dati di attività lavorativa di una persona identificata. Il **motivo del rifiuto è testo libero** e
  porta l'avviso di non inserire dati sensibili — un motivo come «era una visita medica» sarebbe un dato particolare
  scritto a mano.
- **RT-9 — Registrazione eventi (§14).** Gli eventi `nota approvata`, `nota respinta`, `spesa respinta` portano
  `tenant_id`, `app_id`, `user_id`, identificativo di correlazione e identificativi — **mai** il motivo scritto,
  che è testo libero.

## 4. Criteri di accettazione

**CA-1 — Approvazione tracciata**
- **Dato** una nota `inviata` e il suo approvatore assegnato
- **Quando** l'approvatore la approva
- **Allora** la nota è `approvata`, con nome e momento dell'approvatore registrati, e non è più modificabile da
  nessuno

**CA-2 — Rifiuto con motivo**
- **Dato** una nota `inviata` · **Quando** l'approvatore la respinge senza scrivere il motivo
- **Allora** l'operazione è respinta con `400` e il campo è segnalato; scrivendo il motivo, la nota torna in `bozza`
  e le spese tornano libere

**CA-3 — Rifiuto di una sola spesa**
- **Dato** una nota con sei spese di cui una senza giustificativo
- **Quando** l'approvatore respinge quella spesa e approva la nota
- **Allora** la nota è `approvata` con cinque spese, la sesta è tornata libera con il motivo, e i totali sono
  ricalcolati

**CA-4 — Non sei l'approvatore**
- **Dato** un utente con ruolo `approva` ma **non** assegnato a quel collaboratore
- **Quando** tenta di approvare la nota
- **Allora** riceve `403` e nulla cambia

**CA-5 — Autoapprovazione dichiarata**
- **Dato** un account con un unico membro, che sostiene e approva
- **Quando** approva la propria nota
- **Allora** l'operazione riesce e la nota resta marcata come «autoapprovata», visibile nella cronologia e nel
  pacchetto per il commercialista

**CA-6 — Isolamento fra account**
- **Dato** due account `A` e `B` · **Quando** un approvatore di `A` tenta di approvare una nota di `B`
- **Allora** riceve `404`

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno backend e frontend; l'intera suite prima del commit);
- [ ] prove di **unità** sul ricalcolo dei totali dopo il rifiuto di una spesa; di **integrazione** sugli atti con
      database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** e **matrice dei ruoli** sull'approvazione, compreso il caso «ruolo giusto,
      assegnazione sbagliata»;
- [ ] **prova end-to-end**: *coprire ora* i passi «approvo» e «respingo con motivo» nel percorso `[J-NOTESPESE]`,
      attraversando due utenti diversi; registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) aggiornato;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese, compreso l'avviso sul motivo a testo libero;
- [ ] **registro delle decisioni** compilato, con il divieto di approvazione tramite assistente e il perché;
- [ ] contratto degli **strumenti conversazionali**: `elenca_da_approvare` in lettura; l'approvazione **non**
      esposta, e il divieto scritto nel contratto;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| `0014` | Serve una nota inviata da approvare |
| `0012` | Serve l'assegnazione approvatore → collaboratori, che qui viene verificata |

## 7. Fuori ambito

- La registrazione del pagamento del rimborso: storia `0016`.
- L'approvazione a due livelli (per esempio sopra una soglia serve anche il titolare): utile per la piccola impresa,
  ma non per la micro, e raddoppierebbe la macchina a stati. Rimandata a una storia dedicata, se la si vorrà.

## 8. Punti aperti

- **Delega dell'approvazione** durante le assenze (ferie dell'approvatore): oggi la nota resterebbe ferma. Serve
  una regola — sostituto, scadenza, escalation — che è una decisione di prodotto.
