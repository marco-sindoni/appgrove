# 0023 — Avviso di trattative ferme

**Applicazione**: 04 — LeadGrove (`sales`) · **Epica**: 04 — Attività e storico della relazione
**Storia**: `0023` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0015`, `0019`, `0020`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare
> voglio che le trattative dimenticate saltino fuori da sole
> così da non scoprire a fine trimestre che tre affari sono morti perché nessuno ha richiamato.

**Contesto.** È la funzione che trasforma l'archivio in uno strumento che lavora: senza, le trattative ferme
restano ferme, e sono esattamente quelle che si perdono. Si appoggia allo storico dei passaggi di fase (0015) e
alle attività (0019): «ferma» significa nessun movimento di fase **e** nessuna attività completata da un certo
numero di giorni.

## 2. Requisiti funzionali

1. **RF-1** — Una trattativa aperta è considerata **ferma** quando non ha passaggi di fase, attività completate né
   note da più di un numero di giorni configurabile per fase (valore predefinito: 14 giorni).
2. **RF-2** — Le trattative ferme compaiono in un gruppo dedicato nella panoramica e sono segnalate sulla lavagna.
3. **RF-3** — Da lì si programma un'attività di richiamo in un clic, oppure si chiude la trattativa come persa.
4. **RF-4** — La soglia si può cambiare per fase: una trattativa in «Da qualificare» ferma da 3 giorni è normale,
   una in «In chiusura» ferma da 14 non lo è.
5. **RF-5** — Il conteggio delle trattative ferme e il loro valore complessivo compaiono nella panoramica, perché
   il numero da solo non dice quanto si rischia.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Il calcolo si fa solo sulle trattative dell'account del token verificato.
- **RT-2 — Interfaccia di programmazione (§2).** Rotta `GET /api/sales/v1/deals/stale` con parametri di
  responsabile e pipeline; la soglia per fase si imposta su `PATCH /api/sales/v1/stages/{id}`; errori in
  `application/problem+json`; OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Colonna della soglia sulla tabella `stage`, con migrazione
  `V<N>__stage_stale_threshold.sql`. Nessuna tabella nuova: «ferma» è una interrogazione, non uno stato conservato
  — conservarlo richiederebbe una lavorazione periodica che può sbagliare.
- **RT-4 — Modulo frontend (§3, §5).** Gruppo «Ferme da troppo» nella panoramica, segnalazione sulle schede della
  lavagna, azioni rapide; solo token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Etichette, espressioni di durata e messaggi in `en, it, fr, es, de`.
- **RT-6 — Varchi e quota (§6, §7).** Nessun consumo di quota. Cambiare le soglie richiede ruolo `owner` o
  `admin`.
- **RT-7 — Esposizione conversazionale (§12).** `list_deals` (storia 0034) accetta il filtro «ferme»: «quali
  trattative sono ferme da più di due settimane» è una delle domande più utili che si possano fare in chat.
- **RT-8 — Dati personali (§10).** Nessun dato personale nuovo.
- **RT-9 — Registrazione eventi (§14).** Nessun evento nuovo; il conteggio delle trattative ferme si registra come
  metrica aggregata, senza identificativi di persone.

## 4. Criteri di accettazione

**CA-1 — Rilevazione**
- **Dato** una trattativa aperta senza movimenti, attività o note da 20 giorni, in una fase con soglia 14
- **Quando** si apre la panoramica
- **Allora** compare fra le ferme, con l'indicazione di quanti giorni sono passati

**CA-2 — Un movimento la toglie dall'elenco**
- **Dato** la stessa trattativa
- **Quando** un venditore vi completa un'attività
- **Allora** non risulta più ferma

**CA-3 — Soglia per fase**
- **Dato** due trattative ferme da 10 giorni, una in una fase con soglia 7 e una in una fase con soglia 21
- **Quando** si apre l'elenco
- **Allora** compare solo la prima

**CA-4 — Azione rapida**
- **Dato** una trattativa ferma
- **Quando** il venditore programma il richiamo dall'elenco
- **Allora** l'attività è creata con il riferimento giusto e la trattativa esce dalle ferme

**CA-5 — Isolamento fra account**
- **Dato** due account `A` e `B` con trattative ferme
- **Quando** un utente di `A` apre l'elenco
- **Allora** vede solo le proprie

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (`backend`, `frontend`; l'intera suite prima del commit);
- [ ] prove di **unità** sul calcolo dell'inattività con le tre sorgenti e di **integrazione** sulla rotta;
- [ ] prova di **isolamento fra account** sull'elenco delle ferme;
- [ ] **prova end-to-end**: nessun impatto sul percorso minimo; coperta da prove d'integrazione, con il motivo nel
      registro di copertura;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati**: nessuna voce nuova;
- [ ] **registro delle decisioni** compilato, con annotata la scelta di calcolare «ferma» invece di conservarlo;
- [ ] contratto degli **strumenti conversazionali**: filtro «ferme» su `list_deals`;
- [ ] controllo automatico di **accessibilità** verde sul gruppo delle ferme;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storia `0015` | I passaggi di fase sono una delle tre sorgenti di movimento |
| Storie `0019`, `0020` | Attività e agenda sono le altre due, e sono anche il rimedio proposto |

## 7. Fuori ambito

- l'invio di un avviso in posta elettronica al responsabile: è un effetto verso l'esterno, punto aperto della
  storia 0019;
- la chiusura automatica delle trattative ferme da troppo: esclusa, chiudere è un atto di una persona.

## 8. Punti aperti

- **Soglia predefinita di 14 giorni** — è una proposta ragionevole ma non misurata: nessuna delle fonti consultate
  dà un valore di riferimento per il segmento micro. Chi la conferma è lo sviluppatore; il modo giusto di chiuderla
  è guardare i dati reali dopo qualche mese di uso.
