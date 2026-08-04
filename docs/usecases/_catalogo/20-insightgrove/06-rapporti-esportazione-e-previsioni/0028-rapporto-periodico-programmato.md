# 0028 — Rapporto periodico programmato

**Applicazione**: 20 — InsightGrove (`insights`) · **Epica**: 06 — Rapporti, esportazione e previsioni
**Storia**: `0028` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0017`, `0020`, `0027`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che si ricorda di guardare i numeri solo quando qualcosa è già andato storto
> voglio ricevere il lunedì mattina, per posta elettronica, il riepilogo del mio cruscotto
> così da vedere l'andamento senza dover decidere di andarlo a cercare.

**Contesto.** L'invio programmato non è una rifinitura: è, insieme all'esportazione, il modo in cui i numeri
arrivano a chi non entrerà mai nell'app — e il pubblico esterno si aspetta un documento **impaginato e statico**,
non un collegamento (§2.5 della [descrizione](../application-description.md), fonte 9). C'è però una trappola
specifica di questa applicazione: un rapporto che parte da solo è un numero **letto senza contesto**. Se quel
lunedì una fonte era ferma, il titolare legge un fatturato dimezzato e chiama il commercialista. Per questo la
regola degli avvisi (storia 0020) vale identica qui: **l'incompletezza si dichiara in testa**, non si nasconde.

## 2. Requisiti funzionali

1. **RF-1** — Si programma l'invio di **un cruscotto** con una cadenza fra `settimanale`, `mensile` e
   `trimestrale`, un giorno e un'ora di partenza, e il fuso orario dell'account.
2. **RF-2** — I destinatari sono indirizzi di posta elettronica: persone dell'account **e** indirizzi esterni (il
   commercialista). Un destinatario esterno riceve **soltanto** il documento allegato: nessun collegamento che
   apra l'app, nessun accesso.
3. **RF-3** — Il documento contiene i riquadri del cruscotto con il periodo, il confronto col periodo precedente,
   il blocco di provenienza della storia 0027 e — se attivo — un **riepilogo scritto** di tre righe prodotto dal
   copilota, che consuma **una** unità della metrica `questions` (storia 0026).
4. **RF-4** — Se al momento dell'invio uno dei valori è `parziale` o `non calcolabile`, il documento lo dice
   **nella prima riga** e il riquadro interessato porta il contrassegno; se **tutti** i valori sono non
   calcolabili l'invio si sospende e chi ha programmato il rapporto riceve un avviso tecnico al posto del
   rapporto.
5. **RF-5** — La classe di riservatezza si applica **al destinatario, non a chi programma**: un rapporto che
   contiene metriche economiche si può inviare solo a destinatari che l'account ha dichiarato autorizzati, e per
   gli indirizzi esterni serve una **conferma esplicita** al momento della programmazione.
6. **RF-6** — Ogni esecuzione lascia una riga nel registro degli invii con periodo coperto, esito del recapito e
   riferimento al documento prodotto; un rapporto si può **sospendere** senza cancellarlo e provare subito con un
   invio di prova al solo autore.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Rapporti ed esecuzioni filtrano per `tenant_id` preso dal gettone
  verificato; la lavorazione periodica scorre gli account uno per uno e non compone mai una interrogazione senza
  il filtro.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `POST|GET|PATCH /api/insights/v1/rapporti-programmati`,
  `POST /api/insights/v1/rapporti-programmati/{id}/prova` e `GET …/{id}/esecuzioni`; errori in
  `application/problem+json`; definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V<N>__rapporti_programmati.sql` sullo schema `app_insights`: tabelle
  `rapporto_programmato` (cruscotto, cadenza, destinatari, formato, prossima esecuzione, stato) ed
  `esecuzione_rapporto` (momento, periodo coperto, esito, riferimento al documento); `tenant_id`, chiave primaria
  UUID versione 7, colonne di controllo, cancellazione logica.
- **RT-4 — Modulo frontend (§3, §5).** Sezione `Rapporti` del modulo `insights`: programmazione, elenco degli
  invii, anteprima del documento; solo token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Interfaccia **e documento** in `en, it, fr, es, de`. La lingua del rapporto si
  sceglie alla programmazione, perché il destinatario può non parlare la lingua di chi lo programma.
- **RT-6 — Varchi e quota (§6, §7).** Il riepilogo scritto consuma una unità di `questions` (natura `flow`); a
  quota esaurita **il rapporto parte comunque, senza il riepilogo**, e lo dice — un blocco che impedisse l'invio
  del cruscotto per mancanza di domande sarebbe un varco applicato alla cosa sbagliata. Con abbonamento
  `canceled` gli invii programmati si sospendono.
- **RT-8 — Dati personali (§10).** `rapporto_programmato.destinatari` è un **contatto** e va nel manifesto in
  italiano e inglese, annotato `@PersonalData`; le tabelle `rapporto_programmato` ed `esecuzione_rapporto` sono
  in `exportData` e `purgeData`. Il documento allegato può contenere etichette di dimensione (via A del §6.1).
- **RT-10 — Nessun fornitore esterno nuovo (§10).** L'invio usa il servizio di posta **già in uso dalla
  piattaforma**: questa storia **non** introduce un responsabile esterno del trattamento (§6.5 della
  descrizione). Se servisse un fornitore diverso, è una fermata di escalation.
- **RT-14 — Registrazione eventi (§14).** «Rapporto programmato», «rapporto inviato», «invio sospeso per valori
  non calcolabili», «recapito fallito» con `tenant_id`, `app_id`, `user_id` e identificativo di correlazione;
  **mai** gli indirizzi dei destinatari né i valori.

## 4. Criteri di accettazione

**CA-1 — Il lunedì mattina arriva**
- **Dato** un rapporto settimanale programmato per il lunedì alle 07:00 su un cruscotto con quattro riquadri
- **Quando** scatta l'ora
- **Allora** i destinatari ricevono il documento con i quattro riquadri, il confronto con la settimana
  precedente, il blocco di provenienza e il riepilogo di tre righe; il registro riporta l'esito del recapito

**CA-2 — Incompletezza in prima riga**
- **Dato** una fonte richiesta silente da quattro giorni al momento dell'invio
- **Quando** il rapporto viene composto
- **Allora** la prima riga del documento dice quale fonte tace e da quando, e i riquadri interessati portano il
  contrassegno di incompletezza

**CA-3 — Niente da dire, niente da mandare**
- **Dato** un rapporto i cui riquadri sono **tutti** non calcolabili (fonti scollegate)
- **Quando** scatta l'ora
- **Allora** nessun destinatario riceve il rapporto; chi lo ha programmato riceve un avviso tecnico che spiega
  perché

**CA-4 — Quota esaurita, rapporto salvo**
- **Dato** un account che ha esaurito le domande del mese
- **Quando** parte il rapporto
- **Allora** il documento arriva **senza** il riepilogo scritto e con una riga che dice perché; il contatore non
  scende sotto zero

**CA-5 — Destinatario esterno con conferma**
- **Dato** un rapporto che contiene metriche economiche e un destinatario fuori dall'account
- **Quando** si prova a salvarlo senza la conferma esplicita
- **Allora** il salvataggio è rifiutato con un messaggio che dice che si stanno inviando importi a un indirizzo
  esterno; con la conferma il salvataggio riesce e la conferma resta tracciata con autore e momento

**CA-6 — Isolamento fra account**
- **Dato** due account con rapporti programmati
- **Quando** la lavorazione periodica gira
- **Allora** ogni destinatario riceve solo i numeri del proprio account, e una prova lo verifica su due account
  con cruscotti diversi

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno le aree toccate; l'intera suite prima del commit);
- [ ] prove di **unità** sul calcolo della prossima esecuzione (cadenza, fuso orario, cambio dell'ora legale) e
      di **integrazione** sulla composizione e sul recapito con servizio di posta simulato;
- [ ] prova di **isolamento fra account** sulla lavorazione periodica, con due account e due cruscotti;
- [ ] **prova end-to-end**: *rimando* alla storia 0034, che possiede il percorso `[J-INSIGHTS]`; voce
      `da-coprire` nel registro di copertura con motivo «invio programmato: richiede il tempo simulato»;
- [ ] **traduzioni** presenti in tutte e cinque le lingue, comprese quelle del documento;
- [ ] **manifesto dei dati** aggiornato per i destinatari, in italiano e inglese, con tabelle in `exportData` e
      `purgeData`;
- [ ] **registro delle decisioni** compilato, con «il rapporto parte anche senza riepilogo a quota esaurita» e la
      conferma esplicita per i destinatari esterni;
- [ ] contratto degli **strumenti conversazionali**: `programma_rapporto` è **scrittura con conferma umana**
      (storia 0032), perché manda messaggi a persone;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0017` | si programma l'invio di un cruscotto, che deve esistere |
| storia `0020` | la lavorazione periodica e la regola «non suonare su un numero incompleto» sono già costruite lì |
| storia `0027` | il documento riusa il blocco di provenienza dell'esportazione |

## 7. Fuori ambito

- il rapporto costruito su misura, diverso da un cruscotto esistente: si programma ciò che si guarda;
- la scelta del fornitore di posta e la sua configurazione: è di piattaforma;
- il collegamento a un cruscotto vivo per il destinatario esterno: **fuori ambito dichiarato** (§11, punto 4);
- la conservazione dei documenti inviati oltre il registro degli invii: si conserva il metadato, non il file.

## 8. Punti aperti

- **Il riepilogo scritto dentro un rapporto automatico è opportuno?** Consuma quota, costa, e soprattutto è
  l'unico pezzo del documento che non è deterministico. Raccomandazione: **sì, ma spegnibile e spento in modo
  predefinito**, così che chi lo vuole lo accenda sapendo cosa accende. Chiude: **sviluppatore**.
- **Quanti destinatari esterni per rapporto?** Nessun dato rilevato. Un tetto basso (proposta: cinque) riduce il
  rischio che l'app diventi uno strumento di spedizione. Chiude: **sviluppatore**.
- **Che cosa succede a un rapporto quando chi l'ha creato lascia l'account?** Proposta: il rapporto resta e passa
  in carico a `owner`, con avviso. È un caso che si presenta sempre e che quasi sempre nessuno ha previsto.
  Chiude: **sviluppatore**.
