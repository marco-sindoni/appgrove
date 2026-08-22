# Come verificare a mano la change 0095 — semantica dei tre ruoli

> Guida scritta sul branch `change/0095-use-case-0101-semantica-ruoli`, base `74e8d7a`, il 2026-08-22.
> È una **fotografia**: se una change successiva cambia questi comportamenti, i punti superati si scoprono
> **rieseguendola**, non rileggendola.
>
> **Passi non visivi già eseguiti** dall'agente il 2026-08-22 sullo stack locale — §1 (tutti, comprese le
> tre manomissioni che devono far diventare rosso il collaudo), §2, §3, §5, §6.1, §6.2, §7 — con gli esiti
> attesi riportati qui **come si leggono davvero**. Eseguirli ha prodotto **quattro correzioni alla guida**
> (messaggi attesi imprecisi ai passi 1.3, 1.5 e 5.3; un `grep` che tagliava la frase al passo 5.2) e ha
> scoperto **un difetto di prodotto**, corretto in questa change (§0 bis).
> Restano allo sviluppatore i passi **visivi** di §4.
>
> **Passata di fine lotto (change 0095–0099), 2026-08-22.** Tutti i passi non visivi sono stati
> **rieseguiti** contro lo stato finale di `main` (dopo le change 0096, 0097, 0098 e 0099): §1 comprese le
> tre manomissioni, §2, §3, §5, §6.1, §6.2, §7 — **tutti verdi, nessun punto superato e nessun difetto di
> prodotto**. Le storie dei posti (0098, 0099) non hanno intaccato nulla di questa guida: in particolare il
> §2.1 continua a elencare **soltanto** applicazioni vere (`fatture`, `notes`, `teams`), perché la voce di
> piattaforma dei posti è esclusa da `me/app-access` per costruzione. L'unica correzione è alle **etichette
> visive** del §4, che erano scritte in italiano mentre l'interfaccia parte in inglese (correzione di prosa:
> non c'è nulla da rieseguire).

Guida di verifica **manuale** per UC 0101. Serve a vedere con i propri occhi le quattro cose che questa
change introduce e che nessun collaudo automatico può mostrare a una persona:

1. la **regola di classificazione** esiste, è scritta, ed è **verificata da un collaudo** che si può vedere
   diventare rosso;
2. l'app #1 `fatture` — che prima non chiedeva alcun ruolo — ora **rispetta i tre ruoli** come il Mini-CRM;
3. i due rifiuti **si leggono a parole sullo schermo**: «non hai accesso» e «serve almeno editor» sono cose
   diverse e si distinguono senza interpretare un codice;
4. le operazioni **esenti** (lo stato di quota) restano raggiungibili anche da chi non ha alcun ruolo.

Ogni voce è **azione → risultato atteso**.

---

## 0. Preparazione

| # | Azione | Risultato atteso |
|---|---|---|
| 0.1 | `./app-start.sh` | Tutto verde: Postgres, proxy, Mailpit, MinIO, ElasticMQ, i servizi backend scoperti (`auth`, `core`, `crm`, `fatture`) e le due SPA. |
| 0.2 | `./dev.sh services` | La mappa scoperta elenca `fatture` (porta 8081, schema `app_fatture`) e `crm` (8082, `app_crm`). |

**A differenza della guida della change 0092, non serve accendere il Mini-CRM**: questa verifica gira
sull'app #1 `fatture`, che nel listino è **già attiva** (fascia gratuita). È proprio il punto della change:
la semantica dei ruoli è finalmente esercitabile su un'applicazione viva.

**I due token che servono.** Comandi completi e incollabili:

```bash
TOKEN_OWNER=$(curl -sk https://api.local.appgrove.app/api/auth/login \
  -H 'content-type: application/json' \
  -d '{"email":"owner@acme.test","password":"Password1!"}' \
  | python3 -c 'import sys,json;print(json.load(sys.stdin)["access_token"])')
echo "${#TOKEN_OWNER} caratteri"   # > 0 = pronto
```

`member@acme.test` **può** dover scegliere l'account (ha due appartenenze dalla change 0090). Il comando
qui sotto copre entrambi i casi, perché la scelta viene chiesta o no a seconda che ci sia una preferenza
già registrata — e una guida non può indovinare quale dei due incontrerai:

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

**L'identificativo dell'applicazione `fatture`** (serve da §2 in poi):

```bash
FATTURE_ID=$(curl -sk https://app.local.appgrove.app/api/platform/v1/me/app-access \
  -H "authorization: Bearer $TOKEN_OWNER" \
  | python3 -c 'import sys,json;print([a["appId"] for a in json.load(sys.stdin) if a["appSlug"]=="fatture"][0])')
echo "$FATTURE_ID"
```

**L'identificativo dell'identità di `member@acme.test`** (nel seme locale è fisso):

```bash
MEMBER_ID=b0000000-0000-4000-8000-000000000003
docker compose -f dev/docker-compose.yml exec -T postgres psql -U appgrove -d appgrove -c \
  "select id, email from platform.identity where email = 'member@acme.test';"
```

> ⚠️ **Attenzione, non è il `sub` del token.** La concessione dell'accesso per applicazione
> (`POST /apps/{appId}/access`) vuole l'identificativo dell'**identità** — quello della tabella
> `platform.identity`, che vedi col comando qui sopra — non l'indirizzo email e non il `sub` del token.
> È diverso dal campo «Identificativo utente» della schermata dei posti del Mini-CRM, che invece vuole il
> `sub`: la disomogeneità è un difetto d'usabilità del meccanismo dei posti, tracciato in UC 0111.

### 0 bis. Un difetto trovato eseguendo questa guida, e corretto in questa change

Alla prima esecuzione, **tutte** le chiamate a `fatture` con `$TOKEN_MEMBER` rispondevano `403` — anche
lo stato di quota, che è dichiarato esente. Non era il varco del ruolo: era la lista dei ruoli di
**piattaforma** di `@RolesAllowed`, che su `fatture` elencava soltanto `owner` e `admin`. Quarkus la applica
**prima** dei filtri (sicurezza «eager», a livello di rotta), quindi ogni collaboratore riceveva un `403`
**senza corpo** e il varco del ruolo non arrivava mai a decidere: la classificazione di questa storia
sarebbe stata una dichiarazione senza effetto per tutti tranne l'owner.

Corretto qui: la lista comprende ora **tutti** i ruoli di piattaforma, quindi dice soltanto «appartieni a
un account» e la decisione passa al ruolo sull'applicazione. Presidiato da
`AppRoleGateTest.aPlatformMemberReachesTheApplicationRoleGateAndGetsARefusalThatSpeaks`, verificato rosso
prima della correzione. La **rimozione** di quelle annotazioni, che a questo punto non aggiungono nulla,
appartiene a UC 0111/0114 ed è tracciata là.

---

## 1. Il collaudo è parte del contratto (verifica NON visiva)

È il punto 3 del Definition of Done della storia: non basta che il documento esista, deve esserci una prova
che **coglie** l'operazione di scrittura non protetta. Qui la si fa fallire di proposito.

| # | Azione | Risultato atteso |
|---|---|---|
| 1.1 | `cd services && mvn -B -pl fatture test -Dtest='AppOperationsContractTest'` | Verde, 2 collaudi. Il documento delle operazioni e il codice dicono la stessa cosa. |
| 1.2 | `cd services && mvn -B -pl commons test -Dtest='AppOperationsContractVerifierTest'` | Verde, 9 collaudi: uno sul caso corretto e **otto** che dimostrano che il verificatore fallisce davvero (scrittura non protetta, scrittura con solo `viewer`, operazione non dichiarata, esenzione protetta, metodo rinominato, due verità sullo stesso potere, identificativo duplicato, metodo senza verbo HTTP). |
| 1.3 | Togli a mano il varco da una scrittura: nel file `services/fatture/src/main/java/app/appgrove/fatture/InvoiceResource.java` cancella la riga `@RequiresAppRole(AppRole.editor)` che sta sopra `@POST` → `public Response create(`. Poi `cd services && mvn -B -pl fatture test -Dtest='AppOperationsContractTest'` | **ROSSO**, con questo messaggio esatto:<br>`invoices.create: il documento dichiara editor, il varco applica viewer. Due verità sullo stesso potere`<br>Il varco non sparisce del tutto perché la classe dichiara `viewer` per le letture: la scrittura **retrocede** a `viewer`, e il verificatore coglie la divergenza fra documento e codice. È la prova che il contratto non è prosa. *(Se togliessi anche l'annotazione di classe, il messaggio diventerebbe «il metodo non è protetto»: quel caso è coperto dall'auto-collaudo del passo 1.2.)* |
| 1.4 | `git checkout -- services/fatture/src/main/java/app/appgrove/fatture/InvoiceResource.java` e ripeti 1.1 | Verde. **Non dimenticare questo passo**: senza di esso i passi successivi girano su codice manomesso. |
| 1.5 | Aggiungi una rotta finta al Mini-CRM: in `services/crm/src/main/java/app/appgrove/crm/ContactResource.java`, **prima della graffa che chiude la classe**, incolla:<br>`    @POST`<br>`    @Path("/prova")`<br>`    public String prova() {`<br>`        return "";`<br>`    }`<br>poi lancia `cd services && mvn -B -pl crm test -Dtest='AppOperationsContractTest'` | **ROSSO**, con questo messaggio esatto:<br>`operazione ESPOSTA e non dichiarata: ContactResource#prova (POST). Ogni operazione va nel documento delle operazioni, letture comprese, col suo ruolo minimo o col motivo dell'esenzione`<br>È la direzione che conta: coglie l'operazione che qualcuno aggiunge domani dimenticando il varco. Rimuovi la rotta finta e ripeti: verde. |

---

## 2. `fatture` rispetta i tre ruoli (verifica NON visiva)

| # | Azione | Risultato atteso |
|---|---|---|
| 2.1 | `curl -sk https://app.local.appgrove.app/api/platform/v1/me/app-access -H "authorization: Bearer $TOKEN_OWNER" \| python3 -m json.tool` | Un elenco con `fatture` e **`"role": "admin"`**: l'owner ha il ruolo massimo su tutte le applicazioni a cui l'account ha diritto, senza righe di accesso. |
| 2.2 | `curl -sk -o /dev/null -w "%{http_code}\n" -X POST https://app.local.appgrove.app/api/fatture/v1/invoices -H "authorization: Bearer $TOKEN_OWNER" -H 'content-type: application/json' -d '{"customerName":"Collaudo 0095"}'` | **201.** L'owner scrive: nulla è cambiato per lui, ed è la prova che il varco nuovo non ha rotto l'esistente. |
| 2.3 | Concedi a `member@acme.test` il ruolo **viewer** su `fatture`:<br>`curl -sk -X POST "https://app.local.appgrove.app/api/platform/v1/apps/$FATTURE_ID/access" -H "authorization: Bearer $TOKEN_OWNER" -H 'content-type: application/json' -d "{\"identityId\":\"$MEMBER_ID\",\"role\":\"viewer\"}"` | **201** (oppure **200** se un accesso esisteva già) con un corpo che riporta `"role":"viewer"` e l'indirizzo `member@acme.test`. |
| 2.4 | `curl -sk https://app.local.appgrove.app/api/platform/v1/me/app-access -H "authorization: Bearer $TOKEN_MEMBER" \| python3 -m json.tool` | Compare `fatture` con **`"role": "viewer"`**. |
| 2.5 | Il `viewer` **legge**:<br>`curl -sk -o /dev/null -w "%{http_code}\n" "https://app.local.appgrove.app/api/fatture/v1/invoices?page=0&size=5" -H "authorization: Bearer $TOKEN_MEMBER"` | **200.** È la metà del contratto che si dimentica sempre di verificare: un `viewer` che non legge non è un `viewer`. |
| 2.6 | Il `viewer` **non scrive**:<br>`curl -sk -X POST https://app.local.appgrove.app/api/fatture/v1/invoices -H "authorization: Bearer $TOKEN_MEMBER" -H 'content-type: application/json' -d '{"customerName":"Tentativo viewer"}' \| python3 -m json.tool` | **403** con `"type": "urn:appgrove:app-role:insufficient"`, `"requiredRole": "editor"`, `"role": "viewer"` e la frase «Per questa operazione su 'fatture' serve almeno il ruolo 'editor': il tuo ruolo è 'viewer'.» Il rifiuto **nomina** ciò che serve. |
| 2.7 | La copia locale del ruolo esiste:<br>`docker compose -f dev/docker-compose.yml exec -T postgres psql -U appgrove -d appgrove -c "select subject, app_slug, role, stale from app_fatture.app_role_projection where subject = 'seed-acme-member';"` | Una riga: `seed-acme-member \| fatture \| viewer \| f`. Il ruolo **non è nel token**: si legge dal modello e si copia nel servizio (UC 0099). |

---

## 3. Il cambio di ruolo si sente senza rientrare (verifica NON visiva)

| # | Azione | Risultato atteso |
|---|---|---|
| 3.1 | Promuovi a `editor` (attenzione: il verbo è **PUT**, non PATCH):<br>`curl -sk -o /dev/null -w "%{http_code}\n" -X PUT "https://app.local.appgrove.app/api/platform/v1/apps/$FATTURE_ID/access/$MEMBER_ID" -H "authorization: Bearer $TOKEN_OWNER" -H 'content-type: application/json' -d '{"role":"editor"}'` | **200.** |
| 3.2 | **Aspetta cinque secondi**, poi riprova la scrittura del passo 2.6 con lo **stesso** token di prima | **201.** Il ruolo nuovo vale **senza** che la persona rientri. L'attesa non è cortesia: è l'evento di invalidazione che viaggia sulla coda e marca la copia locale — misurata a ~5 secondi in locale. Se provi subito, la copia è ancora fresca e vedi ancora `403`: è la finestra di pochi secondi che UC 0099 dichiara di accettare, non un difetto. |
| 3.3 | Revoca l'accesso:<br>`curl -sk -o /dev/null -w "%{http_code}\n" -X DELETE "https://app.local.appgrove.app/api/platform/v1/apps/$FATTURE_ID/access/$MEMBER_ID" -H "authorization: Bearer $TOKEN_OWNER"` | **204.** Ripetendo lo stesso comando una seconda volta si ottiene **404** con `"detail": "Accesso non trovato"`, e **non è un difetto**: la revoca non è idempotente per scelta (`AppAccessResource#revoke`), come l'accesso dell'owner dà `409` invece di un `204` silenzioso — revocare un accesso che non c'è è una richiesta senza oggetto, e rispondere `204` farebbe credere di aver revocato qualcosa. |
| 3.4 | Aspetta cinque secondi, poi:<br>`curl -sk "https://app.local.appgrove.app/api/fatture/v1/invoices?page=0&size=5" -H "authorization: Bearer $TOKEN_MEMBER" \| python3 -m json.tool` | **403** con `"type": "urn:appgrove:app-role:no-access"` e la frase «Non hai accesso all'applicazione 'fatture': chiedi al titolare dell'account o a un amministratore dell'applicazione di abilitarti.» **Un rifiuto diverso, con parole diverse**: è il cuore del §3 di questa verifica. Le virgolette intorno all'URL sono obbligatorie — senza, la shell interpreta il `?`. |

---

## 4. I due rifiuti si leggono a parole — percorso VISIVO (per lo sviluppatore)

Il prerequisito è il **passo 4.0** qui sotto, non una premessa da leggere: se arrivi qui in fila dopo il §3, che
**finisce con la revoca**, `member@acme.test` non ha alcun accesso a `fatture` — il 4.1 mostrerebbe il riquadro
del lucchetto invece dell'elenco, e il 4.3 risponderebbe `404` invece di `204`.

> **L'interfaccia parte in inglese**, non in italiano: le persone del seme hanno `locale = 'en'`
> (`dev/seed/seed.sql`), e la lingua della sessione è la loro. Le etichette qui sotto sono quindi quelle che
> si leggono **davvero** a schermo (`frontend/apps/backoffice/src/modules/fatture/i18n/en.ts`). Le **frasi
> dei due rifiuti**, invece, restano in italiano in qualunque lingua: le scrive il server e la schermata le
> mostra tali e quali. *(Etichette corrette nella passata di fine lotto: la prima stesura le dava in
> italiano — lo stesso errore che la guida della change 0096 aveva già corretto per sé.)*

| # | Azione | Risultato atteso |
|---|---|---|
| 4.0 | **Riporta l'accesso a `viewer`** e aspetta cinque secondi (è il passo 2.3, ripetuto qui per intero perché serve *adesso*):<br>`curl -sk -w "\nHTTP %{http_code}\n" -X POST "https://app.local.appgrove.app/api/platform/v1/apps/$FATTURE_ID/access" -H "authorization: Bearer $TOKEN_OWNER" -H 'content-type: application/json' -d "{\"identityId\":\"$MEMBER_ID\",\"role\":\"viewer\"}"` | **201** con `"role":"viewer"` (**200** se un accesso era ancora vivo). Deve essere un **POST**: il `PUT` del passo 3.1 su un accesso revocato risponde `404`, perché cambia il ruolo di una riga che c'è e qui la riga va **ricreata**. |
| 4.1 | Apri `https://app.local.appgrove.app`, entra come **member@acme.test** / `Password1!`, e apri **Invoices** dalla barra laterale | La pagina **Invoices** si apre e l'elenco si vede: un `viewer` legge tutto ciò che l'applicazione mostra. Si vede anche il banner del consumo («Invoices this month»). |
| 4.2 | Premi **New invoice**, compila **Customer name** e premi **Create invoice** | Compare un avviso rosso con la frase del server: «Per questa operazione su 'fatture' serve almeno il ruolo 'editor': il tuo ruolo è 'viewer'.» **Non** «Something went wrong. Please try again.» — quella frase generica era il difetto corretto da questa change. |
| 4.3 | **Revoca l'accesso** e aspetta cinque secondi, poi ricarica la pagina **Invoices** (è il passo 3.3, per intero):<br>`curl -sk -o /dev/null -w "%{http_code}\n" -X DELETE "https://app.local.appgrove.app/api/platform/v1/apps/$FATTURE_ID/access/$MEMBER_ID" -H "authorization: Bearer $TOKEN_OWNER"` → **204**. Un **404** qui vuol dire che l'accesso era già revocato: rifà il passo 4.0 e ripeti. | Al posto dell'elenco c'è un riquadro con il lucchetto e la frase «Non hai accesso all'applicazione 'fatture': chiedi al titolare dell'account o a un amministratore dell'applicazione di abilitarti.» **Non c'è alcun pulsante «Retry»/«Riprova»**: un rifiuto non è un guasto, e invitare a ripetere una richiesta che fallirà sempre manda una persona a sbattere contro lo stesso muro. |
| 4.4 | Confronta 4.2 e 4.3 | Due schermate **diverse** con due frasi **diverse**. È l'esito che la storia chiede: chi non entra sa a chi chiedere; chi è entrato ma non può fare *quella* cosa sa quale ruolo gli serve. |

> **Ciò che NON si vede ancora, e non è una dimenticanza**: il comando **New invoice** è ancora
> *abilitato* per un `viewer`, che scopre il rifiuto premendolo. L'involucro condiviso che lo renderebbe
> «presente ma disabilitato, con la spiegazione al passaggio del puntatore» esiste in questa change
> (`DisabledForRole` del design system, con le sue traduzioni nelle cinque lingue e i suoi collaudi), ma
> **nessuna schermata può usarlo finché il browser non conosce il ruolo** di chi guarda: quella lettura
> arriva con UC 0107, dov'è tracciata. Qui c'è il contratto e lo strumento, non il cablaggio.

---

## 5. Le operazioni esenti passano per tutti (verifica NON visiva)

Prerequisito: `member@acme.test` **senza** alcun accesso a `fatture` (passo 3.3 eseguito).

| # | Azione | Risultato atteso |
|---|---|---|
| 5.1 | `curl -sk -o /dev/null -w "%{http_code}\n" https://app.local.appgrove.app/api/fatture/v1/quota -H "authorization: Bearer $TOKEN_MEMBER"` | **200**, anche senza alcun accesso all'applicazione. Lo stato di quota è dichiarato **esente dai ruoli** nel documento delle operazioni, col suo motivo: un banner del consumo che diventa un rifiuto non informa nessuno. |
| 5.2 | Leggi il motivo dell'esenzione:<br>`grep -A9 '"quota.status"' services/fatture/src/main/java/app/appgrove/fatture/FattureOperationsContract.java` | Il motivo è scritto per esteso nel documento (con `-A6` la frase resta tagliata a metà). Non è un contrassegno vero/falso: un'esenzione senza motivo non si distingue da una dimenticanza, e il record la rifiuta. |
| 5.3 | Prova a «proteggere» l'esenzione: aggiungi `@RequiresAppRole(AppRole.viewer)` sopra `public QuotaStatusView fatture()` in `services/fatture/src/main/java/app/appgrove/fatture/QuotaResource.java` (e l'import corrispondente), poi `cd services && mvn -B -pl fatture test -Dtest='AppOperationsContractTest'` | **ROSSO**, con questo messaggio (il motivo dell'esenzione viene citato per esteso dentro il messaggio, così chi legge sa *perché* era esente):<br>`quota.status: dichiarata ESENTE dai ruoli («Stato di quota informativo: …») ma protetta dal varco con @RequiresAppRole(viewer). Un'esenzione protetta è un diritto rotto: si toglie l'annotazione, oppure l'operazione non è esente`<br>Rimetti il file come era (`git checkout -- services/fatture/src/main/java/app/appgrove/fatture/QuotaResource.java`) e ripeti: verde. |

---

## 6. Il Mini-CRM e il governo degli accessi (verifica NON visiva, con un limite dichiarato)

Il Mini-CRM è l'unica applicazione che esercita **tutte e tre** le righe della cascata, perché ha operazioni
che governano *chi* usa l'applicazione: il riquadro dei posti. La classificazione è: riepilogo `viewer` (le
sezioni di governo si vedono in sola lettura), assegnazione `admin`, revoca `admin` **con rilettura dal core**
perché è irreversibile.

| # | Azione | Risultato atteso |
|---|---|---|
| 6.1 | `cd services && mvn -B -pl crm test -Dtest='AppRoleGateTest'` | Verde, 14 collaudi. Fra questi `onlyAnAdminGovernsWhoUsesTheApplication` (un `editor` legge chi ha accesso e **non** lo cambia) e `anIrreversibleGovernanceOperationRereadsTheRoleFromTheSourceOfTruth` (la revoca interpella il core anche con la copia locale fresca). |
| 6.2 | `curl -sk -w "\nHTTP %{http_code} — corpo di %{size_download} byte\n" -X POST https://app.local.appgrove.app/api/crm/v1/seats -H "authorization: Bearer $(curl -sk https://api.local.appgrove.app/api/auth/login -H 'content-type: application/json' -d '{"email":"admin@acme.test","password":"Password1!"}' \| python3 -c 'import sys,json;print(json.load(sys.stdin)["access_token"])')" -H 'content-type: application/json' -d '{"userId":"seed-acme-member"}'` | **403 con un corpo di 0 byte.** Non è il varco del ruolo (che risponderebbe con una frase): è il varco vecchio sui ruoli di **piattaforma**, che Quarkus applica prima dei filtri. `admin@acme.test` è `admin` **sul Mini-CRM** ma `member` di piattaforma, quindi non arriva nemmeno al varco del ruolo. **È il limite dichiarato**: sul Mini-CRM la riga `admin` della cascata è provata dai collaudi automatici (6.1) e non dal vivo, finché UC 0111 non ritira i posti. Tracciato là. |

---

## 7. Il resto del contratto (verifica NON visiva)

| # | Azione | Risultato atteso |
|---|---|---|
| 7.1 | `node tools/scaffold-parity/parity-check.mjs` | Verde: i modelli-sorgente della skill `new-application` hanno il documento delle operazioni, il collaudo strutturale, il collaudo per ruolo e la finta sorgente del ruolo. L'applicazione numero tre non nascerà antiquata. |
| 7.2 | `node tools/e2e-coverage/check.mjs` | Verde. Nel registro `J-APP-ROLE-REFUSALS` è passato a **coperto** (livello 2, `frontend/apps/backoffice/e2e/roles.spec.ts`) ed è nato `J-ROLES` come buco dichiarato, posseduto da UC 0113. |
| 7.3 | `cd frontend/apps/backoffice && npx playwright test e2e/roles.spec.ts` | Verde, 2 prove: senza accesso la schermata dice **chi** abilita e non offre di riprovare; col ruolo insufficiente si legge **quale ruolo serve**. |
| 7.4 | `cd frontend && npm test` | Verde. Fra i collaudi nuovi: `appRoleAtLeast` (ordinamento, owner, valori ignoti) e `DisabledForRole` (presente-ma-disabilitato, spiegazione al puntatore, spiegazione per gli strumenti di assistenza, nessun involucro quando il ruolo basta, nessuna violazione di accessibilità). |
| 7.5 | Leggi la regola dove chi scrive un servizio la cerca:<br>`grep -n "cascata di classificazione" -A20 docs/04-services-backend.md` | La sezione «Semantica dei tre ruoli» con le tre domande in cascata, i tre chiarimenti, le esenzioni e il documento delle operazioni. |

---

## Pulizia

Non è obbligatoria, ma riporta lo stack allo stato di partenza:

```bash
# togli l'accesso di prova a `member@acme.test` su fatture (se ancora presente)
curl -sk -o /dev/null -w "%{http_code}\n" -X DELETE \
  "https://app.local.appgrove.app/api/platform/v1/apps/$FATTURE_ID/access/$MEMBER_ID" \
  -H "authorization: Bearer $TOKEN_OWNER"
# le fatture di prova restano: sono dati di dominio del conto di prova, innocui
```

E verifica di non aver lasciato file manomessi dai passi 1.3, 1.5 e 5.3:

```bash
git status --short services/
```
