# Piano di lavoro — UC 0108 · Cruscotto del collaboratore

**Storia**: [0108](../story/0108-cruscotto-collaboratore.md) · **Area toccata**: `frontend/apps/backoffice`
**Dimensione stimata**: piccola · **Prerequisito**: UC 0107

## Passo 1 — Il cruscotto si biforca

**Modifica**: [DashboardPage.tsx](../../../../frontend/apps/backoffice/src/pages/dashboard/DashboardPage.tsx) —

1. `canManage`, oggi `owner` **oppure** `admin`, diventa `isOwner`;
2. per un collaboratore si rendono **solo** il saluto e le schede delle applicazioni a cui ha accesso;
3. le schede mostrano il **ruolo** della persona su quella applicazione con una frase comprensibile («puoi
   consultare» · «puoi modificare» · «puoi gestire gli utenti»), non l'etichetta tecnica;
4. sparisce il comando «gestisci il piano» dalla scheda; spariscono i riquadri economici e le scadenze;
5. la riga di scorciatoie si riduce a catalogo e supporto.

## Passo 2 — Non chiedere ciò che non si può leggere

Le letture economiche (abbonamenti, pagamenti, riepilogo di spesa) vanno chiamate **solo** se chi guarda è
l'owner: la condizione va sull'abilitazione della lettura (`enabled` della richiesta), non su un `if` nella
resa. Altrimenti si producono rifiuti a ogni caricamento, che riempiono la console e i registri del server di
errori inutili — e che un giorno qualcuno interpreterà come un difetto vero.

## Passo 3 — Il caso «nessuna applicazione»

Testo accogliente, senza colpevolizzare, con due vie: catalogo (dove potrà chiedere l'installazione) e
supporto. Traduzioni nelle cinque lingue.

## Passo 4 — Collaudi

- `DashboardPage.test.tsx`: per un collaboratore nessun comando dispositivo, nessuna cifra, nessuna
  scorciatoia di invito; per l'owner tutto invariato (non-regressione).
- Verifica che le letture economiche **non** partano per un collaboratore (con le chiamate simulate).
- Caso «nessuna applicazione» con i suoi rimandi.
- `frontend/apps/backoffice/e2e/dashboard.spec.ts` esteso.

## Verifica finale

```bash
cd frontend && npm run typecheck && npm test
cd .. && ./run-tests.sh frontend
```

## Trappole note

1. **Nascondere nella resa senza disabilitare la lettura** è il difetto tipico: l'interfaccia sembra giusta e
   il server registra rifiuti a ogni ingresso.
2. **La barra di consumo resta** (è informazione di lavoro), ma **senza** l'invito all'aumento di piano, che è
   una leva dell'owner.
