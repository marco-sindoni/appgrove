# 0010 — Calcolo degli spazi liberi

**Applicazione**: 07 — BookGrove (`prenotazioni`) · **Epica**: 02 — Servizi, risorse e disponibilità
**Storia**: `0010` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0006`, `0007`, `0008`, `0009`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come chi risponde al telefono
> voglio chiedere al programma «quando posso mettere una piega questa settimana» e ricevere un elenco di orari
> davvero disponibili
> così da non dover incrociare a mente orari, durate, pause e appuntamenti già presi.

**Contesto.** È il cuore dell'applicazione e la parte che nessun assistente generico può sostituire (§1 della
descrizione): la disponibilità nasce dall'incrocio di quattro sorgenti e cambia ogni minuto. La scelta di modello
già dichiarata (§4 della descrizione) è che gli spazi liberi **non si memorizzano**: si calcolano su richiesta,
su una finestra limitata. Questa storia consegna il motore e la sua rotta; chi lo usa arriva dopo — l'agenda
(`0013`), la prenotazione dal banco (`0014`), la pagina pubblica (`0017`).

## 2. Requisiti funzionali

1. **RF-1** — Data una coppia servizio e finestra temporale, il motore restituisce gli intervalli in cui il
   servizio è prenotabile, con la risorsa che lo erogherebbe.
2. **RF-2** — Il calcolo sottrae, in quest'ordine: ciò che è fuori dagli orari della risorsa, le chiusure, le
   prenotazioni già confermate o richieste, e i tempi di preparazione e riordino del servizio.
3. **RF-3** — Il risultato rispetta il passo della griglia, il preavviso minimo e il massimo anticipo dichiarati
   nella storia `0008`.
4. **RF-4** — Se il servizio è erogato da più risorse, il motore può rispondere «alle 15 è libero» senza dire da
   chi, oppure elencare per risorsa: entrambe le forme servono, la prima al cliente finale, la seconda al banco.
5. **RF-5** — La finestra richiesta è **limitata** (per esempio al massimo sessanta giorni per chiamata): una
   richiesta più ampia viene rifiutata con un messaggio chiaro, non servita a fatica.
6. **RF-6** — Quando non c'è nessuno spazio, la risposta dice **perché** nei casi riconoscibili — nessuna risorsa
   eroga il servizio, tutto chiuso, tutto già prenotato — invece di restituire un elenco vuoto muto.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Il calcolo legge solo entità dell'account del token verificato; un
  `tenant_id` che arrivasse dalla richiesta viene ignorato. Sulla superficie pubblica il `tenant_id` arriva dal
  meccanismo della storia `0016`, non dalla richiesta.
- **RT-2 — Interfaccia di programmazione (§2).** Rotta `GET /api/prenotazioni/v1/disponibilita` con parametri
  servizio, da, a, risorsa facoltativa; risposta paginata per giorno; errori in `problem+json` con codice stabile
  per «finestra troppo ampia»; OpenAPI aggiornata.
- **RT-3 — Persistenza (§8).** **Nessuna tabella nuova**: gli spazi liberi non si memorizzano. Servono invece gli
  indici che rendono efficiente la lettura delle prenotazioni per risorsa e intervallo.
- **RT-4 — Prestazioni.** Il calcolo è deterministico e ripetibile; il risultato può essere messo in memoria per
  pochi secondi, ma **mai** per un tempo tale da mostrare libero uno spazio appena preso: il vero presidio contro
  la doppia prenotazione è il vincolo nel database (storia `0014`), non questo calcolo.
- **RT-5 — Cinque lingue (§4).** I messaggi che spiegano l'assenza di spazi sono tradotti in `en, it, fr, es,
  de`; le date e le ore seguono il formato della lingua.
- **RT-6 — Dati personali (§10).** Nessun dato personale nuovo, e — importante — la risposta **non contiene** chi
  ha prenotato: dice solo che uno spazio è occupato. È ciò che permette di riusare lo stesso motore sulla
  superficie pubblica senza rivelare nulla.
- **RT-7 — Registrazione eventi (§14).** `disponibilita calcolata` con `tenant_id`, `app_id`, `user_id`,
  correlazione e la larghezza della finestra, senza dati personali.
- **RT-8 — Prove (§11).** Prove di unità sui casi limite: servizio più lungo della fascia disponibile, chiusura a
  cavallo di due giorni, prenotazione che tocca esattamente l'inizio di uno spazio, giorno del cambio dell'ora.

## 4. Criteri di accettazione

**CA-1 — Calcolo di base**
- **Dato** una risorsa disponibile 9-13, un servizio da 60 minuti e nessuna prenotazione
- **Quando** si chiede la disponibilità di quel giorno con griglia da 30 minuti
- **Allora** si ottengono gli inizi 9:00, 9:30, 10:00, 10:30, 11:00, 11:30, 12:00 e nessun altro

**CA-2 — I tempi di preparazione contano**
- **Dato** lo stesso servizio con 10 minuti di riordino e una prenotazione dalle 10 alle 11
- **Quando** si ricalcola · **Allora** le 10:30 e le 11:00 non compaiono, la prima disponibilità dopo è le 11:10
  arrotondata al passo della griglia

**CA-3 — Chiusura**
- **Dato** una chiusura dalle 11 alle 13 · **Quando** si ricalcola · **Allora** nessuno spazio dopo le 10:00

**CA-4 — Nessuno spazio, con motivo**
- **Dato** un servizio che nessuna risorsa eroga · **Quando** si chiede la disponibilità · **Allora** la risposta
  dice «nessuna risorsa eroga questo servizio», non un elenco vuoto

**CA-5 — Finestra troppo ampia**
- **Dato** una richiesta di due anni · **Quando** la si esegue · **Allora** risponde con l'errore stabile e il
  limite ammesso

**CA-6 — Nessuna rivelazione**
- **Dato** uno spazio occupato da un cliente · **Quando** si legge la risposta del motore · **Allora** non contiene
  né il nome del cliente né il servizio prenotato: solo che quello spazio non è disponibile

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend`);
- [ ] prove di **unità** estese sui casi limite del calcolo e di **integrazione** sulla rotta;
- [ ] prova di **isolamento fra account**: la disponibilità di un account non risente delle prenotazioni
      dell'altro;
- [ ] **prova end-to-end**: *rimando* — il motore si vede attraverso l'agenda e la pagina pubblica, coperte dalle
      storie `0033` e `0034`, dove si aggiorna
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml);
- [ ] **traduzioni** dei messaggi in tutte e cinque le lingue;
- [ ] **manifesto dei dati**: nessuna voce nuova, con la ragione scritta (la risposta non contiene dati personali);
- [ ] **registro delle decisioni** compilato: gli spazi liberi non si memorizzano, e perché;
- [ ] avvio locale invariato;
- [ ] documentazione aggiornata.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storie `0006`, `0007`, `0008`, `0009` | sono le quattro sorgenti che il motore incrocia |

## 7. Fuori ambito

- gli impegni letti dal calendario esterno: sono una **quinta** sorgente, aggiunta dalla storia `0029`, e il
  motore va scritto in modo da poterla accogliere senza riscritture;
- la scelta automatica della risorsa migliore quando più d'una è libera: rimandata alla storia `0014`, che ha il
  contesto per farla.

## 8. Punti aperti

**Distribuzione degli appuntamenti quando più risorse sono libere.** Riempire prima una risorsa o distribuire fra
tutte sono due strategie con conseguenze economiche opposte (la prima lascia buchi lunghi liberi, la seconda
tiene tutti occupati a metà). È una decisione di prodotto, non tecnica: la proposta è offrire la scelta fra le
due nelle impostazioni, con «riempi prima» come predefinita, ma la decide lo sviluppatore.
