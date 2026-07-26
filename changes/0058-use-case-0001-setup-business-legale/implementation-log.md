# Implementation log — 0058 · Setup business/legale (UC 0001)

## Esito

Change **solo-documentazione**: avvia lo UC 0001 senza completarlo (il setup è extra-codice e non ancora
avvenuto). Nessuna riga di codice, nessuna azione del mondo reale.

## Cosa è stato fatto

1. **[docs/usecases/_INDEX.md](../../docs/usecases/_INDEX.md)** — riga UC 0001: `⬜` → `🟡`.
   Lo stato **resta 🟡 anche dopo il merge** (non passa a ✅): deviazione consapevole dallo step-04, perché il
   lavoro operativo reale non è compiuto (vedi `decisions.json` #4).
2. **[docs/usecases/01-business-legal/0001-setup-business-legale.md](../../docs/usecases/01-business-legal/0001-setup-business-legale.md)**
   — sezione "Punti aperti": nota di avvio-senza-chiusura, prerequisito bloccante (sito con Privacy Policy e
   Termini → Paddle), elenco delle azioni per chiudere a ✅, avviso al futuro agente di non correggere lo stato.
3. Artefatti di change: `requirements.md`, `decisions.json` (6 voci), questo log.

## Cosa NON è stato fatto (deliberatamente, con owner)

- **`content/legal/entity.yaml`**: lasciato con i segnaposto `DA COMPILARE`. I dati reali del titolare non
  esistono ancora e non si inventano (dati legali/fiscali reali → escalation). Owner: UC 0001, alla monetizzazione.
- Azioni reali (commercialista F1–F9, P.IVA, domiciliazione, registrazione Paddle, Domain Review): non
  eseguibili da un agente. Owner: sviluppatore/titolare.
- Segreti Paddle/L3 (Secrets Manager, segreti GitHub): owner UC 0001, alla creazione dell'account.

## Test

**Non applicabile**: la change tocca solo Markdown (nessun codice eseguibile) — vedi CLAUDE.md "Esecuzione dei
test" e la regola stack-aware della skill. Verifica manuale: `decisions.json` valida come JSON; riga UC 0001 = 🟡.

## Gate

- Requisiti: riletti e approvati dallo sviluppatore (scope dettato da lui: "metti in giallo lo UC 0001, per ora
  finiamo così").
- Commit / merge / push: autorizzati esplicitamente ("commit merge e push").
