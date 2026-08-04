# BillGrove — descrizione dell'applicazione

**Numero di catalogo**: 02 · **Tipo**: orizzontale · finanza e amministrazione · **Stato del documento**: 🟡 bozza d'autore
**Scheda d'origine**: [catalogo, scheda 2](../appgrove-catalogo-applicazioni.md)
**Ultimo aggiornamento**: 2026-08-03
**Autore**: agente di catalogo (kit d'autore `_kit/`)

> Documento **di proposta**. Prezzi e classificazione dei dati personali sono proposte da confermare dallo
> sviluppatore, non decisioni prese. Vedi le avvertenze nelle sezioni 5 e 6.

---

## 1. Descrizione dell'applicazione

**Cosa fa.** BillGrove produce i documenti commerciali di una piccola attività: preventivi, fatture, note di
credito, documenti di trasporto e ricevute. Tiene l'anagrafica dei clienti, il catalogo di prodotti e servizi con i
relativi listini, calcola imposte e totali, stampa il documento, registra gli incassi e dice in ogni momento quanto
è stato incassato e quanto resta da incassare. È il sistema **di origine** del documento contabile: il documento
nasce qui, e da qui viene poi trasmesso, conservato e riconciliato.

**Per chi.** Micro-imprese da 1 a 10 addetti e piccole imprese fino a 50: artigiani, studi professionali, agenzie,
piccoli commercianti. Chi compra è il titolare; chi usa tutti i giorni è il titolare stesso o una persona
dell'amministrazione, spesso a tempo parziale. Una terza figura ricorrente è il commercialista esterno, che non
lavora dentro l'app ma ne vuole i dati a scadenze fisse. Mercato globale con priorità europea.

**Quale problema toglie.** Oggi la micro-impresa fa le fatture con un foglio di calcolo o con un modello di
videoscrittura, tiene la numerazione a mano su un quaderno, ricopia gli stessi dati del cliente ogni volta e scopre
di non essere stata pagata solo quando guarda il conto corrente. Il costo non è il tempo di battitura: è l'errore di
numerazione che si scopre a fine anno, il preventivo perso, la fattura scaduta da settanta giorni che nessuno ha
sollecitato perché nessuno la stava guardando. I prodotti concorrenti risolvono questo, ma la ricerca (§2.5) mostra
due lamentele stabili: sono percepiti o come troppo scarni per chi cresce, o come troppo pieni di funzioni per chi
deve solo fare venti fatture al mese.

**Cosa NON fa.** Non è un programma di contabilità: non tiene la partita doppia, non produce il bilancio, non fa le
dichiarazioni fiscali. Non gestisce il magazzino (il documento di trasporto registra ciò che esce, non la giacenza).
Non fa il recupero crediti strutturato — solleciti a più canali, punteggio di rischio, previsione di cassa — che è
CashGrove (3). Non è il motore di conformità multi-paese per la fattura elettronica: BillGrove **produce** il
documento in forma canonica e lo consegna a uno strato di conformità, che è InvoiceGrove (1). Non è un registro di
protocollo né un archivio documentale generico.

**Rischio di sostituzione da parte dei modelli linguistici.** `neutra`. Un assistente generico sa scrivere il testo
di una fattura, ma non tiene la numerazione progressiva, non conosce lo storico dei clienti, non sa che quel
documento è già stato incassato e non risponde di ciò che ha scritto. Il valore sta nel dato proprietario
(anagrafiche, listini, storico dei documenti), nella macchina a stati del documento e nel fatto che l'informazione
serve a valle: al commercialista, allo strato di conformità, alla riconciliazione degli incassi. La frequenza d'uso
quotidiana e il costo di migrazione dei dati storici sono ciò che trattiene il cliente, non la generazione di testo.

---

## 2. Mercato e analisi in rete

> Compilata dopo **dieci** interrogazioni fra ricerche e letture di pagina ([GUIDA-AUTORE.md](../_kit/GUIDA-AUTORE.md) §4).
> Ciò che non è stato trovato è **dichiarato** al §2.7, non colmato a intuito.

### 2.1 Concorrenti

| Prodotto | Dove | Cosa fa | Prezzo rilevato | Fonte |
|---|---|---|---|---|
| Fatture in Cloud (TeamSystem) | Italia | Fatturazione, preventivi, incassi, conservazione, riconciliazione bancaria | **Rilevato su pagina ufficiale**: Forfettari 4 €/mese (48 €/anno, 100 documenti/anno); Standard 12 €/mese (144 €/anno, 100 documenti); Premium 21 €/mese (252 €/anno, 400 documenti); Premium Plus 29 €/mese (348 €/anno, 800 documenti); Complete 51 €/mese (612 €/anno, 3.000 documenti). Prova 31 giorni senza carta | [fattureincloud.it/costo](https://www.fattureincloud.it/costo/) |
| Zoho Invoice | Globale | Fatturazione, preventivi, spese, promemoria di pagamento | **Rilevato su pagina ufficiale**: gratuito, senza limiti dichiarati in pagina | [zoho.com/invoice](https://www.zoho.com/invoice/) |
| Aruba Fatturazione Elettronica | Italia | Emissione, ricezione e conservazione a norma | Da 29,90 € + IVA/anno al rinnovo, conservazione decennale inclusa — **fonte di confronto, non pagina ufficiale** | [softwaresemplice.it — Aruba vs Fatture in Cloud](https://www.softwaresemplice.it/blog/aruba-fatture-in-cloud-prezzi-a-confronto/1203) |
| Danea Easyfatt | Italia | Gestionale da tavolo con magazzino e fatturazione | 15–40 €/mese, prodotto installato, non in mobilità — **fonte di confronto, non pagina ufficiale** | [srlonline.com — confronto gestionali 2026](https://www.srlonline.com/software-gestionali-2026-fatture-in-cloud-vs-danea-teamsystem-confronto-prezzi-funzioni/) |
| FreshBooks | Globale | Fatturazione e piccola contabilità, prezzo per utente | Da ~21–23 $/mese, utenti aggiuntivi ~11 $/mese — **fonte di confronto, non pagina ufficiale** | [temperstack.com — FreshBooks vs Invoice Ninja](https://www.temperstack.com/versus/freshbooks-vs-invoice-ninja/) |
| Invoice Ninja | Globale | Fatturazione, anche installabile in proprio | Piano gratuito; a pagamento da ~14 $/mese — **fonte di confronto** | [temperstack.com](https://www.temperstack.com/versus/freshbooks-vs-invoice-ninja/) |

**Lettura.** Il mercato è affollato e maturo: la sola emissione del documento è una merce indifferenziata, e la
presenza di prodotti gratuiti solidi (Zoho Invoice, il piano base di Invoice Ninja, il portale gratuito
dell'Agenzia delle Entrate) tiene bassa la disponibilità a pagare per la funzione nuda. Restano scoperti tre punti:
(a) la fascia fra il prodotto gratuito troppo scarno e il gestionale da 21–51 €/mese troppo pieno; (b) la
condivisione ordinata col commercialista, che nei prodotti italiani esiste ma è vissuta come laboriosa; (c) il
comando da chat, che nessuno dei prodotti esaminati offre. Il posizionamento difendibile di BillGrove non è
«fatturare meglio», è **essere il primo mattone di una suite** e l'unico posto dove il documento nasce già pronto
per la conformità, per l'incasso e per l'assistente conversazionale.

### 2.2 Prezzi praticati nel dominio

Unità di misura prevalente nel mercato italiano: **numero di documenti per anno**, con un secondo tetto sulle
anagrafiche (Fatture in Cloud: 100/400/800/3.000 documenti per anno e 500/1.000/2.500/5.000 anagrafiche). Il mercato
internazionale usa invece il **prezzo per utente al mese** (FreshBooks). Fasce osservate su **pagina ufficiale**:
4–51 €/mese in Italia (Fatture in Cloud), gratuito (Zoho Invoice). Fasce osservate su **fonti di confronto**, quindi
da verificare: 29,90 €/anno (Aruba), 15–40 €/mese (Danea), 14–23 $/mese (FreshBooks, Invoice Ninja).

Piano gratuito: presente e credibile (Zoho Invoice è gratuito senza limiti dichiarati; Wave offre fatture illimitate
a costo zero, [freshbooks.com — confronto app di fatturazione](https://www.freshbooks.com/hub/invoicing/best-invoice-app)).
Prova gratuita tipica: **31 giorni senza carta** in Fatture in Cloud — più lunga e più permissiva della prova di
14 giorni con carta che la piattaforma appgrove raccomanda in modo predefinito. È una differenza da mettere in conto
al momento di fissare il listino.

### 2.3 Obblighi normativi del settore

Il dominio è **fortemente normato**, e la normativa entra nel modello dati, non solo nell'interfaccia.

- **Numerazione progressiva e univoca** dei documenti emessi, per anno e per sezionale, senza salti né riuso: è
  l'obbligo che più spesso rompe le implementazioni ingenue, perché impone di assegnare il numero al momento
  dell'emissione e non della creazione della bozza
  ([cesystemweb.it — conservazione e numerazione](https://www.cesystemweb.it/news/conservazione-delle-fatture-elettroniche-entro-il-29-febbraio-2024-94)).
- **Conservazione a norma per dieci anni** dalla data del documento, in linea con l'articolo 2220 del Codice civile
  e l'articolo 39 del D.P.R. 633/1972; l'operazione di conservazione va fatta almeno una volta l'anno, entro tre
  mesi dal termine di presentazione della dichiarazione dei redditi
  ([centrofiscale.com](https://centrofiscale.com/conservazione-sostitutiva-fatture-elettroniche-2026/),
  [sibill.com](https://sibill.com/contabilita/fatturazione-elettronica-e-conservazione-guida-e-scadenze-2026/)).
  **Conseguenza diretta sul prodotto**: un documento emesso non si cancella e non si modifica — si rettifica con una
  nota di credito. E la richiesta di cancellazione di un interessato **non può** cancellare una fattura emessa
  (vedi §6).
- **Imposta di bollo** di 2 € sui documenti non soggetti a imposta sul valore aggiunto di importo superiore a
  77,47 €, assolta in modo virtuale; l'addebito arriva trimestralmente dal portale dell'amministrazione finanziaria
  ([agendadigitale.eu](https://www.agendadigitale.eu/documenti/fatturazione-elettronica/imposta-di-bollo-nelle-fatture-la-guida-per-tutte-le-situazioni/)).
  È un calcolo che il documento deve fare da sé, e una casella che deve comparire nel documento.
- **Fattura elettronica** obbligatoria in Italia per i titolari di partita IVA, compreso il regime forfettario
  ([leggeinchiaro.it](https://leggeinchiaro.it/forfettario-fatturazione-elettronica-obblighi/)). BillGrove non
  costruisce il canale di trasmissione: produce il documento in forma canonica e lo passa allo strato di conformità
  (§10).
- **Riforma europea ViDA**, adottata e in attuazione progressiva: dal 14 aprile 2025 gli Stati membri possono
  imporre la fattura elettronica senza deroga; dal 1º luglio 2030 scattano gli obblighi di comunicazione digitale
  sulle operazioni fra imprese fra Stati diversi; entro il 1º gennaio 2035 i sistemi nazionali devono allinearsi. Il
  formato di riferimento è la norma europea **EN 16931**
  ([Commissione europea — pagina ufficiale ViDA](https://taxation-customs.ec.europa.eu/taxation/vat/vat-digital-age-vida_en)).
  **Conseguenza di progetto**: il modello del documento nasce allineato a EN 16931, anche se il primo mercato è uno
  solo. Rifarlo dopo è una migrazione di dati.

### 2.4 Integrazioni attese dal cliente

In ordine di richiesta osservata:

1. **Canale di trasmissione della fattura elettronica** (in Italia il Sistema di Interscambio) — non è opzionale,
   è la ragione per cui il documento esiste. **Fornitore esterno che tratterebbe dati per nostro conto.**
2. **Conservazione a norma** presso un conservatore accreditato — decennale, obbligatoria. **Fornitore esterno.**
3. **Commercialista**: esportazione periodica o accesso in sola lettura. È la funzione che i clienti citano per
   prima quando spiegano perché hanno scelto un prodotto invece di un altro.
4. **Incassi**: collegamento del conto corrente e riconciliazione automatica dei movimenti, collegamento di
   pagamento sul documento
   ([pmi.it — riconciliazione bancaria in Fatture in Cloud](https://www.pmi.it/tecnologia/prodotti-e-servizi-ict/398408/riconciliazione-bancaria-conti-in-ordine-con-fatture-in-cloud.html)).
   **Fornitore esterno**, e con dati bancari: da trattare con cautela. In BillGrove resta **fuori ambito** in questa
   stesura (l'incasso si registra a mano, storia `0017`); la riconciliazione automatica appartiene a CashGrove (3).
5. **Posta elettronica** per l'invio del documento al cliente e per i solleciti. **Fornitore esterno.**
6. **Negozio in rete / punto cassa** per generare il documento dalla vendita: richiesto solo dai commercianti, fuori
   ambito qui (RetailGrove, 29).

### 2.5 Aspettative funzionali dei clienti micro e piccoli

Quello che chiedono: velocità («fare una fattura in meno di un minuto»), la ripetizione senza fatica (rifare la
fattura del mese scorso), il documento che si vede bene ed è riconoscibile, sapere chi non ha pagato, e la
tranquillità che il commercialista riceva ciò che gli serve. Le fonti distinguono nettamente due bisogni: il
libero professionista vuole «uno strumento semplice e veloce», la micro-impresa vuole «multiutenza, controllo di
entrate e uscite e integrazioni»
([flowerista.it](https://www.flowerista.it/piattaforme-di-fatturazione-elettronica-per-freelance-e-micro-imprese-quale-scegliere/)).

Quello che rifiutano: la contabilità completa (partita doppia, piano dei conti) — se compare, il prodotto diventa
«roba da commercialista» e viene abbandonato; la migrazione lunga; l'assistenza che non risponde. Su quest'ultimo
punto la lamentela ricorrente rilevata sul concorrente principale riguarda proprio il supporto, descritto come
sempre più affidato a risposte automatiche con attese di giorni sulle questioni complesse
([centrofiscale.com — recensione](https://centrofiscale.com/fatture-in-cloud-recensione-completa-2026/); è una fonte
di settore, non una raccolta di recensioni verificate: va letta come segnale, non come misura). Non ho trovato una
raccolta strutturata e citabile di recensioni negative su questo dominio: quanto sopra è il segnale più solido che
ho potuto documentare.

### 2.6 Fonti consultate

1. **Fatture in Cloud — pagina prezzi ufficiale** — https://www.fattureincloud.it/costo/ — i cinque piani con
   prezzo mensile e annuale, il tetto di documenti per anno e di anagrafiche, la prova di 31 giorni senza carta e le
   spese amministrative all'acquisto. È la fonte di prezzo più solida che ho: rilevata sulla pagina del venditore.
2. **Zoho Invoice — pagina ufficiale** — https://www.zoho.com/invoice/ — il prodotto è dichiarato gratuito per
   sempre e la pagina **non dichiara alcun limite** di fatture, clienti o utenti. Serve a misurare quanto è bassa la
   disponibilità a pagare per la sola emissione.
3. **Aruba vs Fatture in Cloud, confronto prezzi 2026** — https://www.softwaresemplice.it/blog/aruba-fatture-in-cloud-prezzi-a-confronto/1203 —
   la fascia bassa italiana (29,90 €/anno con conservazione decennale inclusa). Fonte di confronto, non ufficiale.
4. **Confronto gestionali 2026 (Fatture in Cloud, Danea, TeamSystem)** — https://www.srlonline.com/software-gestionali-2026-fatture-in-cloud-vs-danea-teamsystem-confronto-prezzi-funzioni/ —
   la fascia alta e il prodotto installato (Danea, 15–40 €/mese). Fonte di confronto.
5. **FreshBooks vs Invoice Ninja** — https://www.temperstack.com/versus/freshbooks-vs-invoice-ninja/ — il modello
   internazionale a prezzo per utente e la presenza di piani gratuiti. Fonte di confronto.
6. **Conservazione sostitutiva delle fatture elettroniche** — https://centrofiscale.com/conservazione-sostitutiva-fatture-elettroniche-2026/ —
   dieci anni di conservazione, riferimenti all'articolo 2220 del Codice civile e all'articolo 39 del D.P.R. 633/1972.
7. **Fatturazione elettronica e conservazione: scadenze** — https://sibill.com/contabilita/fatturazione-elettronica-e-conservazione-guida-e-scadenze-2026/ —
   la cadenza annuale della conservazione (entro tre mesi dal termine della dichiarazione).
8. **Imposta di bollo nelle fatture** — https://www.agendadigitale.eu/documenti/fatturazione-elettronica/imposta-di-bollo-nelle-fatture-la-guida-per-tutte-le-situazioni/ —
   soglia di 77,47 €, importo di 2 €, assolvimento virtuale e addebito trimestrale.
9. **Commissione europea — VAT in the Digital Age (ViDA)** — https://taxation-customs.ec.europa.eu/taxation/vat/vat-digital-age-vida_en —
   le date della riforma (14 aprile 2025, 1º gennaio 2027, 1º luglio 2028, 1º luglio 2030, 1º gennaio 2035) e il
   riferimento alla norma EN 16931. È l'unica fonte primaria istituzionale che ho usato.
10. **Riconciliazione bancaria in Fatture in Cloud** — https://www.pmi.it/tecnologia/prodotti-e-servizi-ict/398408/riconciliazione-bancaria-conti-in-ordine-con-fatture-in-cloud.html —
    l'integrazione col conto corrente come funzione attesa nel segmento.
11. **Piattaforme di fatturazione per freelance e micro-imprese** — https://www.flowerista.it/piattaforme-di-fatturazione-elettronica-per-freelance-e-micro-imprese-quale-scegliere/ —
    la distinzione fra il bisogno del libero professionista (semplicità) e quello della micro-impresa (multiutenza,
    integrazioni).
12. **Recensione del concorrente principale** — https://centrofiscale.com/fatture-in-cloud-recensione-completa-2026/ —
    la lamentela ricorrente sull'assistenza. Segnale, non misura.

### 2.7 Cosa NON sono riuscito a determinare

- **Prezzi ufficiali di Aruba e Danea** — non li ho letti sulla pagina del venditore ma su siti di confronto, che il
  catalogo stesso avverte invecchiare male (§8). Servirebbe una lettura diretta delle due pagine di listino prima di
  fissare il posizionamento.
- **Il limite reale del piano gratuito di Zoho Invoice** — le fonti secondarie sono in contraddizione (alcune
  parlano di 500 fatture l'anno, altre di fatture illimitate) e la pagina ufficiale non dichiara alcun limite. Non
  ho una risposta affidabile.
- **Il costo variabile di un canale di trasmissione della fattura elettronica per documento** — il catalogo cita
  ~0,18 €/fattura per InvoiceGrove (1), ma non ho verificato quel numero su un listino di fornitore. È il dato che
  decide se il piano base di BillGrove sta in piedi quando la trasmissione sarà inclusa: va rilevato prima.
- **Il costo della conservazione a norma decennale per documento** — non rilevato. Ha lo stesso peso del punto
  precedente.
- **Dati di abbandono e permanenza nel segmento** (quanti clienti restano dopo un anno) — non ho trovato fonti
  citabili. L'affermazione del catalogo sulla «altissima frequenza d'uso e stickiness» resta plausibile ma non
  documentata.
- **Una raccolta strutturata di recensioni negative** su questo dominio, che avrebbe reso il §2.5 molto più solido.

---

## 3. Varco d'identità — le risposte pronte per `new-application`

| Voce | Valore proposto | Motivazione |
|---|---|---|
| **Identificativo dell'app** (`app_id`) | `billing` | Rispetta `^[a-z][a-z0-9_]{0,30}$`. Descrive che cosa l'app è — il posto dove nasce il documento commerciale — e non il nome commerciale. **Due avvertenze da leggere prima di usarlo**: (a) nel repository esiste già l'app reale `fatture` (servizio `services/fatture`, listino `pricing/fatture.yaml`, utente singolo, metrica `fatture` a consumo mensile), che è una versione minima di questa stessa idea: `fatture` è quindi **occupato** e il rapporto fra le due app va deciso (§10, §11); (b) nel backoffice esiste già una sezione «Fatturazione» che riguarda l'abbonamento del cliente ad appgrove e vive sotto `/api/platform/v1/...`: la sovrapposizione è solo di parole, non di rotte, ma va tenuta a mente nei registri e nei testi dell'interfaccia. L'alternativa scartata è `invoicing`, che rischia invece di collidere con l'app 01 InvoiceGrove. |
| **Modello utente** | `multi` | Nella giornata tipo del cliente il documento lo prepara una persona dell'amministrazione e lo approva il titolare; il commercialista guarda senza toccare. Servono quindi più persone sullo stesso account, gli inviti e la tracciatura di chi ha fatto cosa — che un'app a utente singolo non ha. È anche la differenza principale rispetto all'app reale `fatture`, che è a utente singolo. La ricerca conferma che la multiutenza è il discrimine fra il bisogno del libero professionista e quello della micro-impresa (§2.5). |
| **Porta locale** | `8102` | Convenzione del kit: 8100 + numero di catalogo. Da confermare con `./dev.sh services` al momento dello scaffolding. |
| **Metrica di quota** | `documenti` | È la sola cosa che il piano limita: il numero di documenti **emessi** (fatture, note di credito, documenti di trasporto, ricevute). Cresce esattamente con il valore che il cliente riceve — chi emette di più fattura di più — ed è l'unità che tutto il mercato italiano usa (§2.2). I preventivi **non** consumano quota: sono lavoro commerciale, non documento fiscale, e farli pagare scoraggerebbe l'uso della parte dell'app che porta i clienti dentro. Gli utenti (posti) **non** sono la metrica: farli pagare a testa scoraggerebbe proprio la multiutenza che è il nostro elemento distintivo. |
| **Natura della metrica** | `flow` | «60 documenti al mese»: a marzo se ne possono emettere altri 60 comunque sia andato febbraio. È un consumo su una finestra che si azzera, non un tetto su ciò che esiste: i documenti già emessi restano lì per dieci anni (§2.3) e contarli come giacenza bloccherebbe il cliente per sempre entro il primo anno. Nota da portare all'escalation del listino: il mercato italiano ragiona per **documenti all'anno** (100/400/800/3.000), mentre l'app reale `fatture` usa una finestra **mensile**; le due letture non sono equivalenti per un'attività stagionale. |
| **Colore-categoria e icona** | `teal` · icona `receipt` (documento con bordo dentellato) | Deve essere lo stesso nel listino (`category`) e nel modulo frontend (`accentToken`). `green` è già dell'app reale `fatture` e `blue` del mini-CRM: `teal` tiene BillGrove nella famiglia fredda dell'amministrazione senza confondersi con nessuna delle due, e lascia `amber` e `red` alle app che segnalano scadenze e rischi (CashGrove, 3). |

---

## 4. Modello di dominio

**Entità principali**

| Entità | Cosa rappresenta | Attributi rilevanti | Contiene dati di persone? |
|---|---|---|---|
| `Customer` | Il cliente a cui si intesta il documento | denominazione, nome e cognome, partita IVA, codice fiscale, indirizzo, posta elettronica, telefono, codice destinatario, termini di pagamento predefiniti | **sì** — quando il cliente è una persona fisica (professionista o consumatore) tutti i campi sono suoi |
| `Product` | Voce di catalogo, bene o servizio | codice, descrizione, unità di misura, prezzo base, aliquota d'imposta predefinita | no |
| `PriceList` | Listino applicabile a un insieme di clienti | nome, validità, valuta, righe con prezzo o sconto per voce | no |
| `Document` | Il documento commerciale in tutte le sue forme | tipo (preventivo, fattura, nota di credito, documento di trasporto, ricevuta), sezionale, numero, data, stato, valuta, cambio, totali, imposta di bollo, riferimento al documento d'origine | **indirettamente** — porta copia congelata dei dati del cliente |
| `DocumentLine` | Riga del documento | descrizione, quantità, prezzo unitario, sconto, aliquota, totale di riga | no |
| `TaxSummary` | Riepilogo per aliquota | aliquota, imponibile, imposta, natura dell'operazione se esente | no |
| `Payment` | Incasso registrato su un documento | data, importo, mezzo, nota | no (il mezzo di pagamento è testuale, **non** si registrano coordinate bancarie) |
| `Numbering` | Contatore per sezionale e anno | sezionale, anno, ultimo numero assegnato | no |
| `DunningRule` | Regola di sollecito | soglia di giorni, testo del messaggio, cadenza | no |
| `RecurringPlan` | Modello di fattura ricorrente | cadenza, prossima emissione, documento modello, data di fine | no |

**Relazioni.** `Customer` uno-a-molti `Document`; `Document` uno-a-molti `DocumentLine` e `Payment`; `Document`
può riferirsi a un altro `Document` (preventivo → fattura, fattura → nota di credito); `Product` alimenta le righe
ma **non** le vincola (una riga può essere libera); `PriceList` si applica a `Customer` e determina il prezzo
proposto sulla riga.

**Macchina a stati del documento** — è la parte che tutte le storie devono rispettare:

```
                     ┌──────────► scaduto ────┐
bozza ──► emesso ──► inviato ──► pagato_parz ──► pagato ──► (archiviato)
  │         │                                        ▲
  │         └──► stornato (per nota di credito) ─────┘
  └──► annullato   (possibile SOLO da bozza)
```

Regole che ne discendono e che non sono negoziabili: il **numero si assegna al passaggio bozza → emesso**, mai
prima; da `emesso` in poi il documento **non si modifica e non si cancella**, si rettifica con una nota di credito;
il passaggio a `pagato` è la somma degli incassi registrati, non un interruttore.

Il **preventivo** ha una macchina a stati propria e più semplice: `bozza → inviato → accettato | rifiutato | scaduto`.
Non ha numerazione fiscale, non consuma quota, e da `accettato` può generare una fattura.

**Vincoli che discendono dalla piattaforma.** Ogni tabella porta `tenant_id`, chiave primaria UUID versione 7,
colonne di controllo (`created_at`, `updated_at`, `created_by`, `updated_by`) e cancellazione logica
(`deleted_at`); schema `app_billing`; nessuna chiave esterna verso altri schemi
([PRINCIPI-APPGROVE.md](../_kit/PRINCIPI-APPGROVE.md) §8). **Eccezione da discutere**: la cancellazione logica su
un documento emesso non basta a rispettare l'obbligo decennale, e non deve essere possibile — vedi §6 e il punto
aperto 3 del §11.

---

## 5. Proposta di listino

> ⚠️ **Proposta da confermare — fermata di escalation dello sviluppatore.** Prezzi, limiti dei piani e durata della
> prova gratuita non li fissa un agente. Quanto segue è una proposta motivata, da validare prima di scrivere il
> file `services/core/src/main/resources/pricing/billing.yaml`.

**Ragionamento.** I numeri nascono da tre vincoli. Primo: la fascia rilevata su pagina ufficiale in Italia va da
4 a 51 €/mese, con il grosso del segmento micro fra 12 e 21 €/mese (§2.2); stare sopra i 21 €/mese senza la
contabilità completa non è difendibile. Secondo: esiste un'offerta gratuita credibile (Zoho Invoice), quindi un
piano gratuito serve non per fare margine ma per non perdere il confronto al primo istante; deve però essere
abbastanza stretto da non bastare a un'attività vera. Terzo: il costo variabile per documento **oggi non esiste**,
perché in questa stesura BillGrove non trasmette e non conserva presso terzi (§2.4); il giorno in cui la
trasmissione o la conservazione decennale saranno incluse, un costo per documento comparirà e il piano base andrà
rivisto — è la stessa avvertenza che il catalogo fa per InvoiceGrove (1).

| Piano | Prezzo mensile | Prezzo annuale | Limite sulla metrica `documenti` | Prova gratuita | A chi è rivolto |
|---|---|---|---|---|---|
| `free` | — | — | 5 al mese | — | Chi vuole vedere come funziona e chi emette qualche documento l'anno. Abbastanza per capire il valore, non abbastanza per viverci |
| `pro` | 9 € | 90 € (= 10× il mensile, «due mesi in regalo») | 60 al mese | 14 giorni | Il libero professionista e la micro-impresa fino a 3 persone: il grosso del mercato |
| `business` | 19 € | 190 € | 400 al mese | 14 giorni | La piccola impresa con amministrazione dedicata, che emette ogni giorno |

**Note obbligate.**

- Tre piani, come raccomandato: aggiungerne è facile, toglierne quando qualcuno ci sta sopra è difficile.
- I **posti non si pagano** in nessun piano: è la scelta di posizionamento più importante della tabella, e va
  confermata esplicitamente. Rende BillGrove più conveniente di FreshBooks per un'attività di tre persone e
  incoraggia l'ingresso del commercialista come utente in sola lettura.
- Un limite lasciato vuoto significa **illimitato**, non zero: nessuno dei tre piani qui sopra lo usa.
- **Prova gratuita e piano gratuito insieme sono in parte ridondanti.** Qui la prova ha comunque senso perché serve
  a provare i **volumi** del piano superiore, non le funzioni. Va però notato che il concorrente principale offre
  31 giorni senza carta (§2.2) contro i 14 giorni con carta della raccomandazione di piattaforma: è una decisione di
  prodotto, non una decisione tecnica.
- **Costo effettivo dell'incasso**: il piano `pro` a 9 €/mese è sopra la soglia dei ~5 € oltre la quale la parte
  fissa per transazione diventa preoccupante, ma non di molto; spingere il ciclo annuale è il rimedio naturale.
- La finestra della metrica è proposta **mensile** per coerenza con l'app reale `fatture`, ma il mercato italiano
  vende a documenti per **anno**: la scelta della finestra è parte della fermata di escalation.
- I prezzi sono **immutabili una volta vivi**: un cambio si fa creando un prezzo nuovo, non modificando quello
  esistente.

---

## 6. Proposta di classificazione dei dati personali

> ⚠️ **Proposta da confermare — fermata di escalation dello sviluppatore.** Il manifesto dei dati
> (`docs/compliance/manifests/billing.yaml`) si compila **insieme** allo sviluppatore: «niente contratto, niente
> produzione». Un manifesto inventato è **peggio** di uno assente, perché sembra conformità ed è finzione.

**Categorie particolari (articolo 9): NO.** BillGrove non tratta dati sanitari, biometrici, genetici, opinioni
politiche, convinzioni religiose, orientamento sessuale o appartenenza sindacale. Il dominio non li richiede e
l'app **non deve** aprire campi che li facciano entrare. L'unico ingresso non presidiato è il testo libero (vedi
più sotto): va scoraggiato nell'interfaccia, non vietato a parole.

**Categorie trattate**

| Voce | Dove vive | Di chi è | Che dato è | A cosa serve | Perché è lecito | Per quanto si tiene |
|---|---|---|---|---|---|---|
| `customer.denominazione` | `customer.legal_name` | cliente dell'account (persona fisica o giuridica) | anagrafico | intestare il documento | esecuzione del contratto con l'account; obbligo di legge sul documento emesso | finché l'account la tiene in anagrafica; **10 anni** dalla data del documento per la copia congelata sul documento |
| `customer.nome_cognome` | `customer.first_name`, `customer.last_name` | cliente persona fisica | anagrafico | intestare il documento e rivolgersi alla persona giusta | esecuzione del contratto | come sopra |
| `customer.codice_fiscale` | `customer.tax_code` | cliente persona fisica | identificativo fiscale | obbligo di indicazione sul documento | obbligo di legge | 10 anni sulla copia del documento |
| `customer.indirizzo` | `customer.address_*` | cliente | anagrafico | obbligo di indicazione sul documento, consegna | obbligo di legge; esecuzione del contratto | come sopra |
| `customer.email` | `customer.email` | cliente e sua persona di riferimento | contatto | invio del documento e dei solleciti | esecuzione del contratto | finché l'account la tiene in anagrafica |
| `customer.telefono` | `customer.phone` | cliente | contatto | contatto amministrativo | esecuzione del contratto | come sopra |
| `customer.nota` | `customer.notes` | cliente | **testo libero** | annotazioni commerciali | legittimo interesse dell'account | come sopra |
| `document.dati_cliente_congelati` | `document.customer_snapshot` | cliente | anagrafico e fiscale | il documento deve riportare i dati **al momento dell'emissione**, non quelli attuali | obbligo di legge | **10 anni** dalla data del documento |
| `document.nota` | `document.notes`, `document_line.description` | cliente e terzi citati | **testo libero** | descrizione della prestazione | esecuzione del contratto | 10 anni |
| `payment.nota` | `payment.notes` | cliente | testo libero | tracciare come e quando è stato pagato | esecuzione del contratto | 10 anni |
| controllo | `*.created_by`, `*.updated_by` | utenti dell'account | identificativo interno | sapere chi ha fatto cosa | legittimo interesse dell'account | vita dell'account |

**🛑 Conflitto reale fra cancellazione e obbligo fiscale — da portare allo sviluppatore.** La piattaforma prescrive
che la cancellazione sia **fisica** e che la pseudonimizzazione non basti. Qui la legge dice il contrario per una
parte precisa dei dati: una fattura emessa va conservata dieci anni (§2.3), e cancellarne l'intestatario la rende
non conforme. La lettura che propongo — **da validare, non è una decisione dell'agente** — è che l'anagrafica del
cliente (tabella `customer`) sia cancellabile su richiesta, mentre la copia congelata dentro il documento emesso
resti, coperta dall'obbligo di legge, con una limitazione del trattamento e una riga di prova nel registro delle
purghe che spieghi **che cosa non è stato cancellato e perché**. Non l'ho decisa io: è un punto di conformità con
conseguenze legali.

**Esportazione e cancellazione.** Tabelle che contengono dati di persone e che devono comparire **tutte** sia in
`exportData` sia in `purgeData` del contratto dati dell'app (`BillingDataContract`): `customer`, `document`
(compresa `customer_snapshot`), `document_line`, `payment`, `recurring_plan` (porta il riferimento al cliente),
`dunning_log` (porta l'indirizzo a cui è stato mandato il sollecito). Dimenticarne una è il difetto di conformità
più probabile: `dunning_log` è quella che si dimentica.

**Testo libero.** Ci sono quattro campi liberi (nota del cliente, nota del documento, descrizione di riga, nota
dell'incasso). Sono un ingresso non presidiato per categorie particolari: un artigiano che scrive «riparazione
eseguita a domicilio perché il cliente è allettato» ha appena scritto un dato sanitario. L'app **non** fa rilevazione
di contenuto; il presidio, se servirà, è un tema trasversale di piattaforma. Quello che l'app deve fare subito è
**dirlo nell'interfaccia**, accanto al campo.

**Integrazioni esterne.** Nessuna in questa stesura tratta dati per nostro conto, perché la trasmissione, la
conservazione presso terzi e il collegamento bancario sono tutti fuori ambito (§2.4). Le tre che entreranno per
prime — canale di trasmissione della fattura elettronica, conservatore accreditato, servizio di posta per l'invio
dei documenti — sono **tutte** potenziali responsabili esterni del trattamento e vanno dichiarate quando arrivano:
le storie `0025` e `0026` lo prevedono espressamente.

**Classificazione della change.** Una app nuova introduce finalità nuove e categorie nuove: è un cambiamento
**sostanziale**. Lo confermo, e non solo per la regola generale: qui si aggiunge una base giuridica nuova per la
piattaforma (l'obbligo di legge sulla conservazione decennale) che non è né l'esecuzione del contratto né il
legittimo interesse.

---

## 7. Strumenti per il livello conversazionale

> Requisito trasversale del catalogo: ogni funzione dev'essere comandabile da una chat. Il livello conversazionale
> **non esiste ancora** nel repository (epica `12-ready-for-ai-mcp`, UC 0061-0066, scritta e non implementata):
> qui si dichiara il **contratto**, che vive dentro il servizio dell'app.
> Regola di sicurezza: **lettura libera, scrittura con bozza e conferma umana** per gli effetti irreversibili.

| Strumento | Firma | Effetto | Natura | Conferma umana |
|---|---|---|---|---|
| `elenca_documenti` | `(tipo?, stato?, cliente?, periodo?) → elenco minimizzato di documenti` | numero, data, cliente, totale, stato | lettura | no |
| `leggi_documento` | `(id) → documento completo` | il documento con le righe e gli incassi | lettura | no |
| `elenca_non_pagati` | `(giorni_di_ritardo?) → elenco con importo e giorni di scaduto` | chi non ha pagato | lettura | no |
| `riepilogo_incassi` | `(periodo) → incassato, da incassare, scaduto` | il numero che il titolare chiede più spesso | lettura | no |
| `cerca_cliente` | `(testo) → elenco di clienti minimizzato` | trova l'anagrafica | lettura | no |
| `crea_preventivo` | `(cliente, righe) → bozza di preventivo` | crea un preventivo in stato bozza | scrittura | **sì** |
| `crea_fattura` | `(cliente, righe, data?) → bozza di fattura` | crea una fattura **non emessa**: nessun numero assegnato | scrittura | **sì** |
| `converti_preventivo_in_fattura` | `(id_preventivo) → bozza di fattura` | ricopia le righe del preventivo accettato | scrittura | **sì** |
| `registra_incasso` | `(id_documento, importo, data, mezzo) → nuovo stato del documento` | cambia lo stato di pagamento | scrittura | **sì** |
| `emetti_documento` | `(id) → numero assegnato` | **irreversibile**: assegna il numero progressivo e congela il documento | scrittura irreversibile | **sì, obbligatoria** |
| `invia_documento` | `(id, destinatario) → esito dell'invio` | **irreversibile**: il documento esce verso il cliente | scrittura irreversibile | **sì, obbligatoria** |
| `emetti_nota_di_credito` | `(id_fattura, motivo, righe?) → bozza di nota di credito` | prepara la rettifica; l'emissione passa da `emetti_documento` | scrittura | **sì** |

**Lettura.** Le tre domande che il titolare fa davvero — «chi non mi ha pagato?», «quanto ho fatturato questo
mese?», «rifammi la fattura del mese scorso per il cliente X» — diventano tre frasi invece di tre schermate. È qui
che il livello conversazionale rende BillGrove più utile dei suoi concorrenti, nessuno dei quali (§2.1) lo offre. La
linea di sicurezza è netta e va tenuta: **la chat può preparare qualunque cosa, non può emettere né inviare nulla**.
Emissione e invio sono gli unici due atti che il documento non può disfare, e sono i due che chiedono un «sì»
umano esplicito.

---

## 8. Indice delle epiche e delle storie

### Epica 01 — Fondamenta

Alla fine dell'epica l'app esiste, è accesa, è vuota e si apre: servizio avviabile in locale, schema con il modello
del documento, modulo nella barra laterale in cinque lingue, quota che blocca a `429`, dati di prova.

| # | Storia | In una riga |
|---|---|---|
| [0001](01-fondamenta/0001-impianto-del-servizio.md) | Impianto del servizio | Istanza di scaffolding, rotte `/api/billing/v1/*`, definizione delle interfacce, infrastruttura dal modulo comune |
| [0002](01-fondamenta/0002-modello-dati-del-documento.md) | Modello dati del documento | Schema `app_billing`, tabelle del documento e delle righe, `tenant_id`, colonne di controllo |
| [0003](01-fondamenta/0003-guscio-del-modulo-frontend.md) | Guscio del modulo frontend | Manifesto, registrazione, sezioni, cinque lingue, tema chiaro e scuro |
| [0004](01-fondamenta/0004-abbonamento-e-quota-documenti.md) | Abbonamento e quota documenti | Proiezione dell'abilitazione, prenotazione della metrica `documenti`, `402` e `429` |
| [0005](01-fondamenta/0005-avvio-locale-e-dati-di-prova.md) | Avvio locale e dati di prova | Scoperta automatica del servizio, dati inventati per due account, avvio senza passi manuali |

### Epica 02 — Anagrafiche e catalogo

Ciò che serve **prima** di poter fare un documento: a chi lo intesto e che cosa ci scrivo dentro.

| # | Storia | In una riga |
|---|---|---|
| [0006](02-anagrafiche-e-catalogo/0006-anagrafica-clienti.md) | Anagrafica clienti | Creazione, modifica, ricerca e cancellazione del cliente, con i primi dati personali dichiarati |
| [0007](02-anagrafiche-e-catalogo/0007-catalogo-prodotti-e-servizi.md) | Catalogo prodotti e servizi | Voci riutilizzabili con prezzo, unità di misura e aliquota predefinita |
| [0008](02-anagrafiche-e-catalogo/0008-listini-e-sconti.md) | Listini e sconti | Prezzi differenziati per gruppo di clienti, applicati in automatico sulla riga |
| [0009](02-anagrafiche-e-catalogo/0009-importazione-anagrafiche-da-file.md) | Importazione delle anagrafiche da file | Caricamento da file tabellare con anteprima, controllo dei duplicati e rapporto degli scarti |

### Epica 03 — Preventivi e fatture

Il cuore dell'app: la catena preventivo → fattura, la numerazione, le imposte, la rettifica e la stampa.

| # | Storia | In una riga |
|---|---|---|
| [0010](03-preventivi-e-fatture/0010-preventivo.md) | Preventivo | Creazione e ciclo di vita del preventivo, senza numerazione fiscale e senza consumo di quota |
| [0011](03-preventivi-e-fatture/0011-conversione-del-preventivo-in-fattura.md) | Conversione del preventivo in fattura | Da preventivo accettato a bozza di fattura, con il legame fra i due documenti |
| [0012](03-preventivi-e-fatture/0012-emissione-e-numerazione.md) | Emissione e numerazione progressiva | Assegnazione del numero per sezionale e anno, senza salti, con il documento che si congela |
| [0013](03-preventivi-e-fatture/0013-imposte-del-documento.md) | Imposte del documento | Aliquote e riepilogo per aliquota, natura delle operazioni esenti, imposta di bollo |
| [0014](03-preventivi-e-fatture/0014-nota-di-credito.md) | Nota di credito | Rettifica totale o parziale di una fattura emessa, con il riferimento al documento d'origine |
| [0015](03-preventivi-e-fatture/0015-documento-di-trasporto.md) | Documento di trasporto | Documento di consegna con causale e dati del trasporto, convertibile in fattura differita |
| [0016](03-preventivi-e-fatture/0016-stampa-del-documento.md) | Stampa del documento | Rappresentazione stampabile del documento con il marchio dell'attività |

### Epica 04 — Incassi e solleciti

Dopo l'emissione: sapere chi ha pagato, ricordarlo a chi non l'ha fatto, e non riscrivere ogni mese la stessa fattura.

| # | Storia | In una riga |
|---|---|---|
| [0017](04-incassi-e-solleciti/0017-registrazione-degli-incassi.md) | Registrazione degli incassi | Incassi totali e parziali su un documento, con lo stato che si aggiorna da sé |
| [0018](04-incassi-e-solleciti/0018-scadenzario.md) | Scadenzario | Termini di pagamento, scadenze e vista di ciò che scade o è scaduto |
| [0019](04-incassi-e-solleciti/0019-solleciti-automatici.md) | Solleciti automatici | Regole di sollecito a soglie di giorni, con registro degli invii e possibilità di fermarli |
| [0020](04-incassi-e-solleciti/0020-fatture-ricorrenti.md) | Fatture ricorrenti | Modelli che generano una **bozza** alla scadenza, mai un documento emesso da solo |
| [0021](04-incassi-e-solleciti/0021-report-incassato-e-da-incassare.md) | Report incassato e da incassare | I tre numeri del titolare: fatturato, incassato, scaduto, per periodo e per cliente |

### Epica 05 — Conformità e apertura verso l'esterno

Ciò che rende il documento utilizzabile fuori dall'app: valuta, lingua, formato canonico, trasmissione,
conservazione e commercialista.

| # | Storia | In una riga |
|---|---|---|
| [0022](05-conformita-e-apertura-esterna/0022-documento-in-valuta-estera.md) | Documento in valuta estera | Valuta e cambio congelati sul documento, con i totali nella valuta di conto |
| [0023](05-conformita-e-apertura-esterna/0023-documento-nella-lingua-del-cliente.md) | Documento nella lingua del cliente | La stampa esce nella lingua del destinatario, indipendente dalla lingua dell'interfaccia |
| [0024](05-conformita-e-apertura-esterna/0024-esportazione-canonica-en-16931.md) | Esportazione canonica EN 16931 | Il documento in forma canonica europea: il confine esatto con InvoiceGrove (1) |
| [0025](05-conformita-e-apertura-esterna/0025-trasmissione-a-canale-esterno.md) | Trasmissione a un canale esterno | Consegna del documento a un adattatore esterno, con bozza, conferma umana e stato di ritorno |
| [0026](05-conformita-e-apertura-esterna/0026-conservazione-dei-documenti-emessi.md) | Conservazione dei documenti emessi | Registro immutabile decennale e blocco della cancellazione dei documenti emessi |
| [0027](05-conformita-e-apertura-esterna/0027-condivisione-con-il-commercialista.md) | Condivisione con il commercialista | Estrazione periodica dei documenti di un periodo, o accesso in sola lettura |

### Epica 06 — Esposizione conversazionale e prove end-to-end

Il contratto degli strumenti, la regola bozza-e-conferma, i varchi applicati alle chiamate dell'assistente e il
percorso end-to-end dell'app.

| # | Storia | In una riga |
|---|---|---|
| [0028](06-esposizione-conversazionale-e-prove/0028-strumenti-di-lettura.md) | Strumenti di lettura | Contratto versionato dei sei strumenti di sola lettura, con risultati minimizzati |
| [0029](06-esposizione-conversazionale-e-prove/0029-strumenti-di-scrittura-con-conferma.md) | Strumenti di scrittura con conferma | Bozza e conferma umana per creazione, emissione e invio |
| [0030](06-esposizione-conversazionale-e-prove/0030-varchi-sugli-strumenti.md) | Varchi sugli strumenti | Abilitazione, ruolo e quota applicati anche quando a chiamare è l'assistente |
| [0031](06-esposizione-conversazionale-e-prove/0031-percorso-end-to-end.md) | Percorso end-to-end dell'app | Il percorso `[J-BILLING]` dal preventivo all'incasso e le voci del registro di copertura |

**Totale**: 6 epiche, 31 storie.

---

## 9. Estensioni della console di amministrazione

Servono tre cose oltre lo standard: una vista sui **contatori di numerazione** per account (è la prima cosa da
guardare quando un cliente segnala un salto di numero), una vista sullo **stato del canale di trasmissione** e delle
lavorazioni programmate (solleciti e ricorrenti), e una **deroga temporanea alla quota documenti** per il primo mese
di chi migra da un altro prodotto. Va inoltre esposto il **blocco decennale** sui documenti emessi, perché chi
gestisce una richiesta di cancellazione deve poter vedere che cosa non è cancellabile e perché.

Dettaglio: [estensioni-admin.md](estensioni-admin.md).

---

## 10. Dipendenze e sinergie con altre app del catalogo

| App del catalogo | Rapporto | Cosa condividono |
|---|---|---|
| **01 — InvoiceGrove** | **alimenta** (rapporto strutturale) | Il catalogo (§6) dice che InvoiceGrove non è un prodotto autonomo difendibile e va progettato come **strato di conformità di BillGrove**. Qui questa indicazione è presa alla lettera: BillGrove è il **sistema di origine** del documento, lo produce in forma canonica EN 16931 (storia `0024`) e lo consegna a un canale (storia `0025`). Il ciclo di vita legale per giurisdizione — accettazione dall'autorità, rete a quattro angoli, doppio canale — **non** sta in BillGrove |
| **03 — CashGrove** | alimenta | I crediti scaduti: BillGrove sa chi non ha pagato (storie `0018`, `0021`), CashGrove ci costruisce sopra solleciti a più canali, punteggio di rischio e previsione di cassa. Il confine: BillGrove fa il sollecito **semplice** (storia `0019`), non il recupero crediti |
| **04 — LeadGrove** | condivide dati con | Anagrafica clienti: la stessa scheda serve alla pipeline di vendita e all'intestazione del documento |
| **06 — Preventivi** (catena del catalogo) | dipende da / si sovrappone a | Il catalogo colloca il preventivo nell'app 6 e la fattura nella 2. Qui il preventivo è dentro BillGrove (epica 03) perché senza di esso la catena si spezza e l'app perde il suo caso d'uso d'ingresso. **È una sovrapposizione dichiarata e da risolvere** (§11, punto 2) |
| **14 — Magazzino** | condivide dati con | Catalogo prodotti e listini; il documento di trasporto (storia `0015`) è il punto di contatto naturale |
| **App reale `fatture`** (nel repository, non nel catalogo) | **si sovrappone a** | È l'app #1 già costruita: fatturazione a utente singolo, metrica `fatture` a consumo mensile, categoria `green`. Fa un sottoinsieme di ciò che fa BillGrove |

**Lettura.** BillGrove ha senso da sola — è il caso d'uso più frequente e più immediato di tutta la suite, e la
scheda di catalogo la indica come **entry point naturale**. Ma il suo valore massimo è dentro la suite: tocca **tre
delle quattro entità condivise** individuate dal catalogo (anagrafica clienti, catalogo prodotti e listini, catena
del documento contabile) e ne è l'anello centrale. La catena preventivo → ordine → fattura → incasso, che il
catalogo indica come l'argomento di vendita più forte della suite, passa **tutta** da qui: chi entra da BillGrove
trova già dentro le anagrafiche di cui hanno bisogno LeadGrove, CashGrove e i verticali. È la ragione per cui questa
app va costruita **prima** delle sue vicine, e per cui le sue tabelle `customer` e `product` vanno progettate
pensando che un giorno saranno lette da altri (a eventi, mai con una interrogazione fra schemi).

**Sovrapposizioni da evitare.**

1. **Con l'app reale `fatture`**: sono due prodotti che fanno la stessa cosa a due livelli diversi. Costruire
   BillGrove senza decidere che cosa succede a `fatture` significa mantenere due basi di codice per lo stesso
   dominio. Punto aperto 1 del §11.
2. **Con l'app 06 del catalogo (Preventivi)**: il preventivo non può stare in due app. Punto aperto 2.
3. **Con CashGrove (3)**: il sollecito semplice è qui, il recupero crediti è là. Se BillGrove aggiunge il punteggio
   di rischio o la previsione di cassa, ha invaso il campo dell'altra.
4. **Con InvoiceGrove (1)**: la conversione di formato e il trasporto **non** sono di BillGrove. Se l'adattatore
   della storia `0025` comincia a conoscere le regole di validazione di una giurisdizione, il confine è stato rotto.

---

## 11. Rischi e punti aperti

| # | Punto | Perché è aperto | Chi lo chiude |
|---|---|---|---|
| 1 | Rapporto fra BillGrove e l'app reale `fatture` già presente nel repository: evoluzione della stessa app, oppure due prodotti distinti? | È una decisione di prodotto con conseguenze su identificativo, migrazione dei dati e listino. `fatture` è a utente singolo con metrica `fatture`; BillGrove è a più utenti con metrica `documenti` | sviluppatore |
| 2 | Il preventivo sta in BillGrove o nell'app 06 del catalogo? | Il catalogo lo mette nell'app 06, ma senza preventivo la catena di BillGrove si spezza e si perde il caso d'uso d'ingresso | sviluppatore, insieme all'agente dell'app 06 |
| 3 | Come si concilia la cancellazione fisica prescritta dalla piattaforma con l'obbligo di conservare dieci anni il documento emesso | È un punto di conformità con conseguenze legali: la proposta del §6 (anagrafica cancellabile, documento no, prova nel registro delle purghe) va validata | sviluppatore, con la revisione legale |
| 4 | Prezzi, limiti dei piani, finestra della metrica (mensile o annuale) e durata della prova | Fermata di escalation della piattaforma. Inoltre il concorrente principale offre 31 giorni senza carta contro i 14 con carta della raccomandazione | sviluppatore |
| 5 | Se e quando includere la trasmissione e la conservazione presso terzi nel prezzo | Fa comparire un costo variabile per documento oggi assente, e il §2.7 dice che quel costo non l'ho potuto rilevare | sviluppatore |
| 6 | La numerazione per **sezionali**: quanti ne servono al cliente tipo e chi li configura | Troppi sezionali confondono la micro-impresa, troppo pochi bloccano chi ne ha bisogno per legge. Non ho trovato un dato di mercato | storia `0012`, con conferma dello sviluppatore |
| 7 | Il livello conversazionale non esiste: le storie dell'epica 06 dichiarano un contratto che nessuno esegue ancora | Dipende dalle UC 0061-0066, scritte e non implementate | epica di piattaforma `12-ready-for-ai-mcp` |

**Rischi noti**

- **Mercato affollato e disponibilità a pagare bassa** — se BillGrove viene venduta come «un altro software di
  fatturazione», perde contro un prodotto gratuito. Lo attenua il posizionamento come primo mattone della suite e
  come unica app comandabile da chat.
- **La conformità cresce sotto i piedi** — le date ViDA (§2.3) spostano l'asticella fino al 2035 e i mandati
  nazionali sono già stati rinviati più volte (il catalogo lo avverte al §8). Lo attenua tenere il modello canonico
  EN 16931 dentro e la conformità per giurisdizione **fuori**, in InvoiceGrove.
- **Numerazione sbagliata = danno fiscale al cliente** — è il difetto più costoso possibile in questa app. Lo
  attenua assegnare il numero in una transazione con un contatore per sezionale e anno, e non permettere in nessun
  caso la modifica o la cancellazione di un documento emesso (storie `0012` e `0026`).
- **Il testo libero fa entrare dati che non vogliamo** (§6) — lo attenua l'avviso accanto al campo; non esiste un
  presidio tecnico in questa stesura.
- **Doppia costruzione con l'app reale `fatture`** — lo attenua solo la decisione del punto aperto 1, presa presto.

**Fuori dimensionamento**: no. Sei epiche (fascia 4-7), storie per epica fra 4 e 7 (fascia 4-8), 31 storie in tutto
(fascia 20-45).
