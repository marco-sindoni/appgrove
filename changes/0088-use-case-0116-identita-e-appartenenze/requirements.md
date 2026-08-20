# 0088 — UC 0116 · Identità della persona e appartenenze agli account

**Use case sorgente**: [docs/usecases/22-refactor-membership-model/story/0116-identita-e-appartenenze.md](../../docs/usecases/22-refactor-membership-model/story/0116-identita-e-appartenenze.md)
**Piano di lavoro**: [task/0116](../../docs/usecases/22-refactor-membership-model/task/0116-identita-e-appartenenze.md)
**Epica**: E22.5 — Identità e appartenenze (prima storia dell'epica 22)
**Modalità**: fast (autopilot senza fermate di workflow; suite completa verde prima del commit)
**Aree toccate**: `services/core`, `services/auth`, `infra` (funzione che compone il token), conformità,
`tools/platform-e2e`, `dev/seed`, documenti

## 1. Obiettivo

Sciogliere il vincolo «una persona appartiene a un solo account», oggi imposto da due indici unici
globali su `platform.users` — una tabella che sta **dentro** l'account — e sostituirlo con **una
persona, più appartenenze**:

- l'**identità** della persona diventa un'entità di **piattaforma** (`platform.identity`): indirizzo
  di posta e identificativo di autenticazione unici globalmente, ma su una riga che non appartiene a
  nessun account;
- l'**appartenenza** diventa un'entità propria di account (`platform.membership`): la coppia
  (account, identità) con ruolo, stato e date, unica **sulle righe vive**.

Il vincolo che serve davvero — «non due volte nello stesso account» — diventa **esplicito** invece di
essere l'effetto collaterale di un vincolo più forte del necessario.

## 2. Perimetro

**Dentro**

1. le due entità nuove nello schema, con l'unicità spostata dove deve stare;
2. il **travaso** dei dati di oggi, con controllo dei conteggi **dentro** la migrazione;
3. entità, repository e letture del core: la schermata dei membri continua a vedere «le persone di
   questo account», ma i campi arrivano da appartenenza + identità;
4. il fornitore di identità (Cognito e locale, in parità) e la funzione che compone il token;
5. conformità: manifesto dati, registro dei trattamenti, esportazione e cancellazione;
6. `platform.users` **resta** come rete di sicurezza, ma **nessuno la legge e nessuno la scrive più**;
7. seme di sviluppo, collaudi automatici e percorsi end-to-end di piattaforma allineati.

**Fuori** (di altri use case, tracciato come rimando)

- quale account è **attivo** in una sessione e come si cambia → UC 0117 (con una sola appartenenza il
  comportamento di oggi si conserva identico: la prima e unica appartenenza attiva);
- i due percorsi d'ingresso — invitare chi esiste già, registrare chi è già membro altrove — e i
  messaggi comprensibili che non rivelano l'esistenza di un'identità → UC 0118;
- il ruolo per applicazione e il ritiro del ruolo `admin` → UC 0098/0113;
- lo stato «appartenenza in attesa di accettazione» → UC 0118 (oggi l'attesa è la riga di invito);
- la rimozione fisica di `platform.users` → migrazione successiva, quando la rete di sicurezza non
  serve più.

## 3. Modello dati

### `platform.identity` — entità di piattaforma (nessun `tenant_id`)

| Colonna | Tipo | Nota |
|---|---|---|
| `id` | uuid | chiave; **coincide con l'id dell'utente di oggi** (vedi §4) |
| `cognito_sub` | varchar(128) | identificativo di autenticazione, unico globalmente |
| `email` | varchar(320) | unico globalmente in minuscolo |
| `display_name` | varchar(255) | |
| `locale` | varchar(8) | lingua della persona (`en`/`it`), non dell'account |
| `status` | varchar(32) | `active` · `suspended` |
| `suspended_reason` | varchar(32) | causale (`gdpr_restriction`, art. 18) |
| audit + soft-delete | | `created_at`, `updated_at`, `created_by`, `updated_by`, `deleted_at` |

### `platform.membership` — entità di account

| Colonna | Tipo | Nota |
|---|---|---|
| `id` | uuid | chiave |
| `tenant_id` | varchar(64) | discriminatore di account (invariante 2) |
| `identity_id` | uuid | riferimento all'identità (chiave esterna) |
| `role` | varchar(32) | `owner` · `admin` · `member` (i tre di oggi; il ritiro di `admin` è di UC 0113) |
| `status` | varchar(32) | `active` · `suspended` |
| audit + soft-delete | | come sopra |

Indici: unico su `(tenant_id, identity_id)` **sulle righe vive**; indice su `identity_id` (per «a
quali account appartiene questa persona?»); indice su `tenant_id`.

### Le due domande, tenute distinte

- «chi sono le persone di questo account?» → appartenenze dell'account, **con** il filtro per riga;
- «a quali account appartiene questa persona?» → appartenenze dell'identità, **senza** filtro per
  account, per costruzione: è una lettura di piattaforma e va marcata come tale nel codice.

### Dove finisce lo stato

Due stati distinti, entrambi necessari:

- `identity.status` = la persona può accedere alla piattaforma (leva del titolare: limitazione del
  trattamento, art. 18);
- `membership.status` = la persona può presentarsi come persona di **quell'** account (leva
  dell'owner).

Chi emette il token e chi autentica pretende **entrambi** attivi: a chiusura in caso di dubbio.

## 4. Migrazione — `V17__identity_membership.sql`

Una sola migrazione, in ordine:

1. crea `platform.identity` e `platform.membership` con i loro indici;
2. **travasa** ogni riga viva di `platform.users` in una identità **con lo stesso `id`** — così ogni
   riferimento già memorizzato (chi ha invitato, chi ha accettato, il bersaglio di una limitazione, il
   soggetto di un'accettazione legale) continua a risolvere — più una appartenenza con account, ruolo
   e stato di oggi;
3. rimuove da `platform.users` i due indici unici globali: sono il vincolo che questa storia scioglie;
4. **verifica i conteggi dentro la migrazione** e la fa **fallire** se non tornano: utenti vivi =
   identità = appartenenze, e nessun indirizzo perso. Una migrazione che perde persone in silenzio è
   il difetto peggiore possibile qui;
5. lascia `platform.users` in piedi, con un commento che dice che è una rete di ritorno e che nessuno
   la legge più.

## 5. Requisiti funzionali

1. **Interfaccia invariata**: `GET/PATCH/DELETE /api/platform/v1/users*` conservano forma e semantica.
   `id` nella risposta è l'id dell'**identità** (che è l'id dell'utente di oggi): nessun contratto
   cambia, nessun documento OpenAPI cambia.
2. **Uscita da un account** = chiusura dell'**appartenenza** (soft-delete della sola appartenenza).
   L'identità resta, e le altre appartenenze non ne sanno nulla.
3. **Una persona in due account** è ammessa: due appartenenze, la stessa identità.
4. **Due appartenenze allo stesso account** sono rifiutate dal **vincolo**, non solo dall'interfaccia.
5. **Nessun percorso di account espone le altre appartenenze** di una persona: le letture di account
   filtrano per account; la lettura «a quali account appartiene» è riservata al percorso di accesso e
   alla console di piattaforma.
6. **Registrazione**: crea account + identità (se non esiste) + appartenenza `owner`.
   **Accettazione di un invito**: crea l'appartenenza, e l'identità solo se manca.
7. **Lingua della persona**: si legge dall'identità (per indirizzo di posta), non più dall'utente di
   un account.
8. **Funzione che compone il token** (Cognito) e **fornitore locale**: cercano l'identità per
   identificativo di autenticazione e ne prendono l'appartenenza **attiva**; con una sola appartenenza
   il comportamento è identico a oggi. La scelta fra più appartenenze è di UC 0117: qui si prende
   quella più antica in modo **deterministico**, e la funzione lo dice a voce alta nel commento.
9. **Cancellazione di un account**: si cancellano le sue appartenenze e i suoi dati; l'identità
   **sopravvive** se ha altre appartenenze e viene cancellata solo quando resta senza.

## 6. Conformità (#13)

- **Nessun trattamento nuovo**: indirizzo, nome e lingua sono già trattati e dichiarati. Cambia la
  **titolarità del posto**: l'identità è un dato di piattaforma, l'appartenenza è un dato dell'account.
  Il manifesto `platform` sposta le voci `users.*` su `identity.*` (con la posizione e i soggetti
  aggiornati) e aggiunge la voce dell'appartenenza; il registro dei trattamenti si rigenera.
- **Classificazione**: MINORE — nessuna nuova finalità, nessuna nuova base giuridica, nessun nuovo
  responsabile esterno, nessuna categoria particolare. Nessun aumento di versione di privacy/termini.
- **Esportazione** di un account: contiene le appartenenze e l'identità **della persona di
  quell'account**, e nulla di altri account.
- **Cancellazione** di un account: appartenenze via, identità solo se orfana.

## 7. Requisiti di test

- **Migrazione**: conteggi a confronto sui dati di riferimento (utenti vivi = identità = appartenenze),
  e la guardia dentro la migrazione provata a sua volta.
- **Ciclo di vita**: due appartenenze per la stessa identità; rifiuto della seconda appartenenza allo
  stesso account; uscita da un account che lascia intatta l'altra; identità senza appartenenze che non
  ottiene accesso.
- **Separazione**: la stessa identità in due account non attraversa il confine, in lettura, in
  scrittura e in esportazione — nella sede delle prove di separazione già esistenti.
- **Conformità**: cancellare l'account A non cancella l'identità se esiste l'appartenenza a B;
  cancellare l'ultima appartenenza rende l'identità cancellabile.
- **Parità dei fornitori**: la stessa tabella di casi su Java (locale) e Python (funzione del token).
- **Percorsi end-to-end**: nessuno proprio (storia di modello dati). Nel registro di copertura la voce
  0116 passa da `non-implementato` a **`senza-superficie`**; i percorsi di piattaforma esistenti
  vengono adeguati alle tabelle nuove e restano verdi — è quella la prova che il travaso non ha rotto
  nulla di visibile.

## 8. Definition of Done

1. identità e appartenenza esistono come entità distinte, con l'unicità spostata dove deve stare;
2. il travaso converte i dati di oggi senza perdite, provato con conteggi a confronto;
3. la stessa persona può avere due appartenenze e la separazione dei dati è provata;
4. nessun percorso di account rivela le altre appartenenze di una persona;
5. manifesto dati e registro dei trattamenti riflettono il nuovo assetto;
6. `docs/02 §14` dice che «1 utente → 1 account» è superata e da cosa; `docs/01` e `docs/05` seguono;
7. registro di copertura end-to-end coerente e `docs/usecases/_INDEX.md` aggiornato;
8. **`./run-tests.sh` completa verde** prima del commit (contropartita della modalità fast).
