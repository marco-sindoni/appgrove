# Come collaudare a mano la change 0082 (UC 0067 — sezione "Abbonamenti" self-service)

Questa change non aggiunge azioni: le azioni c'erano già. Aggiunge **il contesto per decidere** prima di
compierle — quanto stai consumando, quanto costa il piano che stai per scegliere, cosa succede e da quando.
I test automatici sanno dire che i testi compaiono; **non** sanno dire se, guardando la card, capisci in tre
secondi cosa sta succedendo al tuo abbonamento. Quella parte si fa con gli occhi.

Tempo indicativo: **25 minuti** di controlli visivi, **10** di controlli non visivi.

---

## Parte 0 — Avvio

**Azione** — dalla radice del repository:

```bash
./app-start.sh
```

**Risultato atteso** — l'avvio arriva in fondo senza errori e il riepilogo elenca il backoffice su
`https://app.local.appgrove.app`. Utenti di prova, password `Password1!`:

| Utente | Ruolo | Workspace | Cosa ha |
|---|---|---|---|
| `owner@acme.test` | titolare | Acme | Teams **attivo**, Notes **con pagamento fallito** |
| `member@acme.test` | membro | Acme | gli stessi abbonamenti, ma non può toccarli |
| `bob@bob.test` | titolare | Bob | Notes **in prova**, Teams **disdetto/scaduto** |

> Se lo stack era già acceso da prima, rilancia `./app-start.sh` (il seed è idempotente) oppure `./dev.sh seed`.
> Se il browser protesta per il certificato, è il proxy locale: accetta l'eccezione.

Tutta la sezione vive dentro **Billing** (`/billing`), sotto il titolo **`Your subscriptions`**: è lì per
scelta (la pagina Fatturazione è la casa degli abbonamenti dalla UC 0096, il catalogo è un'altra cosa). Se ti
aspettavi una nuova voce di menù "Abbonamenti", **non deve esserci**.

---

## Parte 1 — La card, a colpo d'occhio

### 1.1 Il caricamento non fa saltare la pagina

**Azione** — entra come `owner@acme.test`, vai su **Billing** e ricarica con `Cmd+Shift+R` tenendo gli occhi
sulla parte alta della pagina.

**Risultato atteso** — per una frazione di secondo compaiono **due card grigie** della forma di quelle vere
(titolo, riga, barra) che poi si riempiono. **Non** deve comparire una riga di testo "Loading…" seguita da un
salto del contenuto. Se la rete locale è troppo veloce per vederlo, rallentala dagli strumenti per
sviluppatori (scheda **Rete** → *Slow 3G*) e ricarica.

### 1.2 Il guasto ha un rimedio, e resta confinato

**Azione** — con `/billing` aperto, dagli strumenti per sviluppatori blocca la richiesta `me/subscriptions`
(clic destro sulla richiesta → *Block request URL*) e ricarica.

**Risultato atteso** — al posto delle card compare un **riquadro rosso tenue** con "Something went wrong" e
un pulsante **`Retry`**. La sezione **`Payments & receipts`** sotto resta perfettamente funzionante: due
guasti indipendenti, non una pagina tutta rossa.

**Azione** — togli il blocco e premi `Retry`.

**Risultato atteso** — le card ricompaiono **senza ricaricare la pagina**.

---

## Parte 2 — Il consumo della quota

In locale nessuna app riporta davvero i posti occupati (Teams è una fixture di catalogo senza servizio
dietro), quindi la giacenza va **forzata a mano**. È esattamente il dato che l'app riporterebbe per evento.

### 2.1 "8 su 10 posti", con la barra

**Azione** — inserisci una giacenza di 8 posti per Acme sull'app `teams`, poi ricarica `/billing`:

```bash
docker compose -f dev/docker-compose.yml exec -T postgres \
  psql -U appgrove -d appgrove -c "
    insert into platform.app_usage_stock (app_slug, tenant_id, metric, value, reported_at, updated_at)
    values ('teams', 'a0000000-0000-4000-8000-000000000001', 'seats', 8, now(), now())
    on conflict (app_slug, tenant_id, metric)
      do update set value = excluded.value, reported_at = excluded.reported_at, updated_at = now();"
```

**Risultato atteso** — sulla card **Teams** compare la riga **`8 of 10 seats used`** con sotto una **barra
di riempimento all'80%**, di colore **giallo/ambra** (non più il colore d'accento), e sotto ancora l'avviso
`You are close to your plan limit for seats…`. La riga "Your plan includes: up to 10 seats" resta: il tetto e
il consumo sono due informazioni diverse.

### 2.2 Limite raggiunto

**Azione** — ripeti il comando sopra con `value` **10**, poi con **14**, ricaricando ogni volta.

**Risultato atteso** — con 10 e con 14 la barra è **piena e rossa** e il testo diventa `Plan limit reached for
seats…`. Con 14 la barra **non deve uscire** dal suo contenitore (il riempimento si ferma al 100%).

### 2.3 Le metriche a finestra non mentono

**Azione** — guarda la card **Notes** dello stesso account (la sua metrica è "note al mese", che si azzera a
ogni periodo).

**Risultato atteso** — Notes mostra **solo** `Your plan includes: 100 notes / month`. **Nessuna barra**,
nessun "N su 100". È voluto: di quel consumo `core` non sa nulla, e un numero inventato sarebbe peggio di
nessun numero (è il punto rimasto aperto nello use case 0067).

---

## Parte 3 — La finestra "Cambia piano"

L'account **Bob** è quello giusto: il suo Notes è in prova e l'app ha due piani (Free e Pro).

### 3.1 Prezzi, piano attuale, consigliato

**Azione** — esci, entra come `bob@bob.test`, vai su **Billing** e premi **`Change plan`** sulla card Notes.

**Risultato atteso** — si apre una **finestra al centro dello schermo**, con lo sfondo oscurato. Dentro:

- in alto un interruttore **`Monthly` / `Annual`**;
- un elenco di piani, **dal più economico al più caro**;
- ogni riga ha **nome**, **prezzo** (es. `€9.00 /mo` per Notes Pro; il piano senza prezzo dice `Free`) e i
  **limiti** del piano;
- il piano su cui sei ha il bollino **`Current plan`** e il suo pulsante è **spento**;
- il primo piano superiore ha il bollino verde **`Recommended`**.

Quello che **non** deve esserci: una fila di pulsanti nudi coi soli nomi dei piani, e nessuna indicazione di
prezzo. Era così prima.

### 3.2 Il ciclo di fatturazione è una scelta

**Azione** — nella stessa finestra premi **`Annual`**.

**Risultato atteso** — i prezzi cambiano nella stessa riga: Notes Pro passa da `€9.00 /mo` a `€90.00 /yr`. Il
piano attuale resta marcato.

### 3.3 La conferma dice cosa succede e da quando

**Azione** — torna su `Monthly`, scegli un piano **più caro** di quello attuale e premi `Choose`.

**Risultato atteso** — la finestra passa a un **secondo passo** intitolato `Move to a higher plan`, con il
testo che dice che il cambio è **immediato** e che ti viene addebitata la **differenza proporzionale**.
Ci sono `Back` e `Confirm`. **Nessun comando è ancora partito**: verificalo nella scheda **Rete** — non c'è
nessuna chiamata a `change-tier`.

**Azione** — premi `Back`, poi scegli un piano **più economico** e premi `Choose`.

**Risultato atteso** — il titolo diventa `Move to a lower plan` e il testo riporta **una data reale** (la fine
del periodo corrente), dicendo che fino ad allora non cambia nulla e che non c'è rimborso. La data deve essere
una data vera e formattata secondo la lingua scelta, non un timestamp grezzo.

### 3.4 Premendo Confirm

**Azione** — premi **`Confirm`**.

**Risultato atteso** — la finestra si chiude e sulla card compare **`Update in progress — waiting for
confirmation from the payment provider.`** Dopo **1–3 secondi** (il tempo che il webhook finto attraversi la
pipeline) il messaggio **sparisce da solo** e la card mostra la riduzione programmata:
`Downgrade scheduled to "…" from <data>`.

Il punto da osservare: la card **non ha mai dichiarato il successo prima** che il dato lo confermasse, e non
hai dovuto ricaricare la pagina.

### 3.5 Il piano troppo piccolo non è cliccabile

**Azione** — torna come `owner@acme.test`. Serve un caso in cui la riduzione sarebbe vietata: l'app `crm` ha
due piani a giacenza (Free = 2 posti, Team = 10) ma è spenta di default. Accendila dalla console di
amministrazione (`https://admin.local.appgrove.app`, `admin@appgrove.test`) → **Apps** → `crm` → stato
**active**; poi acquista il piano **Team** da `/catalog` con Acme, e infine forza la giacenza:

```bash
docker compose -f dev/docker-compose.yml exec -T postgres \
  psql -U appgrove -d appgrove -c "
    insert into platform.app_usage_stock (app_slug, tenant_id, metric, value, reported_at, updated_at)
    values ('crm', 'a0000000-0000-4000-8000-000000000001', 'seats', 8, now(), now())
    on conflict (app_slug, tenant_id, metric)
      do update set value = excluded.value, reported_at = excluded.reported_at, updated_at = now();"
```

**Azione** — su `/billing`, card Mini-CRM → `Change plan`.

**Risultato atteso** — la riga del piano **Free** è **attenuata**, riporta in giallo la spiegazione
(«la metrica 'seats' è a 8, sopra il limite 2 del piano scelto…») e il suo pulsante `Choose` è **spento**.
Non deve essere possibile cliccarlo e prendersi un errore dopo: è la differenza che questa change introduce.

**Azione** — riporta la giacenza a `1` con lo stesso comando e ricarica.

**Risultato atteso** — il piano Free torna **selezionabile**, senza spiegazione in giallo.

---

## Parte 4 — Disdetta, riattivazione, avvisi

### 4.1 Disdetta con conferma dell'applicazione

**Azione** — come `owner@acme.test`, card **Teams**, premi **`Cancel`**.

**Risultato atteso** — si apre una **finestra dell'applicazione** (non la finestra grigia di sistema del
browser!) intitolata `Cancel subscription`, che dice fino a **quale data** resta l'accesso e che fino ad
allora puoi annullare. Premi **`Esc`**: la finestra si chiude senza fare nulla.

**Azione** — riapri e premi `Confirm`.

**Risultato atteso** — "aggiornamento in corso", poi la card diventa **`Cancelling`** con la riga
`Cancellation scheduled: access until <data>` e appare il pulsante **`Undo cancellation`**.

### 4.2 Riattivazione

**Azione** — premi **`Undo cancellation`**.

**Risultato atteso** — di nuovo l'aggiornamento in corso, poi la card torna **`Active`** con `Renews on <data>`
e riappare `Cancel`.

### 4.3 Pagamento in ritardo

**Azione** — guarda la card **Notes** di Acme (nel seed il suo pagamento è fallito).

**Risultato atteso** — dentro la card, in evidenza, un **riquadro ambra** con `Payment overdue`, la
spiegazione che l'accesso resta per un breve periodo di tolleranza, e il pulsante **`Update payment method`**.
Premendolo si apre **una nuova scheda** verso il portale finto di Paddle
(`sandbox-customer-portal.paddle.com/stub/…`). Il punto: l'avviso **dice cosa fare**, non è un bollino muto.

### 4.4 Abbonamento scaduto

**Azione** — entra come `bob@bob.test` e guarda la card **Teams** (disdetta e scaduta nel seed).

**Risultato atteso** — riquadro neutro `Subscription expired` che dice che **i dati restano**, più i pulsanti
`Reactivate` e **`Export / delete your data`**. Il secondo porta a `/privacy` e **deve esserci anche con
l'abbonamento morto**: i diritti sui dati non dipendono dall'abbonamento.

### 4.5 Chi non è titolare guarda ma non tocca

**Azione** — esci ed entra come `member@acme.test`, vai su `/billing`.

**Risultato atteso** — vede le card e tutti gli stati, ma sotto compare
`Only the workspace owner can change billing.` e i pulsanti `Change plan` / `Cancel` sono **spenti**
(grigi, non cliccabili). La sezione dei pagamenti dice invece che è riservata a titolari e amministratori.

---

## Parte 5 — Lingue e temi

**Azione** — come `owner@acme.test` su `/billing`, cambia lingua dal menu in alto a destra girando fra
**English, Italiano, Français, Español, Deutsch**, con una finestra di cambio piano aperta e una card in
ritardo di pagamento visibile.

**Risultato atteso** — **tutto** è tradotto: titoli delle conferme, testo dell'aggiornamento in corso, avviso
di pagamento in ritardo, avviso di scadenza, "N su M", i bollini `Piano attuale` / `Consigliato`, i pulsanti.
Nessuna stringa in inglese rimasta in mezzo all'italiano, nessuna chiave grezza tipo
`subscriptions.quotaWarn`. Le **date** seguono la lingua (`12/09/2026` in italiano, `9/12/2026` in inglese) e
anche i **prezzi** (`€9,00` contro `€9.00`).

**Azione** — cambia tema chiaro/scuro con una finestra aperta.

**Risultato atteso** — la finestra resta leggibile in entrambi, lo sfondo oscurato dietro non sparisce, la
barra della quota resta visibile (accento/ambra/rosso) e i riquadri di avviso non diventano illeggibili.

---

## Parte 6 — Controlli non visivi

### 6.1 La lettura espone i due campi nuovi

**Azione** — prendi un token dell'owner (strumenti per sviluppatori → **Rete** → richiesta `me/subscriptions`
→ *Copy as cURL*), poi:

```bash
curl -sk https://app.local.appgrove.app/api/platform/v1/me/subscriptions \
  -H "Authorization: Bearer <token-owner-acme>" | jq '.subscriptions[] | {appSlug, tierKey, usage, blockedTiers}'
```

**Risultato atteso** — per `teams` un `usage` con `{"seats": 8}` (o il valore che hai forzato) e un
`blockedTiers` che elenca **solo** i piani troppo piccoli, con la spiegazione **in chiaro**. Per un'app senza
riporti d'uso entrambi sono **oggetti vuoti** `{}`, non `null`.

### 6.2 Il divieto vero è nel backend, non nell'interfaccia

**Azione** — con un token di **`member@acme.test`**:

```bash
curl -sk -X POST https://app.local.appgrove.app/api/platform/v1/me/subscriptions/teams/cancel \
  -H "Authorization: Bearer <token-member>" -i | head -3
```

**Risultato atteso** — **403**. I pulsanti spenti sono cortesia; il divieto è qui.

### 6.3 La giacenza di un altro workspace non si vede

**Azione** — inserisci una giacenza per **Bob** sull'app `teams` e poi rileggi con il token di **Acme**:

```bash
docker compose -f dev/docker-compose.yml exec -T postgres \
  psql -U appgrove -d appgrove -c "
    insert into platform.app_usage_stock (app_slug, tenant_id, metric, value, reported_at, updated_at)
    values ('teams', 'a0000000-0000-4000-8000-000000000002', 'seats', 99, now(), now())
    on conflict (app_slug, tenant_id, metric)
      do update set value = excluded.value, reported_at = excluded.reported_at, updated_at = now();"

curl -sk https://app.local.appgrove.app/api/platform/v1/me/subscriptions \
  -H "Authorization: Bearer <token-owner-acme>" | jq '.subscriptions[] | select(.appSlug=="teams") | .usage'
```

**Risultato atteso** — il valore di **Acme**, mai `99`. Il tenant viene dal token e da nient'altro.

### 6.4 La riga la scrive il webhook, non la richiesta

**Azione** — guarda la subscription prima e dopo una disdetta:

```bash
docker compose -f dev/docker-compose.yml exec -T postgres \
  psql -U appgrove -d appgrove -c "
    select a.slug, s.status, s.app_tier_id, s.cancel_at, s.scheduled_tier_id, s.scheduled_change_at,
           s.last_event_occurred_at
      from platform.subscription s join platform.app a on a.id = s.app_id
     where s.tenant_id = 'a0000000-0000-4000-8000-000000000001' order by a.slug;"
```

**Risultato atteso** — dopo una **riduzione** confermata: `app_tier_id` **invariato** (resti sul piano attuale)
e `scheduled_tier_id` + `scheduled_change_at` valorizzati. Dopo una **disdetta**: `cancel_at` valorizzato.
Dopo una **riattivazione**: `cancel_at` di nuovo nullo. In tutti i casi `last_event_occurred_at` si è mosso:
è la prova che il valore è arrivato dalla pipeline dei webhook e non è stato scritto a mano dalla richiesta.

### 6.5 Il contratto pubblicato è allineato

**Azione**:

```bash
grep -A 10 "SubscriptionView:" services/core/src/main/resources/META-INF/openapi/openapi.yaml | grep -c "usage\|blockedTiers"
git diff --stat main -- frontend/packages/api-client/src/schema.ts
```

**Risultato atteso** — i due campi sono nello spec **e** nei tipi del client, cambiati nello stesso commit. Se
uno dei due mancasse, il controllo di scostamento del contratto sarebbe rosso in integrazione continua.

---

## Cosa NON deve essere cambiato

- La pagina **Billing** ha ancora **due** sezioni: abbonamenti e `Payments & receipts` (UC 0096). Nessuna
  vetrina d'acquisto, nessun "Get an app".
- Il **catalogo** (`/catalog`) e il **checkout** sono identici a prima.
- Un workspace **senza abbonamenti** vede ancora `You don't have any subscriptions yet.` con il rimando al
  catalogo (provalo creando un workspace nuovo dalla registrazione).
- Le **conferme dei membri** (invita, rimuovi) continuano a funzionare: la loro finestra è stata riscritta
  sopra la primitiva condivisa nuova, quindi vale la pena riaprirle una volta (`/members`) e controllare che
  `Esc` chiuda e che il pulsante di conferma prenda il fuoco all'apertura.
