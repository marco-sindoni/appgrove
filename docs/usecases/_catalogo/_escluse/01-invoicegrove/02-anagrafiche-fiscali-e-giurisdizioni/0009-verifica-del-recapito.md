# 0009 — Verifica del recapito

**Applicazione**: 01 — InvoiceGrove (`einvoicing`) · **Epica**: 02 — Anagrafiche fiscali e giurisdizioni
**Storia**: `0009` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0006`, `0008`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come responsabile amministrativo
> voglio sapere **prima** di emettere se il recapito del mio cliente esiste davvero e sa ricevere quel tipo di
> documento
> così da non scoprire dopo tre giorni che la fattura è stata rifiutata e da non doverla rifare.

**Contesto.** La validazione di forma (storia `0008`) intercetta il codice destinatario di cinque caratteri, ma
non il codice **formalmente valido e sbagliato**, che è il caso frequente. Sulla rete a quattro angoli esiste un
registro pubblico che dice se un identificativo è registrato e quali documenti sa ricevere: interrogarlo prima di
emettere trasforma uno scarto in un avviso. È una delle poche funzioni che il cliente percepisce come «mi ha
salvato», e per questo vale una storia sua.

## 2. Requisiti funzionali

1. **RF-1** — Per una controparte sulla rete a quattro angoli, l'app verifica che l'identificativo sia
   **registrato** e che il destinatario dichiari di saper ricevere il tipo di documento che si vuole inviare.
2. **RF-2** — Per l'Italia, l'app verifica la coerenza fra tipo di controparte e recapito (per esempio: una
   pubblica amministrazione richiede un codice di sei caratteri, un'impresa di sette), e segnala l'incoerenza.
3. **RF-3** — L'esito della verifica è mostrato sulla scheda della controparte con tre stati soli: `verificato`,
   `non verificabile`, `problema rilevato` — e, nell'ultimo caso, cosa fare.
4. **RF-4** — La verifica si può richiedere a mano dalla scheda e viene rifatta **automaticamente** quando il
   recapito cambia.
5. **RF-5** — Se il registro esterno non risponde, l'esito è `non verificabile` e **non blocca** l'emissione: un
   servizio esterno lento non deve impedire di fatturare.
6. **RF-6** — L'esito è ricordato con la data della verifica, e viene considerato scaduto dopo un periodo
   configurabile.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** L'esito della verifica è legato alla controparte, quindi filtrato per
  `tenant_id` preso dal token verificato. Prova di isolamento su due account.
- **RT-2 — Interfaccia di programmazione (§2).** Rotta
  `POST /api/einvoicing/v1/counterparties/{id}/verify-routing`; errori in `application/problem+json`; definizione
  OpenAPI aggiornata nello stesso commit. La chiamata al registro esterno ha un **tempo massimo** e non blocca la
  richiesta oltre quello.
- **RT-3 — Persistenza (§8).** Migrazione `V6__routing_verification.sql`: colonne di esito, data e dettaglio sulla
  tabella `counterparty`, con `tenant_id` e colonne di controllo.
- **RT-4 — Modulo frontend (§3, §5).** La scheda della controparte mostra lo stato di verifica con un indicatore
  chiaro e un pulsante «verifica adesso»; solo token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** I tre stati e i messaggi di rimedio passano dallo spazio-nomi `einvoicing` e sono
  presenti in `en, it, fr, es, de`.
- **RT-6 — Varchi e quota (§6, §7).** La verifica **non** consuma la metrica `documenti`: non è un documento e non
  ha costo verso il fornitore di trasmissione. Va però protetta da un limite di frequenza per account, per non
  trasformarsi in un modo gratuito di interrogare in massa un registro pubblico.
- **RT-7 — Esposizione conversazionale (§12).** Strumento dichiarato:
  `verify_counterparty_routing(id) → esito, dettaglio, rimedio`, marcato **lettura** (interroga e memorizza un
  esito, non produce effetti verso terzi). Nessuna conferma umana. Contratto dentro il servizio; server
  conversazionale di piattaforma non implementato (UC 0061-0063).
- **RT-8 — Dati personali (§10).** Nessun campo nuovo che riguardi una persona: si memorizza un esito, non un
  dato personale nuovo. **Ma la verifica invia l'identificativo della controparte a un registro esterno**: è un
  trasferimento verso terzi e va segnalato nell'elenco dei fornitori e nell'informativa. Se il registro
  interrogato è quello pubblico della rete, l'identificativo è già pubblico: circostanza da verificare, non da
  assumere.
- **RT-9 — Registrazione eventi (§14).** Gli eventi `verifica richiesta`, `esito registrato`, `registro esterno
  non raggiungibile` sono registrati con `tenant_id`, `app_id`, `user_id` e identificativo di correlazione, senza
  denominazioni né indirizzi.

## 4. Criteri di accettazione

**CA-1 — Recapito verificato**
- **Dato** una controparte belga con un identificativo di rete registrato
- **Quando** si chiede la verifica
- **Allora** l'esito è `verificato`, con la data, e la scheda lo mostra

**CA-2 — Problema rilevato**
- **Dato** una controparte con un identificativo formalmente valido ma non registrato
- **Quando** si chiede la verifica
- **Allora** l'esito è `problema rilevato`, con la spiegazione «questo identificativo non risulta registrato» e il
  suggerimento di chiederlo al cliente

**CA-3 — Registro esterno non raggiungibile**
- **Dato** il registro esterno che non risponde entro il tempo massimo
- **Quando** si chiede la verifica
- **Allora** l'esito è `non verificabile`, l'emissione resta possibile, e l'evento è registrato

**CA-4 — La verifica si rifà quando il recapito cambia**
- **Dato** una controparte con esito `verificato`
- **Quando** se ne modifica il recapito elettronico
- **Allora** l'esito precedente decade e la verifica riparte

**CA-5 — Isolamento fra account**
- **Dato** due account `A` e `B` con controparti diverse
- **Quando** un utente di `A` chiede la verifica di una controparte di `B`
- **Allora** riceve `404`, non l'esito

**CA-6 — Limite di frequenza**
- **Dato** un account che chiede molte verifiche in rapida successione
- **Quando** supera il limite configurato
- **Allora** riceve `429` con l'indicazione di quando riprovare, e nessuna chiamata parte verso il registro
  esterno

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (backend, frontend, compliance);
- [ ] prove di **unità** sulla mappatura esito → stato e sulla coerenza tipo/recapito italiana, di **integrazione**
      sulla rotta con il registro esterno **simulato** (compresi il caso di indisponibilità e quello di lentezza);
- [ ] prova di **isolamento fra account** sulla risorsa;
- [ ] **prova end-to-end**: *rimando* — il percorso `[J-EINVOICING]` (storia `0030`) attraverserà una verifica
      riuscita;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati**: nessuna voce nuova, ma **il trasferimento verso il registro esterno va dichiarato**
      nell'elenco dei fornitori e verificato prima del rilascio;
- [ ] **registro delle decisioni** compilato, con la scelta «esito non verificabile non blocca» motivata;
- [ ] contratto degli **strumenti conversazionali** dichiarato per `verify_counterparty_routing`.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| `0006` | La regola di verifica appartiene al profilo del paese |
| `0008` | Serve la controparte con il suo recapito |

## 7. Fuori ambito

- La verifica dell'**esistenza fiscale** della partita IVA presso i registri nazionali o europei: è un'altra
  integrazione, con un altro fornitore e un'altra classificazione. Rimandata; se servirà, sarà una storia nuova
  di questa epica.
- La verifica in blocco su tutta l'anagrafica: rimandata per non trasformare la funzione in un carico verso un
  registro pubblico. Se servirà, va progettata come lavorazione differita con un ritmo controllato.

## 8. Punti aperti

- **Quale registro si interroga e a quali condizioni d'uso.** I registri pubblici della rete hanno politiche
  d'uso; interrogarli in massa da un prodotto commerciale può richiedere un accordo. Non l'ho verificato e non lo
  do per acquisito: lo chiude lo sviluppatore.
- **Se l'identificativo trasmesso al registro sia da considerarsi dato personale** quando la controparte è un
  libero professionista. La classificazione è materialmente ambigua e va portata allo sviluppatore, non decisa
  qui.
