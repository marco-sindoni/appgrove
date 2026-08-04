# 0017 — Il punteggio non decide da solo

**Applicazione**: 33 — RenewGrove (`fidelizzazione`) · **Epica**: 03 — Punteggio di rischio spiegabile e contestabile
**Storia**: `0017` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0014`, `0015`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che mette il nome della propria attività sotto ogni parola che arriva ai suoi clienti
> voglio la certezza — non la promessa — che nessun numero calcolato da questa app possa far partire qualcosa
> così da poter usare il punteggio senza temere che un giorno scriva a un cliente al posto mio.

**Contesto.** È la storia che chiude l'epica 03 trasformando in **codice verificabile** i presidi che le altre
cinque hanno preparato. La ragione sta nella sentenza della Corte di giustizia dell'Unione europea del 7 dicembre
2023, causa C-634/21: un punteggio calcolato in automatico è una **decisione automatizzata ai sensi
dell'articolo 22** del regolamento europeo quando determina «in modo decisivo» la conclusione, l'esecuzione o la
cessazione di un rapporto contrattuale, **anche se chi calcola e chi decide sono soggetti diversi** — e qui chi
calcola siamo noi e chi decide è il nostro cliente, cioè esattamente lo schema esaminato dalla Corte (§2.3 e §6
della [descrizione](../application-description.md)).

La conseguenza pratica è che il punteggio deve restare **materiale per una decisione**, mai la decisione. Il modo
per garantirlo non è una frase nella documentazione: è un vincolo strutturale che una prova automatica sorveglia,
perché una promessa che nessun collaudo controlla è una promessa che prima o poi qualcuno smentisce con una
modifica innocente.

Questa storia sta **prima** dell'epica 04 di proposito: costruisce i presidi che gli interventi dovranno usare,
così che la conferma umana nasca vincolata invece di essere aggiunta dopo.

## 2. Requisiti funzionali

1. **RF-1** — **Nessun intervento parte da una soglia di punteggio**, e non esiste alcuna configurazione che lo
   abiliti: non un interruttore spento, non una funzione riservata, non un'opzione per i piani alti. Il passaggio
   da un punteggio a un effetto verso il cliente finale esiste **solo** attraverso la conferma di una persona
   (epica 04).
2. **RF-2** — Esiste un **riquadro dei tre fatti principali**, riutilizzabile: dato un rapporto, restituisce i tre
   contributi di peso maggiore del suo punteggio corrente, ciascuno con tipo di segnale, data, peso e verso, più
   la versione del modello. È l'oggetto che ogni conferma di intervento dovrà mostrare (`0019`).
3. **RF-3** — La **spiegazione del punteggio entra nell'esportazione dei dati dell'interessato**: chi esporta i
   dati di un rapporto ottiene, insieme ai fatti, i punteggi con i loro contributi in forma leggibile — non solo
   i numeri. Il cliente finale ha diritto a sapere da che cosa nasce il giudizio che lo riguarda; non è un segreto
   industriale (§6).
4. **RF-4** — Esiste una **prova automatica** che dimostra che **nessun effetto verso l'esterno può partire da un
   cambio di punteggio**: il percorso di calcolo (storia `0013`) non pubblica eventi verso altre applicazioni, non
   crea interventi, non scrive in alcuna coda di uscita, e non dipende — nemmeno indirettamente — da un componente
   che sappia farlo. La prova fallisce se qualcuno introduce quella dipendenza.
5. **RF-5** — La sezione `Modello` e la scheda del punteggio dichiarano a chi legge, in una riga leggibile e non in
   una nota legale, **che cosa il punteggio non fa**: non manda messaggi, non applica sconti, non chiude rapporti.
   È l'informazione sulle «conseguenze previste» richiesta dall'articolo 22, detta in italiano corrente.
6. **RF-6** — La documentazione dell'app dichiara **il limite di questo presidio**, e lo dichiara anche a schermo
   dove serve: se il titolare conferma a occhi chiusi, l'intervento umano diventa una formalità. Mostrare i tre
   fatti principali fa sì che confermare significhi **almeno averli avuti davanti**; non garantisce che siano stati
   letti. La difesa completa è **organizzativa, non tecnica**, e fingere il contrario sarebbe il difetto peggiore
   di questa storia.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** La lettura dei tre fatti principali filtra per `tenant_id` preso dal
  token di accesso verificato, sulle stesse tabelle `punteggio` e `contributo_punteggio`; un `tenant_id` che
  arrivasse dal corpo della richiesta o dai parametri viene ignorato.
- **RT-2 — Interfaccia di programmazione (§2).** Rotta
  `GET /api/fidelizzazione/v1/rapporti/{id}/fatti-principali` → i tre contributi di peso maggiore, la versione del
  modello, il momento del calcolo e la marcatura di parzialità; errori in `application/problem+json`; definizione
  OpenAPI aggiornata nello stesso commit. **Nessuna rotta nuova che produca effetti**: questa storia toglie
  percorsi, non ne aggiunge.
- **RT-3 — Persistenza (§8).** **Nessuna tabella nuova e nessuna migrazione**: si legge da `punteggio` e
  `contributo_punteggio` (storia `0013`) sullo schema `app_fidelizzazione`. È coerente con la natura della storia:
  un presidio non è un dato in più.
- **RT-4 — Modulo frontend (§3, §5).** Componente riutilizzabile «tre fatti principali» nel modulo
  `fidelizzazione`, pensato per essere incastonato nella conferma dell'intervento (`0019`); riga «che cosa questo
  punteggio non fa» nella sezione `Modello` e nella scheda del punteggio. Dati letti con il client generato; solo
  token del sistema di design; funziona in tema chiaro e scuro; controllo automatico di accessibilità.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe — i tre fatti, la riga «che cosa questo punteggio non fa»,
  l'avvertenza sul limite del presidio — passano dallo spazio-nomi `fidelizzazione` e sono presenti in
  `en, it, fr, es, de`.
- **RT-6 — Varchi e quota (§6, §7).** Catena dei varchi completa: `401`, `403` ad app spenta, `402` ad account non
  abilitato o abbonamento `canceled`, `403` a ruolo insufficiente. I tre fatti principali sono in lettura per
  tutti i ruoli, `member` compreso: chi prepara un intervento deve poterli vedere. **Nessun consumo di quota.**
  I **diritti dell'interessato restano accessibili in ogni caso** (§13), anche con app disabilitata o abbonamento
  scaduto: è il motivo per cui RF-3 riguarda l'esportazione e non l'interfaccia.
- **RT-7 — Esposizione conversazionale (§12).** **Nessuno strumento nuovo**; questa storia impone invece un
  vincolo agli strumenti dichiarati altrove: **nessuno strumento può, da solo, trasformare un punteggio in un
  effetto**. `conferma_intervento` e `autorizza_offerta` restano scritture irreversibili con conferma umana
  obbligatoria (§7 della descrizione), e la regola vale a maggior ragione dalla chat, dove è più facile confondere
  «scrivimi una bozza» con «mandagliela». Il livello conversazionale è di piattaforma e **non è ancora
  implementato** (UC 0061-0063): il vincolo va scritto nel contratto degli strumenti perché sia già vero quando
  quel livello arriverà.
- **RT-8 — Dati personali (§10).** **Nessun campo nuovo**, ma una modifica al contratto dati: `exportData` di
  `FidelizzazioneDataContract` deve restituire, per ogni rapporto, i punteggi **con i loro contributi in forma
  leggibile** (tipo di segnale, data, peso, verso, versione del modello) e non solo il valore numerico. Le voci di
  manifesto sono già dichiarate — `punteggio.valore_e_contributi` (storia `0013`) e
  `contestazione.autore_e_motivo` (storia `0015`), entrambe in italiano e inglese: qui si verifica che
  l'esportazione le renda **comprensibili**, non solo presenti. La chiusura formale del contratto dati è della
  storia `0032`.
- **RT-9 — Registrazione eventi (§14).** `fatti principali consultati`, con `tenant_id`, `app_id`, `user_id` e
  identificativo di correlazione, senza dati personali. Nessun evento nuovo di scrittura, perché questa storia non
  ne introduce.

## 4. Criteri di accettazione

**CA-1 — Prova che nessun effetto parte da un cambio di punteggio**
- **Dato** il percorso di calcolo del punteggio (lavorazione giornaliera e ricalcolo su segnale nuovo)
- **Quando** gira la prova strutturale dell'area `backend`
- **Allora** la prova verifica che il calcolo non pubblichi alcun evento verso altre applicazioni, non scriva in
  alcuna coda di uscita e non dipenda da un componente capace di farlo; e **fallisce** se in una versione
  successiva quella dipendenza viene introdotta

**CA-2 — Nessuna configurazione abilita l'automatismo**
- **Dato** un account su qualunque piano, con qualunque ruolo, e la ricerca su tutte le opzioni esposte
  dall'interfaccia e dalla definizione OpenAPI
- **Quando** si cerca un'impostazione che faccia partire un intervento al superamento di una soglia
- **Allora** non ne esiste alcuna, e la prova che lo verifica è parte della suite

**CA-3 — I tre fatti principali sono disponibili e ordinati**
- **Dato** un rapporto con sei contributi al punteggio corrente
- **Quando** si chiedono i fatti principali
- **Allora** si ottengono i **tre** di peso maggiore, in ordine decrescente, ciascuno con tipo di segnale, data,
  peso e verso, più la versione del modello; se i contributi sono meno di tre, si ottengono quelli che ci sono e
  il risultato lo dichiara

**CA-4 — La spiegazione entra nell'esportazione**
- **Dato** un rapporto con punteggi calcolati e una contestazione attiva
- **Quando** si esegue l'esportazione dei dati dell'interessato per quel rapporto
- **Allora** il risultato contiene i punteggi **con i contributi in forma leggibile** (tipo di segnale, data,
  peso, verso, versione del modello) e la contestazione con il suo motivo, non i soli valori numerici

**CA-5 — Il limite è dichiarato**
- **Dato** un utente che apre la sezione Modello
- **Quando** legge la riga «che cosa questo punteggio non fa»
- **Allora** trova in chiaro che il punteggio non manda messaggi, non applica sconti e non chiude rapporti, e che
  la conferma umana protegge solo se chi conferma guarda — in tutte e cinque le lingue

**CA-6 — Isolamento fra account**
- **Dato** due account `A` e `B`
- **Quando** un utente di `A` chiede i fatti principali di un rapporto di `B` usandone l'identificativo
- **Allora** riceve `404` e nessun contributo di `B` compare nella risposta

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend`, `frontend`, `compliance`; l'intera suite prima del
      commit);
- [ ] **prova propria del divieto di automatismo**, richiesta esplicitamente da questa storia: prova strutturale
      di **unità** che verifica l'assenza di percorso dal calcolo del punteggio a qualunque effetto verso
      l'esterno, scritta in modo da restare valida — e da poter fallire — anche dopo che l'epica 04 avrà
      introdotto gli interventi;
- [ ] prove di **integrazione** sull'esportazione dei dati dell'interessato, che verificano la presenza dei
      contributi in forma leggibile;
- [ ] prova di **isolamento fra account** sulla rotta dei fatti principali;
- [ ] **prova end-to-end**: *rimandare* per il percorso, **coprire ora** per il presidio — il percorso
      `[J-FIDELIZZAZIONE]` nasce nella storia `0030` e vi si aggiungerà il passo «un cambio di punteggio non
      produce alcun effetto»; la prova strutturale di questa storia non è end-to-end e vive nell'area `backend`.
      Voce `da-coprire` con motivo e storia proprietaria `0030` nel registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml); la ripetizione a livello di
      percorso è della storia `0031`;
- [ ] **traduzioni** presenti in tutte e cinque le lingue (`en, it, fr, es, de`);
- [ ] **manifesto dei dati**: nessuna voce nuova, ma `exportData` verificata sui contributi in forma leggibile;
- [ ] **registro delle decisioni** compilato con: quale obbligo dell'articolo 22 adempie ciascun presidio, perché
      la prova è strutturale e non funzionale, e il limite dichiarato del presidio organizzativo;
- [ ] contratto degli **strumenti conversazionali**: nessuno nuovo, e il vincolo «nessuno strumento trasforma da
      solo un punteggio in un effetto» scritto nel contratto;
- [ ] documentazione aggiornata: la descrizione §6 e §11 riflettono i presidi effettivamente implementati.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0014` — spiegazione del punteggio | i tre fatti principali sono un estratto dei contributi, e l'esportazione leggibile riusa la stessa forma |
| storia `0015` — contestazione del punteggio | l'intervento umano non è solo «confermare»: comprende il poter dire di no, ed è ciò che rende il presidio credibile |
| storia `0013` — calcolo e storico | è il percorso su cui la prova strutturale verifica l'assenza di effetti |
| storia `0019` — intervento con conferma umana (epica 04) | **consumatore**: userà il componente dei tre fatti nella schermata di conferma e porterà la propria prova sulla macchina a stati. Qui si prepara il vincolo, lì lo si applica |
| storia `0032` — chiusura del contratto dati | chiude formalmente esportazione e cancellazione su tutte le tabelle; qui si verifica il solo pezzo della spiegazione |

## 7. Fuori ambito

- **la macchina a stati dell'intervento** e la sua prova che da `bozza` non si esce senza una persona: storia
  `0019`. Sono due presidi diversi: qui si dimostra che il **punteggio** non fa partire nulla, lì che
  l'**intervento** non esce da solo;
- **la ripetizione del presidio a livello di percorso end-to-end**: storia `0031`;
- **la chiusura del contratto dati** su tutte le tabelle: storia `0032`;
- **un obbligo di lettura verificato** (per esempio richiedere che l'utente scorra i tre fatti prima di poter
  confermare): deliberatamente fuori. Sarebbe attrito travestito da presidio e non dimostrerebbe comunque che
  qualcuno ha capito; il limite si dichiara invece di simularne la soluzione;
- **la valutazione d'impatto sulla protezione dei dati**: non è un artefatto di codice ed è del punto aperto n. 4.

## 8. Punti aperti

- **La base giuridica della profilazione, l'informativa al cliente finale e la valutazione d'impatto.** Il
  legittimo interesse è la candidatura naturale e richiede una valutazione di bilanciamento che nessun agente può
  scrivere; una profilazione sistematica su una popolazione che non ha rapporti con noi richiede molto
  probabilmente una valutazione d'impatto. Questa storia realizza i presidi tecnici, **non** li sostituisce.
  Chiude: **sviluppatore** e **revisione legale** — punto aperto n. 4 della descrizione.
- **Se i tre fatti principali debbano essere tre.** Il numero viene dalla descrizione (§11, attenuazione del
  rischio «decisione automatizzata di fatto») ed è una convenzione ragionevole: pochi abbastanza da leggerli,
  abbastanza da non essere un titolo. **Raccomandazione**: tenerli tre e renderli espandibili all'elenco completo
  con un clic, che rimanda alla spiegazione (`0014`). Chiude: **sviluppatore**.
