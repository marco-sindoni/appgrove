# Nota di stile — illustrazioni appgrove

Fa parte del brand kit (UC 0086) ed è il **riferimento a cui si è attenuto UC 0087** producendo gli artwork.
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
- **Due riquadri di disegno, non uno a piacere** (fissato da UC 0087):
  - `0 0 480 340` — **figure di sezione** della vetrina: rapporto largo, si accosta bene a una colonna di testo;
  - `0 0 240 160` — **figure compatte** delle applicazioni web: stanno dentro una scheda, uno stato vuoto, una
    pagina di errore. Stesso rapporto 3:2, quindi lo stesso occhio.
  Un terzo riquadro va discusso, non inventato: è così che due figure smettono di sembrare della stessa mano.
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

## Dove vivono le figure

| Consumatore | Dove | Forma |
|---|---|---|
| Sito vetrina (Astro) | `site/src/components/illustrations/*.astro` | componenti Astro, riquadro 480×340 |
| Applicazioni web (React) | `frontend/packages/design-system/src/illustrations/*.tsx` | componenti React esportati dal pacchetto, riquadro 240×160 |

Le figure React si appoggiano tutte alla cornice comune `<Illustration name="…">`, che porta il pannello di
fondo, le classi e la marcatura decorativa: chi disegna una figura nuova scrive **solo il proprio disegno**.
Regola pratica per non moltiplicare: prima di crearne una, guardare se una di quelle che esistono dice già la
stessa cosa. «Poche e coerenti» è una regola, non un auspicio.

## Il logo non è un'illustrazione

Il mark vive in [`src/brand/logo.mjs`](src/brand/logo.mjs), definito una volta sola e consumato sia dal
componente React sia dai generatori (icone, immagine social). Dall'artwork definitivo (UC 0087) è una **foglia
in una piastrella ad angoli morbidi**, con tre letture — completa, compatta, monocroma — descritte nel
[README](README.md).

Corollario operativo: **il logo non entra dentro una figura**. Una tessera generica in accento dice «un'app»
senza pretendere di essere il marchio; il marchio, ricopiato dentro un disegno, diventa una copia che diverge.
