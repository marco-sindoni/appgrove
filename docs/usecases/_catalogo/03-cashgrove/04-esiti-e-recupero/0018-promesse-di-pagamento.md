# 0018 — Promesse di pagamento

**Applicazione**: 03 — CashGrove (`crediti`) · **Epica**: 04 — Esiti e recupero
**Storia**: `0018` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0016`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come addetta all'amministrazione che ha appena chiuso una telefonata
> voglio annotare che il cliente ha promesso di pagare entro venerdì
> così da non risollecitarlo prima di venerdì e da accorgermi subito se venerdì passa senza il bonifico.

**Contesto.** La promessa di pagamento è la risposta più comune al sollecito, e oggi vive in un post-it o nella memoria
di chi ha telefonato. Se non viene registrata succedono due cose, entrambe brutte: il sollecito successivo parte lo
stesso — e il cliente si offende, avendo appena parlato con qualcuno — oppure la promessa scade e nessuno se ne accorge.
Questa storia trasforma un'informazione orale in un dato che il motore rispetta e che il cruscotto usa.

## 2. Requisiti funzionali

1. **RF-1** — L'utente registra una promessa su un credito indicando data promessa, importo promesso e, facoltativamente,
   una nota.
2. **RF-2** — Una promessa attiva sospende i solleciti automatici sul credito fino al giorno dopo la data promessa.
3. **RF-3** — Se entro la data promessa arriva un incasso che copre l'importo promesso, la promessa risulta
   **mantenuta**; altrimenti, il giorno dopo, risulta **mancata** e i solleciti riprendono.
4. **RF-4** — Una promessa mancata è visibile sulla scheda del credito e del debitore e alza la priorità del credito
   negli elenchi.
5. **RF-5** — Un credito può avere più promesse nel tempo, ma **una sola attiva**: registrarne una nuova chiude la
   precedente come sostituita.
6. **RF-6** — Il conteggio delle promesse mantenute e mancate per debitore è disponibile, perché è la componente più
   informativa del punteggio di rischio.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura e scrittura dell'entità `PromessaDiPagamento` filtra per
  `tenant_id` preso dal token verificato.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `GET`, `POST /api/crediti/v1/crediti/{id}/promesse` e
  `PATCH /api/crediti/v1/promesse/{id}`; corpo validato; errori in `application/problem+json`; definizione OpenAPI
  aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione per la tabella `promessa_di_pagamento` sullo schema `app_crediti` con
  `tenant_id`, chiave UUID versione 7, colonne di controllo e cancellazione logica. Vincolo che garantisce una sola
  promessa attiva per credito.
- **RT-4 — Modulo frontend (§3, §5).** Azione «registra promessa» dalla scheda del credito, indicatore della promessa
  attiva negli elenchi, evidenza delle promesse mancate; solo token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe visibili passano dallo spazio-nomi `crediti` e sono presenti in
  `en, it, fr, es, de`; le date sono formattate secondo la lingua attiva.
- **RT-6 — Varchi e quota (§6, §7).** La promessa non consuma quota; il credito resta monitorato mentre è sospeso.
- **RT-7 — Esposizione conversazionale (§12).** `registra_promessa_di_pagamento(credito, data, importo) → bozza` è
  dichiarato nella storia `0029` come **scrittura con conferma**: è l'operazione da chat più naturale dell'app — si
  registra la promessa mentre si è ancora al telefono — ma sospende i solleciti, quindi non parte senza un sì.
- **RT-8 — Dati personali (§10).** Voce nuova nel manifesto in italiano e inglese: la promessa è un dato di
  **comportamento** riferibile a una persona quando il debitore è persona fisica. Tabella presente in `exportData` e
  `purgeData`. Il campo nota è a testo libero e porta l'avvertenza.
- **RT-9 — Registrazione eventi (§14).** Gli eventi «promessa registrata», «promessa mantenuta», «promessa mancata»
  sono registrati con `tenant_id`, `app_id`, `user_id` (o «sistema»), identificativi e identificativo di correlazione,
  senza importi né dati personali.

## 4. Criteri di accettazione

**CA-1 — La promessa sospende**
- **Dato** un credito scaduto con un sollecito programmato fra due giorni
- **Quando** si registra una promessa di pagamento per fra cinque giorni
- **Allora** il sollecito non parte, e la coda mostra la sospensione con motivo «promessa di pagamento»

**CA-2 — Promessa mantenuta**
- **Dato** una promessa da 800 € per il giorno 20 · **Quando** il giorno 19 si registra un incasso da 800 € · **Allora**
  la promessa risulta mantenuta e il credito risulta `incassato`

**CA-3 — Promessa mancata**
- **Dato** una promessa per il giorno 20 e nessun incasso · **Quando** arriva il giorno 21 · **Allora** la promessa
  risulta mancata, il credito torna sollecitabile e negli elenchi sale di priorità

**CA-4 — Una sola promessa attiva**
- **Dato** una promessa attiva · **Quando** se ne registra un'altra sullo stesso credito · **Allora** la precedente è
  chiusa come «sostituita» e resta nello storico

**CA-5 — Isolamento fra account**
- **Dato** due account `A` e `B` · **Quando** un utente di `A` tenta di registrare una promessa su un credito di `B` ·
  **Allora** riceve l'errore di risorsa non trovata

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (backend e frontend);
- [ ] prove di **unità** sulla valutazione mantenuta/mancata e di **integrazione** sulla sospensione conseguente;
- [ ] prova di **isolamento fra account** sulla risorsa introdotta;
- [ ] **prova end-to-end**: *rimando* alla storia `0031`, che percorre promessa → mancata → ripresa dei solleciti;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese, tabella presente in esportazione e cancellazione;
- [ ] **registro delle decisioni** compilato, in particolare sulla regola della promessa unica attiva;
- [ ] contratto degli **strumenti conversazionali**: la funzione è predisposta, il contratto si dichiara in `0029`;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0016` | La promessa agisce attraverso il meccanismo di sospensione, non ne crea uno parallelo |

## 7. Fuori ambito

- I piani di rientro a rate con scadenze multiple: rimandati. Nel segmento micro la promessa è quasi sempre una sola
  data; se emergerà il bisogno, sarà una storia propria dell'epica 04.
- Il promemoria automatico al debitore il giorno della promessa: rimandato, perché è un sollecito travestito e va
  pensato dentro le sequenze.

## 8. Punti aperti

Nessuno.
