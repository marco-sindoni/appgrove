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

## Avvio manuale (finché non arriva UC 0009)

Prerequisiti una tantum:

```bash
# 1) container engine attivo (qui: Colima)
colima start

# 2) CA locale mkcert fidata nel sistema (chiede la password del Mac)
mkcert -install

# 3) certificati per i domini locali → dev/certs/ (nomi stabili usati dal Caddyfile)
mkcert -cert-file dev/certs/local.appgrove.app.pem \
       -key-file  dev/certs/local.appgrove.app-key.pem \
       "local.appgrove.app" "*.local.appgrove.app"

# 4) /etc/hosts → 127.0.0.1 per i domini locali (richiede sudo)
#    app.local.appgrove.app  admin.local.appgrove.app  api.local.appgrove.app  local.appgrove.app

# 5) config locale
cp dev/.env.example dev/.env
```

Su/giù:

```bash
docker compose -f dev/docker-compose.yml --env-file dev/.env up -d
docker compose -f dev/docker-compose.yml --env-file dev/.env down
```

Smoke test rapido:

```bash
curl -s http://localhost/healthz                 # → "appgrove-dev proxy ok"
open http://localhost:8025                        # Mailpit UI
open http://localhost:9001                        # MinIO console
curl -s "http://localhost:9324/?Action=ListQueues" # ElasticMQ: elenca le code
```

> `dev/.env` e `dev/certs/*` **non sono committati** (`.gitignore`). Reset dati = `docker compose ... down -v`.
