# 0009 — Registrazione degli incassi

**Applicazione**: 03 — CashGrove (`crediti`) · **Epica**: 02 — Portafoglio crediti
**Storia**: `0009` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0007`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come addetta all'amministrazione
> voglio segnare che un bonifico è arrivato e a quali fatture si riferisce
> così da non sollecitare mai più qualcuno che ha già pagato.

**Contesto.** È il requisito che regge la fiducia nell'automatismo: un sollecito mandato a chi ha pagato fa più danno
di un sollecito mancato, perché il cliente spegne l'automatismo e smette di usare il prodotto
([documento capofila](../application-description.md) §11). Senza incassi registrati, il residuo è fermo all'importo di
fattura e ogni numero mostrato dall'app è falso. La riconciliazione automatica dai movimenti del conto richiederebbe un
accesso ai conti di pagamento e resta dichiaratamente fuori dalle 31 storie.

## 2. Requisiti funzionali

1. **RF-1** — L'utente registra un incasso indicando data, importo, mezzo dichiarato e riferimento, e lo imputa a uno o
   più crediti.
2. **RF-2** — Un incasso può coprire più crediti (un bonifico che salda tre fatture) e un credito può ricevere più
   incassi (pagamento a rate): l'imputazione è distribuita per importo, e la somma delle imputazioni non può superare
   l'importo dell'incasso.
3. **RF-3** — L'importo residuo del credito si ricalcola a ogni imputazione; quando arriva a zero il credito passa a
   `incassato`.
4. **RF-4** — Un incasso si può correggere o annullare: il residuo torna indietro e, se il credito era `incassato`,
   riapre — riconsumando una unità di quota, e se il tetto non basta l'annullamento è respinto con la spiegazione.
5. **RF-5** — La scheda del credito mostra la lista degli incassi ricevuti con data e importo.
6. **RF-6** — Un pagamento inferiore al dovuto **non** chiude il credito e resta segnalato come parziale.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura e scrittura dell'entità `Incasso` e delle sue imputazioni filtra
  per `tenant_id` preso dal token verificato; l'imputazione a un credito di un altro account è impossibile e restituisce
  lo stesso errore di un credito inesistente.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `GET`, `POST`, `DELETE /api/crediti/v1/incassi` (e `/{id}`);
  corpo validato; errori in `application/problem+json`; definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione per le tabelle `incasso` e `imputazione` sullo schema `app_crediti`, con
  `tenant_id`, chiave UUID versione 7, colonne di controllo e cancellazione logica. Gli importi sono in valuta con
  precisione decimale esatta, mai in virgola mobile.
- **RT-4 — Modulo frontend (§3, §5).** Modulo di registrazione dell'incasso raggiungibile sia dalla scheda del credito
  sia dalla sezione *Crediti*, con imputazione multipla; solo token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe visibili passano dallo spazio-nomi `crediti` e sono presenti in
  `en, it, fr, es, de`; gli importi sono formattati secondo la lingua attiva.
- **RT-6 — Varchi e quota (§6, §7).** Registrare un incasso non consuma quota; **chiudere** un credito la libera e
  **riaprirlo** la riconsuma. Se al momento della riapertura il tetto è pieno, la richiesta è respinta con `429` e una
  spiegazione che dice che il credito non può tornare aperto finché non si libera spazio.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento in questa storia; `registra_incasso` è dichiarato
  nella storia `0029` come scrittura con bozza e conferma, perché sposta numeri di denaro.
- **RT-8 — Dati personali (§10).** Nessun dato personale nuovo: l'incasso non contiene dati di persone. La tabella è
  comunque aggiunta a `exportData` e `purgeData` perché è **riferibile** al debitore attraverso il credito.
- **RT-9 — Registrazione eventi (§14).** Gli eventi «incasso registrato», «credito chiuso», «credito riaperto» sono
  registrati con `tenant_id`, `app_id`, `user_id`, identificativi e identificativo di correlazione, senza importi né
  dati personali.

## 4. Criteri di accettazione

**CA-1 — Pagamento totale**
- **Dato** un credito da 1.200 € con residuo 1.200 €
- **Quando** si registra un incasso da 1.200 € imputato a quel credito
- **Allora** il residuo va a zero, lo stato passa a `incassato` e il consumo della quota scende di uno

**CA-2 — Pagamento parziale**
- **Dato** lo stesso credito · **Quando** si registra un incasso da 400 € · **Allora** il residuo è 800 €, lo stato
  **non** cambia e la scheda segnala il pagamento parziale

**CA-3 — Un bonifico per tre fatture**
- **Dato** tre crediti da 300 €, 500 € e 200 €
- **Quando** si registra un incasso da 1.000 € imputato ai tre
- **Allora** tutti e tre risultano `incassato` e la somma delle imputazioni è esattamente 1.000 €

**CA-4 — Imputazione eccedente**
- **Dato** un incasso da 500 € · **Quando** si tenta di imputarne 600 € · **Allora** la richiesta è respinta con `400`
  e nulla viene scritto

**CA-5 — Annullamento con quota piena**
- **Dato** un account al tetto della quota e un credito `incassato`
- **Quando** si annulla l'incasso che lo aveva chiuso
- **Allora** la richiesta è respinta con `429` e una spiegazione, e il credito resta `incassato`

**CA-6 — Isolamento fra account**
- **Dato** due account `A` e `B` · **Quando** un utente di `A` tenta di imputare un incasso a un credito di `B` ·
  **Allora** riceve l'errore di risorsa non trovata e nulla viene modificato

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (backend e frontend);
- [ ] prove di **unità** sull'aritmetica delle imputazioni (nessun arrotondamento che perda centesimi) e di
      **integrazione** sulla chiusura e riapertura del credito;
- [ ] prova di **isolamento fra account** sulle risorse introdotte;
- [ ] **prova end-to-end**: *rimando* alla storia `0031`, dove «incassa e verifica che il sollecito non parta» è il
      passo finale del percorso;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese con `incasso` e `imputazione`, presenti in esportazione e
      cancellazione;
- [ ] **registro delle decisioni** compilato, in particolare sul comportamento della quota alla riapertura;
- [ ] contratto degli **strumenti conversazionali**: nessuna aggiunta in questa storia;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0007` | Serve il credito a cui imputare l'incasso |

## 7. Fuori ambito

- La riconciliazione automatica dai movimenti bancari: punto aperto n. 10 del documento capofila §11.
- L'importazione degli incassi da file: rimandata; il caso d'uso frequente è il bonifico singolo registrato a mano.
- Le note di credito: rimandate, sono un documento contabile e appartengono all'app di fatturazione.

## 8. Punti aperti

Nessuno.
