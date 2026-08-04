# 0022 — Promemoria automatici

**Applicazione**: 07 — BookGrove (`prenotazioni`) · **Epica**: 05 — Promemoria, acconti e mancate presentazioni
**Storia**: `0022` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0017`, `0021`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare
> voglio che il programma ricordi l'appuntamento ai miei clienti al posto mio, sempre, senza che io me ne debba
> ricordare
> così da smettere di perdere ore di lavoro per gente che semplicemente si era dimenticata.

**Contesto.** È la funzione che giustifica il canone. Gli studi disponibili misurano riduzioni delle mancate
presentazioni intorno al 38 % con il solo promemoria per messaggio, e cali del 25-40 % con più contatti (§2.5
della descrizione); le fonti sono sanitarie, ma l'ordine di grandezza regge. La storia consegna il **motore**:
quando i messaggi partono, cosa contengono, in che lingua, cosa succede se falliscono. Il canale di messaggistica
arriva con la storia `0023`; qui il canale è la posta elettronica, che non ha costo per messaggio e non richiede
modelli approvati.

## 2. Requisiti funzionali

1. **RF-1** — Alla conferma di una prenotazione parte subito il messaggio di **conferma**, con riepilogo,
   politica di disdetta e collegamento per gestirla.
2. **RF-2** — L'attività configura fino a tre promemoria per appuntamento, ciascuno con il proprio anticipo (per
   esempio 72 ore, 24 ore, 2 ore), attivabili singolarmente.
3. **RF-3** — Ogni messaggio è scritto nella **lingua preferita del cliente** fra `en, it, fr, es, de`.
4. **RF-4** — I messaggi si vedono in agenda con il loro stato: programmato, inviato, consegnato per quanto il
   canale lo sappia dire, fallito con il motivo in parole comprensibili.
5. **RF-5** — Un messaggio fallito **ricade** sul canale successivo disponibile; se non ce n'è, l'appuntamento è
   segnalato all'attività come «non è stato possibile avvisare».
6. **RF-6** — Disdetta e spostamento generano il proprio messaggio, e i promemoria di una prenotazione non più
   viva **non partono**: è l'errore più imbarazzante possibile e va escluso per costruzione.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** La coda dei messaggi è per `tenant_id`; nessuna lavorazione attraversa
  gli account.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte
  `GET|PUT /api/prenotazioni/v1/impostazioni/promemoria` e `GET /api/prenotazioni/v1/prenotazioni/{id}/messaggi`;
  errori in `problem+json`; OpenAPI aggiornata.
- **RT-3 — Persistenza (§8).** Migrazione `V14__promemoria.sql`: tabella `promemoria` con `tenant_id`, UUID
  versione 7, colonne di controllo, prenotazione, canale, momento previsto, stato, esito e momento di consegna.
- **RT-4 — Idempotenza.** Ogni messaggio ha una chiave che ne impedisce il doppio invio anche se la lavorazione
  viene ripetuta: un promemoria doppio è un difetto visibile al cliente finale.
- **RT-5 — Minimizzazione del contenuto.** Il testo predefinito contiene data, ora, nome dell'attività e
  collegamento; **non** contiene il nome del servizio, perché il servizio può rivelare informazioni delicate
  (§6 della descrizione) e il messaggio viaggia su canali che l'attività non controlla. L'attività può
  aggiungerlo, con un avviso esplicito su cosa comporta.
- **RT-6 — Modulo frontend (§3, §5).** Impostazioni dei promemoria con anteprima del testo nelle cinque lingue;
  stato dei messaggi visibile dal blocco in agenda; solo token del sistema di design; tema chiaro e scuro.
- **RT-7 — Cinque lingue (§4).** Interfaccia in `en, it, fr, es, de`; i **testi dei messaggi** sono anch'essi
  nelle cinque lingue e seguono la lingua del destinatario, non quella dell'operatore.
- **RT-8 — Dati personali (§10).** Voci nuove nel manifesto in italiano e inglese: destinatario ed esito del
  messaggio, con finalità «prova dell'avviso» e durata proposta 12 mesi; campi annotati `@PersonalData`; tabella
  `promemoria` in `exportData` e `purgeData`. **Il fornitore di posta elettronica riceve dati personali di una
  categoria di interessati nuova per la piattaforma — i clienti dei nostri clienti — e va segnalato nell'elenco
  dei fornitori e nell'informativa.**
- **RT-9 — Registrazione eventi (§14).** `promemoria programmato`, `promemoria inviato`, `invio fallito` con
  `tenant_id`, `app_id`, correlazione e canale — **mai il destinatario né il testo**.
- **RT-10 — Esposizione conversazionale (§12).** Base dello strumento `invia_promemoria`, dichiarato nella storia
  `0032`: **scrittura irreversibile** verso l'esterno, quindi bozza e conferma umana obbligatorie.

## 4. Criteri di accettazione

**CA-1 — Conferma immediata**
- **Dato** una prenotazione appena confermata · **Quando** si guarda l'elenco dei messaggi · **Allora** la
  conferma risulta inviata, con il collegamento per gestire la prenotazione

**CA-2 — Promemoria all'anticipo giusto**
- **Dato** promemoria a 24 ore attivo e un appuntamento fra 30 ore · **Quando** passa il tempo · **Allora** il
  messaggio parte a 24 ore dall'appuntamento, una volta sola

**CA-3 — Lingua del cliente**
- **Dato** un cliente con lingua preferita tedesca in un'attività italiana · **Quando** riceve il promemoria
- **Allora** è in tedesco

**CA-4 — Prenotazione disdetta**
- **Dato** un appuntamento disdetto ieri con un promemoria programmato per oggi · **Quando** arriva il momento
- **Allora** il promemoria **non** parte

**CA-5 — Fallimento e ricaduta**
- **Dato** un indirizzo di posta inesistente · **Quando** l'invio fallisce · **Allora** l'esito è visibile in
  agenda con un motivo comprensibile, si prova il canale successivo, e se non ce n'è l'appuntamento è segnalato

**CA-6 — Nessun doppio invio**
- **Dato** la lavorazione ripetuta due volte · **Quando** si guardano i messaggi · **Allora** ce n'è uno solo

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend`, `frontend` e `compliance`);
- [ ] prove di **unità** sulla programmazione e sull'idempotenza, e di **integrazione** con fornitore simulato;
- [ ] prova di **isolamento fra account** sulla coda dei messaggi;
- [ ] **prova end-to-end**: **coperta ora** — la conferma è un passo del percorso `[J-BOOKGROVE-PUB]` della storia
      `0034`, con il registro [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml);
- [ ] **traduzioni** dell'interfaccia **e dei testi dei messaggi** in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato, con il fornitore di posta segnalato;
- [ ] **registro delle decisioni** compilato: minimizzazione del contenuto del messaggio, ricaduta di canale,
      idempotenza;
- [ ] contratto degli **strumenti conversazionali** predisposto per `invia_promemoria`;
- [ ] avvio locale invariato: in locale non parte nessun messaggio vero;
- [ ] documentazione aggiornata.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0021` | serve sapere su quale canale si può scrivere |
| storia `0017` | i messaggi nascono dalle prenotazioni |

## 7. Fuori ambito

- il canale di messaggistica e i messaggi brevi: storia `0023`;
- la richiesta di recensione dopo l'appuntamento: è dell'applicazione 17, qui esce solo l'evento;
- qualunque messaggio promozionale.

## 8. Punti aperti

**Quanti promemoria sono troppi.** Gli studi indicano che tre contatti funzionano meglio di uno, ma sono studi
clinici su appuntamenti importanti; per una piega, tre messaggi sono fastidio. Proposta: predefinito **uno** a 24
ore, e fino a tre configurabili. Da confermare, ed è una scelta di prodotto.
