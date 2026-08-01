# UC 0095 — Pagina "App catalog" del backoffice (scoperta e attivazione delle app)

**Area**: 21-catalogo-app-backoffice · **Fase**: evo · **Stato**: 🟢 scritto (evo, da implementare)
**Dipendenze**: UC 0020 (backoffice SPA shell), UC 0022 (pricing-as-code/catalogo), UC 0024 (checkout), UC 0026 (ciclo di vita subscription), UC 0077 (regola unica di accesso)
**Fonte decisioni**: change `0066` (proposta UX approvata — artefatto `changes/0066-app-catalog-billing-ux-proposal/proposta-ux.html`), #09 (pagamenti), #03 (frontend)
**Ultimo aggiornamento**: 2026-08-01

## 1. Obiettivo / Scope

Dare al backoffice una **pagina di catalogo** dedicata — nuova voce di menu "App catalog" sotto Dashboard — dove
l'utente **scopre le app disponibili** sulla piattaforma e le **attiva**, separandola dalla fatturazione (che resta
in Billing, UC 0096). Chiude il punto aperto "vetrina dal catalogo reale" di UC 0024: oggi la lista delle app
acquistabili deriva dai moduli impacchettati nel frontend (`MODULES`) ed è annegata nella pagina Billing.

**Incluso**: read-model del catalogo per-tenant; pagina con griglia di card, ricerca, paginazione, stati; ingresso
al checkout esistente. **Escluso**: modifiche al flusso di checkout (UC 0024); pulizia di Billing (UC 0096);
nuova Dashboard (UC 0097).

## 2. Attori & ruoli

- **Owner/admin del tenant**: naviga il catalogo e attiva le app (azione d'acquisto).
- **Member**: vede il catalogo e gli stati; le azioni d'acquisto non gli sono offerte attive (vede il perché).
- **Sistema**: read-model catalogo (backend `core`), checkout (UC 0024), webhook → entitlement (UC 0025/0077).

## 3. Precondizioni

Utente autenticato nel backoffice. Catalogo popolato dal pricing-as-code (UC 0022: `app`/`app_tier`/`app_price`).

## 4. Flusso principale

1. L'utente apre **App catalog** dal menu (gruppo PLATFORM, sotto Dashboard).
2. La pagina carica il **read-model catalogo per-tenant**: per ogni app pubblicata — nome, **descrizione breve
   localizzata** (5 lingue), tinta/icona di categoria, **prezzo di partenza** ("from €N/mo", dal tier più basso),
   e **stato per questo tenant**, derivato dalle stesse fonti della piattaforma (regola unica di accesso UC 0077 +
   subscription UC 0026): `available` · `active` · `trial` · `payment pending` · `cancellation scheduled` ·
   `disabled by platform`.
3. Le app sono mostrate come **card in griglia** (2–3 per riga su desktop, 1 su mobile) con azione contestuale:
   - `available` → **Subscribe** → flusso di checkout esistente (UC 0024);
   - `active`/`trial` → **Open** (rotta del modulo) + indicazione piano/scadenza trial;
   - `payment pending` → **Fix payment** → Billing;
   - `cancellation scheduled` → **Undo cancellation** → Billing;
   - `disabled by platform` → **Contact support** (nessuna azione d'acquisto).
4. **Ricerca**: un campo filtra le app per nome e descrizione; il conteggio dei risultati è visibile.
5. **Paginazione**: l'elenco è paginato (dimensione pagina decisa in change; il mockup usa 6); la ricerca riporta
   alla prima pagina.
6. Dopo un acquisto completato, il ritorno al catalogo mostra lo stato aggiornato (stessa invalidazione degli
   entitlement di UC 0077 — nessuna nuova meccanica).

## 5. Flussi alternativi / edge / errori

- **Nessun risultato di ricerca**: stato vuoto dedicato con invito a cambiare parola o azzerare il filtro.
- **Catalogo non leggibile**: stato di errore con "riprova" — mai confuso con "nessuna app" (stesso principio
  UC 0077 della sidebar).
- **Member**: le card `available` mostrano il prezzo ma l'azione d'acquisto è disabilitata con spiegazione
  ("chiedi a un owner/admin") — il gate vero resta sul backend.
- **App attiva ma modulo frontend non ancora caricabile**: "Open" segue le regole di routing esistenti (guard).
- La **sidebar non cambia semantica**: continua a mostrare solo le app entitled (il catalogo è la vista completa).

## 6. Schermate & stati

Riferimento visivo vincolante: vista "App catalog" dell'artefatto approvato (change 0066) — card con testata
tinta categoria, titolo, badge stato, descrizione, azione + prezzo; barra ricerca con conteggio; paginazione
Previous/Next con "Page N of M"; stati loading/error/empty/success.

## 7. Dati toccati

- **Nessun dato personale nuovo** (il read-model espone dati di catalogo e stato dell'account già esistenti).
- **Descrizioni brevi localizzate e tinta/icona di categoria**: oggi non esistono come dato di catalogo servito dal
  backend; la change decide la fonte (candidata: pricing-as-code/manifesto app, coerente con "il catalogo è
  codice") e la via di consegna nelle 5 lingue. Nessuna migrazione di dati personali.

## 8. Permessi & gate

- Pagina raggiungibile da ogni utente autenticato del tenant (nessun entitlement richiesto: è la vetrina).
- **Azione d'acquisto** riservata ai ruoli abilitati al billing (come il checkout oggi): il frontend disabilita,
  il backend fa fede.
- Invarianti: `tenant_id` solo dal JWT per lo stato per-tenant; il read-model non accetta identificatori di
  account dal client; logging strutturato su lettura catalogo e avvio checkout.

## 9. Requisiti di test

- Unit: derivazione dello stato della card dalle combinazioni (entitlement × subscription × app disattivata).
- L2 Playwright: ricerca (con e senza risultati), paginazione, azioni per stato, member senza azione d'acquisto,
  errore di lettura ≠ catalogo vuoto.
- **Journey end-to-end di piattaforma** (epica 20): estensione di J-BUY — l'acquisto parte dal catalogo e lo stato
  della card passa `available → active`; voce nel registro di copertura (UC 0093) quando disponibile, altrimenti
  rimando tracciato nel registro alla sua nascita.

## 10. Riferimenti & Definition of Done

- **Decisioni**: change 0066 (`decisions.json` + artefatto approvato); UC 0024 punto aperto "vetrina real-catalog"
  (che questo UC assorbe); UC 0077 (stati e freschezza).
- **DoD**: pagina in produzione locale con `./app-start.sh`; menu aggiornato; i 6 stati corretti contro dati veri;
  ricerca+paginazione; test verdi delle aree toccate; i18n 5 lingue; `_INDEX.md`/indici aggiornati dalla change.

## Punti aperti / decisioni differite

Aperti dalla change `0076` (implementazione di questo use case).

- **Nessuna via all'acquisto dalla vetrina per un'app freemium.** Un'app con una fascia gratuita di
  baseline e fasce a pagamento è, per questo use case, in stato `active`: la card offre "Open" e il nome
  del piano, e il passaggio a una fascia superiore vive in Billing. È coerente con la tabella
  stato → azione approvata (change `0066`), ma significa che per la maggior parte delle app freemium il
  catalogo **non** è l'ingresso all'acquisto che il titolo promette. Chi decide: **UC 0096** (Billing
  solo-fatturazione), che possiede il cambio di piano — se la scelta è portarlo in vetrina, la card
  `active` guadagna un'azione secondaria verso lo stesso selettore di fascia. Effetto collaterale già
  visibile: il journey `J-BUY` deve comprare **Teams** (l'unica app genuinamente `available`) per provare
  la transizione della card, e continua a comprare il Mini-CRM per provare il montaggio del modulo.
- **Ricerca e paginazione lato client.** Decise in locale perché il catalogo è un elenco piccolo e
  limitato (decine di app) e la ricerca deve comunque filtrare l'intero catalogo. Il giorno in cui il
  catalogo crescesse di un ordine di grandezza vanno portate sul server, con l'ordinamento come
  contratto. Nessun use case lo possiede ancora: si riapre qui.
- **Descrizione e categoria non sono persistite nel catalogo del database.** Il read-model le legge
  in-processo dal pricing-as-code (contenuto di presentazione env-agnostico). La decisione va rivista se
  il catalogo diventasse modificabile dalla console admin: in quel caso servono colonne su
  `platform.app`, un ramo di sincronizzazione e la loro migrazione. Possiede il tema **UC 0022**
  (pricing-as-code) insieme alla console di piattaforma.
