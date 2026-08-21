# Come verificare a mano la change 0092 — autorizzazione per applicazione

Guida di verifica **manuale** per UC 0099. Serve a vedere con i propri occhi le tre cose che questa change
introduce e che nessun collaudo automatico può mostrare a una persona:

1. il **token porta un ruolo in meno** (il claim non contiene più `admin` come ruolo di piattaforma);
2. esiste una lettura che dice **dove posso entrare e con che ruolo**;
3. il ruolo su una applicazione **viene rispettato** dal servizio, con un solo varco condiviso — e un cambio
   di ruolo si sente **entro pochi secondi, senza rientrare**.

Ogni voce è **azione → risultato atteso**. Le parti non visive (chiamate alle API, righe di banca dati,
contenuto del token) sono altrettanto obbligatorie: il cuore della storia è invisibile a schermo.

---

## 0. Preparazione

| # | Azione | Risultato atteso |
|---|---|---|
| 0.1 | `./app-start.sh` | Tutto verde: Postgres, proxy, Mailpit, MinIO, ElasticMQ, i servizi backend scoperti (`auth`, `core`, `crm`, `fatture`) e le due SPA. In coda, il controllo di salute end-to-end via HTTPS non segnala nulla. |
| 0.2 | `./dev.sh services` | La mappa scoperta elenca `crm` (porta 8082, schema `app_crm`) e `fatture`. Nessun cablaggio a mano: se `crm` non compare, non c'è motivo di andare avanti. |
| 0.3 | Apri `https://app.local.appgrove.app` ed entra come **owner@acme.test** / `Password1!` | Il cruscotto si apre. Nella barra laterale ci sono Account, Billing, Members. |

**Nota importante prima di iniziare**: il Mini-CRM è **spento di proposito** nel listino
(`services/core/src/main/resources/pricing/crm.yaml`, `status: inactive`, scelta della change 0042). Con
l'applicazione spenta l'account non ha alcun diritto su di essa e il **varco più esterno** (i diritti
d'accesso) risponde `402` prima che il varco del ruolo possa dire qualcosa. Va quindi accesa per la durata
della verifica:

| # | Azione | Risultato atteso |
|---|---|---|
| 0.4 | Leggi l'identificativo dell'applicazione:<br>`docker compose -f dev/docker-compose.yml exec -T postgres psql -U appgrove -d appgrove -c "select id, slug, status from platform.app where slug in ('crm','fatture');"` | Due righe. `crm` risulta `inactive`. Annota il suo `id` (serve al passo dopo e ai passi 2 e 4). |
| 0.5 | Entra nella console admin `https://admin.local.appgrove.app` come **admin@appgrove.test** / `Password1!` e accendi il Mini-CRM dalla pagina delle applicazioni. In alternativa, via API:<br>`PATCH /api/platform/v1/admin/apps/<app-id>` con corpo `{"status":"active"}` e token di **admin@appgrove.test** | L'applicazione passa a `active`. Rieseguendo il passo 0.4 lo stato è `active`. |
| 0.6 | Torna su `https://app.local.appgrove.app` come **owner@acme.test** e ricarica | Nella barra laterale compare il **Mini-CRM**. Se non compare, ricarica: il registry legge i diritti dell'account. |

> ⚠️ **L'accensione via API è temporanea: ogni riavvio del `core` la annulla.** `PricingSyncStartup`
> risemina il listino dal file `pricing/crm.yaml` a ogni avvio, e là `status` è `inactive` — è scritto nel
> file stesso. Quindi dopo il passo 6.2 (riavvio del `core`) e dopo il 6.3 (`./app-start.sh`) il Mini-CRM
> **torna spento**, e i passi successivi risponderebbero `402` senza spiegazione. **Riesegui il passo 0.5
> dopo ogni riavvio.** Nota collegata: per un difetto della copia locale dei diritti (tracciato in
> [docs/_BACKLOG.md](../../docs/_BACKLOG.md), proprietario UC 0046) riaccendere l'applicazione **non basta**
> — l'account che ha ricevuto un rifiuto mentre era spenta resta bloccato con `402` a tempo indeterminato.
> Per sbloccarlo:
>
> ```bash
> docker compose -f dev/docker-compose.yml exec -T postgres psql -U appgrove -d appgrove -c \
>   "update app_crm.entitlement_projection set stale = true where tenant_id = 'a0000000-0000-4000-8000-000000000001';"
> ```

> Alla fine della verifica puoi rimettere `crm` a `inactive` (stesso comando del passo 0.5 con
> `{"status":"inactive"}`): non è obbligatorio, ma riporta lo stack allo stato di partenza — e in ogni caso
> il primo riavvio del `core` lo farà per te.

**Come ottenere un token per le chiamate a mano** (serve dai passi 2 in poi):

```bash
curl -s https://api.local.appgrove.app/api/auth/login \
  -H 'content-type: application/json' \
  -d '{"email":"owner@acme.test","password":"Password1!"}'
```

La risposta contiene l'access token nel campo **`access_token`** (non `accessToken`). Nei comandi seguenti lo chiamo `$TOKEN_OWNER`,
`$TOKEN_MEMBER` (per `member@acme.test`) e `$TOKEN_ADMIN` (per `admin@acme.test`) — stessa chiamata,
cambiando indirizzo. La password è la stessa per tutte le persone del seme. Comodo da incollare:

```bash
TOKEN_OWNER=$(curl -sk https://api.local.appgrove.app/api/auth/login \
  -H 'content-type: application/json' \
  -d '{"email":"owner@acme.test","password":"Password1!"}' \
  | python3 -c 'import sys,json;print(json.load(sys.stdin)["access_token"])')
```

**`member@acme.test` può chiedere di scegliere l'account** (ha due appartenenze dopo la change 0090: Acme
Corp e un account proprio). In quel caso il login **non** risponde con `access_token` ma con
`account_selection_required` e un `choice_token`, e serve un secondo passo. Il comando qui sotto copre
**entrambi** i casi, perché la scelta viene chiesta o no a seconda che ci sia una preferenza già registrata
— e una guida non può indovinare quale dei due incontrerai:

```bash
RISPOSTA=$(curl -sk https://api.local.appgrove.app/api/auth/login \
  -H 'content-type: application/json' \
  -d '{"email":"member@acme.test","password":"Password1!"}')
TOKEN_MEMBER=$(printf '%s' "$RISPOSTA" | python3 -c 'import sys,json;print(json.load(sys.stdin).get("access_token",""))')
if [ -z "$TOKEN_MEMBER" ]; then
  CHOICE=$(printf '%s' "$RISPOSTA" | python3 -c 'import sys,json;print(json.load(sys.stdin)["choice_token"])')
  TOKEN_MEMBER=$(curl -sk https://api.local.appgrove.app/api/auth/login/account \
    -H 'content-type: application/json' \
    -d "{\"choice_token\":\"$CHOICE\",\"account_id\":\"a0000000-0000-4000-8000-000000000001\"}" \
    | python3 -c 'import sys,json;print(json.load(sys.stdin)["access_token"])')
fi
echo "${#TOKEN_MEMBER} caratteri"   # > 0 = pronto
```

---

## 1. Il token porta un ruolo in meno (verifica NON visiva)

Il seme ha una persona che si chiama ancora «Acme Admin» ma che, dalla change 0091, è `member` di
piattaforma con ruolo `admin` **sul Mini-CRM**. È il caso interessante.

| # | Azione | Risultato atteso |
|---|---|---|
| 1.1 | Ottieni `$TOKEN_ADMIN` (login di `admin@acme.test`) e decodifica la parte centrale del token:<br>`echo "$TOKEN_ADMIN" \| cut -d. -f2 \| base64 -d 2>/dev/null \| python3 -m json.tool` | Il claim `roles` contiene **`["member"]`**. **Non** contiene `admin`. Il claim `tenant_id` è l'account Acme. |
| 1.2 | Stessa cosa con `$TOKEN_OWNER` | `roles` = `["owner"]`. |
| 1.3 | Login di `admin@appgrove.test` e stessa decodifica | `roles` contiene `owner` **e** `platform-admin`: chi amministra la piattaforma resta fuori da questo meccanismo. |
| 1.4 | Guarda l'elenco dei claim | **Non** c'è alcun claim con l'elenco delle applicazioni né con i ruoli su di esse. È la decisione centrale della storia: se un giorno comparissero, un cambio di ruolo avrebbe effetto solo al rinnovo del token. |
| 1.5 | *(prova della tolleranza, opzionale)* Forza a mano il valore ritirato:<br>`docker compose -f dev/docker-compose.yml exec -T postgres psql -U appgrove -d appgrove -c "update platform.membership set role='admin' where identity_id='b0000000-0000-4000-8000-000000000002';"`<br>poi rifai il login di `admin@acme.test` e decodifica | `roles` = `["member"]` **comunque**: il valore vecchio nei dati viene letto come `member` mentre si compone il claim. La persona entra col potere *minore*, non con *nessun* potere. **Rimetti il valore giusto** subito dopo (`set role='member'` sulla stessa riga) oppure riesegui `./dev.sh seed`. |

---

## 2. «Dove posso entrare, e con che ruolo» (verifica NON visiva)

| # | Azione | Risultato atteso |
|---|---|---|
| 2.1 | `curl -s https://app.local.appgrove.app/api/platform/v1/me/app-access -H "authorization: Bearer $TOKEN_OWNER" \| python3 -m json.tool` | Un elenco che contiene il Mini-CRM con **`"role": "admin"`** — l'owner ha il ruolo massimo su tutte le applicazioni a cui l'account ha diritto, **senza righe di accesso**. Ogni voce porta `appId`, `appSlug`, `appName` e `role`. |
| 2.2 | Stessa chiamata con `$TOKEN_MEMBER` | Compare **solo** il Mini-CRM, con **`"role": "editor"`** (è la riga del seme). Nessuna altra applicazione. |
| 2.3 | Stessa chiamata con `$TOKEN_ADMIN` | Solo il Mini-CRM, con **`"role": "admin"`**. |
| 2.4 | Spegni il Mini-CRM (passo 0.5 con `inactive`) e ripeti 2.2 | L'elenco è **vuoto**: l'accesso della persona resta scritto in tabella ma senza il diritto dell'account non apre nulla. **Riaccendi** l'applicazione prima di proseguire. |
| 2.5 | Login di **bob@bob.test** (`Password1!`) e stessa chiamata col suo token | Non compare nulla dell'account Acme. La lettura dice dove può entrare *chi chiama*, dentro l'account del suo token: nessun parametro identifica la persona o l'account. |

---

## 3. Il ruolo viene rispettato — percorsi VISIVI nel Mini-CRM

Per usare il Mini-CRM servono **due** cose, oggi: un **posto** (il meccanismo che quell'applicazione si era
costruita da sé, UC 0054) e un **ruolo** sull'applicazione (il varco nuovo). La convivenza dei due varchi è
voluta e temporanea: il posto verrà ritirato da UC 0111.

| # | Azione | Risultato atteso |
|---|---|---|
| 3.1 | Come **owner@acme.test**, apri il Mini-CRM dalla barra laterale e vai a «Membri» del modulo. Nel campo «Identificativo utente» scrivi **`seed-acme-owner`**, assegna il posto, poi ripeti con **`seed-acme-member`**. **Non è l'indirizzo email**: leggi la nota qui sotto prima di provare. | I posti risultano assegnati; il contatore dei posti occupati cresce (il tetto del piano gratuito è 2). |
| 3.2 | Sempre come owner, crea un contatto | Il contatto viene creato: l'owner ha il ruolo massimo per costruzione. |
| 3.3 | Esci ed entra come **member@acme.test**, apri il Mini-CRM | Vedi l'elenco dei contatti (ruolo `editor` dal seme: legge e scrive). |
| 3.4 | Crea un contatto come `member@acme.test` | Il contatto viene creato. |
| 3.5 | Esci ed entra come **admin@acme.test** | Nella barra laterale **non** ci sono Account, Billing e Members, e il Mini-CRM **c'è**. **Questo è il modello nuovo, non una regressione**: quella persona è `member` di piattaforma con ruolo `admin` sul Mini-CRM (change 0091). Se le assegni un posto, legge e scrive i contatti normalmente. |
| 3.6 | Entra come una persona **senza** posto e **senza** accesso — per esempio invita un indirizzo nuovo e completa la registrazione dalla posta locale (`http://localhost:8025`) — e prova ad aprire il Mini-CRM | Rifiuto. Il messaggio dice che serve l'abilitazione del titolare dell'account o di un amministratore dell'applicazione: **non** «ruolo insufficiente». Sono due frasi diverse per due situazioni diverse. |

> ⚠️ **Il posto si assegna al `subject` del token, non all'indirizzo email.** Il campo «Identificativo
> utente» della schermata «Membri» del Mini-CRM vuole l'identificativo con cui la persona compare nel
> token — nel seme locale `seed-acme-owner`, `seed-acme-member`, `seed-acme-admin` — mentre il server
> confronta quel valore col `sub` del token di chi chiede. Se scrivi `owner@acme.test` il posto viene
> creato **e non serve a nulla**: ogni operazione sul Mini-CRM risponde `403` «nessun posto assegnato».
> È un difetto d'usabilità reale del meccanismo dei posti (UC 0054), tracciato là e destinato a sparire
> col ritiro dei posti (UC 0111): qui va solo evitato. Per verificare gli identificativi:
>
> ```bash
> curl -sk https://api.local.appgrove.app/api/auth/login -H 'content-type: application/json' \
>   -d '{"email":"owner@acme.test","password":"Password1!"}' \
>   | python3 -c 'import sys,json,base64;t=json.load(sys.stdin)["access_token"].split(".")[1];print(json.loads(base64.urlsafe_b64decode(t+"="*(-len(t)%4)))["sub"])'
> ```
>
> Se hai già assegnato i posti con gli indirizzi email, liberali prima di riprovare (il tetto del piano
> gratuito è 2 e li avresti già occupati entrambi):
>
> ```bash
> TOKEN_OWNER=$(curl -sk https://api.local.appgrove.app/api/auth/login -H 'content-type: application/json' \
>   -d '{"email":"owner@acme.test","password":"Password1!"}' | python3 -c 'import sys,json;print(json.load(sys.stdin)["access_token"])')
> curl -sk -X DELETE "https://app.local.appgrove.app/api/crm/v1/seats/owner@acme.test"  -H "authorization: Bearer $TOKEN_OWNER" -w ' %{http_code}\n'
> curl -sk -X DELETE "https://app.local.appgrove.app/api/crm/v1/seats/member@acme.test" -H "authorization: Bearer $TOKEN_OWNER" -w ' %{http_code}\n'
> ```

### 3bis. Forzare gli stati cambiando gli accessi (il cuore della storia)

Qui si vede che **un cambio di ruolo si sente senza rientrare**. Serve l'identificativo dell'applicazione
(passo 0.4) e quello dell'**identità** di `member@acme.test`, che è `b0000000-0000-4000-8000-000000000003`.

| # | Azione | Risultato atteso |
|---|---|---|
| 3.7 | Tieni aperta la sessione di **member@acme.test** sul Mini-CRM, **senza uscire**. Da un terminale, come owner, retrocedi il suo ruolo a `viewer`:<br>`curl -s -X PUT https://app.local.appgrove.app/api/platform/v1/apps/<app-id>/access/b0000000-0000-4000-8000-000000000003 -H "authorization: Bearer $TOKEN_OWNER" -H 'content-type: application/json' -d '{"role":"viewer"}'` | Risposta `200` con `"role": "viewer"`. |
| 3.8 | Nella sessione già aperta (niente logout, niente ricarica del token: al più ricarica la pagina) prova a **creare** un contatto, entro pochi secondi | Rifiuto **403** con un messaggio che **nomina il ruolo che serve**: «serve almeno `editor`, il tuo ruolo è `viewer`». La **lettura** dell'elenco continua a funzionare. È la prova che il ruolo nuovo vale senza rientrare. |
| 3.9 | Come owner, **revoca** l'accesso:<br>`curl -s -X DELETE https://app.local.appgrove.app/api/platform/v1/apps/<app-id>/access/b0000000-0000-4000-8000-000000000003 -H "authorization: Bearer $TOKEN_OWNER"` | Risposta `204`. |
| 3.10 | Nella sessione di `member@acme.test`, ricarica l'elenco dei contatti | Ora anche la **lettura** è rifiutata, e con l'**altro** messaggio: «non hai accesso a questa applicazione, chiedi l'abilitazione». Il posto ce l'ha ancora: è il ruolo che manca. |
| 3.11 | Come owner, riconcedi l'accesso come `editor`:<br>`curl -s -X POST https://app.local.appgrove.app/api/platform/v1/apps/<app-id>/access -H "authorization: Bearer $TOKEN_OWNER" -H 'content-type: application/json' -d '{"identityId":"b0000000-0000-4000-8000-000000000003","role":"editor"}'` | Risposta `201`, e in pochi secondi la persona torna a leggere e scrivere nella sessione già aperta. |

---

## 4. Le API con e senza accesso (verifica NON visiva)

Gli stessi tre esiti, guardando i corpi degli errori: l'interfaccia deve poterli distinguere **senza
leggere una frase in italiano**, perché parla cinque lingue.

| # | Azione | Risultato atteso |
|---|---|---|
| 4.1 | Con un token di persona **con ruolo `viewer`** (passo 3.7):<br>`curl -si https://app.local.appgrove.app/api/crm/v1/contacts -H "authorization: Bearer $TOKEN_MEMBER"` | `200`. |
| 4.2 | Stesso token, in scrittura:<br>`curl -si -X POST https://app.local.appgrove.app/api/crm/v1/contacts -H "authorization: Bearer $TOKEN_MEMBER" -H 'content-type: application/json' -d '{"displayName":"Prova"}'` | `403`, `content-type: application/problem+json`, e nel corpo: `"type": "urn:appgrove:app-role:insufficient"`, `"requiredRole": "editor"`, `"role": "viewer"`. |
| 4.3 | Con l'accesso **revocato** (passo 3.9), rifai 4.1 | `403` con `"type": "urn:appgrove:app-role:no-access"`. Identificativo **diverso** dal precedente: è la distinzione fra «non entri» e «non puoi fare *questo*». |
| 4.4 | Con l'applicazione **spenta** (passo 0.5 con `inactive`), rifai 4.1 | `402` «abbonamento richiesto», **non** `403`: il varco dei diritti d'accesso viene prima di quello del ruolo, perché l'ordine delle risposte è l'ordine in cui una persona può rimediare. Riaccendi l'applicazione. |
| 4.5 | Come owner, prova a concedere l'accesso a **te stesso** (owner):<br>`POST .../access` con il tuo `identityId` (`b0000000-0000-4000-8000-000000000001`) | `409` con `"type": "urn:appgrove:app-access:owner-implicit"`: l'owner ha già accesso a tutto, non è un ruolo di applicazione. |

---

## 5. La copia locale del ruolo: righe da guardare (verifica NON visiva)

È la parte che nessuno vede e che spiega perché tutto il resto è veloce. La copia vive nello schema del
servizio, **non** in quello di piattaforma.

```bash
docker compose -f dev/docker-compose.yml exec -T postgres \
  psql -U appgrove -d appgrove -c \
  "select tenant_id, subject, app_slug, role, stale, refreshed_at, invalidated_at
     from app_crm.app_role_projection order by refreshed_at desc;"
```

| # | Azione | Risultato atteso |
|---|---|---|
| 5.1 | Svuota la tabella (`docker compose -f dev/docker-compose.yml exec -T postgres psql -U appgrove -d appgrove -c "delete from app_crm.app_role_projection;"`), poi apri l'elenco dei contatti come `member@acme.test` e riesegui la query | Compare **una** riga: `subject` = identificativo di autenticazione della persona (`seed-acme-member`), `app_slug` = `crm`, `role` = `editor`, `stale` = `false`, `refreshed_at` = adesso. **Nessuna email, nessun nome**: la copia non contiene dati personali oltre all'identificativo. |
| 5.2 | Ricarica l'elenco più volte di seguito e riesegui la query | `refreshed_at` **non** cambia: con la copia fresca il servizio dell'applicazione **non** interpella il core. È il senso del disaccoppiamento. |
| 5.3 | Cambia il ruolo (passo 3.7) e **subito** riesegui la query, prima di fare altre richieste | La riga ha `stale = true` e `invalidated_at` valorizzato: l'evento di invalidazione è arrivato dalla coda ed è stato consumato. Il ruolo scritto è ancora quello *vecchio* — la copia non viene cancellata, viene **marcata**. |
| 5.4 | Ora fai una richiesta al Mini-CRM e riesegui la query | `stale = false`, `role` = `viewer`, `refreshed_at` = adesso. Il rinfresco è **pigro**: avviene alla prima richiesta utile, non all'arrivo dell'evento. |
| 5.5 | Revoca l'accesso (passo 3.9), fai una richiesta e riesegui la query | La riga esiste con `role` **vuoto** (`NULL`): è un **diniego noto**, non l'assenza di informazione. Serve a non richiedere al core a ogni richiesta di chi non ha accesso. |
| 5.6 | Attendi più di **60 secondi** senza fare nulla, poi fai una richiesta e guarda `refreshed_at` | È stato aggiornato: oltre la durata massima la copia si rinfresca **anche senza evento**. È la rete che tiene se il canale degli eventi è rotto — l'unico caso in cui l'invalidazione, da sola, non proteggerebbe nulla. |
| 5.7 | Guarda che l'evento passa dalla coda già esistente:<br>`curl -s "http://localhost:9324/?Action=GetQueueAttributes&QueueUrl=http://localhost:9324/queue/entitlement-crm&AttributeName.1=All"` | La coda `entitlement-crm` esiste e viene usata: **una sola** coda per servizio, con il tipo di evento nel messaggio (`app_access.role_changed`, `app_access.revoked`, …). Non è stata creata una seconda coda per la stessa notizia. |
| 5.8 | Guarda i log del servizio: `tail -n 50 dev/.run/crm.log \| grep app_role` | Le righe portano account, persona e applicazione. Dopo un'invalidazione, `entitlement.invalidation … righe_marcate=[entitlement_projection=N app_role_projection=M]`: **un** evento marca **entrambe** le copie locali del servizio. |

---

## 6. Fallimento chiuso: si nega, ma dicendolo bene (verifica NON visiva)

L'ultima verifica, e la più facile da sbagliare in un prodotto: quando il guasto è nostro, non si accusa
l'utente di non avere permessi.

| # | Azione | Risultato atteso |
|---|---|---|
| 6.1 | Ferma il solo servizio `core` (per esempio `kill` del processo elencato da `./dev.sh services`, oppure `./app-stop.sh` e riavvio del solo `crm`), **svuota** la copia (`docker compose -f dev/docker-compose.yml exec -T postgres psql -U appgrove -d appgrove -c "delete from app_crm.app_role_projection;"`) e chiama l'elenco dei contatti | **`503`** con `"type": "urn:appgrove:app-role:unavailable"` e un messaggio che parla di un problema momentaneo e dice esplicitamente che **non** riguarda i permessi. Non è un `403`: la richiesta non è vietata, non si è potuta decidere. |
| 6.2 | Con il core ancora fermo, ripopola la copia? Non si può — quindi fai il contrario: riavvia il core, fai una richiesta (la copia si popola), **ferma di nuovo** il core, marca la riga come da rinfrescare (`docker compose -f dev/docker-compose.yml exec -T postgres psql -U appgrove -d appgrove -c "update app_crm.app_role_projection set stale = true;"`) e richiama l'elenco | Funziona: `200`. Con una copia vecchia e il core giù si usa l'**ultima verità nota**, perché un guasto del core non deve bloccare tutte le persone di tutti gli account. Nel log del `crm` compare un avviso `app_role.projection servita copia vecchia`. Il rischio accettato — una revoca decisa *durante* il guasto arriva in ritardo — dura quanto il guasto. |
| 6.3 | Riavvia tutto (`./app-start.sh`) | Tutto verde. |

---

## 7. Cancellazione dei dati: la copia sparisce con l'account (verifica NON visiva)

| # | Azione | Risultato atteso |
|---|---|---|
| 7.1 | Verifica che esista almeno una riga in `app_crm.app_role_projection` per l'account Acme | Almeno una. |
| 7.2 | Esercita la cancellazione dell'account dalla schermata «I miei dati» di un account **di prova** (non Acme, se vuoi conservarlo) e attendi il completamento della purga | Le righe della copia di quell'account **spariscono fisicamente** dalla tabella. |
| 7.3 | Guarda la traccia di controllo della purga:<br>`docker compose -f dev/docker-compose.yml exec -T postgres psql -U appgrove -d appgrove -c "select * from app_crm.gdpr_purge_audit order by executed_at desc limit 1;"` | Fra le entità cancellate compare **`app_role_projection`** con il suo conteggio, accanto a `entitlement_projection`. Una prova di cancellazione incompleta non è una prova. |

---

## Cosa NON deve accadere (segnali di guasto)

- Una persona **senza** accesso che riesce a leggere o scrivere i dati di una applicazione.
- Un rifiuto `403` che parla di permessi quando il core è irraggiungibile (deve essere `503`).
- Un cambio di ruolo che si sente **solo** dopo un nuovo accesso: l'invalidazione non sta funzionando.
- Il claim `roles` con `admin`, oppure con l'elenco delle applicazioni.
- Una riga della copia locale con un'email o un nome dentro.
- Un'applicazione che risponde `403` «ruolo insufficiente» a un account che non ha l'abbonamento: l'ordine
  dei varchi è sbagliato (deve rispondere `402`).
