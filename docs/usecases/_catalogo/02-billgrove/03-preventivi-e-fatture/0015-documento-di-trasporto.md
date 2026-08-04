# 0015 — Documento di trasporto

**Applicazione**: 02 — BillGrove (`billing`) · **Epica**: 03 — Preventivi e fatture
**Storia**: `0015` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0012`, `0013`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come piccolo commerciante che consegna la merce prima di fatturarla
> voglio emettere il documento che accompagna la consegna e poi trasformare in fattura le consegne del mese
> così da consegnare subito senza fatturare subito, e da fare una sola fattura di fine mese invece di dodici.

**Contesto.** Il documento di trasporto è nella scheda di catalogo dell'app, ma non è la stessa cosa di una fattura:
ha causali proprie, dati del trasporto, e serve soprattutto a rendere possibile la **fattura differita**, che è il
modo in cui lavora chi consegna spesso allo stesso cliente. Va dopo l'emissione e le imposte, perché ne riusa
entrambi i meccanismi.

## 2. Requisiti funzionali

1. **RF-1** — Si può emettere un documento di trasporto con: cliente, righe (voci o testo libero), causale del
   trasporto, aspetto dei beni, numero di colli, peso e destinazione se diversa dalla sede del cliente.
2. **RF-2** — Il documento di trasporto **non riporta i prezzi** se l'utente non lo chiede: è la forma più usata.
3. **RF-3** — Si possono selezionare più documenti di trasporto dello stesso cliente non ancora fatturati e
   generarne una **bozza** di fattura differita che ne raccoglie le righe.
4. **RF-4** — Un documento di trasporto già fatturato non è riselezionabile.
5. **RF-5** — L'emissione segue le regole della storia `0012`: sezionale proprio, numerazione progressiva, consumo
   di quota, documento congelato.
6. **RF-6** — La fattura differita riporta gli estremi dei documenti di trasporto che la compongono.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura e scrittura del documento di trasporto filtra per
  `tenant_id` preso dal token verificato; raggruppare documenti di trasporto di un altro account è impossibile
  perché non risultano esistenti.
- **RT-2 — Interfaccia di programmazione (§2).** Le rotte dei documenti con tipo `documento_di_trasporto`, più
  `POST /api/billing/v1/documents/deferred-invoice` che riceve l'elenco degli identificativi e restituisce la
  bozza; errori in `application/problem+json`; definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione sullo schema `app_billing` per i campi propri del trasporto (causale,
  aspetto, colli, peso, destinazione) e per il legame «fatturato da», con `tenant_id` e colonne di controllo.
- **RT-4 — Modulo frontend (§3, §5).** Sezione «Documenti di trasporto» con selezione multipla e azione «Fattura le
  consegne scelte». Solo token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe visibili passano dallo spazio-nomi `billing` e sono presenti in
  `en, it, fr, es, de`. Le causali di trasporto sono un elenco predefinito e vanno tradotte con cura, perché sono
  termini tecnici.
- **RT-6 — Varchi e quota (§6, §7).** Il documento di trasporto **consuma una unità** della metrica `documenti`
  all'emissione, come ogni documento emesso; la fattura differita ne consuma un'altra quando viene emessa. Va detto
  chiaramente all'utente, perché chi consegna ogni giorno consuma in fretta: è anche un dato utile alla fermata di
  escalation sul listino.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento proprio in questa stesura: la composizione di una
  fattura differita richiede una selezione che si fa male a parole. `elenca_documenti(tipo='documento_di_trasporto',
  stato='non fatturato')` è però utile ed è coperto dallo strumento di lettura generale (epica 06).
- **RT-8 — Dati personali (§10).** La **destinazione diversa** è un dato personale nuovo quando è l'indirizzo di una
  persona fisica: voce nuova nel manifesto in italiano e inglese, campo annotato `@PersonalData`, già coperta da
  `document` in esportazione e cancellazione.
- **RT-9 — Registrazione eventi (§14).** Gli eventi `documento di trasporto emesso` e `fattura differita generata`
  (con il numero di documenti raggruppati) sono registrati con `tenant_id`, `app_id`, `user_id` e identificativo di
  correlazione.

## 4. Criteri di accettazione

**CA-1 — Emissione**
- **Dato** un cliente e tre righe di merce
- **Quando** si emette un documento di trasporto con causale «vendita»
- **Allora** il documento è numerato sul proprio sezionale, non riporta i prezzi e consuma una unità di quota

**CA-2 — Fattura differita**
- **Dato** tre documenti di trasporto dello stesso cliente non ancora fatturati
- **Quando** si chiede la fattura differita
- **Allora** nasce una bozza di fattura con tutte le righe dei tre documenti e i loro estremi in evidenza

**CA-3 — Documento già fatturato**
- **Dato** un documento di trasporto già incluso in una fattura differita
- **Quando** si tenta di includerlo in un'altra
- **Allora** la risposta è `409` con l'indicazione della fattura che lo contiene già

**CA-4 — Clienti diversi**
- **Dato** documenti di trasporto di due clienti diversi
- **Quando** si tenta di raggrupparli in una sola fattura
- **Allora** la risposta è `409`: una fattura si intesta a un cliente solo

**CA-5 — Isolamento fra account**
- **Dato** due account `A` e `B` con propri documenti di trasporto
- **Quando** un utente di `A` chiede la fattura differita passando anche un identificativo di `B`
- **Allora** la richiesta è rifiutata e nessun dato di `B` compare

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend` e `frontend`; l'intera suite prima del commit);
- [ ] prove di **unità** sul raggruppamento delle righe e di **integrazione** sulla fattura differita, con database
      effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** sul raggruppamento;
- [ ] **prova end-to-end**: *rimando* — non è nel percorso principale `[J-BILLING]`, che segue preventivo →
      fattura → incasso. Motivo: il documento di trasporto riguarda una parte della clientela, non tutta.
      Proprietaria del rimando: storia `0031`;
- [ ] **traduzioni** presenti in tutte e cinque le lingue, comprese le causali di trasporto;
- [ ] **manifesto dei dati** aggiornato per la destinazione diversa;
- [ ] **registro delle decisioni** compilato;
- [ ] contratto degli **strumenti conversazionali**: nessuno proprio, con la motivazione scritta;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0012` | Riusa numerazione, emissione e congelamento |
| storia `0013` | La fattura differita calcola le imposte sulle righe raccolte |

## 7. Fuori ambito

- la **ricevuta**: è un tipo di documento già previsto dalla storia `0002` e si emette con il meccanismo della
  `0012`; non merita una storia propria perché non ha regole aggiuntive;
- la giacenza di magazzino: non è di BillGrove (app 14 del catalogo);
- il tracciamento della spedizione presso un corriere: fuori ambito.

## 8. Punti aperti

Nessuno.
