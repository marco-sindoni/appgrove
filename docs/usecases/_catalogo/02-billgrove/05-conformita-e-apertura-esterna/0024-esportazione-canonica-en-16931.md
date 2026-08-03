# 0024 — Esportazione canonica EN 16931

**Applicazione**: 02 — BillGrove (`billing`) · **Epica**: 05 — Conformità e apertura verso l'esterno
**Storia**: `0024` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0012`, `0013`, `0022`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come responsabile della piattaforma che deve poter far viaggiare i documenti verso l'esterno
> voglio che ogni documento emesso sia esportabile in una **forma canonica** conforme alla norma europea
> così da poter aggiungere domani un canale di trasmissione, un conservatore o uno strato di conformità senza
> riaprire il cuore dell'app.

**Contesto.** È la storia che rende vera l'indicazione del catalogo (§6): InvoiceGrove (1) non è un prodotto
autonomo, va progettato come **strato di conformità di BillGrove**, che è il **sistema di origine** del documento.
Perché quella divisione funzioni serve un confine preciso: BillGrove produce un documento in forma canonica
allineata alla norma europea EN 16931 — che la riforma ViDA indica come riferimento comune, con obblighi che
maturano fino al 2030 e al 2035 (§2.3 della descrizione) — e **non** conosce le regole di validazione, i formati né
i cicli di vita legali delle singole giurisdizioni. Quelli sono di InvoiceGrove.

## 2. Requisiti funzionali

1. **RF-1** — Ogni documento emesso è esportabile in una rappresentazione canonica strutturata, allineata al modello
   semantico della norma europea EN 16931.
2. **RF-2** — La rappresentazione canonica contiene tutto ciò che serve a valle: identificazione delle due parti,
   numero e data, righe, riepiloghi per aliquota con la natura delle operazioni, imposta di bollo, valuta e tasso,
   riferimenti ad altri documenti, condizioni e scadenze di pagamento.
3. **RF-3** — L'esportazione è **deterministica**: lo stesso documento produce sempre lo stesso risultato.
4. **RF-4** — Se un documento emesso non è esportabile perché gli manca un dato obbligatorio del modello canonico,
   la mancanza è **segnalata prima**, in fase di emissione, non scoperta dopo.
5. **RF-5** — L'esportazione è disponibile per un documento singolo e per un insieme di documenti di un periodo.
6. **RF-6** — La forma canonica **non** contiene formati né campi propri di una singola giurisdizione: se comincia a
   contenerli, il confine con InvoiceGrove è stato rotto.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** L'esportazione riguarda solo documenti del `tenant_id` preso dal token
  verificato; chiedere l'esportazione di un documento altrui risponde `404`.
- **RT-2 — Interfaccia di programmazione (§2).** `GET /api/billing/v1/documents/{id}/canonical` e
  `GET /api/billing/v1/exports/canonical?from=&to=`; errori in `application/problem+json`; definizione OpenAPI
  aggiornata nello stesso commit. La forma canonica è **parte del contratto pubblico** dell'app: cambiarla è un
  cambiamento che rompe chi la consuma, e va versionata.
- **RT-3 — Persistenza (§8).** Nessuna tabella nuova: la forma canonica si genera dai dati congelati del documento.
  Conservare il risultato sarebbe una seconda fonte di verità.
- **RT-4 — Modulo frontend (§3, §5).** Azione «Esporta in formato standard» sulla scheda del documento e nella
  sezione «Report» per un periodo. Solo token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Le etichette dell'interfaccia sono nelle cinque lingue; la forma canonica, invece,
  **non è tradotta**: è un formato di scambio, non un testo da leggere.
- **RT-6 — Varchi e quota (§6).** Nessun consumo di quota: si esportano documenti già emessi, che hanno già
  consumato.
- **RT-7 — Esposizione conversazionale (§12).** **Nessuno strumento**: restituire una struttura di scambio a una
  chat non serve a nessuno e apre a usi impropri. Va dichiarato.
- **RT-8 — Dati personali (§10).** L'esportazione fa **uscire** dati personali dall'app in forma strutturata: nel
  manifesto va dichiarato che esiste un'uscita di questo genere, con la finalità (adempimento fiscale) e la base
  giuridica (obbligo di legge). Chi riceve l'esportazione — un canale, un conservatore, il commercialista — è un
  destinatario, e se agisce per nostro conto è un responsabile esterno del trattamento: va dichiarato **prima** di
  attivarlo (storie `0025` e `0027`).
- **RT-9 — Registrazione eventi (§14).** Gli eventi `documento esportato in forma canonica` e `esportazione
  rifiutata per dato obbligatorio mancante` sono registrati con `tenant_id`, `app_id`, `user_id` e identificativo di
  correlazione, senza dati personali.

## 4. Criteri di accettazione

**CA-1 — Esportazione di un documento completo**
- **Dato** una fattura emessa con due aliquote, imposta di bollo e scadenza
- **Quando** si chiede la forma canonica
- **Allora** si ottiene una struttura che contiene tutte le informazioni elencate in RF-2

**CA-2 — Determinismo**
- **Dato** lo stesso documento · **Quando** si esporta due volte
- **Allora** il risultato è identico byte per byte

**CA-3 — Dato obbligatorio mancante**
- **Dato** un cliente senza indirizzo completo
- **Quando** si tenta di **emettere** una fattura per quel cliente
- **Allora** l'emissione è rifiutata con l'indicazione del dato mancante: la mancanza si scopre qui, non
  all'esportazione

**CA-4 — Nessun campo di giurisdizione**
- **Dato** la forma canonica prodotta
- **Quando** la si confronta con il modello semantico europeo
- **Allora** non contiene campi propri di una singola giurisdizione

**CA-5 — Isolamento fra account**
- **Dato** un documento dell'account `B` · **Quando** un utente di `A` ne chiede la forma canonica
- **Allora** riceve `404`

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend`, `frontend`, `compliance`; l'intera suite prima del
      commit);
- [ ] prove di **unità** sulla generazione della forma canonica (casi con esenzioni, bollo, valuta estera, nota di
      credito con riferimento) e di **integrazione** sulle rotte, con database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** sull'esportazione;
- [ ] **prova end-to-end**: *rimando* — l'esportazione non è nel percorso principale `[J-BILLING]`, che segue
      l'utente e non il formato. Proprietaria del rimando: storia `0031`;
- [ ] **traduzioni** presenti in tutte e cinque le lingue per l'interfaccia;
- [ ] **manifesto dei dati** aggiornato con l'uscita strutturata di dati e la sua base giuridica;
- [ ] **registro delle decisioni** compilato, con annotato il **confine con InvoiceGrove**: è la decisione più
      importante di questa storia;
- [ ] contratto degli **strumenti conversazionali**: nessuno, con la motivazione scritta;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata: la forma canonica è contratto pubblico e va descritta.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0012` | Si esportano documenti emessi, con i dati congelati |
| storia `0013` | I riepiloghi per aliquota e la natura delle operazioni sono parte del modello canonico |
| storia `0022` | Valuta e tasso sono parte del modello canonico |

## 7. Fuori ambito

- la conversione nei formati nazionali (Italia, Francia, Polonia, Germania, rete europea di interscambio): è di
  InvoiceGrove (1);
- le regole di validazione per giurisdizione e le macchine a stati del ciclo di vita legale: idem;
- la firma digitale del documento: idem;
- la trasmissione: storia `0025`.

## 8. Punti aperti

Il confine con InvoiceGrove (1) è indicato dal catalogo ma non è ancora una decisione presa: se InvoiceGrove nasca
come app separata che consuma questa esportazione, oppure come epica dentro BillGrove, lo decide lo sviluppatore.
Questa storia è scritta in modo da funzionare in entrambi i casi, ma la scelta va fatta prima di costruire il
canale della storia `0025`.
