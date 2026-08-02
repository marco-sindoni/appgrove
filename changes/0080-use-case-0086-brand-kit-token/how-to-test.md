# Come collaudare a mano la change 0080 (UC 0086 — brand kit fonte unica)

Questa change tocca **l'aspetto**, e l'aspetto è la cosa che i test automatici vedono peggio: sanno dire che un
colore è un token del brand, non che la pagina è bella o leggibile. Perciò la parte che conta qui è quella che
si guarda **con i propri occhi**, in tema chiaro e in tema scuro.

Tempo indicativo: 20 minuti per i controlli visivi, 5 per quelli a riga di comando.

---

## Parte 1 — Verifiche visive

### 1.1 Avvia lo stack

**Azione**: dalla radice del repo, `./app-start.sh`, poi apri il backoffice nel browser (l'indirizzo lo stampa
lo script; è la porta servita da Caddy).

**Risultato atteso**: le pagine si aprono normalmente. Questa change **non deve cambiare nulla** nelle due SPA:
è la verifica di non-regressione più importante, perché il componente del logo è stato ricablato.

### 1.2 Il logo non è cambiato

**Azione**: guarda il logo in alto a sinistra nella barra laterale (o nella barra superiore) del backoffice.
Confrontalo, se puoi, con uno screenshot precedente o con `main`.

**Risultato atteso**: **identico a prima** — piastrella con angoli morbidi nel colore d'accento, foglia chiara
dentro, scritta «appgrove» accanto. Il disegno è stato spostato in un altro file, non modificato: se noti una
differenza qualsiasi (foglia storta, tratto mancante, angoli diversi), è un difetto di questa change.

### 1.3 Tema chiaro e scuro

**Azione**: usa l'interruttore del tema nella barra superiore. Passa a scuro e torna a chiaro, guardando il logo
e un paio di schede della pagina.

**Risultato atteso**: il logo resta leggibile in entrambi i temi e cambia insieme al resto dell'interfaccia.
Nessun rettangolo bianco attorno al logo nel tema scuro, nessun contorno che sparisce.

### 1.4 Cambio di accento

**Azione**: cambia il colore d'accento dai pallini nella barra superiore — prova tutti e quattro (corallo,
violetto, verde-acqua, blu).

**Risultato atteso**: il pallino selezionato ha **esattamente** il colore che poi assume l'interfaccia (e il
logo). Se un pallino mostra una tinta e l'interfaccia ne prende un'altra, la copia degli esadecimali in
`theme.ts` è divergente — è la cosa che il nuovo test sorveglia, ma vale la pena vederlo.

### 1.5 L'email — è qui che si vede il cambiamento vero

Le email ora usano i neutri **caldi** del brand invece dei grigi freddi di prima. È il cambiamento visivo più
netto della change.

**Azione**: fai partire un flusso che spedisce un'email reale. La via più comoda è la suite di piattaforma, che
avvia Mailpit (casella di posta finta con interfaccia web):

```bash
tools/platform-e2e/run.sh
```

…oppure, con lo stack locale in piedi, registra un utente nuovo dal backoffice e apri Mailpit nel browser
(l'indirizzo è nel README di `tools/platform-e2e`). Apri l'email di verifica.

**Risultato atteso**, guardando l'email:
- lo sfondo attorno alla scheda è un **beige tenue e caldo**, non un grigio azzurrino;
- il bordo della scheda bianca è un grigio **caldo** appena percettibile;
- il titolo è quasi nero ma **tendente al bruno**, non nero-blu;
- il bottone d'azione è **corallo** (lo stesso corallo dell'accento predefinito dell'interfaccia);
- il testo di corpo, il ripiego («se il bottone non funziona…») e il piè di pagina sono tutti dello stesso
  grigio caldo medio e **si leggono senza sforzo**. Se un testo ti sembra troppo chiaro per essere letto
  comodamente, segnalalo: è il punto su cui la change ha scelto la leggibilità rispetto alla gerarchia a tre
  livelli di prima.

Confronto utile: apri accanto una vecchia email (se ne hai una) — il salto dal freddo al caldo è evidente.

### 1.6 L'anteprima social — il pezzo che nessuno guarda mai

Era l'artefatto più fuori brand di tutti: fondo blu-navy, mai visto nella palette appgrove.

**Azione**: genera l'immagine e aprila.

```bash
cd tools/finalize-landing
node -e "
import('./lib/og-image.mjs').then(async (m) => {
  const png = await m.renderOgImage({ appName: 'Fatture', tagline: 'Fatture a norma, senza pensieri', accent: 'cat-blue' })
  const fs = await import('node:fs')
  fs.writeFileSync('/tmp/og-prova.png', png)
  console.log('scritta /tmp/og-prova.png')
})"
open /tmp/og-prova.png
```

**Risultato atteso**:
- il fondo è **scuro e caldo** (bruno-nerastro), con una sfumatura appena percettibile — **non** blu notte;
- a sinistra c'è una banda verticale nel colore-categoria dell'app;
- in alto compare il **mark del logo** (la piastrella con la foglia) accanto alla scritta «appgrove»: prima
  c'era solo la scritta;
- il titolo dell'app è chiaro e ben leggibile sul fondo, la frase sotto è grigia più tenue ma leggibile;
- in basso «all-EU · GDPR-first» nel colore-categoria.

Riprova cambiando `accent` (`cat-violet`, `cat-green`, `cat-amber`, `cat-red`, `cat-teal`): banda, mark e
scritta in basso devono cambiare colore insieme, il fondo no.

### 1.7 Vetrina e Storybook (facoltativo ma consigliato)

**Azione**: `cd site && npm run dev`, apri la homepage e una pagina di landing. Poi, in un'altra finestra,
`cd frontend && npm run storybook` e apri la storia «Foundations/Overview».

**Risultato atteso**: nulla è cambiato nella vetrina (la change non tocca le sue pagine). Nello Storybook, la
tavola dei colori mostra i neutri caldi, i sei colori-categoria e la tipografia — è il posto giusto per
confrontare a occhio i colori dell'email con quelli veri del brand.

---

## Parte 2 — Verifiche non visive

### 2.1 Il controllo anti-drift è verde sul repository vero

**Azione**:
```bash
node tools/design-tokens/check.mjs
```

**Risultato atteso**: stampa il numero di colori del brand riconosciuti (28), i consumatori sorvegliati (5) e i
file scandagliati, e chiude con «nessuna divergenza». Uscita 0.

### 2.2 Il controllo sa diventare rosso — provalo

Un controllo che non hai mai visto fallire non ti protegge da niente.

**Azione**: introduci un colore inventato in un consumatore, per esempio aggiungi a
`frontend/apps/backoffice/src/main.tsx` una riga `const provaDrift = '#0b1020'`, poi rilancia il controllo.

**Risultato atteso**: uscita 1, con il file, la riga e il colore segnalati, e il messaggio che indica le
riparazioni giuste (usare il token, o aggiungerlo alla fonte unica). **Poi rimuovi la riga** e verifica che
torni verde.

### 2.3 I valori dei token si leggono davvero dal file sorgente

**Azione**:
```bash
node -e "
import('./frontend/packages/design-system/src/tokens/tokens.mjs').then(t => {
  console.log('accento corallo :', t.hex('accent'))
  console.log('sfondo chiaro   :', t.hex('bg'))
  console.log('sfondo scuro    :', t.hex('bg', { theme: 'dark' }))
  console.log('accento teal    :', t.hex('accent', { accent: 'teal' }))
})"
```

**Risultato atteso**: `#ec5a72`, `#f4f4f1`, `#161512`, `#16b6a4`.

**Prova che è davvero la fonte unica**: cambia temporaneamente `--ag-accent` in
`frontend/packages/design-system/src/tokens/tokens.css` (per esempio `0 128 0`), rilancia il comando: deve
stampare `#008000`. **Poi annulla la modifica** (`git checkout -- frontend/packages/design-system/src/tokens/tokens.css`).

### 2.4 Suite completa

**Azione**: `./run-tests.sh`

**Risultato atteso**: riepilogo con tutte e otto le aree verdi. L'area `tooling` ora comprende anche
`design-tokens`.

---

## Cosa NON deve essere cambiato

Da verificare per esclusione, perché sarebbe una regressione:

- l'aspetto delle due SPA (colori, spaziature, tipografia): **identico** a prima;
- l'aspetto della vetrina e delle landing: **identico**;
- il testo delle email (solo i colori e i raggi sono cambiati, non le parole né le traduzioni);
- la dimensione e il rapporto dell'immagine social: sempre 1200×630.
