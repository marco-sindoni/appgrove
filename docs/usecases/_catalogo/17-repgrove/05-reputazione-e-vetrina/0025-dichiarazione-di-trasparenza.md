# 0025 — Dichiarazione di trasparenza

**Applicazione**: 17 — RepGrove (`recensioni`) · **Epica**: 05 — Reputazione e vetrina
**Storia**: `0025` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0012`, `0024`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che mostra le recensioni sul proprio sito
> voglio che l'app scriva da sola la dichiarazione su **se e come** le recensioni sono verificate, con le parole
> giuste e nella mia lingua
> così da assolvere un obbligo europeo che non sapevo di avere, senza pagare un legale per due paragrafi.

**Contesto.** La direttiva europea 2019/2161 («omnibus»), recepita in Italia con il decreto legislativo 26/2023,
impone a chi dà accesso a recensioni dei consumatori di dichiarare **se e come** verifica che provengano da
consumatori che hanno effettivamente usato il servizio (descrizione §2.3, punto 4). Non c'è obbligo di verificare:
c'è obbligo di **dire la verità su cosa si fa**, e la sanzione per la pratica commerciale scorretta arriva al 4 %
del fatturato annuo. L'obbligo cade su chi **mostra** le recensioni — cioè sul nostro cliente nel momento in cui
incolla il riquadro nel proprio sito (storia 0024), non su di noi.

È il momento giusto per farla adesso perché il riquadro esiste da una storia e **non si pubblica senza**: il
vincolo è già scritto in 0024 (RF-4) e qui viene onorato. Ed è una funzione che vale come argomento di vendita: i
prodotti nordamericani della categoria non hanno motivo di conoscere questo adempimento (descrizione §2.1), e il
cliente non sa nemmeno che gli serve finché non gliene arriva la contestazione.

Il punto che rende la storia non banale: la dichiarazione **non è un testo di modello da copiare**, è la
descrizione di come l'app è configurata *davvero* per quella sede. Se la regola di equità è «tutti», la
dichiarazione dice una cosa; se è «uno ogni tre», ne dice un'altra. Una dichiarazione che non corrisponde alla
configurazione è peggio di nessuna dichiarazione, perché è essa stessa una dichiarazione ingannevole.

## 2. Requisiti funzionali

1. **RF-1** — Per ogni sede l'app **genera** il testo della dichiarazione a partire dalla configurazione reale:
   piattaforme collegate e loro modo di verifica, forma della regola di equità in vigore (`tutti` oppure
   `uno_ogni_n` con il valore di `n`), fatto che l'invito parta solo a fronte di un servizio erogato registrato,
   assenza di incentivi, assenza di qualunque filtro per voto nel riquadro, criterio di ordinamento e numero di
   recensioni mostrate, trattamento delle recensioni non conformi (storia 0021).
2. **RF-2** — Il testo è disponibile nelle cinque lingue dell'interfaccia e il riquadro mostra quella del
   visitatore; la lingua mancante non è ammessa, perché una dichiarazione assente vale come dichiarazione omessa.
3. **RF-3** — La dichiarazione è **versionata e ad accrescimento**: ogni generazione produce una versione nuova con
   il momento, la configurazione da cui è nata e chi l'ha accettata. Le versioni precedenti restano, perché servono
   a dimostrare cosa era esposto in un dato giorno.
4. **RF-4** — La dichiarazione entra in vigore solo quando una persona con ruolo `admin` o `owner` la **accetta**,
   dopo averla letta: è il cliente che la pubblica a proprio nome, non noi al posto suo.
5. **RF-5** — Quando cambia una delle configurazioni da cui il testo dipende, la dichiarazione in vigore è marcata
   **da riconfermare**, l'app lo segnala nella schermata e nel rapporto periodico, e dopo un periodo di tolleranza
   dichiarato il riquadro serve la versione nuova solo se accettata — altrimenti si comporta come se la
   dichiarazione mancasse (storia 0024, RF-4).
6. **RF-6** — Il cliente può **aggiungere** un paragrafo proprio (per esempio le recensioni che raccoglie fuori
   dall'app), non può **modificare né rimuovere** le frasi generate: il testo generato descrive fatti che l'app
   conosce, e riscriverli sarebbe esattamente il modo di renderlo falso.
7. **RF-7** — La dichiarazione si scarica in un file leggibile, con la sua versione e la sua data, per essere
   allegata alle condizioni del sito del cliente o mostrata a chi contesta.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Generazione, accettazione e lettura della dichiarazione filtrano per
  `tenant_id` preso dal token verificato; la rotta pubblica del riquadro risolve la dichiarazione **dalla chiave
  pubblica**, mai da un identificativo passato dal chiamante.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `POST /api/recensioni/v1/sedi/{id}/dichiarazione` (genera
  una versione nuova), `POST …/dichiarazione/{versione}/accetta`, `GET …/dichiarazione` e
  `GET /api/recensioni/v1/pubblico/riquadro/{chiave}/dichiarazione` (non autenticata). Corpo validato, errori in
  `application/problem+json` con codice distinto per «nessuna versione accettata»; definizione OpenAPI aggiornata
  nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V<N>__dichiarazione_trasparenza.sql` sullo schema `app_recensioni`:
  tabella `dichiarazione_trasparenza` con `tenant_id`, sede, versione, testo per lingua, **istantanea della
  configurazione** da cui è nata, stato (`bozza`, `in_vigore`, `da_riconfermare`, `superata`), chi ha accettato e
  quando; chiave primaria a identificativo universale versione 7, colonne di controllo, `deleted_at`. Tabella **ad
  accrescimento**: il testo di una versione non si modifica mai. Il riferimento alla versione in vigore vive su
  `riquadro_pubblico`.
- **RT-4 — Modulo frontend (§3, §5).** Sezione *Impostazioni* → «Riquadro per il sito», scheda «Trasparenza»: testo
  generato con evidenza di cosa deriva da quale impostazione, campo per il paragrafo aggiuntivo, pulsante di
  accettazione, storia delle versioni, avviso ben visibile quando la dichiarazione è `da_riconfermare`. Solo token
  del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Sia le stringhe dell'interfaccia sia **il testo generato** esistono in
  `en, it, fr, es, de` sotto lo spazio-nomi `recensioni`. Il testo generato è composto da frammenti tradotti, non
  tradotto a macchina al volo: una dichiarazione legale non si improvvisa a runtime.
- **RT-6 — Varchi e quota (§6, §7).** Generazione e accettazione richiedono ruolo `admin` o `owner`; nessun consumo
  di quota. Con abbonamento `canceled` la rotta pubblica non serve la dichiarazione perché non serve più il
  riquadro (storia 0024, RT-6).
- **RT-7 — Esposizione conversazionale (§12).** Strumento di **sola lettura**
  `dichiarazione_trasparenza(sede) → testo in vigore, versione, stato` (storia 0027). La generazione e soprattutto
  l'**accettazione** non sono strumenti: accettare è un atto del cliente con effetti verso l'esterno, e un
  assistente non lo compie al posto suo.
- **RT-8 — Dati personali (§10).** Nessun dato personale nuovo: la dichiarazione parla della configurazione, non
  delle persone. Il campo «chi ha accettato» è un utente della piattaforma, già coperto dal contratto dati comune.
- **RT-9 — Registrazione eventi (§14).** `dichiarazione generata`, `dichiarazione accettata`,
  `dichiarazione da riconfermare` (con quale impostazione è cambiata), con `tenant_id`, `app_id`, `user_id` e
  identificativo di correlazione. Nessun testo nei registri.

## 4. Criteri di accettazione

**CA-1 — Il testo descrive la configurazione vera**
- **Dato** una sede collegata al solo profilo Google, con regola di equità `uno_ogni_n` con `n = 3`
- **Quando** si genera la dichiarazione
- **Allora** il testo dice che si invita un cliente ogni tre secondo un criterio indipendente dalla soddisfazione,
  che l'invito parte solo a fronte di un servizio registrato e che non vengono offerti incentivi, e nomina la sola
  piattaforma collegata

**CA-2 — Senza accettazione non è in vigore**
- **Dato** una versione generata e non accettata
- **Quando** si carica il riquadro pubblico
- **Allora** il riquadro si comporta come se la dichiarazione mancasse e non mostra recensioni

**CA-3 — Il cambio di configurazione la invalida**
- **Dato** una dichiarazione in vigore con regola `tutti`
- **Quando** il cliente registra una regola nuova `uno_ogni_n`
- **Allora** la dichiarazione passa a `da_riconfermare`, l'app lo segnala e propone il testo nuovo, evidenziando
  la frase cambiata

**CA-4 — Il testo generato non si può riscrivere**
- **Dato** la scheda «Trasparenza»
- **Quando** si tenta di modificare una frase generata, anche chiamando la rotta a mano
- **Allora** l'operazione è rifiutata; è ammesso solo il paragrafo aggiuntivo, che compare separato

**CA-5 — Cinque lingue**
- **Dato** una dichiarazione accettata
- **Quando** un visitatore con browser in tedesco apre il riquadro
- **Allora** legge la dichiarazione in tedesco, con lo stesso contenuto della versione italiana

**CA-6 — Isolamento fra account**
- **Dato** due account con una sede ciascuno
- **Quando** un utente di `A` chiede la dichiarazione della sede di `B`, forzandone l'identificativo
- **Allora** riceve `404` e nessun testo

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno le aree toccate; l'intera suite prima del commit);
- [ ] prove di **unità** sulla composizione del testo a partire dalla configurazione (una prova per ciascuna
      combinazione che cambia una frase) e sul passaggio a `da_riconfermare`; di **integrazione** sulle rotte con
      database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** sulla dichiarazione, compresa la rotta pubblica per chiave;
- [ ] **prova end-to-end**: *coprire ora* il passo «accetto la dichiarazione e il riquadro comincia a servire» nel
      percorso `[J-RECENSIONI]` (storia 0030), e registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) aggiornato;
- [ ] **traduzioni** presenti in tutte e cinque le lingue, per l'interfaccia **e** per i frammenti del testo
      generato;
- [ ] **manifesto dei dati**: nessuna voce nuova, e lo si dichiara nel registro delle decisioni invece di tacerlo;
- [ ] **registro delle decisioni** compilato, con la scelta di rendere il testo generato non modificabile e con il
      periodo di tolleranza dopo un cambio di configurazione;
- [ ] contratto degli **strumenti conversazionali**: `dichiarazione_trasparenza` di sola lettura; l'accettazione
      esclusa deliberatamente e annotata;
- [ ] documentazione: la scheda di vendita dell'app dice che questo adempimento è compreso.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0012` | la forma della regola di equità in vigore è il contenuto principale della dichiarazione |
| storia `0024` | il riquadro è il luogo dove la dichiarazione va mostrata, ed è ciò che la rende obbligatoria |
| storie `0007`, `0008` | l'elenco delle piattaforme collegate e il loro modo di verifica entrano nel testo |

## 7. Fuori ambito

- la revisione legale del testo generato: è necessaria prima del rilascio ed è un compito umano — qui si prepara
  il materiale, non si valida (descrizione §11);
- la dichiarazione per recensioni raccolte fuori dall'app (per esempio su carta): il cliente la aggiunge nel
  paragrafo proprio, l'app non può descrivere ciò che non vede;
- l'informativa sui dati personali del cliente finale, che è un documento diverso e sta alla piattaforma;
- le lingue oltre le cinque dell'interfaccia: un visitatore in portoghese vede la versione inglese.

## 8. Punti aperti

- **Il testo esatto va rivisto da un legale** prima del go-live: qui si definisce da quali fatti il testo discende
  e come si tiene aggiornato, non la sua formulazione definitiva. Va nel registro della revisione legale
  pre-go-live.
- **Il periodo di tolleranza dopo un cambio di configurazione** (proposta: sette giorni) è una decisione di
  prodotto: azzerarlo spegnerebbe il riquadro del cliente per una modifica innocua, allungarlo lascerebbe in
  pagina una dichiarazione non più vera. **Decide lo sviluppatore.**
- **Se la dichiarazione debba comparire anche dentro l'invito** mandato al cliente finale, e non solo nel riquadro,
  non l'ho trovato risolto nelle fonti: dipende da come si qualifica l'invito (descrizione §11.1).
