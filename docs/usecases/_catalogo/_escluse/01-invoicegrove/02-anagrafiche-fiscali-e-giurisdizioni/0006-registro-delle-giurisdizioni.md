# 0006 — Registro delle giurisdizioni

**Applicazione**: 01 — InvoiceGrove (`einvoicing`) · **Epica**: 02 — Anagrafiche fiscali e giurisdizioni
**Storia**: `0006` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0002` — è la prima dell'epica
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come sviluppatore che manterrà questa app negli anni
> voglio che ogni paese sia descritto da un profilo di conformità versionato, non da condizioni sparse nel codice
> così da poter aggiungere una giurisdizione senza riscrivere il motore e senza rompere quelle che già funzionano.

**Contesto.** La nota architetturale del catalogo è netta: «non trattare il problema come "stessi dati, XML
diversi"». Il ciclo di vita legale cambia famiglia con la giurisdizione — a liberatoria in Italia e Polonia, a
quattro angoli in Belgio, a cinque angoli in Francia — e le date di entrata in vigore si muovono (la Polonia ha
spostato le micro-imprese a gennaio 2027; la Francia ha già rinviato una volta). Un profilo dichiarativo e
versionato è l'unico modo per assorbire quei movimenti senza toccare il codice ogni volta.

## 2. Requisiti funzionali

1. **RF-1** — Esiste un registro delle giurisdizioni: per ogni paese, il codice, la **famiglia del modello**
   (`clearance`, `four_corner`, `five_corner`), il formato di serializzazione, il canale, e lo stato
   (`implementata`, `dichiarata_non_implementata`).
2. **RF-2** — Ogni profilo porta una **versione** e un intervallo di validità, così che si possa descrivere «dal
   1° febbraio 2026 il formato polacco è FA(3)» senza cancellare la versione precedente.
3. **RF-3** — Ogni profilo porta le **date di entrata in vigore** degli obblighi e le soglie di applicabilità
   (per esempio: micro-imprese polacche dal 1° gennaio 2027; imprese tedesche sopra €800.000 dal 2027).
4. **RF-4** — Le giurisdizioni implementate nella prima versione sono **Italia** (a liberatoria) e la **rete a
   quattro angoli** (Belgio e paesi che la usano). Le altre sono presenti nel registro come *dichiarate non
   implementate*, con la data prevista.
5. **RF-5** — Scegliere una giurisdizione non implementata è consentito per l'anagrafica e l'archivio ma **non**
   per la trasmissione, che risponde con un errore che dice esattamente perché e cosa si può fare intanto.
6. **RF-6** — Il registro è **dati, non codice**: si aggiorna senza ricompilare, ed è versionato nel repository.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Il registro delle giurisdizioni è **comune a tutti gli account**: non
  porta `tenant_id` perché non contiene dati di clienti. È l'unica tabella dell'app in questa condizione e il
  motivo va scritto nella migrazione, altrimenti sembra una dimenticanza.
- **RT-2 — Interfaccia di programmazione (§2).** Rotta `GET /api/einvoicing/v1/jurisdictions` in sola lettura, con
  filtro per stato; errori in `application/problem+json`; definizione OpenAPI aggiornata nello stesso commit. Non
  esiste una rotta di scrittura: il registro si aggiorna con una migrazione, non da interfaccia.
- **RT-3 — Persistenza (§8).** Migrazione `V5__jurisdiction_registry.sql` sullo schema `app_einvoicing`: tabelle
  `jurisdiction` e `jurisdiction_version`, con chiave UUID versione 7 e colonne di controllo. Nessuna cancellazione
  logica: un profilo non si cancella, si chiude con una data di fine validità.
- **RT-4 — Modulo frontend (§3, §5).** Sezione «Impostazioni → Paesi»: elenco in sola lettura di cosa l'app sa
  fare in ciascun paese, con lo stato ben visibile. Solo token del sistema di design, tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** I nomi dei paesi, delle famiglie di modello e le spiegazioni degli stati passano
  dallo spazio-nomi `einvoicing` e sono presenti in `en, it, fr, es, de`.
- **RT-6 — Varchi e quota (§6, §7).** Nessun consumo di quota: leggere il registro non costa nulla.
- **RT-7 — Esposizione conversazionale (§12).** Strumento dichiarato: `list_jurisdictions() → elenco dei paesi con
  stato e date`, marcato **lettura**, nessuna conferma. Il contratto vive dentro il servizio; il server
  conversazionale è di piattaforma e non ancora implementato (UC 0061-0063).
- **RT-8 — Dati personali (§10).** **Nessun dato personale nuovo**: il registro descrive norme, non persone.
- **RT-9 — Registrazione eventi (§14).** L'evento `profilo di giurisdizione applicato` — con codice paese e
  versione — è registrato con `tenant_id`, `app_id`, `user_id` e identificativo di correlazione. Serve a sapere,
  fra tre anni, con quale versione di regole un documento è stato validato.

## 4. Criteri di accettazione

**CA-1 — Il registro elenca ciò che l'app sa fare**
- **Dato** un utente su un account abilitato
- **Quando** apre «Impostazioni → Paesi»
- **Allora** vede Italia e la rete a quattro angoli come implementate, e Polonia, Francia e Germania come
  dichiarate non implementate con la loro data prevista

**CA-2 — Le versioni convivono**
- **Dato** il profilo polacco con una versione valida fino al 31 gennaio 2026 e una dal 1° febbraio 2026
- **Quando** si chiede il profilo valido a una data
- **Allora** si ottiene quello giusto per quella data, non l'ultimo scritto

**CA-3 — Giurisdizione non implementata**
- **Dato** un soggetto emittente in una giurisdizione dichiarata non implementata
- **Quando** si tenta di trasmettere un suo documento
- **Allora** si riceve un errore che dice quale paese non è coperto, da quando è previsto, e che l'archiviazione
  resta possibile

**CA-4 — Il registro non si scrive da interfaccia**
- **Dato** un utente con qualunque ruolo
- **Quando** tenta una scrittura sul registro delle giurisdizioni
- **Allora** non esiste alcuna rotta che glielo consenta

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (backend, frontend);
- [ ] prove di **unità** sulla selezione del profilo valido a una data e di **integrazione** sulla rotta di
      lettura;
- [ ] prova di **isolamento fra account**: **non applicabile e dichiarato** — il registro è comune e non contiene
      dati di clienti; il motivo è scritto nella migrazione e nel registro delle decisioni;
- [ ] **prova end-to-end**: *nessun impatto diretto* — la schermata è di sola lettura; il percorso
      `[J-EINVOICING]` (storia `0030`) userà il registro implicitamente;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati**: nessuna modifica, nessun dato personale;
- [ ] **registro delle decisioni** compilato, con la scelta «registro come dati versionati, non come codice»
      motivata dalla mobilità delle scadenze normative;
- [ ] contratto degli **strumenti conversazionali** dichiarato per `list_jurisdictions`.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| `0002` | Serve lo schema |

## 7. Fuori ambito

- Le **regole di validazione** vere e proprie: sono della storia `0014`, che si appoggia a questo registro per
  sapere quale insieme applicare.
- Gli **adattatori** di serializzazione e canale: sono della storia `0016`. Qui si dichiara quale famiglia serve,
  non come si realizza.
- L'avviso all'utente quando una scadenza si avvicina: storia `0010`.

## 8. Punti aperti

- **Quali giurisdizioni nella prima versione** è una decisione di prodotto (descrizione dell'applicazione §11,
  punto 6). La proposta — Italia più rete a quattro angoli — nasce dal fatto che sono le due famiglie
  architetturalmente diverse, quindi provano davvero il contratto dell'adattatore, e sono le sole raggiungibili
  senza un'immatricolazione.
- **La Francia non è aggiungibile con una storia.** Richiede l'immatricolazione di una piattaforma accreditata
  presso l'amministrazione fiscale, con certificazione ISO 27001 (descrizione dell'applicazione §2.3): è una
  decisione aziendale. L'alternativa — appoggiarsi a una piattaforma accreditata di terzi come fornitore — va
  valutata, non assunta.
