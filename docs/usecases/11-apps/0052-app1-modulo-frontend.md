# UC 0052 — App #1 frontend module (React lazy, manifest registry, UI)

**Area**: 11-apps · **Fase**: 4 · **Stato**: 🟢 deciso
**Dipendenze**: UC [0020](../06-frontend/0020-shell-spa-backoffice.md) (shell), UC [0051](0051-app1-backend.md) (backend app #1)
**Fonte decisioni**: #03 (frontend/contratto moduli), #01 (App Registry/contratto shell↔app)
**Ultimo aggiornamento**: 2026-06-21
**Aree collegate**: [03-frontend](../../03-frontend.md), [01-architettura](../../01-architettura.md)

## 1. Obiettivo / Scope
Implementare il **modulo frontend della prima app** come componente React lazy montato nella shell.
**Incluso**: modulo nel workspace `frontend/`; **manifest co-locato** (`{id, sezioni sidebar, route interne, metadata}` +
componente lazy); registrazione nell'**App Registry** (∩ entitlement → sidebar); UI dell'app (es. lista/editor fatture) con
design system (UC 0019); client API da OpenAPI (UC 0051); stati loading/empty/error; banner quota.
**Escluso**: il backend (UC 0051), la landing (UC 0053), l'admin (UC 0021), il checkout/billing (UC 0024/0028).

## 2. Attori & ruoli
- **Utente B2C**: usa il modulo dentro il backoffice.
- **Shell** (UC 0020): monta il modulo via React Context (token/tenant/ruoli/theme/nav).

## 3. Precondizioni
- Shell (UC 0020); backend app #1 (UC 0051) con OpenAPI; entitlement attivo per il tenant (subscription, UC 0025).

## 4. Flusso principale
1. **Manifest** del modulo: id, sezioni sidebar, route interne, metadata (#03 6).
2. **Registrazione** nel registry → la sidebar mostra l'app solo se **entitled** (∩, #01 10).
3. **Componente lazy**: caricato on-demand; riceve il **contesto** dalla shell (token getter, `tenant_id`, `user_id`, ruoli, theme, nav) — **non** gestisce auth né legge `tenant_id` fuori dal contesto (#01 11).
4. **UI**: schermate dell'app (es. fatture) con design system, **client da OpenAPI** (Bearer, 401→refresh→retry, problem+json) (#03 5).
5. **Banner quota**: mostra consumo/limite; a quota raggiunta CTA upgrade (l'enforcement vero è backend, #09 F30).

## 5. Flussi alternativi / edge / errori
- **Non entitled**: l'app non compare; route diretta → guard UX + 402 backend (UC 0014/0020).
- **Quota 429**: messaggio "limite raggiunto, fai upgrade" (#09 A6); frontend solo UX.
- **problem+json** → errori tipizzati con stati empty/error (#03 5).
- **Estrazione microfrontend futura**: tocca solo la entry del registry (#01 11).

## 6. Schermate & stati
Schermate dell'app (es. lista fatture, editor) in light/dark, responsive; stati loading/empty/error/success; banner quota.
Coerenti col design system (UC 0019) e col chrome della shell.

## 7. Dati toccati
Nessuna persistenza client; legge/scrive via API backend (filtrate per tenant lato server). Mostra contenuti dell'app
(dati personali dell'utente) ricevuti dall'API. Manifest: nessun nuovo trattamento (i dati sono dichiarati in UC 0051).

## 8. Permessi & gate
- **Invarianti**: il modulo è **solo UX**; non legge `tenant_id` se non dal contesto; enforcement nel backend (#09 dec.30).
- **Route guard** `requireEntitlement(app_id)` come difesa in profondità UX (#03 8).

## 9. Requisiti di test
- **Component** (Vitest+RTL+MSW): rendering condizionale per entitlement/ruolo/quota; stati query.
- **E2E** (Playwright, #10 F): core-loop dell'app come utente B2C entitled; banner quota; app non visibile se non entitled.
- **a11y** (axe) sulle schermate chiave.

## 10. Riferimenti & Definition of Done
- **Decisioni**: #03 5/6/8, #01 10/11, #09 A6/F30.
- **DoD**:
  1. Modulo React lazy + manifest registrato nell'App Registry (∩ entitlement).
  2. UI app #1 con design system + client OpenAPI; banner quota.
  3. Solo UX (enforcement backend); route guard difesa in profondità.
  4. Component + E2E + a11y verdi.
