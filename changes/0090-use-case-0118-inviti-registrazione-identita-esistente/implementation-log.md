# Implementation Log — Change 0090: Inviti e registrazione quando l'identità esiste già

**Branch**: `change/0090-use-case-0118-inviti-registrazione-identita-esistente`
**Aree**: `services/core`, `services/auth`, `frontend/apps/backoffice`, `frontend/packages/api-client`,
`frontend/packages/i18n`, `tools/platform-e2e`, `dev/seed`, `docs`
**Completata**: 2026-08-21
**Modalità**: **fast** — autopilot senza fermate di workflow (dichiarata all'invocazione dalla skill
`go-fast`). Le risposte alle domande di approfondimento sono dell'agente e sono tracciate in
[decisions.json](decisions.json) (28 voci, 27 in autopilot). Contropartite: suite completa
`./run-tests.sh` verde prima del commit, registro integrale, `how-to-test.md` nella cartella della change.

## File modificati

| File | Azione |
|---|---|
| `services/core/src/main/resources/db/migration/V19__invitation_identity.sql` | Creato |
| `services/core/src/main/java/app/appgrove/core/platform/MeInvitationsResource.java` | Creato |
| `services/core/src/main/java/app/appgrove/core/platform/MeAccountsResource.java` | Creato |
| `services/core/src/main/java/app/appgrove/core/platform/MeDtos.java` | Creato |
| `services/core/src/main/java/app/appgrove/core/platform/InvitationResource.java` | Modificato |
| `services/core/src/main/java/app/appgrove/core/platform/InvitationRepository.java` | Modificato |
| `services/core/src/main/java/app/appgrove/core/platform/Invitation.java` · `InvitationStatus.java` | Modificati |
| `services/core/src/main/java/app/appgrove/core/platform/MembershipRepository.java` | Modificato |
| `services/core/src/main/java/app/appgrove/core/gdpr/PlatformDataContract.java` | Modificato |
| `services/core/src/main/resources/META-INF/openapi/openapi.{yaml,json}` | Rigenerati dalla compilazione |
| `services/auth/.../IdentityProvider.java` · `AuthResource.java` · `AuthDtos.java` | Modificati |
| `services/auth/.../PlatformWriter.java` · `local/LocalIdentityProvider.java` · `local/TokenService.java` | Modificati |
| `services/auth/.../cognito/CognitoIdentityProvider.java` | Modificato |
| `frontend/packages/api-client/src/problem.ts` | Modificato (difetto del client condiviso) |
| `frontend/apps/backoffice/src/pages/dashboard/PendingInvitesSection.tsx` | Creato |
| `frontend/apps/backoffice/src/pages/dashboard/DashboardPage.tsx` · `shell/Sidebar.tsx` | Modificati |
| `frontend/apps/backoffice/src/pages/auth/LoginPage.tsx` · `AcceptInvitePage.tsx` | Modificati |
| `frontend/apps/backoffice/src/pages/Account.tsx` · `pages/members/MembersPage.tsx` | Modificati |
| `frontend/apps/backoffice/src/api/hooks.ts` · `auth/authApi.ts` | Modificati |
| `frontend/packages/i18n/src/resources/{en,it,fr,es,de}.ts` | Modificati (5 lingue) |
| `tools/platform-e2e/journeys/J-INVITE-EXISTING.spec.ts` | Creato |
| `tools/platform-e2e/helpers/browser.ts` · `journeys/J-ACCOUNT-SWITCH.spec.ts` | Modificati (passi condivisi estratti) |
| `frontend/apps/backoffice/e2e/invitesExisting.spec.ts` · `e2e/accountChoice.spec.ts` | Creati |
| `dev/seed/seed.sql` | Modificato (difetto ereditato dalla change `0088`) |
| `docs/compliance/manifests/platform.yaml` · `docs/compliance/ropa.{it,en}.md` | Modificati |
| `docs/testing/copertura-e2e.yaml` · `docs/02-auth-sicurezza.md` · `docs/_BACKLOG.md` · `docs/_PARITA-SCAFFOLD.md` | Modificati |
| `docs/usecases/22-refactor-membership-model/{story/0116,story/0117,story/0118,story/0103,epic/E22-05}` | Modificati |
| `docs/usecases/08-compliance-gdpr/0033-self-service-gdpr.md` | Modificato |

## Cosa è stato fatto

I due percorsi d'ingresso della sotto-epica E22.5 funzionano davvero. **Percorso A**: l'invio dell'invito
distingue tre esiti — «già membro di questo account» e «invito già in attesa» sono rifiuti *leciti*, con due
identificativi stabili nel campo `type` del problem+json perché l'interfaccia possa mostrare due testi
diversi nelle cinque lingue; «l'identità esiste altrove» produce invece un esito **identico** a un indirizzo
sconosciuto, e la riga di invito acquista `identity_id`, valorizzato lato server e mai restituito a chi
invita. L'accettazione di chi ha già un'identità avviene **dalla propria sessione**, da una sezione in testa
al cruscotto (con il numero degli inviti sulla voce «Dashboard» del menu): nasce **solo l'appartenenza**, che
diventa anche l'account attivo. **Percorso B**: `POST /api/platform/v1/me/accounts` apre un proprio account
da dentro la sessione, senza chiedere di nuovo parola d'accesso e nome — la registrazione con un indirizzo
già noto continua a rifiutare, ma il testo mostrato ora dice dove andare.

Chiusi anche i tre rimandi che le due storie precedenti avevano lasciato qui: la **schermata di scelta
dell'account** senza sessione (realizzata come sfida sul modello del secondo fattore, in **entrambi** i
fornitori di identità), lo stato «appartenenza in attesa» (**non** si introduce: l'attesa è la riga di
invito) e il **riuso di un indirizzo dopo la cancellazione** di un'identità (il controllo di esistenza
diventa incondizionato come l'indice, quindi un messaggio comprensibile invece di un errore del servizio).

## Decisioni prese

Change condotta in **fast**: tutte le decisioni sono dell'agente e stanno in
[decisions.json](decisions.json). Le portanti:

- **la superficie «di me stesso» degli inviti sta in `services/core`** sotto `/api/platform/v1/me`, accanto
  a `/me/memberships`: accettare un invito da dentro l'applicazione è un atto del prodotto, non un flusso di
  autenticazione. Il percorso via collegamento resta in `auth` (è lì che nasce un'identità nuova) e per
  un'identità che esiste già rimanda alla sessione (dec. 3);
- **i due rifiuti leciti si distinguono col campo `type`** del corpo problem+json, non con un messaggio da
  interpretare — il messaggio del server è in italiano e l'interfaccia parla cinque lingue (dec. 4);
- **la riservatezza tiene per costruzione, non per simmetria**: la lettura dell'identità si esegue *sempre*,
  in entrambi i rami, e il risultato cambia solo il valore scritto in banca dati. Nessuna asserzione sui
  tempi nei collaudi: una soglia temporale in una suite condivisa è instabile, e instabile vuol dire
  disattivata (dec. 5);
- **la scelta dell'account è una sfida a vita breve** e non una sessione senza claim: in `services/core` il
  risolutore del tenant è a chiusura e rifiuta appena si apre una sessione verso la banca dati, quindi un
  token senza claim non potrebbe nemmeno leggere `/me/memberships` — servirlo avrebbe richiesto di
  indebolire il presidio dell'invariante 1 (dec. 7). Attuata anche su **Cognito**, dove il caso si riconosce
  dall'assenza del claim `tenant_id` nel token appena emesso (dec. 8);
- **la chiusura dell'invito precede la creazione dell'appartenenza** ed è condizionata a
  `status = 'pending'`: è ciò che rende impossibile un'appartenenza doppia con due richieste simultanee
  (dec. 14);
- **difetto del client condiviso corretto** (dec. 20): `unwrap` perdeva il corpo problem+json perché
  `openapi-fetch` consuma la risposta d'errore prima di restituirla. Senza questa correzione i due rifiuti
  tipizzati sarebbero stati indistinguibili — ed è un difetto che riguardava ogni chiamata del client;
- **`invitations.identity_id` è dichiarato nel manifesto ma esportato solo per gli inviti accettati**
  (dec. 22): su un invito in attesa quel valore direbbe all'account ciò che la storia gli tiene nascosto; a
  invito accettato quella persona è già un membro noto. Stessa forma della restrizione usata per
  `identity.active_membership_id` in UC 0117;
- **difetto ereditato corretto**: il ri-seme delle appartenceze usava `ON CONFLICT (id)` invece del vincolo
  vero e stampava un errore su ogni banca dati già migrata. Corretto perché ostruiva la verifica manuale di
  *questa* storia ed era una riga in un file di solo sviluppo (dec. 19).

## Invarianti appgrove

1. **Tenant ID solo dal JWT verificato** — le tre operazioni nuove di `core` prendono il perimetro dal `sub`
   del token, mai da un identificativo del chiamante. L'account di destinazione non arriva **mai** dal
   chiamante: è quello di una riga di invito verificata (accettazione) o di un account appena creato nella
   stessa transazione (percorso B). La sfida di scelta **scrive un suggerimento**, non un claim: il claim
   continua a essere calcolato solo alla creazione del token, riverificando l'appartenenza.
2. **Filtro row-level** — le letture e le scritture che attraversano gli account sono **native e
   dichiaratamente senza filtro**, con la ragione dell'assenza scritta nel javadoc, come `tenantsOf` e
   `activeAccountsOf` di UC 0116/0117. Tutto il resto conserva il discriminatore. L'unico punto in cui il
   confine si attraversa di proposito è `MembershipRepository.createMembership`, che pretende un
   `tenant_id` **esplicito** e lo dice a voce alta.
3. **Modulo Terraform `microsaas_app`** — non toccato (nessuna app nuova, nessuna infrastruttura).
4. **Logging strutturato** — gli eventi nuovi (`member.invitation.accepted/rejected`,
   `account.self-created`) portano `user_id`, `tenant_id` e soli identificativi opachi: **mai** l'indirizzo
   dell'invitato, come già `member.invited`.

## Note per il revisore

- **Contratto cross-area**: `POST /api/auth/login` e `/login/2fa` acquistano una **terza** forma di
  risposta (`account_selection_required`), additiva come fu `mfa_required`; nuovo `POST /login/account` e
  nuovo `POST /invitations/lookup`. In `core`: tre operazioni nuove sotto `/api/platform/v1/me`. Lo spec
  OpenAPI è rigenerato dalla compilazione e i tipi del client frontend con `npm run gen`.
- **`loginMfa` cambia tipo di ritorno** (`Session` → `LoginResult`) in entrambi i fornitori: la scelta
  dell'account può servire anche dopo il secondo fattore.
- **Due collaudi esistenti sono stati riscritti, non indeboliti**: `piuAppartenenzeSenzaScelta…` e
  `accountAttivoManomessoNonProduceMaiUnClaim` asserivano un `409`; ora asseriscono la sfida **e** che
  nessun token esista — la proprietà essenziale è la stessa, il comportamento è migliore.
- **Decisioni differite tracciate** (nessuna lasciata in conversazione): limite al numero di account che una
  persona può aprire e conteggio dei posti → **UC 0103**; liberare un indirizzo dopo la cancellazione di
  un'identità → **UC 0033**; tensione fra manifesto dei dati ed esportazione dell'account, e passo condiviso
  del gate legale → **docs/_BACKLOG.md**. I punti aperti di UC 0116 e UC 0117 che appartenevano a questa
  storia sono **chiusi**, non spostati.
- **Gate privacy (UC 0031)**: due segnali, entrambi lo stesso campo `invitations.identity_id`.
  Classificazione **MINORE** — nessuna finalità nuova, nessuna base giuridica nuova, nessuna categoria
  particolare, nessun responsabile esterno nuovo, nessun aumento di versione di privacy policy o termini.
  Manifesto aggiornato nelle due lingue e registro dei trattamenti rigenerato con `npm run assemble`.
- **Gate parità scaffold**: percorso-sorgente toccato (`tools/platform-e2e/helpers/browser.ts`) →
  **deroga registrata** in `docs/_PARITA-SCAFFOLD.md`: l'aggiunta è puramente additiva, i passi che il
  modello importa non cambiano, e il journey core-loop di un'app generata entra in un account una volta sola.
- **Promemoria landing**: nessuna landing pubblicata è toccata (nessuna superficie di app: solo piattaforma).
- **`run-tests.sh` non richiede modifiche**: nessun modulo aggiunto o rimosso, nessun comando di test
  cambiato; i due collaudi end-to-end nuovi rientrano negli insiemi già raccolti.

## Test

- **`services/core`** — `MeInvitationsApiTest` (nuovo, 6 prove): percorso A intero (seconda appartenenza,
  una sola identità, invito `accepted`, account attivo sul nuovo), rifiuto dell'invito, invito di un altro
  indirizzo e invito scaduto → `404`, «già membro» che chiude l'invito senza appartenenza doppia, percorso B.
  `InvitationLifecycleTest` (esteso, +3): **esiti indistinguibili** fra identità esistente e sconosciuta
  (stesso codice, stesse chiavi, differenza solo in banca dati) e i due rifiuti leciti riconoscibili dal
  `type`. `PlatformGdprContractTest`/`PersonalDataManifestTest` verdi con la voce nuova del manifesto.
- **`services/auth`** — `ActiveAccountTokenTest` (riscritto + 3 nuove): sfida di scelta senza token, scelta
  che produce la sessione **e** conserva la scelta, account non proprio → `404`, token di scelta non valido →
  `401`. `InviteAcceptTest` (+3): ispezione dell'invito (`register`/`signin`), token non valido/scaduto,
  indirizzo di un'identità **cancellata** rifiutato con un messaggio in iscrizione e in accettazione.
  `CognitoAuthFlowsTest` (+2): la stessa sfida sul fornitore Cognito, riconosciuta dall'assenza del claim, e
  la scelta di un account non proprio.
- **`frontend`** — `PendingInvitesSection.test.tsx` (nuovo, 4): chi invita e chi paga il posto, sezione
  assente senza inviti, accettazione che **ricarica**, rifiuto che fa sparire la voce. `LoginPage.test`
  (+1): schermata di scelta e nessuna sessione finché non si è scelto. `AcceptInvitePage.test` (+1): a chi
  ha già un'identità **non** si chiede una parola d'accesso. `MembersPage.test` (+2): due messaggi distinti e
  la regola del posto. `problem.test` (+1): il problem+json non si perde quando il corpo è già consumato.
- **End-to-end** — `J-INVITE-EXISTING` (stack vero: invito, risposta senza traccia dell'identità,
  accettazione dal cruscotto, seconda appartenenza, passaggio fra i due account), `L2-INVITE-EXISTING`,
  `L2-ACCOUNT-CHOICE`. **Copertura e2e**: 0118 passa da esenzione `non-implementato` a use case *con
  superficie*, tre percorsi nuovi nel registro, `node tools/e2e-coverage/check.mjs` verde.
- **Esito della suite completa** (`./run-tests.sh` senza parametri): **verde in tutte e otto le aree** —
  backend, frontend, infra, compliance, tooling, smoke, platform, site.

## Stato criteri di accettazione

- [x] L'invito a un indirizzo che ha già un'identità si crea e la risposta è **identica** a quella di un
      indirizzo sconosciuto, provato da un collaudo dedicato.
- [x] «Già membro» e «invito già in attesa» producono due rifiuti distinti e riconoscibili da un programma,
      con due testi localizzati diversi.
- [x] Chi ha già un'identità vede l'invito nel cruscotto, lo accetta dalla propria sessione e ottiene una
      seconda appartenenza; il nuovo account è quello attivo e compare nel selettore.
- [x] Un invito indirizzato a un altro indirizzo non è accettabile dall'identità in sessione (`404`).
- [x] Chi è membro di un'azienda apre un proprio account dalla pagina Account e vi si trova `owner`, con una
      sola identità e due appartenenze.
- [x] Con più appartenenze attive e nessuna scelta valida, l'accesso mostra la schermata di scelta e, scelto
      l'account, la sessione nasce con quel claim.
- [x] Un indirizzo appartenente a un'identità cancellata dà un rifiuto comprensibile, non un errore.
- [x] `./run-tests.sh` (suite completa) verde.
