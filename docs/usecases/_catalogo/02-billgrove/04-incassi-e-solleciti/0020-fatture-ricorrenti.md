# 0020 — Fatture ricorrenti

**Applicazione**: 02 — BillGrove (`billing`) · **Epica**: 04 — Incassi e solleciti
**Storia**: `0020` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0012`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come consulente che fattura ogni mese lo stesso canone agli stessi otto clienti
> voglio che il sistema mi prepari da solo le fatture del mese
> così da controllarle e emetterle in cinque minuti, invece di rifarle una per una copiando quelle del mese scorso.

**Contesto.** La fatturazione ricorrente è nella scheda di catalogo dell'app ed è ciò che trasforma BillGrove da
strumento occasionale in abitudine quotidiana. C'è una regola di sicurezza che non si negozia: la ricorrenza
prepara **bozze**, non emette da sola. Un documento fiscale emesso da un automatismo senza che nessuno l'abbia
guardato è un errore che non si può disfare (storia `0012`).

## 2. Requisiti funzionali

1. **RF-1** — Si può creare un modello ricorrente da una fattura esistente o da zero, con cliente, righe, cadenza
   (mensile, bimestrale, trimestrale, annuale), data di inizio ed eventuale data di fine.
2. **RF-2** — Alla scadenza della cadenza il sistema genera una **bozza** di fattura, mai un documento emesso.
3. **RF-3** — L'utente riceve nella Panoramica l'indicazione che ci sono bozze ricorrenti in attesa di controllo.
4. **RF-4** — Un modello si può sospendere e riprendere; una sospensione non genera bozze arretrate al momento della
   ripresa.
5. **RF-5** — Il modello mostra sempre la **prossima generazione** prevista e quelle già fatte.
6. **RF-6** — La generazione è **ripetibile senza danno**: una lavorazione ripetuta due volte nello stesso giorno
   non produce due bozze.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Modelli e bozze generate filtrano per `tenant_id` preso dal token
  verificato; la lavorazione programmata elabora un account alla volta con il contesto impostato in modo esplicito.
- **RT-2 — Interfaccia di programmazione (§2).** `GET|POST /api/billing/v1/recurring-plans`,
  `GET|PUT|DELETE /api/billing/v1/recurring-plans/{id}`, `POST /api/billing/v1/recurring-plans/{id}/pause`; errori
  in `application/problem+json`; definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V11__recurring_plan.sql` sullo schema `app_billing`: tabelle
  `recurring_plan` e `recurring_run` con `tenant_id`, chiave primaria UUID versione 7, colonne di controllo e
  cancellazione logica. Il vincolo di unicità su `(tenant_id, plan_id, periodo)` è ciò che rende la generazione
  ripetibile senza danno.
- **RT-4 — Modulo frontend (§3, §5).** Sezione «Ricorrenti» del modulo `billing` e avviso nella Panoramica quando
  ci sono bozze da controllare. Solo token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe visibili passano dallo spazio-nomi `billing` e sono presenti in
  `en, it, fr, es, de`, comprese le cadenze.
- **RT-6 — Varchi e quota (§6, §7).** La **bozza generata non consuma quota**: la consumerà l'emissione (storia
  `0012`). È una scelta importante: se la generazione consumasse quota, un account con la ricorrenza attiva e la
  quota esaurita si troverebbe bloccato senza aver emesso nulla. Per un account con abbonamento non attivo la
  lavorazione non genera nulla.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento proprio: le bozze generate si leggono con
  `elenca_documenti(stato='bozza')` (epica 06). Creare o cambiare un modello ricorrente da chat non è previsto in
  questa stesura, perché un modello sbagliato produce errori ripetuti nel tempo, e va dichiarato.
- **RT-8 — Dati personali (§10).** `recurring_plan` porta il riferimento al cliente: va aggiunta a `exportData` e
  `purgeData`. Nessun campo personale nuovo.
- **RT-9 — Registrazione eventi (§14).** Gli eventi `bozza ricorrente generata`, `modello sospeso` e `generazione
  saltata per abbonamento non attivo` sono registrati con `tenant_id`, `app_id`, `user_id` e identificativo di
  correlazione, senza dati personali.

## 4. Criteri di accettazione

**CA-1 — Generazione della bozza**
- **Dato** un modello mensile attivo con data di generazione oggi
- **Quando** la lavorazione programmata gira
- **Allora** nasce **una bozza** di fattura con le righe del modello, e nessun documento risulta emesso

**CA-2 — Ripetizione senza danno**
- **Dato** la lavorazione già eseguita oggi · **Quando** viene eseguita una seconda volta
- **Allora** non nasce una seconda bozza per lo stesso periodo

**CA-3 — Sospensione**
- **Dato** un modello sospeso da due mesi · **Quando** lo si riprende
- **Allora** viene generata solo la bozza del periodo corrente, non le due arretrate

**CA-4 — Quota esaurita**
- **Dato** un account con quota `documenti` esaurita e un modello attivo
- **Quando** la lavorazione gira
- **Allora** la bozza viene comunque generata; sarà l'emissione a rispondere `429`

**CA-5 — Isolamento fra account**
- **Dato** due account con modelli attivi · **Quando** la lavorazione gira
- **Allora** ogni bozza nasce nell'account del proprio modello, e nessuna incrocia i due

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend` e `frontend`; l'intera suite prima del commit);
- [ ] prove di **unità** sul calcolo della prossima data e di **integrazione** sulla generazione ripetuta, con
      database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** sulla lavorazione in blocco;
- [ ] **prova end-to-end**: *rimando* — la lavorazione programmata non si presta al percorso interattivo; coperta da
      prove di integrazione. Proprietaria del rimando: storia `0031`;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato con `recurring_plan` in esportazione e cancellazione;
- [ ] **registro delle decisioni** compilato, con annotata la regola «la ricorrenza prepara, non emette»;
- [ ] contratto degli **strumenti conversazionali**: nessuno proprio, con la motivazione scritta;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0012` | La bozza generata si emette con il meccanismo di numerazione |

## 7. Fuori ambito

- l'emissione automatica senza controllo umano: **esclusa per scelta**, non rimandata;
- l'addebito automatico al cliente: BillGrove non incassa denaro;
- gli abbonamenti con fasce di consumo variabile: rimandati, non sono un bisogno del segmento micro.

## 8. Punti aperti

Nessuno. Se qualcuno chiederà l'emissione automatica, la risposta è nella storia `0012`: un documento emesso non si
disfa, quindi lo emette una persona.
