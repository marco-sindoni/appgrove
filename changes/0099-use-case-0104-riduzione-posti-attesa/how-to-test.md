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
>
> ---
>
> **Passata di fine lotto — 2026-08-22, stato finale di `main`.** Tutti i passi non visivi (§1 … §9) sono
> stati **rieseguiti** contro lo stack locale già mosso dalle guide di 0095–0098. Esito:
>
> - **nessun difetto di prodotto**: ogni regola vale ancora, compresa la correzione del §9 (le righe di
>   persona indicata si chiudono davvero);
> - **nessun invecchiamento**, e per una ragione strutturale: 0104 è l'**ultima** storia del lotto, quindi
>   nessuna change successiva può averla superata. Tutte le correzioni qui sotto sono cose che la guida
>   sbagliava **dall'inizio**;
> - **otto correzioni**: identificativi di persona **cablati** e non più veri (§0), un prerequisito
>   **rovesciato** — il §1.2 chiede un conto *senza* posti a pagamento e lo cercava sul conto che li ha (§1),
>   una regola detta in modo impreciso (§2.2), interrogazioni e aggiornamenti alla banca dati **senza il
>   filtro di conto** (§1, §8.1, §8.4, §9), l'effetto **distruttivo** del §8 non dichiarato, numeri assoluti
>   presentati come attesi (tutti gli «esito osservato»), la riga-testimone del difetto corretto che fa
>   sembrare rosso il §9.4 (§9) e la **pulizia mancante**, che a ogni esecuzione lasciava indietro due
>   persone, tre inviti e due riduzioni (nuovo §9-bis).
>
> Lo **stato di partenza** della riesecuzione, dichiarato prima di toccare qualsiasi cosa: `usedSeats 11` ·
> `paidSeats 8` · `dueCents 2292` · `paidQuantity 10` · `hasSubscription true` · `pendingReduction false` ·
> `quantity 10` · periodo dell'abbonamento dei posti `2027-08-22 → 2028-08-21`. Alla fine lo stack è stato
> rimesso lì, con la sola eccezione dichiarata al §8 (il periodo avanza, e non si riscrive a mano).

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

**Gli identificativi delle persone dell'account di prova** (servono nei corpi delle richieste) — si
**leggono**, non si copiano da questa guida:

```bash
curl -sk "$B/users?size=100" -H "Authorization: Bearer $TOKEN_OWNER" \
  | python3 -c 'import sys,json
for u in json.load(sys.stdin)["content"]: print(u["id"], u["email"], u["role"], u.get("endingAt"))'
```

> **Gli identificativi cablati sono una trappola, e questa guida vi è caduta.** La prima stesura scriveva di
> usare `…-0003` (`member@acme.test`) e `…-0004` (`bob@bob.test`) come persone da cessare. Alla riesecuzione
> di fine lotto `bob@bob.test` **non appartiene più ad Acme**: `…-0004` è il suo identificativo come *owner
> del suo conto* «Bob Personal». Chi seguisse la guida alla lettera indicherebbe una persona che non fa parte
> del conto — e otterrebbe `409 urn:appgrove:seats:reduction-person-unknown` (verificato) oppure, nella stima,
> il silenzioso scarto descritto al §2.2. **L'owner** si riconosce dal campo `role`, non dall'identificativo.

**Due persone usa-e-getta, perché il §8 fa uscire davvero chi è indicato.** Non si indicano persone del seme:
l'esecuzione della riduzione **cancella l'appartenenza e gli accessi**, e ci si ritroverebbe senza
`member@acme.test` per le prove successive. Si creano due persone per l'occasione — invito, e accettazione
dell'invito col token grezzo:

```bash
for n in a b; do
  T=$(curl -sk -X POST "$B/invitations" -H "Authorization: Bearer $TOKEN_OWNER" \
        -H 'content-type: application/json' -d "{\"email\":\"prova-0099-$n@acme.test\"}" \
      | python3 -c 'import sys,json;print(json.load(sys.stdin)["token"])')
  curl -sk -o /dev/null -w "accept $n: %{http_code}\n" \
    -X POST https://api.local.appgrove.app/api/auth/invitations/accept \
    -H 'content-type: application/json' \
    -d "{\"token\":\"$T\",\"password\":\"Password1!\",\"displayName\":\"Prova 0099 $n\"}"
done

curl -sk "$B/users?size=100" -H "Authorization: Bearer $TOKEN_OWNER" | python3 -c 'import sys,json
for u in json.load(sys.stdin)["content"]:
    if u["email"].startswith("prova-0099"): print(u["id"], u["email"])'
```

Atteso: due `accept: 200` e due identificativi. Nel resto della guida si chiamano **`$U1`** e **`$U2`**
(al §7 si «mantiene» `$U2`, e al §8 esce solo `$U1`). L'owner è la riga con `role = owner`, qui
`b0000000-0000-4000-8000-000000000001`.

**Attenzione ai posti**: i due inviti occupano due posti e **possono** produrre un addebito (guida 0098, §4);
se `next.chargeCents` era `0`, non lo producono. Non cambia nulla per questa guida, ma il conto dei posti
cambia — e per questo qui **non si scrivono numeri assoluti attesi**.

---

## 1. Senza posti a pagamento non si programma niente

| # | Azione | Risultato atteso |
|---|---|---|
| 1.1 | `curl -sk -o /dev/null -w "%{http_code}\n" "$B/me/seats/reduction" -H "Authorization: Bearer $TOKEN_OWNER"` | **`204`** — nessuna attesa in corso. È uno stato normale, non un «non trovato». Se risponde `200`, c'è già una riduzione in attesa: annullala (`DELETE`) prima di proseguire, altrimenti il §4 troverà `409 already-pending`. |
| 1.2 | Vedi il comando qui sotto | **`409`** con `type` = `urn:appgrove:seats:reduction-not-needed` e il testo che indica la via giusta. |

> **Il §1.2 va eseguito su un conto che NON paga posti**, ed è il prerequisito che la prima stesura di questa
> guida aveva rovesciato: lo chiedeva ad Acme, che nel frattempo l'abbonamento dei posti **lo ha**. Sul conto
> che paga, la stessa chiamata non rifiuta niente — **programma una riduzione** (`201`), cioè fa esattamente
> il contrario di quello che il passo vuole dimostrare. Il conto giusto il seme lo porta già: «Bob Personal»,
> di cui `bob@bob.test` è owner e unica persona (un posto occupato, zero a pagamento).

```bash
TOKEN_BOB=$(curl -sk https://api.local.appgrove.app/api/auth/login \
  -H 'content-type: application/json' \
  -d '{"email":"bob@bob.test","password":"Password1!"}' \
  | python3 -c 'import sys,json;print(json.load(sys.stdin)["access_token"])')

BOB_ID=$(curl -sk "$B/users?size=100" -H "Authorization: Bearer $TOKEN_BOB" \
  | python3 -c 'import sys,json;print(json.load(sys.stdin)["content"][0]["id"])')

curl -sk "$B/me/seats/reduction" -H "Authorization: Bearer $TOKEN_BOB" \
  -H 'content-type: application/json' \
  -d "{\"userIds\":[\"$BOB_ID\"]}" -w '\nHTTP %{http_code}\n'
```

Esito osservato, identico alla prima esecuzione e alla riesecuzione di fine lotto:

```
{"type":"urn:appgrove:seats:reduction-not-needed","title":"Conflict","status":409,
 "detail":"Non stai pagando alcun posto: per far uscire una persona subito rimuovila dall'elenco,
 è immediato e non costa nulla."}
HTTP 409
```

> Notare **l'ordine dei controlli**: la persona indicata qui è l'owner, ma la risposta è `not-needed`, non
> `reduction-owner`. Si rifiuta prima ciò che rende la richiesta inutile, poi ciò che la rende illecita: chi
> non paga posti non deve nemmeno arrivare a discutere di chi può cessare.

**Perché è il comportamento giusto e non una scorciatoia**: un account interamente dentro la franchigia non
risparmierebbe nulla programmando una cessazione per fine periodo, e si vedrebbe negare gli inviti per un
mese in cambio di niente. La via giusta è la rimozione immediata, gratuita — e il testo la nomina.

**Per proseguire serve un abbonamento dei posti sul conto di prova** (Acme). Se non c'è, lo crea un invito
che paga il posto (UC 0103); se c'è già — ed è il caso su qualunque stack su cui sia passata la guida 0098 —
questo passo si salta e si legge soltanto la riga.

```bash
ACME=a0000000-0000-4000-8000-000000000001

# solo se l'abbonamento dei posti non esiste ancora
curl -sk "$B/invitations" -H "Authorization: Bearer $TOKEN_OWNER" \
  -H 'content-type: application/json' -d '{"email":"collaudo-0104@acme.test"}' \
  | python3 -c 'import sys,json;d=json.load(sys.stdin);print(d["id"], d["email"])'

docker compose -f dev/docker-compose.yml exec -T postgres psql -U appgrove -d appgrove -c \
  "select quantity, current_period_end from platform.subscription
   where app_id = (select id from platform.app where slug = 'platform-seats')
     and tenant_id = '$ACME' and deleted_at is null;"
```

> Il filtro `tenant_id` non è ornamentale: la banca dati locale è una sola per tutti i conti, e senza filtro
> la riga letta può essere di un altro conto. Vale anche per il §8.1 (un `update` senza filtro
> **retrodaterebbe le riduzioni di tutti**), per il §8.4 e per il §9.

Esito osservato: una riga con `quantity | current_period_end` valorizzati — `7` e `2026-09-21 05:48:12+00`
alla prima esecuzione, `10` e `2028-08-21 05:51:25+00` alla riesecuzione di fine lotto. **La data che conta è
quella**: è la data che la riduzione userà. Il valore non va confrontato con questa guida, va **annotato**.

> **Perché in locale la data può essere fra due anni.** Il periodo dell'abbonamento dei posti in locale è
> quello che il **simulatore** del fornitore scrive: lo scenario `renewal` (§8.2) fa avanzare il periodo di
> **un anno** per volta (`StubScenarioEmitter`, caso `renewal`: `base+365` → `base+730`), non di un mese.
> Ogni riesecuzione del §8 sposta quindi la data un anno più in là. Non è il prodotto a sbagliare periodo:
> l'abbonamento dei posti vero è mensile, e questo è un simulatore.

---

## 2. La stima dice l'effetto prima della conferma

```bash
U1=<identificativo di prova-0099-a>
U2=<identificativo di prova-0099-b>

curl -sk "$B/me/seats/reduction/preview?userId=$U1&userId=$U2" \
  -H "Authorization: Bearer $TOKEN_OWNER" | python3 -m json.tool
```

| # | Che cosa guardare | Risultato atteso |
|---|---|---|
| 2.1 | `executeAt` | **coincide** con `current_period_end` letto al §1. Non «fra un mese», non «a fine mese solare». |
| 2.2 | `seatsNow` / `seatsAfter` | il secondo è il primo **meno il numero di persone elencate in `people`** — non meno il numero di identificativi passati: vedi il riquadro qui sotto. |
| 2.3 | `dueCentsNow` / `dueCentsAfter` | il secondo è **minore o uguale**. |
| 2.4 | `bandsNow` / `bandsAfter` | la **composizione degli scaglioni**, con `seats` e `subtotalCents` per fascia: è il conto che permette di verificare l'importo. |
| 2.5 | Rifai il §1.1 (`GET .../reduction`) | ancora **`204`**: la stima è una **lettura**, non ha programmato niente. |

> **La stima ignora chi non appartiene (più) al conto; la richiesta no.** È una scelta scritta nel codice
> (`SeatDowngradeService.preview`): la selezione a schermo può essere diventata vecchia — qualcuno è stato
> rimosso in un'altra scheda — e un errore al posto della stima costringerebbe a ricaricare la pagina per
> capirlo. Perciò `people` può contenere **meno** persone di quante ne hai indicate, e `seatsAfter` scende
> solo di quelle. La prima stesura di questa guida scriveva «meno il numero di persone indicate», e alla
> riesecuzione il passo è fallito: due identificativi passati, `people` con **uno** solo, `seatsAfter` scesa
> di uno. Non è un difetto — la guida diceva la regola sbagliata. Il rigore sta dov'è necessario, cioè dove
> si crea uno stato, e si verifica così:
>
> ```bash
> curl -sk "$B/me/seats/reduction" -H "Authorization: Bearer $TOKEN_OWNER" \
>   -H 'content-type: application/json' \
>   -d '{"userIds":["b0000000-0000-4000-8000-000000000004"]}' -w '\nHTTP %{http_code}\n'
> ```
>
> Atteso ed eseguito: **`409`** con `type` = `urn:appgrove:seats:reduction-person-unknown` e il testo «Una
> delle persone indicate non fa parte di questo account.» — l'identificativo è quello di `bob@bob.test`, che
> è owner di un altro conto.

Esito della prima esecuzione: `executeAt` = `2026-09-21T05:48:12.466708Z`, `seatsNow` 10 → `seatsAfter` 8,
`dueCentsNow` 2093 → `dueCentsAfter` 1495, composizione da `3 × 0 + 7 × 299` a `3 × 0 + 5 × 299`
(5 × 299 = 1495 ✓).

Esito della riesecuzione di fine lotto: `executeAt` = `2028-08-21T05:51:25Z` — **uguale** al
`current_period_end` letto al §1 ✓ —, `seatsNow` 13 → `seatsAfter` 11 (due persone in `people` ✓),
`dueCentsNow` 2690 → `dueCentsAfter` 2292 ✓, composizione da `3 × 0 + 7 × 299 + 3 × 199` a
`3 × 0 + 7 × 299 + 1 × 199`. Il conto torna in entrambi i casi: 2093 + 597 = 2690 e 2093 + 199 = 2292. Il
§2.5 ha risposto `204` ✓: la stima non programma niente.

---

## 3. L'owner non è indicabile, e una seconda riduzione è rifiutata

```bash
# 3.1 — l'owner
curl -sk "$B/me/seats/reduction" -H "Authorization: Bearer $TOKEN_OWNER" \
  -H 'content-type: application/json' \
  -d '{"userIds":["b0000000-0000-4000-8000-000000000001"]}' -w '\nHTTP %{http_code}\n'
```

(L'identificativo è quello della riga con `role = owner` letta al §0.)

Atteso: **`409`**, `type` = `urn:appgrove:seats:reduction-owner`.
Esito osservato, uguale nella prima esecuzione e nella riesecuzione di fine lotto:
*«Chi governa l'account non può essere indicato per la cessazione.»*

Il caso della **seconda** riduzione si verifica al §4, dopo averne aperta una.

---

## 4. L'indicazione apre l'attesa e non cambia nulla di ciò che si paga

```bash
curl -sk "$B/me/seats/reduction" -H "Authorization: Bearer $TOKEN_OWNER" \
  -H 'content-type: application/json' \
  -d "{\"userIds\":[\"$U1\",\"$U2\"]}" \
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
   where i.email in ('prova-0099-a@acme.test','prova-0099-b@acme.test');"
```

Esito della prima esecuzione: `201` con `executeAt` = `2026-09-21T05:48:12.466708Z`, due persone,
`seatsAfter` 8, `dueCentsAfter` 1495; la seconda richiesta `409 reduction-already-pending`; il riquadro con
`pendingReduction: True`, `paidQuantity: 7` (**la stessa di prima**), `usedSeats: 10`, `dueCents: 2093`.

Esito della riesecuzione di fine lotto: `201` con `executeAt` = `2028-08-21T05:51:25Z`, `overdue: false`, due
persone, `seatsAfter` 11, `dueCentsAfter` 2292; la seconda richiesta (con `member@acme.test`)
`409 reduction-already-pending`; il riquadro con `pendingReduction: True` e il dettaglio `reduction`
presente, `paidQuantity: 10`, `usedSeats: 13`, `dueCents: 2690` — **tutti e tre identici a prima della
richiesta**; `quantity` dell'abbonamento ancora `10`; le due appartenenze `active` e `viva = t`.

**Quello che va confrontato non sono i numeri, sono i delta**: prima e dopo la richiesta, `usedSeats`,
`dueCents`, `paidQuantity` e `quantity` devono essere **gli stessi**. È l'unica lettura che regge su
qualunque stato di partenza.

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

Esito della prima esecuzione:

```
in cessazione: bob@bob.test 2026-09-21T05:48:12.466708Z
in cessazione: member@acme.test 2026-09-21T05:48:12.466708Z
```

Esito della riesecuzione di fine lotto — e vale la pena stampare **anche** l'elenco di chi non porta data,
perché è metà dell'asserzione:

```
con data:   [('prova-0099-a@acme.test', '2028-08-21T05:51:25Z'),
             ('prova-0099-b@acme.test', '2028-08-21T05:51:25Z')]
senza data: ['invitee-member@acme.test', 'member@example.test', 'owner@acme.test',
             'admin@acme.test', 'member@acme.test']
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

Esito osservato — identico nella prima esecuzione e nella riesecuzione di fine lotto: *«C'è una riduzione dei
posti programmata: fino alla sua esecuzione non è possibile aggiungere persone. Annulla la riduzione, oppure
attendi la data prevista.»*, `quantity` invariata e **nessuna riga** in `platform.invitations` per
`bloccato-0104@acme.test`.

E il presidio è **nel servizio**, non nel comando spento a schermo: questa chiamata non passa
dall'interfaccia, e viene rifiutata comunque.

---

## 7. Mantenere una singola persona

```bash
curl -sk -X DELETE "$B/me/seats/reduction/people/$U2" \
  -H "Authorization: Bearer $TOKEN_OWNER" -o /dev/null -w 'HTTP %{http_code}\n'

curl -sk "$B/me/seats/reduction" -H "Authorization: Bearer $TOKEN_OWNER" | python3 -c \
  'import sys,json;d=json.load(sys.stdin)
print("indicati:", [p["email"] for p in d["people"]], "| seatsAfter", d["seatsAfter"],
      "| dueCentsAfter", d["dueCentsAfter"])'
```

Atteso: **`204`**, l'attesa **resta** con le persone rimanenti, e `seatsAfter`/`dueCentsAfter` sono
**ricalcolati** su una persona in meno: mantenere qualcuno cambia il conto, e il conto deve dirlo subito.
Esito della prima esecuzione: `204`, poi `indicati: ['member@acme.test']`.
Esito della riesecuzione di fine lotto: `204`, poi `indicati: ['prova-0099-a@acme.test']` con `seatsAfter`
da 11 a **12** e `dueCentsAfter` da 2292 a **2491**.

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

> **Questo passo fa uscire davvero la persona indicata**: appartenenza cancellata, accessi alle applicazioni
> revocati. Non è reversibile con un annullamento — l'annullamento (§9) vale *prima* dell'esecuzione. Per
> questo al §0 si creano due persone usa-e-getta: eseguire il §8 su una persona del seme significa perderla
> per le prove successive, e la prima stesura di questa guida non lo diceva.

```bash
# 8.1 — la data diventa passata: l'attesa risulta SCADUTA (solo per QUESTO conto)
docker compose -f dev/docker-compose.yml exec -T postgres psql -U appgrove -d appgrove -c \
  "update platform.seat_downgrade set execute_at = now() - interval '1 minute'
   where status = 'pending' and deleted_at is null and tenant_id = '$ACME';"

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
   where d.tenant_id = '$ACME'
   order by d.created_at desc, i.email limit 5;"

# 8.4 — la quantità dell'abbonamento SCENDE, e il periodo è avanzato
docker compose -f dev/docker-compose.yml exec -T postgres psql -U appgrove -d appgrove -c \
  "select quantity, current_period_start, current_period_end from platform.subscription
   where app_id = (select id from platform.app where slug = 'platform-seats')
     and tenant_id = '$ACME' and deleted_at is null;"

# 8.5 — la persona indicata è uscita, quella mantenuta no
docker compose -f dev/docker-compose.yml exec -T postgres psql -U appgrove -d appgrove -c \
  "select i.email, m.deleted_at is not null as uscita
   from platform.membership m join platform.identity i on i.id = m.identity_id
   where i.email in ('prova-0099-a@acme.test','prova-0099-b@acme.test');"

# 8.6 — il riquadro: nessuna attesa, dovuto ricalcolato, invito di nuovo possibile
curl -sk "$B/me/seats" -H "Authorization: Bearer $TOKEN_OWNER" | python3 -c 'import sys,json
d=json.load(sys.stdin)
print("pendingReduction:", d["pendingReduction"], "| usedSeats:", d["usedSeats"],
      "| paidSeats:", d["paidSeats"], "| dueCents:", d["dueCents"], "| paidQuantity:", d["paidQuantity"])
print("dettaglio presente:", "reduction" in d)'

curl -sk "$B/invitations" -H "Authorization: Bearer $TOKEN_OWNER" \
  -H 'content-type: application/json' -d '{"email":"dopo-0099@acme.test"}' \
  -o /dev/null -w 'invito dopo l esecuzione: %{http_code}\n'

# 8.7 — la misura: nessuna riduzione scaduta e non eseguita
docker compose -f dev/docker-compose.yml exec -T postgres psql -U appgrove -d appgrove -c \
  "select count(*) as scadute_non_eseguite from platform.seat_downgrade
   where status = 'pending' and deleted_at is null and execute_at <= now();"
```

| # | Risultato atteso |
|---|---|
| 8.3 | `status` = **`executed`**, `eseguita` = `t`, e **nessuna** riga di persona indicata con `chiusa = f` per quella riduzione. |
| 8.4 | `quantity` **scesa** al numero di posti a pagamento effettivi (posti occupati meno la franchigia), e il periodo **avanzato**: il periodo nuovo nasce con la quantità già ridotta. |
| 8.5 | la persona indicata `uscita = t`, quella mantenuta al §7 `uscita = f`. |
| 8.6 | `pendingReduction` **`false`**, `dettaglio presente: False`, `dueCents` ricalcolato, e l'invito che risponde **`201`**. |
| 8.7 | **`0`**. È l'invariante che la misura `appgrove.seats.reduction.overdue` sorveglia. |

Esito della prima esecuzione: `executed`/`t`; quantità **da 7 a 6** con nove posti occupati e franchigia di
tre (9 − 3 = 6) e periodo avanzato; `member@acme.test uscita = t`, `bob@bob.test uscita = f`;
`pendingReduction: False`, `usedSeats: 9`, `paidSeats: 6`, `dueCents: 1794`, invito **`201`**;
scadute-non-eseguite **`0`**.

Esito della riesecuzione di fine lotto: `overdue: True` dopo la retrodatazione; l'evento di rinnovo accettato
(`202`); poi `executed`/`t` con **entrambe** le righe di persona `chiusa = t` (anche quella della persona
mantenuta al §7, chiusa già allora); quantità **da 10 a 9** con dodici posti occupati e franchigia di tre
(12 − 3 = 9 ✓) e periodo avanzato da `2027-08-22 05:51` a `2027-08-22 10:12`;
`prova-0099-a@acme.test uscita = t`, `prova-0099-b@acme.test uscita = f`; `pendingReduction: False`,
`dettaglio presente: False`, `usedSeats: 12`, `paidSeats: 9`, `dueCents: 2491`, invito **`201`**;
scadute-non-eseguite **`0`**.

**La regola da verificare è `quantity = max(0, usedSeats − freeSeats)` dopo l'esecuzione**, non «6» né «9»:
il numero dipende da quante persone c'erano prima.

> **La prova che l'ordine conta** è il §8.4 letto insieme al §8.3: la riduzione risulta eseguita *e* il
> periodo è avanzato, nella stessa transazione dell'evento. Se l'ordine fosse invertito, il periodo nuovo
> sarebbe nato con la quantità vecchia e il cliente avrebbe pagato un mese intero di troppo.

---

## 9. L'annullamento non ha effetti contabili

```bash
Q1=$(docker compose -f dev/docker-compose.yml exec -T postgres psql -U appgrove -d appgrove -tAc \
  "select quantity from platform.subscription
   where app_id = (select id from platform.app where slug = 'platform-seats')
     and tenant_id = '$ACME' and deleted_at is null;" | tr -d '[:space:]')

curl -sk "$B/me/seats/reduction" -H "Authorization: Bearer $TOKEN_OWNER" \
  -H 'content-type: application/json' \
  -d "{\"userIds\":[\"$U2\"]}" -o /dev/null -w 'POST: %{http_code}\n'

curl -sk "$B/invitations" -H "Authorization: Bearer $TOKEN_OWNER" \
  -H 'content-type: application/json' -d '{"email":"x-0099@acme.test"}' \
  -o /dev/null -w 'invito durante l attesa: %{http_code}\n'

curl -sk -X DELETE "$B/me/seats/reduction" -H "Authorization: Bearer $TOKEN_OWNER" \
  -o /dev/null -w 'DELETE: %{http_code}\n'

Q2=$(docker compose -f dev/docker-compose.yml exec -T postgres psql -U appgrove -d appgrove -tAc \
  "select quantity from platform.subscription
   where app_id = (select id from platform.app where slug = 'platform-seats')
     and tenant_id = '$ACME' and deleted_at is null;" | tr -d '[:space:]')
echo "quantità prima=$Q1 dopo=$Q2 — devono coincidere"

docker compose -f dev/docker-compose.yml exec -T postgres psql -U appgrove -d appgrove -c \
  "select status, count(*) from platform.seat_downgrade
   where tenant_id = '$ACME' group by status order by status;"

docker compose -f dev/docker-compose.yml exec -T postgres psql -U appgrove -d appgrove -c \
  "select d.status, i.email, it.deleted_at is not null as chiusa
   from platform.seat_downgrade_item it
   join platform.seat_downgrade d on d.id = it.downgrade_id
   join platform.identity i on i.id = it.identity_id
   where d.tenant_id = '$ACME'
   order by d.created_at, i.email;"

curl -sk "$B/invitations" -H "Authorization: Bearer $TOKEN_OWNER" \
  -H 'content-type: application/json' -d '{"email":"y-0099@acme.test"}' \
  -o /dev/null -w 'invito dopo l annullamento: %{http_code}\n'
```

| # | Risultato atteso |
|---|---|
| 9.1 | `POST` **`201`**, invito durante l'attesa **`409`**, `DELETE` **`204`**. |
| 9.2 | **`quantità prima = quantità dopo`**: annullare non addebita e non rimborsa, perché nulla era stato cambiato. |
| 9.3 | la riga della riduzione **resta**, con `status = cancelled`: la storia di che cosa era stato deciso è un'informazione dell'account. |
| 9.4 | **nessuna riga di persona indicata con `chiusa = f`**: una riduzione che non è più in attesa non ha persone indicate vive — vale per l'annullamento e per l'esecuzione. |
| 9.5 | l'invito dopo l'annullamento **`201`**: la prova vera che il blocco è caduto. |

Esito della prima esecuzione: `201` / `409` / `204`; `quantità prima=7 dopo=7`; `cancelled | 1` e
`executed | 2` in tabella; l'invito dopo l'annullamento **`201`**.

Esito della riesecuzione di fine lotto: `201` / `409` / `204`; `quantità prima=10 dopo=10`; `cancelled | 2` e
`executed | 3`; **tutte** le righe di persona delle riduzioni create dalla guida con `chiusa = t`; l'invito
dopo l'annullamento **`201`**.

> **Qui è stato trovato il difetto.** Alla prima esecuzione il §9.4 mostrava **una** riga di persona
> indicata ancora viva, appartenente a una riduzione già **eseguita**: la scrittura che doveva chiuderle
> aggiornava soltanto `updated_at`. Corretto in `SeatDowngradeExecutor` (`CLOSE_ITEMS` ora cancella
> logicamente), coperto da un'asserzione in `SeatDowngradeExecutionTest`, e il passo è stato **rieseguito**:
> la riduzione eseguita dopo la correzione ha la sua riga chiusa, quella eseguita prima resta come
> testimonianza del difetto in banca dati locale (si azzera con `./dev.sh reset`). Nessuna informazione
> viene perduta dalla cancellazione logica: l'esportazione dei dati personali legge **anche** le righe
> cancellate.
>
> **Come si legge il §9.4 su una banca dati vecchia, senza spaventarsi.** Quella riga-testimone è ancora lì:
> alla riesecuzione di fine lotto l'interrogazione risponde `executed | member@acme.test | chiusa = f`. Non è
> un difetto che torna — è la riduzione eseguita **prima** della correzione, e si riconosce dalla sua
> `created_at` anteriore. L'invariante da verificare è quindi: *nessuna riga con `chiusa = f` fra le
> riduzioni che questa guida ha creato*. Per non dover distinguere a occhio, si può restringere per data
> aggiungendo `and d.created_at > now() - interval '1 hour'`, oppure ripartire da una banca dati pulita con
> `./dev.sh reset`.

---

## 9-bis. Pulizia (per lasciare l'ambiente come si è trovato)

La prima stesura di questa guida **non aveva la pulizia**, e ogni sua esecuzione lasciava indietro due
persone, tre inviti e due riduzioni. Si cancella **solo ciò che la guida ha creato**, in ordine di
dipendenza, e si rimette `quantity` al valore annotato al §1:

```bash
docker compose -f dev/docker-compose.yml exec -T postgres psql -U appgrove -d appgrove -v ON_ERROR_STOP=1 <<SQL
begin;
delete from platform.invitations
  where email in ('dopo-0099@acme.test','x-0099@acme.test','y-0099@acme.test','bloccato-0104@acme.test')
     or email like 'prova-0099%';
delete from platform.seat_downgrade_item where downgrade_id in (
  select id from platform.seat_downgrade where tenant_id = '$ACME'
    and created_at > now() - interval '1 hour');
delete from platform.seat_downgrade where tenant_id = '$ACME'
  and created_at > now() - interval '1 hour';
delete from platform.membership
  where identity_id in (select id from platform.identity where email like 'prova-0099%');
delete from auth_local.credentials
  where cognito_sub in (select cognito_sub from platform.identity where email like 'prova-0099%');
delete from platform.identity where email like 'prova-0099%';
update platform.subscription set quantity = <la quantità annotata al §1>
  where app_id = (select id from platform.app where slug = 'platform-seats')
    and tenant_id = '$ACME' and deleted_at is null;
commit;
SQL
```

**Che cosa NON si rimette a posto, e perché**: il **periodo** dell'abbonamento dei posti resta avanzato dal
§8. L'evento di rinnovo è registrato in `platform.webhook_event` e in `platform.billing_transaction`:
riscrivere a mano le date renderebbe la riga dell'abbonamento incoerente col registro degli eventi, cioè
sostituirebbe una traccia vera con una finta. Un periodo avanzato non disturba nulla; una banca dati che si
contraddice sì. Chi vuole lo stato originale usa `./dev.sh reset`.

**Esito della riesecuzione di fine lotto**: eseguita; il riquadro è tornato esattamente allo stato di
partenza dichiarato in testa (`usedSeats 11` · `paidSeats 8` · `dueCents 2292` · `paidQuantity 10` ·
`hasSubscription true`), `GET .../me/seats/reduction` di nuovo `204`, elenco degli inviti e delle riduzioni
identico a prima della passata.

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
