# Implementation Log — Change 0034: Fedeltà al design di riferimento (backoffice + admin)

**Branch**: `change/0034-fedelta-design-frontend`
**Aree**: frontend
**Completata**: 2026-07-05

## File modificati

| File | Azione |
|---|---|
| `frontend/packages/design-system/src/tokens/tokens.css` | Modificato — tavolozza esatta dei mockup: aggiunti `surface-3`, `border-strong`, `text-faint`; ombre calde; colori funzionali/categoria allineati; raggi 9/11/18/20 |
| `frontend/packages/design-system/tailwind-preset.js` | Modificato — mapping dei nuovi token (`surface-3`, `line-strong`, `fg-faint`) |
| `frontend/packages/design-system/src/styles/fonts.css` | Modificato — pesi font aggiunti (Plus Jakarta Sans 500/700, JetBrains Mono 700/800) |
| `frontend/packages/design-system/src/components/{Button,Badge,Card,Input,Switch,SegmentedControl}.tsx` | Modificato — metriche del mockup (bottoni 700 + ombra accent, badge 7px/11.5px con tinta 15% e `withDot`, card 18px/22px, input su `surface-2`, switch verde, segmented su `surface-3`) |
| `frontend/packages/design-system/src/components/Table.tsx` (+test, +story) | Creato — primitivo tabella-card del mockup (header 11px maiuscolo spaziato, celle 13px, hover, bordi) |
| `frontend/packages/design-system/src/components/PageHeader.tsx` (+test, +story) | Creato — header di pagina (27px/800, variante con riquadro icona 44px, sottotitolo, azioni) |
| `frontend/packages/design-system/src/theme/theme.ts` + `index.ts` | Modificato — export `ACCENT_COLORS` per i selettori a pallini |
| `frontend/apps/backoffice/src/shell/{Sidebar,Topbar,Breadcrumb,ShellLayout}.tsx` | Modificato — sidebar 266px a due livelli (gruppi app richiudibili, sottovoci indentate con riga verticale e font ridotto, icone piene se attive, brand WORKSPACE, footer Settings + scheda utente), topbar 64px traslucida (pallini accent, tema/notifiche a icona), breadcrumb con chevron senza segmento tecnico `app`, contenuti max 1180px |
| `frontend/apps/backoffice/src/modules/fatture/*` | Modificato — lista fatture su `PageHeader`+`Table` (avatar cliente, importi mono a destra, CTA blu app), dettaglio ritoccato, `StatusBadge` con mappa colori mockup (emessa→ambra), `QuotaBanner` in stile card; creato `CustomerAvatar` |
| `frontend/apps/backoffice/src/pages/**` + `modules/demo` | Modificato — tipografia titoli 27px/800, tabelle con header maiuscolo, spaziature 22px |
| `frontend/apps/admin/src/shell/{Sidebar,Topbar,ShellLayout}.tsx` | Modificato — brand scuro con scudetto accent + PLATFORM ADMIN, nav a gruppi (Platform/Catalog/Revenue/Governance) con icone del mockup, operatore nel footer, topbar 62px con pill PLATFORM nel breadcrumb, contenuti max 1240px |
| `frontend/apps/admin/src/pages/**` | Modificato — Overview con KPI a riquadro icona + valore mono, liste su primitivi `Table` con avatar e badge con puntino, GDPR ristilizzata con titoli sezione visibili; creato `TenantAvatar` |
| `frontend/apps/{admin,backoffice}/tailwind.config.js` | Modificato — **bugfix**: il glob `./node_modules/@appgrove/design-system/dist` non corrispondeva a nulla (pacchetto hoistato in `frontend/node_modules`) → `../../node_modules/...`; prima le classi dei primitivi sopravvivevano solo se presenti per coincidenza nel codice dell'app |
| `docs/usecases/06-frontend/0020-*.md`, `0021-*.md`, `11-apps/0052-*.md` | Modificato — punti aperti: elementi del mockup fuori scope tracciati nei UC proprietari |

## Cosa è stato fatto

Allineamento visivo e strutturale delle due SPA ai mockup di `docs/frontend-design/` (consumer e admin),
lavorando alla radice: token e primitivi in `@appgrove/design-system` (inclusi i nuovi `Table` e
`PageHeader`), poi shell e schermate di entrambe le app. Il menu laterale del backoffice ora replica il
mockup (secondo livello indentato con riga verticale, font ridotto, gruppi richiudibili, icone piene sullo
stato attivo); l'admin ha la struttura a gruppi con brand "PLATFORM ADMIN". Verifica visiva eseguita sullo
stack locale con screenshot su backoffice (dashboard, fatture, billing, tema scuro) e admin (overview,
accounts).

## Decisioni prese

- **Bugfix Tailwind scoperto in corso d'opera** (vedi tabella): senza la correzione del glob, le classi dei
  primitivi del design system non venivano generate nelle app; era il motivo per cui alcuni badge
  renderizzavano come testo nudo.
- Gruppi app della sidebar **espansi di default** (richiudibili col chevron): le sezioni restano
  raggiungibili subito, coerente con i test e2e e con l'usabilità a poche app.
- Il menu utente è stato spostato dalla topbar al **footer della sidebar** (posizione del mockup), con
  Security e logout invariati.
- Stato fattura `issued` → tono **ambra** (nel mockup gli stati "in attesa" sono ambra; prima era accent).

## Invarianti appgrove

Nessuno toccato: change solo presentazionale, nessuna modifica ad auth, query, API o infrastruttura.
Il design system resta la fonte unica di brand & UI per entrambe le app (zero drift).

## Note per il revisore

- **Baseline visive**: il criterio di accettazione sulla ri-registrazione delle baseline Playwright è
  decaduto — **non esistono baseline visive** nel repo (la rete e2e è su ruoli/aria-snapshot, #10 20);
  nessuna baseline da ri-registrare. I nomi accessibili sono stati mantenuti stabili e gli e2e passano
  senza modifiche alle spec.
- **Gate privacy (UC 0031)**: eseguito sul diff finale → *nessun segnale*.
- **Decisioni differite tracciate**: ricerca globale ⌘K, selettore workspace, badge numerici e stato
  PAUSED in sidebar → UC 0020; KPI billing MRR/ARR, filtri/ricerca liste, Invitations/System/Settings,
  modale catalogo, skeleton → UC 0021; schede riassuntive e tab filtro della lista fatture → UC 0052.
- Errore `tsc` preesistente su `frontend/apps/backoffice/e2e/privacy.spec.ts:92` (`.at` con lib ES2021):
  presente identico su `main`, fuori scope; nessun nuovo errore introdotto.
- In sviluppo locale, dopo una rebuild del design system serve riavviare i dev server Vite perché
  Tailwind riscansioni `dist/` (non è osservato dal watcher).

## Test

- **Design system**: aggiornati i test di `Button` (variante danger ora outline) e aggiunti
  `Table.test.tsx` e `PageHeader.test.tsx` (semantica, classi chiave, a11y con axe) → 31 test verdi.
- **Backoffice**: 82 test unitari verdi (nessuna asserzione indebolita); **admin**: 11 verdi.
- **Suite canonica** `./run-tests.sh frontend` (unit + e2e Playwright L2 di backoffice e admin) eseguita
  sullo stato finale: **verde** (backoffice 17 e2e, admin e2e inclusi).

## Stato criteri di accettazione

- [x] Confronto affiancato mockup ↔ app sulle schermate principali (screenshot su stack locale: layout,
      spaziature, tipografia, icone e menu allineati al linguaggio del mockup)
- [x] Menu laterale backoffice a due livelli fedele (indentazione, riga verticale, font ridotto, stato
      attivo — verificato su "Fatture")
- [x] Correzioni condivise in `@appgrove/design-system` (token + primitivi, inclusi Table/PageHeader);
      `npm test` e typecheck verdi, nessuna regressione
- [x] Baseline visive: N/A — non esistono baseline nel repo (motivato sopra); e2e verdi senza modifiche
