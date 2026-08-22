# Change 0095 — Semantica dei tre ruoli come contratto di piattaforma verificabile

**Use case sorgente**: [docs/usecases/22-refactor-membership-model/story/0101-semantica-ruoli-viewer-editor-admin.md](../../docs/usecases/22-refactor-membership-model/story/0101-semantica-ruoli-viewer-editor-admin.md)
**Piano di lavoro**: [task/0101](../../docs/usecases/22-refactor-membership-model/task/0101-semantica-ruoli-viewer-editor-admin.md)
**Epica**: 22 — rifacimento del modello di appartenenza, sotto-epica E22.1 (Fondamenta)
**Dipendenze già in `main`**: UC 0098 (modello dati dell'accesso per applicazione, change `0091`), UC 0099
(varco riusabile `@RequiresAppRole`, change `0092`)
**Modalità**: fast (autopilot senza fermate di workflow), dichiarata dall'orchestratore `go-fast`
**Aree toccate**: `services/commons`, `services/fatture`, `services/crm`, `frontend/packages/*`,
`tools/new-application` (modelli-sorgente), `tools/scaffold-parity`, documentazione

---

## 1. Perché questa change

Il varco esiste (UC 0099) ma **non dice niente su quando usarlo**. Oggi il Mini-CRM dichiara
`viewer` sulle letture ed `editor` sulle scritture perché chi ha scritto quella change l'ha ritenuto
sensato; `fatture` non dichiara **nulla**. Due applicazioni della stessa piattaforma interpretano quindi i
tre ruoli in due modi diversi, e la terza applicazione li interpreterà in un terzo modo — senza che nulla
diventi rosso.

Questa change trasforma la semantica dei tre ruoli da *buona volontà* in **contratto verificabile**: una
regola di classificazione scritta una volta, un documento per applicazione che dichiara le proprie
operazioni con il ruolo minimo di ognuna, e un collaudo che coglie l'operazione di scrittura non protetta.

## 2. Ambito

**Incluso**

1. La **regola di classificazione** delle operazioni (la cascata a tre domande della storia §4),
   documentata dove chi scrive un servizio la cerca.
2. Il **formato del documento delle operazioni**, in `services/commons`: contratto che ogni applicazione
   realizza, leggibile da un programma.
3. La sua **realizzazione nelle due applicazioni esistenti** (`fatture`, `crm`), con le annotazioni del
   varco allineate alla classificazione.
4. Il **collaudo strutturale** che rende vero il contratto, riusabile da ogni applicazione, più
   l'auto-collaudo che dimostra che il collaudo **fallisce** davanti a una scrittura non protetta.
5. I **collaudi di integrazione** per ruolo su entrambe le applicazioni.
6. La **regola dell'interfaccia** «disabilitato con spiegazione» contro «assente»: l'involucro condiviso
   nel design system, con le sue traduzioni nelle cinque lingue.
7. I **modelli-sorgente** della skill `new-application` allineati, così che l'applicazione numero tre nasca
   col suo documento delle operazioni e non antiquata.

**Escluso** (e tracciato)

- Dove vive il ruolo → UC 0098 (fatto). Come si fa rispettare → UC 0099 (fatto).
- La **schermata «Gestione utenti»** dentro l'applicazione → UC 0111.
- Il **cablaggio dell'involucro nelle schermate**: richiede che il browser conosca il ruolo della persona
  sull'applicazione, cioè la lettura `GET /api/platform/v1/me/app-access` portata nella shell. È la
  fondazione di UC 0107 (menu, rotte e visibilità per ruolo) e resta là.
- Il **copilota** che fa la domanda di classificazione a chi crea un'applicazione → UC 0112.
- Il **ritiro dei posti** del Mini-CRM e dei nomi di ruolo di piattaforma in `Roles.java` → UC 0111 e
  UC 0114.
- Un **quarto ruolo** per poteri intermedi: la storia lo vieta finché un'applicazione reale non dimostrerà
  che tre non bastano.

## 3. La regola di classificazione (il contratto)

Ogni operazione esposta da un'applicazione riceve **una** etichetta, secondo questa cascata:

1. **Cambia dati, invia qualcosa fuori, o consuma quota?** → almeno **`editor`**. Sono le operazioni
   *dispositive*: creazione, modifica, cancellazione, invio, esportazione che genera un documento,
   importazione, cambio di stato.
2. **Governa *chi* usa l'applicazione?** → **`admin`**. Abilitazione, revoca, cambio di ruolo dentro
   quell'applicazione.
3. **Altrimenti** → **`viewer`**. Elenchi, dettagli, ricerche, riepiloghi.

Con tre chiarimenti vincolanti: l'esportazione di ciò che si vede già è una **lettura**; le **preferenze
personali** non sono dati dell'applicazione; il `viewer` vede **tutti** i dati che l'ambito
dell'applicazione gli attribuisce — nascondergliene una parte sarebbe un ruolo nuovo, non una restrizione
silenziosa.

Fuori dalla cascata esistono le operazioni **esenti dai ruoli**, e sono poche e dichiarate: i diritti
dell'interessato sui propri dati personali, e lo **stato di quota informativo** — già escluse per
costruzione dal varco (UC 0099 rese `@RequiresAppRole` volutamente *opt-in*). L'esenzione va **dichiarata
col suo motivo** nel documento dell'applicazione: è l'unico modo di distinguere «esente di proposito» da
«dimenticata».

## 4. Requisiti funzionali

### R1 — Formato del documento delle operazioni (`services/commons`)

- `AppOperationsContract`: interfaccia con `appId()` e `operations()`, sul modello di `AppDataContract`.
- `AppOperation`: record con **identificativo stabile**, **descrizione breve in italiano e in inglese**,
  la **classe della risorsa** e il **nome del metodo Java** che la realizza, e poi **o** il ruolo minimo
  **o** il motivo dell'esenzione — mai entrambi, mai nessuno dei due. Due fabbriche (`requiring`,
  `exempt`) rendono lo stato illegale non rappresentabile.
- Il riferimento a classe + metodo è ciò che permette al collaudo di legare la dichiarazione al codice
  reale, invece di confrontare stringhe di percorso.

### R2 — Il collaudo che rende vero il contratto

`AppOperationsContractVerifier` nel *test-jar* di `commons` (come `PersonalDataManifestVerifier`), invocato
da un collaudo di una riga in ogni applicazione. Verifica **tre** direzioni:

1. **Dichiarato → reale**: ogni operazione dichiarata esiste come metodo di una risorsa `@Path` e porta
   un verbo HTTP; identificativi non duplicati; descrizioni non vuote in entrambe le lingue.
2. **Reale → dichiarato**: ogni operazione esposta dall'applicazione (qualunque verbo) è dichiarata nel
   documento. È la direzione che coglie l'operazione aggiunta domani e dimenticata.
3. **Coerenza col varco**: il ruolo dichiarato coincide con quello **effettivo** dell'annotazione
   `@RequiresAppRole` (metodo, poi classe); ogni operazione **di scrittura** non esente richiede almeno
   `editor`; ogni operazione **esente** non porta l'annotazione del varco (un'esenzione protetta è un
   diritto rotto).

Più l'**auto-collaudo del verificatore** in `commons`, su risorse-campione: dimostra che una scrittura non
protetta, una scrittura con solo `viewer`, un'operazione non dichiarata e un'esenzione protetta fanno
diventare **rosso** il collaudo. Un verificatore che non ha mai fallito non è una prova.

### R3 — `fatture` allineata alla cascata

`fatture` oggi non dichiara alcun ruolo minimo. Diventa: letture (`GET` elenco e dettaglio) `viewer`,
scritture (`POST`, `PATCH`, `DELETE`) `editor`, stato di quota **esente** (informativo, come già deciso in
UC 0099). Serve anche l'infrastruttura di collaudo che il Mini-CRM ha già (finta sorgente del ruolo
qualificata come rete di sicurezza, azzeramento della copia locale fra i test), altrimenti il varco
tenterebbe una chiamata di rete verso il core e l'intera suite dell'applicazione diventerebbe rossa.

### R4 — `crm` completata

Il Mini-CRM ha già letture e scritture di contatti e interazioni. Si aggiungono le operazioni che
**governano chi usa l'applicazione** — il riquadro dei posti: lettura del riepilogo `viewer` (§6: le
sezioni di governo sono *visibili in sola lettura*), assegnazione `admin`, revoca `admin` **con rilettura
dal core** (`fresh = true`: è irreversibile). Stato di quota esente.

### R5 — Collaudi di integrazione per ruolo (entrambe le applicazioni)

Su `crm` e su `fatture`: un `viewer` legge e non scrive; un `editor` scrive; un `admin` compie anche le
operazioni di governo; le operazioni esenti passano per tutti, **anche per chi non ha alcun accesso**
all'applicazione.

### R6 — La regola dell'interfaccia: disabilitato con spiegazione ≠ assente

- `DisabledForRole` nel design system: rende il comando **presente ma disabilitato**, con la spiegazione
  al passaggio del puntatore (`title`) **e** leggibile dagli strumenti di assistenza (testo collegato via
  `aria-describedby`, `aria-disabled`, comando fuori dal percorso di tabulazione).
- Il **confronto fra ruoli** per il browser (`appRoleAtLeast`, con l'owner rappresentato come `admin`) vive
  in `@appgrove/api-client`, che possiede i tipi del contratto di piattaforma — **non** nel design system,
  che resta presentazionale e non conosce né il contratto né le traduzioni.
- Chiavi di traduzione nella sezione nuova `roles` delle cinque lingue.

### R7 — Modelli-sorgente della skill `new-application`

I modelli derivano da `fatture`: tutto ciò che R3 aggiunge all'applicazione numero uno va aggiunto anche
ai modelli, o la prossima applicazione nascerà senza documento delle operazioni. `RequiresAppRole` entra
fra le annotazioni portanti del collaudo di parità.

### R8 — Documentazione

`docs/04-services-backend.md` guadagna la sezione della cascata: è una regola di piattaforma e va dove chi
scrive un servizio la cerca.

## 5. Invarianti di piattaforma

- **Tenant dal solo JWT verificato**: nessuna operazione nuova, nessun parametro nuovo. Il ruolo continua a
  non stare nel token e a leggersi dal modello (UC 0099).
- **Filtro per tenant**: nessuna query nuova.
- **Modulo Terraform**: nessuna infrastruttura toccata.
- **Log strutturato**: il varco già registra `app_id`, ruolo richiesto e ruolo posseduto.

## 6. Dati personali

Nessun dato personale nuovo, nessuna tabella nuova, nessuna finalità nuova. Il documento delle operazioni
è codice che descrive codice. Classificazione attesa: **nessun segnale**, quindi nessun aggiornamento di
manifesti, RoPA o versione dei documenti legali.

## 7. Requisiti di test

| Livello | Cosa | Dove |
|---|---|---|
| Unità (Java) | Confronto fra ruoli e posizione dell'owner | già coperto da `AppRoleTest` (UC 0099) — nessuna duplicazione |
| Unità (Java) | Validazione di `AppOperation` (stato illegale non rappresentabile) | `services/commons/src/test/.../AppOperationTest.java` |
| Struttura (Java) | Auto-collaudo del verificatore: quattro difetti diventano rossi | `services/commons/src/test/.../AppOperationsContractVerifierTest.java` |
| Struttura (Java) | Contratto ↔ codice, per applicazione | `AppOperationsContractTest` in `fatture` e in `crm` |
| Integrazione (Java) | `viewer` legge/non scrive, `editor` scrive, `admin` governa, esenti per tutti | `AppRoleGateTest` in `fatture` (nuovo) e in `crm` (esteso) |
| Unità (frontend) | `appRoleAtLeast` con ordinamento e owner | `frontend/packages/api-client/src/app-role.test.ts` |
| Unità (frontend) | `DisabledForRole`: presente-ma-disabilitato, spiegazione al puntatore, spiegazione per gli strumenti di assistenza, e nessun involucro quando il ruolo basta | `frontend/packages/design-system/src/components/DisabledForRole.test.tsx` |
| Parità scaffolding | Modelli ↔ `fatture` | `tools/scaffold-parity` (area `tooling`) |

### Copertura end-to-end

La storia §9 lo dice già: nessun percorso proprio: i percorsi per ruolo nascono con UC 0111 e UC 0107 e
sono **posseduti da UC 0113** (che introduce `J-ROLES`). La voce di registro di UC 0101 passa quindi da
esenzione `non-implementato` a **`senza-superficie`** — con la ragione scritta per esteso: la parte visibile
di questa change è un involucro riusabile senza schermata che lo usi, e un percorso che non può cliccare
niente non è un percorso. L'involucro è coperto al livello di componente.

## 8. Definition of Done

1. La regola di classificazione è scritta e **applicata** alle due applicazioni esistenti.
2. Ogni applicazione ha il suo documento delle operazioni con il ruolo minimo di ognuna.
3. Esiste il collaudo che **coglie** una operazione di scrittura non protetta (e lo dimostra fallendo).
4. L'interfaccia distingue *disabilitato con spiegazione* da *assente*.
5. I diritti dell'interessato e lo stato di quota restano **esenti** dai ruoli, e l'esenzione è dichiarata.
6. `./run-tests.sh` **completo** verde (modalità fast), registro `decisions.json` integrale,
   `docs/testing/copertura-e2e.yaml` aggiornato e `how-to-test.md` scritta **ed eseguita** nei passi non
   visivi.
