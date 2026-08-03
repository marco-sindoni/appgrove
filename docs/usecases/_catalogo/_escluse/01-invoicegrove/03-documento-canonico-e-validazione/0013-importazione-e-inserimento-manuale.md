# 0013 — Importazione e inserimento manuale

**Applicazione**: 01 — InvoiceGrove (`einvoicing`) · **Epica**: 03 — Documento canonico e validazione
**Storia**: `0013` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0011`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come responsabile amministrativo che tiene la fatturazione su un gestionale che non è della suite
> voglio poter caricare i miei documenti da un file, o inserirne uno a mano quando serve
> così da poter usare InvoiceGrove anche senza cambiare il modo in cui produco le fatture.

**Contesto.** La porta principale dell'app è l'ingresso a eventi (storia `0012`), e resta tale. Ma un'app di
conformità che si possa usare **solo** insieme a una specifica app di fatturazione taglia fuori chiunque abbia già
un gestionale — che nel segmento micro è la maggioranza. Questa storia costruisce la porta di servizio: dichiarata
**minima e di ripiego**, e tenuta tale di proposito. Se l'inserimento manuale crescesse fino a diventare comodo,
avremmo costruito una seconda app di fatturazione dentro questa, che è esattamente ciò che la nota del catalogo §6
mette in guardia dal fare.

## 2. Requisiti funzionali

1. **RF-1** — Si può caricare un file contenente uno o più documenti in un formato tabellare dichiarato, e
   ottenerne dei `CanonicalDocument` in stato `bozza`.
2. **RF-2** — Prima di creare qualcosa, l'app mostra un'**anteprima**: quante righe ha letto, quanti documenti
   riconosciuto, quali righe non è riuscita a leggere e perché. Nulla viene creato finché l'utente non conferma.
3. **RF-3** — L'importazione è **idempotente sul numero di documento**: ricaricare lo stesso file non crea
   duplicati, segnala i documenti già presenti.
4. **RF-4** — Esiste un modulo di inserimento manuale con i soli campi indispensabili: controparte, data, numero,
   righe (descrizione, quantità, prezzo, aliquota, natura), riferimenti.
5. **RF-5** — Il modulo manuale non ha listini, prodotti, sconti a cascata, né ricorrenze: se servono, la fattura
   va fatta nell'app di fatturazione.
6. **RF-6** — Entrambe le vie producono documenti **indistinguibili** da quelli arrivati per evento, salvo il
   campo «origine».

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni documento creato porta il `tenant_id` preso dal token verificato;
  un `tenant_id` presente nel file caricato viene **ignorato**. Prova di isolamento su due account.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `POST /api/einvoicing/v1/documents/import/preview`,
  `POST /api/einvoicing/v1/documents/import/commit` e `POST /api/einvoicing/v1/documents`; corpo validato in modo
  dichiarativo; errori in `application/problem+json` con l'indicazione della riga e della colonna in errore;
  definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V10__import_batch.sql`: tabella della sessione di importazione con
  esito e conteggi, `tenant_id`, chiave UUID versione 7, colonne di controllo, cancellazione logica. Il file
  caricato **non** si conserva oltre la sessione: se ne conserva l'esito, non il contenuto.
- **RT-4 — Modulo frontend (§3, §5).** Schermata di caricamento con anteprima e conferma, e modulo di inserimento
  manuale con React Hook Form e Zod. Solo token del sistema di design; tema chiaro e scuro; l'avviso di quota
  compare **prima** del modulo, non dopo il salvataggio.
- **RT-5 — Cinque lingue (§4).** Etichette, aiuti e messaggi di errore per riga dallo spazio-nomi `einvoicing`,
  presenti in `en, it, fr, es, de`.
- **RT-6 — Varchi e quota (§6, §7).** Coerentemente con la storia `0012`, la creazione **non** consuma la metrica
  `documenti`: il consumo è alla trasmissione. L'importazione è però protetta da un limite sul numero di documenti
  per sessione, per evitare che un file da centomila righe blocchi il servizio. Ruolo `member` sufficiente.
- **RT-7 — Esposizione conversazionale (§12).** Strumenti dichiarati: `create_document(emittente, controparte,
  righe, giurisdizione) → bozza` e `import_document(riferimento) → bozza`, entrambi **scrittura**, entrambi
  producono una **bozza** e richiedono **conferma umana** prima di qualunque effetto ulteriore. Nessuno dei due
  trasmette: la trasmissione è un altro strumento con un altro varco (storia `0029`). Contratto dentro il
  servizio; server conversazionale non implementato (UC 0061-0063).
- **RT-8 — Dati personali (§10).** **Sì.** Il file caricato contiene dati personali di controparti. Voci del
  manifesto già presenti dalle storie `0008` e `0011`; va dichiarata la **provenienza «importazione»** e va
  motivato perché il file non si conserva. La tabella della sessione di importazione, se conserva estratti delle
  righe in errore, va in `exportData` e `purgeData`: se non li conserva, va detto.
- **RT-9 — Registrazione eventi (§14).** Gli eventi `importazione avviata`, `anteprima prodotta`,
  `importazione confermata`, `riga non leggibile` sono registrati con `tenant_id`, `app_id`, `user_id`,
  identificativo di correlazione e conteggi, **senza** il contenuto delle righe.

## 4. Criteri di accettazione

**CA-1 — Anteprima prima di creare**
- **Dato** un file con dodici documenti di cui due malformati
- **Quando** si carica il file
- **Allora** l'anteprima dice «dieci documenti leggibili, due righe in errore» con il motivo per ciascuna, e
  **nulla è ancora stato creato**

**CA-2 — Conferma**
- **Dato** l'anteprima precedente
- **Quando** l'utente conferma
- **Allora** vengono creati dieci documenti in stato `bozza` con origine «importazione», e i due in errore no

**CA-3 — Ricarico dello stesso file**
- **Dato** lo stesso file caricato una seconda volta
- **Quando** si conferma
- **Allora** non viene creato alcun duplicato e l'esito segnala dieci documenti già presenti

**CA-4 — Inserimento manuale**
- **Dato** un utente con ruolo `member`
- **Quando** compila il modulo con controparte, data, numero e due righe
- **Allora** il documento è creato in stato `bozza` con origine «manuale»

**CA-5 — Il `tenant_id` nel file viene ignorato**
- **Dato** un file che contiene una colonna con l'identificativo di un altro account
- **Quando** lo si importa
- **Allora** i documenti nascono nell'account dell'utente che ha caricato, non nell'altro

**CA-6 — File troppo grande**
- **Dato** un file oltre il limite di documenti per sessione
- **Quando** lo si carica
- **Allora** riceve un errore che dice il limite e come spezzare il file, e nulla viene creato

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (backend, frontend, compliance);
- [ ] prove di **unità** sulla lettura del file e sulla deduplica per numero, di **integrazione** sul percorso
      anteprima → conferma;
- [ ] prova di **isolamento fra account**, compreso il caso del `tenant_id` presente nel file;
- [ ] **prova end-to-end**: *coprire ora* — il percorso `[J-EINVOICING]` (storia `0030`) includerà l'inserimento
      manuale di un documento, che è la via più corta per arrivare a una trasmissione nella prova;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato con la provenienza e con la sorte del file caricato;
- [ ] controllo automatico di **accessibilità** sul modulo di inserimento e sulla schermata di anteprima;
- [ ] **registro delle decisioni** compilato, con la scelta «modulo manuale deliberatamente minimo» e il motivo;
- [ ] contratto degli **strumenti conversazionali** dichiarato per `create_document` e `import_document`.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| `0011` | Serve il modello canonico e il controllo di coerenza dei totali |
| `0008` | Le controparti devono esistere o essere create in bozza durante l'importazione |

## 7. Fuori ambito

- **Listini, catalogo prodotti, fatture ricorrenti, sconti, solleciti**: sono dell'app di fatturazione. Metterli
  qui significherebbe costruire due volte la stessa cosa (descrizione dell'applicazione §10).
- La lettura di un file già in formato ufficiale di una giurisdizione (per esempio un XML italiano): rimandata
  alla storia `0021`, che porta la ricezione dei documenti passivi e quindi il lettore di quei formati.
- La conservazione del file caricato: deliberatamente esclusa, per non creare un secondo archivio non governato.

## 8. Punti aperti

- **Quale formato tabellare accettare.** La proposta è un formato dichiarato e documentato dall'app, non «quello
  che esce dal gestionale del cliente»: supportare i formati altrui non ha fine. Va confermato, perché è una
  scelta che sposta lavoro sul cliente.
- **Quanto minimo debba restare il modulo manuale** è direzione di prodotto. La pressione a farlo crescere sarà
  costante: la decisione di resistere va presa una volta e scritta, non rinegoziata a ogni richiesta.
