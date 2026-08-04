# 0032 — Contratto degli strumenti di lettura

**Applicazione**: 32 — TokenGrove (`spesa_modelli`) · **Epica**: 07 — Esposizione conversazionale e prove end-to-end
**Storia**: `0032` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0011`, `0012`, `0021`, `0024`, `0026`, `0028`, `0029`
**Ultimo aggiornamento**: 2026-08-04

## 1. Narrazione

> Come titolare che non aprirà mai un pannello di osservabilità
> voglio chiedere a parole «quanto ho speso questa settimana per il cliente Rossi?» e «perché martedì è costato il
> triplo?»
> così da usare davvero i numeri della mia azienda, invece di dover sapere in anticipo quale filtro impostare in
> quale schermata.

**Contesto.** Le domande di questo dominio **sono domande**, non cruscotti: sono le tre che l'indagine di settore
trova senza risposta — chi possiede il conto, perché è salito, se sta rendendo (§2.6, fonte 12 del documento
capofila) — ed è la ragione per cui il livello conversazionale rende questa app più utile delle concorrenti (§7).
Questa storia scrive il **contratto** degli strumenti di sola lettura: nome stabile, descrizione in lingua naturale,
schema dei parametri, schema del risultato, marcatura *lettura*, idempotenza.

**Cosa questa storia non costruisce.** Il server conversazionale è di **piattaforma** e nel repository **non esiste
ancora**: è l'epica `12-ready-for-ai-mcp` (UC 0061-0066), scritta e non implementata. Qui si dichiara il contratto e
lo si tiene **dentro il servizio dell'app**, versionato con essa, verificabile con prove proprie; l'aggancio al
server avviene quando il server ci sarà, senza riscrivere niente.

## 2. Requisiti funzionali

1. **RF-1** — Sono dichiarati **sei** strumenti di sola lettura, con la firma già fissata al §7 del documento
   capofila:
   - `leggi_spesa(periodo, raggruppamento?, filtro?)` → tavola di importi **con la propria copertura di
     attribuzione**;
   - `elenca_maggiori_consumatori(periodo, dimensione, quanti?)` → elenco ordinato per importo;
   - `confronta_costo_modelli(periodo, modelli[], per_unita?)` → tavola comparativa ai prezzi datati;
   - `stato_budget(budget?)` → semaforo, consumato, previsione di fine periodo;
   - `spiega_impennata(periodo)` → scomposizione del salto per modello, etichetta e ora;
   - `stato_fonti()` → elenco delle fonti con ritardo osservato e scarto di riconciliazione.
2. **RF-2** — Ogni risultato porta con sé il proprio **grado di fiducia**, sempre, senza doverlo chiedere:
   copertura di attribuzione, istante dell'ultimo dato disponibile, età del catalogo prezzi usato. Una risposta
   conversazionale che dà un numero senza il suo contesto è peggiore di una schermata, perché nessuno vede gli
   indicatori accanto.
3. **RF-3** — Gli strumenti leggono **dalla stessa sorgente** delle schermate (la sintesi della storia `0028` e i
   costi congelati): chat e cruscotto non possono dare due numeri diversi per la stessa domanda.
4. **RF-4** — I periodi si esprimono in modo tollerante («questa settimana», «marzo», «ultimi 30 giorni») e il
   risultato **restituisce sempre il periodo effettivamente usato**, con inizio, fine e fuso orario dell'account:
   l'interpretazione non resta implicita.
5. **RF-5** — Ogni strumento è **idempotente e senza effetti**: chiamarlo dieci volte non cambia nulla, non consuma
   la metrica di quota e non lascia dati nuovi.
6. **RF-6** — Il contratto dichiara i **limiti** di ogni strumento: numero massimo di righe restituite, periodo
   massimo interrogabile in una volta, e cosa succede oltre lo storico coperto dal piano. Un troncamento è sempre
   dichiarato nel risultato, mai silenzioso.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni strumento riceve il `tenant_id` dal gettone verificato passato dal
  livello conversazionale; un `tenant_id` che arrivasse fra i parametri dello strumento viene **ignorato**. È il
  punto più delicato dell'esposizione conversazionale: i parametri di uno strumento sono testo prodotto da un
  modello, e nessun parametro può decidere di quale account si stia parlando.
- **RT-2 — Contratto dentro il servizio (§12).** Le definizioni vivono in `services/spesa_modelli`, versionate con
  l'app: nome stabile, descrizione in lingua naturale, schema dei parametri e schema del risultato. Marcatura
  **lettura** e idempotenza dichiarate. Nessuna parte del server conversazionale è costruita qui.
- **RT-3 — Interfaccia di programmazione (§2).** Gli strumenti si appoggiano alle rotte già esistenti
  (`/api/spesa_modelli/v1/panoramica`, `/spesa`, `/confronto-modelli`, `/budget`, `/impennate`, `/fonti`): **nessuna
  logica di dominio nuova**, nessun percorso di lettura parallelo. Se uno strumento avesse bisogno di un dato che
  nessuna rotta espone, il difetto è della rotta.
- **RT-4 — Varchi, ruoli e quota (§6, §7).** Le chiamate dell'assistente attraversano la stessa catena di varchi
  delle chiamate umane: abbonamento (`402`), ruolo (`403`), limite di frequenza. La lettura **non consuma** la
  metrica `misure_registrate`. Un utente `member` che chiede in chat un dato riservato a `owner` riceve un rifiuto
  spiegato, non un dato.
- **RT-5 — Cinque lingue (§4).** Le **descrizioni** degli strumenti e i messaggi di rifiuto sono presenti in
  `en, it, fr, es, de`. I nomi degli strumenti restano identificatori tecnici stabili e non si traducono.
- **RT-6 — Dati personali (§10).** `leggi_spesa` e `elenca_maggiori_consumatori` possono restituire valori di
  etichetta riferibili a persone (cliente finale, utente finale): il contratto lo dichiara, il rispetto dei ruoli
  vale come nelle schermate, e **nessun valore di etichetta finisce nei registri applicativi**. Nessun campo nuovo
  nel manifesto dei dati.
- **RT-7 — Registrazione eventi (§14).** Ogni chiamata a uno strumento è registrata con `tenant_id`, `app_id`,
  `user_id`, nome dello strumento, periodo interpretato ed esito, con identificativo di correlazione — **senza**
  importi, valori di etichetta né il testo della domanda.

## 4. Criteri di accettazione

**CA-1 — La domanda in italiano trova il numero giusto**
- **Dato** un account con spesa attribuita a due clienti finali nella settimana corrente
- **Quando** si invoca `leggi_spesa` con periodo «questa settimana» e raggruppamento «cliente finale»
- **Allora** la tavola contiene i due valori con i rispettivi importi, la copertura di attribuzione, e il periodo
  effettivamente usato con inizio, fine e fuso orario

**CA-2 — Chat e schermata dicono la stessa cosa**
- **Dato** lo stesso periodo e lo stesso filtro
- **Quando** si confronta il risultato di `leggi_spesa` con la panoramica
- **Allora** i totali coincidono alla cifra

**CA-3 — Il `tenant_id` nei parametri non conta**
- **Dato** un gettone dell'account `A`
- **Quando** uno strumento viene invocato con un parametro che dichiara l'account `B`
- **Allora** la risposta riguarda `A`, il parametro è ignorato e l'evento resta a registro

**CA-4 — Troncamento dichiarato**
- **Dato** una richiesta che supera il numero massimo di righe o il periodo massimo
- **Quando** lo strumento risponde
- **Allora** il risultato dichiara di essere troncato, dice quanto manca e come restringere la domanda

**CA-5 — Nessun effetto**
- **Dato** dieci invocazioni consecutive di tutti e sei gli strumenti
- **Quando** si rileggono quota, numero di righe e registri di dominio
- **Allora** nulla è cambiato

**CA-6 — Ruolo insufficiente**
- **Dato** un utente `member`
- **Quando** chiede in chat un dato riservato a `owner` e `admin`
- **Allora** riceve un rifiuto spiegato nella propria lingua, e nessun dato parziale

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (backend; l'intera suite prima del commit);
- [ ] prove di **unità** sull'interpretazione dei periodi e sui limiti dichiarati, e di **integrazione** su ciascuno
      dei sei strumenti contro le rotte reali;
- [ ] prova di **isolamento fra account** su ogni strumento, compreso il tentativo di forzare l'account dai
      parametri;
- [ ] prova che i risultati degli strumenti **coincidono** con quelli delle schermate corrispondenti;
- [ ] **prova end-to-end**: **si rimanda** alla storia `0034`, che è la proprietaria del percorso
      `[J-SPESA-MODELLI]` e vi include il passo conversazionale;
- [ ] **traduzioni** delle descrizioni e dei rifiuti presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati**: nessuna modifica; è però verificato che nessun registro contenga valori di etichetta;
- [ ] **registro delle decisioni** compilato, in particolare sul rifiuto del `tenant_id` dai parametri e sulla
      regola «stessa sorgente per chat e schermata»;
- [ ] contratto degli **strumenti conversazionali** versionato dentro il servizio dell'app;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storie `0011`, `0012` | `stato_fonti` restituisce ritardo osservato e scarto di riconciliazione |
| Storie `0021`, `0024` | Copertura di attribuzione e previsione fanno parte dei risultati |
| Storia `0026` | `spiega_impennata` usa la scomposizione già costruita per la rilevazione delle impennate |
| Storie `0028`, `0029` | Sono le sorgenti di `leggi_spesa`, `elenca_maggiori_consumatori` e `confronta_costo_modelli` |
| UC 0061-0063 (piattaforma) | Il server conversazionale non esiste ancora: nel frattempo il contratto si verifica con prove proprie, senza server |

## 7. Fuori ambito

- **la costruzione del server conversazionale**, l'autenticazione delegata e il consenso: sono di piattaforma
  (UC 0061-0064);
- **gli strumenti di scrittura**: sono la storia `0033`, che porta la regola della bozza e della conferma umana;
- la lettura in chat del **registro dei rapporti programmati** e del **registro delle esportazioni**: le rotte
  esistono (storie `0030`, `0031`) ma non sono esposte come strumento in questa prima versione del contratto. Se
  servirà, si aggiunge uno strumento di lettura estendendo **questo** contratto, che ne resta il proprietario;
- la generazione di testo esplicativo («la spesa è salita perché…»): gli strumenti restituiscono **dati con il loro
  contesto**; il racconto lo fa il livello conversazionale, non l'app. Mettere una spiegazione scritta dentro il
  risultato significherebbe scrivere due volte la stessa cosa e farla invecchiare male.

## 8. Punti aperti

- **Quanto storico può leggere l'assistente rispetto a quello che il piano concede.** Le due cose devono
  coincidere, ma vale la pena verificare che un rifiuto per storico non coperto arrivi come spiegazione utile e non
  come errore tecnico. Proposta: stessa regola della panoramica, messaggio dedicato. La conferma lo sviluppatore.
- **Se registrare il testo della domanda dell'utente.** Sarebbe utilissimo per capire come le persone interrogano
  l'app, ma è un contenuto libero che può contenere qualunque cosa — ed è esattamente ciò che questa app ha scelto
  di **non** conservare (§6 del documento capofila). Proposta: **no**. La conferma lo sviluppatore, ed è una
  decisione che riguarda tutta la piattaforma, non solo questa app.
