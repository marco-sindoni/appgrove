# 0018 — Importazione massiva dei movimenti

**Applicazione**: 14 — StockGrove (`magazzino`) · **Epica**: 03 — Registro dei movimenti e giacenze
**Storia**: `0018` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0013`, `0014`, `0011`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che sta passando dal foglio di calcolo
> voglio caricare in una volta sola le giacenze di partenza e i movimenti che ho già registrato altrove
> così da cominciare a usare il programma con i miei numeri veri, senza ribattere quattrocento righe a mano.

**Contesto.** L'anagrafica si importa già (storia `0011`), ma un'anagrafica senza quantità non serve a nulla: il
primo giorno il cliente deve poter dire «di questo ne ho 24, di quello 7». È la condizione per cominciare
(descrizione dell'applicazione, §2.4 punto 6). Il carico iniziale è tecnicamente una serie di movimenti di carico
con motivo dedicato, e i movimenti storici — chi arriva da un altro programma e vuole portarsi dietro un anno di
storia — seguono la stessa strada. Il rischio specifico dell'importazione è il **doppio conteggio**: un file
rieseguito perché «non si era capito se era andato» raddoppia il magazzino, e a quel punto il cliente non si fida
più di un programma che ha appena installato.

## 2. Requisiti funzionali

1. **RF-1** — Si carica un file con una riga per movimento: codice interno dell'articolo, deposito, quantità con
   segno, data, motivo, riferimento facoltativo, nota facoltativa, e una **chiave di riga** scelta dal cliente o
   generata dal contenuto della riga.
2. **RF-2** — Prima di scrivere qualsiasi cosa, l'importazione mostra un'**anteprima**: quante righe valide, quante
   in errore e con quale motivo riga per riga (articolo sconosciuto, deposito sconosciuto, quantità non numerica,
   motivo incompatibile con il segno, data non leggibile).
3. **RF-3** — **Nessun caricamento parziale muto**: o si conferma l'importazione delle sole righe valide sapendo
   esattamente quante e quali sono, oppure non si importa nulla; in nessun caso l'esito è un numero di righe scritte
   diverso da quello mostrato in anteprima.
4. **RF-4** — L'importazione è **idempotente per riga**: la chiave di riga concorre alla chiave di idempotenza del
   movimento, quindi rieseguire lo stesso file non crea un secondo movimento e l'esito riporta quante righe erano
   già presenti.
5. **RF-5** — I movimenti importati sono **marcati come tali**, con il riferimento all'importazione che li ha
   generati: si distinguono nello storico e si possono stornare in blocco quando l'importazione era sbagliata.
6. **RF-6** — L'esito dell'importazione resta consultabile dopo la chiusura della schermata: quando fu eseguita, da
   chi, quante righe scritte, quante ignorate perché già presenti, quante in errore, con il file degli errori
   scaricabile.
7. **RF-7** — Il carico iniziale usa il motivo dedicato `giacenza iniziale`, che è ammesso solo con segno positivo e
   solo su articoli senza movimenti precedenti nel deposito indicato: serve a impedire che un secondo «carico
   iniziale» si sommi a un magazzino già avviato.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** L'importazione, i suoi esiti e i movimenti generati filtrano per
  `tenant_id` preso dal token verificato; la chiave di riga è univoca **per account**, non globale. Prova di
  isolamento fra due account che importano file con le stesse chiavi di riga.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `POST /api/magazzino/v1/importazioni-movimenti` (invio del
  file e anteprima), `POST /api/magazzino/v1/importazioni-movimenti/{id}/conferma`,
  `GET /api/magazzino/v1/importazioni-movimenti` (elenco degli esiti); errori in `application/problem+json`;
  definizione OpenAPI aggiornata nello stesso commit. L'elaborazione avviene in modo asincrono con stato
  interrogabile: un file di qualche migliaio di righe non tiene aperta una richiesta.
- **RT-3 — Persistenza (§8).** Migrazione `V11__importazione_movimenti.sql` sullo schema `app_magazzino`: tabelle
  `importazione_movimenti` (origine, stato, conteggi, autore, momenti) e `riga_importazione` (chiave di riga,
  contenuto originale, esito, motivo dell'errore, movimento generato), entrambe con `tenant_id`, chiave primaria
  UUID versione 7, colonne di controllo e cancellazione logica. Vincolo di unicità su
  `(tenant_id, importazione_id, chiave_riga)`. Ogni riga confermata scrive il proprio movimento con la stessa
  transazione e lo stesso aggiornamento condizionato delle storie `0014` e `0015`: l'importazione non è una via
  privilegiata per scrivere la giacenza.
- **RT-4 — Modulo frontend (§3, §5).** Nuova sezione «Importazioni» del modulo `magazzino`: caricamento del file,
  anteprima con i conteggi e la tabella degli errori, conferma esplicita, storico degli esiti; solo token del
  sistema di design; funziona in tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe visibili, compresi i messaggi di errore riga per riga, passano
  dallo spazio-nomi `magazzino` e sono presenti in `en, it, fr, es, de`; il modello di file scaricabile ha le
  intestazioni tradotte e accetta comunque le intestazioni in inglese.
- **RT-6 — Varchi e quota (§6, §7).** **I movimenti importati non consumano quota** e l'importazione non viene mai
  respinta con `429` per il numero di movimenti. Attenzione al caso reale: se il file contenesse articoli non
  ancora in anagrafica, la loro creazione **sì** consumerebbe la metrica `articoli_gestiti` (natura `stock`) — ma
  la creazione di articoli non appartiene a questa storia (è della `0011`), quindi qui un articolo sconosciuto è un
  **errore di riga**, non una creazione implicita.
- **RT-7 — Esposizione conversazionale (§12).** **Nessuno strumento dichiarato**: caricare un file da una chat non
  ha senso e un'importazione massiva è per definizione l'operazione che si vuole guardare in faccia prima di
  confermare.
- **RT-8 — Dati personali (§10).** **Nessun dato personale nuovo.** L'autore dell'importazione (`created_by`) è già
  dichiarato nel manifesto dalla storia `0010`. Il file caricato viene conservato solo per il tempo necessario a
  produrre l'esito e il suo contenuto rientra nelle note già dichiarate; la tabella `riga_importazione` va aggiunta
  a `exportData` e `purgeData` del contratto `MagazzinoDataContract` perché conserva testo libero.
- **RT-9 — Registrazione eventi (§14).** Gli eventi `importazione avviata`, `importazione confermata`,
  `riga ignorata per idempotenza`, `riga in errore` sono registrati con `tenant_id`, `app_id`, `user_id`,
  identificativo di correlazione e conteggi, **senza** il contenuto delle righe.

## 4. Criteri di accettazione

**CA-1 — Carico iniziale da file**
- **Dato** un account con 40 articoli in anagrafica e nessun movimento
- **Quando** si importa un file con 40 righe di giacenza iniziale, tutte valide, e si conferma
- **Allora** l'anteprima annuncia 40 righe valide e 0 errori, dopo la conferma esistono 40 movimenti di carico e le
  40 giacenze corrispondono ai valori del file

**CA-2 — File rieseguito per errore**
- **Dato** l'importazione della prova precedente, già confermata
- **Quando** si carica e si conferma **lo stesso identico file** una seconda volta
- **Allora** l'esito riporta 40 righe già presenti e 0 movimenti nuovi, e le giacenze restano quelle di prima

**CA-3 — Righe in errore e nessun caricamento parziale muto**
- **Dato** un file di 50 righe di cui 3 con un codice articolo inesistente
- **Quando** si carica il file
- **Allora** l'anteprima mostra 47 righe valide e 3 in errore con il motivo per ciascuna; se si conferma vengono
  scritti esattamente 47 movimenti; se non si conferma non viene scritto nulla; il file degli errori è scaricabile

**CA-4 — Secondo carico iniziale rifiutato**
- **Dato** un articolo che ha già movimenti nel deposito «Magazzino»
- **Quando** un file contiene per quell'articolo e quel deposito una riga con motivo `giacenza iniziale`
- **Allora** la riga risulta in errore con l'indicazione di usare un carico ordinario, e le altre righe restano
  valide

**CA-5 — Isolamento fra account**
- **Dato** due account `A` e `B` che importano file con le stesse chiavi di riga
- **Quando** entrambi confermano
- **Allora** ciascuno ottiene i propri movimenti, nessuna riga viene scartata per idempotenza a causa dell'altro
  account, e nessuno dei due vede le importazioni dell'altro

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno backend e frontend; l'intera suite prima del commit);
- [ ] prove di **unità** sulla validazione della riga e sulla generazione della chiave di idempotenza, e di
      **integrazione** sull'importazione completa con database effimero e migrazioni vere;
- [ ] prova di **idempotenza** che riesegue lo stesso file e verifica che le giacenze non cambino;
- [ ] prova di **isolamento fra account** su importazioni e chiavi di riga omonime;
- [ ] **prova end-to-end**: *rimando* — il percorso `[J-MAGAZZINO]` è di proprietà della storia `0036`; il registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) riceve lì la voce;
- [ ] **traduzioni** presenti in tutte e cinque le lingue, compresi i messaggi di errore per riga;
- [ ] **manifesto dei dati**: `riga_importazione` aggiunta a esportazione e cancellazione, senza voci nuove di
      categoria;
- [ ] **registro delle decisioni** compilato, con la scelta dell'idempotenza per riga e del divieto di caricamento
      parziale muto;
- [ ] contratto degli **strumenti conversazionali**: nessuna funzione esposta, con il motivo scritto;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| `0013` | Registro, giacenza e chiave di idempotenza |
| `0014` | Il movimento importato è un carico a tutti gli effetti, costo compreso |
| `0011` | Gli articoli devono già esistere: l'importazione dei movimenti non crea anagrafica |

## 7. Fuori ambito

- La creazione di articoli mancanti durante l'importazione dei movimenti: è della storia `0011` e resterebbe
  ambigua qui (chi decide unità di misura e categoria di un articolo nato da una riga di movimento?).
- Lo storno in blocco di un'importazione sbagliata: previsto come possibilità dal marcatore di RF-5, ma
  l'operazione di massa e la sua schermata restano fuori; si stornano i movimenti uno per uno con la storia `0017`
  finché qualcuno non chiede diversamente.
- L'importazione periodica automatica da un altro programma: sarebbe un'integrazione esterna e cambierebbe il
  paragrafo dei fornitori (descrizione dell'applicazione, §6).

## 8. Punti aperti

- **Formato del file.** La proposta è il formato a valori separati da virgola con separatore riconosciuto
  automaticamente, più il modello scaricabile; l'accettazione dei formati dei fogli di calcolo proprietari va
  decisa guardando cosa esportano davvero i programmi da cui i clienti arrivano — dato che non ho.
- **Tetto alla dimensione del file.** Serve un limite, ma il numero giusto dipende dalla capacità dell'ambiente e
  dalla deroga temporanea al tetto di articoli prevista in console per il primo mese (`estensioni-admin.md`): lo
  chiude lo sviluppatore.
