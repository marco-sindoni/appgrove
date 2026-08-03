# 0002 — Modello dati delle spese, per account

**Applicazione**: 08 — SpendGrove (`notespese`) · **Epica**: 01 — Fondamenta
**Storia**: `0002` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0001`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come sviluppatore dell'applicazione
> voglio le tabelle di base delle spese, isolate per account e già pronte a essere estese
> così da poter costruire cattura, approvazione e contabilità senza rifare il fondamento a metà strada.

**Contesto.** Il servizio esiste ma è vuoto. Questa storia mette a terra le entità che tutte le altre useranno:
`Spesa`, `Ricevuta`, `Categoria`, e le colonne che decidono il destino fiscale di una riga — mezzo di pagamento,
tracciabilità, tipo di documento, dentro o fuori Comune. Vanno messe **adesso** e non dopo, perché non sono
raffinatezze: derivano da obblighi di legge (descrizione dell'applicazione, §2.3) e aggiungerle a tabelle già piene
di dati costa una migrazione.

## 2. Requisiti funzionali

1. **RF-1** — Esistono le tabelle `categoria`, `ricevuta` e `spesa` sullo schema dell'app, ciascuna con
   `tenant_id`, chiave primaria UUID versione 7, colonne di controllo e cancellazione logica.
2. **RF-2** — La tabella `spesa` porta i campi che il regime fiscale richiede: `data`, `esercente`, `imponibile`,
   `imposta`, `totale`, `valuta`, `mezzo_pagamento`, `pagamento_tracciabile`, `tipo_documento`,
   `fuori_comune_sede`, `stato`, `collaboratore_id`.
3. **RF-3** — Lo stato della spesa è un elenco chiuso e le transizioni ammesse sono verificate dal servizio:
   `caricata → letta → da_rivedere → confermata → in_nota → approvata → rimborsata`, con le uscite `scartata` e
   `respinta`; una transizione non prevista è respinta con un errore parlante.
4. **RF-4** — Esistono le rotte di elenco, lettura, creazione e modifica della spesa, con paginazione a
   pagina/dimensione e totale.
5. **RF-5** — Ogni account nasce con un insieme predefinito di categorie (vitto, alloggio, trasporto, carburante,
   materiali di consumo, rappresentanza, altro), modificabili dal cliente.
6. **RF-6** — Le somme di denaro sono conservate in **centesimi interi** con la valuta accanto, mai in virgola
   mobile.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura e scrittura di `spesa`, `ricevuta` e `categoria` filtra per
  `tenant_id` preso dal token verificato; un `tenant_id` che arrivasse dal corpo della richiesta o dai parametri
  viene ignorato. Prova di isolamento fra due account su tutte e tre le risorse.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `GET|POST /api/notespese/v1/spese`,
  `GET|PATCH /api/notespese/v1/spese/{id}`, `GET /api/notespese/v1/categorie`; oggetti di trasferimento al bordo
  (le entità non si espongono mai); validazione dichiarativa; errori in `application/problem+json`; definizione
  OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V1__spese_categorie_ricevute.sql` sullo schema `app_notespese`: tre
  tabelle con `tenant_id`, chiave primaria UUID versione 7, colonne di controllo e `deleted_at`. Nessuna chiave
  esterna verso altri schemi; il riferimento al collaboratore è **logico** e diventerà vero con la storia `0012`.
- **RT-4 — Modulo frontend (§3, §5).** Nessuna schermata: la storia si ferma al servizio.
- **RT-5 — Cinque lingue (§4).** I nomi delle categorie predefinite sono **chiavi**, non testo: la traduzione sta
  nello spazio-nomi `notespese` in tutte e cinque le lingue (`en, it, fr, es, de`) e il cliente può rinominarle.
- **RT-6 — Varchi e quota (§6, §7).** Nessun consumo qui: il conteggio della metrica `receipts` si aggancia alla
  transizione verso `confermata` nella storia `0004`.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento dichiarato: gli strumenti sull'elenco delle spese
  arrivano con la storia `0028`.
- **RT-8 — Dati personali (§10).** La storia introduce campi che riguardano persone: `spesa.esercente`,
  `spesa.data`, `spesa.totale` raccontano dove e quando ha speso un lavoratore. Voci nuove nel manifesto
  `docs/compliance/manifests/notespese.yaml` in italiano e inglese, campi annotati `@PersonalData`, tabelle
  `spesa` e `ricevuta` aggiunte a `exportData` e `purgeData`.
- **RT-9 — Registrazione eventi (§14).** Gli eventi `spesa creata`, `stato della spesa cambiato`,
  `transizione respinta` sono registrati con `tenant_id`, `app_id`, `user_id` e identificativo di correlazione,
  **senza** l'esercente e senza gli importi.

## 4. Criteri di accettazione

**CA-1 — Creazione e lettura di una spesa**
- **Dato** un utente autenticato di un account abilitato
- **Quando** crea una spesa con data, esercente, totale e categoria
- **Allora** la spesa esiste in stato `caricata`, ha un identificativo UUID versione 7 e compare nell'elenco
  paginato del suo account

**CA-2 — Transizione di stato non ammessa**
- **Dato** una spesa in stato `caricata` · **Quando** si tenta di portarla direttamente a `approvata`
- **Allora** la risposta è `409` in `application/problem+json`, con un messaggio che elenca le transizioni ammesse,
  e lo stato resta `caricata`

**CA-3 — Isolamento fra account**
- **Dato** due account `A` e `B`, ciascuno con le proprie spese
- **Quando** un utente di `A` chiede l'elenco delle spese
- **Allora** vede solo le proprie, anche se forza l'identificativo dell'account `B` nel corpo della richiesta o in
  un parametro

**CA-4 — Importi senza errori di arrotondamento**
- **Dato** una spesa con imponibile 81,97 € e imposta 18,03 €
- **Quando** la si rilegge dopo il salvataggio
- **Allora** i valori tornano esattamente 8197 e 1803 centesimi e il totale è 10000, senza scarti

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno backend e compliance; l'intera suite prima del commit);
- [ ] prove di **unità** sulla macchina a stati e di **integrazione** sulle tre risorse, con database effimero e
      migrazioni vere;
- [ ] prova di **isolamento fra account** su `spesa`, `ricevuta` e `categoria`;
- [ ] **prova end-to-end**: *rimando* — non c'è ancora superficie utente; il percorso `[J-NOTESPESE]` è di proprietà
      della storia `0031`, e il registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) riceve lì la voce;
- [ ] **traduzioni** delle categorie predefinite presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese, campi annotati, tabelle presenti in esportazione e
      cancellazione;
- [ ] **registro delle decisioni** compilato, con la scelta degli importi in centesimi e degli stati chiusi;
- [ ] contratto degli **strumenti conversazionali**: nessuna funzione esposta in questa storia;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| `0001` | Servizio, schema e cartella delle migrazioni devono esistere |

## 7. Fuori ambito

- Il collegamento alla tabella dei collaboratori: qui il riferimento è un identificativo logico, l'anagrafica è
  della storia `0012`.
- La qualificazione dell'imposta riga per riga: storia `0024`.
- Trasferte e percorrenze: epica 04.

## 8. Punti aperti

- **Valuta diversa dall'euro**: il campo c'è, ma la conversione (a che cambio, di che giorno, con quale fonte) non
  è decisa. Serve una decisione di prodotto prima di aprirla; nel frattempo l'app registra l'importo nella valuta
  originale e non converte.
- Il campo `fuori_comune_sede` presuppone di conoscere il Comune sede di lavoro del collaboratore: finché
  l'anagrafica non esiste (storia `0012`) il valore è dichiarato da chi inserisce la spesa.
