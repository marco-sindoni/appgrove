# Come verificare a mano la change 0076 — la pagina "App catalog" del backoffice

Questa change aggiunge una **pagina visiva**: quasi tutto quello che va verificato si guarda con gli occhi,
in un browser. Sotto, prima il percorso visivo completo, poi i controlli che gli occhi non possono fare
(chiamate all'API, righe di database).

Tutti i comandi partono dalla radice del repository, sul branch
`change/0076-use-case-0095-pagina-app-catalog`.

---

## 0. Avvio dello stack locale

**Azione**

```bash
./app-start.sh
```

**Risultato atteso** — al termine il riepilogo elenca il backoffice su
`https://app.local.appgrove.app` e gli utenti di prova (password `Password1!`):
`owner@acme.test`, `admin@acme.test`, `member@acme.test`, `bob@bob.test`, `admin@appgrove.test`.

> Se il browser protesta per il certificato di `app.local.appgrove.app`, è il proxy locale: accetta
> l'eccezione una volta sola.

---

## 1. La voce di menu esiste, ed è dove deve stare

**Azione** — apri `https://app.local.appgrove.app` ed entra come **`owner@acme.test`**. Guarda il menu a
sinistra, gruppo **PLATFORM**.

**Risultato atteso**

- fra **Dashboard** e **Account** c'è una voce nuova: **App catalog**, con l'icona a quadratini;
- cliccandola l'indirizzo diventa `/catalog` e la voce si evidenzia come le altre;
- la pagina si apre con il titolo grande **App catalog** e, sotto, la frase
  «Discover the apps available on appgrove and activate them for this workspace.»

---

## 2. Le card, e gli stati che raccontano la verità

**Azione** — resta su `/catalog` come `owner@acme.test`.

**Risultato atteso** — una griglia di card (3 per riga su schermo largo), ciascuna con: una **testata
colorata** con l'iniziale dell'app dentro un riquadro tinto, il **nome**, un **badge di stato** a destra del
nome, la **descrizione breve** e, in fondo, **un pulsante e un'informazione a destra**. In particolare, per
l'account Acme del seed:

| App | Badge atteso | Azione attesa | A destra |
|---|---|---|---|
| **Fatture** | `Active` (verde) | `Open` | `Fatture Free` |
| **Teams** | `Active` (verde) | `Open` | `Teams` |
| **Notes** | `Payment pending` (giallo) | `Fix payment` | — |
| **Legacy** | `Disabled by platform` (rosso) | `Contact support` | «Currently unavailable» |

Due cose da guardare bene, perché sono il motivo per cui questa pagina esiste:

1. **Legacy** ha un abbonamento formalmente attivo ma l'app è spenta dalla piattaforma: la card **lo dice** e
   **non offre alcun pulsante di acquisto**. (Prima di questa change quello stato era invisibile e la pagina
   Billing mostrava "Active" senza spiegazioni.)
2. **Mini-CRM** non compare affatto: è spenta e Acme non ha alcun abbonamento — non si fa vetrina di ciò che
   la piattaforma ha deciso di non vendere.

**Azione** — clicca `Open` sulla card di **Fatture**.

**Risultato atteso** — si apre il modulo dell'app (`/app/fatture`), esattamente come dal menu laterale.

---

## 3. Un'app acquistabile: prezzo di partenza e pulsante attivo

**Azione** — esci e rientra come **`bob@bob.test`**, poi vai su `/catalog`.

**Risultato atteso**

- la card **Teams** ha **nessun badge** (è acquistabile), il pulsante **`Subscribe`** attivo e, a destra,
  **`from €19,00/mese`** — il prezzo viene dal listino vero, non è scritto nella pagina;
- la card **Notes** ha il badge **`Trial`** e, a destra, **`Trial ends <data>`**;
- la card **Fatture** dice `Active` / `Fatture Free`: ha una fascia gratuita, quindi è già in uso — e per
  questo **non** offre "Subscribe". Se offrisse un acquisto, sarebbe una bugia.

**Azione** — clicca `Subscribe` sulla card di **Teams**.

**Risultato atteso** — **senza cambiare pagina** (il titolo resta "App catalog") compare la scelta della
fascia: interruttore mensile/annuale in alto a destra, la card della fascia `Teams` con il badge
`14-day free trial`, il prezzo e il pulsante `Subscribe`. In alto a sinistra c'è `← Back`: cliccandolo si
torna alla griglia.

---

## 4. L'acquisto parte dalla vetrina e la card cambia sotto i tuoi occhi

**Azione** — sempre come `bob@bob.test`, da `/catalog` → `Subscribe` su **Teams** → `Subscribe` sulla fascia
→ nell'overlay di pagamento finto (Paddle stub) completa il pagamento.

**Risultato atteso**

1. compare la schermata di attesa **«Activating your subscription…»** con il cerchietto che gira (l'attivazione
   arriva dal webhook, non dal browser: qualche secondo è normale);
2. poi **«All set! Your subscription is active.»** con due pulsanti: `Open app` e `Back`;
3. clicca **`Back`**: torni alla griglia e **la card di Teams è cambiata** — badge `Active` (o `Trial`),
   pulsante `Open` al posto di `Subscribe`, e **nessun `Subscribe`** da nessuna parte su quella card;
4. il menu laterale, sotto **YOUR APPS**, non mostra Teams: è corretto, Teams non ha un modulo frontend. Il
   menu continua a mostrare solo le app che si possono aprire.

> Questa è la verifica più importante della change: la card si aggiorna **senza ricaricare la pagina**.

---

## 5. Ricerca: filtra, conta, e distingue il vuoto dall'errore

**Azione** — su `/catalog`, guarda la barra sopra la griglia.

**Risultato atteso** — un campo di ricerca con la lente e il segnaposto «Search apps…», e alla sua destra il
conteggio (per esempio **`4 apps`**).

**Azione** — scrivi `team` nel campo.

**Risultato atteso** — resta la sola card **Teams**, il conteggio diventa **`1 app`** (al singolare).

**Azione** — cancella e scrivi `condivise` con l'interfaccia in **italiano** (cambia lingua dal menu in alto
a destra).

**Risultato atteso** — la ricerca trova **Notes**: la ricerca guarda anche la **descrizione**, nella lingua
attiva.

**Azione** — scrivi `zzz`.

**Risultato atteso** — una card larga con **«Nessuna app corrisponde alla ricerca»**, l'invito a provare
un'altra parola e un pulsante **«Azzera la ricerca»**; cliccandolo torna la griglia intera. **Non** deve
apparire nulla che somigli a un errore.

---

## 6. Paginazione

**Azione** — azzera la ricerca e guarda sotto la griglia.

**Risultato atteso** — la riga `← Previous` · **`Page 1 of 1`** · `Next →`, con **entrambi** i pulsanti
disabilitati (in locale il catalogo ha meno di 6 app).

**Azione (facoltativa, per vedere davvero due pagine)** — dalla console admin
(`https://admin.local.appgrove.app`, utente `admin@appgrove.test`) non si creano app nuove; il modo rapido è
aggiungere due listini fittizi in `services/core/src/main/resources/pricing/fixtures/` più le loro righe in
`fixtures/index.yaml`, poi `./app-stop.sh && ./app-start.sh`.

**Risultato atteso** — con più di 6 app: `Page 1 of 2`, `Next` attivo, e cliccandolo compare la seconda
pagina con `Previous` attivo e `Next` disabilitato. Scrivendo qualcosa nella ricerca mentre si è a pagina 2
si torna a **`Page 1 of …`** con i risultati visibili (mai una pagina vuota).

---

## 7. Un member vede tutto ma non può comprare

**Azione** — esci e rientra come **`member@acme.test`**, poi vai su `/catalog`.

**Risultato atteso**

- la pagina si apre lo stesso: la vetrina non richiede alcun diritto d'uso;
- gli stati e i prezzi sono gli stessi che vede l'owner;
- su una card acquistabile il pulsante **`Subscribe` è grigio e non cliccabile**, e accanto si legge
  **«Ask an owner to activate it»** (in italiano: «Chiedi a un owner di attivarla»).

**Nota** — l'utente `admin@acme.test` vede lo stesso divieto del member: l'avvio del pagamento è riservato al
solo ruolo `owner`, ed è il backend a deciderlo (vedi §10).

---

## 8. Le cinque lingue

**Azione** — dal menu in alto a destra cambia lingua fra **English, Italiano, Français, Español, Deutsch**
restando su `/catalog`.

**Risultato atteso** — a ogni cambio, **immediatamente e senza ricaricare**: titolo, sottotitolo, segnaposto
della ricerca, conteggio, badge di stato, pulsanti, paginazione **e la descrizione delle app** passano nella
lingua scelta. Nessuna chiave grezza tipo `catalog.title` deve comparire.

---

## 9. Errore di lettura ≠ catalogo vuoto

**Azione** — con la pagina `/catalog` aperta, ferma il solo servizio `core` (la sua porta la dice
`./dev.sh services`, riga `core` → di norma **8080**) e ricarica la pagina:

```bash
./dev.sh services               # per leggere la porta del core
lsof -ti :8080 | xargs kill     # ferma il solo core, il resto dello stack resta su
```

**Risultato atteso** — la pagina mostra un **messaggio di errore** («We couldn't load…» / «Non siamo riusciti
a caricare…») con un pulsante **`Retry` / `Riprova`**. **Non** deve dire "nessuna app": un guasto non è un
catalogo vuoto.

**Azione** — rimetti su il core e premi `Retry`:

```bash
./app-start.sh --no-seed
```

**Risultato atteso** — la griglia ricompare senza ricaricare la pagina.

---

## 10. Controlli non visivi

### 10.1 L'API risponde e non accetta identificativi di account

**Azione** — prendi un token da una sessione del browser (DevTools → Application → il token di accesso) e:

```bash
curl -s https://app.local.appgrove.app/api/platform/v1/me/catalog \
  -H "Authorization: Bearer $TOKEN" | jq .
```

**Risultato atteso** — un oggetto `{ "apps": [ … ] }` in cui ogni voce ha `appSlug`, `name`, `category`,
`descriptions` **con tutte e cinque le lingue**, `state` fra
`available|active|trial|payment_pending|cancellation_scheduled|disabled_by_platform`, ed eventualmente
`planName`, `trialEndsAt`, `cancelAt`, `startingPrice`.

**Azione** — prova ad aggiungere un parametro di account:

```bash
curl -s -o /dev/null -w '%{http_code}\n' \
  "https://app.local.appgrove.app/api/platform/v1/me/catalog?tenantId=a0000000-0000-4000-8000-000000000002" \
  -H "Authorization: Bearer $TOKEN"
```

**Risultato atteso** — `200`, e il corpo è **identico** a quello di prima: il parametro viene ignorato perché
non esiste. Il tenant può venire solo dal token.

**Azione** — senza token:

```bash
curl -s -o /dev/null -w '%{http_code}\n' https://app.local.appgrove.app/api/platform/v1/me/catalog
```

**Risultato atteso** — `401`.

### 10.2 Il divieto vero è sul backend, non sul pulsante

**Azione** — con un token di **`member@acme.test`** (o `admin@acme.test`), prova ad avviare il pagamento
scavalcando l'interfaccia:

```bash
curl -s -o /dev/null -w '%{http_code}\n' -X POST \
  https://app.local.appgrove.app/api/platform/v1/checkout/apps/teams \
  -H "Authorization: Bearer $TOKEN_MEMBER" -H 'content-type: application/json' \
  -d '{"tierKey":"team","billingCycle":"monthly"}'
```

**Risultato atteso** — `403`. Il pulsante disabilitato è cortesia; il divieto è qui.

### 10.3 Il log strutturato della lettura

**Azione**

```bash
grep 'catalog.read' dev/.run/*.log | tail -5
```

**Risultato atteso** — righe tipo `catalog.read apps=4`, con nel contesto `tenant_id`, `user_id`, `app_id`.

### 10.4 Le righe di database che spiegano gli stati

**Azione**

```bash
PGPASSWORD=appgrove psql -h localhost -U appgrove -d appgrove -c "
  select a.slug, a.status as app_status, s.tenant_id, s.status as sub_status, s.cancel_at, s.trial_end
    from platform.app a
    left join platform.subscription s on s.app_id = a.id and s.deleted_at is null
   where a.deleted_at is null
   order by a.slug;"
```

**Risultato atteso** — la corrispondenza con quello che hai visto nelle card:
`legacy` con `app_status = inactive` e un abbonamento `active` per Acme → card `Disabled by platform`;
`notes` con `past_due` per Acme → `Payment pending`; `notes` con `trialing` per Bob → `Trial`.

**Azione (stato "disdetta programmata", l'unico che il seed non produce)** — forza una disdetta:

```bash
PGPASSWORD=appgrove psql -h localhost -U appgrove -d appgrove -c "
  update platform.subscription
     set cancel_at = now() + interval '20 days'
   where tenant_id = 'a0000000-0000-4000-8000-000000000001'
     and app_id = (select id from platform.app where slug = 'teams');"
```

poi ricarica `/catalog` come `owner@acme.test`.

**Risultato atteso** — la card **Teams** ora ha il badge **`Cancellation scheduled`**, il pulsante
**`Undo cancellation`** (che porta a Billing) e, a destra, **`Until <data fra 20 giorni>`**.
Per tornare indietro: rimetti `cancel_at = null` con la stessa query.

### 10.5 Nessuna email in gioco

Questa change non manda email: **Mailpit** (`http://localhost:8025`) non deve mostrare nulla di nuovo
navigando il catalogo. (Le email dell'acquisto, se ce ne sono, sono quelle già esistenti del checkout.)

---

## 11. La pagina Billing è rimasta com'era

**Azione** — vai su `/billing` come `owner@acme.test`.

**Risultato atteso** — tutto come prima della change: gli abbonamenti in corso e la griglia "Get an app" con
i pulsanti `Subscribe`, e il checkout che funziona da lì. È voluto: la pulizia di Billing è la storia
successiva (UC 0096).
