# UC 0098 — Modello dati dell'accesso per applicazione e ruolo di piattaforma

**Area**: 22-refactor-membership-model · **Fase**: evo · **Stato**: ✅ implementato (change 0091)
**Epica**: [E22.1 Fondamenta](../epic/E22-01-fondamenta-modello-centralizzato.md)
**Dipendenze**: [UC 0116](0116-identita-e-appartenenze.md) (identità e appartenenze: la tabella riferisce l'identità), UC 0013 (account, utenti, inviti e interfaccia del core), UC 0059 (schermata membri)
**Sostituisce**: UC 0072 e UC 0073 dell'epica 14 (appartenenza e posti per applicazione)
**Piano di lavoro**: [task/0098](../task/0098-modello-dati-accesso-per-applicazione.md)
**Ultimo aggiornamento**: 2026-08-21

## 1. Obiettivo / Scope

Creare il luogo in cui vive la frase «questa persona può usare questa applicazione con questo ruolo», e
ridurre il ruolo di piattaforma ai due soli valori che hanno senso in un modello centralizzato:
`owner` e `member`.

**Incluso**: la nuova tabella `platform.app_access` con la sua entità e il suo repository; la riduzione
dell'enumerazione del ruolo di piattaforma; l'interfaccia del core per leggere e scrivere gli accessi;
i vincoli strutturali (un solo owner, owner non rimovibile, accesso implicito dell'owner); la traccia di
controllo delle operazioni.

**Escluso**: come i servizi delle applicazioni **fanno rispettare** il ruolo → UC 0099; il significato
di comportamento dei tre ruoli → UC 0101; la schermata → UC 0100 e UC 0111; la migrazione dei dati
esistenti → UC 0113.

## 2. Attori & ruoli

- **Owner dell'account**: concede e revoca accessi su qualunque applicazione, cambia qualunque ruolo.
- **`admin` di una applicazione**: concede e revoca accessi **a quella sola applicazione**, e solo a
  persone già esistenti e attive.
- **`editor` / `viewer`**: leggono chi ha accesso; non scrivono.
- **Sistema**: valuta gli accessi a ogni richiesta e mantiene la traccia di controllo.

## 3. Precondizioni

- Esistono identità e appartenenze ([UC 0116](0116-identita-e-appartenenze.md)), che prendono il posto di
  `platform.membership`/`platform.identity` (UC 0116), e gli inviti (UC 0013).
- Esiste il catalogo delle applicazioni (`platform.app`) con i suoi identificativi.
- L'account ha almeno un utente: l'owner, creato all'iscrizione.

## 4. Flusso principale

1. L'owner (o l'`admin` di quella applicazione) chiede di **concedere accesso** a una persona esistente
   e attiva, indicando il ruolo (`viewer`, `editor` o `admin`).
2. Il sistema verifica, nell'ordine: che chi chiede ne abbia il diritto; che la persona appartenga
   **allo stesso account** (dal claim verificato, mai dal corpo della richiesta); che sia in stato
   attivo; che l'account abbia diritto a quella applicazione; che non esista già un accesso.
3. Scrive una riga in `platform.app_access` con account, applicazione, persona, ruolo e chi l'ha
   concesso.
4. Emette l'evento di invalidazione della copia locale, così che il servizio dell'applicazione veda il
   cambiamento entro pochi secondi (meccanismo di UC 0099).
5. Scrive la traccia di controllo `app_access.granted` con soli identificativi opachi — **mai** email o
   nome.
6. **Cambio di ruolo** e **revoca** seguono lo stesso schema, con eventi `app_access.role_changed` e
   `app_access.revoked`.

## 5. Flussi alternativi / edge / errori

- **Errore — persona di un altro account**: rifiuto `404` (non `403`: non si rivela l'esistenza di
  utenti di altri account).
- **Errore — persona sospesa o in attesa di accettare l'invito**: rifiuto tipizzato «la persona non è
  attiva»; l'accesso si concede solo a persone attive. Un invito accettato in seguito **non** concede
  accessi in automatico.
- **Errore — l'account non ha diritto a quella applicazione**: rifiuto tipizzato. Non si concede accesso
  a un'applicazione che l'account non ha.
- **Errore — `admin` che tenta su un'altra applicazione**: rifiuto `403`. Il suo potere è circoscritto.
- **Errore — `admin` che tenta di nominare un altro `admin`**: **ammesso** (il requisito dice che
  l'`admin` «può modificare i ruoli»), ma **non** può concedere a sé stesso l'owner, che non è un ruolo
  di applicazione.
- **Edge — owner**: non serve una riga di accesso, ce l'ha implicito su tutte le applicazioni
  dell'account. Un tentativo di revocargli l'accesso è rifiutato.
- **Edge — ultimo owner**: non rimovibile, non retrocedibile, non sospendibile. Vincolo strutturale, non
  solo controllo dell'interfaccia.
- **Edge — accesso già esistente con ruolo diverso**: non è un errore ma un **cambio di ruolo**;
  l'interfaccia lo tratta come tale invece di chiedere due operazioni.
- **Edge — applicazione disattivata dalla piattaforma**: le righe di accesso **restano**; è il diritto
  dell'account che decade. Riattivando l'applicazione gli accessi tornano validi senza doverli
  ricostruire.
- **Edge — persona rimossa dall'account**: i suoi accessi vengono cancellati logicamente insieme a lei.

## 6. Risorse & dati _(storia di modello dati)_

Nessuna schermata. Le superfici che consumano questo modello sono UC 0100 (elenco unico) e UC 0111
(gestione utenti dentro l'applicazione).

## 7. Dati toccati

**Nuova tabella `platform.app_access`** — separata per account (portatrice della colonna `tenant_id`
come tutte le altre):

| Colonna | Tipo | Nota |
|---|---|---|
| `id` | uuid | chiave |
| `tenant_id` | varchar(64) | discriminatore di account, invariante 2 |
| `app_id` | uuid | riferimento a `platform.app` |
| `identity_id` | uuid | riferimento a `platform.identity` ([UC 0116](0116-identita-e-appartenenze.md)): l'appartenenza cambia nel tempo, l'identità no |
| `role` | varchar(16) | `viewer` · `editor` · `admin` |
| `granted_by` | uuid | chi ha concesso, per la traccia di controllo |
| campi di audit | | `created_at`, `updated_at`, `created_by`, `updated_by`, `deleted_at` |

Indici: unicità su `(tenant_id, app_id, identity_id)` sulle righe vive; indice su `(tenant_id, identity_id)` per
la domanda «quali applicazioni vede questa persona?» che serve al menu laterale; indice su
`(tenant_id, app_id)` per «chi ha accesso a questa applicazione?».

**Modifica all'appartenenza** (`platform.membership`, creata da [UC 0116](0116-identita-e-appartenenze.md)):
l'enumerazione del ruolo passa da tre valori a due (`owner`, `member`). La colonna resta la stessa; cambia
l'insieme dei valori ammessi. La conversione dei dati esistenti è di UC 0113.

**Dati personali**: questa storia **non apre un nuovo trattamento**. `app_access` non contiene dati
personali in senso proprio — soltanto identificativi interni — ma **descrive** chi accede a cosa, e va
quindi dichiarata nel manifesto dei dati della piattaforma come dato di **autorizzazione**:
*categoria* dato di autorizzazione (nessun dato personale diretto); *finalità* consentire l'accesso
delle persone dell'account alle applicazioni acquistate; *base giuridica* esecuzione del contratto con
l'account titolare; *conservazione* legata alla vita dell'appartenenza. La riduzione dei ruoli di
piattaforma non cambia alcuna categoria di dato.

## 8. Permessi & gate

- **Account solo dal token verificato**: l'account è sempre il claim verificato, mai dal corpo o dai
  parametri. Vale anche per l'identità bersaglio: si accetta solo se **ha un'appartenenza attiva** a
  quell'account. È il controllo che tiene, ora che la stessa identità può appartenere a più account.
- **Filtro riga per riga**: ogni lettura e scrittura porta il filtro sull'account.
- **Chi può scrivere**: `owner` su tutto; `admin` **solo** sull'applicazione su cui è `admin`. La
  verifica del secondo caso **non** può basarsi sul token (non contiene i ruoli per applicazione): si
  legge dal modello, dentro la stessa transazione.
- **Diritto dell'account all'applicazione**: verificato prima di concedere. Un accesso a
  un'applicazione a cui l'account non ha diritto non deve poter esistere.
- **Traccia di controllo obbligatoria** per concessione, cambio di ruolo e revoca, con soli
  identificativi opachi.

## 9. Requisiti di test

- **Unità**: le regole di chi-può-cosa come funzione pura (owner, `admin` della stessa applicazione,
  `admin` di un'altra, `editor`, `viewer`, persona esterna); il vincolo dell'ultimo owner.
- **Integrazione** con banca dati reale: concessione, cambio di ruolo, revoca; unicità della terna;
  rifiuto per persona non attiva; rifiuto per applicazione senza diritto.
- **Separazione fra account** (prova di sicurezza dedicata): un owner dell'account A non concede né
  legge accessi dell'account B; una persona dell'account B non è nemmeno visibile.
- **Concorrenza**: due concessioni simultanee sulla stessa terna producono una sola riga.
- **Percorsi end-to-end**: nessuno proprio (storia senza superficie); i percorsi arrivano con UC 0100,
  0107 e 0111. Nel registro di copertura questa storia è esente come *senza superficie*.

## 10. Riferimenti & Definition of Done

- **Riferimenti**: [Epica 22 §2](../epic/E22-00-rifacimento-modello-appartenenza.md),
  [UC 0013](../../04-platform-core/0013-account-utenti-inviti-api.md),
  [User.java](../../../../services/core/src/main/java/app/appgrove/core/platform/User.java),
  [UserRole.java](../../../../services/core/src/main/java/app/appgrove/core/platform/UserRole.java).
- **Definition of Done**:
  1. `platform.app_access` esiste con i suoi vincoli e i suoi indici;
  2. il ruolo di piattaforma ammette due soli valori;
  3. l'interfaccia del core concede, cambia e revoca, rispettando i limiti dell'`admin`;
  4. l'owner ha accesso implicito e non è rimovibile né retrocedibile;
  5. la separazione fra account è provata da una prova di sicurezza dedicata;
  6. il manifesto dei dati della piattaforma dichiara la nuova tabella;
  7. `run-tests.sh backend` verde.

## Punti aperti / decisioni differite

- **Più di un owner**: fuori scope (requisito dello sviluppatore). La tabella e i vincoli non lo
  precludono: il vincolo «un solo owner» è una regola applicativa, non una chiave unica sulla riga.
  Proprietario: Epica 22.
- **Accesso implicito dell'owner o riga esplicita?** Si adotta l'implicito, perché una riga per l'owner
  andrebbe mantenuta a ogni applicazione installata e potrebbe essere cancellata per errore. Costo da
  ricordare: ogni lettura di «chi ha accesso» deve **aggiungere** l'owner al risultato. Sorvegliato dai
  collaudi di UC 0111.
- **Ruolo predefinito quando si concede accesso** — **chiuso** (change 0091): il servizio **non** ha un
  valore predefinito, il ruolo è obbligatorio nel corpo della richiesta, perché un potere concesso per
  omissione di un campo è il modo peggiore di concederlo. Il predefinito prudente da *proporre* resta
  `viewer` e appartiene all'interfaccia: rimando tracciato in UC 0111.
- **Accesso già esistente** — **chiuso** (change 0091): la concessione su una terna che esiste già non è un
  errore ma un **cambio di ruolo**, deciso dal servizio e non dedotto dall'interfaccia. L'arbitro
  dell'unicità è l'indice della banca dati, non la lettura che precede la scrittura: due concessioni
  simultanee producono una riga e un rifiuto.
- **Rimandi lasciati alle storie successive** (change 0091): l'evento di invalidazione della copia locale e
  la lettura «dove può entrare questa persona» → UC 0099; la conversione dei dati reali, il vincolo di
  controllo sui valori del ruolo di piattaforma e il ritiro della tolleranza `admin` nel token → UC 0113;
  la schermata dei membri senza colonna del ruolo e il campo `role` dell'invito → UC 0100; il predefinito
  dell'interfaccia e le schermate che consumano le nuove operazioni → UC 0111.
