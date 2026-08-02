# Come collaudare a mano la change 0081 (UC 0087 — artwork logo + illustrazioni)

Questa change consegna **materiale grafico**: il logo definitivo, le sue icone e due illustrazioni. I test
automatici sanno dire che nessun colore è fuori palette e che i file committati corrispondono al disegno —
non sanno dire se il segno è **bello, leggibile e riconoscibile**. Quella parte si fa con gli occhi, ed è la
parte che conta qui.

Il logo attuale sostituisce un **segnaposto**: aspettati che sia diverso da prima. Il compito non è verificare
che nulla sia cambiato, ma che ciò che è cambiato **regga** — a tutte le dimensioni, su entrambi i temi.

Tempo indicativo: 20 minuti di controlli visivi, 5 di controlli non visivi.

---

## Parte 0 — Avvio

**Azione**: dalla radice del repository, `./app-start.sh`. Attendi che l'avvio dichiari tutto sano.

**Risultato atteso**: lo script arriva in fondo senza errori. Da qui in poi servono tre indirizzi:

| Cosa | Indirizzo |
|---|---|
| Backoffice (applicazione cliente) | `https://app.local.appgrove.app` |
| Console di amministrazione | `https://admin.local.appgrove.app` |
| Sito vetrina | `http://localhost:4321` |

---

## Parte 1 — Il logo

### 1.1 Il segno, a dimensione normale

**Azione**: entra nel backoffice e guarda il logo nell'intestazione (o nella barra laterale).

**Risultato atteso**: una **piastrella ad angoli morbidi color corallo** con dentro una **foglia chiara**,
attraversata da una nervatura sottile che parte dalla punta in basso a sinistra e si ferma prima dell'altra
punta. Accanto, la scritta «appgrove». La foglia deve leggersi come una foglia: se ti sembra una lente, un
chicco o un occhio, è un difetto da segnalare.

### 1.2 Chiaro e scuro

**Azione**: usa l'interruttore del tema nella barra superiore, avanti e indietro un paio di volte, tenendo
l'occhio sul logo.

**Risultato atteso**: la piastrella resta corallo in entrambi i temi; la foglia resta **chiara** e ben staccata.
Nessun alone bianco attorno al logo nel tema scuro, nessuna parte che sparisce.

### 1.3 I quattro accenti

**Azione**: cambia il colore d'accento (corallo, violetto, verde-acqua, blu) dai controlli della barra superiore.

**Risultato atteso**: la piastrella cambia colore insieme al resto dell'interfaccia; la foglia resta leggibile su
tutti e quattro. Se su un accento la foglia diventa poco contrastata, va segnalato.

### 1.4 La dimensione piccola — dove cade la nervatura

**Azione**: guarda la **linguetta della scheda del browser** (la favicon) del backoffice, della console admin e
del sito vetrina. Se serve, ingrandisci la finestra o allontanati di mezzo metro dallo schermo.

**Risultato atteso**: si vede una piastrella corallo con una foglia chiara **piena, senza nervatura**. È voluto:
a quella dimensione il tratto sarebbe meno di un pixel. Il segno deve restare riconoscibile e **non impastato**:
se vedi una macchia indistinta, è un difetto.

Controprova utile: apri direttamente `http://localhost:4321/favicon.svg` e riduci la finestra del browser
finché l'immagine è piccolissima — deve reggere.

### 1.5 L'intestazione della vetrina

**Azione**: apri `http://localhost:4321` (verrai portato su una lingua, ad esempio `/it/`).

**Risultato atteso**: in alto a sinistra il **mark accanto alla scritta «appgrove»** — prima c'era solo il testo.
Il mark è allineato in verticale con la scritta, non più alto né più basso, e non «balla» passando da una pagina
all'altra (prova la home, una pagina legale, il blog).

### 1.6 L'icona da schermata iniziale

**Azione**: apri `http://localhost:4321/apple-touch-icon.png` e `http://localhost:4321/icon.svg`.

**Risultato atteso**: un quadrato **pieno di corallo fino ai bordi** con la foglia al centro, con un buon margine
attorno. Il margine serve: su alcuni telefoni l'icona viene ritagliata in tondo, e la foglia non deve essere
tagliata. Immagina un cerchio inscritto nel quadrato — la foglia ci sta comodamente dentro.

### 1.7 La variante monocromatica (facoltativo, per chi tocca il codice)

**Azione**: in un punto qualunque dell'interfaccia, usa temporaneamente `<Logo mono />` al posto di `<Logo />`.

**Risultato atteso**: il segno diventa **di un colore solo**, quello del testo circostante: la piastrella si
svuota diventando un contorno e la foglia resta piena. Serve per fotografie e sfondi colorati. Ricorda di
annullare la modifica.

---

## Parte 2 — Le illustrazioni

### 2.1 Pagina non trovata (backoffice)

**Azione**: con la sessione aperta, vai a un indirizzo inesistente, ad esempio
`https://app.local.appgrove.app/questa-pagina-non-esiste`.

**Risultato atteso**: sopra il «404» compare una figura: una **griglia di tessere arrotondate** con un posto
vuoto in alto a destra, segnato da un **contorno tratteggiato corallo**. La figura è larga al massimo quanto il
testo sotto e non è enorme.

### 2.2 La stessa figura nella console admin

**Azione**: ripeti su `https://admin.local.appgrove.app/pagina-che-non-esiste`.

**Risultato atteso**: identica figura, stessa collocazione. Le due applicazioni devono sembrare la stessa
famiglia.

### 2.3 Stato vuoto del catalogo app

**Azione**: nel backoffice apri la pagina del **catalogo delle app** e scrivi nella ricerca qualcosa che non
esiste, per esempio `zzzz`.

**Risultato atteso**: al posto delle schede compare una figura — un **contenitore aperto** in alto, con una
**tessera corallo che scende dentro** e tre trattini di caduta — sopra il testo «nessun risultato» e il pulsante
per azzerare la ricerca. La figura è centrata e non spinge il pulsante fuori dalla scheda.

### 2.4 Le figure in tema scuro

**Azione**: con lo stato vuoto ancora a schermo (e poi con la pagina «non trovato»), passa al **tema scuro**.

**Risultato atteso**: le figure restano visibili e con lo stesso peso: il pannello di fondo si scurisce, i
contorni restano percepibili, la parte corallo resta l'unico punto vivo. **Difetto tipico da cercare**: una forma
che sparisce perché diventa dello stesso colore del fondo. Se una figura si «buca», va segnalata.

### 2.5 Coerenza con le figure della vetrina

**Azione**: apri la home della vetrina e scorri fino alle illustrazioni delle sezioni; poi torna alle due figure
delle applicazioni.

**Risultato atteso**: devono sembrare **della stessa mano** — stesso tratto tondo, stessi angoli morbidi, stesso
uso parsimonioso del corallo su fondo neutro. Non stesso soggetto: stessa grafia.

---

## Parte 3 — Controlli non visivi

### 3.1 Anteprima social della vetrina

**Azione**: apri `http://localhost:4321/og-appgrove.png` (l'immagine vera), poi guarda il sorgente di una pagina
della vetrina (`Ctrl/Cmd+U`) e cerca `og:image`.

**Risultato atteso**: l'immagine è un rettangolo largo **scuro nei neutri caldi del marchio**, col mark e la
scritta «appgrove» a sinistra e la riga `micro-SaaS · all-EU · GDPR-first` sotto; nessun testo in italiano o in
altre lingue, perché la stessa immagine serve tutte e cinque. Nel sorgente, `og:image` punta a
`/og-appgrove.png` e `twitter:card` vale `summary_large_image`.

### 3.2 Le landing per-app mantengono la loro

**Azione**: apri il sorgente di una pagina landing di un'app (per esempio `/it/fatture/`) e cerca `og:image`.

**Risultato atteso**: punta all'immagine **dell'app** (`/landings/fatture/og.png`), non a quella di piattaforma.
Se una landing mostrasse l'anteprima generica, è un difetto.

### 3.3 Le icone sono davvero servite

**Azione**: nel sorgente di una pagina della vetrina cerca `rel="icon"` e `rel="apple-touch-icon"`; poi fai lo
stesso sul backoffice e sulla console admin (lì basta `rel="icon"`).

**Risultato atteso**: i riferimenti ci sono e i file rispondono (nessun 404 nella scheda «rete» degli strumenti
per sviluppatori).

### 3.4 Rigenerazione degli artefatti (per chi tocca l'artwork)

**Azione**:

```bash
cd frontend/packages/design-system && npm run brand:assets && git status --short
```

**Risultato atteso**: lo script scrive i sette artefatti e `git status` resta **pulito** — segno che i file
committati sono esattamente quelli che il disegno produce. Se compaiono modifiche senza che tu abbia toccato
`logo.mjs`, qualcosa è fuori sincronia.

Controprova del presidio: cambia di proposito un numero nel tracciato della foglia in
`frontend/packages/design-system/src/brand/logo.mjs`, esegui `./run-tests.sh tooling` e verifica che diventi
**rosso** con il messaggio che invita a rilanciare `npm run brand:assets`. Poi annulla la modifica
(`git checkout -- .`).

---

## Cosa NON è in collaudo qui

- Il comportamento delle pagine (navigazione, ricerca, autenticazione): questa change non tocca la logica.
- L'aspetto delle email e delle landing generate: usano già il brand kit e non sono state modificate, se non per
  il fatto che il mark che disegnano è ora quello definitivo — controllabile en passant su una landing.
