# Piano di lavoro — UC 0109 · Catalogo in sola lettura e richiesta all'owner

**Storia**: [0109](../story/0109-catalogo-sola-lettura-richiesta-owner.md) · **Aree toccate**: `services/core`, `services/commons`, `frontend/`, conformità
**Dimensione stimata**: media · **Prerequisito**: UC 0107

## Passo 1 — Migrazione e servizio

**Migrazione**: `platform.app_install_request` (account, applicazione, richiedente, nota, istante, campi di
audit). Indice su `(tenant_id, app_id, requested_by, created_at)` per il limite di frequenza.

**File nuovi** in `services/core/src/main/java/app/appgrove/core/catalog/`:

- `AppInstallRequest.java` — entità separata per account. La nota è **dato personale**: annotarla con
  `@PersonalData` (categoria «contenuto fornito dall'utente», finalità, base giuridica, conservazione), perché
  il collaudo del manifesto verifica proprio questo accoppiamento.
- `AppInstallRequestResource.java` — creazione (chiunque autenticato) e lettura delle proprie richieste
  recenti (per lo stato «già richiesto il …»).
- `AppInstallRequestService.java` — limite di frequenza (una per applicazione ogni ventiquattro ore per
  persona), invio dell'email, e **nessuna riga registrata se l'invio fallisce**.

## Passo 2 — L'email

**File nuovo**: modello di email in `shared/email-templates/` (verificare la struttura esistente), reso dal
renderer condiviso di `services/commons/.../email/`, nelle cinque lingue, nella lingua **dell'owner** (che è
il destinatario e la cui preferenza è nota: `platform.users.locale`).

Contenuto: nome dell'applicazione, nome di chi chiede, nota, collegamento alla scheda dell'applicazione.
Nient'altro: il minimo necessario è anche una regola di conformità, non solo di buon gusto.

## Passo 3 — La pagina del catalogo

**Modifica**: [AppCatalogPage.tsx](../../../../frontend/apps/backoffice/src/pages/catalog/AppCatalogPage.tsx) —
i comandi cambiano secondo il ruolo di piattaforma e l'accesso:

| Stato dell'applicazione | Owner | Collaboratore |
|---|---|---|
| attiva e con accesso | «Apri» | «Apri» |
| attiva senza accesso | «Apri» | «Chiedi l'abilitazione» |
| non attiva | «Attiva» (acquisto) | «Chiedi all'owner di installarla» |
| già richiesta nelle 24 ore | — | «Già richiesto il …», disabilitato |

Prezzi e livelli restano visibili a tutti: servono a motivare la richiesta.

**File nuovo**: finestra della richiesta con la nota facoltativa (massimo un paio di righe, con contatore),
e la spiegazione di dove finirà.

## Passo 4 — Conformità

- **Manifesto dei dati** della piattaforma: dichiarazione del nuovo trattamento (è l'**unico** trattamento
  nuovo dell'epica).
- **Registro dei trattamenti**: aggiornamento con finalità, base giuridica, conservazione (proposta novanta
  giorni) e categoria di interessati.
- Il rilevatore dei segnali privacy (`tools/compliance/privacy-scan.mjs`) segnalerà la nuova annotazione: è
  il comportamento atteso, e la change deve rispondere al suo segnale con la classificazione, non silenziarlo.
- **Spazzino di conservazione**: la riga si cancella dopo il periodo dichiarato, sul modello degli altri
  spazzini di conservazione già presenti.

## Passo 5 — Collaudi

- `AppInstallRequestApiTest.java`: creazione, limite di frequenza, invio fallito senza riga registrata,
  rifiuto dell'acquisto per un collaboratore.
- Collaudo del manifesto: la nuova annotazione è dichiarata (il collaudo esistente lo pretende
  automaticamente).
- Email: resa nelle cinque lingue, con il solo contenuto previsto.
- `frontend`: la tabella dei comandi del passo 3, un caso per riga.
- `frontend/apps/backoffice/e2e/catalog.spec.ts` esteso.

## Verifica finale

```bash
cd services && mvn -B -pl core -am test
cd ../frontend && npm run typecheck && npm test
cd .. && ./run-tests.sh backend frontend compliance
```

## Trappole note

1. **La nota è testo libero scritto da una persona**: è un dato personale a tutti gli effetti e va dichiarato.
   Ometterlo è esattamente il difetto che il rilevatore dei segnali privacy esiste per cogliere.
2. **Registrare la richiesta prima di aver inviato l'email** produce «già richiesto» senza che nessuno abbia
   ricevuto nulla: l'ordine conta.
3. **La lingua dell'email è quella dell'owner**, non di chi chiede: il destinatario è l'owner.
