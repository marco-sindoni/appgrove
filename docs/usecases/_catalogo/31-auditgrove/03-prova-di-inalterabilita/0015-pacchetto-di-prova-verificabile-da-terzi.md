# 0015 — Pacchetto di prova verificabile da terzi

**Applicazione**: 31 — AuditGrove (`agentaudit`) · **Epica**: 03 — Prova di inalterabilità
**Storia**: `0015` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0013`, `0014`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come persona che deve dimostrare a qualcun altro cosa ha fatto un agente
> voglio poter scaricare un pacchetto che il mio interlocutore possa verificare da solo, senza fidarsi di
> AuditGrove e senza usare il nostro programma
> così da poter sostenere quello che dico anche davanti a chi ha tutte le ragioni per dubitare di me e del mio
> fornitore.

**Contesto.** La verifica della storia 0014 è eseguita dal nostro codice, sui nostri dati, e restituisce un
verdetto nostro. Serve, e serve a chi già si fida. Ma il momento per cui esiste questo prodotto è l'altro: quando
un cliente contesta, un revisore chiede, un'assicurazione istruisce una pratica. In quel momento *«il nostro
sistema dice che è integro»* non è una prova: è una nostra affermazione.

Un pacchetto è una prova quando chi lo riceve può ricalcolare tutto per conto proprio, con strumenti suoi. Il che
significa una cosa scomoda e necessaria: **dobbiamo pubblicare l'algoritmo**. Come si serializzano i campi, in che
ordine, con quale funzione di impronta, come si concatena. Un formato segreto non è verificabile; un formato
pubblicato lo è, e non indebolisce la sicurezza — la catena non protegge un segreto, dimostra una sequenza.

## 2. Requisiti funzionali

1. **RF-1** — Una persona dell'account può richiedere il **pacchetto di prova** di un intervallo indicato per date
   o per sequenze; la produzione è differita e il pacchetto viene reso disponibile per lo scaricamento quando è
   pronto.
2. **RF-2** — Il pacchetto contiene: le **azioni** dell'intervallo nella loro forma canonica, i **sigilli** che lo
   coprono con le rispettive firme, la **parte pubblica** delle chiavi di firma usate con il loro periodo di
   validità, e il **documento che descrive l'algoritmo** (forma canonica dei campi, ordine di concatenamento,
   funzione di impronta, modo di verificare la firma).
3. **RF-3** — Il pacchetto è **autosufficiente**: chi lo riceve può ricalcolare la catena e verificare le firme
   senza collegarsi ai nostri sistemi e senza eseguire codice nostro. Nessun riferimento a risorse in rete.
4. **RF-4** — Il pacchetto **non contiene i contenuti allegati cifrati**, salvo richiesta esplicita di chi lo
   produce; e quando li contiene, lo dichiara in prima pagina insieme al motivo per cui includerli è una scelta
   pesante (epica 06).
5. **RF-5** — La produzione di un pacchetto è **un'esportazione**: è tracciata (chi l'ha prodotta, quando, su
   quale intervallo, con o senza contenuti), richiede un ruolo adeguato, e chi la richiede vede prima un avviso
   che spiega che il pacchetto contiene identificativi di persone.
6. **RF-6** — La produzione del pacchetto è essa stessa **una riga del registro**, come la verifica: portare via
   una porzione del registro è un fatto che il registro deve conoscere.
7. **RF-7** — Il pacchetto porta un proprio **indice** leggibile da una persona: intervallo, numero di azioni,
   numero di sigilli, esito della verifica al momento della produzione, e le istruzioni in lingua per verificarlo.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Il pacchetto contiene esclusivamente righe dell'account ricavato dal
  `tenant_id` del token verificato; un intervallo di sequenze che sconfinasse in un altro account produce un
  pacchetto vuoto per quella parte, non un errore che ne riveli l'esistenza. Ogni interrogazione che alimenta la
  produzione filtra per `tenant_id`.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `POST /api/agentaudit/v1/evidence-packages` (richiesta),
  `GET /api/agentaudit/v1/evidence-packages` (elenco e stato) e una rotta di scaricamento a validità limitata;
  corpo validato; errori in `application/problem+json`; definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V<N>__pacchetti_di_prova.sql` sullo schema `app_agentaudit`: tabella
  `evidence_packages` con `tenant_id`, chiave primaria UUID versione 7, colonne di controllo, intervallo, stato,
  inclusione o meno dei contenuti, momento di scadenza dello scaricamento. Il pacchetto prodotto ha una **scadenza
  di disponibilità** e non resta a disposizione per sempre: è materiale che contiene identificativi di persone.
- **RT-4 — Modulo frontend (§3, §5).** La richiesta e lo scaricamento vivono nella sezione «Integrità» già
  introdotta dalla storia 0014; nessuna sezione nuova. Solo token del sistema di design, tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe visibili passano dallo spazio-nomi `agentaudit` e sono
  presenti in `en, it, fr, es, de`, **comprese le istruzioni di verifica contenute nel pacchetto**: chi le legge
  può non parlare la lingua di chi lo ha prodotto.
- **RT-6 — Varchi e quota (§6, §7).** Come la verifica, la produzione del pacchetto **non consuma** la metrica
  `actions`. Restano i varchi: `401` senza token, `403` per ruolo insufficiente — non tutti devono poter portare
  via il registro —, `402` con abbonamento non attivo. Con abbonamento in `past_due` la funzione resta
  accessibile; l'esportazione dei dati resta accessibile in ogni caso, perché è anche un diritto dell'interessato.
- **RT-7 — Esposizione conversazionale (§12).** Lo strumento corrispondente è `prepara_esportazione(periodo,
  formato) → bozza di esportazione`, marcato **scrittura** e con **conferma umana obbligatoria**: produce
  materiale che contiene identificativi di persone e lo rende scaricabile, quindi ha un effetto verso l'esterno.
  Il contratto vive dentro il servizio; il server conversazionale è di piattaforma e non ancora implementato
  (UC 0061-0063); la dichiarazione completa è la storia 0035.
- **RT-8 — Dati personali (§10).** Il pacchetto **contiene identificativi di persone** (chi ha chiesto un'azione,
  chi l'ha approvata) e, se richiesto, contenuti allegati che possono contenere qualunque cosa. La tabella dei
  pacchetti va aggiunta a `exportData` e `purgeData` del contratto dati dell'app; le voci di manifesto in italiano
  e inglese descrivono il pacchetto come *estrazione temporanea* con la propria scadenza; i campi che riportano
  identificativi sono annotati `@PersonalData`.
- **RT-9 — Registrazione eventi (§14).** Gli eventi `pacchetto richiesto`, `pacchetto prodotto`, `pacchetto
  scaricato` e `pacchetto scaduto` sono registrati con `tenant_id`, `app_id`, `user_id` e identificativo di
  correlazione, senza dati personali. Il secondo e il terzo sono anche righe del registro dell'app, non solo del
  registro tecnico: sono due fatti diversi e vanno distinti.

## 4. Criteri di accettazione

**CA-1 — Un terzo verifica senza il nostro codice**
- **Dato** un pacchetto di prova di un intervallo integro
- **Quando** una persona che non ha accesso ai nostri sistemi ricalcola la catena seguendo il documento
  dell'algoritmo contenuto nel pacchetto, con strumenti propri
- **Allora** ottiene le stesse impronte, le firme dei sigilli risultano valide, e la conclusione coincide con la
  nostra

**CA-2 — Il pacchetto smaschera un'alterazione**
- **Dato** un pacchetto prodotto su un intervallo in cui una riga è stata alterata dopo l'ultimo sigillo
- **Quando** il terzo ricalcola
- **Allora** trova la divergenza alla stessa riga indicata dalla nostra verifica, e il sigillo precedente
  risulta comunque valido e coerente fino al proprio limite

**CA-3 — Niente contenuti senza volerlo**
- **Dato** una richiesta di pacchetto che non chiede espressamente i contenuti allegati
- **Quando** il pacchetto viene prodotto
- **Allora** non contiene nessun contenuto allegato, contiene solo forme e impronte, e l'indice lo dichiara

**CA-4 — L'esportazione lascia traccia**
- **Dato** un account con il registro attivo
- **Quando** una persona produce e poi scarica un pacchetto
- **Allora** nel registro compaiono le righe corrispondenti con chi, quando e su quale intervallo, e quelle righe
  sono a loro volta incatenate come tutte le altre

**CA-5 — Isolamento fra account**
- **Dato** due account `A` e `B` con azioni proprie
- **Quando** un utente di `A` richiede un pacchetto forzando nella richiesta un intervallo o un identificativo di
  account che appartiene a `B`
- **Allora** riceve un pacchetto contenente esclusivamente righe di `A`, e nessuna informazione sull'esistenza di
  righe di `B`

**CA-6 — Lo scaricamento scade**
- **Dato** un pacchetto prodotto e mai scaricato
- **Quando** è trascorso il tempo di disponibilità previsto
- **Allora** il collegamento di scaricamento non funziona più, il pacchetto viene rimosso dal deposito, e resta la
  riga di registro che ne attesta la produzione

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno le aree toccate; l'intera suite prima del commit);
- [ ] prove di **unità** sulla forma canonica scritta nel pacchetto e sulla coerenza fra documento dell'algoritmo
      e implementazione, e di **integrazione** sulla produzione differita, con database effimero e migrazioni
      vere;
- [ ] **prova di verificabilità indipendente**: un programma di verifica scritto **senza riusare le classi del
      servizio**, che segue solo il documento dell'algoritmo, ricalcola correttamente un pacchetto di prova. È la
      prova che vale più di tutte le altre in questa storia: se non passa, il pacchetto non è verificabile da
      terzi;
- [ ] prova di **isolamento fra account** sulla produzione e sullo scaricamento;
- [ ] **prova end-to-end**: risposta «coprire ora» — il percorso `[J-AGENTAUDIT]` riceve il passo «richiedi il
      pacchetto di prova, attendi, scaricalo», e il registro di copertura
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) viene aggiornato;
- [ ] **traduzioni** presenti in tutte e cinque le lingue (`en, it, fr, es, de`), comprese le istruzioni dentro il
      pacchetto;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese, con i campi annotati e la tabella dei pacchetti
      presente in esportazione e cancellazione;
- [ ] **registro delle decisioni** `changes/NNNN-*/decisions.json` compilato, con la voce obbligatoria sulla
      pubblicazione dell'algoritmo e sul perché non indebolisce la sicurezza;
- [ ] contratto degli **strumenti conversazionali** dichiarato per `prepara_esportazione`, marcato scrittura con
      conferma umana;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata: il documento dell'algoritmo è materiale pubblico e va versionato con il servizio.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0013` | Senza sigilli il pacchetto contiene una catena che si autocertifica, cioè poco |
| storia `0014` | Riusa il ricalcolo e la logica di confronto con i sigilli, e la sezione «Integrità» dove vive |
| Deposito per file temporanei con scadenza | Il pacchetto è materiale che contiene identificativi di persone e non deve restare a disposizione indefinitamente |

## 7. Fuori ambito

- l'inclusione dei contenuti allegati in chiaro: possibile solo su richiesta esplicita e governata dall'epica 06,
  che decide come i contenuti sono cifrati e cancellabili;
- l'invio automatico del pacchetto a un destinatario esterno: qui si scarica, non si spedisce. Il recapito
  periodico riguarda i **sigilli**, non i pacchetti, ed è la storia 0017;
- la firma del pacchetto da parte di un prestatore di servizi fiduciari: punto aperto di prodotto (§11 della
  descrizione dell'applicazione);
- l'esportazione operativa del registro in formato tabellare per uso quotidiano: storia 0027, che è un'altra cosa
  — quella serve a lavorare, questa a dimostrare.

## 8. Punti aperti

- **Formato del pacchetto.** Un archivio con file di testo strutturato è la scelta più verificabile e la più
  brutta da leggere; un documento impaginato è il contrario. Propongo l'archivio come formato primario, con
  l'indice leggibile in aggiunta. Chi chiude: sviluppatore.
- **Chi può produrre un pacchetto.** Portare via una porzione del registro è un'azione delicata: propongo che
  richieda un ruolo amministrativo dell'account e non il solo permesso di lettura, ma questo confligge con il caso
  d'uso del revisore esterno che deve poter dimostrare senza chiedere permesso. Il compromesso — il revisore
  produce, l'amministratore autorizza — va deciso insieme alla storia 0029. Chi chiude: sviluppatore.
- **Quanto vale davvero questa prova.** Tecnicamente il pacchetto dimostra che la catena è coerente con sigilli
  firmati a certe date. Se questo basti in un contenzioso, e in quali giurisdizioni, è una domanda per un legale e
  non per noi: è il punto 3 dei rischi e punti aperti della descrizione dell'applicazione. Chi chiude: revisione
  legale.
