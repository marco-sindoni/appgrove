# Come verificare a mano la change 0099 — la riduzione dei posti in attesa

> Guida scritta sul branch `change/0099-use-case-0104-riduzione-posti-attesa`, base `133eb01`,
> il 2026-08-22. È una **fotografia**: se una change successiva cambia questi comportamenti, i punti
> superati si scoprono **rieseguendola**, non rileggendola.
>
> **Passi non visivi ESEGUITI** dall'agente il 2026-08-22 contro lo stack locale — §1 … §9, tutti — con
> gli esiti riportati qui **come si leggono davvero**. Eseguirli ha prodotto:
>
> - **un difetto di prodotto trovato e corretto**: il §9 (annullamento) ha mostrato una riga di persona
>   indicata ancora **viva** dopo l'esecuzione di una riduzione. La causa era una scrittura che sembrava
>   chiudere quelle righe e non chiudeva niente — aggiornava soltanto `updated_at`. La regola corretta è
>   una sola per tutti i modi in cui una riduzione finisce: *una riduzione che non è più in attesa non ha
>   persone indicate vive*. Corretto in `SeatDowngradeExecutor` (istruzione `CLOSE_ITEMS`), con
>   un'asserzione nuova in `SeatDowngradeExecutionTest` perché non possa tornare. **La guida non è stata
>   ammorbidita**: il passo §9 è rimasto quello che era, ed è stato rieseguito a correzione fatta;
> - **nessun altro scostamento**: tutti gli altri passi hanno dato l'esito atteso al primo tentativo;
> - **una scoperta sull'ambiente**, non un difetto: l'account di prova ha **nove** posti occupati e
>   `paidQuantity` a **zero** all'inizio, perché quelle persone sono entrate quando il posto era gratuito
>   (è lo stesso punto che la guida della change 0098 aveva già registrato, e la decisione di migrazione
>   per gli account preesistenti è nei punti aperti di UC 0113). Da qui una **scelta di forma**: i passi
>   verificano le **regole** — «la quantità scende ai posti a pagamento effettivi», «nulla cambia
>   all'atto della richiesta» — e non numeri assoluti, che nell'account di prova cambiano a ogni giro
>   della suite di piattaforma.
>
> Restano allo sviluppatore i soli passi **visivi** del §10: le frasi a schermo e le caselle di scelta.
> Sono comunque coperti dal percorso Playwright `[J-SEATS]` di
> `frontend/apps/backoffice/e2e/seats.spec.ts` e dai collaudi di componente di `MembersPage.test.tsx` —
> qui si guardano con gli occhi, che è un'altra cosa.

Le nove cose da vedere:

1. **senza posti a pagamento non si programma niente**, e il rifiuto dice cosa fare invece;
2. la **stima** dice l'effetto prima della conferma, con la composizione degli scaglioni;
3. l'**owner non è indicabile**, e una **seconda** riduzione è rifiutata;
4. l'indicazione apre l'attesa e **non cambia nulla di ciò che si paga**;
5. le persone indicate compaiono **«in cessazione dal …»** nell'elenco;
6. **nessun invito** durante l'attesa, e il rifiuto offre le **due vie d'uscita**;
7. si può **mantenere** una singola persona, e l'attesa resta con le altre;
8. alla **scadenza** la riduzione si esegue: persone fuori, accessi cancellati, **quantità che scende**,
   dovuto ricalcolato — e il periodo nuovo nasce con la quantità già ridotta;
9. l'**annullamento** non ha effetti contabili e riapre gli inviti.

Ogni voce è **azione → risultato atteso**.

---

## 0. Preparazione

| # | Azione | Risultato atteso |
|---|---|---|
| 0.1 | `./app-start.sh` | Tutto verde: Postgres, proxy, Mailpit, MinIO, ElasticMQ, i servizi backend scoperti (`auth`, `core`, `crm`, `fatture`) e le due interfacce. |
| 0.2 | `./dev.sh migrate core` | Le migrazioni girano fino a **`V23 - seat downgrade`** senza errori. Se lo stack era già acceso su una banca dati a `V22`, questo passo è **necessario**: `core` gira in modalità sviluppo e ricarica il codice a caldo, ma non riapplica Flyway. |

**Il token dell'owner e la base delle chiamate** — comandi completi e incollabili:

```bash
TOKEN_OWNER=$(curl -sk https://api.local.appgrove.app/api/auth/login \
  -H 'content-type: application/json' \
  -d '{"email":"owner@acme.test","password":"Password1!"}' \
  | python3 -c 'import sys,json;print(json.load(sys.stdin)["access_token"])')

B=https://api.local.appgrove.app/api/platform/v1
```

**Gli identificativi delle persone dell'account di prova** (servono nei corpi delle richieste):

```bash
curl -sk "$B/users?size=100" -H "Authorization: Bearer $TOKEN_OWNER" \
  | python3 -c 'import sys,json
for u in json.load(sys.stdin)["content"]: print(u["id"], u["email"], u["role"], u.get("endingAt"))'
```

Esito osservato: `b0000000-…-0001 owner@acme.test owner`, `…-0002 admin@acme.test member`,
`…-0003 member@acme.test member`, `…-0004 bob@bob.test member`, più due persone entrate dagli inviti.
Nella guida si usano `…-0003` e `…-0004` come persone da cessare, e `…-0001` come owner.

---

## 1. Senza posti a pagamento non si programma niente

| # | Azione | Risultato atteso |
|---|---|---|
| 1.1 | `curl -sk -o /dev/null -w "%{http_code}\n" "$B/me/seats/reduction" -H "Authorization: Bearer $TOKEN_OWNER"` | **`204`** — nessuna attesa in corso. È uno stato normale, non un «non trovato». |
| 1.2 | Vedi il comando qui sotto | **`409`** con `type` = `urn:appgrove:seats:reduction-not-needed` e il testo che indica la via giusta. |

```bash
curl -sk "$B/me/seats/reduction" -H "Authorization: Bearer $TOKEN_OWNER" \
  -H 'content-type: application/json' \
  -d '{"userIds":["b0000000-0000-4000-8000-000000000003"]}' -w '\nHTTP %{http_code}\n'
```

Esito osservato (con l'account di prova che non ha ancora l'abbonamento dei posti):

```
{"type":"urn:appgrove:seats:reduction-not-needed","title":"Conflict","status":409,
 "detail":"Non stai pagando alcun posto: per far uscire una persona subito rimuovila dall'elenco,
 è immediato e non costa nulla."}
HTTP 409
```

**Perché è il comportamento giusto e non una scorciatoia**: un account interamente dentro la franchigia non
risparmierebbe nulla programmando una cessazione per fine periodo, e si vedrebbe negare gli inviti per un
mese in cambio di niente. La via giusta è la rimozione immediata, gratuita — e il testo la nomina.

**Per proseguire serve un abbonamento dei posti**: lo crea un invito, che paga il posto (UC 0103).

```bash
curl -sk "$B/invitations" -H "Authorization: Bearer $TOKEN_OWNER" \
  -H 'content-type: application/json' -d '{"email":"collaudo-0104@acme.test"}' \
  | python3 -c 'import sys,json;d=json.load(sys.stdin);print(d["id"], d["email"])'

docker compose -f dev/docker-compose.yml exec -T postgres psql -U appgrove -d appgrove -c \
  "select quantity, current_period_end from platform.subscription
   where app_id = (select id from platform.app where slug = 'platform-seats') and deleted_at is null;"
```

Esito osservato: una riga con `quantity | current_period_end` valorizzati (nell'esecuzione: `7` e
`2026-09-21 05:48:12+00`). **La data che conta è quella**: è la data che la riduzione userà.

---

## 2. La stima dice l'effetto prima della conferma

```bash
curl -sk "$B/me/seats/reduction/preview?userId=b0000000-0000-4000-8000-000000000003&userId=b0000000-0000-4000-8000-000000000004" \
  -H "Authorization: Bearer $TOKEN_OWNER" | python3 -m json.tool
```

| # | Che cosa guardare | Risultato atteso |
|---|---|---|
| 2.1 | `executeAt` | **coincide** con `current_period_end` letto al §1. Non «fra un mese», non «a fine mese solare». |
| 2.2 | `seatsNow` / `seatsAfter` | il secondo è il primo **meno il numero di persone indicate**. |
| 2.3 | `dueCentsNow` / `dueCentsAfter` | il secondo è **minore o uguale**. |
| 2.4 | `bandsNow` / `bandsAfter` | la **composizione degli scaglioni**, con `seats` e `subtotalCents` per fascia: è il conto che permette di verificare l'importo. |
| 2.5 | Rifai il §1.1 (`GET .../reduction`) | ancora **`204`**: la stima è una **lettura**, non ha programmato niente. |

Esito osservato: `executeAt` = `2026-09-21T05:48:12.466708Z`, `seatsNow` 10 → `seatsAfter` 8,
`dueCentsNow` 2093 → `dueCentsAfter` 1495, e la composizione da `3 × 0 + 7 × 299` a `3 × 0 + 5 × 299`.
Il conto torna: 5 × 299 = 1495.

---

## 3. L'owner non è indicabile, e una seconda riduzione è rifiutata

```bash
# 3.1 — l'owner
curl -sk "$B/me/seats/reduction" -H "Authorization: Bearer $TOKEN_OWNER" \
  -H 'content-type: application/json' \
  -d '{"userIds":["b0000000-0000-4000-8000-000000000001"]}' -w '\nHTTP %{http_code}\n'
```

Atteso: **`409`**, `type` = `urn:appgrove:seats:reduction-owner`.
Esito osservato: *«Chi governa l'account non può essere indicato per la cessazione.»*

Il caso della **seconda** riduzione si verifica al §4, dopo averne aperta una.

---

## 4. L'indicazione apre l'attesa e non cambia nulla di ciò che si paga

```bash
curl -sk "$B/me/seats/reduction" -H "Authorization: Bearer $TOKEN_OWNER" \
  -H 'content-type: application/json' \
  -d '{"userIds":["b0000000-0000-4000-8000-000000000003","b0000000-0000-4000-8000-000000000004"]}' \
  -w '\nHTTP %{http_code}\n'
```

| # | Che cosa guardare | Risultato atteso |
|---|---|---|
| 4.1 | codice di stato | **`201`**, col corpo che ripete data, persone, posti e importi. |
| 4.2 | `overdue` | **`false`**: la data è nel futuro. |
| 4.3 | seconda richiesta, subito dopo, con un'altra persona | **`409`**, `type` = `urn:appgrove:seats:reduction-already-pending`. |
| 4.4 | `GET "$B/me/seats"` | `pendingReduction` **`true`**, `reduction` col dettaglio, **`paidQuantity` invariata** rispetto al §1 e `usedSeats`/`dueCents` invariati. |
| 4.5 | Le persone indicate in banca dati | appartenenza **viva** e stato **`active`**: lavorano fino allo scadere. |

```bash
docker compose -f dev/docker-compose.yml exec -T postgres psql -U appgrove -d appgrove -c \
  "select i.email, m.status, m.deleted_at is null as viva
   from platform.membership m join platform.identity i on i.id = m.identity_id
   where i.email in ('member@acme.test','bob@bob.test');"
```

Esito osservato: `201` con `executeAt` = `2026-09-21T05:48:12.466708Z`, due persone, `seatsAfter` 8,
`dueCentsAfter` 1495; la seconda richiesta `409 reduction-already-pending`; il riquadro con
`pendingReduction: True`, `paidQuantity: 7` (**la stessa di prima**), `usedSeats: 10`, `dueCents: 2093`.

> **È la promessa centrale della storia**: la riduzione è *programmata*, non applicata. Se `paidQuantity`
> o `dueCents` cambiassero qui, un cliente si vedrebbe togliere qualcosa a metà periodo.

---

## 5. «In cessazione dal …» nell'elenco delle persone

```bash
curl -sk "$B/users?size=100" -H "Authorization: Bearer $TOKEN_OWNER" | python3 -c 'import sys,json
for u in json.load(sys.stdin)["content"]:
    if u.get("endingAt"): print("in cessazione:", u["email"], u["endingAt"])'
```

Atteso: **solo** le persone indicate, ognuna con **la data**. Chi non è indicato non porta alcuna data.

Esito osservato:

```
in cessazione: bob@bob.test 2026-09-21T05:48:12.466708Z
in cessazione: member@acme.test 2026-09-21T05:48:12.466708Z
```

---

## 6. Nessun invito durante l'attesa, e il rifiuto offre le due vie d'uscita

```bash
curl -sk "$B/invitations" -H "Authorization: Bearer $TOKEN_OWNER" \
  -H 'content-type: application/json' -d '{"email":"bloccato-0104@acme.test"}' \
  -w '\nHTTP %{http_code}\n'
```

| # | Che cosa guardare | Risultato atteso |
|---|---|---|
| 6.1 | codice e `type` | **`409`**, `type` = `urn:appgrove:seats:reduction-pending`. |
| 6.2 | il testo | nomina **due** vie d'uscita: annullare, oppure attendere la data. Un rifiuto senza uscita è un vicolo cieco. |
| 6.3 | la quantità dell'abbonamento | **invariata**: un rifiuto noto in anticipo non deve costare denaro (il varco sta **prima** dell'addebito). |

Esito osservato: *«C'è una riduzione dei posti programmata: fino alla sua esecuzione non è possibile
aggiungere persone. Annulla la riduzione, oppure attendi la data prevista.»*, e nessun invito creato.

E il presidio è **nel servizio**, non nel comando spento a schermo: questa chiamata non passa
dall'interfaccia, e viene rifiutata comunque.

---

## 7. Mantenere una singola persona

```bash
curl -sk -X DELETE "$B/me/seats/reduction/people/b0000000-0000-4000-8000-000000000004" \
  -H "Authorization: Bearer $TOKEN_OWNER" -o /dev/null -w 'HTTP %{http_code}\n'

curl -sk "$B/me/seats/reduction" -H "Authorization: Bearer $TOKEN_OWNER" | python3 -c \
  'import sys,json;print("indicati:", [p["email"] for p in json.load(sys.stdin)["people"]])'
```

Atteso: **`204`**, e l'attesa **resta** con le persone rimanenti.
Esito osservato: `204`, poi `indicati: ['member@acme.test']`.

**Solo l'owner governa la riduzione** — un collaboratore non la vede nemmeno:

```bash
TOKEN_MEMBER=$(curl -sk https://api.local.appgrove.app/api/auth/login \
  -H 'content-type: application/json' \
  -d '{"email":"member@acme.test","password":"Password1!"}' \
  | python3 -c 'import sys,json;print(json.load(sys.stdin)["access_token"])')

curl -sk -o /dev/null -w 'GET: %{http_code}\n'    "$B/me/seats/reduction" -H "Authorization: Bearer $TOKEN_MEMBER"
curl -sk -o /dev/null -w 'DELETE: %{http_code}\n' -X DELETE "$B/me/seats/reduction" -H "Authorization: Bearer $TOKEN_MEMBER"
```

Atteso ed esito osservato: **`403`** su entrambe. Un collaboratore che potesse leggere l'elenco degli
indicati saprebbe di una cessazione prima della persona interessata.

---

## 8. L'esecuzione alla scadenza

Lo spazzino gira **ogni ora** e non c'è — deliberatamente — nessuna operazione di rete per forzarlo: un
comando che esegue a richiesta qualcosa che muove il conto di un cliente è una leva che nessuno dovrebbe
avere. Per verificarlo **adesso** si usano due leve legittime: si **retrodata** la data di esecuzione, e si
consegna l'**evento di rinnovo** dell'abbonamento dei posti col simulatore del fornitore — che è
esattamente il percorso su cui la storia promette che «l'ordine conta».

```bash
# 8.1 — la data diventa passata: l'attesa risulta SCADUTA
docker compose -f dev/docker-compose.yml exec -T postgres psql -U appgrove -d appgrove -c \
  "update platform.seat_downgrade set execute_at = now() - interval '1 minute'
   where status = 'pending' and deleted_at is null;"

curl -sk "$B/me/seats/reduction" -H "Authorization: Bearer $TOKEN_OWNER" \
  | python3 -c 'import sys,json;print("overdue:", json.load(sys.stdin)["overdue"])'

# 8.2 — l'evento di rinnovo dell'abbonamento dei POSTI (identificativo deterministico della voce)
curl -sk "$B/dev/paddle/scenarios/renewal" -H "Authorization: Bearer $TOKEN_OWNER" \
  -H 'content-type: application/json' \
  -d '{"appId":"22c25c07-0247-3196-8d05-a2d26587295a"}' -o /dev/null -w 'HTTP %{http_code}\n'
sleep 6
```

Poi le quattro verifiche:

```bash
# 8.3 — la riduzione è eseguita, e nessuna persona resta indicata
docker compose -f dev/docker-compose.yml exec -T postgres psql -U appgrove -d appgrove -c \
  "select d.status, d.executed_at is not null as eseguita, i.email, it.deleted_at is not null as chiusa
   from platform.seat_downgrade d
   join platform.seat_downgrade_item it on it.downgrade_id = d.id
   join platform.identity i on i.id = it.identity_id
   order by d.created_at desc, i.email limit 5;"

# 8.4 — la quantità dell'abbonamento SCENDE, e il periodo è avanzato
docker compose -f dev/docker-compose.yml exec -T postgres psql -U appgrove -d appgrove -c \
  "select quantity, current_period_end from platform.subscription
   where app_id = (select id from platform.app where slug = 'platform-seats') and deleted_at is null;"

# 8.5 — la persona indicata è uscita, quella mantenuta no
docker compose -f dev/docker-compose.yml exec -T postgres psql -U appgrove -d appgrove -c \
  "select i.email, m.deleted_at is not null as uscita
   from platform.membership m join platform.identity i on i.id = m.identity_id
   where i.email in ('member@acme.test','bob@bob.test');"

# 8.6 — il riquadro: nessuna attesa, dovuto ricalcolato, invito di nuovo possibile
curl -sk "$B/me/seats" -H "Authorization: Bearer $TOKEN_OWNER" | python3 -c 'import sys,json
d=json.load(sys.stdin)
print("pendingReduction:", d["pendingReduction"], "| usedSeats:", d["usedSeats"],
      "| paidSeats:", d["paidSeats"], "| dueCents:", d["dueCents"], "| paidQuantity:", d["paidQuantity"])
print("dettaglio presente:", "reduction" in d)'

curl -sk "$B/invitations" -H "Authorization: Bearer $TOKEN_OWNER" \
  -H 'content-type: application/json' -d '{"email":"dopo-0104@acme.test"}' \
  -o /dev/null -w 'invito dopo l esecuzione: %{http_code}\n'

# 8.7 — la misura: nessuna riduzione scaduta e non eseguita
docker compose -f dev/docker-compose.yml exec -T postgres psql -U appgrove -d appgrove -c \
  "select count(*) as scadute_non_eseguite from platform.seat_downgrade
   where status = 'pending' and deleted_at is null and execute_at <= now();"
```

| # | Risultato atteso |
|---|---|
| 8.3 | `status` = **`executed`**, `eseguita` = `t`, e **nessuna** riga di persona indicata con `chiusa = f` per quella riduzione. |
| 8.4 | `quantity` **scesa** al numero di posti a pagamento effettivi (posti occupati meno la franchigia), e `current_period_end` **avanzato**: il periodo nuovo nasce con la quantità già ridotta. |
| 8.5 | la persona indicata `uscita = t`, quella mantenuta al §7 `uscita = f`. |
| 8.6 | `pendingReduction` **`false`**, `dettaglio presente: False`, `dueCents` ricalcolato, e l'invito che risponde **`201`**. |
| 8.7 | **`0`**. È l'invariante che la misura `appgrove.seats.reduction.overdue` sorveglia. |

Esito osservato: `executed`/`t`; quantità **da 7 a 6** con nove posti occupati e franchigia di tre (il conto
torna: 9 − 3 = 6) e periodo avanzato; `member@acme.test uscita = t`, `bob@bob.test uscita = f`;
`pendingReduction: False`, `usedSeats: 9`, `paidSeats: 6`, `dueCents: 1794`, invito **`201`**;
scadute-non-eseguite **`0`**.

> **La prova che l'ordine conta** è il §8.4 letto insieme al §8.3: la riduzione risulta eseguita *e* il
> periodo è avanzato, nella stessa transazione dell'evento. Se l'ordine fosse invertito, il periodo nuovo
> sarebbe nato con la quantità vecchia e il cliente avrebbe pagato un mese intero di troppo.

---

## 9. L'annullamento non ha effetti contabili

```bash
Q1=$(docker compose -f dev/docker-compose.yml exec -T postgres psql -U appgrove -d appgrove -tAc \
  "select quantity from platform.subscription
   where app_id = (select id from platform.app where slug = 'platform-seats') and deleted_at is null;")

curl -sk "$B/me/seats/reduction" -H "Authorization: Bearer $TOKEN_OWNER" \
  -H 'content-type: application/json' \
  -d '{"userIds":["b0000000-0000-4000-8000-000000000004"]}' -o /dev/null -w 'POST: %{http_code}\n'

curl -sk "$B/invitations" -H "Authorization: Bearer $TOKEN_OWNER" \
  -H 'content-type: application/json' -d '{"email":"x-0104@acme.test"}' \
  -o /dev/null -w 'invito durante l attesa: %{http_code}\n'

curl -sk -X DELETE "$B/me/seats/reduction" -H "Authorization: Bearer $TOKEN_OWNER" \
  -o /dev/null -w 'DELETE: %{http_code}\n'

Q2=$(docker compose -f dev/docker-compose.yml exec -T postgres psql -U appgrove -d appgrove -tAc \
  "select quantity from platform.subscription
   where app_id = (select id from platform.app where slug = 'platform-seats') and deleted_at is null;")
echo "quantità prima=$Q1 dopo=$Q2 — devono coincidere"

docker compose -f dev/docker-compose.yml exec -T postgres psql -U appgrove -d appgrove -c \
  "select status, count(*) from platform.seat_downgrade group by status order by status;"

docker compose -f dev/docker-compose.yml exec -T postgres psql -U appgrove -d appgrove -c \
  "select d.status, i.email, it.deleted_at is not null as chiusa
   from platform.seat_downgrade_item it
   join platform.seat_downgrade d on d.id = it.downgrade_id
   join platform.identity i on i.id = it.identity_id
   order by d.created_at, i.email;"

curl -sk "$B/invitations" -H "Authorization: Bearer $TOKEN_OWNER" \
  -H 'content-type: application/json' -d '{"email":"y-0104@acme.test"}' \
  -o /dev/null -w 'invito dopo l annullamento: %{http_code}\n'
```

| # | Risultato atteso |
|---|---|
| 9.1 | `POST` **`201`**, invito durante l'attesa **`409`**, `DELETE` **`204`**. |
| 9.2 | **`quantità prima = quantità dopo`**: annullare non addebita e non rimborsa, perché nulla era stato cambiato. |
| 9.3 | la riga della riduzione **resta**, con `status = cancelled`: la storia di che cosa era stato deciso è un'informazione dell'account. |
| 9.4 | **nessuna riga di persona indicata con `chiusa = f`**: una riduzione che non è più in attesa non ha persone indicate vive — vale per l'annullamento e per l'esecuzione. |
| 9.5 | l'invito dopo l'annullamento **`201`**: la prova vera che il blocco è caduto. |

Esito osservato: `201` / `409` / `204`; `quantità prima=7 dopo=7`; `cancelled | 1` e `executed | 2` in
tabella; l'invito dopo l'annullamento **`201`**.

> **Qui è stato trovato il difetto.** Alla prima esecuzione il §9.4 mostrava **una** riga di persona
> indicata ancora viva, appartenente a una riduzione già **eseguita**: la scrittura che doveva chiuderle
> aggiornava soltanto `updated_at`. Corretto in `SeatDowngradeExecutor` (`CLOSE_ITEMS` ora cancella
> logicamente), coperto da un'asserzione in `SeatDowngradeExecutionTest`, e il passo è stato **rieseguito**:
> la riduzione eseguita dopo la correzione ha la sua riga chiusa, quella eseguita prima resta come
> testimonianza del difetto in banca dati locale (si azzera con `./dev.sh reset`). Nessuna informazione
> viene perduta dalla cancellazione logica: l'esportazione dei dati personali legge **anche** le righe
> cancellate.

---

## 10. Le frasi a schermo (passi **visivi**, non eseguiti dall'agente)

Su `https://app.local.appgrove.app/members`, entrati come `owner@acme.test`:

| # | Azione | Risultato atteso |
|---|---|---|
| 10.1 | Guarda l'elenco delle persone | Ogni riga di **persona** (non l'owner, non un invito in attesa) ha una **casella di scelta** nella prima colonna. |
| 10.2 | Spunta una persona | Compare «**1 persona selezionata**» e il comando «**Indica per la cessazione**»; sotto, la **stima**: «Cesseranno il … e fino ad allora continueranno a lavorare normalmente. Dal … pagherai … invece di ….» |
| 10.3 | Leggi la riga sotto la stima | Il suggerimento per chi deve escludere qualcuno **subito**: togliere gli accessi alle applicazioni, immediato e gratuito, **in più** dell'indicazione. |
| 10.4 | Conferma | Il riquadro dei posti mostra l'avviso «**Riduzione programmata**» con quante persone e quando, quanto si pagherà da allora, la composizione degli scaglioni, l'elenco degli indicati con il comando «**Mantieni**», e il comando «**Annulla la riduzione**». |
| 10.5 | Guarda la riga della persona indicata | Etichetta «**In cessazione dal …**», tono attenuato: sta lavorando normalmente. |
| 10.6 | Prova a invitare | Il comando **è spento** (non nascosto) e il suggerimento sul passaggio del mouse spiega perché. |
| 10.7 | «Annulla la riduzione» | Chiede **conferma esplicita**, con tono neutro (non distruttivo: rimette tutti al loro posto). Dopo, l'avviso scompare e il comando di invito si riaccende. |
| 10.8 | Cambia lingua (5 lingue) | Tutte le frasi sono tradotte, con le **date formattate secondo la lingua**. |

---

## Che cosa questa guida **non** copre

- il **rinnovo del periodo** dei posti nella realtà: il §8 lo simula col fornitore finto, perché il
  prodotto dei posti presso il fornitore vero non esiste ancora (prerequisito #14). Quando esisterà, il
  percorso va riesercitato — è scritto nei punti aperti dello use case, proprietario UC 0106;
- lo **spazzino periodico** nel suo giro naturale (ogni ora): il §8 usa l'evento di rinnovo, che passa dallo
  stesso codice di esecuzione. Il giro periodico è coperto da `SeatDowngradeExecutionTest`;
- la **misura** come serie temporale: il §8.7 verifica l'invariante in banca dati, che è la stessa cosa che
  la misura pubblica. La lettura della misura vera richiede l'ambiente di osservazione del cloud (UC 0035).
