# Implementation Log — Change 0094: Tre presidi che non proteggevano

**Branch**: `change/0094-scadenza-copia-diritti-e-collaudi`
**Aree**: `services/commons` · `services/{auth,core,crm,fatture}` · `tools/platform-e2e` ·
`tools/new-application` · `docs/`
**Data**: 2026-08-22
**Modalità**: autopilot
**Use case sorgente**: nessuno (change ad-hoc; il difetto 1 ha come proprietario **UC 0046**)

## File modificati

| File | Cosa |
|---|---|
| `services/commons/.../projection/EntitlementProjectionStore.java` | `ProjectedEntitlement` acquisisce `usable(maxAge, now)`, gemello di quello della copia del ruolo. |
| `services/commons/.../projection/ProjectedEntitlementService.java` | La condizione di uso della copia include l'**età**; nuova chiave di durata (60 s); il motivo della rilettura distingue **tre** casi. Documentazione della postura riscritta: dice che una scelta deliberata è stata rovesciata, e perché. |
| `services/{fatture,crm}/src/main/resources/application.properties` | `appgrove.entitlement.projection.max-age=60s`. |
| `services/{auth,core,crm,fatture}/src/test/resources/application.properties` | `quarkus.http.test-port=0` — porta di test effimera. |
| `tools/new-application/templates/service/src/{main,test}/resources/application.properties` | Le stesse due chiavi nel modello: un'applicazione nuova non nasce col difetto. |
| `services/fatture/src/test/java/.../TestProjection.java` | Nuovo `ageBySeconds`: invecchia la riga senza marcarla. |
| `services/fatture/src/test/java/.../EntitlementProjectionTest.java` | Tre collaudi della scadenza; tetto di quota alzato nella preparazione (difetto latente, sotto). |
| `tools/platform-e2e/playwright.config.ts` | Resoconto in **formato dati** accanto a quello leggibile. |
| `tools/platform-e2e/run.sh` | Passo 5bis: i percorsi **instabili** rendono la suite rossa, e vengono **nominati**. |
| `docs/_BACKLOG.md` | Le tre voci chiuse (una a metà, con la causa residua motivata). |

## Cosa è stato fatto

**1. La copia dei diritti scade.** La riga si usa se non è marcata **e** non è più vecchia della durata
massima (60 secondi, come la copia del ruolo). Oltre la durata si rilegge dalla fonte; se la fonte non
risponde si continua con l'ultima verità nota — il comportamento a fonte irraggiungibile non cambia.

**2. La porta di test è effimera.** `quarkus.http.test-port=0` nei quattro servizi e nel modello: cade la
collisione con `fatture` sulla 8081 e anche quella, mai notata, fra due esecuzioni Maven in parallelo.

**3. Un percorso instabile rende la suite rossa.** Playwright emette il resoconto in formato dati,
l'orchestratore conta gli instabili e li nomina. Il ritentativo resta: si è tolto il condono, non il tentativo.

## Decisioni prese

Quindici voci in [decisions.json](decisions.json). Le tre che vale la pena leggere:

**Voce 7 — non era una dimenticanza.** L'assenza di scadenza era una scelta **deliberata e argomentata nel
codice**: «è l'evento — non il tempo — a dire che qualcosa è cambiato», e una scadenza sarebbe stata «un blocco
a orologeria senza aggiungere sicurezza reale». L'argomento è **corretto per i cambiamenti che generano un
evento** e falso per tutti gli altri. Il commento non è stato cancellato ma riscritto: chi legge deve sapere che
qui c'era una tesi opposta e cosa l'ha smentita. E deve sapere perché il timore non si materializza: **scadere
significa rileggere, non negare**.

**Voce 8 — tre motivi, non due.** La rilettura distingue «assente», «da rinfrescare» (evento) e «scaduta»
(tempo). Se le riletture fossero quasi tutte per scadenza, vorrebbe dire che gli eventi non arrivano — e con due
soli motivi quel guasto resterebbe invisibile **proprio grazie al rimedio introdotto qui**.

**Voce 9 — la prova che i test provano.** I due collaudi della scadenza sono stati eseguiti **contro il codice
vecchio**: falliscono. Ripristinata la correzione: passano. Un test che non è mai stato visto fallire non
dimostra nulla — la stessa regola che la change 0093 ha imposto alle guide, applicata ai test.

## Invarianti appgrove

- **Tenant ID solo dal JWT** — invariato: la copia è già cercata con l'account preso dal token (fallimento
  chiuso se manca) e la change aggiunge una condizione sull'**età** della riga, non sulla sua appartenenza.
- **Filtro per riga sull'account** — invariato: la ricerca resta vincolata ad account e applicazione, e
  l'aiutante di test che invecchia le righe filtra anch'esso per account.
- **Logging strutturato** — la misura «servita copia vecchia» esisteva e resta; nessuna registrazione nuova.
- **Modulo Terraform** — non toccato.

## Note per il revisore

**Un difetto latente trovato per caso, e corretto.** `EntitlementProjectionTest` creava fatture sullo stesso
conto senza azzerarle mai, e viveva a **due fatture dal tetto** gratuito di dieci al mese. Aggiungendo tre
collaudi l'ho superato, e i test cadevano con «429 quota esaurita»: un rosso che non parla di ciò che il test
verifica. Ho alzato il tetto nella preparazione, perché quella classe collauda la **copia locale** e non la
quota — per quella c'è `QuotaTest`. Chi aggiungerà un test domani non deve più contare le fatture.

**Ho corretto una causa su due, e la seconda cambia natura.** Nessun evento di invalidazione nasce ancora dal
cambio di stato di un'applicazione nel listino. Con la scadenza, però, il disallineamento dura **al massimo un
minuto** invece che per sempre: non è più una falla, è una **latenza**. Chiuderla del tutto richiede di
pubblicare invalidazioni per **tutti** i conti di un'applicazione, e le domande aperte sono vere (chi pubblica?
per quanti conti alla volta?). Tracciato.

**La verifica del terzo presidio non è automatizzata**, per scelta: servirebbe una suite che esegue una suite.
L'ho eseguita a mano con una sonda temporanea, poi rimossa.

## Test

- **`./run-tests.sh backend` con lo stack locale ACCESO** → **verde** (54 collaudi in `crm`, e l'intera area).
  È la verifica diretta del difetto 2: prima era rossa in questa esatta condizione.
- **`EntitlementProjectionTest`** → 12 su 12 verdi, di cui **tre nuovi**: la rilettura oltre la durata **senza
  alcun evento** (il collaudo che sarebbe fallito prima), il ripiego sull'ultima verità nota con la fonte giù, e
  la non-regressione del disaccoppiamento entro la durata.
- **`./run-tests.sh tooling`** → verde, parità dei modelli inclusa: le chiavi nuove sono anche nel modello.
- **Presidio sugli instabili** → verificato con una sonda: suite **rossa**, percorso **nominato**, codice di
  uscita **1** (letto senza pipe: con `| tail` si legge il codice di `tail`).
- **`./run-tests.sh` completa** → **verde su tutte le otto aree** (backend, frontend, infra, compliance,
  tooling, smoke, platform, site). Nota che vale più di un segno di spunta: `platform` è verde con 16 percorsi
  su 16 e **zero instabili** — e da questa change quella non è più una speranza, perché un solo percorso
  instabile l'avrebbe resa rossa.

Copertura end-to-end: **nessun impatto** (voce 13) — nessuna superficie applicativa nuova o modificata.

## Stato criteri di accettazione

- [x] Oltre la durata la copia viene riletta **anche senza evento**; entro la durata **non** viene riletta.
      Entrambi i versanti coperti.
- [x] Fonte irraggiungibile + copia scaduta → **ultima verità nota**. Coperto.
- [x] La chiave della durata è nei servizi **e** nel modello; parità verde.
- [x] `./run-tests.sh backend` verde **con lo stack acceso**.
- [x] Un percorso che passa solo al ritentativo rende la suite **rossa**, nominandolo.
- [x] `./run-tests.sh` completa verde — tutte e otto le aree, `platform` con zero percorsi instabili.
