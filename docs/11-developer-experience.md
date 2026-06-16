# Developer experience / local dev — Decisioni

**Stato**: 🟡 in corso (direttiva registrata; resto da discutere)
**Ultimo aggiornamento**: 2026-06-16

## Scope
Come si sviluppa appgrove in locale: setup, esecuzione di frontend + servizi + DB, emulazione di auth/Cognito,
seed data, e gli script che rendono tutto semplice. Border con [12-environments-config](12-environments-config.md)
(tier/ambienti) e [06-infra-iac](06-infra-iac.md) (Terraform/AWS, NON usato in locale).

## Direttiva registrata (2026-06-16)
- **Sviluppo 100% locale, ZERO AWS**: si deve poter sviluppare e provare l'intero sistema senza creare nulla su AWS;
  gli ambienti cloud (test/prod) si creano **solo a sviluppo finito**. _Supera la scelta #12 "pool Cognito dev su AWS"._
- **Setup con uno script**: ambiente di sviluppo avviabile in modo semplice con un singolo script (ben documentato),
  coerente con lo spirito dei wrapper di [06-infra-iac](06-infra-iac.md) §25.

## Topic dell'area (agenda, da discutere)
- **A. Orchestrazione locale** — come girano insieme DB Postgres + servizi Quarkus + 2 frontend (docker compose? processi? Quarkus dev mode?).
- **B. Emulazione auth/Cognito in locale** — emulatore Cognito (es. `cognito-local`) vs OIDC/JWT fittizio vs Quarkus OIDC mock; come si replica il flusso auth Lambda + claim `tenant_id`/`roles` + cookie.
- **C. Setup script** — un comando per: dipendenze, DB locale + schemi/grant, migrazioni Flyway, seed, avvio servizi/frontend. Documentato.
- **D. Seed data** — dati demo coerenti (account, utenti, app catalog, entitlements) per lavorare subito.
- **E. Config locale** — `config.json` runtime puntato al locale; profili Quarkus `%dev`; domini locali + mkcert (da #12 §11).
- **F. Parità & limiti** — cosa NON è replicabile in locale (RDS Proxy, IAM, EventBridge reale?) e come si simula (es. LocalStack mirato? o stub).

## Decisioni prese
1. **Locale 100% offline (zero AWS)** + **setup script unico e documentato** (direttiva sopra).

## Questioni aperte
_Vedi agenda (A→F)._

## Alternative valutate / scartate
- **Pool Cognito dev su AWS per il locale** (#12 originale) — scartato: in locale niente AWS, Cognito si emula.

## Impatti su altre aree
- [02-auth-sicurezza](02-auth-sicurezza.md), [06-infra-iac](06-infra-iac.md), [12-environments-config](12-environments-config.md), [10-testing](10-testing.md)
