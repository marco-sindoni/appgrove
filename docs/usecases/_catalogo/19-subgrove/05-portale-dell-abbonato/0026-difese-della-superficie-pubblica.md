# 0026 — Difese della superficie pubblica

**Applicazione**: 19 — SubGrove (`abbonati`) · **Epica**: 05 — Portale dell'abbonato
**Storia**: `0026` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0023`, `0024`, `0025`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che ha appena messo in rete una pagina raggiungibile da chiunque abbia il collegamento
> voglio essere certo che nessuno possa pescare i dati dei miei iscritti provando collegamenti a caso
> così da poter usare il portale senza aver aperto una porta sul mio archivio clienti.

**Contesto.** Le storie `0023`, `0024` e `0025` hanno costruito una superficie **senza credenziali**: è ciò che la
legge tedesca pretende (§ 312k del codice civile: pulsante di disdetta raggiungibile senza fare accesso) e ciò che
la prassi italiana richiede (canale digitale di recesso semplice quanto l'adesione) — vedi §2.3 della
[descrizione](../application-description.md). Ma «senza credenziali» significa che **l'unica difesa è il gettone**:
se il gettone si indovina, si enumera, non scade mai o rivela con la sua risposta che un abbonamento esiste, allora
l'app regala l'anagrafica degli iscritti a chi ha pazienza. È il rovescio esatto dell'obbligo di legge, ed è il
motivo per cui questa storia esiste come storia a sé: le difese non sono un dettaglio implementativo delle pagine,
sono il loro presupposto. **Va rilasciata insieme alla `0024`**: una disdetta pubblica senza queste difese non si
mette in produzione.

## 2. Requisiti funzionali

1. **RF-1** — Il gettone è **imprevedibile** (generato da sorgente crittografica, lunghezza tale da rendere la
   ricerca esaustiva inutile) e non contiene in chiaro né l'identificativo dell'abbonamento né quello dell'account.
2. **RF-2** — Il gettone ha una **scadenza** e un **limite d'uso**: scaduto o revocato, la pagina spiega cosa è
   successo e offre di ricevere un collegamento nuovo al recapito già noto, mai a un recapito indicato lì per lì.
3. **RF-3** — Il gettone si **revoca da solo** in tre casi: cambio del recapito dell'abbonato, cessazione
   dell'abbonamento, richiesta esplicita del cliente dalla scheda dell'abbonamento.
4. **RF-4** — Ogni risposta della superficie pubblica è **neutra**: gettone inesistente, gettone scaduto e gettone
   di un altro account producono la stessa risposta e lo stesso tempo di attesa percepito, e nessuna di esse
   conferma o smentisce che un abbonamento esista.
5. **RF-5** — Esiste un **limite di frequenza** sulle rotte pubbliche, applicato per gettone e per origine della
   richiesta: superato, si risponde `429` con l'indicazione di quando riprovare e senza altre informazioni.
6. **RF-6** — La richiesta di un collegamento nuovo ha un **tetto proprio** per abbonato e per finestra di tempo:
   nessuno deve poter usare il portale come macchina per inondare di posta un abbonato.
7. **RF-7** — Le pagine pubbliche chiedono ai motori di ricerca di **non indicizzarle**, non compaiono in alcuna
   mappa del sito e non portano alcun tracciamento: solo i cookie tecnici indispensabili, nessun banner di consenso.

## 3. Requisiti tecnici

- **RT-1 — Superficie pubblica.** Le difese vivono nel **servizio**, non nella pagina: una pagina che non chiama
  più la rotta non è una difesa. Il limite di frequenza si applica alla rotta, non al componente grafico.
- **RT-2 — Isolamento fra account (§1).** La verifica del gettone risolve **insieme** account e abbonamento: non
  esiste un percorso in cui un gettone valido dell'account `A` possa toccare un dato dell'account `B`. Il
  `tenant_id` non arriva mai dalla richiesta pubblica: si ricava dal gettone verificato.
- **RT-3 — Interfaccia di programmazione (§2).** Le rotte `/api/abbonati/v1/pubblico/**` rispondono in
  `application/problem+json` con un tipo di errore **unico e generico** per tutti i casi di gettone non valido, e
  con `Retry-After` quando il limite di frequenza scatta. La definizione OpenAPI dichiara le risposte `404`/`429`
  senza rivelare la differenza fra i casi.
- **RT-4 — Persistenza (§8).** Migrazione `V19__difese_gettone.sql` sullo schema `app_abbonati`: colonne di
  revoca (momento e motivo) e conteggio degli usi sulla tabella dei gettoni della storia `0023`, più la tabella
  `tentativo_pubblico` con `tenant_id`, colonne di controllo e finestra di conteggio. Il gettone resta conservato
  **come impronta**, mai in chiaro.
- **RT-5 — Modulo frontend (§3, §5).** La pagina di errore usa solo i token del sistema di design, dice una cosa
  sola («questo collegamento non è più valido») e offre una sola azione; nessun messaggio diverso per i casi
  diversi, altrimenti la neutralità del backend si perde davanti all'utente.
- **RT-6 — Cinque lingue (§4).** I testi delle pagine di errore e di limite raggiunto in `en, it, fr, es, de`.
- **RT-7 — Varchi e quota (§6, §7).** La superficie pubblica **non** consuma la metrica `abbonamenti_attivi`: non
  crea nulla. Il limite di frequenza è un presidio di sicurezza, non una quota commerciale, e **non si vende**.
- **RT-8 — Esposizione conversazionale (§12).** Nessuno strumento: la revoca di un gettone si fa dall'interfaccia
  del cliente. Uno strumento che revocasse collegamenti in blocco sarebbe un modo elegante per impedire una
  disdetta, cioè esattamente ciò che la norma vieta.
- **RT-9 — Dati personali (§10).** Nessun campo nuovo riferito a persone. Il conteggio dei tentativi **non**
  conserva l'indirizzo di rete oltre la finestra di conteggio, e ciò va scritto nel manifesto come esclusione
  esplicita insieme alla sua motivazione.
- **RT-10 — Registrazione eventi (§14).** `gettone respinto`, `limite di frequenza raggiunto`, `gettone revocato
  (motivo)`, con `tenant_id`, `app_id` e identificativo di correlazione, **senza** il gettone, senza il recapito e
  senza l'indirizzo di rete.
- **RT-11 — Prove (§11).** Prova che le tre risposte negative sono indistinguibili; prova che il limite di
  frequenza scatta e che si riapre; prova che un gettone revocato non risolve più.

## 4. Criteri di accettazione

**CA-1 — Risposte indistinguibili**
- **Dato** un gettone inesistente, uno scaduto e uno valido di un altro account
- **Quando** si chiamano le rotte pubbliche con ciascuno dei tre
- **Allora** le tre risposte hanno lo stesso codice, lo stesso corpo e lo stesso testo a schermo, e nessuna dice
  se un abbonamento esiste

**CA-2 — Limite di frequenza**
- **Dato** una raffica di richieste dalla stessa origine sulla rotta pubblica
- **Quando** si supera la soglia
- **Allora** si riceve `429` con l'indicazione di quando riprovare, l'evento è registrato senza indirizzo di rete,
  e dopo la finestra la pagina torna a funzionare per un gettone valido

**CA-3 — Tetto ai reinvii del collegamento**
- **Dato** un abbonato per cui è già stato chiesto un collegamento nuovo il numero massimo di volte consentito
- **Quando** si chiede ancora
- **Allora** non parte alcun messaggio, la pagina risponde nello stesso modo neutro di sempre e il cliente vede
  l'evento nella scheda

**CA-4 — Revoca automatica**
- **Dato** un abbonamento che passa a `cessato` e un abbonato che ha cambiato indirizzo di posta
- **Quando** si aprono i collegamenti emessi prima
- **Allora** nessuno dei due risolve più, e il motivo della revoca è visibile al cliente (non all'abbonato)

**CA-5 — Il gettone non è indovinabile né indicizzato**
- **Dato** la pagina pubblica di un abbonamento
- **Quando** la si esamina
- **Allora** l'indirizzo non contiene identificativi ricavabili per tentativi, la pagina chiede di non essere
  indicizzata e non carica alcun tracciamento

**CA-6 — Isolamento fra account**
- **Dato** due account `A` e `B` con i propri gettoni
- **Quando** si usa un gettone di `A` contro una rotta che nomina l'account `B`
- **Allora** la risposta è quella neutra e nessun dato di `B` viene toccato

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend`, `frontend`, `compliance`);
- [ ] prove di **unità** sulla generazione e sulla verifica del gettone e sul limite di frequenza;
      **integrazione** sulle rotte pubbliche con database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** sulla superficie pubblica;
- [ ] **prova end-to-end**: *coprire ora* — il percorso `[J-ABBONATI-PUBBLICO]` della storia `0034` verifica il
      collegamento scaduto e la risposta neutra; registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) aggiornato;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato con l'esclusione esplicita dell'indirizzo di rete oltre la finestra di
      conteggio;
- [ ] **registro delle decisioni** compilato: risposte neutre, limiti di frequenza, casi di revoca, nessun
      tracciamento;
- [ ] controllo di accessibilità verde sulle pagine di errore e di limite raggiunto;
- [ ] documentazione aggiornata dove descrive la superficie pubblica.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0023` | è la pagina e il gettone che qui si difendono |
| storia `0024` | **le due vanno rilasciate insieme**: la disdetta pubblica senza difese non si mette in produzione |
| storia `0025` | anche la richiesta di cambio piano passa dalle stesse rotte pubbliche |

## 7. Fuori ambito

- un servizio esterno di protezione dagli abusi (rete di distribuzione dei contenuti, filtro automatico dei
  robot): introdurrebbe un fornitore, ed è una scelta di piattaforma, non di questa app;
- l'enigma anti-robot da risolvere prima di disdire: **deliberatamente escluso**, sarebbe un ostacolo alla disdetta
  e quindi un rischio di conformità peggiore di quello che eviterebbe;
- la sorveglianza dei volumi lato piattaforma: sta nella console di amministrazione
  ([estensioni-admin.md](../estensioni-admin.md)).

## 8. Punti aperti

**Dove finisce la difesa applicativa e dove comincia quella di infrastruttura.** Un limite di frequenza scritto
nell'applicazione ferma il curioso, non una raffica distribuita: quella la ferma solo il livello di rete, che è di
piattaforma. **Proposta**: fare bene ciò che tocca all'app (imprevedibilità, revoca, neutralità, tetto per gettone)
e portare il resto come richiesta all'infrastruttura, senza fingere che l'app possa reggerlo da sola.
Chiude: **piattaforma**.

**Quanti reinvii del collegamento sono ragionevoli.** Troppo pochi e l'abbonato che ha perso la mail resta fuori —
cioè non riesce a disdire, che è il difetto peggiore; troppi e il portale diventa un innaffiatoio di posta.
**Proposta**: tetto giornaliero basso con ripristino automatico, e nessun messaggio di errore che riveli il tetto.
Chiude: lo sviluppatore.
