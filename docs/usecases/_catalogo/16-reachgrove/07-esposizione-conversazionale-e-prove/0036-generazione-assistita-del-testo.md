# 0036 — Generazione assistita del testo

**Applicazione**: 16 — ReachGrove (`campaigns`) · **Epica**: 07 — Esposizione conversazionale e prove end-to-end
**Storia**: `0036` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0014`, `0035`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che davanti al foglio bianco si blocca
> voglio poter chiedere alla chat una bozza di testo per la prossima campagna, e poi sistemarla io
> così da partire da qualcosa invece che da niente, senza che quel testo venga salvato o mandato senza che io lo
> abbia letto.

**Contesto.** La descrizione dell'applicazione dichiara il rischio di sostituzione da parte dei modelli
linguistici come `misto` e dice quale metà è minacciata: **scrivere il testo di una campagna è esattamente ciò che
un assistente generico fa bene** ([application-description.md](../application-description.md) §1). Questa storia
lo riconosce invece di negarlo. La generazione del testo è una **funzione della chat**, non un modulo che
proviamo a vendere: il valore dell'app sta nel flusso di lavoro, nella prova del consenso e nel recapito, e il
testo è la parte che si regala. Ne discende la forma dello strumento: produce una bozza e non salva niente.

## 2. Requisiti funzionali

1. **RF-1** — Il servizio dichiara lo strumento `genera_testo(obiettivo, tono, lingua, riferimenti?)`, marcato
   **scrittura**, che restituisce una bozza di oggetto e di corpo e **non scrive nulla**: nessuna campagna, nessun
   modello, nessuna riga in archivio.
2. **RF-2** — Salvare la bozza — come campagna in stato `bozza` o come modello riusabile — è un secondo atto, che
   passa da `crea_bozza_di_campagna` (storia 0035) e richiede la conferma di una persona.
3. **RF-3** — Nella richiesta di generazione **non finisce nessun dato personale degli iscritti**: non i recapiti,
   non i nomi, non i campi personalizzati. I `riferimenti` ammessi sono materiale che il cliente fornisce
   volontariamente — una descrizione del prodotto, un testo precedente — e il campo lo dichiara.
4. **RF-4** — La bozza generata è marcata come tale finché una persona non la modifica o la approva, e
   l'interfaccia lo mostra: chi apre la campagna deve sapere che quel testo l'ha scritto un assistente.
5. **RF-5** — Il testo generato porta comunque le parti obbligatorie del messaggio — collegamento di disiscrizione
   e identificazione del mittente — che non si possono togliere: se il modello non le include, le aggiunge la
   composizione (storia 0014), non l'assistente.
6. **RF-6** — La generazione avviene nella lingua richiesta fra quelle dell'interfaccia, e la bozza dichiara in
   quale lingua è stata prodotta: un messaggio commerciale nella lingua sbagliata è un messaggio buttato.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Lo strumento opera nell'account del chiamante autenticato; i
  `riferimenti` che rimandano a contenuti dell'account (un modello, una campagna precedente) sono risolti
  filtrando per `tenant_id` preso dal token verificato.
- **RT-2 — Interfaccia di programmazione (§2).** Contratto nel pacchetto `app.appgrove.campaigns.tools`,
  versionato con il servizio. **Il servizio non chiama un modello linguistico**: la generazione avviene nel
  livello conversazionale, e questa storia dichiara la forma della richiesta e del risultato. Se in futuro
  servisse una generazione dentro l'app, sarebbe un fornitore esterno nuovo e una decisione da prendere a parte
  (§7).
- **RT-3 — Persistenza (§8).** Nessuna tabella nuova: una bozza non salvata non esiste in archivio. Si aggiunge la
  colonna che marca l'origine assistita del testo sulla campagna e sul modello, con la relativa migrazione sullo
  schema `app_campaigns`.
- **RT-4 — Modulo frontend (§3, §5).** Nella schermata di composizione del modulo `campaigns` compare la
  marcatura «testo proposto dall'assistente», che sparisce quando una persona modifica o approva il testo; solo
  token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** La marcatura e i testi dell'interfaccia sono presenti in `en, it, fr, es, de`.
  Il **contenuto generato** è nella lingua chiesta dall'utente e non è interfaccia: non passa dallo spazio-nomi
  delle traduzioni.
- **RT-6 — Varchi e quota (§6, §7).** Generare un testo **non** consuma la metrica `messages_sent` (natura
  `flow`): non parte nessun messaggio. Attraversa comunque la catena dei varchi (`401`, `403`, `402`). Se in
  futuro la generazione avesse un costo variabile, servirebbe una metrica sua: non si nasconde un costo dentro una
  quota che misura un'altra cosa.
- **RT-7 — Esposizione conversazionale (§12).** È lo strumento più naturale della chat e insieme il più
  innocuo, perché non produce effetti: è marcato **scrittura** solo perché il suo esito è destinato a diventare
  contenuto, e il salvataggio richiede conferma (RF-2). Livello conversazionale non ancora implementato
  (UC 0061-0063).
- **RT-8 — Dati personali (§10).** Nessun dato personale nuovo, e un divieto esplicito: RF-3 impedisce che
  recapiti, nomi e campi personalizzati degli iscritti entrino nella richiesta di generazione. Il divieto va
  scritto nella descrizione del manifesto `docs/compliance/manifests/campaigns.yaml`, perché è una misura di
  protezione dei dati e non una preferenza di stile.
- **RT-9 — Registrazione eventi (§14).** «Testo generato» registrato con `tenant_id`, `app_id`, `user_id`,
  lingua, identificativo di correlazione e lunghezza del risultato; **mai** il testo, che è contenuto del cliente,
  né i riferimenti forniti.

## 4. Criteri di accettazione

**CA-1 — Genera e non salva**
- **Dato** una chiamata a `genera_testo` con obiettivo, tono e lingua
- **Quando** risponde
- **Allora** restituisce oggetto e corpo proposti, e in archivio non compare alcuna campagna né alcun modello
  nuovo

**CA-2 — Il salvataggio è un atto separato**
- **Dato** una bozza generata
- **Quando** si chiede di salvarla come campagna
- **Allora** l'operazione passa da `crea_bozza_di_campagna` e richiede la conferma di una persona

**CA-3 — Nessun dato di iscritto nella richiesta**
- **Dato** un tentativo di passare come riferimento l'elenco degli iscritti di un segmento
- **Quando** si chiama lo strumento
- **Allora** la richiesta è rifiutata con la spiegazione, e nessun recapito viene trasmesso

**CA-4 — La bozza è riconoscibile**
- **Dato** una campagna il cui testo viene da una generazione
- **Quando** un'altra persona la apre nell'interfaccia
- **Allora** vede la marcatura «testo proposto dall'assistente», che sparisce dopo una modifica o un'approvazione

**CA-5 — Le parti obbligatorie ci sono comunque**
- **Dato** un testo generato che non contiene il collegamento di disiscrizione
- **Quando** lo si salva come campagna
- **Allora** la composizione lo aggiunge: nessuna campagna può esistere senza

**CA-6 — Isolamento fra account**
- **Dato** due account `A` e `B`
- **Quando** un utente di `A` passa come riferimento una campagna di `B`
- **Allora** il riferimento non viene risolto e la generazione procede senza, oppure viene rifiutata: in nessun
  caso il contenuto di `B` entra nella richiesta

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (`backend`, `frontend`; l'intera suite prima del commit);
- [ ] prove di **unità** sul rifiuto dei riferimenti che contengono dati di iscritti e di **integrazione** sullo
      strumento e sulla marcatura di origine;
- [ ] prova di **isolamento fra account** sulla risoluzione dei riferimenti;
- [ ] **prova end-to-end**: rimando — la generazione richiede il livello conversazionale, che non esiste
      (UC 0061-0063); voce `da-coprire` nel registro di copertura con motivo e storia proprietaria `0036`,
      coperta nel frattempo da prove d'integrazione sul contratto;
- [ ] **traduzioni** della marcatura presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati**: nessuna voce nuova; divieto di RF-3 scritto nella descrizione;
- [ ] **registro delle decisioni** compilato, con annotato che la generazione del testo è una funzione della chat
      e non un modulo dell'app, e perché;
- [ ] contratto degli **strumenti conversazionali**: `genera_testo` dichiarato come scrittura senza effetti, con
      il salvataggio separato;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storia `0014` | La bozza diventa un messaggio solo passando dalla composizione, che aggiunge le parti obbligatorie |
| Storia `0035` | Il salvataggio passa da `crea_bozza_di_campagna` e dal suo ciclo di conferma |
| UC 0061-0063 (livello conversazionale) | La generazione avviene lì: qui si dichiara la forma della richiesta |

## 7. Fuori ambito

- la chiamata a un modello linguistico dentro il servizio: sarebbe un fornitore esterno nuovo che tratta contenuti
  del cliente, e una decisione da prendere a parte;
- la traduzione automatica di una campagna nelle lingue degli iscritti: rimandata, perché un messaggio commerciale
  tradotto male costa più di uno non tradotto;
- la generazione di immagini: fuori perimetro.

## 8. Punti aperti

- **Dove avviene materialmente la generazione e con quale fornitore.** Se il livello conversazionale si appoggerà
  a un fornitore fuori dall'Unione europea, i contenuti del cliente vi transiteranno: è una domanda di piattaforma
  (UC 0061-0062) e non di questa app, ma va posta prima che lo strumento sia richiamabile davvero.
