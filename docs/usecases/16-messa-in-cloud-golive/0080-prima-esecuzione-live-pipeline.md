# UC 0080 — Prima esecuzione live della pipeline + configurazione repo GitHub

**Area**: 16-messa-in-cloud-golive · **Fase**: evo (messa in cloud) · **Stato**: 🟢 scritto (evo, da implementare)
**Dipendenze**: UC 0005 (pipeline CI/CD), UC 0003 (fondamenta Terraform), UC 0004 (modulo microsaas_app), UC 0055 (risorse condivise per-ambiente)
**Fonte**: R14 (Tabella residui _INDEX.md); docs/_BACKLOG.md §"Attivazione ambienti cloud", §Script/tooling DevOps
**Ultimo aggiornamento**: 2026-07-26

## 1. Obiettivo / Scope
Accendere per la **prima volta dal vivo** la pipeline (GitHub Actions), che finora è stata scritta e validata solo staticamente
ma **mai eseguita** verso AWS. I workflow sono **inerti** finché la variabile di repo `AWS_ACCOUNT_ID` non esiste: senza di
essa i job cloud non si attivano. Questo UC configura il repository GitHub e poi esegue la prima corsa completa.
**Incluso**: variabile `AWS_ACCOUNT_ID`, environment `prod` con revisore obbligatorio, protezione del ramo `main`, secret
`INFRACOST_API_KEY`, verifica della fatturazione dei runner, e la **prima esecuzione live** (PR di prova → merge → deploy
`test` → build native → tag → promozione `prod`). **Escluso**: la definizione della pipeline e dell'infra (UC 0005/0003/0004),
gli smoke reali del cloud (UC 0081).

## 2. Attori & ruoli
- **Founder / platform engineer**: configura variabili, environment, protezioni e secret del repo; approva il gate di release.
- **Sistema (CI GitHub Actions)**: esegue plan in PR, deploy-test, release-prod, env-ops con i ruoli **OIDC** (OpenID Connect,
  autenticazione senza chiavi statiche) `appgrove-github-actions-{test,prod}` già presenti in `infra/global/oidc.tf`.
- **Terzi**: **GitHub** (repo, environment, protezioni), **AWS** (destinazione del deploy), **Infracost** (stima costi, terzo
  con trattamento negli Stati Uniti).

## 3. Precondizioni
- Fondamenta Terraform (UC 0003) applicate: ruoli OIDC e stato remoto esistono.
- Modulo `microsaas_app` (UC 0004) e risorse condivise per-ambiente (UC 0055) definiti.
- Ambiente `test` pronto ad accogliere il primo deploy (aggancio con lo script di attivazione, UC 0082).

## 4. Flusso principale
1. **Variabile di repo `AWS_ACCOUNT_ID`**: crearla nelle impostazioni del repository. È l'interruttore che **accende i job
   cloud** (plan in PR, deploy-test, release-prod, env-ops): senza, restano inerti.
2. **Environment GitHub `prod` con revisore obbligatorio** (required reviewer): è il **gate di approvazione** della release
   verso produzione; nessun apply in prod senza un OK umano.
3. **Protezione del ramo `main`** (branch protection): PR obbligatoria + check richiesti — backend/backend-security, frontend,
   `oasdiff` (confronto delle differenze del contratto delle interfacce), compliance; `infra-check` e `plan-test` quando la PR
   tocca infra; unione tramite squash-merge.
4. **Secret `INFRACOST_API_KEY`**: senza, il job Infracost **salta con un avviso** (non blocca). Nota di residenza dati:
   Infracost è un fornitore statunitense → valutare l'opportunità coerentemente con la postura "dati in Unione Europea".
5. **Verifica fatturazione dei runner** `ubuntu-24.04-arm` su repository privato: le build native sono ARM64 e girano su runner
   a pagamento sul privato → confermare che la fatturazione sia in ordine prima della prima corsa.
6. **Prima esecuzione live**, in ordine:
   a. aprire una **PR di prova** → verificare il plan commentato + la suite completa;
   b. **squash-merge** → parte il **deploy in `test`**: `terraform apply` → **Flyway migrate** (migrazione schema database) →
      **apply ECS** rolling → **health check**;
   c. lanciare la build **native** (via `[graal]` o dispatch manuale) sullo stesso commit;
   d. creare un **tag `v*`** → gate native (solo immagini native su ECR) + plan salvato + **approvazione** del revisore →
      **promozione in `prod`**.
7. Verificare il criterio chiave di UC 0005: **una PR con una violazione cross-tenant non passa** (il check di sicurezza è
   bloccante).

## 5. Flussi alternativi / edge / errori
- **`AWS_ACCOUNT_ID` assente/errato**: i job cloud non partono (o falliscono sull'assunzione del ruolo OIDC) → ricontrollare la
  variabile e la corrispondenza con l'account AWS della regione `eu-west-1`.
- **Gate prod senza immagine native**: la pipeline del tag **fallisce** con messaggio guidato → lanciare la build native sullo
  stesso commit e ri-taggare (comportamento definito in UC 0005).
- **Check richiesti non configurati**: senza branch protection una PR può essere unita senza suite verde → configurare i check
  come obbligatori prima della prima PR reale.
- **Health check rosso in `test`**: il deploy si ferma → diagnosi con l'osservabilità (UC 0006); non promuovere in prod finché
  `test` non è verde.
- **Runner ARM64 non fatturabile**: la build native fallisce all'avvio del runner → sistemare la fatturazione GitHub.

## 6. Risorse & runbook
**Risorse (lato GitHub, non Terraform)**: variabile `AWS_ACCOUNT_ID`; environment `prod` con revisore; regole di protezione di
`main`; secret `INFRACOST_API_KEY`. **Lato AWS**: nessuna risorsa nuova da questo UC — usa quelle di UC 0003/0004/0055. I ruoli
OIDC sono già in `infra/global/oidc.tf`.
**Runbook passo-passo**:
1. Impostazioni repo → Variables → `AWS_ACCOUNT_ID`.
2. Impostazioni repo → Environments → `prod` → required reviewer.
3. Impostazioni repo → Branches → protezione di `main` (PR + check richiesti + squash).
4. Impostazioni repo → Secrets → `INFRACOST_API_KEY` (opzionale, con nota di residenza).
5. Verificare piano/fatturazione runner ARM64.
6. PR di prova → merge → osservare deploy `test` → build native → tag → approvare gate → prod.
**Rollback**: rimuovere `AWS_ACCOUNT_ID` **rispegne** i job cloud (torna tutto inerte). Un deploy `test` andato male si corregge
con un nuovo commit; la promozione prod resta dietro il gate umano, quindi è sempre volontaria.

## 7. Dati toccati
Nessun dato personale trattato direttamente. Si toccano **configurazione del repository** e **stato dell'infrastruttura**
(Terraform state), non dati applicativi. La migrazione Flyway crea/aggiorna lo schema, senza inserire dati personali.

## 8. Permessi & gate
- Accessi: chi configura il repo deve avere i permessi di amministrazione su GitHub; l'apply su AWS avviene **solo** tramite i
  ruoli OIDC per ambiente, mai con chiavi statiche.
- **Gate di ambiente**: `test` si aggiorna automaticamente al merge; `prod` è protetto dal **revisore obbligatorio** e dal gate
  native. Questi presidi restano dello sviluppatore (coerente con i limiti dell'autopilot).

## 9. Requisiti di test / verifica
Verifiche dal vivo alla prima corsa:
- la PR di prova produce plan commentato e suite verde; senza check richiesti non è unibile;
- una PR con violazione cross-tenant **non passa** (criterio di UC 0005);
- il merge deploya in `test` con migrate + health verde;
- il tag richiede build native presente, plan salvato e approvazione prima di toccare `prod`;
- con `AWS_ACCOUNT_ID` assente i job cloud non partono (verifica dell'interruttore).

## 10. Riferimenti & Definition of Done
- **Fonte**: R14 (Tabella residui _INDEX.md); docs/_BACKLOG.md §"Attivazione ambienti cloud", §Script/tooling DevOps.
- **DoD**: repo configurato (variabile, environment prod, protezione main, secret); prima corsa completa eseguita
  (PR→merge→deploy test→native→tag→prod approvato); criterio cross-tenant verificato dal vivo.

## Punti aperti / decisioni differite
- **Infracost sì/no**: la scelta se usare un fornitore statunitense per la stima costi è di postura privacy → se si decide di
  non usarlo, il job resta a saltare con avviso; traccia da tenere qui finché non si decide.
- **Primo deploy di quali app**: la prima corsa live deve includere almeno l'app #1 `fatture`; se all'accensione ci fossero più
  app, verificare che il deploy le copra tutte — dettaglio da confermare al momento dell'esecuzione.
