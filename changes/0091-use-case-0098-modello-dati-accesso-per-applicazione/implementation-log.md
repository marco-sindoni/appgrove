# Implementation Log — Change 0091: Modello dati dell'accesso per applicazione e ruolo di piattaforma a due valori

**Branch**: `change/0091-use-case-0098-modello-dati-accesso-per-applicazione`
**Aree**: `services/core`, `services/auth` (solo collaudi), `frontend/`, `dev/seed`, `docs/`, `tools/platform-e2e`
**Completata**: 2026-08-21
**Modalità**: **fast** — autopilot senza fermate di workflow, dichiarata dall'orchestratore `go-fast`. Le risposte
alle domande di approfondimento sono dell'agente e sono tracciate in [decisions.json](decisions.json) (22 voci, 21
in autopilot). Contropartite rispettate: suite completa `./run-tests.sh` verde prima del commit, registro integrale,
[how-to-test.md](how-to-test.md) nella cartella della change.

## File modificati

| File | Azione |
|---|---|
| `services/core/src/main/resources/db/migration/V20__app_access.sql` | Creato |
| `services/core/.../platform/AppAccess.java` | Creato |
| `services/core/.../platform/AppRole.java` | Creato |
| `services/core/.../platform/AppAccessRepository.java` | Creato |
| `services/core/.../platform/AppAccessRules.java` | Creato |
| `services/core/.../platform/AppAccessDtos.java` | Creato |
| `services/core/.../platform/AppAccessResource.java` | Creato |
| `services/core/.../platform/MembershipRole.java` | Modificato (due soli valori) |
| `services/core/.../platform/Roles.java` | Modificato (perché `ADMIN` resta) |
| `services/core/.../platform/MembershipRepository.java` | Modificato (`owners`, `countOwners`) |
| `services/core/.../platform/UserResource.java` | Modificato (vincolo ultimo owner) |
| `services/core/.../platform/InvitationResource.java` | Modificato (si invita solo come `member`) |
| `services/core/.../gdpr/PlatformDataContract.java` | Modificato (export, purga, manifesto) |
| `services/core/src/main/resources/META-INF/openapi/openapi.{yaml,json}` | Rigenerati dal build |
| `services/core/src/test/.../AppAccessRulesTest.java`, `AppAccessApiTest.java` | Creati |
| `services/core/src/test/.../{MultiTenancyTest,AccountUserApiTest,SeedDataTest,TestData}.java` | Modificati |
| `services/core/src/test/.../gdpr/{PlatformGdprContractTest,AccountDeletionApiTest,ProfileSelfServiceTest}.java`, `InvitationLifecycleTest.java` | Modificati |
| `services/auth/src/test/.../{InviteAcceptTest,cognito/CognitoInvitationsTest}.java` | Modificati (ruolo dell'invito del seme) |
| `frontend/apps/backoffice/src/pages/members/MembersPage.tsx` + `.test.tsx` | Modificati (selettori rimossi) |
| `frontend/apps/backoffice/src/auth/schemas.ts` | Modificato (un solo ruolo invitabile) |
| `frontend/packages/i18n/src/resources/{en,it,fr,es,de}.ts` | Modificati (tre chiavi rimosse) |
| `frontend/packages/api-client/src/schema.ts` | Rigenerato da OpenAPI |
| `dev/seed/seed.sql` + `README.md` | Modificati (traduzione del ruolo + accessi) |
| `docs/compliance/manifests/platform.yaml`, `docs/compliance/ropa.{it,en}.md` | Modificati / rigenerati |
| `docs/testing/copertura-e2e.yaml` | Modificato (0098 → senza-superficie) |
| `tools/platform-e2e/journeys/J-MEMBERS.spec.ts` | Modificato |
| `docs/01-architettura.md`, `docs/02-auth-sicurezza.md`, `docs/05-persistenza-dati.md` | Modificati |
| `docs/usecases/22-refactor-membership-model/story/{0098,0099,0100,0111,0113}-*.md` | Modificati |
| `docs/usecases/22-refactor-membership-model/epic/E22-01-*.md`, `task/0098-*.md` | Modificati |

## Cosa è stato fatto

Nasce `platform.app_access` (migrazione `V20`): una riga per la terna (account, applicazione, persona) con il suo
ruolo — il luogo in cui vive «questa persona può usare questa applicazione con questo ruolo». Riferisce l'**identità**
(UC 0116) e non l'appartenenza, ha l'unicità della terna sulle righe vive e i due indici che servono alle due domande
vere. Sopra ci sono l'entità, il repository, l'enumerazione del ruolo di applicazione con il suo ordinamento scritto
una volta sola, le regole di chi-può-cosa come funzione pura e le quattro operazioni di rete sotto
`/api/platform/v1/apps/{appId}/access`.

In parallelo il **ruolo di piattaforma scende a due valori** (`owner`, `member`): il valore `admin` esce
dall'enumerazione e da tutto ciò che lo offre o lo scrive — invito, cambio di ruolo della persona, seme di sviluppo,
i due selettori della schermata dei membri. Il **claim del token** e le annotazioni che lo leggono restano intatti,
come tolleranza dei token già emessi. Infine il divieto sull'ultimo owner passa dall'interfaccia al **servizio**.

## Decisioni prese

Change in modalità **fast**: tutte le scelte sono dell'agente e stanno in [decisions.json](decisions.json). Le
portanti:

- **fin dove arriva la riduzione a due valori** (decisione 4): si ferma dove comincia il token. `Roles.ADMIN` e le
  `@RolesAllowed` che la nominano restano — la riduzione del claim è di UC 0099, la tolleranza dei token già emessi
  di UC 0113 §6 — e la restrizione avviene comunque per costruzione, perché nessuna appartenenza produce più quel
  valore. Conseguenza: la compilazione non si rompe da nessuna parte, contro quello che il piano di lavoro temeva;
- **nessun vincolo di controllo e nessuna conversione** sul ruolo di piattaforma nella migrazione (5): un vincolo
  aggiunto prima della conversione rifiuterebbe di applicarsi. Il **seme** è invece convertito (6), perché un dato
  di sviluppo che dichiara un ruolo non più ammesso non si caricherebbe;
- **la concessione è un innesto-o-cambio** (12): una terna che esiste già è un cambio di ruolo, non un errore;
  l'arbitro dell'unicità è l'indice della banca dati e la perdente di una gara riceve 409, non 500;
- **`@RolesAllowed` non basta** sulle nuove operazioni (7): il ruolo di applicazione non è nel token, quindi
  `@Authenticated` più verifica esplicita in transazione. È scritto nel commento della classe, perché è il punto in
  cui una «semplificazione» aprirebbe un varco;
- **`app_access.identity_id` è dato personale** (10, 21): annotato e dichiarato nel manifesto, come
  `membership.identity_id` e per la stessa ragione — in deroga al piano di lavoro, che diceva il contrario;
- **il frontend perde i selettori, non li riduce a una voce** (18); i collaudi del comportamento sparito sono
  **sostituiti**, non cancellati (19);
- **chi esce dall'account porta via i suoi permessi** (23): edge della storia §5 che rileggendo il codice prima del
  commit risultava non implementato. L'uscita cancella logicamente anche gli accessi; la sospensione no, perché è
  reversibile.

## Invarianti appgrove

- **Account solo dal token verificato** — `app_access` è entità tenant-scoped: il `tenant_id` lo scrive il
  discriminatore dal token, mai il chiamante. L'identità bersaglio è accettata solo se ha un'**appartenenza viva a
  quell'account**, letta dal modello: l'identificativo che arriva dal chiamante non è mai una prova di appartenenza.
- **Filtro riga per riga** — ogni lettura e scrittura passa dal repository Panache con filtro automatico. Nessuna
  lettura trasversale agli account è stata introdotta (le tre esistenti restano quelle dichiarate in UC 0116/0117).
- **Logging strutturato** — le tre operazioni registrano `tenant_id`, `app_id` e `user_id`; la traccia di controllo
  (`app_access.granted` · `.role_changed` · `.revoked`) porta soli identificativi opachi, mai indirizzo né nome.
- **Modulo Terraform `microsaas_app`** — non toccato: nessuna infrastruttura nuova.

## Note per il revisore

- **Contratto verso il frontend**: quattro operazioni nuove (nessun consumatore ancora) e due restringimenti —
  l'invito accetta il solo ruolo `member`, il cambio di ruolo della persona rifiuta `admin`. Lo schema OpenAPI e i
  tipi del client sono stati rigenerati.
- **Comportamento che cambia in locale**: `admin@acme.test` è ora `member` di piattaforma con ruolo `admin` sul
  Mini-CRM, quindi **non vede più** Account, Billing e Members. È il modello nuovo, non una regressione.
- **Gate privacy** (UC 0031): eseguito, 8 segnali, tutti attesi e classificati — classificazione **MINORE**, nessun
  responsabile esterno nuovo, nessun aumento di versione di privacy policy o termini. Registro dei trattamenti
  rigenerato con `npm run assemble`.
- **Gate parità scaffold** (UC 0046): nessun percorso-sorgente dei modelli toccato.
- **Copertura end-to-end** (UC 0093/0094): voce 0098 da esenzione `non-implementato` a **`senza-superficie`**;
  nessun percorso nuovo. `J-MEMBERS` perde il segmento del cambio di ruolo (l'oggetto coperto è sparito) e
  **guadagna** la prova che il vincolo dell'ultimo owner vive nel servizio. Controllo `check.mjs` verde.
- **Promemoria landing**: nessuna. La change non ha toccato la superficie di feature o listino di un'app.
- **Decisioni differite tracciate** — UC 0099: evento di invalidazione non emesso (punti di scrittura già marcati),
  lettura «dove può entrare questa persona», ritiro del valore `admin` dal claim; UC 0113: conversione dei dati
  reali, vincolo di controllo su `membership.role`, ritiro della tolleranza con la sua data; UC 0100: colonna del
  ruolo, elenco unico, invito riservato all'owner, campo `role` del contratto dell'invito; UC 0111: ruolo
  predefinito dell'interfaccia, schermate che consumano le nuove operazioni, eventuale restrizione dei nomi al
  `viewer`. Nessuna voce nuova in `docs/_BACKLOG.md`: ogni punto ha uno use case proprietario.

## Test

- **`services/core`** — nuovi: `AppAccessRulesTest` (7 prove sulla funzione pura: owner, `admin` della stessa
  applicazione, `admin` di un'altra, `editor`, `viewer`, persona senza accesso, ordinamento dei tre ruoli) e
  `AppAccessApiTest` (9 prove: ciclo completo, innesto-o-cambio senza duplicato, potere circoscritto dell'`admin`,
  `editor` che legge e non scrive, persona di un altro account «non trovata», persona sospesa, applicazione senza
  diritto, owner implicito e intoccabile, due concessioni simultanee → una riga). Estesi: `MultiTenancyTest`
  (separazione fra account sulla nuova tabella: non legge, non concede, non revoca), `AccountUserApiTest` (rifiuto
  del ruolo ritirato + ultimo owner non retrocedibile/sospendibile/rimovibile), `SeedDataTest` (due `member`, zero
  `admin`, i due accessi al crm, l'owner senza righe), `PlatformGdprContractTest` (l'export copre `app_access`).
  Aggiunto poi `removingAPersonFromTheAccountRevokesTheirAccesses` (10 prove in tutto).
- **`services/auth`** — due collaudi allineati al ruolo dell'invito del seme, che ora è `member`.
- **`frontend`** — `MembersPage.test.tsx`: la prova del cambio di ruolo è sostituita da «il ruolo è una etichetta,
  non un selettore»; controllo dei tipi e suite di livello 2 verdi senza altre modifiche.
- **`tools/platform-e2e`** — `J-MEMBERS` adeguato (vedi sopra).
- **Esito**: `./run-tests.sh` **completa e verde** su tutte le aree (backend, frontend, infra, compliance, tooling,
  smoke, platform, site).

## Stato criteri di accettazione

- [x] `platform.app_access` con unicità della terna sulle righe vive, i due indici di lettura e il controllo sul ruolo
- [x] Il ruolo di piattaforma ammette due soli valori; nessuna superficie offre più `admin`
- [x] Concessione, cambio e revoca dal core; l'`admin` opera solo sulla propria applicazione
- [x] La persona di un altro account risponde «non trovato», non «vietato»
- [x] La persona non attiva non riceve accesso
- [x] L'applicazione senza diritto non riceve accesso
- [x] L'owner è implicito, in testa all'elenco, e intoccabile
- [x] L'ultimo owner non è rimovibile, retrocedibile né sospendibile — rifiuto del servizio
- [x] Due concessioni simultanee producono una sola riga
- [x] La persona rimossa dall'account perde i suoi accessi (cancellazione logica, storia §5)
- [x] Manifesto, esportazione, cancellazione e registro dei trattamenti aggiornati
- [x] `./run-tests.sh` completa verde
