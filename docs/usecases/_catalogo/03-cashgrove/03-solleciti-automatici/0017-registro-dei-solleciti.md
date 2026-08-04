# 0017 — Registro dei solleciti

**Applicazione**: 03 — CashGrove (`crediti`) · **Epica**: 03 — Solleciti automatici
**Storia**: `0017` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0014`, `0016`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che si trova al telefono con un cliente che dice «non mi avete mai scritto»
> voglio poter dire in tre secondi cosa gli è stato mandato, quando, a quale indirizzo e con che testo esatto
> così da chiudere la discussione con un fatto invece che con una impressione.

**Contesto.** Le storie precedenti mandano i messaggi; nessuna li conserva. Senza registro, l'app fa una cosa che il
titolare non può dimostrare — e in un dominio dove il passo successivo è la messa in mora, la prova dell'attività di
sollecito ha valore anche fuori dalla conversazione. È anche il posto in cui si concentrano i dati più delicati
dell'app: il testo del messaggio, il recapito e la traccia dell'inadempienza. Va quindi costruito insieme alle sue
regole di conservazione, non dopo.

## 2. Requisiti funzionali

1. **RF-1** — Ogni trasmissione riuscita produce una riga di registro con credito, debitore, canale, destinatario,
   istante, passo della sequenza, esito, identificativo presso il fornitore e **il testo effettivamente inviato**.
2. **RF-2** — Il registro è in **sola aggiunta**: non si modifica e non si cancella riga per riga dall'interfaccia.
3. **RF-3** — Il registro si consulta dalla scheda del credito (cronologia), dalla scheda del debitore (tutti i
   solleciti ricevuti) e dalla sezione *Solleciti* (elenco filtrabile per periodo, canale ed esito).
4. **RF-4** — Da ogni riga si può riaprire il testo inviato, così com'era, senza ricompilazione dal modello.
5. **RF-5** — Il registro di un credito è esportabile in un file leggibile, adatto a essere allegato a una
   corrispondenza.
6. **RF-6** — Anche gli invii **non** riusciti e quelli annullati compaiono, distinti dai riusciti: un registro che
   mostra solo i successi non è una prova, è una vetrina.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura del registro filtra per `tenant_id` preso dal token verificato;
  l'esportazione riguarda un credito dello stesso account e nessun altro.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `GET /api/crediti/v1/solleciti` (con filtri e paginazione),
  `GET /api/crediti/v1/solleciti/{id}` e `GET /api/crediti/v1/crediti/{id}/solleciti/esportazione`; errori in
  `application/problem+json`; definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione per la tabella `sollecito_inviato` sullo schema `app_crediti` con `tenant_id`,
  chiave UUID versione 7 e colonne di controllo. **Nessuna cancellazione logica riga per riga**: la cancellazione esiste
  solo per i diritti dell'interessato e per la chiusura dell'account, ed è fisica. Indice su (`tenant_id`, credito,
  istante) e (`tenant_id`, debitore, istante).
- **RT-4 — Modulo frontend (§3, §5).** Cronologia nella scheda del credito e del debitore, elenco filtrabile nella
  sezione *Solleciti*, finestra di lettura del testo inviato; solo token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Le stringhe dell'interfaccia passano dallo spazio-nomi `crediti` e sono presenti in
  `en, it, fr, es, de`; il **testo inviato** resta nella lingua in cui è stato mandato e non si traduce mai.
- **RT-6 — Varchi e quota (§6, §7).** Il registro non consuma quota. È accessibile in lettura anche al ruolo in sola
  lettura (il commercialista), perché è materiale probatorio dell'azienda.
- **RT-7 — Esposizione conversazionale (§12).** `storico_solleciti(credito) → cosa è stato mandato, quando, con che
  esito` è dichiarato qui come strumento di **lettura** e raccolto nel contratto della storia `0028`. Il risultato è
  **minimizzato**: non restituisce il corpo integrale del messaggio se non su richiesta esplicita, perché un elenco è
  una risposta, un archivio di testi no.
- **RT-8 — Dati personali (§10).** Questa tabella è il punto più sensibile dell'app: contiene destinatario e testo.
  Voci nuove nel manifesto in italiano e inglese per `sollecito_inviato.destinatario` e `sollecito_inviato.corpo`, con
  finalità «prova dell'attività di recupero» e base «legittimo interesse, difesa di un diritto»; campi annotati
  `@PersonalData`; tabella presente in `exportData` e `purgeData`. **È la tabella che si dimentica più facilmente**,
  perché sembra un registro tecnico.
- **RT-9 — Registrazione eventi (§14).** L'evento «registro consultato» non si registra (sarebbe un registro del
  registro); si registra invece «registro esportato» con `tenant_id`, `app_id`, `user_id` e identificativo di
  correlazione, senza contenuti.

## 4. Criteri di accettazione

**CA-1 — Ogni invio lascia traccia**
- **Dato** un sollecito trasmesso con successo
- **Quando** si apre la cronologia del credito
- **Allora** compare la riga con istante, canale, destinatario, passo ed esito, e da lì si apre il testo inviato

**CA-2 — Anche i fallimenti**
- **Dato** un invio respinto e uno annullato dal controllo di sospensione · **Quando** si guarda il registro · **Allora**
  compaiono entrambi, distinti dai riusciti, con la ragione

**CA-3 — Sola aggiunta**
- **Dato** una riga di registro · **Quando** si tenta di modificarla o cancellarla da qualsiasi rotta · **Allora** non
  esiste una rotta che lo consenta, e il tentativo diretto è respinto

**CA-4 — Testo immutato**
- **Dato** un modello di messaggio modificato dopo l'invio
- **Quando** si riapre il testo di un sollecito precedente
- **Allora** si legge il testo **originale**, non quello che il modello produrrebbe oggi

**CA-5 — Esportazione**
- **Dato** un credito con quattro solleciti · **Quando** si esporta il registro del credito · **Allora** si ottiene un
  file leggibile con i quattro messaggi, i loro istanti e i loro esiti

**CA-6 — Isolamento fra account**
- **Dato** due account `A` e `B` · **Quando** un utente di `A` chiede il registro forzando l'identificativo di un
  sollecito di `B` · **Allora** riceve l'errore di risorsa non trovata

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (backend, frontend, compliance);
- [ ] prove di **unità** sulla immutabilità del testo e di **integrazione** sul registro con database effimero;
- [ ] prova di **isolamento fra account** su lettura ed esportazione;
- [ ] **prova end-to-end**: *coprire ora* — «dopo l'invio, il registro mostra la riga» è un passo del percorso
      `[J-CREDITI]`; voce registrata nel registro di copertura con proprietaria la storia `0031`;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese con destinatario e corpo, campi annotati, tabella presente
      in esportazione e cancellazione;
- [ ] **registro delle decisioni** compilato, in particolare sulla conservazione del testo inviato e sulla sua durata;
- [ ] contratto degli **strumenti conversazionali**: `storico_solleciti` dichiarato come lettura minimizzata;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0014` | Il registro nasce dalle trasmissioni |
| storia `0016` | Anche gli invii annullati devono comparire |

## 7. Fuori ambito

- La cancellazione selettiva delle righe su richiesta del debitore: è il conflitto fra diritto alla cancellazione e
  conservazione della prova, punto aperto n. 4 del documento capofila §11, che si affronta nella storia `0030`.
- L'archiviazione a lungo termine fuori dal database: non serve ai volumi del segmento.

## 8. Punti aperti

**Per quanto tempo si conserva il testo inviato.** La proposta del documento capofila §6 è dieci anni, per allinearsi
alla conservazione dei documenti commerciali, ma **non è fondata su una fonte** e va confermata dallo sviluppatore, se
possibile in sede di revisione legale. Da questa scelta dipende anche il comportamento della cancellazione (storia
`0030`).
