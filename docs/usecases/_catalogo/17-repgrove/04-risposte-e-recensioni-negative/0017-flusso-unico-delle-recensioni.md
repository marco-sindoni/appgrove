# 0017 — Flusso unico delle recensioni

**Applicazione**: 17 — RepGrove (`recensioni`) · **Epica**: 04 — Risposte e recensioni negative
**Storia**: `0017` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0009`, `0010`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare con due sedi e due piattaforme collegate
> voglio vedere tutte le recensioni in un elenco solo, ordinato, con quello che aspetta una risposta in cima
> così da smettere di girare fra applicazioni diverse per capire cosa è successo oggi.

**Contesto.** Le recensioni arrivano (storia 0009) ma non si vedono. Questa storia costruisce la schermata su cui
il cliente passerà il novanta per cento del tempo. La rassegna di mercato dice che il segmento vuole esattamente
questo e poco altro: «chiedere ai clienti e tenere traccia delle risposte» (descrizione §2.5). La tentazione da
evitare è il cruscotto pieno di indicatori: qui serve un elenco che risponde a tre domande — cosa è arrivato di
nuovo, cosa è negativo, cosa non ho ancora risposto.

Una dipendenza da tenere presente: la **ricerca nel testo** funziona solo se il testo si conserva, e questo è un
punto aperto (storia 0010). La schermata deve funzionare anche senza.

## 2. Requisiti funzionali

1. **RF-1** — Un elenco unico mostra le recensioni di tutte le sedi e di tutte le piattaforme collegate, ordinate
   per data decrescente, con voto, autore pubblico, piattaforma d'origine, prime righe del testo e stato della
   risposta.
2. **RF-2** — Si filtra per sede, piattaforma, voto, periodo e stato della risposta (`senza risposta`, `bozza`,
   `pubblicata`). I filtri sono pochi e stanno tutti sopra l'elenco, senza pannelli nascosti.
3. **RF-3** — Esiste una ricerca nel testo, **quando il testo è conservato**; quando non lo è, il campo di ricerca
   non compare e una riga spiega perché.
4. **RF-4** — La scheda di una recensione mostra il testo completo, l'attribuzione con il collegamento
   all'originale (storia 0010), la risposta se c'è, e la storia di ciò che è successo (raccolta, modificata,
   risposta pubblicata).
5. **RF-5** — Le recensioni non più pubbliche all'origine restano visibili, marcate come tali, finché non scadono:
   il cliente deve poter capire perché il suo punteggio è cambiato.
6. **RF-6** — L'elenco regge il caso reale: qualche migliaio di recensioni per account, con paginazione e senza
   caricare tutto in una volta.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura di `recensione` filtra per `tenant_id` preso dal token
  verificato; un `tenant_id` che arrivasse dal corpo, dai parametri o da un'intestazione viene ignorato.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `GET /api/recensioni/v1/recensioni` con filtri e
  paginazione a pagina e dimensione con totale, e `GET /api/recensioni/v1/recensioni/{id}`; errori in
  `application/problem+json`; definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Indici a supporto dei filtri usati davvero: `(tenant_id, sede_id, pubblicata_il)` e
  `(tenant_id, voto)`. La ricerca nel testo usa gli strumenti del database, non un motore esterno: un servizio in
  più per una funzione secondaria non si giustifica.
- **RT-4 — Modulo frontend (§3, §5).** Sezione *Recensioni*: elenco con filtri, scheda di dettaglio, stato vuoto
  che spiega cosa fare quando non c'è ancora niente. Dati letti con il client generato; solo token del sistema di
  design; funziona in tema chiaro e scuro. Il voto si legge **anche senza colore** (numero e stelle, non solo
  tinta): è un requisito di accessibilità, non un dettaglio grafico.
- **RT-5 — Cinque lingue (§4).** Interfaccia in `en, it, fr, es, de`; il **testo delle recensioni non si
  traduce** e si mostra nella lingua in cui è stato scritto, con l'indicazione della lingua quando è diversa da
  quella dell'interfaccia.
- **RT-6 — Varchi e quota (§6, §7).** La lettura richiede abbonamento in uno stato che dà accesso; con `canceled`
  risponde `402`. Nessun consumo di quota.
- **RT-7 — Esposizione conversazionale (§12).** È la funzione dietro `elenca_recensioni` (storia 0027): i filtri
  di questa storia sono gli stessi parametri dello strumento, e il risultato è **minimizzato** — non si
  restituisce l'intero testo di cento recensioni a un assistente.
- **RT-8 — Dati personali (§10).** Nessuna voce nuova nel manifesto: `recensione.autore` e `recensione.testo` sono
  già dichiarati (storia 0009). Qui però quei dati diventano **visibili a tutti gli utenti dell'account**: va
  verificato che la matrice dei ruoli sia quella voluta.
- **RT-9 — Registrazione eventi (§14).** Nessun evento di dominio nuovo; le letture non si registrano una per una.

## 4. Criteri di accettazione

**CA-1 — Elenco unico**
- **Dato** un account con due sedi e due piattaforme collegate
- **Quando** apre la sezione *Recensioni*
- **Allora** vede tutte le recensioni ordinate per data, ciascuna con la sua sede e la sua piattaforma

**CA-2 — Filtri**
- **Dato** l'elenco
- **Quando** filtra per voto minore o uguale a 2 e stato «senza risposta»
- **Allora** vede solo quelle, e il conteggio totale è coerente

**CA-3 — Isolamento fra account**
- **Dato** due account `A` e `B` con recensioni
- **Quando** un utente di `A` chiede l'elenco o il dettaglio di una recensione di `B`
- **Allora** vede solo le proprie e sul dettaglio altrui riceve `404`, anche forzando l'identificativo dell'altro
  account nella richiesta

**CA-4 — Attribuzione sempre presente**
- **Dato** una qualunque recensione dell'elenco
- **Quando** la si guarda in elenco o in dettaglio
- **Allora** porta la piattaforma d'origine e il collegamento alla recensione originale

**CA-5 — Senza testo conservato**
- **Dato** la politica di conservazione configurata per non tenere il testo (storia 0010)
- **Quando** si apre la sezione
- **Allora** l'elenco funziona con voto, data, autore e attribuzione, il campo di ricerca non compare e una riga
  spiega il motivo

**CA-6 — Recensione non più pubblica**
- **Dato** una recensione marcata non più pubblica
- **Quando** si guarda l'elenco
- **Allora** compare marcata come tale, e l'app dice che non conta più nel punteggio

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno le aree toccate; l'intera suite prima del commit);
- [ ] prove di **unità** sui componenti dell'elenco e di **integrazione** sulla rotta con filtri e paginazione,
      con database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** su elenco e dettaglio;
- [ ] **prova end-to-end**: *coprire ora* il passo «vedo la recensione arrivata» nel percorso `[J-RECENSIONI]`, e
      registro [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) aggiornato;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati**: nessuna voce nuova; verificata la visibilità per ruolo;
- [ ] **registro delle decisioni** compilato, con la scelta di non introdurre un motore di ricerca esterno;
- [ ] controllo automatico di **accessibilità** verde, compresa la leggibilità del voto senza colore.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0009` | servono recensioni raccolte |
| storia `0010` | l'attribuzione e la politica di conservazione decidono cosa si può mostrare e cercare |

## 7. Fuori ambito

- la risposta — storie 0018 e 0019;
- l'avviso sulle recensioni negative — storia 0020;
- il punteggio — storia 0022.

## 8. Punti aperti

- **Visibilità per ruolo.** Con più sedi, un responsabile di sede dovrebbe vedere solo la propria: la piattaforma
  non ha l'ambito per sede (storia 0006, punti aperti). Nella prima versione tutti vedono tutto.
- **Se il testo non fosse conservabile** (storia 0010), questa schermata perde la ricerca e diventa più povera: è
  previsto, non è un guasto, ma va valutato quanto cambia il valore percepito del prodotto.
</content>
