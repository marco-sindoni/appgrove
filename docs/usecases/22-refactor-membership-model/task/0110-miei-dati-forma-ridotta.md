# Piano di lavoro — UC 0110 · «I miei dati» in forma ridotta

**Storia**: [0110](../story/0110-miei-dati-forma-ridotta.md) · **Aree toccate**: `frontend/`, `services/core`
**Dimensione stimata**: piccola · **Prerequisito**: UC 0107

## Passo 1 — Stringere i presidi degli atti sull'account

**Modifiche** in `services/core/src/main/java/app/appgrove/core/gdpr/`: cercare con
`grep -rn "RolesAllowed" services/core/src/main/java/app/appgrove/core/gdpr/` e portare a **solo owner** le
operazioni che riguardano l'account:

- esportazione dell'account (`GdprResource`, avvio dell'esportazione);
- recesso per applicazione (`GdprWithdrawalResource`);
- cancellazione dell'account (`AccountDeletionResource`).

**Da non toccare**: `ProfileExportResource` (esportazione del proprio profilo) e la rettifica del proprio nome
in `UserResource.updateMe`, che sono diritti della persona e restano aperti a ogni ruolo. Il commento della
classe già lo dichiara: leggerlo prima di modificare.

**Collaudi**: aggiornare quelli che oggi provano l'accesso di un `admin`; aggiungere la prova che un
collaboratore riceve un rifiuto e che l'esportazione del proprio profilo riesce per ogni ruolo.

## Passo 2 — La pagina

**Modifica**: [PrivacyPage.tsx](../../../../frontend/apps/backoffice/src/pages/privacy/PrivacyPage.tsx) —

1. `canManageAccountData`, oggi `owner` **oppure** `admin`, diventa `isOwner`;
2. i blocchi degli atti sull'account si rendono solo per l'owner;
3. si aggiunge la riga che spiega la separazione e, per il collaboratore, il paragrafo su **come chiedere la
   cancellazione** (rivolgersi al titolare dell'account; contatto per la protezione dei dati sempre
   disponibile);
4. l'ordine dei blocchi per l'owner non cambia.

## Passo 3 — Traduzioni

Cinque lingue: la riga di separazione e il paragrafo sulla cancellazione. Testo in lingua corrente, con gli
articoli nominati ma non usati come spiegazione.

## Passo 4 — Registro di revisione legale

**Modifica**: [docs/_REVISIONE-LEGALE.md](../../../_REVISIONE-LEGALE.md) — voce nuova: far confermare a un
legale la ripartizione fra ciò che il collaboratore esercita da sé e ciò che deve chiedere al titolare
dell'account. Non è un blocco al rilascio, è una verifica dichiarata.

## Passo 5 — Collaudi

- `PrivacyPage.test.tsx`: tre blocchi per il collaboratore, pagina completa per l'owner.
- Integrazione: rifiuti e permessi come dal passo 1.
- `frontend/apps/backoffice/e2e/privacy.spec.ts` esteso.

## Verifica finale

```bash
cd services && mvn -B -pl core -am test
cd ../frontend && npm run typecheck && npm test
cd .. && ./run-tests.sh backend frontend
```

## Trappole note

1. **Non proteggere l'esportazione del proprio profilo.** È un diritto: proteggerla «per coerenza» sarebbe un
   difetto di conformità, non un irrigidimento.
2. **Nascondere senza spiegare**: un collaboratore che non trova come chiedere la cancellazione scriverà al
   supporto, e la risposta dovrà essere quella che avremmo dovuto scrivere nella pagina.
