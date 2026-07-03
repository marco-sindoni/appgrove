# appgrove — Riferimento applicativo

Indice delle decisioni tecnologiche, architetturali, DevOps e applicative del monorepo appgrove.
Questo file è il punto di ingresso: ogni area ha un documento dedicato in [docs/](docs/) che viene
popolato man mano che le scelte vengono dipanate (un argomento alla volta, per domande e risposte).

- Contesto di prodotto: [recap_marketplace_microsaas.md](recap_marketplace_microsaas.md)
- Struttura del repo: [README.md](README.md)

## Lingua (non negoziabile)

**Tutta la comunicazione è in italiano.** Documenti, chat, messaggi di stato, requirements, log di
implementazione, commenti di PR e ogni testo rivolto all'utente vanno **sempre scritti in italiano**.
Restano in inglese solo gli identificatori tecnici intrinseci: codice, nomi di simboli/file/branch,
comandi, chiavi di config e citazioni letterali di output di tool.

## Invarianti architetturali (non negoziabili)

1. **Tenant ID solo dal JWT verificato** — claim `tenant_id` (= account, iniettato dal Pre-Token-Gen Lambda); `sub` = user_id. Mai da request body/params
2. **Filtro row-level** `WHERE tenant_id = :tid` su ogni query tenant-scoped
3. **Modulo Terraform `microsaas_app`** — nuova app = istanziare il modulo, non infra bespoke
4. **Logging strutturato** ovunque: ogni log porta `tenant_id`, `app_id`, `user_id`

## Tracciamento delle decisioni differite (non-negoziabile)

Quando, durante **qualsiasi** attività (implementazione, analisi, ricerca, discussione), riscontri una
**decisione architetturale, un drift o un punto aperto che NON appartiene al task corrente** ma a un
altro use case — o non ancora maturo per essere deciso — **DEVI tracciarlo subito**, prima di chiudere
il task:

- se riguarda **uno** use case → nel suo file `docs/usecases/<area>/NNNN-*.md`, sezione
  **"## Punti aperti / decisioni differite"** (creala in coda se manca): annota *cosa*, *perché è
  differito*, *quale UC lo possiede*;
- se è **trasversale** → in [docs/_BACKLOG.md](docs/_BACKLOG.md).

Non lasciare questi punti solo nella conversazione: **si perdono**, ed è un problema grave.
Tracciare ≠ risolvere: le decisioni premature si annotano, **non** si forzano fuori dal loro contesto.

## Avvio locale di nuove app/moduli (non negoziabile)

Ogni change che introduce una **nuova applicazione backend** (`services/<app>`) o un **nuovo modulo frontend avviabile**
DEVE **cablarne l'avvio e lo stop negli script di sviluppo locale** nello stesso pattern degli esistenti, così che l'app/
modulo sia **eseguibile in locale subito dopo il merge**, senza passi manuali impliciti:

- backend: aggiungere il processo a [app-start.sh](app-start.sh) e [app-stop.sh](app-stop.sh) (Postgres condiviso, porta
  dedicata, profilo `%dev`) **e** la route `/api/<app_id>/v1/*` in [dev/Caddyfile](dev/Caddyfile);
- frontend: registrare il modulo nell'App Registry (∩ entitlement) e — finché l'entitlement reale non esiste — abilitarlo
  nello stub locale.

È parte del **Definition of Done** della change e va verificato eseguendo l'app in locale. L'**auto-discovery
multi-servizio** del dev stack è di **UC 0046** (`dev service` è uno stub dichiarato): finché non esiste, il cablaggio è
**esplicito** e non si rimanda.

## Esecuzione dei test (non negoziabile)

Lo script **[run-tests.sh](run-tests.sh)** alla root è la **sorgente di verità unica** per "lanciare tutti i test automatici
di tutti i moduli": backend (`services/*` via Maven), frontend (`frontend/` via npm/vitest **+ Playwright e2e L2**, browser
auto-installato — UC 0029; la suite L3 sandbox è pre-release e resta fuori), infra (`infra/` via Terraform).
Esegue tutte le aree, non si ferma al primo errore e ritorna exit-code ≠ 0 se una qualsiasi suite è rossa (`./run-tests.sh`
per tutto, oppure `./run-tests.sh backend|frontend|infra`).

**Va tenuto costantemente aggiornato**: ogni change che **aggiunge/rimuove un modulo** (`services/<app>`, package frontend) o
**cambia il comando di test** di un'area DEVE aggiornare `run-tests.sh` nello stesso commit, così che resti l'unico entrypoint
completo. È parte del **Definition of Done** di `new-change`. Prima del commit, una change che tocca codice eseguibile lancia
`run-tests.sh` (almeno le aree toccate) e verifica il verde — l'esecuzione per-area dei singoli comandi resta valida, ma
`run-tests.sh` è il modo canonico per "eseguire tutto".

## Documenti di decisione

Legenda stato: 🔴 da definire · 🟡 in corso · 🟢 deciso

| # | Area | Documento | Stato |
|---|---|---|---|
| 01 | Architettura applicativa & multi-tenancy | [docs/01-architettura.md](docs/01-architettura.md) | 🟢 |
| 02 | Auth & sicurezza | [docs/02-auth-sicurezza.md](docs/02-auth-sicurezza.md) | 🟢 |
| 03 | Frontend | [docs/03-frontend.md](docs/03-frontend.md) | 🟢 |
| 04 | Backend / services (Quarkus) | [docs/04-services-backend.md](docs/04-services-backend.md) | 🟢 |
| 05 | Persistenza & dati | [docs/05-persistenza-dati.md](docs/05-persistenza-dati.md) | 🟢 |
| 06 | Infrastruttura / IaC (Terraform) | [docs/06-infra-iac.md](docs/06-infra-iac.md) | 🟢 |
| 07 | DevOps / CI-CD | [docs/07-devops-cicd.md](docs/07-devops-cicd.md) | 🟢 |
| 08 | Observability | [docs/08-observability.md](docs/08-observability.md) | 🟢 |
| 09 | Pagamenti (Paddle) | [docs/09-pagamenti.md](docs/09-pagamenti.md) | 🟢 |
| 10 | Testing strategy | [docs/10-testing.md](docs/10-testing.md) | 🟢 |
| 11 | Developer experience / local dev | [docs/11-developer-experience.md](docs/11-developer-experience.md) | 🟢 |
| 12 | Environments & config management | [docs/12-environments-config.md](docs/12-environments-config.md) | 🟢 |
| 13 | Compliance & Privacy (GDPR) | [docs/13-compliance-privacy.md](docs/13-compliance-privacy.md) | 🟢 |
| 14 | Sito vetrina & testi legali (ToU/PP) — **prerequisito attivazione Paddle** | [docs/14-sito-vetrina-legale.md](docs/14-sito-vetrina-legale.md) | 🟢 |

## Casi d'uso (use case)

Catalogo unico di specifiche implementative dettagliate, organizzato **per area in sottocartelle numerate** con
**numerazione assoluta a 4 cifre** (`NNNN`): indice per area → [docs/usecases/README.md](docs/usecases/README.md) ·
**ordine di esecuzione + stato implementazione** → [docs/usecases/_INDEX.md](docs/usecases/_INDEX.md) (sync da `new-change`) ·
template → [docs/usecases/_TEMPLATE.md](docs/usecases/_TEMPLATE.md). Aree: `01-business-legal … 11-apps`.
- Creazione/numerazione/scaffolding: skill **`new-usecase`**; implementazione: **`new-change`** (folder
  `NNNN-use-case-YYYY-…` quando nasce da uno use case).
- Catalogo: **59 use case** (0001–0059), drill-down scritti (🟢) salvo 0002/0044/0055/0056/0057/0059 in corso (🟡). Esempio
  migrato: [0017 — Flussi auth](docs/usecases/05-auth/0017-flussi-auth.md) (UC1–UC10). 0055–0057 aggiunti dopo la revisione
  di copertura requisiti→use case (infra condivisa per-env, ri-accettazione legali runtime, skill `finalize-landing`);
  **0058** scorporato da UC 0010 (flussi auth locali completi) nella change `0009-use-case-0010-…`;
  **0059** (gestione membri & inviti UI) scorporato da UC 0017 nella change `0012-use-case-0017-…`.

## Backlog trasversale

Temi sollevati da affrontare nell'argomento giusto (o dedicato): [docs/_BACKLOG.md](docs/_BACKLOG.md)
— compliance/privacy (GDPR, tracking, T&C con Paddle MoR), configurazione admin, skill da creare.

## Costi AWS

Stima costi viva (principio: costo minimo compatibile coi requisiti), aggiornata a ogni decisione:
[docs/_COSTI-AWS.md](docs/_COSTI-AWS.md).

## Evoluzioni DevOps

Registro vivo delle scelte cost-min con il relativo percorso di hardening/scaling (NAT, ALB, HA, …):
[docs/_EVOLUZIONI-DEVOPS.md](docs/_EVOLUZIONI-DEVOPS.md).

## Inquadramento fiscale (commercialista)

Note fiscali/contributive per l'avvio come **persona fisica senza società** con Paddle MoR (P.IVA forfettaria, abitualità,
rischi, vie compliant) + checklist da verificare con un commercialista: [docs/_COMMERCIALISTA.md](docs/_COMMERCIALISTA.md).
Regola chiave: l'obbligo P.IVA nasce dall'**attività abituale**, non dal payout.

## Revisione legale pre-go-live

Registro vivo dei punti da far rivedere a un legale prima del go-live (DPA, privacy policy, ToS, retention, sub-processor,
accessibilità): [docs/_REVISIONE-LEGALE.md](docs/_REVISIONE-LEGALE.md). Consigliata/opzionale, nessun blocco pre-go-live.

## Git: account personale vs lavoro (EMU)

Il repo `marco-sindoni/appgrove` va pushato con l'account **personale** `marco-sindoni`. Se il push
fallisce con `403 — Permission denied to Marco-Sindoni_ElGhEmu`, è attivo per sbaglio l'account di
lavoro (EMU) e `osxkeychain` ne ha cachato il token per `github.com`. Fix:

```bash
gh auth switch -u marco-sindoni
printf "protocol=https\nhost=github.com\n\n" | git credential-osxkeychain erase
git push origin main
```

Lo switch di `gh` è persistente; verificare l'identità con `gh api user --jq .login` (dev'essere
`marco-sindoni`). Non serve riautenticarsi: entrambi i token sono già nel keyring di `gh`.

## Come si decide (processo)

Un argomento alla volta. Per ciascuno: si elencano i topic, si risolvono per domande e risposte,
e le scelte confermate vengono scritte nel documento dell'area (template in
[docs/_TEMPLATE.md](docs/_TEMPLATE.md)). Lo stato nell'indice passa da 🔴 → 🟡 → 🟢.
Le decisioni prese qui sono vincolanti per la skill `/new-change`.
