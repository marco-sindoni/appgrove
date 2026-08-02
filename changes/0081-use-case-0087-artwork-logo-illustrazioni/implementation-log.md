# 0081 — Log di implementazione

**Use case**: 0087 (artwork logo finale + illustrazioni on-brand) · **Modalità**: fast · **Branch**: `change/0081-use-case-0087-artwork-logo-illustrazioni`
**Registro strutturato delle scelte**: [`decisions.json`](decisions.json) (17 voci) · **Verifica manuale**: [`how-to-test.md`](how-to-test.md)

## Cosa è cambiato

### 1. L'artwork, nella sua unica sede
`frontend/packages/design-system/src/brand/logo.mjs` non ospita più un segnaposto: la foglia dentro la piastrella
ad angoli morbidi è ora costruita con due archi di cerchio simmetrici rispetto alla diagonale, con una nervatura che
parte dalla punta bassa e si ferma ai due terzi. Il raggio degli archi (13) è l'unica leva sulla forma, scelto
confrontando a vista tre valori: 15 assottiglia la foglia fino alla lente, 11.5 la gonfia fino al cerchio.

Lo stesso segno ha **tre letture**, non tre disegni:

| Lettura | Quando | Come |
|---|---|---|
| completa | ≥ 22 px | predefinita |
| compatta (cade la nervatura) | < 22 px | automatica, o `compact: true` |
| monocroma (piastrella a contorno) | sfondi difficili, stampa a un colore | `mono: '<colore>'`, in React `<Logo mono />` |

La soglia dei 22 px è una costante dichiarata (`LOGO_COMPACT_BELOW`): sotto quel valore un tratto da 1.7 unità del
riquadro è meno di un pixel e sporca la foglia invece di descriverla.

### 2. I derivati sono funzioni del disegno
Nuovo `src/brand/assets.mjs` + `npm run brand:assets`: icona della scheda del browser (per vetrina e due applicazioni
web), icona applicativa quadrata, icona da schermata iniziale iOS, **anteprima social di piattaforma** — che prima
non esisteva affatto. Nessun file è disegnato a mano; aggiungere un consumatore è aggiungere una riga a `svgAssets()`.

I due file a griglia di pixel esistono solo perché le destinazioni non accettano il vettore, e sono rasterizzati dalla
stessa sorgente. La libreria di rasterizzazione non è una dipendenza nuova: lo script la cerca dove il monorepo già
ce l'ha (sito, `tools/finalize-landing`).

### 3. Cablaggio nei consumatori
Icone nella testa delle due applicazioni web e della vetrina; icona da schermata iniziale sulla vetrina; anteprima
social predefinita per tutte le pagine che non ne dichiarano una propria (le landing pubblicate mantengono la loro,
le bozze continuano a non averne — i tre casi sono distinti apposta). Nell'intestazione della vetrina il mark
affianca il nome, che prima era solo testo: la vetrina disegna leggendo i tracciati dal pacchetto, non copiandoli.

### 4. Illustrazioni condivise
Due figure sole, in React, dentro il brand kit: stato vuoto e pagina non trovata. Si appoggiano alla cornice comune
`<Illustration>` (pannello di fondo, classi, marcatura decorativa) e al riquadro compatto 240×160, ora fissato nella
nota di stile accanto al 480×340 delle figure di sezione della vetrina. Usate in tre punti reali: pagina «non
trovato» del backoffice e della console admin, stato vuoto della ricerca nel catalogo app. Le cinque figure della
vetrina restano dove sono: erano già in stile.

Una regola nuova è stata scritta perché è stata violata durante il lavoro: **il logo non entra dentro
un'illustrazione**. La prima stesura della figura degli stati vuoti conteneva la foglia ricopiata a mano.

## Test

- **Pacchetto** (`vitest`): artwork — tutti i tracciati disegnati, il dettaglio fine cade solo sotto la soglia,
  la monocromia usa un colore solo, nessun colore cablato oltre a quelli passati dal chiamante. Illustrazioni —
  decoratività, marcatura, assenza di testo, nessun colore cablato nel sorgente, accento usato con parsimonia.
- **`tools/design-tokens`** (area `tooling`): i file vettoriali committati rigenerati e confrontati con la
  generazione; colori dentro la palette; disegno compatto nell'icona della scheda; rapporto 1.91:1 dell'anteprima;
  dimensioni reali dei PNG lette dall'intestazione del file.
- **Corretto un test che ricopiava il disegno**: `tools/finalize-landing/test/og-image.test.mjs` conteneva il
  tracciato del segnaposto come stringa letterale ed è diventato rosso col nuovo artwork — dimostrando il difetto
  che denunciava. Ora chiede i tracciati al pacchetto.
- **Copertura end-to-end**: nessun impatto. UC 0087 resta `senza-superficie` (figure decorative e nascoste alle
  tecnologie assistive, logo di cornice); il motivo è stato riformulato per indicare dove sta il presidio vero.
  Nessuna voce `da-coprire` aperta.

**Esito**: `./run-tests.sh` completa — backend, frontend, infra, compliance, tooling, smoke, platform, site — **tutte verdi**.

## Cosa resta fuori (tracciato)

Nello use case 0087, sezione «Punti aperti / decisioni differite»: manifesto per l'installazione su telefono
(decisione di prodotto, appartiene a uno use case di superficie del backoffice), anteprima social per lingua
(appartiene a UC 0038), terza illustrazione per gli errori di rete/permesso (nessuna schermata la richiede).
Chiuso invece il punto sulla licenza degli strumenti di generazione: gli artwork sono geometria scritta a mano,
senza elementi generati da modelli di immagini né materiale di terzi.
