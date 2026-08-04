# 0024 — Disdetta con un pulsante

**Applicazione**: 19 — SubGrove (`abbonati`) · **Epica**: 05 — Portale dell'abbonato
**Storia**: `0024` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0023`, `0011`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come persona abbonata che ha deciso di smettere
> voglio disdire con un clic, dalla stessa pagina in cui vedo l'abbonamento, senza telefonare a nessuno
> così da chiudere la cosa in trenta secondi e avere per iscritto che l'ho fatta.

**Contesto.** Non è una gentilezza verso l'abbonato: è **conformità del nostro cliente**, e l'app è lo strumento
con cui la ottiene. Le fonti del §2.3 della descrizione convergono: in Germania il § 312k del codice civile
impone un **pulsante di disdetta** ben riconoscibile, con dicitura chiara, **raggiungibile senza inserire
credenziali** e disponibile per tutta la procedura, con diffide e azioni inibitorie per chi non lo mette; in
Italia, per i contratti sottoscritti online, si richiede un canale digitale di recesso **almeno tanto semplice
quanto quello di adesione**, e sono vietate le pratiche dilatorie — centralino obbligatorio, moduli
irraggiungibili, finestre irragionevoli.

Ne discendono due regole di progetto che non si negoziano: **nessuna credenziale** per disdire, e **nessun
ostacolo** — niente domande obbligatorie prima del pulsante, niente offerte di trattenimento che blocchino il
percorso, niente conferme a catena. Si può chiedere il motivo **dopo** aver disdetto, e solo come domanda
facoltativa.

## 2. Requisiti funzionali

1. **RF-1** — Sulla pagina pubblica dell'abbonamento c'è un pulsante di disdetta **ben visibile**, con dicitura
   chiara e inequivocabile, raggiungibile **senza inserire credenziali** e senza passaggi intermedi.
2. **RF-2** — Premuto il pulsante, l'abbonato vede un riepilogo di **cosa succede**: fino a quando resta
   coperto, cosa non pagherà più, e conferma con un secondo clic. Non ci sono altri passaggi.
3. **RF-3** — Alla conferma l'abbonamento passa a `disdetto_a_scadenza`: resta valido fino a fine periodo, senza
   rimborso del periodo in corso, e non si rinnova.
4. **RF-4** — L'abbonato riceve **subito** una ricevuta scritta al proprio recapito: quando ha disdetto, quale
   abbonamento, fino a quando resta coperto.
5. **RF-5** — La richiesta resta come **prova**: momento, gettone usato, esito, e la si può esibire in caso di
   contestazione. La prova non si cancella con la disdetta.
6. **RF-6** — Il motivo della disdetta si può chiedere **dopo** la conferma, con una domanda **facoltativa** e
   un elenco chiuso; saltarla non ha alcuna conseguenza.
7. **RF-7** — Se il piano prevede una durata minima non ancora raggiunta, la pagina **non** blocca la disdetta:
   informa di quando avrà effetto secondo le condizioni del piano, e registra comunque la richiesta.
8. **RF-8** — Il cliente vede le disdette arrivate in un elenco dedicato, con l'abbonato e il momento.

## 3. Requisiti tecnici

- **RT-1 — Superficie pubblica.** Nessuna credenziale: l'unica autorizzazione è il gettone firmato della storia
  `0023`. Un percorso che chiedesse di autenticarsi sarebbe una violazione dell'obbligo, non un dettaglio.
- **RT-2 — Isolamento fra account (§1).** Il gettone identifica account e abbonamento; nessuna disdetta può
  toccare un abbonamento di un altro account.
- **RT-3 — Ciclo di vita (§ storia `0011`).** Il passaggio a `disdetto_a_scadenza` avviene dalla macchina a
  stati, con motivo «richiesta dell'abbonato» e la cronologia che ne conserva l'origine pubblica.
- **RT-4 — Interfaccia di programmazione (§2).** Rotte pubbliche
  `POST /api/abbonati/v1/pubblico/{gettone}/disdetta/anteprima` e `.../disdetta`; risposta che dice fino a quando
  l'abbonamento resta valido; errori in `problem+json`; OpenAPI aggiornata.
- **RT-5 — Persistenza (§8).** Migrazione `V18__richiesta_abbonato.sql`: tabella `richiesta_dell_abbonato` con
  `tenant_id`, colonne di controllo, tipo, momento, esito, prova. **Non** si cancella logicamente quando
  l'abbonamento cessa: è la prova che l'obbligo è stato rispettato.
- **RT-6 — Modulo frontend (§3, §5).** Pulsante in evidenza sulla pagina pubblica, con contrasto e dimensione
  adeguati; nessun elemento che ne distolga; solo token del sistema di design; funziona su telefono.
- **RT-7 — Cinque lingue (§4).** Dicitura del pulsante, riepilogo, ricevuta e motivi in `en, it, fr, es, de`.
  **Attenzione**: la dicitura tedesca deve essere quella prevista dalla norma o una formulazione altrettanto
  chiara — non una traduzione libera (vedi punto aperto).
- **RT-8 — Comunicazioni.** La ricevuta si compone con il **renderer condiviso** della piattaforma (change
  `0079`).
- **RT-9 — Esposizione conversazionale (§12).** Strumento dichiarato:
  `disdici_abbonamento(abbonamento, decorrenza, motivo) → bozza`, marcato **scrittura irreversibile**, con
  **conferma umana obbligatoria** — è la disdetta fatta dal **cliente**, non dall'abbonato: la disdetta
  dell'abbonato passa solo dalla pagina pubblica.
- **RT-10 — Dati personali (§10).** La richiesta e la sua prova sono dati riferiti a una persona: voce nuova nel
  manifesto in italiano e inglese, tabella in `exportData` e `purgeData`, con la conservazione della prova che
  è il punto aperto n. 8 della descrizione.
- **RT-11 — Registrazione eventi (§14).** `disdetta ricevuta dal portale`, con `tenant_id`, `app_id` e
  correlazione, senza nomi né gettone.
- **RT-12 — Prove (§11).** Prova che il percorso di disdetta **non** richiede autenticazione in nessun passaggio
  e che è raggiungibile in **due** interazioni dalla pagina: è la prova che tutela dall'obbligo, e va scritta in
  modo che si rompa se qualcuno aggiunge un passaggio.

## 4. Criteri di accettazione

**CA-1 — Disdetta in due clic, senza credenziali**
- **Dato** un abbonato che apre il collegamento ricevuto per posta
- **Quando** preme il pulsante di disdetta e conferma
- **Allora** l'abbonamento è `disdetto_a_scadenza`, in nessun passaggio è stata chiesta una credenziale, e non
  ci sono stati altri passaggi

**CA-2 — Riepilogo onesto prima della conferma**
- **Dato** un abbonamento mensile con periodo fino al 30 · **Quando** l'abbonato preme il pulsante
- **Allora** legge «resti coperto fino al 30, non ti sarà addebitato altro» prima di confermare

**CA-3 — Ricevuta scritta**
- **Dato** una disdetta confermata · **Quando** si guarda il recapito dell'abbonato
- **Allora** è arrivata la ricevuta con momento, abbonamento e data di fine copertura

**CA-4 — Il motivo è facoltativo**
- **Dato** la schermata successiva alla conferma · **Quando** l'abbonato chiude senza rispondere
- **Allora** la disdetta resta valida e non succede nulla di diverso

**CA-5 — Durata minima non ancora raggiunta**
- **Dato** un piano con durata minima di 12 mesi e un abbonamento al terzo mese
- **Quando** l'abbonato disdice
- **Allora** la disdetta **è registrata**, la pagina dice da quando avrà effetto secondo le condizioni, e nulla è
  bloccato

**CA-6 — Prova conservata**
- **Dato** una disdetta di sei mesi fa e l'abbonamento ormai cessato
- **Quando** il cliente cerca la prova
- **Allora** la trova con momento ed esito

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend`, `frontend`, `compliance`);
- [ ] prove di **unità** sul calcolo della data di fine copertura; **integrazione** sulle rotte pubbliche;
- [ ] prova che il percorso **non** richiede autenticazione e sta in due interazioni;
- [ ] prova di **isolamento fra account** sul gettone;
- [ ] **prova end-to-end**: *coprire ora* — è il cuore del percorso `[J-ABBONATI-PUBBLICO]` della storia `0034`;
      registro [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) aggiornato;
- [ ] **traduzioni** presenti in tutte e cinque le lingue, **con la dicitura tedesca verificata**;
- [ ] **manifesto dei dati** aggiornato con la richiesta e la sua prova;
- [ ] **registro delle decisioni** compilato: nessuna credenziale, nessun ostacolo, motivo facoltativo e
      posteriore, prova conservata oltre la fine del rapporto;
- [ ] contratto dello strumento `disdici_abbonamento` dichiarato con conferma obbligatoria;
- [ ] controllo di accessibilità verde sul percorso di disdetta.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0023` | serve la pagina e il gettone |
| storia `0011` | il passaggio di stato passa dalla macchina |
| storia `0026` | le difese della superficie pubblica: **le due vanno rilasciate insieme** |
| storia `0013` | l'avviso di rinnovo deve portare il collegamento a questa pagina, altrimenti l'obbligo di preavviso resta monco |

## 7. Fuori ambito

- il rimborso del periodo in corso: non esiste, coerentemente con la semantica adottata (§10.1 della descrizione);
- le offerte di trattenimento («resta con noi, ti facciamo uno sconto»): **deliberatamente escluse**, perché il
  loro posto naturale è dentro il percorso di disdetta, dove sarebbero un ostacolo vietato;
- la richiesta di cambio piano: storia `0025`, che è una richiesta, non un diritto.

## 8. Punti aperti

**La dicitura esatta del pulsante per giurisdizione.** La norma tedesca prevede una formulazione precisa o
«altrettanto chiara», e la giurisprudenza citata al §2.3 è severa sui dettagli — visibilità permanente,
raggiungibilità senza credenziali. Le altre lingue non hanno una formula imposta che io abbia verificato.
**Proposta**: dicitura letterale in tedesco, formulazione chiara e diretta nelle altre, e la questione portata
alla revisione legale prima del rilascio. Chiude: **revisione legale**
([docs/_REVISIONE-LEGALE.md](../../../../_REVISIONE-LEGALE.md)).

**Quando ha effetto la disdetta se c'è una durata minima.** La proposta registra la richiesta e informa, senza
bloccare; ma *da quando* abbia effetto dipende dalle condizioni del piano e dalla legge applicabile, e le fonti
italiane consultate suggeriscono che in mancanza del preavviso dovuto il recesso possa essere immediato e senza
penali. Non sono in grado di stabilirlo. Chiude: revisione legale.
