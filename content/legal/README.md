# `content/legal/` — Documenti legali pubblici (UC 0002)

Fonte unica dei documenti legali pubblici del marketplace appgrove, in **5 lingue** (`en`, `it`, `fr`, `es`, `de`).
Gli **stessi** markdown servono sia il sito vetrina (UC 0036) sia il rendering in-app (UC 0056): non duplicare i testi.

> Radice **pubblica** `content/` — distinta da `docs/` (interna). Il RoPA interno (art. 30 GDPR) resta in
> `docs/compliance/`, **non** qui.

## Componenti (un file `<componente>.<lang>.md` per lingua)

| Componente      | Cos'è                                                                 |
|-----------------|----------------------------------------------------------------------|
| `privacy`       | Privacy Policy (art. 13-14 GDPR)                                      |
| `terms`         | Terms & Conditions (erogazione servizio, Paddle Merchant of Record)  |
| `refund`        | Refund Policy (terzo documento richiesto da Paddle)                  |
| `cookie`        | Disclosure cookie (sezione, **nessun banner**)                       |
| `subprocessors` | Lista pubblica dei sub-responsabili (viva)                           |

**IT facente fede**: sui documenti legali, in caso di difformità fra le lingue prevale la versione **italiana**
(clausola esplicita in ciascun testo). Le altre lingue sono traduzioni fedeli.

## Frontmatter (obbligatorio in ogni file)

```yaml
---
version: 1.0.0          # semver: major = ri-accettazione, minor = notifica, patch = refuso
effective_date: 2026-07-25   # YYYY-MM-DD
lang: it                # coerente col suffisso del nome file
---
```

## Identità del titolare — `entity.yaml` (fonte unica) e token

I dati del titolare (nome legale, sede, P.IVA, contatti) **non** sono scritti nei testi: stanno **solo** in
[`entity.yaml`](entity.yaml). Nei documenti compaiono come **token** `{{titolare.<campo>}}`, per esempio:

- `{{titolare.ragione_sociale}}`, `{{titolare.forma}}`, `{{titolare.sede}}`, `{{titolare.piva}}`
- `{{titolare.giurisdizione}}`, `{{titolare.dominio}}`
- `{{titolare.email_privacy}}`, `{{titolare.email_support}}`

Quando l'identità legale sarà disponibile (UC 0001), **modifica solo `entity.yaml`** e ogni documento — più sito e
app in futuro — si aggiorna. I valori nascono come `DA COMPILARE`.

### Contratto di sostituzione (per i renderer futuri — UC 0036 sito, UC 0056 in-app)

Chi renderizza questi markdown **deve** sostituire ogni token `{{titolare.<campo>}}` con il valore corrispondente da
`entity.yaml` **prima** di mostrare il testo. Oggi nessun renderer esiste, quindi i token restano letterali nei file:
è atteso e corretto. Regola di risoluzione: chiave puntata (`titolare.email_privacy`) → valore stringa; token senza
chiave = errore (lo previene già il check CI).

## Controllo di integrazione continua

`tools/compliance` verifica ad ogni build (area `compliance`, gate CI sempre attivo):

1. **Parità lingue**: ogni `component` di `_config.yaml` esiste in tutte le `required_languages` (build rossa se manca).
2. **Frontmatter valido**: `version` semver, `effective_date` data ISO, `lang` coerente col nome file.
3. **Integrità dei token**: ogni `{{titolare.<campo>}}` risolve a una chiave di `entity.yaml` (errore se orfano); i
   valori ancora `DA COMPILARE` sono un **avviso non bloccante** (attesi pre-go-live).

Esecuzione locale: `cd tools/compliance && npm run legal-check` (oppure `./run-tests.sh compliance` dalla radice).
