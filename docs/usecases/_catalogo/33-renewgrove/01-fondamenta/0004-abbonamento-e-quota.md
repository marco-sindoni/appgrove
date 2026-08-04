# 0004 — Abbonamento e quota

**Applicazione**: 33 — RenewGrove (`fidelizzazione`) · **Epica**: 01 — Fondamenta
**Storia**: `0004` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0002`, `0003`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che sorveglia duecentocinquanta clienti su un piano che ne prevede duecentocinquanta
> voglio sapere in anticipo che cosa succede al duecentocinquantunesimo, e come si rimedia
> così da non scoprirlo il giorno in cui il cliente che stavo per perdere è proprio quello che non è entrato.

**Contesto.** La quota di questa app è **a giacenza** sulla metrica `rapporti_sorvegliati`: il tetto vale su quanti
rapporti l'app sorveglia adesso, non su quanti ne entrano nel mese. La scelta è argomentata nel varco d'identità
(§3 della [descrizione](../application-description.md)) e ha una conseguenza che questa storia deve rendere vera e
comprensibile: la **riduzione di piano è sbarrata** finché i rapporti sorvegliati eccedono il tetto del piano di
destinazione. È la regola di piattaforma per le metriche a giacenza, e va detta con il rimedio accanto, non solo con
il divieto. Il varco sta nelle fondamenta perché un varco che arriva tardi lascia dietro di sé tutte le funzioni
nate senza. Va anche detto che cosa **non** limita il piano: gli interventi. Metterci sopra un contatore
insegnerebbe al cliente a non farli, e questa app si misura proprio sulla loro efficacia (epica 05).

## 2. Requisiti funzionali

1. **RF-1** — L'accesso a ogni funzione dell'app attraversa la catena dei varchi: token valido altrimenti `401`;
   app non spenta dalla piattaforma altrimenti `403`; account abilitato altrimenti `402`; ruolo sufficiente
   altrimenti `403`; quota non esaurita altrimenti `429`.
2. **RF-2** — Nella metrica `rapporti_sorvegliati` contano i rapporti nello stato **sorvegliato**; **non** contano
   quelli archiviati né quelli esclusi. Gli stati sono introdotti dalla storia `0009`: qui si dichiara quali
   consumano quota e si predispone il conteggio.
3. **RF-3** — Portare un rapporto in sorveglianza oltre il tetto risponde `429` con un messaggio che dice quanti
   ne sono sorvegliati, quanti ne prevede il piano e come rimediare; nulla entra in sorveglianza.
4. **RF-4** — Il passaggio a un piano di appgrove inferiore è **bloccato** finché i rapporti sorvegliati superano
   il tetto del piano di destinazione, con un messaggio che dice quanti archiviarne; nulla viene programmato.
5. **RF-5** — Con abbonamento di piattaforma in `trialing`, `active` o `past_due` l'app funziona; con `paused` o
   `canceled` risponde `402`. L'esportazione e la cancellazione dei dati restano accessibili in ogni caso.
6. **RF-6** — L'abilitazione e il tetto si leggono dalla **proiezione locale** alimentata a eventi, mai con una
   chiamata di rete sincrona al servizio centrale sul percorso caldo.
7. **RF-7** — La Panoramica mostra rapporti sorvegliati su tetto del piano **prima** che il tetto sia toccato, non
   dopo il rifiuto.

## 3. Requisiti tecnici

- **RT-1 — Varchi e quota (§6, §7).** Prima di portare un rapporto in sorveglianza il servizio prenota una unità
  della metrica `rapporti_sorvegliati` (natura `stock`); a quota esaurita risponde `429` con l'indicazione del
  rimedio. Con abbonamento non attivo risponde `402`. La storia **non fissa prezzi**: consuma il tetto pubblicato
  dall'abilitazione.
- **RT-2 — Listino come codice (§7).** Il file `services/core/src/main/resources/pricing/fidelizzazione.yaml` è
  registrato in `pricing/index.yaml` e dichiara `userModel: multi`, `category: teal` — che deve coincidere con
  l'`accentToken` del modulo frontend (storia `0003`) — e i tre piani proposti `free`, `cura`, `portafoglio`, con
  `limits: { metric: rapporti_sorvegliati, cap: …, type: stock }`, doppio ciclo mensile e annuale, importi in
  centesimi. I valori sono una **proposta dello sviluppatore** (§5 della descrizione), non di questa storia.
- **RT-3 — Isolamento fra account (§1).** Il conteggio è per `tenant_id` preso dal token verificato; nessun
  conteggio attraversa gli account e nessun `tenant_id` arriva dal corpo o dai parametri.
- **RT-4 — Interfaccia di programmazione (§2).** Errori in `application/problem+json` con codici stabili per
  «quota esaurita», «abbonamento di piattaforma non attivo» e «ruolo insufficiente»; definizione OpenAPI aggiornata
  nello stesso commit.
- **RT-5 — Modulo frontend (§3, §5).** Riquadro di quota nella Panoramica del modulo `fidelizzazione`, dati letti
  con il client generato; solo token del sistema di design; funziona in tema chiaro e scuro.
- **RT-6 — Cinque lingue (§4).** Indicatore di quota, messaggio di rifiuto per quota, messaggio di blocco della
  riduzione di piano e messaggio di abbonamento non attivo, tutti nello spazio-nomi `fidelizzazione` e presenti in
  `en, it, fr, es, de`.
- **RT-7 — Dati personali (§10).** Nessun dato personale nuovo: si contano righe, non persone. Il messaggio di
  rifiuto non nomina alcun rapporto.
- **RT-8 — Esposizione conversazionale (§12).** Nessuno strumento nuovo, ma la regola vale per tutti: ogni
  strumento di scrittura che porta un rapporto in sorveglianza attraversa lo stesso varco e riceve lo stesso `429`.
  Il contratto vive dentro il servizio; il server conversazionale è di piattaforma e non ancora implementato
  (UC 0061-0063).
- **RT-9 — Registrazione eventi (§14).** `rapporto messo in sorveglianza`, `messa in sorveglianza respinta per
  quota`, `riduzione di piano bloccata`, `accesso respinto per abbonamento non attivo`, con `tenant_id`, `app_id`,
  `user_id` e identificativo di correlazione, senza dati personali.
- **RT-10 — Prove (§11).** Integrazione sulla catena dei varchi; matrice dei ruoli `owner` / `admin` / `member`;
  una prova per ciascuno stato dell'abbonamento di piattaforma; prova che l'archiviato non consuma quota; prova che
  l'esportazione resta accessibile con abbonamento `canceled`.

## 4. Criteri di accettazione

**CA-1 — Quota rispettata**
- **Dato** un account sul piano `cura` con 250 rapporti sorvegliati
- **Quando** un utente prova a portarne in sorveglianza un altro
- **Allora** riceve `429`, il messaggio dice «250 su 250: archiviane uno o passa al piano superiore», e nulla entra
  in sorveglianza

**CA-2 — L'archiviazione restituisce la quota**
- **Dato** lo stesso account · **Quando** archivia un rapporto e ne porta in sorveglianza un altro
- **Allora** l'operazione riesce, il conteggio resta 250 e lo storico del rapporto archiviato è ancora lì

**CA-3 — Riduzione di piano sbarrata**
- **Dato** un account con 400 rapporti sorvegliati che tenta di scendere dal piano `portafoglio` a `cura` (250)
- **Quando** conferma la riduzione
- **Allora** l'operazione è bloccata con un messaggio che dice quanti archiviarne, e nulla è programmato

**CA-4 — Abbonamento di piattaforma non attivo**
- **Dato** un account con abbonamento `canceled` · **Quando** apre l'app
- **Allora** riceve `402`, ma l'esportazione e la cancellazione dei propri dati restano accessibili

**CA-5 — Tolleranza sui pagamenti falliti**
- **Dato** un account in `past_due` · **Quando** lavora sui rapporti · **Allora** funziona tutto normalmente

**CA-6 — Isolamento del conteggio**
- **Dato** due account `A` e `B`, entrambi vicini al proprio tetto
- **Quando** `A` porta un rapporto in sorveglianza
- **Allora** il conteggio di `B` non cambia, e nessuna manipolazione della richiesta permette ad `A` di consumare
  la quota di `B`

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend` e `frontend`; l'intera suite prima del commit);
- [ ] prove di **unità** sul conteggio a giacenza (quali stati contano) e di **integrazione** sulla catena dei
      varchi, con database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** sul conteggio della quota;
- [ ] **prova end-to-end**: *rimando* — il rifiuto per quota entra nel percorso `[J-FIDELIZZAZIONE]` della storia
      `0030`, con voce `da-coprire` nel registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) e motivo «percorso non ancora
      creato»;
- [ ] **traduzioni** dei messaggi in tutte e cinque le lingue (`en, it, fr, es, de`);
- [ ] **manifesto dei dati**: nessuna modifica — nessun dato personale nuovo;
- [ ] **registro delle decisioni** compilato: quali stati del rapporto consumano quota, perché l'archiviato no, e
      perché gli interventi non sono una metrica;
- [ ] contratto degli **strumenti conversazionali**: nessuna funzione nuova, ma la regola del varco vale anche per
      le chiamate dell'assistente;
- [ ] avvio locale invariato, con fornitore di pagamento della piattaforma simulato;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0002` | serve una tabella `rapporto` su cui contare |
| storia `0003` | serve la Panoramica dove mostrare l'indicatore di quota |
| decisione dello sviluppatore sul listino (§5 della descrizione, punto aperto n. 3) | i tetti dei piani e la durata della prova gratuita vengono da lì: sono una fermata di escalation |

## 7. Fuori ambito

- gli stati del rapporto e il passaggio in sorveglianza vero e proprio: storia `0009` — qui si predispone il varco,
  lì si consuma;
- l'acquisto e il cambio del piano di appgrove: è di piattaforma;
- qualunque limite sugli interventi: **non esiste e non deve esistere**, per la ragione scritta nel contesto.

## 8. Punti aperti

- ⚠️ **La prova gratuita di quattordici giorni non basta a dimostrare il valore di questa app** (punto aperto n. 3
  della descrizione). L'argomento di vendita è l'epica 05, che richiede una finestra di mesi: al quattordicesimo
  giorno il cliente vede lavoro preparato, non risultati. È una decisione commerciale, non tecnica. Chiude: lo
  sviluppatore, insieme al listino.
- **Il rapporto escluso deve davvero non consumare quota?** La proposta dice di sì, perché su un rapporto escluso
  l'app smette di calcolare e di avvisare. Resta l'effetto sgradevole simmetrico: escludere è anche il modo di
  liberare quota senza archiviare. Chiude: lo sviluppatore, insieme alla storia `0009`.
