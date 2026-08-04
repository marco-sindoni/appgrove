# 0020 — Verifica della tracciabilità del pagamento

**Applicazione**: 08 — SpendGrove (`notespese`) · **Epica**: 04 — Trasferte e rimborsi chilometrici
**Storia**: `0020` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0008`, `0018`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che paga i rimborsi
> voglio sapere **prima di approvare** quali spese rischiano di non essere deducibili perché pagate in contanti
> così da non scoprire a fine anno, dal commercialista, che quella colonna di costi non vale niente e che il
> rimborso andava tassato.

**Contesto.** È la storia che giustifica da sola una parte del prezzo dell'app. Dal 2025, in Italia, le spese di
vitto, alloggio, viaggio e trasporto con taxi o noleggio con conducente sostenute in trasferta sono deducibili per
l'azienda e non tassabili per il lavoratore **solo se pagate con strumenti tracciabili**; per il trasporto l'obbligo
vale sia dentro sia fuori il Comune sede di lavoro, per vitto e alloggio solo fuori (descrizione, §2.3, fonti 3 e
4). Un foglio di calcolo non lo sa dire. Nessuno dei prodotti internazionali esaminati lo mette in prima fila. È il
punto in cui questa app conosce il mestiere del suo cliente.

## 2. Requisiti funzionali

1. **RF-1** — Ogni spesa porta il **mezzo di pagamento** scelto da un elenco chiuso (carta aziendale, carta
   personale, bonifico, assegno, altro strumento elettronico, **contanti**), con la marcatura tracciabile o no.
2. **RF-2** — Alla conferma della spesa, l'app valuta il rischio combinando categoria, mezzo di pagamento e
   contesto dentro/fuori Comune, e mostra un avviso comprensibile: **che cosa** rischia, **perché**, **che cosa si
   può fare** (per esempio farsi rifare il pagamento con carta, se si è ancora in tempo).
3. **RF-3** — Le regole di valutazione sono **dati di configurazione per giurisdizione**, non condizioni scritte nel
   codice: cambiano con le leggi e devono cambiare senza una nuova versione dell'applicazione.
4. **RF-4** — L'avviso viaggia con la spesa fino alla nota e alla schermata di approvazione, ed è conteggiato nel
   riepilogo.
5. **RF-5** — Esiste una vista «spese a rischio» per periodo, che è la domanda che il titolare fa prima della
   chiusura dell'anno.
6. **RF-6** — L'avviso **non blocca** nulla: la spesa è stata sostenuta, e il compito dell'app è farla conoscere,
   non farla sparire.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** La valutazione e la vista «a rischio» filtrano per `tenant_id` preso dal
  token verificato; dentro l'account vale la visibilità per ruolo (storia `0012`).
- **RT-2 — Interfaccia di programmazione (§2).** L'esito della valutazione è restituito nella risposta di conferma
  e da `GET /api/notespese/v1/spese?rischio=deducibilita`; errori in `application/problem+json`; definizione
  OpenAPI aggiornata.
- **RT-3 — Persistenza (§8).** Migrazione `V17__rischio_deducibilita.sql`: colonne `mezzo_pagamento`,
  `pagamento_tracciabile` (già previste dalla storia `0002`, qui valorizzate e indicizzate) e tabella degli avvisi
  di rischio con `tenant_id`, codice della regola applicata, versione della regola, colonne di controllo. La
  **versione della regola** si conserva: una spesa valutata con le regole del 2026 non deve cambiare giudizio nel
  2028.
- **RT-4 — Modulo frontend (§3, §5).** Avviso in linea nella schermata di revisione, contatore nel riepilogo della
  nota, evidenza nella schermata di approvazione, vista «spese a rischio» nella panoramica. Solo token del sistema
  di design; tema chiaro e scuro; l'avviso non è segnalato **solo** dal colore.
- **RT-5 — Cinque lingue (§4).** I testi degli avvisi passano dallo spazio-nomi `notespese` e sono presenti in
  `en, it, fr, es, de`. **Sono testi di giurisdizione**: la traduzione non basta, perché la regola italiana non
  vale in Germania. Il testo dice a quale giurisdizione si riferisce.
- **RT-6 — Varchi e quota (§6, §7).** Nessun consumo nuovo: la valutazione avviene dentro la conferma.
- **RT-7 — Esposizione conversazionale (§12).** La storia dichiara
  `verifica_deducibilita(id_spesa | periodo) → elenco dei rischi rilevati`, marcato **lettura**: è una delle
  domande per cui il livello conversazionale vale davvero («quali spese di giugno rischiano di non essere
  deducibili?»). Dipendenza: UC 0061-0063.
- **RT-8 — Dati personali (§10).** Il mezzo di pagamento è un dato riferito a una persona (con che cosa ha pagato):
  voce aggiornata nel manifesto in italiano e inglese. Nessun dato nuovo di categoria diversa; nessun numero di
  carta — **al massimo le ultime quattro cifre**, e solo se arrivano dai movimenti importati (storia `0022`).
- **RT-9 — Registrazione eventi (§14).** L'evento `rischio di deducibilità rilevato` porta `tenant_id`, `app_id`,
  `user_id`, identificativo di correlazione e il **codice della regola** — non l'importo, non l'esercente.

## 4. Criteri di accettazione

**CA-1 — Pranzo in trasferta pagato in contanti**
- **Dato** una spesa di categoria Vitto, fuori dal Comune sede di lavoro, pagata in contanti
- **Quando** il collaboratore la conferma
- **Allora** compare l'avviso che il costo rischia l'indeducibilità e che il rimborso rischia di essere tassato, con
  il riferimento alla regola e il suggerimento di rimediare se possibile; **la spesa viene comunque confermata**

**CA-2 — Stesso pranzo dentro il Comune**
- **Dato** la stessa spesa, ma dentro il Comune sede di lavoro
- **Quando** viene confermata
- **Allora** **non** compare l'avviso su vitto e alloggio, perché l'obbligo di tracciabilità per queste categorie
  vale solo fuori Comune

**CA-3 — Taxi dentro il Comune pagato in contanti**
- **Dato** una spesa di categoria Trasporto con servizio non di linea, dentro il Comune, in contanti
- **Quando** viene confermata
- **Allora** l'avviso compare **lo stesso**, perché per il trasporto l'obbligo vale anche dentro il Comune

**CA-4 — Vista di periodo**
- **Dato** un trimestre con quattro spese a rischio · **Quando** si apre «spese a rischio» per quel trimestre
- **Allora** si vedono esattamente quelle quattro, con il totale a rischio

**CA-5 — Il giudizio del passato non cambia**
- **Dato** una spesa valutata con le regole vigenti al momento
- **Quando** le regole di configurazione vengono aggiornate
- **Allora** la spesa continua a mostrare la valutazione e la versione di regola con cui fu giudicata

**CA-6 — Isolamento fra account**
- **Dato** due account con spese simili · **Quando** l'uno consulta le proprie spese a rischio
- **Allora** non vede nulla dell'altro

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno backend e frontend; l'intera suite prima del commit);
- [ ] prove di **unità** sulla matrice categoria × mezzo di pagamento × dentro/fuori Comune, con **tutti** i casi
      della regola italiana; di **integrazione** sulla conferma con database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** sulla vista dei rischi;
- [ ] **prova end-to-end**: *coprire ora* il passo «la spesa in contanti mi avvisa e l'avviso arriva
      all'approvatore» nel percorso `[J-NOTESPESE]`; registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) aggiornato;
- [ ] **traduzioni** presenti in tutte e cinque le lingue, con l'indicazione della giurisdizione;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese;
- [ ] **registro delle decisioni** compilato, con la scelta di tenere le regole come configurazione e con la fonte
      normativa citata;
- [ ] contratto dello strumento `verifica_deducibilita` dichiarato, marcato lettura;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| `0008` | L'avviso vive nella schermata di revisione, al momento della conferma |
| `0018` | Il contesto dentro/fuori Comune arriva dalla trasferta |

## 7. Fuori ambito

- La verifica **automatica** che il pagamento sia davvero avvenuto con quel mezzo: si può ottenere solo abbinando il
  movimento della carta (storia `0023`), e resta un indizio, non una prova.
- Le giurisdizioni diverse dall'Italia: la struttura le regge come configurazione, i contenuti no.
- Il calcolo dell'imponibile da tassare in busta paga quando il rimborso perde l'esenzione: è materia di PayGrove
  (catalogo 10); qui si avvisa che il rischio esiste.

## 8. Punti aperti

- 🛑 **Chi tiene aggiornate le regole fiscali.** Sono configurazione, ma qualcuno deve scriverle e correggerle
  quando la legge cambia — con la responsabilità che ne deriva verso il cliente. È una decisione di prodotto e di
  rischio d'impresa: la chiude lo sviluppatore, non questa storia. Va inoltre deciso **come si comunica il limite**:
  l'app segnala, non fa consulenza fiscale, e questo va scritto dove l'utente lo legge.
- **Regole per Francia, Spagna e Germania**: punto aperto n. 3 della descrizione dell'applicazione.
