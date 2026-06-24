# Change 0005: Design system & brand kit (`packages/design-system`)

**Branch**: `change/0005-use-case-0019-design-system-brand-kit`
**Aree**: frontend (`frontend/` workspace + `frontend/packages/design-system`), docs (README)
**Data**: 2026-06-24
**Autore**: Platform Engineering
**Use case sorgente**: [docs/usecases/06-frontend/0019-design-system-brand-kit.md](../../docs/usecases/06-frontend/0019-design-system-brand-kit.md)
**Tocca dati personali?**: No — token, asset, componenti di presentazione (UC 0019 §7, manifest GDPR N/A).

## Problema / Obiettivo

Creare la **fonte unica** del brand/design system come **package del monorepo** (`frontend/packages/design-system`),
consumabile da backoffice (UC 0020), admin (UC 0021) e — cross-progetto — dalla vetrina Astro (UC 0036), così che
l'estetica sia identica senza duplicazione. È il **primo pezzo di frontend** del monorepo: fissa anche il tooling
(workspace npm, Vite, Tailwind, test, Storybook).

## Scope

- **Workspace frontend** (#03 dec.14): `frontend/package.json` root privato con npm workspaces `["packages/*","apps/*"]`;
  creare `frontend/packages/design-system`. **Rimuovere** lo scaffold obsoleto `frontend/src/{shell,admin,shared,apps}`
  (visione "una sola SPA", superata da #03 dec.13/14).
- **Token** estratti dal mockup v1 ([docs/frontend-design/v1/](../../docs/frontend-design/v1/)) come **CSS custom
  properties**: palette neutri caldi, accent **coral `#ec5a72`** (default) + violet/teal/blue, tema **light/dark**,
  colori-categoria per-app (green/amber/red/blue/violet/teal), raggi, ombre, spacing, type scale.
- **Tailwind preset** che mappa i token (consumabile anche da progetti non-React → vincolo Astro).
- **Tipografia** Plus Jakarta Sans + JetBrains Mono; **icone** Material Symbols Rounded (wrapper); **logo** light/dark
  (placeholder SVG: foglia `eco` in quadrato ad angoli morbidi + wordmark; artwork finale escluso, UC 0019 §1).
- **Primitivi** token-driven (shadcn/Radix headless, componenti propri): `Button`, `Input`, `Card`, `Badge`,
  `Switch`/Toggle, `SegmentedControl`.
- **Showcase con Storybook** (light/dark, accent switch): palette, type scale, icone, logo, primitivi, e i pattern
  table/stati documentati (come esempi, non come componenti del package).
- **Test**: Vitest + React Testing Library (component) + jest-axe (a11y) sui primitivi e sullo showcase.
- **Docs**: riscrivere `frontend/README.md` (obsoleto: descrive la "singola SPA"); precisare il root `README.md`
  sul confine di `packages/design-system` e sul progetto Astro come consumatore esterno (posizione → UC 0036).

## Fuori scope

- Componenti **applicativi** e composizioni: **table** e **stati composti** (loading/empty/error/success) come
  componenti riusabili → composti nelle SPA (UC 0020/0021), tracciato nei rispettivi use case.
- Package condivisi `api-client` / `i18n` → UC 0020.
- Le SPA reali (UC 0020/0021) e la vetrina Astro (UC 0036): qui solo il package + showcase.
- **Baseline visual-snapshot in CI** (#10 20): la CI è UC 0005 (non esiste). Si predispone la possibilità ma la
  baseline-in-CI è differita; nessun pixel-snapshot bloccante ora.
- Artwork finale del logo (task di produzione, UC 0019 §1).

## Criteri di accettazione

- [ ] `frontend/` è un workspace npm valido con `packages/design-system`; lo scaffold `frontend/src/*` è rimosso;
      `npm install` e `npm test` (Vitest) girano dalla root `frontend/`.
- [ ] Il package esporta **token** (CSS custom properties light/dark + accent coral/violet/teal/blue + colori-categoria),
      **Tailwind preset**, **tipografia**, **icone**, **logo** e i **primitivi** elencati.
- [ ] **Storybook** mostra token e primitivi in light/dark con cambio accent; **component test (RTL)** e **a11y (axe)**
      verdi sui primitivi.
- [ ] I token sono consumabili **cross-progetto** (CSS vars + preset Tailwind, non solo componenti React) → non preclude
      il consumo Astro (UC 0036). [Nota: "zero drift" tra backoffice/admin/Astro è verificabile end-to-end solo quando
      quelle app esistono — qui si garantisce la **fonte unica** e la forma di export.]
- [ ] `frontend/README.md` allineato al workspace; root `README.md` preciso sul confine del package.

## Invarianti appgrove toccati

Nessuno a runtime: il design system è presentazione (UC 0019 §8). I componenti **non** leggono `tenant_id` né dati —
ricevono props/contesto dalla shell (contratto #01 11). Nessun gate multi-tenant, nessuna query, nessun log applicativo.

## Requisiti di test

- **Component** (Vitest + RTL): ogni primitivo renderizza, risponde a props/stati (es. `Switch` on/off,
  `SegmentedControl` selezione), e rispetta i token (light/dark via classe/attributo tema).
- **a11y** (jest-axe): nessuna violazione sui primitivi e sullo showcase; ruoli/aria corretti (Radix accessibile by-design).
- **Tema/accent**: test che cambiando tema/accent le CSS custom properties applicate cambino (cambio istantaneo).

## Valutazione di impatto

| Area | Impatto |
|---|---|
| Breaking change | No (primo frontend; rimuove solo scaffold vuoto) |
| Contratto cross-area | N/A ora (consumatori — SPA/Astro — arriveranno; export pensato per loro) |
| Version bump | minor (nuovo package `@appgrove/design-system` 0.x) |
