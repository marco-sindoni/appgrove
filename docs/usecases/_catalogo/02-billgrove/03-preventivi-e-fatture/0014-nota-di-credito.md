# 0014 — Nota di credito

**Applicazione**: 02 — BillGrove (`billing`) · **Epica**: 03 — Preventivi e fatture
**Storia**: `0014` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0012`, `0013`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come addetto all'amministrazione che ha appena scoperto un errore su una fattura già emessa
> voglio poterla rettificare in modo corretto, in tutto o in parte
> così da sistemare la posizione del cliente senza toccare un documento che per legge non posso più modificare.

**Contesto.** La storia `0012` chiude a chiave il documento emesso, e fa bene: un documento fiscale non si modifica
e non si cancella. Ma gli errori esistono, i resi esistono, gli sconti concordati dopo esistono. La nota di credito
è **l'unica** via corretta, e senza di essa l'app costringerebbe l'utente a soluzioni scorrette. È quindi la
naturale conseguenza della `0012` e va subito dopo.

## 2. Requisiti funzionali

1. **RF-1** — Da una fattura emessa si può generare una bozza di nota di credito, totale o parziale (righe scelte,
   quantità ridotte).
2. **RF-2** — La nota di credito porta il **riferimento obbligatorio** al documento rettificato, visibile da
   entrambi i lati.
3. **RF-3** — La nota di credito richiede un **motivo** e non è emettibile senza.
4. **RF-4** — L'importo complessivo delle note di credito su una fattura non può superare l'importo della fattura.
5. **RF-5** — L'emissione della nota di credito segue le stesse regole della `0012`: numerazione sul proprio
   sezionale, consumo di quota, documento congelato.
6. **RF-6** — Lo stato di pagamento della fattura rettificata tiene conto della nota di credito: una fattura
   rettificata per intero non risulta più «da incassare».

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Fattura d'origine e nota di credito appartengono allo stesso `tenant_id`,
  preso dal token verificato; rettificare una fattura di un altro account risponde `404`.
- **RT-2 — Interfaccia di programmazione (§2).** `POST /api/billing/v1/documents/{id}/credit-note` che produce la
  bozza; l'emissione passa dalla rotta della storia `0012`. Errori in `application/problem+json`; definizione
  OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Nessuna tabella nuova: tipo `nota_di_credito` sulla tabella `document` e riferimento
  al documento d'origine, già previsti dalla storia `0002`. Il controllo sul non superamento dell'importo è
  transazionale.
- **RT-4 — Modulo frontend (§3, §5).** Azione «Emetti nota di credito» sulla fattura, con scelta delle righe e del
  motivo; sulla fattura compare il collegamento alle note che la rettificano. Solo token del sistema di design; tema
  chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe visibili passano dallo spazio-nomi `billing` e sono presenti in
  `en, it, fr, es, de`.
- **RT-6 — Varchi e quota (§6, §7).** L'emissione della nota di credito **consuma una unità** della metrica
  `documenti`, come ogni documento emesso: a quota esaurita risponde `429`. È una scelta discutibile — si può
  sostenere che rettificare un proprio errore non debba costare quota — e va portata alla fermata di escalation sul
  listino.
- **RT-7 — Esposizione conversazionale (§12).** Strumento dichiarato:
  `emetti_nota_di_credito(id_fattura, motivo, righe?) → bozza`, marcato **scrittura**; produce una bozza e richiede
  conferma umana. L'emissione vera resta a `emetti_documento`, che è **scrittura irreversibile** con conferma
  obbligatoria. Dipendenza dichiarata: UC 0061-0063.
- **RT-8 — Dati personali (§10).** Nessun campo personale nuovo; la nota di credito eredita la copia congelata dei
  dati del cliente dalla fattura d'origine, con la stessa conservazione decennale.
- **RT-9 — Registrazione eventi (§14).** Gli eventi `nota di credito creata` e `rettifica respinta per importo
  eccedente` sono registrati con `tenant_id`, `app_id`, `user_id`, identificativo di correlazione e i due
  identificativi dei documenti, senza dati personali.

## 4. Criteri di accettazione

**CA-1 — Rettifica totale**
- **Dato** una fattura emessa da 1.000 € · **Quando** si emette una nota di credito totale con motivo
- **Allora** nasce un documento di tipo nota di credito da 1.000 €, numerato sul proprio sezionale, legato alla
  fattura, e la fattura non risulta più da incassare

**CA-2 — Rettifica parziale**
- **Dato** una fattura con tre righe · **Quando** si rettificano solo due righe
- **Allora** la nota di credito riporta solo quelle righe e i relativi riepiloghi per aliquota

**CA-3 — Motivo obbligatorio**
- **Dato** una bozza di nota di credito senza motivo · **Quando** si tenta di emetterla
- **Allora** la risposta è `409` con l'indicazione del campo mancante, e nulla viene emesso

**CA-4 — Importo eccedente**
- **Dato** una fattura da 1.000 € già rettificata per 800 €
- **Quando** si tenta una seconda nota di credito da 300 €
- **Allora** la risposta è `409` con l'importo residuo rettificabile, e nulla viene creato

**CA-5 — Isolamento fra account**
- **Dato** una fattura dell'account `B` · **Quando** un utente di `A` tenta di rettificarla
- **Allora** riceve `404`

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend` e `frontend`; l'intera suite prima del commit);
- [ ] prove di **unità** sul calcolo dell'importo residuo rettificabile e di **integrazione** sulla rotta, con
      database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** sulla rettifica di una fattura altrui;
- [ ] **prova end-to-end**: *rimando* — il percorso `[J-BILLING]` copre il cammino felice fino all'incasso; la
      rettifica è coperta dalle prove di integrazione. Motivo: tenere il percorso corto. Proprietaria: storia
      `0031`;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati**: nessuna voce nuova, dichiarato;
- [ ] **registro delle decisioni** compilato, con annotata la questione del consumo di quota sulla rettifica;
- [ ] contratto degli **strumenti conversazionali** dichiarato per `emetti_nota_di_credito`;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0012` | Serve una fattura emessa e il meccanismo di numerazione |
| storia `0013` | I riepiloghi per aliquota della nota di credito seguono le stesse regole |

## 7. Fuori ambito

- la nota di **debito**: rimandata, è meno frequente e si costruisce sullo stesso meccanismo;
- il rimborso vero al cliente: BillGrove registra il documento, non muove denaro;
- l'annullamento della fattura: **non esiste**, ed è una scelta (storia `0012`, fuori ambito).

## 8. Punti aperti

Se l'emissione di una nota di credito debba consumare quota è una decisione di listino, non tecnica: fa parte della
fermata di escalation del §5 della descrizione dell'applicazione. La proposta qui è che consumi, per coerenza con
«ogni documento emesso conta»; l'argomento contrario è che si fa pagare al cliente la correzione di un errore.
