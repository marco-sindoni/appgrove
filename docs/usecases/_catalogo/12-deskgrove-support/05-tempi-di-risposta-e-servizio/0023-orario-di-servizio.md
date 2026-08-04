# 0023 — Orario di servizio

**Applicazione**: 12 — DeskGrove Support (`helpdesk`) · **Epica**: 05 — Tempi di risposta e livello di servizio
**Storia**: `0023` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0002`, `0003`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che ha deciso di misurare il servizio
> voglio dichiarare quando la mia azienda risponde davvero — giorni, fasce orarie, fuso, chiusure
> così da non vedermi contare come ritardo le ore in cui l'ufficio è chiuso.

**Contesto.** Fino a qui l'app sa *quando* è arrivata una richiesta ma non sa *quando* qualcuno c'era per leggerla.
È la differenza fra una misura vera e un numero che nessuno guarda più: una richiesta arrivata il sabato alle 20 e
letta il lunedì alle 9 non è «in ritardo di 37 ore», è in ritardo di zero. Il [documento capofila](../application-description.md)
§2.5 lo dice esplicitamente: gli obiettivi di servizio si esprimono in **ore lavorative**, ed è per questo che il
calendario dell'azienda va costruito **prima** delle politiche (storia `0024`) e prima degli orologi (storia `0025`).
Farlo dopo significherebbe riscrivere il calcolo delle scadenze una seconda volta.

## 2. Requisiti funzionali

1. **RF-1** — L'account dichiara, per ciascuno dei sette giorni della settimana, da zero a due fasce orarie di
   apertura (per esempio lunedì 09:00-13:00 e 14:00-18:00, sabato nessuna fascia).
2. **RF-2** — L'account dichiara il proprio fuso orario; ogni conversione fra istante assoluto e ora locale usa
   quel fuso, comprese le settimane in cui cambia l'ora legale.
3. **RF-3** — L'account dichiara un elenco di giorni di chiusura come date singole con un'etichetta libera (per
   esempio «15 agosto — chiusura estiva»); in quei giorni nessuna fascia è valida, anche se il giorno della
   settimana ne prevede.
4. **RF-4** — Il servizio espone una funzione di calcolo che, dato un istante di partenza e una quantità di ore
   lavorative, restituisce l'istante di scadenza saltando le ore di chiusura, e una funzione inversa che misura
   quante ore lavorative sono trascorse fra due istanti.
5. **RF-5** — Alla prima apertura del modulo l'account ha già un orario predefinito valido — dal lunedì al venerdì,
   09:00-13:00 e 14:00-18:00, fuso dell'account, nessuna chiusura — così che le storie successive funzionino senza
   che nessuno configuri nulla.
6. **RF-6** — La schermata mostra un esempio calcolato dal vivo che rende verificabile la configurazione: «una
   richiesta arrivata sabato alle 20:00 con obiettivo di 4 ore lavorative scade lunedì alle 13:00».
7. **RF-7** — Un orario privo di qualunque fascia su tutti e sette i giorni viene rifiutato al salvataggio: senza
   almeno un'ora aperta a settimana nessuna scadenza sarebbe mai raggiungibile.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Lettura e scrittura dell'orario di servizio e dei giorni di chiusura
  filtrano per `tenant_id` preso dal token verificato; un `tenant_id` che arrivasse dal corpo della richiesta o dai
  parametri viene ignorato. Prova di isolamento su entrambe le risorse.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `GET /api/helpdesk/v1/orario-di-servizio` e
  `PUT /api/helpdesk/v1/orario-di-servizio` (fasce e fuso, sostituzione integrale),
  `GET|POST /api/helpdesk/v1/orario-di-servizio/chiusure` e
  `DELETE /api/helpdesk/v1/orario-di-servizio/chiusure/{id}`; corpo validato (fasce non sovrapposte, inizio prima
  della fine, fuso fra quelli riconosciuti); errori in `application/problem+json`; definizione OpenAPI aggiornata
  nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V<N>__business_calendar.sql` sullo schema `app_helpdesk`: tabelle
  `business_calendar` (fuso, riferimento all'account), `business_hour` (giorno della settimana, ora di inizio, ora
  di fine) e `closure_day` (data, etichetta), tutte con `tenant_id`, chiave primaria UUID versione 7, colonne di
  controllo (`created_at`, `updated_at`, `created_by`, `updated_by`) e cancellazione logica `deleted_at`. Nessuna
  chiave esterna verso altri schemi.
- **RT-4 — Modulo frontend (§3, §5).** Sezione *Impostazioni → Orario di servizio* del modulo `helpdesk`: griglia
  dei sette giorni, selettore del fuso, elenco delle chiusure e il riquadro dell'esempio calcolato (RF-6). Dati
  letti con il client generato dalla definizione OpenAPI; solo token del sistema di design, colore-categoria
  `teal`; funziona in tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe visibili — nomi dei giorni, messaggi di validazione, testo
  dell'esempio — passano dallo spazio-nomi `helpdesk` e sono presenti in `en, it, fr, es, de`.
- **RT-6 — Varchi e quota (§6, §7).** La configurazione dell'orario non consuma la metrica `agents` (natura
  `stock`): non crea posti operatore. Restano i varchi a monte: `401` senza token valido, `402` con abbonamento
  `canceled`, `403` per ruolo insufficiente — la modifica è riservata ai ruoli `owner` e `admin`, la lettura è di
  tutti gli operatori perché serve a capire perché una scadenza cade dove cade.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento nuovo: configurare il calendario dell'azienda non
  è un'operazione che si comanda da una chat, e sbagliarla falsa in silenzio ogni misura successiva. Esclusione
  deliberata, da annotare nel registro delle decisioni. Il contratto degli strumenti resta quello delle storie
  `0034` e `0035`; dipendenza di piattaforma dichiarata: UC 0061-0063, non ancora implementati.
- **RT-8 — Dati personali (§10).** Nessun dato personale nuovo: fasce orarie, fuso e giorni di chiusura descrivono
  l'azienda, non una persona. L'etichetta della chiusura è testo libero scritto da un operatore dell'account: va
  detto nel registro delle decisioni che non è un campo in cui inserire nomi, e le tre tabelle vanno comunque in
  `exportData` del contratto `HelpdeskDataContract` perché fanno parte della configurazione da restituire al
  titolare alla fine del rapporto.
- **RT-9 — Registrazione eventi (§14).** Gli eventi «orario di servizio modificato», «giorno di chiusura aggiunto»,
  «giorno di chiusura rimosso» sono registrati con `tenant_id`, `app_id`, `user_id` e identificativo di
  correlazione, senza dati personali.

## 4. Criteri di accettazione

**CA-1 — Scadenza dentro l'orario**
- **Dato** un account con orario dal lunedì al venerdì 09:00-13:00 e 14:00-18:00, fuso `Europe/Rome`
- **Quando** si chiede la scadenza di 4 ore lavorative a partire da martedì alle 10:00
- **Allora** la funzione restituisce martedì alle 15:00, perché l'ora della pausa pranzo non si conta

**CA-2 — Arrivo a ufficio chiuso**
- **Dato** lo stesso orario
- **Quando** si chiede la scadenza di 4 ore lavorative a partire da sabato alle 20:00
- **Allora** la funzione restituisce lunedì alle 13:00, e la misura delle ore lavorative fra sabato alle 20:00 e
  lunedì alle 09:00 vale zero

**CA-3 — Giorno di chiusura**
- **Dato** lo stesso orario con il 15 agosto (un venerdì) dichiarato giorno di chiusura
- **Quando** si chiede la scadenza di 2 ore lavorative a partire da giovedì 14 agosto alle 17:00
- **Allora** la scadenza cade lunedì 18 agosto alle 10:00, saltando l'intero venerdì

**CA-4 — Orario vuoto rifiutato**
- **Dato** un utente con ruolo `admin` · **Quando** salva un orario senza alcuna fascia su nessun giorno ·
  **Allora** riceve `400` in `application/problem+json` con la spiegazione che serve almeno un'ora aperta a
  settimana, e la configurazione precedente resta intatta

**CA-5 — Cambio dell'ora legale**
- **Dato** un account nel fuso `Europe/Rome` e un obiettivo di 8 ore lavorative a partire dal venerdì prima del
  cambio d'ora
- **Quando** si calcola la scadenza sulla settimana in cui l'ora cambia
- **Allora** il risultato resta ancorato all'ora **locale** di apertura e chiusura, non a uno scarto fisso di ore
  rispetto al tempo universale

**CA-6 — Isolamento fra account**
- **Dato** due account `A` e `B` con orari e chiusure diversi
- **Quando** un utente di `A` legge o modifica l'orario di servizio, anche forzando l'identificativo del calendario
  di `B` nel percorso della richiesta
- **Allora** vede e modifica soltanto il proprio, e la richiesta su una risorsa di `B` risponde come se non
  esistesse

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (backend e frontend; l'intera suite prima del commit);
- [ ] prove di **unità** sul calcolo delle ore lavorative — pausa pranzo, fine settimana, giorni di chiusura,
      cambio dell'ora legale, obiettivo pari a zero — e di **integrazione** sulle rotte, con database effimero e
      migrazioni vere;
- [ ] prova di **isolamento fra account** sull'orario e sulle chiusure;
- [ ] **prova end-to-end**: *rimando* alla storia `0037`, proprietaria del percorso `[J-HELPDESK]`, dove la
      configurazione dell'orario è il presupposto del passo sulle scadenze; motivo e storia proprietaria annotati
      nel registro di copertura;
- [ ] **traduzioni** presenti in tutte e cinque le lingue (`en, it, fr, es, de`);
- [ ] **manifesto dei dati** aggiornato: nessuna voce nuova di persone, tabelle di configurazione presenti in
      esportazione;
- [ ] **registro delle decisioni** `changes/NNNN-*/decisions.json` compilato, in particolare sull'orario
      predefinito e sul rifiuto dell'orario vuoto;
- [ ] contratto degli **strumenti conversazionali**: esclusione deliberata, annotata con il motivo;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0002` (modello dati multi-account) | Servono lo schema `app_helpdesk` e le convenzioni delle tabelle |
| storia `0003` (guscio del modulo frontend) | Serve la sezione *Impostazioni* in cui la schermata si innesta |
| epica di piattaforma non implementata (UC 0061-0063) | Il livello conversazionale non esiste: qui non si dichiara alcuno strumento e non serve alcun ripiego |

## 7. Fuori ambito

- **Gli obiettivi di tempo** (quante ore per la prima risposta, quante per la risoluzione): li fissa la storia
  `0024`. Qui si costruisce solo il righello, non la misura.
- **Il calcolo delle scadenze sulle richieste** e la pausa dell'orologio: storia `0025`.
- **Più calendari per lo stesso account** (uno per coda, uno per squadra): rimandato perché il segmento micro ha un
  solo orario di apertura e più calendari sarebbero il primo passo verso il motore di regole che il
  [documento capofila](../application-description.md) §2.5 esclude di proposito. Se servisse, lo aprirà una storia
  nuova, non questa.
- **Il calendario delle festività nazionali precaricato** per Paese: rimandato perché richiede una sorgente di dati
  esterna e aggiornata ogni anno; qui le chiusure si inseriscono a mano, che per una micro-impresa sono meno di
  dieci date all'anno.
- **Le chiusure ricorrenti** («ogni 1° gennaio»): rimandate insieme al punto precedente.

## 8. Punti aperti

- **Il valore predefinito dell'orario** (lunedì-venerdì 09:00-13:00 e 14:00-18:00) è una proposta ragionevole per il
  mercato italiano ma non per tutti i mercati serviti dalle cinque lingue dell'interfaccia. **Decide lo
  sviluppatore** se lasciarlo unico o derivarlo dal fuso dell'account.
- **Il numero massimo di fasce per giorno** (proposta: due) è un limite di semplicità, non un vincolo tecnico.
  **Decide lo sviluppatore**; se salisse, la schermata va ripensata perché la griglia dei sette giorni smette di
  essere leggibile.
