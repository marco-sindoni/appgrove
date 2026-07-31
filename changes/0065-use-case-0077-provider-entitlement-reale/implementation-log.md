# Implementation Log — Change 0065: Provider entitlement reale — chiusura dei punti scoperti

**Branch**: `change/0065-use-case-0077-provider-entitlement-reale`
**Aree**: `services/core`, `frontend/`
**Completata**: 2026-07-30
**Modalità**: autopilot — le risposte alle domande di approfondimento sono dell'agente e sono tracciate in
[decisions.json](decisions.json) (25 decisioni, 21 in autopilot); i tre gate sono rimasti dello sviluppatore.

## File modificati

| File | Azione |
|---|---|
| `services/core/src/main/java/app/appgrove/core/billing/EntitlementAccess.java` | Creato |
| `services/core/src/main/java/app/appgrove/core/billing/EntitlementReadModel.java` | Modificato |
| `services/core/src/main/java/app/appgrove/core/billing/CheckoutResource.java` | Modificato |
| `services/core/src/main/java/app/appgrove/core/platform/AdminResource.java` | Modificato |
| `services/core/src/test/java/app/appgrove/core/billing/EntitlementAccessTest.java` | Creato |
| `services/core/src/test/java/app/appgrove/core/billing/EntitlementCoherenceTest.java` | Creato |
| `frontend/apps/backoffice/src/registry/entitlementsApi.ts` | Modificato |
| `frontend/apps/backoffice/src/registry/entitlements.tsx` | Modificato |
| `frontend/apps/backoffice/src/shell/Sidebar.tsx` | Modificato |
| `frontend/apps/backoffice/src/routing/guards.tsx` | Modificato |
| `frontend/apps/backoffice/src/billing/subscriptionsApi.ts` | Modificato |
| `frontend/apps/backoffice/src/pages/Billing.tsx` | Modificato |
| `frontend/apps/backoffice/src/test/utils.tsx` | Modificato |
| `frontend/apps/backoffice/src/registry/entitlementsApi.test.ts` | Modificato |
| `frontend/apps/backoffice/src/registry/entitlementsFreshness.test.tsx` | Creato |
| `frontend/apps/backoffice/src/shell/Sidebar.test.tsx` | Modificato |
| `frontend/apps/backoffice/src/routing/navigation.test.tsx` | Modificato |
| `frontend/apps/backoffice/e2e/shell.spec.ts` | Modificato |
| `frontend/apps/backoffice/e2e/auth.spec.ts` · `checkout.spec.ts` · `members.spec.ts` | Modificati |
| `frontend/packages/i18n/src/resources/{en,it,fr,es,de}.ts` | Modificati |
| `docs/usecases/15-supporto-e-piattaforma/0077-provider-entitlement-reale.md` | Modificato |
| `docs/usecases/EPICS-WAVE-2.md` | Modificato |
| `docs/_BACKLOG.md` | Modificato |

## Cosa è stato fatto

Il cuore dello use case 0077 era **già in `main`**, consegnato in anticipo da UC 0027: l'endpoint del tenant
`GET /api/platform/v1/me/entitlements` e il consumo reale lato backoffice esistevano, con lo stub confinato ai test.
La change non li ha riscritti: ha chiuso i **quattro punti scoperti** della Definition of Done.

Lato backend è stata estratta la **regola unica di accesso** (`EntitlementAccess`) — account non in attesa di
eliminazione ∧ app attiva ∧ (abbonamento che concede accesso, oppure fascia gratuita di baseline se non c'è
abbonamento) — e la consumano tutti e tre i punti che prima la riscrivevano per conto proprio: il read-model del
tenant, la matrice della console admin (riscritta su `account × app`, così vede anche le app abilitate dalla fascia
gratuita e azzera gli account in eliminazione) e lo stato usato dal polling post-acquisto (che prima avrebbe
dichiarato "attiva" un'app disabilitata dalla piattaforma).

Lato frontend la shell distingue ora i **quattro stati** previsti — caricamento, errore con riprova, vuoto con
invito all'acquisto, pronto — sia nella barra laterale sia sulla rotta di un modulo, dove un errore di lettura non
diventa più un diniego di accesso. La lettura degli entitlement viene invalidata dopo attivazione post-acquisto,
cambio fascia, disdetta e ripresa, e riletta al ritorno sulla scheda del browser e alla riconnessione di rete.

## Decisioni prese

Condotta in **autopilot**: tutte le domande di approfondimento hanno avuto risposta dall'agente. Registro completo e
strutturato in [decisions.json](decisions.json); in sintesi:

- **Non rifare ciò che esiste** (dec. 3, 5). L'endpoint realizzato è `GET /api/platform/v1/me/entitlements`, non
  `/entitlements` come ipotizzato nello use case: resta quello, nessun secondo endpoint.
- **Una regola, tre consumatori** (dec. 8, 12–15). La regola è una classe di sola decisione, senza stato e senza
  accesso al database: i chiamanti raccolgono gli ingredienti come è naturale per loro — entità nel percorso del
  tenant, SQL nativo nella superficie admin cross-account — e chiedono qui il verdetto. Centralizzare anche la
  lettura avrebbe costretto uno dei due a un accesso innaturale o ad aggirare il filtro per tenant.
- **Due endpoint, una derivazione** per il polling post-acquisto (dec. 9, 15). La fascia gratuita non entra nella
  domanda del polling: un'app gratuita avrebbe risposto "sì" prima che il webhook materializzasse l'acquisto.
- **Freschezza** (dec. 7, 11). Invalidazione esplicita dopo le azioni dell'utente, più rilettura al ritorno sulla
  scheda e alla riconnessione — dichiarata solo su questa lettura, non riattivata in generale. Scartata la rilettura
  periodica (chiamata continua per un evento raro).
- **Correzione di scope in fase di rilettura dei requisiti** (dec. 11), su osservazione dello sviluppatore: il rimando
  iniziale sull'aggiornamento del menu era più largo del necessario e buttava fuori anche la parte risolvibile a
  costo quasi nullo (il ritorno sulla scheda). Portata in scope; resta fuori la sola notifica in tempo reale.
- **Difetto trovato dalle prove end-to-end** (dec. 22). Gli stati esposti alla shell non sono quelli grezzi della
  libreria delle richieste: quando una lettura fallita viene ritentata senza dati, la libreria torna "in attesa" e
  azzera l'errore — la guardia di rotta smontava la pagina aperta a ogni ritentativo, e per un istante l'elenco vuoto
  passava per "non hai diritto a nulla", cioè lo stesso difetto rientrato da un'altra porta. Gli stati sono ora
  derivati da una funzione pura, provata caso per caso.
- **Regressione trovata dallo sviluppatore sullo stack locale, e corretta** (dec. 24). L'applicazione esplodeva
  all'apertura con "Maximum update depth exceeded". Riprodotta guidando un browser sullo stack locale: veniva dal
  componente della rotta protetta. Con la sessione anonima la lettura degli entitlement è disabilitata e non sarà
  **mai** conclusa; dichiararla perennemente in caricamento teneva la rotta montata a tempo indeterminato invece di
  redirigere al login. La regola diventa "in corso **e** nessun esito precedente". Nessuna prova la copriva perché
  quelle di navigazione usano lo stub, sempre "concluso": aggiunta una prova end-to-end col provider reale e sessione
  anonima, **verificata sensibile** (reintroducendo il difetto, fallisce).
- **Difetto preesistente emerso e corretto** (dec. 25). Nelle guardie di rotta il selettore dei ruoli dello store
  creava un array nuovo a ogni lettura — condizione che fa ciclare il render all'infinito, la stessa trappola già
  evitata di proposito nella barra laterale. Restava innocua solo perché la rotta si smontava subito: una trappola
  armata. Corretta qui perché è nel file che si stava modificando e perché era ormai un difetto noto e riprodotto.

## Invarianti appgrove

- **Tenant dal token verificato** — invariato: il read-model e lo stato del polling ricavano l'account dal claim
  verificato, mai da corpo o parametri. La regola condivisa **non** prende l'account come parametro nel percorso del
  tenant: riceve solo il suo *stato*, letto da chi ha già risolto il tenant dal token. La matrice admin resta
  l'eccezione cross-account già documentata, riservata al ruolo `platform-admin` e in sola lettura.
- **Filtro riga per riga** — invariato nel percorso del tenant (letture degli abbonamenti tenant-scoped). L'apertura
  della matrice admin alla fascia gratuita agisce solo dentro la superficie admin già protetta, tramite query native
  come le altre di quella superficie.
- **Modulo Terraform `microsaas_app`** — non toccato: nessuna modifica infrastrutturale.
- **Logging strutturato** — invariato: le letture continuano a portare `tenant_id`/`user_id`, il cambio di stato app
  lato admin continua a registrare `app_id`, attore ed esito.

## Note per il revisore

- **Contratto verso la console admin**: la matrice entitlement può ora contenere **righe in più** — le app abilitate
  dalla fascia gratuita di baseline, che prima non comparivano affatto — e su quelle lo stato dell'abbonamento è
  assente. L'interfaccia admin già gestiva quel caso (`e.subscriptionStatus ?? '—'`), ma è il punto da guardare.
- **Decisioni differite tracciate**: la notifica dal server al browser in tempo reale è in
  [docs/_BACKLOG.md](../../docs/_BACKLOG.md) §"Canale di notifica dal server al browser" (capacità trasversale), con
  richiamo nei punti aperti di [UC 0077](../../docs/usecases/15-supporto-e-piattaforma/0077-provider-entitlement-reale.md);
  la forma della risposta dell'endpoint e il confine con UC 0076 restano annotati nello stesso use case. La voce
  storica del backlog §"Backoffice shell — Endpoint entitlement nel core" è stata chiusa con l'esito reale.
- **Prove end-to-end**: tre spec (autenticazione, checkout, membri) non simulavano affatto l'endpoint degli
  entitlement e giravano quindi su una shell in errore senza accorgersene, perché prima l'errore era invisibile. Ora
  lo simulano come le altre.
- **gate privacy: nessun segnale** (`npm run privacy-scan`, uscita 0). Nessun nuovo trattamento: la change lavora su
  identificatori tecnici di app e stati di abbonamento già trattati (UC 0013/0025).
- **gate parità scaffold**: nessun percorso-sorgente dei modelli toccato (uscita 0).
- **Promemoria landing**: non pertinente — la change non tocca la superficie funzionale o di listino di un'app.
- Nessuna base grafica di riferimento è stata ri-registrata: nessuno scarto visivo inatteso.
- **Verifica manuale sullo stack locale eseguita** (`./app-start.sh`, browser guidato): login → menu con le app reali;
  lettura degli entitlement bloccata → "Non riusciamo a caricare le tue app" con riprova e nessuna falsa dichiarazione
  di "nessuna app"; rotta di modulo in errore → messaggio di errore e non diniego; riprova → menu ripopolato senza
  ricaricare la pagina. In questo giro sono emersi e sono stati corretti i difetti delle decisioni 24 e 25.

## Test

**Backend (`services/core`)** — `./run-tests.sh backend` verde (tutti i moduli).
- `EntitlementAccessTest` (nuovo, 6 prove, puro): la regola caso per caso — abbonamento che concede accesso, dunning,
  abbonamento che non concede accesso che **sovrascrive** la fascia gratuita, baseline gratuita, app disabilitata,
  account in attesa di eliminazione, stato dell'account ignoto.
- `EntitlementCoherenceTest` (nuovo, 4 prove, database reale): le due viste **combaciano** sugli stessi dati per due
  account; la baseline gratuita è visibile anche all'admin; disabilitando un'app sparisce da menu, matrice admin **e**
  stato del polling; l'account in attesa di eliminazione è vuoto in entrambe le viste.
- Suite esistenti invariate e verdi, incluso il test di isolamento fra account (`entitlementsAreScopedToTenant`), non
  riscritto.

**Frontend** — `./run-tests.sh frontend` verde: 120 prove unitarie + 25 end-to-end.
- `deriveEntitlementsStatus` (4 prove): caricamento solo al primo avvio effettivo; **lettura mai partita** (sessione
  non autenticata) né caricamento né errore; errore persistente durante il ritentativo.
- Barra laterale (4 prove nuove): caricamento, errore con riprova, vuoto con invito all'acquisto, riprova che richiama
  davvero la rilettura.
- Guardia di rotta (1 prova nuova): con entitlement non leggibili la rotta mostra l'errore, non il diniego.
- Freschezza (3 prove nuove, con finto backend): rilettura al ritorno sulla scheda, invalidazione esplicita, e la
  verifica che la rilettura al ritorno resti disattivata per tutte le altre letture della shell.
- End-to-end (3 prove nuove): stato vuoto con invito all'acquisto; endpoint irraggiungibile → il menu lo dice, offre
  la riprova e non dichiara "nessuna app", e la rotta non trasforma il guasto in un diniego; da anonimo la radice
  porta al login e la shell non resta appesa (prova verificata sensibile: fallisce se si reintroduce il difetto).

**Suite completa** — `./run-tests.sh`: backend, frontend, infra, compliance, tooling, smoke, site tutte verdi.

## Stato criteri di accettazione

- [x] Un solo punto nel core decide l'accesso; read-model del tenant, matrice admin e stato del polling lo usano tutti.
- [x] Sugli stessi dati le due viste coincidono — verificato da `EntitlementCoherenceTest`.
- [x] Il polling non dichiara attiva un'app disabilitata dalla piattaforma.
- [x] Con l'endpoint in errore la shell mostra errore + riprova e non afferma che l'account non ha app; la rotta di un
      modulo mostra l'errore e non il diniego.
- [x] Con elenco vuoto la shell mostra lo stato "nessuna app attiva" con invito all'acquisto, distinto per testo e
      ruolo di accessibilità dallo stato di errore.
- [x] Dopo attivazione post-acquisto, cambio fascia, disdetta e ripresa la lettura viene invalidata.
- [x] Il ritorno sulla scheda rilegge gli entitlement; il comportamento resta disattivato per le altre letture.
- [x] Lo stub resta usato solo da test e sviluppo locale: nessun percorso vi ricade come ripiego in errore.
- [x] `./run-tests.sh backend` e `./run-tests.sh frontend` verdi (e l'intera suite).
