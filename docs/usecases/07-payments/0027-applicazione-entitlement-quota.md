# UC 0027 — Enforcement entitlement + quota SPI (flow/stock) runtime

**Area**: 07-payments · **Fase**: 5 · **Stato**: 🟢 deciso
**Dipendenze**: UC [0014](../04-platform-core/0014-authorizer-custom.md) (authorizer/gate edge), UC [0026](0026-ciclo-vita-abbonamento.md) (lifecycle)
**Fonte decisioni**: #09 F (enforcement/catena gate), #04 §7, #13 (diritti esenti)
**Ultimo aggiornamento**: 2026-06-21
**Aree collegate**: [09-pagamenti](../../09-pagamenti.md), [04-services-backend](../../04-services-backend.md), [13-compliance-privacy](../../13-compliance-privacy.md)

## 1. Obiettivo / Scope
Implementare l'**enforcement runtime** della catena di gate lato **servizio** (i gradini fini, complementari ai grossolani edge
dell'authorizer UC 0014) e l'**SPI di quota** (flow/stock).
**Incluso**: gate **ruolo** (`@RolesAllowed` → 403), **entitlement** sfumature stato/grace (402), **quota** via SPI
(`quota.checkAndReserve(metric)` → hard-limit **429**); natura **flow** (finestra/reset) vs **stock** (tetto sullo stato);
**diritti GDPR esenti** dai gate (2)/(3).
**Escluso**: i check grossolani edge (UC 0014), la semantica lifecycle (UC 0026), l'implementazione della metrica per-app (UC 0051/0046), il portal (UC 0028).

## 2. Attori & ruoli
- **Servizio** (piattaforma + app): applica i gate fini.
- **Piattaforma** fornisce l'**SPI di quota**; l'**app** lo chiama prima dell'azione che consuma quota.
- **Utente**: riceve 402/403/429 con messaggi azionabili.

## 3. Precondizioni
- Authorizer (UC 0014) ha già fatto authN + app-abilitata + entitlement grossolano; `subscription` aggiornata (UC 0025/0026); tier `limits` definiti (UC 0051/0022).

## 4. Flusso principale (catena #09 dec.30, lato servizio)
1. **Ruolo permesso?** `@RolesAllowed` → altrimenti **403** (#09 F30.4).
2. **Sfumature entitlement/stato/grace**: il servizio ri-valida (difesa in profondità) e gestisce le sfumature di stato → **402** dove serve (#09 F30.3, #04 §7).
3. **Entro quota?** l'app chiama l'**SPI** `quota.checkAndReserve(metric)` **prima** dell'azione che consuma quota → hard-limit → **429** "limite raggiunto, fai upgrade" (#09 F30.5).
4. **flow**: accumula nella finestra e **resetta**; **stock**: tetto sullo stato corrente (downgrade gated a monte, UC 0026) (#09 E23).
5. **Frontend = solo UX** (banner quota); il confine di enforcement è il backend (#09 F30).

## 5. Flussi alternativi / edge / errori
- **Quota raggiunta (flow)**: aspetta reset finestra **oppure** upgrade (#09 A6).
- **Quota (stock)**: l'azione che supererebbe il tetto è bloccata; per scendere di tier serve rientrare (#09 E23).
- **Diritti GDPR ESENTI** (#09 F31): export/erasure non passano dai gate (2)/(3); restano disponibili (authN+ownership) per tutta la retention anche con subscription canceled/app disabilitata.
- **Accesso off ≠ dati persi**: UX "abbonamento scaduto: riattiva **o** esporta/elimina i tuoi dati" (#09 F31).

## 6. Risorse & runbook  _(runtime)_
**SPI quota** in `commons`/piattaforma (faccia runtime del contratto generico A5/E23); ogni app la chiama. **Runbook**: il
co-pilota `new-application` (UC 0046) genera lo stub del contratto quota e chiede flow/stock per metrica. Osservabilità: 429/402/403
loggati con `tenant_id`/`app_id` (#08).

## 7. Dati toccati
Legge `subscription` (derivazione entitlement) + contatori quota per-app (nel `app_<id>` o derivati). Nessun dato carta. Manifest:
billing/quota (base contratto). Diritti GDPR sempre esercitabili (UC 0032/0033).

## 8. Permessi & gate
- **Catena gate completa (#09 dec.30)**: authN(401)→app-abilitata(403)→entitled(402) [edge UC 0014] → **ruolo(403)→quota(429)** [servizio, qui].
- **Invarianti**: `tenant_id` dal JWT; entitlement derivato; filtro row-level; **diritti GDPR esenti** (#09 F31).

## 9. Requisiti di test
- **L1/Integration**: 403 ruolo, 402 stato/grace, 429 quota (flow reset + stock tetto); ri-validazione servizio coerente con authorizer.
- **Security/multi-tenancy**: quota e entitlement per (tenant, app) corretti; **diritti GDPR non bloccati** dai gate (#09 F31).

## 10. Riferimenti & Definition of Done
- **Decisioni**: #09 A5/A6/E23/F30/F31, #04 §7, #13 (diritti).
- **DoD**:
  1. Gate servizio: ruolo(403), stato/grace(402), quota SPI(429) flow/stock.
  2. SPI quota chiamata dall'app prima del consumo; frontend solo UX.
  3. Diritti GDPR esenti dai gate per tutta la retention.
  4. Test catena gate (401/402/403/429) + isolamento verdi.
