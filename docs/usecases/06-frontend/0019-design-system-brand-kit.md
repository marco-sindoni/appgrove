# UC 0019 — Design system & brand kit (token dai mockup, light/dark, Material Symbols, font)

**Area**: 06-frontend · **Fase**: 0 · **Stato**: 🟢 deciso
**Dipendenze**: — (consumato da UC 0020 backoffice, UC 0036 vetrina, landing generate)
**Fonte decisioni**: #03 (stack/design system), #14 F (brand & identità visiva)
**Ultimo aggiornamento**: 2026-06-21
**Aree collegate**: [03-frontend](../../03-frontend.md), [14-sito-vetrina-legale](../../14-sito-vetrina-legale.md), [frontend-design](../../frontend-design/)

## 1. Obiettivo / Scope
Definire il **brand kit / design system** come **pacchetto di token condiviso nel monorepo**, **fonte unica** consumata da
backoffice SPA (#03), admin SPA (#03), **sito vetrina Astro** (UC 0036) e **landing generate** da `new-application` (#14 F3).
**Incluso**: token estratti dai **mockup Claude Design** (`docs/frontend-design/`) — palette accent corallo **`#ec5a72`** +
neutri caldi + colori-categoria; tema **light/dark**; tipografia **Plus Jakarta Sans** + **JetBrains Mono**; icone
**Material Symbols Rounded**; logo (foglia `eco` in quadrato ad angoli morbidi + wordmark); radii/ombre/spacing; nota stile illustrazioni.
**Escluso**: l'implementazione dei componenti applicativi (UC 0020/0021), l'artwork finale del logo (task di produzione, anche AI).

## 2. Attori & ruoli
- **Developer / designer**: definisce e mantiene i token.
- **Consumatori**: le 2 SPA + il sito Astro + le landing (stessi token → estetica identica senza duplicazione, #03 14, #14 F3).
- **`new-application`** (UC 0046): usa i token per generare landing on-brand e assegnare icona+colore-categoria per-app.

## 3. Precondizioni
- Mockup di riferimento in `docs/frontend-design/` (v1 backoffice + admin/v1, token coerenti, #14 F2).
- Monorepo frontend con workspace (`packages/design-system`, #03 14).

## 4. Flusso principale
1. Estrarre i **token** dai mockup → variabili CSS (custom properties) per colori/temi/raggi/ombre (#03 4).
2. Mappare i token nel **theme Tailwind** + componenti propri (shadcn/Radix headless, no look marcato) (#03 4).
3. Definire **light/dark** + colori-categoria per-app (green/amber/red/blue/violet/teal, #14 F2).
4. Includere **logo** (light/dark), **type scale** (Plus Jakarta Sans/JetBrains Mono), **icone** Material Symbols.
5. Pubblicare il tutto come **package del monorepo** importato dalle 2 SPA (build-time) e usato come base dei token Astro (UC 0036).

## 5. Flussi alternativi / edge / errori
- **Accent configurabile**: corallo default + violet/teal/blue selezionabili (#03 4) — il cambio tema/accent è istantaneo (CSS vars).
- **Tema scuro**: ogni token ha la variante dark; i componenti non hardcodano colori.
- **Drift tra app**: evitato dal package unico (una sola fonte di token); un cambio al design-system → ripubblica **entrambe** le SPA (#07 B3).
- **Per-app**: ogni app riceve **icona (Material Symbol) + colore-categoria** assegnati da `new-application` (#14 F3).

## 6. Schermate & stati
**Showcase / storybook leggero** dei token e dei primitivi in **light e dark**: palette (accent + neutri + colori-categoria),
type scale, icone, bottoni/input/card/table, stati (loading/empty/error/success), logo light/dark. Serve da riferimento
visivo e da superficie per i **component test** (#10 E) e gli **a11y check** (axe, #10 39).

## 7. Dati toccati
Nessun dato applicativo/personale: token, asset (logo, font), componenti. Manifest GDPR N/A.

## 8. Permessi & gate
- **Invarianti multi-tenancy**: N/A (è presentazione). I componenti non leggono `tenant_id` né dati: ricevono props/contesto
  dalla shell (contratto #01 11). Nessun gate.

## 9. Requisiti di test
- **Component test** (Vitest + RTL) mirati sul design-system (più riuso → più copertura, #10 E17).
- **Accessibilità**: `jest-axe`/axe sui componenti (Radix accessibile by-design, #10 39); aria-snapshot come rete primaria (#10 20).
- **Visual**: poche pixel-snapshot tolleranti su componenti chiave, baseline generate in CI (#10 20).

## 10. Riferimenti & Definition of Done
- **Decisioni**: #03 4/14/17, #14 F1/F2/F3.
- **DoD**:
  1. Esiste `packages/design-system` con token (light/dark, accent corallo + colori-categoria), tipografia, icone, logo.
  2. Backoffice, admin e sito Astro consumano gli **stessi** token (zero drift).
  3. Showcase + component/a11y test verdi.
  4. `new-application` può assegnare icona + colore-categoria per-app dai token condivisi.
