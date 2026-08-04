# 0011 — Importazione dell'anagrafica da file

**Applicazione**: 14 — StockGrove (`magazzino`) · **Epica**: 02 — Anagrafiche e catalogo prodotti
**Storia**: `0011` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0006`, `0007`, `0008`, `0009`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che ha già quattrocento articoli in un foglio di calcolo
> voglio caricare quel foglio e vedere prima cosa succederà
> così da cominciare a usare il programma in un pomeriggio invece che in tre settimane di ribattitura.

**Contesto.** Nessuno migra un'anagrafica a mano: o l'importazione c'è, o il cliente non parte (descrizione
dell'applicazione, §2.4 punto 6). È anche la ragione per cui la prova gratuita di quattordici giorni ha senso su
un'app che ha già un piano gratuito: il valore di StockGrove si vede solo dopo aver caricato l'anagrafica **vera**,
e cinquanta articoli non bastano a caricarla (descrizione, §5). La storia arriva a valle di tutte le anagrafiche
perché un file reale contiene in una riga sola l'articolo, il suo codice a barre, il deposito e il fornitore: se
si importasse prima, si importerebbe un quarto del file.

Il vincolo di progetto che governa questa storia è uno solo, e viene dal segmento: **niente caricamenti parziali
muti**. Un'importazione che scrive metà file e tace è peggio di un'importazione che rifiuta tutto, perché lascia
l'anagrafica in uno stato che nessuno sa descrivere e che il cliente scoprirà settimane dopo davanti a una
giacenza sbagliata.

## 2. Requisiti funzionali

1. **RF-1** — Si carica un file di valori separati da virgola con una riga di intestazione; le colonne si
   **associano** ai campi dell'articolo in una schermata (codice interno, descrizione, unità di misura, categoria,
   codice a barre, deposito, ubicazione, fornitore preferito), e l'associazione proposta si basa sui nomi trovati.
2. **RF-2** — Prima di scrivere qualunque cosa, l'app mostra un'**anteprima**: quante righe sono valide, quante
   creerebbero un articolo nuovo, quante ne aggiornerebbero uno esistente, quante sono in errore e **per quale
   motivo, riga per riga**.
3. **RF-3** — Chi importa sceglie cosa fare degli articoli già presenti (riconosciuti per codice interno o per
   codice a barre): **aggiornare** i campi non vuoti oppure **saltare** la riga. Non esiste una terza via
   implicita.
4. **RF-4** — L'importazione è **tutto o niente**: si esegue in una sola transazione e, se una riga valida
   fallisce in scrittura, non resta scritto nulla. Le righe **in errore** individuate in anteprima non fanno
   fallire l'importazione: vengono scartate e riportate nell'esito, e l'utente decide se procedere con le altre.
5. **RF-5** — L'esito è consultabile e **scaricabile** come file, con il numero di riga originale accanto a ogni
   errore, così da poterlo correggere nel foglio di partenza.
6. **RF-6** — L'importazione rispetta il tetto della quota: se il file porterebbe gli articoli attivi sopra il
   limite del piano, l'anteprima lo dice **prima** di scrivere, indicando quanti articoli entrerebbero, quanti ne
   restano disponibili e i rimedi — archiviare, passare di piano, oppure chiedere all'assistenza la deroga
   temporanea prevista per la migrazione iniziale ([estensioni-admin.md](../estensioni-admin.md)).
7. **RF-7** — I depositi e i fornitori nominati nel file e non esistenti **non si creano di nascosto**:
   l'anteprima li elenca e chiede conferma esplicita di crearli, oppure segna le righe come in errore.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** L'importazione scrive **solo** nell'account del token verificato; un
  `tenant_id` che arrivasse dal corpo della richiesta, dai parametri o da una colonna del file viene ignorato — una
  colonna di quel nome nel foglio non è un comando. Prova di isolamento fra due account sulla risorsa delle
  importazioni e sul risultato scritto.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `POST /api/magazzino/v1/importazioni` (caricamento e
  anteprima), `POST /api/magazzino/v1/importazioni/{id}/conferma`, `GET /api/magazzino/v1/importazioni/{id}` e
  `GET /api/magazzino/v1/importazioni/{id}/esito`; corpo validato; errori in `application/problem+json`; definizione
  OpenAPI aggiornata nello stesso commit. Il file caricato ha un limite di dimensione dichiarato e superarlo dà un
  errore parlante, non un fallimento generico.
- **RT-3 — Persistenza (§8).** Migrazione `V6__importazione_anagrafica.sql` sullo schema `app_magazzino`: tabelle
  `importazione` (stato, associazione delle colonne, conteggi, momento) e `importazione_riga` (numero di riga,
  esito, motivo dell'errore), con `tenant_id`, chiave primaria UUID versione 7, colonne di controllo e
  cancellazione logica. Il file caricato **non si conserva oltre il necessario**: si conserva l'esito, non il
  documento originale.
- **RT-4 — Modulo frontend (§3, §5).** Percorso in tre passi dentro la sezione `articoli` del modulo `magazzino`:
  caricamento → associazione delle colonne e anteprima → conferma ed esito. Dati letti con il client generato;
  solo token del sistema di design; funziona in tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe visibili — nomi dei passi, motivi di errore riga per riga,
  messaggio del tetto di quota — passano dallo spazio-nomi `magazzino` e sono presenti in `en, it, fr, es, de`. I
  motivi d'errore sono **chiavi con parametri**, non frasi composte a mano, altrimenti non si traducono.
- **RT-6 — Varchi e quota (§6, §7).** L'importazione prenota **tante unità** della metrica `articoli_gestiti`
  (natura `stock`) quanti sono gli articoli nuovi che creerebbe, e lo fa **prima** di scrivere: a tetto superato
  risponde `429` con conteggio e rimedi, e non scrive nulla. Gli articoli aggiornati non consumano nulla. Con
  abbonamento `canceled` risponde `402`.
- **RT-7 — Esposizione conversazionale (§12).** **Nessuno strumento**: caricare un file da una chat non è un gesto
  che ha senso, e un'importazione confermata a voce sarebbe una scrittura di massa senza l'anteprima che è il
  presidio di questa storia. L'esclusione va scritta nel contratto degli strumenti, non lasciata implicita.
- **RT-8 — Dati personali (§10).** Nessuna categoria nuova, ma due avvertenze: le colonne libere del file
  (descrizione, note) possono contenere qualunque cosa, e l'interfaccia lo avvisa; i fornitori creati
  dall'importazione portano dati personali già dichiarati nel manifesto (storia `0009`). Le tabelle
  `importazione` e `importazione_riga` entrano in `exportData` e `purgeData` perché i motivi d'errore possono
  citare valori del file.
- **RT-9 — Registrazione eventi (§14).** Gli eventi `importazione caricata`, `anteprima calcolata`,
  `importazione confermata`, `importazione respinta per quota` sono registrati con `tenant_id`, `app_id`,
  `user_id`, identificativo di correlazione e i **conteggi**, mai il contenuto delle righe.

## 4. Criteri di accettazione

**CA-1 — Anteprima prima della scrittura**
- **Dato** un file di 400 righe di cui 12 senza codice interno
- **Quando** lo si carica e si associano le colonne
- **Allora** l'anteprima dichiara 388 righe valide e 12 in errore con il numero di riga e il motivo, e **nel
  database non è stato scritto nessun articolo**

**CA-2 — Conferma e tutto o niente**
- **Dato** l'anteprima precedente, confermata con l'opzione «aggiorna gli esistenti»
- **Quando** una scrittura fallisce a metà per un errore imprevisto
- **Allora** nessuna delle 388 righe risulta scritta, l'importazione è in stato `fallita` e l'esito lo spiega

**CA-3 — Tetto di quota superato**
- **Dato** un account sul piano `pro` con 480 articoli attivi su 500 e un file che ne creerebbe 60 nuovi
- **Quando** si chiede la conferma
- **Allora** la risposta è `429`, il messaggio dice «60 nuovi, 20 disponibili» ed elenca i rimedi compresa la
  deroga temporanea dell'assistenza, e nulla viene scritto

**CA-4 — Deposito nominato ma inesistente**
- **Dato** un file che nomina il deposito «Furgone 2», non presente in anagrafica
- **Quando** si guarda l'anteprima
- **Allora** l'app elenca «Furgone 2» fra le entità mancanti e chiede conferma esplicita di crearlo; senza conferma
  le righe che lo citano risultano in errore e **nessun deposito viene creato di nascosto**

**CA-5 — Isolamento fra account**
- **Dato** due account `A` e `B` e un file caricato da `A` con una colonna chiamata `tenant_id` valorizzata con
  l'identificativo di `B`
- **Quando** l'importazione viene confermata
- **Allora** tutti gli articoli risultano creati in `A`, nessuno in `B`, e la colonna è stata ignorata

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno backend e frontend; l'intera suite prima del commit);
- [ ] prove di **unità** sull'analisi del file, sull'associazione delle colonne e sul calcolo dei conteggi
      dell'anteprima; prove di **integrazione** sul percorso completo con un file di prova **inventato**, con
      database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** su `importazione` e sul risultato scritto;
- [ ] **prova end-to-end**: *rimando* — l'importazione non è nel percorso `[J-MAGAZZINO]` di proprietà della storia
      `0036`, che parte da un articolo creato a mano per restare leggibile; se il percorso venisse esteso, la voce
      si aggiunge in [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) lì. Il controllo di
      accessibilità automatico sul percorso in tre passi è invece in questa storia;
- [ ] **traduzioni** presenti in tutte e cinque le lingue (`en, it, fr, es, de`), motivi d'errore compresi;
- [ ] **manifesto dei dati** aggiornato per `importazione` e `importazione_riga`, con le tabelle presenti in
      esportazione e cancellazione;
- [ ] **registro delle decisioni** compilato, con la scelta del «tutto o niente», della non creazione implicita di
      depositi e fornitori e della non conservazione del file originale;
- [ ] contratto degli **strumenti conversazionali**: esclusione esplicita dell'importazione;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| `0006` | L'anagrafica degli articoli è ciò che si importa |
| `0007` | Le colonne dei codici a barre hanno senso solo se la tabella dei codici esiste |
| `0008` | Il file nomina depositi e ubicazioni |
| `0009` | Il file nomina fornitori preferiti |
| `0004` | La prenotazione multipla di quota passa dalla catena dei varchi |

## 7. Fuori ambito

- **Importazione dei movimenti e delle giacenze iniziali**: è un'altra cosa e ha un'altra storia, la `0018`. Qui si
  importa **cosa** si conta, non **quanto** ce n'è. La distinzione è la ragione per cui l'articolo non ha e non
  avrà mai una colonna di quantità.
- **Formati diversi dal valore separato da virgola** (fogli di calcolo binari, connettori verso programmi di terzi):
  non nel perimetro; il file separato da virgola è ciò che ogni foglio di calcolo esporta.
- **Importazioni programmate o ricorrenti**: fuori perimetro. Gli aggiornamenti continui arrivano per eventi dalle
  altre app della suite (storia `0019`), non da un file caricato ogni notte.
- **Correzione delle righe in errore dentro l'app**: si corregge il foglio e si ricarica. Un editor di righe
  sbagliate è una schermata in più che nessuno ha chiesto.

## 8. Punti aperti

- **Dimensione massima del file e numero massimo di righe per importazione**: la proposta è un limite dichiarato e
  visibile, ma il valore giusto dipende dalla memoria del servizio e non l'ho dimensionato. Chiude lo sviluppatore
  in fase di realizzazione, misurando.
- **Deroga temporanea al tetto per la migrazione iniziale**: la propone la console di amministrazione
  ([estensioni-admin.md](../estensioni-admin.md)) e presuppone che la piattaforma sappia applicare un tetto
  alternativo con scadenza. Non ho verificato che il meccanismo esista. Chiude lo sviluppatore.