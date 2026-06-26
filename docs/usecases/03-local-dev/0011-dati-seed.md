# UC 0011 — Seed data deterministico (condiviso dev↔E2E)

**Area**: 03-local-dev · **Fase**: 0 · **Stato**: 🟢 deciso
**Dipendenze**: UC [0008](0008-stack-sviluppo-locale.md) (stack Compose)
**Fonte decisioni**: #11 D (seed), #10 I (test data/seed)
**Ultimo aggiornamento**: 2026-06-21
**Aree collegate**: [11-developer-experience](../../11-developer-experience.md), [10-testing](../../10-testing.md), [05-persistenza-dati](../../05-persistenza-dati.md)

> **Aggancio da change 0002 (UC 0008).** Il seed gira contro le dipendenze già definite in `dev/docker-compose.yml`:
> **Postgres** (`postgres:17`) per il cast multi-tenant/catalogo e **MinIO** per eventuali asset/export. Nessun blocco da
> scommentare qui; il seed è invocato dagli script `dev seed`/`dev reset` (UC 0009) sul DB di questo stack.

> **Open point da change 0004 (UC 0009).** `dev/lib/seed.sh` è uno **stub** (`cmd_seed` stampa solo un rimando) e
> `dev/lib/reset.sh` lo richiama al passo "3/3 reseed". Questo UC deve implementare il caricamento reale del seed in
> `dev/lib/seed.sh` (idempotente, ID stabili), così `./dev.sh seed` e `./dev.sh reset` popolano davvero il DB.

## 1. Obiettivo / Scope
Definire un **unico set di seed deterministico, idempotente, versionato** (ID stabili) condiviso tra **dev locale** ed
**E2E Playwright**, così i flussi sono riproducibili (#11 D12, #10 I32/33).
**Incluso**: il cast multi-tenant, il catalogo (`app`/`app_tier`/`app_price`), le `subscription` in stati di lifecycle
vari (per esercitare l'**entitlement derivato**, #09 dec.12), inviti pending, un platform-admin.
**Escluso**: il meccanismo di caricamento (`dev seed`/`reset`, UC 0009); i builder di fixture per integration test
(self-contained, #10 I31, distinti dal seed).

## 2. Attori & ruoli
- **Developer / suite E2E**: consumano lo stesso seed.
- **Sistema (seed runner)**: applica i dati in modo idempotente.
- Cast seedato: tenant **B2B** (owner/admin/member), tenant **B2C** (single-user), **platform-admin**.

## 3. Precondizioni
- Stack locale attivo (UC 0008), schemi creati e migrazioni applicate (`dev migrate`, UC 0009).

## 4. Flusso principale
1. `dev seed` (o setup) esegue il seed **idempotente** con **UUID fissi** (#10 I32).
2. Crea il cast: **Acme** (B2B, owner+admin+member), **Bob** (B2C single-user), **platform-admin** (#10 I32, #11 D12).
3. Popola il **catalogo**: app demo (una single-user, una multi-user), tier con `limits` (metrica/finestra/tetto, flow e
   stock) e `app_price` monthly/annual (#05 A7, #09 B10).
4. Crea **subscription** in stati diversi: `trialing`, `active`, `past_due`, una con **app disabilitata** dall'admin → così
   l'**entitlement derivato** e la **catena di gate** (#09 dec.30) sono esercitabili end-to-end.
5. Crea **inviti pending** per il tenant B2B.

## 5. Flussi alternativi / edge / errori
- **Reset**: `dev reset` (UC 0009) azzera e riapplica → stato identico, deterministico.
- **Multi-tenant per isolamento**: ≥2 tenant per testare la matrice cross-tenant (#10 D8) — A non vede i dati di B.
- **Auto-wiring nuova app**: lo scaffold `new-application` (UC 0046) genera il **seed-base** della nuova app (#11 11, #10 I32).
- **Stati edge**: includere almeno un `canceled`/`paused` per verificare "no accesso" (#09 dec.29).

## 6. Risorse & runbook
**Risorsa**: codice seed versionato nel repo (es. `dev/seed/` o modulo dedicato), eseguito da `dev seed`. **ID stabili**
documentati (gli E2E ci asseriscono sopra). **Runbook**: `dev seed` per caricare, `dev reset` per ripristino pulito.
**Composizione minima**:

| Tenant | Tipo | Utenti | Subscription (stato) |
|---|---|---|---|
| Acme | B2B multi-user | owner, admin, member | app multi-user `active`; una `past_due` |
| Bob | B2C single-user | owner | app single-user `trialing` |
| (varie) | — | — | una app `disabled-by-admin`, una `canceled` |

## 7. Dati toccati
Schema `platform` (`accounts`, `users`, `invitations`, catalogo, `subscription`) + eventuali schemi `app_<id>` demo.
**Dati 100% sintetici** (email `*.test`, nessun PII reale) — coerente #08/#13/#10 I33. Manifest GDPR N/A (sintetico).

## 8. Permessi & gate
- **Invarianti**: il seed crea **≥2 tenant** apposta per testare il **filtro row-level** e il fail-closed; `tenant_id` =
  account id su ogni riga tenant-scoped. Le subscription seedate alimentano l'**entitlement derivato** (nessuna tabella entitlements).
- Esercita tutti i gradini della catena gate (app abilitata/entitlement/ruolo/quota) tramite gli stati seedati.

## 9. Requisiti di test
Il seed **è** il dato di base degli E2E (#10 F/I) e utile in dev. DEVE risultare: idempotente (riesecuzione = stesso stato);
ID stabili; copre B2C + B2B + ruoli + stati subscription (trial/active/past_due/canceled/disabled) per esercitare gate ed
entitlement derivato; dati sintetici senza PII.

## 10. Riferimenti & Definition of Done
- **Decisioni**: #11 12, #10 31/32/33, #05 7, #09 10/12/29/30.
- **DoD**:
  1. Esiste un seed deterministico/idempotente/versionato con ID stabili, condiviso dev↔E2E.
  2. Copre il cast multi-tenant + catalogo + subscription in stati vari (entitlement derivato).
  3. Dati 100% sintetici (no PII).
  4. `new-application` genera il seed-base della nuova app.

## Punti aperti / decisioni differite

_Tracciato dalla change `0007-use-case-0013-…` (regola CLAUDE.md "Tracciamento delle decisioni differite")._

- **Ordinamento indice: UC 0011 deve seguire UC 0013.** L'indice di esecuzione [_INDEX.md](../_INDEX.md) colloca
  UC 0011 (seed, riga 7) **prima** di UC 0013 (riga 9), ma il seed popola le tabelle `accounts`/`users`/`invitations`/
  catalogo/`subscription` il cui DDL Flyway è di proprietà di UC 0013. Un seed non può precedere il DDL delle tabelle
  che popola: la dipendenza hard di 0011 è **0013**, non solo 0012. **Fix differito**: ri-ordinare l'indice (0011 dopo
  0013) — da applicare quando si apre la change del seed (UC 0011). **Proprietario**: UC 0011 (indice local-first).
