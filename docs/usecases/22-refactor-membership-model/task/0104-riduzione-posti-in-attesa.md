# Piano di lavoro — UC 0104 · Riduzione dei posti in attesa

**Storia**: [0104](../story/0104-riduzione-posti-in-attesa.md) · **Aree toccate**: `services/core`, `frontend/`
**Dimensione stimata**: media-grande · **Prerequisito**: UC 0103

## Passo 1 — Migrazione

Due tabelle: `platform.seat_downgrade` (account, data di esecuzione, stato `pending`/`executed`/`cancelled`,
autore, campi di audit) e `platform.seat_downgrade_item` (riduzione, persona). Vincolo: **una sola** riduzione
in attesa per account — indice unico parziale sullo stato in attesa. Il vincolo in banca dati vale più di
qualunque controllo applicativo.

## Passo 2 — Il servizio

**File nuovo**: `SeatDowngradeService.java` in `core/billing/seats/`:

- `request(List<UUID> userIds)` — verifica che nessuno sia l'owner, che non ci sia già una riduzione in
  attesa, calcola la data di esecuzione dalla fine del periodo dell'abbonamento dei posti, crea le righe.
  **Non** tocca la quantità dell'abbonamento: la riduzione è programmata.
- `cancel()` — chiude la riduzione come annullata; nessun effetto contabile.
- `removeItem(userId)` — toglie una persona; se restano zero persone, la riduzione si chiude da sé.
- `execute(tenant)` — rimozione logica delle persone, cancellazione dei loro accessi, aggiornamento della
  quantità dell'abbonamento, ricalcolo del dovuto, chiusura della riduzione.
- `blocksAdditions(tenant)` — il predicato che UC 0103 interroga prima di creare un invito.

## Passo 3 — Il lavoro periodico

**File nuovo**: `SeatDowngradeSweeper.java`, sul modello degli spazzini già presenti
([AccountDeletionSweeper.java](../../../../services/core/src/main/java/app/appgrove/core/gdpr/AccountDeletionSweeper.java)):
periodico, prende le riduzioni con data di esecuzione passata e le esegue una per una, ognuna nella propria
transazione (un guasto su un account non blocca gli altri).

**Misura**: contatore delle riduzioni scadute e non ancora eseguite, con allarme sopra zero per più di un
periodo di controllo. Registrarla dove vivono le altre misure del core. Senza questa misura, una riduzione non
eseguita è invisibile e il cliente paga.

**Ordine col rinnovo**: se il rinnovo dell'abbonamento è guidato dagli eventi del fornitore di pagamento, la
riduzione va eseguita **prima** che il nuovo periodo venga calcolato. Verificare come arrivano i due eventi e,
se l'ordine non è garantito, eseguire la riduzione **all'arrivo** dell'evento di rinnovo invece che dallo
spazzino. Decisione da prendere leggendo
[PaddleWebhookConsumer.java](../../../../services/core/src/main/java/app/appgrove/core/billing/PaddleWebhookConsumer.java).

## Passo 4 — Interfaccia di rete

`SeatDowngradeResource.java`: creazione (con l'elenco delle persone), annullamento, rimozione di una persona,
lettura dello stato. Tutto riservato all'owner.

**Modifica**: la lettura dei posti (UC 0103) include lo stato della riduzione, così la schermata la mostra
senza una seconda chiamata.

## Passo 5 — La schermata

**Modifica**: `MembersPage.tsx` —

1. selezione multipla nell'elenco delle persone (caselle di scelta) con il comando «indica per la
   cessazione», che mostra l'effetto **prima** della conferma;
2. riquadro di avviso quando la riduzione è in attesa, con l'elenco delle persone indicate, la rimozione
   singola e il comando di annullamento;
3. stato «in cessazione dal …» nella colonna di stato;
4. comando di invito **disabilitato con spiegazione** durante l'attesa, e il testo che offre le due vie
   (annullare, o attendere).

Traduzioni nelle cinque lingue, con le date formattate secondo la lingua.

## Passo 6 — Collaudi

- `SeatDowngradeServiceTest.java`: richiesta, annullamento, rimozione singola, chiusura automatica a zero
  persone, rifiuto dell'owner, rifiuto della seconda riduzione.
- Integrazione: invito bloccato durante l'attesa (è la prova che lega questa storia a UC 0103).
- `SeatDowngradeSweeperTest.java`: esecuzione alla scadenza; esecuzione interrotta e ritentata che non
  rimuove due volte.
- Ordine col rinnovo: prova che il nuovo periodo nasce con la quantità ridotta.
- `frontend`: selezione multipla, riquadro di avviso, invito disabilitato, annullamento.

## Verifica finale

```bash
cd services && mvn -B -pl core -am test
cd ../frontend && npm run typecheck && npm test
cd .. && ./run-tests.sh backend frontend
```

## Trappole note

1. **Non applicare la riduzione subito** «per semplicità»: cambierebbe l'importo a metà periodo e
   contraddirebbe la permanenza minima mensile.
2. **L'idempotenza dell'esecuzione**: lo spazzino può girare due volte sulla stessa riduzione. Ogni passo va
   scritto per essere ripetibile senza danni.
3. **La riduzione scaduta e non eseguita** è il guasto che costa al cliente e che nessuno vede: la misura del
   passo 3 non è un extra.
