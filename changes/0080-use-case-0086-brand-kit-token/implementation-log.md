# 0080 — Log di implementazione (UC 0086, modalità fast)

## Cosa è cambiato, in breve

Il pacchetto condiviso esisteva già (UC 0019) e i tre consumatori che compilano CSS lo usavano già davvero. La
change ha chiuso il **divario residuo**, che è quello che l'use case chiama rischio di divergenza: i token non
erano leggibili da un programma, quindi chi non compila CSS li ricopiava a mano — e aveva già cominciato a
sbagliarli.

## Il drift trovato (accertato, non supposto)

Il controllo scritto in questa change, al primo giro sul repository, ha segnalato **12 colori fuori palette**:

- `shared/email-templates/layout.html` — 9 occorrenze: i neutri erano la scala di grigi **freddi** di un'altra
  libreria (`#f4f4f5`, `#e4e4e7`, `#18181b`, `#3f3f46`, `#71717a`), mentre i neutri appgrove sono **caldi**;
- `tools/finalize-landing/lib/og-image.mjs` — 3 occorrenze: l'anteprima social disegnava su un fondo
  **blu-navy** (`#0b1020` → `#141a30`, testo `#c7cfe2`) inesistente nella palette, mentre un commento del file
  dichiarava di usare «i token del design system».

In più, due duplicazioni che il controllo non poteva vedere perché i valori erano *giusti*: i sei colori-categoria
ricopiati in `branding.mjs` e gli esadecimali degli accenti in `theme.ts`.

## Cosa è stato fatto

1. **Token leggibili da un programma** — `src/tokens/tokens.mjs` legge `tokens.css` (che resta l'unica sorgente)
   e ne espone i valori per tema e accento. Nessun valore ridichiarato: non può divergere per costruzione.
   Nuovo export `@appgrove/design-system/tokens.js`.
2. **Logo in un posto solo** — il disegno esce da `Logo.tsx` e va in `src/brand/logo.mjs`, JavaScript puro che
   produce l'SVG a partire dai colori passati dal chiamante. Il componente React ne consuma i tracciati.
3. **Drift sanato** — email sui neutri caldi veri (raggi inclusi), anteprima social ridisegnata sui neutri scuri
   e caldi del brand e completata col mark del logo condiviso, `branding.mjs` che legge i colori-categoria
   invece di ricopiarli.
4. **Presidio anti-drift** — `tools/design-tokens`, nell'area `tooling` di `run-tests.sh`: fallisce su ogni
   colore scritto a mano che non sia un token. Le due duplicazioni inevitabili (accenti in `theme.ts` per il
   browser, colori nelle email per i client di posta) sono ora **sorvegliate** invece che tollerate.
5. **Documentazione** — `ILLUSTRAZIONI.md` (nota di stile, riferimento per UC 0087) e `README.md` del pacchetto
   (dov'è la verità, chi la consuma, runbook di aggiornamento di un token).

## Test

Nuovi: 8 test del lettore dei token + anti-drift di `ACCENT_COLORS`, 5 test del logo condiviso, 10 test del
rilevatore anti-drift su cartelle di prova, 4 test dell'anteprima social on-brand. Il rilevatore è collaudato
nei due versi: deve trovare il colore inventato **e** assolvere quello del brand.

**Suite completa `./run-tests.sh`: verde su tutte e 8 le aree** (backend, frontend, infra, compliance, tooling,
smoke, platform, site).

## Copertura end-to-end

Nessun impatto: lo use case è già classificato `senza-superficie` nel registro e la classificazione resta vera —
la change non aggiunge percorsi utente, cambia i valori con cui le superfici esistenti sono dipinte. Vedi
decisione 12.

## Lasciato indietro (tracciato)

Artwork definitivo e illustrazioni → UC 0087, che trova il brand kit pronto (disegno isolato in un file solo,
nota di stile scritta). File `.svg` statici del logo su disco e versionamento del pacchetto → annotati fra i
punti aperti dello use case 0086. Vedi decisione 13.
