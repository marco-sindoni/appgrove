# appgrove — Platform Admin Console

Pannello di **amministrazione di piattaforma** di appgrove, usato dal ruolo **`platform-admin`** per gestire l'intero marketplace multi-tenant. È un'area operatore **distinta** dal backoffice del cliente: amministra *tutti* i tenant, gli utenti, il catalogo app, i modelli di prezzo, il billing di piattaforma e la compliance.

- **File principale:** `AppgroveAdmin.dc.html` — si apre direttamente nel browser, auto-contenuto.
- **Runtime:** `support.js` (da tenere accanto all'HTML).
- **Cross-link:** dal dettaglio account un'azione *"Open backoffice"* apre `Appgrove.dc.html` (il backoffice del cliente), se presente nella stessa cartella.
- **Lingua UI:** Inglese, predisposta al multilingua (selettore **EN/IT** in topbar e in Settings).

> appgrove è un marketplace di micro-SaaS: i clienti (tenant) attivano N app da un catalogo; ogni app inietta sezioni in una sidebar dinamica e ha billing indipendente. Questo console **non** è il backoffice cliente — è l'area operatore della piattaforma.

---

## 1. Navigazione

Chrome permanente: **sidebar admin** raggruppata a sinistra + **topbar** (badge `PLATFORM ADMIN`, breadcrumb, selettore accent, lingua EN/IT, toggle tema, notifiche, profilo operatore). Routing interno a stati con `go(route, sub)`; su schermo stretto la sidebar diventa un **drawer** con hamburger.

| Gruppo | Schermata | Contenuto |
|--------|-----------|-----------|
| PLATFORM | **Overview** | KPI di piattaforma (tenant, abbonamenti attivi, **MRR**, **ARR**, nuovi signup, **churn**), grafico MRR a 6 mesi, pannello *"Needs attention"* (pagamenti falliti, trial in scadenza, coda erasure), feed attività recente |
| PLATFORM | **Accounts (Tenants)** | Tabella con ricerca e filtri (status): account, owner, segmento B2C/B2B/Mixed, n. utenti, n. app, MRR, status, data creazione. Stato di **caricamento (skeleton)** ed **empty state**. Riga → dettaglio |
| PLATFORM | **Account detail** | Header con azioni (**sospendi/riattiva**, *Open backoffice*) + tab: **Users** (ruoli owner/admin/member, invita), **Apps & entitlements** (toggle attiva/sospendi per app), **Billing** (abbonamenti + ID Paddle, banner *past due*), **Danger zone** (sospensione, **erasure GDPR**) |
| PLATFORM | **Users (globale)** | Tutti gli utenti tra i tenant, ricerca + filtro ruolo. Regola: **1 utente → 1 tenant** |
| PLATFORM | **Entitlements** | **Matrice tenant × app**: celle Active / Trial / Paused / — cliccabili per attivare o sospendere |
| CATALOG | **App catalog** | Gestione app del marketplace (status available/deprecated/hidden, capability single/multi-user, mapping Paddle, n. tenant); **modale crea/modifica app** |
| CATALOG | **Pricing models** | Editor del modello di costo per-app: **B2C a funzionalità** (checklist) o **B2B a utenze** (limite posti + prezzo per utente), trial, mensile/annuale, mapping ai prezzi Paddle |
| REVENUE | **Billing & payments** | Vista Paddle: MRR/ARR, **dunning** (pagamenti falliti), rimborsi, stato abbonamenti; banner di **errore sync**; prevalentemente in lettura |
| REVENUE | **Invitations** | Inviti pendenti/recenti tra i tenant (status, scadenza, reinvia/revoca) |
| GOVERNANCE | **Compliance / GDPR** | Coda offboarding/erasure, richieste di export dati, **audit log** delle azioni admin |
| GOVERNANCE | **System** | Stato salute servizi (operational/degraded/down), link a **Swagger/OpenAPI**, log/status page |
| — | **Settings** | Aspetto (tema/accent), lingua EN/IT, profilo operatore |

---

## 2. Principi di design

Riusa **esattamente** il design system del backoffice cliente — estetica ispirata a **n8n**: morbida, arrotondata, neutri caldi.

- **Colore** — neutri caldi (light `#f4f4f1`/bianco; dark `#161512`/`#211f1b`); **accent configurabile** (corallo default, viola, teal, blu); colori funzionali verde/ambra/rosso; fondi di icone e badge sempre con **tinte trasparenti** (`color-mix`), mai tinte piatte.
- **Tipografia** — **Plus Jakarta Sans** per la UI, **JetBrains Mono** per numeri/importi/ID/KPI. Titoli con letter-spacing negativo, peso 800.
- **Icone** — **Material Symbols Rounded** (asse FILL per stati attivi).
- **Forma/profondità** — raggi generosi (card 16–20px, controlli 10–12px, pill), ombre soffici, densità bilanciata.
- **Identità operatore** — badge `PLATFORM ADMIN` e logo con scudo per distinguere chiaramente il contesto dal backoffice cliente.
- **Stati** — **loading** (skeleton shimmer), **empty state** (liste/ricerche senza risultati) ed **error** (banner sync Paddle, servizio *degraded/down*).
- **Responsive** — sidebar collassabile a drawer + hamburger sotto soglia, griglie con `auto-fit` che riflowano, tabelle con scroll orizzontale su schermi stretti.
- **Interazione** — hover ovunque, transizioni .15–.25s, switch/toggle custom, segmented control, tema/accent applicati via **CSS custom properties** sul nodo radice (cambio istantaneo).

---

## 3. Recap delle logiche

Design Component (`AppgroveAdmin.dc.html`): template HTML con segnaposto + classe `Component` che espone i dati al template. Stile **inline**, nessun foglio di stile.

### Stato principale
```
route          schermata corrente (overview, accounts, account, users,
               entitlements, catalog, pricing, billing, invitations,
               compliance, system, settings)
accountId      tenant selezionato nel dettaglio
accSub         tab del dettaglio account (users / apps / billing / danger)
theme          'light' | 'dark'        accent  'coral'|'violet'|'teal'|'blue'
lang           'EN' | 'IT'             navOpen drawer sidebar (mobile)
accSearch/accFilter      ricerca + filtro tabella accounts
userSearch/userFilter    ricerca + filtro utenti globali
catFilter                filtro catalogo (available/deprecated/hidden)
catModal                 app in modifica (o 'new') nella modale catalogo
pricingApp/pricingSeg    app e modello (b2c/b2b) nell'editor prezzi
entOverride              override status entitlement per (tenant:app)
loading                  flag di caricamento iniziale (skeleton ~850ms)
billErr                  banner di errore sync Paddle
```

### Dati demo (coerenti)
- **accounts** — 10 tenant con owner, segmento, n. utenti, n. app, MRR, status, data.
- **users** — utenti globali con `acc` (tenant), `role` (owner/admin/member), status.
- **apps** — catalogo con `model` (single/multi-user), `status`, `paddle` product id, n. tenant.
- **invites**, **accAppsMap** (app per tenant), **trialPairs** (entitlement in trial).

### Meccaniche chiave
- **Routing & navigazione** — `go(route, sub)` e `openAccount(id)` cambiano stato e resettano lo scroll; la sidebar evidenzia la voce attiva (Accounts resta attivo anche nel dettaglio).
- **Entitlements** — `entStatus(tenant, app)` deriva lo stato (active/trial/suspended/none) da `accAppsMap`, dallo status del tenant e dagli override; `toggleEnt` aggiorna l'override. La stessa funzione alimenta sia il tab del dettaglio account sia la **matrice globale**.
- **Account detail** — azione sospendi/riattiva muta lo status del tenant; la tab Billing mostra abbonamenti con ID Paddle e banner *past due* per i tenant sospesi.
- **Pricing editor** — switch `b2c/b2b`: per B2C tier a **funzionalità** (checklist), per B2B tier a **utenze** (limite posti + prezzo per utente); campi prezzo/Paddle/trial come input.
- **Catalogo** — modale crea/modifica (`catModal`) con capability, status e mapping Paddle.
- **Tema/accent/lingua** — `applyTheme()` scrive le CSS custom properties sul nodo radice; esposti anche come **prop del componente** (`theme`, `accent`, `startScreen`).
- **Stati di servizio** — helper `pill(status)` genera badge colorati coerenti per ogni status (account, utenti, entitlement, inviti, servizi, audit).

---

## 4. Come aprirlo
Apri `AppgroveAdmin.dc.html` in un browser moderno. Nessuna build né dipendenze oltre ai font (Google Fonts via `<link>`). Tieni `support.js` nella stessa cartella; opzionalmente anche `Appgrove.dc.html` per far funzionare il link *"Open backoffice"* dal dettaglio account.
