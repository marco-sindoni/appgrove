# Change 0069: Fondamenta suite end-to-end di piattaforma (stack reale + Mailpit + primo journey)

**Branch**: `change/0069-use-case-0090-e2e-platform-fondamenta`
**Aree**: tools/platform-e2e (nuova), tools/smoke, dev/lib, run-tests.sh, .github/workflows
**Data**: 2026-08-01
**Autore**: Platform Engineering (modalità fast, chiamante go-fast)
**Use case sorgente**: `docs/usecases/20-test-e2e-piattaforma/0090-e2e-platform-fondamenta.md`
**Tocca dati personali?**: No — la suite crea e usa soltanto dati sintetici (email fittizie su dominio
non recapitabile `test.appgrove.local`, nomi generati) in un database locale/CI usa-e-getta. Nessun
manifesto GDPR da aggiornare, nessun campo `@PersonalData` nuovo.

## Problema / Obiettivo

Il monorepo ha oggi tre livelli di test end-to-end, ma manca il quarto: un browser reale che naviga i
frontend **costruiti davvero** contro lo **stack backend vero** (Postgres, ElasticMQ, tutti i servizi in
profilo `dev`), con le **email transazionali realmente spedite e verificate** via Mailpit. Gli e2e L2
simulano il backend, lo smoke headless non ha browser, la suite L3 richiede il cloud. Questa change
costruisce l'infrastruttura della suite di piattaforma (nuova area `platform` di `run-tests.sh`) e la
prova con **un primo journey completo** — J-REG, la registrazione: signup → email di verifica ricevuta
davvero e cliccata → onboarding workspace → dashboard — incluse le verifiche a livello di database
(rilevatore di fughe fra tenant).

## Scope

1. **Nuova cartella `tools/platform-e2e/`** — progetto Playwright autonomo e cross-app:
   - `run.sh`: orchestratore dei passi 1–6 del flusso dello use case — avvio compose (Postgres +
     ElasticMQ + **Mailpit**, idempotente), migrazioni + seed (gli stessi passi di `app-start`), avvio di
     **tutti** i servizi scoperti automaticamente (`dev/lib/services.sh`) in profilo `dev` su porte
     dedicate alla suite (offset **+12000**, diverso dal +10000 dello smoke → convivenza), build delle
     SPA (`backoffice`, `admin`) e pubblicazione con un **server statico Node** (senza dipendenze) che
     serve i `dist/` e **inoltra le rotte `/api/*`** derivate dalla stessa scoperta servizi, esecuzione
     dei journey Playwright, verdetto unico e teardown dei processi (i container compose restano su);
   - `helpers/`: libreria condivisa dei journey — `mailbox` (attesa/lettura/estrazione link via API
     Mailpit, timeout con messaggio parlante), `db` (query/assert leak-detector via `docker exec psql`,
     nessuna dipendenza npm), `tenant` (creazione programmatica di un tenant fresco via API
     signup+verify) e `login` (accesso via API); documentate per gli UC 0091/0092;
   - `journeys/J-REG.spec.ts`: il journey registrazione completo (passi 1–5 dello use case, incluse le
     assert sull'email reale in Mailpit — mittente, oggetto nella lingua attesa, link — e il
     leak-detector a livello DB: account/utente col `tenant_id` atteso, nessun dato fuori dal tenant);
   - `--journey <id>` (grep Playwright) per l'esecuzione parziale in sviluppo; ritenti Playwright ≤ 1
     (il ritento che passa è segnalato come "flaky" dal reporter); trace/screenshot/video su fallimento.
2. **Estrazione funzioni comuni** fra `tools/smoke/stack-headless.sh` e il nuovo orchestratore in una
   libreria condivisa `dev/lib/headless.sh` (env dev, compose up, migrate+seed, build artefatti, chiavi
   auth, avvio servizio, attesa readiness) — **senza cambiare il comportamento dello smoke**.
3. **`run-tests.sh`**: nuova area `platform` (stesso commit, regola non negoziabile), inclusa
   nell'esecuzione completa senza parametri.
4. **CI**: job dedicato `platform` in `verify-pr.yml`, path-filtered e non bloccante
   (`continue-on-error`, come `smoke`), fuori dal gate rapido per-change.
5. **Documentazione**: `tools/platform-e2e/README.md` — runbook diagnosi (dove sono trace/screenshot/
   video, come rilanciare un singolo journey, come ispezionare la casella Mailpit dopo un rosso).

## Fuori scope

- La batteria completa dei journey utente (UC 0091) e amministratore (UC 0092); il registro di
  copertura (UC 0093); l'integrazione nel workflow delle skill (UC 0094).
- Gli helper `paddle()` (fake overlay + webhook sintetici firmati) e `totp()` (codici 2FA): differiti ai
  loro **primi consumatori** (UC 0091/0092) — helper non esercitati da alcun journey sarebbero codice
  morto non collaudato. Tracciato nei punti aperti di UC 0090.
- La leva "tempo simulato" (decisa in UC 0091, come già annotato nello use case).
- Ogni fornitore esterno vero (resta al livello L3 pre-release).
- Nessuna modifica alle SPA, ai servizi o allo smoke esistente (solo rifattorizzazione conservativa).

## Criteri di accettazione

- [ ] `./run-tests.sh platform` esiste: orchestra stack vero + frontend costruiti + Mailpit e ritorna
      verdetto unico (exit ≠ 0 se un journey è rosso).
- [ ] J-REG verde: signup dal browser → email di verifica **realmente ricevuta** in Mailpit (mittente,
      oggetto in lingua, link) → click del link → onboarding workspace → dashboard con sidebar in stato
      "senza app attive" → assert DB (tenant/utente creati col `tenant_id` atteso, nessuna fuga).
- [ ] Convivenza: la suite gira con lo stack dev acceso (porte offset +12000) e una **doppia esecuzione
      consecutiva** passa senza pulizia manuale (idempotenza: email/tenant unici per run).
- [ ] Le aree esistenti restano verdi: `./run-tests.sh` completo non regredisce (lo smoke rifattorizzato
      sul lib condiviso si comporta come prima).
- [ ] Job CI `platform` presente in `verify-pr.yml` (non bloccante, come smoke).

## Invarianti appgrove toccati

- **Tenant ID solo dal JWT**: la suite lo **verifica** invece di introdurre superfici — J-REG osserva
  dal browser il comportamento reale e il leak-detector DB controlla che i dati stiano nel tenant.
- **Filtro row-level**: assert a livello database (nessuna riga del tenant sintetico fuori dal suo
  `tenant_id`).
- Modulo Terraform / logging strutturato: non toccati (nessun codice di produzione modificato).

## Requisiti di test

La suite **è** il test. Requisiti di qualità: determinismo (nessuna dipendenza esterna, tutto offline),
journey paralleli e indipendenti (tenant fresco ciascuno), diagnosi immediata (messaggi di fallimento
parlanti: "email di verifica non ricevuta in Mailpit entro Ns", mai un timeout anonimo), niente attese a
tempo fisso (solo polling su condizioni). Collaudo dell'orchestratore: doppia esecuzione consecutiva +
esecuzione con stack dev acceso.

## Valutazione di impatto

| Area | Impatto |
|---|---|
| Breaking change | No |
| Contratto cross-area | N/A (la suite consuma i contratti esistenti, non li cambia) |
| Version bump | nessuno |
