# 0031 — Rapporto periodico programmato

**Applicazione**: 32 — TokenGrove (`spesa_modelli`) · **Epica**: 06 — Cruscotti, confronti e rapporti
**Storia**: `0031` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0025`, `0028`, `0030`
**Ultimo aggiornamento**: 2026-08-04

## 1. Narrazione

> Come titolare che non aprirà TokenGrove tutte le settimane
> voglio ricevere il primo del mese un riepilogo della spesa del mese appena chiuso
> così da accorgermi di un andamento sbagliato anche quando non ho guardato niente per trenta giorni.

**Contesto.** Il cliente tipo di questa app non è un utente quotidiano: chi collega le fonti entra spesso, chi paga
la fattura entra raramente. Il rapporto periodico è ciò che porta il prodotto **a chi non lo apre**, ed è anche il
promemoria che l'abbonamento sta facendo qualcosa: un'app di misurazione che non si fa sentire viene disdetta. È il
gemello preventivo dell'avviso di budget (storia `0025`): quello arriva quando qualcosa va storto, questo arriva
comunque, e serve a stabilire la normalità rispetto a cui il primo si capisce.

**La scelta di fondo, dichiarata subito.** Il messaggio recapitato contiene **numeri complessivi e andamenti**, non
gli elenchi per cliente finale né le etichette: quei valori possono riguardare persone (§6 del documento capofila) e
un messaggio di posta elettronica è il posto meno controllabile in cui farli finire — si inoltra, resta nelle
caselle, esce dal perimetro. Il dettaglio si guarda **dentro l'app**, dietro un accesso, e il messaggio porta il
collegamento.

## 2. Requisiti funzionali

1. **RF-1** — Un account può programmare uno o più rapporti dichiarando: periodicità (settimanale o mensile),
   giorno e ora di recapito, ambito (tutto l'account oppure un valore di dimensione), destinatari e lingua.
2. **RF-2** — Il rapporto recapitato contiene: totale del periodo, variazione rispetto al periodo precedente,
   previsione per quello in corso, i primi modelli per importo, lo stato dei budget e i tre indicatori di
   affidabilità della panoramica (copertura, freschezza, età del catalogo prezzi).
3. **RF-3** — Il rapporto **non contiene** valori di etichetta che identificano clienti o utenti finali: per quel
   dettaglio porta un collegamento alla schermata corrispondente dentro l'app, che richiede l'accesso.
4. **RF-4** — Un rapporto **non parte mai su dati non affidabili senza dirlo**: se nel periodo una fonte è stata
   ferma, se la riconciliazione col rendiconto mostra uno scarto oltre soglia o se il catalogo prezzi è più vecchio
   della soglia, il rapporto lo dichiara in testa, prima dei numeri.
5. **RF-5** — Ogni rapporto programmato si può sospendere e riattivare; ogni recapito resta a registro con esito,
   e un recapito fallito viene ritentato secondo la politica già in uso per gli avvisi (storia `0025`).
6. **RF-6** — Un destinatario può togliersi dai rapporti dal messaggio stesso, senza dover entrare nell'app; la
   rimozione è tracciata e visibile a chi ha programmato il rapporto.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Programmazione, generazione e recapito agiscono sul solo `tenant_id`;
  il collegamento contenuto nel messaggio non concede accesso da solo e porta al normale accesso dell'account.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `POST|GET|PATCH|DELETE /api/spesa_modelli/v1/rapporti` e
  `GET /api/spesa_modelli/v1/rapporti/{id}/recapiti`; errori in `problem+json`; definizione OpenAPI aggiornata nello
  stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione sullo schema `app_spesa_modelli`: tabelle `rapporto` (periodicità,
  ambito, destinatari, lingua, stato) e `recapito_rapporto` (istante, esito, destinatari raggiunti), entrambe con
  `tenant_id`, chiave primaria UUID versione 7, colonne di controllo e cancellazione logica.
- **RT-4 — Generazione periodica.** L'esecuzione programmata è idempotente per periodo e rapporto: una ripartenza
  del servizio o un secondo passaggio dello stesso periodo **non** produce un secondo recapito. Il periodo si
  considera solo quando è **chiuso**, per la stessa ragione dei prospetti (storia `0022`).
- **RT-5 — Recapito (§10).** Il recapito usa il fornitore di posta già in uso dalla piattaforma: **nessun fornitore
  esterno nuovo**. Vale la stessa politica di ritentativi e la stessa tracciatura degli avvisi (storia `0025`).
- **RT-6 — Modulo frontend (§3, §5).** Sezione «Rapporti»: elenco dei rapporti programmati, creazione con
  anteprima del contenuto, registro dei recapiti. Solo token del sistema di design; tema chiaro e scuro.
- **RT-7 — Cinque lingue (§4).** L'interfaccia e **il corpo del messaggio** sono presenti in `en, it, fr, es, de`;
  la lingua del messaggio è quella dichiarata sul rapporto, non quella dell'ultimo utente che l'ha modificato.
- **RT-8 — Varchi, ruoli e quota (§6, §7).** Programmare un rapporto è riservato a `owner` e `admin`. I rapporti
  programmati sono una funzionalità dei piani a pagamento (`features` del listino, §5 del documento capofila), non
  una seconda metrica di quota. La generazione **non consuma** `misure_registrate`. Con abbonamento `canceled` i
  rapporti smettono di partire; in `past_due` continuano.
- **RT-9 — Esposizione conversazionale (§12).** La **creazione o modifica** di un rapporto è **scrittura con
  conferma** (storia `0033`): stabilisce un invio ricorrente verso l'esterno, verso destinatari indicati a parole.
  La **lettura** dei rapporti programmati e dei recapiti resta sulle rotte di lettura e non introduce uno strumento
  conversazionale nuovo in questa prima versione del contratto (storia `0032`, §7).
- **RT-10 — Dati personali (§10).** Gli indirizzi dei destinatari sono dati personali: voci nuove nel manifesto
  `docs/compliance/manifests/spesa_modelli.yaml` in italiano e inglese, campi annotati `@PersonalData`, tabelle
  `rapporto` e `recapito_rapporto` in `exportData` e `purgeData` (storia `0035`). Il corpo del messaggio non
  contiene etichette riferibili a persone (RF-3).
- **RT-11 — Registrazione eventi (§14).** Eventi «rapporto programmato», «rapporto generato», «recapito riuscito o
  fallito» con `tenant_id`, `app_id`, `user_id`, periodo ed esito, **senza** indirizzi di posta né importi.

## 4. Criteri di accettazione

**CA-1 — Il rapporto mensile arriva**
- **Dato** un rapporto mensile programmato per il primo del mese alle 8
- **Quando** il mese si chiude
- **Allora** i destinatari ricevono un messaggio con totale, variazione, previsione, primi modelli, stato dei budget
  e i tre indicatori di affidabilità

**CA-2 — Nessuna etichetta nel messaggio**
- **Dato** un account che attribuisce la spesa a clienti finali con nome e cognome nell'etichetta
- **Quando** il rapporto viene recapitato
- **Allora** il messaggio non contiene nessun valore di etichetta, ma il collegamento alla schermata che li mostra
  dopo l'accesso

**CA-3 — Dati non affidabili, detto in testa**
- **Dato** una fonte rimasta ferma per tre giorni nel periodo del rapporto
- **Quando** il rapporto viene generato
- **Allora** l'avvertenza sulla lacuna precede i numeri, e il rapporto parte comunque

**CA-4 — Nessun doppio recapito**
- **Dato** un riavvio del servizio durante la finestra di generazione
- **Quando** l'esecuzione riparte
- **Allora** per lo stesso rapporto e lo stesso periodo esiste un solo recapito

**CA-5 — Rimozione dal rapporto**
- **Dato** un destinatario che usa il collegamento di rimozione contenuto nel messaggio
- **Quando** il rapporto successivo viene recapitato
- **Allora** non lo riceve, e chi ha programmato il rapporto vede la rimozione nel registro

**CA-6 — Isolamento fra account**
- **Dato** due account con rapporti programmati alla stessa ora
- **Quando** i rapporti partono
- **Allora** ciascun messaggio contiene solo i numeri del proprio account

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (backend e frontend; l'intera suite prima del commit);
- [ ] prove di **unità** sull'idempotenza della generazione per periodo e sulla composizione del contenuto, e di
      **integrazione** sul ciclo programmazione → generazione → recapito → registro;
- [ ] prova esplicita che **nessun valore di etichetta** compare nel corpo del messaggio, in tutte e cinque le
      lingue;
- [ ] prova di **isolamento fra account** e **matrice dei ruoli** sulla programmazione;
- [ ] **prova end-to-end**: **coprire ora**, estendendo `[J-SPESA-MODELLI]` con il passo «programmo il rapporto
      mensile, il periodo si chiude, il recapito risulta a registro», e aggiornare il registro di copertura
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml);
- [ ] **traduzioni** presenti in tutte e cinque le lingue, corpo del messaggio compreso;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese: destinatari dei rapporti e registro dei recapiti, con
      i campi annotati e le tabelle in esportazione e cancellazione;
- [ ] **registro delle decisioni** compilato, in particolare sull'esclusione delle etichette dal messaggio e
      sull'idempotenza per periodo;
- [ ] contratto degli **strumenti conversazionali** dichiarato: programmare un rapporto = scrittura con conferma;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storia `0025` | Riusa la politica di recapito, ritentativo e tracciatura già costruita per gli avvisi |
| Storia `0028` | Il contenuto del rapporto è la panoramica in forma di messaggio |
| Storia `0030` | L'eventuale tavola allegabile riusa la generazione dell'esportazione, non una seconda |

## 7. Fuori ambito

- il recapito su **messaggistica di squadra**: rimandato; è un fornitore esterno nuovo e va valutato come tale
  (§10 dei principi). La posta elettronica è già di piattaforma e basta a coprire il bisogno dichiarato;
- il **rapporto su misura** (scelta libera dei riquadri e delle metriche): rimandato, per la stessa ragione per cui
  la panoramica non è componibile (storia `0028`, §7);
- l'invio del rapporto a un destinatario **esterno all'account** (per esempio il cliente finale che paga il
  ribaltamento): fuori ambito, perché farebbe uscire dall'azienda numeri che oggi restano dentro, e la decisione
  spetta al cliente con i propri strumenti.

## 8. Punti aperti

- **Se allegare al messaggio la tavola aggregata del periodo.** È comodo e molto richiesto, ma un allegato esce
  dal perimetro insieme al messaggio e — anche se aggregato — porta importi. Proposta: **no allegato**
  predefinito, con la possibilità di attivarlo per i soli rapporti a livello di account (senza dettaglio per
  cliente finale). La conferma lo sviluppatore.
- **Se i rapporti debbano essere compresi nel piano intermedio o solo in quello alto.** La proposta di listino (§5
  del documento capofila) mette i «rapporti programmati» nel piano alto; ma è anche la funzione che tiene vivo
  l'abbonamento di chi non apre l'app, e nel piano alto la vedono in pochi. È una decisione di prezzo: la chiude
  lo sviluppatore.
