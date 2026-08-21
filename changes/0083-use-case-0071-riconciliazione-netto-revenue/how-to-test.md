# Come collaudare a mano la change 0083 (UC 0071 — riconciliazione netto/revenue)

Questa change risponde a una domanda che finora nessuna pagina rispondeva: **quanto è davvero entrato**. Il
fornitore di pagamento incassa dal cliente, trattiene le proprie commissioni e ci accredita il netto con
accrediti periodici — quindi il fatturato lordo che vedi nella console e il denaro sul conto **non sono la
stessa cifra**, e la differenza non è una percentuale fissa.

I test automatici sanno dire che i numeri tornano. **Non** sanno dire se, aprendo la pagina, in cinque secondi
capisci quanto ti resta in tasca e se c'è qualcosa che non quadra. Quella parte si fa con gli occhi.

Tempo indicativo: **20 minuti** di controlli visivi, **15** di controlli non visivi.

---

## Parte 0 — Avvio e utenti

**Azione** — dalla radice del repository:

```bash
./app-start.sh
```

**Risultato atteso** — l'avvio arriva in fondo senza errori e il riepilogo elenca backoffice e console di
amministrazione. Password di tutti gli utenti di prova: `Password1!`.

| Utente | Dove | Cosa serve qui |
|---|---|---|
| `admin@appgrove.test` | <https://admin.local.appgrove.app> | è l'unico che vede la riconciliazione |
| `owner@acme.test` | <https://app.local.appgrove.app> | serve a generare pagamenti, rimborsi e accrediti |

> Se lo stack era già acceso, rilancia `./app-start.sh` (il seed è idempotente).
> Se il browser protesta per il certificato, è il proxy locale: accetta l'eccezione.

**La migrazione del database** — la prima accensione dopo questa change applica `V15`. Verifica che sia
passata:

```bash
docker compose -f dev/docker-compose.yml exec -T postgres \
  psql -U appgrove -d appgrove -tAc \
  "select version, description, success from platform.flyway_schema_history where version = '15';"
```

**Risultato atteso** — una riga `15 | payout reconciliation | t`. Se manca, il resto non ha senso.

---

## Parte 1 — La pagina esiste, si raggiunge, e da sola

### 1.1 La voce di menu

**Azione** — entra come `admin@appgrove.test` e guarda il menu laterale.

**Risultato atteso** — sotto l'etichetta di gruppo **`Revenue`** ci sono **due** voci: *Billing* (che c'era) e
**Reconciliation** (nuova), con l'icona di una banca. Non deve essere finita sotto *Platform* né sotto
*Governance*: è una vista sui soldi.

### 1.2 Il primo ingresso: pagina vuota, non pagina rotta

**Azione** — su uno stack appena avviato, prima di generare qualunque pagamento, clicca **Reconciliation**.

**Risultato atteso** — titolo **Reconciliation**, sottotitolo che spiega di cosa si tratta, e — se non ci sono
transazioni — il messaggio **`No transactions recorded yet.`**. Nessuna tabella vuota con le intestazioni
sospese nel nulla, nessun `NaN`, nessun `€0.00` presentato come se fosse un dato.

> Nello stack locale il seed porta già delle transazioni: in quel caso vedi direttamente i totali. Per
> ottenere davvero la pagina vuota serve un database pulito (`./dev.sh reset`, se disponibile) — se non ti
> interessa, salta e vai al punto 1.3.

### 1.3 Il breadcrumb

**Azione** — con la pagina aperta, guarda la barra in alto.

**Risultato atteso** — `PLATFORM › Reconciliation`, non `PLATFORM › reconciliation` (lo slug grezzo). Se vedi
la parola minuscola, manca la voce nella mappa del breadcrumb.

---

## Parte 2 — I quattro totali, e cosa raccontano

**Azione** — genera un po' di movimento. Entra nel backoffice come `owner@acme.test`, vai su **App catalog**,
acquista un'app a pagamento e completa il pagamento finto. Poi torna sulla console di amministrazione,
**Reconciliation**, e ricarica.

**Risultato atteso** — quattro riquadri in riga:

| Riquadro | Cosa deve mostrare |
|---|---|
| **Gross collected** | l'importo **lordo** delle transazioni riuscite, formattato in euro (es. `€19.00`) |
| **Provider fees** | quanto trattiene il fornitore — **non zero**: circa il 5% più mezzo euro per transazione |
| **Net revenue** | lordo meno commissioni |
| **Awaiting payout** | uguale al netto, perché nessun accredito è ancora arrivato |

Il controllo che conta: **netto = lordo − commissioni**, a occhio. Se il netto coincide col lordo, la
commissione non viene calcolata e qualcosa è rotto.

**Azione** — guarda subito sotto i riquadri.

**Risultato atteso** — la riga grigia **`N transactions have an estimated fee: the provider did not declare
it, so it was computed with the price-list formula.`**. È voluta: in locale il simulatore **non** dichiara la
commissione, quindi ogni riga è stimata e la pagina lo dice invece di far finta di sapere. Se questa nota non
compare quando ci sono transazioni locali, l'onestà della vista si è persa per strada.

---

## Parte 3 — Le righe per mese

**Azione** — scorri fino alla tabella **`By month of charge`**.

**Risultato atteso** — una riga per mese, dal più recente, con: mese (`2026-08`), lordo, commissioni, netto,
storni, numero di transazioni e la percentuale di **peso delle commissioni** dentro un contrassegno.

Due cose da guardare bene:

1. il mese è quello dell'**addebito**, non quello dell'eventuale accredito. Lo verificherai davvero al punto 5;
2. la percentuale su un'app economica mensile è **alta** (su 9 euro la mezza commissione fissa pesa da sola più
   del 5%). Se supera l'8% il contrassegno diventa **giallo** e in cima alla pagina compare il riquadro
   d'avviso **`Fees weigh more than expected`**. È il segnale che le micro-transazioni costano.

**Azione** — se nel tuo stack tutte le percentuali sono sotto l'8% e vuoi vedere l'avviso, genera una
transazione piccola su un'app economica e ricarica.

**Risultato atteso** — comparsa del riquadro giallo e del contrassegno giallo sulla riga del mese.

---

## Parte 4 — Gli accrediti e la quadratura (il cuore della change)

Qui serve il simulatore del fornitore. Prendi un gettone per l'utente proprietario:

```bash
TOKEN=$(curl -sk https://app.local.appgrove.app/api/auth/login \
  -H 'content-type: application/json' \
  -d '{"email":"owner@acme.test","password":"Password1!"}' \
  | python3 -c 'import sys,json;print(json.load(sys.stdin)["access_token"])')
```

Ti servono anche gli identificativi dell'app e della fascia che hai acquistato:

```bash
docker compose -f dev/docker-compose.yml exec -T postgres \
  psql -U appgrove -d appgrove -c "
    select a.slug, s.app_id, s.app_tier_id
      from platform.subscription s join platform.app a on a.id = s.app_id
     where s.tenant_id = 'a0000000-0000-4000-8000-000000000001';"
```

### 4.1 Un accredito che quadra

**Azione** — chiedi al simulatore di accreditare il netto in attesa (sostituisci gli identificativi):

```bash
curl -sk -X POST https://app.local.appgrove.app/api/platform/v1/dev/paddle/scenarios/payout \
  -H "authorization: Bearer $TOKEN" -H 'content-type: application/json' \
  -d '{"appId":"<app-id>","appTierId":"<tier-id>"}'
```

Attendi ~3 secondi (il consumer svuota la coda ogni 2) e ricarica la pagina di riconciliazione.

**Risultato atteso, con gli occhi**:

- il riquadro **`Awaiting payout`** è **sceso** (a zero, se hai accreditato tutto);
- nella tabella **`Payouts and matching`** è comparsa una riga nuova, con l'identificativo `pay_…`, la data,
  l'intervallo di addebiti coperti, l'importo accreditato, la somma delle righe, uno **scostamento a zero** e
  il contrassegno **verde `Matched`**.

**Il controllo non visivo** — l'accredito e il suo dettaglio devono esistere entrambi:

```bash
docker compose -f dev/docker-compose.yml exec -T postgres \
  psql -U appgrove -d appgrove -c "
    select p.paddle_payout_id, p.amount, p.currency, count(l.*) as righe, sum(l.net_amount) as somma_righe
      from platform.payout p left join platform.payout_line l on l.payout_id = p.id
     group by 1,2,3 order by p.paid_at desc limit 5;"
```

**Risultato atteso** — `amount` = `somma_righe` per l'accredito appena creato.

### 4.2 Lo stesso evento due volte non raddoppia niente

**Azione** — rilancia lo **stesso** comando del punto 4.1.

**Risultato atteso** — risposta `400` con il messaggio **`nessuna transazione da accreditare per questa app`**:
tutto è già stato accreditato, e il simulatore lo dice invece di inventare un secondo accredito. Nella pagina,
nessuna riga nuova.

### 4.3 Un rimborso: il denaro torna indietro, e si vede dove

**Azione** — genera un altro pagamento (acquista un'altra app, o emetti lo scenario `renewal`), poi chiedi il
rimborso:

```bash
curl -sk -X POST https://app.local.appgrove.app/api/platform/v1/dev/paddle/scenarios/refund \
  -H "authorization: Bearer $TOKEN" -H 'content-type: application/json' \
  -d '{"appId":"<app-id>","appTierId":"<tier-id>"}'
```

Attendi ~3 secondi e ricarica.

**Risultato atteso, con gli occhi**:

- nella riga del mese, la colonna **`Reversed`** è cresciuta dell'importo rimborsato e il **lordo è sceso**
  della stessa cifra: il denaro restituito non deve restare fra gli incassi;
- nella tabella degli accrediti c'è una riga **nuova** con importo **negativo** (es. `-€17.55`), sempre
  **`Matched`**: la restituzione è un accredito negativo, non un accredito mancante;
- l'accredito **precedente** è ancora **`Matched`**. È il punto più importante di tutta la verifica: un fatto
  successivo non deve far apparire sbagliato un accredito che al momento era corretto.

**Il controllo non visivo** — la transazione ha cambiato stato, non è stata duplicata:

```bash
docker compose -f dev/docker-compose.yml exec -T postgres \
  psql -U appgrove -d appgrove -c "
    select paddle_transaction_id, status, amount, fee_amount, net_amount, fee_source
      from platform.billing_transaction
     where tenant_id = 'a0000000-0000-4000-8000-000000000001'
     order by billed_at desc limit 5;"
```

**Risultato atteso** — la riga rimborsata ha `status = refunded` e `net_amount = 0`, mantiene la sua
`fee_amount` (il fornitore la trattiene comunque) e `fee_source = estimated`. **Non** deve esserci una seconda
riga con lo stesso `paddle_transaction_id`.

### 4.4 Un accredito che NON quadra

Questo caso il simulatore non lo produce (per costruzione accredita sempre l'importo giusto): si forza a mano
alterando il dettaglio di un accredito esistente.

**Azione**:

```bash
docker compose -f dev/docker-compose.yml exec -T postgres \
  psql -U appgrove -d appgrove -c "
    update platform.payout_line set net_amount = net_amount - 50
     where payout_id = (select id from platform.payout order by paid_at desc limit 1);"
```

Ricarica la pagina.

**Risultato atteso** — quella riga passa a contrassegno **rosso `Difference`** e la colonna **`Difference`**
mostra **`€0.50`**. Uno scostamento non deve essere un dettaglio che si nota solo leggendo i numeri: deve
saltare all'occhio dal colore.

**Azione (ripristino)** — rimetti a posto:

```bash
docker compose -f dev/docker-compose.yml exec -T postgres \
  psql -U appgrove -d appgrove -c "
    update platform.payout_line set net_amount = net_amount + 50
     where payout_id = (select id from platform.payout order by paid_at desc limit 1);"
```

### 4.5 Valute diverse: nessuno scostamento inventato

**Azione** — sporca la valuta di una riga di dettaglio:

```bash
docker compose -f dev/docker-compose.yml exec -T postgres \
  psql -U appgrove -d appgrove -c "
    update platform.payout_line set currency = 'USD'
     where payout_id = (select id from platform.payout order by paid_at desc limit 1);"
```

Ricarica.

**Risultato atteso** — contrassegno **giallo `Mixed currency`** e colonna **`Difference`** con un trattino
**`—`**. Non deve comparire un numero: sommare euro e dollari darebbe una cifra che *sembra* una differenza e
non lo è. Rimetti `EUR` quando hai finito.

---

## Parte 5 — L'accredito a cavallo di due mesi

**Azione** — sposta indietro di un mese la data di addebito di una transazione già accreditata:

```bash
docker compose -f dev/docker-compose.yml exec -T postgres \
  psql -U appgrove -d appgrove -c "
    update platform.billing_transaction
       set billed_at = billed_at - interval '1 month'
     where paddle_transaction_id = (
       select paddle_transaction_id from platform.billing_transaction
        where status = 'paid' order by billed_at desc limit 1);"
```

Ricarica.

**Risultato atteso**:

- nella tabella per mese compaiono **due** righe (mese corrente e mese precedente), ciascuna con la propria
  quota di lordo: la transazione è rimasta nel mese del **suo addebito**, non è migrata nel mese
  dell'accredito;
- l'accredito che la contiene resta **una sola riga**, e la colonna **`Charges covered`** ora mostra un
  intervallo che **attraversa i due mesi** (es. `01/07/2026 → 02/08/2026`).

---

## Parte 6 — L'accredito atteso che non arriva

**Azione** — invecchia una transazione non ancora accreditata oltre la soglia di 14 giorni:

```bash
docker compose -f dev/docker-compose.yml exec -T postgres \
  psql -U appgrove -d appgrove -c "
    update platform.billing_transaction
       set billed_at = now() - interval '60 days'
     where status = 'paid'
       and not exists (select 1 from platform.payout_line l
                        where l.paddle_transaction_id = billing_transaction.paddle_transaction_id);"
```

Se al momento è tutto accreditato, genera prima un pagamento nuovo (senza accreditarlo) e poi ripeti.
Ricarica la pagina.

**Risultato atteso, con gli occhi** — in cima alla pagina, sotto i totali, compare un riquadro **rosso tenue**
con **`Expected payout has not arrived`** e la spiegazione, che riporta **la data** da cui il netto è fermo e
**la finestra di 14 giorni**. Deve essere un avviso in evidenza, non una nota a piè di pagina.

**Il controllo non visivo** — la stessa condizione deve lasciare traccia nei log del servizio. La rilevazione
gira una volta all'ora, quindi il modo rapido è leggere i log dopo un riavvio del solo `core`:

```bash
./dev.sh service platform 2>&1 | grep -i "reconciliation"
```

**Risultato atteso** — una riga a livello **WARN** contenente `billing.reconciliation accredito atteso non
ricevuto` con il netto fermo e la data. Se il netto è sotto soglia, la riga **non** deve esserci: un allarme
che suona sempre non è un allarme.

---

## Parte 7 — Chi può vedere questi numeri

**Azione (visiva)** — esci dalla console di amministrazione ed entra come `owner@acme.test` su
<https://admin.local.appgrove.app>.

**Risultato atteso** — non entri: vieni respinto sulla pagina «Forbidden». La console intera è riservata
all'amministratore di piattaforma, riconciliazione compresa.

**Azione (non visiva)** — chiedi la vista direttamente con un gettone che **non** ha il ruolo di piattaforma:

```bash
curl -sk -o /dev/null -w '%{http_code}\n' \
  https://admin.local.appgrove.app/api/platform/v1/admin/reconciliation \
  -H "authorization: Bearer $TOKEN"
```

**Risultato atteso** — **`403`**. Non `200` con dati filtrati, non `401`: i dati economici globali non sono
affari di un titolare di conto.

**Azione** — la stessa chiamata **senza** alcun gettone.

**Risultato atteso** — **`401`**.

**Azione (controprova)** — verifica che al cliente **non** sia cambiato nulla: come `owner@acme.test`, apri
**Billing** nel backoffice e guarda lo storico dei pagamenti.

**Risultato atteso** — importi, esiti e ricevute come prima. **Nessuna** colonna con la commissione o con il
netto: al cliente interessa quanto ha pagato, non quanto ci resta.

---

## Parte 8 — I dati personali seguono la persona

**Azione** — come `owner@acme.test`, vai su **I miei dati** (`/privacy`), chiedi l'**esportazione
dell'account**, attendi il completamento e apri lo ZIP.

**Risultato atteso** — dentro `platform.json`, nella chiave **`billing_transactions`**, ogni riga porta ora
anche **`fee_amount`**, **`net_amount`** e **`fee_source`** accanto ai campi che c'erano già. Sono dati che
riguardano quel pagamento: un'esportazione che li omettesse sarebbe incompleta.

**Azione** — apri il registro dei trattamenti `docs/compliance/ropa.it.md` e cerca «commissione».

**Risultato atteso** — due voci nuove per `billing_transactions.fee_amount` e `billing_transactions.net_amount`,
con finalità (riconciliazione), base giuridica (esecuzione del contratto) e conservazione uguali a quelle
dell'importo. Se il registro non le contiene, il manifesto e il codice si sono disallineati.

---

## Parte 9 — Le lingue

**Azione** — dal menu della barra superiore cambia lingua e riapri **Reconciliation**, per almeno **italiano**
e **tedesco**.

**Risultato atteso** — titolo, sottotitolo, i quattro totali, le intestazioni delle due tabelle, i tre esiti di
quadratura (`Quadra` / `Scostamento` / `Valuta mista`) e i due avvisi sono **tutti tradotti**. Nessuna chiave
grezza tipo `admin.reconciliation.colNet` a schermo, nessuna parola rimasta in inglese in mezzo all'italiano.

---

## Cosa NON deve esserci (controlli negativi)

- **Nessuna vista del netto per il cliente**: il netto è un dato della piattaforma, non del cliente.
- **Nessun ricavo ricorrente mensile e nessun tasso di abbandono** in questa pagina: appartengono alla
  panoramica (UC 0021) e non sono stati anticipati.
- **Nessun accredito registrato senza il suo dettaglio** quando il fornitore lo comunica: se vedi righe con
  `Sum of lines` a zero e un importo accreditato non nullo, il dettaglio non è stato scritto.
- **Nessuna commissione sulle transazioni antecedenti a questa change**: restano vuote di proposito
  (`fee_amount is null`) — inventarla a posteriori sarebbe scrivere un numero falso.
