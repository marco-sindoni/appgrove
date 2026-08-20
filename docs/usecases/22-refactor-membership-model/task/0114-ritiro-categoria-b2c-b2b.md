# Piano di lavoro — UC 0114 · Ritiro della categoria B2C/B2B

**Storia**: [0114](../story/0114-ritiro-categoria-b2c-b2b.md) · **Aree toccate**: `services/core`, `tools/`, `frontend/apps/admin`, documentazione
**Dimensione stimata**: media, quasi tutta meccanica · **Prerequisiti**: UC 0099, UC 0101 (verifica strutturale verde), UC 0112

## Prima di iniziare: la condizione di sicurezza

Questa storia **toglie** dalle applicazioni le costanti dei ruoli di piattaforma. Se un endpoint non fosse
ancora passato al varco per applicazione, resterebbe **senza protezione**. Quindi, come primo atto:

```bash
cd services && mvn -B test -Dtest='*OperationsContractTest'   # la verifica strutturale di UC 0101
```

Se non è verde, **si ferma qui**: prima si chiude UC 0101, poi si rimuove.

## Passo 1 — Banca dati

**File nuovo di migrazione**: elimina `platform.app.user_model`. Nessun dato da salvare altrove: nessuno lo
consuma. Il numero della migrazione va riverificato al momento (le storie 0098, 0102, 0103, 0104, 0109,
0115 ne aggiungono ciascuna una).

## Passo 2 — Core

| File | Che cosa |
|---|---|
| `catalog/AppUserModel.java` | **eliminato** |
| `catalog/App.java` | campo, colonna e lettore rimossi |
| `catalog/PricingDefinition.java` | `userModel` fuori dal record e dal costruttore ridotto |
| `catalog/PricingSyncService.java` | fuori dalla `insert … on conflict` (righe 57-60) e dal legame dei parametri (riga 148) |
| `catalog/AppStatusService.java` | fuori dal record del risultato e dalle due interrogazioni |
| `platform/AdminResource.java` | fuori dalla selezione e dalla costruzione della vista |
| `platform/AdminDtos.java` | `AppView` senza `userModel` |
| `resources/META-INF/openapi/openapi.yaml` | proprietà rimossa |

**Tolleranza sul caricamento**: il lettore dei listini deve **ignorare** un `userModel` residuo invece di
fallire. Verificare come si comporta il lettore su un campo sconosciuto: se è severo, va reso tollerante
**prima** di togliere il campo dai file, altrimenti l'avvio si rompe a metà lavoro.

## Passo 3 — Listini

Rimozione della riga `userModel:` da: `services/core/src/main/resources/pricing/fatture.yaml`, `…/crm.yaml`
e le quattro fixture (`notes`, `teams`, `legacy` e l'indice se lo cita).

**Modifica**: `tools/pricing-change/lib/pricing.mjs` riga 174 — `userModel` esce dall'elenco dei campi
obbligatori. Senza questo, la skill dei cambi di prezzo rifiuterebbe ogni listino valido.

## Passo 4 — Generatore di applicazioni

| File | Che cosa |
|---|---|
| `tools/new-application/generate.mjs` | opzione `--user-model` rimossa (righe 115, 135, 174-175) e testo d'aiuto |
| `tools/new-application/lib/context.mjs` | segnaposto `USER_MODEL`, `USER_MODEL_NOTE`, `ROLES_ALLOWED`, `ROLES_EXTRA_CONSTANTS` rimossi (righe 214-228) |
| `templates/service/.../Roles.java` | **il modello sparisce**: i ruoli di piattaforma non servono più a un'applicazione (UC 0099) |
| `templates/service/.../ItemResource.java` · `QuotaResource.java` | `@RolesAllowed(@@ROLES_ALLOWED@@)` → varco per applicazione (già fatto da UC 0112: qui si verifica che non resti nulla) |
| `templates/pricing/*.yaml` | riga `userModel:` rimossa |
| `.claude/skills/new-application/step-01-identity.md` | la domanda 2 (modello utenti) **sostituita** dalla domanda sull'ambito dei dati di UC 0115 |

## Passo 5 — Console di amministrazione

- `frontend/apps/admin/src/pages/Apps.tsx` — colonna rimossa (riga 90 e intestazione);
- `frontend/apps/admin/src/pages/admin.test.tsx` e `frontend/apps/admin/e2e/admin.spec.ts` — i dati finti
  con `userModel: 'b2b' | 'b2c'` puliti. Sono la prova che la confusione era già entrata: vale la pena
  citarli nel messaggio di commit;
- `frontend/packages/api-client/src/schema.ts` — rigenerato dall'interfaccia dichiarata.

## Passo 6 — Documentazione: solo dove significa «modello utenti»

**Da aggiornare**:

| File | Che cosa |
|---|---|
| `docs/01-architettura.md` | §31 (`single-user` come modello di tenancy) e §80 (alternativa scartata): la distinzione non è più del prodotto |
| `docs/02-auth-sicurezza.md` | i due punti sull'«app demo B2B/multi-utente» |
| `docs/usecases/11-apps/0051-app1-backend.md` · `0052-…` · `0054-…` | **nota in testa**, nessuna rinomina |
| `docs/usecases/06-frontend/0060-…` | menzione da rileggere |
| `docs/usecases/_TEMPLATE.md` | la riga «owner/admin/member B2B» degli attori |
| `docs/03-frontend.md` · `docs/10-testing.md` · `docs/11-developer-experience.md` | verificare il senso caso per caso: alcune menzioni sono commerciali |

**Da NON toccare** (senso giuridico e commerciale, resta valido):
`docs/13-compliance-privacy.md`, `docs/compliance/manifests/*.yaml`, `docs/compliance/ropa.*.md`,
`docs/compliance/breach-runbook.md`, `docs/_COMMERCIALISTA.md`, `docs/_REVISIONE-LEGALE.md`, i mockup
storici in `docs/frontend-design/`.

Regola operativa per non sbagliare: **prima di modificare una riga, chiedersi se parla di *quante persone
usano l'app* oppure di *chi è il cliente e chi è titolare dei dati*.** Il primo caso si aggiorna, il
secondo no.

## Passo 7 — Verifica di completezza

```bash
grep -rn "user_model\|userModel\|AppUserModel\|single_user\|multi_user" \
  --include='*.java' --include='*.ts' --include='*.tsx' --include='*.mjs' \
  --include='*.yaml' --include='*.sql' . | grep -v node_modules | grep -v '/target/'
```

Deve restituire **niente**. È la verifica che chiude la storia.

## Verifica finale

```bash
./run-tests.sh          # intero: la storia tocca sei aree
```

## Trappole note

1. **Rimuovere i ruoli dalle applicazioni prima che il varco sia in vigore** lascia endpoint scoperti: la
   condizione di sicurezza in testa a questo piano non è formale.
2. **Il lettore dei listini severo su un campo di troppo**: se non lo si rende tollerante prima, l'avvio
   si rompe nel mezzo del lavoro.
3. **La tentazione di «allineare anche i documenti di conformità»**: là «B2B» significa un'altra cosa.
   Cambiarlo introdurrebbe un errore in documenti che hanno valore legale.
4. **Il campo nel client generato** non si modifica a mano: si rigenera.
