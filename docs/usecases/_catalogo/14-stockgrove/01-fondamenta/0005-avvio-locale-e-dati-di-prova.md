# 0005 — Avvio locale e dati di prova

**Applicazione**: 14 — StockGrove (`magazzino`) · **Epica**: 01 — Fondamenta
**Storia**: `0005` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0001`, `0002`, `0003`, `0004`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come sviluppatore che apre il repository per la prima volta
> voglio avviare StockGrove in locale con un comando e trovarci dentro un magazzino già popolato
> così da poter lavorare, dimostrare l'app e scrivere una prova end-to-end senza passare mezza giornata a inventare
> venti articoli e i loro movimenti.

**Contesto.** L'app si avvia, ma è vuota: un magazzino senza merce non si può né mostrare né provare, e ogni
persona che ci lavora finirebbe per crearsi i propri dati a mano, diversi da quelli di tutti gli altri. Questa
storia chiude l'epica delle fondamenta rendendo l'app **dimostrabile in un minuto**: la scoperta automatica la vede
con la sua porta e il suo schema, gli script comuni la avviano senza cablaggi, e un profilo di dati inventati la
riempie di un magazzino plausibile — con un articolo già sotto scorta, perché la schermata più importante dell'app
è l'elenco di ciò che sta finendo.

## 2. Requisiti funzionali

1. **RF-1** — `./dev.sh services` mostra `magazzino` con porta `8114` e schema `app_magazzino`, ricavati **solo**
   da `services/magazzino/src/main/resources/application.properties`; nessuno script di avvio è stato modificato a
   mano.
2. **RF-2** — `./app-start.sh` avvia il servizio e il modulo insieme al resto dello stack locale, `./app-stop.sh` li
   ferma, `dev migrate` applica le migrazioni dell'app e `dev service magazzino` avvia il solo servizio.
3. **RF-3** — Il proxy locale espone `/api/magazzino/v1/*` grazie al blocco delle rotte rigenerato fra i suoi
   marcatori, senza righe scritte a mano.
4. **RF-4** — Esiste un profilo di **dati di prova** che, caricato su un account di dimostrazione, crea: una
   ventina di articoli inventati ma plausibili (minuteria, ricambi, materiale di consumo), **due depositi** di cui
   uno è un furgone, i movimenti di carico e di scarico che portano alle giacenze, e almeno un articolo **sotto la
   propria soglia di scorta**.
5. **RF-5** — I dati di prova si caricano **solo** con il profilo di sviluppo e con quello di collaudo: in
   produzione il caricamento non esiste e un tentativo di eseguirlo fallisce in modo esplicito.
6. **RF-6** — I dati di prova sono **inventati**: nessuna ragione sociale reale, nessun indirizzo esistente,
   nessuna persona vera; gli eventuali recapiti usano domini di prova (`*.test`).

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** I dati di prova nascono dentro un account di dimostrazione identificato
  come tutti gli altri, con `tenant_id` esplicito: il caricamento non scavalca il filtro per account e non scrive
  righe senza account.
- **RT-2 — Interfaccia di programmazione (§2).** Il caricamento passa dalle stesse rotte pubbliche
  `/api/magazzino/v1/...` o dagli stessi servizi applicativi, **non** da istruzioni SQL scritte a parte: dati di
  prova costruiti aggirando la logica dell'app producono stati che l'app non potrebbe mai produrre da sola, e sono
  la causa più comune delle prove che passano su fatti impossibili.
- **RT-3 — Persistenza (§8).** Nessuna migrazione nuova: le tabelle sono quelle della storia `0002`. Le migrazioni
  restano applicate esplicitamente (`dev migrate`) e mai automaticamente in produzione.
- **RT-4 — Modulo frontend (§3, §5).** Nessuna schermata nuova. Il modulo va **abilitato nello stub locale** delle
  abilitazioni finché l'abilitazione reale non esiste, altrimenti l'app non compare nella barra laterale in
  sviluppo.
- **RT-5 — Cinque lingue (§4).** Le descrizioni degli articoli di prova sono dati, non interfaccia, e restano in
  italiano; nessuna stringa dell'interfaccia viene introdotta da questa storia.
- **RT-6 — Varchi e quota (§6, §7).** L'account di dimostrazione nasce con un'abilitazione attiva e un tetto della
  metrica `articoli_gestiti` sufficiente ai venti articoli del profilo, così che la dimostrazione non inciampi in un
  `429` che non c'entra nulla con ciò che si sta mostrando.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento dichiarato. I dati di prova saranno però la base
  su cui si dimostreranno gli strumenti delle storie `0034` e `0035`: vanno scelti in modo che le domande tipiche
  («quante ne ho in furgone?», «cosa devo ricomprare?») abbiano una risposta interessante.
- **RT-8 — Dati personali (§10).** **Nessun dato personale nuovo, e nessun dato reale**: i fornitori del profilo,
  quando arriveranno con la storia `0009`, saranno inventati. La regola vale anche per le prove automatiche: dati
  deterministici e inventati, mai dati veri.
- **RT-9 — Registrazione eventi (§14).** Il caricamento dei dati di prova registra un solo evento riassuntivo con
  `tenant_id`, `app_id`, `user_id` e identificativo di correlazione — quante righe per tabella — senza il contenuto
  delle righe.

## 4. Criteri di accettazione

**CA-1 — L'app si avvia senza cablaggi a mano**
- **Dato** il repository appena clonato
- **Quando** si eseguono `./dev.sh services` e `./app-start.sh`
- **Allora** `magazzino` compare con porta `8114` e schema `app_magazzino`, il servizio risponde su
  `/api/magazzino/v1/stato` attraverso il proxy locale, e nessuno script è stato modificato a mano

**CA-2 — Il profilo di dati di prova riempie l'app**
- **Dato** lo stack locale avviato e le migrazioni applicate
- **Quando** si carica il profilo di dati di prova
- **Allora** l'account di dimostrazione ha una ventina di articoli, due depositi di cui un furgone, giacenze
  coerenti con i movimenti caricati e **almeno un articolo sotto la propria soglia di scorta**

**CA-3 — Le giacenze sono il risultato dei movimenti, non un numero scritto**
- **Dato** i dati di prova caricati
- **Quando** si sommano i movimenti di un articolo e si confronta il risultato con la giacenza pubblicata
- **Allora** i due valori coincidono per ogni coppia articolo-deposito

**CA-4 — In produzione non si carica niente**
- **Dato** il profilo di produzione
- **Quando** si tenta di eseguire il caricamento dei dati di prova
- **Allora** l'operazione fallisce con un messaggio esplicito e **nessuna riga viene scritta**

**CA-5 — Nessun dato reale**
- **Dato** il profilo di dati di prova
- **Quando** se ne ispezionano descrizioni, nomi dei depositi ed eventuali recapiti
- **Allora** non compare alcuna impresa esistente, alcun indirizzo reale o alcun dominio di posta diverso da uno di
  prova

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno backend e smoke; l'intera suite prima del commit);
- [ ] prova di **integrazione** sul caricamento del profilo con database effimero e migrazioni vere, che verifica
      la coerenza fra movimenti e giacenze;
- [ ] prova di **isolamento fra account**: il caricamento scrive solo nell'account di dimostrazione e non tocca gli
      altri;
- [ ] **prova end-to-end**: *rimando* — i percorsi `[J-MAGAZZINO]` delle storie `0036` e `0037` si appoggeranno a
      questo profilo; il registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) riceve lì le voci;
- [ ] **traduzioni**: nessuna stringa nuova di interfaccia;
- [ ] **manifesto dei dati**: nessuna modifica, nessun dato personale e nessun dato reale;
- [ ] **registro delle decisioni** compilato, con la scelta di costruire i dati di prova passando dalla logica
      dell'app e non da istruzioni SQL a parte;
- [ ] contratto degli **strumenti conversazionali**: nessuna funzione esposta in questa storia;
- [ ] `./dev.sh services` mostra l'app e `./app-start.sh` la avvia **senza** passi manuali; il modulo è abilitato
      nello stub locale delle abilitazioni;
- [ ] `run-tests.sh` aggiornato se l'app introduce o cambia il comando di prova di un'area;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| `0001` | Porta, schema e proprietà da cui la scoperta automatica ricava tutto |
| `0002` | Senza la tabella `articolo` non c'è nulla da caricare |
| `0003` | Il modulo deve esistere ed essere abilitato nello stub locale perché la dimostrazione si veda |
| `0004` | L'account di dimostrazione ha bisogno di un'abilitazione attiva e di un tetto sufficiente |

## 7. Fuori ambito

- I **movimenti veri** e la giacenza derivata: il profilo li carica, ma il modello e le sue regole sono dell'epica
  03 (storia `0013` in avanti). Finché quella non esiste, il profilo si limita agli articoli e ai depositi.
- Le soglie di scorta come funzione: storia `0026`; qui il profilo prepara solo il dato perché la dimostrazione
  abbia senso.
- L'anagrafica dei fornitori nel profilo: storia `0009`.
- L'importazione da file dell'anagrafica del cliente: storia `0011`. Un profilo di dimostrazione non è uno
  strumento di migrazione.

## 8. Punti aperti

- **Ordine di realizzazione**: il profilo dà il meglio quando esistono i movimenti (epica 03) e le soglie
  (storia `0026`). La proposta è realizzarlo ora nella forma minima — articoli e depositi — e **accrescerlo** nelle
  storie `0013` e `0026`, invece di rimandarlo: un'app che non si può mostrare rallenta ogni storia successiva.
- **Quanti articoli**: venti è la taglia che rende la dimostrazione leggibile su uno schermo. Se servirà provare il
  comportamento dell'elenco con numeri grandi, quello è materiale per una prova di carico, non per il profilo di
  dimostrazione.
