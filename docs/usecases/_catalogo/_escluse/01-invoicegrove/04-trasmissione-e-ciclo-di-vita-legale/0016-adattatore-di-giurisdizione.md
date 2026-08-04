# 0016 — Adattatore di giurisdizione

**Applicazione**: 01 — InvoiceGrove (`einvoicing`) · **Epica**: 04 — Trasmissione e ciclo di vita legale
**Storia**: `0016` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0006`, `0011`, `0014`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come sviluppatore che aggiungerà la Polonia fra un anno
> voglio un contratto unico che dica cosa deve fornire una giurisdizione — regole, serializzatore, canale,
> macchina a stati — e un banco di prova che lo verifichi
> così da poter aggiungere un paese scrivendo una realizzazione, non modificando il motore.

**Contesto.** È la storia che traduce in codice la nota architetturale del catalogo: «serve un modello canonico
allineato a EN 16931 e un adapter per giurisdizione che incapsuli regole di validazione, serializzatore, canale di
trasporto **e macchina a stati del ciclo di vita**». L'ultima parte è quella che si dimentica: chi fa l'adattatore
pensando che sia «solo un serializzatore» scopre dopo che il Belgio non ha lo stato «accettato dall'autorità» e
che la Francia ne ha una fila in più.

Va fatta **prima** delle due realizzazioni concrete (storie `0017` e `0018`), perché sono loro che devono
adattarsi al contratto, non il contrario. Il rischio opposto — scrivere prima l'Italia e poi «astrarre» — produce
un contratto a forma di Italia.

## 2. Requisiti funzionali

1. **RF-1** — Esiste il contratto `JurisdictionAdapter` con quattro capacità dichiarate: insieme delle regole di
   validazione, serializzazione del documento canonico nel formato ufficiale, consegna sul canale, e mappatura
   degli esiti del canale sulla macchina a stati.
2. **RF-2** — Il contratto dichiara la **famiglia** del ciclo di vita (`clearance`, `four_corner`, `five_corner`)
   e, per essa, gli stati ammessi e le transizioni consentite.
3. **RF-3** — La selezione dell'adattatore avviene **dal registro delle giurisdizioni**, per codice paese e data,
   mai da una condizione scritta nel codice del motore.
4. **RF-4** — Esiste una **suite di conformità del contratto**: un insieme di prove che ogni realizzazione deve
   superare, uguale per tutte, con documenti di prova comuni.
5. **RF-5** — Una giurisdizione senza realizzazione registrata risponde con un errore che dice quale paese manca e
   che l'archiviazione resta possibile — mai con un errore generico.
6. **RF-6** — Il serializzatore produce l'artefatto ufficiale e la sua **impronta crittografica**, che accompagna
   il documento da lì in poi.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** L'adattatore riceve un documento già filtrato per `tenant_id`; non
  interroga il database da sé. È un vincolo di progetto: un adattatore che facesse letture proprie sarebbe il
  punto in cui l'isolamento si perde.
- **RT-2 — Interfaccia di programmazione (§2).** Nessuna rotta pubblica nuova: il contratto è interno al servizio.
  L'errore «giurisdizione non supportata» esce in `application/problem+json` con un tipo di problema stabile,
  perché il livello conversazionale deve saperlo riconoscere.
- **RT-3 — Persistenza (§8).** Migrazione `V13__serialized_artifact.sql`: tabella dell'artefatto serializzato con
  formato, impronta crittografica, istante e riferimento al documento; `tenant_id`, chiave UUID versione 7,
  colonne di controllo. **Il contenuto dell'artefatto contiene dati personali**: vedi RT-8.
- **RT-4 — Modulo frontend (§3, §5).** Nessuna schermata nuova; sulla scheda del documento compare l'indicazione
  del formato di destinazione e la possibilità di scaricare l'anteprima dell'artefatto.
- **RT-5 — Cinque lingue (§4).** Il messaggio «questo paese non è ancora coperto» e le etichette dei formati dallo
  spazio-nomi `einvoicing`, presenti in `en, it, fr, es, de`.
- **RT-6 — Varchi e quota (§6, §7).** La sola serializzazione **non** consuma la metrica `documenti`: il consumo
  è alla consegna. L'anteprima dell'artefatto è quindi disponibile anche in modalità prova, ed è ciò che rende la
  prova utile senza produrre effetti verso l'esterno.
- **RT-7 — Esposizione conversazionale (§12).** Strumento dichiarato:
  `preview_official_format(id) → artefatto serializzato e impronta`, marcato **lettura** perché non consegna
  nulla; nessuna conferma. La consegna è un altro strumento con un altro varco (storia `0029`). Contratto dentro
  il servizio; server conversazionale non implementato (UC 0061-0063).
- **RT-8 — Dati personali (§10).** **Sì.** L'artefatto serializzato è una copia integrale del documento, quindi
  contiene tutti i dati personali della controparte: la tabella va dichiarata nel manifesto in italiano e inglese
  e inserita in `exportData` e `purgeData`. È una delle tabelle che si dimenticano perché «è solo un file
  generato».
- **RT-9 — Registrazione eventi (§14).** Gli eventi `artefatto serializzato`, `giurisdizione non supportata` sono
  registrati con `tenant_id`, `app_id`, `user_id`, identificativo di correlazione, codice paese, formato e
  impronta — **mai** il contenuto dell'artefatto.

## 4. Criteri di accettazione

**CA-1 — Selezione dal registro**
- **Dato** un documento con giurisdizione «Italia» e data odierna
- **Quando** il motore cerca l'adattatore
- **Allora** lo ottiene dal registro delle giurisdizioni, e nel codice del motore non esiste alcuna condizione sul
  codice paese

**CA-2 — Suite di conformità**
- **Dato** una realizzazione di adattatore registrata
- **Quando** si esegue la suite di conformità del contratto
- **Allora** la realizzazione supera tutte le prove comuni, o la suite è rossa

**CA-3 — Famiglie diverse, stati diversi**
- **Dato** un adattatore di famiglia `four_corner`
- **Quando** si chiede se lo stato «accettato dall'autorità» è ammesso
- **Allora** la risposta è no, e un tentativo di portare un documento di quella famiglia in quello stato è
  rifiutato

**CA-4 — Giurisdizione non supportata**
- **Dato** un documento in una giurisdizione senza realizzazione registrata
- **Quando** si tenta di serializzarlo
- **Allora** l'errore dice quale paese manca e che l'archiviazione resta possibile

**CA-5 — Impronta dell'artefatto**
- **Dato** un documento serializzato due volte senza modifiche
- **Quando** si confrontano le impronte
- **Allora** sono identiche: la serializzazione è deterministica

**CA-6 — Isolamento fra account**
- **Dato** due account con documenti propri
- **Quando** un utente dell'uno chiede l'anteprima dell'artefatto di un documento dell'altro
- **Allora** riceve `404`

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (backend, compliance);
- [ ] **suite di conformità del contratto** scritta e verde su almeno una realizzazione fittizia di prova;
- [ ] prove di **unità** sulla selezione dell'adattatore e sul determinismo della serializzazione, di
      **integrazione** sull'anteprima;
- [ ] prova di **isolamento fra account** sull'artefatto serializzato;
- [ ] **prova end-to-end**: *rimando* — la superficie qui è minima (anteprima); il percorso `[J-EINVOICING]`
      (storia `0030`) attraverserà la serializzazione implicitamente nella trasmissione;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato: l'artefatto serializzato dichiarato, presente in esportazione e
      cancellazione;
- [ ] **registro delle decisioni** compilato, con la scelta «contratto prima delle realizzazioni» e il motivo;
- [ ] contratto degli **strumenti conversazionali** dichiarato per `preview_official_format`.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| `0006` | La selezione dell'adattatore avviene dal registro delle giurisdizioni |
| `0011` | Serve il documento canonico da serializzare |
| `0014` | L'insieme delle regole è una delle quattro capacità del contratto |

## 7. Fuori ambito

- Le realizzazioni concrete: storia `0017` (Italia, a liberatoria) e `0018` (rete a quattro angoli).
- La **famiglia a cinque angoli** (Francia): il contratto la prevede come valore dichiarabile, ma nessuna
  realizzazione la implementa e la suite di conformità non la copre. È una scelta consapevole, non una svista.
- L'archiviazione dell'artefatto a norma: epica 05.

## 8. Punti aperti

- **La famiglia a cinque angoli è dichiarata ma non provata.** Un contratto che prevede una famiglia senza
  nessuna realizzazione che la eserciti è un contratto di cui non si sa se regge. È il rischio noto di questa
  storia: si accetta perché l'alternativa — non prevederla — costerebbe una riscrittura, ma va detto.
- **Se un giorno servisse la Francia**, la scelta fra «immatricolarsi come piattaforma accreditata» (che richiede
  certificazione ISO 27001, descrizione dell'applicazione §2.3) e «appoggiarsi a una piattaforma accreditata di
  terzi» è una decisione aziendale, non un dettaglio dell'adattatore.
