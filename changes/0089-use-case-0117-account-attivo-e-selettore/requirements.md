# Change 0089 — Account attivo nella sessione e selettore (UC 0117)

**Use case sorgente**: [docs/usecases/22-refactor-membership-model/story/0117-account-attivo-e-selettore.md](../../docs/usecases/22-refactor-membership-model/story/0117-account-attivo-e-selettore.md)
**Piano di lavoro**: [task/0117](../../docs/usecases/22-refactor-membership-model/task/0117-account-attivo-e-selettore.md)
**Sotto-epica**: [E22.5 — Identità e appartenenze](../../docs/usecases/22-refactor-membership-model/epic/E22-05-identita-e-appartenenze.md)
**Storia precedente**: UC 0116, change [0088](../0088-use-case-0116-identita-e-appartenenze/)
**Modalità**: fast (nessun gate di workflow; suite completa verde obbligatoria prima del commit)
**Aree toccate**: `services/commons`, `services/auth`, `services/core`, `infra` (funzione che compone il token), `frontend/apps/backoffice`, `tools/platform-e2e`, `docs`

## 1. Perché

Dopo UC 0116 una persona può appartenere a più account, ma il token non sa più *per conto di chi* sta
agendo: la change 0088 ha lasciato un **ripiego dichiarato** — con più appartenenze si prende la più
antica — scritto a voce alta nel commento delle due implementazioni che compongono i claim (il fornitore
locale in Java e la funzione Python del fornitore reale). Questa change sostituisce quel ripiego con
l'**account attivo della sessione**, e dà alla persona il modo di cambiarlo.

È il punto in cui si stabilisce *chi sei* e *per conto di chi lavori*: un errore qui non è un difetto di
interfaccia, è un varco fra due aziende. Da qui l'ossessione della change per una cosa sola: **il valore
conservato non è creduto**.

## 2. Che cosa entra

1. **Dove vive l'account attivo** — una colonna `active_membership_id` (annullabile) sull'identità di
   piattaforma. **Nessun attributo nuovo presso il fornitore di identità**: il gruppo di utenti Cognito
   non dichiara attributi personalizzati e aggiungerne uno per via dichiarativa rischia di ricreare il
   gruppo, cioè di perdere gli utenti. La ragione va scritta nel commento della colonna.
2. **La funzione che scegli l'account** — una funzione **pura**, scritta una volta e usata da entrambi i
   fornitori, con questa tabella di casi:

   | Appartenenze attive | Valore conservato | Esito |
   |---|---|---|
   | nessuna | qualunque | nessun claim (a chiusura in caso di dubbio, come oggi) |
   | una sola | qualunque, anche assente | quella, **ignorando** il valore conservato |
   | più di una | corrisponde a una di esse | quella |
   | più di una | assente, o non corrisponde | nessun claim + esito tipizzato «scegli l'account» |

   L'appartenenza si **riverifica** a ogni creazione di token: il valore conservato è un *suggerimento*,
   e non basta a produrre un claim.
3. **Parità fra i due fornitori** — la stessa tabella di casi, attuata in Java e in Python, con commento
   incrociato e con **gli stessi collaudi** su entrambe.
4. **Il cambio di account** — `GET /api/platform/v1/me/memberships` (le appartenenze della persona che
   chiama, con il nome dell'account e quale è attivo) e `POST /api/platform/v1/me/active-account`
   (scrive il nuovo account attivo). Rifiuto **404** se l'account chiesto non corrisponde a
   un'appartenenza attiva della persona — non 403: non si rivela l'esistenza dell'account. Il cambio
   **non restituisce token**: il rinnovo passa dal percorso esistente, così il claim si costruisce in un
   solo posto.
5. **Traccia di controllo** del cambio, con soli identificativi opachi, con la stessa conservazione a 12
   mesi delle altre prove di audit nella banca dati.
6. **Il selettore nell'interfaccia** — nella **barra laterale, sotto il marchio**. Il nome dell'account
   attivo è **sempre** visibile; il selettore **non viene reso affatto** con una sola appartenenza (non
   «reso disabilitato»). Il cambio chiama l'interfaccia, rinnova il token e **ricarica l'applicazione**.
7. **Avviso «l'account è cambiato in un'altra scheda»** — se l'account del token in uso non è più quello
   attivo conservato, l'interfaccia lo dice e invita a ricaricare.
8. **Nessuna etichetta di ruolo nell'interfaccia di piattaforma** (§4.6 della storia): il ruolo è per
   applicazione e si legge dove è vero.

## 3. Che cosa resta fuori

- **La schermata per scegliere l'account quando non si ha una sessione.** L'esito «scegli l'account» è
  implementato, tipizzato e a chiusura, con un messaggio comprensibile al posto di «credenziali non
  valide»; la *superficie* per rispondere a quella richiesta senza avere un token appartiene a
  **UC 0118**, che è la storia che rende raggiungibile il caso (oggi nessun percorso di prodotto crea una
  seconda appartenenza). Costruirla qui richiederebbe rendere navigabile una sessione priva del claim
  dell'account, cioè allargare il raggio d'azione del percorso più delicato del prodotto per un caso che
  nessuno può raggiungere. Rimando tracciato in UC 0118 e nei punti aperti di UC 0117.
- **La durata del token e il ritardo massimo di una revoca**: proprietario UC 0017 (qui si pretende solo
  che il legame sia scritto).
- **La rilettura dal core per le operazioni che modificano dati** (revoca a effetto immediato):
  UC 0099.
- **Menu e visibilità per ruolo** (UC 0107): il collaudo end-to-end usa la differenza *già vera* oggi
  (le voci Account/Billing/Members sono di owner e admin), non anticipa 0107.

## 4. Invarianti da rispettare

- **Invariante 1 — account solo dal token verificato**: non si tocca. Cambia la funzione che *calcola*
  il claim, non chi se ne fida. Nessun percorso accetta un account da body o parametri: il
  `POST /me/active-account` riceve un account **candidato** e lo accetta solo se corrisponde a
  un'appartenenza attiva della persona del token.
- **A chiusura in caso di dubbio**: ogni incertezza produce l'assenza del claim.
- **Filtro per account** su ogni lettura di account. Le due letture che attraversano gli account («a
  quali account appartiene questa persona?») restano dichiarate ed esplicitamente motivate, come già
  fatto in UC 0116.
- **Log strutturato** con `tenant_id`, `user_id`.

## 5. Requisiti di test

- **Unità della funzione pura**, i quattro casi della tabella, su **entrambe** le attuazioni (Java e
  Python), con la stessa tabella di casi.
- **Sicurezza, la prova che conta**: `active_membership_id` scritto a mano su un'appartenenza revocata o
  di un altro account → **nessun token** con quel claim. Da provare manomettendo la colonna.
- **Nessuna regressione per chi ha una sola appartenenza**: il caso del cento per cento degli utenti di
  oggi, provato da un collaudo.
- **Integrazione del cambio**: cambio → rinnovo → claim nuovo; il token precedente resta valido per il
  suo account fino alla scadenza (comportamento **atteso**, scritto in un collaudo).
- **Interfaccia**: con una sola appartenenza il selettore **non è nel documento reso** (non «è
  nascosto»).
- **Percorsi end-to-end**: `J-ACCOUNT-SWITCH` (suite di piattaforma, stack vero) e
  `L2-ACCOUNT-SWITCH` (livello 2, browser vero e servizi simulati), registrati in
  [copertura-e2e.yaml](../../docs/testing/copertura-e2e.yaml); lo use case 0117 passa da esenzione
  `non-implementato` a use case **con superficie**.

## 6. Definition of Done

1. l'account attivo vive in banca dati e nessun attributo nuovo tocca il gruppo di utenti;
2. l'appartenenza è riverificata a ogni creazione di token, e il valore conservato non è creduto;
3. con una sola appartenenza nulla cambia per l'utente, provato da un collaudo;
4. il selettore compare solo quando serve e il cambio rinnova il token e ricarica l'applicazione;
5. i due fornitori di identità producono claim identici, provato dalla stessa tabella di casi;
6. i percorsi end-to-end del cambio di account sono nel registro di copertura;
7. `./run-tests.sh` (suite completa, senza parametri) **verde**;
8. `decisions.json` integrale, `how-to-test.md` scritto, rimandi tracciati negli use case proprietari.
