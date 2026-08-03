# 0010 — Importazione di liste in quarantena

**Applicazione**: 16 — ReachGrove (`campaigns`) · **Epica**: 02 — Pubblico e prova del consenso
**Storia**: `0010` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0007`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che arriva con il file di indirizzi che usava prima
> voglio poterlo caricare e capire subito, riga per riga, a chi posso scrivere e a chi no
> così da non scoprire il problema il giorno in cui ricevo una contestazione.

**Contesto.** È il momento in cui il prodotto dice cosa è. Chi arriva con tremila indirizzi presi da un foglio di
calcolo si aspetta di premere «importa» e mandare; qui riceve invece una lista **non inviabile** e una spiegazione.
Non è rigore per il gusto del rigore: chi invia deve conservare la prova dell'origine del dato, e la
responsabilità non si scarica su chi ha fornito la lista
([application-description.md](../application-description.md) §2.3 punto 4). C'è anche una ragione che riguarda
tutti gli altri clienti: la reputazione dell'infrastruttura di invio è **condivisa**, e un solo cliente che manda
a una lista comprata fa respingere i messaggi di tutti (§11, rischi noti). La quarantena è la difesa più
importante dell'app, e per questo non ha un pulsante che la scavalchi.

## 2. Requisiti funzionali

1. **RF-1** — Il cliente carica un file tabellare, associa le colonne ai campi dell'iscritto e vede un'anteprima
   delle prime righe prima che venga scritto qualcosa.
2. **RF-2** — Ogni riga importata crea un iscritto **in quarantena**: non contattabile, non incluso in nessun
   segmento che alimenti una campagna, non contato fra i contattabili. Non esiste nessuna opzione, nessun
   parametro e nessuna rotta che faccia entrare una riga già attiva.
3. **RF-3** — Una riga esce dalla quarantena **solo** allegando una prova, riga per riga o per l'intero file:
   origine del consenso, momento, testo accettato e — quando c'è — il documento o l'esportazione di provenienza.
   L'operazione crea registrazioni di consenso con origine `importazione con prova allegata` (storia 0007) ed è
   una dichiarazione firmata da chi la compie, con nome dell'utente e momento.
4. **RF-4** — Le righe il cui recapito è nell'elenco di soppressione (storia 0011) **non entrano affatto**: sono
   scartate e contate a parte, perché una soppressione non si annulla ricaricando un file.
5. **RF-5** — L'esito dell'importazione è un rapporto leggibile: righe lette, iscritti creati in quarantena, righe
   scartate per recapito non valido, per doppione, per soppressione; e — a chiare lettere — quanti degli iscritti
   creati sono contattabili, cioè **zero** finché non c'è la prova.
6. **RF-6** — Il dato grezzo di ogni riga si conserva, con l'indicazione di chi ha caricato il file e quando: serve
   a dimostrare cosa è stato caricato, non a rileggerlo per comodità.
7. **RF-7** — Prima di caricare, l'interfaccia spiega in due righe cosa succederà — «entreranno in quarantena,
   servirà la prova» — e non dopo, quando il cliente ha già fatto il lavoro.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** L'importazione, le sue righe e gli iscritti creati filtrano per
  `tenant_id` preso dal token verificato; un `tenant_id` presente nel file o nella richiesta viene ignorato.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `POST /api/campaigns/v1/imports` (caricamento),
  `GET /api/campaigns/v1/imports/{id}` (stato e rapporto) e
  `POST /api/campaigns/v1/imports/{id}/proofs` (allegazione della prova). **Non esiste** una rotta che imposti lo
  stato di un iscritto: la storia 0006 l'ha già escluso a livello di modello. Errori in `application/problem+json`;
  definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Tabelle `import_batch` e `import_row` sullo schema `app_campaigns` con
  `tenant_id`, chiave UUID versione 7, colonne di controllo e cancellazione logica; l'elaborazione è **idempotente
  rispetto al lotto**, così che una ripetizione dopo un guasto non raddoppi gli iscritti.
- **RT-4 — Modulo frontend (§3, §5).** Sezione «Importazione» del modulo `campaigns`: caricamento, associazione
  delle colonne, anteprima, avanzamento, rapporto, e la schermata di allegazione della prova. Solo token del
  sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe, compresi i motivi di scarto del rapporto, in
  `en, it, fr, es, de`.
- **RT-6 — Varchi e quota (§6, §7).** L'importazione **non** consuma `messages_sent` — gli iscritti archiviati non
  hanno tetto in nessun piano (§5 della descrizione) — ma ha un limite di righe per lotto governato dalla console
  di amministrazione, perché è una risorsa condivisa. Con abbonamento `canceled` risponde `402`.
- **RT-7 — Esposizione conversazionale (§12).** `importa_lista` **non è esposto** alla chat, per scelta
  dichiarata: un'importazione fatta «a voce» è precisamente il modo in cui nascono le liste di cui nessuno sa più
  l'origine (§7 della descrizione). Nemmeno l'allegazione della prova è esposta, per lo stesso motivo della
  registrazione del consenso.
- **RT-8 — Dati personali (§10).** Voce `import_row.payload` del manifesto in italiano e inglese: dato grezzo dei
  contatti importati, finalità «dimostrare cosa è stato caricato e da chi», base giuridica «prova»,
  conservazione proposta 24 mesi. Campi annotati `@PersonalData`, tabelle in `exportData` e `purgeData`. Attenzione
  al punto scomodo: qui entrano dati di persone che non hanno mai avuto rapporti con noi né, forse, col nostro
  cliente — è la ragione per cui la classificazione della change è **sostanziale** (§6 della descrizione).
- **RT-9 — Registrazione eventi (§14).** «Importazione avviata», «importazione conclusa», «righe scartate per
  soppressione», «prova allegata» con `tenant_id`, `app_id`, `user_id`, identificativo del lotto, conteggi e
  identificativo di correlazione; **mai** i recapiti, **mai** il contenuto delle righe.

## 4. Criteri di accettazione

**CA-1 — Tremila righe, zero contattabili**
- **Dato** un file con 3.000 recapiti validi su dominio `.test`
- **Quando** l'importazione si conclude
- **Allora** esistono 3.000 iscritti `in quarantena`, il rapporto dice «contattabili: 0», e nessun segmento li
  include ai fini di una campagna

**CA-2 — La quarantena non si scavalca**
- **Dato** un iscritto creato da importazione
- **Quando** si tenta di renderlo attivo con qualunque richiesta — aggiornamento dell'iscritto, seconda
  importazione, parametro di forzatura
- **Allora** ogni tentativo è respinto e lo stato resta `in quarantena`: l'unica via è allegare la prova

**CA-3 — La prova sblocca**
- **Dato** un lotto in quarantena e una prova allegata con origine, momento e testo accettato
- **Quando** l'utente conferma l'allegazione
- **Allora** nascono le registrazioni di consenso con origine `importazione con prova allegata`, gli iscritti
  diventano `attivi` e resta scritto **chi** ha firmato quella dichiarazione e quando

**CA-4 — La soppressione non si annulla ricaricando**
- **Dato** un recapito presente nell'elenco di soppressione perché si era disiscritto
- **Quando** lo stesso recapito compare nel file importato
- **Allora** la riga è scartata, contata come «scartata per soppressione», e nessun iscritto viene creato o
  riportato in vita

**CA-5 — Ripetere non raddoppia**
- **Dato** un'importazione interrotta a metà da un guasto
- **Quando** la si ripete sullo stesso lotto
- **Allora** gli iscritti già creati non vengono duplicati e il rapporto finale conta ogni riga una volta sola

**CA-6 — Isolamento fra account**
- **Dato** due account `A` e `B`
- **Quando** un utente di `A` chiede il rapporto di un'importazione di `B`
- **Allora** riceve `404`

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (`backend`, `frontend`, `compliance`; l'intera suite prima del commit);
- [ ] prove di **unità** sull'associazione delle colonne, sulla normalizzazione dei recapiti e sull'idempotenza del
      lotto; prove di **integrazione** sull'importazione completa con database effimero;
- [ ] prova di **isolamento fra account** su importazioni, righe e iscritti creati;
- [ ] **prova end-to-end**: coprire ora — il percorso `[J-CAMPAIGNS]` (storia 0037) include l'importazione di un
      file e la verifica che quegli iscritti **non** ricevano la campagna successiva: è il passo che dimostra la
      promessa dell'app; voce aggiunta al registro di copertura;
- [ ] **traduzioni** in tutte e cinque le lingue, compresi i motivi di scarto;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese per `import_batch` e `import_row`, con la nota sul
      trattamento di dati di persone estranee e la scadenza dei dati grezzi;
- [ ] **registro delle decisioni** compilato, con annotato che non esiste alcun modo di forzare la quarantena e
      perché;
- [ ] contratto degli **strumenti conversazionali**: `importa_lista` **non** esposto, con la motivazione scritta;
- [ ] controllo automatico di **accessibilità** verde sulle schermate di importazione;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storia `0007` | L'allegazione della prova crea registrazioni di consenso |
| Storia `0011` | Lo scarto per soppressione presuppone l'elenco; finché non esiste, quel conteggio è sempre zero e va provato così |
| Contratto degli eventi dell'anagrafica condivisa (§11.5 della descrizione) | Vale la stessa regola anche in ingresso: **un contatto che arriva per proiezione dall'app 04 senza la sua prova entra in quarantena**. La provenienza interna non è una prova |

## 7. Fuori ambito

- la **campagna di ri-richiesta del consenso** verso una lista in quarantena: deliberatamente non implementata,
  perché quel messaggio è a sua volta una comunicazione senza consenso e non ho trovato una fonte che ne chiarisca
  la liceità (§2.7 e §11.8 della descrizione). Chi importa una lista senza prova resta senza via d'uscita che non
  sia allegare la prova: è una conseguenza voluta, non una dimenticanza;
- l'unione dei duplicati fra file e archivio esistente: qui il doppione si conta e si scarta, non si fonde;
- l'importazione dei comportamenti storici (aperture, clic) da un altro prodotto: fuori perimetro, perché sarebbe
  un trattamento senza base propria.

## 8. Punti aperti

- **Liceità della ri-richiesta del consenso** — punto aperto dichiarato della descrizione (§11.8), chiude la
  revisione legale. È la domanda che ogni cliente farà il primo giorno, e la risposta attuale è «non lo facciamo».
- **Formato accettabile della prova allegata.** Un'esportazione da un altro prodotto, una fotografia di un modulo
  cartaceo, una dichiarazione scritta: quali di questi siano accettabili non è una decisione tecnica. La proposta
  è accettarli tutti come allegati e registrarne il tipo, lasciando al titolare la responsabilità di ciò che
  dichiara. Chiude lo sviluppatore con la revisione legale.
- **Limite di righe per lotto**: valore di piattaforma, governato dalla console di amministrazione
  ([estensioni-admin.md](../estensioni-admin.md)); il numero lo fissa lo sviluppatore.
