# Implementation log — Change 0064 — Epiche evolutive (evo) in-place

**Modalità**: autopilot · **Tipo**: change normale (solo documentazione) · **Branch**: `change/0064-usecases-base-evo-restructure`

## Cosa è stato fatto

Formalizzato il **lavoro evolutivo** (backlog) in **29 storie use case** numerate e descritte nel dettaglio,
raggruppate in **8 epiche** = nuove cartelle-area `12`–`19` sotto `docs/usecases/`, **senza spostare nulla**.

### Requisito rivisto in corso d'opera (gate di approfondimento)

Due bivi sono stati sollevati allo sviluppatore (non decidibili in autopilot):
1. **Numerazione storie** — l'istruzione iniziale ("ripartire da 0055") confliggeva con l'invariante di
   numerazione assoluta/globale unica (gli UC arrivano già a 0060). Scelta: **continuare da 0061** (niente collisione).
2. **Spostamento 01–11 in `base-implementation/`** — avrebbe rotto **278 riferimenti** `usecases/0N-…`. Lo
   sviluppatore ha **rivisto il requisito**: niente cartelle `base-implementation`/`evo`, **nulla si sposta**; le
   epiche diventano cartelle-area numerate da `12` in-place. Zero link rotti.

### Struttura creata

| Cartella-epica | Storie | Fonte backlog |
|---|---|---|
| `12-ready-for-ai-mcp` | 0061–0066 (6) | R2 · _BACKLOG §Ready for AI |
| `13-abbonamenti-self-service` | 0067–0071 (5) | R4/R5/R21 · _BACKLOG §Pagamenti (K50/K51) |
| `14-modello-utenti-multiapp` | 0072–0074 (3) | R3 |
| `15-supporto-e-piattaforma` | 0075–0077 (3) | R6/R7/R11 |
| `16-messa-in-cloud-golive` | 0078–0083 (6) | R12–R16/R18/R10 |
| `17-skill-e-tooling-contenuto` | 0084–0085 (2) | R1/R17 |
| `18-brand-e-design-system` | 0086–0087 (2) | R20 |
| `19-debito-tecnico` | 0088–0089 (2) | R8/R19 |

Numerazione `0061`–`0089` **contigua**, nessun buco/duplicato. Ogni storia col template di casa (header + sezioni
1–10 + "Punti aperti / decisioni differite"). Stesura delegata a 8 sotto-agenti (uno per epica) con brief derivati
dal backlog.

### Indice di esecuzione dell'onda 2

Creato **`docs/usecases/EPICS-WAVE-2.md`** (richiesto dallo sviluppatore): indice di esecuzione delle 29 storie evo, stesso
pattern di `_INDEX.md` (ordinamento **topologico**, esecuzione dall'alto verso il basso). Poiché **tutti i prerequisiti
base (0001–0060) sono già in `main`**, non vincolano l'ordine: l'unico vincolo reale sono le **dipendenze fra storie evo**.
Strategia a parità di vincolo in 5 fasi (A abilitanti · B cloud/go-live · C self-service/supporto/contenuti · D direzione
prodotto da decidere · E bassa priorità). **Verifica a macchina**: 29 righe, ogni "Dip. evo" più in alto, **zero violazioni**;
29 link risolti. Puntatore aggiunto dal `README.md`.

### Indici e riferimenti vivi aggiornati

- **`docs/usecases/README.md`**: nuova sezione "Epiche evolutive (evo)" con le 8 tabelle e i link alle 29 storie;
  nota di numerazione aggiornata.
- **`docs/usecases/_INDEX.md`**: banner di formalizzazione + sezione "Formalizzazione in epiche evo" (mappatura
  residuo→UC). **Tabella di esecuzione topologica INTATTA**; le evo restano fuori dall'ordine finché non maturano.
- **`CLAUDE.md`**: sezione "Casi d'uso" allineata (aree base 01–11 + evo 12–19; 60 base + 29 evo = 89).

### Escluse (già fatte, non ri-formalizzate)

R9 breach (change 0063), `campaign-guide` (0053), accoppiamento app↔core (risolto 0041), Console/Self-service GDPR
(0033/0034).

## Verifica (change solo-documentazione → suite di test non applicabili)

Regola SKILL: una change che tocca solo Markdown/docs non ha test di area. Verifiche sostitutive eseguite, tutte **verdi**:

- `decisions.json` valido (`JSON.parse` OK, 10 voci).
- Numerazione `0061`–`0089` contigua, 29 numeri distinti, nessun duplicato.
- Ogni storia ha le 11 sezioni attese (10 numerate + "Punti aperti").
- Link evo del catalogo `README.md` → **tutti risolvono** a file reali (0 rotti).
- **Nessun link Markdown esterno** introdotto nelle storie (solo riferimenti testuali "UC NNNN" + link fra fratelli).
- Cartelle `01`–`11` **intatte**; **278** riferimenti `usecases/0N-…` nel repo **immutati** (nessun link rotto).

## Punti aperti / rimandi tracciati

- Epiche **12** (Ready for AI) e **14** (modello utenti) e storia **0069** (trial una-tantum): **direzione di
  prodotto non ancora decisa** — marcato nei "Punti aperti" delle storie (che ne restano proprietarie).
- UC 0021 #18 (`@appgrove/app-runtime`): resta tracciato nel suo UC, **non** trasformato in storia (annotato in UC 0089).
- Schedulazione topologica delle evo: deliberatamente **non fatta** (si fa quando maturano — regola di _INDEX.md).
