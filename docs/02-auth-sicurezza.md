# Auth & sicurezza — Decisioni

**Stato**: 🟢 deciso
**Ultimo aggiornamento**: 2026-08-20

## Scope
Meccanica di autenticazione e autorizzazione: Cognito, flusso OAuth e gestione token nella SPA,
Pre-Token-Generation Lambda e naming dei claim, modello ruoli/authz, verifica JWT nei servizi,
enforcement dell'isolamento tenant, signup/inviti, secrets, CORS. Non copre la forma architetturale
(→ [01-architettura](01-architettura.md)) né i dettagli di networking/IaC (→ [06-infra-iac](06-infra-iac.md)).

## Vincoli ereditati da #01 (già decisi)
- Cognito = **solo autenticazione** (identity provider); membership/ruoli nel **core service** (DB).
- ~~**1 utente → 1 tenant**~~ → **una persona, più appartenenze** (UC 0116, change 0088): `tenant_id`
  = account id, `sub` = user_id; l'identità della persona è di piattaforma, l'appartenenza è di account.
  Vedi la decisione 14 rivista.
- **Pre-Token-Generation Lambda** legge la membership dal core e inietta `tenant_id` + ruoli come claim.
- Authorizer Cognito **centralizzato** su API Gateway; invariante: `tenant_id` solo dal JWT verificato.

## Topic dell'area (agenda)
- **A. Setup Cognito** — User Pool, app client SPA (public, senza secret), domain, MFA, password policy.
- **B. Login UX & flusso OAuth** — Hosted UI vs login custom; Authorization Code + PKCE; storage token nella SPA; refresh; logout.
- **C. Pre-Token-Gen Lambda** — meccanica iniezione claim, naming/namespace (`tenant_id`, `roles`), come la Lambda legge il core (DB diretto vs API interna).
- **D. Modello ruoli & authz** (da #01) — set ruoli (owner/admin/member), tenant-level vs per-app; platform admin; dove si applica l'authz (API GW = authn, servizio = authz).
- **E. Verifica JWT nei servizi** — Quarkus OIDC, issuer/audience/JWKS.
- **F. Enforcement isolamento tenant** — come si garantisce `WHERE tenant_id` (Hibernate filter/interceptor/repo base). Border con #04/#05.
- **G. Signup & inviti** — self-signup crea account + identità + appartenenza owner; token d'invito (scadenza, single-use) e accept flow, con l'identità creata solo se manca (UC 0116).
- **H. Secrets** — webhook Paddle, credenziali Lambda→core; dove (Secrets Manager/SSM). Border con #12.
- **I. CORS** — origin ammessi (CloudFront), config API Gateway.

## Decisioni prese

### Login & flusso (topic B)
1. **Login custom in React** (no Hosted UI): schermate di login/signup/reset dentro la shell.
2. **Pattern mini-BFF via Lambda**: l'autenticazione avviene **server-side** in una **auth Lambda**
   dietro API Gateway (il form posta le credenziali alla Lambda su TLS, non SRP nel browser). Route:
   - `POST /api/auth/login` → Cognito `InitiateAuth`; set refresh token in cookie `HttpOnly`+`Secure`+`SameSite`; ritorna access/id nel body.
   - `POST /api/auth/refresh` → legge il cookie; `REFRESH_TOKEN_AUTH`; ritorna nuovi access/id; **ruota** il cookie.
   - `POST /api/auth/logout` → cancella il cookie + Cognito `RevokeToken`.
3. **Storage token**: access/ID **in-memory** nella SPA; **refresh token in cookie `HttpOnly`** (mai in JS).
   Al reload la SPA chiama `/api/auth/refresh` → sessione ripristinata senza esporre il refresh token.
4. **App client Cognito confidenziale (con secret)**, possibile perché l'auth è server-side; secret in
   Secrets Manager (→ topic H / #12).
5. **TTL**: access/ID = **15 min**; refresh token = **24 h**, con rotazione a ogni refresh.
6. **Vincolo dominio (→ #06)**: frontend e API sotto lo stesso dominio registrabile (es. `app.appgrove.app`
   + `api.appgrove.app`, cookie su `.appgrove.app`) per avere il cookie **first-party**.

### Modello ruoli & authz (topic D, da #01)
7. **Ruoli di account**: `owner` e `member` (nel claim `roles`) — **due valori**, dalla change 0091
   (UC 0098). **`platform-admin`** separato (livello piattaforma) per il backoffice admin.

    **~~Ruolo `admin` di account~~ — RITIRATO** (UC 0098). Era un potere che valeva per *tutto*: ogni
    applicazione dell'account e anche le schermate di piattaforma. Nel modello centralizzato il potere sta
    sull'**applicazione**: `admin` riappare come ruolo di `platform.app_access` su *una* applicazione,
    accanto a `editor` e `viewer`, e non entra nel claim (un cambio di ruolo avrebbe effetto solo al
    rinnovo del token, e dieci applicazioni gonfierebbero ogni richiesta — UC 0099). Cosa **resta vero**:
    il claim `roles` continua a portare il ruolo di account, `@RolesAllowed` continua a governare le
    operazioni di piattaforma, e le annotazioni che nominano ancora `admin` restano come **tolleranza dei
    token già emessi** finché UC 0113 non la ritira con la sua data.

    **Dalla change 0092 (UC 0099) il claim non porta più `admin`**, nemmeno dove i dati non sono ancora
    convertiti: chi compone il token converte quel valore in `member` — in cloud e in locale, con la stessa
    regola scritta due volte e collaudata da entrambe le parti (`PlatformRoles.claimRole` in `commons`,
    `_claim_role` nella funzione Lambda). Chi ha un'appartenenza ancora scritta `admin` entra quindi col
    potere **minore**, non con nessun potere. La conversione dei dati e il ritiro della tolleranza sono di
    UC 0113.

    **Come si fa rispettare il ruolo sull'applicazione**: non con `@RolesAllowed` (che conosce solo il
    claim) ma con il **varco condiviso** di `services/commons` — `@RequiresAppRole(AppRole.editor)` su
    un'operazione, e un filtro che legge il ruolo dalla **copia locale** del servizio, alimentata dalla
    lettura `GET /api/platform/v1/me/app-access` del core e invalidata dagli eventi già usati per i diritti
    d'accesso. Tre rifiuti distinti, con identificativo stabile: nessun accesso (403), ruolo insufficiente
    (403, e dice quale serve), non decidibile (503 — un guasto nostro non si racconta come permesso
    mancante). Nessuna applicazione scrive confronti fra ruoli: è il difetto che l'epica 22 esiste per
    chiudere.
8. **Divisione delle responsabilità** *(rivista dalla change 0039, UC 0014)*: l'**API Gateway** usa l'**authorizer JWT
   nativo** — verifica firma/emittente/destinatario/**scadenza** e respinge con **401**; il **servizio** ri-valida il JWT
   (smallrye-jwt: `token_use`/`client_id`) e applica **authz sui ruoli** (`@RolesAllowed`) **e** l'**entitlement** (402,
   vedi [04-services-backend](04-services-backend.md) §7). Difesa in profondità. L'entitlement NON sta all'edge: su HTTP
   API v2 un authorizer custom non può restituire 402 (né 401 per token scaduto) — vedi UC 0014.

### Pre-Token-Gen Lambda (topic C)
9. **Lettura DB diretta**: la Lambda (in VPC) interroga lo schema `platform` del DB core — dopo UC 0116
    identità ⋈ appartenenza — per l'appartenenza e
   ruoli; credenziali in Secrets Manager. Meno hop nel path critico del login. (Accoppiamento accettato per il PoC.)
10. **Claim iniettati**: `tenant_id` (string) e `roles` (array). Quarkus mappa l'authz con
    `quarkus.oidc.roles.role-claim-path=roles`. **Fail-closed**: utente senza tenant/membership valida → niente claim → accesso negato.
    Il claim `roles` porta **solo** il ruolo di piattaforma (`owner`/`member`, più `platform-admin` da
    allow-list): **niente ruoli per applicazione**, per scelta esplicita di UC 0099 — quelli si leggono dal
    core e si fanno rispettare col varco condiviso, così una revoca vale in pochi secondi invece che al
    rinnovo del token.

    **Quale account, quando le appartenenze sono più di una** *(UC 0117, change 0089)*. Con più
    appartenenze il token non può dedurre l'account dalla persona: lo legge dall'**account attivo**
    conservato sull'identità (`identity.active_membership_id`), applicando una regola scritta una volta
    e attuata due —
    [`ActiveAccount`](../services/commons/src/main/java/app/appgrove/commons/membership/ActiveAccount.java)
    per il fornitore locale e la funzione Python del Pre-Token-Gen, con la stessa tabella di casi
    eseguita dai collaudi di entrambe:

    | Appartenenze attive | Valore conservato | Esito |
    |---|---|---|
    | nessuna | qualunque | nessun claim (come prima) |
    | una sola | qualunque, anche assente | quella, **ignorando** il valore conservato |
    | più di una | corrisponde a una di esse | quella |
    | più di una | assente o non corrispondente | nessun claim + **sfida di scelta dell'account** |

    **La sfida di scelta dell'account** *(UC 0118, change 0090)*. L'ultima riga della tabella non è più
    un rifiuto cieco: l'accesso risponde
    `200 {account_selection_required: true, choice_token, accounts}` — stessa forma della sfida del
    secondo fattore, additiva come fu quella — e `POST /api/auth/login/account` conserva la scelta ed
    emette la sessione. Il `choice_token` nasce **solo dopo** la verifica completa delle credenziali
    (secondo fattore incluso), quindi l'elenco degli account lo vede soltanto la persona. Vale in
    **entrambi** i fornitori: sul provider Cognito il caso si riconosce dall'assenza del claim
    `tenant_id` nell'access token appena emesso — la funzione del token non solleva eccezioni quando non
    riesce a scegliere, quindi l'accesso riesce e il token esce senza claim — e la sessione buona si
    ottiene rinnovando dopo aver conservato la scelta. Sui percorsi **non interattivi** (rinnovo,
    verifica dell'indirizzo con accesso automatico) resta il rifiuto `409`: non c'è nessuno a cui
    mostrare una schermata, e il rinnovo che fallisce riporta all'accesso — dove la scelta si fa, in un
    posto solo.

    **Il valore conservato non è creduto**: vale solo se corrisponde a un'appartenenza **attiva**
    trovata al momento della creazione del token. L'invariante #1 resta intatta — cambia la funzione
    che *calcola* il claim, non chi se ne fida — e una manomissione di quella colonna non diventa un
    varco fra due aziende. L'account attivo **non** è un attributo del gruppo di utenti Cognito:
    quel gruppo non dichiara attributi personalizzati e aggiungerne uno per via dichiarativa rischia
    di ricrearlo, cioè di perdere gli utenti.

    **La durata dell'access token è il ritardo massimo con cui una revoca ha effetto.** Con TTL di 15
    minuti (punto 5), una persona rimossa da un account — o che ha cambiato account attivo — continua
    a poter usare il token già emesso fino alla scadenza. Non è un varco introdotto da UC 0117: quel
    token vale per un account a cui la persona apparteneva davvero. È un legame che c'era già e che
    va detto invece di restare implicito; la stretta per le operazioni che modificano dati (rilettura
    dal core) è di **UC 0099**, la scelta della durata resta di **UC 0017**.

### Verifica JWT nei servizi (topic E)
11. **Quarkus OIDC**: issuer = User Pool Cognito, verifica firma via **JWKS**, audience = app client.
    Si usa l'**access token** per l'authz; `tenant_id`/`roles` letti dai claim verificati.

### Isolamento tenant (topic F)
12. **Hibernate multitenancy `DISCRIMINATOR`** + **`TenantResolver`** request-scoped che legge `tenant_id`
    dal JWT: Hibernate aggiunge il filtro tenant a **ogni** query automaticamente. **Fail-closed** se manca
    il tenant. Rende esecutive le invarianti #1/#2 senza `WHERE` manuale.

### Signup & inviti (topic G)
13. **Signup self-service aperto**: nuovo utente → **nuovo account (tenant) + membership owner**; email
    verification via Cognito.
14. **Flusso inviti**: l'owner invita una email → **invitation** con token **single-use** e **scadenza**
    (default 7 giorni) → all'accept l'invitato entra **nel tenant che invita** come `member`. Dalla change
    0091 (UC 0098) l'invito **non assegna più un ruolo**: chi entra non porta con sé alcun potere, e i
    poteri si concedono dopo, una applicazione alla volta.

    **~~Vincolo «1 utente → 1 tenant»~~ — SUPERATO** (UC 0116, change 0088). Era una scelta dichiarata e
    non una dimenticanza: con un solo utente per cliente, ripiegare l'appartenenza sull'utente
    risparmiava una tabella e una join. La si scriveva come **due indici unici globali** (indirizzo di
    posta e identificativo di autenticazione) su `platform.users`, che però è una tabella **dentro**
    l'account: è quel disallineamento — unicità globale su una riga di account — a produrre il vincolo
    di troppo. Le due conseguenze si vedevano al primo cliente: chi era invitato da un'azienda non
    poteva aprire un proprio account con lo stesso indirizzo, e chi aveva già provato appgrove per
    conto proprio non poteva essere invitato (l'invito partiva e il rifiuto arrivava dopo, come
    violazione di indice invece che come messaggio comprensibile).

    **Cosa lo sostituisce**: **una persona, più appartenenze**. L'identità (`platform.identity`) è
    un'entità di **piattaforma** — non appartiene a nessun account — e su di essa vive l'unicità
    globale di indirizzo e identificativo di autenticazione. L'appartenenza (`platform.membership`) è
    un'entità di **account** e porta ruolo e stato; l'unicità che serve davvero — «non due volte nello
    stesso account» — è ora **esplicita** sulla coppia (account, identità), limitata alle righe vive.
    La regola giusta era nascosta dentro una regola più larga.

    Due stati distinti, non uno: `identity.status` dice se la persona accede alla piattaforma (leva del
    titolare: limitazione del trattamento, art. 18), `membership.status` se si presenta come persona di
    **quell'** account (leva dell'owner). Chi emette il token pretende **entrambi** attivi.

    **Cosa NON cambia**: l'account resta il confine dei dati e `tenant_id` viene sempre e solo dal token
    verificato (invariante #1). Cambia soltanto il modo in cui il token stabilisce l'account: con una
    sola appartenenza — il caso di tutti gli utenti di oggi — il comportamento è identico a prima; la
    scelta fra più appartenenze (account attivo, selettore) è di **UC 0117**, e i due percorsi
    d'ingresso con i loro messaggi non rivelatori sono di **UC 0118** (change `0090`): l'invito a chi ha
    già un'identità la collega lato server e si accetta **dalla propria sessione**, mai coniando una
    parola d'accesso nuova su un'identità esistente; chi è già membro apre un proprio account da dentro
    la sessione. L'esito dell'invito è **identico** esista l'identità o no — sono lecite solo le
    collisioni che l'account può conoscere: «è già membro» e «c'è già un invito in attesa».

### Secrets (topic H, dettaglio store → #12)
15. **Zero secret nel codice o in file committati**; tutti in AWS, iniettati a runtime. Per l'auth:
    app client secret Cognito, credenziali DB per la Lambda→core, signing secret webhook Paddle.
    Store (risolto in [12-environments-config](12-environments-config.md)): **SSM Parameter Store** per config/secret
    applicativi (app client secret, signing webhook Paddle); **Secrets Manager** solo per le credenziali DB.

### CORS & cookie (topic I, border con #06)
16. **API Gateway CORS**: origin = dominio frontend esplicito (es. `https://app.appgrove.app`),
    `Access-Control-Allow-Credentials: true`, **niente wildcard `*`** (incompatibile con credentials).
17. **Cookie refresh**: **host-only sull'host dell'API** (es. `api.appgrove.app`, **nessun** attributo `Domain`),
    `Secure`, `HttpOnly`, `SameSite=Lax`, **`Path=/api/auth`**. Richiede `app.*` e `api.*` sotto lo stesso
    registrable domain (`appgrove.app`) perché Lax lo invii nella fetch cross-sottodominio. Host-only →
    **isolamento automatico tra ambienti** (il cookie di prod non raggiunge test). Schema domini → [12-environments-config](12-environments-config.md).

### 2FA & password policy (dettaglio dai casi d'uso, 2026-06-16)
18. **2FA TOTP (authenticator) opzionale**, opt-in **dal profilo utente**, con **banner** di nudge nel backoffice (no MFA
    obbligatoria al signup). L'**auth Lambda** gestisce la challenge MFA Cognito (`SOFTWARE_TOKEN_MFA`) al login.
19. **Password policy** (default): min 10 caratteri, maiuscola+minuscola+numero. Flussi completi → [usecases/01-auth-registrazione](usecases/01-auth-registrazione.md).

## Questioni aperte
_Nessuna — #02 chiuso (dettaglio flussi auth in usecases/01)._

## Scope PoC (nota cross-area)
- **Una delle due app demo deve essere B2B/multi-utente** (l'altra single-user) per validare end-to-end
  inviti, membership e ruoli. Aggiorna la roadmap del [recap](../recap_marketplace_microsaas.md) (App1 note,
  App2 dashboard erano entrambe single-user). Dettaglio app → [03-frontend](03-frontend.md)/[04-services-backend](04-services-backend.md).

## Alternative valutate / scartate
- **Hosted UI** — scartata (per ora): si preferisce controllo totale su UX/branding col login custom.
- **localStorage / sessionStorage per i token** — scartati: il refresh token sta in cookie `HttpOnly` (no JS).
- **SRP nel browser** — scartato: per avere il cookie `HttpOnly` l'auth è server-side nella Lambda.
- **App client public** — scartato: con auth server-side si usa un client confidenziale (con secret).

## Impatti su altre aree
- [01-architettura](01-architettura.md), [04-services-backend](04-services-backend.md), [05-persistenza-dati](05-persistenza-dati.md), [06-infra-iac](06-infra-iac.md), [12-environments-config](12-environments-config.md)
