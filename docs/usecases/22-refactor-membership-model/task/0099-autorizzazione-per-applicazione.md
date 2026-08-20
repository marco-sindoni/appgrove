# Piano di lavoro — UC 0099 · Autorizzazione per applicazione

**Storia**: [0099](../story/0099-autorizzazione-per-applicazione.md) · **Aree toccate**: `services/commons`, `services/core`, `services/crm`, `infra/`, stack locale
**Dimensione stimata**: grande (la più delicata dell'epica) · **Prerequisito**: UC 0098

## Passo 1 — Il token porta un ruolo in meno

**Modifica**: `infra/modules/platform_shared/lambda/pre_token_gen/handler.py` — la funzione `_roles_for`
resta com'è, ma il ruolo che arriva da `platform.membership` (UC 0116) non potrà più valere `admin` (UC 0113 converte i
dati). Va aggiunta la **tolleranza**: se il valore letto è `admin`, si inietta `member`. Con il suo commento
che dice **quando togliere** questa tolleranza.

**Modifica**: il collaudo accanto, `test_handler.py`, con il caso del valore vecchio.

**Modifica**: il fornitore di identità locale (UC 0010) che costruisce gli stessi claim — cercarlo con
`grep -rn "groupsFor\|platform-admin-subjects" services/auth/src/main`. La **parità fra locale e cloud** è
un invariante dichiarato: se i due divergono, i servizi hanno due comportamenti e il difetto si vede solo
in produzione.

## Passo 2 — La lettura «dove posso entrare»

**File nuovo**: `services/core/src/main/java/app/appgrove/core/platform/MeAppAccessResource.java`
(`GET /api/platform/v1/me/app-access`), sul modello di
[MeResource.java](../../../../services/core/src/main/java/app/appgrove/core/billing/MeResource.java).

Restituisce, per il chiamante: elenco di `{ appSlug, appId, role }` per le applicazioni che **insieme**
hanno il diritto dell'account (riuso di `EntitlementReadModel`) e l'accesso della persona. Per l'owner:
tutte le applicazioni con diritto, ruolo `admin` (il massimo). **Esclude** la voce di catalogo di piattaforma
dei posti (UC 0103): scriverlo nel codice e nel collaudo, perché è il tipo di dimenticanza che finisce nel
menu laterale.

## Passo 3 — Il varco riusabile in `commons`

**File nuovi** in `services/commons/src/main/java/app/appgrove/commons/access/`:

- `RequiresAppRole.java` — annotazione con il ruolo minimo.
- `AppRoleGateFilter.java` — filtro che la interpreta, gemello di
  [EntitlementGateFilter.java](../../../../services/commons/src/main/java/app/appgrove/commons/entitlement/EntitlementGateFilter.java).
- `AppRoleService.java` + `RestAppRoleService.java` — lettura del ruolo dal core.
- `AppRoleRequiredException.java` e il suo traduttore in errore di rete (`web/AppRoleRequiredMapper.java`),
  che distingue **due** casi: nessun accesso e ruolo insufficiente. Due codici di problema diversi, perché
  i messaggi all'utente sono diversi.
- `projection/AppRoleProjectionStore.java` — copia locale, **fotocopia** della struttura di
  `entitlement/projection/`: stesso schema di invalidazione, stessa misura, stessa durata massima. Non
  inventare un secondo meccanismo: riusare la forma di quello che funziona.

**Modifica**: `AppAccessResource` di UC 0098 pubblica gli eventi di invalidazione sulla stessa coda già usata
per i diritti d'accesso (`EntitlementEvents`), o su una gemella. Decidere e scriverlo: una coda sola è più
semplice, due sono più chiare. Raccomandazione: **una sola**, con il tipo di evento nel messaggio.

## Passo 4 — Il metodo per le operazioni distruttive

Nel `AppRoleService`, un metodo `roleFresh(appId)` che **salta** la copia locale. Usarlo solo dove la storia
lo prescrive. Documentare il perché sul metodo, altrimenti verrà usato ovunque «per sicurezza» e la copia
locale diventerà inutile.

## Passo 5 — Prima applicazione che lo usa

**Modifica**: `services/crm/` — sostituire
[SeatAccess.java](../../../../services/crm/src/main/java/app/appgrove/crm/SeatAccess.java) con il varco
condiviso, e annotare le operazioni di `ContactResource` e `InteractionResource` col ruolo minimo (letture
`viewer`, scritture `editor`). I posti locali si ritirano in UC 0111: qui si aggiunge il varco nuovo
**accanto**, e si toglie il vecchio solo quando la schermata è pronta. Nota: due varchi contemporanei sono
accettabili per una change, non per due.

## Passo 6 — Collaudi

- `commons`: unità sul confronto dei ruoli e sulla scadenza della copia; integrazione del filtro con un
  servizio finto.
- `core`: `MeAppAccessApiTest` — contenuto della lettura per owner e per collaboratore; esclusione della voce
  di piattaforma.
- `crm`: integrazione per i tre ruoli su una lettura e una scrittura.
- **Invalidazione**: prova che dopo il cambio di ruolo nel core il servizio applica il nuovo ruolo. È la
  prova che nessuno scrive spontaneamente e che serve di più.
- **Fallimento chiuso**: core non raggiungibile e copia assente → rifiuto con il codice di guasto.

## Verifica finale

```bash
cd services && mvn -B test          # commons, core, crm
cd .. && ./run-tests.sh backend
cd infra/modules/platform_shared/lambda/pre_token_gen && python3 -m pytest test_handler.py
```

## Trappole note

1. **Il claim non cambia nome.** Restare su `roles` evita di toccare la configurazione di lettura dei gruppi
   in ogni servizio. Cambia solo il **contenuto**.
2. **La copia locale non deve contenere dati personali**: indicizzarla sull'identità di autenticazione, non
   sull'email.
3. **Parità locale/cloud**: se si dimentica il fornitore locale, in sviluppo tutto funziona e in cloud no
   (o viceversa). È il difetto più costoso di questo passo.
4. **Non mettere i ruoli nel token** per «risparmiare una chiamata»: è la scorciatoia che questa storia
   esiste per rifiutare, e le sue conseguenze si vedono solo dopo un incidente.
