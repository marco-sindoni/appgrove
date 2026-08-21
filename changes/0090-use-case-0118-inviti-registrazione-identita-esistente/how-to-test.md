# Come verificare a mano la change 0090 (UC 0118 — inviti e registrazione con identità esistente)

Guida di verifica **manuale**, con gli occhi. Non ripete i collaudi automatici (quelli sono già verdi:
`./run-tests.sh`): serve a vedere di persona che i due percorsi d'ingresso funzionano e che nessun
messaggio dice all'azienda ciò che non le appartiene.

Ogni voce è **azione → risultato atteso**. Le voci marcate **[non visivo]** si verificano con una
chiamata alle API, una riga di banca dati o una casella di posta.

---

## 0. Avvio dello stack

| Azione | Risultato atteso |
|---|---|
| `./app-start.sh` dalla radice del monorepo | Riepilogo finale **«Tutto su e sano.»**. Backoffice su `https://app.local.appgrove.app`, Mailpit su `http://localhost:8025`, Postgres su `localhost:5432`. |
| Guardare le righe del `seed` nell'output (o rilanciare `./dev.sh seed`) | **Nessun** `ERROR: duplicate key value violates unique constraint ux_membership_tenant_identity`. Era il difetto ereditato dalla change `0088`: il ri-seme delle appartenenze stampava quell'errore su ogni banca dati già migrata. Ora il ri-seme è silenzioso e idempotente. |
| Rilanciare `./dev.sh seed` una seconda volta | Di nuovo silenzioso: è la prova che l'idempotenza è tornata. |

Password di tutte le utenze del seme: **`Password1!`**.

---

## 1. Percorso A — l'azienda invita una persona che ha già un account

Lo stato di partenza del seme è perfetto per questo: **`bob@bob.test`** ha già un account tutto suo
(«Bob Personal»), e **Acme Corp** è un'altra azienda con il suo owner.

### 1.1 L'invito parte, e chi invita non impara nulla

| Azione | Risultato atteso |
|---|---|
| Accedi al backoffice come **`owner@acme.test`** e apri **Members** | La pagina dei membri di Acme. Sotto il modulo d'invito c'è **una riga nuova**: «*The seat belongs to this account: it is paid here even if the person already works in another account.*» (in italiano se l'interfaccia è in italiano). È la regola dei posti detta **prima** che il cliente la scopra in fattura. |
| Nel modulo d'invito scrivi **`bob@bob.test`**, ruolo *Member*, e invia | **Invito creato**, messaggio verde «Invitation sent to bob@bob.test.», riga nella tabella degli inviti pendenti, collegamento d'invito mostrato con il pulsante di copia. **Nessun** messaggio del tipo «questa persona ha già un account appgrove»: l'esito è **identico** a quello di un indirizzo mai visto. |
| Ripeti l'invito allo **stesso** indirizzo | Rifiuto con **«There is already a pending invitation for this address.»** — messaggio *specifico*, diverso da quello del passo seguente. |
| Invita **`member@acme.test`** (che è già membro di Acme) | Rifiuto con **«This person is already a member of this account.»** — il secondo messaggio, distinto dal primo. Sono le due collisioni *lecite*: riguardano informazioni dell'account. |
| Invita un indirizzo mai visto, per esempio `mai-visto@example.test` | Creato, con **esattamente** lo stesso aspetto dell'invito a `bob@bob.test`. Metti le due schermate a confronto: devono essere indistinguibili. |

**[non visivo] La differenza esiste, ma solo lato server.** Con `psql`:

```bash
docker compose -f dev/docker-compose.yml exec -T postgres \
  psql -U appgrove -d appgrove -c \
  "select email, identity_id is not null as identita_collegata
     from platform.invitations where status = 'pending' order by created_at desc limit 3;"
```

Atteso: `bob@bob.test` → `t` (collegata), `mai-visto@example.test` → `f`. È il valore che **non compare**
in nessuna risposta e in nessuna schermata dell'account che invita.

### 1.2 Il collegamento dall'email non chiede una password nuova

| Azione | Risultato atteso |
|---|---|
| Apri **Mailpit** (`http://localhost:8025`) e trova l'email d'invito indirizzata a `bob@bob.test` | Messaggio «You have been invited to appgrove» con il collegamento `/accept?token=…`. |
| **In una finestra anonima** (o dopo il logout) apri quel collegamento | Per un istante «*Checking the invitation…*», poi la schermata **«You already have an appgrove account»**: il testo dice che l'invito lo aspetta in testa al suo cruscotto, e c'è un solo comando, **«Sign in to accept»**. **Non c'è nessun campo password**: è il punto della storia — una parola d'accesso nuova su un'identità che esiste già sarebbe una seconda identità mascherata. |
| Per confronto, apri il collegamento dell'invito del seme non ancora usato: `https://app.local.appgrove.app/accept?token=seed-invite-acme-member` | Qui invece compare il **modulo con la password** («Join the workspace»): quell'indirizzo (`invitee-member@acme.test`) non ha ancora un'identità. Le due schermate devono essere visibilmente diverse. |

### 1.3 L'accettazione dalla propria sessione

| Azione | Risultato atteso |
|---|---|
| Accedi come **`bob@bob.test`** | Si entra in **Bob Personal**. Nella barra laterale, sotto il marchio, c'è il **nome dell'account** ma **nessun selettore**: Bob ha una sola appartenenza. |
| Guardare la voce **Dashboard** del menu | Porta un **numero** accanto all'etichetta: `1`. È lì perché un invito visto solo dal cruscotto resterebbe invisibile da qualunque altra schermata. |
| Guardare la **testa del cruscotto** | **Prima** degli avvisi e delle applicazioni c'è la sezione **«Invitations for you»**: «*Acme Corp invites you to work in their account.*», la riga «*The seat is paid by the account that invites you, not by you.*», e i due comandi **Accept** / **Decline**. |
| Premere **Accept** | La pagina **si ricarica** (è parte del comportamento: mezza applicazione con l'account nuovo e mezza col vecchio è il modo peggiore di sbagliare). Dopo il ricaricamento si è **dentro Acme Corp**: primo ingresso, quindi compare il **gate legale** — accettare i documenti. |
| Guardare la barra laterale dopo l'accettazione | Il nome dell'account è **Acme Corp**, e ora **il selettore esiste** («2 accounts · switch»): le appartenenze sono due. La voce **Members** **non** c'è più — in Acme Bob è *member*, non owner. |
| Aprire il selettore e tornare a **Bob Personal** | Ricaricamento, e si è di nuovo nel proprio account: **Members** ricompare, la sezione degli inviti è vuota e il numero sul menu è sparito. |

**[non visivo] Una identità, due appartenenze:**

```bash
docker compose -f dev/docker-compose.yml exec -T postgres \
  psql -U appgrove -d appgrove -c \
  "select (select count(*) from platform.identity where email='bob@bob.test' and deleted_at is null) as identita,
          (select count(*) from platform.membership m join platform.identity i on i.id=m.identity_id
             where i.email='bob@bob.test' and m.deleted_at is null) as appartenenze;"
```

Atteso: `identita = 1`, `appartenenze = 2`. **Nessuna seconda identità**, nessuna seconda password.

### 1.4 Il rifiuto

| Azione | Risultato atteso |
|---|---|
| Come `owner@acme.test`, invita `member@example.test`… oppure ripeti il giro con un'altra persona del seme, per esempio invita **`admin@appgrove.test`** da Acme | Invito creato. |
| Accedi come quella persona e premi **Decline** nella sezione del cruscotto | La voce **sparisce** e il numero sul menu si azzera. **Nessuna** appartenenza nuova: il selettore non compare. |

**[non visivo]** `select status from platform.invitations where email = '…';` → **`rejected`** (non
`revoked`: revocare è l'atto di chi invita, rifiutare è l'atto della persona invitata — la storia
dell'invito deve poter dire chi l'ha chiuso).

---

## 2. Percorso B — chi è già membro apre un proprio account

| Azione | Risultato atteso |
|---|---|
| Accedi come **`member@acme.test`** (persona che esiste solo come membro di Acme) | Si entra in Acme Corp, senza selettore (una sola appartenenza). |
| Apri **Account** e scorri in fondo | Sezione nuova **«Open another account»**, con la spiegazione «*You can own your own account and stay a member of the others. Nothing moves between them.*», **un solo campo** («Account name») e il pulsante. **Non** viene chiesta né la password né il nome della persona: li ha già. |
| Scrivi un nome (per esempio «Studio di Mario») e premere **Open account** | La pagina **si ricarica** e si atterra nel **nuovo** account: gate legale del nuovo account (ogni account è un contratto a sé), poi il nome nuovo nella barra laterale, il **selettore** con due account e la voce **Members** presente — nel proprio account si è **owner**. |
| Tornare ad Acme dal selettore | Si è di nuovo *member*: **Members** scompare. Una persona sola, due esperienze. |
| **Controprova**: esci, vai su `/signup` e prova a registrarti con **`member@acme.test`** | Rifiuto con un messaggio **azionabile**: «*This email is already registered. Sign in: from your session you can open a new account.*» — non più il secco «già registrata» che spingeva a inventarsi un secondo indirizzo. |

---

## 3. La schermata di scelta dell'account all'accesso

È il caso raro che UC 0117 aveva lasciato senza superficie: **più** appartenenze attive e **nessuna**
indicata come attiva. Per costruzione non capita quasi mai (ogni appartenenza nuova nasce già attiva), e
per vederlo va **forzato**.

| Azione | Risultato atteso |
|---|---|
| **[non visivo]** Con Bob che ha due appartenenze (fine del §1.3), azzerare la scelta: `psql … -c "update platform.identity set active_membership_id = null where email='bob@bob.test';"` | Nessun output particolare. |
| Esci dal backoffice (o finestra anonima) e accedi come **`bob@bob.test`** | **Non** si entra e **non** compare un errore: compare la schermata **«Choose an account»**, con la spiegazione «*You belong to more than one account…*» e **due pulsanti**, uno per ciascun account. Prima di questa change qui c'era un messaggio d'errore e la persona restava fuori. |
| Premere uno dei due | Si entra **in quell'account**: il nome nella barra laterale è quello scelto. |
| Uscire e riaccedere | **Non** viene chiesto di nuovo: la scelta è stata **conservata**. |

**[non visivo]** `select active_membership_id is not null from platform.identity where email='bob@bob.test';`
→ `t` (la scelta è sul server, dove il token la rileggerà e la riverificherà).

---

## 4. Riuso di un indirizzo cancellato (rifiuto comprensibile, non un errore)

| Azione | Risultato atteso |
|---|---|
| **[non visivo]** Creare un'identità cancellata: `psql … -c "insert into platform.identity(id,cognito_sub,email,locale,status,created_at,updated_at,deleted_at) values (gen_random_uuid(),'sub-manuale-0118','riuso-manuale@example.test','en','active',now(),now(),now());"` | Riga inserita. |
| Andare su `/signup` e registrarsi con **`riuso-manuale@example.test`** | Rifiuto con lo **stesso** messaggio di un indirizzo vivo («already registered…»). Prima di questa change qui usciva un **errore del servizio** (500), perché il controllo di esistenza ignorava le righe cancellate mentre l'indice unico no. |

---

## 5. Riservatezza — la prova da fare guardando, non leggendo il codice

| Azione | Risultato atteso |
|---|---|
| Come `owner@acme.test`, invitare due indirizzi di seguito: uno che **esiste** (`admin@appgrove.test`) e uno che **non esiste** (`nessuno-0118@example.test`) | Le due risposte sullo schermo devono essere **la stessa cosa**: stesso messaggio verde, stessa riga nella tabella, stesso collegamento con il pulsante di copia. Nessuna spia, nessuna sfumatura, nessun ritardo percepibile. |
| Aprire gli strumenti per sviluppatori del browser, scheda **Rete**, e confrontare i due `POST /api/platform/v1/invitations` | **Stesso** codice `201` e **stesse chiavi** nel corpo della risposta. Il campo `identityId` **non compare** in nessuna delle due. |

---

## 6. Non-regressione da guardare (le cose che NON devono essere cambiate)

| Azione | Risultato atteso |
|---|---|
| Accedere come **`owner@acme.test`** (una sola appartenenza) | Nessun selettore, nessuna sezione inviti, nessun numero sul menu, nessun passaggio in più: chi ha un solo account non deve accorgersi che questa storia esiste. |
| Accettare l'invito del seme non usato (`/accept?token=seed-invite-acme-admin`) impostando una password | Funziona come prima: identità nuova + appartenenza in Acme + accesso automatico. Il percorso di chi **non** esiste ancora è intatto. |
| Fare accesso con la password sbagliata | «Invalid email or password.» — invariato. |
