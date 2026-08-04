# 0010 — Manifesto dei dati e diritti dell'interessato

**Applicazione**: 14 — StockGrove (`magazzino`) · **Epica**: 02 — Anagrafiche e catalogo prodotti
**Storia**: `0010` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0006`, `0009`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare dell'account, che è il titolare del trattamento dei dati che affida ad appgrove
> voglio poter dire con esattezza quali dati di persone l'applicazione tiene, esportarli e farli cancellare
> così da poter rispondere alla richiesta di un fornitore o di un collaboratore senza dover interrogare nessuno.

**Contesto.** L'app ora contiene due sole famiglie di dati di persone: l'**anagrafica dei fornitori** (storia
`0009`) e i **campi liberi** degli articoli (storia `0006`), a cui si aggiungeranno con l'epica 03 gli **autori dei
movimenti**. È il momento giusto per chiudere il contratto di conformità: farlo adesso, con tre tabelle, costa un
giorno; farlo a fine progetto significa rileggersi dodici tabelle e dimenticarne una — e «un campo non dichiarato è
un campo che l'esportazione dimentica e la cancellazione lascia indietro» (principi di piattaforma, §10).

Un punto di sostanza da mettere per iscritto qui, perché è la vera particolarità di questa applicazione: **il
registro dei movimenti dice chi ha fatto cosa**. Ogni movimento porta l'identificativo dell'utente che l'ha
registrato e il momento. Senza quel dato una differenza d'inventario non si spiega e una rettifica non ha un
responsabile; ma è anche un dato sull'**attività di un lavoratore**, e in Italia gli strumenti da cui può derivare
un controllo a distanza dell'attività ricadono nell'articolo 4 della legge 300/1970 (Statuto dei lavoratori), con
l'obbligo in ogni caso di informare adeguatamente la persona. Non è un dato di categoria particolare e l'app resta
uno strumento usato per rendere la prestazione, ma la conseguenza di prodotto è netta e vincola tutte le storie
successive: **il dato di autore serve alla tracciabilità della merce, e non si costruiscono classifiche di
produttività per persona** — niente indicatori del tipo «movimenti registrati per operatore» (descrizione
dell'applicazione, §2.3 punto 3 e §6).

## 2. Requisiti funzionali

1. **RF-1** — Esiste il manifesto `docs/compliance/manifests/magazzino.yaml`, completo **in italiano e inglese** su
   ogni testo, con una voce per ciascun campo che riguarda una persona: le sei del fornitore, i campi liberi
   (`articolo.descrizione`, `articolo.note`) e — dichiarati qui e popolati dall'epica 03 — gli autori
   (`movimento.created_by`, `inventario.created_by`, `riga_inventario.contato_da`, `scansione_in_coda.created_by`)
   e le note libere (`movimento.nota`, `riga_inventario.nota`).
2. **RF-2** — Esiste la classe `MagazzinoDataContract` che implementa `AppDataContract` con `appId()`,
   `exportData(scope)`, `purgeData(scope)` e `manifest()`.
3. **RF-3** — `exportData` copre **tutte** le tabelle elencate al §6 della descrizione, senza eccezioni:
   `fornitore`, `articolo`, `movimento`, `inventario`, `riga_inventario`, `scansione_in_coda`, `regola_scorta`,
   `proposta_riordino`. Le tabelle non ancora esistenti si aggiungono **nella storia che le crea**, e ogni storia
   dell'epica 03 e seguenti lo porta nella propria definizione di fatto.
4. **RF-4** — `purgeData` è una cancellazione **fisica**: sostituire una ragione sociale con un codice non è
   cancellare. La purga lascia una riga di prova nel registro delle purghe con che cosa è stato cancellato, quando
   e per quale richiesta.
5. **RF-5** — La richiesta che riguarda un **fornitore** si esegue interamente: i suoi dati anagrafici si
   cancellano e i movimenti restano **con il riferimento vuoto**, perché il fatto «sono entrati 12 pezzi il 4
   marzo» non è un dato sul fornitore ed è la merce dell'impresa, non la sua.
6. **RF-6** — La richiesta che riguarda un **lavoratore dell'account** (l'autore di un movimento) **non è risolta
   da questa applicazione**: il servizio la accetta, la segnala come richiesta che tocca dati necessari alla
   tracciabilità della merce e la instrada al presidio di piattaforma senza deciderla da solo. La motivazione è
   scritta nel manifesto e il punto resta aperto.
7. **RF-7** — Esportazione e cancellazione restano accessibili **anche quando l'app è disabilitata o
   l'abbonamento è scaduto**: i diritti dell'interessato non passano dalla catena commerciale.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** `exportData` e `purgeData` operano **sempre** entro l'ambito richiesto e
  filtrano per `tenant_id`; un ambito che non porti un `tenant_id` valido è respinto, non allargato. Prova di
  isolamento: l'esportazione di un account non contiene mai una riga dell'altro.
- **RT-2 — Interfaccia di programmazione (§2).** Il contratto non espone rotte proprie dell'app: è invocato dal
  meccanismo comune dei diritti dell'interessato della piattaforma. Gli errori, se emergono, escono in
  `application/problem+json`.
- **RT-3 — Persistenza (§8).** Nessuna tabella nuova. La cancellazione fisica agisce con `DELETE` reale, non con
  `deleted_at`: la cancellazione logica della piattaforma non soddisfa una richiesta di cancellazione. Il caso del
  `movimento` è particolare e va detto: la riga **non si cancella** (cancellarla cambierebbe il passato e la
  giacenza), si azzerano i campi che riguardano la persona.
- **RT-4 — Modulo frontend (§3, §5).** Nessuna schermata nuova nel modulo `magazzino`: i diritti dell'interessato
  si esercitano dalle schermate comuni del backoffice. La storia si ferma al servizio e al manifesto.
- **RT-5 — Cinque lingue (§4).** Non si applica all'interfaccia (nessuna schermata nuova). Attenzione a non
  confondere i due elenchi: il **manifesto** vuole **due** lingue, italiano e inglese, ed è un obbligo verificato
  dal controllo di parità delle lingue nell'area `compliance` di `./run-tests.sh`.
- **RT-6 — Varchi e quota (§6, §7).** L'esercizio dei diritti **non consuma quota** e non passa dal varco
  dell'abbonamento: con `canceled` risponde comunque, per esplicita regola di piattaforma (principi, §13).
- **RT-7 — Esposizione conversazionale (§12).** **Nessuno strumento**, e la scelta è deliberata: esportare o
  cancellare dati personali su richiesta di un assistente sarebbe un effetto irreversibile su dati di terzi, e non
  è un'azione che si comanda da una chat. Va scritto nel contratto degli strumenti come esclusione esplicita, non
  omesso.
- **RT-8 — Dati personali (§10).** È la storia che **chiude** il presidio: manifesto in italiano e inglese, campi
  annotati `@PersonalData` (un campo annotato e non dichiarato fa fallire la compilazione), tabelle presenti in
  esportazione e cancellazione. Nessuna categoria particolare dell'articolo 9 è coinvolta, e il motivo è
  dichiarato: la merce di una farmacia o di un sanitario **non è un dato sulla salute** finché non è legata a una
  persona identificata, e StockGrove non ha il concetto di paziente né di destinatario della merce (descrizione,
  §6).
- **RT-9 — Registrazione eventi (§14).** Gli eventi `esportazione eseguita`, `purga eseguita` e
  `richiesta instradata al presidio di piattaforma` sono registrati con `tenant_id`, `app_id`, `user_id`,
  identificativo di correlazione e identificativo della richiesta, **senza** i dati esportati o cancellati.

## 4. Criteri di accettazione

**CA-1 — Esportazione completa**
- **Dato** un account con fornitori, articoli e — quando l'epica 03 esisterà — movimenti
- **Quando** si esegue `exportData` per quell'account
- **Allora** il risultato contiene una sezione per ciascuna tabella dichiarata nel manifesto, e un controllo
  automatico verifica che **ogni** campo annotato `@PersonalData` compaia nell'esportazione

**CA-2 — Cancellazione dei dati di un fornitore**
- **Dato** un fornitore con ragione sociale, persona di riferimento, posta elettronica e telefono, collegato a
  quattro movimenti di carico
- **Quando** si esegue `purgeData` per quel fornitore
- **Allora** la riga del fornitore è cancellata fisicamente, i quattro movimenti **esistono ancora** con quantità e
  date invariate e il riferimento al fornitore vuoto, e la giacenza non cambia di un pezzo

**CA-3 — Richiesta che riguarda l'autore dei movimenti**
- **Dato** una richiesta di cancellazione riferita a un collaboratore dell'account che ha registrato 300 movimenti
- **Quando** la si sottopone al contratto
- **Allora** la richiesta è **accettata e instradata**, non eseguita in silenzio né rifiutata in silenzio: la
  risposta dichiara che i dati toccano la tracciabilità della merce e rimanda al presidio di piattaforma, e
  l'evento è registrato

**CA-4 — Diritti accessibili con abbonamento scaduto**
- **Dato** un account con abbonamento in stato `canceled`
- **Quando** chiede l'esportazione dei propri dati
- **Allora** l'esportazione viene prodotta regolarmente, mentre le rotte applicative dell'app continuano a
  rispondere `402`

**CA-5 — Manifesto incompleto = compilazione rossa**
- **Dato** un campo annotato `@PersonalData` e non dichiarato nel manifesto
- **Quando** si compila il servizio
- **Allora** la compilazione fallisce con un messaggio che nomina il campo mancante

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno backend e compliance; l'intera suite prima del commit);
- [ ] prove di **unità** sulla selezione delle righe per ambito e prove di **integrazione** su esportazione e
      purga, con database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account**: l'esportazione di un account non contiene mai righe dell'altro;
- [ ] **prova end-to-end**: *nessun impatto* — la storia non introduce superficie utente nuova nel modulo
      `magazzino`; l'esercizio dei diritti è coperto dai percorsi comuni della piattaforma e il registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) non cambia per questa storia;
- [ ] **traduzioni**: non applicabile all'interfaccia; il **manifesto** è completo in italiano e inglese e il
      controllo di parità delle lingue è verde;
- [ ] **manifesto dei dati** completo, campi annotati, tutte le tabelle presenti in esportazione e cancellazione,
      con l'impegno esplicito che ogni storia successiva aggiunga le proprie;
- [ ] **registro delle decisioni** compilato, con la distinzione fra richiesta sul fornitore e richiesta sul
      lavoratore e il motivo per cui la seconda non si chiude qui;
- [ ] contratto degli **strumenti conversazionali**: esclusione esplicita di esportazione e cancellazione;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| `0006` | I campi liberi dell'articolo sono la prima voce di manifesto |
| `0009` | L'anagrafica dei fornitori è il grosso dei dati personali dell'app |
| epica 03 e seguenti | Le tabelle dei movimenti, degli inventari e delle scansioni si aggiungono a esportazione e cancellazione **nella storia che le crea**: questa storia stabilisce l'obbligo, non lo esegue in anticipo |

## 7. Fuori ambito

- **La decisione su cosa prevalga fra il diritto del lavoratore e la tracciabilità del registro**: non spetta a
  questa applicazione (punto aperto §11 punto 5 della descrizione).
- **L'informativa al lavoratore** prevista dall'articolo 4 dello Statuto: è un adempimento del cliente-titolare,
  non una funzione del programma. L'app deve però rendere possibile dirlo, ed è il motivo per cui il trattamento è
  scritto nel manifesto.
- **Il rilevamento automatico di dati sensibili nei campi liberi**: non si fa; l'interfaccia avvisa e il presidio,
  se servirà, è trasversale (descrizione, §6).
- **La valutazione d'impatto sulla protezione dei dati**: è di piattaforma, e questa storia le fornisce il
  materiale.

## 8. Punti aperti

- **Cancellazione dei dati dell'autore dei movimenti**: è il punto aperto §11 punto 5 della descrizione, e resta
  aperto anche a storia conclusa. Chiude lo sviluppatore con revisione legale.
- **Durate di conservazione**: quelle proposte al §6 della descrizione derivano per analogia dal termine di
  prescrizione ordinaria e dalla conservazione dei documenti contabili; **non sono un dato rilevato**. Chiude lo
  sviluppatore con revisione legale.
- **Regole non italiane** sul controllo dell'attività dei lavoratori: non verificate per nessun altro paese
  europeo, mentre il prodotto nasce in cinque lingue (descrizione, §11 punto 7). Chiude lo sviluppatore prima di
  vendere fuori dall'Italia.