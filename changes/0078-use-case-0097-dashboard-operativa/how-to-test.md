# Come verificare a mano la change 0078 — Dashboard operativa del workspace

Questa change riscrive **la prima pagina che un utente vede dopo l'accesso** e sposta l'identificativo tecnico del
workspace in un'altra pagina. È quasi tutta verifica con gli occhi. Sotto: prima il percorso visivo completo, poi i
controlli che gli occhi non possono fare (chiamate all'API, righe di database, posta).

Tutti i comandi partono dalla radice del repository, sul branch `change/0078-use-case-0097-dashboard-operativa`.

---

## 0. Avvio dello stack locale

**Azione**

```bash
./app-start.sh
```

**Risultato atteso** — al termine il riepilogo elenca il backoffice su `https://app.local.appgrove.app` e gli utenti di
prova (password `Password1!`): `owner@acme.test`, `admin@acme.test`, `member@acme.test`, `bob@bob.test`,
`admin@appgrove.test`. Fra i servizi ci sono anche **Mailpit** su `http://localhost:8025` e le due app `fatture` (porta
8081) e `crm` (porta 8082) — servono ai punti 3 e 8.

> Se il browser protesta per il certificato di `app.local.appgrove.app`, è il proxy locale: accetta l'eccezione una
> volta sola.

> Utile per orientarsi: `./dev.sh services` stampa la mappa servizio → porta scoperta automaticamente.

---

## 1. La pagina d'atterraggio non è più un segnaposto

**Azione** — apri `https://app.local.appgrove.app`, entra come **`owner@acme.test`** e resta sulla **Dashboard** (è la
pagina che si apre da sola dopo l'accesso; nel menu a sinistra è la prima voce del gruppo PLATFORM).

**Risultato atteso**

- il titolo grande è **«Welcome back, Acme Owner»** (il nome viene dal profilo: se il tuo utente non ha nome
  visualizzato, leggerai il solo «Welcome back»);
- sotto si legge **«Here's what's happening in the Acme Corp workspace.»**, col nome vero del workspace;
- **da nessuna parte compare un codice esadecimale lungo** tipo `a0000000-0000-4000-8000-000000000001`. È il cuore
  della change: prima era l'unica cosa che la pagina mostrava, ora è in **Account** (punto 6).

---

## 2. Gli avvisi: solo quelli veri, e in ordine di gravità

**Azione** — guarda la fascia di strisce colorate subito sotto il titolo.

**Risultato atteso** — **due** strisce, in quest'ordine dall'alto:

| # | Colore | Testo | Pulsante |
|---|---|---|---|
| 1 | rosso | «**Notes** has a payment pending — your access may be limited soon.» | `Go to Billing` |
| 2 | giallo/ambra | «Protect your account: enable two-factor authentication.» | `Enable 2FA` |

L'ordine non è casuale: prima ciò che fa **perdere l'accesso**, poi ciò che **espone il conto**.

**Azione** — premi `Go to Billing` sulla prima striscia.

**Risultato atteso** — atterri su `/billing` e la card di **Notes** è quella con la fase `Payment pending`: l'avviso
diceva la verità.

**Azione** — torna alla Dashboard e premi `Enable 2FA` sulla seconda striscia.

**Risultato atteso** — atterri sulla pagina **Security**.

> ⚠️ **Atteso, non un difetto**: con un utente **del seed** premere `Enable 2FA` su Security restituisce un errore
> («2FA non disponibile per account senza credenziali»). Gli utenti del seed non hanno una riga di credenziali locali —
> comportamento preesistente a questa change. Per vedere il percorso completo del secondo fattore serve un account
> registrato davvero: è il punto 9.

**Azione** — guarda in cima alla finestra, sopra il contenuto.

**Risultato atteso** — **non** c'è più la vecchia striscia azzurra «Protect your account…» sopra la barra superiore, con
la × per chiuderla. Quel banner è stato **rimosso dal guscio**: adesso il messaggio esiste in un posto solo, dice la
verità e non si può far sparire finché il problema c'è.

---

## 3. Le app in uso, con il consumo reale

**Azione** — guarda la sezione **«Your apps»** (colonna larga, a sinistra).

**Risultato atteso** — una card per ogni app **attiva o in prova** del workspace, con la tinta di categoria, l'iniziale,
il badge di stato e i pulsanti `Open` e `Manage plan`. Per l'account Acme dovresti vedere almeno:

| Card | Barra di consumo attesa |
|---|---|
| **Fatture** | «N of 10 fatture» con la barra sotto (fascia gratuita: 10 fatture al mese) |
| **Mini-CRM** | «N of 2 posti» con la barra sotto (fascia gratuita: 2 posti) |
| **Teams** | *nessuna barra*, al suo posto la descrizione dell'app |

> La barra c'è **solo** per le app che hanno un modulo impacchettato nel frontend, perché il consumo lo conosce il
> servizio dell'app e nessun altro. **Teams** è un'app di prova senza modulo: card senza barra e — se premi `Open` —
> una rotta che non monta nulla. È un limite **noto e tracciato** nei punti aperti di UC 0097, non un difetto di questa
> change.

**Azione** — apri **Fatture** dal menu di sinistra, crea una fattura, torna alla Dashboard e ricarica la pagina.

**Risultato atteso** — il numero a sinistra della barra è **aumentato di uno** e la barra si è allungata di
conseguenza. Continuando a creare fatture, superata la soglia dell'**80 %** la barra cambia colore e diventa
**ambra**: si sta arrivando al tetto.

**Azione** — guarda l'ultima cella della griglia, quella tratteggiata.

**Risultato atteso** — «**Get more apps** · Browse the catalog and activate what you need.»; cliccandola atterri su
**App catalog**.

---

## 4. Il riepilogo e le scorciatoie

**Azione** — guarda la colonna stretta a destra, riquadro **«At a glance»**.

**Risultato atteso** — quattro righe con i numeri veri del workspace Acme:

| Riga | Valore atteso |
|---|---|
| Members | **3** (owner, admin, member del seed) |
| Pending invites | **2** (i due inviti del seed) |
| Active apps | quante card hai contato al punto 3 |
| Next renewal | una data (il rinnovo più vicino fra gli abbonamenti vivi) |

**Azione** — sotto il riquadro, prova i tre pulsanti-scorciatoia.

**Risultato atteso** — `Invite a member` → **Members**; `Payments and receipts` → **Billing**;
`Browse the catalog` → **App catalog**.

---

## 5. Che cosa vede un membro semplice

**Azione** — esci e rientra come **`member@acme.test`**. Resta sulla Dashboard.

**Risultato atteso**

- la panoramica c'è tutta: saluto, app in uso, riquadro di riepilogo, scorciatoie;
- nel riquadro **«At a glance»** **non** compaiono le righe **Members** e **Pending invites** (sono letture riservate a
  owner/admin: mostrargliele rotte sarebbe peggio che ometterle);
- fra le scorciatoie **non** c'è `Invite a member`;
- sulle card delle app c'è `Open` ma **non** `Manage plan`.

> Nessuna riga dice «non hai i permessi»: le cose che non ti riguardano semplicemente non ci sono.

---

## 6. L'identificativo del workspace è in Account

**Azione** — rientra come `owner@acme.test` e apri **Account** dal menu di sinistra.

**Risultato atteso** — sotto la scheda del profilo compare una **seconda scheda «Workspace»** con:

- **Workspace name** → `Acme Corp` (in sola lettura: si modifica in Impostazioni, non qui);
- **Workspace ID** → il codice `a0000000-0000-4000-8000-000000000001` in **carattere a larghezza fissa**, su fondo
  grigio;
- il pulsante **`Copy`** con l'icona della copia;
- sotto, in piccolo: «Share this ID with support when opening a ticket.»

**Azione** — premi `Copy`, poi incolla in un editor di testo.

**Risultato atteso** — il pulsante diventa per ~2 secondi **`Copied`** con il segno di spunta, e negli appunti trovi
esattamente `a0000000-0000-4000-8000-000000000001`.

---

## 7. Le cinque lingue

**Azione** — dalla barra superiore cambia lingua (menu delle lingue) e ripassa su Dashboard e Account per **italiano,
francese, spagnolo, tedesco**.

**Risultato atteso** — saluto, sottotitolo, testi degli avvisi, «Your apps», «At a glance», le tre scorciatoie e tutta
la scheda Workspace sono tradotti. **Nessuna stringa deve apparire come una chiave** tipo `dashboard.glanceRenewal`.
In italiano, per esempio: «Bentornato, Acme Owner», «In sintesi», «Prossimo rinnovo», «Identificativo del workspace».

> L'unità della barra segue la lingua per **Fatture** (`fatture`/`invoices`/`factures`/…). Per **Mini-CRM** resta
> `posti` in tutte le lingue: quel modulo non è ancora tradotto (limite preesistente, UC 0060).

---

## 8. La degradazione: una fonte che si guasta non spegne la pagina

**Azione** — spegni **solo** il servizio dell'app fatture, lasciando su tutto il resto:

```bash
lsof -ti tcp:8081 | xargs kill
```

Poi torna sulla Dashboard e **ricarica** la pagina.

**Risultato atteso** — la card di **Fatture** perde la barra e mostra al suo posto la descrizione dell'app, ma **resta
lì con i suoi pulsanti**; la card di **Mini-CRM** ha ancora la sua barra; avvisi, riepilogo e scorciatoie sono intatti.
Nessun messaggio d'errore rosso a tutta pagina.

**Azione** — spegni ora il **core** (la fonte della vetrina) e ricarica:

```bash
lsof -ti tcp:8080 | xargs kill
```

**Risultato atteso** — al posto della griglia delle app compare **«We couldn't load your apps.»** con il pulsante
`Retry`, **solo lì**. Il riquadro «At a glance» resta in pagina (con trattini al posto dei numeri che non si sono
potuti leggere) e le scorciatoie funzionano ancora.

**Azione** — riaccendi tutto e premi `Retry`:

```bash
./app-stop.sh --apps-only && ./app-start.sh
```

**Risultato atteso** — le app ricompaiono **senza ricaricare la pagina** (`Retry` rilegge la vetrina; per la barra di
quota basta un cambio di pagina e ritorno).

---

## 9. Il secondo fattore dice la verità (percorso completo, con account vero)

Serve un account **registrato davvero**, perché gli utenti del seed non hanno credenziali locali.

**Azione** — esci, vai su `/signup` e registra un account nuovo (email qualsiasi `@example.test`). Apri **Mailpit**
(`http://localhost:8025`), trova l'email «Confirm your email address» e clicca il collegamento; completa la procedura
guidata del workspace fino alla Dashboard, accettando i documenti legali quando compaiono.

**Risultato atteso (prima)** — sulla Dashboard del nuovo account c'è la striscia **«Protect your account: enable
two-factor authentication.»**.

**Azione** — premi `Enable 2FA`, inquadra il codice QR con un'app di autenticazione (o copia il segreto), inserisci il
codice a 6 cifre e conferma.

**Risultato atteso** — la pagina Security scrive **«Two-factor authentication is enabled.»**.

**Azione** — torna sulla **Dashboard** e ricarica la pagina.

**Risultato atteso** — la striscia del secondo fattore **è sparita**, e resta sparita anche dopo un nuovo accesso e su
un altro browser: lo stato viene dal server, non dalla memoria del browser. È esattamente ciò che il vecchio banner non
sapeva fare.

**Azione** — riapri la pagina **Security**.

**Risultato atteso** — non ti propone più di attivare il secondo fattore: annuncia che è **già attivo**.

---

## Controlli non visivi

### A. La nuova lettura dello stato del secondo fattore

**Azione** — prendi un gettone di accesso e interroga il servizio di autenticazione:

```bash
TOKEN=$(curl -sk https://app.local.appgrove.app/api/auth/login \
  -H 'content-type: application/json' \
  -d '{"email":"owner@acme.test","password":"Password1!"}' | python3 -c 'import sys,json;print(json.load(sys.stdin)["access_token"])')

curl -sk https://app.local.appgrove.app/api/auth/2fa/status -H "authorization: Bearer $TOKEN"
```

**Risultato atteso** — `{"enabled":false}` per un utente del seed; `{"enabled":true}` col gettone dell'account del
punto 9.

**Azione** — chiedi la stessa cosa **senza** gettone:

```bash
curl -sk -o /dev/null -w '%{http_code}\n' https://app.local.appgrove.app/api/auth/2fa/status
```

**Risultato atteso** — `401`. La lettura non è mai anonima e il soggetto viene sempre dal gettone verificato, mai da un
parametro.

### B. La Dashboard non ha inventato endpoint nuovi

**Azione** — apri gli strumenti di sviluppo del browser, scheda **Rete**, filtra su `api/` e ricarica la Dashboard.

**Risultato atteso** — le chiamate sono **solo** letture che esistevano già, più le due di quota e quella del secondo
fattore:

```
GET /api/platform/v1/accounts/me
GET /api/platform/v1/me/catalog
GET /api/platform/v1/me/subscriptions
GET /api/platform/v1/me/entitlements
GET /api/platform/v1/users?size=100          ← solo owner/admin
GET /api/platform/v1/invitations?size=100    ← solo owner/admin
GET /api/auth/2fa/status
GET /api/fatture/v1/quota                    ← una per app in uso col modulo
GET /api/crm/v1/quota
```

Nessuna chiamata a un endpoint riassuntivo nuovo: la pagina **compone** ciò che già esisteva.

**Azione** — ripeti da `member@acme.test`.

**Risultato atteso** — `/users` e `/invitations` **non compaiono affatto** nell'elenco (non vengono nemmeno chieste), e
non ci sono risposte `403` nella scheda Rete.

### C. Il consumo mostrato è quello vero del database

**Azione** — confronta il numero della barra di **Mini-CRM** con i posti assegnati sul database:

```bash
docker exec -i appgrove-dev-postgres-1 psql -U appgrove -d appgrove -c \
  "select count(*) from app_crm.seat
    where tenant_id = 'a0000000-0000-4000-8000-000000000001' and deleted_at is null;"
```

**Risultato atteso** — lo stesso numero che leggi a sinistra della barra («N of 2 posti»).

**Azione** — fai lo stesso per **Fatture** (metrica a consumo: contano le fatture del mese corrente):

```bash
docker exec -i appgrove-dev-postgres-1 psql -U appgrove -d appgrove -c \
  "select count(*) from app_fatture.invoice
    where tenant_id = 'a0000000-0000-4000-8000-000000000001'
      and deleted_at is null
      and created_at >= date_trunc('month', (now() at time zone 'utc'));"
```

**Risultato atteso** — lo stesso numero che leggi a sinistra della barra («N of 10 fatture»). La metrica è **a
consumo**: conta le fatture create nel mese corrente (in UTC), non tutte quelle esistenti.

### D. Nessuna posta nuova

Questa change **non manda email**. In **Mailpit** (`http://localhost:8025`) non deve comparire nulla di nuovo a causa
della Dashboard: le uniche email che vedrai sono quelle della registrazione del punto 9.

---

## Riepilogo di ciò che deve essere vero alla fine

- [ ] La Dashboard saluta, nomina il workspace e **non mostra più** il codice del workspace.
- [ ] Gli avvisi ci sono solo quando servono, in ordine di gravità, ciascuno con la sua azione che porta dove dice.
- [ ] Ogni app in uso ha la sua card; quelle con un modulo impacchettato hanno la barra di consumo, che cresce con
      l'uso e diventa ambra oltre l'80 %.
- [ ] La cella tratteggiata porta al catalogo.
- [ ] «At a glance» mostra i numeri veri; a un membro le righe e le azioni riservate non sono offerte.
- [ ] Il guasto di **una** fonte degrada la sua sola sezione o card, con la riprova; il resto resta utile.
- [ ] L'identificativo del workspace è in **Account**, in carattere a larghezza fissa, e il pulsante lo copia davvero.
- [ ] Con il secondo fattore attivo nessuno lo propone più — né in Dashboard né nel guscio né in Security.
- [ ] Tutto tradotto nelle 5 lingue, nessuna chiave a vista.
