# UC 0091 — Batteria journey end-to-end lato utente (copertura codebase esistente)

**Area**: 20-test-e2e-piattaforma · **Fase**: evo · **Stato**: 🟢 scritto (evo, da implementare)
**Dipendenze**: UC 0090 (fondamenta suite), UC 0024 (checkout), UC 0027 (enforcement entitlement+quota), UC 0059 (membri & inviti UI), UC 0028 (self-service abbonamento), UC 0033 (self-service GDPR), UC 0056 (ri-accettazione legale runtime)
**Fonte decisioni**: #10 (testing)
**Ultimo aggiornamento**: 2026-08-01

## 1. Obiettivo / Scope

Costruire, sulle fondamenta di UC 0090, la **batteria dei journey lato utente** che copre le funzionalità già
esistenti della piattaforma — dal primo acquisto fino ai diritti sui dati — su stack reale, con email vere (Mailpit)
e pagamenti sul fake Paddle. Al termine, l'esecuzione di `./run-tests.sh platform` dà la certezza di **non avere
regressioni sui percorsi utente end-to-end** della codebase attuale.

**Incluso**: i journey J-BUY … J-LEGAL descritti sotto (ognuno = tenant fresco, indipendente, parallelizzabile).
**Escluso**: i journey amministratore e i guasti di piattaforma (UC 0092); il registro di copertura (UC 0093);
nuove funzionalità di prodotto (i journey testano l'esistente, non lo estendono).

**Criterio di riparto con l'L2 esistente** (vincolante, per non duplicare): la suite di piattaforma copre i
**percorsi integrati reali** (email, webhook, entitlement veri, effetti su DB); l'L2 con backend simulato resta la
sede degli **stati difficili da produrre** con uno stack vero (errori 500 puntuali, risposte malformate, ritardi
estremi). Un caso coperto in piattaforma non va duplicato in L2 e viceversa, salvo il percorso felice minimo.

## 2. Attori & ruoli

- **Owner** del tenant (creato dal journey), **member** invitati, utente B2C singolo — tutti sintetici.
- **Sistemi**: stack della suite (UC 0090), Mailpit, fake Paddle + webhook sintetici firmati (UC 0023).

## 3. Precondizioni

UC 0090 implementato (orchestratore, helper, area `platform`). Ogni journey parte da **zero**: crea il proprio
tenant col helper `tenant()` (per i journey che non testano la registrazione stessa, la creazione può avvenire via
API per velocità — decisione di implementazione).

## 4. Flusso principale — i journey

Ogni journey elenca i passi principali e le **assert esterne** (email/DB/webhook) che lo distinguono dall'L2.

**J-BUY — Acquisto e attivazione**
1. Tenant fresco senza app → catalogo → scelta tier → overlay fake Paddle → completamento.
2. Il webhook (sintetico firmato, stessa pipeline reale: ingest → coda → consumer) materializza la subscription.
3. Polling post-checkout → "attivata"; la sidebar mostra l'app tra YOUR APPS; il modulo si monta e risponde.
4. Assert DB: subscription e entitlement col `tenant_id` giusto; nessuna riga per altri tenant.

**J-QUOTA — Uso dell'app e limite quota**
1. Tenant con app attivata (via J-BUY helper) → core-loop dell'app #1 (`fatture`): crea elementi fino al limite del
   tier; il banner consumo/limite avanza.
2. Al superamento: 429 con invito all'upgrade → upgrade di tier (fake Paddle) → il webhook aggiorna → la creazione
   riesce di nuovo. Assert: il contatore quota su DB riflette il consumo reale.

**J-MEMBERS — Inviti e ruoli (B2B)**
1. Owner con app B2B (`crm`) → invita un membro → **email di invito realmente ricevuta** (Mailpit) → il membro
   apre il link in una **seconda sessione browser**, imposta la password, entra col ruolo member.
2. Assegnazione posto (seat) → a posti esauriti: 429 + invito upgrade.
3. Cambio ruolo e revoca dall'owner; il membro revocato perde l'accesso (assert dalla sua sessione).
4. Protezioni: l'ultimo owner non può auto-degradarsi (assert UI).

**J-SUB — Ciclo di vita abbonamento**
1. Tenant con app attiva → downgrade programmato a fine periodo (self-service) → stato mostrato.
2. Disdetta → fine periodo simulata (webhook sintetico di scadenza) → 402 con azioni "riattiva/esporta"; il modulo
   non è più raggiungibile ma i dati non sono cancellati (assert DB).
3. Riattivazione → accesso ripristinato, dati intatti.

**J-PWD — Credenziali e secondo fattore**
1. Reset password: richiesta → risposta neutra → **email di reset ricevuta** → link → nuova password → accesso.
2. Attivazione 2FA: enroll → secret → codici a tempo generati dal helper `totp()` → login con secondo fattore.
3. Assert: il vecchio refresh non vale più dopo il reset (sessioni invalidate).

**J-PRIVACY — Diritti sui dati**
1. Rettifica nome (art. 16) → visibile ovunque.
2. Export account: avvio → job asincrono reale (coda) → link con scadenza → **download effettivo** e validazione
   del contenuto (i dati del tenant, solo del tenant).
3. Recesso per-app: esporta → conferma → recesso; eliminazione account: conferma → grace con scadenza → annulla.
4. Assert DB su stati dei job e assenza di effetti cross-tenant.

**J-LEGAL — Ri-accettazione legale runtime**
1. Si pubblica (via leva di test dell'ambiente) una nuova versione major dei documenti → al login la schermata
   bloccante; la pagina "I miei dati" resta raggiungibile (esenzione GDPR).
2. Lettura documento → spunte → accettazione → ingresso; assert DB sull'accettazione registrata (chi, cosa, quando).

## 5. Flussi alternativi / edge / errori

- **Tempo simulato** (J-SUB, J-PRIVACY grace): la leva raccomandata sono i **webhook sintetici di stato** (già
  scenari lifecycle di UC 0023) e, dove non bastano, una leva di test esplicita dei servizi in profilo `dev`
  (mai nei profili di spedizione). La scelta puntuale è della change; va tracciata in `decisions.json`.
- **Ordine/parallelismo**: nessun journey dipende da un altro; helper condivisi sì, stato condiviso no.
- **Fallimenti parziali**: ogni journey lascia trace/screenshot; il verdetto elenca i journey rossi per ID.

## 6. Risorse & runbook

- File in `tools/platform-e2e/journeys/` — uno per journey, ID stabili (`J-BUY`, `J-QUOTA`, `J-MEMBERS`, `J-SUB`,
  `J-PWD`, `J-PRIVACY`, `J-LEGAL`) riusati dal registro di copertura (UC 0093).
- Estensioni ai helper di UC 0090 dove servono (seconda sessione browser, download, leve di stato abbonamento).
- Runbook: come rilanciare un singolo journey; tabella journey ↔ funzionalità coperta.

## 7. Dati toccati

Solo dati sintetici nel database usa-e-getta della suite (come UC 0090). L'export di J-PRIVACY scarica dati
sintetici; nessun manifesto GDPR da toccare.

## 8. Permessi & gate

I journey **verificano** i gate reali: entitlement (J-BUY, J-SUB), quota flow/stock (J-QUOTA, J-MEMBERS), ruoli
(J-MEMBERS), esenzione dei diritti GDPR dai blocchi (J-LEGAL, J-PRIVACY). Invarianti: ogni journey che crea dati
chiude con l'assert leak-detector sul `tenant_id` (#10 dec. 13).

## 9. Requisiti di test

- Tutti i 7 journey verdi in locale e CI, in parallelo, tempo totale della batteria compatibile col target di
  UC 0090 (<10 minuti; se sforato, motivare e aggiornare il target nella change).
- Doppia esecuzione consecutiva verde (idempotenza; nessun residuo tra run).
- L2 esistente: rimosse le eventuali duplicazioni nate col criterio di riparto (§1), senza perdita di copertura
  (la mappa di cosa vive dove confluisce nel registro di UC 0093).

## 10. Riferimenti & Definition of Done

- **Decisioni**: #10; UC 0023 (scenari lifecycle webhook); UC 0027 (semantica 402/429); UC 0033 (flussi GDPR);
  UC 0056 (gate legale).
- **DoD**:
  1. i 7 journey esistono, sono verdi e girano nel comando unico;
  2. ogni journey ha ID stabile e descrizione (input per il registro di UC 0093);
  3. criterio di riparto piattaforma/L2 applicato e documentato;
  4. `_INDEX.md` aggiornato dalla change.

## Punti aperti / decisioni differite

- **Journey di localizzazione end-to-end** (interfaccia + email nella stessa lingua per le 5 lingue): rimandato —
  la parità cataloghi è già coperta da unit (i18n) e L2; un journey per lingua quintuplicherebbe il tempo. Se ne
  riparla quando UC 0060 (localizzazione UI app) matura; il registro di UC 0093 lo elenca come `da-coprire`.
- **App future**: ogni nuova app porta il proprio journey core-loop via scaffolding (UC 0094), non si accresce
  questo UC.
