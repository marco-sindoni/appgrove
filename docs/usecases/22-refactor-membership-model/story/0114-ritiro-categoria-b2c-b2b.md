# UC 0114 — Ritiro della categoria B2C/B2B (`App.user_model`)

**Area**: 22-refactor-membership-model · **Fase**: evo · **Stato**: 🟢 scritto (da implementare)
**Epica**: [E22.4 Dentro le applicazioni](../epic/E22-04-app-e-industrializzazione.md)
**Dipendenze**: UC 0099 (il varco per applicazione sostituisce i ruoli di piattaforma sugli endpoint), UC 0112 (il generatore viene rifatto lì)
**Sostituisce**: UC 0072 dell'epica 14, che su questa categoria fondava l'intera semantica della gestione utenti
**Piano di lavoro**: [task/0114](../task/0114-ritiro-categoria-b2c-b2b.md)
**Ultimo aggiornamento**: 2026-08-19

## 1. Obiettivo / Scope

Togliere dal prodotto la categoria **B2C/B2B** delle applicazioni — il campo `App.user_model` con
l'enumerazione `single_user` / `multi_user` — perché il nuovo modello di appartenenza la rende **falsa** e
il suo nome era già **ambiguo**.

**Incluso**: rimozione del campo dalla banca dati, dal codice del core, dai listini, dal generatore di
applicazioni, dalla console di amministrazione, dall'interfaccia dichiarata e dal client generato;
allineamento della documentazione **dove** significava «modello utenti dell'applicazione»; nota
chiarificatrice sugli use case storici che portano l'etichetta nel titolo.

**Escluso**: la distinzione che **sopravvive** — l'ambito dei dati dell'applicazione — che è
[UC 0115](0115-ambito-dati-applicazione.md); ogni uso di «B2C/B2B» in senso **giuridico o commerciale**
(appgrove titolare verso i consumatori, responsabile verso i clienti-azienda), che resta valido e non si
tocca.

## 2. Perché va ritirata: tre ragioni indipendenti

1. **«Applicazione privata» non è più una proprietà dell'applicazione.** L'accesso è un'entità di
   piattaforma (UC 0098): qualunque applicazione può essere data a più persone con ruoli diversi. Dare
   l'applicazione delle fatture al proprio contabile come `viewer` non è un abuso del modello: è ciò per
   cui il modello esiste.
2. **Non tocca più il prezzo.** I posti si pagano una volta a livello di piattaforma (UC 0102): il listino
   di un'applicazione non dipende più dal numero di persone.
3. **Il suo unico uso funzionale scompare.** Oggi il campo serve **solo** al generatore, per scegliere i
   ruoli degli endpoint prodotti: `single` → `OWNER, ADMIN`; `multi` → anche `MEMBER`
   ([context.mjs](../../../../tools/new-application/lib/context.mjs)). Con il varco per applicazione
   (UC 0099) gli endpoint non si proteggono più coi ruoli di piattaforma, e quella scelta non esiste più.

Dopo il refactor il campo sarebbe **obbligatorio, dichiarato e mai letto**, con un nome che descrive una
cosa non più vera: esattamente il tipo di difetto che non diventa mai rosso.

## 3. L'ambiguità dell'etichetta, che è un problema a sé

«B2C/B2B» significa **due cose diverse** nel progetto, e questo ritiro serve anche a separarle:

| Senso | Dove vive | Sorte |
|---|---|---|
| **Modello utenti dell'applicazione** (una persona contro molte) | `App.user_model`, `docs/01` §31 e §80, `docs/02`, UC 0051/0052/0054/0060, generatore | **si ritira** |
| **Ruolo giuridico e segmento di mercato** (titolare verso i consumatori, responsabile verso i clienti-azienda) | `docs/13`, manifesti dati, registro dei trattamenti, note fiscali e di revisione legale, runbook violazioni | **resta, intatto** |

Che l'ambiguità sia concreta lo dimostra il fatto che `docs/13` §34–35 ha già dovuto scrivere una nota
difensiva: «il "modello B2B" (multi-utente, pricing a tier) è una **feature di prodotto**, NON un ruolo
giuridico». Quella nota resta utile, ma da qui in avanti difende un solo significato invece di due.

Prova ulteriore che la confusione era già entrata nel codice: i collaudi della console di amministrazione
usano come valori finti `'b2b'` e `'b2c'`, che **non esistono** nell'enumerazione reale.

## 4. Flusso principale

Non c'è un flusso d'uso: è una rimozione. L'effetto osservabile è **nessun cambiamento** per chi usa il
prodotto — e questa è la verifica principale della storia.

1. La colonna e l'enumerazione spariscono; nessun comportamento cambia, perché nessun comportamento ne
   dipendeva.
2. La console di amministrazione perde una colonna informativa.
3. Il generatore di applicazioni perde una domanda e quattro segnaposto; chi crea un'applicazione nuova
   risponde invece alle domande del copilota dei ruoli (UC 0112) e a quella sull'ambito dei dati
   (UC 0115), che hanno conseguenze vere.
4. I listini delle applicazioni perdono un campo obbligatorio.

## 5. Flussi alternativi / edge / errori

- **Edge — informazione che si vuole conservare**: «questa applicazione è pensata per lavorare in
  squadra» è un'informazione **commerciale** utile. Non torna come campo: si scrive nella
  **descrizione** dell'applicazione, che è già multilingua ed è già mostrata nel catalogo e nella
  vetrina. Una frase in cinque lingue costa meno di un campo da mantenere allineato.
- **Edge — use case storici col nome nel titolo** (per esempio `0054-app2-b2b-via-new-application.md`):
  **non** si rinominano. Il numero è l'identità dello use case, il nome del file è cosmetico, e
  rinominarlo romperebbe i collegamenti nelle change già in `main`, che sono la memoria del progetto. Si
  aggiunge una **nota in testa** che dice come va letta l'etichetta dopo l'epica 22.
- **Errore — un listino che dichiara ancora `userModel`**: il caricamento deve **ignorare** il campo
  sconosciuto senza fallire durante il periodo di transizione, e i file del repository vanno puliti nella
  stessa change. Un caricamento che esplode su un campo di troppo trasformerebbe una pulizia in un guasto
  all'avvio.
- **Edge — la colonna è obbligatoria in banca dati**: la migrazione la elimina; nessun dato va salvato
  altrove, perché nessuno lo consuma.

## 6. Risorse & runbook _(storia di pulizia strutturale)_

Nessuna schermata nuova. Una schermata **perde** una colonna: la tabella delle applicazioni nella console
di amministrazione.

## 7. Dati toccati

- **`platform.app.user_model`**: eliminata. Nessuna informazione da conservare: il campo non era la
  sorgente di verità di nulla.
- **Listini come codice**: `fatture.yaml`, `crm.yaml` e le quattro fixture perdono `userModel`.
- **Nessun dato personale coinvolto**, in nessun modo. Nessuna modifica ai manifesti dati né al registro
  dei trattamenti — e questo va detto esplicitamente, perché la parola «B2B» compare in quei documenti
  con l'altro significato e la tentazione di «allineare anche là» va respinta.

## 8. Permessi & gate

Nessun cambiamento di permessi. Attenzione a una conseguenza indiretta: rimuovendo da `Roles.java` delle
applicazioni le costanti dei ruoli di piattaforma, ogni endpoint che le usava **deve** già essere passato
al varco per applicazione (UC 0099). Se qualcuno restasse indietro, resterebbe **senza protezione**: la
verifica strutturale di UC 0101 (nessuna operazione di scrittura priva di dichiarazione) è il presidio che
lo impedisce, e va **verde prima** di questa storia.

## 9. Requisiti di test

- **Non-regressione, la prova che conta**: la suite intera resta verde senza modifiche di comportamento.
  Nessun collaudo nuovo verifica una funzione: questa storia rimuove.
- **Collaudi da aggiornare**: quelli della console di amministrazione che usano i valori finti `b2b`/`b2c`
  (frontend e percorso end-to-end), i collaudi del caricamento dei listini, quelli del generatore.
- **Collaudo di tolleranza**: un listino con un campo `userModel` residuo viene caricato senza errore.
- **Verifica strutturale**: nessuna occorrenza di `user_model`, `userModel`, `AppUserModel`,
  `single_user`, `multi_user` resta nel codice — ricerca esplicita da eseguire a fine lavoro, non
  fiducia.
- **Percorsi end-to-end**: nessuno proprio; esente come *senza superficie*.

## 10. Riferimenti & Definition of Done

- **Riferimenti**: [AppUserModel.java](../../../../services/core/src/main/java/app/appgrove/core/catalog/AppUserModel.java),
  [context.mjs](../../../../tools/new-application/lib/context.mjs),
  [Apps.tsx](../../../../frontend/apps/admin/src/pages/Apps.tsx),
  [docs/01-architettura.md](../../../01-architettura.md) §31 e §80,
  [docs/13-compliance-privacy.md](../../../13-compliance-privacy.md) §34–35 (l'altro significato, da non toccare).
- **Definition of Done**:
  1. la colonna, l'enumerazione e il campo nei listini non esistono più;
  2. il generatore non chiede più il modello utenti e non emette ruoli di piattaforma;
  3. la documentazione è allineata **solo** dove significava «modello utenti»; il senso giuridico è
     rimasto intatto e la cosa è verificata leggendo i file di conformità;
  4. gli use case storici hanno la nota chiarificatrice, senza rinomine;
  5. la ricerca delle cinque parole chiave non trova nulla nel codice;
  6. `./run-tests.sh` intero verde.

## Punti aperti / decisioni differite

- **Il nome «B2B» nei manifesti dati e nel registro dei trattamenti** resta con il significato giuridico.
  Se un giorno si volesse disambiguare anche là (per esempio «cliente-azienda» invece di «tenant B2B»),
  è un lavoro di conformità, non di prodotto: annotato in [docs/_REVISIONE-LEGALE.md](../../../_REVISIONE-LEGALE.md).
- **Descrizione commerciale «pensata per il lavoro di squadra»**: se serva davvero nel catalogo lo dirà
  l'uso. Nessun campo, eventualmente una frase nella descrizione. Proprietario: UC 0095.
- **`Roles.java` dei due servizi di applicazione** (da [UC 0101](0101-semantica-ruoli-viewer-editor-admin.md),
  change `0095`): dichiarano ancora i nomi di ruolo di **piattaforma** (`owner`, `admin`, `member`) usati
  da `@RolesAllowed`, e quello di `fatture` porta perfino nel commento «in B2C single-user l'utente è
  owner» — una frase che questa storia rende falsa. La riscrittura sui ruoli di applicazione non è stata
  fatta dalla change 0095 perché toglierli significa togliere `@RolesAllowed` dalle risorse, cioè
  cambiare una superficie di autorizzazione: è il ritiro dei posti, che appartiene a UC 0111. Qui resta la
  parte di **linguaggio** (il commento che nomina B2C). Proprietario: questa storia per il commento,
  UC 0111 per le annotazioni.

