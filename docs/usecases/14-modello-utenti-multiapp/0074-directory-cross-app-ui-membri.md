# UC 0074 — Directory cross-app + UI "Membri" ripensata per-app

**Area**: 14-modello-utenti-multiapp · **Fase**: evo · **Stato**: 🗄️ **SUPERATA dall'epica 22** — archivio della decisione precedente, **non da implementare**
**Dipendenze**: UC 0059 (Gestione membri & inviti UI backoffice), UC 0013 (Accounts/Users/Invitations + core REST API), UC 0072 (Distinzione B2C/B2B a livello app + semantica gestione utenti), UC 0073 (Invito utenti per-app con "posti" come metrica quota stock)
**Fonte**: R3 (Tabella residui _INDEX.md) · docs/_BACKLOG.md §"Modello di gestione utenti — tenant-level vs per-app (B2B/B2C)"
**Ultimo aggiornamento**: 2026-07-26


> **Questa storia è superata e non va implementata.** L'epica **22 — rifacimento del modello di appartenenza**
> (change `0087`) adotta il modello **opposto** a quello di questa epica: appartenenza **centralizzata** sulla
> piattaforma, ruolo della persona **sull'applicazione**, posti **di piattaforma** a listino unico — cioè
> esattamente l'opzione che l'epica 14 registrava come scartata. Lo sviluppatore ha cambiato direzione, e la
> decisione è registrata in `changes/0087-epica-22-refactor-membership-model/decisions.json`.
>
> **Sostituita da**: [0100](../22-refactor-membership-model/story/0100-sezione-members-elenco-unico.md) (elenco unico) e [0111](../22-refactor-membership-model/story/0111-schermata-gestione-utenti-app.md) (gestione utenti nell'applicazione).
>
> Il documento resta come **archivio**: dice cosa si era pensato e perché non si è fatto. Cancellarlo farebbe
> perdere il ragionamento, che è la parte che serve a chi un giorno riaprirà la questione.

## 1. Obiettivo / Scope

Due cose collegate. Primo: una **directory di comodo cross-app** (cioè "fra più app") — al momento di invitare qualcuno
su un'app B2B, mostrare a chi invita le persone **già presenti su altre app B2B dello stesso account**, così da
ri-invitare in un clic chi esiste già senza ridigitarne l'email. Secondo: il **ridisegno della schermata "Membri"** del
backoffice, oggi pensata a livello di account (UC 0059, Gestione membri & inviti UI backoffice), in modo che diventi
**per-app** — completa per le app B2B, assente o ridotta per le app B2C (UC 0072, Distinzione B2C/B2B a livello app +
semantica gestione utenti).

Punto fermo, ereditato dal backlog: la directory è **solo una comodità di lettura**. **Non** rende l'appartenenza né la
quota centrali: il diritto d'accesso e il limite dei posti restano **per-app** (UC 0073, Invito utenti per-app con
"posti" come metrica quota stock). Suggerire una persona già nota **non** consuma un posto e **non** dà accesso: l'invito
per-app resta necessario e passa dal gate posti.

**Incluso**: la directory cross-app in sola lettura per il ri-invito rapido; il ridisegno della schermata "Membri"
per-app (B2B) e la sua forma ridotta/assente per B2C; stati, testi e messaggi. **Escluso**: la definizione B2C/B2B →
UC 0072; il meccanismo e il prezzo dei posti → UC 0073.

## 2. Attori & ruoli

- **Proprietario/Amministratore** (*owner*/*admin*) di un'app B2B: usa la schermata "Membri" e la directory per invitare.
- **Membro** (*member*): compare in elenco; non gestisce e, di norma, non vede la directory.
- **Proprietario di un'app B2C**: vede la forma ridotta della sezione (solo sé stesso, sola lettura).
- **Sistema**: compone la directory leggendo, entro l'account, gli utenti delle **altre app B2B**.

## 3. Precondizioni

- Nucleo utenti/inviti disponibile — UC 0013 (Accounts/Users/Invitations + core REST API).
- La schermata "Membri" esiste già — UC 0059 (Gestione membri & inviti UI backoffice) — e va evoluta, non creata da zero.
- La distinzione B2C/B2B per app è in vigore — UC 0072 — e il gate posti è disponibile — UC 0073.
- L'account ha almeno un'app B2B perché la directory cross-app abbia contenuto.

## 4. Flusso principale

1. Owner/admin apre "Membri" di un'app **B2B**.
2. La schermata mostra: intestazione con l'indicatore posti "usati / totali" (da UC 0073); la **lista membri dell'app**
   (attivi) e la lista **inviti pendenti dell'app**; il pulsante "Invita".
3. Alla pressione di "Invita" si apre il form (email + ruolo) affiancato dalla **directory cross-app**: l'elenco, in
   sola lettura, delle persone già presenti su **altre app B2B** dello stesso account, con indicazione di dove sono già
   presenti (es. "già in: app A, app B").
4. Chi invita può **selezionare una persona dalla directory** (che precompila email/nome) oppure digitare un indirizzo
   nuovo.
5. In entrambi i casi si crea un **invito per questa app**, che passa dal **gate posti** di UC 0073 e occupa un posto.
   La directory **non** aggira il gate: se i posti sono pieni, l'invito è bloccato anche per una persona già nota.
6. L'invitato accetta col flusso lato invitato (UC 0017, Flussi auth UI). Log strutturato con `tenant_id`, `app_id`,
   `user_id`.
7. Su un'app **B2C** i passi 2–6 non esistono: la sezione mostra la forma ridotta (solo il proprietario) o è assente.

## 5. Flussi alternativi / edge / errori

- **Edge — directory vuota**: se l'account non ha altre app B2B (o nessun utente altrove), la directory non compare o
  mostra uno stato vuoto con invito a digitare l'email.
- **Edge — persona già membro di questa app**: chi è già membro dell'app corrente non va riproposto come "da invitare";
  eventualmente compare come già presente, non selezionabile per un nuovo invito.
- **Edge — persona già invitata (pendente) su questa app**: mostrata come pendente, non ri-invitabile.
- **Errore — posti pieni**: selezionare una persona dalla directory con posti esauriti produce lo stesso rifiuto
  tipizzato (*problem+json*) di UC 0073; la directory non è una scorciatoia sul limite.
- **Edge/privacy — cosa mostra la directory**: la directory espone il **minimo** utile al ri-invito (nome ed email
  delle persone dell'account su altre app B2B) e **solo** entro lo stesso account. Non deve mai mostrare persone di
  altri account né dati oltre il necessario (minimizzazione).
- **Edge — app B2B senza posti liberi ma con directory piena**: la directory resta visibile in lettura, ma ogni tentativo
  di invito è bloccato; il messaggio spiega di liberare un posto o cambiare fascia.

## 6. Schermate & stati

Questo use case ridisegna la schermata "Membri" di UC 0059 (Gestione membri & inviti UI backoffice), spostandola da
account a **app**.

- **Contesto app nel titolo**: la schermata dichiara **di quale app** sono i membri (es. "Membri — app A"), perché
  l'appartenenza è per-app.
- **App B2B — vista completa**:
  - intestazione con **indicatore posti** "Posti usati X su Y" (UC 0073);
  - **tabella membri dell'app** (email, nome, ruolo, stato);
  - **tabella inviti pendenti dell'app** (email, ruolo, scadenza, azione "revoca");
  - **form invito** con, accanto, la **directory cross-app** (elenco selezionabile "già presenti su altre tue app",
    con etichette di provenienza), in sola lettura;
  - pulsante "Invita" disabilitato quando i posti sono pieni, con spiegazione.
- **App B2C — vista ridotta/assente**: nessuna directory, nessun invito; al più una riga con il proprietario in sola
  lettura e la nota "app a utente singolo" (UC 0072).
- **Stati**: caricamento (membri, inviti, directory), vuoto (solo proprietario; directory vuota), errore (per ciascuna
  lista, con messaggio tipizzato), successo (dopo invito/revoca, con aggiornamento dell'indicatore posti).
- **Copy**: italiano e inglese; nessuna sigla non spiegata. Conferme esplicite per le azioni distruttive (revoca invito,
  rimozione membro), come già in UC 0059.

## 7. Dati toccati

- **`platform.users`** e **`invitations`**: la schermata li **legge filtrati per app** (membri e inviti dell'`app_id`
  corrente); la **directory** li legge, sempre entro l'account, per le **altre app B2B** (unione delle persone
  presenti altrove, in sola lettura).
- **`App.user_model`**: determina se la sezione è completa (B2B) o ridotta/assente (B2C) — UC 0072.
- **`app_tier.limits`**: fonte dell'indicatore posti mostrato in intestazione — UC 0073.
- **Dati personali** (email/nome dei membri e delle persone in directory): trattamento **già dichiarato** in UC 0013
  (Accounts/Users/Invitations + core REST API). Ai fini del manifesto:
  - *categoria*: dati di contatto/identificativi (email, nome);
  - *finalità*: agevolare il ri-invito di persone già presenti nell'account su altre app B2B (comodità operativa),
    oltre alla gestione membri della singola app;
  - *base giuridica*: esecuzione del contratto con l'account titolare;
  - *conservazione*: legata all'appartenenza; la directory non conserva dati propri, **rilegge** dati già esistenti.
  Nessun nuovo trattamento: la directory è una **vista di comodo** su dati già trattati, sempre entro lo stesso account,
  con minimizzazione di ciò che mostra.

## 8. Permessi & gate

- **`tenant_id` solo dal token verificato**: la directory legge **esclusivamente** dentro l'account del claim
  verificato; mai da corpo o parametri. È il presidio che impedisce alla directory di diventare un varco cross-account.
- **Filtro riga per riga**: sia la vista membri per-app sia la directory portano `WHERE tenant_id = :tid`; la directory
  aggiunge il filtro "app B2B dell'account diverse da quella corrente".
- **Gate posti per-app resta sovrano**: la directory è sola lettura e **non** consuma posti né concede accesso; l'invito
  passa comunque dal gate stock di UC 0073.
- **Ruoli**: `owner`/`admin` vedono e usano directory e invito; `member` no.
- **Gate di modello**: la vista completa esiste solo se l'app è B2B (UC 0072); su B2C non c'è directory.
- **Minimizzazione**: la directory espone solo i campi necessari al ri-invito, mai altro.

## 9. Requisiti di test

- **Unità**: costruzione della directory = unione delle persone su altre app B2B dell'account, escluse quelle già
  membri/invitate dell'app corrente.
- **Integrazione** (con database reale, tipo Testcontainers): un invito da directory crea un invito per-app che passa
  dal gate posti; a posti pieni è rifiutato anche partendo dalla directory.
- **Isolamento fra account**: la directory dell'account A **non** contiene mai persone dell'account B (test di sicurezza
  esplicito).
- **Comportamento per modello**: aprendo un'app B2C non compaiono directory né invito; aprendo un'app B2B compaiono.
- **End-to-end** (Playwright): flusso completo "apri Membri di app B2B → Invita → seleziona dalla directory → invito
  creato → indicatore posti aggiornato"; e il caso ridotto per un'app B2C.
- **Privacy/minimizzazione**: la directory non espone campi oltre nome/email.
- Verde su `run-tests.sh` per le aree toccate (backend, frontend) prima del merge.

## 10. Riferimenti & Definition of Done

- **Fonte**: R3 della tabella dei residui in `_INDEX.md`; sezione dedicata di `docs/_BACKLOG.md`.
- **Use case sorelle**: [UC 0072 (Distinzione B2C/B2B a livello app + semantica gestione utenti)](0072-distinzione-b2c-b2b-livello-app.md),
  [UC 0073 (Invito utenti per-app con "posti" come metrica quota stock)](0073-invito-utenti-per-app-posti-quota.md).
- **Definition of Done**:
  1. La schermata "Membri" è per-app: completa per B2B, ridotta/assente per B2C;
  2. la directory cross-app suggerisce, in sola lettura ed entro l'account, le persone già presenti su altre app B2B;
  3. il ri-invito dalla directory passa sempre dal gate posti e non concede accesso da solo;
  4. l'isolamento fra account è verificato da un test di sicurezza dedicato;
  5. il manifesto dati resta coerente (nessun nuovo trattamento; minimizzazione);
  6. `run-tests.sh` verde sulle aree toccate.

## Punti aperti / decisioni differite

- **Direzione preferita, non ancora decisa**: la directory cross-app e la "Membri" per-app sono la **direzione
  preferita** dell'epica 14, **non** una decisione presa; vanno confermate nella **sessione dedicata** di piattaforma
  richiesta dall'utente dopo UC 0028.
- **Opzione scartata**: la directory è esplicitamente pensata **senza** rendere centrale l'entitlement o la quota — è
  l'anti-tesi dell'opzione di **gestione utenti centralizzata di piattaforma**, che l'utente ha **scartato**. La
  directory dà la comodità del ri-invito **senza** i costi di un listino "posti" centrale.
- **Portata della directory**: se includere anche le sole app B2B a cui l'utente-che-invita ha accesso, o tutte quelle
  dell'account, è un dettaglio da rifinire in implementazione (default proposto: tutte le app B2B dell'account, coerente
  con la titolarità dell'account).
- **Aspetti privacy fini**: confermare in revisione legale (registro di revisione pre-go-live) che mostrare, entro lo
  stesso account, nome/email di persone di altre app B2B per il ri-invito è coerente con l'informativa; la
  minimizzazione è già vincolata qui.
- **Impatto su UC 0059**: questo use case **evolve** la schermata di UC 0059 (Gestione membri & inviti UI backoffice) da
  account a per-app; quando si implementa, UC 0059 va aggiornato di conseguenza (l'epica 14 lo possiede).
- **Proprietà**: parte dell'epica 14 (decisione di piattaforma trasversale) con impatti su UC 0059, UC 0017 (Flussi
  auth UI) e UC 0013 (Accounts/Users/Invitations + core REST API).
