# 0020 — Evento «giacenza variata»

**Applicazione**: 14 — StockGrove (`magazzino`) · **Epica**: 03 — Registro dei movimenti e giacenze
**Storia**: `0020` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0013`, `0014`, `0015`, `0016`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che usa più applicazioni della suite
> voglio che le altre app sappiano quanta merce ho senza chiedermelo e senza chiederlo a StockGrove ogni volta
> così da non promettere in un preventivo o su un negozio online un articolo che non c'è più.

**Contesto.** StockGrove è il punto in cui la catena del documento tocca la realtà fisica: un preventivo accettato,
una fattura emessa, uno scontrino battuto sono promesse, la giacenza è il fatto (descrizione dell'applicazione,
§10). Perché le altre app possano leggere quel fatto senza chiamare StockGrove — cosa vietata dai principi di
piattaforma, §2 — StockGrove deve **raccontare** ogni variazione di saldo con un evento.

**Perché si scrive bene adesso anche se non lo ascolta nessuno.** Oggi ShopGrove (29), QuoteGrove (06) e
InsightGrove (20) non esistono. Un evento pubblicato è però un contratto verso il futuro: cambiargli forma dopo
significa rompere consumatori che non si controllano. Costa poco scriverlo bene ora e costa molto rifarlo dopo, ed è
il motivo per cui questa storia sta nell'epica del registro e non in fondo.

## 2. Requisiti funzionali

1. **RF-1** — Ogni variazione della giacenza di una coppia articolo-deposito — da qualunque movimento provenga:
   carico, scarico, trasferimento, storno, rettifica, evento ricevuto, importazione — produce un evento
   `giacenza.variata`.
2. **RF-2** — L'evento porta: identificativo dell'evento, account, articolo con **codice interno** e **codice
   GTIN** quando c'è, deposito con il suo codice, **quantità nuova**, **variazione** (il delta con segno), momento,
   **causa** (il tipo di movimento che l'ha prodotta), **versione della giacenza** e **versione dello schema
   dell'evento**.
3. **RF-3** — L'evento è scritto nella **stessa transazione** del movimento che lo genera, in una coda di uscita:
   se la transazione fallisce non resta un evento senza movimento, e se riesce non manca l'evento.
4. **RF-4** — La consegna è **almeno una volta**: lo stesso evento può arrivare più volte al consumatore, che deve
   poterlo trattare senza danno. L'identificativo dell'evento e la versione della giacenza sono i due appigli che lo
   rendono possibile.
5. **RF-5** — L'**ordinamento è garantito solo per coppia articolo-deposito**, tramite la versione crescente: fra
   articoli diversi non c'è alcun ordine promesso, e un consumatore che ricevesse la versione `7` dopo la `9` deve
   scartare la più vecchia, non applicarla.
6. **RF-6** — La documentazione dell'evento — campi, significato, garanzie, esempio — è versionata insieme al
   servizio: un consumatore deve poter capire il contratto senza leggere il codice.
7. **RF-7** — Gli eventi consegnati restano nella coda di uscita per una finestra dichiarata e poi vengono rimossi:
   la coda non è un archivio, l'archivio è il registro dei movimenti.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni evento porta il proprio `tenant_id` e la coda si legge sempre
  filtrando per account; nessun consumatore riceve mai eventi di un account a cui non è abilitato. Prova di
  isolamento fra due account sulla coda di uscita.
- **RT-2 — Interfaccia di programmazione (§2).** Nessuna rotta pubblica nuova: la comunicazione fra app è
  **asincrona a eventi** e mai sincrona. Resta esposta la lettura delle giacenze `GET /api/magazzino/v1/giacenze`
  per l'utente. La definizione dell'evento è pubblicata come schema versionato accanto alla definizione OpenAPI del
  servizio, aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V13__evento_in_uscita.sql` sullo schema `app_magazzino`: tabella
  `evento_in_uscita` (tipo, versione dello schema, contenuto, stato, tentativi, momento di creazione e di consegna)
  con `tenant_id`, chiave primaria UUID versione 7, colonne di controllo e cancellazione logica; indice sullo stato
  e sul momento per la spedizione ordinata. Scrittura nella **stessa transazione** del movimento; nessuna chiave
  esterna verso altri schemi.
- **RT-4 — Modulo frontend (§3, §5).** **Nessuna schermata**: l'evento è un fatto tecnico e l'utente non lo vede. Lo
  stato della coda è materia della console di amministrazione (`estensioni-admin.md`), come metadato e conteggio.
- **RT-5 — Cinque lingue (§4).** **Non applicabile**: l'evento non ha testo visibile. I nomi dei campi e i valori
  della causa sono **identificatori tecnici stabili** in inglese, non etichette da tradurre: un consumatore che
  legge `cause: "sale"` non deve dipendere dalla lingua di chi ha registrato il movimento.
- **RT-6 — Varchi e quota (§6, §7).** **Nessun consumo di quota**: la pubblicazione dell'evento è un effetto del
  movimento, e i movimenti non consumano mai la metrica `articoli_gestiti` né vengono respinti con `429`. Gli eventi
  si producono anche per account con abbonamento `past_due`; per account `canceled` la produzione continua ma la
  consegna è sospesa, coerentemente con l'accesso.
- **RT-7 — Esposizione conversazionale (§12).** **Nessuno strumento dichiarato**: un evento non è un'operazione che
  una persona chiede a voce. Le domande sulla giacenza passano da `leggi_giacenza` (storia `0034`).
- **RT-8 — Dati personali (§10).** **Nessun dato personale nell'evento**, ed è un requisito verificato: l'evento
  **non porta** l'autore del movimento, né note, né il riferimento al documento d'origine — solo cose, quantità e
  luoghi. È la scelta che rende questo evento sicuro da consegnare ad app che non conoscono il contesto di chi lo ha
  prodotto.
- **RT-9 — Registrazione eventi (§14).** Gli eventi `evento in uscita creato`, `consegnato`, `consegna fallita` sono
  registrati con `tenant_id`, `app_id`, `user_id` quando c'è e identificativo di correlazione, con l'identificativo
  dell'evento e **senza** il suo contenuto.

## 4. Criteri di accettazione

**CA-1 — Ogni movimento produce un evento**
- **Dato** un articolo con giacenza `10`
- **Quando** si registra uno scarico di 2 pezzi
- **Allora** nella coda di uscita esiste un evento `giacenza.variata` con quantità nuova `8`, variazione `−2`,
  causa corrispondente allo scarico, versione della giacenza pari a quella della riga aggiornata e versione dello
  schema dell'evento valorizzata

**CA-2 — Evento e movimento vivono o cadono insieme**
- **Dato** uno scarico che fallisce per merce insufficiente
- **Quando** la transazione viene annullata
- **Allora** nella coda di uscita non resta alcun evento per quel tentativo

**CA-3 — Trasferimento: due eventi, saldo d'impresa invariato**
- **Dato** un trasferimento di 5 pezzi fra due depositi
- **Quando** l'operazione va a buon fine
- **Allora** esistono **due** eventi, uno per ciascuna coppia articolo-deposito, con variazioni `−5` e `+5` e
  versioni proprie di ciascuna riga di giacenza

**CA-4 — Ordinamento per coppia articolo-deposito**
- **Dato** tre movimenti consecutivi sulla stessa coppia articolo-deposito
- **Quando** si leggono gli eventi prodotti
- **Allora** le versioni della giacenza sono crescenti e senza salti, e permettono a un consumatore di scartare un
  evento più vecchio arrivato in ritardo

**CA-5 — Nessun dato personale nell'evento**
- **Dato** uno scarico registrato da una persona, con nota e riferimento al documento
- **Quando** si ispeziona l'evento prodotto
- **Allora** non contiene l'autore, non contiene la nota e non contiene il riferimento al documento

**CA-6 — Isolamento fra account**
- **Dato** due account `A` e `B` con movimenti sullo stesso codice articolo
- **Quando** si legge la coda di uscita per l'account `A`
- **Allora** compaiono solo gli eventi di `A`, ciascuno con il proprio `tenant_id`

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno backend; l'intera suite prima del commit);
- [ ] prove di **unità** sulla composizione dell'evento (compresa l'assenza dei campi personali) e di
      **integrazione** che verifichino la scrittura nella stessa transazione del movimento;
- [ ] prova che l'annullamento della transazione **non lasci** eventi orfani;
- [ ] prova di **isolamento fra account** sulla coda di uscita;
- [ ] **prova end-to-end**: *nessun impatto sulla superficie utente* — l'evento non ha schermate; la verifica sta
      nelle prove di integrazione, e il registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) non riceve una voce nuova per questa
      storia;
- [ ] **traduzioni**: non applicabile, l'evento non ha testo visibile (motivo scritto nella storia);
- [ ] **manifesto dei dati**: nessuna voce nuova; verificato che l'evento non trasporti dati personali;
- [ ] **registro delle decisioni** compilato, con la forma dell'evento, la garanzia di consegna almeno una volta e
      il limite dell'ordinamento;
- [ ] contratto degli **strumenti conversazionali**: nessuna funzione esposta, con il motivo scritto;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione dell'evento scritta e versionata insieme al servizio.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| `0013` | La versione della riga di giacenza è ciò che rende ordinabile l'evento |
| `0014`, `0015`, `0016` | Servono movimenti di tutti i tipi per verificare tutte le cause |
| Canale a eventi di piattaforma | Il trasporto è comune: qui si scrive la coda di uscita e la forma del contenuto |

## 7. Fuori ambito

- Il consumo dell'evento da parte delle altre app: sono loro a scriverlo, quando esisteranno.
- L'evento sulle soglie di scorta («sceso sotto la scorta minima»): appartiene alla storia `0027`, che possiede il
  concetto di soglia.
- Un'interfaccia pubblica per consumatori esterni all'azienda: sarebbe un'apertura verso l'esterno con implicazioni
  proprie e non è nel perimetro.

## 8. Punti aperti

- **Finestra di conservazione della coda di uscita.** Serve un numero (giorni), che dipende da quanto a lungo un
  consumatore può restare fermo senza perdere eventi. Oggi non ci sono consumatori e non c'è un dato su cui
  basarsi: lo chiude lo sviluppatore quando nascerà la prima app che ascolta.
- **Se pubblicare anche il valore gestionale** insieme alla quantità: sarebbe utile a InsightGrove (20) ma
  esporterebbe un numero che l'applicazione dichiara come non fiscale, con il rischio che a valle diventi altro. La
  proposta è **non pubblicarlo** finché non c'è un consumatore che lo chiede e sa cosa non è.
