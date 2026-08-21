# Change 0094: Tre presidi che non proteggevano — scadenza della copia dei diritti, porta di test, journey instabili

**Branch**: `change/0094-scadenza-copia-diritti-e-collaudi`
**Aree**: `services/commons` · `services/{auth,core,crm,fatture}` (configurazione di test) ·
`tools/platform-e2e` · `tools/new-application` (modelli)
**Data**: 2026-08-22
**Autore**: Platform Engineering (modalità autopilot)
**Use case sorgente**: Nessuno (change ad-hoc). I tre difetti sono tracciati in
[docs/_BACKLOG.md](../../docs/_BACKLOG.md); il primo ha come proprietario **UC 0046** (proiezione locale dei
diritti), gli altri due sono debito degli strumenti di collaudo.
**Tocca dati personali?**: No — nessun dato nuovo, nessuna nuova finalità. Il primo punto tocca una
**decisione di autorizzazione**, non un trattamento: la copia locale contiene solo identificativi già
inventariati.

## Problema / Obiettivo

Tre difetti indipendenti, un tema solo: **un presidio che non protegge**. Uno lascia entrare dove non si
dovrebbe; uno rende rossa la suite per un motivo falso; uno la rende verde per un motivo falso. Tutti e tre
sono stati trovati usando il sistema, non leggendolo.

### 1. La copia locale dei diritti d'accesso non scade mai — falla di autorizzazione

La copia locale del **ruolo** per applicazione ha una durata massima di 60 secondi, e il commento accanto alla
configurazione spiega perché è indispensabile: «è la rete che tiene quando il canale degli eventi è rotto,
l'unico caso in cui l'invalidazione, da sola, non protegge nulla». La copia locale dei **diritti d'accesso** —
più vecchia — **non ha nulla di simile**: se la riga è presente e non marcata, non viene mai riletta.

Misurato il 2026-08-22 nelle due direzioni:

- **applicazione spenta e poi riaccesa** → l'account che ha fatto una richiesta durante lo spegnimento resta
  bloccato con «abbonamento richiesto» **a tempo indeterminato**: otto letture in due minuti, sempre lo stesso
  rifiuto, la data di rinfresco immobile. Si sblocca solo marcando la riga a mano;
- **direzione opposta, più grave** → un'applicazione **spenta** resta **accessibile** a chi ha in casa una
  copia «attiva» non invalidata. Osservato dopo un riavvio del servizio di piattaforma, che rimette il
  Mini-CRM a «spento» riseminando il listino: il varco dei diritti concedeva ancora, e a fermare la richiesta è
  stato **solo** il varco del ruolo — quello che la durata massima ce l'ha.

### 2. La porta di test collide con l'applicazione #1 — suite rossa con diagnosi fuorviante

I test dei servizi girano sulla porta di test predefinita di Quarkus, **8081**, che è la porta su cui gira
l'applicazione `fatture`. Con lo stack locale acceso — cioè ogni volta che si sta collaudando a mano — le aree
`backend` e `tooling` risultano rosse, e il messaggio d'errore parla di **modelli-sorgente invecchiati**: una
diagnosi che manda a cercare nel posto sbagliato.

### 3. Un journey instabile è perdonato in silenzio — suite verde per un motivo falso

La suite di piattaforma prevede un tentativo ripetuto, e il suo orchestratore propaga tale e quale il codice di
uscita di Playwright, che vale **zero** anche quando un percorso è passato **solo al secondo tentativo**.
L'informazione esiste (il resoconto lo segnala come instabile) ma non arriva al verdetto. È il motivo per cui i
tre difetti di instabilità corretti il 2026-08-21 sono vissuti a lungo senza essere visti: la suite li
perdonava a ogni corsa, e ci si accorge di loro solo quando anche il secondo tentativo fallisce — cioè quando
il difetto è già peggiorato.

## Scope

### 1. Durata massima della copia dei diritti

- la riga della copia dei diritti acquisisce la stessa nozione di **usabilità** che ha già quella del ruolo:
  non marcata da rinfrescare **e** non più vecchia della durata massima;
- oltre la durata si **rilegge dalla fonte**; se la fonte non risponde si continua a usare **l'ultima verità
  nota**, con la misura dello scostamento già prevista oggi. Il comportamento a fonte irraggiungibile **non
  cambia**: cambia solo quando si prova a rileggere;
- la durata è **configurabile per ambiente**, **60 secondi** per default — identica a quella del ruolo;
- vale per tutti i servizi che tengono la copia, presenti e futuri: la chiave entra anche nel **modello** di
  `new-application`, altrimenti ogni applicazione nuova nascerebbe con la falla.

### 2. Porta di test libera

`quarkus.http.test-port=0` nella configurazione **di test** dei quattro servizi e nel modello di
`new-application`. Ogni suite prende una porta libera a runtime: cade la collisione con lo stack locale e
anche quella, mai notata, fra due esecuzioni Maven in parallelo.

### 3. I journey instabili rendono la suite rossa

- la suite di piattaforma produce il resoconto in **formato dati** accanto a quello leggibile;
- l'orchestratore non si fida più del solo codice di uscita: conta i percorsi **instabili** e, se ce n'è
  almeno uno, la suite è **rossa** e i percorsi vengono **nominati**;
- i tentativi ripetuti **restano**: servono come informazione diagnostica. Ciò che si toglie è il **condono**.

## Fuori scope

- **L'evento di invalidazione che nasce dal cambio di stato dell'applicazione nel listino** — la seconda causa
  del difetto 1. Dovrebbe pubblicare invalidazioni per **tutti** i tenant di un'applicazione: ampiezza diversa
  da questa change, e va deciso a sé. La durata massima rende il difetto limitato nel tempo; l'evento lo
  renderebbe immediato. Tracciato in [docs/_BACKLOG.md](../../docs/_BACKLOG.md).
- **La suite di livello 2 del frontend**: verificato che non prevede tentativi ripetuti, quindi non ha il
  difetto 3. Nessuna modifica.
- **La riorganizzazione delle porte dei servizi** (`fatture` su 8081 e le altre): funzionano, e spostarle
  toccherebbe scoperta dei servizi, proxy locale e script. Il difetto è nella porta di **test**, e là si
  corregge.
- **Il messaggio fuorviante sui «modelli-sorgente invecchiati»** in sé: con la porta libera non si presenta
  più per questa causa. Non si riscrive la diagnostica della parità dei modelli.

## Criteri di accettazione

- [ ] Superata la durata massima, la copia dei diritti viene **riletta** anche senza evento di invalidazione;
      entro la durata **non** viene riletta (il disaccoppiamento resta). Entrambi i versanti coperti da un test.
- [ ] Con la fonte irraggiungibile e una copia scaduta si continua a servire l'**ultima verità nota** — il
      comportamento di oggi non regredisce. Coperto da un test.
- [ ] La chiave della durata è presente nella configurazione dei servizi che tengono la copia **e** nel
      modello di `new-application` (la parità dei modelli resta verde).
- [ ] `./run-tests.sh backend` è **verde con lo stack locale acceso**: è la verifica diretta del difetto 2.
- [ ] Un journey che passa solo al secondo tentativo rende la suite di piattaforma **rossa**, nominando il
      percorso.
- [ ] `./run-tests.sh` completa verde.

## Invarianti appgrove toccati

- **Tenant ID solo dal JWT verificato** — la copia dei diritti è già letta con l'account preso dal token
  (fallimento chiuso se manca) e la change non tocca quel punto: si aggiunge una condizione sull'**età** della
  riga, non sulla sua appartenenza.
- **Filtro per riga sull'account** — la ricerca della riga resta vincolata ad account e applicazione.
- **Logging strutturato** — lo scostamento «servita copia vecchia» è già misurato e registrato; la change non
  riduce quella visibilità. Se nasce una registrazione nuova, porta account e applicazione.
- Modulo Terraform: non toccato.

## Requisiti di test

Oltre a quanto implicano le modifiche:

- **un test che dimostri la falla chiusa nel verso pericoloso**: un'applicazione che non è più accessibile
  deve diventare inaccessibile **allo scadere della durata**, anche se nessun evento di invalidazione è
  arrivato. È il test che sarebbe fallito prima della correzione — la prova, non il rito;
- **un test di non-regressione sul disaccoppiamento**: entro la durata, letture ripetute **non** interpellano
  la fonte. Senza questo, «rileggi più spesso» degenererebbe in «rileggi sempre», che è il difetto opposto;
- **un test sul ripiego**: copia scaduta più fonte irraggiungibile = ultima verità nota, non rifiuto;
- per il difetto 3, la verifica è **eseguita a mano** e riportata: si forza un percorso a fallire al primo
  tentativo e si controlla che la suite risulti rossa nominandolo. Un test automatico del meccanismo
  richiederebbe un doppio livello di esecuzione (una suite che esegue una suite) sproporzionato al presidio.

## Valutazione di impatto

| Area | Impatto |
|---|---|
| Breaking change | No |
| Contratto cross-area | No (nessun contratto fra aree cambia; cambia una configurazione interna ai servizi) |
| Version bump | nessuno |
