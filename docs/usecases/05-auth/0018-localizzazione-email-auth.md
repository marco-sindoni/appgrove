# UC 0018 — Localizzazione email auth (Custom Message Lambda EN/IT)

**Area**: 05-auth · **Fase**: 2 · **Stato**: 🟢 deciso (resta la stesura dei testi)
**Dipendenze**: UC [0015](0015-cognito-auth-bff.md) (Cognito/SES)
**Fonte decisioni**: #02 §6 (email/template), #06 §26 (SES/DKIM/Custom Message Lambda), #14 (i18n tono)
**Ultimo aggiornamento**: 2026-07-19
**Aree collegate**: [02-auth-sicurezza](../../02-auth-sicurezza.md), [06-infra-iac](../../06-infra-iac.md), [0017-flussi-auth](0017-flussi-auth.md)

## 1. Obiettivo / Scope
Localizzare le email transazionali di auth (verifica/reset/invito) in **EN/IT**, in base al `locale` utente, tramite il
**Custom Message Lambda trigger** di Cognito e **SES**.
**Incluso**: template propri EN/IT (verifica email, reset password, invito); Custom Message Lambda che seleziona la lingua;
invio da **`noreply@appgrove.app`** (dominio verificato + DKIM su Route53); coerenza tono/brand (#14 F1).
**Escluso**: i flussi UI (UC 0017); le altre email non-auth (es. reminder/export — capability core, UC 0032/0033); il sito (UC 0036+).

## 2. Attori & ruoli
- **Cognito** invoca il Custom Message Lambda alla generazione del messaggio.
- **Custom Message Lambda**: sceglie lingua + template, compone il testo.
- **SES**: consegna l'email da `noreply@appgrove.app`.

## 3. Precondizioni
- Cognito + auth BFF (UC 0015); SES con dominio `appgrove.app` verificato + DKIM (infra UC 0003); `locale` utente disponibile.

## 4. Flusso principale
1. Un evento auth (signup/reset/invito) fa generare a Cognito un messaggio → invoca il **Custom Message Lambda** (#02 6, #06 26).
2. La Lambda determina la **lingua** dal `locale` utente (fallback EN) e seleziona il **template** EN/IT corrispondente.
3. Compone il testo (codice/link, tono brand #14 F1) e lo restituisce a Cognito → invio via **SES** da `noreply@appgrove.app`.

## 5. Flussi alternativi / edge / errori
- **Locale assente/non supportato** → fallback **EN** (default).
- **Invito (B2B)**: il link funge da verifica email (coerente UC 0017 UC7/decisione #3).
- **Resend**: cooldown lato flusso (UC 0017 UC2); il testo resta localizzato.
- **Solo EN/IT ora**: FR/ES/DE sono per i contenuti pubblici del sito (#13 G38), non per le email auth di base (estendibili in futuro).

## 6. Schermate & stati
Nessuna UI: artefatti = template email EN/IT. Coerenza visiva minima col brand (header/wordmark), testo "lean".

## 7. Dati toccati
Email dell'utente (destinatario) + codice/link temporaneo. **Dati personali**: email, base **contratto** (email transazionali,
#13 B). Invio via SES (UE). Nessun tracking nelle email. Manifest: trattamento "email transazionali" (#13 B).

## 8. Permessi & gate
- **Invarianti**: N/A diretto (è messaging); l'identità/tenant non transita nelle email se non l'indirizzo destinatario.
- Nessun gate entitlement (auth è pre-entitlement). Email transazionali ≠ newsletter (consenso separato, #13 F29).

## 9. Requisiti di test
- **Integration/unit**: selezione lingua corretta da `locale` (+ fallback EN); template renderizzati con i campi giusti.
- **E2E** (UC 0017): verifica/reset/invito generano l'email attesa (in locale via **Mailpit**, UC 0010); contenuto localizzato.

## 10. Riferimenti & Definition of Done
- **Decisioni**: #02 6, #06 26, #14 F1, #13 B/F29.
- **DoD**:
  1. Custom Message Lambda seleziona EN/IT dal `locale` (fallback EN).
  2. Template verifica/reset/invito in EN+IT, tono brand, invio da `noreply@appgrove.app` (DKIM).
  3. Email transazionali su base contrattuale (separate dalla newsletter).
  4. Verificabili in locale via Mailpit. (Resta la stesura dei **testi** definitivi.)

## Punti aperti / decisioni differite

_Tracciato dalla change `0010-use-case-0058-…` (regola CLAUDE.md "Tracciamento delle decisioni differite")._

- **Template email localizzati EN/IT.** UC 0058 (provider locale) invia email **funzionali** verso Mailpit (portano
  link/codice di verifica/reset/invito), senza il copy localizzato definitivo. I **template EN/IT** (lingua dal `locale`
  utente, mittente `noreply@appgrove.app`) sono materia di questo UC (in prod: Custom Message Lambda su Cognito; in
  locale: rendering dei template nel provider). **Proprietario**: UC 0018.

_Aggiunti dalla change `0037-use-case-0015-…` (Cognito + auth BFF):_

- ~~**Formato dei link nelle email Cognito: token `base64url(email|codice)`.**~~ ✅ **CHIUSO — e la soluzione
  ipotizzata si è rivelata NON REALIZZABILE** (change `0040`). Quando Cognito invoca il Custom Message Lambda **il
  codice non esiste ancora**: la Lambda riceve solo il segnaposto `{####}`, che Cognito sostituisce *dopo* aver
  ricevuto il messaggio. Costruire lì `base64url(email|codice)` incapsulerebbe il segnaposto dentro la codifica,
  Cognito non lo riconoscerebbe più e il collegamento arriverebbe **rotto**. Soluzione adottata: il collegamento
  porta **due parametri distinti** (`?email=…&code={####}`), così il segnaposto resta in chiaro; la ricomposizione
  del token avviene **nel backend**, che accetta la coppia `email`+`code` in alternativa al `token` unico
  (`IdentityProvider.emailActionToken`). Il contratto a token unico resta valido e continua a servire il provider
  locale — nessuna rottura. Presidiato da `test_handler.py` (il segnaposto deve arrivare non codificato) e da
  `CognitoEmailLinkTest`.
- ~~**Infrastruttura SES + raggiungibilità dalla VPC.**~~ ✅ **CHIUSO** (change `0040`). (1) Identità di dominio
  SES + firma DKIM provisionate in `infra/global/ses.tf` (una sola per dominio: test e prod spediscono entrambi da
  `noreply@appgrove.app`), con i record su Route53 e una politica DMARC in sola osservazione. (2) Aggiunto
  l'endpoint di rete `email` in `env_baseline/endpoints.tf` (~$7/mese/ambiente, in `_COSTI-AWS`). Verificato che
  **non esistono scorciatoie**: qualunque altra via (spedire da una funzione fuori dalla rete, passare da una coda)
  richiede comunque un endpoint dello stesso tipo e costo, perché la rete non ha uscita a internet. L'unico modo di
  toglierlo è far generare anche l'invito a Cognito, che significa riprogettare il flusso inviti (oggi token, ruolo,
  tenant e scadenza sono **nostri** in `platform.invitations`): tracciato come evoluzione **E24**.
- ~~**Email default Cognito fino a questo UC.**~~ ✅ **CHIUSO** (change `0040`): il pool è configurato con
  `email_sending_account = "DEVELOPER"` e spedisce via SES da `noreply@appgrove.app`, con testo e lingua resi dal
  Custom Message Lambda. Il tetto di ~50 email/giorno del mittente di default non c'è più.

_Aggiornamento dalla change `0040-use-case-0018-…` — punti aperti NUOVI:_

- **Uscita dalla modalità di prova di SES (sandbox).** Non risolvibile da codice: un account SES nuovo spedisce solo
  a indirizzi verificati a mano, e l'uscita è una **richiesta manuale ad AWS** che può richiedere giorni. Va avviata
  **in anticipo** rispetto al go-live. Tracciata nella checklist di prima accensione (`docs/_BACKLOG.md`, voce 11).
  **Proprietario**: fase di messa in cloud.
- **Gestione dei rimbalzi e dei reclami SES.** Oggi non esiste: nessuna notifica di rimbalzo, nessuna lista di
  soppressione, nessun monitoraggio del tasso. Non è un dettaglio operativo — SES **sospende l'account** se il tasso
  di rimbalzo resta alto, e la sospensione blocca registrazioni e reimpostazioni password per tutti. Da affrontare
  prima di volumi reali (probabile aggancio con l'osservabilità, UC 0006). **Proprietario**: UC 0018 (residuo).
- **Irrigidimento della politica DMARC.** Oggi `p=none` (sola osservazione) e nessun indirizzo per i rapporti
  aggregati, perché punterebbe a una casella inesistente. Dopo aver osservato il traffico reale: attivare i rapporti
  su una casella vera e valutare il passaggio a quarantena/rifiuto. **Proprietario**: UC 0018 (residuo).
- **Lingua modificabile dall'utente.** La colonna `locale` si valorizza alla registrazione e non è più modificabile
  da nessuna interfaccia. Renderla persistente dal selettore in topbar ed esporla nelle impostazioni utente è
  tracciato in **UC 0020**, che possiede quella parte di interfaccia.
