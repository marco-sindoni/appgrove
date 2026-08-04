# 0016 — Taratura per attività

**Applicazione**: 33 — RenewGrove (`fidelizzazione`) · **Epica**: 03 — Punteggio di rischio spiegabile e contestabile
**Storia**: `0016` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0012`, `0013`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare di un'officina, dove un cliente che salta un tagliando non è un cliente perso
> voglio cambiare quanto pesano i singoli fatti e dove passano le fasce, vedendo **prima** che cosa cambierebbe
> così da adattare l'app al mio mestiere invece di adattare il mio mestiere all'app.

**Contesto.** È la storia che rende sopportabile l'ammissione più scomoda della
[descrizione](../application-description.md), quella del §2.7: **non esistono pesi validati per imprese che non
vendono software**. Tutte le fonti trovate sull'anticipazione dell'abbandono parlano di aziende di software con
dati di utilizzo del prodotto, che il nostro cliente non ha. I pesi con cui l'app parte sono quindi una
convenzione dichiarata, e un prodotto che non permettesse di correggerli chiederebbe al cliente di fidarsi di un
numero che noi stessi diciamo non validato. Le fonti aggiungono che **la soglia non è universale**: lo stesso
valore è sano per un cliente maturo e allarmante per uno nuovo (§2.5).

Il vincolo di forma viene dalla storia `0012` e non si tocca: un modello `vivo` non si modifica. Una taratura
**crea una versione nuova**, i punteggi già calcolati continuano a citare la versione con cui sono stati fatti, e
la serie storica resta intatta. È la stessa ragione per cui la serie è in sola aggiunta: se cambiare un peso
riscrivesse il passato, l'epica 05 misurerebbe un passato che non è mai esistito.

## 2. Requisiti funzionali

1. **RF-1** — Un utente con ruolo `owner` o `admin` può aprire una **bozza di versione** del modello, partendo da
   quella viva, e modificarne i **pesi**, i **versi**, le **finestre di osservazione** delle voci e le **soglie
   delle tre fasce**.
2. **RF-2** — Prima di applicare, la bozza mostra l'**anteprima dell'effetto** sui rapporti sorvegliati oggi:
   **quanti** cambierebbero fascia, **in quale direzione**, e **quali** — con l'elenco dei rapporti che salgono a
   `a rischio` e di quelli che scendono. L'anteprima si calcola sui segnali reali dell'account e **non scrive
   nulla**.
3. **RF-3** — Applicare la bozza la rende `vivo` e **archivia** la versione precedente. Da quel momento i calcoli
   nuovi usano la versione nuova; **i punteggi già calcolati non vengono riscritti** e continuano a citare la
   versione con cui sono stati prodotti.
4. **RF-4** — Una bozza si può **abbandonare** senza conseguenze, e più bozze non possono esistere insieme per lo
   stesso account: una taratura per volta, altrimenti nessuno sa più che cosa sta per applicare.
5. **RF-5** — La schermata di taratura **ripete l'avvertenza** che i valori di partenza sono una convenzione e non
   una stima, e rimanda al rendiconto di efficacia (`0027`) come unico modo serio per sapere se una taratura ha
   migliorato qualcosa. Non si nasconde l'incertezza: si dice dove si scioglie.
6. **RF-6** — Un utente con ruolo `member` **vede** la taratura e l'anteprima in sola lettura e riceve `403` se
   tenta di applicare: chi lavora sui rapporti deve poter capire perché il punteggio è cambiato, senza poterlo
   cambiare da solo.
7. **RF-7** — L'elenco delle versioni è consultabile: numero, chi l'ha applicata, quando, e che cosa è cambiato
   rispetto alla precedente. Una versione archiviata resta leggibile per sempre, perché i punteggi la citano.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura e scrittura di `modello_di_punteggio`, delle sue voci e
  dell'anteprima filtra per `tenant_id` preso dal token di accesso verificato; un `tenant_id` che arrivasse dal
  corpo della richiesta o dai parametri viene ignorato. L'anteprima gira **solo** sui rapporti dell'account
  richiedente.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `POST /api/fidelizzazione/v1/modelli/bozza` (apre la bozza
  dalla versione viva), `PUT /api/fidelizzazione/v1/modelli/{id}` (modifica la sola bozza),
  `POST /api/fidelizzazione/v1/modelli/{id}/anteprima` (calcola e restituisce l'effetto, senza scrivere),
  `POST /api/fidelizzazione/v1/modelli/{id}/applicazione`, `DELETE /api/fidelizzazione/v1/modelli/{id}`
  (abbandona la bozza). Corpo validato con gli stessi vincoli dichiarativi della storia `0012` (intervallo dei
  pesi, finestra fra 7 e 365 giorni, tipo di segnale nell'elenco chiuso, soglie ordinate e non sovrapposte);
  errori in `application/problem+json`; definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V13__taratura_modello.sql` sullo schema `app_fidelizzazione`: colonna
  `derivato_da` su `modello_di_punteggio` (quale versione ha generato questa) e colonne su chi e quando ha
  applicato. Il vincolo di unicità «un solo modello `vivo` per account» della storia `0012` resta, e se ne
  aggiunge uno gemello: **al più una bozza per account**. Applicazione e archiviazione della versione precedente
  avvengono nella **stessa transazione**: non deve esistere un istante senza modello vivo. Nessuna chiave esterna
  verso altri schemi.
- **RT-4 — Modulo frontend (§3, §5).** Sezione `Modello` del modulo `fidelizzazione`, ora modificabile: voci con
  peso, verso e finestra; cursori delle tre soglie; pannello dell'anteprima con il conteggio dei cambi di fascia e
  l'elenco dei rapporti interessati; storico delle versioni. Dati letti e scritti con il client generato; solo
  token del sistema di design; funziona in tema chiaro e scuro; il numero di rapporti che salgono in `a rischio`
  è leggibile senza distinguere i colori.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe — nomi delle voci, testi dell'anteprima («12 rapporti
  salirebbero in fascia a rischio»), avvertenza sulla natura convenzionale dei pesi, conferma di applicazione —
  passano dallo spazio-nomi `fidelizzazione` e sono presenti in `en, it, fr, es, de`.
- **RT-6 — Varchi e quota (§6, §7).** Catena dei varchi completa: `401`, `403` ad app spenta, `402` ad account non
  abilitato o abbonamento `canceled`, `403` a ruolo insufficiente — qui aprire, modificare e applicare richiedono
  `owner` o `admin`; `member` legge. **Nessun consumo di quota**: la taratura non aggiunge rapporti sorvegliati e
  la metrica `rapporti_sorvegliati` (natura `stock`) resta invariata. L'anteprima, però, **è un calcolo su tutti i
  rapporti dell'account**: va limitata in frequenza per non diventare una leva di carico (vedi punti aperti).
- **RT-7 — Esposizione conversazionale (§12).** **Nessuno strumento nuovo**, e la scelta è motivata: la tabella
  degli strumenti (§7 della descrizione) non prevede la taratura, perché cambiare i pesi del giudizio è
  un'operazione che va guardata con l'anteprima davanti, non dettata a voce. La versione del modello resta però
  presente nei risultati di `spiega_punteggio` (`0014`), così che dalla chat si capisca **con quale taratura** un
  punteggio è stato prodotto. Dipendenza dichiarata: livello conversazionale di piattaforma, **non ancora
  implementato** (UC 0061-0063).
- **RT-8 — Dati personali (§10).** **Nessun dato personale nuovo.** Il modello e le sue voci contengono regole e
  numeri e restano fuori da `exportData` e `purgeData`, come già stabilito dalla storia `0012`. Attenzione a un
  punto: l'**anteprima** elenca rapporti, quindi mostra a schermo dati riferiti a persone — ma non ne conserva
  alcuno, perché non scrive nulla. Nel registro delle decisioni va scritto esplicitamente che l'anteprima è
  **volatile**: se un giorno si volesse conservarla, servirebbe una voce nuova nel manifesto.
- **RT-9 — Registrazione eventi (§14).** `bozza di modello aperta`, `bozza abbandonata`,
  `modello applicato (versione precedente, versione nuova, numero di rapporti che cambiano fascia)`,
  `anteprima calcolata`, con `tenant_id`, `app_id`, `user_id` e identificativo di correlazione; **mai l'elenco dei
  rapporti** e mai le loro etichette.

## 4. Criteri di accettazione

**CA-1 — Vedere l'effetto prima di applicare**
- **Dato** un account con 180 rapporti sorvegliati e il modello vivo alla versione 2
- **Quando** un utente `owner` apre una bozza, alza il peso del tipo di segnale «rata non rientrata» e chiede
  l'anteprima
- **Allora** vede «14 rapporti salirebbero in fascia a rischio, 3 scenderebbero in attenzione», con i due elenchi
  apribili, e **nessuna riga di punteggio è stata scritta**

**CA-2 — Applicare crea una versione, non modifica quella viva**
- **Dato** una bozza pronta, derivata dalla versione 2
- **Quando** l'utente `owner` la applica
- **Allora** esiste una versione 3 in stato `vivo`, la versione 2 è `archiviata` e resta leggibile, e i punteggi
  già calcolati continuano a citare la versione 2 con i valori di allora

**CA-3 — Una bozza per volta**
- **Dato** un account con una bozza già aperta
- **Quando** un altro utente `admin` tenta di aprirne una seconda
- **Allora** riceve `409` in `problem+json` con il rimedio indicato (applicare o abbandonare quella esistente, e
  chi l'ha aperta), e nessuna seconda bozza viene creata

**CA-4 — Soglie incoerenti respinte**
- **Dato** una bozza in modifica
- **Quando** l'utente imposta la soglia di `attenzione` sopra quella di `a rischio`
- **Allora** riceve `400` con l'indicazione del vincolo violato, e la bozza resta con i valori precedenti

**CA-5 — Un `member` non applica**
- **Dato** un utente con ruolo `member`
- **Quando** apre la sezione Modello e tenta di applicare una bozza
- **Allora** vede voci, soglie e anteprima in sola lettura e riceve `403` sul tentativo di applicazione

**CA-6 — Isolamento fra account**
- **Dato** due account `A` e `B`, ciascuno con il proprio modello e i propri rapporti
- **Quando** un utente di `A` chiede l'anteprima forzando nella richiesta l'identificativo del modello di `B`
- **Allora** riceve `404`, e nessun rapporto di `B` compare nel risultato

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend`, `frontend`; l'intera suite prima del commit);
- [ ] prove di **unità** sul calcolo dell'anteprima (stessi segnali, pesi diversi, cambi di fascia attesi) e sulla
      validazione delle soglie; prove di **integrazione** sull'applicazione, con database effimero e migrazioni
      vere, che verificano l'atomicità della transizione e che **nessun punteggio storico** sia stato toccato;
- [ ] prova di **isolamento fra account** sulle rotte della taratura e dell'anteprima;
- [ ] prova sulla **matrice dei ruoli**: `owner` e `admin` applicano, `member` no;
- [ ] **prova end-to-end**: *rimandare* — il percorso `[J-FIDELIZZAZIONE]` nasce nella storia `0030` e dovrà
      coprire il tratto «cambio un peso → l'anteprima annuncia N cambi di fascia → applico → il punteggio nuovo
      cita la versione nuova»; voce `da-coprire` con motivo e storia proprietaria `0030` nel registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml);
- [ ] **traduzioni** presenti in tutte e cinque le lingue (`en, it, fr, es, de`);
- [ ] **manifesto dei dati**: nessuna voce nuova, con la nota scritta che l'anteprima è volatile e non conserva
      nulla;
- [ ] **registro delle decisioni** compilato con: perché una taratura crea una versione invece di modificare,
      perché una bozza per volta, perché l'anteprima non scrive, perché la taratura resta fuori dal livello
      conversazionale;
- [ ] contratto degli **strumenti conversazionali**: nessuno nuovo, con la motivazione scritta;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0012` — modello del punteggio | esistono le versioni, gli stati e il vincolo «il modello vivo non si modifica»: qui si costruisce il modo legittimo di cambiarlo |
| storia `0013` — calcolo e storico | l'anteprima riusa il motore di calcolo con pesi diversi, e l'applicazione non deve toccare la serie già scritta |
| storia `0027` — rendiconto dell'efficacia (epica 05, non ancora scritta) | è l'unico modo serio per sapere se una taratura ha migliorato qualcosa; finché non c'è, la schermata lo dichiara invece di fingere una misura |
| epica di piattaforma non implementata, UC 0061-0063 | nessuno strumento conversazionale qui, ma la versione del modello va esposta da `spiega_punteggio` |

## 7. Fuori ambito

- **la taratura automatica dei pesi** a partire dagli esiti misurati: fuori, e non per pigrizia. Un modello che si
  ritara da solo è un modello addestrato con un altro nome, e perderebbe la proprietà per cui l'epica 03 esiste
  (§6 della descrizione). Se mai si farà, sarà una proposta di modifica **da approvare a mano**, non un
  aggiustamento silenzioso;
- **modelli diversi per gruppi di rapporti** (per esempio un modello per i clienti nuovi e uno per i maturi):
  deliberatamente rimandato, perché moltiplicherebbe le versioni da citare e da spiegare. L'anzianità del rapporto
  resta un elemento che si legge **accanto** alla fascia (§2.5), non un secondo modello;
- **il ricalcolo retroattivo** della serie con la versione nuova: escluso per scelta, è la riscrittura all'indietro
  vietata dalla storia `0013`;
- **modelli predefiniti per settore** («studio di consulenza», «manutentore»): idea buona e prematura, perché non
  ci sono dati per distinguerli. Torna quando il rendiconto (`0027`) avrà misurato qualcosa.

## 8. Punti aperti

- **Quanto spesso si può chiedere un'anteprima.** È un calcolo su tutti i rapporti dell'account e, sul piano
  `portafoglio` (fino a 1.200 rapporti), non è gratuito. **Raccomandazione**: limite di frequenza per account con
  risposta `429` e messaggio esplicito, e anteprima calcolata sulla fotografia dei segnali del giorno invece che
  in tempo reale. Chiude: **sviluppatore**.
- **Se una taratura debba poter essere annullata dopo l'applicazione.** Tecnicamente basta riapplicare la versione
  archiviata come nuova versione; resta da decidere se offrirlo come gesto di un clic («torna alla versione 2»)
  con l'anteprima davanti. **Raccomandazione**: sì, ma sempre come **versione nuova derivata**, mai come ritorno
  che riscrive la storia delle versioni. Chiude: **sviluppatore**.
