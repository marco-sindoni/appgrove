# Change 0042: App #2 (B2B multi-utente) «Mini-CRM» generata con `new-application`

**Branch**: `change/0042-use-case-0054-app2-b2b-crm`
**Aree**: `services/crm` (nuovo), `services/core`, `services/commons`, `frontend`, `infra`, `tools/new-application`, `tools/compliance`, `docs`
**Data**: 2026-07-24
**Autore**: Platform Engineering (modalità autopilot)
**Use case sorgente**: [docs/usecases/11-apps/0054-app2-b2b-via-new-application.md](../../docs/usecases/11-apps/0054-app2-b2b-via-new-application.md)
**Tocca dati personali?**: **Sì** — l'app tratta contatti di terzi immessi dal tenant. Si applica il varco privacy/RoPA di step-03 (UC 0031) e il controllo `@PersonalData`↔manifesto dei test backend (UC 0030).

## Problema / Obiettivo

Oggi il marketplace ha **una sola app** (`fatture`: business-to-consumer, un utente per account) e una skill di
scaffolding (`new-application`) **mai usata per davvero**. Finché non nasce una seconda app con caratteristiche
diverse dalla prima, tre cose restano affermazioni non verificate:

1. che la skill generi un'app **funzionante e già conforme**, e non solo un'app *uguale a `fatture`*;
2. che i percorsi **multi-utente** (più persone nello stesso account, con ruoli) reggano fuori dai test;
3. che la quota **a giacenza** (`stock`) — un tetto sullo stato attuale, non sul consumo del mese — funzioni.
   Il contratto esiste in `commons` da UC 0027, ma **nessuna app lo usa**: è coperto solo da test, senza un
   consumatore reale. In particolare il **blocco del downgrade** è logica reale e testata che a runtime riceve
   un uso corrente **vuoto**, quindi oggi non blocca nulla (punto aperto dichiarato di UC 0028).

Obiettivo: far nascere l'app #2 «Mini-CRM» **interamente dalla skill**, con posti (`seats`) come metrica a
giacenza realmente applicata, e correggere nel **generatore** — non nella copia generata — le lacune che
l'esercizio farà emergere.

Risultato osservabile a fine change: `./dev.sh services` mostra un terzo servizio; il Mini-CRM si apre dal
backoffice, gestisce contatti e interazioni isolati per account, assegna e revoca posti con il tetto del piano
applicato davvero, e un tentativo di scendere a un piano con meno posti di quelli occupati viene **rifiutato con
la spiegazione di come rientrare**.

## Scope

### 1. Generazione dell'app (skill `new-application`)

- Identificativo `crm`, nome visibile «Mini-CRM», **modello utente multi-utente**, porta locale **8082**
  (debug 5007), schema `app_crm`, rotta `/api/crm/v1/*`, metrica di quota `seats`.
- L'app nasce dal generatore deterministico; **niente file scritti a mano che il generatore sa produrre**.
  Se l'output è sbagliato si corregge il modello-sorgente e si rigenera.
- L'istanza del modulo Terraform `microsaas_app` è creata dal generatore delegando a `infra/scripts/service-add`.
- Nessun cablaggio manuale in `app-start.sh`, `dev/Caddyfile`, `tools/smoke`, flussi di integrazione continua:
  la scoperta automatica deve derivare tutto dalle proprietà del servizio. Se non ci riesce, si corregge la
  **scoperta**, non lo script.

### 2. Correzioni al generatore emerse dall'esercizio (proprietà di UC 0046)

- Nuova opzione **`--quota-nature flow|stock`**: il modello-sorgente deve generare il conteggio corretto
  (giacenza attuale senza finestra temporale, per `stock`) e il file di listino coerente (`type: stock`, nessuna
  `window`). Oggi il modello genera sempre il conteggio a consumo e delega a un commento l'adattamento a mano:
  è il modo sicuro per far nascere sbagliata la prossima app a giacenza.
- Ogni altra divergenza fra ciò che la skill promette e ciò che fa va corretta **nella skill o nel generatore**
  (compresi i riferimenti a comandi inesistenti nelle istruzioni della skill), oppure registrata in
  [docs/_PARITA-SCAFFOLD.md](../../docs/_PARITA-SCAFFOLD.md) con la motivazione.

### 3. Dominio reale del Mini-CRM (sostituisce il dominio segnaposto)

- **Contatto**: nome, recapito email e telefono, organizzazione, **stato di avanzamento** della trattativa
  (nuovo → qualificato → in trattativa → vinto / perso), note brevi, chi lo ha creato.
- **Interazione**: annotazione datata collegata a un contatto (tipo, data, testo libero).
- Elenco, ricerca per nome/organizzazione, filtro per stato, creazione, modifica, cancellazione logica.
- Tutto rigorosamente per account: nessun dato di un account è visibile o modificabile da un altro.

### 4. Posti (`seats`) come quota a giacenza

- Un **posto** è un utente dell'account abilitato a *questa* app, registrato nello schema dell'app. Non è
  l'utente dell'account in sé: lo stesso account può avere dieci utenti e comprare tre posti di Mini-CRM.
- Owner e admin dell'account **assegnano e revocano** i posti dalla schermata «Membri» dell'app.
- L'assegnazione passa dal contratto di quota già esistente (`checkAndReserve("seats")`): superato il tetto,
  rifiuto **429** con un rimedio leggibile («posti esauriti: libera un posto o passa a un piano superiore»).
- La revoca di un posto libera immediatamente la giacenza (è la differenza sostanziale con la metrica a consumo).
- Il tetto arriva dalla **proiezione locale degli entitlement** introdotta da UC 0046. Vietato reintrodurre la
  lettura sincrona verso il core sul percorso caldo: la rete di sicurezza esistente resta l'unica eccezione, e
  resta com'è.
- Chi non ha un posto **non accede all'app**, anche se è membro dell'account: rifiuto **403** con spiegazione.
  Owner e admin conservano sempre l'accesso all'amministrazione dei posti, anche a posti esauriti — altrimenti
  un account pieno non potrebbe più liberare nulla.

### 5. Ruoli dentro l'account

- Ruoli `owner`, `admin`, `member` letti **solo dal token verificato**, mai da parametri della richiesta.
- Gestione posti: solo `owner` e `admin`. Contatti e interazioni: tutti e tre i ruoli.
- Gli inviti restano quelli del core (UC 0013/0059): l'app **non** reimplementa il flusso di invito.

### 6. Blocco del downgrade reso reale

- L'app pubblica il proprio **uso a giacenza** (posti occupati, per account) sul bus di messaggi già esistente,
  ogni volta che cambia.
- Il core lo materializza in un read-model e lo passa a `TierChangePolicy.evaluateDowngrade`, che oggi riceve
  una mappa vuota.
- Effetto atteso: chiedere il passaggio a un piano con meno posti di quelli occupati viene **rifiutato** con il
  rimedio; liberati i posti in eccesso, la stessa richiesta viene accettata e schedulata a fine periodo.
- Ricaduta dichiarata: questo chiude, **limitatamente al varco del downgrade**, il punto aperto «gate stock del
  downgrade contro l'uso reale» di UC 0028.

### 7. Modulo frontend

- Modulo caricato su richiesta dentro il backoffice, registrato nell'App Registry e visibile quando l'account ha
  il diritto d'uso; abilitato nello stub locale.
- Schermate: elenco contatti (con stati vuoto / in caricamento / errore), scheda contatto con interazioni,
  creazione e modifica, e **Membri** con posti occupati su totali.
- Riuso del sistema di design e delle stringhe tradotte esistenti; nessun testo cablato nei componenti.

### 8. Conformità

- **Manifesto dati** dell'app compilato per davvero (italiano e inglese), coerente con le annotazioni
  `@PersonalData` del servizio.
- **Esportazione e cancellazione** dei dati dell'app funzionanti (contatti, interazioni, posti): la cancellazione
  deve cancellare, non pseudonimizzare.
- Registro dei trattamenti aggiornato di conseguenza.

### 9. Listino prezzi — **da confermare (materia dello sviluppatore)**

Proposta portata all'approvazione, **non ancora scritta**:

| Livello | Posti | Prova | Prezzo mensile | Prezzo annuale |
|---|---|---|---|---|
| `free` | 2 | — | 0 | — |
| `team` | 10 | 14 giorni | 19,00 € | 190,00 € (due mesi in omaggio) |

Metrica `seats`, natura **a giacenza**. Stato del listino `active` come per `fatture`: con `inactive` l'app non
supererebbe il varco «app abilitata» e non sarebbe utilizzabile nemmeno in locale. La vendita reale resta comunque
bloccata a monte dall'attivazione del fornitore di pagamento (#14). Due livelli servono anche a **poter provare**
il blocco del downgrade.

### 10. Dati personali — **da confermare (materia dello sviluppatore)**

Proposta portata all'approvazione, **non ancora scritta nel manifesto**:

- **Interessati**: persone di contatto delle organizzazioni clienti del tenant (dati di terzi, immessi dal tenant).
- **Categorie**: identità (nome), recapiti (email, telefono), organizzazione di appartenenza, stato commerciale
  della relazione, annotazioni testuali.
- **Base giuridica**: contratto (art. 6.1.b) verso il tenant; il tenant è titolare, appgrove **responsabile**
  (postura #13 A2, accordo sul trattamento incorporato).
- **Conservazione**: fino a cancellazione da parte del tenant; cancellazione integrale alla chiusura dell'account,
  coerente con quanto la cancellazione fa davvero.
- **Nessuna categoria particolare dell'art. 9** prevista — con **un'avvertenza esplicita**: il campo nota
  dell'interazione è **testo libero**, quindi è un punto d'ingresso non presidiato per dati che nessuno ha
  classificato. Va dichiarato come tale nel manifesto, con l'indicazione che l'informativa del tenant titolare
  deve coprirlo.
- **Classificazione del cambio**: **MINOR** — nessuna nuova finalità, nessuna nuova base giuridica, nessun nuovo
  responsabile esterno rispetto a quanto già dichiarato per `fatture`; cambia l'insieme dei dati trattati, non la
  natura del trattamento. Da riconfermare con lo scanner dei segnali privacy in fase di implementazione.

## Fuori scope

- **Implementare o riscrivere la skill `new-application`** (UC 0046, già fatta): qui si *usa*, e si correggono
  solo le lacune che l'uso fa emergere.
- **Pubblicare la landing** dell'app: qui solo la bozza; la pubblicazione è di UC 0057.
- **Sincronizzare i prezzi** verso il fornitore di pagamento: di UC 0022, e comunque bloccata da #14.
- **Mostrare il consumo di quota nel pannello di fatturazione** («4 posti su 10» nel portale cliente): resta di
  UC 0028. Qui si costruisce solo la sorgente d'uso di cui quel pannello avrà bisogno.
- **Console di amministrazione** (UC 0021) e qualunque sua evoluzione.
- **Applicare infrastruttura in cloud**: la change genera e valida l'istanza del modulo Terraform, non la applica
  (l'applicazione è della pipeline, #07 G18).
- **Funzioni avanzate di un CRM** — importazione contatti, calendario, attività assegnate, rapporti commerciali:
  sono prodotto, non validazione della skill. Se emergessero come necessarie, vanno tracciate come punto aperto
  di UC 0054, non aggiunte qui.

## Criteri di accettazione

- [ ] `./dev.sh services` elenca `crm` con porta 8082 e schema `app_crm`, **senza** che nessuno script di avvio,
      il proxy locale o i flussi di integrazione continua siano stati modificati a mano.
- [ ] L'app è stata generata dal generatore; l'eventuale differenza fra output e codice finale è solo dominio reale
      e decisioni dei due co-piloti, mai correzione di difetti del modello lasciata nella copia.
- [ ] Il generatore accetta `--quota-nature stock` e produce un servizio che conta la **giacenza attuale** e un
      listino che dichiara `type: stock`; il collaudo di parità dei modelli-sorgente è verde.
- [ ] Un utente di un account non vede né modifica contatti, interazioni o posti di un altro account (verificato
      da test, non a mano).
- [ ] Con il tetto a N posti: la (N+1)-esima assegnazione è rifiutata con **429** e un rimedio leggibile; revocato
      un posto, l'assegnazione successiva riesce **senza attendere alcuna finestra temporale**.
- [ ] Un membro dell'account senza posto riceve **403** sulle rotte del dominio; owner e admin mantengono l'accesso
      alla gestione dei posti anche a posti esauriti.
- [ ] La richiesta di passaggio a un piano con meno posti di quelli occupati è **rifiutata con il rimedio**;
      dopo aver liberato i posti in eccesso la stessa richiesta è accettata. Verificato da test end-to-end, non
      solo dalla logica pura.
- [ ] Il manifesto dati è compilato in italiano e inglese, il controllo `@PersonalData`↔manifesto è verde, e
      l'esportazione e la cancellazione dei dati dell'app coprono contatti, interazioni e posti.
- [ ] Il modulo Mini-CRM compare nel backoffice per un account che ha il diritto d'uso e le sue schermate hanno
      stato vuoto, di caricamento e di errore.
- [ ] `./run-tests.sh` è verde su tutte le aree, e include la nuova app senza modifiche manuali oltre a quelle
      previste dal generatore.

## Invarianti appgrove toccati

Tutti e quattro, più quello specifico dello scaffolding.

- **Identificativo dell'account solo dal token verificato** — l'app lo legge dal contesto del chiamante ereditato
  da `commons`; posti, contatti e interazioni non accettano mai un identificativo di account dal corpo o dai
  parametri della richiesta. Va coperto da un test che tenta l'inganno e si aspetta il rifiuto.
- **Filtro per account su ogni query** — ereditato dal discriminatore delle entità di `commons`; le nuove tabelle
  (`contact`, `interaction`, `seat`) sono tutte con identificativo di account e indicizzate su di esso.
- **Modulo Terraform `microsaas_app`** — l'infrastruttura dell'app è un'**istanza del modulo**, prodotta dallo
  script proprietario del formato. Nessun blocco scritto o modificato a mano.
- **Registrazione strutturata** — ogni riga di registro dell'app porta account, app e utente; i rifiuti di quota
  e di ruolo devono essere riconoscibili nei registri.
- **Diritti d'uso letti dalla proiezione locale** (UC 0046) — nessuna nuova chiamata sincrona verso il core sul
  percorso caldo. L'unico canale nuovo verso il core è **asincrono** e in direzione opposta (l'app dichiara il
  proprio uso).

## Requisiti di test

Oltre a quanto implicano le modifiche:

- **Isolamento fra account**: due account, stessi dati, verifica che nessuna rotta dell'app faccia trapelare nulla.
- **Tentativo di forzatura dell'account**: richiesta che dichiara un account diverso da quello del token → rifiuto.
- **Matrice dei ruoli**: per ogni rotta, quali dei tre ruoli passano e quali no, incluso il caso «membro senza posto».
- **Quota a giacenza**: assegnazione fino al tetto, rifiuto al superamento, **liberazione immediata** dopo revoca,
  e prova esplicita che *non* esiste una finestra temporale che azzera il conteggio (è l'errore classico che
  rende un tetto a giacenza inutilizzabile).
- **Blocco del downgrade**: prova end-to-end che parte dall'uso reale dei posti e arriva al rifiuto del cambio
  piano, e non si limita alla logica pura già coperta da UC 0028.
- **Percorso B2B end-to-end** (nel browser): invito di un collega, accettazione, assegnazione del posto, uso come
  membro, banner dei posti.
- **Conformità**: parità delle lingue del manifesto, controllo annotazioni↔manifesto, esportazione e cancellazione
  che coprono davvero le tre tabelle.
- **Parità dei modelli-sorgente**: verde dopo le modifiche al generatore.

## Valutazione di impatto

| Area | Impatto |
|---|---|
| Breaking change | No — l'app #1, il core e il frontend esistenti mantengono il comportamento attuale; il varco del downgrade passa da inefficace a efficace, che è una correzione dichiarata, non una rottura di contratto. |
| Contratto cross-area | Sì — (1) frontend ↔ nuove API `/api/crm/v1/*`; (2) nuovo canale asincrono app → core per l'uso a giacenza; (3) servizio ↔ infrastruttura, nuova istanza del modulo `microsaas_app`. |
| Version bump | minor — nuova app e nuove funzionalità, nessuna rimozione. Manifesto privacy: cambio **MINOR** (da riconfermare con lo scanner in fase di implementazione). |
