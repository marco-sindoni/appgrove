# Log di implementazione — Change 0096: «Members» come elenco unico di persone

**Branch**: `change/0096-use-case-0100-sezione-members`
**Use case**: [0100](../../docs/usecases/22-refactor-membership-model/story/0100-sezione-members-elenco-unico.md)
· piano di lavoro [task/0100](../../docs/usecases/22-refactor-membership-model/task/0100-sezione-members-elenco-unico.md)
**Modalità**: **fast** (autopilot senza gate di workflow — vedi `decisions.json` voce 1)
**Aree**: `services/core`, `frontend/` (backoffice, i18n, api-client), `docs/`

## Che cosa è cambiato, e perché

La schermata «Members» era ancora costruita sul modello vecchio: una colonna del **ruolo** e **due**
tabelle — le persone e gli inviti in attesa. Due elenchi della stessa cosa, che obbligavano chi guardava a
sommare a mente per sapere quante persone avesse nel proprio gruppo di lavoro; e una colonna che, dopo
UC 0098, ripeteva «Membro» su tutte le righe tranne una.

Ora è il **registro delle persone** dell'account: chi c'è, in che stato è, su quante e quali applicazioni è
abilitato, da quando fa parte del gruppo. Al posto della colonna del ruolo c'è l'informazione che quella
colonna nascondeva.

### Servizio (`services/core`)

- **Gestione riservata all'owner.** `UserResource` (elenco, lettura per identificativo, modifica, uscita) e
  `InvitationResource` (invio, elenco, revoca) passano da `@RolesAllowed({owner, admin})` a
  `@RolesAllowed(owner)`. Le operazioni su `/users/me` restano aperte a chiunque appartenga a un account:
  sono i propri dati. La tolleranza per i token già emessi che portano `admin` è quindi **ritirata qui**, in
  anticipo su UC 0113 — dove resta il ritiro generale, con la sua data (rimando scritto nella storia 0113).
- **Il ruolo esce dal contratto dell'invito**, in entrambe le direzioni: `CreateInvitation` ha il solo campo
  `email`, `InvitationView` non espone più il ruolo. Un `role` mandato da un chiamante vecchio è ignorato e
  **non concede nulla**: l'invito nasce sempre `member`. La colonna `invitations.role` sopravvive (è
  `NOT NULL` senza valore predefinito, e il suo valore è quello con cui nasce l'appartenenza
  all'accettazione), ma il costruttore dell'entità non la fa più scegliere.
- **Le applicazioni per persona.** `UserView` porta `apps` — elenco di `{appId, app, role, implicit}` — e
  `joinedAt`. Per l'owner l'elenco sono le applicazioni **a cui l'account ha diritto**, marcate `implicit`
  e senza ruolo: il suo accesso è implicito e non ha righe di permesso (UC 0098 §5). Il numero in colonna è
  la lunghezza dell'elenco: un campo solo, perché due rappresentazioni della stessa verità divergono e poi
  nessuno sa quale credere.
- **Costo della lettura.** L'aiutante `AccountApps` si costruisce **una volta per richiesta**: una lettura
  di tutte le righe di accesso dell'account (già filtrata per account dal discriminatore), una del catalogo
  per tradurre l'identificativo dell'applicazione nel suo nome, e — solo se nell'elenco c'è un owner — una
  dei diritti dell'account. Il numero di interrogazioni **non cresce** con il numero di persone.
- `apps` è valorizzato solo nelle letture di governo (elenco e dettaglio) e resta assente su `/users/me`,
  che è la lettura più calda dell'applicazione.

### Interfaccia (`frontend/`)

- **`MembersPage` riscritta**: una tabella con sei colonne (indirizzo, nome, stato, applicazioni, nel
  gruppo dal, azioni), l'owner in testa, gli inviti in attesa **nelle stesse righe** con lo stato «Invito in
  attesa» e la scadenza accanto. La colonna delle applicazioni è un comando che apre il dettaglio **sul
  posto**, in sola lettura, con la frase che dice dove si cambia il ruolo (la schermata dove si cambia è
  UC 0111 e non esiste ancora: rimando scritto là).
- **`roster.ts`**: la fusione delle due letture è una funzione **pura** fuori dal componente, con le regole
  dell'elenco (ordinamento, traduzione degli stati, righe intoccabili) e sette collaudi di unità.
- L'**invito** chiede solo l'indirizzo, con la riga che spiega l'assenza del selettore — la sparizione più
  visibile, e quella che senza spiegazione si legge come un difetto.
- La **rotta** `/members` passa a `requireRole('owner')`; la guardia `requireAnyRole`, rimasta senza
  consumatori, è stata rimossa. Le letture di persone e inviti si abilitano solo per l'owner, anche nel
  cruscotto (UC 0097) che mostra gli stessi due numeri: a chi non le può leggere si omette la riga.
- **Cinque lingue** riscritte nella sezione `members`: chiavi nuove (elenco, applicazioni, stati, le due
  spiegazioni) e chiavi morte **rimosse** invece di lasciate orfane.
- **Contratto rigenerato** nello stesso commit: spec OpenAPI del core (prodotto dal build Quarkus) e tipi
  del client (`schema.ts`), con `UserAppView` esposto da `contract.ts`.

## Collaudi

| Livello | Che cosa prova |
|---|---|
| Integrazione core — `MembersRosterApiTest` (nuovo, 4 casi) | L'owner vede i **diritti dell'account** (confrontati con la loro API, non con un numero fisso); il raggruppamento regge su cinque persone e nove righe di permesso; chi non è abilitato a nulla ha l'elenco vuoto; il dettaglio ha la stessa forma della riga; `/users/me` non porta le applicazioni. |
| Integrazione core — `RolesTest` | Il collaudo che provava che un `admin` **può** invitare è stato **sostituito** da tre che provano che non può (invito, elenco persone, elenco inviti). |
| Integrazione core — `InvitationLifecycleTest` | «il ruolo owner non è invitabile» sostituito da «un ruolo nel corpo non concede nulla»: `201`, nessuna chiave `role` nella risposta, valore memorizzato `member`. |
| Unità frontend — `roster.test.ts` (nuovo, 7 casi) | Fusione, ordinamento, stati, applicazioni, righe intoccabili, il caso «due owner», il primo caricamento senza dati. |
| Componente — `MembersPage.test.tsx` (riscritto, 10 casi) | Una sola tabella; le sei intestazioni esatte e l'**assenza** della colonna del ruolo; i tre casi della colonna delle applicazioni; il dettaglio in sola lettura; l'invito che manda solo l'indirizzo; le due collisioni lecite; la revoca; i comandi disabilitati; accessibilità. |
| End-to-end L2 — `members.spec.ts` (riscritto) | `L2-MEMBERS` sul flusso nuovo, corpo della richiesta d'invito verificato. |

**Copertura end-to-end (UC 0094)**: scelta «coprire ora». Nel registro `docs/testing/copertura-e2e.yaml`
UC 0100 entra fra gli use case con superficie, la sua esenzione `non-implementato` esce, e `L2-MEMBERS`
elenca 0100 fra i suoi use case. Il percorso di piattaforma `J-MEMBERS` gira sulla schermata nuova senza
modifiche, perché già selezionava per riga e per cella.

**`run-tests.sh`**: nessuna modifica necessaria — nessun modulo aggiunto o rimosso, nessun comando di
collaudo cambiato. Verificato esplicitamente.

**Suite completa** `./run-tests.sh` (senza parametri): **verde** — è la contropartita obbligatoria della
modalità fast, che sostituisce il consenso umano al commit.

## Gate privacy (UC 0031)

Eseguito. Lo scanner segnala **due** segnali, entrambi falsi positivi con il loro motivo: `Invitation.role`
non è un campo nuovo (cambia solo il valore iniziale) e `UserResource.ownerApps` è un campo privato di un
aiutante per-richiesta, non un'entità. Il segnale che lo scanner non può vedere — l'API mostra all'owner
quali applicazioni una persona usa e con quale ruolo — è stato valutato a parte: è il dato di
`platform.app_access.identity_id`, già dichiarato con quella finalità, mostrato allo stesso titolare che
l'ha creato. **Classificazione MINOR**: nessuna categoria nuova, nessuna finalità nuova, nessuna base
giuridica nuova, nessun destinatario nuovo. Nessun aggiornamento di manifesto, nessuna rigenerazione della
RoPA, nessun potenziale responsabile esterno.

## Guida di collaudo manuale

`how-to-test.md` scritta **ed eseguita** nei passi non visivi (§1, §2, §3, §6.1). L'esecuzione ha prodotto
**cinque correzioni alla guida** e **nessun difetto di prodotto**. La quinta merita una riga: le etichette
dei passi visivi erano scritte in italiano, mentre l'interfaccia parte in **inglese** perché le persone del
seme hanno l'inglese come lingua — un passo visivo non si esegue, ma le sue etichette si verificano contro
le stringhe delle cinque lingue, e una guida che cita parole che a schermo non compaiono è inservibile.
Restano allo sviluppatore i passi visivi (§4 la schermata, §5 la rotta e il cruscotto, §6.2 le cinque lingue
a schermo). Dettaglio nella guida e in `decisions.json` (voci 26 e 27).

## Rimandi tracciati

- **storia 0104** — lo stato «in cessazione» e la sua azione: oggi l'appartenenza ha due soli stati e la
  cessazione nasce con la logica dei posti;
- **storia 0111** — il dettaglio delle applicazioni diventerà un collegamento verso la gestione utenti
  dell'applicazione;
- **storia 0113** — il ritiro generale della tolleranza `admin` (qui anticipato solo su persone e inviti) e
  la rimozione della colonna `invitations.role`;
- **UC 0033** — come si mostra a schermo una sospensione per limitazione del trattamento (art. 18) senza
  dire più del dovuto: oggi le due sospensioni non sono nemmeno distinguibili nel contratto, ed è una
  classificazione su dati personali che non spetta a un agente;
- **`docs/_BACKLOG.md` + storia 0100** — l'**offerta di rimandare** un invito già in attesa (§5 della
  storia) è implementata a metà: il secondo invito non si crea, ma il comando per rispedire il collegamento
  non c'è, perché il token grezzo esce dal servizio una sola volta e rimandare è **contratto nuovo**, fuori
  dai requisiti scritti di questa change. La voce di backlog dice dove andrà (la riga dell'invito
  nell'elenco unico, accanto a «Revoca») e le due trappole da non sbagliare.

## Note per chi rivede

- **Cambio di contratto** verso il frontend: il corpo dell'invito perde `role`, la vista dell'invito perde
  `role`, `UserView` guadagna `apps` e `joinedAt`. Consumatore unico è il backoffice, aggiornato nello
  stesso commit.
- Chi ha in mano un token coniato con `admin` e senza `owner` **perde la gestione dei membri subito**, non
  alla data del ritiro generale della tolleranza. È il punto della storia, e sta scritto nella 0113.
- 27 decisioni in `decisions.json`, tutte marcate `(autopilot)` tranne la prima (la modalità).
