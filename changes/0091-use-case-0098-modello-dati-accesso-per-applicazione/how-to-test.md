# Come verificare a mano la change 0091 — accesso per applicazione e ruolo di piattaforma a due valori

Questa storia è **prevalentemente modello dati**: il grosso della verifica non si vede con gli occhi, si legge nella
banca dati e si chiede alle API. Ma qualcosa **si vede**, e va guardato: il ruolo di piattaforma a due valori sparisce
dai selettori della schermata dei membri, e la persona che prima era «admin dell'account» non vede più le schermate
riservate all'owner.

Ogni voce è **azione → risultato atteso**. Se un'attesa non si verifica, fermati e riportalo: non è una sfumatura.

Ogni comando della guida è **completo e pronto da incollare**: nessuna scorciatoia da definire prima.

## 0. Preparazione

```bash
./app-start.sh
```

**Risultato atteso** — l'avvio arriva in fondo senza errori. Password di tutti gli utenti del seme: `Password1!`.

| Utente | Dove | Cosa serve qui |
|---|---|---|
| `owner@acme.test` | <https://app.local.appgrove.app> | titolare del conto Acme: concede e revoca accessi |
| `admin@acme.test` | <https://app.local.appgrove.app> | **la persona che cambia con questa change**: era «admin dell'account», ora è collaboratrice con ruolo `admin` sul Mini-CRM |
| `member@acme.test` | <https://app.local.appgrove.app> | collaboratrice, con ruolo `editor` sul Mini-CRM |
| `bob@bob.test` | <https://app.local.appgrove.app> | titolare di un **altro** conto: serve alla prova di separazione |
| `admin@appgrove.test` | <https://admin.local.appgrove.app> | console di piattaforma (per attivare il Mini-CRM) |

> Se lo stack era già acceso, rilancia `./app-start.sh`: il seme è idempotente ed è **cambiato** con questa change.
> Il ri-seme deve aggiornare le righe esistenti **senza errori**: la persona `admin@acme.test` passa da ruolo
> `admin` a `member`. Un messaggio `duplicate key` sarebbe un difetto da riportare.

### 0.1 La migrazione è passata

**Azione**

```bash
docker compose -f dev/docker-compose.yml exec -T postgres \
  psql -U appgrove -d appgrove -tAc "select version, description, success from platform.flyway_schema_history where version = '20';"
```

**Risultato atteso** — una riga `20|app access|t`. Se `success` non è `t`, **fermati qui**: il resto non ha senso.

## 1. La tabella c'è, con i suoi vincoli e i suoi indici (non visivo)

**Azione**

```bash
docker compose -f dev/docker-compose.yml exec -T postgres \
  psql -U appgrove -d appgrove -c "\d platform.app_access"
```

**Risultato atteso** — le colonne `id, tenant_id, app_id, identity_id, role, granted_by` più quelle di audit
(`created_at, updated_at, created_by, updated_by, deleted_at`), e **tutti e tre** gli indici:

- `ux_app_access_tenant_app_identity` — **unico** e **parziale**: nella definizione deve comparire
  `WHERE (deleted_at IS NULL)`. Senza «parziale», revocare un accesso chiuderebbe la porta per sempre;
- `ix_app_access_tenant_identity` — «quali applicazioni vede questa persona?»;
- `ix_app_access_tenant_app` — «chi ha accesso a questa applicazione?».

**Azione**

```bash
docker compose -f dev/docker-compose.yml exec -T postgres \
  psql -U appgrove -d appgrove -c "select conname, pg_get_constraintdef(oid) from pg_constraint
        where conrelid = 'platform.app_access'::regclass order by conname;"
```

**Risultato atteso** — il controllo `ck_app_access_role` sui tre valori `viewer`, `editor`, `admin`; due chiavi
esterne, verso `platform.app` e verso `platform.identity`; e **nessuna** chiave esterna sul `tenant_id` — è una
chiave logica governata dal token, non una chiave esterna.

## 2. Le righe attese dopo il ri-seme (non visivo)

**Azione**

```bash
docker compose -f dev/docker-compose.yml exec -T postgres \
  psql -U appgrove -d appgrove -c "select i.email, m.role from platform.membership m
         join platform.identity i on i.id = m.identity_id
        where m.tenant_id = 'a0000000-0000-4000-8000-000000000001' order by m.role, i.email;"
```

**Risultato atteso** — tre righe: `owner@acme.test` → **`owner`**, `admin@acme.test` e `member@acme.test` →
**`member`**.

**Azione**

```bash
docker compose -f dev/docker-compose.yml exec -T postgres \
  psql -U appgrove -d appgrove -tAc "select count(*) from platform.membership where role = 'admin';"
```

**Risultato atteso** — **`0`**. Il valore `admin` non esiste più a livello di account: è il cuore della change.

**Azione**

```bash
docker compose -f dev/docker-compose.yml exec -T postgres \
  psql -U appgrove -d appgrove -c "select i.email, app.slug, aa.role from platform.app_access aa
         join platform.identity i on i.id = aa.identity_id
         join platform.app app on app.id = aa.app_id
        where aa.deleted_at is null order by aa.role;"
```

**Risultato atteso** — **due** righe, entrambe sull'applicazione `crm`: `admin@acme.test` con ruolo **`admin`** e
`member@acme.test` con ruolo **`editor`**. **Nessuna riga per `owner@acme.test`**: l'accesso dell'owner è implicito e
non si scrive. È la traduzione che UC 0113 farà sui conti reali, già applicata al dato di sviluppo.

**Azione**

```bash
docker compose -f dev/docker-compose.yml exec -T postgres \
  psql -U appgrove -d appgrove -c "select email, role from platform.invitations where status = 'pending' order by email;"
```

**Risultato atteso** — due inviti in attesa, **entrambi** di ruolo `member`. L'indirizzo `invitee-admin@acme.test`
conserva il nome storico, il ruolo no: chi entra non porta con sé alcun potere.

## 3. Attiva il Mini-CRM (presupposto del punto 4)

Nel seme l'applicazione `crm` è **disabilitata** di proposito. Entra in <https://admin.local.appgrove.app> come
`admin@appgrove.test`, sezione **App**, e portala ad `active`.

**Risultato atteso** — la console mostra `crm` come attiva. Se non la attivi, le chiamate del punto 4 rispondono
`urn:appgrove:app-access:not-entitled` — che è, per la cronaca, esattamente il controllo «l'account ha diritto a
questa applicazione» che funziona.

## 4. Le API dell'accesso — il cuore della storia (non visivo)

Prendi un token dell'owner e gli identificativi che servono:

```bash
TOKEN=$(curl -sk https://app.local.appgrove.app/api/auth/login \
  -H 'content-type: application/json' \
  -d '{"email":"owner@acme.test","password":"Password1!"}' \
  | python3 -c 'import json,sys; print(json.load(sys.stdin)["access_token"])')

CRM=$(docker compose -f dev/docker-compose.yml exec -T postgres psql -U appgrove -d appgrove -tAc "select id from platform.app where slug = 'crm';")
FATT=$(docker compose -f dev/docker-compose.yml exec -T postgres psql -U appgrove -d appgrove -tAc "select id from platform.app where slug = 'fatture';")
OWNER=$(docker compose -f dev/docker-compose.yml exec -T postgres psql -U appgrove -d appgrove -tAc "select id from platform.identity where email = 'owner@acme.test';")
ADMIN=$(docker compose -f dev/docker-compose.yml exec -T postgres psql -U appgrove -d appgrove -tAc "select id from platform.identity where email = 'admin@acme.test';")
MEMBRO=$(docker compose -f dev/docker-compose.yml exec -T postgres psql -U appgrove -d appgrove -tAc "select id from platform.identity where email = 'member@acme.test';")
BOB=$(docker compose -f dev/docker-compose.yml exec -T postgres psql -U appgrove -d appgrove -tAc "select id from platform.identity where email = 'bob@bob.test';")
```

### 4.1 Chi ha accesso: l'owner è in testa, senza avere una riga

**Azione**

```bash
curl -sk https://app.local.appgrove.app/api/platform/v1/apps/$CRM/access \
  -H "Authorization: Bearer $TOKEN" | python3 -m json.tool
```

**Risultato atteso** — un elenco la cui **prima voce è l'owner**, con `"implicit": true` e ruolo `admin`, seguito
dalle due persone abilitate con `"implicit": false`. Confronta con il punto 2: nella tabella l'owner **non c'è**, e
compare comunque. È il costo dell'accesso implicito, pagato dal servizio.

### 4.2 La concessione su chi ha già accesso è un cambio di ruolo

**Azione**

```bash
curl -sk -o /dev/null -w '%{http_code}\n' -X POST \
  https://app.local.appgrove.app/api/platform/v1/apps/$CRM/access \
  -H "Authorization: Bearer $TOKEN" -H 'content-type: application/json' \
  -d "{\"identityId\":\"$ADMIN\",\"role\":\"viewer\"}"
docker compose -f dev/docker-compose.yml exec -T postgres \
  psql -U appgrove -d appgrove -tAc "select role from platform.app_access where identity_id = '$ADMIN' and deleted_at is null;"
docker compose -f dev/docker-compose.yml exec -T postgres \
  psql -U appgrove -d appgrove -tAc "select count(*) from platform.app_access where identity_id = '$ADMIN' and deleted_at is null;"
```

**Risultato atteso** — **`200`** (non `201`), ruolo `viewer`, e **una sola** riga: non nasce un doppione. Riporta
tutto come prima rifacendo la stessa chiamata con `"role":"admin"` (attesa: `200`, ruolo `admin`).

### 4.3 L'owner è intoccabile: tre rifiuti tipizzati

**Azione**

```bash
curl -sk -X POST https://app.local.appgrove.app/api/platform/v1/apps/$CRM/access \
  -H "Authorization: Bearer $TOKEN" -H 'content-type: application/json' \
  -d "{\"identityId\":\"$OWNER\",\"role\":\"viewer\"}" | python3 -m json.tool
curl -sk -X PUT https://app.local.appgrove.app/api/platform/v1/apps/$CRM/access/$OWNER \
  -H "Authorization: Bearer $TOKEN" -H 'content-type: application/json' \
  -d '{"role":"viewer"}' | python3 -m json.tool
curl -sk -X DELETE https://app.local.appgrove.app/api/platform/v1/apps/$CRM/access/$OWNER \
  -H "Authorization: Bearer $TOKEN" | python3 -m json.tool
```

**Risultato atteso** — **tre volte `409`**, ognuna con `"type": "urn:appgrove:app-access:owner-implicit"` e un
messaggio comprensibile in italiano. Non un `500`, non un `403` generico.

### 4.4 La persona di un altro conto non esiste

**Azione**

```bash
curl -sk -o /dev/null -w '%{http_code}\n' -X POST \
  https://app.local.appgrove.app/api/platform/v1/apps/$CRM/access \
  -H "Authorization: Bearer $TOKEN" -H 'content-type: application/json' \
  -d "{\"identityId\":\"$BOB\",\"role\":\"viewer\"}"
```

**Risultato atteso** — **`404`**, non `403`. La differenza *è* il punto: un `403` direbbe «esiste ma non puoi», cioè
rivelerebbe a Acme l'esistenza di una persona del conto di Bob.

### 4.5 Una persona non attiva non riceve accesso

**Azione** — sospendi `member@acme.test` dalla schermata **Members** del backoffice (come owner), poi:

```bash
curl -sk -X POST https://app.local.appgrove.app/api/platform/v1/apps/$FATT/access \
  -H "Authorization: Bearer $TOKEN" -H 'content-type: application/json' \
  -d "{\"identityId\":\"$MEMBRO\",\"role\":\"viewer\"}" | python3 -m json.tool
```

**Risultato atteso** — **`409`** con `"type": "urn:appgrove:app-access:person-not-active"`. Riattivala dalla stessa
schermata prima di proseguire.

### 4.6 L'admin di una applicazione opera **solo** su quella

**Azione** — prendi un token di `admin@acme.test` (che sul `crm` ha ruolo `admin`) e prova su **entrambe** le
applicazioni:

```bash
TOKEN_ADMIN=$(curl -sk https://app.local.appgrove.app/api/auth/login \
  -H 'content-type: application/json' \
  -d '{"email":"admin@acme.test","password":"Password1!"}' \
  | python3 -c 'import json,sys; print(json.load(sys.stdin)["access_token"])')

curl -sk -o /dev/null -w 'sul crm: %{http_code}\n' -X POST \
  https://app.local.appgrove.app/api/platform/v1/apps/$CRM/access \
  -H "Authorization: Bearer $TOKEN_ADMIN" -H 'content-type: application/json' \
  -d "{\"identityId\":\"$MEMBRO\",\"role\":\"viewer\"}"

curl -sk -o /dev/null -w 'su fatture: %{http_code}\n' -X POST \
  https://app.local.appgrove.app/api/platform/v1/apps/$FATT/access \
  -H "Authorization: Bearer $TOKEN_ADMIN" -H 'content-type: application/json' \
  -d "{\"identityId\":\"$MEMBRO\",\"role\":\"viewer\"}"
```

**Risultato atteso** — sul `crm` **`200`** (era `editor`, diventa `viewer`: cambio di ruolo); su `fatture` **`403`**.
Il potere dell'`admin` è circoscritto alla **sua** applicazione, e il token non lo aiuta — il ruolo per applicazione
non è nel token, si legge dal modello. Riporta `member@acme.test` a `editor` sul `crm`.

### 4.7 Chi non ha accesso a una applicazione non ne conosce nemmeno le persone

**Azione**

```bash
curl -sk -o /dev/null -w '%{http_code}\n' \
  https://app.local.appgrove.app/api/platform/v1/apps/$FATT/access \
  -H "Authorization: Bearer $TOKEN_ADMIN"
```

**Risultato atteso** — **`403`**. `admin@acme.test` non ha accesso a `fatture`, quindi non vede chi ce l'ha.

### 4.8 La revoca è una cancellazione logica

**Azione**

```bash
curl -sk -o /dev/null -w '%{http_code}\n' -X DELETE \
  https://app.local.appgrove.app/api/platform/v1/apps/$CRM/access/$MEMBRO \
  -H "Authorization: Bearer $TOKEN"
docker compose -f dev/docker-compose.yml exec -T postgres \
  psql -U appgrove -d appgrove -c "select role, deleted_at is not null as revocato from platform.app_access
        where identity_id = '$MEMBRO';"
```

**Risultato atteso** — **`204`**, e la riga **c'è ancora** con `revocato = t`. Poi riconcedi lo stesso accesso
(`POST` con `"role":"editor"`): attesa **`201`** e una riga viva nuova accanto a quella revocata. Se il vincolo unico
non fosse parziale, questa riconcessione fallirebbe.

### 4.9 Chi esce dall'account porta via i suoi permessi

**Azione** — dalla schermata **Members** rimuovi `member@acme.test` (bottone **Remove**, poi conferma), quindi:

```bash
docker compose -f dev/docker-compose.yml exec -T postgres \
  psql -U appgrove -d appgrove -c "select aa.role, aa.deleted_at is not null as revocato from platform.app_access aa
        where aa.identity_id = '$MEMBRO';"
```

**Risultato atteso** — le sue righe di accesso risultano **tutte revocate** (`revocato = t`), non solo
l'appartenenza. Un permesso che sopravvive alla persona tornerebbe valido il giorno in cui quella persona rientra, in
silenzio e con i poteri di prima. La riga **resta** (cancellazione logica): la storia è leggibile, il permesso no.

> Attenzione a non confondere con la **sospensione** (punto 4.5): quella è reversibile e **non** tocca gli accessi.
> Al termine, `./app-start.sh` o `./dev.sh seed` rimettono il seme come prima.

## 5. Il vincolo dell'ultimo owner arriva dal servizio (non visivo)

**Azione**

```bash
curl -sk -o /dev/null -w 'rimozione: %{http_code}\n' -X DELETE \
  https://app.local.appgrove.app/api/platform/v1/users/$OWNER -H "Authorization: Bearer $TOKEN"
curl -sk -o /dev/null -w 'retrocessione: %{http_code}\n' -X PATCH \
  https://app.local.appgrove.app/api/platform/v1/users/$OWNER \
  -H "Authorization: Bearer $TOKEN" -H 'content-type: application/json' -d '{"role":"member"}'
curl -sk -o /dev/null -w 'sospensione: %{http_code}\n' -X PATCH \
  https://app.local.appgrove.app/api/platform/v1/users/$OWNER \
  -H "Authorization: Bearer $TOKEN" -H 'content-type: application/json' -d '{"status":"suspended"}'
docker compose -f dev/docker-compose.yml exec -T postgres \
  psql -U appgrove -d appgrove -c "select role, status from platform.membership where identity_id = '$OWNER';"
```

**Risultato atteso** — **tre volte `409`**, e l'appartenenza dell'owner ancora `owner` / `active`. Prima di questa
change queste tre richieste **passavano**: il divieto viveva soltanto come comando disabilitato nell'interfaccia, e
bastava chiamare l'API per lasciare un conto senza nessuno che potesse governarlo.

## 6. Il ruolo ritirato non si può più assegnare (non visivo)

**Azione**

```bash
curl -sk -o /dev/null -w 'cambio ruolo: %{http_code}\n' -X PATCH \
  https://app.local.appgrove.app/api/platform/v1/users/$MEMBRO \
  -H "Authorization: Bearer $TOKEN" -H 'content-type: application/json' -d '{"role":"admin"}'
curl -sk -o /dev/null -w 'invito: %{http_code}\n' -X POST \
  https://app.local.appgrove.app/api/platform/v1/invitations \
  -H "Authorization: Bearer $TOKEN" -H 'content-type: application/json' \
  -d '{"email":"prova-0091@acme.test","role":"admin"}'
```

**Risultato atteso** — **`400`** su entrambe. `admin` non è più un ruolo di conto, né in assegnazione né in invito.

## 7. Quello che si vede: la schermata dei membri

Entra in <https://app.local.appgrove.app> come **`owner@acme.test`** e apri **Members**.

| Cosa guardare | Risultato atteso |
|---|---|
| Modulo «invita una persona» | c'è **solo** il campo dell'indirizzo e il bottone. **Nessun menu a tendina del ruolo**: si entra sempre come collaboratore, e i poteri si concedono dopo, una applicazione alla volta |
| Colonna «Role» della tabella | è un **testo**, non un menu a tendina: `Owner` sulla riga dell'owner, `Member` su tutte le altre. Prima da lì si cambiava il ruolo; ora non c'è nulla da scegliere |
| Riga dell'owner | il bottone **Remove** resta **disabilitato**, come prima — ma ora dietro c'è anche il rifiuto del servizio (punto 5) |
| Invita una persona nuova | l'invito parte e compare fra i pendenti; nella banca dati ha ruolo `member` |
| Sospendi e riattiva `member@acme.test` | funziona **come prima**: questa change non ha toccato lo stato |
| Cambia lingua (menu in alto) | le etichette del ruolo restano tradotte in tutte le lingue; nessuna scritta grezza tipo `members.roleMember` |

## 8. Quello che si vede: la persona che prima era «admin dell'account»

Esci ed entra come **`admin@acme.test`**.

| Cosa guardare | Risultato atteso |
|---|---|
| Menu laterale | **non** compaiono più **Account**, **Billing** e **Members**: erano riservate a owner e admin, e `admin` non è più un ruolo di conto. È il modello nuovo, **non** un difetto |
| Rotta `/members` scritta a mano nella barra dell'indirizzo | la pagina non è utilizzabile: la rotta è protetta o le sue letture rispondono «vietato» |
| Il resto | Dashboard, catalogo, supporto e «I miei dati» funzionano **come prima** |

> È la voce più importante della verifica visiva, e quella che sorprende: è il senso della storia. Il potere di quella
> persona non è sparito, si è **spostato** — sul Mini-CRM ha ruolo `admin` (punto 2). Che quel potere diventi
> *visibile* nell'interfaccia è lavoro delle storie successive dell'epica (UC 0107 e UC 0111).

## 9. Quello che non deve essere cambiato

| Azione | Risultato atteso |
|---|---|
| Accesso, uscita e ripresa della sessione con tutti gli utenti del seme | funzionano come prima |
| Selettore dell'account (se hai costruito una persona con due conti) | funziona come prima |
| Sezione **Billing** entrando come `owner@acme.test` | funziona come prima |
| Mini-CRM: elenco contatti, creazione, posti | funzionano come prima — il varco dei ruoli nei servizi delle applicazioni è di UC 0099, questa change non lo tocca |
| «I miei dati» → esporta i dati del conto | il file scaricato contiene una sezione **`app_access`** con lo *slug* dell'applicazione, il ruolo e l'identificativo della persona: dato nuovo nell'esportazione, dichiarato nel manifesto |
| Elimina un conto di prova (creane uno nuovo dalla registrazione) | l'eliminazione va a termine: le righe di accesso sono cancellate **prima** delle appartenenze, senza errori di chiave esterna |
