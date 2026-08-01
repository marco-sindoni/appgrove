# Requirements — Change 0074: copertura end-to-end dentro il workflow delle skill

**Use case sorgente**: [UC 0094 — Integrazione della copertura end-to-end nel workflow delle skill](../../docs/usecases/20-test-e2e-piattaforma/0094-e2e-platform-workflow-skill.md)
**Branch**: `change/0074-use-case-0094-copertura-e2e-workflow-skill`
**Modalità**: fast (autopilot senza fermate di workflow; ogni scelta è registrata in [decisions.json](decisions.json))
**Aree previste**: documenti di processo (`CLAUDE.md`, `docs/`), skill (`.claude/skills/`), strumenti Node
(`tools/new-application`, `tools/drop-application`, `tools/scaffold-parity`, `tools/e2e-coverage`)

## 1. Perché

UC 0093 ha consegnato il **registro di copertura** `docs/testing/copertura-e2e.yaml` e il suo controllo meccanico
`tools/e2e-coverage` (area `tooling` di `run-tests.sh`). Il registro oggi è vero, ma **nessuno obbliga una change
o uno use case nuovo a tenerlo vero**: il controllo si accorge di ciò che diverge da solo (test senza etichetta,
voci orfane, esenzioni temporanee scadute), ma non può accorgersi di una funzionalità che nasce **senza** che
qualcuno l'abbia classificata.

Questa change chiude il ciclo dell'epica 20: la copertura end-to-end diventa un **prodotto del workflow**. Le skill
che governano il ciclo di vita del monorepo — `new-usecase`, `new-change`, `new-application`, `drop-application` —
identificano, registrano e mantengono le necessità di collaudo end-to-end a ogni passo.

## 2. Perimetro

**Incluso** (i cinque punti della Definition of Done di UC 0094):

1. `docs/usecases/_TEMPLATE.md` §9 acquisisce la sotto-sezione **"Journey end-to-end di piattaforma"**;
   `new-usecase` la compila (step-02) e **classifica il nuovo use case nel registro** già allo scaffolding (step-01).
2. `new-change` acquisisce il **passo di copertura** nel processo di implementazione e nella Definition of Done, più
   la menzione del registro in `CLAUDE.md`.
3. `new-application` genera il **journey core-loop di piattaforma** dell'app e le voci di registro; `drop-application`
   li rimuove; la **parità** del nuovo modello col journey vivo di `fatture` entra in `tools/scaffold-parity`.
4. Collaudi di processo e di scaffolding verdi nell'area `tooling`.
5. Aggiornamento dell'indice di esecuzione (per quanto applicabile alle storie evolutive).

**Escluso**: il registro e il controllo (UC 0093, prerequisito); la scrittura di journey nuovi per funzionalità già
esistenti (UC 0091/0092); l'aggancio alle skill minori `pricing-change` e `finalize-landing` (punto aperto di UC 0094,
differito a evidenza raccolta).

## 3. Requisiti funzionali

### R1 — `new-usecase` classifica alla nascita (leva «a» della consegna di UC 0093)

- **R1.1** — `_TEMPLATE.md`, sezione «9. Requisiti di test», contiene una sotto-sezione fissa
  **«Journey end-to-end di piattaforma»** con le tre risposte ammesse: journey nuovo, estensione di un journey
  esistente, esenzione motivata.
- **R1.2** — `new-usecase` step-01 (scaffolding) **classifica il nuovo use case nel registro**: `usecases_con_superficie`
  se la superficie esiste già, altrimenti una voce fra le `esenzioni` con categoria e motivo. Senza questo passo il
  controllo di UC 0093 diventa rosso al primo `run-tests.sh tooling`, perché ogni use case del catalogo deve essere
  classificato: la classificazione va fatta **nello stesso commit** che crea il file.
- **R1.3** — `new-usecase` step-02 pone esplicitamente la domanda sui journey e compila la sotto-sezione; un drill-down
  con superficie applicativa e senza quella sotto-sezione è incompleto.

### R2 — `new-change` salda i conti (leva «b» della consegna di UC 0093)

- **R2.1** — step-03 acquisisce la sequenza-tipo: **leggi il registro → decidi → implementa o rimanda → aggiorna il
  registro → il controllo conferma**. Vale anche per le change senza superficie frontend: la risposta «nessun impatto
  end-to-end» è legittima ma va data ed è registrata in `decisions.json`.
- **R2.2** — una change che implementa una **storia evolutiva** deve togliere l'esenzione `non-implementato` dello use
  case e dichiarare il percorso (anche solo `da-coprire`). Il controllo la rifiuta da sé appena esiste la cartella
  `changes/*-use-case-NNNN-*`: la skill deve **rimediare**, non accorgersi.
- **R2.3** — step-04 acquisisce un varco di verifica del registro, simmetrico a quello di parità dello scaffolding, che
  gira **prima** del varco di commit e non chiede consenso finché il registro non è coerente.
- **R2.4** — `CLAUDE.md` menziona il registro di copertura nella sezione «Esecuzione dei test» e fra i doveri della
  change (era il punto 2 della Definition of Done, lasciato scoperto di proposito da UC 0093).

### R3 — `new-application` fa nascere l'app con la sua copertura

- **R3.1** — il modello del test di livello 2 generato porta l'**etichetta** `[L2-<APP>]` in testa al titolo di ogni
  test (oggi nasce senza etichetta, e l'area `tooling` resta rossa finché una persona non la aggiunge a mano).
- **R3.2** — nuovo modello-sorgente `tools/new-application/templates/platform-e2e/`: il **journey core-loop di
  piattaforma** dell'app, derivato dal journey vivo di `fatture` (`J-QUOTA`), etichettato `[J-<APP>]`.
- **R3.3** — il generatore scrive le **due voci di registro** corrispondenti (`L2-<APP>` e `J-<APP>`, entrambe
  `coperto`) dentro `docs/testing/copertura-e2e.yaml`, fra marcatori che ne rendano possibile la rimozione esatta.
- **R3.4** — il journey generato **non può essere rosso alla nascita**: un'app appena scaffoldata ha il listino
  `status: inactive`, quindi nessun diritto d'uso (`EntitlementAccess.granted` richiede l'app `active`). Il journey
  si **salta da sé, con motivo esplicito**, finché l'app non è concessa, e diventa vero senza alcun intervento
  quando il listino passa ad `active`.
- **R3.5** — il journey generato non dipende dai **testi** dell'interfaccia (che l'autore dell'app riscriverà
  subito): si aggancia all'identificativo di prova del modulo, alle API reali e al database.

### R4 — `drop-application` è l'inverso esatto

- **R4.1** — il de-generatore rimuove il file del journey di piattaforma e le due voci di registro; il collaudo di
  round-trip (genera → de-genera → repository identico byte per byte) resta verde senza modifiche al suo corpo.

### R5 — Parità sorvegliata

- **R5.1** — `tools/scaffold-parity` confronta il nuovo modello di journey con `tools/platform-e2e/journeys/J-QUOTA.spec.ts`
  (insieme dei file + insieme dei moduli importati: se il journey vivo comincia a usare un helper nuovo, il modello
  invecchia e il collaudo lo dice).
- **R5.2** — `tools/scaffold-parity/source-paths.json` dichiara il journey di `fatture` e la cartella degli helper della
  suite di piattaforma fra i **percorsi-sorgente**, così il varco di step-04 di `new-change` scatta quando qualcuno li tocca.

### R6 — Il presidio morde (collaudo di processo)

- **R6.1** — i test di `tools/e2e-coverage` dimostrano su cartelle di prova che una change **simulata** con superficie
  nuova e registro non aggiornato produce `tooling` rosso, nelle tre forme in cui il caso si presenta: use case nuovo
  non classificato, superficie senza alcun percorso, esenzione `non-implementato` scaduta perché la change esiste.
- **R6.2** — il collaudo di livello 3 dello scaffolding (`generate-smoke.sh`) verifica che l'app generata nasca con il
  journey, l'etichetta e le voci di registro, e che il **controllo di copertura resti verde** sulla copia usa-e-getta.

## 4. Requisiti di test

| Area | Cosa deve essere verde |
|---|---|
| `tooling` | test di `tools/e2e-coverage` (compresi i nuovi casi di R6.1) + controllo del registro vero; test e collaudo di parità di `tools/scaffold-parity`; round-trip di `tools/drop-application`; collaudo di livello 3 di `new-application` |
| tutte | `./run-tests.sh` senza parametri (obbligo della modalità fast) |

Nessun codice applicativo (servizi, frontend, infrastruttura) viene toccato: le aree `backend`, `frontend`, `infra`,
`compliance`, `smoke`, `platform`, `site` devono restare verdi **senza regressioni**.

## 5. Dati personali

Nessuno. La change tocca skill, modelli di scaffolding, documenti di processo e strumenti di collaudo.
Classificazione attesa del varco privacy: nessun segnale.

## 6. Criteri di accettazione

- [ ] `_TEMPLATE.md` ha la sotto-sezione dei journey; `new-usecase` classifica (step-01) e interroga (step-02).
- [ ] `new-change` ha il passo di copertura in step-03 e il varco di verifica in step-04; `CLAUDE.md` menziona il registro.
- [ ] Un'app generata da `new-application` nasce con journey di piattaforma etichettato, test di livello 2 etichettato e
      due voci di registro; `drop-application` le toglie e il round-trip resta byte per byte identico.
- [ ] La parità del modello di journey è sorvegliata e i percorsi-sorgente sono dichiarati.
- [ ] `./run-tests.sh` completa verde.
- [ ] `decisions.json` registra ogni scelta, marcata `(autopilot)`.
