# 0018 — Scadenzario

**Applicazione**: 02 — BillGrove (`billing`) · **Epica**: 04 — Incassi e solleciti
**Storia**: `0018` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0006`, `0012`, `0017`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che il lunedì mattina vuole sapere «chi mi deve pagare questa settimana»
> voglio una vista delle scadenze, con ciò che scade e ciò che è già scaduto
> così da telefonare a chi serve prima che il ritardo diventi un problema di cassa.

**Contesto.** Il ritardo nei pagamenti è il dolore principale delle micro-imprese, tanto che il catalogo gli dedica
un'app intera (CashGrove, 3). BillGrove non fa recupero crediti, ma **deve** dire chi non ha pagato: senza questa
vista, gli incassi registrati nella storia precedente restano dati che nessuno guarda. È anche il presupposto dei
solleciti automatici, che senza scadenze non saprebbero quando partire.

## 2. Requisiti funzionali

1. **RF-1** — Ogni documento emesso ha una **scadenza**, calcolata dai termini di pagamento del cliente o
   impostata a mano sul documento.
2. **RF-2** — Sono ammesse più scadenze su uno stesso documento (pagamento rateale), ciascuna con la propria data e
   il proprio importo.
3. **RF-3** — Esiste una vista dello scadenzario, filtrabile per periodo, cliente e stato (da incassare, scaduto,
   incassato).
4. **RF-4** — La vista mostra per ogni riga i **giorni di ritardo** e il residuo.
5. **RF-5** — Gli incassi registrati si applicano alle scadenze dalla più vecchia alla più recente, se non
   diversamente indicato.
6. **RF-6** — La somma degli importi delle scadenze coincide sempre con il totale del documento.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura e scrittura delle scadenze filtra per `tenant_id` preso dal
  token verificato; lo scadenzario di un account non è mai visibile a un altro.
- **RT-2 — Interfaccia di programmazione (§2).** `GET /api/billing/v1/due-dates` con filtri e paginazione,
  `PUT /api/billing/v1/documents/{id}/due-dates` per impostare le rate; errori in `application/problem+json`;
  definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V9__due_date.sql` sullo schema `app_billing`: tabella `due_date` con
  `tenant_id`, chiave primaria UUID versione 7, colonne di controllo e cancellazione logica; indice su
  `(tenant_id, due_on, status)`, perché è l'interrogazione più frequente dell'app.
- **RT-4 — Modulo frontend (§3, §5).** Sezione «Scadenzario» del modulo `billing`, con i filtri e i totali di
  colonna. Solo token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe visibili passano dallo spazio-nomi `billing` e sono presenti in
  `en, it, fr, es, de`; le date usano il formato della lingua scelta.
- **RT-6 — Varchi e quota (§6).** Nessun consumo di quota: è una lettura.
- **RT-7 — Esposizione conversazionale (§12).** Strumento dichiarato:
  `elenca_non_pagati(giorni_di_ritardo?) → elenco con importo e giorni di scaduto`, marcato **lettura**. È lo
  strumento che risponde alla domanda che il titolare fa più spesso, e per questo va progettato con un risultato
  **minimizzato**: cliente, importo, giorni, non l'intero documento. Dipendenza dichiarata: UC 0061-0063.
- **RT-8 — Dati personali (§10).** Nessun campo personale nuovo; lo scadenzario mostra clienti già dichiarati. Va
  però annotato che il risultato dello strumento conversazionale **esce dal perimetro dell'interfaccia**: deve
  contenere il minimo indispensabile.
- **RT-9 — Registrazione eventi (§14).** Nessun evento nuovo di scrittura; le letture non si registrano una per una.

## 4. Criteri di accettazione

**CA-1 — Scadenza calcolata**
- **Dato** un cliente con termini di pagamento a 30 giorni
- **Quando** si emette una fattura datata 1º settembre
- **Allora** la scadenza è il 1º ottobre e compare nello scadenzario

**CA-2 — Pagamento rateale**
- **Dato** una fattura da 1.200 € · **Quando** si impostano tre rate da 400 €
- **Allora** lo scadenzario mostra tre righe e la loro somma è pari al totale del documento

**CA-3 — Ritardo**
- **Dato** una scadenza del mese scorso non incassata
- **Quando** si apre lo scadenzario filtrato su «scaduto»
- **Allora** la riga compare con i giorni di ritardo calcolati rispetto a oggi

**CA-4 — Incasso applicato alla rata più vecchia**
- **Dato** due rate scadute da 400 € ciascuna · **Quando** si registra un incasso di 400 €
- **Allora** risulta incassata la rata più vecchia e l'altra resta scaduta

**CA-5 — Isolamento fra account**
- **Dato** due account `A` e `B` con proprie scadenze
- **Quando** un utente di `A` apre lo scadenzario, anche forzando l'identificativo di `B`
- **Allora** vede solo le proprie

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend` e `frontend`; l'intera suite prima del commit);
- [ ] prove di **unità** sul calcolo della scadenza e sull'applicazione degli incassi alle rate, di
      **integrazione** sulla vista, con database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** sullo scadenzario;
- [ ] **prova end-to-end**: *coprire ora* — passo «apri lo scadenzario e trova la fattura appena emessa» del
      percorso `[J-BILLING]`; registro di copertura aggiornato;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati**: nessuna voce nuova, dichiarato;
- [ ] **registro delle decisioni** compilato;
- [ ] contratto degli **strumenti conversazionali** dichiarato per `elenca_non_pagati`, con risultato minimizzato;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0006` | I termini di pagamento predefiniti vengono dal cliente |
| storia `0012` | Le scadenze nascono all'emissione |
| storia `0017` | Le scadenze si chiudono con gli incassi |

## 7. Fuori ambito

- i solleciti: storia `0019`;
- la previsione di cassa, il punteggio di rischio e la prioritizzazione dei morosi: sono di CashGrove (3), e
  aggiungerli qui significherebbe invadere il campo di un'altra app;
- gli interessi di mora: idem.

## 8. Punti aperti

Nessuno.
