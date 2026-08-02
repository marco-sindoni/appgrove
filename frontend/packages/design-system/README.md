# `@appgrove/design-system` — brand kit e fonte unica dell'aspetto

Un solo posto in cui vivono i valori dell'identità appgrove — colori, tipografia, forme, ombre, logo — e da cui
li leggono **tutti** i prodotti. Se un valore è scritto altrove, è un difetto: è la ragione per cui esiste il
controllo anti-drift descritto più sotto (UC 0019 per il design system, UC 0086 per la fonte unica).

## Dov'è la verità

| Cosa | File | Chi lo legge |
|---|---|---|
| **Token** (colori chiaro/scuro, raggi, ombre, tipografia, colori-categoria) | `src/tokens/tokens.css` | tutti, direttamente o di rimbalzo |
| Mappatura dei token su Tailwind | `tailwind-preset.js` | chi compila CSS |
| Lettura dei token **da un programma** | `src/tokens/tokens.mjs` | chi non compila CSS |
| Caratteri e icone, ospitati in casa | `src/styles/fonts.css` | tutti |
| Disegno del **logo** | `src/brand/logo.mjs` | componente React e generatori |
| **Derivati** del logo (icone, anteprima social) | `src/brand/assets.mjs` + `scripts/brand-assets.mjs` | vetrina e due SPA, come file statici |
| Stile delle **illustrazioni** | [`ILLUSTRAZIONI.md`](ILLUSTRAZIONI.md) | chi produce artwork |
| **Illustrazioni** condivise (React) | `src/illustrations/*` | le due SPA |
| Primitivi React | `src/components/*` | le due SPA |

`tokens.css` è la **sorgente**: tutto il resto ne discende, nulla la duplica. Il preset Tailwind mappa le
variabili, il lettore programmatico legge quel file, i primitivi usano le classi del preset.

## I consumatori

**Chi compila CSS** usa il preset e i token:

```js
// tailwind.config.js
import preset from '@appgrove/design-system/preset'
export default { presets: [preset], content: [/* … */] }
```

```ts
import '@appgrove/design-system/tokens.css'
import '@appgrove/design-system/fonts.css'
```

Sono le due SPA (backoffice e admin) e il sito vetrina Astro, e con esso le landing generate da
`new-application`. Da lì in poi si dipinge **solo** con le classi del preset: `bg-surface`, `text-fg-muted`,
`bg-accent`, `rounded-lg`, `shadow`, `font-mono`… Il tema chiaro/scuro e l'accento cambiano a runtime scrivendo
`data-theme` e `data-accent` sul nodo radice, senza ricompilare nulla.

**Chi non compila CSS** — un generatore di immagini in Node, uno script, un renderer — legge i valori:

```js
import { hex, colorHexes, allBrandHexes } from '@appgrove/design-system/tokens.js'

hex('accent')                       // '#ec5a72'
hex('bg', { theme: 'dark' })        // '#161512'
hex('accent', { accent: 'teal' })   // '#16b6a4'
colorHexes()                        // tutti i colori del tema chiaro
```

È JavaScript puro senza dipendenze: lo importano anche strumenti fuori dal workspace frontend
(`tools/finalize-landing` per l'immagine social, `tools/design-tokens` per il controllo).

**I client di posta** sono il caso limite: non leggono le variabili CSS, quindi in
`shared/email-templates/layout.html` i colori sono necessariamente scritti per esteso. Sono ammessi **perché**
il controllo anti-drift verifica che ognuno corrisponda a un token vero.

## Il logo

Il disegno vive in `src/brand/logo.mjs` e da nessun'altra parte: una **foglia dentro una piastrella ad angoli
morbidi**, costruita con archi di cerchio. Il componente React `<Logo />` ne usa i tracciati; chi ha bisogno di
una stringa SVG (immagine social, icone, anteprime) chiama `logoMarkSvg()` o `logoLockupSvg()` passando i colori
letti dai token.

Il segno ha **tre letture**, non tre disegni (UC 0087):

| Lettura | Quando | Come si ottiene |
|---|---|---|
| completa | dimensioni normali (≥ 22 px) | è il comportamento predefinito |
| compatta | spazi piccoli: icona della scheda, elenchi fitti | automatica sotto la soglia, o `compact: true` |
| monocroma | sfondi difficili: fotografie, aree colorate, stampa a un colore | `mono: '<colore>'` (React: `<Logo mono />`) |

Sotto i 22 px cade la nervatura: a quella dimensione un tratto da 1.7 unità è meno di un pixel e sporca invece
di descrivere. La soglia è dichiarata (`LOGO_COMPACT_BELOW`), non sparsa nel codice.

## I derivati — icone e anteprima social

I browser non vogliono «il disegno», vogliono **un file**. Quei file non si disegnano a mano: sono funzioni del
disegno, generate da uno script.

```bash
cd frontend/packages/design-system && npm run brand:assets
```

| File generato | Dove | A cosa serve |
|---|---|---|
| `favicon.svg` | `site/public/`, `frontend/apps/{backoffice,admin}/public/` | icona della scheda del browser (disegno compatto) |
| `icon.svg` | `site/public/` | icona applicativa quadrata, pensata anche per il ritaglio |
| `apple-touch-icon.png` (180×180) | `site/public/` | icona da schermata iniziale su iOS, che non accetta il vettore |
| `og-appgrove.svg` / `.png` (1200×630) | `site/public/` | anteprima social **di piattaforma** della vetrina |

Gli SVG sono sorvegliati: il collaudo `tools/design-tokens/test/brand-assets.test.mjs` li rigenera e li confronta
con quelli committati. Se l'artwork cambia e nessuno rilancia lo script, la suite diventa **rossa** — è il modo
in cui la fonte unica resta unica. I due PNG richiedono la libreria di rasterizzazione (presente nel sito e in
`tools/finalize-landing`, non fra le dipendenze di questo pacchetto) e sono verificati solo su presenza e
dimensioni.

Le **landing per-app** non usano l'anteprima di piattaforma: hanno la propria, generata da `finalize-landing` col
colore della loro categoria — e anch'essa disegna il mark da qui.

## Le illustrazioni

Le regole di stile stanno in [`ILLUSTRAZIONI.md`](ILLUSTRAZIONI.md); le figure riusabili dalle applicazioni React
stanno in `src/illustrations/` e si usano come qualunque altro componente:

```tsx
import { EmptyIllustration, NotFoundIllustration } from '@appgrove/design-system'

<EmptyIllustration className="mx-auto max-w-[200px]" />
```

Sono decorative (`aria-hidden`), non contengono testo e dipingono **solo** con classi-token: la stessa figura vale
in chiaro e in scuro, senza una seconda versione.

## Anti-drift — perché la fonte unica regge nel tempo

Il rischio non è sbagliare un colore: è **ricopiarlo**. Un valore ricopiato funziona, non rompe niente, e diverge
mesi dopo senza che nessun test diventi rosso — finché qualcuno guarda una pagina e nota che il grigio è freddo
dove il brand è caldo. È successo davvero, prima di UC 0086: l'anteprima social disegnava su un blu-navy
inesistente nella palette e le email usavano la scala di grigi di un'altra libreria.

Il presidio è `tools/design-tokens`, nell'area `tooling` di `./run-tests.sh`: scandaglia i consumatori e
**fallisce** su ogni colore scritto a mano che non corrisponde a un token.

```bash
node tools/design-tokens/check.mjs
```

Se diventa rosso, le riparazioni giuste sono due: **usare il token** che già esiste, oppure — se il colore
appartiene davvero al brand — **aggiungerlo a `tokens.css`**. Dichiararlo fra le eccezioni è l'ultima risorsa e
richiede un motivo scritto.

## Runbook — cambiare un token

1. Modifica il valore in `src/tokens/tokens.css` (e la sua variante scura, se ne ha una).
2. Ricostruisci i consumatori che lo compilano: le due SPA e il sito si ripubblicano ai rispettivi deploy.
3. Verifica **a occhio, in chiaro e in scuro** — lo Storybook del pacchetto (`npm run storybook`) mostra i token
   e i primitivi insieme.
4. Esegui `./run-tests.sh frontend tooling site`: i test dei token, l'anti-drift e la build dei consumatori
   coprono il resto.

Un cambio ai token è trasversale per definizione: tocca ogni prodotto insieme. È un cambio da trattare con
l'attenzione che merita, non una rifinitura.
