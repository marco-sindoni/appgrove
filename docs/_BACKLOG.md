# Backlog — topic trasversali da discutere

Lista dei temi sollevati durante le sessioni di decisione, da affrontare nell'argomento giusto (o in uno dedicato).

## Compliance & privacy (GDPR) — richiesto 2026-06-14
Probabilmente merita un **documento dedicato** (nuova area, es. `13-compliance-privacy.md`). Da coprire:
- **GDPR**: basi giuridiche, data retention, diritto all'oblio/erasure (già impostato a livello dati in [05-persistenza-dati](05-persistenza-dati.md) §12), portabilità, DPA.
- **Tracciamento dati comportamentali utenti**: cosa si traccia, consenso/cookie banner, anonimizzazione, finalità.
- **Log**: PII nei log, retention, masking (border con [08-observability](08-observability.md)).
- **Funzionalità GDPR dentro le applicazioni**: export/cancellazione dati per-tenant/per-utente lato app.
- **Privacy Policy & Terms and Conditions**: tenuto conto che **Paddle è Merchant of Record** (gestisce
  tax/fatturazione, ma privacy/T&C del servizio restano a noi) — border con [09-pagamenti](09-pagamenti.md).

## Configurazione admin — richiesto 2026-06-14
Già parcheggiato negli scope di [03-frontend](03-frontend.md) (pannello admin in generale) e
[09-pagamenti](09-pagamenti.md) (config admin del modello di costo per-app).

## Dettaglio funzionalità / use case — richiesto 2026-06-14
Le decisioni di [03-frontend](03-frontend.md) (e affini) fissano stack/architettura/UX a grandi linee. Resta da
**progettare in dettaglio tutti gli use case** delle varie funzionalità (backoffice, moduli app, console admin):
flussi, schermate per stato, edge case, validazioni, permessi per ruolo. Da affrontare in sessioni dedicate per area/app.

- **PRIORITARIO (richiesto 2026-06-16, ricordare a fine #06)**: casi d'uso di **autenticazione e registrazione** del
  frontend — signup self-service (crea account+owner), verifica email, login, refresh/logout, reset password, accept-invite
  (multi-utente), gestione errori. Allineati a #02 (auth Lambda, cookie, ruoli) e al design v1.

## Script / tooling DevOps
- **Start/stop servizi test** (scale 0↔1 task Fargate) — script semplice per riaccendere/spegnere l'ambiente test
  on-demand (cost-min: test a 0 task da idle, #06 C). Da realizzare con #07 (DevOps/CI-CD).

## Skill Claude Code da creare — richiesto 2026-06-14
Es. **setup-nuova-applicazione** (scaffold servizio + modulo frontend + entry catalog/registry + schema/migration
+ modulo Terraform). Dettaglio in memoria `skills-backlog`. Da affrontare con tooling/DX ([11-developer-experience](11-developer-experience.md)) o in sessione dedicata.
