# UC 0100 — Sezione «Members» come elenco unico di utenti, senza ruolo

**Area**: 22-refactor-membership-model · **Fase**: evo · **Stato**: 🟢 scritto (da implementare)
**Epica**: [E22.1 Fondamenta](../epic/E22-01-fondamenta-modello-centralizzato.md)
**Dipendenze**: UC 0098 (modello dati), UC 0099 (autorizzazione), UC 0059 (schermata membri attuale, che questa storia evolve)
**Sostituisce**: UC 0074 dell'epica 14 (elenco fra applicazioni e schermata membri per applicazione)
**Piano di lavoro**: [task/0100](../task/0100-sezione-members-elenco-unico.md)
**Prototipo**: [owner.html](../prototype/owner.html), schermata «Members»
**Ultimo aggiornamento**: 2026-08-19

## 1. Obiettivo / Scope

Trasformare la schermata «Members» da luogo dove si assegnano i poteri a **registro delle persone**
dell'account: chi c'è, in che stato è, quale posto occupa, su quante applicazioni è abilitato. **Nessun
ruolo**, perché a questo livello il ruolo non esiste.

**Incluso**: la nuova forma della schermata (colonne, stati, azioni); la rimozione della colonna e del
selettore di ruolo; la colonna «applicazioni» come conteggio con dettaglio; l'invito riservato all'owner;
la restrizione della rotta al solo owner; le traduzioni nelle cinque lingue.

**Escluso**: l'indicatore dei posti e il costo dell'invito → UC 0103; la riduzione in attesa → UC 0104; la
gestione degli accessi per applicazione → UC 0111; la visibilità della voce di menu → UC 0107.

## 2. Attori & ruoli

- **Owner**: unico attore. Vede l'elenco, invita, sospende, riattiva, rimuove, e da qui raggiunge gli
  accessi di una persona.
- **Tutti gli altri**: non vedono la sezione né la sua rotta (UC 0107).

## 3. Precondizioni

- Esiste `platform.app_access` e la lettura «chi ha accesso a cosa» (UC 0098).
- La schermata di UC 0059 esiste e va **evoluta**, non ricreata:
  [MembersPage.tsx](../../../../frontend/apps/backoffice/src/pages/members/MembersPage.tsx).

## 4. Flusso principale

1. L'owner apre «Members».
2. Vede l'**elenco delle persone** dell'account con: email, nome, **stato** (attiva · invito in attesa ·
   sospesa · in cessazione), **numero di applicazioni** a cui è abilitata, data di ingresso.
3. Cliccando sul numero di applicazioni vede **quali** e con che ruolo — in **sola lettura**: si cambia
   dalla gestione utenti dell'applicazione (UC 0111). Il collegamento porta là.
4. L'owner invita una persona nuova (email); non sceglie alcun ruolo, perché il ruolo non è dell'utente.
   L'effetto economico dell'invito è di UC 0103.
5. L'owner può **sospendere** una persona (perde l'accesso a tutto, mantiene il posto), **riattivarla**,
   o **indicarla per la cessazione** (entra la logica dei posti, UC 0104).
6. Le persone con invito in attesa compaiono **nello stesso elenco**, con il loro stato, non in una
   tabella separata: sono persone dell'account che stanno arrivando, e occupano già un posto.

## 5. Flussi alternativi / edge / errori

- **Edge — la tabella oggi è doppia** (membri e inviti in attesa, due schede). Diventa **una**, con la
  colonna di stato. Motivo: erano due elenchi della stessa cosa, e chi guardava doveva sommare a mente
  per sapere quante persone ha.
- **Errore — invito a un indirizzo già presente**: messaggio chiaro «questa persona fa già parte del tuo
  gruppo di lavoro», senza creare nulla.
- **Errore — invito a un indirizzo già invitato**: si offre di **rimandare** l'invito, non si crea un
  secondo invito.
- **Edge — sospensione dell'owner**: impossibile, come la sua rimozione e la sua retrocessione.
- **Edge — persona sospesa per limitazione del trattamento** (art. 18 del Regolamento europeo): la
  causale esistente resta e va **distinta** visivamente da una sospensione amministrativa, perché non è
  una decisione dell'owner e non va rimossa per errore.
- **Edge — persona senza alcuna applicazione**: legittimo (è entrata ma non è ancora stata abilitata).
  L'elenco lo mostra come «nessuna applicazione» con un invito ad abilitarla, non come un errore.
- **Stati della schermata**: caricamento, elenco vuoto (impossibile in pratica: c'è sempre l'owner),
  errore di lettura con possibilità di riprovare, esito riuscito dopo ogni azione.

## 6. Schermate & stati

Struttura della pagina, nell'ordine in cui si legge:

1. **Intestazione**: titolo, sottotitolo che spiega il modello in una riga («le persone del tuo gruppo di
   lavoro; i permessi si assegnano dentro ogni applicazione»).
2. **Riquadro dei posti** (introdotto da UC 0103): posti usati su totali, costo del posto successivo,
   eventuale riduzione in attesa.
3. **Elenco unico delle persone** con le colonne del §4 e, per riga, le azioni ammesse.
4. **Invito**: campo per l'indirizzo e pulsante; **nessun selettore di ruolo**. È la differenza più
   visibile rispetto a oggi e va accompagnata da una riga di spiegazione, altrimenti chi conosce la
   schermata attuale penserà a un difetto.

Le azioni distruttive (rimozione, revoca dell'invito, indicazione per la cessazione) chiedono conferma
esplicita, come già oggi. Testi nelle cinque lingue del prodotto; ogni comando raggiungibile da tastiera
e descritto per gli strumenti di assistenza.

## 7. Dati toccati

- **`platform.identity` + `platform.membership`** (UC 0116): lette e scritte come `platform.users` prima del rifacimento — indirizzo e nome dall'identità, ruolo e stato dall'appartenenza — meno il ruolo, che scende a due valori (UC 0098).
- **`platform.invitations`**: letta come parte dello stesso elenco; il ruolo dell'invito **scompare**
  (era un ruolo di piattaforma che non esiste più).
- **`platform.app_access`**: letta in sola lettura per il conteggio delle applicazioni per persona.
- **Dati personali**: email e nome delle persone, **trattamento già dichiarato** in UC 0013. Nessun nuovo
  trattamento: cambia la presentazione, non le categorie né le finalità. La colonna «applicazioni»
  aggiunge una informazione di autorizzazione, non un dato personale nuovo.

## 8. Permessi & gate

- **Rotta riservata all'owner**: la guardia attuale ammette `owner` oppure `admin` e va **stretta** a
  `owner`, perché `admin` non è più un ruolo di piattaforma
  ([routes.tsx](../../../../frontend/apps/backoffice/src/routing/routes.tsx)).
- **Interfaccia del core coerente**: le operazioni su utenti e inviti passano da «owner o admin» a **solo
  owner**. La difesa vera è là, non nella guardia del frontend.
- **Account solo dal token verificato**, filtro riga per riga su ogni lettura.

## 9. Requisiti di test

- **Componente**: l'elenco unico mostra gli inviti in attesa insieme alle persone attive; nessuna colonna
  di ruolo compare; il conteggio delle applicazioni è corretto; le azioni vietate sono disabilitate.
- **Integrazione nel core**: un `member` che chiama l'interfaccia degli utenti o degli inviti riceve un
  rifiuto (prima di questa storia era ammesso se `admin`).
- **Percorso end-to-end di livello 2** su `frontend/apps/backoffice/e2e/members.spec.ts` (che esiste già e
  va riscritto): l'owner apre l'elenco, invita, vede la persona in stato «invito in attesa», la revoca.
- **Percorso di piattaforma**: parte del percorso «stessa applicazione vista dai quattro ruoli» (UC 0113).
- **Traduzioni**: nessuna chiave mancante nelle cinque lingue; il collaudo di parità delle lingue è già
  in essere.

## 10. Riferimenti & Definition of Done

- **Riferimenti**: [UC 0059](../../06-frontend/0059-gestione-membri-inviti.md) che questa storia evolve;
  [MembersPage.tsx](../../../../frontend/apps/backoffice/src/pages/members/MembersPage.tsx);
  [prototipo owner](../prototype/owner.html).
- **Definition of Done**:
  1. la schermata è un elenco unico, senza ruolo, con lo stato e il conteggio delle applicazioni;
  2. l'invito non chiede il ruolo;
  3. la rotta e l'interfaccia del core sono riservate all'owner;
  4. il percorso end-to-end esistente è aggiornato e verde;
  5. le cinque lingue sono complete;
  6. `run-tests.sh frontend backend` verde.

## Punti aperti / decisioni differite

- **Un percorso rapido «abilita su …» dall'elenco delle persone**: comodo ma duplicherebbe la gestione
  utenti dell'applicazione. Proposta: per ora solo il collegamento; si valuta dopo il primo uso reale.
  Proprietario: UC 0111.
- **Ricerca e paginazione dell'elenco**: la paginazione esiste già nell'interfaccia del core; la ricerca
  serve solo sopra qualche decina di persone. Rimandata, e annotata nella ricerca globale del backoffice
  (UC 0088).
- **Come mostrare una persona sospesa per limitazione del trattamento** senza rivelare più del dovuto:
  da rifinire con chi cura la conformità. Proprietario: UC 0033.

### Lasciato da UC 0098 (change 0091)

- **Il selettore del ruolo è già sparito** dalla schermata dei membri — dal modulo di invito e dalla riga
  della tabella — perché con due soli valori non c'era nulla da scegliere e un comando che offre un valore
  che il servizio rifiuta è un difetto. Resta a questa storia tutto il resto: la **colonna** del ruolo (oggi
  una etichetta in sola lettura), l'elenco unico con gli inviti in attesa, la colonna delle applicazioni,
  l'invito riservato all'owner e la restrizione della rotta. Le chiavi di traduzione `members.roleAdmin`,
  `members.inviteRole` e `members.changeRole` sono state rimosse dalle cinque lingue perché nessuno le usava
  più; `members.roleOwner` e `members.roleMember` restano finché la colonna esiste.
- **Il campo `role` del corpo dell'invito** è ancora nel contratto e accetta il solo valore `member`.
  Toglierlo è un cambio di contratto e appartiene a questa storia, che rifà quella schermata.
