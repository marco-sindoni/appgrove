# Piano di lavoro — UC 0116 · Identità e appartenenze

**Storia**: [0116](../story/0116-identita-e-appartenenze.md) · **Aree toccate**: `services/core`, `services/auth`, `infra` (funzione del token), conformità
**Dimensione stimata**: grande — è una migrazione di modello, non un'aggiunta · **Prerequisiti**: nessuno (prima dell'epica)

## Prima di iniziare: cosa rompe

Questa storia cambia **dove vive l'identità di una persona**. Prima di scrivere, fare l'elenco dei punti che
la assumono dentro l'account:

```bash
grep -rn "platform.users" services/ infra/ tools/ --include='*.java' --include='*.py' --include='*.sql' | grep -v test
```

I punti noti: [PlatformWriter.java](../../../../services/auth/src/main/java/app/appgrove/auth/PlatformWriter.java)
(crea account con owner, crea utente in account, legge la lingua),
[UserDirectory.java](../../../../services/auth/src/main/java/app/appgrove/auth/local/UserDirectory.java)
(fornitore locale), [handler.py](../../../../infra/modules/platform_shared/lambda/pre_token_gen/handler.py)
(funzione del token), `User.java`/`UserResource.java` nel core, e la traccia di controllo. L'elenco fatto
**prima** è la differenza fra una migrazione e una settimana di sorprese.

## Passo 1 — Migrazione della banca dati

**File nuovo**: `services/core/src/main/resources/db/migration/V17__identity_membership.sql`
(l'ultima esistente è `V16__support_ticket_source_review.sql` — **riverificare**; nota: il piano di
[UC 0098](0098-modello-dati-accesso-per-applicazione.md) prevedeva `V17` per gli accessi, che ora slitta al
numero successivo, perché questa storia si esegue prima).

In una sola migrazione, nell'ordine:

1. creazione di `platform.identity` (colonne della storia §7), **senza** colonna dell'account: è una tabella
   di piattaforma come `platform.app`;
2. creazione di `platform.membership` con il discriminatore di account e l'unicità su `(tenant_id,
   identity_id)` limitata alle righe vive;
3. **travaso**: per ogni riga viva di `platform.users`, una identità (indirizzo, identificativo di
   autenticazione, nome, lingua, stato) e una appartenenza (account, ruolo, stato);
4. rimozione degli indici unici globali da `platform.users` — sono il vincolo che questa storia scioglie;
5. `platform.users` **resta** in questo passo, come rete di sicurezza, e si rimuove in una migrazione
   successiva quando nulla la legge più. Toglierla subito significa non avere via di ritorno se il travaso
   ha un difetto.

**Verifica del travaso nella migrazione stessa**: un controllo che confronti i conteggi (utenti vivi =
identità = appartenenze) e faccia **fallire** la migrazione se non tornano. Una migrazione che perde righe in
silenzio è il difetto peggiore possibile qui.

## Passo 2 — Entità, repository e interfaccia del core

**File nuovi** in `services/core/src/main/java/app/appgrove/core/platform/`:

- `Identity.java` — entità **senza** `BaseTenantEntity` (non è dell'account). Porta le annotazioni di dato
  personale su indirizzo e nome: sono gli stessi dati di prima, ma cambiano posto e il manifesto deve
  seguirli.
- `Membership.java` — entità che estende `BaseTenantEntity`, con `identityId`, `role` (`owner`/`member`),
  `status`.
- `IdentityRepository.java`, `MembershipRepository.java` — con le due domande della storia §4.3:
  `membersOf(tenant)` e `membershipsOf(identityId)`. La seconda **non** porta il filtro per account, per
  costruzione: va marcata con un commento che dice perché, altrimenti sembra un difetto.

**Modifiche**: `UserResource.java` e i suoi oggetti di trasporto passano da `User` a `Membership` +
`Identity`. L'interfaccia esposta **non cambia forma** (la schermata dei membri continua a vedere un elenco
di persone dell'account): cambia da dove vengono i campi. Questa è la parte noiosa e va fatta con calma.

**Attenzione — `platform.app_access` di UC 0098** riferirà l'identità, non l'utente. Poiché 0098 si esegue
dopo, basta che nasca già con `identity_id`: nessuna migrazione doppia.

## Passo 3 — Il fornitore di identità e la funzione del token

**Modifiche**:

- [PlatformWriter.java](../../../../services/auth/src/main/java/app/appgrove/auth/PlatformWriter.java) —
  `createAccountWithOwner` diventa «crea account + identità (se non c'è) + appartenenza owner»;
  `createUserInTenant` diventa «crea appartenenza», con l'identità creata solo quando manca. `insertUser`
  si scinde in due inserimenti. È il file che concentra il rischio: qui nasce ogni persona della piattaforma.
- [UserDirectory.java](../../../../services/auth/src/main/java/app/appgrove/auth/local/UserDirectory.java) —
  la lettura per l'accesso locale interroga identità + appartenenze.
- [handler.py](../../../../infra/modules/platform_shared/lambda/pre_token_gen/handler.py) — la ricerca
  passa da «la riga di questo identificativo» a «le appartenenze attive di questa identità». In **questa**
  storia si mantiene il comportamento di oggi quando c'è una sola appartenenza; la scelta fra più
  appartenenze è di [UC 0117](0117-account-attivo-e-selettore.md). Da non anticipare: una funzione che
  scegli senza un criterio scritto scegli male.

## Passo 4 — Conformità

- **Manifesto dei dati della piattaforma**: indirizzo, nome e lingua passano dall'entità di account
  all'entità di piattaforma. La classificazione va **rifatta**, non spostata di riga: cambia chi risponde
  per quel dato. Eseguire `npm run privacy-scan` in `tools/compliance` e rispondere ai segnali.
- **Registro dei trattamenti**: la voce sulle persone dell'account si sdoppia — identità (piattaforma) e
  appartenenza (account).
- **Esportazione e cancellazione** (UC 0032/0033): l'esportazione di un account comprende le appartenenze e
  i dati dell'account, **non** l'identità intera della persona; la cancellazione di un account non cancella
  un'identità che ha altre appartenenze. Due modifiche piccole e due collaudi che valgono molto.

## Passo 5 — Collaudi

- `MembershipTest` / `IdentityTest` — ciclo di vita: due appartenenze, rifiuto della seconda nello stesso
  account, uscita da uno, ultima appartenenza.
- **Estensione** di `MultiTenancyTest.java` — la stessa identità in due account non attraversa il confine.
  Va **là**, dove vivono le prove di separazione.
- `MigrationCountsTest` — sui dati di riferimento: conteggi a confronto prima e dopo. È il collaudo che
  permette di dormire.
- **Conformità**: cancellazione dell'account A con identità che sopravvive; cancellazione dell'ultima
  appartenenza che rende l'identità cancellabile.

## Passo 6 — Documenti

- [docs/02 §14](../../../02-auth-sicurezza.md) — la decisione «1 utente → 1 tenant» è **superata**: si
  riscrive dicendo cosa la sostituisce e perché, senza cancellare la storia della scelta precedente.
- [docs/01](../../../01-architettura.md) e [docs/05](../../../05-persistenza-dati.md) — dove descrivono
  l'appartenenza ripiegata sull'utente.

## Verifica finale

```bash
cd services && mvn -B test
cd .. && ./run-tests.sh backend compliance
```

## Trappole note

1. **Il travaso senza controllo dei conteggi** è il modo più facile di perdere persone in silenzio. Il
   controllo va **dentro** la migrazione, non in un collaudo che qualcuno potrebbe non eseguire.
2. **Rimuovere `platform.users` subito**: tentazione forte, errore serio. Serve la via di ritorno.
3. **La funzione del token gira in un altro linguaggio e in un altro luogo** (Python, dentro
   l'infrastruttura): è facile aggiornare il core e dimenticarla. Il collaudo `test_handler.py` esiste già
   accanto ad essa: estenderlo nello stesso momento.
4. **Il fornitore locale deve restare in parità**: se divergono, i collaudi locali dicono una cosa e
   l'ambiente reale un'altra. La parità è dichiarata nel commento di `handler.py`: mantenerla vera.
5. **La lingua della persona** si legge oggi per indirizzo di posta (`localeOf`): dopo, si legge
   dall'identità. Piccolo, ma se sfugge le comunicazioni partono nella lingua sbagliata.
