# 0025 — Evento «preventivo accettato»

**Applicazione**: 06 — QuoteGrove (`preventivi`) · **Epica**: 05 — Esito, acconti e catena del documento
**Storia**: `0025` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0019`, `0023`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che ha appena vinto un lavoro
> voglio che la fattura possa nascere da quel preventivo senza che io ridigiti nulla
> così da smettere di copiare a mano le stesse righe da un programma all'altro sbagliando un numero su dieci.

**Contesto.** Il catalogo appgrove indica la catena **preventivo → ordine → fattura → incasso** (app 6 → 2 → 1 →
3) come l'argomento di vendita più forte della suite, e la conversione in fattura è la prima integrazione che i
clienti si aspettano (§2.4 della descrizione dell'applicazione). Oggi le app a valle **non esistono**: si scrive
comunque l'evento, e si scrive bene, perché un evento pubblicato male non si cambia più. Vale la regola di
piattaforma: **un'app non chiama un'altra app**; l'unica via è asincrona, a eventi.

## 2. Requisiti funzionali

1. **RF-1** — Quando un preventivo è accettato, il servizio pubblica un evento `preventivo.accettato` con un
   contratto stabile e versionato.
2. **RF-2** — L'evento porta **il documento congelato**, non un riferimento da risolvere: destinatario, righe,
   riepilogo per aliquota, totale, valuta, condizioni di pagamento, acconto atteso, numero e versione del
   preventivo, impronta della versione accettata, momento dell'accettazione.
3. **RF-3** — L'evento porta l'identificativo dell'account e quello del preventivo, così che chi lo consuma possa
   risalire alla fonte con i propri strumenti.
4. **RF-4** — La pubblicazione è **almeno una volta** e l'evento è **idempotente** per chi lo riceve: porta un
   identificativo stabile che permette di riconoscere il duplicato.
5. **RF-5** — Il contratto dell'evento è documentato e versionato dentro il servizio, come quello degli strumenti
   conversazionali.
6. **RF-6** — Finché nessuno consuma l'evento, tutto continua a funzionare: nessuna funzione dell'app dipende
   dall'esistenza di un ascoltatore.

## 3. Requisiti tecnici

- **RT-1 — Comunicazione fra servizi (§2).** **Nessuna chiamata sincrona** verso altre app: solo evento
  asincrono. È l'invariante che rende sostituibile l'app a valle.
- **RT-2 — Isolamento fra account (§1).** L'evento porta il `tenant_id` come dato, ma nasce dentro una operazione
  già filtrata: non esiste un modo di pubblicare un evento per un account diverso dal proprio.
- **RT-3 — Persistenza (§8).** Migrazione `V16__eventi_uscenti.sql`: tabella dei messaggi in uscita con
  `tenant_id`, UUID versione 7, colonne di controllo e stato di pubblicazione — l'evento si scrive **nella stessa
  transazione** dell'accettazione e si pubblica dopo, così che non possa esistere un'accettazione senza evento né
  un evento senza accettazione.
- **RT-4 — Dati personali (§10).** **L'evento contiene dati personali del destinatario** (nome, recapiti,
  indirizzo): va dichiarato nel manifesto come trasferimento interno alla piattaforma, con la finalità («continuare
  la catena del documento su richiesta del titolare») e con la nota che l'app che lo riceve dovrà dichiararli a sua
  volta. Il contenuto va **minimizzato**: solo ciò che serve a emettere una fattura, non tutto ciò che sappiamo.
- **RT-5 — Registrazione eventi (§14).** `evento pubblicato`, `pubblicazione fallita` con `tenant_id`, `app_id`,
  correlazione e identificativo dell'evento, senza contenuti.
- **RT-6 — Prove (§11).** Prova di contratto sull'evento: la sua forma è verificata da una prova che fallisce se
  qualcuno cambia o toglie un campo senza cambiare versione.

## 4. Criteri di accettazione

**CA-1 — L'evento nasce con l'accettazione**
- **Dato** un preventivo accettato dal destinatario · **Quando** l'accettazione è registrata · **Allora** esiste
  un messaggio in uscita `preventivo.accettato` con il documento congelato e la sua impronta

**CA-2 — Transazione unica**
- **Dato** un guasto nel momento della scrittura · **Quando** la transazione fallisce · **Allora** non esistono né
  l'accettazione né l'evento: mai uno senza l'altro

**CA-3 — Duplicato riconoscibile**
- **Dato** una pubblicazione ripetuta per un guasto di rete · **Quando** l'evento arriva due volte · **Allora**
  porta lo stesso identificativo stabile e chi lo riceve può scartarlo

**CA-4 — Nessun ascoltatore, nessun problema**
- **Dato** che nessuna app consuma l'evento · **Quando** si accettano dieci preventivi · **Allora** l'app funziona
  normalmente e i messaggi restano in coda senza effetti

**CA-5 — Contratto stabile**
- **Dato** una modifica che toglie un campo dall'evento · **Quando** si eseguono le prove · **Allora** la prova di
  contratto fallisce, obbligando a una versione nuova

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh`;
- [ ] prove di **unità** sulla forma dell'evento e di **integrazione** sulla transazione unica;
- [ ] prova di **isolamento fra account** sulla pubblicazione;
- [ ] **prova end-to-end**: nessun impatto sulla superficie utente; l'evento è coperto da prova di contratto —
      risposta scritta nel registro [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml);
- [ ] **traduzioni**: non applicabile;
- [ ] **manifesto dei dati** aggiornato con il trasferimento interno e la minimizzazione dei campi;
- [ ] **registro delle decisioni** compilato: **il contratto dell'evento campo per campo e il motivo di ciascuno**
      — è la decisione che vincolerà le app a valle;
- [ ] avvio locale invariato.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0019` | l'evento nasce dall'accettazione |
| storia `0023` | l'acconto atteso viaggia nell'evento |

## 7. Fuori ambito

- il consumo dell'evento: è dell'app di fatturazione (catalogo 02), che non esiste;
- la creazione dell'ordine: idem;
- eventi per altri fatti (rifiuto, scadenza): si aggiungeranno quando qualcuno li chiederà. Pubblicare eventi che
  nessuno ascolta è debito, non lungimiranza — questo si pubblica perché la catena è dichiarata nel catalogo.

## 8. Punti aperti

**La forma esatta del contratto** andrà rivista quando l'app di fatturazione esisterà davvero: oggi si scrive
sulla base di ciò che serve per emettere una fattura, cioè un'ipotesi informata. Il rischio è registrato fra
quelli noti della descrizione dell'applicazione e l'attenuazione è portare il documento congelato, che invecchia
meglio di un riferimento.
