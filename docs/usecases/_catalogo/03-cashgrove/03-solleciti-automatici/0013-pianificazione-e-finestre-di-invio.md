# 0013 — Pianificazione e finestre di invio

**Applicazione**: 03 — CashGrove (`crediti`) · **Epica**: 03 — Solleciti automatici
**Storia**: `0013` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0011`, `0012`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare
> voglio che i solleciti partano da soli negli orari giusti e mai troppo spesso
> così da non dover premere un pulsante ogni mattina e da non mettermi nei guai con clienti tempestati di messaggi.

**Contesto.** Le sequenze e i testi esistono, ma nulla li fa scattare. Questa storia costruisce il motore: ogni giorno
calcola quali passi sono dovuti, li mette in coda e li rilascia nelle finestre consentite. Non è solo una questione di
comodità: il vademecum del Garante sul recupero crediti considera **invasiva** la sollecitazione ripetuta, e il confine
fra sollecito legittimo e molestia lo può far superare il prodotto da solo se le finestre e i tetti di frequenza non
sono presidiati ([documento capofila](../application-description.md) §2.3, punto 4).

## 2. Requisiti funzionali

1. **RF-1** — Una lavorazione programmata quotidiana calcola, per ogni credito attivo, quale passo della sua sequenza è
   dovuto oggi e crea l'invio in stato «in coda».
2. **RF-2** — Gli invii escono solo dentro la finestra consentita dell'account: giorni della settimana e fascia oraria,
   nel fuso orario dell'account; fuori finestra restano in coda fino alla prima finestra utile.
3. **RF-3** — Fra due solleciti allo stesso debitore non possono passare meno di un numero minimo di giorni, deciso dal
   motore e non dall'utente; se il tetto è superato, l'invio slitta e la ragione è registrata.
4. **RF-4** — Se un debitore ha più crediti scaduti che maturano lo stesso giorno, parte **un solo** messaggio con
   l'elenco dei documenti, non un messaggio per fattura.
5. **RF-5** — Ogni invio è idempotente: la stessa combinazione di credito e passo non produce due messaggi, nemmeno se
   la lavorazione viene eseguita due volte.
6. **RF-6** — La sezione *Solleciti* mostra la coda dei prossimi invii con data, ora prevista, destinatario e passo, e
   permette di annullare un singolo invio prima che parta.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** La lavorazione elabora account per account con filtro
  `WHERE tenant_id = :tid`; il raggruppamento per debitore (RF-4) non attraversa mai gli account, nemmeno quando lo
  stesso identificativo fiscale compare in due account diversi.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `GET /api/crediti/v1/invii` (coda) e
  `DELETE /api/crediti/v1/invii/{id}` (annullamento prima della partenza); errori in `application/problem+json`;
  definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione per la tabella `invio_programmato` sullo schema `app_crediti` (credito,
  passo, debitore, canale, istante previsto, stato, motivo dello slittamento) con `tenant_id`, chiave UUID versione 7,
  colonne di controllo e cancellazione logica. Vincolo di unicità su (`tenant_id`, credito, passo) che garantisce
  l'idempotenza di RF-5.
- **RT-4 — Modulo frontend (§3, §5).** Vista della coda dentro la sezione *Solleciti*, con annullamento del singolo
  invio; impostazioni della finestra dentro la sezione *Impostazioni*; solo token del sistema di design; tema chiaro e
  scuro.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe visibili, compresi i motivi di slittamento, passano dallo
  spazio-nomi `crediti` e sono presenti in `en, it, fr, es, de`.
- **RT-6 — Varchi e quota (§6, §7).** La pianificazione non consuma la metrica `crediti_monitorati`. Un account il cui
  abbonamento è passato a `canceled` non produce più invii: la lavorazione lo salta e lo registra.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento in questa storia: la coda si consulta con
  `storico_solleciti` e `elenca_crediti_scaduti` (storia `0028`); annullare un invio programmato è una scrittura e
  passa dalla regola della conferma (storia `0029`).
- **RT-8 — Dati personali (§10).** La tabella `invio_programmato` contiene il riferimento al debitore ma **non** il
  testo né il recapito: quelli si risolvono al momento dell'invio. La tabella è aggiunta a `exportData` e `purgeData`.
- **RT-9 — Registrazione eventi (§14).** Gli eventi «invio programmato», «invio slittato per finestra», «invio slittato
  per frequenza», «invio annullato» sono registrati con `tenant_id`, `app_id`, `user_id` (o «sistema»), identificativi
  e identificativo di correlazione, senza recapiti né nomi.
- **RT-10 — Condotta verso il debitore.** Finestra predefinita: giorni feriali, dalle 9 alle 18, nel fuso dell'account;
  distanza minima fra due solleciti allo stesso debitore: 3 giorni. Sono valori **del motore**: l'utente può
  restringerli, non allargarli oltre un limite invalicabile.

## 4. Criteri di accettazione

**CA-1 — L'invio matura e si mette in coda**
- **Dato** un credito scaduto da 7 giorni con una sequenza che prevede un passo a +7
- **Quando** la lavorazione quotidiana viene eseguita
- **Allora** compare in coda un invio per quel credito, con l'ora prevista dentro la finestra consentita

**CA-2 — Fuori finestra**
- **Dato** una lavorazione eseguita di domenica e una finestra limitata ai giorni feriali
- **Quando** l'invio matura
- **Allora** resta in coda con istante previsto il lunedì successivo e motivo «fuori finestra»

**CA-3 — Distanza minima fra solleciti**
- **Dato** un debitore che ha ricevuto un sollecito ieri e un secondo passo che maturerebbe oggi
- **Quando** la lavorazione viene eseguita
- **Allora** l'invio slitta di almeno tre giorni dal precedente, con motivo «frequenza massima»

**CA-4 — Un solo messaggio per debitore**
- **Dato** un debitore con quattro crediti che maturano lo stesso passo lo stesso giorno
- **Quando** la lavorazione viene eseguita
- **Allora** viene programmato **un** invio, che elencherà i quattro documenti

**CA-5 — Idempotenza**
- **Dato** la lavorazione già eseguita oggi · **Quando** viene eseguita di nuovo · **Allora** la coda non cambia e non
  compaiono invii doppi

**CA-6 — Annullamento**
- **Dato** un invio in coda · **Quando** l'utente lo annulla · **Allora** sparisce dalla coda, il passo risulta saltato
  nella cronologia del credito e il passo successivo resta programmato

**CA-7 — Isolamento fra account**
- **Dato** due account con debitori omonimi · **Quando** la lavorazione viene eseguita · **Allora** i raggruppamenti per
  debitore restano separati e nessun invio riguarda crediti di due account

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (backend e frontend);
- [ ] prove di **unità** sul calcolo del passo dovuto e sulle finestre (compresi cambio d'ora e giorni festivi), di
      **integrazione** sull'idempotenza della lavorazione;
- [ ] prova di **isolamento fra account** sulla lavorazione e sulla coda;
- [ ] **prova end-to-end**: *rimando* alla storia `0031`, che percorre maturazione → invio → esito;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato con `invio_programmato`, presente in esportazione e cancellazione;
- [ ] **registro delle decisioni** compilato, in particolare sui limiti invalicabili di finestra e frequenza e sul
      motivo normativo che li giustifica;
- [ ] contratto degli **strumenti conversazionali**: nessuna aggiunta in questa storia;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0011` | Serve la sequenza da cui ricavare il passo dovuto |
| storia `0012` | Serve il modello da compilare al momento dell'invio |

## 7. Fuori ambito

- La trasmissione vera del messaggio: storia `0014` (posta elettronica) e `0015` (canali brevi).
- La sospensione per promessa o contestazione: storia `0016` — qui il motore prevede il **punto** in cui la
  sospensione verrà interrogata, ma non la implementa.
- Il calendario dei giorni festivi per Paese: si usa il solo criterio dei giorni feriali; una gestione delle festività
  locali è rimandata perché richiede una sorgente di dati che nessuna fonte consultata rende ovvia.

## 8. Punti aperti

I valori dei limiti invalicabili (finestra 9-18 nei giorni feriali, distanza minima di 3 giorni) sono una proposta
prudente derivata dal vademecum del Garante, che però non fissa numeri. Li conferma lo sviluppatore, se possibile in
sede di revisione legale.
