# 0023 — Attribuzione del venduto

**Applicazione**: 21 — SalonGrove (`salone`) · **Epica**: 06 — Operatori, provvigioni e rendimento
**Storia**: `0023` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0009`, `0019`, `0021`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che a fine mese deve dire a Sara quanto ha prodotto
> voglio che ogni riga di ogni conto porti scritto chi l'ha eseguita, anche quando il servizio è passato per tre
> mani
> così da non ricostruire niente a memoria e da non discutere su chi ha fatto cosa.

**Contesto.** L'epica 06 poggia tutta su un fatto solo: **ogni riga di conto ha un operatore**. Se quel fatto non
è vero, le provvigioni si calcolano su una base inventata e a fine mese si litiga. La chiusura del conto (storia
`0019`) già propone un'attribuzione, presa da chi ha eseguito le fasi (storia `0009`): questa storia la rende
completa, correggibile finché il conto è aperto, e definitiva quando il conto si chiude.

⚠️ **Il confine di questa epica, dichiarato subito perché tutto il resto ne discende.** Sapere *chi ha eseguito
cosa* è un **fatto commerciale**: serve ad attribuire il venduto e a pagare quanto dovuto. Non è una rilevazione
della prestazione lavorativa — non ci sono orari, timbrature, presenze, tempi effettivi confrontati con i tempi
previsti (§1 e §10 della [descrizione](../application-description.md): l'app 11 ShiftGrove è **esclusa** dal
catalogo proprio per questo). E non è una misura del rendimento della persona: quella soglia si tratta nella
storia `0026`, ed è una soglia che questa storia **non** attraversa.

## 2. Requisiti funzionali

1. **RF-1** — Ogni `RigaConto` porta un **operatore attribuito**, obbligatorio alla chiusura: un conto con anche
   una sola riga senza operatore non si chiude.
2. **RF-2** — L'attribuzione è **proposta** dal sistema: per un servizio, l'operatore che ha eseguito la fase
   principale (storia `0009`); per un prodotto di rivendita, chi sta chiudendo il conto (storia `0021`). La
   proposta si cambia con un tocco.
3. **RF-3** — Un servizio a più mani si può **ripartire fra più operatori** sulla stessa riga, in percentuale o a
   valore, con somma obbligatoriamente pari all'importo della riga.
4. **RF-4** — Finché il conto è aperto l'attribuzione si modifica liberamente; a conto chiuso si corregge solo con
   una **rettifica** (storia `0019`), che sposta la base da un operatore all'altro lasciando visibili entrambi i
   passaggi.
5. **RF-5** — Esiste un **operatore predefinito** per l'account (di norma il titolare): un salone che non vuole
   occuparsene attribuisce tutto a lui e l'epica 06 resta inerte senza rompere niente.
6. **RF-6** — L'operatore che ha lasciato il salone resta selezionabile in **sola lettura** sulle righe passate:
   i conti storici non si riscrivono perché una persona se n'è andata.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Righe, attribuzioni e ripartizioni filtrano per `tenant_id` preso dal
  token verificato; un `tenant_id` che arrivasse dal corpo della richiesta viene ignorato. L'operatore
  attribuibile deve appartenere allo stesso account: un identificativo di utente di un altro account viene
  respinto come inesistente.
- **RT-2 — Interfaccia di programmazione (§2).** `PATCH /api/<app>/v1/conti/{id}/righe/{riga}/attribuzione` con
  corpo validato (elenco di operatori e quote, somma pari all'importo della riga, quote non negative); errori in
  `application/problem+json`; definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione sullo schema dell'app: colonna `operatore_id` su `riga_conto` e tabella
  `riga_conto_attribuzione` (riga, operatore, quota) per le ripartizioni, con `tenant_id`, chiave primaria UUID
  versione 7, colonne di controllo e cancellazione logica. `operatore_id` è un riferimento **logico** all'utente
  dell'account: nessuna chiave esterna verso altri schemi.
- **RT-4 — Modulo frontend (§3, §5).** Sull'interfaccia del conto ogni riga mostra l'operatore come un campo di
  scelta a un tocco, con la ripartizione nascosta dietro un'azione secondaria: il caso normale è una persona sola
  e non deve costare due tocchi in più. Solo token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Etichette, messaggi di errore sulla somma delle quote e testo dello stato «ha
  lasciato il salone» presenti in `en, it, fr, es, de`.
- **RT-6 — Varchi e quota (§6, §7).** Nessun consumo di quota: attribuire non apre una postazione. La funzione è
  accesa dal piano che comprende le provvigioni; a piano insufficiente `402`.
- **RT-7 — Dati personali (§10).** Voce di manifesto `riga_conto.operatore` (già introdotta dalla storia `0019`)
  estesa alla tabella delle ripartizioni: interessato = **chi lavora nel salone**, finalità «attribuire il venduto
  e calcolare quanto spetta», base «esecuzione del contratto di lavoro o di collaborazione, per conto del salone»,
  durata proposta 24 mesi. Il manifesto dichiara espressamente che il dato **non** serve a valutare la persona.
  Campi annotati `@PersonalData`; tabelle in esportazione e cancellazione (storie `0014` e `0032`).
- **RT-8 — Esposizione conversazionale (§12).** `aggiungi_riga_conto(conto, voce, operatore) → bozza aggiornata`
  (storia `0019`) accetta l'operatore; **nessuno strumento** modifica l'attribuzione di un conto già chiuso. La
  correzione di quanto spetta a una persona non si delega a un assistente.
- **RT-9 — Registrazione eventi (§14).** `attribuzione modificata` con `tenant_id`, `app_id`, `user_id`,
  correlazione, identificativo della riga e identificativi degli operatori — **mai** nomi di persone.

## 4. Criteri di accettazione

**CA-1 — L'attribuzione arriva già proposta**
- **Dato** un appuntamento in cui Sara ha fatto il colore e Marco il taglio
- **Quando** si apre il conto
- **Allora** la riga del colore è attribuita a Sara e quella del taglio a Marco, senza che nessuno scelga nulla

**CA-2 — Il servizio a più mani si ripartisce**
- **Dato** una riga di 90 € da dividere fra Sara (70 %) e l'assistente (30 %)
- **Quando** si registra la ripartizione
- **Allora** la riga resta di 90 € e le due quote valgono 63 € e 27 €

**CA-3 — La somma deve tornare**
- **Dato** una riga di 90 € · **Quando** si ripartisce 60 € + 20 €
- **Allora** la richiesta è respinta con un errore che dice di quanto manca, e nulla viene salvato

**CA-4 — Non si chiude un conto senza operatore**
- **Dato** un conto con una riga aggiunta a mano e senza operatore
- **Quando** si tenta la chiusura
- **Allora** la chiusura è rifiutata indicando la riga incompleta

**CA-5 — A conto chiuso si corregge solo rettificando**
- **Dato** un conto chiuso con una riga attribuita per errore a Marco
- **Quando** si sposta la base su Sara
- **Allora** l'operazione avviene tramite rettifica, restano visibili l'attribuzione originaria e la correzione, e
  le provvigioni maturate delle due persone cambiano di conseguenza

**CA-6 — Isolamento fra account**
- **Dato** due account con propri operatori
- **Quando** un utente del primo attribuisce una riga passando l'identificativo di un operatore del secondo
- **Allora** la richiesta è respinta e nulla cambia in nessuno dei due account

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend` e `frontend`; l'intera suite prima del commit);
- [ ] prove di **unità** sulla somma delle quote e sulla proposta automatica; di **integrazione** sulle rotte con
      database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** sull'attribuzione, compreso il tentativo con un operatore altrui;
- [ ] **prova end-to-end**: *coprire ora* — il percorso `[J-SALONGROVE]` (storia `0030`) attribuisce due righe a
      due operatori diversi e verifica la base che ne risulta; registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) aggiornato;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese, con la finalità dichiarata e il limite d'uso;
- [ ] **registro delle decisioni**: attribuzione obbligatoria alla chiusura, ripartizione a quote, operatore
      predefinito, correzione solo per rettifica;
- [ ] avvio locale invariato; il salone di prova ha conti attribuiti a operatori diversi.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0009` | la proposta automatica viene da chi ha eseguito le fasi della sequenza |
| storia `0019` | l'attribuzione vive sulle righe del conto e si congela alla chiusura |
| storia `0021` | la rivendita ha una regola di attribuzione propria (chi vende) |

## 7. Fuori ambito

- il **calcolo** di quanto spetta: è la storia `0024`, che ha bisogno di questa base ma è un'altra cosa;
- qualunque misura del **tempo** di lavoro dell'operatore: fuori perimetro, e resta fuori;
- i ruoli e i permessi delle persone dell'account: sono di piattaforma, non di questa app.

## 8. Punti aperti

**Se l'operatore possa cambiare l'attribuzione delle proprie righe.** La proposta è che chi ha ruolo di membro
possa correggere le righe del conto che sta chiudendo, e che le rettifiche su conti chiusi restino a chi
amministra. Un permesso più stretto rende il dato più affidabile ma sposta lavoro sul titolare; un permesso più
largo rende possibile spostarsi il venduto addosso. È una scelta di prodotto e di fiducia, e va registrata in
`decisions.json`.
