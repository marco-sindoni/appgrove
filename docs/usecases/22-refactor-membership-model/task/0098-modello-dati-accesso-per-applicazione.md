# Piano di lavoro — UC 0098 · Modello dati dell'accesso per applicazione

**Storia**: [0098](../story/0098-modello-dati-accesso-per-applicazione.md) · **Area toccata**: `services/core`
**Dimensione stimata**: media (1 change) · **Prerequisiti**: [UC 0116](0116-identita-e-appartenenze.md) — la tabella nasce riferendo l'**identità**, non l'utente-dentro-l'account

## Passo 1 — Migrazione della banca dati

**File nuovo**: `services/core/src/main/resources/db/migration/V20__app_access.sql`
(numero **corretto in sede di implementazione**, change 0091: `V17` è di UC 0116, `V18` di UC 0117 e `V19`
di UC 0118 — tutte già in main. Il piano diceva `V18` perché scritto prima di quelle due change.
`platform.identity` e `platform.membership` esistono già: `app_access.identity_id` può riferire
direttamente `platform.identity(id)`, senza migrazione doppia.)

Contenuto: creazione di `platform.app_access` con le colonne della storia §7, i tre indici (unicità sulla
terna limitata alle righe vive, indice per persona, indice per applicazione) e il commento sulla tabella che
dice a che serve. Nessun vincolo di chiave esterna verso `platform.accounts` sul campo `tenant_id`, coerentemente
con la scelta già in vigore (il discriminatore è una chiave logica, non una chiave esterna).

Nella stessa migrazione **non** si tocca il ruolo di piattaforma: la conversione dei valori è di UC 0113.
**Deciso in sede di implementazione (change 0091): nessun vincolo di controllo su `membership.role`, nemmeno
condizionato.** L'ipotesi «lo aggiungo se non esistono righe `admin`» è stata scartata perché produrrebbe
schemi diversi fra ambienti a seconda dei dati, che è peggio dell'assenza del vincolo; e un vincolo aggiunto
prima della conversione rifiuterebbe di applicarsi — una migrazione che non parte. Il vincolo lo aggiunge
UC 0113 dopo il passo 3 della conversione. Sulla nuova tabella il vincolo sul ruolo di **applicazione** c'è
invece, perché non esistono righe pregresse da rispettare.

## Passo 2 — Entità e repository

**File nuovi**, in `services/core/src/main/java/app/appgrove/core/platform/`:

- `AppAccess.java` — entità che estende `BaseTenantEntity` (come `Membership` e `Invitation`), con
  `@SQLRestriction("deleted_at is null")`, i campi `appId`, `identityId`, `role`, `grantedBy`.
  **Corretto in sede di implementazione (change 0091)**: `identityId` **è** annotato `@PersonalData`, come
  `membership.identity_id` e per la stessa ragione — non contiene né nome né indirizzo ma dice qualcosa di
  una persona, e qui dice di più (quale applicazione usa e con quale potere). Il collaudo del manifesto è
  bidirezionale: una voce dichiarata senza annotazione è rossa quanto il contrario.
- `AppRole.java` — enumerazione `viewer`, `editor`, `admin`, con il metodo di confronto
  `atLeast(AppRole)` che incarna l'ordinamento. Il metodo va qui e **non** duplicato altrove: lo useranno
  il varco di UC 0099 e l'interfaccia.
- `AppAccessRepository.java` — repository su `PanacheRepositoryBase`, con: `findByIdentity(identityId)`,
  `findByApp(appId)`, `findOne(appId, identityId)`, `roleOf(appId, identityId)`. Il filtro per account è
  automatico (discriminatore), da **non** riscrivere a mano. La lettura per identificativo di
  autenticazione non serve qui: la fa il varco di UC 0099.

**Modifica**: `MembershipRole.java` (il nome dopo la change 0088; il piano diceva ancora `UserRole`) —
rimozione del valore `admin`. **Fin dove arriva la rimozione, deciso dalla change 0091**: si toglie
dall'enumerazione e da tutto ciò che *offre* o *scrive* quel valore (invito, cambio di ruolo della persona,
seme di sviluppo, i due selettori della schermata dei membri); **non** si tocca la costante `Roles.ADMIN` né
le annotazioni `@RolesAllowed` che la nominano, che leggono il **claim del token** e restano come tolleranza
dei token già emessi (UC 0113 §6). La compilazione, di conseguenza, non si rompe da nessuna parte.

## Passo 3 — Regole di autorizzazione, come funzione pura

**File nuovo**: `AppAccessRules.java` — classe senza stato, sul modello di
[EntitlementAccess.java](../../../../services/core/src/main/java/app/appgrove/core/billing/EntitlementAccess.java),
che risponde a: «questo chiamante può concedere/revocare/cambiare ruolo su questa applicazione?». Ingredienti
in ingresso: ruolo di piattaforma del chiamante, suo eventuale ruolo su quella applicazione, applicazione
bersaglio. Nessun accesso alla banca dati: i chiamanti raccolgono gli ingredienti.

È il punto in cui la regola resta **una**, invece di essere ripetuta in ogni operazione. Il collaudo di questa
classe è il più importante del passo.

## Passo 4 — Interfaccia di rete

**File nuovi**: `AppAccessResource.java`, `AppAccessDtos.java`, sul modello di `UserResource.java`.

Operazioni:

| Verbo | Percorso | Chi | Nota |
|---|---|---|---|
| lettura | `/api/platform/v1/apps/{appId}/access` | chiunque abbia accesso a quella applicazione | elenco, con l'owner **aggiunto in testa** dal codice (non ha righe) |
| creazione | `/api/platform/v1/apps/{appId}/access` | owner, `admin` di quella applicazione | corpo: identificativo persona + ruolo |
| modifica | `/api/platform/v1/apps/{appId}/access/{identityId}` | owner, `admin` | solo il ruolo |
| cancellazione | `/api/platform/v1/apps/{appId}/access/{identityId}` | owner, `admin` | cancellazione logica |

Ogni operazione: verifica dei diritti dell'account sull'applicazione (riusando
`EntitlementReadModel`/`EntitlementAccess`), verifica dello stato attivo della persona, traccia di controllo
con `AuditLogger` e **soli identificativi opachi**, log strutturato con account, applicazione e persona.

**Trappola**: `@RolesAllowed` non basta più. Il ruolo di applicazione non è nel token, quindi la protezione
è `@Authenticated` più la verifica esplicita dentro il metodo, dentro la transazione, con
`AppAccessRules`. Scriverlo nel commento della classe, altrimenti qualcuno «semplificherà» rimettendo
`@RolesAllowed` e aprirà un varco.

## Passo 5 — Vincoli sull'owner

**Modifiche**: `UserResource.java` — il divieto di rimuovere, retrocedere e sospendere l'owner oggi vive
nell'interfaccia ([MembersPage.tsx](../../../../frontend/apps/backoffice/src/pages/members/MembersPage.tsx))
come `isLastOwner`. Va portato nel servizio come rifiuto tipizzato. È una stretta e va provata.

## Passo 6 — Manifesto dei dati

**Modifica**: `PlatformDataContract.java` — dichiarazione della nuova tabella come dato di autorizzazione,
con le voci della storia §7. Il collaudo `PersonalDataManifestTest` verifica la coerenza fra annotazioni e
manifesto: se la tabella non porta dati personali va dichiarata come tale, non omessa.

## Passo 7 — Collaudi

**File nuovi** in `services/core/src/test/java/app/appgrove/core/`:

- `AppAccessRulesTest.java` — la funzione pura, tutti i casi della storia §9.
- `AppAccessApiTest.java` — sul modello di `AccountUserApiTest.java`: concessione, cambio, revoca, unicità,
  persona non attiva, applicazione senza diritto, `admin` di un'altra applicazione.
- **Estensione** di `MultiTenancyTest.java` — la separazione fra account sulla nuova tabella. Va **là**,
  dove vivono le prove di separazione, non in un file nuovo.

## Verifica finale

```bash
cd services && mvn -B -pl core -am test
cd .. && ./run-tests.sh backend
```

## Trappole note

1. **La rimozione del valore `admin`** — trappola **evitata** dalla change 0091 fermandosi al confine del
   token: nessun servizio ha perso la compilazione, e nessuna operazione è stata riscritta. Quello che si
   rompe davvero sono i **collaudi e i dati di prova** che scrivevano `admin` come ruolo di appartenenza
   (seme, inviti del seme, alcune fixture): quelli sì, vanno cercati prima.
2. **L'owner senza righe di accesso** va ricordato in ogni lettura: la prova più utile è chiedere l'elenco
   degli accessi di un'applicazione appena installata e pretendere che contenga l'owner.
3. **Il numero della migrazione** va riverificato: se un'altra change ne ha aggiunta una, il numero cambia.
   È successo due volte fra la scrittura del piano e l'implementazione: `V18` → `V20`.
