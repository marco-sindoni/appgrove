# Come collaudare a mano la change 0084 (UC 0075 — ticketing nativo in-house)

Il sistema di richieste di assistenza esisteva già, ma era **uno strumento della console dei diritti GDPR**:
un riquadro in fondo a una pagina che parla d'altro, senza ordinamento per scadenza, senza modo di sapere chi
sta aspettando chi, con email di servizio scritte a mano in italiano soltanto.

Questa change lo rende un vero canale di assistenza. I test automatici sanno dire che le transizioni sono
giuste e che le query tornano. **Non** sanno dire se, aprendo la coda, in tre secondi vedi quale richiesta
sta per sforare il termine di legge di un mese e quale va letta da un essere umano prima delle altre. Quella
parte si fa con gli occhi.

Tempo indicativo: **25 minuti** di controlli visivi, **10** di controlli non visivi.

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
| `owner@acme.test` | <https://app.local.appgrove.app> | apre le richieste e legge le risposte |
| `admin@appgrove.test` | <https://admin.local.appgrove.app> | è l'unico che vede la coda di tutti i conti |
| — | <http://localhost:8025> | Mailpit: la casella dove finiscono le email vere dello stack |

> Se lo stack era già acceso, rilancia `./app-start.sh` (il seed è idempotente).
> Se il browser protesta per il certificato, è il proxy locale: accetta l'eccezione.
> Tieni aperte **due finestre separate** (o una in incognito): cliente e piattaforma sono due sessioni diverse.

**La migrazione del database** — la prima accensione dopo questa change applica `V16`. Verifica che sia
passata:

```bash
docker compose -f dev/docker-compose.yml exec -T postgres \
  psql -U appgrove -d appgrove -tAc \
  "select version, description, success from flyway_schema_history where version = '16';"
```

**Risultato atteso** — una riga `16 | support ticket source review | t`. Se manca, il resto non ha senso.

---

## Parte 1 — Il cliente apre una richiesta

### 1.1 La pagina dice subito cosa non si può fare

**Azione** — entra come `owner@acme.test` e apri **Supporto** dal menu di sinistra.

**Risultato atteso** — sotto la casella «Messaggio» c'è una riga grigia che spiega che **gli allegati non
sono ancora supportati** e invita a non incollare documenti personali che non serve condividere. Deve essere
visibile *prima* che l'utente scriva, non dopo che ha provato: è lì per evitare che qualcuno incolli il
proprio referto medico nel testo per aggirare la mancanza.

### 1.2 Apertura di una richiesta ordinaria

**Azione** — tipo «Supporto», oggetto `Non trovo lo storico pagamenti`, messaggio
`Dalla pagina Billing non vedo le ricevute.`, poi «Apri ticket».

**Risultato atteso** — si apre il dettaglio con il messaggio nel filo. Nella riga di riepilogo lo stato è
«Aperto», il tipo «Supporto», **nessuna scadenza**.

### 1.3 L'email di conferma arriva — e non porta con sé la conversazione

**Azione** — apri <http://localhost:8025> e cerca l'email più recente per `owner@acme.test`.

**Risultato atteso** — c'è un messaggio con oggetto `We received your request: Non trovo lo storico
pagamenti` (o l'equivalente italiano se la lingua dell'utente è `it`), **impaginato come le altre email della
piattaforma** (intestazione col marchio, pulsante, testo di ripiego), non testo grezzo.

Guarda il corpo: **non deve contenere** la frase `Dalla pagina Billing non vedo le ricevute.` — l'email dice
che la richiesta è stata registrata e porta alla pagina Supporto, e basta. È la scelta di minimizzazione di
questa change: il contenuto della conversazione non viaggia mai per posta.

> Questo è il controllo più facile da sbagliare a occhio: cerca proprio la frase del messaggio, non fidarti
> dell'impressione generale.

### 1.4 La casella della piattaforma è stata avvisata

**Azione** — sempre in Mailpit, cerca le email per `support@appgrove.app`.

**Risultato atteso** — un messaggio con oggetto `[support] Non trovo lo storico pagamenti`, che nel corpo
riporta l'identificativo della richiesta e quello del conto, e un pulsante che punta alla console di
amministrazione.

---

## Parte 2 — La piattaforma vede la coda

### 2.1 La sezione ha una casa propria

**Azione** — nell'altra finestra entra come `admin@appgrove.test`. Guarda il menu di sinistra, gruppo
**Governance**.

**Risultato atteso** — sotto «GDPR rights» c'è una voce nuova, **«Support requests»**, con un'icona di
assistenza. Cliccandola si arriva su `/tickets` e il titolo della pagina è «Support requests».

### 2.2 La coda dice tutto senza aprire nulla

**Risultato atteso** — la tabella mostra la richiesta appena aperta con: oggetto (cliccabile), **conto**
(`Acme`), tipo, **provenienza «In-app form»**, priorità, stato e colonna della scadenza vuota.

### 2.3 La pagina «Diritti GDPR» non ha più il riquadro dei ticket

**Azione** — apri «GDPR rights».

**Risultato atteso** — la pagina ha l'aggregazione delle richieste, la limitazione del trattamento e il
registro delle prove di cancellazione, ma **non** ha più la tabella «Tickets» in fondo: quella si è spostata.
Se in tabella c'è una riga di tipo «Privacy ticket», il suo collegamento «Detail» deve portare alla **nuova**
sezione (`/tickets/…`), non a un percorso sotto `/gdpr/`.

---

## Parte 3 — Il rimpallo fra i due attori

### 3.1 La piattaforma risponde

**Azione** — dalla coda apri la richiesta, scrivi una risposta e premi «Send reply».

**Risultato atteso** — la risposta compare nel filo e la riga di riepilogo dice **`Status: Waiting for the
user`**. Sotto la casella di risposta c'è una nota che lo spiega in anticipo («la tua risposta mette la
richiesta in attesa dell'utente; l'email che parte non contiene il contenuto del filo»).

### 3.2 Il cliente capisce che tocca a lui

**Azione** — torna alla finestra del cliente e ricarica **Supporto**.

**Risultato atteso** — nell'elenco lo stato è **«Waiting for you»** («In attesa di una tua risposta» in
italiano). Aprendo la richiesta si legge la risposta e, sopra il filo, una riga in grassetto:
**«We replied — the request is waiting for you.»**

### 3.3 L'avviso di aggiornamento è arrivato per email

**Azione** — Mailpit, casella `owner@acme.test`.

**Risultato atteso** — un messaggio «Update on your request: …» che **non contiene il testo della risposta**
e che dice esplicitamente che il contenuto della conversazione resta nello spazio di lavoro.

### 3.4 La replica rimette la palla alla piattaforma

**Azione** — dal cliente, rispondi qualcosa nel filo.

**Risultato atteso** — il messaggio compare, e ricaricando la coda della piattaforma lo stato della richiesta
è tornato **«Open»**. È il cuore del ciclo di vita nuovo: non deve mai restare «in attesa dell'utente» dopo
che l'utente ha scritto.

---

## Parte 4 — L'escalation delle categorie particolari (art. 9)

Questa è la parte che vale la pena guardare con più attenzione: è una salvaguardia, e una salvaguardia che
non si vede non serve a niente.

### 4.1 Una richiesta delicata nasce urgente

**Azione** — dal cliente apri una nuova richiesta di tipo **Privacy**, oggetto `Cancellate i miei dati`,
messaggio `Vi ho mandato per errore il referto della mia malattia.`

**Risultato atteso, lato cliente** — nel dettaglio compare la scadenza *e* una frase in chiaro:
**«This is a data-rights request: by law we reply within one month, by …»** con la data. Il termine non deve
restare una data anonima in una colonna.

**Risultato atteso, lato piattaforma** — nella coda la riga ha, accanto all'oggetto, un'etichetta rossa
**«Needs review»**, priorità **High**, e nella colonna della scadenza un'etichetta **«Due soon»** (il termine
è fra meno di una settimana? no — a 30 giorni non lo è: vedi 4.3 per forzare lo stato).

### 4.2 Aprendo il dettaglio l'avviso è esplicito

**Azione** — apri la richiesta contrassegnata.

**Risultato atteso** — sopra il filo c'è un riquadro rosso: **«Needs review — The text may touch special
categories of data (art. 9): read it before replying.»**

### 4.3 La scadenza in evidenza e l'ordinamento

**Azione** — forza due scadenze diverse a database, poi ricarica la coda:

```bash
docker compose -f dev/docker-compose.yml exec -T postgres psql -U appgrove -d appgrove -c \
  "update platform.support_ticket set due_at = now() - interval '2 days'
     where subject = 'Cancellate i miei dati';"
```

**Risultato atteso** — la richiesta sale **in cima alla coda** e accanto alla data compare un'etichetta rossa
**«Overdue»**; nel dettaglio la riga della scadenza diventa rossa e riporta «— Overdue». Rimetti poi una data
futura (`now() + interval '3 days'`) e verifica che l'etichetta diventi **«Due soon»** arancione.

### 4.4 Una richiesta ordinaria non viene contrassegnata

**Azione** — apri una richiesta di tipo Supporto con oggetto `Fattura sbagliata` e messaggio
`Il totale della fattura di marzo non torna.`

**Risultato atteso** — priorità **Normal**, nessuna etichetta «Needs review». Il riconoscitore deve essere
generoso, non isterico: se contrassegna anche questa, l'elenco di parole-spia è troppo largo.

### 4.5 I filtri

**Azione** — nella coda usa i tre elenchi a tendina in alto.

**Risultato atteso** — «Type» limita a supporto o privacy; «Status» ha **cinque** stati compreso «Waiting for
the user»; «Priority» limita a bassa/normale/alta. Con `Priority = High` deve restare solo la richiesta
contrassegnata.

---

## Parte 5 — La chiusura

### 5.1 Chiudere chiede conferma

**Azione** — nel dettaglio di una richiesta, scegli stato «Closed» e premi «Update».

**Risultato atteso** — si apre una finestra di conferma: **«Close this request?»**, con la spiegazione che
l'utente non potrà più rispondere e dovrà aprirne una nuova. Premi «Annulla»: **niente deve cambiare** (lo
stato in pagina resta quello di prima).

Ripeti e conferma: la richiesta si chiude, la casella di risposta sparisce e compare «This request is
closed…».

### 5.2 Il cliente non può più scrivere

**Azione** — dal cliente riapri quella richiesta.

**Risultato atteso** — il testo «This ticket is closed: open a new one if you need anything else.» e
**nessuna** casella di risposta.

---

## Parte 6 — Controlli non visivi

### 6.1 Le colonne nuove sono valorizzate

```bash
docker compose -f dev/docker-compose.yml exec -T postgres psql -U appgrove -d appgrove -c \
  "select subject, type, source, priority, flagged_for_review, status, closed_at is not null as chiusa
     from platform.support_ticket order by created_at desc limit 5;"
```

**Risultato atteso** — le richieste aperte dal modulo hanno `source = form`; quella delicata ha
`priority = high` e `flagged_for_review = t`; quella chiusa ha `closed_at` valorizzato.
**Non deve esistere** nessuna colonna che dica *quale* categoria particolare sarebbe stata riconosciuta: la
segnalazione è volutamente un sì/no.

### 6.2 L'auto-ticket dell'esportazione fallita nasce come «evento»

```bash
docker compose -f dev/docker-compose.yml exec -T postgres psql -U appgrove -d appgrove -c \
  "select subject, source, priority from platform.support_ticket where export_job_id is not null;"
```

**Risultato atteso** — se ne esiste uno (lo produce un'esportazione GDPR fallita), ha `source = event` e
`priority = high`. Se il database è pulito la query non restituisce righe: va bene, è un caso raro da
provocare a mano.

### 6.3 L'API di amministrazione risponde sul percorso nuovo — e solo al ruolo giusto

**Azione** — prendi il token di un utente non amministratore (dagli strumenti per sviluppatori del browser,
scheda Rete, intestazione `Authorization` di una qualsiasi chiamata dal backoffice) e prova:

```bash
curl -k -s -o /dev/null -w '%{http_code}\n' \
  -H "Authorization: Bearer <token-owner>" \
  https://admin.local.appgrove.app/api/platform/v1/admin/tickets
```

**Risultato atteso** — `403`. Con il token dell'amministratore, `200` e la lista completa.

**Azione** — verifica che il vecchio percorso non risponda più:

```bash
curl -k -s -o /dev/null -w '%{http_code}\n' \
  -H "Authorization: Bearer <token-admin>" \
  https://admin.local.appgrove.app/api/platform/v1/admin/gdpr/tickets
```

**Risultato atteso** — `404`. È una rottura di contratto voluta e interna.

### 6.4 Isolamento fra conti

**Azione** — entra come un utente di un altro conto (per esempio l'owner di un secondo workspace di prova) e
apri Supporto.

**Risultato atteso** — vede **solo** le proprie richieste. Provando a chiedere per identificativo una
richiesta di `Acme`:

```bash
curl -k -s -o /dev/null -w '%{http_code}\n' \
  -H "Authorization: Bearer <token-altro-conto>" \
  https://app.local.appgrove.app/api/platform/v1/tickets/<id-di-acme>
```

**Risultato atteso** — `404` (non `403`: il ticket di un altro conto semplicemente non esiste, per chi
guarda).

### 6.5 Un guasto della posta non perde la richiesta

**Azione** — ferma Mailpit e apri una richiesta dal backoffice:

```bash
docker compose -f dev/docker-compose.yml stop mailpit
```

**Risultato atteso** — l'apertura **va comunque a buon fine** (la richiesta compare nell'elenco e nel
database); nei log del core c'è un avviso `ticket.notify invio email fallito …` e nient'altro. Riavvia
Mailpit quando hai finito (`docker compose -f dev/docker-compose.yml start mailpit`).

### 6.6 Le email esistono in entrambe le lingue

**Azione** — porta la lingua dell'utente a italiano e riapri una richiesta:

```bash
docker compose -f dev/docker-compose.yml exec -T postgres psql -U appgrove -d appgrove -c \
  "update platform.users set locale = 'it' where email = 'owner@acme.test';"
```

**Risultato atteso** — la conferma successiva arriva in italiano («Abbiamo ricevuto la tua richiesta: …») e,
se la richiesta è di tipo privacy, il testo dice «per legge dobbiamo risponderti entro un mese: il nostro
termine è il …» con la data.

---

## Cosa NON è coperto da questa change (e non va cercato)

- **Ricezione delle email in ingresso** su `privacy@`/`support@`: non implementata (dipende dall'uscita del
  servizio email dalla modalità di prova). La colonna `source` prevede già il valore `email`, ma nessuno lo
  produce.
- **Allegati**: esclusi per scelta; l'interfaccia lo dice.
- **Promemoria automatico della scadenza**: la scadenza è *visibile* e ordina la coda, ma nessuno la ricorda
  attivamente. Se non guardi la coda, il termine può passare: è un rimando scritto nei punti aperti di
  UC 0075.
- **Assegnatario della richiesta**: con un solo operatore non serve.
