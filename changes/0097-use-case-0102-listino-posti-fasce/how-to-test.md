# Come verificare a mano la change 0097 — listino dei posti a scaglioni progressivi

> Guida scritta sul branch `change/0097-use-case-0102-listino-posti-fasce`, base `06db5dc`, il 2026-08-22.
> È una **fotografia**: se una change successiva cambia questi comportamenti, i punti superati si scoprono
> **rieseguendola**, non rileggendola.
>
> **Passi non visivi ESEGUITI** dall'agente il 2026-08-22 sullo stack locale — §1, §2, §3, §4, §5, §6, §7,
> §8, §9.2, §9.3 — con gli esiti riportati qui **come si leggono davvero**. Eseguirli ha prodotto **due
> correzioni alla guida** e **nessun difetto di prodotto**:
>
> 1. il §7 dava per presupposto una banca dati **appena seminata** (3 persone + 2 inviti = 5 posti) e non lo
>    diceva. Eseguito dopo la suite di piattaforma, l'account Acme ne aveva 6 e 3: il conto del prodotto era
>    **giusto** (9 posti, con i due inviti *accettati* correttamente esclusi perché quelle persone sono già
>    contate fra le appartenenze), sbagliato era il numero atteso scritto nella guida. Il paragrafo è stato
>    riscritto per verificare la **regola** invece del numero, dichiarando lo stato di partenza — ed è
>    diventato un controllo migliore, perché mostra a occhio che le due liste non si sovrappongono;
> 2. il §8 riavviava con `./dev.sh service core restart`, sottocomando che **non esiste** (il dispatcher
>    conosce `service <app_id>`, non `restart`). Sostituito con `./app-stop.sh --apps-only &&
>    ./app-start.sh --no-build --no-spa`, che riavvia le sole applicazioni lasciando i dati dov'erano.
>
> Resta allo sviluppatore il solo passo **visivo** §9.1 — ed è una verifica di *assenza*: non deve essere
> comparso nulla di nuovo nell'interfaccia.

Questa storia **non ha schermate** (lo dice il suo §6: «Nessuna schermata»). Le superfici che mostreranno i
posti al cliente arrivano dopo — il riquadro dei posti con UC 0103, la sezione «Billing» con UC 0106, il
governo delle tariffe in console con UC 0105. Perciò questa guida è quasi tutta **non visiva**: si guarda la
banca dati e si chiamano le interfacce di programmazione. È un vantaggio, non un limite: significa che
**tutto** quello che c'è scritto qui si può eseguire davvero.

Le cinque cose da vedere con i propri occhi:

1. il listino esiste in banca dati come **versione con le sue fasce**, non come numeri nel codice;
2. la **franchigia è una riga di listino** (1–3 a tariffa zero), non una condizione nel programma;
3. il calcolo a **scaglioni progressivi** produce i numeri della storia — e li produce **anche rifacendo il
   conto a mano sul listino della banca dati**, senza passare dal codice Java;
4. il listino si **legge** da un'operazione di rete, con qualunque ruolo, e non si legge senza token;
5. il **riavvio non duplica** il listino.

Ogni voce è **azione → risultato atteso**.

---

## 0. Preparazione

| # | Azione | Risultato atteso |
|---|---|---|
| 0.1 | `./app-start.sh` | Tutto verde: Postgres, proxy, Mailpit, MinIO, ElasticMQ, i servizi backend scoperti (`auth`, `core`, `crm`, `fatture`) e le due interfacce. |
| 0.2 | `./dev.sh migrate` | Le migrazioni girano fino a `V21` senza errori (`app-start.sh` le applica già; questo passo serve solo se si parte da una banca dati vecchia). |

**I due token che servono** — comandi completi e incollabili:

```bash
TOKEN_OWNER=$(curl -sk https://api.local.appgrove.app/api/auth/login \
  -H 'content-type: application/json' \
  -d '{"email":"owner@acme.test","password":"Password1!"}' \
  | python3 -c 'import sys,json;print(json.load(sys.stdin)["access_token"])')
echo "${#TOKEN_OWNER} caratteri"   # > 0 = pronto

TOKEN_MEMBER=$(curl -sk https://api.local.appgrove.app/api/auth/login \
  -H 'content-type: application/json' \
  -d '{"email":"member@acme.test","password":"Password1!"}' \
  | python3 -c 'import sys,json;print(json.load(sys.stdin)["access_token"])')
echo "${#TOKEN_MEMBER} caratteri"   # > 0 = pronto
```

---

## 1. Il listino esiste in banca dati, versionato

```bash
docker compose -f dev/docker-compose.yml exec -T postgres psql -U appgrove -d appgrove -c \
  "select id, effective_from, currency, created_by, left(note, 40) as nota
     from platform.seat_pricing_version
    where deleted_at is null
    order by effective_from;"
```

**Atteso**: **una sola** riga, con `effective_from` = `1970-01-01 00:00:00+00`, `currency` = `EUR`,
`created_by` = `seat-pricing-loader`. La decorrenza all'inizio dei tempi non è una svista: la prima versione
deve risultare vigente anche per una domanda su un periodo anteriore all'installazione.

```bash
docker compose -f dev/docker-compose.yml exec -T postgres psql -U appgrove -d appgrove -c \
  "select b.from_seat, b.to_seat, b.unit_price_cents
     from platform.seat_pricing_band b
     join platform.seat_pricing_version v on v.id = b.version_id
    where b.deleted_at is null and v.deleted_at is null
    order by b.from_seat;"
```

**Atteso**: **cinque** fasce, esattamente queste —

| from_seat | to_seat | unit_price_cents |
|---|---|---|
| 1 | 3 | 0 |
| 4 | 10 | 299 |
| 11 | 50 | 199 |
| 51 | 100 | 99 |
| 101 | *(vuoto)* | 49 |

L'ultima fascia ha il posto finale **vuoto**: è la fascia aperta, quella che dà un prezzo al posto
successivo qualunque sia il numero di persone. Un listino la cui ultima fascia fosse chiusa viene rifiutato
all'avvio.

## 2. Il listino è di piattaforma, non di un account

```bash
docker compose -f dev/docker-compose.yml exec -T postgres psql -U appgrove -d appgrove -c \
  "select table_name, column_name
     from information_schema.columns
    where table_schema = 'platform'
      and table_name in ('seat_pricing_version', 'seat_pricing_band')
      and column_name = 'tenant_id';"
```

**Atteso**: **zero righe** (`(0 rows)`). Le due tabelle **non** portano il discriminatore di account, ed è
deliberato: il listino è uguale per tutti. Una tariffa per singolo account sarebbe un prezzo negoziato, che
l'epica 22 esclude esplicitamente.

## 3. La franchigia è una riga di listino, non una riga di codice

```bash
docker compose -f dev/docker-compose.yml exec -T postgres psql -U appgrove -d appgrove -c \
  "select b.from_seat, b.to_seat, b.unit_price_cents
     from platform.seat_pricing_band b
     join platform.seat_pricing_version v on v.id = b.version_id
    where b.unit_price_cents = 0 and b.deleted_at is null and v.deleted_at is null;"
```

**Atteso**: una riga sola, `1 | 3 | 0`. È tutta la franchigia: nel programma non esiste alcuna condizione
«se i posti sono al massimo tre allora zero». Il giorno in cui la franchigia cambia, cambia questa riga.

```bash
grep -rn "≤ 3\|<= 3\|posti <= 3" services/core/src/main/java/app/appgrove/core/billing/seats/ ; echo "esito grep: $?"
```

**Atteso**: nessuna corrispondenza, `esito grep: 1`. È la prova al contrario: la soglia dei tre posti non è
scritta nel codice del calcolo.

## 4. Il conto a scaglioni, rifatto a mano sul listino della banca dati

Questo è il passo che conta. La somma viene rifatta **in SQL**, partendo dalle fasce come sono scritte in
banca dati: se combacia con la tabella dello use case, il calcolo del prodotto e il listino sono d'accordo
fra loro **e** con la specifica — e il conto non passa dal codice Java, quindi non può «essere giusto perché
il programma lo dice».

```bash
docker compose -f dev/docker-compose.yml exec -T postgres psql -U appgrove -d appgrove -c \
  "with posti(n) as (values (0),(1),(2),(3),(4),(5),(8),(10),(11),(12),(50),(51),(52),(55),(100),(101),(120)),
        fasce as (
          select b.from_seat, b.to_seat, b.unit_price_cents
            from platform.seat_pricing_band b
            join platform.seat_pricing_version v on v.id = b.version_id
           where b.deleted_at is null and v.deleted_at is null
        )
   select p.n as posti,
          sum(greatest(least(p.n, coalesce(f.to_seat, p.n)) - f.from_seat + 1, 0) * f.unit_price_cents) as dovuto_centesimi,
          to_char(sum(greatest(least(p.n, coalesce(f.to_seat, p.n)) - f.from_seat + 1, 0) * f.unit_price_cents) / 100.0, 'FM999990.00') as dovuto_euro
     from posti p cross join fasce f
    group by p.n
    order by p.n;"
```

**Atteso**, riga per riga (sono i valori della tabella dello use case §4):

| posti | dovuto_centesimi | dovuto_euro |
|---|---|---|
| 0 | 0 | 0.00 |
| 1 | 0 | 0.00 |
| 2 | 0 | 0.00 |
| 3 | 0 | 0.00 |
| 4 | 299 | 2.99 |
| 5 | 598 | 5.98 |
| 8 | 1495 | 14.95 |
| 10 | 2093 | 20.93 |
| 11 | 2292 | 22.92 |
| 12 | 2491 | 24.91 |
| 50 | 10053 | 100.53 |
| 51 | 10152 | 101.52 |
| 52 | 10251 | 102.51 |
| 55 | 10548 | 105.48 |
| 100 | 15003 | 150.03 |
| 101 | 15052 | 150.52 |
| 120 | 15983 | 159.83 |

**Due cose da guardare, non solo da leggere**:

- il totale **non scende mai** passando da una riga alla successiva (10 → 11 → 12 cresce: 20,93 → 22,92 →
  24,91). È la proprietà per cui il modello a scaglioni è stato scelto: quello scartato faceva costare
  undici posti **meno** di dieci;
- il 52 è l'esempio svolto della storia: `20,93 + 79,60 + 1,98 = 102,51 €`.

## 5. Il costo del posto successivo scende ai tre confini

```bash
docker compose -f dev/docker-compose.yml exec -T postgres psql -U appgrove -d appgrove -c \
  "with posti(n) as (values (1),(2),(3),(4),(9),(10),(49),(50),(99),(100),(101))
   select p.n as posti_attuali,
          (select b.unit_price_cents
             from platform.seat_pricing_band b
             join platform.seat_pricing_version v on v.id = b.version_id
            where b.deleted_at is null and v.deleted_at is null
              and p.n + 1 >= b.from_seat
              and (b.to_seat is null or p.n + 1 <= b.to_seat)) as posto_successivo_centesimi
     from posti p order by p.n;"
```

**Atteso**: `1 → 0`, `2 → 0`, `3 → 299`, `4 → 299`, `9 → 299`, **`10 → 199`**, `49 → 199`, **`50 → 99`**,
`99 → 99`, **`100 → 49`**, `101 → 49`.

I tre valori in grassetto sono i confini: **è il costo del prossimo posto a scendere, non il totale**. Con 1
o 2 posti vale zero perché la seconda e la terza persona sono gratuite — la riga «1 · 2 · 3 → 2,99 €» della
tabella dello use case comprime tre casi in uno e dice il costo del *primo posto a pagamento*, cioè è esatta
per 3 posti.

## 6. Il listino si legge dall'interfaccia di programmazione, con qualunque ruolo

```bash
curl -sk https://api.local.appgrove.app/api/platform/v1/seat-pricing \
  -H "authorization: Bearer $TOKEN_OWNER" | python3 -m json.tool
```

**Atteso**: `200` con `"currency": "EUR"`, `"effectiveFrom": "1970-01-01T00:00:00Z"` e cinque fasce, l'ultima
con `"toSeat": null` e `"unitPriceCents": 49`.

```bash
curl -sk -o /dev/null -w '%{http_code}\n' https://api.local.appgrove.app/api/platform/v1/seat-pricing \
  -H "authorization: Bearer $TOKEN_MEMBER"
```

**Atteso**: `200`. **Non è un errore**: leggere quanto costa un posto non richiede il diritto di comprarlo —
la pagina dei prezzi la vede chiunque. Il divieto vero sta a valle (comprare è di UC 0103, cambiare le
tariffe è dell'amministratore di piattaforma in UC 0105).

```bash
curl -sk -o /dev/null -w '%{http_code}\n' https://api.local.appgrove.app/api/platform/v1/seat-pricing
```

**Atteso**: `401`. Senza token il listino non si legge.

## 7. Quanti posti occupa l'account Acme, e quanto dovrebbe

**Stato di partenza dichiarato.** I numeri di questo paragrafo dipendono da chi c'è nell'account, quindi
prima di leggerli bisogna sapere da dove si parte. Su una banca dati **appena seminata**
(`./app-stop.sh --wipe && ./app-start.sh`) l'account Acme ha **3 persone e 2 inviti in attesa = 5 posti**.
Se invece sono già girate la suite di piattaforma o un'altra guida, ci sono più persone e più inviti: **non
è un difetto**, è lo stato che qualcun altro ha lasciato. Per questo il paragrafo verifica la **regola**,
non il numero 5.

La regola è una sola: occupa un posto chi ha un'**appartenenza viva** all'account, più ogni **invito in
attesa non scaduto**. L'owner rientra per costruzione, le persone sospese rientrano, le persone rimosse e
gli inviti scaduti, revocati, rifiutati o **accettati** no.

**7.1 — chi occupa i posti, uno per uno.** Si guarda l'elenco e si controlla la regola a mano:

```bash
docker compose -f dev/docker-compose.yml exec -T postgres psql -U appgrove -d appgrove -c \
  "select 'persona' as tipo, i.email, m.status, m.role
     from platform.membership m
     join platform.identity i on i.id = m.identity_id
    where m.tenant_id = 'a0000000-0000-4000-8000-000000000001' and m.deleted_at is null
   union all
   select 'invito' as tipo, inv.email, inv.status, ''
     from platform.invitations inv
    where inv.tenant_id = 'a0000000-0000-4000-8000-000000000001' and inv.deleted_at is null
    order by tipo, email;"
```

**Atteso**: ogni riga `persona` conta un posto, qualunque sia il suo `status`; fra le righe `invito`
contano **solo** quelle con `status = pending` **e** scadenza futura. Le righe `invito` con
`status = accepted` **non** contano — e non è una dimenticanza: quella persona compare già fra le
`persona`, e contarla due volte la farebbe pagare due volte. È il controllo che vale la pena fare con
l'occhio, perché è l'unico modo di vedere che le due liste non si sovrappongono.

**7.2 — il conteggio e il dovuto, calcolati insieme.** Questa interrogazione applica la regola e poi il
listino vigente, quindi è **coerente con qualunque stato** si sia trovato al 7.1:

```bash
docker compose -f dev/docker-compose.yml exec -T postgres psql -U appgrove -d appgrove -c \
  "with occupati as (
     select count(*) as posti from (
       select m.id from platform.membership m
        where m.tenant_id = 'a0000000-0000-4000-8000-000000000001' and m.deleted_at is null
       union all
       select i.id from platform.invitations i
        where i.tenant_id = 'a0000000-0000-4000-8000-000000000001' and i.deleted_at is null
          and i.status = 'pending' and i.expires_at > now()
     ) tutti
   ), fasce as (
     select b.from_seat, b.to_seat, b.unit_price_cents
       from platform.seat_pricing_band b
       join platform.seat_pricing_version v on v.id = b.version_id
      where b.deleted_at is null and v.deleted_at is null
        and v.effective_from <= now()
   )
   select o.posti,
          sum(greatest(least(o.posti, coalesce(f.to_seat, o.posti)) - f.from_seat + 1, 0) * f.unit_price_cents) as dovuto_centesimi,
          to_char(sum(greatest(least(o.posti, coalesce(f.to_seat, o.posti)) - f.from_seat + 1, 0) * f.unit_price_cents) / 100.0, 'FM999990.00') as dovuto_euro
     from occupati o cross join fasce f group by o.posti;"
```

**Atteso**: `posti` = il numero di righe contate al 7.1 secondo la regola, e `dovuto_euro` = il valore che
la tabella del §4 dà per quel numero di posti. Su seme pulito: `5 posti → 5.98 €`. Dopo la suite di
piattaforma, per esempio: `9 posti → 17.94 €` (nove posti, tre gratuiti, sei a 2,99 €).

**Nota sull'assenza di un'operazione di rete per questo numero**: è voluta. Il conteggio esiste nel servizio
(`SeatCount`) ma nessun percorso di prodotto lo espone ancora: il riquadro dei posti con il dovuto è di
UC 0103. Nei collaudi automatici il conteggio è esercitato attraverso un endpoint che vive **solo** nel
classpath di test.

## 8. Il riavvio non duplica il listino

Si riavviano le sole applicazioni, lasciando in piedi Postgres (così i dati restano quelli di prima —
`--apps-only` non tocca i container, e nemmeno `app-stop.sh` senza `--wipe` cancella i volumi):

```bash
./app-stop.sh --apps-only && ./app-start.sh --no-build --no-spa
```

Poi:

```bash
docker compose -f dev/docker-compose.yml exec -T postgres psql -U appgrove -d appgrove -c \
  "select count(*) as versioni from platform.seat_pricing_version where deleted_at is null;"
```

**Atteso**: `versioni = 1`, come prima del riavvio. È il punto in cui un caricamento non idempotente si
vedrebbe: moltiplicherebbe i listini a ogni riavvio, e con due versioni alla stessa decorrenza la domanda
«quale vigeva quel giorno» non avrebbe più risposta.

**In locale la semina gira allo startup; in produzione no**, e la differenza è deliberata: l'artefatto di
spedizione deve arrivare in ascolto senza toccare la banca dati. In produzione la prima versione nasce dal
passo di distribuzione `seed-seat-pricing`, cablato nelle due pipeline dopo il `migrate`. Che il comando
esista e sia idempotente si vede così:

```bash
grep -n "seed-seat-pricing" .github/workflows/deploy-test.yml .github/workflows/release-prod.yml
grep -n "seat-pricing.seed-on-startup" services/core/src/main/resources/application.properties
```

**Atteso**: un passo per ciascuna delle due pipeline, e la proprietà `appgrove.seat-pricing.seed-on-startup`
a `false` con l'override `%dev` a `true`.

## 9. Che cosa NON deve essere cambiato

| # | Azione | Risultato atteso |
|---|---|---|
| 9.1 | Aprire il backoffice e guardare il menu laterale | Nessuna voce nuova: questa change non ha schermate. |
| 9.2 | `curl -sk https://api.local.appgrove.app/api/platform/v1/me/catalog -H "authorization: Bearer $TOKEN_OWNER" \| python3 -m json.tool \| head -20` | Il catalogo delle applicazioni è quello di prima: i posti **non** sono un'applicazione e non compaiono in vetrina. La voce di catalogo di piattaforma dei posti arriva con UC 0103. |
| 9.3 | `docker compose -f dev/docker-compose.yml exec -T postgres psql -U appgrove -d appgrove -c "select count(*) from platform.app where deleted_at is null;"` | Lo stesso numero di applicazioni di prima della change (il seme ne ha cinque fra reali e fittizie). |

---

## Riepilogo di che cos'è «passato»

- §1, §2, §3, §4, §5, §6, §7, §8, §9.2, §9.3 → **non visivi**, **eseguiti** il 2026-08-22 (vedi
  l'intestazione per le due correzioni che l'esecuzione ha prodotto).
- §9.1 → **visivo**, resta allo sviluppatore (ed è una verifica di *assenza*: non deve essere comparso nulla).
