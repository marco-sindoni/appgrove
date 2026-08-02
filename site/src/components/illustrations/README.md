# Illustrazioni del sito vetrina

> **La nota di stile autorevole è ora nel brand kit**:
> [`frontend/packages/design-system/ILLUSTRAZIONI.md`](../../../../frontend/packages/design-system/ILLUSTRAZIONI.md)
> (UC 0086/0087). Questo documento resta la guida pratica *del sito*; le figure riusabili anche dalle
> applicazioni web (stati vuoti, pagina non trovata) vivono invece nel pacchetto condiviso, in formato React.

Illustrazioni vettoriali **on-brand** del sito (introdotte nella change `0048-use-case-0038-…`, UC 0037).
**Direttiva dello sviluppatore**: usare **questo stesso stile** per arricchire le **pagine future** del sito quando
sono "solo testo". (Il template landing per-app, UC 0038, resta invece **screenshot-first**: lì l'immagine primaria è
lo screenshot dell'app, niente illustrazioni decorative che gli fanno concorrenza.)

## Lo stile in breve

- **SVG inline** in componenti Astro (`*.astro`), un componente per illustrazione.
- **Colori dai token del design system** (UC 0019) via classi Tailwind `fill-*` / `stroke-*`
  (`fill-accent`, `fill-surface`, `fill-surface-2`, `fill-surface-3`, `fill-line`, `fill-accent-contrast`,
  `stroke-accent`, `stroke-line`). Si risolvono in `rgb(var(--ag-*))`, quindi seguono **accento** e **tema
  chiaro/scuro** a runtime, senza JavaScript. **Mai** colori assoluti cablati (`#fff`, `black`).
- **Decorative**: `role="presentation"` + `aria-hidden="true"` (il significato è nel testo accanto).
- **Responsive**: `viewBox` fisso (es. `0 0 480 340`) + classi `h-auto w-full`.
- **Marcatore** `data-illustration="<nome>"` sull'elemento `<svg>`: il controllo post-build
  (`site/scripts/postbuild-check.mjs`) verifica che ogni home localizzata ne porti almeno alcune (rete di regressione).
- Geometria semplice e pulita: pannello di sfondo arrotondato, tessere/finestre `fill-surface` con `stroke-line`,
  accenti in `fill-accent`, glifi neutri in `fill-line`.

## Come aggiungerne una

1. Creare `NomeVisual.astro` copiando la struttura di una esistente (es. `EcosystemVisual.astro`).
2. Usare solo le classi-token elencate sopra; tenere il disegno sui token, non su colori fissi.
3. Importarla nella pagina e collocarla in un layout a due colonne (testo + illustrazione), come in
   `site/src/pages/[lang]/index.astro`.
4. `./run-tests.sh site` deve restare verde.

## Inventario attuale (homepage)

| Componente | Sezione |
|---|---|
| `HeroVisual.astro` | Hero — crescita |
| `AppsVisual.astro` | "Un account, ogni strumento" (griglia app) |
| `EcosystemVisual.astro` | "Un account, tanti strumenti" (hub + satelliti) |
| `AiVisual.astro` | "Ready for AI" (chat ↔ app via MCP) |
| `PrivacyVisual.astro` | Privacy / EU (scudo + stelle) |
