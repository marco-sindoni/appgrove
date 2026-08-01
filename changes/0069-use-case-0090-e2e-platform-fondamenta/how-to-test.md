# Come verificare a mano — Change 0069 (suite e2e di piattaforma, UC 0090)

Checklist di verifica **manuale** per questa story: cosa lanciare, cosa aprire e cosa devi
vedere coi tuoi occhi. Ogni voce è "azione → risultato atteso". Prerequisiti: Docker/Colima
attivo, Java 21 + Maven, Node 24.

## 1. La suite di piattaforma gira ed è verde

- **Azione**: dalla radice del repo lancia `./run-tests.sh platform` (prima volta: qualche
  minuto — build Maven dei servizi e build delle SPA).
- **Atteso**: passi colorati in sequenza (compose → migrate+seed → build artefatti → avvio
  servizi con porte `+12000` → build SPA → "SPA servite: backoffice :24173 · admin :24174" →
  journey). In coda: `2 passed` e `✓ suite di piattaforma: verde`, exit code 0.

- **Azione**: rilancia **subito** lo stesso comando una seconda volta, senza pulire nulla.
- **Atteso**: di nuovo verde (idempotenza: email e tenant nuovi a ogni run, compose riusato).

## 2. Il journey visto coi tuoi occhi (riproduzione manuale di J-REG)

La suite fa questo percorso in automatico; rifallo a mano per vederlo. Serve lo stack di
sviluppo: `./app-start.sh` (poi naviga su https://app.local.appgrove.app — è lo stesso
percorso utente, sulle porte dev anziché su quelle della suite).

- **Azione**: apri il backoffice → "Create your account" (`/signup`); inventa un'email tipo
  `mario+test1@esempio.it`, password valida (≥10 caratteri, maiuscola, minuscola, numero),
  nome a piacere; lascia la spunta newsletter **spenta**; premi "Create account".
- **Atteso**: passi allo step "Verify email": "We sent a verification link to …".
- **Azione**: apri l'interfaccia di Mailpit su <http://localhost:8025>.
- **Atteso**: in casella c'è un'email da **noreply@appgrove.app** con oggetto
  **"Confirm your email address"** (in inglese se la UI era in inglese), con un bottone e un
  link testuale verso `/verify?token=…`.
- **Azione**: clicca il link dell'email.
- **Atteso**: pagina di verifica → accesso automatico → step "Workspace" col campo
  **prefillato** col nome che hai dato; cambia il nome (es. "Officina Rossi") e premi
  "Continue", poi "Go to dashboard".
- **Atteso**: al primo ingresso compare il modale **"Updated legal documents"**: spunta tutti
  i consensi e premi "Continue" — il modale sparisce e non ricompare ai reload.
- **Atteso (sidebar)**: sotto "Your apps" vedi le app della **baseline freemium** (con il
  seed attuale: "Invoices"); NON vedi un elenco vuoto — è il comportamento corretto (tier
  gratuito = entitled, UC 0027).

## 3. Controlli non visivi (DB e API)

- **Azione**: `docker exec -it $(docker ps --format '{{.Names}}' | grep -m1 postgres) psql -U appgrove -d appgrove`
  poi: `select u.email, u.role, u.status, a.name from platform.users u join platform.accounts a on a.id::text = u.tenant_id where u.email like 'mario+test1%';`
- **Atteso**: **una sola riga**: ruolo `owner`, status `active`, nome account = il nome
  workspace scelto. Nessuna seconda riga con la stessa email (isolamento tenant).
- **Azione** (facoltativa, entitlements): dal browser autenticato apri gli strumenti di
  sviluppo → rete → ricarica: la chiamata `GET /api/platform/v1/me/entitlements` risponde 200
  e le app elencate coincidono con quelle mostrate in sidebar.

## 4. Convivenza degli stack (porte)

- **Azione**: con lo stack dev ancora acceso (`./app-start.sh`), lancia in parallelo
  `./run-tests.sh platform`.
- **Atteso**: la suite resta verde e lo stack dev continua a rispondere: i servizi della
  suite stanno su porte +12000 (core :20080, auth :21100), le SPA su :24173/:24174 — nessuna
  collisione con :8080/:9100/:5173.

## 5. Diagnosi su rosso (da conoscere, non da riprodurre)

- **Azione**: apri `tools/platform-e2e/README.md`.
- **Atteso**: trovi il runbook: trace/screenshot/video in `tools/platform-e2e/test-results/`
  (`npx playwright show-trace …`), log dei servizi in `tools/platform-e2e/.run/*.log`,
  casella Mailpit su :8025, rilancio mirato con `tools/platform-e2e/run.sh --journey J-REG`.

## 6. L'entrypoint canonico resta completo

- **Azione**: `./run-tests.sh -h`.
- **Atteso**: l'area `platform` compare nell'elenco e nella descrizione in testa allo script.
- **Azione**: `./run-tests.sh` (senza parametri; lungo).
- **Atteso**: tutte le aree eseguite, incluse `smoke` (rifattorizzata, comportamento
  invariato) e `platform`; riepilogo finale tutto verde.
