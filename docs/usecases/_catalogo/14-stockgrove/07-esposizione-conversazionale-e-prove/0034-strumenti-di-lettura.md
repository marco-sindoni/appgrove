# 0034 — Contratto degli strumenti di lettura

**Applicazione**: 14 — StockGrove (`magazzino`) · **Epica**: 07 — Esposizione conversazionale e prove end-to-end
**Storia**: `0034` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0013`, `0014`, `0015`, `0027`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare di una micro-impresa che sta in piedi davanti a uno scaffale
> voglio poter chiedere a voce quante ne ho, dove sono e cosa sta finendo
> così da avere la risposta senza tornare al computer e senza aprire un programma.

**Contesto.** Le epiche precedenti hanno costruito il registro dei movimenti, la giacenza derivata e l'avviso di
sotto scorta: tutto è raggiungibile da una schermata, e la schermata richiede di sedersi. Le domande vere di questo
dominio — «quante ne ho?», «dov'è finito?», «cosa devo ricomprare?» — si fanno con le mani occupate, ed è la ragione
per cui la descrizione dell'applicazione (§7) considera il livello conversazionale un guadagno di **modo d'uso**,
non un contorno. Questa storia dichiara il **contratto** dei cinque strumenti di sola lettura: nomi stabili,
descrizioni in lingua naturale, schemi dei parametri e del risultato. Non costruisce il server che li espone —
quello è di piattaforma (UC 0061-0063) e non è ancora implementato — ma tiene il contratto **dentro** il servizio
`magazzino`, versionato con esso, così che il giorno in cui il server esisterà l'app abbia già la sua superficie
dichiarata e non la si debba inventare a posteriori.

## 2. Requisiti funzionali

1. **RF-1** — Il servizio `magazzino` dichiara cinque strumenti di **sola lettura**, tutti idempotenti e privi di
   effetti: `leggi_giacenza(codice_o_gtin, deposito?)`, `trova_articolo(testo_o_codice)`,
   `elenca_articoli(categoria?, deposito?, solo_sotto_scorta?, pagina?)`, `elenca_sotto_scorta(deposito?)`,
   `storico_movimenti(articolo, deposito?, periodo?)`.
2. **RF-2** — Ogni strumento porta un **nome stabile** (non cambia più una volta pubblicato), una descrizione in
   lingua naturale rivolta a un assistente, lo schema dei parametri con quali sono obbligatori, lo schema del
   risultato e la marcatura `lettura` con idempotenza dichiarata.
3. **RF-3** — I risultati sono **minimizzati**: si restituisce ciò che serve a rispondere alla domanda, non
   l'entità intera. `leggi_giacenza` restituisce quantità per deposito e totale, non il registro dei movimenti;
   `trova_articolo` restituisce codice, descrizione, ubicazione e giacenza per deposito, non le soglie né il costo.
4. **RF-4** — Gli strumenti che possono restituire molte righe (`elenca_articoli`, `elenca_sotto_scorta`,
   `storico_movimenti`) sono **paginati** a pagina/dimensione con il totale, hanno una dimensione predefinita
   contenuta e una dimensione massima, e dichiarano nel risultato se ci sono altre pagine.
5. **RF-5** — Ogni chiamata attraversa la **stessa catena dei varchi** delle rotte pubbliche — utente autenticato,
   app accesa, account abilitato, ruolo sufficiente, quota — con lo stesso `tenant_id` preso dal token verificato.
   Un assistente non è una scorciatoia: non vede nulla che l'utente per cui agisce non veda dalla sua interfaccia.
6. **RF-6** — `storico_movimenti` restituisce l'autore di ogni movimento, perché senza il «chi» una differenza non
   si spiega; **non esiste e non esisterà** uno strumento che aggreghi o classifichi le persone per numero di
   movimenti, per velocità o per rettifiche fatte.
7. **RF-7** — Il contratto è pubblicato come descrittore leggibile da un programma dentro il servizio e la sua
   coerenza con le rotte esistenti è verificata da una prova automatica: uno strumento che dichiara un campo che
   il servizio non sa produrre fa fallire la suite.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni strumento risolve il `tenant_id` **solo** dal token verificato della
  chiamata delegata; un `tenant_id` che arrivasse fra i parametri dello strumento viene ignorato. Prova di
  isolamento fra due account su tutti e cinque gli strumenti: un assistente che agisce per l'account `A` non
  raggiunge nessun dato di `B`, nemmeno passando l'identificativo di un articolo di `B`.
- **RT-2 — Interfaccia di programmazione (§2).** Gli strumenti non duplicano la logica: si appoggiano agli stessi
  servizi applicativi che servono le rotte `GET /api/magazzino/v1/giacenze`, `/articoli`, `/movimenti` e
  `/sotto-scorta`. Gli errori restano in `application/problem+json` e vengono tradotti in un esito d'errore
  dello strumento con lo stesso codice; la definizione OpenAPI non cambia, il descrittore degli strumenti è un
  artefatto separato versionato nello stesso commit.
- **RT-3 — Persistenza (§8).** Nessuna tabella nuova e nessuna migrazione: la storia è di sola lettura sopra lo
  schema `app_magazzino` già esistente.
- **RT-4 — Modulo frontend (§3, §5).** Nessuna schermata nuova. Nella sezione impostazioni del modulo compare la
  sola voce di sola lettura che elenca gli strumenti dichiarati e la loro natura, con i token del sistema di
  design e funzionante in tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** I **nomi** degli strumenti e i nomi dei parametri sono identificatori tecnici e
  restano invariati; le **descrizioni** e i messaggi d'errore rivolti alla persona passano dallo spazio-nomi
  `magazzino` e sono presenti in `en, it, fr, es, de`. La storia non è conclusa se ne manca una.
- **RT-6 — Varchi e quota (§6, §7).** Nessuno strumento di lettura **consuma** quota: la metrica
  `articoli_gestiti` è a giacenza e conta gli articoli attivi, non le domande. Gli strumenti però **rispettano** la
  catena: `401` senza token valido, `403` ad app spenta o ruolo insufficiente, `402` con abbonamento `canceled`,
  e restituiscono l'esito `429` se la piattaforma lo impone alla chiamata delegata. Con abbonamento in `past_due`
  la lettura resta accessibile.
- **RT-7 — Esposizione conversazionale (§12).** È la storia che **istituisce** il contratto: cinque strumenti
  marcati `lettura`, tutti idempotenti, nessuno con effetti. Il contratto vive dentro il servizio dell'app; il
  server conversazionale è di piattaforma e **non è ancora implementato** (UC 0061-0063). Fino ad allora il
  contratto è verificato dalle prove ma non raggiungibile da un assistente reale, ed è dichiarato come tale.
- **RT-8 — Dati personali (§10).** Nessun campo nuovo e nessuna voce nuova nel manifesto. Due presidi vanno però
  scritti qui: `storico_movimenti` espone l'**identificativo** dell'autore e il suo nome visualizzato, che sono
  dati sull'attività di un lavoratore (§6 della descrizione, art. 4 della legge 300/1970 — Statuto dei
  lavoratori); nessuno strumento aggrega quel dato per persona. Le note a testo libero dei movimenti sono
  restituite solo da `storico_movimenti` e non compaiono negli elenchi, dove non servono a rispondere.
- **RT-9 — Registrazione eventi (§14).** Ogni invocazione registra `strumento invocato` con nome dello strumento,
  esito, numero di righe restituite, `tenant_id`, `app_id`, `user_id` e identificativo di correlazione — **senza**
  i parametri a testo libero, senza descrizioni di articoli e senza note.

## 4. Criteri di accettazione

**CA-1 — Domanda semplice, risposta minimizzata**
- **Dato** un account con l'articolo `RIC-014` presente in due depositi, 7 pezzi in «Magazzino» e 2 in «Furgone»
- **Quando** viene invocato `leggi_giacenza("RIC-014")` senza deposito
- **Allora** il risultato porta le due righe per deposito e il totale 9, e **non** contiene movimenti, costi né
  soglie di scorta

**CA-2 — Elenco lungo, paginato e dichiarato**
- **Dato** un account con 120 articoli attivi
- **Quando** viene invocato `elenca_articoli()` senza pagina
- **Allora** torna la prima pagina con la dimensione predefinita, il totale 120 e l'indicazione che esistono altre
  pagine; una dimensione richiesta oltre il massimo viene ridotta al massimo, non respinta in silenzio

**CA-3 — Isolamento fra account**
- **Dato** due account `A` e `B`, ciascuno con i propri articoli
- **Quando** un assistente che agisce per un utente di `A` invoca `trova_articolo` con il codice di un articolo di
  `B`, o passa l'identificativo dell'account `B` fra i parametri
- **Allora** riceve «nessun articolo trovato», il parametro estraneo è ignorato e nulla di `B` compare

**CA-4 — Abbonamento non attivo**
- **Dato** un account con abbonamento `canceled` · **Quando** viene invocato `elenca_sotto_scorta()`
- **Allora** l'esito è `402` con un messaggio che dice come riattivare, e nessun dato di magazzino viene restituito

**CA-5 — Nessuna classifica delle persone**
- **Dato** il contratto degli strumenti pubblicato
- **Quando** lo si interroga per cercare uno strumento che aggreghi i movimenti per autore
- **Allora** non ne esiste alcuno; `storico_movimenti` restituisce l'autore riga per riga e non offre alcun
  raggruppamento per persona, e una prova automatica fallisce se un raggruppamento del genere viene introdotto

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno backend e frontend; l'intera suite prima del commit);
- [ ] prove di **unità** sulla minimizzazione del risultato e sulla paginazione, e di **integrazione** su tutti e
      cinque gli strumenti, con database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** su ognuno dei cinque strumenti;
- [ ] **prova end-to-end**: *nessun impatto* — la storia non introduce superficie utente nuova oltre alla voce di
      sola lettura nelle impostazioni; il percorso `[J-MAGAZZINO]` è di proprietà delle storie `0036` e `0037`, e
      il registro [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) riceve lì le voci;
- [ ] **traduzioni** delle descrizioni e dei messaggi d'errore presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati**: nessuna voce nuova; verificato che l'esposizione dell'autore dei movimenti sia già
      coperta dalla voce `movimento.autore` introdotta con la storia `0013`;
- [ ] **registro delle decisioni** compilato, con la scelta della minimizzazione dei risultati e il divieto di
      aggregazione per persona;
- [ ] contratto degli **strumenti conversazionali** dichiarato e verificato dalla prova di coerenza;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata: il §7 della descrizione dell'applicazione resta la fonte del contratto.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| `0013` | La giacenza derivata e il registro devono esistere: sono ciò che gli strumenti leggono |
| `0014`, `0015` | Carico e scarico popolano il registro che `storico_movimenti` racconta |
| `0027` | L'avviso di sotto scorta definisce chi è «sotto soglia», che `elenca_sotto_scorta` restituisce |
| UC 0061-0063 (piattaforma, non implementati) | Il server conversazionale, l'autenticazione delegata e la mappatura operazioni → strumenti. Nel frattempo il contratto è dichiarato e provato dentro il servizio, ma nessun assistente reale lo raggiunge |

## 7. Fuori ambito

- **Gli strumenti di scrittura**: sono della storia `0035`, che porta la regola della bozza e della conferma umana.
- **Il server conversazionale, l'autenticazione delegata e il consenso**: sono di piattaforma (UC 0061-0062).
- **Il conteggio delle chiamate dell'assistente ai fini della quota**: è UC 0064, di piattaforma; qui si dichiara
  soltanto che gli strumenti rispettano l'esito `429` quando arriva.
- **Il valore gestionale delle giacenze**: non è esposto da nessuno strumento di lettura in questa storia, perché
  il numero ha bisogno dell'etichetta che spiega cosa non è (storia `0025`) e in una risposta parlata l'etichetta
  si perde. Rimando dichiarato.

## 8. Punti aperti

- **Come si dice «valore gestionale» a voce.** È il punto aperto 4 della descrizione dell'applicazione: finché non
  è deciso il nome nelle cinque lingue, nessuno strumento restituisce importi. Chiude lo sviluppatore con la
  revisione dei testi.
- **Quanta parte del risultato regga in una risposta parlata**: la dimensione predefinita delle pagine è fissata a
  intuito e andrà tarata quando il server conversazionale esisterà davvero. Nessuna fonte consultata la illumina.
