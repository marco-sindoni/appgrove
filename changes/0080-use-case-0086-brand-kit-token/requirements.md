# 0080 — Brand kit / token condiviso: fonte unica (UC 0086)

**Use case sorgente**: [docs/usecases/18-brand-e-design-system/0086-brand-kit-token-condiviso.md](../../docs/usecases/18-brand-e-design-system/0086-brand-kit-token-condiviso.md)
**Modalità**: fast (autopilot senza gate di workflow) · **Aree**: `frontend/packages/design-system`, `tools/`, `shared/email-templates`, `run-tests.sh`, docs

## Contesto — cosa esiste già e qual è il divario reale

Il pacchetto condiviso `frontend/packages/design-system` **esiste già** (UC 0019) ed espone token CSS
(`tokens.css`), preset Tailwind (`tailwind-preset.js`), font self-hosted (`fonts.css`) e primitivi React. I tre
consumatori che compilano CSS — SPA backoffice, SPA admin, sito vetrina Astro (e con esso le landing generate,
che vivono dentro il sito) — **usano già** il preset: nelle loro sorgenti non c'è un solo colore scritto a mano.

Il divario che questa change chiude è un altro, ed è quello che l'use case chiama *rischio di divergenza*:

1. **I token non sono leggibili da un programma.** Vivono solo come CSS, cioè solo per chi compila CSS. Ogni
   consumatore **fuori dal mondo CSS** è costretto a ricopiare i valori a mano — ed è esattamente quello che è
   successo:
   - `tools/finalize-landing/lib/branding.mjs` ricopia i sei colori-categoria in esadecimale, con un commento
     che ammette la duplicazione («duplicare qualche costante stabile è la scelta meno rischiosa»);
   - `tools/finalize-landing/lib/og-image.mjs` dichiara di usare «i token del design system» ma disegna
     l'immagine social su un fondo **blu-navy freddo** (`#0b1020`, `#141a30`, `#c7cfe2`) che nella palette
     appgrove non esiste: i neutri del brand sono **caldi**. Drift silenzioso, visibile a chiunque condivida un
     link su un social;
   - `shared/email-templates/layout.html` usa neutri **grigi freddi** (`#f4f4f5`, `#e4e4e7`, `#18181b`,
     `#3f3f46`, `#71717a` — la scala *zinc* di Tailwind, non quella di appgrove) e il corallo `#ec5a72`
     ricopiato a mano;
   - `frontend/packages/design-system/src/theme/theme.ts` ricopia gli esadecimali dei quattro accent per i
     pallini del selettore colore: oggi coincidono, ma nulla lo garantisce domani.
2. **Il logo non è consumabile fuori da React.** Esiste solo come componente `Logo.tsx`: il generatore
   dell'immagine social, un favicon o un'email non possono usarlo.
3. **Manca la nota di stile delle illustrazioni**, che il DoD dello use case vuole nel pacchetto come
   riferimento per UC 0087 (artwork).
4. **Nessun presidio contro il ritorno del drift**: oggi un colore scritto a mano non fa diventare rosso nulla.

## Requisiti

### R1 — Lettura programmatica dei token (fonte unica anche fuori dal CSS)
Il pacchetto espone i token in forma **leggibile da un programma** (Node puro, nessuna dipendenza), senza
introdurre una seconda copia dei valori: il modulo **legge `tokens.css`**, che resta l'unica sorgente. Espone i
valori come terne RGB e come esadecimali, per tema (chiaro/scuro) e per accento. Nuovo export del pacchetto
`@appgrove/design-system/tokens.js`.

### R2 — Il logo diventa consumabile da chiunque
Il disegno del mark vive in **un solo punto**, in forma pura (nessun React), e produce l'SVG del logo nelle
varianti chiara e scura a partire dai token. `Logo.tsx` consuma quello stesso disegno invece di ridefinirlo.
UC 0087 sostituirà l'artwork toccando quell'unico file.

### R3 — Drift sanato nei consumatori non-CSS
- `finalize-landing` legge i colori-categoria dal pacchetto invece di ricopiarli;
- l'immagine Open Graph è ridisegnata sui **neutri caldi** del brand e usa il logo del pacchetto;
- il layout email adotta i neutri caldi e il corallo veri. I client di posta non supportano le variabili CSS,
  quindi i valori restano necessariamente "cotti" nell'HTML: sono ammessi **solo** se corrispondono a un token
  reale, e il controllo di R4 lo verifica;
- `theme.ts` mantiene gli esadecimali degli accent (il browser non può leggere il filesystem), ma un test
  verifica che coincidano con `tokens.css`.

### R4 — Controllo anti-drift automatico
Nuovo strumento `tools/design-tokens` nell'area `tooling` di `run-tests.sh`: scandaglia i consumatori dichiarati
e **fallisce** se trova un colore scritto a mano che non corrisponde a nessun token del pacchetto. Le eccezioni
legittime si dichiarano con un motivo scritto. Esclusi i mockup sorgente (`docs/frontend-design/`) e l'archivio
delle change.

### R5 — Nota di stile delle illustrazioni + documentazione della fonte unica
Nel pacchetto: la nota di stile delle illustrazioni (riferimento per UC 0087) e un README che spiega cos'è la
fonte unica, chi la consuma e qual è il runbook di aggiornamento di un token.

## Fuori ambito (tracciato, non implementato)
- **Artwork definitivo** del logo e produzione delle illustrazioni → UC 0087.
- **Componenti applicativi** delle SPA: restano nei rispettivi use case, qui solo consumano.
- **Versionamento del pacchetto**: resta il punto aperto già annotato nello use case.

## Requisiti di test
- Test del lettore dei token (valori attesi per chiaro/scuro/accenti, coerenza con `tokens.css`).
- Test che `ACCENT_COLORS` di `theme.ts` coincida con i token (anti-drift del duplicato necessario).
- Test del generatore del logo (varianti chiara/scura, SVG ben formato) e non-regressione di `Logo.tsx`.
- Test dello strumento anti-drift su cartelle di prova (deve trovare il drift finto e assolvere il pulito) +
  esecuzione del controllo sul repository vero.
- Test aggiornati di `finalize-landing` (colori-categoria letti dal pacchetto, immagine social on-brand).
- Suite completa `./run-tests.sh` verde prima del commit.

### Copertura end-to-end
Nessuna nuova superficie di navigazione: la change non aggiunge pagine né flussi utente, cambia i valori con cui
le superfici esistenti sono dipinte. Il registro `docs/testing/copertura-e2e.yaml` non cambia; la decisione è
registrata in `decisions.json`.
