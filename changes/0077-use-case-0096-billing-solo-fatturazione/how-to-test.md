# Come verificare a mano la change 0077 — Billing di sola fatturazione

Questa change **toglie** metà di una pagina e **aggiunge** una sezione che nel prodotto non esisteva
affatto. Quasi tutto si verifica con gli occhi, in un browser. Sotto: prima il percorso visivo completo,
poi i controlli che gli occhi non possono fare (chiamate all'API, righe di database).

Tutti i comandi partono dalla radice del repository, sul branch
`change/0077-use-case-0096-billing-solo-fatturazione`.

---

## 0. Avvio dello stack locale

**Azione**

```bash
./app-start.sh
```

**Risultato atteso** — al termine il riepilogo elenca il backoffice su `https://app.local.appgrove.app` e
gli utenti di prova (password `Password1!`): `owner@acme.test`, `admin@acme.test`, `member@acme.test`,
`bob@bob.test`, `admin@appgrove.test`. Fra i servizi c'è anche **Mailpit** su `http://localhost:8025`
(serve solo al punto 8).

> Il seed di questa change aggiunge quattro pagamenti finti ma deterministici: se lo stack era già su da
> prima, rilancia `./app-start.sh` (il seed è idempotente) o `./dev.sh seed`, altrimenti lo storico
> risulterà vuoto e sembrerà un difetto.

> Se il browser protesta per il certificato di `app.local.appgrove.app`, è il proxy locale: accetta
> l'eccezione una volta sola.

---

## 1. La pagina non è più una vetrina

**Azione** — apri `https://app.local.appgrove.app`, entra come **`owner@acme.test`** e vai su **Billing**
dal menu a sinistra (gruppo PLATFORM).

**Risultato atteso**

- il titolo grande è **`Billing`** e sotto si legge **«Manage your plans, payments and receipts.»**;
- **non** c'è più il titolo «Get an app»;
- **non** c'è più alcuna griglia di app acquistabili, e in tutta la pagina non compare **nessun pulsante
  `Subscribe`**;
- la pagina è fatta di **due sezioni**, in quest'ordine: **`Your subscriptions`** e
  **`Payments & receipts`**.

> È il cuore della change: chi cerca «dove si compra» ora va in **App catalog**, chi cerca «quanto ho
> pagato» resta qui.

---

## 2. Gli abbonamenti: com'erano, dove devono stare

**Azione** — resta su `/billing` come `owner@acme.test` e guarda la sezione **`Your subscriptions`**.

**Risultato atteso** — una card per abbonamento del workspace Acme, con nome dell'app, badge di fase,
piano, limiti inclusi, data di rinnovo e i pulsanti d'azione già esistenti (`Change plan`, `Cancel`,
`Manage payment & invoices` quando disponibile). In particolare:

| App | Badge atteso | Nota |
|---|---|---|
| **Teams** | `Active` (verde) | rinnovo indicato |
| **Notes** | `Payment pending` (giallo) | è quello che al punto 4 vedrai fallito nello storico |
| **Legacy** | `Suspended` + `Active` | **due** badge: l'app è spenta dalla piattaforma |

**Azione** — guarda bene la card di **Legacy**.

**Risultato atteso** — sotto l'intestazione c'è un **riquadro d'avviso** con
**«App suspended by the platform»** e la spiegazione «…your subscription stays valid and your data is
untouched…», più — novità di questa change — il pulsante **`Contact support`**, che porta alla pagina
Supporto.

> Nota deliberata: l'avviso **non** dice che l'abbonamento smette di essere addebitato. Nulla nel prodotto
> lo sospende, quindi sarebbe una promessa falsa. La domanda è tracciata come punto aperto in UC 0076.

---

## 3. Lo storico pagamenti — la sezione nuova

**Azione** — sempre come `owner@acme.test`, scorri fino a **`Payments & receipts`**.

**Risultato atteso** — sotto il titolo si legge «Processed by Paddle, our merchant of record. Receipts open
on Paddle.» e c'è una **tabella** con le colonne `DATE · APP · DESCRIPTION · AMOUNT · STATUS · RECEIPT`, e
queste righe (dalla più recente):

| Data | App | Descrizione | Importo | Esito | Ricevuta |
|---|---|---|---|---|---|
| 2 giu 2024 | Notes | `Notes Pro — monthly` | `€9.00` | badge **`Failed`** (giallo) | **«Not available yet»**, nessun link |
| 1 giu 2024 | Teams | `Teams — monthly` | `€19.00` | badge `Paid` (verde) | link **`Receipt ↗`** |
| 1 mag 2024 | Teams | `Teams — monthly` | `€19.00` | badge `Paid` | link `Receipt ↗` |

Tre cose da guardare bene:

1. gli **importi sono in carattere a larghezza fissa** e incolonnati: si confrontano a colpo d'occhio;
2. il **pagamento fallito c'è**. Non è nascosto: è la riga da cui l'utente capisce perché l'abbonamento di
   Notes è "Payment pending";
3. la riga senza ricevuta **esiste comunque**, con lo stato onesto «Not available yet» al posto del link.

**Azione** — clicca un link **`Receipt ↗`**.

**Risultato atteso** — si apre una **scheda nuova** verso `sandbox-my.paddle.com` (in locale è un indirizzo
finto: la pagina non esisterà, e va benissimo — ciò che si verifica è che il link ci sia, porti fuori e non
sostituisca la scheda corrente).

---

## 4. Lo storico è del workspace, non dell'utente

**Azione** — esci e rientra come **`bob@bob.test`**, poi vai su `/billing`.

**Risultato atteso**

- la tabella contiene **una sola riga**: `Teams — monthly`, `€19.00`, `Paid`, del 1° maggio 2024;
- **nessuna** delle righe di Acme è visibile;
- l'abbonamento di Bob a Teams risulta **disdetto** nella sezione sopra, ma **il pagamento resta nello
  storico**: una disdetta non cancella la storia di ciò che è stato pagato.

---

## 5. Lo stato vuoto manda al catalogo (non a una griglia d'acquisto)

**Azione** — registra un workspace nuovo: dalla schermata d'ingresso scegli **Sign up**, usa un indirizzo
qualsiasi (per esempio `nuovo@prova.test`), poi apri **Mailpit** su `http://localhost:8025`, apri l'email di
verifica e segui il collegamento. Completa l'ingresso e vai su **Billing**.

**Risultato atteso**

- sezione `Your subscriptions`: una card con **«You don't have any subscriptions yet.»** e un pulsante
  **`Browse the app catalog`**;
- cliccandolo si arriva su **`/catalog`**, la pagina **App catalog** — **non** su una griglia dentro Billing;
- sezione `Payments & receipts`: la card **«No payments yet.»**. Nessun errore, nessuna tabella vuota con le
  intestazioni.

---

## 6. La via al piano a pagamento per un'app freemium (il buco chiuso)

**Azione** — con lo **stesso workspace nuovo** del punto 5, vai su **`/catalog`** e guarda la card di
**Notes**.

**Risultato atteso**

- badge **`Active`** (l'app ha una fascia gratuita: è già usabile) e, a destra, il nome del piano
  **`Notes Free`**;
- accanto al pulsante `Open` c'è ora un secondo pulsante: **`Upgrade plan`**.

> Prima di questa change quella card offriva **solo** `Open`, e il piano a pagamento di Notes non era
> comprabile da nessuna parte del prodotto: in Billing non esisteva alcuna card di abbonamento su cui agire.

**Azione** — clicca **`Upgrade plan`**.

**Risultato atteso** — senza cambiare pagina (il titolo resta «App catalog») compare la scelta della fascia,
con la card **`Notes Pro`**, il prezzo e il pulsante `Subscribe`. Completa l'acquisto nell'overlay di
pagamento finto.

**Risultato atteso** — «Activating your subscription…» → **«All set! Your subscription is active.»**

**Azione** — vai su **Billing**.

**Risultato atteso** — **questa è la verifica più importante della change**:

- in `Your subscriptions` compare la card di **Notes**;
- in `Payments & receipts` compare **una riga nuova**, con la data di oggi, `Notes`, la descrizione
  `Notes Pro — monthly`, l'importo **vero letto dal listino** (`€9.00`), il badge **`Paid`** e un link
  `Receipt ↗`.

Quella riga **non** è stata scritta dal browser: è arrivata dal webhook, per la stessa strada dei pagamenti
veri (vedi il punto 11 per la prova dal database).

---

## 7. Il confine dei ruoli

**Azione** — esci e rientra come **`member@acme.test`**, poi vai su `/billing`.

**Risultato atteso**

- la sezione `Your subscriptions` c'è, come prima;
- la sezione `Payments & receipts` **c'è come intestazione**, ma al posto della tabella si legge
  **«Only owners and admins can see payments and receipts.»**;
- **nessuna tabella**, nessun importo, nessun link a ricevute.

**Azione** — rientra come **`admin@acme.test`** e torna su `/billing`.

**Risultato atteso** — l'admin **vede** la tabella completa, come l'owner.

---

## 8. Le cinque lingue

**Azione** — come `owner@acme.test` su `/billing`, cambia lingua dal menu in alto a destra fra **English,
Italiano, Français, Español, Deutsch**.

**Risultato atteso** — a ogni cambio, **immediatamente e senza ricaricare**: titolo, sottotitolo, i titoli
delle due sezioni, le intestazioni di colonna, i badge di esito (`Pagato`/`Fallito`…), il testo
«Non ancora disponibile», la parola «Ricevuta» **e il ciclo dentro la descrizione** (`Notes Pro — mensile`)
passano nella lingua scelta. Nessuna chiave grezza tipo `payments.title` deve comparire.

---

## 9. Un guasto dello storico non spegne gli abbonamenti

**Azione** — con `/billing` aperto come `owner@acme.test`, ferma il solo servizio `core` e ricarica:

```bash
./dev.sh services               # per leggere la porta del core (di norma 8080)
lsof -ti :8080 | xargs kill     # ferma il solo core
```

**Risultato atteso** (con il core giù entrambe le sezioni sono in errore — è corretto). Rimetti su il core:

```bash
./app-start.sh --no-seed
```

**Azione (la prova vera del guasto isolato)** — apri gli strumenti per sviluppatori del browser, scheda
**Rete**, e blocca la sola richiesta `GET /api/platform/v1/me/payments` (in Chrome: tasto destro sulla
richiesta → *Block request URL*), poi ricarica la pagina.

**Risultato atteso**

- la sezione **`Payments & receipts`** mostra **«We couldn't load your payment history.»** con un pulsante
  **`Retry`**;
- la sezione **`Your subscriptions`** sopra **resta perfettamente funzionante**: le card ci sono e i pulsanti
  `Change plan` / `Cancel` sono cliccabili;
- premendo `Retry` dopo aver tolto il blocco, la tabella ricompare **senza ricaricare la pagina**.

---

## 10. Controlli non visivi — l'API

**Azione** — prendi un token dell'owner e chiama la lettura nuova. Il modo più rapido è dagli strumenti per
sviluppatori: scheda **Rete** → richiesta `me/payments` → *Copy as cURL*. Oppure a mano:

```bash
# La risposta della lettura, così com'è (owner)
curl -sk https://app.local.appgrove.app/api/platform/v1/me/payments \
  -H "Authorization: Bearer <token-owner>" | jq
```

**Risultato atteso** — un oggetto `{ "payments": [ … ] }` con, per ogni riga: `billedAt`, `appSlug`,
`appName`, `planName`, `billingCycle`, `amount` (in **centesimi**: `1900`, non `19`), `currency`, `status`
(`paid` / `failed` / `disputed`) e `receiptUrl` **assente** quando la ricevuta non c'è (chiave proprio
mancante, non `null`).

**Azione** — ripeti con il token di un **member**.

**Risultato atteso** — **403**, con corpo `application/problem+json`. È il divieto vero: l'interfaccia
nasconde la sezione, ma è il backend a decidere.

**Azione** — prova a farti dare i pagamenti di un altro workspace.

**Risultato atteso** — **non c'è modo**: la lettura non accetta **alcun** parametro. Qualunque cosa si
aggiunga all'indirizzo (`?tenantId=…`) viene semplicemente ignorata e la risposta resta quella del proprio
workspace. Verificalo:

```bash
curl -sk "https://app.local.appgrove.app/api/platform/v1/me/payments?tenantId=a0000000-0000-4000-8000-000000000002" \
  -H "Authorization: Bearer <token-owner-acme>" | jq '.payments | length'
```

**Risultato atteso** — il numero di righe di **Acme** (3, o 4 dopo un acquisto), **non** quelle di Bob.

---

## 11. Controlli non visivi — il database

**Azione** — apri il database locale:

```bash
docker compose -f dev/docker-compose.yml exec -T postgres \
  psql -U appgrove -d appgrove -c "
    select a.slug, bt.status, bt.amount, bt.currency, bt.billing_cycle,
           (bt.receipt_url is not null) as ha_ricevuta, bt.billed_at
      from platform.billing_transaction bt
      left join platform.app a on a.id = bt.app_id
     where bt.tenant_id = 'a0000000-0000-4000-8000-000000000001'
     order by bt.billed_at desc;"
```

**Risultato atteso** — le righe di Acme: due `teams`/`paid`/`1900` con ricevuta, una `notes`/`failed`/`900`
**senza** ricevuta, più l'eventuale riga dell'acquisto fatto al punto 6.

**Azione (idempotenza della pipeline)** — riemetti due volte lo stesso scenario di rinnovo per un'app e
conta le righe:

```bash
# 1) conta le transazioni prima
docker compose -f dev/docker-compose.yml exec -T postgres \
  psql -U appgrove -d appgrove -tAc \
  "select count(*) from platform.billing_transaction where tenant_id='a0000000-0000-4000-8000-000000000001';"

# 2) emetti lo scenario 'renewal' (endpoint dev, solo in locale) — sostituisci <app-id> e <tier-id>
curl -sk -X POST https://app.local.appgrove.app/api/platform/v1/dev/paddle/scenarios/renewal \
  -H "Authorization: Bearer <token-owner-acme>" -H 'Content-Type: application/json' \
  -d '{"appId":"1c4ea96d-bc57-3109-9c83-0933a3553779","appTierId":"e075f588-c33b-35c5-af41-285c1d006f8e"}'

# 3) attendi qualche secondo (il consumer drena ogni 2s) e riconta
```

**Risultato atteso** — **una sola riga in più** per ogni scenario emesso (il rinnovo produce una
transazione), e ricaricando Billing la nuova riga compare in cima con il badge `Paid`. Riemettendo lo
**stesso** evento non nascono duplicati: l'unicità è sul riferimento della transazione presso il fornitore.

**Azione (esportazione dei dati — GDPR)** — come `owner@acme.test` vai su **I miei dati** (`/privacy`),
chiedi l'**esportazione dell'account**, attendi il completamento e scarica lo ZIP.

**Risultato atteso** — dentro `platform.json` c'è la chiave **`billing_transactions`** con le righe di
questo workspace (importo, valuta, esito, riferimento della transazione, collegamento alla ricevuta). Se
manca, l'esportazione non è più completa.

**Azione (cancellazione — GDPR)** — sempre da `/privacy`, con il workspace di prova creato al punto 5 (non
con Acme!), chiedi l'**eliminazione dell'account** e lascia scadere o forza il periodo di grazia.

**Risultato atteso** — dopo la purga, nessuna riga di `platform.billing_transaction` con quel `tenant_id`:

```bash
docker compose -f dev/docker-compose.yml exec -T postgres \
  psql -U appgrove -d appgrove -tAc \
  "select count(*) from platform.billing_transaction where tenant_id='<tenant-del-workspace-di-prova>';"
# atteso: 0
```

---

## 12. Email

**Azione** — nessuna.

**Risultato atteso** — questa change **non manda alcuna email**: non ci sono notifiche di pagamento, né
avvisi di ricevuta disponibile. Mailpit serve solo per completare la registrazione del punto 5. Se
apparisse un'email nuova legata a Billing, è un difetto.
