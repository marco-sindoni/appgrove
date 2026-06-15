# appgrove — Backoffice multi-applicazione

Prototipo navigabile di un backoffice in cui un cliente acquista **N applicazioni** da un catalogo. Ogni app, una volta attiva, inietta le proprie **sezioni e sottosezioni** in un menu laterale dinamico, ha un **billing indipendente** (attivabile/disattivabile) e tier di abbonamento dedicati. Tutto in **light e dark mode**, con **accent color** configurabile.

- **File principale:** `Appgrove.dc.html` — si apre direttamente nel browser, è un'unica applicazione auto-contenuta.
- **Brand:** appgrove · workspace dimostrativo "Studio Marchetti" (persona: PMI / professionista).
- **Lingua UI:** Inglese (predisposto al multilingua).

---

## 1. Mappa di navigazione

L'app è una SPA con routing interno a stati. La barra laterale e la topbar restano sempre visibili; cambia solo l'area centrale di contenuto. Di seguito **tutte le schermate** raggiungibili e come arrivarci.

### Chrome permanente
- **Sidebar (sinistra, 266px)** — logo appgrove, ricerca, sezione *PLATFORM*, sezione *YOUR APPS* (menu dinamico), Settings, profilo utente.
- **Topbar** — breadcrumb contestuale, selettore accent color (4 pallini), selettore lingua (EN), toggle tema light/dark, notifiche.

### Schermate

| # | Schermata | Come ci si arriva | Cosa contiene |
|---|-----------|-------------------|---------------|
| 1 | **Dashboard** | Sidebar → *Dashboard* | Saluto, 4 KPI (app attive, fatture aperte, prenotazioni, spesa mensile), elenco app attive, riepilogo spesa per app, attività recenti |
| 2 | **App catalog** (store) | Sidebar → *App catalog* / "Browse apps" | Filtri per categoria, griglia di 10 app con badge B2C/B2B, prezzo e pulsante *Get app* / *Installed* |
| 3 | **App detail / acquisto** | Catalog → click su una card app | Hero app (rating, installazioni, categoria), screenshot placeholder, descrizione, feature, **card prezzi sticky** con toggle mensile/annuale e CTA *Start free trial* |
| 4 | **Billing & subscriptions** | Sidebar → *Billing* | Toggle mensile/annuale (-17%), 3 stat (totale, abbonamenti attivi, risparmio annuale), **tabella per-app** con piano, dimensione di fatturazione, prezzo, rinnovo e **switch attiva/sospendi** indipendente |
| 5 | **Plans & pricing** | Billing → *Compare plans* · Manage → *Change plan* | **3 tier B2C** (Mindmaps: differenziati per *funzionalità*) + **3 tier B2B** (CRM: differenziati per *numero di utenze*, prezzo per utente). Tier "Most popular" evidenziato, piano corrente selezionabile |
| 6 | **Manage app** (singola app) | Dashboard/Invoices → ingranaggio · breadcrumb | Header con **switch attiva/disattiva**, card abbonamento, utilizzo del periodo, **elenco sezioni che l'app aggiunge alla sidebar**, *Cancel subscription* e *Uninstall* |
| 7 | **Cancel / Opt-out** | Manage → *Cancel subscription* | Card "**giorni di accesso residui**" con barra di avanzamento, "cosa succede se cancelli", motivo dell'abbandono, alternativa *Pause*, e stato finale **"Cancellation scheduled"** con *Resume* |
| 8 | **App: Invoices** | Sidebar → *Invoices* (+ sottosezioni) | 4 stat (in sospeso, incassato, scaduto, bozze), tab di stato, tabella fatture con avatar cliente, importi e badge di stato |
| 9 | **App: Calendar** | Sidebar → *Calendar* (+ sottosezioni) | Vista mensile (Giugno 2026), griglia 7×6 con giorno corrente evidenziato ed eventi colorati per app |
| 10 | **App: Mindmaps** | Sidebar → *Mindmaps* (+ sottosezioni) | Canvas con sfondo a griglia di punti, nodo centrale + rami + figli, connettori curvi SVG, toolbar zoom/share |
| 11 | **Settings** | Sidebar → *Settings* | Aspetto (tema, accent, lingua), Account, Notifiche con toggle, *Re-run setup wizard* |
| 12 | **Onboarding** | Profilo utente → *Re-run setup* / *Skip setup* | Wizard a 4 step (Welcome → Workspace → Pick apps → Done) in overlay full-screen con indicatore di avanzamento |

### Menu dinamico (cuore del prodotto)
Nella sezione **YOUR APPS** ogni app installata è una voce **espandibile** che mostra le proprie sottosezioni:

- **Invoices** → Overview · Invoices · Clients · Reports
- **Calendar** → Schedule · Bookings · Resources
- **Mindmaps** → My maps · Templates · Shared with me

Quando un'app viene **sospesa** (dalla schermata Manage o dallo switch in Billing) viene marcata *PAUSED*, attenuata, e concettualmente le sue sezioni spariscono dalla navigazione. Questo dimostra il legame "1 app acquistata = N sezioni nel menu".

---

## 2. Principi di design

Riferimento estetico: **n8n** — morbido, arrotondato, neutro e caldo, mai aggressivo.

### Colore
- **Neutri caldi** (non grigi freddi): sfondo `#f4f4f1`, superfici bianche, testo `#262420`. In dark mode tonalità calde scure (`#161512` / `#211f1b`).
- **Accent configurabile** tra 4 palette costruite con la stessa luminosità/croma e sola variazione di tinta: **corallo** (default, stile n8n), **viola**, **teal**, **blu**.
- **Colori funzionali**: verde (successo/attivo), ambra (attenzione/pausa), rosso (azioni distruttive), più i colori-brand delle singole app (blu, teal, viola, ambra…).
- I colori non sono mai "a tinta piatta" sui contenitori: si usano **tinte trasparenti** (`color-mix`) per i fondi delle icone e dei badge, così l'app resta leggera.

### Tipografia
- **Plus Jakarta Sans** (geometrica, morbida, moderna) per tutta l'interfaccia.
- **JetBrains Mono** per numeri, importi, ID fattura e KPI — dà ritmo "da prodotto" e allineamento tabellare.
- Titoli con `letter-spacing` negativo e peso 800 per gerarchia netta.

### Forma, spazio, profondità
- **Raggi generosi**: card 16–20px, bottoni/input 10–12px, pill arrotondate al massimo.
- **Ombre soffici e diffuse** (`--shadow-sm` / `--shadow` / `--shadow-lg`), mai dure.
- **Densità bilanciata**: aria sufficiente senza sprechi, layout su griglia con `gap` espliciti.

### Iconografia
- **Material Symbols Rounded** (variante arrotondata coerente con l'estetica). Asse `FILL` usato per evidenziare stati attivi.
- Nessuna illustrazione SVG disegnata a mano; per le immagini reali si usano **placeholder a righe** con etichetta monospace.

### Interazione
- Stati **hover** su tutti gli elementi cliccabili; transizioni brevi (.15–.25s).
- **Switch/toggle** custom (tema, attiva/disattiva app, abbonamenti, notifiche) con knob animato.
- **Segmented control** per scelte binarie (mensile/annuale, light/dark, viste calendario).
- Tema e accent applicati via **CSS custom properties** sul nodo radice → cambio istantaneo e globale.

---

## 3. Recap delle logiche

L'app è un **Design Component** (`Appgrove.dc.html`): un template HTML con segnaposto + una classe `Component` che espone i dati al template. Stile **inline** (nessun foglio di stile) così la pagina dipinge subito.

### Modello di stato (principali campi)
```
route          schermata corrente (dashboard, catalog, billing, plans, optout,
               manage, settings, onboarding, detail, invoices, calendar, mindmaps)
sub            sottosezione attiva dell'app corrente
theme          'light' | 'dark'
accent         'coral' | 'violet' | 'teal' | 'blue'
expanded       quali app del menu laterale sono espanse
apps           stato attivo/sospeso per app installata
subStatus      stato di ogni abbonamento (active / paused / trial)
cycle          'monthly' | 'annual'  (condiviso da Billing, Plans, Detail)
planCur        tier corrente per segmento  { b2c: 'Pro', b2b: 'Business' }
cat / invTab   filtro categoria catalogo / filtro stato fatture
onbStep/onbApps stato del wizard di onboarding
optReason/optDone stato del flusso di cancellazione
```

### Routing
- `go(route, sub)` cambia schermata e resetta lo scroll.
- `goApp(id)` apre un'app e ne espande il menu selezionando la prima sottosezione.
- `toggleExpand(id)` espande/comprime la voce app nella sidebar.
- I flag booleani (`isDashboard`, `isBilling`, `isPlans`, `isOptOut`, …) pilotano i blocchi `sc-if` del template.
- Il **breadcrumb** è derivato dalla rotta tramite una mappa (`crumbMap`).

### Catalogo e menu
- `catalog`: 10 app con `id, name, tagline, glyph, colore, categoria, segmento (B2C/B2B), prezzo mensile/annuale, perUser`.
- `installedIds`: app effettivamente nel workspace (invoices, calendar, mindmaps).
- `menus`: definizione delle sottosezioni che ogni app aggiunge alla sidebar → è ciò che genera il **menu dinamico**.

### Billing
- **Tabella abbonamenti**: ogni riga calcola prezzo = prezzo unitario × posti (per le app B2B *per utente*, es. CRM 8 utenti), rispetta il ciclo mensile/annuale e ha uno **switch** che aggiorna `subStatus`.
- **Statistiche** (totale fatturato, n. abbonamenti attivi, risparmio annuale) si **ricalcolano** in tempo reale sui soli abbonamenti attivi.

### Plans & pricing — due modelli distinti
- **B2C (Mindmaps)** — i tier (`Free`, `Pro`, `Studio`) sbloccano **funzionalità**: ogni tier ha una lista di feature con check (incluse) o trattino (escluse) e l'intestazione "Everything in … plus:".
- **B2B (CRM)** — i tier (`Team`, `Business`, `Enterprise`) scalano sul **numero di utenze** (chip "Up to 5 / 25 / Unlimited users") con prezzo **per utente** decrescente; Enterprise → *Contact sales*.
- La CTA di ogni tier è dinamica (`Current plan` / `Upgrade` / `Downgrade` / `Contact sales`) in base al `planCur`; cliccare un tier lo imposta come corrente.

### Opt-out / cancellazione
- Calcola i **giorni di accesso residui** (`fine periodo − oggi`) e la percentuale del periodo rimasta, mostrandoli in una card con barra di avanzamento.
- Principio chiave: **nessun taglio a metà periodo** — l'accesso resta pieno fino al rinnovo, poi le sezioni spariscono e i dati restano 30 giorni.
- `confirm()` porta allo stato "Cancellation scheduled"; `resume()` annulla; `pause()` propone l'alternativa di sospensione (mantiene i dati a costo zero).

### Tema e accent
- `applyTheme()` scrive le CSS custom properties (`--bg`, `--surface`, `--text`, `--accent`, ombre…) sul nodo radice in base a `theme`/`accent`.
- Esposti anche come **prop del componente** (`theme`, `accent`, `startScreen`) → modificabili dall'esterno senza toccare il codice.

### Onboarding
- Wizard a 4 step con selezione ruolo e scelta delle app iniziali; `Skip` / `Enter workspace` portano alla dashboard.

---

## 4. Come aprirlo
Apri `Appgrove.dc.html` in un browser moderno. Non servono build né dipendenze esterne oltre ai font (Google Fonts) caricati via `<link>`. `support.js` è il runtime del Design Component e va tenuto accanto al file HTML.
