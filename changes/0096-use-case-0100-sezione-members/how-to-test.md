# Come verificare a mano la change 0096 — «Members» come elenco unico di persone

> Guida scritta sul branch `change/0096-use-case-0100-sezione-members`, base `ae8a692`, il 2026-08-22.
> È una **fotografia**: se una change successiva cambia questi comportamenti, i punti superati si scoprono
> **rieseguendola**, non rileggendola.
>
> **Passi non visivi già eseguiti** dall'agente il 2026-08-22 sullo stack locale — §1, §2, §3, §6.1 — con gli
> esiti riportati qui **come si leggono davvero**. Eseguirli ha prodotto **quattro correzioni alla guida** —
> l'indirizzo con il punto di domanda va fra apici, altrimenti la shell interattiva lo interpreta come un
> carattere jolly e non chiama nulla; il programma Python del §1 era scritto con apici sfuggiti dentro una
> stringa formattata e non compilava; l'elenco dei diritti dell'account non contiene `crm`, che la prima
> stesura dava per presente; la lettura del ruolo memorizzato non filtrava sugli inviti **in attesa** e a
> una seconda esecuzione mostrava anche quelli revocati dalla prima — e **nessun difetto di prodotto**. Una
> quinta correzione riguarda i passi visivi, che non si possono eseguire ma le cui **etichette** si possono
> verificare: erano scritte in italiano, mentre l'interfaccia parte in **inglese** perché le persone del
> seme hanno l'inglese come lingua. Ora sono quelle che si leggono davvero a schermo. Un'osservazione da leggere prima di sorprendersi sta al §1 bis.
> Restano allo sviluppatore i passi **visivi** di §4, §5 e §6.2 — con le etichette corrette.
>
> **Passata di fine lotto (change 0095–0099), 2026-08-22.** Tutti i passi non visivi sono stati
> **rieseguiti** contro lo stato finale di `main`: §1, §2, §3 e §6.1 — **tutti verdi nel merito** (elenco
> con applicazioni e data di ingresso, `403`/`200` sul governo delle persone, invito senza ruolo, chiavi di
> lingua). Nessun difetto di prodotto. Sono emersi però **sei scostamenti**, tutti da storie successive del
> lotto, e tutti corretti qui:
>
> 1. **§1 — la tabella dei risultati attesi era un elenco di righe fisse** e la composizione dell'account
>    cambia a ogni giro delle altre suite (all'esecuzione: `bob@bob.test` non c'era più, `member@acme.test`
>    aveva una applicazione invece di due). Riscritta per verificare le **tre regole**, dichiarando lo stato
>    di partenza: è la stessa lezione che le guide 0097, 0098 e 0099 avevano già imparato per sé;
> 2. **§1 bis — il numero di applicazioni dell'owner regge grazie alla change 0098**: dopo UC 0103 l'account
>    ha un abbonamento in più (la voce di piattaforma dei posti) e resta fuori dall'elenco solo perché
>    quella storia la esclude. Aggiunto un controllo **eseguibile** che lo dimostra invece di darlo per
>    buono;
> 3. **§3 — l'invito ora passa dalla cassa** (change 0098): compra un posto, fa crescere l'abbonamento dei
>    posti e può rispondere `402`. Il contratto dell'invito verificato dal paragrafo è intatto; è cambiato
>    ciò che l'invito lascia dietro di sé, ed è ora dichiarato prima dei comandi;
> 4. **§3, «Pulizia» — «così il seme resta com'era» non è più vero**: la revoca libera il posto ma non
>    annulla l'addebito (misurato: posti `11 → 13 → 11`, quantità pagata `7 → 10 → 10`). Riscritto, con la
>    via vera per tornare indietro;
> 5. **§4 — la schermata ora contiene anche i posti**: «esattamente sei intestazioni» sono sei con etichetta
>    più una colonna di selezione senza intestazione visibile (change 0099), e le «due righe» sotto il
>    modulo di invito sono **tre** (change 0098). Il conteggio precedente era vero: era intermedio;
> 6. **§6.2 — gli stati sono quattro**, non tre: si è aggiunto «Ending …» (change 0099).
>
> Le correzioni ai punti 1–4 sono state **rieseguite**; quelle ai punti 5 e 6 riguardano prosa puramente
> visiva, verificata sul codice della schermata (`frontend/apps/backoffice/src/pages/members/MembersPage.tsx`
> e le chiavi di lingua) e non rieseguibile per definizione.

Guida di verifica **manuale** per UC 0100. Serve a vedere con i propri occhi le quattro cose che questa
change fa, e che nessun collaudo automatico può mostrare a una persona:

1. la schermata «Members» è **una** tabella e non due: le persone che ci sono e quelle che stanno
   arrivando stanno nello stesso elenco, e il numero delle persone si legge senza sommare a mente;
2. la **colonna del ruolo è sparita**, e al suo posto c'è l'informazione che nascondeva: su quante e
   **quali** applicazioni ciascuno è abilitato;
3. l'**invito non chiede più il ruolo** — ed è la sparizione che più facilmente si legge come un difetto,
   per questo la pagina la spiega;
4. la sezione è del **solo owner**: un collaboratore non la apre, e soprattutto il servizio gli dice no.

Ogni voce è **azione → risultato atteso**.

---

## 0. Preparazione

| # | Azione | Risultato atteso |
|---|---|---|
| 0.1 | `./app-start.sh` | Tutto verde: Postgres, proxy, Mailpit, MinIO, ElasticMQ, i servizi backend scoperti (`auth`, `core`, `crm`, `fatture`) e le due SPA. |
| 0.2 | `./dev.sh services` | La mappa scoperta elenca `fatture` (porta 8081, schema `app_fatture`) e `crm` (8082, `app_crm`). |

**I due token che servono.** Comandi completi e incollabili:

```bash
TOKEN_OWNER=$(curl -sk https://api.local.appgrove.app/api/auth/login \
  -H 'content-type: application/json' \
  -d '{"email":"owner@acme.test","password":"Password1!"}' \
  | python3 -c 'import sys,json;print(json.load(sys.stdin)["access_token"])')
echo "${#TOKEN_OWNER} caratteri"   # > 0 = pronto

TOKEN_MEMBER=$(curl -sk https://api.local.appgrove.app/api/auth/login \
  -H 'content-type: application/json' \
  -d '{"email":"member@acme.test","password":"Password1!"}' \
  | python3 -c 'import sys,json;print(json.load(sys.stdin).get("access_token",""))')
echo "${#TOKEN_MEMBER} caratteri"   # > 0 = pronto
```

> **Attenzione all'apostrofo mancante intorno agli indirizzi con il punto di domanda.** Tutti i comandi qui
> sotto racchiudono l'indirizzo fra apici doppi: senza, la shell interattiva prende `?size=100` per un
> carattere jolly e non chiama nulla (`no matches found`). È il primo dei quattro errori trovati eseguendo
> questa guida.

---

## 1. L'elenco delle persone porta le applicazioni e la data di ingresso

```bash
curl -sk "https://api.local.appgrove.app/api/platform/v1/users?size=100" \
  -H "authorization: Bearer $TOKEN_OWNER" \
  | python3 -c "
import sys, json
d = json.load(sys.stdin)
for r in sorted(d['content'], key=lambda r: r['email']):
    apps = r.get('apps') or []
    print('{:<28} {:<7} dal {}  {} app: {}'.format(
        r['email'], r['role'], r['joinedAt'][:10], len(apps),
        ', '.join('{}({})'.format(a['app'], a.get('role') or 'implicito') for a in apps)))
"
```

**Risultato atteso.** *Non un elenco di righe: la composizione dipende da chi c'è nell'account, e chi c'è
cambia a ogni giro delle altre suite e delle altre guide.* Su una banca dati **appena seminata**
(`./app-stop.sh --wipe && ./app-start.sh`) le righe sono quelle qui sotto; se sono già girate la suite di
piattaforma o un'altra guida del lotto ci saranno **altre righe** (spesso con `0 app`), qualcuna delle
quattro potrà **non esserci più** e i conteggi di applicazioni saranno diversi — non è un difetto, è lo
stato che qualcun altro ha lasciato. Quello che si verifica sono le **tre regole** dell'elenco (sotto), non
questi numeri:

```
admin@acme.test              member  dal 2024-01-01  1 app: crm(admin)
bob@bob.test                 member  dal 2026-08-21  0 app:
member@acme.test             member  dal 2024-01-01  2 app: crm(editor), fatture(editor)
owner@acme.test              owner   dal 2024-01-01  3 app: fatture(implicito), notes(implicito), teams(implicito)
```

*(Esempio di scostamento legittimo, visto nella passata di fine lotto: `bob@bob.test` non c'era più — la
sua appartenenza ad Acme era stata chiusa da una suite — e `member@acme.test` aveva **una** applicazione
invece di due, perché la guida della change 0095 revoca `fatture` al passo 3.3. Entrambe le regole
reggevano.)*

Le tre cose da leggere:

- **ogni riga ha una data di ingresso** — è la nascita dell'appartenenza, non dell'identità: la persona può
  esistere da prima, in un altro account;
- l'**owner** ha le applicazioni **a cui l'account ha diritto**, tutte marcate `implicito`: non ha righe di
  permesso, il suo accesso è implicito (UC 0098 §5). Verificabile:
  ```bash
  docker compose -f dev/docker-compose.yml exec -T postgres psql -U appgrove -d appgrove -c \
    "select count(*) as righe_di_permesso_dell_owner from platform.app_access
      where identity_id = 'b0000000-0000-4000-8000-000000000001' and deleted_at is null;"
  ```
  → `0`;
- chi **non è abilitato a nulla** ha `0 app`, ed è uno stato legittimo (è entrato e non è ancora stato
  abilitato), non un errore.

### 1 bis. Perché l'owner NON vede `crm` e due collaboratori sì

Nel seme locale l'account Acme è abbonato a `legacy`, `notes` e `teams`, e ha diritto a `fatture` per la
fascia gratuita — **non** a `crm`. Ma il seme concede a `admin@acme.test` e a `member@acme.test` un permesso
su `crm`. L'elenco dice quindi due cose entrambe vere e diverse fra loro:

- per l'**owner** dice a quali applicazioni **l'account ha diritto** (è ciò che lui può aprire);
- per gli **altri** dice quali **permessi** hanno, e un permesso **sopravvive** al decadere del diritto
  dell'account, di proposito (UC 0098: «riattivandola gli accessi tornano validi senza doverli
  ricostruire»).

Non è un difetto ed è come la storia lo chiede (§4.2 «a quante applicazioni è abilitata» per le persone,
«applicazioni con diritto dell'account» per l'owner). Va saputo prima di guardare lo schermo, altrimenti
l'asimmetria sembra un errore di conteggio.

**Aggiunta della passata di fine lotto: il numero dell'owner regge grazie alla change 0098.** Dopo la
storia dei posti (UC 0103) l'account Acme ha un abbonamento in più — quello della voce di **piattaforma**
dei posti — e senza un'esclusione esplicita l'elenco direbbe «l'owner è abilitato ai *Posti dell'account*»,
cioè una cosa che non è una applicazione e che nessuno può aprire. L'esclusione c'è, e qui si **verifica**
invece di darla per buona:

```bash
docker compose -f dev/docker-compose.yml exec -T postgres psql -U appgrove -d appgrove -c \
  "select a.slug, s.quantity, s.status from platform.subscription s
     join platform.app a on a.id = s.app_id
    where s.tenant_id = 'a0000000-0000-4000-8000-000000000001' order by a.slug;"
curl -sk "https://api.local.appgrove.app/api/platform/v1/users?size=100" \
  -H "authorization: Bearer $TOKEN_OWNER" | grep -c 'platform-seats'
```

→ fra gli abbonamenti dell'account **compare** `platform-seats` (se una guida dei posti è già girata), e
nell'elenco delle persone il conteggio è **`0`**: la voce di piattaforma non è nominata da nessuna riga.
Il numero di applicazioni dell'owner resta quindi quello delle sole applicazioni vere.

---

## 2. La gestione delle persone è del solo owner

```bash
for p in "users?size=100" "invitations?size=100"; do
  printf '%-22s member → %s   owner → %s\n' "$p" \
    "$(curl -sk -o /dev/null -w '%{http_code}' "https://api.local.appgrove.app/api/platform/v1/$p" -H "authorization: Bearer $TOKEN_MEMBER")" \
    "$(curl -sk -o /dev/null -w '%{http_code}' "https://api.local.appgrove.app/api/platform/v1/$p" -H "authorization: Bearer $TOKEN_OWNER")"
done
printf 'users/me               member → %s\n' \
  "$(curl -sk -o /dev/null -w '%{http_code}' https://api.local.appgrove.app/api/platform/v1/users/me -H "authorization: Bearer $TOKEN_MEMBER")"
```

**Risultato atteso**:

```
users?size=100         member → 403   owner → 200
invitations?size=100   member → 403   owner → 200
users/me               member → 200
```

Il **proprio** profilo resta leggibile da chiunque: sono i propri dati. Ciò che si stringe è il governo
delle persone **altrui**.

> Il caso «token che porta ancora `admin`» non si può costruire in locale, perché il seme non ha più
> nessuna appartenenza con quel ruolo (l'enumerazione non lo ammette da UC 0098) e il servizio di
> autenticazione conia il ruolo dall'appartenenza. Lo coprono i collaudi automatici
> `RolesTest.adminCannotCreateInvitation`, `adminCannotListUsers`, `adminCannotListInvitations`.

---

## 3. Il ruolo è uscito dal contratto dell'invito

> ⚠️ **Da sapere prima di lanciare questi comandi (superato dalla change 0098).** Quando questa guida è
> stata scritta, creare un invito era un atto gratuito. **Non lo è più**: con UC 0103 l'invito **passa dalla
> cassa** — se l'account è oltre la franchigia dei tre posti compresi, ogni invito compra un posto, fa
> nascere (o crescere) l'abbonamento di piattaforma dei posti e può rispondere **`402`** invece di `201` se
> l'addebito viene rifiutato. Il **contratto dell'invito** che questo paragrafo verifica non è cambiato di
> una virgola (nessun `role` in ingresso, nessun `role` in uscita): è cambiato ciò che l'invito **lascia
> dietro di sé**, e la «Pulizia» in coda al paragrafo non lo riporta indietro (vedi lì).
>
> Conviene quindi annotare lo stato dei posti prima e dopo, per non attribuire a questa guida un effetto
> che è di un'altra storia:
>
> ```bash
> curl -sk https://api.local.appgrove.app/api/platform/v1/me/seats \
>   -H "authorization: Bearer $TOKEN_OWNER" \
>   | python3 -c 'import sys,json;d=json.load(sys.stdin);print("posti",d["usedSeats"],"quantità pagata",d["paidQuantity"],"dovuto",d["dueCents"])'
> ```

```bash
# 3.1 — invito con SOLO l'indirizzo
curl -sk -X POST https://api.local.appgrove.app/api/platform/v1/invitations \
  -H "authorization: Bearer $TOKEN_OWNER" -H 'content-type: application/json' \
  -d '{"email":"guida-0096-a@acme.test"}' | python3 -m json.tool

# 3.2 — invito che TENTA di far entrare qualcuno come owner
curl -sk -X POST https://api.local.appgrove.app/api/platform/v1/invitations \
  -H "authorization: Bearer $TOKEN_OWNER" -H 'content-type: application/json' \
  -d '{"email":"guida-0096-b@acme.test","role":"owner"}' | python3 -m json.tool

# 3.3 — che cosa è finito in banca dati
docker compose -f dev/docker-compose.yml exec -T postgres psql -U appgrove -d appgrove -c \
  "select email, role from platform.invitations
    where email like 'guida-0096-%' and status = 'pending' order by email;"
```

**Risultato atteso**:

- 3.1 e 3.2 rispondono **entrambe** `201`, e il corpo della risposta **non contiene la chiave `role`**:
  ```
  { "id": "...", "email": "guida-0096-a@acme.test", "status": "pending", "expiresAt": "...", "token": "..." }
  ```
- 3.3 mostra `member` per **entrambi**:
  ```
           email          |  role
  ------------------------+--------
   guida-0096-a@acme.test | member
   guida-0096-b@acme.test | member
  ```

È il punto che conta: il ruolo mandato nel corpo **non concede nulla**. La via d'ingresso nell'account con
i poteri è chiusa per costruzione, non da un messaggio di errore.

L'elenco degli inviti non porta più il ruolo nemmeno in lettura:

```bash
curl -sk "https://api.local.appgrove.app/api/platform/v1/invitations?size=100" \
  -H "authorization: Bearer $TOKEN_OWNER" \
  | python3 -c 'import sys,json;d=json.load(sys.stdin);print("chiavi:",sorted({k for r in d["content"] for k in r}))'
```

→ `chiavi: ['email', 'expiresAt', 'id', 'status']`

**Pulizia** (revoca i due inviti della guida):

```bash
for id in $(docker compose -f dev/docker-compose.yml exec -T postgres psql -U appgrove -d appgrove -tA -c \
    "select id from platform.invitations where email like 'guida-0096-%' and status = 'pending'"); do
  curl -sk -o /dev/null -w "$id → %{http_code}\n" -X DELETE \
    "https://api.local.appgrove.app/api/platform/v1/invitations/$id" \
    -H "authorization: Bearer $TOKEN_OWNER"
done
```

→ `204` per ciascuno.

> **La pulizia NON riporta il seme com'era, e non è un difetto** (change 0098, UC 0103). Revocare libera il
> **posto** — `usedSeats` torna al valore di prima — ma **non annulla l'addebito**: `paidQuantity` e la
> `quantity` dell'abbonamento dei posti restano al valore più alto, perché il periodo è pagato e un posto
> pagato resta riutilizzabile fino alla fine del periodo (è la permanenza minima mensile). Rileggendo il
> riquadro dei posti si vede `next.chargeCents = 0`: il prossimo invito è già coperto. Misurato nella
> passata di fine lotto: `posti 11 → 13 → 11`, `quantità pagata 7 → 10 → 10`.
>
> Per riportare **davvero** l'ambiente allo stato di partenza serve la pulizia del §8 della guida della
> change 0098 (che cancella la riga di abbonamento dei posti), oppure `./app-stop.sh --wipe`.

---

## 4. La schermata (VISIVO — resta allo sviluppatore)

Entra su <https://app.local.appgrove.app> come **owner@acme.test** / `Password1!` e apri **Members**.

> **L'interfaccia parte in inglese**, non in italiano: le persone del seme hanno `locale = 'en'`
> (`dev/seed/seed.sql`), e la lingua della sessione è la loro. Le etichette qui sotto sono quindi quelle
> che si leggono **davvero** a schermo. Per vederle in italiano si cambia lingua dal menu del profilo — ed è
> proprio il §6.2.

> **Superato in parte dalle change 0098 e 0099: la schermata ora contiene anche i posti.** Questa guida
> descrive la pagina «Members» come era il 2026-08-22 mattina; nel pomeriggio UC 0103 le ha aggiunto in
> testa il **riquadro dei posti** e la **stima** sotto il modulo di invito, e UC 0104 le ha aggiunto la
> **colonna di selezione** per la cessazione programmata e lo stato «Ending …». I punti qui sotto sono stati
> riscritti sulla verità finale, e dove il testo diceva «esattamente sei» o «due righe» il numero è stato
> aggiornato: **il conteggio precedente era vero, era intermedio**. Quel che questa storia consegna — una
> tabella sola, nessuna colonna del ruolo, nessun selettore di ruolo nell'invito — è intatto: sono aggiunte
> accanto, non ritorni indietro. Le cose dei posti **non** si verificano qui: sono i §9 delle guide 0098 e
> il §10 della guida 0099.

| # | Azione | Risultato atteso |
|---|---|---|
| 4.1 | Guarda la pagina | **Una sola tabella**, dentro un riquadro intitolato **«People»**. Prima erano due riquadri, «Members» e «Pending invitations»: il secondo non c'è più. Sotto il titolo della pagina, la riga che spiega il modello: «The people in your workspace. Permissions are granted inside each app.» Fra quella riga e il modulo di invito c'è ora il **riquadro dei posti** («Seats», con «… seats in use»): è della change 0098 e si collauda là. |
| 4.2 | Leggi le intestazioni della tabella | Sei intestazioni **con un'etichetta**, in quest'ordine: **Email · Name · Status · Apps · Joined · Actions** — precedute da una **settima colonna strettissima e senza intestazione visibile** (la casella di selezione per la cessazione programmata, change 0099: chi naviga con un lettore di schermo la sente come «Schedule termination»). **Nessuna colonna «Role»**, e da nessuna parte le etichette «Owner»/«Member» come ruolo di una persona: è questo il punto della storia. |
| 4.3 | Trova la prima riga | È **owner@acme.test**: l'owner è in testa, sempre. |
| 4.4 | Guarda le righe degli inviti (`invitee-admin@acme.test`, `invitee-member@acme.test`) | Sono **nella stessa tabella**, con lo stato «Invitation pending» e, sotto, «expires …». Le colonne Apps e Joined mostrano `—`: non sono ancora entrate, e non c'è nulla da contare né da datare. |
| 4.5 | Nella colonna **Apps** della riga **member@acme.test**, premi il comando delle applicazioni («2 apps», o il numero che quella riga porta) | Si apre una riga di dettaglio **sotto** la persona: ogni applicazione col suo ruolo (**Editor**, **Viewer**, **Admin**) e la frase «Roles on an app are changed from that app's user management.» Nessun comando per cambiarlo: è **sola lettura**, ed è voluto (la schermata dove si cambia è UC 0111 e non esiste ancora). |
| 4.6 | Premi il comando delle applicazioni sulla riga di **owner@acme.test** | Le applicazioni sono etichettate **«Full access (owner)»**, non con un ruolo: l'owner non ha un ruolo *su* una applicazione, ce l'ha sull'account. Sono le applicazioni **vere** a cui l'account ha diritto (`fatture`, `notes`, `teams`), non `crm` (vedi §1 bis) e **non** «Posti dell'account», che è una voce di piattaforma e non una applicazione. |
| 4.7 | Premi «No apps» su una persona che non è abilitata a nulla | «Not enabled on any app yet — you enable it from that app's user management.» Non è un errore: è uno stato normale. |
| 4.8 | Guarda il riquadro dell'invito | Un solo campo, **Email**, e il pulsante «Send invitation». **Nessun selettore di ruolo**, e sotto **tre** righe: prima la **stima del posto** («This person will be seat number …», della change 0098 — in grassetto, è la prima che si legge), poi «There is no role to choose: …» e infine la nota sul posto che è dell'account. La seconda serve proprio a questo momento: senza, l'assenza del selettore si legge come un difetto. |
| 4.9 | Invita `vista-0096@acme.test` | Messaggio verde «Invitation sent to vista-0096@acme.test.», il collegamento con il pulsante «Copy link», e la persona **compare nello stesso elenco** con lo stato «Invitation pending». (L'email arriva su Mailpit, <http://localhost:8025>.) **Attenzione, dalla change 0098 questo invito compra un posto** e i numeri del riquadro dei posti cambiano subito; se una riduzione è programmata il pulsante è **spento** e il puntatore spiega perché. |
| 4.10 | Sulla riga dell'invito appena creato, premi **Revoke** e conferma nella finestra | La riga scompare dall'elenco. Il **posto** si libera; l'addebito **no** (vedi la nota in coda al §3). |
| 4.11 | Sulla riga di **owner@acme.test**, guarda **Remove** e **Suspend** | Entrambi **disabilitati**: è l'ultimo owner. Passando il puntatore sulla cella si legge il perché («You can't change or remove the last owner.»). |
| 4.12 | Sulla riga di **member@acme.test**, premi **Suspend** e conferma | Lo stato passa a «Suspended» (pastiglia gialla). Premi **Reactivate** per rimettere la persona come prima. |
| 4.13 | Naviga con il solo **tabulatore** | Si raggiungono il campo dell'invito, il pulsante di invio, e per ogni riga la **casella di selezione** (change 0099, si annuncia «Select \<indirizzo\>»), il comando delle applicazioni e i comandi di azione. Il comando delle applicazioni è un pulsante e si annuncia «Apps for \<indirizzo\>». |

---

## 5. La sezione è del solo owner (VISIVO — resta allo sviluppatore)

| # | Azione | Risultato atteso |
|---|---|---|
| 5.1 | Esci, entra come **member@acme.test** / `Password1!` e vai a mano su <https://app.local.appgrove.app/members> | Si viene rimandati a **/forbidden**. La schermata non si apre nemmeno vuota. |
| 5.2 | Da quella sessione, apri la pagina iniziale (il cruscotto) | Nel riquadro «At a glance» **non** compaiono le righe «Members» e «Pending invites»: a chi non le può leggere non si mostra una riga rotta. Le altre righe (Apps, Renewal) ci sono. |
| 5.3 | Rientra come **owner@acme.test** e guarda lo stesso riquadro | Le due righe ci sono, con i loro numeri. |

> La voce di menu «Members» resta **visibile** anche a chi non può aprirla: nasconderla è UC 0107, e questa
> change non ridisegna il menu. Vederla e ricevere il rifiuto è quindi il comportamento atteso oggi, non un
> difetto.

---

## 6. Le cinque lingue

**6.1 — le chiavi (non visivo, già eseguito).**

```bash
for l in en it fr es de; do printf '%-3s nuove=%s  vecchie=%s\n' "$l" \
 "$(grep -cE '^    (rosterHeading|colApps|colJoined|statusInvited|noRoleHint|appsManagedInApp):' frontend/packages/i18n/src/resources/$l.ts)" \
 "$(grep -cE '^    (colRole|roleOwner|roleMember|invitesHeading|noInvites):' frontend/packages/i18n/src/resources/$l.ts)"; done
```

**Risultato atteso**: `nuove=6  vecchie=0` per tutte e cinque. Le chiavi del ruolo e della seconda tabella
sono state **rimosse**, non lasciate orfane: il collaudo di parità pretende lo stesso insieme di chiavi in
tutte le lingue, ma non segnala una chiave che nessuno usa — quindi una chiave morta non diventa mai rossa
e resterebbe per sempre.

**6.2 — a schermo (VISIVO).** Dalla schermata «Members», cambia lingua dal menu del profilo e passa per
tutte e cinque. **Nessuna etichetta in inglese** deve restare nelle altre quattro. I punti dove
guardare: le sei intestazioni con etichetta, i **quattro** stati — «Active», «Suspended», «Invitation
pending» e «Ending …», il quarto aggiunto dalla change 0099 — il conteggio delle applicazioni con il
**singolare e il plurale** (in italiano «1 applicazione» / «2 applicazioni»), l'etichetta dell'accesso
implicito dell'owner e le due righe di spiegazione sotto il modulo di invito (la terza, la stima del posto,
è della change 0098 e si guarda là).

---

## 7. Chiusura

```bash
./app-stop.sh
```

Se hai eseguito il §4.9 senza revocare, revoca l'invito `vista-0096@acme.test` dalla schermata. **Non basta
più a riportare il seme com'era**: dalla change 0098 l'invito ha comprato un posto e la revoca non lo
rimborsa (vedi la nota in coda al §3). Per tornare davvero allo stato iniziale: la pulizia del §8 della
guida della change 0098, oppure `./app-stop.sh --wipe`.
