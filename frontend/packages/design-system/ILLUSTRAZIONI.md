# Nota di stile — illustrazioni appgrove

Fa parte del brand kit (UC 0086) ed è il **riferimento a cui si attiene UC 0087** quando produce gli artwork.
Non descrive un'aspirazione: codifica lo stile **già in uso** nelle illustrazioni del sito vetrina
(`site/src/components/illustrations/*.astro`, UC 0037), così che ogni figura nuova nasca coerente con quelle
esistenti invece di aprire un secondo dialetto visivo.

## Il principio, in una riga

Un'illustrazione appgrove è un **vettore piatto, geometrico e sobrio**, costruito con i colori del design system
e mai con colori propri: deve seguire tema chiaro/scuro e accento **senza essere ridisegnata**.

## Regole

### 1. Sempre SVG scritto a mano, mai immagini raster
Nessun PNG, nessun JPEG, nessuna illustrazione di libreria. Il vettore scala senza sfocarsi, pesa poco e —
soprattutto — i suoi colori possono restare agganciati ai token. Un'immagine raster congela i colori: al primo
cambio di tema è fuori posto.

### 2. I colori vengono dai token, sempre
Si dipinge con le classi che il preset Tailwind deriva dai token: `fill-surface`, `fill-surface-2`,
`fill-surface-3`, `fill-accent`, `stroke-line`, `fill-cat-*`. **Mai** un colore scritto per esteso dentro l'SVG:
il controllo `tools/design-tokens` lo segnala e la suite diventa rossa. Le sfumature si ottengono con
`opacity` sul colore d'accento (valori tipici: `0.16`, `0.35`), non con una tinta inventata.

### 3. Impianto della figura
- **Riquadro di disegno** `0 0 480 340` per le figure di sezione (rapporto largo, si accosta bene a una colonna
  di testo).
- **Pannello di fondo**: rettangolo `rx="20"` in `fill-surface-2` con bordo `stroke-line` a `1.5` — è la cornice
  che dà unità a tutte le figure.
- **Angoli morbidi ovunque**: i raggi seguono la scala del brand (8–20 nelle unità del riquadro). Nessuno
  spigolo vivo.
- **Spessore dei tratti** 1.5–4, con `stroke-linecap="round"` e `stroke-linejoin="round"`: il tratto è tondo,
  mai tagliato di netto.

### 4. Il ruolo dell'accento
L'accento è l'**unico** colore vivo della figura e va usato con parsimonia: guida l'occhio sull'elemento che
conta (la barra più alta, la freccia di crescita, il segno di spunta). Tutto il resto vive sui neutri caldi. Una
figura in cui l'accento è ovunque non ha più un punto focale.

### 5. Tema chiaro e scuro senza doppio lavoro
Poiché i colori sono token, la stessa identica figura funziona in entrambi i temi. Verificare **sempre** la resa
in scuro prima di considerarla finita: gli errori tipici sono un `fill-surface` che sparisce sul fondo scuro e
un'opacità tarata solo per il chiaro.

### 6. Le illustrazioni sono decorative
Sono corredo del testo, non lo sostituiscono. Quindi `role="presentation"` e `aria-hidden="true"`: chi usa un
lettore di schermo non deve sentirsi leggere una figura che ripete ciò che c'è già scritto accanto. Se una
figura porta informazione che nel testo non c'è, il difetto è nel testo.

### 7. Convenzioni di marcatura
Classe `ag-illustration h-auto w-full` e attributo `data-illustration="<nome>"`: servono a riconoscerle e a
verificarle dai controlli automatici.

## Cosa evitare

- Persone disegnate, mascotte, personaggi: appgrove parla di strumenti, non racconta storie.
- Prospettive tridimensionali, ombreggiature elaborate, gradienti vistosi: lo stile è piatto.
- Icone di libreria incollate dentro un'illustrazione — per le icone c'è **Material Symbols Rounded**, che è
  cosa diversa dalle figure.
- Testo dentro l'SVG: non è traducibile e le lingue del sito sono cinque.

## Il logo non è un'illustrazione

Il mark vive in [`src/brand/logo.mjs`](src/brand/logo.mjs), definito una volta sola e consumato sia dal
componente React sia dai generatori (immagine social). Quello attuale è un **segnaposto on-brand**: l'artwork
definitivo è compito di UC 0087, che lo sostituisce intervenendo **solo** su quel file.
