# Implementation Log — Change 0040: Localizzazione email auth (EN/IT) + invio via SES

**Branch**: `change/0040-use-case-0018-localizzazione-email-auth`
**Aree**: `services/auth`, `services/core`, `frontend`, `infra`, `shared/email-templates` (nuova), documentazione
**Completata**: 2026-07-19

## File modificati

| File | Azione |
|---|---|
| `shared/email-templates/{README.md,layout.html,layout.txt,en.json,it.json}` | Creati — sorgente unica dei testi |
| `services/core/src/main/resources/db/migration/V8__user_locale.sql` | Creato — colonna `locale` |
| `services/core/.../platform/User.java` | Modificato — campo `locale` + `@PersonalData` |
| `services/core/.../gdpr/PlatformDataContract.java` | Modificato — `locale` nell'esportazione |
| `services/auth/.../EmailTemplates.java`, `Locales.java` | Creati — resa template e ripiego lingua |
| `services/auth/.../EmailService.java`, `MailSender.java`, `local/MailerMailSender.java`, `cognito/SesMailSender.java` | Modificati — doppia versione grafica/testuale |
| `services/auth/.../AuthDtos.java`, `AuthResource.java`, `IdentityProvider.java` | Modificati — due forme del collegamento, lingua nei flussi |
| `services/auth/.../PlatformWriter.java`, `AuthUser.java`, `local/UserDirectory.java` | Modificati — lettura/scrittura della lingua |
| `services/auth/.../local/LocalIdentityProvider.java`, `cognito/CognitoIdentityProvider.java` | Modificati — lingua e `ClientMetadata` |
| `services/auth/pom.xml`, `application.properties` | Modificati — copia template + inclusione risorse nativa |
| `services/auth/src/test/.../{EmailTemplatesTest,EmailTemplatesParityTest,EmailLocalizationTest}.java` | Creati |
| `services/auth/src/test/.../cognito/CognitoEmailLinkTest.java` | Creato |
| `services/auth/src/test/.../TestSchema.java` | Modificato — applica tutte le migrazioni, non un elenco cablato |
| `infra/global/ses.tf` | Creato — identità dominio + DKIM + DMARC |
| `infra/modules/platform_shared/custom_message.tf` | Creato — Custom Message Lambda |
| `infra/modules/platform_shared/lambda/custom_message/{handler.py,test_handler.py}` | Creati |
| `infra/modules/platform_shared/{auth.tf,data.tf,tests/plan.tftest.hcl}` | Modificati — trigger, SES, verifiche |
| `infra/modules/env_baseline/endpoints.tf` | Modificato — accesso di rete verso SES |
| `infra/global/outputs.tf`, `run-tests.sh` | Modificati |
| `frontend/apps/backoffice/src/auth/authApi.ts` + 6 pagine/test | Modificati — lingua e due forme del collegamento |
| `docs/compliance/manifests/platform.yaml`, `ropa.{it,en}.md` | Modificati — voce `users.locale` |
| `docs/usecases/05-auth/0018-*.md`, `06-frontend/0020-*.md`, `docs/_BACKLOG.md`, `docs/_COSTI-AWS.md`, `docs/_EVOLUZIONI-DEVOPS.md` | Modificati — punti aperti, costi, evoluzioni |

## Cosa è stato fatto

I testi delle email di autenticazione sono ora una **sorgente unica** (`shared/email-templates`) resa da due
programmi: il servizio Java (tutte le email in locale, l'invito anche in cloud) e un **Custom Message Lambda** in
Python (verifica e reimpostazione password in cloud, dentro Cognito). La lingua è un attributo dell'utente
(`platform.users.locale`), valorizzato alla registrazione dalla lingua dell'interfaccia; verso la Lambda viaggia come
parametro della chiamata Cognito, così la Lambda non deve leggere il database. In Terraform: identità di dominio SES
con firma DKIM, Cognito configurato per spedire da `noreply@appgrove.app`, accesso di rete verso SES. Nulla è stato
applicato — il cloud resta spento.

## Decisioni prese

1. **Lingua sul nostro database, non su Cognito** — unica fonte di verità in `platform.users.locale`. La Lambda la
   riceve come parametro della chiamata (`ClientMetadata`), che Cognito inoltra tale e quale ai trigger. Alternativa
   scartata: replicare la lingua sull'attributo Cognito (duplicazione) o farla leggere dal database alla Lambda
   (rete privata + connessione a Postgres + avvio a freddo su ogni email, per scegliere fra due valori).

2. **Collegamento a due parametri — correzione di un'assunzione errata dello use case.** UC 0018 dava per scontato
   che la Lambda potesse generare `?token=<base64url(email|codice)>`. **Non è realizzabile**: quando Cognito invoca
   la Lambda il codice non esiste ancora (arriva il segnaposto `{####}`, sostituito *dopo*), quindi il segnaposto
   finirebbe dentro la codifica e il collegamento arriverebbe rotto. Adottato `?email=…&code={####}`, con la
   ricomposizione del token **nel backend** (`IdentityProvider.emailActionToken`): il formato resta un dettaglio
   interno del provider e il contratto a token unico continua a servire il provider locale, senza rotture.

3. **Nessuna scorciatoia sull'accesso di rete a SES.** Verificato che spedire da una funzione fuori dalla rete o
   passare da una coda richiede comunque un endpoint dello stesso tipo e costo (la rete non ha uscita a internet).
   L'unica via per rimuoverlo è far generare anche l'invito a Cognito, che significa riprogettare il flusso inviti
   (oggi token, ruolo, tenant e scadenza sono nostri): tracciata come evoluzione **E24**, non fatta.

4. **`TestSchema` applica tutte le migrazioni del core** invece di un elenco cablato (era V1+V2). Senza, ogni
   migrazione futura che tocchi `platform.users` farebbe fallire i test auth con "colonna inesistente".

## ⚠️ Scelta discutibile: degrado silenzioso in caso di errore di resa

In `custom_message/handler.py`, se la resa del template fallisce **non solleviamo**: si registra un errore nei log e
si restituisce l'evento inalterato, il che fa spedire a Cognito il suo **messaggio di default** (inglese, non
brandizzato).

**Perché così**: il trigger è **sincrono e sul percorso della registrazione**. Sollevare non farebbe fallire solo
l'email: farebbe fallire la registrazione dell'utente. Fra "l'utente si registra e riceve un'email brutta" e
"l'utente non riesce a registrarsi", la prima è meno grave.

**Cosa ci costa**: è un **peggioramento silenzioso dal punto di vista dell'utente**. Un difetto nei template — per
esempio un segnaposto rinominato in una lingua sola — non romperebbe nulla di visibile: le email continuerebbero a
partire, in inglese, e ce ne accorgeremmo solo leggendo i log o ricevendo una segnalazione. È il tipo di guasto che
può durare settimane.

**Cosa lo mitiga** (ed è il motivo per cui la scelta regge):
- il test di **parità fra lingue** (`EmailTemplatesParityTest`) intercetta a monte proprio la classe di difetti che
  causerebbe questo degrado — chiavi o segnaposto divergenti fra `en.json` e `it.json`;
- l'errore è registrato con evento dedicato (`custom_message_render_failed`), quindi è **allarmabile**: agganciarlo
  agli allarmi di UC 0006 renderebbe il degrado rumoroso invece che silenzioso. Non fatto in questa change perché
  l'osservabilità cloud non è ancora accesa.

**Se si preferisce l'opposto**, il cambiamento è di tre righe (togliere il `try/except` attorno a `render`), ma va
fatto con consapevolezza: significa accettare che un difetto nei template blocchi le registrazioni.

## Invarianti appgrove

- **Tenant ID solo dal JWT** — non toccato: le email non veicolano identità di tenant, e la lingua è un attributo
  dell'utente, non un parametro di richiesta usato per autorizzare. Gli endpoint estesi (`email`+`code`) restano
  pre-autenticazione e non concedono nulla senza il codice generato da Cognito.
- **Filtro row-level** — invariato; la lettura della lingua avviene per identità dell'utente (`PlatformWriter.localeOf`).
- **Modulo Terraform** — la Lambda entra in `platform_shared` accanto al Pre-Token-Gen, riusandone lo schema.
- **Logging strutturato** — la Lambda registra lingua, evento e tipo di messaggio, e **mai** l'indirizzo o il codice
  (presidiato da un test dedicato).

## Note per il revisore

**Contratti cross-area toccati** (tutti additivi, nessuna rottura):
- frontend ↔ auth: `signup`/`invitations/send`/`invitations/accept` accettano `locale`; `verify` e `password/reset`
  accettano `email`+`code` oltre a `token`;
- auth ↔ infra: la lingua viaggia in `ClientMetadata` verso il Custom Message Lambda;
- auth ↔ core: nuova colonna `locale` (migrazione `V8`).

**Cosa resta da rileggere con occhio umano**: il **copy** in `shared/email-templates/{en,it}.json` e la grafica in
`layout.html` sono scritti in questa change e non hanno avuto revisione redazionale. Lo use case lo prevedeva
("resta la stesura dei testi").

**Decisioni differite tracciate** (regola CLAUDE.md):
- **UC 0020** — lingua persistente dal selettore in topbar ed esposta nelle impostazioni utente (possiede quella UI).
- **UC 0018 §Punti aperti** — gestione rimbalzi/reclami SES; irrigidimento della politica DMARC (oggi in sola
  osservazione, nessun indirizzo per i rapporti perché punterebbe a una casella inesistente).
- **`docs/_BACKLOG.md`** — nuova sezione "Recapito delle email transazionali (SES)" con i due lavori che il codice
  non può fare: **uscita dalla modalità di prova di SES** (richiesta manuale ad AWS, giorni di attesa, bloccante per
  il go-live) e **gestione di rimbalzi e reclami** (SES sospende l'account oltre soglia, bloccando registrazioni e
  reimpostazioni per tutti). Più la voce 11 della checklist di prima accensione.
- **`docs/_COSTI-AWS.md`** — accessi di rete da ~$22 a ~$29/ambiente, totale da ~$100–120 a ~$115–135/mese: il
  budget di $100 (#08) è ora **superato dalla stima** e va rivisto all'attivazione.
- **`docs/_EVOLUZIONI-DEVOPS.md`** — evoluzione **E24**: come togliere l'endpoint SES e a che prezzo.

**Gate privacy/RoPA**: eseguito, 5 segnali, tutti classificati. Classificazione **MINORE**, componente nucleo di
piattaforma (motivazione completa in `requirements.md`). **Nessun nuovo sub-processor**: lo scanner segnala
`maven-resources-plugin`, che è un componente di compilazione, non un servizio esterno.

## Test

| Area | Comando | Esito |
|---|---|---|
| backend | `mvn test` (via `run-tests.sh`) | ✅ |
| frontend | `npm test` + Playwright e2e L2 | ✅ 88 test |
| infra | `infra/scripts/check` + test Python delle Lambda | ✅ |
| compliance | parità manifesti + freschezza RoPA | ✅ |
| smoke | avvio reale degli artefatti + login end-to-end | ✅ |

`./run-tests.sh` completo: **tutte e cinque le aree verdi**. Lo smoke ha applicato la migrazione `V8 - user locale`
a un Postgres reale ed eseguito un login vero — la colonna non è stata provata solo in memoria.

**Test aggiunti** (in ordine di ciò che difendono):

- `EmailTemplatesParityTest` — `en.json` e `it.json` devono avere **le stesse chiavi e gli stessi segnaposto**. È la
  rete che impedisce la divergenza fra lingue: senza, si corregge una frase da una parte e l'email sbagliata parte
  dall'altra, per giunta da un programma diverso. Verifica anche che i file siano davvero nell'artefatto.
- `custom_message/test_handler.py` (14 test) — scelta della lingua e ripiego; e soprattutto che il segnaposto
  `{####}` arrivi nel messaggio **non codificato** e che la `&` fra i parametri sia sottoposta a escape: sono le due
  cose che, sbagliate, producono un collegamento che non verifica nulla.
- `CognitoEmailLinkTest` — gli endpoint accettano `email`+`code`, e la lingua viaggia in `ClientMetadata`. Presidia
  regressioni che **non si vedrebbero in locale**: in locale le email le manda il servizio Java.
- `EmailTemplatesTest`, `EmailLocalizationTest` — resa, ripiego, escape, e il percorso completo
  registrazione → memorizzazione lingua → email successive nella lingua giusta.
- `plan.tftest.hcl` (`email_auth_via_ses_e_custom_message`) — trigger cablato, invio via SES e non dal mittente di
  default (che ha un tetto di ~50 email al giorno), Lambda **fuori** dalla rete privata, soli permessi di log.
- Frontend — la registrazione trasmette la lingua; la pagina di verifica accetta il collegamento a due parametri.

**Nessuna baseline visiva ri-registrata**: la change non tocca l'aspetto della SPA.

## Stato criteri di accettazione

- [x] Utente con `locale = it` riceve le email in italiano; `en`/assente/non riconosciuta → inglese.
- [x] I testi provengono da un solo punto: modificare `it.json` cambia sia la resa Java sia quella Python.
- [x] Il collegamento porta `{####}` non codificato; il backend accetta sia `token` sia `email`+`code`.
- [x] I test esistenti che estraggono il collegamento dal corpo testuale continuano a passare.
- [x] Suite Terraform verdi con Custom Message, identità SES, DKIM e accesso di rete definiti; nessun `apply`.
- [x] In locale l'email arriva su Mailpit nella lingua giusta — verificato dallo smoke (avvio reale + login);
      la conferma visiva del contenuto su Mailpit resta alla verifica manuale dello sviluppatore.
