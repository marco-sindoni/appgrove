# Requirements — 0058 · Setup business/legale (UC 0001) — avvio, non completamento

**Use case sorgente**: [docs/usecases/01-business-legal/0001-setup-business-legale.md](../../docs/usecases/01-business-legal/0001-setup-business-legale.md)
**Modalità**: classica (autopilot OFF) · **Aree toccate**: solo documentazione (nessun codice)

## Contesto

Lo UC 0001 è per sua natura **extra-codice**: è la sequenza di azioni operative del mondo reale che
sblocca Paddle e la monetizzazione — consulenza commercialista (checklist F1–F9), apertura P.IVA
forfettaria ditta individuale alla prima vendita, domiciliazione/virtual office, registrazione account
Paddle "Individual" con superamento della Domain Review. L'unico artefatto realmente compilabile nel
repository è [content/legal/entity.yaml](../../content/legal/entity.yaml) (identità del titolare), ma
richiede dati legali/fiscali **reali** che oggi non esistono ancora.

## Decisione dello sviluppatore (che delimita questa change)

- **Commercialista e legale saranno interpellati più avanti**: il setup fiscale/legale non è ancora avvenuto.
- **La Domain Review di Paddle è bloccata a monte**: richiede il **sito pubblicato con Privacy Policy e
  Termini** live in HTTPS (precondizione dello UC). Lo sviluppatore **non è ancora pronto** per questo passo.
- Quindi: **si avvia** lo UC 0001 (lavoro reale in corso, non ancora concluso) ma **non lo si completa**.

## Scope di questa change

**Incluso**
1. Portare lo UC 0001 a **🟡 in corso** in [docs/usecases/_INDEX.md](../../docs/usecases/_INDEX.md) e
   **lasciarlo 🟡 anche dopo il merge** — deviazione consapevole dal normale step-04 (che porterebbe a ✅):
   il lavoro operativo del mondo reale non è compiuto, lo UC è genuinamente *in corso*.
2. Tracciare nei **"Punti aperti"** dello UC 0001 il motivo dell'avvio-senza-chiusura e il prerequisito
   bloccante (sito pubblicato con Privacy Policy e Termini prima di qualunque uso del vero Paddle).
3. Artefatti di change: questo `requirements.md`, `decisions.json`, `implementation-log.md`.

**Escluso (deliberatamente, con owner)**
- Compilazione di `content/legal/entity.yaml` con i dati reali del titolare (ragione sociale, forma,
  sede, P.IVA) → resta **owner UC 0001**, si concretizza alla monetizzazione. I segnaposto `DA COMPILARE`
  restano; il check compliance li segnala come **avviso non bloccante**.
- Qualsiasi azione del mondo reale (commercialista, P.IVA, domiciliazione, registrazione Paddle,
  Domain Review) — non eseguibile da un agente.
- Scrittura dei segreti Paddle/L3 (Secrets Manager, segreti GitHub) → owner UC 0001, alla creazione
  dell'account.

## Requisiti di test

Nessuno. La change tocca **solo Markdown** (indice use case + tracciamento): `run-tests.sh` non applicabile
(vedi CLAUDE.md, "Esecuzione dei test"). Verifica manuale: `decisions.json` valido, riga UC 0001 = 🟡.

## Definition of Done

1. Riga UC 0001 in `_INDEX.md` = 🟡.
2. "Punti aperti" dello UC 0001 aggiornati con l'avvio (change 0058) e il blocco (sito + legali → Paddle).
3. `decisions.json` valido e coerente con l'`implementation-log.md`.
4. Commit + merge + push su `main` (autorizzati esplicitamente dallo sviluppatore).
