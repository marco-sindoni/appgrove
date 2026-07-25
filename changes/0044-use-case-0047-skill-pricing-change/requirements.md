# Change 0044: skill `pricing-change` — cambi di pricing successivi al lancio

**Branch**: `change/0044-use-case-0047-skill-pricing-change`
**Aree**: `.claude/skills/pricing-change/` (nuova skill · prosa), `tools/pricing-change/` (nuovo tool Node · codice eseguibile), `run-tests.sh` (registrazione area tooling), `docs/usecases/_INDEX.md` (stato use case)
**Data**: 2026-07-25
**Autore**: Platform Engineering (modalità autopilot)
**Use case sorgente**: [docs/usecases/10-skills-tooling/0047-skill-pricing-change.md](../../docs/usecases/10-skills-tooling/0047-skill-pricing-change.md)
**Tocca dati personali?**: No — il catalogo pricing-as-code è "cosa si vende" (app / tier / limiti / prezzi), dato di piattaforma non legato al singolo cliente, senza dati personali. Stessa natura della change 0019 (motore di sync). Nessun gate privacy/RoPA.

## Problema / Obiettivo

Dopo il lancio, i prezzi cambiano: si aggiunge un livello (tier), si alza o abbassa un prezzo, si stringono o allargano i limiti, si aggiunge il ciclo annuale accanto al mensile. Oggi non esiste un modo guidato e sicuro per farlo. Farlo a mano è pericoloso su due fronti che il fornitore di pagamento e il motore di sync (UC 0022) rendono non negoziabili:

1. **Immutabilità** — un prezzo già pubblicato non si muta nell'importo; il motore di sync **rifiuta** la mutazione e la build va in errore.
2. **Grandfathering** — gli abbonati esistenti non vanno spostati di nascosto: restano sul vecchio prezzo salvo migrazione **esplicita**, che è una decisione di denaro ed effetto verso l'esterno.

A ciò si aggiunge che chi cambia un prezzo raramente ha davanti l'informazione che conta: **quanto di quel prezzo se ne va in commissioni**. Su prezzi bassi e mensili la parte fissa del fornitore pesa oltre il 10%.

Obiettivo: la skill **`pricing-change`** (env-agnostica, chiude attraverso `new-change` → branch + PR, nessun dialogo diretto col fornitore di pagamento) che co-pilota questi cambi, produce un pricing-as-code **valido** che il motore di sync (UC 0022) applica in modo idempotente, fa **decidere** il grandfathering, e mostra la **fee effettiva** con avviso soft oltre il 10%. È il gemello di `new-application` per i cambi *successivi*: `new-application` scrive il pricing **iniziale**, `pricing-change` i cambi che vengono dopo (#09 H36).

## Scope

Coerentemente con lo stile delle due skill sorella — un **tool deterministico** per la parte meccanica/numerica testabile, più un **co-pilota** per le decisioni che uno strumento non può prendere (#09 H36).

**1. Tool deterministico `tools/pricing-change/`** (Node ESM, testato con `node --test`, come `tools/drop-application/`). Possiede la parte meccanica e numerica:
- **Calcolo della fee effettiva** (#09 K46/K47): per un importo, un ciclo e una valuta, calcola percentuale di commissione effettiva e **netto incassato**, con la parte percentuale (~5%) e la parte fissa per transazione (~$0,50, convertita in euro con un tasso **assunto e documentato** — la fee è un segnale-guida soft, non contabilità, quindi il tool resta deterministico e testabile). Espone l'**avviso soft oltre ~10%** senza bloccare.
- **Operazioni di modifica del pricing-as-code** sul contratto YAML **congelato dalla change 0019** (`services/core/src/main/resources/pricing/<slug>.yaml`), rispettando l'immutabilità: aggiungere un tier, aggiungere un ciclo (mensile/annuale) a un tier, cambiare i limiti di un tier, e il **cambio prezzo** nelle sue due forme (vedi sotto). Ogni operazione produce YAML che **carica correttamente** contro lo schema; il tool si autocontrolla con fixture YAML in `node --test`.

**2. Skill co-pilota `.claude/skills/pricing-change/`** (SKILL.md + step, prosa in italiano, stile delle sorelle). Possiede le decisioni:
- **Tipo di cambio** (nuovo tier / cambio prezzo / cambio limiti / aggiungi ciclo) — #09 H36.
- **Come si esprime un cambio prezzo** rispettando l'immutabilità (#09 H35/H37):
  - prezzo **non ancora sincronizzato** (nessun `paddle_price_id`) → l'importo si corregge in loco;
  - prezzo **vivo** → immutabile: il nuovo prezzo si porta con un **nuovo tier** (nuova chiave stabile), lasciando definito il vecchio tier per gli abbonati esistenti. È la mappatura del «nuovo Price + archivia il vecchio» di #09 H35 sul modello appgrove, dove `(tier × ciclo) = un prezzo` con identità deterministica `(slug, tier.key, ciclo)` **senza versione**.
- **Grandfathering / migrazione**: mostra il quadro delle subscription attive sul tier toccato e fa **decidere**. Il default (gli esistenti restano) è già garantito dalla sync. La **migrazione esplicita** (di proprietà di UC 0047) è **denaro ed effetto verso l'esterno**: si **escala allo sviluppatore** anche in autopilot e, se scelta, si consegna come passo di **runbook** riusando il meccanismo esistente `changeSubscriptionTier` — la skill non la esegue.
- **Fee effettiva**: per ogni prezzo proposto (mensile **e** annuale) mostra il risultato del calcolo del tool, avvisa soft oltre il 10%, spinge verso l'annuale (#09 K48/K49).
- Chiude attraverso **`new-change`** (branch + PR); la **sync** (UC 0022) propaga a sandbox al merge e a produzione al tag.

**3. `run-tests.sh`** — registra `tools/pricing-change` nell'area **tooling** (Definition of Done di `new-change`: l'aggiunta di un modulo aggiorna `run-tests.sh` nello stesso commit).

**4. `docs/usecases/_INDEX.md`** — stato di 0047 a 🟡 all'apertura, ✅ alla chiusura.

## Fuori scope

- **Il motore di sync e il modello di identità del catalogo** (UC 0022, congelato dalla change 0019): la skill **scrive** gli YAML che quel motore consuma, non lo modifica. Nessuna modifica a `services/core`, a `CatalogIds`, alla DDL, alla sync.
- **Una dimensione di versione a livello di prezzo** — che consentirebbe a vecchio e nuovo prezzo di coesistere sullo stesso `(tier, ciclo)` senza coniare un intero nuovo tier — è un **lavoro sull'engine** di proprietà di UC 0022, **tracciato** nei suoi punti aperti, non anticipato qui.
- **Il pricing iniziale** di un'app: è `new-application` (UC 0046).
- **Il dialogo diretto con le API del fornitore di pagamento**: lo fa la sync (UC 0022), oggi comunque bloccata da #14.
- **Cambiare i prezzi di un'app reale** (`crm.yaml`, `fatture.yaml`): questa change *implementa la skill*, non esegue un cambio di listino; i listini reali restano invariati e i test del tool usano YAML fixture.
- **Il cablaggio della sync nella pipeline CI** (UC 0005) e il **client Paddle reale** (#14): fuori scope, già tracciati in UC 0022.

## Criteri di accettazione

- [ ] Esiste `tools/pricing-change/` (Node, `node --test`) con: (a) calcolo della **fee effettiva + netto** conforme a #09 K46 (~5% + parte fissa ~$0,50 con tasso di cambio documentato) e **avviso soft >10%**, coperto da test; (b) operazioni di modifica del pricing-as-code (nuovo tier, cambio prezzo, cambio limiti, aggiungi ciclo) che producono YAML valido contro lo schema congelato, con test di round-trip su fixture.
- [ ] Il **cambio prezzo** rispetta l'immutabilità: correzione in loco per un prezzo non sincronizzato; **nuovo tier** per un prezzo vivo, mai mutazione dell'importo di un prezzo vivo. Un test lo dimostra su fixture.
- [ ] Esiste la skill `.claude/skills/pricing-change/` che co-pilota i quattro tipi di cambio, fa **decidere** il grandfathering (con **escalation** allo sviluppatore per la migrazione esplicita), mostra la fee effettiva e chiude attraverso `new-change` (branch + PR), senza mai parlare col fornitore di pagamento né eseguire migrazioni.
- [ ] `run-tests.sh` esegue i test del nuovo tool nell'area **tooling** e resta verde; `docs/usecases/_INDEX.md` segna 0047 ✅ alla chiusura.
- [ ] I punti aperti non risolti qui (versione del prezzo lato engine; eventuale comando di migrazione batch se `changeSubscriptionTier` non copre il caso) sono **tracciati** nei loro use case proprietari prima della chiusura.

## Invarianti appgrove toccati

- **Tenant ID solo dal JWT** / **filtro row-level**: **non toccati** — il catalogo pricing-as-code è platform-level, non tenant-scoped; la skill non interroga dati tenant. L'eventuale migrazione (runbook) riusa `changeSubscriptionTier`, che porta con sé i propri presidi.
- **Modulo Terraform `microsaas_app`**: non toccato (nessuna infra).
- **Logging strutturato**: il tool è uno strumento da riga di comando (non un servizio); non introduce log applicativi tenant/app/user.

## Requisiti di test

- **Fee effettiva** (#09 K47, Definition of Done item 4): test che, per importi e cicli noti, verificano percentuale effettiva, netto e la soglia dell'avviso soft (>10% acceso sotto ~€5-6/mese in mensile; spento sull'annuale corrispondente).
- **Immutabilità del cambio prezzo**: test che un prezzo **vivo** non viene mai mutato nell'importo, e che il cambio prezzo su prezzo vivo produce un **nuovo tier** con il nuovo prezzo lasciando il vecchio definito.
- **Validità del pricing-as-code**: test di round-trip che ogni operazione produce YAML che ricarica correttamente contro lo schema congelato (parse → modifica → parse). La **idempotenza della sync** sullo YAML prodotto è già garantita dai test di `PricingSyncService` (UC 0022): il tool produce lo stesso formato che quel motore, già testato, consuma.

## Valutazione di impatto

| Area | Impatto |
|---|---|
| Breaking change | No |
| Contratto cross-area | No — la skill *consuma* il contratto YAML congelato dalla change 0019, non lo cambia |
| Version bump | nessuno (nuova skill + nuovo tool; nessun modulo pubblicato versionato) |
