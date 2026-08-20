# Piano di lavoro — UC 0101 · Semantica dei tre ruoli

**Storia**: [0101](../story/0101-semantica-ruoli-viewer-editor-admin.md) · **Aree toccate**: `services/*`, documentazione
**Dimensione stimata**: media · **Prerequisiti**: UC 0098, UC 0099

## Passo 1 — Il documento delle operazioni, come formato

**File nuovo**: `services/commons/src/main/java/app/appgrove/commons/access/AppOperationsContract.java` —
l'interfaccia che ogni applicazione realizza per dichiarare le proprie operazioni e il ruolo minimo di
ognuna, sul modello di
[AppDataContract.java](../../../../services/commons/src/main/java/app/appgrove/commons/gdpr/AppDataContract.java)
(precedente identico nella forma: un contratto che ogni applicazione realizza e che un collaudo verifica).

Per ogni operazione: identificativo stabile, descrizione breve in italiano e inglese, ruolo minimo, e un
contrassegno «esente dai ruoli» per le vie di conformità.

## Passo 2 — Realizzazione nelle due applicazioni esistenti

- `services/fatture/.../FattureOperationsContract.java`
- `services/crm/.../CrmOperationsContract.java`

Il contenuto si ricava percorrendo le operazioni esposte: `grep -n "@GET\|@POST\|@PUT\|@PATCH\|@DELETE"` su
ogni classe di interfaccia del servizio. La classificazione segue la cascata della storia §4, senza
inventare eccezioni.

## Passo 3 — Il collaudo che rende vero il contratto

**File nuovo** in ogni servizio di applicazione: `AppOperationsContractTest.java`, che verifica **due**
direzioni:

1. ogni operazione dichiarata nel contratto esiste come metodo annotato;
2. ogni metodo di **scrittura** esposto dal servizio è dichiarato nel contratto **e** porta l'annotazione del
   varco con almeno `editor`.

La seconda direzione è quella che conta: coglie l'operazione nuova che qualcuno aggiunge dimenticando il
varco. Realizzabile con la stessa tecnica di
[ArchitectureTest.java](../../../../services/core/src/test/java/app/appgrove/core/ArchitectureTest.java),
che già ispeziona il codice compilato.

## Passo 4 — La regola dell'interfaccia: disabilitato contro assente

**File nuovo**: `frontend/packages/design-system/src/components/DisabledForRole.tsx` (nome da confermare) —
un piccolo involucro che rende un comando disabilitato con la spiegazione del ruolo mancante, coerente per
tutte le applicazioni e già corretto per gli strumenti di assistenza (`aria-disabled`, descrizione
collegata). Senza questo, ogni modulo inventerà il proprio modo e l'accessibilità andrà persa in tre punti su
quattro.

Chiavi di traduzione nelle cinque lingue in `frontend/packages/i18n/src/resources/*.ts`, sezione nuova
`roles`.

## Passo 5 — Documentazione della regola

**Modifica**: [docs/04-services-backend.md](../../../04-services-backend.md) — la cascata di classificazione
in una sezione dedicata, perché è una regola di piattaforma e va dove chi scrive un servizio la cerca.

## Verifica finale

```bash
cd services && mvn -B test
cd .. && ./run-tests.sh backend frontend
```

## Trappole note

1. **Il contratto non deve diventare un doppione del codice**: dichiara le operazioni, non le rifà. Se
   mantenerlo diventa noioso, il collaudo lo dirà subito.
2. **La tentazione di rendere il `viewer` cieco su qualche dato**: la storia lo vieta. Un `viewer` che non
   vede tutto è un ruolo nuovo, da discutere, non una restrizione silenziosa.
3. **Le vie di conformità**: se qualcuno le protegge col varco «per coerenza», si rompe un diritto. Il
   contrassegno di esenzione serve esattamente a questo, e il collaudo lo verifica.
