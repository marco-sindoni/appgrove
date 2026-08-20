# Piano di lavoro — UC 0107 · Menu, rotte e visibilità per ruolo

**Storia**: [0107](../story/0107-menu-rotte-visibilita-per-ruolo.md) · **Area toccata**: `frontend/apps/backoffice`
**Dimensione stimata**: media · **Prerequisiti**: UC 0099, UC 0100

## Passo 1 — La lettura degli accessi nella shell

**File nuovo**: `frontend/apps/backoffice/src/registry/appAccess.ts` — gemello di
[entitlementsApi.ts](../../../../frontend/apps/backoffice/src/registry/entitlementsApi.ts): legge
`/api/platform/v1/me/app-access`, espone `{ accesses, isLoading, isError, retry }` e una funzione **pura**
`computeVisible(modules, entitled, accesses, isOwner)` che realizza l'intersezione a tre.

La funzione pura è il punto in cui questa storia si prova davvero: va scritta fuori dai componenti e coperta
con una tabella di casi, esattamente come `computeEntitled` di oggi.

## Passo 2 — Il menu laterale

**Modifica**: [Sidebar.tsx](../../../../frontend/apps/backoffice/src/shell/Sidebar.tsx) —

1. `useVisibleModules()` passa dall'intersezione a due a quella a tre (modifica in
   [registry.ts](../../../../frontend/apps/backoffice/src/registry/registry.ts));
2. il selettore `canManageMembers`, oggi `owner` **oppure** `admin`, diventa `isOwner`;
3. le voci Account, Billing e Members si mostrano **solo** all'owner;
4. la scheda utente mostra «Titolare dell'account» solo per l'owner e nessun ruolo per gli altri;
5. lo stato di errore della lettura degli accessi si somma a quello dei diritti, con lo **stesso**
   trattamento: errore, non elenco vuoto.

**Modifica**: `Sidebar.test.tsx` — la tabella dei quattro ruoli. È il collaudo più utile dell'intera storia:
scriverlo come elenco di casi con l'insieme atteso di voci.

## Passo 3 — Le guardie di rotta

**Modifica**: [routes.tsx](../../../../frontend/apps/backoffice/src/routing/routes.tsx) — Account, Billing e
Members sotto `requireRole('owner')`. Attenzione: `requireAnyRole(['owner','admin'])` va **rimosso**, non
ampliato.

**Modifica**: `navigation.test.tsx` — i casi di rimando alla pagina di rifiuto per un collaboratore.

## Passo 4 — Il ruolo nel contratto con i moduli

**Modifica**: [types.ts](../../../../frontend/apps/backoffice/src/registry/types.ts) — `ShellContextValue`
perde `roles: string[]` e guadagna `platformRole: 'owner' | 'member'` e `appRole: 'viewer' | 'editor' |
'admin'` (il ruolo sull'applicazione **corrente**).

**Modifica**: `ShellContext.tsx` e ogni modulo che leggeva `roles` — cercarli con
`grep -rn "useShellContext\|\.roles" frontend/apps/backoffice/src/modules/`. È una modifica che tocca tutti i
moduli e va fatta in una volta: lasciarne uno indietro significa un modulo che non sa chi sta guardando.

## Passo 5 — Collaudi

- Unità su `computeVisible`, tabellare.
- `Sidebar.test.tsx`, `navigation.test.tsx` come sopra.
- `frontend/apps/backoffice/e2e/shell.spec.ts`: menu del collaboratore (etichetta del percorso nel titolo).

## Verifica finale

```bash
cd frontend && npm run typecheck && npm test
cd .. && ./run-tests.sh frontend
```

## Trappole note

1. **Il selettore dello stato che restituisce un array nuovo** provoca un ciclo di render infinito: la
   trappola è già documentata due volte nel codice (in `Sidebar.tsx` e in `guards.tsx`). La nuova lettura degli
   accessi deve restituire riferimenti stabili.
2. **Errore travestito da diniego**: la storia UC 0077 ha già chiuso questo difetto per i diritti d'accesso;
   reintrodurlo con gli accessi sarebbe la stessa ferita nello stesso punto.
3. **Il caricamento non abilita nulla**: mai comandi attivi «in attesa» del ruolo.
