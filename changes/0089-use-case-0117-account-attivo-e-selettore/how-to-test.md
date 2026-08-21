# Come collaudare a mano la change 0089 (UC 0117 — account attivo nella sessione e selettore)

Questa change decide **per conto di chi** stai lavorando. Dopo UC 0116 una persona può appartenere a più
account, ma il token non sapeva più quale scegliere: prendeva l'appartenenza più antica, che era un ripiego
dichiarato. Ora l'account attivo vive sull'identità, la regola che lo scegli è scritta una volta e attuata
due (fornitore locale e funzione che compone il token in cloud), e nella barra laterale c'è un **selettore**.

Il collaudo a mano ha tre obiettivi, in ordine di importanza:

1. **verificare che per chi ha una sola appartenenza non sia cambiato nulla** — è il cento per cento delle
   persone di oggi, e qualunque differenza qui è una regressione;
2. **vedere con gli occhi** il selettore, il cambio di account, il ricaricamento e l'avviso della scheda
   rimasta indietro;
3. **provare a mano la cosa che conta**: un account attivo **manomesso** non produce mai un token con quel
   claim.

Tempo indicativo: **25 minuti** di controlli visivi, **20** di controlli non visivi (database, API, posta).

> **Attenzione — guida scritta il giorno della 0089, il modello è andato avanti.** Se la esegui su un `main`
> che contiene anche le change **0090–0092**, due affermazioni di questa guida sono superate, e i punti
> relativi portano la nota «dopo la 0090»:
> - la **schermata di scelta dell'account** all'accesso, qui data per inesistente, **esiste** (change 0090,
>   UC 0118): compare quando si arriva senza una sessione che dica in quale account lavorare, e ha preso il
>   posto del rifiuto con messaggio del punto 4.1;
> - la seconda appartenenza si può ora creare **dal prodotto** (invito da «Membri» e accettazione), non solo
>   con la leva d'ambiente del punto 2.1.
>
> Resta invece vera l'assenza di **etichette di ruolo** nel selettore: è UC 0107, non implementata.
>
> **Nomi delle schermate.** La guida usa i nomi inglesi; con l'interfaccia in italiano leggi *Members* →
> **Membri**, *My data* → **I miei dati**, *Billing* → **Fatturazione**, *Account* → **Account**.

---

## Parte 0 — Avvio e attori

> **Prerequisito: parti da una banca dati pulita.** Questa guida presuppone il seme appena creato — per
> esempio «Acme Owner ha **una sola** appartenenza» al punto 1.3. Se hai appena collaudato un'altra change,
> il tuo database porta i residui di quelle prove (appartenenze aggiunte a mano, righe cancellate
> logicamente, colonne manomesse) e alcuni comandi qui falliscono con errori che **non** sono difetti del
> prodotto — il caso tipico è `more than one row returned by a subquery used as an expression`.
>
> ```bash
> ./app-stop.sh
> docker compose -f dev/docker-compose.yml down -v   # azzera la banca dati locale
> ./app-start.sh                                     # riparte da seme pulito
> ```
>
> Se preferisci non azzerare, controlla prima lo stato con il comando del punto 0.3 e adatta i comandi
> (tutte le letture di appartenenza qui filtrano `deleted_at is null`).

**Azione** — dalla radice del repository:

```bash
./app-start.sh
```

**Risultato atteso** — l'avvio arriva in fondo senza errori. Password di tutti gli utenti del seme:
`Password1!`.

| Utente | Dove | Cosa serve qui |
|---|---|---|
| `owner@acme.test` | <https://app.local.appgrove.app> | titolare di **Acme Corp** (una sola appartenenza) |
| `bob@bob.test` | <https://app.local.appgrove.app> | titolare di **Bob Personal**: gli daremo la seconda appartenenza |
| `admin@appgrove.test` | <https://admin.local.appgrove.app> | console di piattaforma |
| — | <http://localhost:8025> | Mailpit: la posta vera dello stack |

Identificativi fissi del seme, utili nei comandi qui sotto:

| Cosa | Valore |
|---|---|
| account **Acme Corp** | `a0000000-0000-4000-8000-000000000001` |
| account **Bob Personal** | `a0000000-0000-4000-8000-000000000002` |
| identità di **Bob** | `b0000000-0000-4000-8000-000000000004` |
| identità di **Acme Owner** | `b0000000-0000-4000-8000-000000000001` |

> Ogni comando della guida è **completo e pronto da incollare**: nessuna scorciatoia da definire prima.
> Se il browser protesta per il certificato, è il proxy locale: accetta l'eccezione. Tieni **due finestre
> separate** (una in incognito): serviranno per l'avviso «account cambiato in un'altra scheda».

### 0.1 La migrazione è passata

**Azione**

```bash
docker compose -f dev/docker-compose.yml exec -T postgres \
  psql -U appgrove -d appgrove -tAc "select version, description, success from platform.flyway_schema_history where version = '18';"
```

**Risultato atteso** — `18|active account|t`. Se `success` non è `t`, fermati: il resto non ha senso.

### 0.2 La colonna e la tabella nuove esistono, e la colonna dice *perché* sta lì

**Azione**

```bash
docker compose -f dev/docker-compose.yml exec -T postgres \
  psql -U appgrove -d appgrove -c "select col_description('platform.identity'::regclass,
                 (select attnum from pg_attribute
                   where attrelid = 'platform.identity'::regclass
                     and attname = 'active_membership_id')) as commento;"
docker compose -f dev/docker-compose.yml exec -T postgres \
  psql -U appgrove -d appgrove -c "\d platform.active_account_audit"
```

**Risultato atteso** — il commento della colonna spiega che il valore è un **suggerimento** e che non è un
attributo del gruppo di utenti Cognito perché aggiungerne uno rischia di ricreare il gruppo. La tabella
`platform.active_account_audit` esiste con `identity_id`, `from_tenant_id`, `to_tenant_id`, `executed_at` e
**nessun** indirizzo o nome: soli identificativi opachi.

### 0.3 Da quale stato parti (fotografia dei due attori)

Serve a sapere se la banca dati è pulita **prima** di manomettere qualcosa, e a rileggerla se un comando dà un
risultato inatteso.

**Azione**

```bash
docker compose -f dev/docker-compose.yml exec -T postgres \
  psql -U appgrove -d appgrove -c "select i.email, a.name as conto, m.role, m.status,
                m.deleted_at is null as viva, i.active_membership_id is not null as ha_attivo
           from platform.identity i
           join platform.membership m on m.identity_id = i.id
           join platform.accounts a on a.id::text = m.tenant_id
          where i.id in ('b0000000-0000-4000-8000-000000000001',
                         'b0000000-0000-4000-8000-000000000004')
          order by i.email, m.created_at;"
```

**Risultato atteso su seme pulito** — **due** righe, entrambe `viva = t` e `ha_attivo = f`:
`owner@acme.test / Acme Corp / owner` e `bob@bob.test / Bob Personal / owner`.

Se vedi **più** righe per una persona, o righe con `viva = f`, stai partendo dai residui di un collaudo
precedente: non è un difetto del prodotto, ma alcune attese di questa guida (per esempio «Acme Owner ha una
sola appartenenza» al punto 1.3) non valgono più. Azzera la banca dati come indicato all'inizio della Parte 0,
oppure tienine conto leggendo i comandi.

---

## Parte 1 — Una sola appartenenza: NON deve cambiare nulla (la regressione da escludere)

### 1.1 Accesso: nessun passaggio in più

**Azione** — entra come `owner@acme.test` su <https://app.local.appgrove.app>.

**Risultato atteso** — accesso al primo colpo, nessuna schermata intermedia che chieda «in quale account
vuoi entrare», si arriva al cruscotto come prima.

### 1.2 Il nome dell'account c'è, il selettore NO

**Azione** — guarda la **barra laterale**, subito **sotto il marchio appgrove**.

**Risultato atteso** — c'è il nome **Acme Corp** in grassetto, e **nient'altro**: nessuna freccetta, nessun
sottotitolo «2 account · cambia», nessun elemento cliccabile. Passa il puntatore sopra il nome: non deve
succedere niente e non deve comparire alcun cursore a mano. Il selettore non è «disabilitato»: **non
esiste**.

Controprova non visiva (nel documento reso non c'è nessun comando di cambio):

**Azione** — apri gli strumenti dello sviluppatore e cerca nel DOM l'etichetta `Switch account`
(oppure, in italiano, `Cambia account`).

**Risultato atteso** — **zero** risultati.

### 1.3 Il valore conservato non conta quando l'appartenenza è una sola

**Azione** — manometti la colonna facendola puntare all'appartenenza di **un'altra persona** (è la
manomissione che conta: la chiave esterna non la può impedire), poi ricarica la pagina di Acme Owner e
naviga.

```bash
docker compose -f dev/docker-compose.yml exec -T postgres \
  psql -U appgrove -d appgrove -c "update platform.identity
            set active_membership_id = (select m.id from platform.membership m
                                         where m.identity_id = 'b0000000-0000-4000-8000-000000000004'
                                           and m.tenant_id   = 'a0000000-0000-4000-8000-000000000002'
                                           and m.deleted_at is null)
          where id = 'b0000000-0000-4000-8000-000000000001';"
```

**Risultato atteso** — Acme Owner **continua a lavorare in Acme Corp**, come se nulla fosse: con una sola
appartenenza la regola **ignora** il valore conservato. In nessun punto dell'interfaccia compare
`Bob Personal`. Se invece finisci nell'account di Bob, hai trovato un varco fra due aziende: fermati e
segnalalo.

**Azione** — rimetti a posto:

```bash
docker compose -f dev/docker-compose.yml exec -T postgres \
  psql -U appgrove -d appgrove -c "update platform.identity set active_membership_id = null
          where id = 'b0000000-0000-4000-8000-000000000001';"
```

---

## Parte 2 — Una persona, due account: il selettore

Il giorno della 0089 non esisteva un percorso di prodotto per creare la seconda appartenenza (i modi
d'ingresso erano di UC 0118): si costruiva a mano, con una leva d'ambiente.

> **Dopo la change 0090 il percorso di prodotto esiste**: come `owner@acme.test`, in **Membri**, invita
> l'indirizzo di una persona già registrata; lei accetta dalla posta e ottiene la seconda appartenenza senza
> che nessuno tocchi la banca dati. È la via da preferire perché collauda anche il codice; il comando qui
> sotto resta la scorciatoia per costruire il caso in un colpo solo.

### 2.1 Costruisci il caso

**Azione**

```bash
docker compose -f dev/docker-compose.yml exec -T postgres \
  psql -U appgrove -d appgrove -c "insert into platform.membership
           (id, tenant_id, identity_id, role, status, created_at, updated_at, created_by)
         values (gen_random_uuid(), 'a0000000-0000-4000-8000-000000000001',
                 'b0000000-0000-4000-8000-000000000004', 'member', 'active', now(), now(), 'collaudo');"
docker compose -f dev/docker-compose.yml exec -T postgres \
  psql -U appgrove -d appgrove -c "update platform.identity
            set active_membership_id = (select m.id from platform.membership m
                                         where m.identity_id = 'b0000000-0000-4000-8000-000000000004'
                                           and m.tenant_id   = 'a0000000-0000-4000-8000-000000000002'
                                           and m.deleted_at is null)
          where id = 'b0000000-0000-4000-8000-000000000004';"
```

**Risultato atteso** — `INSERT 0 1` e `UPDATE 1`. Bob è ora **owner di Bob Personal** e **member di Acme
Corp**, con l'account attivo sul proprio.

### 2.2 Il selettore compare, e dice quanti account sono

**Azione** — entra come `bob@bob.test` in una finestra in incognito. Se compare il gate legale, accetta i
documenti (è il primo ingresso di questa sessione). Guarda la barra laterale sotto il marchio.

**Risultato atteso** — il nome **Bob Personal** in grassetto, sotto di esso il conteggio **«2 accounts ·
switch»** (le persone del seme hanno lingua `en`; in italiano è «2 account · cambia») e una freccetta a
destra: adesso è un comando. Sotto, il menu completo di un owner (Dashboard, App catalog,
Account, Billing, **Members**, …).

**Nessuna etichetta di ruolo**: non deve comparire «Sei il titolare», «Owner», «Collaboratore» o simili
(§4.6 della storia — il ruolo è per applicazione, arriverà con UC 0107). Se la vedi, è lavoro anticipato.

### 2.3 Il menu del selettore elenca i due account e marca quello attivo

**Azione** — clicca sul selettore.

**Risultato atteso** — si apre un pannellino con **due** voci: `Bob Personal` e `Acme Corp`. La voce attiva
(`Bob Personal`) è **evidenziata** e porta un segno di spunta; l'altra no. Nessuna delle due mostra un ruolo.

### 2.4 Il cambio: l'applicazione ricarica e l'esperienza cambia

**Azione** — clicca `Acme Corp`.

**Risultato atteso** — nell'ordine:

1. la pagina **si ricarica per intero** (lo vedi dalla barra di caricamento del browser) e atterra sulla
   **radice** (`/`, il cruscotto), non sulla pagina da cui venivi;
2. se è il primo ingresso di Bob in Acme, compare il **gate legale**: è corretto — l'accettazione dei
   documenti è per **account** e non per persona, ogni account è un contratto a sé. Accetta e prosegui;
3. la barra laterale mostra ora **Acme Corp** come nome dell'account;
4. **la voce «Members» è scomparsa** dal menu, e con lei Account e Billing: in Acme Bob è *member*, non
   owner. È la prova più utile della storia — la stessa persona, due esperienze diverse, perché i permessi
   seguono l'**account** e non la persona.

Se invece del ricaricamento vedi la pagina aggiornarsi «a pezzi» (nome nuovo ma menu vecchio, o viceversa),
è il difetto peggiore possibile e va segnalato.

### 2.5 La scelta è conservata lato server, non nel browser

**Azione**

```bash
docker compose -f dev/docker-compose.yml exec -T postgres \
  psql -U appgrove -d appgrove -c "select a.name as account_attivo
           from platform.identity i
           join platform.membership m on m.id = i.active_membership_id
           join platform.accounts a on a.id::text = m.tenant_id
          where i.id = 'b0000000-0000-4000-8000-000000000004';"
```

**Risultato atteso** — `Acme Corp`. È da lì che il token rileggerà l'account al prossimo rinnovo: svuotare
la memoria del browser non riporta indietro la scelta.

### 2.6 Il cambio ha lasciato una traccia di controllo, e solo identificativi opachi

**Azione**

```bash
docker compose -f dev/docker-compose.yml exec -T postgres \
  psql -U appgrove -d appgrove -c "select from_tenant_id, to_tenant_id, executed_at
           from platform.active_account_audit
          where identity_id = 'b0000000-0000-4000-8000-000000000004'
          order by executed_at;"
```

**Risultato atteso** — **una** riga, da `a0000000-…-0002` (Bob Personal) a `a0000000-…-0001` (Acme Corp).
Nessun indirizzo di posta, nessun nome: la traccia risponde a «chi ha fatto cosa e per conto di chi» senza
raccogliere nulla in più.

### 2.7 Chiedere l'account su cui si è già non lascia una prova falsa

**Azione** — riapri il selettore e clicca l'account **su cui sei già** (`Acme Corp`), poi ricontrolla il
registro col comando del punto 2.6.

**Risultato atteso** — sempre **una** sola riga. Un cambio che non è avvenuto non si registra: una prova di
cambio senza cambio sarebbe una prova falsa.

### 2.8 Torna indietro

**Azione** — dal selettore, torna su `Bob Personal`.

**Risultato atteso** — ricaricamento, nome `Bob Personal`, e **«Members» è tornato** nel menu. Il registro
ha ora **due** righe, la seconda verso `a0000000-…-0002`.

### 2.9 Acme non deve sapere nulla dell'altro account di Bob

**Azione** — nella finestra di `owner@acme.test`, apri **Members**.

**Risultato atteso** — `bob@bob.test` compare come *member* di Acme e **nulla** nella pagina dice che Bob ha
anche un account proprio: nessuna etichetta, nessun contatore, nessun suggerimento. L'owner governa le
appartenenze al proprio account, non le identità.

---

## Parte 3 — L'avviso «l'account è cambiato in un'altra scheda»

Serve una **seconda scheda** nella stessa sessione di Bob.

**Azione**

1. nella finestra in incognito di Bob (che è su `Bob Personal`), apri una **seconda scheda** sulla stessa
   applicazione e lasciala sul cruscotto;
2. nella **prima** scheda, cambia account su `Acme Corp` dal selettore;
3. torna sulla **seconda** scheda e clicca dentro la pagina (il rientro sulla scheda è il momento in cui
   la lettura si rinfresca).

**Risultato atteso** — in cima al contenuto della seconda scheda appare una fascia di avviso:
**«L'account attivo è cambiato in un'altra scheda»**, con un pulsante **Ricarica**. Non è un errore rosso:
non c'è nessun varco (quel token vale per un account a cui Bob appartiene davvero), è una **confusione** da
sciogliere.

**Azione** — clicca **Ricarica**.

**Risultato atteso** — la scheda ricarica, atterra su `/` e mostra `Acme Corp`: le due schede tornano
d'accordo. L'avviso sparisce.

---

## Parte 4 — La prova di sicurezza: un account attivo manomesso non produce un claim

È la verifica che giustifica l'intera architettura della storia. Va fatta **a mano**, perché nessuna
schermata può produrre questo stato.

### 4.1 L'appartenenza puntata è di un'altra persona

**Azione** — fai puntare l'account attivo di Bob all'appartenenza di **Acme Owner**, che non è sua, e poi
prova ad accedere:

```bash
docker compose -f dev/docker-compose.yml exec -T postgres \
  psql -U appgrove -d appgrove -c "update platform.identity
            set active_membership_id = (select m.id from platform.membership m
                                         where m.identity_id = 'b0000000-0000-4000-8000-000000000001'
                                           and m.deleted_at is null)
          where id = 'b0000000-0000-4000-8000-000000000004';"
```

**Azione** — esci dalla sessione di Bob e rientra come `bob@bob.test`.

**Risultato atteso al giorno della 0089** — l'accesso era **rifiutato** con un messaggio che dice cosa
succede — *«Appartieni a più account e nessuno è impostato come attivo: scegli l'account su cui vuoi
lavorare»* — e **non** «credenziali non valide», che sarebbe una bugia.

**Dopo la change 0090** al posto del rifiuto compare la **schermata «Choose an account»**: quel caso ora ha
una superficie per rispondere, e il messaggio del 409 resta come rete di ritorno dei percorsi **non
interattivi** (per esempio un rinnovo di sessione che fallisce riporta al modulo d'accesso con quella frase).
Vedere la schermata qui è corretto, non una regressione.

**L'invariante di sicurezza, che è il vero oggetto di questo punto, non cambia e va verificato in entrambi i
casi:** Bob **non entra mai in `Acme Corp` come owner**. L'appartenenza si riverifica quando si crea il
token, e il valore conservato **non è creduto**. Attenzione a leggere bene il caso: nella schermata `Acme
Corp` **può** comparire, perché al punto 2.1 gli hai dato un'appartenenza vera lì — se la scegli entri come
**member**, che è giusto. Il difetto da cercare è il ruolo: se Bob si ritrova owner di Acme, o vede in Acme
qualcosa che solo un titolare vede, il valore manomesso è stato creduto ed è un difetto grave.

> Il caso è raro per costruzione — serve un'appartenenza attiva puntata che non sia della persona — e la via
> d'uscita automatica è il punto 4.2.

### 4.2 Con una sola appartenenza residua la regola sceglie da sé

**Azione** — revoca l'appartenenza di Bob ad Acme lasciando puntata la colonna manomessa, poi riprova
l'accesso:

```bash
docker compose -f dev/docker-compose.yml exec -T postgres \
  psql -U appgrove -d appgrove -c "update platform.membership set status = 'revoked', updated_at = now()
          where identity_id = 'b0000000-0000-4000-8000-000000000004'
            and tenant_id = 'a0000000-0000-4000-8000-000000000001';"
```

**Risultato atteso** — Bob **entra** e si trova in `Bob Personal`: resta una sola appartenenza attiva, quindi
il valore conservato (sbagliato) viene ignorato. Nella barra laterale il selettore è di nuovo **assente** e
resta solo il nome.

### 4.3 Un account che non è tuo si rifiuta con «non trovato», non con «vietato»

Verifica non visiva, dall'interfaccia programmatica. Prendi un token di Bob:

```bash
TOKEN=$(curl -sk https://app.local.appgrove.app/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"bob@bob.test","password":"Password1!"}' | python3 -c 'import sys,json;print(json.load(sys.stdin)["access_token"])')
```

**Azione** — leggi le tue appartenenze, poi chiedi un account che non è tuo:

```bash
curl -sk https://app.local.appgrove.app/api/platform/v1/me/memberships \
  -H "Authorization: Bearer $TOKEN"

curl -sk -o /dev/null -w '%{http_code}\n' -X POST \
  https://app.local.appgrove.app/api/platform/v1/me/active-account \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"accountId":"a0000000-0000-4000-8000-000000000003"}'
```

**Risultato atteso** — la prima chiamata elenca **solo** gli account di Bob, con `activeAccountId`
valorizzato; la seconda risponde **404**, non 403. La differenza non è formale: un 403 direbbe «quell'account
esiste, ma non è tuo», e l'esistenza di un account non è un'informazione che appartiene a chi chiede.

**Azione** — prova ora un account **tuo** (l'identificativo lo hai appena letto):

```bash
curl -sk -o /dev/null -w '%{http_code}\n' -X POST \
  https://app.local.appgrove.app/api/platform/v1/me/active-account \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"accountId":"a0000000-0000-4000-8000-000000000002"}'
```

**Risultato atteso** — **204**, e **nessun token nel corpo della risposta**: il cambio scrive la scelta e
nulla più. Il rinnovo passa dal percorso di rinnovo esistente, così l'account si stabilisce in un posto solo.

### 4.4 Senza token non si legge nulla

**Azione**

```bash
curl -sk -o /dev/null -w '%{http_code}\n' https://app.local.appgrove.app/api/platform/v1/me/memberships
```

**Risultato atteso** — **401**. Il perimetro di queste due operazioni è sempre la persona del token, mai un
identificativo che arrivi da chi chiama.

---

## Parte 5 — Il token precedente resta valido per il suo account (comportamento atteso)

Non è un difetto e va visto una volta, per non scoprirlo un giorno e chiamarlo varco.

**Azione** — rimetti attiva l'appartenenza di Bob ad Acme (revocata al punto 4.2), prendi un token
**adesso** (nasce con `Bob Personal`), poi cambia account e **riusa il token vecchio**:

```bash
docker compose -f dev/docker-compose.yml exec -T postgres \
  psql -U appgrove -d appgrove -c "update platform.membership set status = 'active', updated_at = now()
          where identity_id = 'b0000000-0000-4000-8000-000000000004'
            and tenant_id = 'a0000000-0000-4000-8000-000000000001';"

TOKEN=$(curl -sk https://app.local.appgrove.app/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"bob@bob.test","password":"Password1!"}' | python3 -c 'import sys,json;print(json.load(sys.stdin)["access_token"])')

curl -sk -o /dev/null -w 'cambio: %{http_code}\n' -X POST \
  https://app.local.appgrove.app/api/platform/v1/me/active-account \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"accountId":"a0000000-0000-4000-8000-000000000001"}'

curl -sk https://app.local.appgrove.app/api/platform/v1/accounts/me \
  -H "Authorization: Bearer $TOKEN"
```

**Risultato atteso** — il cambio risponde `204`, ma l'ultima chiamata riguarda ancora `Bob Personal`, cioè
l'account **vecchio** — e non è un errore: quel token vale
per un account a cui Bob appartiene davvero, e vale fino alla sua scadenza (15 minuti). La durata
dell'access token **è** il ritardo massimo con cui una revoca ha effetto — scritto in
[docs/02 §10](../../docs/02-auth-sicurezza.md). La stretta per le operazioni che modificano dati è di
UC 0099.

---

## Parte 6 — Che cosa NON deve essere apparso

Rilettura finale, con gli occhi:

- [ ] **nessun selettore** per chi ha una sola appartenenza — non «disabilitato»: assente dal documento;
- [ ] **nessuna etichetta di ruolo** nel selettore o accanto al nome dell'account («Owner», «Sei il
      titolare», «Collaboratore»): arriva con UC 0107;
- [ ] **nessun nome di account inventato**: se la lettura delle appartenenze fallisce (spegni lo stack con
      `./app-stop.sh` lasciando la pagina aperta, poi ricaricala), la barra laterale mostra **niente** al
      posto del nome — non un segnaposto, non un errore rosso dentro il menu;
- [ ] **nessun mezzo cambio**: dopo ogni cambio la pagina si ricarica per intero, non si aggiorna a pezzi;
- [ ] ~~**nessuna schermata di scelta dell'account** all'accesso: non esiste ancora, è di UC 0118 (al suo
      posto c'è il rifiuto con messaggio comprensibile del punto 4.1);~~ **superata dalla change 0090**: la
      schermata «Choose an account» ora esiste e ha preso il posto del rifiuto. Vederla con **due o più**
      appartenenze è corretto; il controllo che resta è che con **una sola** appartenenza **non** compaia;
- [ ] **nessun indirizzo o nome** nella tabella `platform.active_account_audit`.

---

## Ripulire

```bash
./app-stop.sh
docker compose -f dev/docker-compose.yml down -v   # azzera il database locale
./app-start.sh                                     # riparte da seme pulito
```

Le prove qui sopra sporcano il database locale (una seconda appartenenza aggiunta a mano, una colonna
manomessa, un'appartenenza revocata): riparti da zero prima di collaudare la storia successiva.
