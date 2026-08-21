# Piano di lavoro — UC 0108 · Cruscotto del collaboratore

**Storia**: [0108](../story/0108-cruscotto-collaboratore.md) · **Area toccata**: `frontend/apps/backoffice`
**Dimensione stimata**: piccola-media (il passo 4 è arrivato dopo) · **Prerequisito**: UC 0107

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

## Passo 4 — Invito ad aprire un proprio account (§4.5 della storia)

Aggiunto su richiesta dello sviluppatore il 2026-08-21. **Non è un passo di sola interfaccia**: comincia dal
contratto, perché il dato che serve oggi non c'è.

1. **Estendere la lettura delle appartenenze col ruolo.** `GET /api/platform/v1/me/memberships` restituisce
   oggi `MembershipRef(accountId, accountName)` —
   [MembershipDtos.java](../../../../services/core/src/main/java/app/appgrove/core/platform/MembershipDtos.java).
   Senza il ruolo la condizione non è calcolabile. Aggiungerlo lì (sono dati di chi chiede: nulla da
   minimizzare), rigenerare il contratto del client frontend, e coordinarsi con UC 0107 se serve anche a lei:
   il campo si aggiunge **una volta**.
2. **Nuovo componente** `dashboard/OwnAccountInviteSection.tsx`, reso da `DashboardPage.tsx` **sotto** la
   sezione degli inviti ricevuti (UC 0118) e **sopra** le schede delle applicazioni.
3. **La condizione**: `!memberships.some(m => m.role === 'owner')`. **Non** `platformRole !== 'owner'`, che è
   la condizione sbagliata e la trappola numero 3 qui sotto.
4. **I due orologi** (decisi il 2026-08-21, §4.5 della storia): l'invito vive **un anno** dalla nascita
   dell'identità (`platform.identity.created_at`), e ogni «Non ora» lo rinvia di **una settimana**. Oltre l'anno
   non compare più e il rinvio non serve più.

   **Conviene calcolarlo sul server, non nell'interfaccia.** Il server ha già tutto e l'interfaccia no: il ruolo
   delle appartenenze (punto 1), la data di iscrizione (`BaseEntity.createdAt`, mai esposta) e — se si scarta il
   cookie — anche il rinvio. Esporre **una sola risposta** («mostrare l'invito: sì/no») invece di tre dati
   grezzi tiene la regola in un posto solo e non fa uscire due date che l'interfaccia non ha motivo di
   conoscere. Se invece la regola vivesse nell'interfaccia, andrebbe esposta la data di iscrizione: un dato in
   più, per riscrivere la stessa condizione una seconda volta.

   **Il rinvio si conserva sul server**, non in un cookie (deciso il 2026-08-21): colonna
   `own_account_invite_snoozed_until` su `platform.identity`. Serve quindi anche: la migrazione con la colonna,
   la voce nel **manifesto dei dati** con la sua finalità, e un'operazione che scriva il rinvio quando la persona
   preme «Non ora». Nessun cookie, quindi nulla da aggiungere all'inventario dei cookie e nessun effetto
   sull'assenza del banner di consenso.

   **L'invito si spegne da sé quando la persona apre un proprio account**: nasce l'appartenenza `owner`, la
   condizione del punto 3 diventa falsa, l'invito sparisce. Nessuna regola in più da scrivere — ma **un collaudo
   sì**, perché se la risposta finisse in una copia locale, la creazione dell'account dovrebbe invalidarla.
5. **Non renderlo mentre le appartenenze caricano**: comparire e sparire è peggio che comparire tardi.
6. **Testi**: già scritti e approvati nelle cinque lingue in §4.5 della storia — si copiano da là, non si
   riscrivono. Due avvertenze che vengono da lì: le chiavi sono **due corpi interi**
   (`bodyMultiAccount`/`bodySingleAccount`) e non un corpo con un frammento innestato, perché la frase sul
   selettore cade in posizioni diverse fra le lingue; e l'elenco degli account si compone con `Intl.ListFormat`
   nella lingua attiva, perché la congiunzione va localizzata.

Reso nei prototipi [editor.html](../prototype/editor.html) e [viewer.html](../prototype/viewer.html);
**assente** in `owner.html` e `admin.html`. La funzione del prototipo è `sezioneAccountProprio()` in
`prototype/assets/proto.js`, e la condizione vive in `SENZA_ACCOUNT_PROPRIO`: si legge da là.

## Passo 5 — Collaudi

- `DashboardPage.test.tsx`: per un collaboratore nessun comando dispositivo, nessuna cifra, nessuna
  scorciatoia di invito; per l'owner tutto invariato (non-regressione).
- Verifica che le letture economiche **non** partano per un collaboratore (con le chiamate simulate).
- Caso «nessuna applicazione» con i suoi rimandi.
- `frontend/apps/backoffice/e2e/dashboard.spec.ts` esteso.
- **Invito ad aprire un account** (passo 4): compare senza appartenenze `owner`; **non** compare col caso
  insidioso «titolare altrove, collaboratore qui», che è la prova che separa la condizione giusta da quella
  sbagliata; non compare dopo il rinvio; non compare durante il caricamento.

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
3. **Confondere «sono collaboratore qui» con «non sono titolare da nessuna parte»** è la trappola del passo 4:
   la prima condizione mostrerebbe l'invito ad aprire un account anche a chi ne ha già uno, cioè proprio a chi
   non deve vederlo. La condizione guarda l'**insieme** delle appartenenze, non il ruolo nell'account attivo.
4. **Rimandare al selettore dell'account a chi non ce l'ha**: con una sola appartenenza il selettore non viene
   reso (UC 0117), quindi il testo va declinato nei due casi. Difetto trovato rendendo il prototipo, non in
   revisione: la copy sembrava giusta finché non l'abbiamo vista sullo schermo dell'`editor`.
