# UC 0116 — Identità della persona e appartenenze agli account

**Area**: 22-refactor-membership-model · **Fase**: evo · **Stato**: ✅ implementato (change `0088`)
**Epica**: [E22.5 Identità e appartenenze](../epic/E22-05-identita-e-appartenenze.md)
**Dipendenze**: UC 0013 (account, utenti, inviti e interfaccia del core)
**Piano di lavoro**: [task/0116](../task/0116-identita-e-appartenenze.md)
**Ultimo aggiornamento**: 2026-08-20 (implementazione: change `0088`)

## 1. Obiettivo / Scope

Sciogliere il vincolo **«una persona appartiene a un solo account»**, che oggi è imposto per costruzione, e
sostituirlo con **una persona, più appartenenze**.

Il vincolo di oggi non è una convenzione: è scritto nello schema come indice unico globale
sull'indirizzo di posta e sull'identificativo di autenticazione
([V2__core_domain.sql](../../../../services/core/src/main/resources/db/migration/V2__core_domain.sql),
commento «membership foldata 1 utente→1 tenant»), dichiarato in
[docs/02 §14](../../../02-auth-sicurezza.md) e assunto dalla funzione che costruisce il token, che cerca
**una** riga per identificativo di autenticazione. Da qui due conseguenze che si vedono subito:

- una persona invitata da un'azienda **non può** aprire un proprio account con lo stesso indirizzo;
- e chi ha già provato appgrove per conto proprio **non può essere invitato** da un'azienda: l'invito parte e
  il rifiuto arriva più tardi, come violazione di indice invece che come messaggio comprensibile.

**Incluso**: le due entità (identità della persona, appartenenza a un account); lo spostamento
dell'unicità dall'utente-dentro-l'account all'identità globale; la migrazione dei dati esistenti; le
letture che l'interfaccia e il token richiedono; il ciclo di vita (uscita da un account, cancellazione
dell'identità).

**Escluso**: quale account è **attivo** in una sessione e come si cambia → [UC 0117](0117-account-attivo-e-selettore.md);
i due percorsi d'ingresso (invito a chi esiste già, registrazione di chi è già membro altrove) →
[UC 0118](0118-inviti-e-registrazione-con-identita-esistente.md); il ruolo per applicazione → UC 0098.

## 2. Attori & ruoli

- **Persona**: una sola identità sulla piattaforma, indipendentemente da quanti account la ospitano.
- **Owner di un account**: governa le **appartenenze** al proprio account, non le identità. Non vede, e non
  deve vedere, a quali altri account quella persona appartenga.
- **Amministratore di piattaforma**: vede identità e appartenenze come entità distinte, perché è l'unico
  che assiste sui problemi di accesso.
- **Sistema**: garantisce che un'identità non veda mai i dati di un account attraverso un'altra
  appartenenza.

## 3. Precondizioni

- Esiste il nucleo account/utenti/inviti (UC 0013) con `platform.users` e `platform.invitations`.
- Esiste il fornitore di identità in due varianti — Cognito e locale — con la parità di claim già
  stabilita (UC 0010).

## 4. Flusso principale

1. L'identità della persona diventa un'entità **di piattaforma, non di account**: indirizzo di posta e
   identificativo di autenticazione unici **globalmente**, come già oggi, ma su un'entità che **non**
   appartiene a nessun account.
2. L'appartenenza diventa un'entità propria: la coppia (account, identità) con il suo ruolo di piattaforma
   (`owner` o `member`), il suo stato e le sue date. Una persona può averne più di una; per ogni account
   ne ha **al massimo una**.
3. Le domande che il sistema deve saper rispondere diventano due, e vanno tenute distinte:
   - «chi sono le persone di questo account?» → le appartenenze di quell'account (la schermata dei membri);
   - «a quali account appartiene questa persona?» → le sue appartenenze (il selettore di UC 0117 e il
     percorso di accesso).
4. **L'account resta il confine dei dati.** Tutto ciò che è separato per account lo resta: cambia soltanto
   il modo in cui si stabilisce *chi* può presentarsi come persona di quell'account.
5. La migrazione dei dati esistenti è **meccanica e senza perdite**: ogni riga di `platform.users` diventa
   una identità più una appartenenza. Le persone di oggi hanno una sola appartenenza, quindi nessuno vede
   cambiamenti il giorno del rilascio.

## 5. Il nodo della storia: dove si sposta l'unicità

Oggi l'indirizzo di posta è unico **globalmente** su una tabella che è **dentro** l'account: è quel
disallineamento a produrre il vincolo. Dopo:

| | Oggi | Dopo |
|---|---|---|
| Indirizzo di posta | unico globalmente su `platform.users` (tabella separata per account) | unico globalmente sull'**identità**, che non appartiene a nessun account |
| Persona nello stesso account due volte | impossibile per effetto collaterale | impossibile per vincolo **esplicito** sulla coppia (account, identità) |
| Persona in due account | **impossibile** | ammessa, ed è il punto della storia |

Il vincolo che serve davvero — «non due volte nello stesso account» — diventa quindi **esplicito** invece
di essere un effetto collaterale di un vincolo più forte del necessario. È il difetto di fondo che questa
storia corregge: la regola giusta era nascosta dentro una regola troppo larga.

## 6. Flussi alternativi / edge / errori

- **Edge — la persona esce da un account**: si chiude l'**appartenenza**, non l'identità. Le altre
  appartenenze non ne sanno nulla.
- **Edge — la persona esce dall'ultimo account**: l'identità resta senza appartenenze. Non è uno stato
  proibito, ma è uno stato **inutilizzabile**: chi si autentica senza appartenenze attive non ottiene un
  token valido (già oggi il comportamento è questo, «a chiusura in caso di dubbio», e va conservato). La
  cancellazione dell'identità avviene con le regole di conservazione, non all'istante.
- **Edge — cancellazione di un account** (UC 0033): si cancellano le sue appartenenze e i suoi dati; le
  identità delle persone **sopravvivono** se hanno altre appartenenze. Oggi non è così, perché l'utente è
  parte dell'account: è la stretta più delicata verso la conformità e va provata in entrambi i versi.
- **Edge — la stessa persona è owner del proprio account e member di un altro**: caso normale, non
  eccezione. È il caso che ha originato la storia.
- **Errore — due appartenenze allo stesso account**: rifiutato dal vincolo, non solo dall'interfaccia.
- **Errore — appartenenza a un account inesistente o cancellato**: rifiutata.
- **Edge — l'owner di un account non deve poter dedurre le altre appartenenze** della persona. Nessuna
  interfaccia dell'account le espone, e nemmeno indirettamente (per esempio con un messaggio d'errore che
  dica «questa persona ha già un account»: vedi [UC 0118](0118-inviti-e-registrazione-con-identita-esistente.md),
  che deve conciliare messaggi comprensibili e riservatezza).

## 7. Dati toccati

**Nuova entità — identità della persona** (`platform.identity`), **non** separata per account:

| Colonna | Tipo | Nota |
|---|---|---|
| `id` | uuid | chiave |
| `cognito_sub` | varchar(128) | identificativo di autenticazione, unico globalmente |
| `email` | varchar(320) | unico globalmente in minuscolo, come oggi |
| `display_name` | varchar(255) | |
| `locale` | varchar(16) | preferenza di lingua della persona, che è sua e non dell'account |
| `status` | varchar(32) | attiva, sospesa |
| campi di audit | | |

**Nuova entità — appartenenza** (`platform.membership`), separata per account:

| Colonna | Tipo | Nota |
|---|---|---|
| `id` | uuid | chiave |
| `tenant_id` | varchar(64) | discriminatore di account, invariante 2 |
| `identity_id` | uuid | riferimento all'identità |
| `role` | varchar(32) | `owner` · `member` (UC 0098) |
| `status` | varchar(32) | attiva, sospesa, in attesa di accettazione |
| campi di audit | | |

Indici: unicità su `(tenant_id, identity_id)` sulle righe vive — il vincolo che serve; indice su
`identity_id` per «a quali account appartiene questa persona?».

**Sorte di `platform.users`**: le sue colonne si dividono fra le due entità nuove. Le altre tabelle che la
riferiscono per identificativo di persona (`platform.app_access` di UC 0098, la traccia di controllo, i
biglietti di assistenza) passano a riferire l'**identità**: l'appartenenza cambia nel tempo, l'identità no.

**Dati personali**: nessun trattamento nuovo — indirizzo, nome e lingua sono già trattati e dichiarati.
Cambia però **la titolarità**: oggi ogni riga utente sta dentro un account; dopo, l'identità è un dato di
piattaforma e le appartenenze sono dati dell'account. Il manifesto dei dati della piattaforma e il registro
dei trattamenti vanno aggiornati di conseguenza, e la classificazione va rifatta con il rilevatore dei
segnali privacy: non è un cambio di scopo, ma è un cambio di **chi risponde per quel dato**.

## 8. Permessi & gate

- **Account solo dal token verificato**, invariante 1: non cambia. Cambia soltanto come il token stabilisce
  l'account (UC 0117).
- **Filtro riga per riga sull'account**: resta su `membership` e su tutto ciò che è di account. L'identità
  **non** porta il filtro, perché non è dell'account: è l'unica entità di piattaforma con dati personali
  diretti, e va trattata con la stessa cura del catalogo (nessuna interrogazione dell'identità da un
  percorso di account senza passare per un'appartenenza).
- **Nessun percorso di account espone le altre appartenenze** di una persona.
- **Prova di separazione dedicata**: la stessa identità, presente in due account, non raggiunge i dati
  dell'uno dall'altro; l'esportazione chiesta in un account non contiene nulla dell'altro.

## 9. Requisiti di test

- **Unità**: le regole di ciclo di vita (uscita da un account, ultima appartenenza, identità senza
  appartenenze).
- **Integrazione**: creazione di due appartenenze per la stessa identità; rifiuto della seconda
  appartenenza allo stesso account; uscita da un account che lascia intatta l'altra appartenenza.
- **Migrazione**: su una copia dei dati di oggi, ogni utente diventa una identità più una appartenenza,
  con lo stesso indirizzo, lo stesso ruolo, lo stesso stato — e nessuna riga persa. Collaudo con conteggi
  a confronto, non a occhio.
- **Sicurezza, la prova che conta**: la stessa identità in due account non vede nulla dell'uno dall'altro,
  in lettura, in scrittura e in esportazione. Va nella sede delle prove di separazione già esistenti, non
  in un file nuovo.
- **Conformità**: cancellare l'account A non cancella l'identità se esiste l'appartenenza a B; cancellare
  l'ultima appartenenza rende l'identità cancellabile. Due prove, entrambe necessarie.
- **Percorsi end-to-end**: nessuno proprio (storia di modello dati); arrivano con UC 0117 e UC 0118. Nel
  registro di copertura esente come *senza superficie*.

## 10. Riferimenti & Definition of Done

- **Riferimenti**: [V2__core_domain.sql](../../../../services/core/src/main/resources/db/migration/V2__core_domain.sql)
  (il vincolo di oggi e il suo commento), [handler.py](../../../../infra/modules/platform_shared/lambda/pre_token_gen/handler.py)
  (la funzione che assume una sola riga), [PlatformWriter.java](../../../../services/auth/src/main/java/app/appgrove/auth/PlatformWriter.java)
  (chi scrive utenti oggi), [UserDirectory.java](../../../../services/auth/src/main/java/app/appgrove/auth/local/UserDirectory.java)
  (il fornitore locale, che va tenuto in parità), [docs/02 §14](../../../02-auth-sicurezza.md).
- **Definition of Done**:
  1. identità e appartenenza esistono come entità distinte, con l'unicità spostata dove deve stare;
  2. la migrazione converte i dati di oggi senza perdite, provata con conteggi a confronto;
  3. la stessa persona può avere due appartenenze, e la separazione dei dati è provata;
  4. nessun percorso di account rivela le altre appartenenze di una persona;
  5. il manifesto dei dati e il registro dei trattamenti riflettono il nuovo assetto di titolarità;
  6. `docs/02 §14` è aggiornato: la decisione «1 utente → 1 account» è superata, e si dice da cosa;
  7. `run-tests.sh backend compliance` verde.

## Punti aperti / decisioni differite

- ~~**Nome dell'entità**~~ — **chiuso** (change `0088`): `platform.identity`. È l'identità di **accesso**
  alla piattaforma — indirizzo di posta e identificativo di autenticazione — non l'anagrafica della
  persona, e il nome deve dire quello.
- **Preferenza di lingua**: portata sull'identità (è della persona, non dell'account). Se un giorno un
  account volesse imporre la lingua ai propri membri, servirebbe un valore per appartenenza che vince su
  quello dell'identità. Non ora. Proprietario: UC 0060.
- ~~**Assistenza e biglietti**~~ — **verificato in implementazione** (change `0088`): nessuna
  riassegnazione necessaria. Il biglietto è dell'account in cui è stato aperto e lì resta; il recapito di
  chi l'ha aperto si legge passando dall'appartenenza di **quell'** account all'identità, quindi una
  persona che appartiene anche altrove non porta con sé i propri biglietti. Proprietario: UC 0041 se un
  giorno servisse spostarli.
- **Piano per liberi professionisti** che lavorano per più clienti: questa storia lo rende possibile, ma il
  prodotto non lo prevede. È materia commerciale, non tecnica. Proprietario: docs/_BACKLOG.md.
- **Scelta fra più appartenenze**: la change `0088` prende in modo deterministico la **più antica**, sia
  nel fornitore locale sia nella funzione che compone il token, e lo dichiara nel commento di entrambe.
  È un ripiego, non un criterio di prodotto: il criterio vero — quale account è **attivo** in una
  sessione e come si cambia — è di **UC 0117**, che deve sostituire quel ripiego, non affiancarlo.
- **Stato «appartenenza in attesa di accettazione»**: previsto dalla tabella §7 ma **non** introdotto
  dalla change `0088`. Oggi l'attesa è già rappresentata dalla riga di invito (`platform.invitations`,
  stato `pending`) e un secondo modo di dire la stessa cosa, che nessun percorso usa, sarebbe soltanto
  un'ambiguità in più. Proprietario: **UC 0118**, che possiede i percorsi d'ingresso.
- **Riuso di un indirizzo dopo la cancellazione di un'identità**: l'unicità sull'identità è
  **incondizionata** (vale anche sulle righe cancellate), come era su `platform.users`. Chi cancella
  un'identità e poi si ripresenta con lo stesso indirizzo trova ancora un rifiuto di indice. Non è un
  problema nuovo e non lo si è risolto qui per non anticipare i messaggi d'ingresso. Proprietario:
  **UC 0118**.
- **Limitazione del trattamento su un'identità senza appartenenze**: la console risponde
  «non trovato», perché il tenant di contesto dell'atto si legge dall'appartenenza più antica. È uno
  stato inutilizzabile (chi non ha appartenenze non ottiene un token valido), quindi limitarlo non
  avrebbe effetto osservabile — ma se un giorno servisse limitare una persona *prima* che entri in un
  account, la console va estesa. Proprietario: **UC 0034**.
- **Rimozione fisica di `platform.users`**: la tabella resta come rete di ritorno, fredda. Chi la
  rimuove lo fa con una migrazione dedicata, dopo un periodo di esercizio. Tracciato in
  [docs/_BACKLOG.md](../../../_BACKLOG.md).
- **Il ri-seed delle appartenenze stampa un errore e non aggiorna le righe** (rilevato dalla change
  `0089`, UC 0117, fuori dal suo perimetro). `dev/seed/seed.sql` inserisce le appartenenze con
  `ON CONFLICT (id) DO UPDATE`, ma su una banca dati già seminata la seconda esecuzione viola l'indice
  unico parziale `ux_membership_tenant_identity` prima di arrivare al ramo di conflitto sull'`id`, e
  l'esecuzione stampa `ERROR: duplicate key value violates unique constraint` proseguendo. Effetto
  pratico: nulla si rompe (le righe ci sono già e sono corrette) ma il seme **non è più idempotente**
  su quella tabella e l'errore resta a video in ogni avvio dello stack. La correzione (arbitro di
  conflitto coerente con l'indice vero, oppure `insert ... where not exists`) appartiene a chi possiede
  il seme: **UC 0011**, con questa storia come causa.
