# 0015 — Esiti della prenotazione

**Applicazione**: 07 — BookGrove (`prenotazioni`) · **Epica**: 03 — Anagrafica dei clienti e agenda interna
**Storia**: `0015` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0014`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare
> voglio registrare in un secondo che un appuntamento è saltato, e sapere se il cliente aveva avvisato oppure no
> così da capire quanto mi costano davvero le mancate presentazioni e con chi vale la pena chiedere un acconto.

**Contesto.** La mancata presentazione è il problema economico che questa applicazione esiste per ridurre: la
scheda di catalogo lo dice esplicitamente («riduce i no-show, quindi salva fatturato»). Ma per ridurla bisogna
prima **misurarla**, e per misurarla serve che il personale la registri in un gesto solo, altrimenti non lo farà
mai. Questa storia chiude la macchina a stati della prenotazione e crea il dato su cui poggiano gli indicatori
della storia `0026`.

## 2. Requisiti funzionali

1. **RF-1** — Dall'agenda si porta una prenotazione a `eseguita`, `non_presentato`, `disdetta_dal_cliente` o
   `disdetta_dall_attivita`, in un gesto, con il motivo facoltativo.
2. **RF-2** — Lo spostamento cambia orario o risorsa **senza** cambiare stato, e lascia una traccia con il valore
   precedente: uno spostamento non è una disdetta più una nuova prenotazione, e confonderli falsa i conteggi.
3. **RF-3** — Una disdetta o uno spostamento **libera lo spazio** e fa scattare l'offerta a chi è in lista
   d'attesa (storia `0020`), se ce n'è.
4. **RF-4** — Gli appuntamenti passati che nessuno ha chiuso restano in uno stato **da definire** e sono
   evidenziati: il programma non decide da solo che qualcuno non si è presentato.
5. **RF-5** — Ogni passaggio di stato è registrato in `evento_prenotazione` con chi, quando e da quale superficie
   (banco, pagina pubblica, assistente).
6. **RF-6** — Gli stati finali non tornano indietro; una correzione di un errore è un atto esplicito e tracciato,
   non una modifica silenziosa.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni passaggio di stato filtra per `tenant_id` preso dal token
  verificato.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `POST /api/prenotazioni/v1/prenotazioni/{id}/stato` e
  `POST /api/prenotazioni/v1/prenotazioni/{id}/spostamento`; corpo validato; errori in `problem+json` con codice
  stabile per «transizione non ammessa»; OpenAPI aggiornata.
- **RT-3 — Persistenza (§8).** Migrazione `V8__stati_prenotazione.sql`: colonna di stato con vincolo sui valori
  ammessi ed estensione di `evento_prenotazione`; il vincolo di non sovrapposizione vale solo per gli stati che
  occupano tempo, così che una disdetta liberi davvero lo spazio.
- **RT-4 — Modulo frontend (§3, §5).** Azioni raggiungibili dal blocco in agenda senza aprire una schermata
  nuova; conferma esplicita solo per le azioni che scrivono al cliente; solo token del sistema di design; tema
  chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Etichette degli stati e messaggi in `en, it, fr, es, de`.
- **RT-6 — Dati personali (§10).** Il motivo della disdetta è **testo libero** e va dichiarato come tale nel
  manifesto in italiano e inglese, annotato `@PersonalData`, con l'avviso a schermo di non scriverci informazioni
  sulla salute della persona.
- **RT-7 — Registrazione eventi (§14).** `prenotazione disdetta`, `mancata presentazione registrata`,
  `prenotazione spostata` con `tenant_id`, `app_id`, `user_id`, correlazione e stato precedente — **mai il
  motivo né il nome del cliente**.
- **RT-8 — Esposizione conversazionale (§12).** Le operazioni introdotte qui sono la base degli strumenti
  `disdici_prenotazione` e `sposta_prenotazione`, dichiarati nella storia `0032`: entrambi di **scrittura**, con
  bozza e conferma umana obbligatoria perché toccano un impegno preso con una persona.

## 4. Criteri di accettazione

**CA-1 — Mancata presentazione in un gesto**
- **Dato** un appuntamento di ieri ancora aperto
- **Quando** si tocca il blocco e si sceglie «non presentato»
- **Allora** lo stato cambia, la scheda del cliente lo riporta e il conteggio aumenta

**CA-2 — Lo spostamento non è una disdetta**
- **Dato** un appuntamento spostato dalle 15 alle 17
- **Quando** si guardano i conteggi
- **Allora** non risulta nessuna disdetta, e la traccia riporta l'orario precedente

**CA-3 — Lo spazio si libera**
- **Dato** un appuntamento disdetto · **Quando** si ricalcola la disponibilità · **Allora** quello spazio è di
  nuovo prenotabile

**CA-4 — Transizione non ammessa**
- **Dato** un appuntamento già `eseguita` · **Quando** si prova a portarlo a `richiesta` · **Allora** la
  richiesta è rifiutata con l'errore stabile e nulla cambia

**CA-5 — Appuntamenti passati non chiusi**
- **Dato** tre appuntamenti di ieri mai chiusi · **Quando** si apre l'agenda · **Allora** sono evidenziati come da
  definire, e nessuno è stato marcato automaticamente

**CA-6 — Isolamento fra account**
- **Dato** l'identificativo di una prenotazione di un altro account · **Quando** si prova a cambiarne lo stato
- **Allora** la richiesta è rifiutata

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend`, `frontend` e `compliance`);
- [ ] prove di **unità** sulla macchina a stati e di **integrazione** sulle rotte;
- [ ] prova di **isolamento fra account** su ogni passaggio di stato;
- [ ] **prova end-to-end**: **coperta ora** — chiusura del percorso `[J-BOOKGROVE]` della storia `0033`, con il
      registro [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml);
- [ ] **traduzioni** in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato con il motivo della disdetta;
- [ ] **registro delle decisioni** compilato: la distinzione fra spostamento e disdetta, e il rifiuto della
      chiusura automatica degli appuntamenti passati;
- [ ] contratto degli **strumenti conversazionali** predisposto per `disdici_prenotazione` e
      `sposta_prenotazione`;
- [ ] avvio locale invariato;
- [ ] documentazione aggiornata.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0014` | serve la prenotazione da far transitare |
| storia `0020` | per l'offerta del posto liberato; se non esiste ancora, la disdetta libera lo spazio e basta |

## 7. Fuori ambito

- il messaggio automatico al cliente quando l'attività disdice: motore dei promemoria, storia `0022`;
- la penale in caso di disdetta tardiva: storia `0024`;
- gli indicatori: storia `0026`.

## 8. Punti aperti

**Chiusura automatica dopo molto tempo.** Lasciare in eterno appuntamenti «da definire» sporca i conteggi; ma
marcarli automaticamente come eseguiti falsa il tasso di mancata presentazione, e marcarli come non presentati è
peggio. Proposta: dopo un periodo configurabile passano a uno stato `scaduta senza esito`, che esiste ma non entra
in nessuno dei due conteggi. Da confermare con la storia `0026`.
