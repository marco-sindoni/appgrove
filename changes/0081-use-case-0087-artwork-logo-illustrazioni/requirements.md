# 0081 — Artwork logo finale + illustrazioni on-brand (UC 0087)

**Use case sorgente**: [docs/usecases/18-brand-e-design-system/0087-artwork-logo-e-illustrazioni.md](../../docs/usecases/18-brand-e-design-system/0087-artwork-logo-e-illustrazioni.md)
**Prerequisito**: change `0080` (UC 0086 — pacchetto brand kit / token condiviso), già su `main`.
**Modalità**: fast (autopilot senza gate di workflow, dichiarata dallo sviluppatore all'invocazione).
**Aree toccate**: `frontend/` (pacchetto design system + due SPA), `site/` (vetrina Astro), `tooling` (controlli).

## 1. Perché

La change 0080 ha isolato il **disegno** del logo in un solo file (`frontend/packages/design-system/src/brand/logo.mjs`)
e ha scritto la **nota di stile delle illustrazioni** (`ILLUSTRAZIONI.md`), lasciando dentro un artwork dichiaratamente
**segnaposto**. Questa change consegna l'artwork **definitivo** e le prime illustrazioni riusabili, sostituendo il
segnaposto nella sua unica sede e portandone i derivati (icona del browser, icona da schermata iniziale, anteprima
social) là dove oggi semplicemente **non esistono**: né la vetrina né le due applicazioni web hanno oggi un'icona nella
scheda del browser, e la vetrina non ha alcuna immagine di anteprima quando un suo indirizzo viene condiviso.

## 2. Cosa si consegna

### 2.1 Logo definitivo (fonte unica)
- Ridisegno dei tracciati in `src/brand/logo.mjs`: **foglia dentro piastrella ad angoli morbidi**, geometria
  costruita su archi di cerchio, nessuno spigolo vivo, colori dichiarati **per ruolo** (`accent` / `contrast`) e
  mai cablati.
- **Due livelli di dettaglio**: disegno completo (foglia + nervatura) per le dimensioni normali, e disegno
  **compatto** (sola foglia) per gli spazi piccoli, dove un tratto sottile sparisce. La scelta è automatica in base
  alla dimensione richiesta.
- **Variante monocromatica** a un solo colore, per gli sfondi difficili (fotografie, aree colorate, stampa a un
  colore).
- Il componente React, il generatore dell'anteprima social per-app e ogni consumatore futuro seguono da soli:
  nessuna copia del disegno da nessuna parte.

### 2.2 Derivati generati dalla sorgente, non disegnati a mano
Uno script del pacchetto (`npm run brand:assets`) produce i file statici a partire dal disegno unico:
- `favicon.svg` per **vetrina**, **backoffice** e **console di amministrazione**;
- `icon.svg` (quadrato pieno, per l'icona applicativa) e `apple-touch-icon.png` 180×180 per la vetrina;
- `og-appgrove.png` 1200×630 — anteprima social **di piattaforma** della vetrina, oggi assente.
Gli SVG generati sono sorvegliati da un test anti-divergenza: se il disegno cambia e i file non vengono rigenerati,
la suite diventa rossa. I due file a griglia di pixel richiedono la libreria di rasterizzazione e non sono ricalcolati
nella suite (verifica ridotta: presenza e dimensioni reali lette dall'intestazione del file).

### 2.3 Cablaggio nei consumatori
- `<link rel="icon">` nelle due applicazioni web e nella pagina base della vetrina; icona da schermata iniziale
  sulla vetrina.
- Anteprima social **predefinita** della vetrina: le pagine che non dichiarano un'immagine propria (homepage, pagine
  legali, blog) usano quella di piattaforma; le landing per-app continuano a usare la loro.
- Logo (mark) accanto al nome nell'intestazione della vetrina, che oggi mostra solo testo.

### 2.4 Illustrazioni on-brand riusabili
- Le cinque illustrazioni della vetrina restano quelle che sono (sono già nello stile). Si aggiungono **due** figure
  nel pacchetto condiviso, in formato React, così che anche le applicazioni web possano usarle: una per gli **stati
  vuoti** e una per la **pagina non trovata**.
- Riquadro di disegno compatto (240×160), colori solo dai token, decorative (`aria-hidden`), stesso tratto e stesso
  impianto delle figure della vetrina.
- Uso reale, non solo vetrina di componenti: pagina «non trovato» del backoffice e della console admin, stato vuoto
  del catalogo delle app.
- La nota di stile (`ILLUSTRAZIONI.md`) viene estesa con le **regole operative** che questa change fissa: riquadri
  ammessi, uso dalle applicazioni React, regola del livello di dettaglio.

## 3. Vincoli

- **Nessun colore fuori palette**: il controllo `tools/design-tokens` deve restare verde senza nuove eccezioni.
- **Mai immagini a griglia di pixel** dove basta il vettore; i due file PNG esistono solo perché i formati di
  destinazione (icona iOS, anteprima social) non accettano SVG.
- **Nessun dato personale**, nessuna superficie multi-tenant toccata: sono materiali presentazionali.
- Nessun testo dentro le illustrazioni (il sito è in cinque lingue).

## 4. Requisiti di test

- Test del pacchetto: il mark contiene tutti i tracciati dichiarati; la variante compatta ne contiene meno; la
  variante monocromatica usa un solo colore; nessun colore cablato oltre a quelli passati dal chiamante.
- Test anti-divergenza sugli asset generati: i file SVG committati coincidono, byte per byte, con la generazione
  dalla sorgente.
- Test dei PNG: esistono e hanno le dimensioni attese (lette dall'intestazione del file, senza rasterizzare).
- Test dei componenti illustrazione: resa, marcatura decorativa, assenza di colori cablati.
- Suite completa `./run-tests.sh` verde prima del commit (modalità fast).
- **Copertura end-to-end**: nessun percorso utente nuovo — le illustrazioni sono decorative e nascoste alle
  tecnologie assistive, il logo è un elemento di cornice. L'esenzione `senza-superficie` di UC 0087 nel registro
  `docs/testing/copertura-e2e.yaml` resta valida e viene solo riformulata; nessuna voce `da-coprire`.

## 5. Definition of Done

1. Artwork definitivo nel solo `logo.mjs`, con variante compatta e monocromatica.
2. Derivati generati e committati, cablati nei tre consumatori; anteprima social di piattaforma attiva.
3. Due illustrazioni condivise, usate in tre punti reali delle applicazioni web.
4. `ILLUSTRAZIONI.md` esteso con le regole operative; `README.md` del pacchetto aggiornato.
5. `docs/usecases/EPICS-WAVE-2.md` → UC 0087 ✅; sezione «Stato dopo l'implementazione» scritta nello use case.
6. `./run-tests.sh` completa verde; `decisions.json`, `implementation-log.md`, `how-to-test.md` presenti.
