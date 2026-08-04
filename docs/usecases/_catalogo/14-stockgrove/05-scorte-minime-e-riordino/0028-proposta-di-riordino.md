# 0028 — Proposta di riordino

**Applicazione**: 14 — StockGrove (`magazzino`) · **Epica**: 05 — Scorte minime e riordino
**Storia**: `0028` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0026`, `0027`, `0009`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che il venerdì sera deve decidere cosa ricomprare
> voglio che l'applicazione mi prepari la lista della spesa già divisa per fornitore, con le quantità proposte, e
> che io possa correggerla e portarmela via
> così da chiudere in dieci minuti un lavoro che oggi non fa nessuno perché richiede di sedersi con l'elenco in
> mano.

**Contesto.** La storia `0027` sa dire che cosa sta finendo. Le fonti sul segmento sono esplicite sul fatto che
questo non basta: un elenco di articoli sotto scorta che non produce almeno una lista della spesa viene percepito
come **lavoro in più**, non in meno (descrizione dell'applicazione, §2.5; guida agli avvisi di scorta bassa, §2.6
fonte 9). Questa storia è la risposta a quel rilievo ed è il punto in cui StockGrove smette di raccontare il
passato e comincia a proporre qualcosa.

Il confine è netto e va tenuto: **la proposta di riordino non diventa un ordine**. StockGrove non manda niente a
nessuno fuori dall'azienda (§1 della descrizione) — nessun messaggio al fornitore, nessuna trasmissione, nessun
fornitore esterno che tratti dati per nostro conto. Il documento si esporta e lo consegna una persona. L'ordine
vero, la conferma e il ciclo degli acquisti sono di ProcureGrove (48): quando esisterà, l'ordine ricevuto tornerà
indietro come **carico** attraverso il meccanismo a eventi della storia `0019`, e questa proposta ne sarà
l'ingresso naturale.

## 2. Requisiti funzionali

1. **RF-1** — Si genera una proposta di riordino a partire dalle coppie articolo e deposito sotto scorta
   (`0027`), con ambito scegliibile: tutto l'account, un deposito, una categoria o una selezione di righe.
2. **RF-2** — La quantità suggerita di ogni riga porta la giacenza **fino alla quantità di riordino** della
   regola (`0026`): suggerimento = `quantita_riordino − giacenza_corrente`, arrotondato per eccesso all'unità di
   misura dell'articolo. Se la regola non ha quantità di riordino, il suggerimento usa la scorta minima e la riga
   è marcata come «stima da rivedere».
3. **RF-3** — Le righe sono **raggruppate per fornitore preferito**; le righe senza fornitore finiscono in un
   gruppo «da assegnare» che resta ben visibile invece di sparire.
4. **RF-4** — Ogni riga è modificabile prima di chiudere la proposta: si cambia la quantità, si cambia il
   fornitore, si toglie la riga, si aggiunge a mano un articolo che non era sotto scorta. Le modifiche sono
   registrate come tali, così che si veda cosa era proposto e cosa ha deciso la persona.
5. **RF-5** — La proposta ha tre stati: `aperta` (si modifica), `esportata` (è stata scaricata; resta consultabile
   e non si modifica più) e `archiviata` (chiusa, per storico). Il passaggio è a senso unico e ogni transizione
   porta chi l'ha fatta e quando.
6. **RF-6** — La proposta si **esporta in un file** per gruppo di fornitore o per intero, in un formato leggibile
   sia da una persona sia da un foglio di calcolo, con articolo, codice interno, codice GTIN se presente,
   quantità, unità di misura, deposito di destinazione e recapiti del fornitore.
7. **RF-7** — Una proposta `aperta` per lo stesso ambito è unica: rigenerarla aggiorna quella esistente segnalando
   le righe nuove e quelle rientrate sopra soglia, invece di creare un secondo elenco che compete con il primo.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura e scrittura di `proposta_riordino` e `riga_proposta`
  filtra per `tenant_id` preso dal token verificato; l'esportazione produce solo righe dell'account che la chiede;
  un `tenant_id` che arrivasse dal corpo o dai parametri viene ignorato. Prova di isolamento fra due account sulla
  risorsa e sul file esportato.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `GET|POST /api/magazzino/v1/proposte-riordino`,
  `GET|PATCH /api/magazzino/v1/proposte-riordino/{id}`,
  `PATCH|DELETE /api/magazzino/v1/proposte-riordino/{id}/righe/{rigaId}`,
  `POST /api/magazzino/v1/proposte-riordino/{id}/esporta` e `POST .../archivia`; oggetti di trasferimento al
  bordo; validazione dichiarativa; errori in `application/problem+json`; paginazione a pagina e dimensione con
  totale; definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V18__proposta_riordino.sql` sullo schema `app_magazzino`: tabelle
  `proposta_riordino` (con `tenant_id`, chiave primaria UUID versione 7, ambito, `stato`, momento di generazione,
  momento di esportazione, colonne di controllo, `deleted_at`) e `riga_proposta` (articolo, deposito, fornitore
  facoltativo, `quantita_suggerita`, `quantita_scelta`, `modificata_a_mano`, `aggiunta_a_mano`, nota). Vincolo che
  impedisce più di una proposta `aperta` per lo stesso ambito fra le righe non cancellate. Nessuna chiave esterna
  verso altri schemi.
- **RT-4 — Modulo frontend (§3, §5).** La proposta vive nella sezione `riordino` del modulo `magazzino`, accanto
  all'elenco delle coppie sotto scorta: si genera da lì con un pulsante, si modifica riga per riga, si esporta.
  Dati letti con il client generato; solo token del sistema di design; funziona in tema chiaro e scuro. **Nessun
  prezzo di vendita compare in questa schermata** (§10 della descrizione, regola 1 del confine): la proposta parla
  di quantità, non di denaro incassato.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe visibili passano dallo spazio-nomi `magazzino` e sono presenti
  in `en, it, fr, es, de`, comprese le intestazioni del file esportato e l'etichetta del gruppo «da assegnare».
- **RT-6 — Varchi e quota (§6, §7).** Nessun consumo di quota: la metrica `articoli_gestiti` (natura `stock`)
  conta gli articoli attivi e la tocca solo la loro creazione. Generare, modificare, esportare o archiviare una
  proposta **non viene mai respinto con `429`**, nemmeno a tetto raggiunto. Restano i varchi precedenti: `402` con
  abbonamento non attivo, `403` per ruolo insufficiente — la generazione e l'esportazione sono riservate ai ruoli
  `owner` e `admin`, perché decidere cosa comprare non è un compito di chi movimenta la merce.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento dichiarato qui. La lettura
  (`elenca_sotto_scorta`) è della storia `0034`; la generazione di una proposta **non** è fra gli strumenti
  previsti dalla descrizione (§7) e non va aggiunta di iniziativa. Resta valida la regola generale: nessuno
  strumento di questa applicazione ha effetti verso l'esterno, perché l'applicazione non manda niente a nessuno.
  Il contratto vive dentro il servizio; il server conversazionale è di piattaforma e non ancora implementato
  (UC 0061-0063).
- **RT-8 — Dati personali (§10).** Nessun campo personale nuovo, ma un uso nuovo di dati esistenti da dichiarare:
  il file esportato **contiene i recapiti del fornitore** (ragione sociale, persona di riferimento, posta
  elettronica, telefono), già dichiarati nel manifesto `docs/compliance/manifests/magazzino.yaml` dalla storia
  `0010`. Le tabelle `proposta_riordino` e `riga_proposta` vanno aggiunte a `exportData` e `purgeData` del
  contratto `MagazzinoDataContract` per il riferimento al fornitore. **Nessun fornitore esterno nuovo che tratti
  dati per nostro conto**: il file lo scarica l'utente e lo consegna lui.
- **RT-9 — Registrazione eventi (§14).** Gli eventi `proposta di riordino generata` (con il numero di righe),
  `riga modificata a mano`, `proposta esportata` e `proposta archiviata` sono registrati con `tenant_id`,
  `app_id`, `user_id` e identificativo di correlazione, **senza** ragioni sociali dei fornitori e senza
  descrizioni di articoli.

## 4. Criteri di accettazione

**CA-1 — Generazione con quantità suggerite e raggruppamento**
- **Dato** tre articoli sotto scorta nel deposito `MAG`: due con fornitore preferito `F1` e quantità di riordino
  20 su giacenze 4 e 6, uno senza fornitore
- **Quando** si genera la proposta di riordino per quel deposito
- **Allora** la proposta è `aperta`, il gruppo `F1` contiene due righe con quantità suggerite 16 e 14, e la terza
  riga compare nel gruppo «da assegnare»

**CA-2 — Modifica di una riga e tracciamento**
- **Dato** una proposta `aperta` con una riga di quantità suggerita 16
- **Quando** l'utente porta la quantità a 24 e assegna un fornitore alla riga «da assegnare»
- **Allora** la riga conserva sia la quantità suggerita 16 sia la quantità scelta 24, risulta `modificata_a_mano`,
  e la riga prima orfana passa nel gruppo del fornitore assegnato

**CA-3 — Esportazione e blocco delle modifiche**
- **Dato** una proposta `aperta` con due gruppi di fornitore
- **Quando** l'utente la esporta
- **Allora** ottiene un file con articolo, codice, quantità, unità di misura, deposito e recapiti del fornitore;
  la proposta passa in `esportata`, resta consultabile e ogni tentativo di modificarne una riga riceve `409` con
  la spiegazione che una proposta esportata non si modifica

**CA-4 — Rigenerazione invece di duplicazione**
- **Dato** una proposta `aperta` per il deposito `MAG` e, nel frattempo, un articolo rientrato sopra soglia e uno
  nuovo sceso sotto
- **Quando** si rigenera la proposta per lo stesso ambito
- **Allora** non nasce una seconda proposta: quella esistente viene aggiornata, la riga rientrata è segnalata come
  non più necessaria, quella nuova è aggiunta e segnalata come novità

**CA-5 — Nessun effetto verso l'esterno**
- **Dato** una proposta esportata con i recapiti di posta elettronica dei fornitori
- **Quando** si cercano, nell'applicazione e nelle sue interfacce, azioni che trasmettano la proposta al fornitore
- **Allora** non ne esiste nessuna: il solo esito possibile è un file scaricato dall'utente, e nessuna chiamata
  esce verso servizi di terzi

**CA-6 — Isolamento fra account**
- **Dato** due account `A` e `B`, ciascuno con le proprie proposte
- **Quando** un utente di `A` chiede l'elenco delle proposte, ne apre una per identificativo o ne esporta il file
- **Allora** vede ed esporta solo le proprie, anche forzando l'identificativo dell'account `B` nella richiesta

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno backend, frontend, compliance e la suite end-to-end di
      piattaforma; l'intera suite prima del commit);
- [ ] prove di **unità** sul calcolo della quantità suggerita e sul raggruppamento per fornitore, e di
      **integrazione** sulle risorse `proposte-riordino` e sull'esportazione, con database effimero e migrazioni
      vere;
- [ ] prova di **isolamento fra account** su `proposta_riordino`, `riga_proposta` e sul file esportato;
- [ ] **prova end-to-end**: *coprire ora* — il percorso `[J-MAGAZZINO]` è esteso con la catena completa soglia →
      scarico → avviso → proposta → esportazione, e il registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) riceve la voce corrispondente per
      le storie `0026`, `0027` e `0028`, con questa storia come proprietaria;
- [ ] **traduzioni** presenti in tutte e cinque le lingue (`en, it, fr, es, de`), comprese le intestazioni del file
      esportato;
- [ ] **manifesto dei dati** verificato: nessun campo nuovo, ma il file esportato contiene recapiti di fornitori e
      le due tabelle nuove sono presenti in esportazione e cancellazione;
- [ ] **registro delle decisioni** `changes/NNNN-*/decisions.json` compilato, con la scelta di fermarsi alla
      proposta e di non trasmettere nulla all'esterno;
- [ ] contratto degli **strumenti conversazionali**: nessuna funzione esposta in questa storia, e il motivo scritto;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| `0026` — soglie di scorta | la quantità suggerita nasce dalla quantità di riordino della regola |
| `0027` — avviso di sotto scorta | la proposta si genera dall'elenco delle coppie sotto scorta |
| `0009` — anagrafica dei fornitori | il raggruppamento e i recapiti nel file esportato vengono da lì |
| `0019` — movimenti dagli eventi delle altre app | è la via per cui, quando ProcureGrove (48) esisterà, l'ordine ricevuto tornerà indietro come carico |
| ProcureGrove (48), non implementata | l'ordine vero, la conferma e il ciclo degli acquisti stanno lì; nel frattempo la catena si chiude con un file consegnato a mano |

## 7. Fuori ambito

- **Mandare l'ordine al fornitore**, in qualunque forma: non è un rimando, è una scelta di perimetro (§1 e §10
  della descrizione). Appartiene a ProcureGrove (48).
- **Il ciclo passivo**: conferma d'ordine, consegna parziale, controllo della fattura del fornitore.
- **Il prezzo d'acquisto e la valorizzazione della proposta**: la proposta parla di quantità. Il costo medio
  ponderato mobile è della storia `0025` e resta un valore gestionale, mai una valutazione fiscale delle
  rimanenze.
- **Il calcolo della quantità da comprare in base al consumo e al tempo di consegna**: la copertura in giorni è
  della storia `0029`; qui il suggerimento è la semplice differenza rispetto alla quantità di riordino, ed è
  dichiarato come tale.

## 8. Punti aperti

- **Arrotondamento al lotto d'acquisto del fornitore** (si compra a confezioni da 12, non a pezzi): è un dato che
  oggi il modello di dominio non ha. Aggiungerlo significa aggiungere un campo alla regola di scorta; la proposta
  è di **non** farlo ora e di riparlarne quando ProcureGrove (48) definirà la propria anagrafica di acquisto.
- **Formato del file esportato** — un formato tabellare per foglio di calcolo, un documento stampabile, o
  entrambi: dipende da cosa fa il cliente con il file. Non l'ho trovato illuminato da nessuna fonte (§2.7) e va
  deciso con lo sviluppatore.
- **Chi può esportare** — la proposta limita generazione ed esportazione a `owner` e `admin`; se in un cliente è
  il magazziniere a preparare la lista, il vincolo è sbagliato. Da confermare.
