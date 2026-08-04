# 0004 — Abbonamento e quota delle ricevute

**Applicazione**: 08 — SpendGrove (`notespese`) · **Epica**: 01 — Fondamenta
**Storia**: `0004` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0002`, `0003`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che paga un abbonamento
> voglio sapere quante ricevute mi restano questo mese e cosa succede quando finiscono
> così da non scoprire il limite nel momento peggiore, cioè mentre sto chiudendo le note spese di fine mese.

**Contesto.** Il servizio e l'interfaccia esistono, ma niente distingue un account che paga da uno che non paga, e
niente ferma chi consuma senza limite. Questa storia aggancia la catena dei varchi della piattaforma e definisce la
regola più delicata del listino: **quando si consuma una unità**. La risposta scelta è «alla conferma umana della
spesa» e non «al caricamento del file», perché una foto storta ricaricata tre volte non deve costare tre ricevute:
sarebbe un modo elegante di far pagare al cliente i difetti della nostra lettura automatica.

## 2. Requisiti funzionali

1. **RF-1** — Ogni funzione protetta attraversa la catena dei varchi: token valido, app non spenta, account
   abilitato, ruolo sufficiente, quota disponibile.
2. **RF-2** — Una unità della metrica `receipts` si consuma **alla transizione della spesa verso `confermata`**,
   una sola volta per spesa, sia che la spesa nasca da una ricevuta letta sia che sia inserita a mano.
3. **RF-3** — Se la quota del mese è esaurita, la conferma della spesa risponde `429` con un messaggio che dice
   quante ne restano, quando si azzerano e come si passa di piano; **nulla viene creato o modificato**.
4. **RF-4** — Caricare una ricevuta, correggerne i dati, scartarla o rileggerla **non** consuma quota: il conteggio
   è sull'esito, non sui tentativi.
5. **RF-5** — L'interfaccia mostra il consumo (usate / tetto / finestra) nella panoramica e ripete l'avviso
   **prima** del modulo di conferma, non dopo il salvataggio.
6. **RF-6** — L'abilitazione si legge dalla proiezione locale alimentata a eventi, mai da una chiamata di rete
   sincrona verso l'app centrale sul percorso caldo.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Il consumo e il tetto sono per account: `tenant_id` dal token verificato,
  contatore filtrato riga per riga. Un account non vede né influenza il contatore di un altro.
- **RT-2 — Interfaccia di programmazione (§2).** La conferma è `POST /api/notespese/v1/spese/{id}/conferma`; a quota
  esaurita risponde `429` in `application/problem+json` con un campo che indica il rimedio; definizione OpenAPI
  aggiornata.
- **RT-3 — Persistenza (§8).** Migrazione `V2__consumo_quota.sql`: tabella del consumo per account e finestra
  mensile, con `tenant_id` e colonne di controllo; la prenotazione dell'unità e il cambio di stato avvengono nella
  **stessa transazione**, così che un errore non lasci quota consumata senza spesa confermata.
- **RT-4 — Modulo frontend (§3, §5).** Barra di consumo nella panoramica e avviso in testa alla schermata di
  conferma; solo token del sistema di design; funziona in tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** I messaggi di quota — avviso vicino al limite, blocco raggiunto — passano dallo
  spazio-nomi `notespese` e sono presenti in tutte e cinque le lingue.
- **RT-6 — Varchi e quota (§6, §7).** Prima di confermare una spesa il servizio prenota una unità della metrica
  `receipts` (natura `flow`, finestra mensile); a quota esaurita risponde `429`. Con abbonamento `past_due` la
  funzione resta accessibile (periodo di tolleranza), con `canceled` risponde `402`. Il listino resta un file nel
  repository: la storia **non fissa prezzi**.
- **RT-7 — Esposizione conversazionale (§12).** Gli strumenti che scrivono (storia `0029`) consumeranno quota
  esattamente come l'interfaccia: il varco sta nel servizio, non nel chiamante.
- **RT-8 — Dati personali (§10).** Nessun dato personale nuovo: il contatore è un numero per account.
- **RT-9 — Registrazione eventi (§14).** Gli eventi `spesa confermata`, `quota prenotata`, `conferma respinta per
  quota` sono registrati con `tenant_id`, `app_id`, `user_id` e identificativo di correlazione, senza dati personali.

## 4. Criteri di accettazione

**CA-1 — Consumo alla conferma, una sola volta**
- **Dato** un account con 3 ricevute usate su 20
- **Quando** l'utente conferma una spesa e poi ne modifica la categoria e la salva di nuovo
- **Allora** il consumo è 4, non 5: la modifica non consuma

**CA-2 — Tentativi che non costano**
- **Dato** un account con 3 ricevute usate su 20
- **Quando** l'utente carica la stessa foto tre volte perché venuta male e ne scarta due
- **Allora** il consumo resta 3 finché non conferma una spesa

**CA-3 — Quota esaurita**
- **Dato** un account che ha raggiunto il tetto di `receipts` del mese
- **Quando** tenta di confermare un'altra spesa
- **Allora** riceve `429`, il messaggio dice quante ne ha usate, quando si azzerano e come cambiare piano, e la
  spesa resta in `da_rivedere`

**CA-4 — Abbonamento non attivo**
- **Dato** un account con abbonamento `canceled` · **Quando** chiama una qualsiasi rotta di dominio
- **Allora** riceve `402`; **ma** la rotta di esportazione dei propri dati continua a rispondere, perché i diritti
  dell'interessato restano accessibili in ogni caso

**CA-5 — Isolamento del contatore**
- **Dato** due account `A` e `B` sullo stesso piano
- **Quando** `A` esaurisce la quota
- **Allora** `B` continua a confermare spese senza alcun effetto

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno backend e frontend; l'intera suite prima del commit);
- [ ] prove di **unità** sul conteggio e la finestra mensile, di **integrazione** sulla conferma con database
      effimero e migrazioni vere, compresa la prova che quota e stato cambiano nella stessa transazione;
- [ ] prova di **isolamento fra account** sul contatore e sulla conferma;
- [ ] **prova end-to-end**: *rimando* alla storia `0031`, che porta il passo «quota esaurita» dentro il percorso
      `[J-NOTESPESE]` e aggiorna
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml);
- [ ] **traduzioni** dei messaggi di quota in tutte e cinque le lingue;
- [ ] **manifesto dei dati**: nessuna voce nuova;
- [ ] **registro delle decisioni** compilato, in particolare con la regola «si consuma alla conferma» e il perché;
- [ ] contratto degli **strumenti conversazionali**: nessuno nuovo, ma è annotato che gli strumenti futuri passano
      dallo stesso varco;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| `0002` | Serve la macchina a stati della spesa: il consumo è agganciato a una transizione |
| `0003` | Serve dove mostrare il consumo e l'avviso |
| Listino come codice (`pricing/notespese.yaml`) | I tetti vengono dal piano; la storia li legge, non li decide |

## 7. Fuori ambito

- I prezzi e i tetti dei piani: sono una fermata di escalation dello sviluppatore (descrizione, §5).
- Le deroghe di quota concesse in assistenza: [estensioni-admin.md](../estensioni-admin.md).
- Il cambio di piano dall'interfaccia: è della sezione Fatturazione della piattaforma, non del modulo.

## 8. Punti aperti

- **Se la metrica diventasse `seats`** (punto aperto n. 1 della descrizione), questa storia cambierebbe natura:
  natura `stock`, blocco sul passaggio a un piano inferiore quando i posti eccedono il tetto. Va deciso **prima** di
  implementare, non dopo.
- **Ricevute di un mese confermate il mese dopo**: se un collaboratore consegna a marzo gli scontrini di febbraio,
  il consumo cade a marzo. È coerente con la natura `flow` e va spiegato nel messaggio, ma è una scelta di prodotto
  da confermare.
