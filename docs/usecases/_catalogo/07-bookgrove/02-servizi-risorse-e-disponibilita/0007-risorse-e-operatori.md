# 0007 — Risorse e operatori

**Applicazione**: 07 — BookGrove (`prenotazioni`) · **Epica**: 02 — Servizi, risorse e disponibilità
**Storia**: `0007` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0004`, `0006`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare
> voglio dire al programma chi e cosa può erogare ciascun servizio
> così da non ritrovarmi con due appuntamenti sulla stessa poltrona o con un servizio assegnato a chi non lo sa
> fare.

**Contesto.** «Risorsa» è volutamente più largo di «operatore»: in un salone la risorsa scarsa è la poltrona, in
un poliambulatorio è la sala, in un'officina è il ponte, in un noleggio è il mezzo. Tenere un solo concetto
generale è ciò che permette a questa app di essere la base dei cinque verticali che la contengono (§10 della
descrizione): se qui si scrivesse «operatore», ogni verticale dovrebbe reinventare il pezzo. È anche la storia in
cui la quota diventa concreta, perché è la risorsa aperta alla prenotazione a consumarla.

## 2. Requisiti funzionali

1. **RF-1** — Si crea, modifica e disattiva una risorsa con: nome, tipo (persona, postazione, sala, attrezzatura),
   colore in agenda, sede di appartenenza.
2. **RF-2** — Una risorsa di tipo persona può essere **collegata a un utente** dell'account, così che quella
   persona veda la propria agenda; il collegamento è facoltativo (un collaboratore può non avere un accesso).
3. **RF-3** — Si dichiara quali servizi eroga ciascuna risorsa; un servizio senza nessuna risorsa che lo eroghi
   non è prenotabile, e il programma lo dice invece di mostrare un'agenda vuota senza spiegazioni.
4. **RF-4** — Aprire una risorsa alla prenotazione **consuma una unità** della metrica `risorse_prenotabili`;
   chiuderla la restituisce.
5. **RF-5** — Disattivare una risorsa con prenotazioni future non è consentito finché non sono state spostate o
   disdette, e il messaggio dice quante sono e dove trovarle.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura e scrittura delle `risorsa` filtra per `tenant_id` preso
  dal token verificato; il collegamento a un utente vale solo per utenti dello stesso account.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `GET|POST /api/prenotazioni/v1/risorse` e
  `GET|PUT|DELETE /api/prenotazioni/v1/risorse/{id}`; corpo validato; errori in `problem+json` con codici stabili
  per «quota esaurita» e «ha prenotazioni future»; OpenAPI aggiornata.
- **RT-3 — Persistenza (§8).** Migrazione `V3__risorse.sql`: tabella `risorsa` e tabella di raccordo
  `risorsa_servizio`, con `tenant_id`, UUID versione 7, colonne di controllo e cancellazione logica.
- **RT-4 — Varchi e quota (§6, §7).** Prima di aprire una risorsa alla prenotazione il servizio prenota una unità
  della metrica `risorse_prenotabili` (natura `stock`); a quota esaurita risponde `429` con l'indicazione del
  rimedio. Con abbonamento non attivo risponde `402`.
- **RT-5 — Modulo frontend (§3, §5).** Sezione «Servizi e risorse»: elenco delle risorse con il loro colore,
  matrice risorsa×servizio, indicatore di quota; solo token del sistema di design; tema chiaro e scuro.
- **RT-6 — Cinque lingue (§4).** Interfaccia e messaggi in `en, it, fr, es, de`; i **tipi** di risorsa sono voci
  tradotte, non testo libero.
- **RT-7 — Dati personali (§10).** `risorsa.nome` di tipo persona è il nome di un operatore: voce già presente nel
  manifesto dalla storia `0002`, campo annotato `@PersonalData`, tabella in `exportData` e `purgeData`. Il
  collegamento risorsa↔utente è un dato sul rapporto di lavoro: si dichiara.
- **RT-8 — Registrazione eventi (§14).** `risorsa aperta`, `risorsa chiusa`, `apertura respinta per quota` con
  `tenant_id`, `app_id`, `user_id` e correlazione — **mai il nome della risorsa**, che è un dato personale.

## 4. Criteri di accettazione

**CA-1 — Creazione e abbinamento**
- **Dato** i quattro servizi della storia `0006`
- **Quando** si crea la risorsa «Poltrona 1» e le si assegnano due servizi
- **Allora** quei due risultano prenotabili su quella poltrona e gli altri due no

**CA-2 — Quota a giacenza**
- **Dato** un account al tetto delle risorse aperte · **Quando** apre una risorsa in più · **Allora** riceve `429`
  con il conteggio e il rimedio, e nulla viene aperto

**CA-3 — Disattivazione protetta**
- **Dato** una risorsa con tre prenotazioni future · **Quando** la si disattiva · **Allora** l'operazione è
  rifiutata con un messaggio che dice quante sono, e nulla cambia

**CA-4 — Servizio senza risorsa**
- **Dato** un servizio che nessuna risorsa eroga · **Quando** si guarda l'elenco dei prenotabili · **Allora** il
  servizio è segnalato come non prenotabile, con il motivo

**CA-5 — Isolamento fra account**
- **Dato** due account · **Quando** un utente prova a collegare la propria risorsa a un utente dell'altro account
- **Allora** l'operazione è rifiutata

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend` e `frontend`);
- [ ] prove di **unità** sull'abbinamento risorsa×servizio e di **integrazione** sulla risorsa `risorse`;
- [ ] prova di **isolamento fra account** sulla risorsa e sul collegamento all'utente;
- [ ] **prova end-to-end**: *rimando* — passo del percorso `[J-BOOKGROVE]` della storia `0033`, dove si aggiorna
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml);
- [ ] **traduzioni** in tutte e cinque le lingue, compresi i tipi di risorsa;
- [ ] **manifesto dei dati** verificato: `risorsa.nome` presente, in italiano e inglese;
- [ ] **registro delle decisioni** compilato: la scelta del concetto generale «risorsa» invece di «operatore», e
      il perché in vista dei verticali;
- [ ] avvio locale invariato; i dati di prova comprendono due risorse;
- [ ] documentazione aggiornata.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0004` | il varco della quota deve esistere prima di consumarla |
| storia `0006` | servono i servizi da assegnare |

## 7. Fuori ambito

- gli orari in cui la risorsa è disponibile: storia `0008`;
- i turni del personale come strumento di pianificazione del lavoro: è l'applicazione 11;
- la capienza di una risorsa (un tavolo da sei, una lezione da dodici): rimandata ai verticali che ne hanno
  bisogno, perché infilarla qui riempirebbe di casi particolari il motore comune.

## 8. Punti aperti

**Risorse necessarie insieme.** Una visita può richiedere contemporaneamente un operatore **e** una sala. Il
modello attuale lega una prenotazione a una sola risorsa. Estenderlo a più risorse per prenotazione è possibile ma
raddoppia la complessità del calcolo della disponibilità: la proposta è **non** farlo ora e lasciarlo al verticale
sanitario (catalogo 23), dichiarando il limite. Da confermare dallo sviluppatore.
