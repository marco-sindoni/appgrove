# 0017 — Politiche di spesa e massimali

**Applicazione**: 08 — SpendGrove (`notespese`) · **Epica**: 03 — Note spese e approvazione
**Storia**: `0017` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0010`, `0015`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che ha stabilito quanto si può spendere per un pranzo
> voglio che la regola sia scritta nell'app e che chi la supera lo sappia mentre inserisce la spesa
> così da non dover fare il poliziotto ogni fine mese su cose che si potevano dire prima.

**Contesto.** In una micro-impresa le regole di spesa esistono ma sono orali: «per il pranzo trenta euro», «sopra i
cento chiedi prima». Non essendo scritte, si applicano a discrezione, e questo produce le discussioni più
antipatiche dell'amministrazione. Scriverle nell'app le rende **prevedibili**. Va fatta dopo l'approvazione perché
la politica è utile solo se qualcuno la fa valere: il suo effetto naturale è un avviso all'approvatore.

## 2. Requisiti funzionali

1. **RF-1** — Si definiscono politiche per categoria: massimale per singola spesa, massimale giornaliero, obbligo di
   giustificativo sopra una soglia.
2. **RF-2** — Ogni politica ha un **comportamento allo sforamento**: `avvisa` (si può procedere, resta l'avviso) o
   `richiedi motivazione` (si procede solo scrivendo perché). **Non esiste il blocco assoluto**: una spesa già
   sostenuta esiste comunque, e impedire di registrarla la farebbe uscire dall'app invece di farla sparire.
3. **RF-3** — Lo sforamento si segnala **al momento della conferma della spesa**, non a fine mese: è la sola cosa
   che permette a chi ha speso di spiegarsi finché se lo ricorda.
4. **RF-4** — Gli avvisi si sommano nel riepilogo della nota (storia `0013`) e compaiono in evidenza nella schermata
   di approvazione (storia `0015`).
5. **RF-5** — Le politiche possono valere per tutti o per un gruppo di collaboratori; una politica cambiata **non
   riscrive il passato**: le spese già confermate restano valutate con la politica del loro momento.
6. **RF-6** — Un account senza politiche funziona esattamente come oggi: la funzione è facoltativa e non impone
   nessuna soglia predefinita.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Politiche e valutazioni filtrano per `tenant_id` preso dal token
  verificato; la definizione delle politiche è riservata ai ruoli `approva` e `amministra`.
- **RT-2 — Interfaccia di programmazione (§2).** `GET|POST|PATCH /api/notespese/v1/politiche` e la valutazione
  esposta nella risposta di conferma della spesa; errori in `application/problem+json`; definizione OpenAPI
  aggiornata.
- **RT-3 — Persistenza (§8).** Migrazione `V14__politiche_di_spesa.sql`: tabella `politica_di_spesa` con
  `tenant_id`, chiave UUID versione 7, categoria, tipo di limite, valore, comportamento, validità da/a, colonne di
  controllo e cancellazione logica; tabella degli sforamenti registrati, con il riferimento alla politica **nella
  versione vigente al momento della spesa**.
- **RT-4 — Modulo frontend (§3, §5).** Sezione *Impostazioni → Politiche*; avviso in linea nella schermata di
  revisione e nel riepilogo della nota. Solo token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Testi delle politiche e degli avvisi passano dallo spazio-nomi `notespese` e sono
  presenti in `en, it, fr, es, de`; gli importi si mostrano nel formato della lingua scelta.
- **RT-6 — Varchi e quota (§6, §7).** Nessun consumo nuovo: la valutazione avviene dentro la conferma, che consuma
  già la sua unità di `receipts`.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento di scrittura. In lettura, gli sforamenti compaiono
  fra i motivi di attenzione restituiti da `elenca_da_rivedere` e `verifica_deducibilita` (epica 06).
- **RT-8 — Dati personali (§10).** Le politiche non contengono dati personali; **lo sforamento sì**, perché lega una
  persona a un comportamento valutato. Voce nuova nel manifesto in italiano e inglese e tabella degli sforamenti in
  `exportData` e `purgeData`. Il campo della motivazione è **testo libero**: porta l'avviso di non inserire dati
  sensibili.
- **RT-9 — Registrazione eventi (§14).** Gli eventi `politica creata`, `politica modificata`, `sforamento rilevato`
  portano `tenant_id`, `app_id`, `user_id`, identificativo di correlazione e identificativi — mai la motivazione
  scritta.

## 4. Criteri di accettazione

**CA-1 — Avviso allo sforamento**
- **Dato** una politica «Vitto, massimo 30 € per spesa, comportamento *avvisa*»
- **Quando** un collaboratore conferma un pranzo da 42 €
- **Allora** la spesa è confermata e porta un avviso di sforamento visibile nella nota e all'approvatore

**CA-2 — Motivazione richiesta**
- **Dato** una politica con comportamento *richiedi motivazione*
- **Quando** il collaboratore conferma una spesa oltre soglia senza scrivere nulla
- **Allora** la conferma è respinta con `400`, il campo è segnalato; scrivendo la motivazione la conferma riesce e
  il testo viaggia fino all'approvatore

**CA-3 — Massimale giornaliero**
- **Dato** una politica «Vitto, massimo 45 € al giorno» e due pranzi da 25 € nello stesso giorno
- **Quando** si conferma il secondo
- **Allora** l'avviso cita il totale del giorno, non solo la singola spesa

**CA-4 — Il passato non si riscrive**
- **Dato** spese confermate con il massimale a 30 € · **Quando** il massimale viene portato a 20 €
- **Allora** le spese già confermate non diventano sforanti a posteriori, e quelle nuove seguono il valore nuovo

**CA-5 — Ruolo insufficiente**
- **Dato** un collaboratore con solo ruolo `sostiene` · **Quando** tenta di cambiare una politica
- **Allora** riceve `403`

**CA-6 — Isolamento fra account**
- **Dato** due account con la stessa categoria *Vitto* e massimali diversi
- **Quando** ciascuno conferma una spesa da 35 €
- **Allora** ognuno riceve la valutazione della **propria** politica

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno backend e frontend; l'intera suite prima del commit);
- [ ] prove di **unità** sul motore di valutazione (singola spesa, giornaliero, validità nel tempo); di
      **integrazione** sulla conferma con politiche attive, con database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** e di ruolo su politiche e valutazioni;
- [ ] **prova end-to-end**: *rimando* alla storia `0031`, che porta uno sforamento dentro il percorso
      `[J-NOTESPESE]` fino alla schermata di approvazione; registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) aggiornato lì;
- [ ] **traduzioni** presenti in tutte e cinque le lingue, con formati numerici corretti per lingua;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese per gli sforamenti, tabella in esportazione e
      cancellazione;
- [ ] **registro delle decisioni** compilato, con la scelta di non prevedere il blocco assoluto e il perché;
- [ ] contratto degli **strumenti conversazionali**: nessuno nuovo di scrittura; gli sforamenti entrano nelle
      letture dell'epica 06;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| `0010` | Le politiche si appoggiano alle categorie dell'account |
| `0015` | L'avviso serve a chi approva: senza approvazione non avrebbe destinatario |

## 7. Fuori ambito

- Le diarie e i massimali **fiscali** (le soglie di esenzione previste dalla legge): sono un'altra cosa e stanno
  nella storia `0021`. Qui si tratta di regole che l'azienda si dà, non di regole che la legge le impone.
- Le approvazioni condizionate alla soglia («sopra 200 € serve anche il titolare»): è l'approvazione a due livelli,
  fuori ambito nella storia `0015`.

## 8. Punti aperti

- Nessuno.
