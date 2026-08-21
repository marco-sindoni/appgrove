# Implementation Log — Change 0092: Autorizzazione per applicazione (varco riusabile in `commons`, ruolo fuori dal token)

**Branch**: `change/0092-use-case-0099-autorizzazione-per-applicazione`
**Aree**: `services/commons`, `services/core`, `services/crm`, `services/fatture`, `services/auth`, `infra/`, `tools/new-application`, `docs/`
**Completata**: 2026-08-21
**Modalità**: **fast** — autopilot senza gate di workflow, dichiarata all'invocazione dall'orchestratore `go-fast`.
Le risposte alle domande di approfondimento sono dell'agente e sono tracciate in [decisions.json](decisions.json)
(24 voci, 23 in autopilot). Contropartite rispettate: suite completa `./run-tests.sh` verde prima del commit,
registro integrale, [how-to-test.md](how-to-test.md).

## File modificati

| File | Azione |
|---|---|
| `services/commons/.../access/AppRole.java` | Spostato da `core/platform` (unica sede dell'ordinamento dei ruoli) |
| `services/commons/.../access/RequiresAppRole.java` | Creato — annotazione del varco (ruolo minimo + `fresh`) |
| `services/commons/.../access/AppRoleGateFilter.java` | Creato — il filtro che la interpreta |
| `services/commons/.../access/AppRoleService.java` | Creato — faccia di piattaforma (`roleOf` / `roleFresh`) |
| `services/commons/.../access/AppRoleOutcome.java` | Creato — tre esiti sigillati: ruolo, diniego noto, non decidibile |
| `services/commons/.../access/ProjectedAppRoleService.java` | Creato — attuazione predefinita: legge la copia locale |
| `services/commons/.../access/RestAppRoleService.java` | Creato — rete di sicurezza: legge dal core |
| `services/commons/.../access/AppAccessClient.java` | Creato — client verso `GET /me/app-access` |
| `services/commons/.../access/MyAppAccessView.java` | Creato — contratto della lettura, condiviso core ↔ applicazioni |
| `services/commons/.../access/AppRoleRequiredException.java` | Creato — tre rifiuti tipizzati |
| `services/commons/.../access/projection/AppRoleProjectionStore.java` | Creato — copia locale del ruolo (con scadenza) |
| `services/commons/.../projection/LocalProjection.java` | Creato — interfaccia comune delle copie locali |
| `services/commons/.../web/AppRoleRequiredMapper.java` | Creato — problem+json con `type` stabile e i due ruoli |
| `services/commons/.../membership/PlatformRoles.java` | Creato — ruolo di piattaforma nel claim (`admin` → `member`) |
| `services/commons/.../entitlement/SafetyNet.java` | Modificato — qualificatore riusato, javadoc generalizzato |
| `services/commons/.../entitlement/projection/EntitlementProjectionStore.java` | Modificato — attua `LocalProjection` |
| `services/commons/.../entitlement/projection/EntitlementInvalidationConsumer.java` | Modificato — marca **tutte** le copie |
| `services/commons/.../gdpr/TenantPurgeConsumer.java` | Modificato — purga **tutte** le copie, ognuna nella traccia |
| `services/core/.../platform/MeAppAccessResource.java` | Creato — `GET /api/platform/v1/me/app-access` |
| `services/core/.../platform/AppAccessResource.java` | Modificato — i tre punti di scrittura emettono l'invalidazione |
| `services/core/.../platform/UserResource.java` | Modificato — l'uscita di una persona invalida le copie |
| `services/core/.../billing/EntitlementInvalidationPublisher.java` | Modificato — javadoc: canale condiviso |
| `services/core/.../platform/{AppAccess,AppAccessRepository,AppAccessRules,MembershipRole}.java` | Modificati — importano `AppRole` da `commons` |
| `services/core/src/main/resources/META-INF/openapi/openapi.{json,yaml}` | Rigenerati (nuova rotta) |
| `services/auth/.../local/TokenService.java` | Modificato — claim dei ruoli normalizzato |
| `infra/modules/platform_shared/lambda/pre_token_gen/handler.py` | Modificato — `_claim_role`: `admin` → `member` |
| `services/crm/.../{ContactResource,InteractionResource}.java` | Modificati — dichiarano il ruolo minimo |
| `services/{crm,fatture}/.../db/migration/V5__app_role_projection.sql` | Creati — tabella della copia locale |
| `services/{crm,fatture}/src/main/resources/application.properties` | Modificati — tabella e durata massima |
| `tools/new-application/templates/service/...` | Modificati — migrazione e chiavi nei modelli (parità) |
| `docs/{02-auth-sicurezza,04-services-backend}.md` | Modificati — claim e varco |
| `docs/compliance/manifests/{crm,fatture}.yaml`, `docs/compliance/ropa.{it,en}.md` | Modificati / rigenerati |
| `docs/testing/copertura-e2e.yaml` | Modificato — 0099: `non-implementato` → `senza-superficie` |
| `docs/usecases/22-.../story/{0099,0103,0111}.md`, `.../epic/E22-01-...md` | Modificati — stato e rimandi |
| Collaudi: `commons/access/*`, `commons/membership/PlatformRolesTest`, `commons/web/AppRoleRequiredMapperTest`, `core/MeAppAccessApiTest`, `crm/{AppRoleGateTest,MockAppRoleService}`, `crm/{TestProjection,ProjectionResetCallback}`, `pre_token_gen/test_handler.py` | Creati / aggiornati |

## Cosa è stato fatto

Il ruolo di una persona **su una applicazione** ora viene rispettato, da un solo meccanismo condiviso. In
`services/commons` nasce il varco dichiarativo `@RequiresAppRole(AppRole.editor)`: un filtro name-bound legge il
ruolo dalla **copia locale** del servizio (tabella `<schema>.app_role_projection`, durata massima sessanta secondi),
la rinfresca dal core quando serve tramite la nuova lettura `GET /api/platform/v1/me/app-access`, e rifiuta con tre
esiti distinti — nessun accesso (403), ruolo insufficiente (403, nominando il ruolo che serve), non decidibile (503).
Nessuna applicazione scrive confronti fra ruoli: il Mini-CRM dichiara `viewer` sulle letture e `editor` sulle
scritture e non contiene una riga di logica di autorizzazione. Le tre scritture dell'accesso in `core` (concessione,
cambio di ruolo, revoca) e l'uscita di una persona dall'account **emettono** l'evento di invalidazione sulla coda già
esistente dei diritti d'accesso, e un solo consumatore marca da rinfrescare **tutte** le copie locali del servizio
attraverso la nuova interfaccia `LocalProjection` (che porta con sé anche la cancellazione per esercizio del diritto
di cancellazione). Il token, infine, porta **un ruolo in meno**: il claim `roles` non contiene più `admin` come ruolo
di piattaforma, né in cloud né in locale.

## Decisioni prese

Change condotta in **fast**: tutte le scelte sono dell'agente e sono in [decisions.json](decisions.json). Le portanti:

- **L'enumerazione del ruolo si sposta in `commons`** (dec. 3), perché il varco deve poter nominare il ruolo minimo e
  `commons` non può dipendere da `core`. Duplicarla avrebbe duplicato l'**ordinamento**, che è la cosa che meno di
  ogni altra va scritta due volte.
- **La copia locale del ruolo scade** (sessanta secondi, dec. 4), a differenza di quella dei diritti d'accesso. La
  differenza è voluta: un abbonamento cambia di rado, un ruolo spesso, e un evento perso qui significa un **permesso
  revocato che sopravvive**. La scadenza è la rete che tiene quando il canale degli eventi è rotto.
- **Una coda sola** (dec. 5–6): quella già esistente, con il tipo di evento nel messaggio. Perché un solo consumatore
  potesse marcare due copie senza conoscerle, nasce `LocalProjection`; il consumatore dell'invalidazione e quello
  della purga iterano su tutte le copie invece di nominarne una. Chi aggiungerà la terza non deve ricordarsi di due
  file.
- **Tre rifiuti, non due** (dec. 7): il piano di lavoro ne prevedeva due, la storia §5 pretende di distinguere il
  guasto nostro dal permesso mancante. «Non decidibile» è **503**, perché è la verità del codice di stato.
- **La rilettura obbligatoria è un attributo dell'annotazione** (`fresh = true`, dec. 8) e non una chiamata che
  l'operazione fa a mano: se fosse una chiamata, l'operazione scriverebbe logica di autorizzazione — ciò che la storia
  vieta. Nel Mini-CRM non è usata, e il perché è scritto (le operazioni irreversibili di oggi vivono in `core` e
  leggono il ruolo in transazione: fresche per costruzione).
- **La tolleranza `admin` → `member` sta nel claim, non nel modello** (dec. 9), attuata due volte per parità
  locale/cloud, con la stessa tabella di casi eseguita da entrambi i collaudi. Il ritiro è di UC 0113.
- **Postura del fallimento in tre righe** (dec. 16): copia assente + core giù → si nega (503); copia vecchia + core
  giù → ultima verità nota; rilettura obbligatoria → mai la copia.
- **Fuori scope, tracciato**: esclusione della voce di catalogo dei posti (→ UC 0103, con il punto esatto del codice
  segnato da un commento), ritiro del varco dei posti del Mini-CRM (→ UC 0111), misure e allarmi della copia del
  ruolo e annotazioni di `fatture` (→ punti aperti di UC 0099).

## Invarianti appgrove

- **Account solo dal token verificato** — la nuova lettura del core **non ha parametri**: dice dove può entrare *chi
  chiama*. Nel varco condiviso, account e persona arrivano dal JWT verificato e la copia locale è indicizzata su
  entrambi: non si legge mai il ruolo di un'altra coppia. `TenantNotResolvedException` a chiusura se il claim manca.
- **Filtro per account su ogni lettura** — ogni interrogazione della copia locale porta `tenant_id` nella condizione
  (JDBC diretto con parametro esplicito, come la copia dei diritti d'accesso, perché i consumatori girano fuori da una
  richiesta autenticata). Le letture del core restano su entità con discriminatore automatico.
- **Modulo Terraform `microsaas_app`** — **non toccato**: si riusa la coda per servizio già dichiarata dal modulo, e
  ogni applicazione nuova eredita il canale senza righe in più.
- **Logging strutturato** — ogni diniego per guasto e ogni invalidazione consumata portano account, persona e
  applicazione; il conteggio delle righe marcate è per copia.

## Note per il revisore

- **Contratto cross-area nuovo**: `GET /api/platform/v1/me/app-access` (core → servizi delle applicazioni, e domani il
  menu laterale di UC 0107). Lo schema OpenAPI committato del core è stato rigenerato dalla build. **Nessuna riga di
  frontend**: il client tipizzato del backoffice per le API di piattaforma è scritto a mano; solo i moduli `fatture` e
  `crm` sono generati, e i loro schemi non cambiano.
- **Effetto già visibile in locale**, ereditato dalla change 0091 e ora *sentito* dal Mini-CRM: `admin@acme.test` è
  collaboratrice con ruolo `admin` sul Mini-CRM. Dopo questa change quel ruolo **conta davvero** (prima era solo una
  riga in tabella). Da qui in poi, per usare il Mini-CRM servono **due** cose: un posto **e** un ruolo. È la
  convivenza dei due varchi, che UC 0111 chiuderà.
- **Decisioni differite tracciate**: punti aperti di UC 0099 (riscritti dividendo chiusi da aperti), UC 0103
  (esclusione della voce di catalogo dei posti dalla lettura «dove posso entrare») e UC 0111 (ritiro del varco dei
  posti). Nessun punto lasciato solo in conversazione.
- **gate privacy (UC 0031)**: eseguito sul diff finale — 11 segnali, tutti classificati. Il solo dato nuovo reale è la
  copia locale del ruolo; classificazione **MINORE** (finalità identica alla fonte, base = contratto art. 6.1.b,
  conservazione = cache cancellata fisicamente con l'account, categoria ordinaria, nessun responsabile esterno nuovo,
  nessun aumento di versione di privacy policy o termini). Dichiarata nella **prosa** dei manifesti delle due
  applicazioni — non come voce con entità e campo — seguendo il precedente della tabella dei posti e della copia dei
  diritti d'accesso; RoPA rigenerata con `npm run assemble`. Dettaglio in `decisions.json` dec. 19.
- **gate parità scaffold (UC 0046)**: il rilevatore ha segnalato `services/commons` e `services/fatture`. Scelta la
  **prima** via: modelli aggiornati nello stesso commit (migrazione `V5` + chiavi di configurazione). Collaudo di
  parità verde, nessuna deroga da registrare in `docs/_PARITA-SCAFFOLD.md`.
- **copertura e2e (UC 0093/0094)**: nessun impatto — la voce 0099 passa da esenzione `non-implementato` a
  `senza-superficie`, come la storia stessa prevede. `node tools/e2e-coverage/check.mjs` verde.
- **promemoria landing**: valutato e **negativo**. L'unica landing pubblicata è quella di `fatture`, e questa change
  non ne ha toccato né le funzionalità né il listino (solo una migrazione e due chiavi di configurazione, invisibili al
  cliente). Nessun `finalize-landing` da rieseguire.
- **`run-tests.sh` non cambia**: nessun modulo aggiunto o rimosso, nessun comando di area cambiato.
- **`docs/usecases/_INDEX.md` non cambia**: 0099 è una storia evolutiva e non vi compare (l'indice copre le 60 storie
  base); lo stato vive nell'intestazione della storia e nell'epica E22.1, come stabilito dalla change 0088.

## Test

Aggiunti o aggiornati, per area:

- **`services/commons`** (unità, 4 classi nuove): `AppRoleTest` — l'ordinamento completo `viewer < editor < admin`, la
  posizione dell'owner sopra tutti, e che un valore ignoto non diventi mai un permesso; `AppRoleProjectionExpiryTest`
  — la scadenza della copia, compreso l'estremo esatto e il caso degenere di configurazione;
  `AppRoleGateFilterTest` — i tre esiti del varco, la precedenza del metodo sulla classe, il fatto che **solo**
  un'operazione che lo chiede rilegga dalla fonte di verità, e che un varco senza requisito neghi;
  `AppRoleRequiredMapperTest` — i tre corpi problem+json distinguibili senza leggere un messaggio;
  `PlatformRolesTest` — la normalizzazione del claim, gemella del collaudo Python.
- **`services/core`** (integrazione): `MeAppAccessApiTest` — il collaboratore vede solo dove è stato abilitato, un
  accesso a una applicazione senza diritto non apre nulla, l'owner le vede tutte col ruolo massimo, le applicazioni di
  un altro account non compaiono mai, il chiamante anonimo è rifiutato.
- **`services/crm`** (integrazione, 11 casi): `AppRoleGateTest` — i tre ruoli su una lettura e una scrittura, i due
  rifiuti distinti, il diniego copiato per non richiedere al core a ogni richiesta, la prova che una copia fresca
  **non** genera traffico, il ciclo di invalidazione (dopo l'evento vale il ruolo nuovo e la copia vecchia non
  sopravvive), la prova che **un** evento marca **entrambe** le copie locali, il fallimento chiuso con 503 e l'ultima
  verità nota con il core giù.
- **`infra/`** (unità Python): `test_handler.py` — il valore ritirato diventa `member`, anche insieme a
  `platform-admin`; `owner`/`member` passano inalterati; e il collaudo che diventa rosso se qualcuno mettesse i ruoli
  per applicazione nel token «per risparmiare una chiamata».

**Esito**: `./run-tests.sh` (suite completa, senza parametri) **verde** su tutte le aree — backend, frontend, infra,
compliance, tooling, smoke, platform, site. Le esecuzioni per area durante il lavoro: `commons` 77 test verdi,
`crm` 54 verdi, `core` `MeAppAccessApiTest` 6 verdi, `pre_token_gen` 21 verdi.

## Stato criteri di accettazione

- [x] Un'operazione dichiara il ruolo minimo con un'annotazione e non contiene alcun confronto fra ruoli.
- [x] Ruolo sufficiente → passa; insufficiente → rifiuto che **nomina** il ruolo richiesto; nessun accesso → rifiuto
      diverso, che dice a chi chiedere l'abilitazione.
- [x] Copia assente **e** core non raggiungibile → si nega con **503** e il messaggio del guasto.
- [x] Cambiato il ruolo nel core e consumato l'evento, il servizio applica il ruolo nuovo; la copia vecchia non
      sopravvive.
- [x] Scaduta la durata massima, la copia si rinfresca anche senza evento.
- [x] `GET /me/app-access` restituisce solo le applicazioni con diritto **e** accesso; l'owner le vede tutte col ruolo
      massimo; un altro account non compare.
- [x] Un'appartenenza che vale ancora `admin` produce `member` nel claim, in cloud e in locale.
- [x] La copia locale del ruolo viene cancellata con l'account e il conteggio entra nella traccia di controllo.
- [x] `./run-tests.sh` (suite completa) verde.
