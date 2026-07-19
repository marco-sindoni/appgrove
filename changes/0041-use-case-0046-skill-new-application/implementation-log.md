# Change 0041 — Log di implementazione

**Branch**: `change/0041-use-case-0046-skill-new-application`
**Use case**: [UC 0046](../../docs/usecases/10-skills-tooling/0046-skill-new-application.md) — skill `new-application`
**Data**: 2026-07-19

## Cosa è stato fatto

### 1. Skill `new-application` — struttura ibrida

`.claude/skills/new-application/`: `SKILL.md` + quattro passi (identità/generazione, co-pilota prezzi,
co-pilota dati personali, chiusura). La skill **gira dentro `new-change`**: non reinventa i varchi di
consenso a commit e unione, li eredita.

Regola resa esplicita in più punti perché è quella che decide se lo strumento invecchia bene:
**se l'output generato è sbagliato si corregge il modello e si rigenera, mai si rattoppa l'output.**
Un output rattoppato è una divergenza che nessuno ricorderà, e l'app successiva eredita il difetto.

### 2. Generatore deterministico — `tools/new-application/`

`generate.mjs` + 63 modelli-sorgente. Genera 63 file, ne modifica 5, delega l'infrastruttura a
`infra/scripts/service-add`. Segnaposto `@@NOME@@`, verificati **prima e dopo** la scrittura: un
segnaposto sopravvissuto è il fallimento più insidioso (il file sembra giusto ed esplode a runtime).

Scelto Node e non bash per tre ragioni concrete: `package.json` va riscritto come JSON valido; la
sostituzione su decine di file pieni di virgolette e simboli di espansione in bash è un campo minato;
`--dry-run` richiede il piano completo prima di scrivere, e in bash sarebbe una seconda
implementazione destinata a divergere dalla prima.

### 3. Debito 1 — scoperta automatica degli script di sviluppo

`dev/lib/services.sh` deriva la mappa servizio → identificativo → porta → schema dagli
`application.properties` **già presenti** nei servizi: nessun registro nuovo da tenere allineato a
mano, che sarebbe stato lo stesso debito con un altro nome. La consumano `dev migrate`, `dev service`,
`app-start.sh`, `app-stop.sh`, `dev/Caddyfile` (marker rigenerati) e i due script di verifica d'avvio.
**Il generatore non tocca nessuno di questi file.**

### 4. Debito 2 — liste dei servizi nei workflow

`tools/ci/services.sh` riusa la **stessa** scoperta e alimenta matrice di build e cicli per-servizio
dei tre workflow. Restano espliciti i cicli sulle due interfacce web: sono fissate dall'infrastruttura
condivisa e non crescono con le app.

### 5. Debito 3 — proiezione locale dei diritti

Il pezzo con il rischio vero. Postura decisa col Platform Engineer: **cache con rete di sicurezza**.

- `core` pubblica un **evento sottile** ("i diritti del tenant T sono cambiati") sulla coda
  `entitlement-<app_id>`, agganciato ai due punti in cui lo stato cambia davvero: applicazione di un
  evento di pagamento (solo esito *applicato*, non duplicato né fuori ordine) e richiesta/annullo di
  eliminazione account.
- `commons` porta proiezione, consumatore, misure e il servizio predefinito che risolve i diritti.
- `fatture` è migrata; la chiamata sincrona **non sparisce**: retrocede a rete di sicurezza,
  qualificata `@SafetyNet` perché iniettarla sia un atto esplicito.

**Perché l'evento è sottile e non grasso.** Trasportare i diritti calcolati avrebbe costretto `core` a
ri-derivarli fuori da una richiesta autenticata, duplicando la logica del read-model e aggirando il
filtro per tenant. Il rischio di due derivazioni che divergono nel tempo è molto peggiore del costo di
un rinfresco: così la derivazione resta **in un posto solo**.

**Tre situazioni, tre risposte** — riga fresca → si usa, senza soglia di scadenza (è l'evento, non il
tempo, a dire che qualcosa è cambiato); riga da rinfrescare + core giù → si serve il valore vecchio
(un guasto di core non deve bloccare i paganti: è il disastro che la proiezione esiste per evitare);
riga assente + core giù → si nega, unico caso di utente legittimo respinto.

Scostamenti strumentati con misure e **allarmi** nel modulo `microsaas_app`: ricorso alla rete di
sicurezza, proiezione vecchia servita, accesso negato per assenza di base, coda degli scarti.

### 6. Presidio anti-invecchiamento a tre strati

Aggiunto su richiesta esplicita, in dialogo. Il rischio non è che i modelli si rompano, è che
**invecchino in silenzio**: il codice evolve, i modelli restano indietro, tutto resta verde e ogni app
nuova nasce antiquata.

1. **Collaudo di parità** (`tools/scaffold-parity`) — confronto strutturale modelli ↔ app #1: file,
   dipendenze, chiavi di configurazione, annotazioni portanti. Rosso da solo.
2. **Rilevatore dei percorsi-sorgente** + varco in `new-change` step-04: toccare un percorso da cui i
   modelli derivano obbliga ad aggiornarli o a motivare per iscritto.
3. **`docs/_PARITA-SCAFFOLD.md`** — registro delle deviazioni consapevoli. Ogni riga silenzia una
   segnalazione: è una firma, non un silenziamento distratto.

Discusso e **scartato** l'aggancio automatico a ogni sessione (come per la regola della lingua): quella
è una direttiva di comportamento che si logora a ogni messaggio, questo registro serve in due momenti
precisi. Richiamo affidato a `CLAUDE.md` + i due varchi mirati.

## Test

`./run-tests.sh` completo — **tutte e sei le aree verdi**.

| Area | Esito |
|---|---|
| backend | 165 test `core`, 38 `fatture` (9 nuovi sulla proiezione), 26 `commons` |
| frontend | unitari + 17 end-to-end |
| infra | formattazione, validazione, linter dei workflow |
| compliance | 17 test + parità lingue e allineamento del registro trattamenti |
| **tooling** (nuova) | 17 test del presidio + parità verde + **collaudo livello 3** |
| smoke | avvio reale dei tre servizi + accesso end-to-end |

**Area `tooling`, collaudo di livello 3** (scelta esplicita del Platform Engineer: livello 3 *sempre*):
`generate-smoke.sh` copia il repo, genera davvero un'app ed **esegue la sua intera suite** — 38 test
verdi. È la dimostrazione letterale della promessa "l'app generata nasce con suite verde". Messa in
un'area a sé, fuori da `./run-tests.sh backend`, per non appesantire i cicli rapidi.

**Presidio dimostrato, non solo scritto**: la divergenza è stata **provocata apposta** in una copia
usa-e-getta (un file, una dipendenza, una chiave, un'annotazione) e tutti e quattro i controlli hanno
morso; una deroga firmata nel registro ne ha silenziata esattamente una. Un presidio mai visto fallire
non è un presidio.

## Tre cose scoperte strada facendo

**1. I test di `fatture` scavalcavano la proiezione.** Il finto servizio dei diritti era il bean
*predefinito*: la suite non attraversava il percorso che gira in produzione. Riqualificato come rete di
sicurezza, sono emersi subito 9 fallimenti reali — la proiezione è una cache **su tabella** e
sopravviveva fra un test e l'altro. Regola "ogni test parte da cache pulita" espressa in **un punto
solo** (`ProjectionResetCallback`) invece che ripetuta in tre classi: dimenticarla in una classe futura
darebbe un fallimento intermittente e difficile da leggere.

**2. La cancellazione dei dati non rimuoveva la proiezione** — lacuna introdotta da questa change e
trovata *grazie* al gate privacy. Dopo un'erasure sopravviveva in ogni schema applicativo una riga con
l'identificativo dell'account e il piano che aveva: la traccia di chi ha chiesto di sparire. Corretta in
`commons` (consumatore della cancellazione) e **non** nel contratto delle singole app: se ogni app
dovesse ricordarsene, prima o poi una se ne dimenticherebbe, in silenzio. Il conteggio entra nell'audit,
perché l'audit è la prova dell'erasure e una prova incompleta non è una prova. Coperta da test sul
percorso reale (messaggio in coda + consumatore), non con una scorciatoia.

**3. Lo scanner privacy era cieco sui file nuovi.** Elencava i file non tracciati dalla **directory
corrente**: lanciato nel modo documentato vedeva **0** file nuovi invece di 95 — cioè taceva proprio sul
caso a più alto segnale, una migrazione o un'entità appena create. Corretto ancorando i comandi alla
radice del repository. Tracciato in UC 0031 insieme al rumore che i modelli-sorgente ora producono sul
gate (~40 segnali per change), che rischia di allenare a ignorarlo.

## Gate privacy/RoPA (UC 0031)

Eseguito **dopo** la correzione dello scanner. Segnali: la migrazione della proiezione e i modelli-sorgente.

- **Proiezione entitlement**: nessun dato personale nuovo. Contiene identificativo di account, piano e
  tetti — informazione di abbonamento già trattata da `core`, qui in copia. Nessuna nuova finalità, base
  giuridica, categoria o conservazione. **Cancellata insieme al resto all'erasure** (vedi sopra).
- **Modelli-sorgente**: file inerti finché non istanziati; i campi segnaposto vengono dichiarati nel
  manifesto dell'app generata, che il co-pilota obbliga a compilare.

**Classificazione: MINOR** — nessun cambiamento materiale (finalità, basi, categorie, conservazione
invariate). Componente interessato: piattaforma. Nessun nuovo sub-responsabile esterno.

## Decisioni differite tracciate

- **UC 0046**: tre punti aperti marcati **chiusi** con il rationale della soluzione (non cancellati: il
  *perché* è memoria del caso d'uso).
- **UC 0054**: la seconda app eredita la proiezione dallo scaffold; annotato il rischio di regressione
  (iniettare la rete di sicurezza "per semplificare") e la nota sui posti a giacenza.
- **UC 0031**: difetto dello scanner (corretto) + rumore dei modelli sul gate (da decidere).
- **`docs/_BACKLOG.md`**: la voce sull'accoppiamento app↔core è marcata risolta, con le scelte effettive.

## Valutazione finale

| Area | Impatto |
|---|---|
| Breaking change | Sì, interno: la risoluzione dei diritti passa dalla chiamata sincrona alla proiezione. Nessun contratto pubblico rotto; `fatture` migrata nella stessa change |
| Contratto cross-area | Sì: nuovo evento core → app; nuove code e allarmi nel modulo `microsaas_app` |
| Version bump | minor |

**Dove concentrare la revisione**: non sullo scaffolding, che è meccanico e coperto dal collaudo di
livello 3, ma sulla **postura di risoluzione dei diritti** (`ProjectedEntitlementService`) e sulla sua
strumentazione. È lì che vive il rischio: privilegia la disponibilità sulla freschezza, e la
contropartita di quella scelta sono gli allarmi.
