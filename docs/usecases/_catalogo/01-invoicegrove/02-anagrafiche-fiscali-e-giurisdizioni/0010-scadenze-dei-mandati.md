# 0010 — Scadenze dei mandati

**Applicazione**: 01 — InvoiceGrove (`einvoicing`) · **Epica**: 02 — Anagrafiche fiscali e giurisdizioni
**Storia**: `0010` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0006`, `0007`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare di una micro-impresa che vende in più paesi
> voglio essere avvisato quando sta per scattare un obbligo che riguarda **me**, non un obbligo generico
> così da arrivarci preparato invece di scoprirlo da una sanzione o da un cliente che rifiuta la fattura.

**Contesto.** Il calendario europeo è un campo minato di date scaglionate per dimensione d'impresa: micro-imprese
polacche dal 1° gennaio 2027 con sanzioni fino al 100% dell'imposta; micro e piccole imprese francesi dal
1° settembre 2027; imprese tedesche sopra €800.000 di fatturato dal 2027 e tutte dal 2028
(descrizione dell'applicazione §2.3). Nessun micro-imprenditore segue questo calendario, ed è precisamente il
motivo per cui compra il prodotto: è la voce «alert scadenze mandati per paese» della scheda di catalogo.

Il catalogo avverte anche (§8) che **le date si muovono**: la Polonia ha già rinviato, la Francia pure. L'avviso
va quindi costruito su un registro aggiornabile (storia `0006`), non su costanti nel codice, e va detto al cliente
che è un promemoria, non una consulenza.

## 2. Requisiti funzionali

1. **RF-1** — L'app calcola, per ciascun soggetto emittente dell'account, quali obblighi lo riguardano e da quando,
   incrociando paese, soglia di applicabilità e stato del registro delle giurisdizioni.
2. **RF-2** — La panoramica mostra gli obblighi con stato `si avvicina` (entro un orizzonte configurabile),
   `attivo` e `in ritardo`, e tace su quelli non applicabili.
3. **RF-3** — Ogni avviso dice tre cose: **cosa** cambia, **da quando**, e **cosa fare adesso** — compreso il caso
   «questo paese non è ancora coperto da InvoiceGrove».
4. **RF-4** — Se la soglia di applicabilità dipende da un dato che l'app non ha (per esempio il fatturato annuo),
   l'app **lo chiede** al soggetto emittente invece di indovinarlo, e finché non lo ha dichiara l'obbligo come
   `da verificare`.
5. **RF-5** — Ogni avviso porta la **data dell'ultimo aggiornamento del registro** e una nota che è un promemoria,
   non una consulenza fiscale.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Il calcolo è per i soggetti emittenti dell'account, filtrati per
  `tenant_id` preso dal token verificato. Prova di isolamento fra due account.
- **RT-2 — Interfaccia di programmazione (§2).** Rotta `GET /api/einvoicing/v1/mandate-watch` che restituisce
  l'elenco degli obblighi con stato; errori in `application/problem+json`; definizione OpenAPI aggiornata nello
  stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V7__mandate_watch.sql`: tabella `mandate_watch` con l'esito calcolato
  per soggetto emittente, `tenant_id`, chiave UUID versione 7, colonne di controllo. Il calcolo è memorizzato per
  poter dire «te l'avevamo segnalato il …», non ricalcolato a ogni lettura.
- **RT-4 — Modulo frontend (§3, §5).** Riquadro «Cosa richiede attenzione» nella panoramica, più l'elenco completo
  in «Impostazioni → Paesi». Solo token del sistema di design; tema chiaro e scuro; gli stati usano i colori
  funzionali, non colori scritti a mano.
- **RT-5 — Cinque lingue (§4).** Testi degli avvisi, degli stati e delle azioni suggerite dallo spazio-nomi
  `einvoicing`, presenti in `en, it, fr, es, de`. Sono testi normativi: una traduzione approssimativa qui fa
  danno, e la storia non è conclusa se ne manca una.
- **RT-6 — Varchi e quota (§6, §7).** Nessun consumo di quota.
- **RT-7 — Esposizione conversazionale (§12).** Strumento dichiarato:
  `list_upcoming_mandates(orizzonte?) → obblighi che riguardano l'account, con data e azione`, marcato
  **lettura**, nessuna conferma. È uno degli strumenti che rendono l'app utile da una chat: «cosa mi cambia
  l'anno prossimo?» è la domanda tipica. Contratto dentro il servizio; server conversazionale non implementato
  (UC 0061-0063).
- **RT-8 — Dati personali (§10).** Nessun dato personale nuovo. **Attenzione**: il fatturato annuo dichiarato per
  la soglia è un dato dell'impresa; se il soggetto emittente è una ditta individuale è riferibile a una persona.
  Va valutato con lo sviluppatore prima di introdurlo (§8).
- **RT-9 — Registrazione eventi (§14).** L'evento `avviso di mandato calcolato` è registrato con `tenant_id`,
  `app_id`, `user_id` e identificativo di correlazione, con il codice paese, senza dati personali.

## 4. Criteri di accettazione

**CA-1 — Obbligo che si avvicina**
- **Dato** un account con un soggetto emittente polacco di dimensione micro e la data odierna a quattro mesi dal
  1° gennaio 2027
- **Quando** apre la panoramica
- **Allora** vede un avviso `si avvicina` che dice cosa cambia, da quando, e cosa fare adesso

**CA-2 — Obbligo non applicabile**
- **Dato** un account con il solo soggetto emittente italiano
- **Quando** apre la panoramica
- **Allora** non vede avvisi su Polonia, Francia o Germania: l'app tace su ciò che non lo riguarda

**CA-3 — Soglia non determinabile**
- **Dato** un soggetto emittente tedesco senza fatturato dichiarato
- **Quando** si calcolano gli obblighi
- **Allora** l'obbligo risulta `da verificare` e l'app chiede il dato mancante, senza indovinarlo

**CA-4 — Paese non coperto**
- **Dato** un soggetto emittente in una giurisdizione dichiarata non implementata con obbligo attivo
- **Quando** apre la panoramica
- **Allora** l'avviso dice esplicitamente che InvoiceGrove non copre ancora quel paese e quale alternativa resta

**CA-5 — Isolamento fra account**
- **Dato** due account con soggetti emittenti in paesi diversi
- **Quando** un utente di uno chiede l'elenco degli obblighi
- **Allora** vede solo i propri

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (backend, frontend);
- [ ] prove di **unità** sul calcolo di applicabilità (paese × soglia × data) e di **integrazione** sulla rotta;
- [ ] prova di **isolamento fra account**;
- [ ] **prova end-to-end**: *rimando* — il percorso `[J-EINVOICING]` (storia `0030`) verificherà la presenza del
      riquadro nella panoramica;
- [ ] **traduzioni** presenti in tutte e cinque le lingue, riviste con attenzione perché sono testi normativi;
- [ ] **manifesto dei dati**: nessuna voce nuova, salvo decisione sul fatturato dichiarato (punto aperto);
- [ ] **registro delle decisioni** compilato, con la scelta «promemoria, non consulenza» e il motivo;
- [ ] contratto degli **strumenti conversazionali** dichiarato per `list_upcoming_mandates`.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| `0006` | Le date e le soglie stanno nel registro delle giurisdizioni |
| `0007` | Servono i soggetti emittenti su cui calcolare l'applicabilità |

## 7. Fuori ambito

- **L'invio dell'avviso per posta elettronica.** Qui l'avviso vive dentro l'app. La notifica in uscita è un
  effetto verso l'esterno e un tema di piattaforma: rimandata deliberatamente, va progettata dove si progettano le
  notifiche, non qui.
- L'aggiornamento automatico del registro delle giurisdizioni da una fonte esterna: il registro si aggiorna con
  una migrazione (storia `0006`), consapevolmente.

## 8. Punti aperti

- **Il fatturato annuo dichiarato** serve per le soglie tedesche ma è un dato in più da chiedere e da custodire.
  Se il soggetto emittente è una ditta individuale, è riferibile a una persona: la classificazione è ambigua e la
  chiude lo sviluppatore. Alternativa: non chiederlo e lasciare l'obbligo `da verificare`, che è meno utile ma non
  introduce nulla.
- **Fino a che punto ci si spinge nel dire "cosa fare adesso"** senza scivolare nella consulenza fiscale. È
  direzione di prodotto e ha risvolti di responsabilità: la proposta è restare su indicazioni operative
  («configura il canale», «chiedi al cliente l'identificativo») e mai su interpretazioni di norma.
