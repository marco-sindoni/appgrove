# Piano di lavoro — UC 0112 · Copilota dei ruoli nella skill `new-application`

**Storia**: [0112](../story/0112-copilota-ruoli-new-application.md) · **Aree toccate**: `.claude/skills/`, `tools/new-application/`, `tools/scaffold-parity/`
**Dimensione stimata**: media · **Prerequisiti**: UC 0101, UC 0111

## Passo 1 — Il nuovo passo della skill

**File nuovo**: `.claude/skills/new-application/step-02-roles.md` — il copilota dei ruoli, con le quattro
domande della storia §4, una alla volta, in prosa, secondo lo stile già in uso negli altri copiloti.

**Rinumerazione**: `step-02-pricing.md` → `step-03-pricing.md`, `step-03-personal-data.md` →
`step-04-personal-data.md`, `step-04-close.md` → `step-05-close.md`.

**Modifica**: `.claude/skills/new-application/SKILL.md` — elenco dei passi, e nei presidi obbligatori la
fermata prevista dalla seconda domanda (dati che un `viewer` non dovrebbe vedere).

Motivo della collocazione **prima** del listino: le operazioni dispositive influenzano la metrica di quota, e
scegliere i prezzi prima di sapere che cosa l'applicazione fa è l'ordine sbagliato.

## Passo 2 — Il generatore

**Modifiche** in `tools/new-application/`:

- `generate.mjs` — nuove opzioni per le risposte del copilota (elenco delle operazioni dispositive, poteri
  specifici dell'`admin`, operazioni esenti);
- `templates/` — quattro aggiunte ai modelli-sorgente:
  1. il varco (`@RequiresAppRole`) sulle operazioni generate, col ruolo giusto;
  2. il documento delle operazioni dell'applicazione (UC 0101);
  3. i collaudi per ruolo (un caso per ruolo su una lettura e una scrittura, più la verifica strutturale);
  4. la voce «Utenti» nel manifesto del modulo frontend, che monta il componente condiviso di UC 0111.

Regola già in vigore e da rispettare: se l'esito è sbagliato **si corregge il modello e si rigenera**, mai si
corregge l'esito.

## Passo 3 — Il collaudo di parità

**Modifica**: `tools/scaffold-parity/` — la parità deve **accorgersi** se l'applicazione di riferimento porta
i ruoli e il modello no. Concretamente: presenza del varco sulle operazioni di scrittura, presenza del
documento delle operazioni, presenza dei collaudi per ruolo, presenza della voce «Utenti».

**Modifica**: [docs/_PARITA-SCAFFOLD.md](../../../_PARITA-SCAFFOLD.md) — se qualcosa resta deliberatamente
indietro, va scritto là col motivo. Se non resta indietro nulla, va comunque annotato che i ruoli sono ora
parte della parità.

## Passo 4 — Prova sul campo

Generare un'applicazione usa-e-getta e verificare che nasca **verde** e coi ruoli funzionanti:

```bash
node tools/new-application/generate.mjs --app-id provaruoli --port 8099 \
  --user-model multi --metric elementi --quota-nature stock --dry-run
# poi senza --dry-run, e:
cd services && mvn -B -pl provaruoli -am test
./run-tests.sh tooling
# infine si getta via il risultato (git checkout / drop-application)
```

È la sola prova che conta: se un'applicazione generata da zero non rispetta i ruoli senza interventi a mano,
questa storia non è finita.

## Passo 5 — La skill che dismette

**Modifica**: `tools/drop-application/` e `.claude/skills/drop-application/` — la dismissione deve rimuovere
anche le righe di accesso dell'applicazione e la sua voce «Utenti». Se non entra in questa change, va annotato
come rimando in [UC 0048](../../10-skills-tooling/0048-skill-drop-application.md).

## Verifica finale

```bash
./run-tests.sh tooling backend
```

## Trappole note

1. **La domanda numero due è una fermata, non un questionario**: una risposta «sì, ci sono dati che il
   `viewer` non deve vedere» va portata allo sviluppatore, non risolta inventando un quarto ruolo.
2. **I modelli invecchiano in silenzio**: senza il passo 3 questa storia funziona per la prossima applicazione
   e non per quella dopo.
3. **La rinumerazione dei passi** va fatta in tutti i rimandi interni della skill, non solo nei nomi dei file.
