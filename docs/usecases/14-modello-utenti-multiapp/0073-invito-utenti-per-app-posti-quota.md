# UC 0073 — Invito utenti per-app con "posti" come metrica quota stock

**Area**: 14-modello-utenti-multiapp · **Fase**: evo · **Stato**: 🗄️ **SUPERATA dall'epica 22** — archivio della decisione precedente, **non da implementare**
**Dipendenze**: UC 0072 (Distinzione B2C/B2B a livello app + semantica gestione utenti), UC 0027 (Enforcement entitlement + quota SPI flow/stock runtime), UC 0046 (skill new-application), UC 0047 (skill pricing-change), UC 0013 (Accounts/Users/Invitations + core REST API)
**Fonte**: R3 (Tabella residui _INDEX.md) · docs/_BACKLOG.md §"Modello di gestione utenti — tenant-level vs per-app (B2B/B2C)"
**Ultimo aggiornamento**: 2026-07-26


> **Questa storia è superata e non va implementata.** L'epica **22 — rifacimento del modello di appartenenza**
> (change `0087`) adotta il modello **opposto** a quello di questa epica: appartenenza **centralizzata** sulla
> piattaforma, ruolo della persona **sull'applicazione**, posti **di piattaforma** a listino unico — cioè
> esattamente l'opzione che l'epica 14 registrava come scartata. Lo sviluppatore ha cambiato direzione, e la
> decisione è registrata in `changes/0087-epica-22-refactor-membership-model/decisions.json`.
>
> **Sostituita da**: [0098](../22-refactor-membership-model/story/0098-modello-dati-accesso-per-applicazione.md) (accesso per applicazione) e [0102](../22-refactor-membership-model/story/0102-listino-posti-a-fasce.md)–[0103](../22-refactor-membership-model/story/0103-acquisto-anticipato-posto-invito.md) (posti di piattaforma).
>
> Il documento resta come **archivio**: dice cosa si era pensato e perché non si è fatto. Cancellarlo farebbe
> perdere il ragionamento, che è la parte che serve a chi un giorno riaprirà la questione.

## 1. Obiettivo / Scope

Per un'app B2B (a più persone), stabilire che il **numero di utenti ammessi** è una **quota della singola app** — i
"posti" — e non un limite dell'account. Ogni app B2B definisce i **propri** posti e il **proprio** prezzo, in modo
indipendente dalle altre app dello stesso account. Esempio dal backlog: l'app A offre 20 utenti a 10 euro; l'app B, allo
stesso prezzo di 10 euro, ne offre 30. I due listini non hanno alcun vincolo reciproco.

Il meccanismo si appoggia al modello di quota già esistente, che distingue due tipi di limite (UC 0027, Enforcement
entitlement + quota SPI flow/stock runtime): la quota **a consumo nel tempo** (in gergo *flow*, es. "N invii al mese")
e la quota **a giacenza** (in gergo *stock*, es. "N righe presenti contemporaneamente"). I **posti sono una metrica di
tipo stock per-app**: contano quanti membri esistono contemporaneamente in quell'app, non quanti se ne creano nel
tempo.

**Incluso**: definizione dei posti come quota stock per-app; il gate che blocca il nuovo invito quando i posti sono
esauriti; dove vivono il limite (`app_tier.limits`) e il prezzo (il listino dell'app); come le skill di catalogo e
prezzi trattano i posti. **Escluso**: la distinzione B2C/B2B in sé → UC 0072 (Distinzione B2C/B2B a livello app +
semantica gestione utenti); la directory cross-app e la schermata "Membri" → UC 0074 (Directory cross-app + UI "Membri"
ripensata per-app).

## 2. Attori & ruoli

- **Proprietario/Amministratore** (*owner*/*admin*) dell'app B2B: invita utenti e vede quanti posti restano.
- **Membro** (*member*): occupa un posto una volta accettato l'invito.
- **Chi definisce il listino** (Platform Engineer via skill): fissa i posti per fascia di prezzo (in gergo *tier*) alla
  nascita dell'app — UC 0046 (skill new-application) — o li cambia dopo — UC 0047 (skill pricing-change).
- **Sistema di enforcement** (UC 0027): valuta la quota stock "posti" a ogni invito e la fa rispettare a runtime.

## 3. Precondizioni

- L'app è B2B secondo `App.user_model` — UC 0072 (Distinzione B2C/B2B a livello app + semantica gestione utenti).
- L'account ha un abbonamento attivo per quell'app, quindi una fascia di prezzo (*tier*) con i propri limiti in
  `app_tier.limits`.
- È disponibile il motore di quota flow/stock — UC 0027 (Enforcement entitlement + quota SPI flow/stock runtime).
- È disponibile il nucleo utenti/inviti — UC 0013 (Accounts/Users/Invitations + core REST API).

## 4. Flusso principale

1. Owner/admin apre "Membri" dell'app B2B e avvia un invito (email + ruolo).
2. Prima di creare l'invito, il sistema calcola i **posti occupati** dell'app: membri attivi **più** inviti pendenti,
   per quell'`app_id`, per quell'account.
3. Confronta gli occupati con il limite "posti" della fascia attiva, letto da `app_tier.limits` per quell'app.
4. **Se c'è posto**: crea l'invito, che da subito **occupa un posto** (l'invito pendente conta, per non superare il
   limite quando tutti accetteranno). Log strutturato con `tenant_id`, `app_id`, `user_id`, posti occupati/limite.
5. L'invitato accetta (flusso lato invitato di UC 0017): il posto passa da "pendente" a "occupato da membro attivo".
6. Alla **revoca** di un invito o alla **rimozione/disattivazione** di un membro, il posto si **libera** e torna
   disponibile per un nuovo invito.
7. La schermata mostra sempre "posti usati / posti totali" (dettaglio in UC 0074).

## 5. Flussi alternativi / edge / errori

- **Errore — posti esauriti**: l'invito è rifiutato con errore tipizzato (*problem+json*) che indica quota superata, con
  un messaggio che invita a liberare un posto o passare a una fascia superiore. È lo stesso trattamento di ogni altra
  quota stock (UC 0027).
- **Edge — invito pendente che scade**: se un invito scade senza essere accettato, il posto che occupava si **libera**;
  la scadenza va conteggiata come liberazione, non come dato fermo.
- **Edge — downgrade di fascia sotto i posti già occupati**: se l'account passa a una fascia con **meno** posti di
  quanti ne sta usando, non si espellono utenti in automatico. Vale la politica di cambio fascia (in gergo
  *TierChangePolicy*): si blocca l'aumento futuro finché il numero non rientra, ma i membri esistenti restano. Il caso è
  di competenza di UC 0047 (skill pricing-change); qui si annota il vincolo.
- **Edge — ultimo owner**: non conta come "posto liberabile" liberamente; non si può rimuovere l'ultimo proprietario
  (vincolo ereditato da UC 0059, Gestione membri & inviti UI backoffice).
- **Edge — corsa fra due inviti simultanei sull'ultimo posto**: il controllo di quota deve essere atomico rispetto alla
  creazione dell'invito, per non superare il limite con due richieste concorrenti.

## 6. Schermate & stati

La metrica dei posti compare nella schermata "Membri" del backoffice (oggi tenant-level, UC 0059 — Gestione membri &
inviti UI backoffice; ridisegno per-app in UC 0074):

- **Indicatore posti**: "Posti usati X su Y" ben visibile in cima alla sezione, con conteggio che include i membri
  attivi e gli inviti pendenti.
- **Stato pieno**: quando X = Y, il pulsante "Invita" è disabilitato con spiegazione ("Hai occupato tutti i posti.
  Libera un posto o passa a una fascia con più posti."). Eventuale richiamo alla pagina prezzi dell'app.
- **Stati**: caricamento del conteggio, vuoto (solo il proprietario), errore (conteggio non disponibile → non
  permettere inviti alla cieca), successo dopo invito.
- Copy in italiano e inglese; nessuna sigla non spiegata nel testo mostrato all'utente. Il dettaglio di layout è in
  UC 0074.

## 7. Dati toccati

- **`app_tier.limits`**: contiene il limite "posti" per fascia di prezzo dell'app. È il luogo dove il numero di posti
  vive come limite stock, accanto agli altri limiti dell'app. Nessuna struttura nuova: si usa la chiave di limite
  esistente per una metrica stock, coerente con UC 0027 (Enforcement entitlement + quota SPI flow/stock runtime).
- **Listino/prezzo dell'app** (il pricing scritto come codice, gestito dalle skill): il **prezzo** dei posti è per-app e
  indipendente dalle altre app — scritto alla nascita da UC 0046 (skill new-application), modificato dopo da UC 0047
  (skill pricing-change).
- **`platform.membership`** (UC 0116) e **`invitations`**: sorgente del **conteggio** posti (appartenenze attive + inviti pendenti), filtrati
  per account e per app.
- **Dati personali** (email/nome degli invitati): trattamento **già dichiarato** in UC 0013 (Accounts/Users/Invitations
  + core REST API). Ai fini del manifesto:
  - *categoria*: dati di contatto/identificativi;
  - *finalità*: gestire l'accesso a un'app B2B entro i posti acquistati;
  - *base giuridica*: esecuzione del contratto con l'account titolare;
  - *conservazione*: legata all'appartenenza/all'invito (il posto liberato non conserva l'invito scaduto oltre il
    necessario).
  Nessun nuovo trattamento: la novità è **contare** appartenenze già trattate come metrica di quota.

## 8. Permessi & gate

- **`tenant_id` solo dal token verificato**: l'account non arriva mai dal corpo o dai parametri; sempre dal claim
  verificato. Invariante non negoziabile.
- **Filtro riga per riga**: il conteggio dei posti e ogni scrittura portano `WHERE tenant_id = :tid` e il vincolo
  sull'`app_id`. Un account non può contare né consumare i posti di un altro.
- **Gate quota "posti" per-app come metrica stock**: prima di creare un invito, il motore di enforcement (UC 0027)
  valuta la quota stock "posti" della fascia attiva per quell'app; se è satura, blocca. È il presidio centrale di questo
  use case.
- **Ruoli**: solo `owner`/`admin` possono invitare e quindi occupare posti; `member` no.
- **Gate di modello a monte**: gli inviti esistono solo se l'app è B2B (UC 0072); questo gate quantitativo si applica
  dopo quello.

## 9. Requisiti di test

- **Unità**: il conteggio posti = membri attivi + inviti pendenti; la revoca/scadenza libera il posto.
- **Integrazione** (con database reale, tipo Testcontainers): invitare fino al limite riesce; l'invito oltre il limite è
  rifiutato con *problem+json* "quota superata"; dopo una revoca l'invito torna possibile.
- **Concorrenza**: due inviti simultanei sull'ultimo posto → uno solo passa, l'altro è respinto (nessun sforamento).
- **Indipendenza fra app**: due app B2B dello stesso account con limiti posti diversi non si influenzano; consumare i
  posti dell'app A non tocca l'app B.
- **Isolamento fra account**: il conteggio di un account non vede i posti/utenti di un altro.
- **End-to-end** (Playwright): a posti pieni il pulsante "Invita" è disabilitato con spiegazione; l'indicatore "usati/
  totali" è corretto dopo invito e dopo revoca.
- Verde su `run-tests.sh` per le aree toccate (backend, frontend) prima del merge.

## 10. Riferimenti & Definition of Done

- **Fonte**: R3 della tabella dei residui in `_INDEX.md`; sezione dedicata di `docs/_BACKLOG.md`.
- **Use case sorelle**: [UC 0072 (Distinzione B2C/B2B a livello app + semantica gestione utenti)](0072-distinzione-b2c-b2b-livello-app.md),
  [UC 0074 (Directory cross-app + UI "Membri" ripensata per-app)](0074-directory-cross-app-ui-membri.md).
- **Definition of Done**:
  1. I posti sono una quota **stock per-app** in `app_tier.limits`, valutata dal motore di UC 0027;
  2. il gate blocca l'invito oltre il limite in modo tipizzato e atomico;
  3. inviti pendenti e membri attivi occupano posto; revoca/scadenza/rimozione lo liberano;
  4. le skill di catalogo/prezzi (UC 0046, UC 0047) scrivono/cambiano i posti e il loro prezzo per-app;
  5. `run-tests.sh` verde sulle aree toccate.

## Punti aperti / decisioni differite

- **Direzione preferita, non ancora decisa**: come per l'intera epica 14, i "posti per-app" sono la **direzione
  preferita** ma **non una decisione presa**: attendono la **sessione dedicata** di piattaforma richiesta dall'utente.
- **Opzione scartata**: l'alternativa di un listino "posti" **centrale di piattaforma**, unico e slegato dalle app, è
  stata **scartata dall'utente** — proprio perché renderebbe la quota centrale invece che per-app. I posti per-app sono
  la conseguenza diretta di quella scelta.
- **Politica di downgrade con posti già occupati**: la regola precisa (bloccare gli aumenti finché il numero rientra,
  senza espellere) è di competenza di UC 0047 (skill pricing-change); qui è solo vincolata, non decisa.
- **Prezzo dei posti**: che i posti abbiano un prezzo per-app indipendente è direzione preferita; le fasce concrete e
  gli importi sono decisioni di prodotto/prezzi, non di questo use case (owner: catalogo/prezzi).
- **Conteggio degli inviti scaduti**: confermare in sede di implementazione che la scadenza liberi il posto in tempo
  reale e non lasci "posti fantasma".
- **Proprietà**: parte dell'epica 14 (decisione di piattaforma trasversale), con impatti su catalogo e prezzi delle app
  (UC 0046, UC 0047) e sul motore di quota (UC 0027).
