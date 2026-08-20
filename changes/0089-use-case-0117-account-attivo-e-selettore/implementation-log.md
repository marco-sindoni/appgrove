# Log di implementazione — 0089 · Account attivo nella sessione e selettore (UC 0117)

**Requisiti**: [requirements.md](requirements.md) · **Decisioni**: [decisions.json](decisions.json) ·
**Verifica manuale**: [how-to-test.md](how-to-test.md)
**Modalità**: fast · **Suite**: `./run-tests.sh` completa verde prima del commit

## Che cosa è stato fatto

### 1. Dove vive l'account attivo — `V18__active_account.sql`

Colonna `active_membership_id` (annullabile) su `platform.identity`, più la tabella
`platform.active_account_audit` per la traccia dei cambi. Il commento della colonna dice **perché** non
è un attributo del gruppo di utenti Cognito, così nessuno riprova la via «più elegante» rischiando di
ricreare il gruppo.

Due dettagli di integrità che non sono cosmetici, e uno dei due è emerso da un collaudo rosso:

- `ON DELETE SET NULL` sul riferimento all'appartenenza: la cancellazione di un account (UC 0033)
  elimina **fisicamente** le appartenenze, e senza questa regola la purga si romperebbe contro il
  vincolo ogni volta che qualcuno stava lavorando in quell'account;
- `ON DELETE CASCADE` sul riferimento alla persona nella traccia: non è una prova di adempimento come
  `gdpr_purge_audit` ma una traccia operativa il cui unico soggetto è la persona, e se ne va con lei.

### 2. La regola, scritta una volta e attuata due

`ActiveAccount.choose` in `services/commons` — **non** in `services/core` come indicava il piano:
`services/auth` non dipende da `core` e il percorso di accesso non deve acquisire una dipendenza di
rete. `commons` è l'unico posto raggiungibile da entrambi.

L'esito è un tipo sigillato a **tre** valori e non un `Optional`: «nessuna appartenenza attiva» e «più
appartenenze e nessuna scelta valida» sono casi diversi e vanno detti diversamente. La gemella Python
(`choose_active_account` in `handler.py`) applica la stessa tabella; la query non fa più `LIMIT 1`,
perché per riverificare servono **tutte** le appartenenze attive.

Il ripiego di UC 0116 («la più antica») è sparito da entrambe **nello stesso commit**, che era la
condizione posta dal rimando.

### 3. Il cambio di account

`GET /api/platform/v1/me/memberships` e `POST /api/platform/v1/me/active-account` (204), i percorsi già
dichiarati nella tabella di mappatura dei prototipi dell'epica. Il cambio **non** restituisce token: il
rinnovo passa dal percorso esistente, così l'account si stabilisce in un posto solo. Rifiuto **404** per
un account che non è proprio — non 403, che rivelerebbe la sua esistenza.

`activeAccountId` nella risposta è l'account attivo **effettivo** (calcolato con la stessa funzione che
compone il token), non il valore grezzo: è ciò che permette all'interfaccia di accorgersi che l'account
è cambiato in un'altra scheda.

### 4. Il selettore e l'avviso

`shell/AccountSwitcher.tsx` sotto il blocco del marchio: nome sempre visibile, comando **non reso**
con una sola appartenenza, cambio che **ricarica** l'applicazione (è il ricaricamento a rinnovare il
token). `shell/AccountChangedBanner.tsx` per la scheda rimasta indietro. Nessuna etichetta di ruolo, in
deroga esplicita ai prototipi: la vieta §4.6 della storia, e la distinzione owner/collaboratore arriva
con UC 0107.

Il contratto shell↔modulo (`ShellContext`) **non** è stato toccato: nessun modulo ha bisogno
dell'elenco delle appartenenze, e allargare un'interfaccia pubblica senza un consumatore è debito.

### 5. Collaudi

| Livello | Dove | Che cosa prova |
|---|---|---|
| unità | `commons/.../ActiveAccountTest` | la tabella dei casi sulla funzione pura, manomissione compresa |
| unità | `pre_token_gen/test_handler.py` | la **stessa** tabella sulla gemella Python + i casi passanti dal database |
| integrazione | `auth/.../ActiveAccountTokenTest` | la tabella vista attraverso i token realmente emessi; il rinnovo che porta l'account nuovo; il token precedente che resta valido per il suo |
| integrazione | `core/.../MeMembershipsApiTest` | elenco, cambio, traccia, idempotenza, 404 per account non proprio e per appartenenza revocata |
| componente | `backoffice/.../AccountSwitcher.test.tsx` | assenza del selettore con una sola appartenenza; cambio che ricarica; errore che non finge |
| e2e L2 | `backoffice/e2e/accountSwitch.spec.ts` | `L2-ACCOUNT-SWITCH` — finto server **con stato**, altrimenti il ricaricamento non proverebbe nulla |
| e2e piattaforma | `platform-e2e/journeys/J-ACCOUNT-SWITCH.spec.ts` | `J-ACCOUNT-SWITCH` — la stessa persona in due account, menu che seguono l'account, ritorno, traccia |

La prova di sicurezza manomette la colonna puntandola all'appartenenza di **un'altra persona**: con un
identificativo inventato avrebbe provato la chiave esterna, non la riverifica.

### 6. Documenti e registri

- [docs/02 §10](../../docs/02-auth-sicurezza.md): la tabella dei casi e — come la storia pretendeva — il
  legame **scritto** fra durata dell'access token e ritardo massimo di una revoca;
- [copertura-e2e.yaml](../../docs/testing/copertura-e2e.yaml): 0117 fra gli use case con superficie, due
  percorsi nuovi, esenzione rimossa;
- manifesto dati + registro dei trattamenti: `identity.active_membership_id` dichiarato come preferenza
  (coerenza con `identity.locale`), classificazione **minore**;
- storia 0117 → ✅ implementata; epica E22.5 aggiornata; rimandi scritti in UC 0118, UC 0033, UC 0107.

## Che cosa NON è stato fatto, e dove è tracciato

La **schermata** per scegliere l'account quando non si ha una sessione: l'esito è implementato,
tipizzato e a chiusura, con un `409` comprensibile al posto di «credenziali non valide», ma la superficie
appartiene a **UC 0118** — è quella storia a creare le seconde appartenenze (finché non esiste, il caso
non è raggiungibile da nessun percorso di prodotto) e a decidere dove atterra chi entra. Costruirla qui
avrebbe richiesto rendere navigabile una sessione priva del claim dell'account: un allargamento del
percorso più delicato del prodotto per un caso che nessuno può raggiungere.

Mitigazione applicata perché il rimando fosse accettabile: ogni appartenenza creata dal servizio di
autenticazione imposta anche l'account attivo. Così l'esito «scegli» richiede almeno **tre** appartenenze
con la attiva revocata — con due, revocata l'attiva, ne resta una sola e la regola la scegli da sé.

## Che cosa ha trovato la prima esecuzione della suite completa

Il lavoro è stato interrotto **prima del commit**, con la suite completa mai portata a termine. La sua
prima esecuzione reale ha trovato tre cose, tutte nel perimetro della storia e tutte corrette qui:

1. **backend rosso** — `PlatformGdprContractTest.exportCoversEveryManifestEntity`: il campo
   `identity.active_membership_id` era dichiarato nel manifesto dati ma non compariva nell'export GDPR.
   Il collaudo di contratto pretende che ogni voce del manifesto sia coperta, e faceva bene. È stato
   aggiunto all'export **ristretto a questo account** (`case when i.active_membership_id = m.id then
   m.id end`): il valore grezzo può puntare all'appartenenza in un **altro** account, e restituirlo
   rivelerebbe l'esistenza di quell'account — esattamente ciò che l'export non deve fare (UC 0116 §8).
   Il rosso ha quindi impedito una fuga di informazione, non un dettaglio formale;
2. **percorso di piattaforma rosso** — `J-ACCOUNT-SWITCH`, per difetti **del collaudo** e non del
   prodotto: non attendeva il ricaricamento (e leggeva ancora il documento vecchio, cioè il nome
   dell'account di prima) e non attraversava il **gate legale del nuovo account**, che è pendente
   perché l'accettazione dei documenti è per **account** e non per persona. Sistemato il primo, è
   emerso il secondo strato: il gate è **fail-open mentre il suo stato carica**, quindi per un istante
   la shell è navigabile e il passo condiviso `acceptLegalGateIfPresent` — che chiede «gate oppure
   shell» una volta sola — perde la corsa e lascia il gate aperto un attimo dopo. Il percorso ora
   attende la **risposta che decide** il gate (`/me/legal/status`) e poi insiste sull'esito; tre
   esecuzioni consecutive verdi. Il passo condiviso non è stato toccato — gioverebbe a tutti i tredici
   percorsi ma non tutti gli accessi arrivano alla shell nello stesso modo — e il rimando è in
   [docs/_BACKLOG.md](../../docs/_BACKLOG.md);
3. **conservazione dichiarata e non collaudata** — `platform.active_account_audit` era nell'elenco
   dello sweeper ma non nel suo collaudo: togliere la tabella dall'elenco non avrebbe fatto diventare
   rosso nulla. Aggiunta, con l'obbligo scritto nel javadoc per le tabelle future.

Ripulita anche una chiave di traduzione senza consumatore (`accountSwitch.active` nelle 5 lingue):
l'account attivo nel menu è marcato dall'icona di spunta e da `aria-current`.

**Rimandi aggiunti dopo l'esecuzione**, ognuno dove ha un proprietario: **UC 0056** (il gate legale
ricompare entrando in un account nuovo — esito atteso, ma la schermata non spiega che chiede
l'accettazione *per quell'account*), **UC 0011** via i punti aperti di **UC 0116** (il ri-seed delle
appartenenze stampa un errore di indice unico e non aggiorna le righe: difetto della change `0088`) e
**docs/_BACKLOG.md** (il passo condiviso della suite di piattaforma perde la corsa col fail-open del
gate). Nessuno dei tre è stato corretto qui: non appartengono a questa storia.

## Esito della suite

`./run-tests.sh` (completa, senza parametri): **verde** — tutte le otto aree, `J-ACCOUNT-SWITCH` e
`L2-ACCOUNT-SWITCH` inclusi.
