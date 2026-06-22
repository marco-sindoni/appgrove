# `dev/` — stack di sviluppo locale (UC 0008)

Dipendenze infrastrutturali in **Docker Compose**, 100% offline (**zero AWS**), che riproducono i
**comportamenti applicativi** di prod (#11 §14). Le **app** (Quarkus dev mode + Vite) girano come
**processi sull'host**, non in Compose (modello ibrido #11 §2): il reverse proxy le raggiunge via
`host.docker.internal`.

| Servizio | Immagine | Sostituisce (AWS) | Porte host (default) |
|---|---|---|---|
| `postgres` | `postgres:17` | Aurora Serverless v2 | 5432 |
| `proxy` (Caddy) | `caddy:2` | API Gateway v2 | 80, 443 |
| `mailpit` | `axllent/mailpit` | SES | 1025 (SMTP), 8025 (UI) |
| `minio` | `minio/minio` | S3 | 9000 (API), 9001 (console) |
| `elasticmq` | `softwaremill/elasticmq-native` | SQS | 9324, 9325 (stats) |
| `auth-local` | — | Cognito + auth Lambda | _placeholder, UC 0010_ |

## Cosa NON è qui (per design)

- **Script di orchestrazione** `dev setup/up/down/seed/reset/migrate/service/doctor` → **UC 0009**.
- **Provider auth locale** (JWT/JWKS, claim dal DB, refresh, TOTP) → **UC 0010** (blocco commentato nel compose + route `/api/auth/*` nel Caddyfile).
- **Seed data** deterministico → **UC 0011**. **Stub Paddle** → **UC 0023**.
- Routing `/api/<app_id>/v1/*` verso i servizi → template commentato nel Caddyfile (le porte le assegna UC 0009 / `new-application`).

## Quickstart — gli script `dev` (UC 0009)

Tutto passa per il **dispatcher** `dev/dev`, lanciabile dalla root con `./dev.sh <comando>`
(per digitare `dev <comando>` senza `./dev.sh`: `alias dev="$(pwd)/dev/dev"`).

```bash
./dev.sh doctor     # preflight read-only: engine, mkcert, hosts, porte, prerequisiti
./dev.sh setup      # bootstrap one-time idempotente (CA mkcert, certs, /etc/hosts, .env, stack su)
./dev.sh up         # avvia lo stack (Postgres, proxy, Mailpit, MinIO, ElasticMQ)
./dev.sh down       # ferma lo stack            (./dev.sh down -v  → reset volumi)
./dev.sh reset      # wipe volumi + riavvio (+ reseed quando disponibile)
```

Comodità storiche (alias): `./dev-start.sh` = `./dev.sh up`, `./dev-stop.sh` = `./dev.sh down`.

`./dev.sh <comando> --help` per i dettagli. **Stub** in attesa dei rispettivi UC: `seed` (UC 0011),
`migrate` / `service <app_id>` (servizi/UC 0046), e dentro `setup` i passi chiavi-JWT (UC 0010) e `config.json` (frontend).

Smoke test rapido (a stack su):

```bash
curl -s http://localhost/healthz                   # → "appgrove-dev proxy ok"
open http://localhost:8025                          # Mailpit UI
open http://localhost:9001                          # MinIO console
curl -s "http://localhost:9324/?Action=ListQueues"  # ElasticMQ: elenca le code
```

### Troubleshooting

- **Docker daemon non attivo** → `colima start` (o lascia che `./dev.sh up`/`setup` lo avvii). Persistente al login: `brew services start colima`.
- **Browser non si fida del certificato** → `mkcert -install` (chiede la password del Mac).
- **Porta occupata** (es. 5432 da un Postgres locale che ombreggia il container) → `./dev.sh doctor` la segnala; ferma il processo o cambia porta in `dev/.env`.
- **`JAVA_HOME` / keytool** durante la generazione certificati → `dev` usa già il workaround (`env -u JAVA_HOME`); per `mkcert -install` nello store Java serve `JAVA_HOME` corretta.

> `dev/.env` e `dev/certs/*` **non sono committati** (`.gitignore`). Reset dati = `./dev.sh down -v`.
