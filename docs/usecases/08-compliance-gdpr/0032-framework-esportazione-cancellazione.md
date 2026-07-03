# UC 0032 — Framework export/erasure (contratto per-app, job async EventBridge/SQS, zip S3 presigned)

**Area**: 08-compliance-gdpr · **Fase**: 6 · **Stato**: 🟢 deciso
**Dipendenze**: UC [0013](../04-platform-core/0013-account-utenti-inviti-api.md) (core), UC [0051](../11-apps/0051-app1-backend.md) (contratto per-app)
**Fonte decisioni**: #13 D/L (diritti/contratto per-app), #05 (soft/hard delete), #06 H (EventBridge/SQS)
**Ultimo aggiornamento**: 2026-06-21
**Aree collegate**: [13-compliance-privacy](../../13-compliance-privacy.md), [05-persistenza-dati](../../05-persistenza-dati.md), [06-infra-iac](../../06-infra-iac.md)

## 1. Obiettivo / Scope
Implementare il **framework di export/erasure** GDPR: contratto per-app + orchestrazione async + consegna sicura.
**Incluso**: **contratto GDPR per-app** (`exportData(scope)` con step per il progress + `purgeData(scope)`); **export job** async
(record QUEUED→RUNNING→COMPLETED/FAILED, progress) via **worker SQS**; **ZIP su S3** (privato, SSE, lifecycle auto-delete 7gg) +
**presigned URL 7gg** (link solo in-app); **purge** completa + audit; **orchestrazione account-level** (eliminazione account =
purge piattaforma + `purgeData` di ogni app attivata); **EventBridge purge per-tenant** (offboarding) = invocazione di `purgeData`.
**Escluso**: la UI self-service (UC 0033), la console admin diritti (UC 0034), i job retention/purge programmati (UC 0035).

## 2. Attori & ruoli
- **Utente**: richiede export/cancellazione (UI in UC 0033).
- **Piattaforma (core)**: orchestratore; invoca i contratti per-app.
- **Ogni app**: implementa `exportData`/`purgeData` (contratto obbligatorio).
- **EventBridge/SQS**: trasporto async (offboarding `tenant.offboarded`).

## 3. Precondizioni
- Contratto per-app implementato (UC 0051/0046); EventBridge bus + code SQS (UC 0004); bucket export S3 SSE (UC 0003); in locale MinIO/ElasticMQ (UC 0008).

## 4. Flusso principale
1. **Richiesta** → record **export job** (id, tipo/app, requested_at, status, progress step X/N) (#13 D22).
2. **Worker async (SQS)** esegue gli **step dichiarati dal contratto per-app** (`exportData`) aggiornando il progress; l'orchestratore aggrega (#13 D22).
3. Al completamento → **ZIP su bucket S3 dedicato** (privato, **SSE**, lifecycle **auto-delete 7gg**) + **presigned URL 7gg**; link **solo in-app** all'utente autenticato (#13 D22).
4. **Erasure**: `purgeData(scope)` cancella tutto (incl. cache/derivati) con **record di audit**; l'**EventBridge purge per-tenant** (offboarding) **è** l'invocazione di `purgeData` (#13 L70, #06 H).
5. **Account-level**: eliminazione account = purge piattaforma **+** `purgeData` di ogni app attivata (#13 L71).

## 5. Flussi alternativi / edge / errori
- **Diritti ESENTI dai gate** (#09 F31): export/erasure invocano **internamente** il contratto, non passano dall'API di business gateata → disponibili per tutta la retention anche con subscription canceled/app disabilitata.
- **Export FAILED** → ticket privacy auto-creato (UC 0034/ticketing) (#13 D21).
- **Anonimizzazione** (per-app, se dichiarata): solo se **irreversibile**; pseudonimizzazione **non** vale come erasure (guardrail #13 L72).
- **Grace cancellazione account 14gg** (UC 0035): account disattivato subito, hard-purge dopo 14gg, annullabile (#13 E25).

## 6. Risorse & runbook
**Risorse**: orchestratore core (export job), worker SQS, bucket export S3 (SSE+lifecycle 7gg), EventBridge bus + code purge.
**Runbook (dev)**: export/purge girano **davvero** in locale (MinIO+ElasticMQ, #11 15). **Audit** della purge registrato (#08/#13 L70).

## 7. Dati toccati
Legge/cancella i dati personali di piattaforma + app (per-tenant o per-utente). **ZIP export** = dati personali → bucket cifrato,
auto-delete 7gg, presigned in-app. **Purge** = hard-delete con audit (prova). Manifest: il contratto è collegato al manifesto per-app (#13 C15/L69).

## 8. Permessi & gate
- **Invarianti**: scope per-tenant/per-utente con `tenant_id` dal JWT; ownership verificata; nessun cross-tenant.
- **Esenzione gate** (#09 F31): capability di piattaforma, solo authN + ownership; mai bloccate da disable-app/entitlement.

## 9. Requisiti di test
- **Compliance** (#13 L74): export+purge coprono **ogni** entità con dati personali (no dati orfani); ArchUnit-style.
- **Integration**: export job (progress/ZIP/presigned/auto-delete); purge con audit; offboarding via EventBridge.
- **Security**: scope corretto (no cross-tenant); link export solo all'utente autenticato.

## 10. Riferimenti & Definition of Done
- **Decisioni**: #13 D19/D22/L69/L70/L71/L74, E25, #06 H, #09 F31.
- **DoD**:
  1. Contratto per-app `exportData`/`purgeData`; export job async (SQS) con progress.
  2. ZIP S3 SSE auto-delete 7gg + presigned in-app; purge con audit.
  3. Orchestrazione account-level + EventBridge purge per-tenant; diritti esenti dai gate.
  4. Compliance test (no dati orfani) verde; gira in locale (MinIO/ElasticMQ).

## Punti aperti / decisioni differite

- **Esenzione dai gate di enforcement: gli endpoint export/erasure NON devono annotare il gate** _(tracciato dalla change
  `0023-use-case-0027-…`)_. UC 0027 ha implementato il gate entitlement/quota in `commons` come **annotazione opt-in**
  (`@RequiresEntitlement`) proprio per soddisfare **per costruzione** l'esenzione GDPR (#09 F31): gli endpoint che
  esercitano export/erasure di questo UC **non** devono portare l'annotazione, così restano raggiungibili (solo
  authN+ownership) anche con `subscription` canceled/paused o quota esaurita, per tutta la retention. *Cosa va fatto qui:*
  (a) non annotare gli endpoint dei diritti GDPR; (b) aggiungere un **test** che verifica che export/erasure rispondono
  **anche senza accesso** (subscription non-grant) — il guard concreto di F31 che UC 0027 non ha potuto scrivere perché
  questi endpoint non esistevano ancora.
- **Riconciliare `DataManifest` (record Java della SPI) con i manifesti YAML bilingui** _(tracciato dalla change
  `0026-use-case-0030-…`)_. UC 0030 ha reso i **file YAML** (`docs/compliance/manifests/<app_id>.yaml`, lang-keyed)
  la **fonte unica** legale (RoPA/snippet); il record `DataManifest` di `commons.gdpr` (monolingua, derivato via
  reflection dalle annotazioni in `FattureDataContract.manifest()`) resta l'inventario tecnico della SPI export/purge.
  La coerenza oggi è **transitiva** (entrambi ancorati a `@PersonalData`, check UC 0030 su annotazioni↔YAML). *Cosa
  valutare qui:* se `manifest()` debba derivare dal YAML (o esserne validato: code-manifest ⊆ YAML), ed eventualmente
  sfoltire gli attributi testuali duplicati di `@PersonalData`. Differito perché l'interfaccia SPI è di questo UC.
