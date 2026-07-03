# L3 — smoke reale su Paddle Sandbox (UC 0029)

Terzo livello della strategia di test pagamenti (#09 D20): **uno** smoke E2E contro **Paddle
Sandbox** reale (vero Paddle.js + carta di test + vero webhook) per validare il contratto che
L1/L2 mockano. **Non è per-PR** e **non è in `run-tests.sh`**: appartiene al flusso di release
tag→prod.

## Quando e come gira

- **Attivazione**: la suite si **auto-skippa** se manca l'ambiente. Per eseguirla:

  ```bash
  cd frontend/apps/backoffice
  APPGROVE_L3_BASE_URL=https://<env-deployato> \
  APPGROVE_L3_USER_EMAIL=<utente-smoke> \
  APPGROVE_L3_USER_PASSWORD=<password> \
  npx playwright test -c playwright.l3.config.ts
  ```

- **Carta di test sandbox**: `4242 4242 4242 4242`, scadenza futura qualsiasi, CVC `100`.
- **Mai in locale, mai pagamenti reali** (#09 D20): l'URL deve puntare a un ambiente con account
  Paddle **Sandbox**.

## Runbook di release (#07 b1, #09 D20 L3)

1. Tag di release → la pipeline esegue questo smoke contro l'ambiente di staging/pre-prod.
2. Esito verde → il gate di approvazione manuale prod può procedere.
3. **Sandbox down / smoke rosso per cause esterne**: chi approva può **forzare con motivazione
   registrata** (audit, es. "sandbox Paddle down") — l'override è obbligatorio, non silenzioso.

## Prerequisiti (tracciati, non di questa change)

- **UC 0001** — account Paddle (Sandbox): senza, nessun ambiente sandbox esiste.
- **#14 / gate Paddle** — loader del **vero** Paddle.js e `PaddlePaymentProvider` reale (oggi
  placeholder: `paddle.ts` ritorna sempre lo stub).
- **UC 0005** — pipeline di release tag→prod che esegue questo job e implementa gate + override
  motivato con audit.
- Selettori dell'iframe Paddle da finalizzare alla **prima esecuzione reale** (UC 0029, punti aperti).
