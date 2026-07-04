# Change 0029: Self-service GDPR (UC 0033)

**Branch**: `change/0029-use-case-0033-self-service-gdpr`
**Aree**: `frontend` (backoffice), `services/core`
**Data**: 2026-07-04
**Autore**: Platform Engineering
**Use case sorgente**: [docs/usecases/08-compliance-gdpr/0033-self-service-gdpr.md](../../docs/usecases/08-compliance-gdpr/0033-self-service-gdpr.md)
**Tocca dati personali?**: Sì — si applica il gate privacy/RoPA di step-03 (UC 0031). Attesa: nessuna nuova
categoria/finalità (la change attua diritti già dichiarati, come la 0028); classificazione da confermare al gate.

## Problema / Obiettivo

Rendere esercitabili **dentro il prodotto** i diritti degli interessati (artt. 15–22) sopra il framework
export/erasure della change 0028 (UC 0032), che oggi espone solo API senza interfaccia: vista "I miei dati"
con export scaricabile, rettifica del profilo, eliminazione account con grazia di 14 giorni annullabile,
recesso per-app (esporta → conferma → cancella), diritti dichiarati (limitazione, art. 22).

## Scope

1. **"I miei dati" (artt. 15/20)** — nuova pagina nel backoffice:
   - vista dei propri dati di profilo (`GET /users/me`);
   - **export del profilo utente**: nuovo endpoint core `GET /api/platform/v1/users/me/export`, download
     JSON **sincrono** dei dati personali dell'utente nel core (profilo, appartenenza, inviti collegati) —
     capability del core, **senza interpellare le app** (decisione UC 0032, punto aperto UC 0033);
   - **export dell'account** (owner/admin): interfaccia sopra gli endpoint esistenti
     `POST/GET /gdpr/exports` + `/download` — avvio (completo o per-app), polling stato/progresso, link
     firmato con data/ora di scadenza (#13 D22.4); stato FAILED mostrato con invito a contattare il supporto;
   - dichiarazioni: **art. 22** (nessuna decisione automatizzata) e **limitazione art. 18** come diritto con
     canale di richiesta (contatto privacy).
2. **Rettifica (art. 16)** — nuovo `PATCH /api/platform/v1/users/me` (solo `displayName`) + modifica in UI.
3. **Elimina account con grazia 14 giorni (art. 17, #13 E25)** — solo `owner`:
   - `POST /api/platform/v1/account/deletion` (conferma esplicita) → account **disattivato subito**
     (stato in grazia, data di richiesta persistita); durante la grazia l'accesso alle app è bloccato ma
     l'utente può accedere per **annullare**;
   - `DELETE /api/platform/v1/account/deletion` entro 14 giorni → riattivazione;
   - **job schedulato** (scheduler applicativo, giornaliero): a grazia scaduta invoca
     `TenantOffboarding.offboard(tenantId, reason)` (hard-purge piattaforma + app, change 0028);
   - UI: conferma → countdown della grazia → annulla.
4. **Recedi da app (art. 17, #13 D19/E23)** — `owner`/`admin`, flusso **esporta → conferma → cancella
   immediata**: la UI avvia l'export per-app esistente; a export completato, nuovo endpoint core
   `POST /api/platform/v1/account/apps/{appId}/withdrawal` → disattiva l'app per l'account (entitlement/
   attivazione) e pubblica la purga sulla coda `tenant-purge-<appId>` (consumer e audit della change 0028).
5. **Esenzione dai gate (#09 F31)**: tutti gli endpoint sopra (e la pagina) restano disponibili con
   subscription scaduta/app disabilitata, come i `/gdpr/*` esistenti.

## Fuori scope

Tutti i rimandi sono tracciati nei file degli use case indicati (gate di chiarimento, 2026-07-04):

- **Unsubscribe marketing + centro preferenze consensi** → a valle di **UC 0039** (registro consensi
  inesistente; oggi nessun consenso raccolto). Tracciato su UC 0039 e UC 0033.
- **Cambio email con verifica** (rettifica) → **UC 0017** (+ UC 0058, ☁ 0015/0016). Tracciato su UC 0017 e 0033.
- **Gestione operativa della limitazione** (ticket, flag, evasione) e **ticket privacy su export FAILED**
  → **UC 0034**. Tracciato su UC 0034 e 0033.
- **Dati B2B verso il tenant-titolare** → tooling admin di **UC 0034** (già escluso dallo use case).
- **Trigger di schedulazione cloud** (EventBridge/cron) e restanti job retention (inattività 24 mesi,
  retention per-categoria, archivio audit) → **UC 0035**. La macchina della grazia è anticipata qui
  (passaggio di consegne annotato su UC 0035).

## Criteri di accettazione

- [ ] Pagina "I miei dati": profilo visibile e modificabile (nome), export profilo scaricato, export account
      avviato/monitorato/scaricato con scadenza visibile (E2E Playwright).
- [ ] Eliminazione account: conferma → disattivazione immediata; annullo entro la grazia → riattivazione;
      a grazia scaduta il job invoca l'offboarding (integration test con orologio controllabile).
- [ ] Recesso per-app: export completato → conferma → app disattivata + messaggio in coda purge con audit;
      rifiutato se l'export per-app non è completato (integration test).
- [ ] Sicurezza: ogni endpoint opera solo sui dati del chiamante (tenant dal JWT, `/me` dal `sub`);
      diritti fruibili senza subscription attiva (estensione `GdprGateExemptionTest`).

## Invarianti appgrove toccati

- **Tenant ID solo dal JWT**: tutti i nuovi endpoint leggono tenant/utente da `CallerContext`
  (`tenant_id`/`sub`); nessun identificatore dal body oltre `appId` (validato contro le attivazioni del tenant).
- **Filtro row-level**: query di profilo/account/withdrawal tenant-filtered come le esistenti; il job di
  purga opera per-tenant sullo scope persistito alla richiesta.
- **Modulo Terraform `microsaas_app`**: nessuna infra nuova (scheduler applicativo in locale; trigger cloud
  → UC 0035).
- **Logging strutturato**: richiesta/annullo eliminazione, scadenza grazia, withdrawal e job loggano
  `tenant_id`, `app_id` (dove pertinente), `user_id`.

## Requisiti di test

Dal §9 dello use case: E2E export+download, rettifica, elimina+grazia+annulla, recedi-app; security
(solo propri dati, diritti a subscription scaduta); compliance post-purge senza orfani (già coperta dai test
della change 0028, riusata dal withdrawal). Il job della grazia va testato con orologio iniettabile
(niente attese reali).

## Valutazione di impatto

| Area | Impatto |
|---|---|
| Breaking change | No (soli endpoint nuovi; `UpdateUser` admin invariato) |
| Contratto cross-area | Sì — frontend ↔ nuovi endpoint core (OpenAPI rigenerata) |
| Version bump | minor |
