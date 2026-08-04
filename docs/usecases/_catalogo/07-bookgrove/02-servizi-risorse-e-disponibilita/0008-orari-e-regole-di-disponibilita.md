# 0008 — Orari e regole di disponibilità

**Applicazione**: 07 — BookGrove (`prenotazioni`) · **Epica**: 02 — Servizi, risorse e disponibilità
**Storia**: `0008` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0007`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare
> voglio dire quando ciascuna risorsa è disponibile, compreso il fatto che il lunedì siamo chiusi e che Anna fa
> mezza giornata il mercoledì
> così da non dover controllare a mano ogni proposta di appuntamento.

**Contesto.** L'orario è la seconda sorgente della disponibilità. La tentazione è di modellarlo come «orario di
apertura del negozio», ma non basta quasi mai: la persona ha il suo orario, la sala ha il suo, e il negozio ha il
suo. La proposta è che l'orario stia sulla **risorsa**, con la sede a fare da valore predefinito quando la
risorsa non ne ha uno proprio. La seconda cosa che qui va decisa bene è la **validità nel tempo**: gli orari
cambiano (orario estivo, orario invernale) e cambiarli non deve riscrivere il passato.

## 2. Requisiti funzionali

1. **RF-1** — Si definiscono per ogni risorsa gli intervalli settimanali di disponibilità: giorno della settimana,
   ora di inizio, ora di fine; più intervalli nello stesso giorno (mattina e pomeriggio).
2. **RF-2** — Ogni insieme di regole ha una **validità da/a**: si può preparare l'orario estivo prima che entri in
   vigore, e l'orario passato resta quello che era.
3. **RF-3** — La sede ha un proprio orario, che vale per le risorse che non ne hanno uno proprio.
4. **RF-4** — Si dichiara il **passo della griglia** di prenotazione (per esempio ogni 15 o ogni 30 minuti) e il
   **preavviso minimo** (non si prenota per fra dieci minuti) e il **massimo anticipo** (non si prenota per fra un
   anno).
5. **RF-5** — Gli orari si esprimono in ora locale della sede e si risolvono in istanti secondo il fuso della
   sede, non secondo il fuso di chi sta guardando.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura e scrittura delle `regola_disponibilita` filtra per
  `tenant_id` preso dal token verificato.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte
  `GET|PUT /api/prenotazioni/v1/risorse/{id}/disponibilita` e `GET|PUT /api/prenotazioni/v1/sedi/{id}/orario`;
  corpo validato (fine dopo inizio, intervalli non sovrapposti nello stesso giorno); errori in `problem+json`;
  OpenAPI aggiornata.
- **RT-3 — Persistenza (§8).** Migrazione `V4__disponibilita.sql`: tabella `regola_disponibilita` con
  `tenant_id`, UUID versione 7, colonne di controllo e cancellazione logica; nessuna chiave esterna verso altri
  schemi.
- **RT-4 — Tempo.** Le ore si conservano come ora locale più riferimento alla sede; la risoluzione in istanti usa
  il fuso della sede e gestisce i due casi patologici del cambio dell'ora dichiarati nella storia `0002`.
- **RT-5 — Modulo frontend (§3, §5).** Schermata a griglia settimanale, modificabile per risorsa, con copia
  dell'orario da una risorsa all'altra (è la richiesta più prevedibile); solo token del sistema di design; tema
  chiaro e scuro.
- **RT-6 — Cinque lingue (§4).** Giorni della settimana, formati di ora e messaggi in `en, it, fr, es, de`, con
  il formato dell'ora corretto per lingua (le 14 non si scrivono uguale in inglese e in italiano).
- **RT-7 — Dati personali (§10).** Nessun dato personale nuovo: la regola descrive una fascia oraria, non una
  persona. Il collegamento alla risorsa-persona esiste già ed è dichiarato.
- **RT-8 — Registrazione eventi (§14).** `disponibilita aggiornata` con `tenant_id`, `app_id`, `user_id` e
  correlazione.

## 4. Criteri di accettazione

**CA-1 — Orario settimanale**
- **Dato** la risorsa «Anna» · **Quando** si imposta lunedì-venerdì 9-13 e 15-19, mercoledì solo 9-13
- **Allora** la disponibilità della settimana rispecchia esattamente quello

**CA-2 — Orario con validità futura**
- **Dato** un orario estivo valido dal 1° giugno · **Quando** lo si salva a maggio · **Allora** l'agenda di maggio
  non cambia e quella di giugno sì

**CA-3 — Ricaduta sull'orario della sede**
- **Dato** una risorsa senza orario proprio · **Quando** si guarda la sua disponibilità · **Allora** segue quello
  della sede, e lo dice a schermo invece di sembrare vuota

**CA-4 — Preavviso e anticipo**
- **Dato** preavviso minimo di due ore · **Quando** si chiede la disponibilità di oggi · **Allora** le prime due
  ore non compaiono

**CA-5 — Validazione**
- **Dato** un intervallo che finisce prima di iniziare, o due intervalli sovrapposti nello stesso giorno
- **Quando** si salva · **Allora** l'errore è chiaro e nulla viene salvato

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend` e `frontend`);
- [ ] prove di **unità** sulla risoluzione delle regole in istanti, compresi i due giorni di cambio dell'ora, e di
      **integrazione** sulle rotte;
- [ ] prova di **isolamento fra account** sulla risorsa introdotta;
- [ ] **prova end-to-end**: *rimando* — passo del percorso `[J-BOOKGROVE]` della storia `0033`, dove si aggiorna
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml);
- [ ] **traduzioni** in tutte e cinque le lingue, compresi i formati di ora;
- [ ] **manifesto dei dati**: nessuna voce nuova, con la ragione scritta;
- [ ] **registro delle decisioni** compilato: orario sulla risorsa con ricaduta sulla sede, e validità nel tempo;
- [ ] avvio locale invariato; i dati di prova comprendono orari settimanali;
- [ ] documentazione aggiornata.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0007` | le regole si appendono a una risorsa |
| storia `0002` | la scelta sul tempo e sui fusi orari |

## 7. Fuori ambito

- ferie, festività e chiusure straordinarie: storia `0009`;
- gli impegni personali letti dal calendario esterno: storia `0029`;
- il calcolo degli spazi liberi: storia `0010`.

## 8. Punti aperti

**Festività nazionali.** Sarebbe comodo che il programma sapesse da solo che il 25 aprile è festa. Ma il calendario
delle festività è per Paese e a volte per comune, e sbagliarlo significa chiudere un'attività che era aperta.
Proposta: **non** precaricarle, lasciarle come chiusure inserite a mano (storia `0009`), e valutare un
precaricamento suggerito e modificabile solo se emergerà come richiesta reale. Da confermare.
