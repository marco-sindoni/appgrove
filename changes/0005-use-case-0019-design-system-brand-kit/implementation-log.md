# Implementation Log — Change 0005: Design system & brand kit

**Branch**: `change/0005-use-case-0019-design-system-brand-kit`
**Aree**: frontend (`frontend/` workspace + `frontend/packages/design-system`), docs (README + use case)
**Completata**: 2026-06-24

## File modificati

| File | Azione |
|---|---|
| `frontend/package.json`, `frontend/.gitignore`, `frontend/package-lock.json` | Creato (root workspace npm) |
| `frontend/src/{shell,admin,shared,apps}/.gitkeep` | Eliminato (scaffold "una sola SPA", superato) |
| `frontend/packages/design-system/` (package.json, tsconfig, vite.config, vitest.setup, tailwind preset+config, postcss) | Creato (config) |
| `…/src/tokens/tokens.css`, `…/src/styles/fonts.css` | Creato (token RGB light/dark + accent + categorie; font + Material Symbols) |
| `…/src/theme/{theme.ts,ThemeProvider.tsx}` | Creato (applyTheme + provider React) |
| `…/src/components/{Button,Input,Card,Badge,Switch,SegmentedControl,Icon,Logo}.tsx` + `…/src/lib/cn.ts` + `…/src/index.ts` | Creato (primitivi + entry) |
| `…/*.test.tsx` (9 file) | Creato (Vitest + RTL + jest-axe) |
| `…/.storybook/{main.ts,preview.tsx,storybook.css}` + `…/*.stories.tsx` (7) | Creato (Storybook 10 + showcase) |
| `frontend/README.md` | Riscritto (era obsoleto: "una sola SPA") |
| `README.md` (root) | Precisato confine design-system + Astro consumatore esterno |
| `docs/usecases/06-frontend/{0019,0020,0021}-*.md` | Aggiunta sezione "Punti aperti / decisioni differite" |
| `docs/usecases/_INDEX.md` | 0019 → 🟡 (avvio) → ✅ (chiusura) |

## Cosa è stato fatto

Creato il workspace npm `frontend/` e il package **`@appgrove/design-system`** (UC 0019): token come CSS custom
properties (light/dark + accent coral/violet/teal/blue + colori-categoria, in triplette RGB per le opacità), **Tailwind
preset**, tipografia (Plus Jakarta Sans/JetBrains Mono self-hosted), icone (Material Symbols Rounded), logo placeholder, e i
primitivi token-driven (Button, Input, Card, Badge, Switch, SegmentedControl, Icon, Logo) con `ThemeProvider`. Showcase in
Storybook con toolbar tema/accent. Rimosso lo scaffold obsoleto `frontend/src/*`.

## Decisioni prese

- **Colori come triplette RGB** (`rgb(var(--ag-*) / <alpha-value>)`) invece di hex pieni, per supportare le tinte
  trasparenti del mockup (es. `bg-accent/10`).
- **Tailwind 3** (non 4): il preset JS è richiesto come forma di export cross-progetto.
- **Storybook 10** con solo `addon-a11y` (essentials integrato); theming via decorator + toolbar globale, niente addon-themes.
- **Build dei tipi** con `vite-plugin-dts` senza `rollupTypes` (evita la dipendenza `api-extractor`).

## Invarianti appgrove

Nessuno toccato a runtime: il design system è presentazione (UC 0019 §8). I componenti non leggono `tenant_id` né dati,
non emettono log applicativi, non fanno query — ricevono props/contesto. Nessun dato personale.

## Note per il revisore

- **Decisioni differite tracciate** (regola CLAUDE.md): in [0019](../../docs/usecases/06-frontend/0019-design-system-brand-kit.md)
  (table/stati composti non inclusi; posizione Astro → UC 0036; api-client/i18n → UC 0020), in
  [0020](../../docs/usecases/06-frontend/0020-shell-spa-backoffice.md) e
  [0021](../../docs/usecases/06-frontend/0021-console-admin-spa.md) (table/stati composti da comporre nelle SPA).
- **Contratto cross-area**: nessuno attivo ora; il package è progettato per essere consumato da backoffice/admin (UC 0020/0021)
  e dalla vetrina Astro (UC 0036) — export come token CSS + preset Tailwind, non solo componenti React.
- **DoD #2 ("zero drift" tra backoffice/admin/Astro)**: verificabile end-to-end solo quando quelle app esisteranno; qui è
  garantita la **fonte unica** e la forma di export. Baseline visual-snapshot in CI differita (CI = UC 0005).
- `dist/`, `storybook-static/`, `node_modules/` sono gitignored; committato `package-lock.json`.

## Test

Area **frontend** — `cd frontend && npm test` (Vitest): **9 file, 25 test, tutti verdi**. Coprono: render/varianti/eventi
dei primitivi, `aria-invalid`/stati, role `switch`/`radio`, comportamento `SegmentedControl`, theming (attributi
`data-theme`/`data-accent`), e **a11y (jest-axe) senza violazioni** su ogni primitivo. Inoltre verdi: `npm run typecheck`
(tsc), `npm run build` (lib + `index.d.ts`), `npm run build-storybook`.

## Stato criteri di accettazione

- [x] `frontend/` workspace npm con `packages/design-system`; `frontend/src/*` rimosso; `npm install`/`npm test` ok dalla root.
- [x] Package esporta token (CSS vars light/dark + accent + categorie), Tailwind preset, tipografia, icone, logo, primitivi.
- [x] Storybook (token + primitivi, light/dark + accent); component test (RTL) e a11y (axe) verdi.
- [x] Token consumabili cross-progetto (CSS vars + preset) → non preclude Astro (UC 0036).
- [x] `frontend/README.md` allineato; root `README.md` preciso sul confine del package.
