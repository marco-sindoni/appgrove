# Change 0070: Batteria journey end-to-end lato utente (suite di piattaforma)

**Branch**: `change/0070-use-case-0091-e2e-journey-utente`
**Aree**: tools/platform-e2e (journey, helper, config, orchestratore) · docs (indici e rimandi)
**Data**: 2026-08-01
**Autore**: Platform Engineering (modalità fast, autopilot)
**Use case sorgente**: [docs/usecases/20-test-e2e-piattaforma/0091-e2e-platform-journey-utente.md](../../docs/usecases/20-test-e2e-piattaforma/0091-e2e-platform-journey-utente.md)
**Tocca dati personali?**: No — la suite usa esclusivamente dati sintetici (email su dominio non recapitabile
`test.appgrove.local`) nel database locale usa-e-getta; nessun manifesto, RoPA o annotazione `@PersonalData` da toccare.

## Problema / Obiettivo

La suite di piattaforma (UC 0090) oggi ha un solo journey (J-REG, registrazione). Mancano i percorsi utente
end-to-end sul resto della piattaforma esistente: acquisto e attivazione, uso dell'app con limite di quota,
inviti e ruoli, ciclo di vita dell'abbonamento, credenziali e secondo fattore, diritti sui dati, ri-accettazione
legale. Obiettivo: al termine, `./run-tests.sh platform` dà la certezza di non avere regressioni sui percorsi
utente end-to-end della codebase attuale (stack vero, email vere via Mailpit, pagamenti su stub Paddle con
webhook firmati sulla pipeline reale).

## Scope

Sette journey in `tools/platform-e2e/journeys/`, ognuno indipendente con tenant creato da zero, ID stabili:

- **J-BUY** — acquisto e attivazione: catalogo (`/billing`) → tier a pagamento → overlay stub Paddle →
  webhook sulla pipeline reale (ingest → coda → consumer) → polling "attivata" → sidebar YOUR APPS → il modulo
  si monta e risponde. Assert DB: subscription col `tenant_id` giusto, nessuna riga per altri tenant.
  App usata: **Mini-CRM** (tier `team` a pagamento), attivata a inizio suite con la leva admin sanzionata
  (`PATCH /api/platform/v1/admin/apps/{id}`, platform-admin del seed) — vedi decisioni 3.
- **J-QUOTA** — quota a consumo (flow) su `fatture`: creazione fatture fino al tetto del tier free (10/mese),
  banner consumo che avanza, 429 al superamento con invito all'upgrade (schermata di creazione + banner globale
  con CTA verso `/billing`). Assert DB: il conteggio reale delle fatture del mese riflette il consumo.
  Il ramo "upgrade → la creazione riesce di nuovo" si esercita sulla metrica a giacenza del crm dentro
  J-MEMBERS (fatture non ha tier a pagamento; rimando scritto in UC 0091 — decisione 4).
- **J-MEMBERS** — inviti e ruoli (B2B, crm): invito → **email di invito reale** (Mailpit) → accettazione in
  **seconda sessione browser** (password, auto-login come member) → posti (seat) del crm: assegnazione fino al
  tetto free (2) → 429 + invito upgrade → acquisto tier `team` → il webhook aggiorna → l'assegnazione riesce.
  Cambio ruolo e rimozione dall'owner; il membro rimosso perde l'accesso (assert dalla sua sessione).
  Protezione ultimo owner (assert UI: nessun cambio ruolo, rimozione disabilitata).
- **J-SUB** — ciclo di vita abbonamento (crm `team`): downgrade programmato a fine periodo (self-service, stato
  mostrato) → disdetta (stato mostrato) → fine periodo simulata con **webhook sintetico firmato**
  `subscription.canceled` → 402 e stato "scaduta" con azioni riattiva / esporta-cancella; modulo non
  raggiungibile ma dati intatti (assert DB) → riattivazione via checkout → accesso e dati ripristinati.
- **J-PWD** — credenziali e secondo fattore: reset password (richiesta → risposta neutra → **email reale** →
  link → nuova password → accesso; la vecchia password non vale più). Attivazione 2FA dalla pagina Security
  (enroll → secret → codice generato dall'helper `totp()` → conferma) e login con challenge reale del secondo
  fattore (bypass dev disattivato per il solo servizio auth della suite — decisione 6).
  L'assert sull'invalidazione dei refresh dopo il reset è escluso: il provider locale non la implementa
  (deviazione tracciata — decisione 7).
- **J-PRIVACY** — diritti sui dati: rettifica nome (visibile in UI e su DB) → export account con job asincrono
  reale (code per-servizio + aggregatore, archivio ZIP su MinIO, link presigned con scadenza, **download
  effettivo e validazione del contenuto**: dati del tenant, nessun dato di altri tenant) → recesso per-app
  (esporta → conferma → purge reale su coda con audit su DB) → eliminazione account (grace con scadenza
  mostrata, stato `pending_deletion` su DB, annullamento che ripristina).
- **J-LEGAL** — ri-accettazione legale: leva d'ambiente (UPSERT su `platform.legal_version`, major+1 dei
  Termini — simulazione dell'atto di pubblicazione, non azione utente) → al rientro la schermata bloccante;
  `/privacy` resta raggiungibile (esenzione GDPR) → lettura documento → spunta → accettazione → ingresso;
  assert DB su `platform.legal_acceptance` (chi, cosa, quando). Journey **serializzato dopo gli altri**
  (progetto Playwright dedicato con dipendenza): la leva è globale a tutti i tenant.

Estensioni di contorno richieste dai journey:

- **helper nuovi** in `tools/platform-e2e/helpers/`: `paddle` (firma HMAC dei webhook sintetici + invio
  sull'ingest reale + acquisto via API checkout con attesa attivazione), `totp` (codici RFC 6238 coerenti col
  servizio auth), estensioni a `api`/`db` (accesso browser con attraversamento del gate legale, scrittura DB
  **limitata alla leva d'ambiente legale**);
- **orchestratore** `run.sh`: avvio anche di `minio`/`minio-init` (senza MinIO l'export GDPR fallisce sempre) e
  disattivazione del bypass 2FA per il servizio auth della suite; `global-setup` Playwright che attiva il crm
  con la leva admin una volta per run;
- **documentazione**: README della suite aggiornato (tabella journey ↔ funzionalità coperta, runbook rilancio
  singolo journey); rimandi nei punti aperti degli use case interessati; `EPICS-WAVE-2.md` → ✅ per UC 0091.

## Fuori scope

- Journey amministratore e guasti di piattaforma (UC 0092); registro di copertura (UC 0093); integrazione nel
  workflow delle skill (UC 0094).
- Nuove funzionalità o modifiche di prodotto: nessun cambio a pricing (`crm.yaml` resta `inactive`), nessun tier
  nuovo per `fatture`, nessuna invalidazione dei refresh nel provider locale, nessuna email nuova di piattaforma.
- Rimozione di spec L2: le spec esistenti sono il "percorso felice minimo" che il criterio di riparto (§1 dello
  use case) consente di mantenere in entrambe le sedi; nessuna duplicazione da rimuovere (decisione 10).
- Journey di localizzazione end-to-end (già differito nei punti aperti di UC 0091).

## Criteri di accettazione

- [ ] I 7 journey (J-BUY, J-QUOTA, J-MEMBERS, J-SUB, J-PWD, J-PRIVACY, J-LEGAL) esistono in
      `tools/platform-e2e/journeys/`, con ID stabile nel nome file/test, e sono verdi nel comando unico
      `./run-tests.sh platform`.
- [ ] Ogni journey crea da zero il proprio tenant, gira in parallelo con gli altri (J-LEGAL serializzato dopo,
      per la leva globale) e chiude con gli assert leak-detector sul `tenant_id`.
- [ ] Le assert esterne distintive ci sono: email reali lette da Mailpit (invito, reset), webhook sintetici
      firmati sulla pipeline reale (fine periodo), download effettivo dell'export con validazione del contenuto,
      assert DB su subscription/quota/purge/accettazioni.
- [ ] Nessuna attesa a tempo fisso: solo polling su condizioni; fallimenti con messaggi parlanti; retry ≤ 1.
- [ ] Doppia esecuzione consecutiva verde (idempotenza: tenant/email unici per run; la sync legale allo startup
      riallinea la versione bumpata).
- [ ] Criterio di riparto piattaforma/L2 applicato e documentato (decisione registrata; nessuna spec L2 rimossa).
- [ ] Suite completa `./run-tests.sh` verde; indice epiche (`EPICS-WAVE-2.md`) aggiornato a ✅ per UC 0091.

## Invarianti appgrove toccati

La change non introduce superfici runtime: i journey **verificano** gli invarianti — `tenant_id` solo dal JWT
(assert sul claim vs DB), filtro row-level (leak detector: nessuna riga fuori dal tenant), gate di
entitlement/ruolo/quota osservati dal browser. Nessun codice di prodotto modificato.

## Requisiti di test

La suite **è** il test. Requisiti di qualità: determinismo (zero dipendenze esterne), tempo totale della
batteria compatibile col target < 10 minuti di UC 0090 (se sforato, motivare e registrare), diagnosi immediata
(trace/screenshot/video conservati su fallimento, verdetto che elenca i journey rossi per ID).

## Valutazione di impatto

| Area | Impatto |
|---|---|
| Breaking change | No |
| Contratto cross-area | N/A (la suite consuma i contratti esistenti, non li cambia) |
| Version bump | nessuno |
