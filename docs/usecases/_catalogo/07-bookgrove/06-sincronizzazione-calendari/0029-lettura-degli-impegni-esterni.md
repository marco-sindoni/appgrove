# 0029 — Lettura degli impegni esterni

**Applicazione**: 07 — BookGrove (`prenotazioni`) · **Epica**: 06 — Sincronizzazione con i calendari esterni
**Storia**: `0029` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0028`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come operatore
> voglio che il programma non offra ai clienti le ore in cui ho già un impegno mio
> così da non dover ricordarmi di bloccare a mano l'agenda ogni volta che prendo un appuntamento personale.

**Contesto.** È la metà più preziosa e più delicata della sincronizzazione: preziosa perché evita l'errore più
fastidioso — l'appuntamento preso mentre l'operatore è altrove — delicata perché significa **leggere il
calendario personale di una persona**. La regola che tiene insieme le due cose è la minimizzazione: all'app serve
sapere **che** c'è un impegno, non **quale**. Titoli, partecipanti e luoghi non vengono conservati.

## 2. Requisiti funzionali

1. **RF-1** — Gli impegni presenti sul calendario esterno di un operatore **sottraggono disponibilità**: quelle
   ore non compaiono fra gli spazi liberi.
2. **RF-2** — Si conservano **solo** inizio e fine dell'impegno: nessun titolo, nessun partecipante, nessun luogo,
   nessuna descrizione.
3. **RF-3** — Gli impegni marcati come «disponibile» sul calendario esterno non sottraggono nulla; quelli marcati
   «occupato» sì.
4. **RF-4** — La sincronizzazione avviene periodicamente e su richiesta; è visibile quando è avvenuta l'ultima
   volta, perché è l'informazione che serve a capire un comportamento strano.
5. **RF-5** — Un impegno esterno che si sovrappone a una prenotazione già presa **non** cancella la prenotazione:
   la segnala come conflitto e lascia decidere a una persona.
6. **RF-6** — Se il collegamento è rotto, la disponibilità torna a essere calcolata senza il calendario esterno,
   e la schermata lo dice invece di far finta di niente.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Gli impegni letti appartengono alla risorsa di un `tenant_id`
  determinato e non sono visibili ad altri account né ad altri operatori dello stesso account, se non come «tempo
  non disponibile».
- **RT-2 — Interfaccia di programmazione (§2).** Rotta `POST /api/prenotazioni/v1/risorse/{id}/calendario/sincronizza`
  per la sincronizzazione su richiesta; errori in `problem+json`; OpenAPI aggiornata.
- **RT-3 — Persistenza (§8).** Migrazione `V20__impegni_esterni.sql`: tabella `impegno_esterno` con `tenant_id`,
  UUID versione 7, colonne di controllo, risorsa, inizio, fine e identificativo presso il fornitore —
  **nessun campo di contenuto**.
- **RT-4 — Il motore accoglie la quinta sorgente.** Il calcolo degli spazi liberi (storia `0010`) era stato
  scritto per poter aggiungere una sorgente senza riscritture: qui si verifica che sia vero.
- **RT-5 — Quote del fornitore.** La sincronizzazione rispetta i limiti di frequenza del fornitore e degrada con
  garbo quando li raggiunge: rallenta, non fallisce rumorosamente.
- **RT-6 — Modulo frontend (§3, §5).** In agenda gli impegni esterni appaiono come tempo non disponibile, con una
  resa distinta dalle chiusure e **senza nessun contenuto**; solo token del sistema di design; tema chiaro e
  scuro.
- **RT-7 — Cinque lingue (§4).** Etichette e messaggi in `en, it, fr, es, de`.
- **RT-8 — Dati personali (§10).** Voce nuova nel manifesto in italiano e inglese: gli intervalli di impegno
  personale di un operatore, con base giuridica «consenso dell'operatore» e durata proposta «finché il
  collegamento è attivo, poi cancellazione immediata»; campo annotato `@PersonalData`; tabella in `exportData` e
  `purgeData`. **La minimizzazione è la garanzia principale e va dichiarata**: sappiamo che l'operatore è
  occupato, non cosa sta facendo.
- **RT-9 — Registrazione eventi (§14).** `sincronizzazione eseguita`, `conflitto rilevato`, `limite del fornitore
  raggiunto` con `tenant_id`, `app_id`, correlazione e conteggi — mai contenuti.

## 4. Criteri di accettazione

**CA-1 — L'impegno personale toglie disponibilità**
- **Dato** un impegno esterno dalle 10 alle 11 · **Quando** si calcola la disponibilità · **Allora** quell'ora non
  compare fra gli spazi liberi

**CA-2 — Nessun contenuto conservato**
- **Dato** un impegno esterno con titolo e partecipanti · **Quando** si esaminano tabelle ed esportazioni
- **Allora** ci sono solo inizio e fine

**CA-3 — Impegno «disponibile»**
- **Dato** un impegno marcato come disponibile sul calendario esterno · **Quando** si calcola la disponibilità
- **Allora** non sottrae nulla

**CA-4 — Conflitto con una prenotazione**
- **Dato** una prenotazione già presa e un impegno esterno che ci si sovrappone · **Quando** la sincronizzazione
  gira · **Allora** la prenotazione resta, il conflitto è segnalato in agenda e nessuno viene disdetto
  automaticamente

**CA-5 — Collegamento rotto**
- **Dato** un collegamento scaduto · **Quando** si calcola la disponibilità · **Allora** il calcolo avviene senza
  il calendario esterno e la schermata dice che la sincronizzazione non è attiva

**CA-6 — Isolamento fra account**
- **Dato** due account · **Quando** uno prova a leggere gli impegni dell'altro · **Allora** la richiesta è
  rifiutata

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend`, `frontend` e `compliance`);
- [ ] prove di **unità** sull'integrazione della quinta sorgente nel calcolo e di **integrazione** con fornitore
      simulato;
- [ ] prova di **isolamento fra account** sugli impegni esterni;
- [ ] **prova end-to-end**: *rimando* — fornitore simulato; motivo e storia proprietaria dichiarati in
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml);
- [ ] **traduzioni** in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato con gli impegni personali e la dichiarazione di minimizzazione;
- [ ] **registro delle decisioni** compilato: **si conservano solo gli intervalli**, e il conflitto non disdice
      nulla in automatico;
- [ ] avvio locale invariato;
- [ ] documentazione aggiornata.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0027` | serve il collegamento autorizzato in lettura |
| storia `0028` | gli eventi scritti dall'app vanno riconosciuti per non creare cicli |
| storia `0010` | è il motore che accoglie la nuova sorgente |

## 7. Fuori ambito

- la lettura di calendari condivisi di squadra: non richiesta e con implicazioni di riservatezza maggiori;
- la modifica degli impegni personali dall'app: mai, non sono nostri.

## 8. Punti aperti

**Frequenza della sincronizzazione.** Troppo rada e si prendono appuntamenti su ore già occupate; troppo fitta e
si sbattono contro i limiti del fornitore. Le due vie sono la lettura periodica e la sottoscrizione alle notifiche
di modifica, che è più reattiva ma richiede un indirizzo raggiungibile dall'esterno e la gestione delle
sottoscrizioni che scadono. Proposta: lettura periodica per la prima versione. Da confermare in implementazione.
