# 0009 — Listini e prezzi differenziati

**Applicazione**: 06 — QuoteGrove (`preventivi`) · **Epica**: 02 — Anagrafica, catalogo e listini
**Storia**: `0009` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0006`, `0008`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che ha concordato prezzi diversi con clienti diversi
> voglio che il preventivo prenda da solo il prezzo giusto per quel cliente e per quella quantità
> così da smettere di tenere gli accordi a memoria e di scoprire l'errore quando il cliente protesta.

**Contesto.** È la funzione che i prodotti italiani a basso prezzo non hanno (§2.1 della descrizione
dell'applicazione) e che distingue un generatore di documenti da uno strumento di vendita. Nelle micro-imprese
l'accordo «a te faccio il 10 % in meno» vive nella testa del titolare: quando prepara il preventivo un'altra
persona, il prezzo sbagliato esce e non torna più indietro.

## 2. Requisiti funzionali

1. **RF-1** — Si creano listini con nome, valuta e periodo di validità; uno è il **listino predefinito**
   dell'account.
2. **RF-2** — Un listino contiene un prezzo per voce di catalogo, eventualmente per **scaglione di quantità**
   (da 1 a 9 un prezzo, da 10 in su un altro).
3. **RF-3** — Un destinatario può avere un listino dedicato: quando lo si sceglie su un preventivo, i prezzi
   arrivano da lì.
4. **RF-4** — La risoluzione del prezzo segue un ordine dichiarato e visibile: listino del destinatario →
   listino scelto sul documento → listino predefinito → prezzo base della voce di catalogo.
5. **RF-5** — Su ogni riga del preventivo l'interfaccia dice **da dove viene** il prezzo applicato, e permette di
   sovrascriverlo a mano lasciando traccia della sovrascrittura.
6. **RF-6** — Un listino scaduto non si applica più, e l'app lo segnala invece di applicare in silenzio il
   predefinito.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** `listino` e `voce_listino` filtrano per `tenant_id` dal token
  verificato; il collegamento a un destinatario è verificato dentro lo stesso account.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `/api/preventivi/v1/listini` e un'operazione di
  risoluzione del prezzo `POST /api/preventivi/v1/listini/risoluzione` che restituisce prezzo e origine; errori
  in `problem+json`; OpenAPI aggiornata.
- **RT-3 — Persistenza (§8).** Migrazione `V4__listini.sql`: tabelle `listino` e `voce_listino` con `tenant_id`,
  UUID versione 7, colonne di controllo, cancellazione logica; nessuna chiave esterna verso altri schemi.
- **RT-4 — Modulo frontend (§3, §5).** Sezione **Catalogo e listini → Listini**; l'origine del prezzo è mostrata
  nella riga del preventivo; solo token del sistema di design, tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe visibili in `en, it, fr, es, de`, comprese le etichette
  dell'origine del prezzo («da listino dedicato», «prezzo base», «sovrascritto a mano»).
- **RT-6 — Dati personali (§10).** Nessun campo di persona nuovo; **attenzione**: un listino dedicato è però
  collegato a un destinatario, quindi la tabella entra comunque in `exportData` e `purgeData` (storia `0007`).
- **RT-7 — Registrazione eventi (§14).** `prezzo risolto` con l'origine, `prezzo sovrascritto`, con gli
  identificativi d'obbligo e senza dati personali.

## 4. Criteri di accettazione

**CA-1 — Il prezzo giusto senza pensarci**
- **Dato** un destinatario con listino dedicato che prezza `INST-01` a 40 € · **Quando** si aggiunge quella voce a
  un preventivo per lui · **Allora** la riga vale 40 € e l'interfaccia dice «da listino dedicato»

**CA-2 — Scaglione di quantità**
- **Dato** un listino che prezza `INST-01` a 45 € fino a 9 unità e 38 € da 10 · **Quando** si mette quantità 12
- **Allora** il prezzo unitario applicato è 38 €

**CA-3 — Ordine di risoluzione**
- **Dato** un destinatario senza listino dedicato e un listino predefinito attivo · **Quando** si aggiunge una
  voce · **Allora** vale il prezzo del predefinito, e non il prezzo base della voce di catalogo

**CA-4 — Listino scaduto**
- **Dato** un listino dedicato la cui validità è finita ieri · **Quando** si compone un preventivo per quel
  destinatario · **Allora** l'app avvisa che il listino è scaduto e chiede cosa applicare, invece di scegliere da sé

**CA-5 — Isolamento fra account**
- **Dato** due account con listini omonimi · **Quando** un utente di `A` risolve un prezzo · **Allora** usa solo i
  propri listini, anche forzando identificativi altrui nella richiesta

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh`;
- [ ] prove di **unità** sull'ordine di risoluzione e sugli scaglioni, di **integrazione** sulle risorse;
- [ ] prova di **isolamento fra account** sulle risorse nuove;
- [ ] **prova end-to-end**: rimando alla storia `0029`;
- [ ] **traduzioni** in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato: la tabella dei listini dedicati entra in esportazione e cancellazione;
- [ ] **registro delle decisioni** compilato (ordine di risoluzione, forma degli scaglioni, comportamento sui
      listini scaduti);
- [ ] avvio locale invariato; dati di prova estesi con un listino dedicato.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0006` | il listino dedicato è dedicato a un destinatario |
| storia `0008` | un listino prezza le voci del catalogo |

## 7. Fuori ambito

- gli sconti, che sono una cosa diversa dal prezzo di listino: storia `0010`;
- la conversione fra valute diverse: storia `0011` dichiara la valuta del documento, ma l'app **non** converte;
- le condizioni promozionali a tempo: rimandate, nessuna evidenza di mercato che le chieda in questo segmento.

## 8. Punti aperti

Nessuno.
