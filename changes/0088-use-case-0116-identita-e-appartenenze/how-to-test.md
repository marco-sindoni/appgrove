# Come collaudare a mano la change 0088 (UC 0116 — identità della persona e appartenenze)

Questa change **non aggiunge una schermata**. Cambia dove vive l'identità di una persona: da una riga
`platform.users` *dentro* l'account a due entità distinte — l'**identità** (`platform.identity`, di
piattaforma) e l'**appartenenza** (`platform.membership`, di account). Il contratto esposto è identico:
percorsi, campi e identificativi della persona non cambiano.

Proprio per questo il collaudo a mano ha un obiettivo preciso e diverso dal solito: **verificare che a occhio
NON sia cambiato nulla**, e poi guardare con gli occhi le due cose nuove che i test automatici affermano ma
che vale la pena vedere accadere — una persona in **due** account, e la cancellazione di un account che **non**
si porta via una persona che appartiene anche altrove.

Tempo indicativo: **20 minuti** di controlli visivi, **20** di controlli non visivi (database, API, posta).

---

## Parte 0 — Avvio, migrazione, utenti

**Azione** — dalla radice del repository:

```bash
./app-start.sh
```

**Risultato atteso** — l'avvio arriva in fondo senza errori. Password di tutti gli utenti del seme:
`Password1!`.

| Utente | Dove | Cosa serve qui |
|---|---|---|
| `owner@acme.test` | <https://app.local.appgrove.app> | titolare del conto Acme: invita, cambia ruoli, rimuove |
| `member@acme.test` | <https://app.local.appgrove.app> | membro di Acme |
| `bob@bob.test` | <https://app.local.appgrove.app> | titolare del proprio conto (un conto diverso) |
| `admin@appgrove.test` | <https://admin.local.appgrove.app> | console di piattaforma (l'unica vista cross-conto) |
| — | <http://localhost:8025> | Mailpit: la posta vera dello stack |

> Se lo stack era già acceso, rilancia `./app-start.sh` (il seme è idempotente). Se il browser protesta per il
> certificato, è il proxy locale: accetta l'eccezione. Tieni due finestre separate (o una in incognito):
> cliente e piattaforma sono due sessioni diverse.

### 0.1 La migrazione è passata (non visivo, ma è il presupposto di tutto)

**Azione**

```bash
docker compose -f dev/docker-compose.yml exec -T postgres \
  psql -U appgrove -d appgrove -tAc \
  "select version, description, success from flyway_schema_history where version = '17';"
```

**Risultato atteso** — una riga `17|identity membership|t`. Se `success` non è `t`, **fermati qui**: la
guardia dei conteggi dentro la migrazione ha fatto il suo lavoro e il resto non ha senso.

### 0.2 Le due tabelle esistono, e il vincolo di troppo è caduto

**Azione**

```bash
docker compose -f dev/docker-compose.yml exec -T postgres \
  psql -U appgrove -d appgrove -c \
  "select indexname from pg_indexes where schemaname='platform'
     and indexname in ('ux_users_email','ux_users_cognito_sub',
                       'ux_identity_email','ux_identity_cognito_sub',
                       'ux_membership_tenant_identity') order by indexname;"
```

**Risultato atteso** — **tre** righe: `ux_identity_cognito_sub`, `ux_identity_email`,
`ux_membership_tenant_identity`. I due indici `ux_users_*` **non devono comparire**: erano loro a imporre
«una persona, un solo account».

### 0.3 Il seme ha cinque persone e cinque appartenenze, e `platform.users` è vuota

**Azione**

```bash
docker compose -f dev/docker-compose.yml exec -T postgres \
  psql -U appgrove -d appgrove -c \
  "select (select count(*) from platform.identity)   as identita,
          (select count(*) from platform.membership) as appartenenze,
          (select count(*) from platform.users)      as users_fredda;"
```

**Risultato atteso** — `identita >= 5`, `appartenenze >= 5`, e `users_fredda = 0`. La vecchia tabella esiste
ancora (è la rete di ritorno) ma **nessuno la scrive più**: se contiene righe, qualcosa la sta ancora
scrivendo, ed è un difetto.

---

## Parte 1 — Con una sola appartenenza NON deve cambiare nulla (la regressione da escludere)

È il caso del cento per cento delle persone di oggi. Se qui si vede un cambiamento, è una regressione.

### 1.1 Accesso e cruscotto

**Azione** — entra come `owner@acme.test` su <https://app.local.appgrove.app>.

**Risultato atteso** — accesso al primo colpo. Nessun passaggio in più, **nessun selettore di conto**, nessuna
schermata intermedia che chieda «in quale conto vuoi entrare». Arrivi direttamente al cruscotto del conto
Acme, esattamente come prima.

### 1.2 La schermata dei membri è identica a prima

**Azione** — apri **Members** dal menu di sinistra.

**Risultato atteso** — l'elenco mostra **tre** persone di Acme: `owner@acme.test` (owner),
`admin@acme.test` (admin), `member@acme.test` (member), con indirizzo, nome e ruolo. Più i **due inviti in
attesa** del seme (`invitee-admin@acme.test`, `invitee-member@acme.test`). Nessuna colonna nuova, nessun
campo vuoto, nessun «—» al posto di un nome: se un nome o un indirizzo appare vuoto, la giunzione
appartenenza→identità non sta funzionando.

### 1.3 Cambio ruolo e rimozione

**Azione** — cambia il ruolo di `member@acme.test` in **admin**, poi riportalo a **member**. Poi apri
**Account** e verifica che l'identificativo del workspace sia mostrato.

**Risultato atteso** — il ruolo cambia e resta cambiato dopo un ricaricamento della pagina. Sulla riga
dell'unico owner il selettore del ruolo **non c'è** e il pulsante di rimozione è **disabilitato** (protezione
dell'ultimo titolare, invariata).

### 1.4 Il proprio profilo e la rettifica del nome

**Azione** — apri **My data** (privacy), cambia il **nome visualizzato** in `Nome Rettificato`, salva,
ricarica.

**Risultato atteso** — il nome nuovo resta. Nota: il nome sta ora sull'**identità**, quindi è il nome della
persona e non quello che un conto le assegna. Riportalo a `Acme Owner` quando hai finito.

### 1.5 Registrazione di una persona nuova, con posta vera

**Azione** — esci, vai a **Sign up** e registrati con un indirizzo nuovo, per esempio
`prova-0088@example.test`. Apri <http://localhost:8025> e clicca il collegamento di verifica nell'email
ricevuta. Completa l'accoglienza dando un nome al workspace.

**Risultato atteso** — l'email arriva, la verifica funziona, entri e ti trovi **owner** del tuo conto nuovo.
Non visivo, a conferma che la registrazione crea *due* righe e non una:

```bash
docker compose -f dev/docker-compose.yml exec -T postgres \
  psql -U appgrove -d appgrove -c \
  "select i.email, i.cognito_sub is not null as ha_sub, m.role, m.status, a.name as conto
     from platform.identity i
     join platform.membership m on m.identity_id = i.id
     join platform.accounts a on a.id::text = m.tenant_id
    where lower(i.email) = 'prova-0088@example.test';"
```

**Risultato atteso** — **una** riga: ruolo `owner`, stato `active`, e il nome del workspace che hai scelto.

### 1.6 Invito e accettazione di una persona nuova

**Azione** — torna come `owner@acme.test`, **Members** → invita `nuovo-0088@example.test` con ruolo
`member`. Apri Mailpit, prendi il collegamento d'invito, aprilo in una finestra in incognito e imposta una
password.

**Risultato atteso** — l'invitato entra come **member di Acme** (nessuna scelta di conto: ne ha uno solo) e
compare nella schermata Members dell'owner. In Mailpit c'è l'email d'invito, nella lingua giusta.

---

## Parte 2 — Il caso che ha originato la storia: una persona, due conti

Questo è il pezzo nuovo, e va **visto**, non dedotto.

### 2.1 Costruisci il caso

Non esiste ancora un percorso di prodotto per farlo (i due percorsi d'ingresso sono di UC 0118), quindi la
seconda appartenenza si crea a mano — è una leva d'ambiente, non una funzionalità.

**Azione** — dai a `bob@bob.test` (titolare del proprio conto) una **seconda appartenenza** come `member` di
Acme:

```bash
docker compose -f dev/docker-compose.yml exec -T postgres \
  psql -U appgrove -d appgrove -c \
  "insert into platform.membership (id, tenant_id, identity_id, role, status, created_at, updated_at, created_by)
   select gen_random_uuid(),
          'a0000000-0000-4000-8000-000000000001',
          i.id, 'member', 'active', now(), now(), 'collaudo-manuale'
     from platform.identity i where i.email = 'bob@bob.test';"
```

**Risultato atteso** — `INSERT 0 1`. Prima di questa change lo stesso gesto era **impossibile**: l'indirizzo
di Bob era unico su una tabella interna all'account.

### 2.2 Le due appartenenze esistono, l'identità è una sola

**Azione**

```bash
docker compose -f dev/docker-compose.yml exec -T postgres \
  psql -U appgrove -d appgrove -c \
  "select a.name as conto, m.role, m.status
     from platform.membership m
     join platform.identity i on i.id = m.identity_id
     join platform.accounts a on a.id::text = m.tenant_id
    where i.email = 'bob@bob.test' and m.deleted_at is null
    order by m.created_at;
   select count(*) as identita_con_quell_indirizzo
     from platform.identity where email = 'bob@bob.test';"
```

**Risultato atteso** — **due** righe (`Bob Personal / owner`, `Acme Corp / member`) e
`identita_con_quell_indirizzo = 1`. Una persona, due appartenenze: è esattamente il titolo della storia.

### 2.3 La seconda appartenenza allo stesso conto è rifiutata dal database

**Azione** — riesegui **identico** il comando del punto 2.1.

**Risultato atteso** — errore
`duplicate key value violates unique constraint "ux_membership_tenant_identity"`. Il vincolo che serve
davvero vive nella banca dati, non solo nell'interfaccia: nessuna schermata può aggirarlo.

### 2.4 Bob entra ancora, e nel conto giusto

**Azione** — accedi come `bob@bob.test` in una finestra in incognito.

**Risultato atteso** — entra **senza selettore** e si trova nel **proprio** conto (`Bob Personal`, dove è
owner): è l'appartenenza più antica, il ripiego dichiarato di questa change. Il selettore del conto attivo
arriva con UC 0117 — se lo vedi adesso, qualcuno ha anticipato lavoro che non tocca a questa change.

### 2.5 Acme lo vede come proprio membro, e non sa nient'altro di lui

**Azione** — nella finestra di `owner@acme.test`, apri **Members**.

**Risultato atteso** — `bob@bob.test` compare nell'elenco come **member** di Acme. Guarda bene la riga e la
pagina: **da nessuna parte** appare che Bob ha anche un conto proprio — nessuna etichetta, nessun contatore,
nessun suggerimento, nessun messaggio d'errore che lo faccia intuire. Questa è la cosa da verificare con gli
occhi: l'owner governa le appartenenze al proprio conto, non le identità.

### 2.6 La separazione dei dati tiene (non visivo, ma è la prova che conta)

**Azione** — nella finestra di Bob (il suo conto), apri **My data** e scarica l'**esportazione del profilo**.
Apri il file JSON.

**Risultato atteso** — il documento contiene l'identificativo del conto **di Bob** e il suo ruolo lì
(`owner`). **Non** contiene l'identificativo del conto Acme, né nulla di Acme. L'esportazione di un conto
riguarda quel conto: le altre appartenenze non ci entrano.

### 2.7 Sospendere in un conto non sospende nell'altro

**Azione** — come `owner@acme.test`, in **Members**, cambia lo stato di `bob@bob.test` a **suspended** (se
l'interfaccia non espone lo stato, usa l'API: `PATCH /api/platform/v1/users/<id>` con
`{"status":"suspended"}`, dove `<id>` è l'identificativo della persona letto al punto 2.2). Poi ricarica la
finestra di Bob.

**Risultato atteso** — Bob **continua a operare nel proprio conto**: la sospensione decisa da Acme vale in
Acme. Verifica dal database che sia proprio così:

```bash
docker compose -f dev/docker-compose.yml exec -T postgres \
  psql -U appgrove -d appgrove -c \
  "select a.name as conto, m.status as stato_appartenenza, i.status as stato_persona
     from platform.membership m
     join platform.identity i on i.id = m.identity_id
     join platform.accounts a on a.id::text = m.tenant_id
    where i.email = 'bob@bob.test' and m.deleted_at is null;"
```

**Risultato atteso** — `Acme Corp | suspended | active` e `Bob Personal | active | active`. Lo **stato della
persona** resta attivo: quello lo muove solo il titolare, con la limitazione del trattamento. Rimetti
`active` su Acme quando hai finito.

### 2.8 Uscire da un conto non cancella la persona

**Azione** — come `owner@acme.test`, in **Members**, **rimuovi** `bob@bob.test` da Acme. Poi ricarica la
finestra di Bob.

**Risultato atteso** — Bob sparisce dall'elenco di Acme e **continua a lavorare nel proprio conto senza
accorgersi di nulla**. Controprova:

```bash
docker compose -f dev/docker-compose.yml exec -T postgres \
  psql -U appgrove -d appgrove -c \
  "select count(*) as identita_viva from platform.identity where email='bob@bob.test' and deleted_at is null;
   select a.name, m.deleted_at is null as viva
     from platform.membership m
     join platform.identity i on i.id = m.identity_id
     join platform.accounts a on a.id::text = m.tenant_id
    where i.email='bob@bob.test' order by m.created_at;"
```

**Risultato atteso** — `identita_viva = 1`; l'appartenenza a `Bob Personal` è viva, quella ad `Acme Corp` non
lo è più. Si chiude l'**appartenenza**, non l'identità.

---

## Parte 3 — La console di piattaforma

### 3.1 Persone e conti, come prima

**Azione** — entra come `admin@appgrove.test` su <https://admin.local.appgrove.app>, guarda la
**panoramica**, poi l'elenco **Users** e la scheda di un **conto**.

**Risultato atteso** — la panoramica conta account, persone, abbonamenti e app disabilitate come prima.
L'elenco delle persone mostra indirizzo, nome, ruolo, stato e il conto a cui appartengono. Se hai rifatto la
seconda appartenenza di Bob (punto 2.1), qui la **stessa persona compare due volte**, una per conto, con
ruoli diversi: è corretto — la console è l'unica vista che vede identità e appartenenze come cose distinte.

### 3.2 Limitazione del trattamento su una persona (art. 18)

**Azione** — in console apri **GDPR rights**, scegli bersaglio **user** e incolla l'identificativo della
persona `member@acme.test`:

```bash
docker compose -f dev/docker-compose.yml exec -T postgres \
  psql -U appgrove -d appgrove -tAc \
  "select id from platform.identity where email='member@acme.test';"
```

Applica la limitazione, confermando la finestra di dialogo. Poi prova ad accedere come `member@acme.test`
in una finestra in incognito.

**Risultato atteso** — l'accesso è **rifiutato** (messaggio di errore, nessuna navigazione del prodotto): la
limitazione sospende la **persona**, non la sua appartenenza a un conto — quindi non è aggirabile aprendo un
altro conto. Nella console la voce compare fra le limitazioni attive, con l'indirizzo e il conto di contesto.

**Azione** — rimuovi la limitazione dalla console e riprova l'accesso.

**Risultato atteso** — `member@acme.test` rientra. La limitazione è reversibile e lascia due righe nel
registro delle prove (applicata, rimossa).

---

## Parte 4 — La stretta di conformità: cancellare un conto non porta via una persona

È la verifica più delicata della change, e va fatta **in entrambi i versi**. Usa un conto usa-e-getta: la
cancellazione qui è **fisica**.

### 4.1 Prepara due conti che condividono una persona

**Azione** — registra due persone nuove dall'interfaccia (`ada-0088@example.test` e `bea-0088@example.test`,
come al punto 1.5: ognuna diventa owner del proprio conto). Poi dai ad Ada una seconda appartenenza nel conto
di Bea:

```bash
docker compose -f dev/docker-compose.yml exec -T postgres \
  psql -U appgrove -d appgrove -c \
  "insert into platform.membership (id, tenant_id, identity_id, role, status, created_at, updated_at, created_by)
   select gen_random_uuid(), mb.tenant_id, ia.id, 'member', 'active', now(), now(), 'collaudo-manuale'
     from platform.identity ia,
          platform.membership mb join platform.identity ib on ib.id = mb.identity_id
    where ia.email = 'ada-0088@example.test' and ib.email = 'bea-0088@example.test';"
```

### 4.2 Cancella il conto di Bea e guarda cosa sopravvive

**Azione** — entra come `bea-0088@example.test`, apri **My data** e chiedi l'**eliminazione del conto**;
conferma. Poi forza la purga (la grace di 14 giorni non si aspetta a mano):

```bash
docker compose -f dev/docker-compose.yml exec -T postgres \
  psql -U appgrove -d appgrove -c \
  "update platform.accounts set deletion_requested_at = now() - interval '20 days'
    where name is not null and id::text in (
      select m.tenant_id from platform.membership m
      join platform.identity i on i.id = m.identity_id
      where i.email = 'bea-0088@example.test' and m.role = 'owner');"
```

…poi lascia girare lo spazzino (o riavvia il core: `./app-start.sh`) e attendi che la purga passi.

**Risultato atteso — il verso che conta**

```bash
docker compose -f dev/docker-compose.yml exec -T postgres \
  psql -U appgrove -d appgrove -c \
  "select email from platform.identity
    where email in ('ada-0088@example.test','bea-0088@example.test') order by email;"
```

- `ada-0088@example.test` **è ancora lì**: aveva un'altra appartenenza (il proprio conto), e portarsela via
  sarebbe stato cancellare dati di un titolare diverso.
- `bea-0088@example.test` **non c'è più**: era rimasta orfana con il suo unico conto.

**Azione** — accedi come `ada-0088@example.test`.

**Risultato atteso** — entra nel proprio conto e lavora. È la prova visiva che la cancellazione di un conto
non cancella una persona che appartiene anche altrove.

### 4.3 Il secondo verso: l'ultima appartenenza

**Azione** — ripeti la cancellazione, questa volta sul conto **di Ada** (il suo unico conto rimasto), e
ricontrolla la tabella delle identità.

**Risultato atteso** — `ada-0088@example.test` **non c'è più**: cancellata l'ultima appartenenza, l'identità
è cancellabile e viene cancellata. Entrambi i versi sono necessari: uno solo non prova nulla.

---

## Parte 5 — Che cosa NON deve essere apparso

Rilettura finale, con gli occhi:

- [ ] **Nessun selettore del conto** da nessuna parte (è di UC 0117).
- [ ] **Nessuna schermata o messaggio** che dica a un'azienda che una persona ha «già un account appgrove»,
      né in modo diretto né per deduzione (contatori, etichette, errori più informativi del dovuto).
- [ ] **Nessun cambiamento visibile** per chi ha un solo conto: stesso numero di clic dall'accesso al
      cruscotto di prima della change.
- [ ] **Nessun campo vuoto** dove prima c'era un indirizzo o un nome (sarebbe una giunzione che non lega).
- [ ] `platform.users` **resta a zero righe** dopo tutte le prove sopra:

```bash
docker compose -f dev/docker-compose.yml exec -T postgres \
  psql -U appgrove -d appgrove -tAc "select count(*) from platform.users;"
```

**Risultato atteso** — `0`. La vecchia tabella è la rete di ritorno del travaso: se qualcuno la scrive,
esistono due verità sulla stessa persona e nessuna regola su quale vince.

---

## Ripulire

```bash
./app-stop.sh
docker compose -f dev/docker-compose.yml down -v   # azzera il database locale
./app-start.sh                                     # riparte da seme pulito
```

Le prove qui sopra sporcano il database locale (appartenenze aggiunte a mano, conti cancellati fisicamente):
riparti da zero prima di collaudare la storia successiva.
