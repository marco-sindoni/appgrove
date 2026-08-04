# 0019 — Contestazioni e pagamenti parziali

**Applicazione**: 03 — CashGrove (`crediti`) · **Epica**: 04 — Esiti e recupero
**Storia**: `0019` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0009`, `0016`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare
> voglio distinguere chi non paga da chi non paga **perché contesta**, e sollecitare solo la parte non contestata
> così da non trasformare un disaccordo commerciale in una rottura del rapporto.

**Contesto.** Una quota consistente dei ritardi non è cattiva volontà: è una fattura sbagliata, una consegna incompleta,
uno sconto promesso e non applicato. Se l'app tratta questi casi come gli altri, manda solleciti sempre più fermi a
qualcuno che ha ragione — ed è il modo più veloce di far spegnere l'automatismo. Il pagamento parziale è la forma
silenziosa della stessa cosa: il cliente paga quello che riconosce e tace sul resto. Questa storia dà un nome a entrambi.

## 2. Requisiti funzionali

1. **RF-1** — L'utente apre una contestazione su un credito indicando motivo (da un elenco: merce o servizio non
   conforme, documento errato, sconto non applicato, altro), importo contestato e nota.
2. **RF-2** — Finché la contestazione è aperta, i solleciti automatici sul credito sono sospesi.
3. **RF-3** — La contestazione si chiude con un esito: accolta (l'importo contestato esce dal credito), respinta (il
   credito torna interamente dovuto), risolta con accordo (l'importo viene rettificato del valore concordato).
4. **RF-4** — Alla chiusura, i solleciti riprendono sul residuo aggiornato, ripartendo dal passo dovuto in quel momento.
5. **RF-5** — Un pagamento parziale che lascia un residuo sotto una soglia dell'account **non** riavvia i solleciti
   automatici ma segnala il credito come «da chiudere a mano», per non inseguire venti euro con tre messaggi.
6. **RF-6** — Il tempo medio di apertura delle contestazioni e la loro incidenza sono visibili sulla scheda del
   debitore: un cliente che contesta sempre è un dato commerciale, non solo amministrativo.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura e scrittura dell'entità `Contestazione` filtra per `tenant_id`
  preso dal token verificato.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `GET`, `POST /api/crediti/v1/crediti/{id}/contestazioni` e
  `POST /api/crediti/v1/contestazioni/{id}/chiusura`; corpo validato; errori in `application/problem+json`; definizione
  OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione per la tabella `contestazione` sullo schema `app_crediti` con `tenant_id`,
  chiave UUID versione 7, colonne di controllo e cancellazione logica. La rettifica dell'importo del credito lascia
  traccia nella cronologia dei cambi di stato (storia `0010`), non sovrascrive in silenzio.
- **RT-4 — Modulo frontend (§3, §5).** Azione «apri contestazione» dalla scheda del credito, indicatore negli elenchi,
  finestra di chiusura con scelta dell'esito e conseguenze scritte a parole prima di confermare; solo token del sistema
  di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe visibili, compresi i motivi e gli esiti, passano dallo spazio-nomi
  `crediti` e sono presenti in `en, it, fr, es, de`.
- **RT-6 — Varchi e quota (§6, §7).** La contestazione non consuma quota; il credito resta monitorato. Se l'esito
  accolto azzera il residuo, il credito si chiude e la quota si libera.
- **RT-7 — Esposizione conversazionale (§12).** `apri_contestazione(credito, motivo, importo) → bozza` è dichiarato
  nella storia `0029` come **scrittura con conferma**, perché sospende i solleciti. La **chiusura** con esito «accolta»
  modifica un importo dovuto: resta anch'essa con conferma obbligatoria.
- **RT-8 — Dati personali (§10).** Nessun campo personale nuovo, ma il campo nota è a testo libero ed è, per natura,
  quello in cui è più probabile che finisca qualcosa di delicato («non paga perché ha avuto un lutto»): l'avvertenza
  accanto al campo è obbligatoria. Tabella presente in `exportData` e `purgeData`.
- **RT-9 — Registrazione eventi (§14).** Gli eventi «contestazione aperta», «contestazione chiusa con esito» sono
  registrati con `tenant_id`, `app_id`, `user_id`, motivo, esito e identificativo di correlazione, senza importi né
  dati personali.

## 4. Criteri di accettazione

**CA-1 — La contestazione sospende**
- **Dato** un credito scaduto con solleciti attivi
- **Quando** si apre una contestazione
- **Allora** nessun sollecito parte, e la coda mostra la sospensione con motivo «contestazione aperta»

**CA-2 — Contestazione accolta**
- **Dato** un credito da 1.000 € con contestazione da 200 € · **Quando** la contestazione è chiusa come accolta ·
  **Allora** il residuo diventa 800 €, la cronologia registra la rettifica e i solleciti riprendono su 800 €

**CA-3 — Contestazione respinta**
- **Dato** lo stesso credito · **Quando** la contestazione è chiusa come respinta · **Allora** il residuo resta 1.000 €
  e i solleciti riprendono dal passo dovuto oggi, senza recuperare quelli saltati

**CA-4 — Residuo sotto soglia**
- **Dato** un credito da 1.000 € e una soglia di 25 € · **Quando** arriva un incasso da 985 € · **Allora** il credito
  resta aperto con residuo 15 €, è segnalato «da chiudere a mano» e **nessun sollecito automatico** parte

**CA-5 — Isolamento fra account**
- **Dato** due account `A` e `B` · **Quando** un utente di `A` tenta di chiudere una contestazione di `B` · **Allora**
  riceve l'errore di risorsa non trovata

**CA-6 — Ruolo insufficiente**
- **Dato** un utente in sola lettura · **Quando** tenta di chiudere una contestazione come accolta · **Allora** riceve
  `403`: rettificare un importo dovuto non è una operazione da sola lettura

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (backend e frontend);
- [ ] prove di **unità** sui tre esiti e sulla soglia del residuo, di **integrazione** sulla ripresa dei solleciti;
- [ ] prova di **isolamento fra account** e **matrice dei ruoli** sulle rotte introdotte;
- [ ] **prova end-to-end**: *rimando* alla storia `0031`;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato con `contestazione`, presente in esportazione e cancellazione;
- [ ] **registro delle decisioni** compilato, in particolare sull'elenco chiuso dei motivi e sulla soglia del residuo;
- [ ] contratto degli **strumenti conversazionali**: funzioni predisposte, contratto dichiarato in `0029`;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0009` | Il pagamento parziale è la forma silenziosa della contestazione |
| storia `0016` | La sospensione passa dal meccanismo unico, non da uno parallelo |

## 7. Fuori ambito

- La gestione documentale della contestazione (allegati, corrispondenza): rimandata; il caso frequente si risolve con il
  motivo e una nota.
- L'emissione della nota di credito conseguente: è un documento contabile e appartiene all'app di fatturazione.
- Il collegamento con l'app di assistenza clienti (catalogo 12): sinergia dichiarata nel documento capofila §10, non
  implementabile finché quell'app non esiste.

## 8. Punti aperti

La **soglia del residuo trascurabile** (proposta: 25 €) è una scelta di prodotto ragionevole ma arbitraria: nessuna
fonte consultata indica una prassi. La conferma lo sviluppatore, oppure la si rende una impostazione dell'account con un
valore predefinito.
