# UC 0081 — Smoke reali cloud alla prima accensione di test

**Area**: 16-messa-in-cloud-golive · **Fase**: evo (messa in cloud) · **Stato**: 🟢 scritto (evo, da implementare)
**Dipendenze**: UC 0015 (Cognito), UC 0016 (Pre-Token-Gen + JWT), UC 0014 (authorizer all'edge), UC 0018 (email SES), UC 0055 (risorse condivise per-ambiente), UC 0005 (pipeline CI/CD)
**Fonte**: R15 (Tabella residui _INDEX.md); docs/_BACKLOG.md §"Attivazione ambienti cloud"
**Ultimo aggiornamento**: 2026-07-26

## 1. Obiettivo / Scope
Eseguire, alla **prima accensione dell'ambiente `test`**, le verifiche "di fumo" (smoke: controlli rapidi che le cose
essenziali funzionino) che **in locale non sono possibili**, perché in locale non esistono un vero **API Gateway** (il portone
delle interfacce lato AWS), un vero **Cognito** (il servizio di identità/login di AWS) né un vero SES. Sono le verifiche che
chiudono la classe "funziona in locale ma non nel cloud".
**Incluso**: smoke su **Cognito** (UC 0015), **Pre-Token-Gen + JWT** (UC 0016), **authorizer all'edge** (UC 0014) ed **email
SES** (UC 0018). **Escluso**: gli smoke di build/avvio artefatti già coperti da `tools/smoke` in CI, e la definizione dei
servizi in sé.

Note sulle sigle: **JWT** (JSON Web Token) è il gettone firmato che rappresenta l'utente autenticato; **SPA** (Single Page
Application) è l'applicazione web a pagina singola del frontend; **CORS** (Cross-Origin Resource Sharing) è la regola che
autorizza il browser a chiamare l'interfaccia da un dominio diverso; **DKIM** (DomainKeys Identified Mail) è la firma che
attesta il mittente della posta.

## 2. Attori & ruoli
- **Platform engineer / founder**: esegue le verifiche dal vivo e ne registra l'esito.
- **Sistema**: Cognito, il Pre-Token-Gen Lambda, l'authorizer, i servizi backend, SES.
- **Terzi**: **AWS** (Cognito, API Gateway, SES), **Paddle** (fornitore pagamenti, per la verifica del webhook).

## 3. Precondizioni
- Ambiente `test` acceso (UC 0082) e primo deploy live eseguito (UC 0080).
- Dominio app di test raggiungibile: `app.test.appgrove.app`.
- Dominio email verificato in SES con DKIM attivo (UC 0018); regione `eu-west-1`.

## 4. Flusso principale
1. **Cognito (UC 0015)**: eseguire dal vivo registrazione + verifica indirizzo, login, refresh con **rotazione del cookie**,
   logout. Verificare gli **attributi del cookie** (sicuro, solo-HTTP, dominio corretto) e il **CORS** dal dominio
   `app.test.appgrove.app`.
2. **Pre-Token-Gen + JWT (UC 0016)**: verificare che il ruolo di database `auth_lambdas` abbia i permessi (grant) attesi;
   **popolare `PLATFORM_ADMIN_SUBS`** con il `sub` (identificativo utente) reale del platform-admin; misurare l'avvio a freddo
   (cold-start) di Aurora contro il **limite di circa 5 secondi** imposto da Cognito; verificare che l'access token porti
   `tenant_id` e i ruoli, e che un utente **senza appartenenza** (membership) a un'organizzazione venga **negato**
   (comportamento fail-closed: in dubbio si nega).
3. **Authorizer all'edge (UC 0014)**: senza token → **401**; token malformato o scaduto → **401** (non 403: sul 401 poggia il
   refresh silenzioso della SPA); token valido → **passa**, con eventuali 402/403/429 restituiti dal servizio a valle; webhook
   Paddle raggiungibile **senza token** (il servizio risponde 401 su firma non valida); rotte pubbliche intatte; endpoint di
   salute (health) **non esposti** (il gateway risponde 404).
4. **Email SES (UC 0018)**: verificare dominio verificato + firma DKIM attiva; eseguire un **primo invio reale** in italiano e
   in inglese; verificare che l'email di invito parta dalla **rete privata** (le funzioni girano in VPC).
5. **Registrare gli esiti** di ogni verifica (una lista di controllo passa/non-passa), per avere una traccia della prima
   accensione.

## 5. Flussi alternativi / edge / errori
- **Cold-start Aurora oltre ~5s**: Cognito interrompe la generazione del token → mitigare (mantenere caldo, aumentare capacità
  minima, o accettare un primo colpo lento) e ri-verificare; traccia il compromesso in UC 0016.
- **401 vs 403 invertiti**: se l'authorizer restituisse 403 su token scaduto, il refresh silenzioso della SPA si romperebbe →
  correggere prima di procedere.
- **Health esposti (non 404)**: rischio di superficie non voluta → chiudere l'esposizione al gateway.
- **DKIM non ancora propagato**: la prima email può fallire l'autenticazione del mittente → attendere la propagazione DNS e
  ripetere (aggancio con UC 0078).
- **CORS negato dal dominio app**: login bloccato dal browser → allineare l'origine consentita a `app.test.appgrove.app`.

## 6. Risorse & runbook
**Risorse**: nessuna nuova; si verificano quelle già create (Cognito, authorizer, servizi, SES, database). L'unica scrittura
di configurazione è **popolare `PLATFORM_ADMIN_SUBS`** con il `sub` reale.
**Runbook passo-passo**:
1. Accendere `test` (UC 0082) e confermare il deploy live (UC 0080).
2. Eseguire la lista Cognito (signup/verifica/login/refresh/logout, cookie, CORS).
3. Verificare grant `auth_lambdas`, popolare `PLATFORM_ADMIN_SUBS`, misurare cold-start, controllare i claim del token e il
   diniego fail-closed.
4. Eseguire la lista authorizer (401/402/403/404/429, webhook, rotte pubbliche, health).
5. Verificare SES (dominio+DKIM), inviare le email reali IT/EN, controllare l'invito dalla rete privata.
6. Registrare gli esiti.
**Rollback**: non applicabile (sono verifiche di sola lettura, salvo la scrittura di `PLATFORM_ADMIN_SUBS`, reversibile).

## 7. Dati toccati
Durante le verifiche si crea almeno un utente di prova reale (indirizzo email, credenziali) su Cognito e nel database di
`test`: sono **dati personali** di prova, da rimuovere a valle o da tenere confinati all'ambiente `test`. Si scrive
`PLATFORM_ADMIN_SUBS` (identificativo tecnico dell'amministratore, non un contenuto personale nuovo). Le email reali inviate
sono transazionali (già inquadrate in UC 0018).

## 8. Permessi & gate
- Le verifiche si eseguono con un utente di prova e con l'accesso all'ambiente `test`; le invarianti multi-tenancy sono
  **oggetto** di verifica: `tenant_id` proveniente **solo** dal JWT verificato, diniego fail-closed per chi non ha
  appartenenza.
- **Gate di ambiente**: si esegue **solo su `test`**, prima di qualunque apertura di `prod`. È la porta che dà fiducia al
  passaggio successivo.

## 9. Requisiti di test / verifica
Non esistono test unitari per questi comportamenti (richiedono i servizi AWS reali). La verifica **è** lo smoke dal vivo, con
esito atteso esplicito per ciascun punto (i codici 401/402/403/404/429, la rotazione cookie, i claim del token, il diniego
fail-closed, la consegna email IT/EN). La lista di controllo deve risultare **tutta verde** prima di considerare `test`
affidabile.

## 10. Riferimenti & Definition of Done
- **Fonte**: R15 (Tabella residui _INDEX.md); docs/_BACKLOG.md §"Attivazione ambienti cloud".
- **DoD**: le quattro liste (Cognito, Pre-Token-Gen+JWT, authorizer, email SES) eseguite dal vivo su `test` con tutti gli esiti
  attesi; `PLATFORM_ADMIN_SUBS` popolato; cold-start entro il limite o compromesso tracciato; esiti registrati.

## Punti aperti / decisioni differite
- **Automazione futura degli smoke cloud**: oggi sono manuali alla prima accensione; se le accensioni di `test` diventassero
  frequenti (per lo scale-to-0 di UC 0082), valutare uno script che li ripeta — traccia da tenere qui, non è del task odierno.
- **Pulizia dell'utente di prova**: definire se rimuoverlo o mantenerlo come utente di verifica ricorrente in `test`.
