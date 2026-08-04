# 0016 — Registrazione del rimborso

**Applicazione**: 08 — SpendGrove (`notespese`) · **Epica**: 03 — Note spese e approvazione
**Storia**: `0016` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0015`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come chi tiene l'amministrazione
> voglio segnare che una nota approvata è stata pagata, con la data e il riferimento del bonifico fatto in banca
> così da sapere in ogni momento quali rimborsi restano da liquidare, senza tenere un secondo elenco a parte.

**Contesto.** Dopo l'approvazione, la nota resta in un limbo: approvata ma non pagata. Chi tiene l'amministrazione
ha bisogno di chiudere il cerchio, e soprattutto di vedere **l'arretrato**: le note approvate e non ancora
liquidate sono un debito verso i collaboratori, e un collaboratore che aspetta il rimborso da due mesi smette di
usare l'app. La storia è deliberatamente **sottile**: l'app registra, non paga. Emettere un pagamento è mestiere di
un istituto di pagamento (descrizione, §1) ed è l'effetto irreversibile verso l'esterno che un'app di catalogo non
si prende.

## 2. Requisiti funzionali

1. **RF-1** — Una nota `approvata` si può segnare come rimborsata, indicando data del pagamento, importo e un
   riferimento libero (numero del bonifico, «contanti di cassa», «cedolino di agosto»).
2. **RF-2** — L'importo rimborsato **può differire** dal totale approvato (un arrotondamento, un acconto): la
   differenza è mostrata e va motivata, non nascosta.
3. **RF-3** — Esiste l'elenco «da liquidare»: note approvate e non ancora rimborsate, con l'anzianità in giorni,
   ordinato dalla più vecchia.
4. **RF-4** — La registrazione è **correggibile** entro la chiusura del periodo (storia `0025`): un riferimento
   scritto male non deve richiedere di disfare l'approvazione.
5. **RF-5** — Il collaboratore vede sulle proprie note lo stato «rimborsata» con la data, senza dover chiedere.
6. **RF-6** — L'app **non** invia disposizioni di pagamento e non conserva le coordinate bancarie del collaboratore:
   il campo non esiste, e l'interfaccia lo dice invece di lasciarlo cercare.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** La registrazione filtra per `tenant_id` preso dal token verificato ed è
  riservata ai ruoli `approva` e `amministra`; chi `sostiene` legge lo stato delle proprie note e non scrive.
- **RT-2 — Interfaccia di programmazione (§2).** `POST /api/notespese/v1/note-spese/{id}/rimborso` e
  `PATCH .../rimborso`; errori in `application/problem+json` con `409` se la nota non è `approvata`; definizione
  OpenAPI aggiornata.
- **RT-3 — Persistenza (§8).** Migrazione `V13__rimborso.sql`: tabella `rimborso` con `tenant_id`, chiave UUID
  versione 7, riferimento logico alla nota, importo, data, riferimento libero, motivo dello scostamento, colonne di
  controllo e cancellazione logica.
- **RT-4 — Modulo frontend (§3, §5).** Nella sezione *Note spese*, il filtro «da liquidare» con l'anzianità e la
  finestra di registrazione del rimborso. Solo token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Etichette, aiuti e messaggi passano dallo spazio-nomi `notespese` e sono presenti
  in `en, it, fr, es, de`.
- **RT-6 — Varchi e quota (§6, §7).** Nessun consumo di quota. Con abbonamento `canceled` risponde `402`; con
  `past_due` resta accessibile.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento di scrittura: registrare un pagamento è un fatto
  amministrativo che vuole una persona. In lettura, `riepilogo_spese` (storia `0028`) espone l'arretrato da
  liquidare, che è la domanda vera («quanto devo ai miei?»).
- **RT-8 — Dati personali (§10).** Voce nuova nel manifesto in italiano e inglese per il rimborso, tabella
  `rimborso` aggiunta a `exportData` e `purgeData`. **Nessuna coordinata bancaria**: la scelta di non raccoglierla è
  una misura di minimizzazione e va scritta nel manifesto come tale — se un giorno servisse, sarebbe una categoria
  nuova e una decisione nuova (descrizione, §6).
- **RT-9 — Registrazione eventi (§14).** Gli eventi `rimborso registrato`, `rimborso corretto` portano `tenant_id`,
  `app_id`, `user_id`, identificativo di correlazione e identificativo della nota — non l'importo, non il
  riferimento scritto.

## 4. Criteri di accettazione

**CA-1 — Registrazione**
- **Dato** una nota `approvata` da 312,40 €
- **Quando** l'amministrazione registra il rimborso con data e riferimento del bonifico
- **Allora** la nota passa a `rimborsata`, esce dall'elenco «da liquidare» e il collaboratore ne vede la data

**CA-2 — Importo diverso dal totale**
- **Dato** una nota approvata da 312,40 € · **Quando** si registra un rimborso di 312,00 €
- **Allora** la differenza di 0,40 € è mostrata e va motivata; senza motivo l'operazione è respinta con `400`

**CA-3 — Nota non approvata**
- **Dato** una nota `inviata` · **Quando** si tenta di registrarne il rimborso
- **Allora** l'operazione è respinta con `409` e il messaggio dice che serve prima l'approvazione

**CA-4 — Arretrato visibile**
- **Dato** tre note approvate da 12, 40 e 61 giorni
- **Quando** l'amministrazione apre «da liquidare»
- **Allora** le vede in quest'ordine con l'anzianità accanto

**CA-5 — Ruolo insufficiente**
- **Dato** un collaboratore con solo ruolo `sostiene` · **Quando** tenta di registrare un rimborso
- **Allora** riceve `403`

**CA-6 — Nessuna coordinata bancaria**
- **Dato** la scheda del collaboratore e il modulo del rimborso
- **Quando** li si esamina
- **Allora** non esiste alcun campo per il conto corrente, e un aiuto spiega che il pagamento si fa in banca

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno backend e frontend; l'intera suite prima del commit);
- [ ] prove di **unità** sul calcolo dello scostamento e dell'anzianità; di **integrazione** sulla risorsa con
      database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** e di ruolo sulla registrazione;
- [ ] **prova end-to-end**: *coprire ora* il passo finale «registro il rimborso» nel percorso `[J-NOTESPESE]`;
      registro [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) aggiornato;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese, con la nota esplicita sull'assenza delle coordinate
      bancarie come misura di minimizzazione;
- [ ] **registro delle decisioni** compilato, con la scelta di non pagare e di non conservare le coordinate;
- [ ] contratto degli **strumenti conversazionali**: nessuno di scrittura, con la motivazione scritta;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| `0015` | Solo una nota approvata si rimborsa |
| Catalogo 10 — PayGrove | **Confine, non dipendenza**: se l'azienda vuole pagare davvero dal software, è quell'app a farlo; qui si registra soltanto |

## 7. Fuori ambito

- L'esecuzione del pagamento e l'esportazione di disposizioni di bonifico: fuori dal perimetro dichiarato dell'app.
- Il passaggio del rimborso in busta paga: è PayGrove (catalogo 10).
- La compensazione fra rimborsi dovuti e anticipi di cassa già consegnati al collaboratore: caso reale ma
  minoritario, rimandato.

## 8. Punti aperti

- **Rimborsi parziali multipli** (due acconti su una stessa nota): oggi il modello ammette un rimborso per nota. Se
  servissero, il legame diventerebbe uno a molti e il calcolo del residuo cambierebbe: decisione di prodotto, da
  prendere prima di implementare.
