# Piano di lavoro — UC 0098 · Modello dati dell'accesso per applicazione

**Storia**: [0098](../story/0098-modello-dati-accesso-per-applicazione.md) · **Area toccata**: `services/core`
**Dimensione stimata**: media (1 change) · **Prerequisiti**: [UC 0116](0116-identita-e-appartenenze.md) — la tabella nasce riferendo l'**identità**, non l'utente-dentro-l'account

## Passo 1 — Migrazione della banca dati

**File nuovo**: `services/core/src/main/resources/db/migration/V18__app_access.sql`
(`V17` è di [UC 0116](0116-identita-e-appartenenze.md), che si esegue prima; l'ultima esistente oggi è
`V16__support_ticket_source_review.sql` — **riverificare** entrambi i numeri al momento dell'implementazione).

Contenuto: creazione di `platform.app_access` con le colonne della storia §7, i tre indici (unicità sulla
terna limitata alle righe vive, indice per persona, indice per applicazione) e il commento sulla tabella che
dice a che serve. Nessun vincolo di chiave esterna verso `platform.users` sul campo `tenant_id`, coerentemente
con la scelta già in vigore (il discriminatore è una chiave logica, non una chiave esterna).

Nella stessa migrazione **non** si tocca il ruolo di piattaforma: la conversione dei valori è di UC 0113. Qui
si aggiunge soltanto il vincolo di controllo sui valori ammessi **se** e solo se non esistono ancora righe
`admin` (in sviluppo locale è così; in ambiente reale la conversione precede). Sicurezza: meglio nessun
vincolo di controllo qui che una migrazione che non parte in produzione.

## Passo 2 — Entità e repository

**File nuovi**, in `services/core/src/main/java/app/appgrove/core/platform/`:

- `AppAccess.java` — entità che estende `BaseTenantEntity` (come `User` e `Invitation`), con
  `@SQLRestriction("deleted_at is null")`, i campi `appId`, `identityId`, `role`, `grantedBy`. Nessuna
  annotazione di dato personale: non ne contiene.
- `AppRole.java` — enumerazione `viewer`, `editor`, `admin`, con il metodo di confronto
  `atLeast(AppRole)` che incarna l'ordinamento. Il metodo va qui e **non** duplicato altrove: lo useranno
  il varco di UC 0099 e l'interfaccia.
- `AppAccessRepository.java` — repository su `PanacheRepositoryBase`, con: `findByIdentity(identityId)`,
  `findByApp(appId)`, `findOne(appId, identityId)`, `existsForIdentity(appId, cognitoSub)`. Il filtro per
  account è automatico (discriminatore), da **non** riscrivere a mano.

**Modifica**: `UserRole.java` — rimozione del valore `admin` (l'enumerazione ora vive sull'appartenenza,
[UC 0116](0116-identita-e-appartenenze.md)). Attenzione: rompe la compilazione in ogni punto
che lo nomina. Elencarli prima di iniziare con `grep -rn "UserRole.admin\|Roles.ADMIN" services/`.

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

1. **La rimozione del valore `admin`** da `UserRole` rompe la compilazione in punti sparsi, compreso il
   Mini-CRM ([Roles.java](../../../../services/crm/src/main/java/app/appgrove/crm/Roles.java)) e il fornitore
   di identità locale. Fare l'elenco **prima**.
2. **L'owner senza righe di accesso** va ricordato in ogni lettura: la prova più utile è chiedere l'elenco
   degli accessi di un'applicazione appena installata e pretendere che contenga l'owner.
3. **Il numero della migrazione** va riverificato: se un'altra change ne ha aggiunta una, il numero cambia.
