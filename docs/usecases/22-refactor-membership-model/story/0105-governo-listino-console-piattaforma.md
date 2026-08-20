# UC 0105 — Governo del listino dei posti dalla console di piattaforma

**Area**: 22-refactor-membership-model · **Fase**: evo · **Stato**: 🟢 scritto (da implementare)
**Epica**: [E22.2 Posti a pagamento](../epic/E22-02-posti-a-pagamento.md)
**Dipendenze**: UC 0102 (listino versionato), UC 0021 (console di amministrazione), UC 0047 (skill dei cambi di prezzo)
**Piano di lavoro**: [task/0105](../task/0105-governo-listino-console-piattaforma.md)
**Prototipo**: [platform-admin.html](../prototype/platform-admin.html)
**Ultimo aggiornamento**: 2026-08-19

## 1. Obiettivo / Scope

Requisito aggiunto dallo sviluppatore: l'**amministratore di piattaforma** deve poter cambiare le tariffe
delle fasce dei posti **per tutti gli account**, con effetto dal **ciclo di fatturazione successivo**.

**Incluso**: la schermata di console per creare una nuova versione del listino; la validazione delle fasce;
l'anteprima dell'effetto su casi tipici e sugli account reali; la decorrenza e l'assenza di retroattività;
lo storico delle versioni; la traccia di controllo; l'allineamento col fornitore di pagamento.

**Escluso**: la definizione del calcolo → UC 0102; la presentazione al cliente → UC 0106; i prezzi delle
applicazioni, che restano governati dal listino come codice e dalla sua skill.

## 2. Attori & ruoli

- **Amministratore di piattaforma**: l'unico che può creare una versione del listino. Non è un utente di
  un account: è chi gestisce appgrove.
- **Sistema**: applica la versione vigente alla data, senza toccare i periodi già fatturati.
- **Owner degli account**: subisce l'effetto, e va informato (§5).

## 3. Precondizioni

- Esiste il listino versionato (UC 0102) con almeno la versione iniziale nata dal file.
- Esiste la console di amministrazione con la sua autenticazione separata e il suo ruolo dedicato.

## 4. Flusso principale

1. L'amministratore apre «Posti — listino» nella console.
2. Vede la **versione vigente** (fasce e tariffe), la sua decorrenza, e lo **storico** delle versioni
   precedenti.
3. Crea una **nuova versione** partendo da una copia della vigente: modifica le tariffe, eventualmente i
   confini delle fasce, e scrive una **nota** che dice perché (campo obbligatorio: fra sei mesi nessuno
   ricorderà il motivo).
4. Il sistema **valida**: fasce contigue senza sovrapposizioni né buchi, prima fascia che parte da 1,
   ultima senza limite superiore, tariffe non negative, valuta unica.
5. Il sistema mostra **l'anteprima dell'effetto**: su una tabella di casi tipici (3, 4, 8, 12, 55, 120
   posti) e sul **portafoglio reale** — quanti account cambiano importo, quanto incassa la piattaforma
   prima e dopo, quale è il rincaro massimo subito da un singolo account. Senza questo, cambiare una
   tariffa è un atto alla cieca.
6. L'amministratore indica la **decorrenza**, che non può essere anteriore all'inizio del ciclo successivo,
   e conferma.
7. Il sistema salva la nuova versione, registra la traccia di controllo, e **non tocca nulla d'altro**: gli
   importi cambiano al rinnovo di ciascun account, quando il calcolo (UC 0102) leggerà la versione vigente
   a quella data.

## 5. Flussi alternativi / edge / errori

- **Errore — fasce incoerenti**: rifiuto con l'indicazione precisa (sovrapposizione, buco, ordine).
- **Errore — decorrenza troppo vicina**: rifiuto. La regola: la decorrenza deve essere posteriore alla
  fine del ciclo in corso **più lungo** fra gli account attivi, o comunque a un margine dichiarato (per
  esempio trenta giorni). Serve anche a lasciare il tempo di informare i clienti.
- **Edge — rincaro**: se la nuova versione **aumenta** quanto un account paga, la comunicazione preventiva
  non è cortesia ma probabilmente un obbligo contrattuale. L'anteprima segnala il numero di account
  coinvolti; la comunicazione stessa **non** è automatica in questa storia (punto aperto), ma il sistema
  la rende possibile fornendo l'elenco.
- **Edge — versione futura già programmata**: si può sostituire o annullare finché non è decorsa. Le
  versioni **già decorse** sono immutabili.
- **Edge — errore umano su una versione già decorsa**: non si modifica; si crea una versione correttiva
  con la sua decorrenza. Se il danno è già fatto, si corregge nella fatturazione, non nel listino.
- **Edge — allineamento col fornitore di pagamento**: i prezzi vivono anche presso il fornitore. La nuova
  versione va **sincronizzata** con la stessa regola valida per le applicazioni: un prezzo nuovo è un
  prezzo **nuovo** presso il fornitore, mai la modifica di uno esistente. Se la sincronizzazione fallisce,
  la versione resta creata ma **non decorre**, e l'amministratore vede lo stato «in attesa di
  sincronizzazione».

## 6. Schermate & stati _(console di amministrazione)_

Sezione «Posti — listino», tre blocchi:

1. **Vigente**: tabella delle fasce con tariffa, decorrenza, nota, e il numero di account a cui si applica
   per fascia — informazione utile a capire dove sta il portafoglio.
2. **Nuova versione**: la stessa tabella, modificabile; validazione mentre si scrive; nota obbligatoria;
   scelta della decorrenza; **anteprima dell'effetto** in due parti (casi tipici e portafoglio reale);
   conferma con riepilogo esplicito di che cosa succederà e a quanti.
3. **Storico**: elenco delle versioni con decorrenza, autore, nota, e la possibilità di **vedere** una
   versione passata (mai di modificarla).

Stati: caricamento, validazione in errore (per campo), anteprima in calcolo, versione programmata (con
possibilità di annullarla), versione in attesa di sincronizzazione, esito riuscito.

## 7. Dati toccati

- **`platform.seat_pricing_version`** e **`platform.seat_pricing_band`** (UC 0102): questa storia le
  scrive. Le versioni decorse sono immutabili per regola applicativa e per collaudo.
- **Traccia di controllo**: `seat_pricing.version_created` con autore, decorrenza, e il confronto sintetico
  fra vecchie e nuove tariffe. È materia di prezzi: la traccia serve.
- **Nessun dato personale.** L'anteprima sul portafoglio reale lavora su **numeri aggregati**; l'elenco
  degli account coinvolti mostra identificativi e nomi di account, non persone.

## 8. Permessi & gate

- **Solo il ruolo di amministratore di piattaforma**, che è separato dai ruoli degli account e non dà
  accesso ai dati dei clienti.
- **Nessuna retroattività**: presidio nel servizio, non solo nell'interfaccia. Una versione con decorrenza
  passata è rifiutata.
- **Immutabilità delle versioni decorse**: nessuna via di scrittura le raggiunge.
- **Doppia conferma** per la creazione, con riepilogo dell'effetto: è un atto che tocca tutti i clienti.

## 9. Requisiti di test

- **Unità**: validazione delle fasce (sovrapposizione, buco, prima fascia, ultima aperta, tariffe
  negative).
- **Unità**: selezione della versione per data, con una versione futura programmata presente.
- **Integrazione**: creazione di una versione futura → gli importi degli account **non** cambiano subito;
  superata la decorrenza, il calcolo usa le nuove tariffe.
- **Integrazione**: tentativo di modificare una versione decorsa → rifiutato.
- **Integrazione**: sincronizzazione col fornitore fallita → versione creata ma non decorsa.
- **Percorso end-to-end di livello 2** nella console: creazione di una versione con anteprima e storico.
- **Prova di sicurezza**: un utente di un account (anche owner) non raggiunge questa interfaccia.

## 10. Riferimenti & Definition of Done

- **Riferimenti**: [AdminResource.java](../../../../services/core/src/main/java/app/appgrove/core/platform/AdminResource.java),
  [UC 0021](../../06-frontend/0021-console-admin-spa.md), [UC 0047](../../10-skills-tooling/0047-skill-pricing-change.md)
  per il principio di immutabilità dei prezzi; [prototipo della console](../prototype/platform-admin.html).
- **Definition of Done**:
  1. l'amministratore crea versioni, non modifica tariffe;
  2. la decorrenza non è mai retroattiva e le versioni decorse sono immutabili;
  3. l'anteprima dice quanti account cambiano importo e di quanto, prima di confermare;
  4. la nota è obbligatoria e lo storico è consultabile;
  5. la sincronizzazione col fornitore è condizione per la decorrenza;
  6. `run-tests.sh backend frontend` verde.

## Punti aperti / decisioni differite

- **Comunicazione preventiva ai clienti in caso di rincaro**: obbligo probabile secondo le condizioni
  contrattuali, e comunque buona pratica. Questa storia fornisce l'elenco degli account coinvolti; l'invio
  è da progettare (e da far verificare in revisione legale). Proprietario: registro di revisione legale
  e UC 0106.
- **Margine minimo fra creazione e decorrenza**: proposta trenta giorni. Da confermare con le condizioni
  contrattuali. Proprietario: questa storia.
- **Tariffe negoziate per singolo account**: fuori scope, come in UC 0102.
- **Chi può fare questa operazione quando ci sarà più di un amministratore di piattaforma**: oggi
  l'amministratore è uno; se diventassero più di uno servirebbe un secondo paio di occhi (approvazione a
  due). Annotato in [docs/_BACKLOG.md](../../../_BACKLOG.md).
