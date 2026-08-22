# Come verificare a mano la change 0098 — l'invito che passa dalla cassa

> Guida scritta sul branch `change/0098-use-case-0103-acquisto-anticipato-posto`, base `203900b`,
> il 2026-08-22. È una **fotografia**: se una change successiva cambia questi comportamenti, i punti
> superati si scoprono **rieseguendola**, non rileggendola.
>
> **Passi non visivi ESEGUITI** dall'agente il 2026-08-22 contro lo stack locale — §1, §2, §3, §4, §5, §6,
> §7, §8 — con gli esiti riportati qui **come si leggono davvero**. Eseguirli ha prodotto:
>
> - **nessun difetto di prodotto**: ogni passo ha dato l'esito atteso al primo tentativo;
> - **un punto aperto scoperto**, e tracciato: l'account di prova ha **nove** posti occupati, sei a
>   pagamento, e una quantità pagata di **zero** — perché quelle persone sono entrate quando il posto era
>   gratuito. Il riquadro dice quindi, correttamente, che il prossimo posto costa **20,93 €**: è la
>   differenza fra il dovuto dei posti bersaglio e quello dei posti *già pagati*, che sono nessuno. Il
>   calcolo è giusto; manca la **decisione di migrazione** per gli account preesistenti, che è commerciale
>   oltre che tecnica ed è scritta nei punti aperti di UC 0113;
> - **una scelta di forma**: il §5 è scritto per verificare la **regola** («la quantità sale di uno, il
>   riferimento all'addebito compare») e non numeri assoluti, perché il numero di posti dell'account di
>   prova cambia con la suite di piattaforma. È la lezione già imparata dalla guida della change 0097.
>
> Restano allo sviluppatore i soli passi **visivi** del §9: le frasi a schermo. Sono comunque coperti dai
> collaudi Playwright `[J-SEATS]` di `frontend/apps/backoffice/e2e/seats.spec.ts` e dai collaudi di componente
> di `MembersPage.test.tsx` — qui si guardano con gli occhi, che è un'altra cosa.
>
> ---
>
> **Passata di fine lotto — 2026-08-22, stato finale di `main` dopo la change 0099.** La prosa di una change
> chiusa è un file d'archivio: nessuna change successiva la può far diventare rossa. Perciò i passi non visivi
> sono stati **rieseguiti** contro lo stack locale già mosso dalle guide di 0095–0097 e 0099. Esito:
>
> - **nessun difetto di prodotto**: ogni regola verificata da questa guida vale ancora, parola per parola;
> - **otto correzioni alla guida** — due per **invecchiamento** (la storia 0104 ha portato la banca dati a
>   `V23` e ha aggiunto un terzo collaudo `[J-SEATS]`) e sei perché la guida era **sbagliata dall'inizio**:
>   numeri assoluti scritti come se il conto di prova partisse pulito (§2, §4), due passi dichiarati «non
>   eseguibili senza registrare un conto a mano» che il seme rende invece eseguibili (§3), interrogazioni alla
>   banca dati **senza il filtro di conto** (§3.2, §4.3, §5.4, §7.3, §8), comandi che spengono anche le
>   interfacce web (§7, §9.5) e una pulizia finale che cancellava **più di quanto la guida avesse creato** (§8);
> - **un passo non rieseguito**: il §7, che richiede di fermare e riavviare le applicazioni — fuori dai limiti
>   della passata, che deve lasciare lo stack acceso. Dettaglio e copertura sostitutiva nel §7 stesso.
>
> Lo **stato di partenza** della riesecuzione, letto e dichiarato prima di toccare qualsiasi cosa (è la lezione
> della guida 0097): `usedSeats 11` · `paidSeats 8` · `dueCents 2292` · `paidQuantity 10` · `hasSubscription
> true` · `pendingReduction false`. Alla fine lo stack è stato **rimesso esattamente lì**.

Le sette cose da vedere:

1. la **voce di catalogo dei posti** esiste, è marcata come voce di piattaforma e non è una applicazione;
2. il **riquadro dei posti** risponde con posti usati, composizione, dovuto e costo del prossimo — ed è
   **dell'owner**;
3. l'**invito paga il posto**: l'abbonamento di piattaforma nasce, la quantità sale, l'invito porta il
   riferimento del suo addebito;
4. un posto **liberato** non si rimborsa e non si ripaga nello stesso periodo;
5. senza addebito riuscito **l'invito non nasce**, e il motivo del fornitore arriva a chi ha invitato;
6. la voce di piattaforma è **invisibile in tutte le superfici del cliente** e **visibile-ma-marcata** in
   console;
7. le frasi a schermo dicono le cose giuste, compreso il caso in cui il posto successivo **costa meno**.

Ogni voce è **azione → risultato atteso**.

---

## 0. Preparazione

| # | Azione | Risultato atteso |
|---|---|---|
| 0.1 | `./app-start.sh` | Tutto verde: Postgres, proxy, Mailpit, MinIO, ElasticMQ, i servizi backend scoperti (`auth`, `core`, `crm`, `fatture`) e le due interfacce. |
| 0.2 | `./dev.sh migrate` | Le migrazioni girano senza errori (`app-start.sh` le applica già; questo passo serve solo se si parte da una banca dati vecchia). Alla scrittura di questa guida l'ultima era `V22`; **dalla change 0099 l'ultima è `V23 - seat downgrade`**, e su una banca dati già a `V22` questo passo è necessario. Il numero non va inseguito: conta che il comando finisca con «up to date». |

**I tre token che servono** — comandi completi e incollabili:

```bash
TOKEN_OWNER=$(curl -sk https://api.local.appgrove.app/api/auth/login \
  -H 'content-type: application/json' \
  -d '{"email":"owner@acme.test","password":"Password1!"}' \
  | python3 -c 'import sys,json;print(json.load(sys.stdin)["access_token"])')

TOKEN_MEMBER=$(curl -sk https://api.local.appgrove.app/api/auth/login \
  -H 'content-type: application/json' \
  -d '{"email":"member@acme.test","password":"Password1!"}' \
  | python3 -c 'import sys,json;print(json.load(sys.stdin)["access_token"])')

TOKEN_ADMIN=$(curl -sk https://api.local.appgrove.app/api/auth/login \
  -H 'content-type: application/json' \
  -d '{"email":"admin@appgrove.test","password":"Password1!"}' \
  | python3 -c 'import sys,json;print(json.load(sys.stdin)["access_token"])')

echo "${#TOKEN_OWNER} ${#TOKEN_MEMBER} ${#TOKEN_ADMIN}"   # tre numeri > 0 = pronto
```

L'identificativo della voce dei posti è **deterministico** e serve in quasi tutti i passi:

```bash
SEATS_APP=22c25c07-0247-3196-8d05-a2d26587295a
ACME=a0000000-0000-4000-8000-000000000001
```

---

## 1. La voce di catalogo dei posti esiste, ed è marcata

| # | Azione | Risultato atteso |
|---|---|---|
| 1.1 | `docker compose -f dev/docker-compose.yml exec -T postgres psql -U appgrove -d appgrove -c "select id, slug, name, kind, status from platform.app order by kind desc, slug;"` | Sei righe. **Una sola** con `kind = platform`: `platform-seats` · `Posti dell'account` · `active`, con identificativo `22c25c07-0247-3196-8d05-a2d26587295a`. Tutte le altre `kind = application`. |
| 1.2 | `docker compose -f dev/docker-compose.yml exec -T postgres psql -U appgrove -d appgrove -c "select column_name, column_default, is_nullable from information_schema.columns where table_schema='platform' and ((table_name='subscription' and column_name='quantity') or (table_name='invitations' and column_name='seat_charge_ref') or (table_name='app' and column_name='kind'));"` | Tre righe: `quantity` (predefinito `1`, non nullo), `seat_charge_ref` (nullo ammesso, nessun predefinito), `kind` (predefinito `'application'`, non nullo). |

**Esito reale**: entrambi come atteso. L'identificativo scritto nella migrazione coincide con quello
derivato dallo slug — il collaudo `SeatPurchaseApiTest.laVoceDeiPostiHaLIdentificativoDerivatoDalSuoSlug`
sorveglia le due copie, e questo passo lo conferma sull'ambiente vero.

---

## 2. Il riquadro dei posti risponde, ed è dell'owner

| # | Azione | Risultato atteso |
|---|---|---|
| 2.1 | `curl -sk https://api.local.appgrove.app/api/platform/v1/me/seats -H "Authorization: Bearer $TOKEN_OWNER"` | `200` con: `usedSeats`, `composition` (attive + sospese + inviti in attesa, la cui **somma è `usedSeats`**), `currency: "EUR"`, `freeSeats: 3`, `paidSeats`, `dueCents`, `paidQuantity`, `currentBand`, `next` (numero del posto, tariffa, nuovo totale, addebito, «costa meno del precedente»), `pendingReduction: false`, `hasSubscription`. |
| 2.2 | `curl -sk -o /dev/null -w "%{http_code}\n" https://api.local.appgrove.app/api/platform/v1/me/seats -H "Authorization: Bearer $TOKEN_MEMBER"` | `403`. Il riquadro è dell'owner: dice quanto paga l'account, non quanto costa un posto. |
| 2.3 | `curl -sk -o /dev/null -w "%{http_code}\n" https://api.local.appgrove.app/api/platform/v1/me/seats` | `401`. |
| 2.4 | `curl -sk -o /dev/null -w "%{http_code}\n" https://api.local.appgrove.app/api/platform/v1/seat-pricing -H "Authorization: Bearer $TOKEN_MEMBER"` | `200`. **La differenza è il punto**: il listino è pubblico dentro il prodotto (tariffe uguali per tutti), il riquadro no. |

**Le cose da verificare qui sono regole, non numeri** — il conto di prova cambia a ogni giro della suite di
piattaforma e a ogni guida eseguita prima di questa (è la lezione della guida 0097). Quelle sì valgono sempre:

1. `active + suspended + pendingInvitations = usedSeats`;
2. `freeSeats = 3` (è la prima riga del listino, §3.3);
3. `paidSeats = max(0, usedSeats − freeSeats)`;
4. `dueCents` = somma degli scaglioni sui `usedSeats` occupati;
5. `403` col token del collaboratore, `401` senza token, `200` sul listino col token del collaboratore.

**Esito della prima esecuzione** (2026-08-22, banca dati appena seminata più le tracce della suite di
piattaforma): `usedSeats 9` = `6 + 0 + 3`, `dueCents 1794 = 6 × 299`, `paidQuantity 0`,
`hasSubscription false`; `403`, `401`, `200` come atteso.

**Esito della riesecuzione di fine lotto** (stesso giorno, stack già mosso dalle altre guide):

```json
{"usedSeats":11,"composition":{"active":5,"suspended":0,"pendingInvitations":6},"currency":"EUR",
 "freeSeats":3,"paidSeats":8,"dueCents":2292,"paidQuantity":10,
 "currentBand":{"fromSeat":11,"toSeat":50,"unitPriceCents":199},
 "next":{"seatNumber":12,"unitPriceCents":199,"dueCentsAfter":2491,"chargeCents":0,
         "cheaperThanPrevious":false},
 "pendingReduction":false,"hasSubscription":true}
```

`5 + 0 + 6 = 11` ✓ · `11 − 3 = 8` ✓ · `dueCents 2292 = 7 × 299 + 1 × 199` ✓ (sette posti nello scaglione
4–10, uno nel successivo) · `403` / `401` / `200` ✓. Le cinque regole tengono su entrambi gli stati: è
questo che la guida deve provare.

> **`paidQuantity` diverso da `paidSeats` non è un difetto.** Alla prima esecuzione era `0` con sei posti a
> pagamento: quelle persone sono entrate quando il posto era gratuito, quindi nessuno le ha mai pagate — è il
> punto aperto tracciato in UC 0113 (decisione di migrazione per i conti preesistenti, commerciale oltre che
> tecnica). Alla riesecuzione è `10` con otto posti a pagamento: due posti sono **già pagati e liberi**, e
> per questo `next.chargeCents` vale `0` (§5: un posto pagato non si ripaga nello stesso periodo). In
> entrambi i casi `chargeCents` è la differenza fra il dovuto dei posti bersaglio e quello dei posti già
> pagati, e in entrambi i casi il calcolo è giusto.
>
> **Attenzione al leggere questi numeri in locale**: `dev/seed/seed.sql` riporta a «in attesa» gli inviti del
> seme già accettati mentre l'appartenenza resta, quindi i posti occupati si leggono **più alti del vero** di
> uno per ogni invito del seme già accettato (difetto della semina, tracciato in `docs/_BACKLOG.md`). Non è
> il conteggio dei posti a sbagliare.

> Il §2.1 va letto **senza riduzioni in attesa**: dalla change 0099 il riquadro porta anche
> `pendingReduction: true` e un oggetto `reduction`. Se `pendingReduction` è `true`, annulla la riduzione
> (`DELETE .../me/seats/reduction`) prima di proseguire: con una riduzione in attesa i §4, §5 e §7 non
> possono nemmeno partire, perché gli inviti sono rifiutati (guida 0099, §6).

---

## 3. La franchigia non costa nulla, e non crea nulla

Serve un account **dentro la franchigia**, perché quello di prova è già oltre. Non serve registrarne uno a
mano: **il seme ne porta già uno**, «Bob Personal» (`a0000000-0000-4000-8000-000000000002`), di cui
`bob@bob.test` è owner ed è l'unica persona. Il suo token:

```bash
TOKEN_BOB=$(curl -sk https://api.local.appgrove.app/api/auth/login \
  -H 'content-type: application/json' \
  -d '{"email":"bob@bob.test","password":"Password1!"}' \
  | python3 -c 'import sys,json;print(json.load(sys.stdin)["access_token"])')

BOB=a0000000-0000-4000-8000-000000000002
```

| # | Azione | Risultato atteso |
|---|---|---|
| 3.1 | `curl -sk https://api.local.appgrove.app/api/platform/v1/me/seats -H "Authorization: Bearer $TOKEN_BOB"` | `usedSeats: 1` (solo l'owner), `dueCents: 0`, `paidSeats: 0`, `hasSubscription: false`, e `next.unitPriceCents: 0` — **il secondo e il terzo posto sono compresi**. Da eseguire **prima** del 3.2: dopo due inviti i posti occupati sono tre e il prossimo costa. |
| 3.2 | Invitare due persone da quell'account (`for n in 1 2; do curl -sk -o /dev/null -w "invito $n: %{http_code}\n" -X POST https://api.local.appgrove.app/api/platform/v1/invitations -H "Authorization: Bearer $TOKEN_BOB" -H 'content-type: application/json' -d "{\"email\":\"franchigia-0098-$n@bob.test\"}"; done`), poi `docker compose -f dev/docker-compose.yml exec -T postgres psql -U appgrove -d appgrove -c "select count(*) from platform.subscription where app_id = '$SEATS_APP' and tenant_id = '$BOB';"` | Due `201`, poi `0`. **Dentro la franchigia l'abbonamento dei posti non esiste affatto** — non esiste «con quantità zero»: la differenza conta, perché un abbonamento a zero sarebbe una riga di fatturazione senza fatturazione. |
| 3.3 | `docker compose -f dev/docker-compose.yml exec -T postgres psql -U appgrove -d appgrove -c "select from_seat, to_seat, unit_price_cents from platform.seat_pricing_band b join platform.seat_pricing_version v on v.id = b.version_id order by from_seat;"` | Cinque fasce, la prima `1 → 3` a **tariffa 0**. La franchigia è una **riga di listino**, non una condizione nel programma. Il listino è di piattaforma: non dipende dal conto. |

> **Il filtro `tenant_id` nel 3.2 non è un vezzo.** La banca dati locale è una sola per tutti i conti: senza
> filtro la stessa interrogazione conta anche l'abbonamento dei posti di *Acme* e risponde `1` invece di `0`,
> cioè fa fallire un passo corretto. È l'errore che la prima stesura di questa guida aveva, ed è lo stesso
> nei §4.3, §5.4, §7.3 e §8: **ogni interrogazione su `platform.subscription` va qualificata col conto.**

Non dimenticare di **cancellare i due inviti di prova** quando hai finito col §3:

```bash
docker compose -f dev/docker-compose.yml exec -T postgres psql -U appgrove -d appgrove -c \
  "delete from platform.invitations where email like 'franchigia-0098%';"
```

**Esito reale**: il 3.3 conferma le cinque fasce `1–3 = 0`, `4–10 = 299`, `11–50 = 199`, `51–100 = 99`,
`101–∞ = 49`. Nella prima esecuzione i passi 3.1/3.2 erano stati **dichiarati non eseguiti** perché la guida
pretendeva un conto registrato a mano dall'interfaccia; la riesecuzione di fine lotto li ha **eseguiti** su
«Bob Personal»: `usedSeats 1`, `dueCents 0`, `paidSeats 0`, `hasSubscription false`, `next.unitPriceCents 0`;
poi due `201`, `usedSeats 3` con `dueCents 0` e ancora **nessun abbonamento** per quel conto. La stessa regola
è provata dal collaudo `SeatPurchaseApiTest.dentroLaFranchigiaNonNasceAlcunAbbonamento`.

---

## 4. L'invito paga il posto

**Prerequisito, da dichiarare prima di partire** — la prima stesura di questa guida lo dava per scontato, e
per questo la riesecuzione di fine lotto l'ha vista fallire. L'invito paga il posto **solo se quel posto non
è già stato pagato in questo periodo**. Il riquadro lo dice in una parola: `next.chargeCents`.

- `next.chargeCents > 0` → il prossimo invito **addebita**: si può eseguire il §4 così com'è;
- `next.chargeCents = 0` → ci sono posti **pagati e liberi** (`paidQuantity > paidSeats`): il prossimo invito
  è gratuito e non alza `quantity`. Non è un difetto, è il §5. Per arrivare al caso che il §4 vuole
  provare, si invita **finché `next.chargeCents` diventa maggiore di zero** — cioè finché lo scarto pagato
  in anticipo è consumato — e solo allora si guarda l'invito che addebita.

| # | Azione | Risultato atteso |
|---|---|---|
| 4.1 | Leggere e **annotare** lo stato di partenza: `curl -sk .../me/seats -H "Authorization: Bearer $TOKEN_OWNER"` | `usedSeats`, `paidSeats`, `paidQuantity`, `dueCents`, `next.chargeCents`, e `quantity` dell'abbonamento (§4.3). **Si verificano la regola e i delta, mai un numero assoluto**: il conto di prova cambia con le altre suite e con le guide eseguite prima di questa. |
| 4.2 | `curl -sk -w "\nHTTP %{http_code}\n" -X POST https://api.local.appgrove.app/api/platform/v1/invitations -H "Authorization: Bearer $TOKEN_OWNER" -H 'content-type: application/json' -d '{"email":"collaudo-0098-a@acme.test"}'` (ripetere con `-b`, `-c`, … finché `next.chargeCents` era `0`) | `201` con `id`, `email`, `status: "pending"`, `expiresAt` e il **token grezzo**. Nessun campo che riguardi l'addebito: chi invita non ha bisogno del riferimento della transazione. |
| 4.3 | `docker compose -f dev/docker-compose.yml exec -T postgres psql -U appgrove -d appgrove -c "select quantity, status, current_period_start is not null as ha_periodo, paddle_subscription_id is not null as ha_riferimento from platform.subscription where app_id = '$SEATS_APP' and tenant_id = '$ACME';"` | **Una** riga: `status = active`, periodo valorizzato, riferimento del fornitore valorizzato. `quantity` **sale di uno solo per l'invito che ha addebitato**; per gli inviti gratuiti resta ferma. |
| 4.4 | `docker compose -f dev/docker-compose.yml exec -T postgres psql -U appgrove -d appgrove -c "select email, status, seat_charge_ref is not null as ha_addebito from platform.invitations where email like 'collaudo-0098%' order by email;"` | `status = pending` per tutti; `ha_addebito = t` **solo** per l'invito che ha addebitato. **Il posto di quell'invito è pagato**, e la riga dice con che cosa. |
| 4.5 | `curl -sk .../me/seats -H "Authorization: Bearer $TOKEN_OWNER"` | `usedSeats` salito di uno per ogni invito (**il posto è occupato dall'invito, non dall'accettazione**), `paidQuantity` pari a `paidSeats` dopo l'invito che addebita, `hasSubscription: true`. |
| 4.6 | `grep -a "seats.charge" dev/.run/core.log \| tail -3` | Una riga `seats.charge.accepted` con `target`, `quantity` (nella forma `vecchia→nuova`), `delta_cents`, `due_cents` e il riferimento della transazione — e, per gli inviti gratuiti, una riga `seats.charge.skipped` con `used`, `target`, `free`, `paid_quantity` e la ragione «nessun denaro dovuto». |

**Esito della prima esecuzione** (2026-08-22, `paidQuantity = 0`, nessun abbonamento): `201`; l'abbonamento è
nato con `quantity = 7`, periodo e riferimento valorizzati; l'invito porta il suo addebito; il riquadro è
passato da 9 a 10 posti con `dueCents: 2093` e `paidQuantity: 7`. Qui si è visto **il caso del confine di
fascia**: a dieci posti occupati il prossimo costa `199` invece di `299`, con `cheaperThanPrevious: true` e
`dueCentsAfter: 2292` — il costo del posto in più scende, il totale sale comunque.

**Esito della riesecuzione di fine lotto** (partenza `usedSeats 11`, `paidSeats 8`, `paidQuantity 10`,
`next.chargeCents 0`, `quantity 10`), tre inviti in fila:

| invito | `usedSeats` | `paidSeats` | `paidQuantity` | `quantity` | `next.chargeCents` | `seat_charge_ref` |
|---|---|---|---|---|---|---|
| — (partenza) | 11 | 8 | 10 | 10 | 0 | — |
| `-a` | 12 | 9 | 10 | 10 | 0 | assente |
| `-b` | 13 | 10 | 10 | 10 | **199** | assente |
| `-c` | 14 | 11 | 11 | **11** | 199 | **presente** |

I due primi inviti consumano i posti pagati in anticipo e non costano nulla; il terzo supera `paidQuantity` e
**paga**: `quantity 10→11`, `delta_cents=199`, riferimento della transazione sulla riga dell'invito. Il registro
lo dice in chiaro:

```
INFO  seats.charge.skipped  used=11 target=12 free=3 paid_quantity=10 — nessun denaro dovuto
INFO  seats.charge.skipped  used=12 target=13 free=3 paid_quantity=10 — nessun denaro dovuto
INFO  seats.charge.accepted target=14 quantity=10→11 delta_cents=199 due_cents=2889 txn=txn_1bf9e8c88c68…
```

È **la stessa regola** della prima esecuzione, vista dall'altro capo: là il conto non aveva pagato nulla e il
primo invito pagava tutto lo scarto; qui aveva pagato in anticipo e l'invito paga solo quando lo scarto
finisce. Una guida che avesse preteso «`quantity` sale sempre di uno» avrebbe accusato il prodotto di un
difetto che non c'è.

---

## 5. Un posto liberato non si rimborsa e non si ripaga

| # | Azione | Risultato atteso |
|---|---|---|
| 5.1 | `INV=$(docker compose -f dev/docker-compose.yml exec -T postgres psql -U appgrove -d appgrove -tAc "select id from platform.invitations where email='collaudo-0098-c@acme.test'" \| tr -d '[:space:]')` · `curl -sk -o /dev/null -w "%{http_code}\n" -X DELETE "https://api.local.appgrove.app/api/platform/v1/invitations/$INV" -H "Authorization: Bearer $TOKEN_OWNER"` | `204`. Si revoca **l'invito che ha addebitato** (§4.4): è il suo posto quello di cui si vuole provare che non si rimborsa. |
| 5.2 | `curl -sk .../me/seats -H "Authorization: Bearer $TOKEN_OWNER"` | `usedSeats` scende di uno (**il posto si è liberato**) ma `paidQuantity` **non scende**: il periodo è pagato. E `next.chargeCents` vale **`0`** — il prossimo posto è già coperto. |
| 5.3 | `curl -sk -o /dev/null -w "%{http_code}\n" -X POST .../invitations -H "Authorization: Bearer $TOKEN_OWNER" -H 'content-type: application/json' -d '{"email":"collaudo-0098-bis@acme.test"}'` | `201`. |
| 5.4 | `docker compose -f dev/docker-compose.yml exec -T postgres psql -U appgrove -d appgrove -c "select email, seat_charge_ref is null as senza_addebito from platform.invitations where email like 'collaudo-0098%' order by email;"` · `docker compose -f dev/docker-compose.yml exec -T postgres psql -U appgrove -d appgrove -tAc "select quantity from platform.subscription where app_id = '$SEATS_APP' and tenant_id = '$ACME';"` | L'invito revocato ha l'addebito, il **reinvito no** (`senza_addebito = t`): non si paga due volte lo stesso posto nello stesso periodo. E `quantity` sull'abbonamento è **rimasta uguale**. |

**Esito della prima esecuzione**: `204`, poi il riquadro con `usedSeats: 9`, `paidQuantity: 7` e
`next.chargeCents: 0`; il reinvito `201`; `quantity` ancora `7`; il secondo invito senza riferimento
all'addebito.

**Esito della riesecuzione di fine lotto** (dal §4: `usedSeats 14`, `paidSeats 11`, `paidQuantity 11`,
`quantity 11`): `204`; riquadro `usedSeats 13`, `paidSeats 10`, `paidQuantity 11` — **non scesa** — e
`next.chargeCents 0`; reinvito `201`; `quantity` ancora `11`; il reinvito **senza** riferimento all'addebito,
mentre quello revocato lo conserva. Esattamente la lettura della permanenza minima mensile: nessun rimborso,
posto riutilizzabile. La regola tiene su due stati di partenza diversi, che è il punto.

---

## 6. Le sei esclusioni della voce di piattaforma

Da eseguire **con l'abbonamento dei posti presente** (dopo il §4): senza di esso ogni esclusione passerebbe
per un'altra ragione, e la prova non proverebbe nulla.

| # | Azione | Risultato atteso |
|---|---|---|
| 6.1 | `for p in me/entitlements me/catalog me/app-access me/subscriptions; do echo -n "$p → "; curl -sk "https://api.local.appgrove.app/api/platform/v1/$p" -H "Authorization: Bearer $TOKEN_OWNER" \| grep -c "platform-seats"; done` | Quattro righe, tutte `0`. Diritti, vetrina, «dove posso entrare» e abbonamenti self-service **non** nominano la voce. |
| 6.2 | `curl -sk 'https://api.local.appgrove.app/api/platform/v1/users?size=100' -H "Authorization: Bearer $TOKEN_OWNER" \| grep -c "platform-seats"` | `0`. L'owner ha accesso implicito a tutto ciò a cui l'account ha diritto: senza l'esclusione la schermata dei membri direbbe «l'owner è abilitato ai Posti dell'account». |
| 6.3 | `curl -sk https://api.local.appgrove.app/api/platform/v1/admin/apps -H "Authorization: Bearer $TOKEN_ADMIN"` | La voce **c'è**, con `"kind":"platform"`; tutte le altre `"kind":"application"`. È l'unica superficie in cui si vede, e si vede per quello che è. |
| 6.4 | `curl -sk "https://api.local.appgrove.app/api/platform/v1/admin/accounts/$ACME" -H "Authorization: Bearer $TOKEN_ADMIN" \| grep -c '"appSlug":"platform-seats"'` | `0`. La matrice dei diritti risponde a «che cosa vede questo cliente», e i posti non sono qualcosa che si vede. |
| 6.5 | Barra laterale del backoffice (**visivo**) | Nessuna voce «Posti dell'account» fra le applicazioni. Il menu deriva da `me/app-access`, quindi 6.1 lo anticipa — ma va guardato. |

**Esito reale**: `0` su tutte le superfici del cliente (6.1 e 6.2), sia **prima** che **dopo** aver creato
l'abbonamento dei posti; in console la voce compare marcata `platform` e la matrice di Acme elenca solo
`fatture`, `legacy`, `notes`, `teams`. Il 6.5 resta visivo. **Confermato identico alla riesecuzione di fine
lotto**, con l'abbonamento dei posti presente e con `quantity` diversa da quella di allora: l'esclusione non
dipende dallo stato dell'abbonamento, ed è questo che deve valere.

---

## 7. Senza addebito riuscito l'invito non nasce

Serve il simulatore configurato per **rifiutare**. Si riavviano le applicazioni con la proprietà valorizzata
(i dati restano dove sono: lo stack Compose non si tocca):

```bash
./app-stop.sh --apps-only
APPGROVE_SEATS_STUB_DECLINE_REASON="carta scaduta" ./app-start.sh --no-build
```

> **Niente `--no-spa`, e non è un dettaglio.** `./app-stop.sh --apps-only` ferma **anche** le interfacce web
> (backoffice `:5173`, console `:5174`, sito `:4321`): riavviare con `--no-spa` le lascerebbe spente, e il §9
> — che si guarda a schermo — diventerebbe impossibile. La prima stesura di questa guida aveva `--no-spa` sia
> qui che al §7.5, e chi l'avesse seguita alla lettera si sarebbe ritrovato senza interfacce senza capire
> perché.

| # | Azione | Risultato atteso |
|---|---|---|
| 7.1 | `curl -sk -w "\nHTTP %{http_code}\n" -X POST .../invitations -H "Authorization: Bearer $TOKEN_OWNER" -H 'content-type: application/json' -d '{"email":"rifiutato-0098@acme.test"}'` | `402` con corpo `problem+json`: `"type":"urn:appgrove:seats:charge-declined"`, `"title":"Payment Required"`, `"detail":"carta scaduta"`. **Il motivo del fornitore arriva a chi ha invitato**: «non è andata» senza il perché non permette di rimediare. |
| 7.2 | `docker compose -f dev/docker-compose.yml exec -T postgres psql -U appgrove -d appgrove -c "select count(*) from platform.invitations where email='rifiutato-0098@acme.test';"` | `0`. **L'invito non esiste**: nessuna riga rimasta a metà. È la regola d'oro della storia. |
| 7.3 | `docker compose -f dev/docker-compose.yml exec -T postgres psql -U appgrove -d appgrove -c "select quantity from platform.subscription where app_id = '$SEATS_APP' and tenant_id = '$ACME';"` | Il valore di prima, **invariato**. |
| 7.4 | `grep -a "seats.charge" dev/.run/core.log \| tail -2` | Una riga `seats.charge.declined` con `target`, `quantity`, `delta_cents` e `reason=carta scaduta`. |
| 7.5 | Rimettere il simulatore ad accettare: `./app-stop.sh --apps-only && ./app-start.sh --no-build` | Gli invii tornano a riuscire, **e le interfacce web tornano su**. |

> **Attenzione al prerequisito del §4**: perché il rifiuto sia il rifiuto *di un addebito*, il prossimo posto
> deve costare qualcosa (`next.chargeCents > 0`). Se ci sono posti pagati in anticipo, l'invito non chiede
> denaro a nessuno e riesce — e il §7 non proverebbe niente.

**Esito della prima esecuzione**: tutto come atteso.

```
{"type":"urn:appgrove:seats:charge-declined","title":"Payment Required","status":402,"detail":"carta scaduta"}
HTTP 402
righe_invito: 0 · quantity: 7 (invariata)
WARN [SeatSubscriptionService] seats.charge.declined target=11 quantity=8 delta_cents=199 reason=carta scaduta
```

> **Non rieseguito nella passata di fine lotto, e il motivo è dichiarato.** Il §7 richiede di **fermare e
> riavviare le applicazioni** per cambiare una proprietà di esecuzione; la passata di fine lotto ha il vincolo
> opposto — lasciare lo stack acceso — e quindi non l'ha eseguito. Quello che è stato **verificato senza
> riavvii**: la proprietà `appgrove.seats.stub-decline-reason` esiste ancora in
> `services/core/src/main/resources/application.properties`, è ancora una proprietà di **esecuzione** letta da
> `StubPaymentProvider` (`@ConfigProperty`, quindi la variabile d'ambiente
> `APPGROVE_SEATS_STUB_DECLINE_REASON` la valorizza), e la costante `urn:appgrove:seats:charge-declined` è
> ancora quella prodotta da `InvitationResource`. La regola nel suo insieme è coperta dal collaudo
> `SeatChargeDeclinedApiTest` e dal percorso Playwright `[J-SEATS]` «senza addebito riuscito l'invito non
> nasce» — **eseguirlo a mano resta allo sviluppatore**.

---

## 8. Pulizia (per lasciare l'ambiente come si è trovato)

Si cancella **solo ciò che la guida ha creato**, e si rimette `quantity` al valore annotato al §4.1:

```bash
docker compose -f dev/docker-compose.yml exec -T postgres psql -U appgrove -d appgrove -c \
  "delete from platform.invitations
     where email like 'collaudo-0098%' or email like 'rifiutato-0098%' or email like 'franchigia-0098%';
   update platform.subscription set quantity = <la quantità annotata al §4.1>
     where app_id = '22c25c07-0247-3196-8d05-a2d26587295a'
       and tenant_id = 'a0000000-0000-4000-8000-000000000001' and deleted_at is null;"
```

> **La prima stesura cancellava la riga dell'abbonamento dei posti** (`delete from platform.subscription
> where app_id = …`), senza filtro di conto e senza chiedersi se quella riga esistesse *prima*. Va bene solo
> quando la guida l'ha creata lei: se l'abbonamento c'era già — ed è il caso su qualunque stack su cui sia
> già passata un'altra guida dei posti — quella cancellazione **distrugge stato che non è suo**, e in più
> lo distrugge per tutti i conti. La pulizia di una guida non può essere più larga della guida.

**Esito della prima esecuzione**: eseguita; il riquadro è tornato a `usedSeats: 9`, `paidQuantity: 0`,
`hasSubscription: false` — lo stato di partenza *di allora*, in cui l'abbonamento non esisteva.

**Esito della riesecuzione di fine lotto**: cancellati i quattro inviti `collaudo-0098-*` di Acme e i due
`franchigia-0098-*` di «Bob Personal», `quantity` riportata a `10`. Il riquadro è tornato esattamente allo
stato di partenza dichiarato in testa alla guida (`usedSeats 11` · `paidSeats 8` · `dueCents 2292` ·
`paidQuantity 10` · `hasSubscription true`), abbonamento dei posti **conservato**.

---

## 9. Quello che si guarda con gli occhi (**visivo**, resta allo sviluppatore)

Aprire `https://app.local.appgrove.app`, accedere come `owner@acme.test` / `Password1!`, andare su
**Members**.

| # | Cosa guardare | Cosa deve dire |
|---|---|---|
| 9.1 | Il **riquadro dei posti**, in testa alla pagina fra il titolo e il modulo di invito | Il numero grande («9 posti usati»), sotto la composizione («6 attive · 0 sospese · 3 inviti in attesa»), poi l'importo con la fascia e i posti a pagamento («Stai pagando 17,94 € al mese» · «6 posti a pagamento» · «posti 4–10: 2,99 € ciascuno»), e infine il costo del prossimo. |
| 9.2 | La **stima**, sotto il pulsante di invito, **prima** di aver scritto qualsiasi cosa | «Questa persona sarà il posto numero 10: costa 2,99 € al mese, e il tuo totale passerà da 17,94 € a 20,93 €». La stima c'è **prima** della conferma: è il presidio contro la sorpresa in fattura. |
| 9.3 | Invitare una persona e riguardare il riquadro | I numeri cambiano **subito**: il posto è occupato dall'invito, non dall'accettazione. E la frase del prossimo posto diventa quella del **confine di fascia**: «Il prossimo posto costa 1,99 € invece di 2,99 €, perché entri nello scaglione successivo. Il totale passa da 20,93 € a 22,92 €». |
| 9.4 | Con il simulatore che rifiuta (§7), provare a invitare | Messaggio in rosso: «Il pagamento del posto non è andato a buon fine (carta scaduta): l'invito non è stato creato. Controlla il metodo di pagamento in Fatturazione e riprova.» **E nessuna riga nuova nella tabella.** Nessun banner globale «il tuo abbonamento è scaduto»: quello direbbe una cosa falsa e nasconderebbe la cosa vera da fare. |
| 9.5 | Fermare il **solo** servizio `core` e ricaricare la pagina: `kill $(lsof -ti tcp:8080)` — è lo stesso «stop per porta» che usano gli script, e prende anche il figlio `java` di `mvn quarkus:dev`. **Non** `./app-stop.sh --apps-only`: spegne anche l'interfaccia, e senza interfaccia non c'è nulla da ricaricare. Per riaccendere: `./app-start.sh --no-build` | Il riquadro dice «Non riusciamo a leggere il costo dei posti. Finché non lo sappiamo non puoi invitare nessuno…» con un pulsante **Riprova**, e il pulsante **Invia invito** è **spento**. Mai invitare alla cieca. |
| 9.6 | Cambiare lingua (le cinque bandiere/impostazioni) | Tutte le frasi dei posti sono tradotte, plurali compresi («1 posto usato» / «9 posti usati»). Nessuna chiave grezza a schermo. |
| 9.7 | Barra laterale | Nessuna voce «Posti dell'account» fra le applicazioni. |
