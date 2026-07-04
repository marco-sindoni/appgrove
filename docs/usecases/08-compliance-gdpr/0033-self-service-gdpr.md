# UC 0033 — Self-service GDPR (export, rettifica, elimina account+grace, recedi-app, unsubscribe, consent center)

**Area**: 08-compliance-gdpr · **Fase**: 6 · **Stato**: 🟢 deciso
**Dipendenze**: UC [0032](0032-framework-esportazione-cancellazione.md) (framework export/erasure), UC [0020](../06-frontend/0020-shell-spa-backoffice.md) (shell)
**Fonte decisioni**: #13 D (diritti self-service), #13 F (consenso), #13 E (grace/retention)
**Ultimo aggiornamento**: 2026-06-21
**Aree collegate**: [13-compliance-privacy](../../13-compliance-privacy.md), [03-frontend](../../03-frontend.md)

## 1. Obiettivo / Scope
Implementare i **diritti degli interessati self-service** dentro il prodotto (artt. 15-22), alimentati dal framework export/erasure
e dal manifesto.
**Incluso**: **"I miei dati" + download export** (accesso/portabilità 15/20); **rettifica** in UI (16); **elimina account** (totale,
**grace 14gg** annullabile) **o** **recedi da app** (per-app: esporta→conferma→cancella immediata) (17); **opposizione** (21) =
**unsubscribe** marketing + canale per legittimo interesse; **centro preferenze consensi** (hub newsletter/futuri opt-in con revoca facile);
**limitazione** (18) manuale; **art. 22** = nessuna decisione automatizzata (dichiarato).
**Escluso**: il framework async (UC 0032), la console admin (UC 0034), i job retention (UC 0035); per i dati B2B la richiesta va al tenant-titolare (UC 0034 tooling).

## 2. Attori & ruoli
- **Utente/interessato**: esercita i diritti da solo.
- **Piattaforma**: per piattaforma + consumatori → gestisce **direttamente**; per dati app **B2B** → assiste il tenant-titolare (tooling admin, UC 0034) (#13 D20).

## 3. Precondizioni
- Framework export/erasure (UC 0032); shell/account (UC 0020); consent store (UC 0039); manifesto per-app (UC 0030).

## 4. Flusso principale
1. **"I miei dati"**: vista + **download export** (avvia export job UC 0032; link in-app a completamento) (#13 D19).
2. **Rettifica**: modifica profilo/dati in UI (CRUD) (#13 D19).
3. **Elimina account** (totale): conferma → **disattivazione immediata** + **hard-purge dopo 14gg**, **annullabile** entro il periodo (#13 E25); orchestrazione account-level (UC 0032).
4. **Recedi da app** (per-app): **esporta → conferma → cancella immediata** (post-export) (#13 D19/E23).
5. **Unsubscribe** (opposizione marketing) immediato + **centro preferenze consensi** (newsletter on/off, futuri opt-in, revoca facile) (#13 D19/F31).
6. **Limitazione (18)** = flag/sospensione manuale; **art. 22** N/A dichiarato (#13 D19).

## 5. Flussi alternativi / edge / errori
- **Diritti ESENTI dai gate** (#09 F31): export/elimina disponibili anche con subscription scaduta/app disabilitata (schermata "abbonamento scaduto" offre riattiva **+** esporta/elimina).
- **Annulla eliminazione** entro 14gg → account riattivato (#13 E25).
- **Dati B2B** (appgrove responsabile): la richiesta dell'interessato va al **tenant-titolare**; appgrove fornisce tooling admin (UC 0034) (#13 D20).
- **Export FAILED** → ticket privacy (UC 0034).

## 6. Schermate & stati
"I miei dati"/export (idle/in-corso/pronto con scadenza), rettifica, elimina account (conferma + countdown grace + annulla),
recedi-app (esporta→conferma), centro preferenze consensi, unsubscribe. Stati loading/empty/error; conferme per azioni distruttive.

## 7. Dati toccati
Profilo/account + dati app dell'utente; export ZIP (UC 0032); consent log (UC 0039). **Dati personali**: tutti quelli dell'utente.
Retention/grace per #13 E. Manifest: i diritti operano sui trattamenti dichiarati. `@PersonalData` rispettati.

## 8. Permessi & gate
- **Invarianti**: `tenant_id` dal JWT; l'utente esercita i diritti sui **propri** dati (ownership); nessun cross-tenant.
- **Esenzione gate** (#09 F31): export/erasure mai bloccati da entitlement/disable-app. Privacy by default (consensi opt-in, #13 66).

## 9. Requisiti di test
- **E2E**: export+download; rettifica; elimina account+grace+annulla; recedi-app (esporta→cancella); unsubscribe; centro consensi.
- **Security**: solo i propri dati; diritti disponibili a subscription scaduta.
- **Compliance**: post-recesso/eliminazione nessun dato orfano (con UC 0032).

## 10. Riferimenti & Definition of Done
- **Decisioni**: #13 D19/D20, E23/E25, F31/66, #09 F31.
- **DoD**:
  1. "I miei dati"+export, rettifica, elimina account+grace 14gg, recedi-app, unsubscribe, centro consensi.
  2. Diritti esenti dai gate (disponibili anche a subscription scaduta).
  3. Dati B2B instradati al tenant-titolare (tooling admin).
  4. E2E + security + compliance verdi.

## Punti aperti / decisioni differite

_Tracciato dalla change `0028-use-case-0032-…` (regola CLAUDE.md "Tracciamento delle decisioni differite")._

- **Export per-utente del profilo = capability del core, fuori dal contratto per-app**. UC 0032 ha deciso che
  `GdprScope` resta **a livello di account** (i dati delle app appartengono all'account, non al singolo
  operatore; i dati personali *dell'utente in quanto persona* — profilo, email, inviti — vivono nel core). Il
  diritto d'accesso/portabilità del **singolo utente** ai propri dati di profilo va quindi realizzato come
  funzione del core, senza interpellare le app: definire **qui** trigger e UX (vista "I miei dati" / download).
- **Trigger utente del recesso per-app (esporta → conferma → cancella immediata)**. UC 0032 fornisce la
  macchina interna (export job + purge per-app via coda) ma **non** espone endpoint di cancellazione: il
  flusso di recesso per-app (#13 D19/E23) va esposto qui, riusando l'orchestrazione della change 0028.
- **UI dell'export**: gli endpoint (richiesta export completo/per-app, stato/progress del job, link firmato con
  scadenza) esistono già dalla change 0028 sotto `/api/platform/v1/gdpr/*`: qui va costruita solo l'interfaccia
  (polling stato, link + data/ora scadenza, #13 D22.4).

_Tracciato dalla change `0029-use-case-0033-…`:_

- **Unsubscribe + centro preferenze consensi → rimandati a valle di UC 0039**. Le due funzioni presuppongono
  il registro dei consensi (consent store) che nasce con UC 0039 (oggi non esiste né newsletter né alcun
  consenso raccolto: il centro nascerebbe vuoto). Deciso al gate di chiarimento della change 0029 di non
  anticipare il registro fuori dal suo contesto. La parte in-app (toggle account, centro preferenze, revoca
  facile) è annotata su UC 0039 come requisito da consegnare lì.

- **Macchina della grazia 14 giorni anticipata qui (da UC 0035)**. Il rimando della change 0028 assegnava a
  UC 0035 stato in-grazia / annullamento / job schedulato, con il solo trigger UI qui: al gate di chiarimento
  della change 0029 si è deciso di **anticipare la macchina in questa change** (0033 precede 0035
  nell'ordine di esecuzione; i requisiti di test qui chiedono l'end-to-end elimina+grazia+annulla; la
  decisione di merito era già fissata da #13 E25). Dettaglio del passaggio di consegne annotato su UC 0035,
  sezione "Punti aperti".

- **Rettifica: cambio email rimandato ai flussi di autenticazione**. La change 0029 implementa la rettifica
  self-service del **nome visualizzato**; il **cambio email con verifica** è tracciato su UC 0017 (sezione
  "Punti aperti") con UC 0058 / ☁ UC 0015-0016 per gli endpoint: l'email è identificatore di accesso nel
  provider di identità, il flusso è materia auth. Nel frattempo la rettifica dell'email è esercitabile via
  supporto.

- **Limitazione (art. 18): gestione operativa rimandata a UC 0034**. La change 0029 la espone come diritto
  dichiarato con canale di richiesta (contatto privacy) nella pagina dei diritti, insieme alla
  dichiarazione art. 22; presa in carico, flag di limitazione e prova di evasione sono della console
  "Diritti GDPR" (UC 0034, sezione "Punti aperti"). Nessun flag self-service: diritto eccezionale, valutato
  caso per caso.
