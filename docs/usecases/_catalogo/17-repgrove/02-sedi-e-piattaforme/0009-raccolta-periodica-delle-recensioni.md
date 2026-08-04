# 0009 — Raccolta periodica delle recensioni

**Applicazione**: 17 — RepGrove (`recensioni`) · **Epica**: 02 — Sedi e collegamento alle piattaforme
**Storia**: `0009` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0007`, `0008`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che ha collegato le sue piattaforme
> voglio che le recensioni nuove compaiano nell'app da sole, senza che io debba premere niente
> così da accorgermi di una recensione negativa nel giro di ore e non quando ci passo per caso.

**Contesto.** Il collegamento esiste ma non porta ancora niente dentro. Questa storia fa la lavorazione periodica
che interroga le piattaforme collegate, riconosce le recensioni nuove, aggiorna quelle modificate e non crea
doppioni. È la storia che rende viva l'app, ed è anche quella che consuma le quote delle piattaforme: va scritta
con parsimonia fin dall'inizio, perché correggerla dopo significa scoprire il problema quando i clienti sono
tanti.

Un vincolo di conformità entra già qui: le recensioni portano dentro **testo scritto da terzi**, e quel testo può
contenere dati sulla salute (descrizione §6, avviso sull'articolo 9). È la prima storia che scrive quel testo a
database, quindi è il momento in cui la valutazione d'impatto deve esistere.

## 2. Requisiti funzionali

1. **RF-1** — Una lavorazione programmata interroga, per ogni collegamento `attivo`, le recensioni pubblicate
   dopo l'ultima sincronizzazione riuscita, e le scrive nel flusso della sede.
2. **RF-2** — La scrittura è **idempotente**: la coppia (piattaforma, identificativo esterno) è unica per account,
   quindi ripetere la lavorazione non duplica nulla. Una recensione già presente e **modificata** all'origine
   (l'autore l'ha riscritta) viene aggiornata, conservando il momento in cui l'abbiamo vista la prima volta.
3. **RF-3** — Una recensione **sparita** all'origine (rimossa dall'autore o dalla piattaforma) viene marcata come
   non più pubblica, non cancellata di nascosto: il punteggio smette di contarla e l'interfaccia lo dice.
4. **RF-4** — Il ritmo della lavorazione è configurabile per ambiente e prudente per difetto (proposta: ogni ora
   per le sedi con recensioni recenti, ogni sei ore per le altre); in caso di errore ripetuto si dirada invece di
   insistere, e il collegamento passa in stato `in errore` con il motivo.
5. **RF-5** — Il cliente può chiedere un aggiornamento immediato dalla scheda della sede, con un limite di
   frequenza che protegge la quota della piattaforma e che l'app spiega quando scatta.
6. **RF-6** — La lavorazione non fa mai nulla per le sedi `sospese` o per i collegamenti `revocati`, `scaduti` o
   `in errore` oltre la soglia di tentativi.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** La lavorazione gira per account e scrive sempre con il `tenant_id` del
  collegamento che sta trattando; non esiste un percorso che scriva una recensione senza account. Prova
  obbligatoria: due account con collegamenti simulati che restituiscono la stessa recensione non si contaminano.
- **RT-2 — Interfaccia di programmazione (§2).** Rotta `POST /api/recensioni/v1/sedi/{id}/aggiorna-ora` per la
  richiesta manuale, con `429` quando il limite di frequenza è raggiunto. Errori in `application/problem+json`.
- **RT-3 — Persistenza (§8).** Tabella `recensione` (storia 0002), con il vincolo di unicità su
  `(tenant_id, piattaforma, identificativo_esterno)` e le colonne `vista_la_prima_volta_il` e `non_piu_pubblica_dal`.
  Eventuale migrazione `V4__recensione_sincronizzazione.sql`.
- **RT-4 — Modulo frontend (§3, §5).** Sulla scheda della sede: data dell'ultimo aggiornamento riuscito, pulsante
  «aggiorna ora» con il suo limite spiegato, e stato dell'errore in parole comprensibili.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe in `en, it, fr, es, de`. Nota: il **testo delle recensioni** non
  si traduce — è contenuto del cliente finale e va mostrato nella sua lingua.
- **RT-6 — Varchi e quota (§6, §7).** La lavorazione non consuma la quota `sedi_monitorate` (è già consumata dalla
  sede); il limite di frequenza dell'aggiornamento manuale è un presidio tecnico, non commerciale, e va detto così
  nel messaggio.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento nuovo qui: la lettura delle recensioni è lo
  strumento `elenca_recensioni` della storia 0027.
- **RT-8 — Dati personali (§10).** **Voci nuove nel manifesto** in italiano e inglese: `recensione.autore` (nome
  pubblico dell'autore, terzo non nostro utente) e `recensione.testo` (**testo libero, può contenere categorie
  particolari**). Campi annotati `@PersonalData`; tabella `recensione` già presente in `exportData` e `purgeData`.
  ⚠️ **Questa è la storia in cui l'avviso sull'articolo 9 della descrizione §6 diventa concreto**: non si comincia
  senza la decisione dello sviluppatore sulle tre vie proposte e senza la valutazione d'impatto.
- **RT-9 — Registrazione eventi (§14).** `sincronizzazione avviata`, `n recensioni nuove`, `n aggiornate`,
  `sincronizzazione fallita` con il codice di errore, tutti con `tenant_id`, `app_id`, identificativo del
  collegamento e identificativo di correlazione. **Mai** il testo o l'autore di una recensione nei registri.

## 4. Criteri di accettazione

**CA-1 — Le recensioni nuove arrivano**
- **Dato** un collegamento attivo con tre recensioni all'origine
- **Quando** la lavorazione gira per la prima volta
- **Allora** le tre recensioni compaiono nel flusso della sede con voto, testo, autore pubblico e data

**CA-2 — Nessun doppione**
- **Dato** la lavorazione già eseguita
- **Quando** viene eseguita di nuovo senza novità all'origine
- **Allora** il numero di recensioni non cambia e nessuna riga viene riscritta

**CA-3 — Recensione modificata all'origine**
- **Dato** una recensione già raccolta, il cui testo cambia all'origine
- **Quando** la lavorazione gira
- **Allora** il testo è aggiornato, il momento della prima raccolta resta quello originale e l'app segnala che è
  stata modificata

**CA-4 — Recensione sparita**
- **Dato** una recensione già raccolta che non compare più all'origine
- **Quando** la lavorazione gira
- **Allora** la recensione è marcata non più pubblica, esce dal punteggio e l'interfaccia lo mostra, senza
  cancellarla di nascosto

**CA-5 — Isolamento fra account**
- **Dato** due account con collegamenti simulati che restituiscono la stessa recensione
- **Quando** entrambe le lavorazioni girano
- **Allora** ciascun account vede una sola recensione, la propria, e nessuna riga è condivisa

**CA-6 — Errore ripetuto**
- **Dato** un collegamento la cui delega è scaduta
- **Quando** la lavorazione fallisce per la terza volta consecutiva
- **Allora** il collegamento passa in stato `scaduto`, la lavorazione smette di riprovare e il cliente vede il
  motivo sulla scheda della sede

**CA-7 — Limite dell'aggiornamento manuale**
- **Dato** un cliente che ha appena chiesto un aggiornamento immediato
- **Quando** lo richiede di nuovo entro la finestra minima
- **Allora** riceve `429` con un messaggio che dice fra quanto potrà riprovare, e nessuna chiamata parte verso la
  piattaforma

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno le aree toccate; l'intera suite prima del commit);
- [ ] prove di **unità** sul riconoscimento delle novità e sull'idempotenza, di **integrazione** sulla lavorazione
      con database effimero, migrazioni vere e fornitori **simulati**;
- [ ] prova di **isolamento fra account** sulla scrittura delle recensioni;
- [ ] **prova end-to-end**: *coprire ora* il passo «arriva una recensione» nel percorso `[J-RECENSIONI]`, con
      fornitore simulato, e registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) aggiornato;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese con `recensione.autore` e `recensione.testo`, campi
      annotati, tabella in esportazione e cancellazione;
- [ ] **valutazione d'impatto** avviata per il trattamento del testo di terzi (§6 della descrizione);
- [ ] **registro delle decisioni** compilato, con la decisione presa sull'articolo 9 e il ritmo scelto;
- [ ] contratto degli **strumenti conversazionali**: nessuno nuovo in questa storia.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storie `0007` e `0008` | serve almeno un collegamento attivo da cui leggere |
| **decisione sull'articolo 9** (descrizione §6, §11.7) | decide se il testo si conserva, e quindi la forma della tabella |
| storia `0010` | è scritta subito dopo e fissa i limiti di conservazione: le due vanno lette insieme |

## 7. Fuori ambito

- la visualizzazione del flusso — storia 0017;
- il punteggio — storia 0022;
- l'avviso sulle recensioni negative — storia 0020;
- la risposta — storie 0018 e 0019.

## 8. Punti aperti

- **Il ritmo proposto (un'ora / sei ore) non è verificato contro le quote reali** delle piattaforme: con quote
  basse e molti clienti va rivisto. Serve una misura vera, non una stima.
- **Cosa fare delle recensioni senza testo** (solo voto): contano nel punteggio, ma non nell'analisi dei temi.
  Sembra ovvio; va scritto perché è il genere di cosa che nessuno decide e poi si scopre incoerente.
- **Storicizzazione dei voti**: se un autore cambia il voto da 5 a 1, conserviamo il valore precedente? Serve per
  capire il proprio andamento, ma è conservazione di un dato in più su una persona. Non lo decido qui.
</content>
