# 0029 — Strumenti di scrittura con bozza e conferma

**Applicazione**: 08 — SpendGrove (`notespese`) · **Epica**: 06 — Esposizione conversazionale e prove end-to-end
**Storia**: `0029` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0028`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che dalla chat vuole anche **fare** qualcosa, non solo chiedere
> voglio che l'assistente prepari — la spesa, la nota, l'esportazione — e che a dire di sì sia io
> così da guadagnare il tempo della compilazione senza perdere il controllo su ciò che finisce in contabilità.

**Contesto.** È la storia in cui si applica la regola di sicurezza che il catalogo pone a tutte le app: **strumenti
di lettura liberi, strumenti di scrittura che producono una bozza più un passaggio di approvazione umana** (§8 del
catalogo). Qui la regola non è una formalità: gli effetti in gioco sono soldi che escono, un rimborso che viene
chiesto e un pacchetto che parte verso il commercialista. Vale anche l'inverso, ed è la parte più importante di
questa storia: **ci sono due azioni che restano fuori**, e vanno scritte nel contratto perché nessuno le aggiunga
per comodità in un secondo momento.

## 2. Requisiti funzionali

1. **RF-1** — Il servizio dichiara i **sette strumenti di scrittura**: `leggi_ricevuta`, `crea_spesa`,
   `crea_percorrenza`, `categorizza_spesa`, `crea_nota_spese`, `invia_nota_spese`, `esporta_per_contabilita`,
   ciascuno con nome stabile, descrizione, schemi, marcatura **scrittura** e livello di conferma richiesto.
2. **RF-2** — Ogni strumento di scrittura produce una **bozza** e restituisce un riepilogo di ciò che accadrebbe;
   l'effetto si produce solo con una **conferma umana** esplicita.
3. **RF-3** — Gli strumenti con effetti irreversibili — `invia_nota_spese` e `esporta_per_contabilita` — richiedono
   una conferma **rafforzata**: il riepilogo elenca esattamente cosa esce e verso chi, e la conferma non è
   riutilizzabile per una seconda operazione.
4. **RF-4** — Le bozze scadono: una bozza non confermata entro un tempo dichiarato decade, e nulla resta a metà.
5. **RF-5** — 🛑 **Restano fuori due azioni**, e il contratto lo dichiara con il motivo: **approvare o respingere una
   nota spese** (è un atto di una persona verso un'altra persona) e **confermare i dati letti da una ricevuta** (se
   l'assistente potesse confermare la propria estrazione, la revisione umana sarebbe una finzione).
6. **RF-6** — Ogni operazione confermata attraversa gli stessi varchi delle rotte, quota compresa: una spesa creata
   dall'assistente consuma la sua unità di `receipts` esattamente come le altre.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** `tenant_id` **solo** dal token della chiamata, mai dai parametri; la
  conferma è legata alla bozza e alla stessa identità che l'ha creata: una bozza di un account non si conferma con
  il token di un altro.
- **RT-2 — Interfaccia di programmazione (§2).** Le operazioni si appoggiano alle rotte esistenti; la bozza è uno
  stato del dominio già previsto (`da_rivedere` per le spese, `bozza` per le note), non un magazzino parallelo di
  oggetti a metà.
- **RT-3 — Persistenza (§8).** Migrazione `V24__bozze_da_assistente.sql`: colonne che marcano l'origine
  dell'oggetto (interfaccia o assistente) e il momento di scadenza della bozza, con `tenant_id` e colonne di
  controllo. Sapere che cosa ha preparato l'assistente è indispensabile per capire, dopo, perché un dato è com'è.
- **RT-4 — Modulo frontend (§3, §5).** Gli oggetti preparati dall'assistente si riconoscono nell'interfaccia con
  un contrassegno di origine: chi apre l'app deve sapere che quella riga non l'ha scritta una persona. Solo token
  del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Il contrassegno di origine e i testi delle conferme mostrate nell'interfaccia
  passano dallo spazio-nomi `notespese` e sono presenti in `en, it, fr, es, de`.
- **RT-6 — Varchi e quota (§6, §7).** Gli strumenti attraversano la catena completa: `401` senza token, `403` se
  l'app è spenta o il ruolo non basta, `402` se l'abbonamento non è attivo, `429` a quota esaurita — con lo stesso
  messaggio di rimedio che riceve l'interfaccia.
- **RT-7 — Esposizione conversazionale (§12).** È la storia che *è* il contratto di scrittura. Regola di sicurezza
  applicata alla lettera: bozza più conferma; conferma rafforzata sugli effetti irreversibili; due azioni escluse
  con motivo scritto. Dipendenza: UC 0061-0065 (compresi l'applicazione della quota alle chiamate dell'assistente e
  il tracciamento), non ancora implementati.
- **RT-8 — Dati personali (§10).** Nessun campo nuovo che riguardi una persona; si aggiunge la marcatura di origine,
  che è un dato di attività. Voce aggiornata nel manifesto in italiano e inglese. **Le bozze scadute si cancellano
  fisicamente**: una bozza abbandonata è un dato personale conservato senza scopo.
- **RT-9 — Registrazione eventi (§14).** Ogni bozza creata da uno strumento e ogni conferma sono registrate con
  `tenant_id`, `app_id`, `user_id`, identificativo di correlazione, nome dello strumento ed esito — senza i valori.
  Per gli strumenti irreversibili la registrazione è **obbligatoria e non filtrabile**.

## 4. Criteri di accettazione

**CA-1 — Bozza e conferma**
- **Dato** l'invocazione di `crea_spesa` con dati completi
- **Quando** l'operazione termina
- **Allora** esiste una spesa in `da_rivedere` marcata come preparata dall'assistente, **nessuna quota è stata
  consumata**, e serve una conferma umana perché diventi `confermata`

**CA-2 — Conferma rafforzata sull'irreversibile**
- **Dato** l'invocazione di `invia_nota_spese` · **Quando** si esamina la risposta
- **Allora** contiene il riepilogo di cosa verrà inviato e a chi, e l'invio avviene solo dopo una conferma
  esplicita, non riutilizzabile per un secondo invio

**CA-3 — Le due azioni escluse**
- **Dato** la dichiarazione degli strumenti
- **Quando** la si esamina
- **Allora** non esistono strumenti per approvare o respingere una nota né per confermare i dati letti da una
  ricevuta, e il contratto ne riporta il motivo

**CA-4 — Scadenza della bozza**
- **Dato** una bozza creata dall'assistente e mai confermata
- **Quando** trascorre il tempo dichiarato
- **Allora** la bozza decade e i suoi dati sono cancellati

**CA-5 — Quota e varchi**
- **Dato** un account a quota esaurita · **Quando** si conferma una spesa preparata dall'assistente
- **Allora** la risposta è `429` con lo stesso messaggio di rimedio dell'interfaccia, e nulla viene confermato

**CA-6 — Isolamento fra account**
- **Dato** una bozza dell'account `A` · **Quando** si tenta di confermarla con il token dell'account `B`
- **Allora** l'operazione è respinta e nulla cambia

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno backend e frontend; l'intera suite prima del commit);
- [ ] prove di **unità** sul ciclo bozza → conferma → scadenza; di **integrazione** su ogni strumento con database
      effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** su ogni strumento di scrittura e sulla conferma della bozza; matrice dei
      ruoli completa;
- [ ] prova che dimostra l'**assenza** dei due strumenti esclusi: una prova che fallisce se qualcuno li aggiunge è
      il solo modo perché il divieto sopravviva a chi lo ha scritto;
- [ ] **prova end-to-end**: *nessun impatto* diretto sulla superficie utente oltre al contrassegno di origine, che
      il percorso `[J-NOTESPESE]` della storia `0031` verifica; registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) coerente;
- [ ] **traduzioni** del contrassegno e delle conferme in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese, con la cancellazione fisica delle bozze scadute;
- [ ] **registro delle decisioni** compilato, con i due divieti e il loro motivo;
- [ ] contratto degli **strumenti conversazionali** completo: sette strumenti di scrittura, livelli di conferma,
      due esclusioni motivate;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| `0028` | Il contratto di scrittura estende quello di lettura: forma, filtri e registrazione sono gli stessi |
| UC 0061-0065 | Server conversazionale, consenso delegato, applicazione della quota e tracciamento: tutti di piattaforma e non implementati |

## 7. Fuori ambito

- L'interfaccia di conferma dentro la chat: è del livello conversazionale (UC 0061-0062), non dell'app. L'app
  espone la bozza e pretende la conferma; **come** la conferma viene raccolta lo decide la piattaforma.
- L'esecuzione di pagamenti da chat: non esiste in questa app nemmeno dall'interfaccia (storia `0016`).

## 8. Punti aperti

- **Durata di validità di una bozza**: proposta 24 ore, da tarare. Troppo corta irrita, troppo lunga lascia in giro
  dati personali senza scopo.
- **Come si presenta all'utente il fatto che una riga l'ha preparata l'assistente** una volta che il livello
  conversazionale esisterà davvero: qui si prevede un contrassegno, ma la forma è una decisione di piattaforma.
