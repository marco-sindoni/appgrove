# Registro dei trattamenti (RoPA) — appgrove

> Documento **INTERNO** (art. 30.4 GDPR): si fornisce solo al Garante su richiesta, non è la privacy policy pubblica (#13 C17). **File GENERATO** dai manifesti dati (`docs/compliance/manifests/*.yaml`) con `tools/compliance` — **non modificare a mano**: aggiorna il manifesto e rigenera (`npm run assemble`). Bozza sotto disclaimer: validazione finale del legale (docs/_REVISIONE-LEGALE.md).

## Piattaforma appgrove (core)

Trattamenti trasversali della piattaforma (account, utenti, inviti, autenticazione, log). appgrove agisce come titolare per i consumatori B2C e come responsabile per i tenant B2B titolari (#13 C13). Paddle (Merchant of Record) è titolare autonomo per i dati di pagamento.

### Trattamenti

| Voce | Categoria di dati | Ubicazione | Interessati | Finalità | Base giuridica | Retention |
|---|---|---|---|---|---|---|
| `users.email` | Contatto (indirizzo email) | Tabella `platform.users` (Aurora PostgreSQL, eu-west-1) | Utenti registrati (membri di un account/tenant) | Erogazione e gestione dell'account (login, comunicazioni di servizio) | Contratto (art. 6.1.b GDPR) | Finché l'account è attivo + 14 giorni di grace dopo l'eliminazione (#13 E25) |
| `users.display_name` | Identità (nome visualizzato) | Tabella `platform.users` (Aurora PostgreSQL, eu-west-1) | Utenti registrati | Identificazione dell'utente nell'interfaccia e nelle collaborazioni del tenant | Contratto (art. 6.1.b GDPR) | Finché l'account è attivo + 14 giorni di grace dopo l'eliminazione (#13 E25) |
| `users.locale` | Preferenza dell'utente (lingua) | Tabella `platform.users` (Aurora PostgreSQL, eu-west-1) | Utenti registrati | Scelta della lingua (inglese/italiano) delle email transazionali di autenticazione (verifica indirizzo, reimpostazione password, invito) — UC 0018 | Contratto (art. 6.1.b GDPR) — email transazionali, distinte dalla newsletter (consenso separato, | Finché l'account è attivo + 14 giorni di grace dopo l'eliminazione (#13 E25) |
| `users.cognito_sub` | Identificativo online (subject Cognito, pseudo-identificatore) | Tabella `platform.users` (Aurora PostgreSQL, eu-west-1) | Utenti registrati | Collegamento tra identità di autenticazione (Cognito) e profilo applicativo | Contratto (art. 6.1.b GDPR) | Finché l'account è attivo + 14 giorni di grace dopo l'eliminazione (#13 E25) |
| `invitations.email` | Contatto (indirizzo email dell'invitato) | Tabella `platform.invitations` (Aurora PostgreSQL, eu-west-1) | Persone invitate a entrare in un tenant (non ancora utenti) | Recapito e gestione dell'invito (single-use, con scadenza; il token è salvato solo come hash) | Misure precontrattuali / contratto (art. 6.1.b GDPR) | Fino a scadenza/accettazione dell'invito; soft-delete e purge secondo il ciclo piattaforma |
| `newsletter_subscribers.email` | Contatto (indirizzo email dell'iscritto) | Tabella `platform.newsletter_subscriber` (Aurora PostgreSQL, eu-west-1) | Iscritti alla newsletter (visitatori del sito e utenti registrati) | Invio della newsletter (marketing diretto), con doppia conferma (double opt-in) e prova del consenso nel registro consensi (art. 7); Plausible resta analytics aggregato non identificativo | Consenso (art. 6.1.a GDPR) — separato e distinto dalle email transazionali (#13 F29) | Finché iscritto + 24 mesi dopo la disiscrizione, poi eliminazione automatica (#13 E) |
| `accounts.name` | Identità/anagrafica account (classificazione prudente — può essere ragione sociale) | Tabella `platform.accounts` (Aurora PostgreSQL, eu-west-1) | Titolari dell'account (per i B2C individuali è tipicamente il nome della persona) | Identificazione dell'account/tenant nella piattaforma e nelle comunicazioni | Contratto (art. 6.1.b GDPR) | Finché l'account è attivo + 14 giorni di grace dopo l'eliminazione (#13 E25) |
| `accounts.paddle_customer_id` | Identificativo online (customer id presso Paddle, pseudo-identificatore) | Tabella `platform.accounts` (Aurora PostgreSQL, eu-west-1) | Titolari dell'account con abbonamento | Riconciliazione abbonamenti/pagamenti con Paddle (Merchant of Record, titolare autonomo — #13 H) | Contratto (art. 6.1.b GDPR) | Finché l'account è attivo + 14 giorni di grace; gli obblighi fiscali sui pagamenti restano in capo a Paddle |
| `cognito.credentials` | Credenziali di autenticazione (hash password) e segreti MFA/TOTP | User pool Amazon Cognito (eu-west-1); in locale solo provider dev (`services/auth`, fuori RoPA) | Utenti registrati | Autenticazione degli utenti e protezione dell'accesso (MFA) | Contratto (art. 6.1.b GDPR) | Finché l'account è attivo; eliminazione con la cancellazione dell'utenza Cognito |
| `support_ticket.subject` | Contenuto libero (oggetto della richiesta di supporto/privacy) | Tabella `platform.support_ticket` (Aurora PostgreSQL, eu-west-1) | Utenti che aprono richieste di supporto o di esercizio dei diritti | Gestione delle richieste di supporto e di esercizio dei diritti (ticketing in-house, #13 D21) | Contratto (art. 6.1.b GDPR); per i ticket privacy anche obbligo legale (art. 12 GDPR) | 24 mesi dalla chiusura del ticket, poi eliminazione automatica (#13 E) |
| `support_ticket_message.body` | Contenuto libero (testo dei messaggi del thread di supporto) | Tabella `platform.support_ticket_message` (Aurora PostgreSQL, eu-west-1) | Utenti che scrivono nel thread di un ticket di supporto | Gestione delle richieste di supporto e di esercizio dei diritti (ticketing in-house, #13 D21) | Contratto (art. 6.1.b GDPR); per i ticket privacy anche obbligo legale (art. 12 GDPR) | 24 mesi dalla chiusura del ticket, poi eliminazione automatica (#13 E) |
| `legal_acceptances.user_id` | Identificativo online (subject Cognito) collegato alla prova di accettazione | Tabella `platform.legal_acceptance` (Aurora PostgreSQL, eu-west-1) | Utenti registrati che accettano o prendono atto dei documenti legali | Prova dell'accettazione/presa d'atto dei documenti legali (rendicontazione, art. 5.2 GDPR; UC 0056) | Contratto (art. 6.1.b GDPR) e obbligo di rendicontazione (art. 5.2 GDPR) | Per la vita dell'account e il periodo di prescrizione applicabile (prova del consenso contrattuale, #13 E) — da confermare in revisione legale |
| `logs.structured` | Identificativi tecnici nei log strutturati (`tenant_id`, `app_id`, `user_id`, IP) | CloudWatch Logs (eu-west-1/eu-central-1); audit su S3/Glacier (#08 I) | Utenti della piattaforma | Sicurezza, stabilità e diagnostica (limitazione di finalità — niente profilazione, #13 B12) | Legittimo interesse (art. 6.1.f GDPR — sicurezza e continuità del servizio) | Log applicativi prod 30 giorni; audit/sicurezza 12 mesi su S3→Glacier (#08 I26) |
| `logs.frontend_errors` | Eventi di SOLO errore JavaScript (#08 H23): messaggio/stack, rotta, versione build, identificativi opachi `user_id`/`tenant_id` se esiste una sessione. Niente IP, niente user agent, nessun tracking comportamentale. | CloudWatch Logs (eu-west-1), log group dell'ingest errori (`/aws/lambda/appgrove-<env>-error-ingest`) | Utenti delle SPA (anche non autenticati) | Diagnostica e stabilità del frontend (limitazione di finalità — solo errori, #13 B12) | Legittimo interesse (art. 6.1.f GDPR — qualità e continuità del servizio) | Come i log applicativi (test 7 giorni, prod 30 giorni, |

### Destinatari e sub-responsabili

Sub-responsabili: **AWS** (hosting, regioni UE — DPA con SCC + certificazione DPF) e **Plausible Analytics** (analytics senza cookie, hosting UE). **Paddle** (Merchant of Record) è **titolare autonomo** per i dati di pagamento, non sub-responsabile (#13 H45-47). Lista pubblica: `content/legal/subprocessors.<lang>.md` (UC 0002).

### Trasferimenti extra-UE

Dati a riposo solo in regioni UE (eu-west-1; monitoring eu-central-1) — #13 I51. AWS Inc. (USA, CLOUD Act): garanzie DPF + SCC nel DPA, più cifratura at-rest/in-transit (#13 I52).

### Misure di sicurezza

Cifratura at-rest e in-transit; isolamento per-tenant row-level (`tenant_id` solo dal JWT verificato); least-privilege IAM; token di invito persistiti solo come hash; soft-delete con purge programmata (grace 14 giorni); logging strutturato e audit trail (#02/#05/#06/#08).

## App Mini-CRM (gestione contatti B2B multi-utente)

Dati dei contatti (persone delle organizzazioni clienti) inseriti dal tenant nel proprio CRM (schema `app_crm`). Il tenant è titolare del trattamento; appgrove agisce come responsabile (#13 A2/C13). La tabella `seat` (posti) contiene il solo identificativo interno dei membri del tenant abilitati all'app — trattato da core come titolare — e non è quindi dato di terzi.

### Trattamenti

| Voce | Categoria di dati | Ubicazione | Interessati | Finalità | Base giuridica | Retention |
|---|---|---|---|---|---|---|
| `contact.display_name` | Identità del contatto (nome/denominazione) | Tabella `app_crm.contact` (Aurora PostgreSQL, eu-west-1) | Contatti (persone delle organizzazioni clienti) inseriti dal tenant | Gestione della relazione commerciale con i contatti del tenant | Contratto (art. 6.1.b GDPR) | Fino a cancellazione da parte del tenant o chiusura dell'account |
| `contact.email` | Recapito del contatto (indirizzo email, facoltativo) | Tabella `app_crm.contact` (Aurora PostgreSQL, eu-west-1) | Contatti (persone delle organizzazioni clienti) inseriti dal tenant | Gestione della relazione commerciale con i contatti del tenant | Contratto (art. 6.1.b GDPR) | Fino a cancellazione da parte del tenant o chiusura dell'account |
| `contact.phone` | Recapito del contatto (numero di telefono, facoltativo) | Tabella `app_crm.contact` (Aurora PostgreSQL, eu-west-1) | Contatti (persone delle organizzazioni clienti) inseriti dal tenant | Gestione della relazione commerciale con i contatti del tenant | Contratto (art. 6.1.b GDPR) | Fino a cancellazione da parte del tenant o chiusura dell'account |
| `contact.notes` | Annotazioni a testo libero sul contatto. Campo non strutturato: può contenere informazioni che nessuno ha classificato, comprese — per iniziativa del tenant — categorie particolari (art. 9). L'informativa del tenant titolare deve coprirlo; appgrove non le sollecita. | Tabella `app_crm.contact` (Aurora PostgreSQL, eu-west-1) | Contatti inseriti dal tenant, ed eventuali terzi citati nelle annotazioni | Gestione della relazione commerciale con i contatti del tenant | Contratto (art. 6.1.b GDPR) | Fino a cancellazione da parte del tenant o chiusura dell'account |
| `interaction.note` | Contenuto a testo libero di un'interazione (telefonata, email, incontro, nota). Come le annotazioni del contatto, è un campo non strutturato e un possibile punto d'ingresso non presidiato per dati che nessuno ha classificato. | Tabella `app_crm.interaction` (Aurora PostgreSQL, eu-west-1) | Contatti inseriti dal tenant, ed eventuali terzi citati nelle interazioni | Storico della relazione commerciale con i contatti del tenant | Contratto (art. 6.1.b GDPR) | Fino a cancellazione da parte del tenant o chiusura dell'account |

## App Fatture (fatturazione B2C single-user)

Dati dei clienti finali inseriti dal tenant nelle proprie fatture (schema `app_fatture`). Il tenant è titolare del trattamento; appgrove agisce come responsabile (#13 C13).

### Trattamenti

| Voce | Categoria di dati | Ubicazione | Interessati | Finalità | Base giuridica | Retention |
|---|---|---|---|---|---|---|
| `invoice.customer_name` | Identità cliente (nome/denominazione) | Tabella `app_fatture.invoice` (Aurora PostgreSQL, eu-west-1) | Clienti (destinatari delle fatture) del tenant | Emissione e gestione delle fatture | Contratto (art. 6.1.b GDPR) e obblighi legali del titolare (art. 6.1.c) | 10 anni dall'emissione (obblighi fiscali) |
| `invoice.customer_email` | Contatto cliente (indirizzo email, facoltativo) | Tabella `app_fatture.invoice` (Aurora PostgreSQL, eu-west-1) | Clienti (destinatari delle fatture) del tenant | Recapito e invio della fattura | Contratto (art. 6.1.b GDPR) | 10 anni dall'emissione (obblighi fiscali) |

