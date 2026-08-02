# Change 0082: Gestione abbonamento self-service — sezione "Abbonamenti" completa

**Branch**: `change/0082-use-case-0067-abbonamento-self-service`
**Aree**: `frontend` (apps/backoffice, packages/i18n, packages/api-client), `services/core`
**Data**: 2026-08-02
**Autore**: Platform Engineering (modalità fast)
**Use case sorgente**: [docs/usecases/13-abbonamenti-self-service/0067-gestione-abbonamento-self-service.md](../../docs/usecases/13-abbonamenti-self-service/0067-gestione-abbonamento-self-service.md)
**Tocca dati personali?**: No — nessun dato nuovo. Piano, limiti e giacenza d'uso sono dati contrattuali
riferiti all'account; metodo di pagamento e ricevute restano in capo a Paddle (venditore ufficiale verso
il cliente finale). Il gate privacy di step-03 va comunque eseguito.

## Problema / Obiettivo

La sezione **"I tuoi abbonamenti"** del backoffice oggi esiste (UC 0028 l'ha aperta, UC 0096 l'ha collocata
nella pagina *Fatturazione*) ma è **scheletrica**: mostra il piano, una riga di stato e i limiti teorici del
piano, e per cambiare piano apre un riquadro con dei pulsanti nudi e due finestre di conferma del browser
(`window.confirm`). Mancano tutte le cose che rendono la sezione davvero *self-service*, cioè che permettono
all'utente di governare il proprio abbonamento senza scrivere all'assistenza:

- non si vede **quanto si sta consumando** rispetto a quello che si paga (solo "il piano prevede 10 posti",
  mai "ne stai usando 8");
- la scelta del piano avviene **al buio**: nessun prezzo, nessun limite, nessun confronto, ciclo di
  fatturazione forzato a mensile, e i piani troppo piccoli per l'uso corrente sono cliccabili e falliscono
  con un errore;
- **non si sa cosa succederà**: nessuna conferma spiega che un aumento di piano si addebita subito in
  proporzione e che una riduzione parte a fine periodo;
- gli stati "pagamento in ritardo" e "abbonamento scaduto" **non hanno un avviso dedicato**: sono solo un
  bollino colorato che non dice cosa fare;
- dopo un cambio la card **non dice che l'aggiornamento è in corso**, e i dati si allineano solo se l'utente
  ricarica;
- il caricamento è una riga di testo e l'errore non ha una riprova.

Obiettivo: portare la sezione allo stato descritto dallo use case 0067 — riepilogo onesto, cambio piano
informato e sicuro, disdetta e riattivazione con conferme esplicite, avvisi che dicono cosa fare, uso della
quota visibile dove è misurabile.

## Scope

### `services/core` — read-model più ricco (nessuna nuova azione)

Le azioni self-service (`change-tier`, `cancel`, `resume`, `portal-session`) esistono già e **non cambiano**.
Cambia solo ciò che il read-model `GET /api/platform/v1/me/subscriptions` racconta:

1. **Uso corrente per metrica** — ogni abbonamento espone la giacenza reale già proiettata in
   `platform.app_usage_stock` (alimentata dalle app per evento, UC 0054): la stessa fonte che il gate di
   riduzione piano usa già. Rende possibile "8 su 10 posti" al posto di "10 posti".
2. **Piani non ammissibili** — ogni abbonamento espone quali piani dell'app la regola di riduzione
   bloccherebbe adesso, con la relativa **spiegazione rimediale** già formulata dal backend. Il frontend non
   deve reimplementare la regola: il calcolo resta in un solo posto (`TierChangePolicy`), che è ciò che
   impedisce che i due lati divergano.
3. Contratto pubblicato: `openapi.yaml` rigenerato e tipi del client frontend riallineati.

### `frontend/apps/backoffice` — la sezione "Abbonamenti"

4. **Stati della lista**: scheletro di caricamento al posto della riga di testo; errore con **riprova**;
   stato vuoto invariato (rimando al catalogo).
5. **Card di riepilogo**: piano, stato, fine periodo, cambio già programmato (già presenti) più
   **consumo della quota** dove misurabile — riga "usati N su M" con barra di riempimento e avviso quando si
   supera la soglia di attenzione o si tocca il limite, con invito a passare a un piano superiore.
6. **Finestra "Cambia piano"** vera (finestra modale, non riquadro inline): per ogni piano nome, **prezzo**
   nel ciclo scelto, limiti; il piano attuale è marcato e non selezionabile; il primo piano superiore porta
   il contrassegno **"consigliato"**; i piani non ammissibili sono **disabilitati** e mostrano la
   spiegazione ("hai 8 posti occupati, il piano Base ne prevede 5"); si può scegliere **mensile o annuale**.
7. **Conferma esplicita** dentro la stessa finestra, prima di inviare il comando: riepiloga *cosa* succede e
   *quando* — aumento di piano: subito, con addebito proporzionale; riduzione: dal giorno di fine periodo,
   senza rimborso. La disdetta ha la sua conferma esplicita (accesso garantito fino alla data), al posto
   della finestra del browser.
8. **Stato "in aggiornamento"**: dopo un comando andato a buon fine la card lo dichiara e la lista viene
   riletta a intervalli brevi finché il read-model non riflette il cambiamento (o finché non scade un tempo
   massimo, dopo il quale si smette di insistere senza dichiarare falsamente il successo).
9. **Avviso pagamento in ritardo**: quando l'abbonamento è in ritardo di pagamento, avviso persistente che
   spiega la finestra di tolleranza e porta al portale per aggiornare il metodo di pagamento.
10. **Avviso abbonamento scaduto**: spiega che i dati restano e offre riattivazione **e** esportazione /
    cancellazione dei dati (diritti sempre esercitabili, esenti dai gate).
11. **Ruolo**: chi non è titolare dell'account (`owner`) vede tutto ma trova le azioni di fatturazione
    **disabilitate**, con la ragione scritta; il divieto resta comunque applicato dal backend.
12. **Lingue**: tutte le nuove diciture nei 5 cataloghi (`en`, `it`, `fr`, `es`, `de`).

### Test

13. `services/core`: test d'integrazione su uso corrente e piani bloccati nel read-model.
14. `frontend`: test di componente sui nuovi stati e sulla logica pura di presentazione.
15. **End-to-end Playwright (L2)**: estensione del percorso `L2-SUB` sui nuovi comportamenti.
16. **Registro di copertura end-to-end**: lo use case 0067 esce dalle esenzioni `non-implementato` ed entra
    fra gli use case con superficie, collegato al percorso che lo copre.

## Fuori scope

- **Pausa e ripresa** dell'abbonamento → UC 0068.
- **Prova gratuita una-tantum per tenant/app** → UC 0069.
- **Implementazione reale del fornitore Paddle** per cambio piano / disdetta / portale: resta non
  implementata finché non esiste l'account (bloccato da #14); vale lo stub locale.
- **Consumo delle metriche "a finestra"** (consumo che si azzera a ogni periodo, es. richieste al mese):
  in `core` esiste solo la proiezione della giacenza (`app_usage_stock`, UC 0054), non del consumo a
  finestra. Per quelle metriche la card continua a mostrare **solo il limite del piano**. Rimando tracciato
  nei punti aperti di UC 0067.
- **Riconciliazione dell'incassato netto** → UC 0071. **Storico pagamenti e ricevute** → già UC 0096.
- **Nuova rotta di menù dedicata**: la sezione resta dentro la pagina *Fatturazione* (`/billing`), come
  stabilito da UC 0096; spostarla sarebbe un dietrofront su una decisione appena presa.
- Modifiche al checkout iniziale (UC 0024) e al confine di applicazione runtime dei gate (UC 0027).

## Criteri di accettazione

- [ ] `GET /me/subscriptions` espone, per ogni abbonamento, l'uso corrente per metrica e i piani che la
      regola di riduzione bloccherebbe con la relativa spiegazione; `openapi.yaml` e i tipi del client sono
      riallineati e la suite non segnala scostamenti di contratto.
- [ ] La sezione mostra scheletro in caricamento, errore con riprova, stato vuoto con rimando al catalogo.
- [ ] La card mostra "usati N su M" con barra e avviso di soglia per le metriche a giacenza, e continua a
      mostrare il solo limite per le altre.
- [ ] La finestra "Cambia piano" mostra prezzo e limiti per piano, marca il piano attuale, contrassegna il
      primo piano superiore come consigliato, disabilita i piani non ammissibili mostrandone la ragione, e
      permette di scegliere il ciclo mensile o annuale.
- [ ] Prima di inviare un cambio o una disdetta compare una conferma che dice cosa succede e da quando;
      nessuna finestra di conferma del browser resta nel percorso.
- [ ] Dopo un comando riuscito la card dichiara "aggiornamento in corso" e si allinea da sola quando il
      read-model riflette il cambiamento.
- [ ] Un abbonamento in ritardo di pagamento mostra l'avviso persistente con l'accesso al portale; uno
      scaduto mostra riattivazione ed esportazione/cancellazione dati.
- [ ] Un utente non titolare vede le azioni di fatturazione disabilitate con la ragione.
- [ ] Tutte le nuove diciture esistono nelle 5 lingue; la verifica di parità dei cataloghi è verde.
- [ ] `./run-tests.sh` completa è verde; il registro di copertura end-to-end non ha più lo use case 0067
      fra le esenzioni ed è coerente.

## Invarianti appgrove toccati

- **Tenant dal token verificato**: il read-model continua a leggere il tenant dal `CallerContext`
  (claim `tenant_id`); i nuovi dati (uso, piani bloccati) sono derivati con quel tenant, mai da parametri
  della richiesta.
- **Filtro per riga sul tenant**: le subscription restano lette con il discriminatore di tenant; la lettura
  dell'uso passa per `AppUsageStore` con `tenant_id` esplicito e mai da input del client.
- **Modulo Terraform `microsaas_app`**: non toccato (nessuna infrastruttura in questa change).
- **Log strutturati**: i log della sezione continuano a portare `tenant_id`, `app_id`, `user_id`; nessun
  nuovo log senza contesto.

## Requisiti di test

- **Integrazione (`services/core`)**: con una giacenza riportata per l'app, il read-model espone l'uso e
  marca come bloccato il piano con capienza inferiore, con la spiegazione; senza giacenza nessun piano è
  bloccato e l'uso è vuoto. Le mutazioni restano riservate al titolare (già coperto) e l'isolamento fra
  tenant resta garantito.
- **Componente (`frontend`)**: scheletro in caricamento; errore con riprova che rilancia la chiamata;
  barra e avviso di soglia quota; finestra cambio piano con prezzi, piano attuale marcato, piano bloccato
  disabilitato con spiegazione, scelta del ciclo; conferma esplicita con la data corretta per la riduzione;
  stato "in aggiornamento"; avviso di pagamento in ritardo; avviso di scadenza; azioni disabilitate per chi
  non è titolare.
- **End-to-end L2 (`L2-SUB`)**: percorso guarda → cambia piano (aumento e riduzione) → disdici → riattiva →
  apri portale; avviso di pagamento in ritardo e avviso di scadenza visibili nei rispettivi stati.
- **Verde prima del commit**: `./run-tests.sh` senza parametri (modalità fast).

## Valutazione di impatto

| Area | Impatto |
|---|---|
| Breaking change | No — il read-model si arricchisce di campi facoltativi, nessun campo rimosso o rinominato |
| Contratto cross-area | Sì — frontend ↔ `services/core` (`GET /me/subscriptions`): `openapi.yaml` e tipi del client rigenerati nello stesso commit |
| Version bump | minor |
