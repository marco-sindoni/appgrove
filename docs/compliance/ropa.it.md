# Registro dei trattamenti (RoPA) — appgrove

> Documento **INTERNO** (art. 30.4 GDPR): si fornisce solo al Garante su richiesta, non è la privacy policy pubblica (#13 C17). **File GENERATO** dai manifesti dati (`docs/compliance/manifests/*.yaml`) con `tools/compliance` — **non modificare a mano**: aggiorna il manifesto e rigenera (`npm run assemble`). Bozza sotto disclaimer: validazione finale del legale (docs/_REVISIONE-LEGALE.md).

## Piattaforma appgrove (core)

Trattamenti trasversali della piattaforma (account, utenti, inviti, autenticazione, log). appgrove agisce come titolare per i consumatori B2C e come responsabile per i tenant B2B titolari (#13 C13). Paddle (Merchant of Record) è titolare autonomo per i dati di pagamento.

### Trattamenti

| Voce | Categoria di dati | Ubicazione | Interessati | Finalità | Base giuridica | Retention |
|---|---|---|---|---|---|---|
| `users.email` | Contatto (indirizzo email) | Tabella `platform.users` (Aurora PostgreSQL, eu-west-1) | Utenti registrati (membri di un account/tenant) | Erogazione e gestione dell'account (login, comunicazioni di servizio) | Contratto (art. 6.1.b GDPR) | Finché l'account è attivo + 14 giorni di grace dopo l'eliminazione (#13 E25) |
| `users.display_name` | Identità (nome visualizzato) | Tabella `platform.users` (Aurora PostgreSQL, eu-west-1) | Utenti registrati | Identificazione dell'utente nell'interfaccia e nelle collaborazioni del tenant | Contratto (art. 6.1.b GDPR) | Finché l'account è attivo + 14 giorni di grace dopo l'eliminazione (#13 E25) |
| `users.cognito_sub` | Identificativo online (subject Cognito, pseudo-identificatore) | Tabella `platform.users` (Aurora PostgreSQL, eu-west-1) | Utenti registrati | Collegamento tra identità di autenticazione (Cognito) e profilo applicativo | Contratto (art. 6.1.b GDPR) | Finché l'account è attivo + 14 giorni di grace dopo l'eliminazione (#13 E25) |
| `invitations.email` | Contatto (indirizzo email dell'invitato) | Tabella `platform.invitations` (Aurora PostgreSQL, eu-west-1) | Persone invitate a entrare in un tenant (non ancora utenti) | Recapito e gestione dell'invito (single-use, con scadenza; il token è salvato solo come hash) | Misure precontrattuali / contratto (art. 6.1.b GDPR) | Fino a scadenza/accettazione dell'invito; soft-delete e purge secondo il ciclo piattaforma |
| `accounts.name` | Identità/anagrafica account (classificazione prudente — può essere ragione sociale) | Tabella `platform.accounts` (Aurora PostgreSQL, eu-west-1) | Titolari dell'account (per i B2C individuali è tipicamente il nome della persona) | Identificazione dell'account/tenant nella piattaforma e nelle comunicazioni | Contratto (art. 6.1.b GDPR) | Finché l'account è attivo + 14 giorni di grace dopo l'eliminazione (#13 E25) |
| `accounts.paddle_customer_id` | Identificativo online (customer id presso Paddle, pseudo-identificatore) | Tabella `platform.accounts` (Aurora PostgreSQL, eu-west-1) | Titolari dell'account con abbonamento | Riconciliazione abbonamenti/pagamenti con Paddle (Merchant of Record, titolare autonomo — #13 H) | Contratto (art. 6.1.b GDPR) | Finché l'account è attivo + 14 giorni di grace; gli obblighi fiscali sui pagamenti restano in capo a Paddle |
| `cognito.credentials` | Credenziali di autenticazione (hash password) e segreti MFA/TOTP | User pool Amazon Cognito (eu-west-1); in locale solo stub dev (`auth-local`, fuori RoPA) | Utenti registrati | Autenticazione degli utenti e protezione dell'accesso (MFA) | Contratto (art. 6.1.b GDPR) | Finché l'account è attivo; eliminazione con la cancellazione dell'utenza Cognito |
| `logs.structured` | Identificativi tecnici nei log strutturati (`tenant_id`, `app_id`, `user_id`, IP) | CloudWatch Logs (eu-west-1/eu-central-1); audit su S3/Glacier (#08 I) | Utenti della piattaforma | Sicurezza, stabilità e diagnostica (limitazione di finalità — niente profilazione, #13 B12) | Legittimo interesse (art. 6.1.f GDPR — sicurezza e continuità del servizio) | Log applicativi prod 30 giorni; audit/sicurezza 12 mesi su S3→Glacier (#08 I26) |

### Destinatari e sub-responsabili

Sub-responsabili: **AWS** (hosting, regioni UE — DPA con SCC + certificazione DPF) e **Plausible Analytics** (analytics senza cookie, hosting UE). **Paddle** (Merchant of Record) è **titolare autonomo** per i dati di pagamento, non sub-responsabile (#13 H45-47). Lista pubblica: `content/subprocessors.md` (UC 0002).

### Trasferimenti extra-UE

Dati a riposo solo in regioni UE (eu-west-1; monitoring eu-central-1) — #13 I51. AWS Inc. (USA, CLOUD Act): garanzie DPF + SCC nel DPA, più cifratura at-rest/in-transit (#13 I52).

### Misure di sicurezza

Cifratura at-rest e in-transit; isolamento per-tenant row-level (`tenant_id` solo dal JWT verificato); least-privilege IAM; token di invito persistiti solo come hash; soft-delete con purge programmata (grace 14 giorni); logging strutturato e audit trail (#02/#05/#06/#08).

## App Fatture (fatturazione B2C single-user)

Dati dei clienti finali inseriti dal tenant nelle proprie fatture (schema `app_fatture`). Il tenant è titolare del trattamento; appgrove agisce come responsabile (#13 C13).

### Trattamenti

| Voce | Categoria di dati | Ubicazione | Interessati | Finalità | Base giuridica | Retention |
|---|---|---|---|---|---|---|
| `invoice.customer_name` | Identità cliente (nome/denominazione) | Tabella `app_fatture.invoice` (Aurora PostgreSQL, eu-west-1) | Clienti (destinatari delle fatture) del tenant | Emissione e gestione delle fatture | Contratto (art. 6.1.b GDPR) e obblighi legali del titolare (art. 6.1.c) | 10 anni dall'emissione (obblighi fiscali) |
| `invoice.customer_email` | Contatto cliente (indirizzo email, facoltativo) | Tabella `app_fatture.invoice` (Aurora PostgreSQL, eu-west-1) | Clienti (destinatari delle fatture) del tenant | Recapito e invio della fattura | Contratto (art. 6.1.b GDPR) | 10 anni dall'emissione (obblighi fiscali) |

