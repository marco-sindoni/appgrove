# Change 0090: Inviti e registrazione quando l'identità esiste già

**Branch**: `change/0090-use-case-0118-inviti-registrazione-identita-esistente`
**Aree**: `services/core`, `services/auth`, `frontend/apps/backoffice`, `frontend/packages/i18n`, `tools/platform-e2e`, `dev/seed`, `docs`
**Data**: 2026-08-21
**Autore**: Platform Engineering (modalità fast, autopilot)
**Use case sorgente**: [`docs/usecases/22-refactor-membership-model/story/0118-inviti-e-registrazione-con-identita-esistente.md`](../../docs/usecases/22-refactor-membership-model/story/0118-inviti-e-registrazione-con-identita-esistente.md)
**Tocca dati personali?**: Sì — nessun dato nuovo, ma un **legame nuovo** fra una persona e un account
(`invitations.identity_id`) da dichiarare nel manifesto. Classificazione **MINORE**: nessuna finalità
nuova, nessuna base giuridica nuova, nessun responsabile esterno nuovo, nessuna categoria particolare.

## Problema / Obiettivo

Dopo [UC 0116](../../docs/usecases/22-refactor-membership-model/story/0116-identita-e-appartenenze.md) e
[UC 0117](../../docs/usecases/22-refactor-membership-model/story/0117-account-attivo-e-selettore.md) il
modello **ammette** che una persona appartenga a più account, ma **nessun percorso di prodotto** ne crea
una seconda appartenenza. I due modi di entrare si fermano ancora davanti a un'identità che esiste:

- **percorso A** — un'azienda invita una persona che ha già un'identità appgrove: l'invito parte, e
  all'accettazione arriva un rifiuto (`409`, change 0088) invece dell'ingresso;
- **percorso B** — chi è già membro di un'azienda non ha alcun modo di aprire un proprio account: la
  registrazione con quell'indirizzo risponde «Email già registrata» e finisce lì.

E manca la **superficie** per l'esito che 0117 ha reso possibile: «appartieni a più account e nessuno è
attivo» oggi è un `409` all'accesso, senza schermata per rispondere.

Obiettivo: i due percorsi funzionano davvero, senza che nessun messaggio riveli a un'azienda l'esistenza
di un rapporto fra una persona e la piattaforma.

## Scope

### 1. Invio dell'invito — tre esiti, di cui due leciti (`services/core`)

`InvitationResource.create` distingue:

| Situazione | Esito | Perché |
|---|---|---|
| già membro **di questo** account | rifiuto `409` con identificativo `urn:appgrove:invitation:already-member` | informazione dell'account: lecita |
| invito già in attesa **in questo** account | rifiuto `409` con identificativo `urn:appgrove:invitation:already-invited` | idem |
| l'identità esiste **altrove**, o non esiste | **esito identico nei due casi** (`201`, stesso corpo) | l'esistenza di un rapporto fra quella persona e la piattaforma non è informazione dell'account |

I due rifiuti leciti si distinguono con il campo `type` del corpo problem+json (RFC 9457), non con un
messaggio da interpretare: il testo mostrato al cliente resta localizzato nelle cinque lingue.

La riga di invito acquista `identity_id` (annullabile), valorizzato **lato server** quando l'identità
esiste. Non viene mai restituito a chi invita né mostrato in alcuna interfaccia di account.

### 2. Accettazione da parte di chi è già registrato (`services/core` + `frontend`)

L'accettazione di chi ha già un'identità avviene **dalla propria sessione**, come consenso
nell'applicazione, e non da un modulo di registrazione:

- `GET /api/platform/v1/me/invitations` — gli inviti in attesa indirizzati all'indirizzo della persona
  in sessione, con il nome dell'azienda che invita. Lettura **di piattaforma** (attraversa gli account
  per costruzione, come `/me/memberships`), perimetro = il `sub` del token;
- `POST /api/platform/v1/me/invitations/{id}/accept` — crea **soltanto l'appartenenza** nell'account
  invitante, marca l'invito accettato e rende quell'appartenenza l'**account attivo** della persona;
- `POST /api/platform/v1/me/invitations/{id}/reject` — chiude l'invito come `rejected` (stato nuovo).

L'invito **non è trasferibile**: si accetta solo se l'indirizzo dell'invito coincide (senza distinzione
fra maiuscole e minuscole) con quello dell'identità autenticata. Un invito che non è proprio, revocato,
scaduto o già chiuso risponde `404` — indistinguibile da «non esiste».

Superficie: **sezione in testa al cruscotto** (`PendingInvitesSection`) e **numero degli inviti in attesa
sulla voce «Dashboard»** del menu laterale, perché da un'altra schermata resterebbero invisibili. La
pagina di accettazione via collegamento (`/accept?token=…`) interroga prima il servizio di
autenticazione e, quando l'indirizzo invitato ha già un'identità, mostra «accedi per accettare» invece
del modulo con la parola d'accesso — una parola d'accesso nuova su un'identità esistente sarebbe una
seconda identità mascherata.

### 3. Percorso B — chi è già membro apre un proprio account (`services/core` + `frontend`)

`POST /api/platform/v1/me/accounts` crea un account nuovo con l'appartenenza `owner` della persona in
sessione, **senza** creare una seconda identità e senza chiedere parola d'accesso né nome (li ha già).
Il nuovo account diventa quello attivo. Superficie: sezione «Apri un altro account» nella pagina
**Account**. La registrazione con un indirizzo già registrato continua a rifiutare, ma il testo mostrato
diventa azionabile: «accedi e apri un nuovo account dalla tua sessione».

### 4. La schermata per scegliere l'account (`services/auth` + `frontend`)

L'esito «più appartenenze attive e nessuna scelta valida» smette di essere un `409` cieco e diventa una
**sfida di scelta**, sul modello — già in casa — della sfida del secondo fattore:

- `POST /api/auth/login` e `POST /api/auth/login/2fa` rispondono
  `200 {account_selection_required: true, choice_token, accounts: [{account_id, account_name}]}`;
- `POST /api/auth/login/account` con `{choice_token, account_id}` conserva la scelta e restituisce la
  sessione.

Vale per **entrambi** i fornitori di identità: locale e Cognito, con la stessa forma di risposta. Il
token di scelta è a vita breve e nasce **solo dopo la verifica completa delle credenziali** (secondo
fattore incluso), quindi l'elenco degli account lo vede soltanto la persona. Sui percorsi non
interattivi (rinnovo, verifica dell'indirizzo) resta il rifiuto `409`: a chiusura in caso di dubbio.

### 5. Il posto si paga in ogni account (solo testo)

Il calcolo dei posti non esiste ancora ([UC 0103](../../docs/usecases/22-refactor-membership-model/story/0103-acquisto-anticipato-posto-invito.md)):
questa change scrive la **regola nel testo mostrato al cliente** nel riquadro dell'invito, perché la
prima reazione sarà «ma la paga già l'altra azienda».

### 6. Riuso di un indirizzo dopo la cancellazione di un'identità

L'unicità di `platform.identity` su indirizzo e identificativo di autenticazione è **incondizionata**
(vale anche sulle righe cancellate). Il controllo di esistenza dei percorsi d'ingresso diventa
altrettanto incondizionato, così chi si ripresenta con l'indirizzo di un'identità cancellata riceve lo
**stesso** messaggio comprensibile di sempre e non una violazione di indice (oggi: errore 500).

## Fuori scope

- **Calcolo e prezzo dei posti** → UC 0103 (proprietario); qui solo il testo.
- **Stato «appartenenza in attesa di accettazione»** su `platform.membership`: **non** si introduce.
  L'attesa è già la riga di invito (`platform.invitations`, stato `pending`); un secondo modo di dirla
  senza un percorso che lo usi sarebbe un'ambiguità in più. Se servirà, servirà per tenere occupato un
  posto acquistato in anticipo — cioè a UC 0103.
- **Unione di due identità** create per errore con indirizzi diversi → `docs/_BACKLOG.md`.
- **Rimborso del posto** se l'invito è rifiutato o scade → UC 0103 (riguarda denaro).
- **Limite al numero di account** che una persona può aprire → non deciso qui (direzione di prodotto).
- **Ritiro del ruolo `admin`** e visibilità fine per ruolo → UC 0098/0107/0113.
- **Etichette di ruolo** nell'interfaccia di piattaforma → vietate da UC 0117 §4.6, non cambia nulla.

## Criteri di accettazione

- [ ] Un'azienda invita un indirizzo che ha già un'identità: l'invito si crea e la risposta è
      **identica** (codice e corpo) a quella di un indirizzo sconosciuto — provato da un collaudo
      dedicato accanto a quelli sulle risposte neutre.
- [ ] Invitare chi è **già membro** dello stesso account, o chi ha già un invito in attesa, produce due
      rifiuti **distinti e riconoscibili da un programma**, con due testi localizzati diversi.
- [ ] Chi ha già un'identità vede l'invito nel cruscotto, lo accetta dalla propria sessione e ottiene
      una **seconda appartenenza**: nessuna seconda identità, nessuna parola d'accesso nuova. Il nuovo
      account diventa quello attivo e compare nel selettore.
- [ ] Un invito indirizzato a un altro indirizzo non è accettabile dall'identità in sessione (`404`).
- [ ] Chi è membro di un'azienda apre un proprio account dalla pagina Account e vi si trova `owner`,
      con una sola identità e due appartenenze.
- [ ] Con più appartenenze attive e nessuna scelta valida, l'accesso mostra la **schermata di scelta**
      dell'account e, scelto l'account, la sessione nasce con quel claim.
- [ ] Un indirizzo appartenente a un'identità cancellata dà un rifiuto comprensibile, non un errore.
- [ ] `./run-tests.sh` (suite completa) verde.

## Invarianti appgrove toccati

1. **Tenant ID solo dal JWT verificato** — le tre operazioni nuove di `core` prendono il perimetro dal
   `sub` del token, mai da un identificativo del chiamante. L'account nuovo e l'appartenenza nuova
   nascono da scritture di piattaforma, e il claim continua a essere **calcolato** solo alla creazione
   del token, riverificando l'appartenenza: cambia chi scrive il suggerimento, non chi se ne fida.
   `POST /me/active-account` e la sfida di scelta si limitano a scrivere quel suggerimento.
2. **Filtro row-level** — le letture che attraversano gli account (inviti della persona, appartenenze,
   account di destinazione) sono **native e dichiaratamente senza filtro**, come `tenantsOf` e
   `activeAccountsOf` di UC 0116/0117, e restano riservate ai percorsi «di me stesso» e di piattaforma.
   Nessuna interfaccia di account le usa. Tutto il resto conserva il discriminatore.
3. **Modulo Terraform `microsaas_app`** — non toccato (nessuna app nuova).
4. **Logging strutturato** — gli eventi nuovi (invito accettato/rifiutato, account aperto, account
   scelto all'accesso) portano `user_id`, `tenant_id` e soli identificativi opachi: **mai** l'indirizzo
   dell'invitato, come già fa `member.invited`.

## Requisiti di test

- **Sicurezza, la prova che tiene la riservatezza**: gli esiti dell'invio dell'invito sono
  indistinguibili fra identità esistente e inesistente — stesso codice, stesse chiavi, stessi valori
  dove i valori non dipendono dall'invito. Nessuna asserzione sui tempi (sarebbe instabile): la
  garanzia è che **la stessa lettura si esegue in entrambi i rami**, e sta scritta accanto al codice.
- **Integrazione percorso A**: invito → accettazione autenticata → seconda appartenenza, invito
  `accepted`, `active_membership_id` sulla nuova appartenenza, nessuna identità in più.
- **Integrazione percorso B**: nuovo account dalla sessione → appartenenza `owner`, identità unica.
- **Integrazione collisioni legittime**: già membro e invito in attesa → due `type` distinti.
- **Integrazione invito non proprio / scaduto / revocato / già chiuso** → `404`.
- **Rifiuto dell'invito** → stato `rejected`, non più visibile né accettabile.
- **Riuso di un indirizzo cancellato** → rifiuto comprensibile in iscrizione e in accettazione.
- **Sfida di scelta dell'account**: tabella dei casi sul fornitore locale (una appartenenza → sessione;
  più appartenenze senza scelta → sfida; scelta di un account non proprio → rifiuto; scelta valida →
  sessione con quel claim) e la gemella sul fornitore Cognito con il client simulato.
- **Percorsi end-to-end**: `J-INVITE-EXISTING` (stack vero) — un'azienda invita una persona che ha già
  un proprio account, la persona accetta dal cruscotto e passa fra i due account; `L2-INVITE-EXISTING`
  (livello 2) — sezione del cruscotto, numero sulla voce del menu, accettazione e rifiuto;
  `L2-ACCOUNT-CHOICE` (livello 2) — la schermata di scelta all'accesso.

## Valutazione di impatto

| Area | Impatto |
|---|---|
| Breaking change | No — nessun contratto esistente cambia forma; `POST /api/auth/login` e `/login/2fa` acquistano una **terza** forma di risposta, additiva come fu `mfa_required` |
| Contratto cross-area | Sì — frontend ↔ `core` (tre operazioni nuove sotto `/me`), frontend ↔ `auth` (sfida di scelta, ispezione dell'invito) |
| Version bump | minor |
