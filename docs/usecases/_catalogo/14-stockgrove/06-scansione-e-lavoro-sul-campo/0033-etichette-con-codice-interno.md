# 0033 — Etichette con codice interno

**Applicazione**: 14 — StockGrove (`magazzino`) · **Epica**: 06 — Scansione e lavoro sul campo
**Storia**: `0033` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0006`, `0007`, `0030`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che tiene ricambi, materiale di consumo e merce sfusa
> voglio stampare un'etichetta con un codice mio per la merce che un codice non ce l'ha
> così da poter scansionare anche quello che nessuno ha mai etichettato, invece di cercarlo a mano ogni volta.

**Contesto.** Le storie `0030` e `0031` fanno funzionare la scansione **se** sulla scatola c'è un codice. Nel
magazzino di una micro-impresa metà della merce non ce l'ha: ricambi sfusi, materiale tagliato a misura, pezzi
prodotti in casa, articoli riconfezionati. Senza etichetta quella metà resta fuori dal flusso veloce, e un
programma che funziona a metà del magazzino non viene usato in nessuna metà. È il momento giusto adesso perché i
codici interni della storia `0007` esistono già: qui si aggiunge solo il modo di metterli su carta.

**Il confine che questa storia non attraversa.** Si genera **il codice interno dell'impresa**, mai un codice
GTIN. Il prefisso aziendale da cui nasce un codice GTIN è **noleggiato** a GS1 — in Italia costa 300 € di
iscrizione e 95 € l'anno per un pacchetto di mille codici (descrizione §2.3 punto 4, fonte 7) — e non può essere
inventato né preso in prestito da un altro. Un codice interno stampato per sbaglio come GTIN esce dall'azienda
attaccato alla merce, finisce nel sistema di un cliente o di un distributore e collide con il prodotto di
qualcun altro: è un errore che non si corregge da dentro. Perciò l'app scrive sull'etichetta un codice
riconoscibile come interno e non offre da nessuna parte il gesto «genera un codice a barre commerciale».

## 2. Requisiti funzionali

1. **RF-1** — Per un articolo privo di codici si può **generare un codice interno**, univoco per account, con un
   formato leggibile e riconoscibile come interno; il codice generato diventa un codice dell'articolo secondo il
   modello della storia `0007` e da quel momento è scansionabile.
2. **RF-2** — Il codice interno è reso come **codice QR** sull'etichetta, insieme al codice in chiaro e alla
   descrizione dell'articolo abbreviata; il codice in chiaro serve a chi legge quando la fotocamera non ce la fa
   (descrizione §2.6, fonte 8).
3. **RF-3** — Si possono selezionare **più articoli** dall'elenco e produrre un foglio di etichette in un colpo
   solo, nell'ordine dell'elenco.
4. **RF-4** — Il foglio è impaginato per i **formati comuni di etichette adesive** in fogli A4, fra cui si sceglie
   prima di stampare; l'anteprima mostra la disposizione reale, con le etichette allineate alle caselle del foglio.
5. **RF-5** — La stampa avviene **dal browser**, senza alcun programma da installare e senza inviare niente a
   servizi esterni; il rendering del codice QR avviene sul dispositivo, con la libreria già presente nel pacchetto
   del frontend.
6. **RF-6** — Da nessun punto dell'applicazione è possibile **generare un codice GTIN**: la funzione non esiste,
   e nella schermata delle etichette una nota spiega perché in una riga, con il rimando alla registrazione di un
   codice GTIN già posseduto (storia `0007`).
7. **RF-7** — Ristampare l'etichetta di un articolo che ha già un codice interno **non genera un codice nuovo**:
   si ristampa quello esistente, altrimenti lo stesso pezzo finirebbe con due identità.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** La generazione del codice e la preparazione del foglio filtrano per
  `tenant_id` preso dal token verificato; un `tenant_id` che arrivasse dal corpo della richiesta viene ignorato.
  L'unicità del codice interno è **per account**: due account possono avere lo stesso codice interno senza
  interferenze, e nessuno vede gli articoli dell'altro. Prova di isolamento fra due account sulla rotta.
- **RT-2 — Interfaccia di programmazione (§2).** Rotta `POST /api/magazzino/v1/etichette` che riceve l'elenco
  degli articoli e il formato del foglio e restituisce i dati dell'etichetta (codice, testo abbreviato) già
  ordinati; la generazione del codice interno mancante passa dalla rotta dei codici della storia `0007`. Corpo
  validato; errori in `application/problem+json`; definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** **Nessuna migrazione nuova**: i codici stanno nella tabella della storia `0007`,
  con il proprio vincolo di unicità per account. Non si conserva niente delle stampe: un foglio stampato non è un
  documento da archiviare.
- **RT-4 — Modulo frontend (§3, §5).** La stampa è un percorso della sezione `articoli` del modulo `magazzino`,
  non una sezione nuova del manifesto. L'anteprima e il foglio usano i soli token del sistema di design e
  funzionano in tema chiaro e scuro; il foglio da stampare è però **sempre su fondo bianco con inchiostro nero**,
  in entrambi i temi, perché un'etichetta si legge sulla carta e un fondo scuro sprecherebbe il toner e
  peggiorerebbe la lettura.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe visibili — nomi dei formati di foglio, nota sul codice GTIN,
  messaggi di errore — passano dallo spazio-nomi `magazzino` e sono presenti in `en, it, fr, es, de`. I nomi dei
  formati di etichetta sono chiavi tradotte, non testo scritto a mano nei componenti.
- **RT-6 — Varchi e quota (§6, §7).** **La stampa non consuma quota e non risponde mai `429`**: stampare
  un'etichetta non aumenta il numero di articoli gestiti, perché l'articolo esiste già. Il tetto
  `articoli_gestiti` (natura `stock`) resta sulla sola creazione di articoli nuovi. Con abbonamento `canceled` la
  rotta risponde `402`.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento dichiarato: stampare è un gesto che finisce su
  una stampante fisica e non ha senso da una chat, dove non si può né scegliere il foglio né verificare il
  risultato. Se un domani servisse, sarebbe uno strumento di **scrittura** con bozza e conferma. Server
  conversazionale di piattaforma, non ancora implementato (UC 0061-0063).
- **RT-8 — Dati personali (§10).** **Nessun dato personale nuovo.** L'etichetta porta un codice e una descrizione
  di merce; la descrizione dell'articolo è un campo a testo libero già dichiarato nel manifesto dalla storia
  `0010`, e sull'etichetta compare abbreviata. **Nessuna immagine della fotocamera è coinvolta**: qui non si legge,
  si stampa.
- **RT-9 — Registrazione eventi (§14).** Gli eventi `codice interno generato` e `foglio di etichette preparato`
  (con il numero di etichette) sono registrati con `tenant_id`, `app_id`, `user_id` e identificativo di
  correlazione, senza il codice e senza la descrizione. Nessun conteggio per persona (descrizione §6).

## 4. Criteri di accettazione

**CA-1 — Codice interno generato e subito scansionabile**
- **Dato** un articolo senza alcun codice
- **Quando** se ne genera il codice interno e lo si stampa
- **Allora** l'articolo ha un codice interno univoco per l'account, l'etichetta mostra il codice QR e il codice in
  chiaro, e la lettura di quel codice QR con il lettore della storia `0030` risolve **su quell'articolo**

**CA-2 — Foglio con più articoli**
- **Dato** dodici articoli selezionati dall'elenco e un formato di etichette da ventiquattro caselle per foglio
- **Quando** si prepara la stampa
- **Allora** l'anteprima mostra un solo foglio con le dodici etichette nelle prime dodici caselle, nell'ordine
  dell'elenco, e le caselle restanti vuote

**CA-3 — Nessun codice GTIN generabile**
- **Dato** un utente che cerca la funzione «genera codice a barre commerciale»
- **Quando** percorre la schermata delle etichette e quella dei codici dell'articolo
- **Allora** la funzione non esiste in nessun punto, e la nota spiega che un codice GTIN si può solo registrare se
  già posseduto, perché il prefisso è noleggiato a GS1

**CA-4 — Ristampa senza duplicare l'identità**
- **Dato** un articolo che ha già un codice interno
- **Quando** se ne ristampa l'etichetta
- **Allora** il codice stampato è **lo stesso** di prima, nessun codice nuovo viene creato e l'articolo continua
  ad avere un solo codice interno

**CA-5 — Isolamento fra account**
- **Dato** due account `A` e `B`
- **Quando** un utente di `A` prepara un foglio indicando anche un articolo di `B`
- **Allora** riceve `404`, nessuna etichetta di `B` compare nel foglio e nessuna informazione sull'articolo di `B`
  è restituita

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno backend e frontend; l'intera suite prima del commit);
- [ ] prove di **unità** sulla generazione del codice interno (univocità per account, ristampa che non duplica) e
      sull'impaginazione per formato; prova di **integrazione** sulla rotta delle etichette con database effimero e
      migrazioni vere;
- [ ] prova di **isolamento fra account** sulla preparazione del foglio;
- [ ] **prova end-to-end**: *nessun impatto* sul percorso `[J-MAGAZZINO]` — la stampa finisce su una stampante
      fisica e non è verificabile in una prova automatica; la parte verificabile (il codice generato è
      scansionabile) è coperta dalle prove di integrazione e dal passo di ricerca per codice del percorso di
      proprietà della storia `0036`. Registro di copertura
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) invariato;
- [ ] **traduzioni** presenti in tutte e cinque le lingue, compresi i nomi dei formati di foglio;
- [ ] **manifesto dei dati**: nessuna voce nuova;
- [ ] **registro delle decisioni** compilato, con la scelta di non generare mai un codice GTIN e con il motivo;
- [ ] contratto degli **strumenti conversazionali**: nessuno introdotto, con il motivo scritto;
- [ ] verifica manuale della stampa su carta, con un formato di etichette reale, e controllo che il foglio esca su
      fondo bianco anche in tema scuro;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| `0006` | Gli articoli da etichettare devono esistere, con la loro descrizione |
| `0007` | I codici dell'articolo e la loro unicità per account: il codice interno vive lì |
| `0030` | Il lettore, che è la ragione per cui l'etichetta serve: senza scansione un'etichetta è carta |

## 7. Fuori ambito

- **Etichette per ubicazione o per deposito** (l'etichetta sullo scaffale, non sulla merce): sono utili e diverse
  — cambiano il contenuto del codice e il flusso di lettura — e non sono richieste da nessuna fonte; restano
  fuori, e se serviranno saranno una storia a sé.
- **Stampanti di etichette dedicate** con il proprio linguaggio di comando: qui si stampa dal browser su fogli
  adesivi A4. Il collegamento diretto a una stampante termica è un lavoro di integrazione con hardware, fuori
  perimetro.
- **Codici a barre lineari sull'etichetta**: si stampa un codice QR, che tiene più informazione in meno spazio e
  si legge meglio con la fotocamera; il codice lineare si **legge** (storia `0030`) ma non si produce.
- **Registrare un codice GTIN già posseduto**: è della storia `0007`.
- **Etichette con il prezzo di vendita**: StockGrove non possiede i prezzi di vendita e non li mostra
  (descrizione §10).

## 8. Punti aperti

- **Formato del codice interno**. Un codice generato dall'app (una sequenza breve) è semplice ma non parla; un
  codice che segue la nomenclatura del cliente parla ma va configurato. La proposta è di generare una sequenza
  breve e di lasciare che chi vuole scriva il proprio codice a mano nella storia `0007`; se il primo uso reale
  dicesse il contrario, la configurazione del formato è una storia successiva.
- **Quali formati di etichette adesive sostenere davvero.** I formati comuni in fogli A4 sono molti e variano per
  paese: l'elenco iniziale va scelto dallo sviluppatore su ciò che si compra davvero in Europa, e non l'ho
  determinato in analisi.
