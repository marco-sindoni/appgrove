# 0017 — Invio al cliente con collegamento riservato

**Applicazione**: 06 — QuoteGrove (`preventivi`) · **Epica**: 04 — Invio, accettazione e firma
**Storia**: `0017` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0004`, `0015`, `0016`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come addetto che ha finito di preparare l'offerta
> voglio mandarla al cliente con un colpo solo e sapere che è partita
> così da chiudere il lavoro nel momento in cui l'ho finito, senza passare da un programma di posta.

**Contesto.** È l'atto che dà valore all'app e **l'unico che consuma quota** (metrica `preventivi_inviati`, natura
`flow`). L'invio fa tre cose insieme e deve farle in modo che nessuna resti a metà: congela la versione (storia
`0015`), genera il **collegamento riservato** che il cliente userà, e recapita il messaggio. Se il recapito
fallisce, la quota non deve essere consumata e il documento non deve restare in uno stato ambiguo.

## 2. Requisiti funzionali

1. **RF-1** — Dall'interfaccia si invia il preventivo scegliendo destinatari (uno o più indirizzi), oggetto e
   testo di accompagnamento, con valori proposti dal modello.
2. **RF-2** — L'invio congela la versione, porta lo stato a `inviato` e registra a chi, quando e con quale
   collegamento.
3. **RF-3** — Il collegamento riservato è **lungo e casuale**, riferito a un solo preventivo, con una **scadenza**
   propria, revocabile, e non indicizzabile dai motori di ricerca.
4. **RF-4** — L'invio **consuma una unità** della metrica `preventivi_inviati`; un reinvio dello stesso documento
   alla stessa versione **non** consuma una seconda unità.
5. **RF-5** — Se il recapito fallisce, lo stato torna a `bozza`, la quota è rilasciata e l'errore è mostrato in
   modo comprensibile («l'indirizzo non esiste», non un codice del fornitore).
6. **RF-6** — Prima dell'invio l'app blocca i casi in cui il documento non è pronto: sconto sopra soglia non
   approvato (storia `0010`), destinatario consumatore senza il blocco obbligatorio (storia `0014`), validità già
   scaduta (storia `0021`).

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** L'invio filtra per `tenant_id` preso dal token verificato; il
  collegamento riservato porta dentro di sé, firmato dal server, l'account e il preventivo: non si costruisce da
  parametri della richiesta.
- **RT-2 — Varchi e quota (§6, §7).** Prima di inviare, il servizio prenota una unità della metrica
  `preventivi_inviati` (natura `flow`); a quota esaurita risponde `429` con l'indicazione del rimedio e **non
  invia nulla**. Con abbonamento non attivo risponde `402`.
- **RT-3 — Interfaccia di programmazione (§2).** `POST /api/preventivi/v1/preventivi/{id}/invio`; errori in
  `problem+json` con codici stabili per «quota esaurita», «approvazione mancante», «recapito fallito»; OpenAPI
  aggiornata.
- **RT-4 — Persistenza (§8).** Migrazione `V10__invii.sql`: tabella `invio_preventivo` con `tenant_id`, UUID
  versione 7, colonne di controllo, cancellazione logica; il **gettone del collegamento si memorizza come
  impronta**, non in chiaro.
- **RT-5 — Modulo frontend (§3, §5).** Finestra di invio con anteprima del messaggio; conferma esplicita perché è
  un atto verso l'esterno; solo token del sistema di design; tema chiaro e scuro.
- **RT-6 — Cinque lingue (§4).** L'interfaccia in `en, it, fr, es, de`; il **messaggio al cliente** segue la
  lingua del destinatario.
- **RT-7 — Dati personali (§10).** Voce nuova nel manifesto in italiano e inglese: l'indirizzo di posta a cui il
  documento è stato recapitato, con finalità «prova della consegna» e la sua durata; campo annotato
  `@PersonalData`; tabella `invio_preventivo` aggiunta a `exportData` e `purgeData`. **Il fornitore di posta
  elettronica riceve dati personali di una categoria di interessati nuova per la piattaforma — i clienti dei
  nostri clienti — e va segnalato nell'elenco dei fornitori e nell'informativa.**
- **RT-8 — Registrazione eventi (§14).** `preventivo inviato`, `invio fallito`, `quota rilasciata` con
  `tenant_id`, `app_id`, `user_id`, correlazione — **mai l'indirizzo del destinatario**.

## 4. Criteri di accettazione

**CA-1 — Invio riuscito**
- **Dato** un preventivo pronto e quota disponibile · **Quando** si invia · **Allora** lo stato è `inviato`,
  esiste la versione congelata, il consumo è aumentato di uno e il messaggio risulta recapitato

**CA-2 — Quota esaurita**
- **Dato** un account che ha raggiunto il tetto di `preventivi_inviati` · **Quando** tenta l'invio · **Allora**
  riceve `429` con il rimedio, il documento resta in `bozza` e **nessun messaggio parte**

**CA-3 — Recapito fallito**
- **Dato** un indirizzo inesistente · **Quando** si invia · **Allora** l'app mostra un errore comprensibile, lo
  stato torna a `bozza` e la quota **non** risulta consumata

**CA-4 — Reinvio senza doppio consumo**
- **Dato** un preventivo già inviato · **Quando** lo si reinvia alla stessa versione · **Allora** il consumo
  resta invariato e l'invio è registrato come secondo recapito della stessa versione

**CA-5 — Blocchi prima dell'invio**
- **Dato** un preventivo con sconto sopra soglia non approvato · **Quando** si tenta di inviarlo · **Allora**
  l'app rifiuta indicando chi deve approvare, e nulla parte

**CA-6 — Isolamento fra account**
- **Dato** un preventivo dell'account `A` · **Quando** un utente di `B` tenta di inviarlo · **Allora** riceve la
  risposta che riceverebbe per un documento inesistente, e nessun messaggio parte

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh`;
- [ ] prove di **unità** sulla generazione del gettone e sul rilascio della quota, di **integrazione** sull'invio
      con fornitore di posta simulato;
- [ ] prova di **isolamento fra account** sull'invio;
- [ ] **prova end-to-end**: **coperta ora** come ultimo passo del percorso interno della storia `0029`; registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) aggiornato lì;
- [ ] **traduzioni** in tutte e cinque le lingue, più il messaggio al cliente nella lingua del destinatario;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese con l'indirizzo del destinatario dell'invio, e
      fornitore di posta dichiarato;
- [ ] **registro delle decisioni** compilato (forma e durata del gettone, momento della prenotazione della quota,
      regola del reinvio);
- [ ] avvio locale invariato.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0004` | la catena dei varchi e il contatore |
| storia `0015` | l'invio congela una versione |
| storia `0016` | il documento da allegare o mostrare |
| decisione dello sviluppatore sul gettone di capacità | è la deviazione dall'invariante «`tenant_id` solo dal token»: va approvata (punto 2 dei rischi) |

## 7. Fuori ambito

- la pagina che il cliente apre: storia `0018`;
- i solleciti: storia `0022`.

## 8. Punti aperti

**Il gettone del collegamento riservato** è il punto in cui questa applicazione si discosta dal modo in cui il
resto della piattaforma identifica un account. La forma proposta — gettone lungo e casuale, firmato dal server,
legato a un solo preventivo, a scadenza, revocabile, memorizzato solo come impronta — va approvata dallo
sviluppatore prima di scrivere la storia `0018`.
