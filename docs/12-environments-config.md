# Environments & config management — Decisioni

**Stato**: 🟢 deciso (provisioning per env → #06; run locale passo-passo → #11)
**Ultimo aggiornamento**: 2026-06-14

## Scope
Tier di ambiente (local/test/prod), cosa gira dove, gestione di config e secret per ambiente, profili
applicativi, costi. Border con [11-developer-experience](11-developer-experience.md) (come si esegue in
locale), [06-infra-iac](06-infra-iac.md) (provisioning per env) e [07-devops-cicd](07-devops-cicd.md) (deploy per env).

## Contesto macchina dev (rilevato 2026-06-14)
- AWS CLI autenticata come IAM user `marco.sindoni` (account `834306158663`; profili `default`, `m22`).
- Locale: `psql` 18.1, Node 25, Java 25, Maven 3.9. Docker installato ma **daemon non in esecuzione**.

## Principio guida
**Cost-first**: in locale tutto ciò che può girare gratis (DB); su AWS solo il minimo (test microscopico + prod).
Gli schemi/grant su AWS nascono da **Terraform + Flyway/CI** (mai a mano); in locale possono essere creati via `psql`.

## Topic dell'area (agenda)
- **A. Tier di ambiente** — quanti e quali (local/test/prod); ambienti effimeri PR/CI?
- **B. Scope dello stack locale** — DB locale; Cognito dev pool su AWS (free) vs mock; resto delle dipendenze.
- **C. Forma del DB di test su AWS** — Aurora Serverless v2 scale-to-0 vs RDS micro vs condiviso.
- **D. Profili & config applicativa** — Quarkus `%dev/%test/%prod`, config frontend per env, come si seleziona l'ambiente.
- **E. Secrets per ambiente** — Secrets Manager vs SSM (chiude il rimando di #02); local da `.env` non committato.
- **F. Parità & versioni** — allineamento versione Postgres local/test/prod; differenze accettate (RDS Proxy/IAM solo su AWS).

## Decisioni prese

### Tier (topic A)
1. **Tre tier: `local`, `test`, `prod`.** Niente ambienti effimeri per PR nel PoC.

### Stack locale (topic B)
2. **DB in locale** (Postgres), per costo ~0 e iterazione veloce. Schemi/ruoli/grant locali creabili via `psql`.
3. **Locale 100% offline — ZERO AWS** (revisione 2026-06-16): si sviluppa interamente in locale senza toccare AWS;
   gli ambienti AWS (test/prod) si creano **solo a sviluppo finito**. Conseguenza: **Cognito emulato in locale**
   (emulatore/OIDC fittizio), DB Postgres locale, auth Lambda eseguita localmente. _Supera la precedente scelta
   "pool Cognito dev su AWS". Modalità esatta (emulatore, setup script, seed) → [11-developer-experience](11-developer-experience.md)._

### DB di test su AWS (topic C)
4. **Aurora Serverless v2 con scale-to-0** (min 0 ACU, auto-pause): **parità totale con prod** e costo ~0 da idle.
   Scartati RDS micro (motore diverso) e DB condiviso con prod (blast radius).

### Config applicativa (topic D)
5. **Backend**: profili Quarkus `%dev`/`%test`/`%prod` in `application.properties` + variabili d'ambiente per endpoint/secret.
6. **Frontend**: **runtime `config.json`** caricato all'avvio (API base URL, Cognito pool/client id, ecc.),
   iniettato per ambiente. **Un solo build** promosso `local→test→prod` (niente rebuild per env).

### Secrets per ambiente (topic E — chiude il rimando di #02)
7. **SSM Parameter Store (SecureString)** per config e secret applicativi (economico); **Secrets Manager
   solo per le credenziali DB** (rotation/integrazione RDS). In **local**: secret da **`.env` non committato**
   (`.env.example` committato come template).

### Parità & versioni (topic F)
8. **PostgreSQL major = 17** allineato su local/test/prod (Aurora Serverless v2 supporta 17; server locale
   `postgresql@17`). Differenze accettate solo su AWS: **RDS Proxy** e **IAM auth** assenti in locale.

### Domini per ambiente (topic, provisioning → #06)
9. **prod** su `appgrove.app`: vetrina `appgrove.app`, backoffice `app.appgrove.app`, **console admin `admin.appgrove.app`**, API/auth `api.appgrove.app`.
10. **test** su `test.appgrove.app` (**sottodominio** di prod, niente registrazione extra): vetrina `test.appgrove.app`,
    `app.test.appgrove.app`, `admin.test.appgrove.app`, `api.test.appgrove.app`. Isolamento garantito dal **cookie host-only**
    (§#02): prod e test condividono il registrable domain, quindi **vincolo**: mai cookie con `Domain` esteso, solo host-only.
11. **local**: mirror sotto `appgrove.app` con segmento `local.` — `app.local.appgrove.app` / `admin.local.appgrove.app` /
    `api.local.appgrove.app` → `127.0.0.1` via `/etc/hosts`, **TLS locale via mkcert** (obbligatorio: `.app` è in HSTS preload, HTTPS forzato),
    reverse proxy locale (dettaglio → [11-developer-experience](11-developer-experience.md)). Parità totale cookie/same-site/CORS.
12. **DNS/registrazione**: **un solo dominio** `appgrove.app` su **Route53** (una hosted zone); ACM/CloudFront via
    Terraform (→ #06). **Certificati ACM gratuiti** per livello: `*.appgrove.app` + apex (prod, copre anche `test.appgrove.app`)
    e `*.test.appgrove.app` (test); local servito dalla CA locale mkcert.
13. **`.app` = HSTS preload** → HTTPS obbligatorio ovunque, locale incluso (vincolo, non opzione).

## Questioni aperte
_Nessuna._
_Nessuna sull'impianto degli ambienti. Il provisioning per env (scale-to-0, RDS Proxy, parametri SSM) è
materia di [06-infra-iac](06-infra-iac.md); il "come si esegue in locale" passo-passo è materia di
[11-developer-experience](11-developer-experience.md)._

## Alternative valutate / scartate
- **PR effimeri / 2 soli tier** — scartati: 3 tier (local/test/prod) è l'equilibrio giusto per il PoC.
- **Mock/LocalStack in locale** — scartato: Cognito dev su AWS dà token veri a costo ~0, meno drift.
- **RDS micro / DB condiviso per il test** — scartati a favore di Aurora Serverless v2 scale-to-0 (parità + costo idle ~0).
- **Config frontend build-time / secret su un solo store** — scartati a favore di runtime config.json e SSM+SecretsManager(DB).

## Impatti su altre aree
- [02-auth-sicurezza](02-auth-sicurezza.md), [05-persistenza-dati](05-persistenza-dati.md), [06-infra-iac](06-infra-iac.md), [07-devops-cicd](07-devops-cicd.md), [11-developer-experience](11-developer-experience.md)
