# 0027 — Esportazione dei rapporti

**Applicazione**: 13 — FlowGrove (`progetti`) · **Epica**: 05 — Avanzamento, margine e catena della suite
**Storia**: `0027` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0025`, `0026`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che deve mandare il consuntivo al commercialista e le scadenze nel proprio calendario
> voglio scaricare quello che vedo in un file
> così da non dover ricopiare niente e da poter usare i dati fuori dall'app.

**Contesto.** Due bisogni diversi ma della stessa natura: portare fuori i dati. Il primo è il consuntivo per chi
tiene i conti, e per una micro-impresa è la richiesta più frequente in assoluto. Il secondo è il calendario: il
collegamento vivo a Google o Microsoft 365 è escluso in questa stesura perché introdurrebbe un fornitore che
tratta dati per nostro conto ([application-description.md](../application-description.md) §2.4), ma il file di
calendario copre gran parte del bisogno senza nessuna di quelle conseguenze.

## 2. Requisiti funzionali

1. **RF-1** — Si esporta in formato tabellare il **consuntivo delle ore** di un periodo, con progetto, attività,
   data, durata, fatturabile sì/no, tariffa e importo.
2. **RF-2** — Si esporta in formato tabellare la **redditività** per progetto e per cliente, con le voci del
   conto della storia 0026.
3. **RF-3** — Ogni esportazione dichiara nel proprio contenuto i filtri applicati, la data di generazione e se il
   periodo è chiuso o ancora provvisorio: un file senza contesto viene interpretato male.
4. **RF-4** — Si esportano le **scadenze delle attività** in formato calendario, per un progetto o per la persona
   che chiede; il file contiene solo attività di chi ha diritto di vederle.
5. **RF-5** — Le esportazioni economiche sono riservate al ruolo `admin`; l'esportazione delle proprie ore e delle
   proprie scadenze è accessibile a ogni `member`.
6. **RF-6** — Le esportazioni grandi non bloccano l'interfaccia: si generano e si scaricano da un collegamento a
   scadenza breve.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni esportazione filtra per `tenant_id` dal token verificato; il
  collegamento di scarico è verificato contro l'account, e un collegamento non deve poter attraversare i confini.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `POST /api/progetti/v1/exports` (con il tipo e i filtri) e
  `GET /api/progetti/v1/exports/{id}`; errori in `application/problem+json`; OpenAPI aggiornata nello stesso
  commit.
- **RT-3 — Persistenza (§8).** Migrazione `V17__esportazioni.sql`: `export_job` con `tenant_id`, tipo, filtri,
  stato, riferimento al file, scadenza, colonne di controllo. I file generati si cancellano dopo la scadenza.
- **RT-4 — Modulo frontend (§3, §5).** Pulsanti di esportazione nelle schermate di avanzamento, redditività e
  foglio ore; stato della generazione visibile; solo token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Etichette dell'interfaccia in `en, it, fr, es, de`. **Le intestazioni delle
  colonne del file** seguono la lingua di chi esporta, e il formato dei numeri e delle date pure: un file scaricato
  in italiano e aperto come se fosse inglese produce numeri sbagliati, ed è un difetto ricorrente da evitare.
- **RT-6 — Varchi e quota (§6, §7).** Nessun consumo di quota. Filtro di ruolo come da RF-5. Con abbonamento
  `canceled` le esportazioni di rapporto rispondono `402`; l'esportazione dei **propri dati personali** resta
  invece sempre accessibile (storia 0030), e sono due cose diverse che non vanno confuse.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento: un file da scaricare non è una risposta utile
  in una chat. Le stesse informazioni sono già disponibili con `get_time_summary` e `get_project_margin`
  (storia 0028).
- **RT-8 — Dati personali (§10).** Le esportazioni contengono l'autore delle righe di ore: sono dati personali
  che escono dalla piattaforma per volontà del cliente titolare. Vanno dichiarate nel manifesto come trattamento
  (comunicazione al titolare), e il file va cancellato alla scadenza — non deve restare a giacere.
- **RT-9 — Registrazione eventi (§14).** «Esportazione richiesta», «esportazione scaricata», «file scaduto e
  cancellato» con `tenant_id`, `app_id`, `user_id`, tipo e numero di righe; mai il contenuto.

## 4. Criteri di accettazione

**CA-1 — Consuntivo delle ore**
- **Dato** un periodo con 340 righe
- **Quando** si esporta il consuntivo
- **Allora** il file contiene 340 righe più l'intestazione con filtri, data di generazione e stato del periodo

**CA-2 — Formato per lingua**
- **Dato** un utente che usa l'italiano e uno che usa l'inglese
- **Quando** entrambi esportano lo stesso consuntivo
- **Allora** i due file hanno intestazioni tradotte e numeri formattati secondo la rispettiva lingua

**CA-3 — Calendario**
- **Dato** una persona con cinque attività con scadenza
- **Quando** esporta le proprie scadenze in formato calendario
- **Allora** il file contiene cinque voci e nessuna attività di altri

**CA-4 — Ruolo insufficiente**
- **Dato** un utente con ruolo `member`
- **Quando** chiede l'esportazione della redditività
- **Allora** riceve `403`, e continua a poter esportare le proprie ore

**CA-5 — Collegamento scaduto**
- **Dato** un'esportazione generata e scaduta
- **Quando** si usa il collegamento di scarico
- **Allora** non funziona più e il file non è più presente nell'archivio

**CA-6 — Isolamento fra account**
- **Dato** due account `A` e `B`
- **Quando** un utente di `A` usa un collegamento di scarico di `B`
- **Allora** riceve `404`

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (aree `backend`, `frontend`);
- [ ] prove di **unità** sulla formattazione per lingua e sul formato calendario, e di **integrazione** sulla
      generazione e la scadenza;
- [ ] prova di **isolamento fra account** sui collegamenti di scarico;
- [ ] **prova end-to-end**: rimando — `[J-PROGETTI]` non scarica file, che sono fragili da verificare in un
      percorso automatico; motivo e proprietario registrati;
- [ ] **traduzioni** presenti in tutte e cinque le lingue, intestazioni dei file comprese;
- [ ] **manifesto dei dati** aggiornato: le esportazioni sono un trattamento, e i file hanno una scadenza;
- [ ] **registro delle decisioni** compilato, con annotata la distinzione fra esportazione di rapporto ed
      esportazione dei dati dell'interessato;
- [ ] controllo automatico di **accessibilità** verde sui comandi di esportazione;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storia `0025` | L'avanzamento è uno dei rapporti |
| Storia `0026` | La redditività è l'altro |
| Storia `0020` | Lo stato del periodo determina se il rapporto è provvisorio |

## 7. Fuori ambito

- il collegamento vivo al calendario (Google, Microsoft 365): escluso in questa stesura, punto aperto
  ([application-description.md](../application-description.md) §11.3);
- l'invio automatico del rapporto per posta elettronica: nessun invio verso l'esterno;
- i rapporti configurabili dall'utente: fuori perimetro, è configurazione.

## 8. Punti aperti

- **Durata di vita dei file esportati**: proposta breve (poche ore), ma il valore va confermato insieme alle altre
  durate di conservazione ([application-description.md](../application-description.md) §11.5).
