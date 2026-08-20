# Piano di lavoro — UC 0100 · Sezione «Members» come elenco unico

**Storia**: [0100](../story/0100-sezione-members-elenco-unico.md) · **Aree toccate**: `frontend/`, `services/core`
**Dimensione stimata**: media · **Prerequisiti**: UC 0098, UC 0099

## Passo 1 — Stringere l'interfaccia del core al solo owner

**Modifiche** in `services/core/src/main/java/app/appgrove/core/platform/`:

- `UserResource.java` — le operazioni di elenco, lettura per identificativo, modifica e cancellazione passano
  da `@RolesAllowed({OWNER, ADMIN})` a `@RolesAllowed(OWNER)`. Le operazioni su `/me` restano aperte a tutti
  (sono i propri dati).
- `InvitationResource.java` — stessa stretta. Inoltre **rimozione del ruolo** dal corpo della creazione
  (`InvitationDtos.CreateInvitation`) e dall'entità `Invitation`: il ruolo di piattaforma non si sceglie più.
  La colonna resta in banca dati fino alla conversione (UC 0113) per non rompere le righe esistenti; il codice
  smette di scriverla.
- `Roles.java` — rimozione della costante `ADMIN` **se** non più usata (verificarlo con `grep`).

**Collaudi**: `AccountUserApiTest.java` e `InvitationLifecycleTest.java` vanno aggiornati — oggi provano che
un `admin` può gestire i membri, che è esattamente ciò che smette di valere. Il collaudo nuovo prova il
rifiuto.

## Passo 2 — La lettura del conteggio delle applicazioni per persona

**Modifica**: `UserDtos.UserView` — aggiunta del campo `appCount`, riempito da una lettura aggregata su
`app_access`. Per l'owner il conteggio è il numero di applicazioni con diritto dell'account (accesso
implicito). Attenzione a non fare una interrogazione per riga: una sola aggregata, altrimenti con trenta
persone si fanno trenta interrogazioni.

## Passo 3 — Il client generato

**Comando**: rigenerare il contratto del client dall'interfaccia del core, come già si fa
(`frontend/packages/api-client/src/schema.ts` è generato; `contract.ts` espone i tipi comodi). Verificare che
il tipo di `UserView` porti il conteggio e che `CreateInvitation` non porti più il ruolo.

## Passo 4 — La schermata

**Modifica**: [MembersPage.tsx](../../../../frontend/apps/backoffice/src/pages/members/MembersPage.tsx) —
è una riscrittura sostanziale, non un ritocco:

1. **una** tabella invece di due: le persone e gli inviti in attesa nello stesso elenco, ordinati con l'owner
   in testa;
2. colonna **stato** con quattro valori (attiva · invito in attesa · sospesa · in cessazione dal …);
3. colonna **applicazioni** con il conteggio, che rimanda alla schermata degli utenti dell'applicazione
   (UC 0111) — nessun cambio di ruolo da qui;
4. **rimozione** della colonna del ruolo, del selettore di ruolo e del selettore nel form di invito;
5. riga di spiegazione sotto il titolo, perché chi conosce la schermata attuale penserà a un difetto;
6. il riquadro dei posti **non** si fa qui: lo aggiunge UC 0103. Lasciare il posto (un componente vuoto o un
   commento) evita un secondo rimaneggiamento della struttura.

**Modifica**: `frontend/apps/backoffice/src/api/hooks.ts` — l'invito non manda più il ruolo; l'elenco unisce
le due letture (persone e inviti) in una sola lista per la tabella. La fusione è una funzione **pura** da
tenere fuori dal componente e da provare da sola.

## Passo 5 — Rotta e traduzioni

- [routes.tsx](../../../../frontend/apps/backoffice/src/routing/routes.tsx): la guardia passa da
  `requireAnyRole(['owner','admin'])` a `requireRole('owner')`.
- `frontend/packages/i18n/src/resources/*.ts` (cinque lingue): rimozione delle chiavi dei ruoli nella sezione
  `members`, aggiunta di quelle di stato e della riga di spiegazione. Il collaudo di parità delle lingue
  coglie le dimenticanze.

## Passo 6 — Collaudi

- `MembersPage.test.tsx`: elenco unico, nessuna colonna di ruolo, conteggio, azioni disabilitate dove
  previsto.
- Unità sulla funzione di fusione delle due letture.
- `frontend/apps/backoffice/e2e/members.spec.ts`: riscritto sul flusso nuovo, mantenendo l'etichetta del
  percorso in testa al titolo (il controllo del registro di copertura la pretende).

## Verifica finale

```bash
cd services && mvn -B -pl core -am test
cd ../frontend && npm run typecheck && npm test
cd .. && ./run-tests.sh frontend backend
```

## Trappole note

1. **Il collaudo esistente prova il contrario.** Gli attuali `AccountUserApiTest` e `members.spec.ts` danno
   per buono che un `admin` gestisca i membri. Aspettarsi rosso e leggerlo come conferma, non come guasto.
2. **La colonna `role` di `invitations`** resta in banca dati: non rimuoverla in questa change, o le righe
   esistenti diventano illeggibili.
3. **Il conteggio delle applicazioni per l'owner** non viene da `app_access` (non ha righe): va calcolato
   dai diritti dell'account.
