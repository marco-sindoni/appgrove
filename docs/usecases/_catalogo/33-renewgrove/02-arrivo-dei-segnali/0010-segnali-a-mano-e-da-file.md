# 0010 — Segnali a mano e da file

**Applicazione**: 33 — RenewGrove (`fidelizzazione`) · **Epica**: 02 — Arrivo dei segnali dalle altre app
**Storia**: `0010` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0009`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare di una carrozzeria che ha quaranta clienti fissi, un foglio di calcolo e nessuna delle altre app
> voglio caricare il mio elenco di clienti e segnare io stesso i fatti che contano
> così da poter usare RenewGrove oggi, invece di aspettare di aver comprato altre tre applicazioni.

**Contesto.** RenewGrove **non ha senso da sola**: senza almeno una fonte collegata è un contenitore vuoto, e la
descrizione lo dice apertamente (§10). Questa storia non annulla quella soglia — nessuna storia può — ma la
**abbassa**, e copre il caso più frequente del segmento: il cliente che ha l'elenco in un foglio di calcolo e la
memoria di quel che è successo. Il foglio di calcolo, dice il §2.4, «è sempre la prima cosa che chiedono, e si
sottovaluta sempre». La regola che tiene tutto in piedi è una sola e non si piega per comodità: **i fatti segnati a
mano rispettano lo stesso contratto dei fatti importati**, cioè elenco chiuso di tipi e nessun testo libero. Se qui
si aprisse una casella di testo, il presidio dell'articolo 9 costruito in `0006` cadrebbe dal lato più facile.

## 2. Requisiti funzionali

1. **RF-1** — Esiste la fonte `inserimento_manuale`, che è **una fonte come le altre**: compare nella sezione
   Fonti, ha i suoi stati `collegata / sospesa / scollegata`, e la sua revoca è distruttiva e informata esattamente
   come quella delle altre (`0008`).
2. **RF-2** — Si può importare un elenco di clienti da un file tabellare, con **anteprima prima di scrivere**: il
   sistema mostra quante righe sono valide, quante saranno scartate e perché, e quanti rapporti nuovi entrerebbero
   in sorveglianza rispetto alla quota disponibile. Nulla viene scritto finché l'anteprima non è confermata.
3. **RF-3** — L'importazione crea rapporti con la loro etichetta leggibile e, quando le colonne ci sono, i segnali
   corrispondenti; è **idempotente**: reimportare lo stesso file non duplica nulla.
4. **RF-4** — Un utente può registrare un segnale a mano su un rapporto scegliendo **solo** fra i tipi dell'elenco
   chiuso della fonte `inserimento_manuale`, con momento del fatto e intensità nell'unità dichiarata. **Nessun campo
   di testo libero**, in nessun punto della schermata.
5. **RF-5** — I segnali registrati a mano sono **marcati come tali**, e la spiegazione del punteggio lo dice: chi
   legge deve poter distinguere un fatto arrivato da un'applicazione che lo ha registrato per un altro motivo da un
   fatto segnato da un collega a memoria.
6. **RF-6** — Registrare un segnale a mano è consentito a `owner`, `admin` e `member`; importare un elenco solo a
   `owner` e `admin`, perché tocca la quota e crea rapporti in massa.
7. **RF-7** — L'importazione oltre il tetto non fallisce a metà: i rapporti che eccedono la quota nascono
   `archiviato`, come nella storia `0009`, e il rendiconto finale dice quanti sono e come rimediare.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Importazione e registrazione manuale scrivono con il `tenant_id` preso dal
  **token verificato** — è il solo caso in cui un segnale nasce da una richiesta e non da un evento, e va detto
  espressamente perché è l'eccezione alla regola di `0007`. Il file caricato non può portare un identificativo di
  account: se una colonna lo contenesse, viene ignorata.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `POST /api/fidelizzazione/v1/importazioni` (anteprima),
  `POST /api/fidelizzazione/v1/importazioni/{id}/conferma` e
  `POST /api/fidelizzazione/v1/rapporti/{id}/segnali-manuali`; corpo validato; errori in
  `application/problem+json`; definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Contratto del segnale (`0006`).** I segnali manuali passano dal **medesimo validatore** dei segnali
  importati: elenco chiuso di tipi, intensità con unità, nessun testo libero, nessun campo anagrafico oltre
  l'etichetta del rapporto. Il validatore non ha una modalità permissiva.
- **RT-4 — Persistenza (§8).** Migrazione sullo schema `app_fidelizzazione` che aggiunge il marcatore di origine
  manuale a `segnale`, in sola aggiunta come tutto il resto della tabella; `tenant_id`, chiave primaria UUID
  versione 7, colonne di controllo, cancellazione logica.
- **RT-5 — Modulo frontend (§3, §5).** Schermata di importazione con anteprima nella sezione **Fonti** e azione di
  registrazione manuale nella scheda del rapporto (sezione **Rapporti**); moduli di inserimento con validazione
  dichiarativa; solo token del sistema di design; funziona in tema chiaro e scuro.
- **RT-6 — Cinque lingue (§4).** Testi dell'anteprima, motivi di scarto riga per riga, nomi dei tipi di segnale
  manuali e marcatore «segnato a mano» passano dallo spazio-nomi `fidelizzazione` e sono presenti in
  `en, it, fr, es, de`.
- **RT-7 — Varchi e quota (§6).** L'importazione prenota quota sulla metrica `rapporti_sorvegliati` (natura
  `stock`) per ogni rapporto che entra in sorveglianza; a tetto raggiunto i rapporti eccedenti nascono `archiviato`
  e la risposta dice quanti e come rimediare. Con abbonamento non attivo risponde `402`.
- **RT-8 — Dati personali (§10).** Il file importato **contiene dati personali**: etichette leggibili di clienti
  finali. Va trattato di conseguenza — il file caricato non si conserva oltre il tempo dell'importazione, non
  compare nei registri e non è scaricabile in seguito; solo i dati estratti finiscono nelle tabelle già dichiarate
  (`rapporto`, `segnale`), che sono già in `exportData` e `purgeData`. Il manifesto va aggiornato in **italiano e
  inglese** con la voce sull'origine manuale del segnale e con l'esclusione esplicita della conservazione del file.
  L'interfaccia porta l'avvertenza di non caricare colonne con recapiti, indirizzi, identificativi fiscali o note.
- **RT-9 — Esposizione conversazionale (§12).** Nessuno strumento nuovo: l'importazione di un file non è
  un'operazione che si comanda da una chat, e registrare un fatto a mano dalla chat è deliberatamente escluso perché
  aumenterebbe la superficie in cui un testo libero rientra dalla finestra. La scelta va scritta nel registro delle
  decisioni. Server conversazionale di piattaforma, non ancora implementato (UC 0061-0063).
- **RT-10 — Registrazione eventi (§14).** «importazione avviata», «importazione confermata con N rapporti e M
  segnali», «righe scartate per regola X», «segnale manuale registrato» con `tenant_id`, `app_id`, `user_id` e
  correlazione, **senza** etichette e senza contenuti del file.
- **RT-11 — Prove (§11).** Unità sul lettore del file tabellare e sull'idempotenza; integrazione sull'anteprima e
  sulla conferma, con database effimero e migrazioni vere; prova che un segnale manuale con un tipo fuori elenco è
  rifiutato; prova che l'importazione oltre il tetto non fallisce a metà; isolamento fra due account; controllo
  automatico di accessibilità sulle schermate introdotte.

## 4. Criteri di accettazione

**CA-1 — Anteprima prima di scrivere**
- **Dato** un file con 60 righe, di cui 4 senza etichetta
- **Quando** un `admin` lo carica
- **Allora** vede che 56 righe sono valide, che 4 saranno scartate con il motivo riga per riga e quanti rapporti
  entrerebbero in sorveglianza; **nulla è stato scritto** finché non conferma

**CA-2 — Reimportare non duplica**
- **Dato** un'importazione già confermata · **Quando** si reimporta lo stesso file
- **Allora** non si creano rapporti né segnali doppi, e il rendiconto dice quante righe erano già presenti

**CA-3 — Niente testo libero nei segnali a mano**
- **Dato** la schermata di registrazione manuale di un segnale
- **Quando** un utente la usa
- **Allora** può scegliere solo fra i tipi dell'elenco chiuso e indicare momento e intensità; non esiste alcun campo
  di testo libero, e una richiesta costruita a mano con un tipo fuori elenco è rifiutata con la regola violata

**CA-4 — Il segnale a mano si riconosce**
- **Dato** un rapporto con segnali importati dalla fatturazione e segnali segnati a mano
- **Quando** si apre la spiegazione del punteggio
- **Allora** i secondi sono marcati come registrati a mano, con autore e momento della registrazione

**CA-5 — Importazione oltre il tetto**
- **Dato** un account sul piano `free` con 15 rapporti sorvegliati e un file da 40 clienti
- **Quando** conferma l'importazione
- **Allora** i 40 rapporti sono creati, nessuno entra in sorveglianza, il rendiconto dice quanti sono archiviati per
  quota e come rimediare, e nessun dato è perso

**CA-6 — La revoca della fonte manuale è come le altre**
- **Dato** la fonte `inserimento_manuale` collegata, con 300 segnali segnati a mano
- **Quando** un `owner` la revoca
- **Allora** prima della conferma legge quanti segnali verranno cancellati e su quali rapporti il punteggio
  diventerà non calcolabile; alla conferma la cancellazione è fisica e lascia una riga nel registro delle purghe

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend`, `frontend` e `compliance`; l'intera suite prima del
      commit);
- [ ] prove di **unità** sul lettore del file e sull'idempotenza, e di **integrazione** su anteprima e conferma, con
      database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** sulle rotte di importazione e di registrazione manuale, con tentativo di
      forzare l'account da una colonna del file;
- [ ] **prova end-to-end**: *rimando* alla storia `0030`, che dovrà coprire il percorso di partenza a freddo
      «importo un elenco → segno un fatto a mano → il rapporto compare»; voce `da-coprire` nel registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml);
- [ ] **traduzioni** presenti in tutte e cinque le lingue (`en, it, fr, es, de`), motivi di scarto compresi;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese con l'origine manuale del segnale e l'esclusione
      esplicita della conservazione del file caricato;
- [ ] **registro delle decisioni** compilato: la fonte manuale come fonte a tutti gli effetti, il divieto di testo
      libero anche a mano, e perché la registrazione manuale non è uno strumento conversazionale;
- [ ] contratto degli **strumenti conversazionali**: nessuna funzione nuova, con la motivazione scritta;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0009` | servono il rapporto, l'etichetta e la macchina degli stati di sorveglianza su cui l'importazione scrive |
| storia `0008` (per la forma) | la fonte `inserimento_manuale` riusa stati, collegamento e revoca già costruiti lì |

## 7. Fuori ambito

- l'esportazione dei rapporti verso un file: non è prevista in questa epica; l'esportazione dei dati
  dell'interessato è invece un diritto e sta nella storia `0032`;
- la correzione di un segnale segnato a mano per sbaglio: è una riga nuova che rende superata la precedente
  (`0002`), e la forma visibile della correzione è della storia `0015`;
- il collegamento delle fonti applicative: storia `0008`.

## 8. Punti aperti

- **L'elenco chiuso dei tipi della fonte `inserimento_manuale` non è ancora fissato.** Deve essere corto e
  comprensibile a chi non ha altre app — una prima proposta ragionevole è «ha pagato in ritardo», «si è lamentato»,
  «non compra da un po'», «ha disdetto un appuntamento» — ma sceglierlo è una decisione di prodotto che tocca
  direttamente la credibilità del punteggio. Chiude: lo sviluppatore, insieme alla storia `0012`.
- **I segnali segnati a mano devono pesare meno di quelli importati?** Un fatto ricordato da un collega non ha la
  stessa affidabilità di un fatto registrato da un'applicazione al momento in cui è accaduto. La proposta è di
  **non** introdurre un peso diverso adesso, ma di marcarli e lasciare che il rendiconto di efficacia (`0027`) dica
  se la distinzione serve. Chiude: la storia `0012`.
