# 0030 — Ricerca e inserimento nella risposta

**Applicazione**: 12 — DeskGrove Support (`helpdesk`) · **Epica**: 06 — Base di conoscenza e portale del richiedente
**Storia**: `0030` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0007`, `0022`, `0029`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come operatore che sta rispondendo a una richiesta e sa che la risposta è già scritta da qualche parte
> voglio trovare l'articolo giusto senza uscire dalla casella di risposta e infilarlo nel messaggio
> così da chiudere la richiesta in un minuto invece che in cinque, e da mandare sempre la stessa spiegazione corretta.

**Contesto.** È la funzione che fa risparmiare più tempo di tutta l'applicazione, e il motivo è banale: la base di
conoscenza della storia `0029` non vale niente se per usarla bisogna aprire un'altra scheda, cercare, copiare e
tornare indietro — a quel punto l'operatore riscrive a mano, come faceva prima. La ricerca di mercato mette
«rispondere in fretta senza riscrivere ogni volta» al secondo posto fra le aspettative del segmento (§2.5 della
descrizione dell'applicazione). Questa storia arriva dopo le risposte predefinite (`0022`) perché riusa lo stesso
pannello di ricerca dentro la casella di risposta: due sorgenti di testo pronto, un solo gesto per l'operatore.

## 2. Requisiti funzionali

1. **RF-1** — Dalla casella di risposta di una richiesta l'operatore apre un pannello di ricerca degli articoli
   **senza perdere** il testo già scritto e senza cambiare pagina.
2. **RF-2** — La ricerca è a testo libero su titolo e corpo, limitata agli articoli dell'account, ordinata per
   pertinenza, con filtro per lingua **preimpostato sulla lingua del richiedente** e modificabile.
3. **RF-3** — L'articolo si inserisce in due modi: come **testo** (il corpo dell'articolo, o la parte selezionata,
   entra nel messaggio nel punto in cui era il cursore) oppure come **collegamento** all'articolo sul portale
   pubblico.
4. **RF-4** — L'inserimento come collegamento è offerto **solo** per gli articoli pubblicati: mandare a un cliente il
   collegamento a una bozza produrrebbe una pagina inesistente. Un articolo in bozza resta cercabile e inseribile
   come testo.
5. **RF-5** — Dopo l'inserimento il messaggio resta interamente modificabile: nessuna parte è bloccata e nessun
   segnaposto rimane da riempire senza che sia evidente.
6. **RF-6** — Ogni inserimento incrementa il contatore d'uso dell'articolo, che alimenta l'ordinamento «più usati» e
   la colonna «utilizzi» nell'elenco degli articoli.
7. **RF-7** — Se la ricerca non trova nulla, il pannello lo dice e propone di **creare** un articolo partendo
   dall'oggetto della richiesta, invece di lasciare uno spazio vuoto.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** La ricerca degli articoli filtra per `tenant_id` preso dal token
  verificato, come l'incremento del contatore d'uso; un `tenant_id` che arrivasse dai parametri viene ignorato. È il
  punto in cui una ricerca a testo libero scritta con leggerezza attraverserebbe gli account: la prova di isolamento
  qui è sulla **ricerca**, non solo sulla lettura per identificativo.
- **RT-2 — Interfaccia di programmazione (§2).** `GET /api/helpdesk/v1/knowledge-articles?q=<testo>&locale=<lingua>`
  con paginazione a pagina/dimensione e totale, e `POST /api/helpdesk/v1/knowledge-articles/{id}/usages` per
  registrare l'inserimento. Corpo validato, errori in `application/problem+json`, definizione OpenAPI aggiornata
  nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V<N>__knowledge_article_usage.sql` sullo schema `app_helpdesk`: contatore
  `usage_count` su `knowledge_article` più tabella `knowledge_article_usage` (riferimento all'articolo, riferimento
  alla richiesta, operatore, modo di inserimento) con `tenant_id`, chiave primaria UUID versione 7, colonne di
  controllo e cancellazione logica. La ricerca a testo libero usa gli indici di PostgreSQL sullo schema dell'app:
  nessun motore di ricerca esterno, che sarebbe un fornitore in più su testo di terzi.
- **RT-4 — Modulo frontend (§3, §5).** Il pannello di ricerca vive **dentro** la schermata del filo dei messaggi
  della storia `0007`, accanto al selettore delle risposte predefinite della storia `0022`, e usa lo stesso
  componente. Dati letti con il client generato; solo token del sistema di design; funziona in tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe del pannello passano dallo spazio-nomi `helpdesk` e sono presenti
  in `en, it, fr, es, de`. **Distinzione da tenere ferma**: le cinque lingue riguardano l'interfaccia dell'operatore;
  il **contenuto** dell'articolo che finisce nel messaggio è nella lingua dell'articolo, scelta in base alla lingua
  del richiedente, e non viene tradotto.
- **RT-6 — Varchi e quota (§6, §7).** Cercare e inserire un articolo **non consuma** la metrica `agents`, che è a
  giacenza. Richiede un posto operatore attivo: chi ha accesso in sola lettura ai rapporti non risponde e quindi non
  vede il pannello. Con abbonamento non attivo (`canceled`) le rotte rispondono `402`.
- **RT-7 — Esposizione conversazionale (§12).** Strumento dichiarato: `cerca_articoli(testo, lingua?) → articoli
  pertinenti`, marcato **lettura**, libero (è il `search_kb` della scheda di catalogo, §7 della descrizione
  dell'applicazione). L'inserimento in un messaggio **non** è uno strumento a sé: passa da `prepara_risposta`, che
  produce una bozza e richiede conferma (storia `0035`). Nessun percorso può cercare un articolo e mandarlo al
  cliente in un solo passo. Il contratto vive dentro il servizio; il server conversazionale è di piattaforma e non
  ancora implementato (UC 0061-0063).
- **RT-8 — Dati personali (§10).** **Nessun dato personale nuovo del cliente finale.** La tabella degli utilizzi
  registra quale operatore ha inserito quale articolo in quale richiesta: è un dato del **dipendente dell'azienda
  cliente**, della stessa natura di `agent.display_name`, già dichiarato nel manifesto; `knowledge_article_usage`
  va comunque aggiunta a `exportData` e `purgeData` del contratto `HelpdeskDataContract` perché è collegata alla
  richiesta. Il **testo cercato non si conserva**: la stringa di ricerca serve a interrogare e non finisce in
  nessuna tabella né in nessun registro tecnico — è testo scritto dall'operatore ma copiato spesso dalla richiesta
  del cliente.
- **RT-9 — Registrazione eventi (§14).** Gli eventi `articolo inserito come testo`, `articolo inserito come
  collegamento` e `ricerca senza risultati` sono registrati con `tenant_id`, `app_id`, `user_id`, identificativo
  dell'articolo e identificativo di correlazione, **senza la stringa cercata e senza il corpo dell'articolo**.

## 4. Criteri di accettazione

**CA-1 — Trovare e inserire senza uscire dalla risposta**
- **Dato** un operatore che ha già scritto due righe nella casella di risposta e un articolo pubblicato pertinente
- **Quando** apre il pannello, cerca e sceglie «inserisci come testo»
- **Allora** il corpo dell'articolo compare nel punto del cursore, le due righe già scritte restano intatte e il
  messaggio resta modificabile

**CA-2 — Collegamento solo se pubblicato**
- **Dato** un articolo in **bozza** che compare fra i risultati
- **Quando** l'operatore guarda le azioni disponibili
- **Allora** «inserisci come collegamento» non è offerta, e se la si forza chiamando direttamente la rotta la
  risposta è `409` con la spiegazione che l'articolo non è pubblicato

**CA-3 — Lingua del richiedente**
- **Dato** una richiesta di un richiedente con lingua preferita francese e articoli esistenti in italiano e francese
- **Quando** l'operatore apre il pannello · **Allora** il filtro di lingua è preimpostato sul francese e i risultati
  francesi vengono per primi; il filtro resta modificabile

**CA-4 — Nessun risultato**
- **Dato** una ricerca che non trova nulla · **Quando** il pannello si aggiorna
- **Allora** dice che non ci sono articoli e propone di crearne uno partendo dall'oggetto della richiesta, senza
  mostrare uno spazio vuoto

**CA-5 — Isolamento fra account**
- **Dato** due account `A` e `B` con articoli che contengono la stessa parola
- **Quando** un operatore di `A` cerca quella parola, anche forzando l'identificativo di `B` nei parametri
- **Allora** trova solo gli articoli di `A`, e il tentativo di registrare un utilizzo su un articolo di `B` risponde
  come per un articolo inesistente

**CA-6 — Contatore d'uso**
- **Dato** un articolo con zero utilizzi · **Quando** viene inserito tre volte in tre richieste diverse
- **Allora** l'elenco degli articoli mostra tre utilizzi e l'ordinamento «più usati» lo porta in testa

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend`, `frontend`; l'intera suite prima del commit);
- [ ] prove di **unità** sull'ordinamento per pertinenza e sulla regola «collegamento solo se pubblicato», e di
      **integrazione** sulla ricerca e sulla registrazione degli utilizzi, con database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** **sulla ricerca a testo libero**, non solo sulla lettura per
      identificativo: è la prova specifica di questa storia;
- [ ] **prova end-to-end**: *coprire ora* — passo «cerca l'articolo mentre rispondi e inseriscilo» del percorso
      `[J-HELPDESK]`, e registro di copertura
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) aggiornato;
- [ ] **traduzioni** dell'interfaccia presenti in tutte e cinque le lingue (`en, it, fr, es, de`);
- [ ] **manifesto dei dati** aggiornato in italiano e inglese con `knowledge_article_usage` in `exportData` e
      `purgeData`, e con l'annotazione che la stringa di ricerca non si conserva;
- [ ] **registro delle decisioni** `changes/NNNN-*/decisions.json` compilato, con annotata la scelta di usare la
      ricerca di PostgreSQL invece di un motore esterno;
- [ ] contratto degli **strumenti conversazionali** dichiarato per `cerca_articoli`, con la nota che l'invio passa
      solo da `prepara_risposta`;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0029` — articoli della base di conoscenza | Non si cerca ciò che non esiste; e la distinzione bozza/pubblicato è la regola su cui si fonda RF-4 |
| storia `0007` — filo dei messaggi e risposta | Il pannello vive dentro la casella di risposta: senza quella schermata non c'è dove metterlo |
| storia `0022` — risposte predefinite | Si riusa lo stesso componente di ricerca del testo pronto: due sorgenti, un solo gesto |
| storia `0031` — portale pubblico degli articoli | Il collegamento inserito punta al portale. Finché `0031` non esiste, l'inserimento come **collegamento** resta disattivato e funziona solo l'inserimento come **testo** |
| epica di piattaforma non implementata (UC 0061-0063) | Il livello conversazionale non esiste: si dichiara il contratto di `cerca_articoli`, non lo si espone |

## 7. Fuori ambito

- **La scrittura e la pubblicazione degli articoli**: storia `0029`.
- **La pagina che il cliente apre cliccando il collegamento**: storia `0031`.
- **Il suggerimento automatico dell'articolo prima ancora che l'operatore cerchi** (suggerimento sul contenuto della
  richiesta): rimandato. Ha senso quando ci sono abbastanza articoli e abbastanza utilizzi per misurare la
  pertinenza, e per il cliente finale lo fa già la storia `0033` sul modulo di contatto.
- **La misura di quanto un articolo inserito abbia effettivamente chiuso la richiesta**: fuori ambito; il cruscotto
  del servizio è della storia `0028` e questa storia gli fornisce solo il contatore d'uso.
- **Un motore di ricerca esterno con ordinamento semantico**: fuori ambito per scelta. Sarebbe un responsabile del
  trattamento in più proprio sul testo delle conversazioni (§6 della descrizione dell'applicazione).

## 8. Punti aperti

- **Quanta parte dell'articolo si inserisce.** La proposta è: tutto il corpo, con la possibilità di selezionarne una
  parte prima di inserirla. L'alternativa — un estratto automatico — richiede una regola di sintesi che è materia
  del livello conversazionale, non di questa storia. Da confermare in implementazione; se resta un dubbio, si
  registra nel `decisions.json` della change.
- **Come si ordina per pertinenza.** Con pochi articoli qualunque ordinamento sembra buono; con duecento no. La
  proposta è la ricerca a testo pieno di PostgreSQL con peso maggiore sul titolo, più il contatore d'uso come
  secondo criterio. **La chiude chi implementa**, misurando su dati di prova realistici, e la scelta va nel registro
  delle decisioni.
