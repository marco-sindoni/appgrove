# Step 01 — Tipo di cambio

Tutti i comandi dalla radice del monorepo `/Users/msindoni/Projects/appgrove`.

## Apri dentro `new-change`

Questa skill non inventa un proprio workflow: gira **dentro** `new-change`, da cui eredita i gate
(requisiti, consenso al commit, consenso al merge) e la modalità (classica / autopilot). Avviala dando come
descrizione `pricing <slug> — <cosa cambia in una riga>`.

## Individua il listino

Il pricing-as-code vive in `services/core/src/main/resources/pricing/<slug>.yaml`, un file per app reale
(indice `pricing/index.yaml`). Le app **fixture** (`pricing/fixtures/`) sono solo per dev/test: non si toccano
qui. Leggi il file dell'app e mostra allo sviluppatore lo stato attuale: i tier, i limiti, i prezzi (mensile e
annuale), lo `status` dell'app.

## Stabilisci quale dei quattro cambi

Chiedi, in prosa e una domanda alla volta, **quale** cambio serve (#09 H36):

- **Nuovo tier** — un livello in più (es. un «Pro» sopra il «Team»). Serve chiave stabile, nome, limiti,
  prezzi. → `change.mjs add-tier`.
- **Cambio prezzo** — cambiare l'importo di un prezzo esistente. È il caso delicato: comporta l'immutabilità
  (step-02) e il grandfathering (step-03).
- **Cambio limiti** — cambiare il tetto/metrica di un tier (senza toccare i prezzi → nessun vincolo di
  immutabilità). → `change.mjs set-limits`.
- **Aggiungi ciclo** — affiancare l'annuale al mensile (o viceversa) su un tier. → `change.mjs add-cycle`.
  Ricorda: l'annuale di default è **~10× il mensile** (~17% di sconto, «due mesi in omaggio», #09 K49) e
  **abbatte la fee** (una sola transazione l'anno) — spingilo.

Non anticipare: se emerge un cambio che appartiene a un altro use case (es. un editor di prezzi a runtime — che
**non esiste per scelta**, #09 H34), fermati e traccialo, non allargare lo scope.

## Registra

Il tipo di cambio scelto (e il perché) va in `changes/NNNN-*/decisions.json` appena deciso. Prosegui con
`step-02-apply.md`.
