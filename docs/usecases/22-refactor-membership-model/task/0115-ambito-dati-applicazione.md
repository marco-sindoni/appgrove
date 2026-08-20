# Piano di lavoro — UC 0115 · Ambito dei dati di un'applicazione

**Storia**: [0115](../story/0115-ambito-dati-applicazione.md) · **Aree toccate**: `services/core`, `tools/pricing-change`, `tools/new-application`, `.claude/skills/new-application`
**Dimensione stimata**: piccola — il campo, la guardia e la domanda · **Prerequisiti**: UC 0099, UC 0101, UC 0114

## Come leggere questo piano

I passi **1, 5 e 6** si eseguono in questa storia: la caratteristica si dichiara adesso, insieme alla guardia
che impedisce di rilasciarla senza filtro. I passi **2, 3, 4 e 7** sono il **piano del filtro**, che si esegue
con la **prima applicazione ad ambito `utente`** — non nasce in questa epica. Restano scritti qui, e per una
ragione precisa: chi li eseguirà avrà il progetto già fatto, deciso quando il modello di appartenenza era
fresco, invece di ripensarlo di corsa. Le parti rimandate portano l'avviso **⏸ rimandato**.

## Passo 1 — Il campo dell'applicazione

**Migrazione**: colonna `data_scope` su `platform.app` (`account` | `utente`), predefinito `account`, non
nulla. Nasce **al posto** di `user_model` che UC 0114 ha eliminato.

**Core**: enumerazione `AppDataScope`, campo su `App.java`, propagazione in `PricingDefinition`,
`PricingSyncService`, `AppStatusService`, `AdminResource`/`AdminDtos` (la console la mostra: qui la colonna
ha un senso, perché il valore cambia il comportamento) e nell'interfaccia dichiarata.

**Listini**: nuovo campo `dataScope:` nei file dei listini; `fatture.yaml` e `crm.yaml` dichiarano
`account`. Aggiungerlo ai campi verificati da `tools/pricing-change/lib/pricing.mjs`.

**La guardia** (parte di questa storia, ed è ciò che rende onesto il campo): finché il filtro non esiste,
`utente` è **dichiarabile ma non rilasciabile**. Due punti di arresto, non uno:

- `tools/pricing-change/lib/pricing.mjs` — il controllo dei listini **fallisce** se un listino dichiara
  `utente`, con un messaggio che nomina questo piano;
- `PricingSyncService` (o la convalida all'avvio di `services/core`) — il servizio **non si avvia**, per la
  stessa ragione: un controllo che gira solo in fase di sviluppo si aggira per sbaglio.

Il messaggio dice *perché*, non solo *no*: «ambito `utente` dichiarato ma il filtro per utente non è
implementato — vedi docs/usecases/22-refactor-membership-model/task/0115 §2». Quando il filtro arriverà, la
guardia si toglie in quel momento e non prima.

## Passo 2 ⏸ rimandato — Il meccanismo di persistenza: **non** si riusa quello dell'account

Punto tecnico da sapere prima di scrivere codice. Il filtro per account usa `@TenantId` di Hibernate
([BaseTenantEntity.java](../../../../services/commons/src/main/java/app/appgrove/commons/persistence/BaseTenantEntity.java)),
cioè il **discriminatore di multitenancy**: ce n'è **uno solo** e non si può usare un secondo per l'utente.

Via da seguire — **filtro di sessione parametrico** di Hibernate:

**File nuovi** in `services/commons/src/main/java/app/appgrove/commons/persistence/`:

- `BaseUserOwnedEntity.java` — estende `BaseTenantEntity`, aggiunge la colonna `owner_user_id` (non
  aggiornabile) e dichiara il filtro:
  ```java
  @FilterDef(name = "ownedByUser", parameters = @ParamDef(name = "userId", type = String.class))
  @Filter(name = "ownedByUser", condition = "owner_user_id = :userId")
  ```
  Il valore si assegna alla creazione, dal `sub` del token, **mai** dal corpo della richiesta: stessa
  regola dell'account, stessa ragione.
- `UserScopeFilterActivator.java` — attiva il filtro all'inizio di ogni richiesta con l'identità del
  chiamante, sul modello del filtro di richiesta già presente in `commons/logging`. **Acceso per difetto**:
  un filtro da accendere a mano verrebbe dimenticato, e la dimenticanza sarebbe un varco silenzioso.
- `UnfilteredUserScope.java` — l'annotazione (o il metodo esplicito) che **spegne** il filtro nei percorsi
  dichiarati. L'elenco è chiuso e va scritto nel commento della classe: vie di conformità (esportazione e
  cancellazione dell'account), lavori periodici di conservazione, consumatori di coda.

**Alternativa scartata**: filtrare a mano in ogni interrogazione. Funziona il primo giorno e si rompe alla
prima interrogazione nuova scritta da chi non lo sa. La sicurezza per costruzione batte la disciplina.

## Passo 3 ⏸ rimandato — L'owner: vede dall'interfaccia, no; ottiene dalla conformità, sì

Sono due percorsi diversi e vanno **provati separatamente**:

- l'interfaccia dell'applicazione: il filtro è attivo **anche per l'owner**. Nessuna eccezione di ruolo,
  altrimenti l'ambito non significa nulla;
- l'esportazione dell'account (`GdprExportService` e i lavoratori delle applicazioni,
  `commons/gdpr/GdprExportWorker`): il filtro è **disattivato**, perché il titolare deve poter adempiere ai
  propri obblighi. Verificare che i lavoratori esistenti girino già fuori da una richiesta utente — in quel
  caso il filtro non sarebbe attivo e basta **dichiararlo** invece di spegnerlo.

Lo stesso vale per la purga dei dati (`TenantPurgeConsumer`): deve vedere tutto, o lascerebbe righe dietro
di sé. È il caso in cui un filtro dimenticato **acceso** diventa un difetto di conformità.

## Passo 4 — Ambito e ruoli (la parte documentale ora, il testo mostrato col filtro)

**Ora**: il documento delle operazioni e il contratto dei ruoli di UC 0101 — la frase «il `viewer` vede
tutti i dati dell'applicazione» diventa «tutti i dati **che l'ambito gli attribuisce**». È una precisazione
che va fatta subito, perché altrimenti il contratto dei ruoli resta scritto in una forma che diventerà falsa.

**⏸ Rimandato**: il testo mostrato nella schermata degli utenti (UC 0111) per le applicazioni ad ambito
`utente` — si scrive quando ne esiste una, e va scritto guardando la schermata vera.

## Passo 5 — Il copilota della skill

**Modifica**: `.claude/skills/new-application/step-02-roles.md` (creato da UC 0112) — una domanda in più,
che **prende il posto** di quella sul modello utenti eliminata da UC 0114:

> «I dati di questa applicazione sono **del gruppo di lavoro** — chiunque vi abbia accesso li vede tutti,
> come in un gestionale dei clienti — oppure **della persona** che li ha creati, come in un blocco di
> appunti personali? La risposta cambia il filtro delle interrogazioni, non un'etichetta: da `utente` non
> si torna indietro senza una decisione di prodotto.»

**Il generatore, in questo giro, non emette applicazioni ad ambito `utente`.** È una decisione dello
sviluppatore: le applicazioni si affrontano dopo il rifacimento dell'appartenenza. Quindi il copilota fa la
domanda e, se la risposta è `utente`, **si ferma** con un messaggio esplicito — il supporto del generatore
(entità che estendono `BaseUserOwnedEntity`, collaudi di isolamento fra persone) va aggiunto prima di
procedere. Meglio una fermata chiara che un'applicazione generata a metà.

## Passo 6 — Collaudi

**Di questa storia** — pochi e mirati:

- **`tools/pricing-change`**: il campo si legge; assente vale `account`; valore ignoto è errore.
- **La prova che conta**: un listino di prova che dichiara `utente` fa **fallire** il controllo dei listini
  **e** l'avvio di `services/core`. Due prove, una per punto di arresto: è tutto ciò che regge l'onestà del
  campo finché il filtro non c'è.
- **`services/core`**: le due applicazioni reali dichiarano `account`; la console di piattaforma mostra la
  colonna.

**⏸ Rimandati, col filtro** (elencati perché sono il contratto, non da inventare allora):

- **`commons`**: unità sull'attivazione del filtro; integrazione con due identità diverse sulla stessa
  tabella. L'entità su cui provarlo può essere una **entità di prova** sul modello di
  `services/commons/src/test/java/app/appgrove/commons/privacy/fixtures/FixtureCustomer.java` — precedente
  già in casa — se il filtro arriva prima dell'applicazione che lo usa.
- **Il collaudo che conta**: due identità creano una riga ciascuna e non vedono quella dell'altra — in
  lettura, modifica, cancellazione e conteggi. Ad ambito `account`, la vedono entrambe.
- **Owner**: non vede dall'interfaccia; ottiene dall'esportazione dell'account. Due prove distinte.
- **Percorsi senza identità**: i lavori periodici vedono tutto; se il filtro restasse acceso, la purga
  lascerebbe righe — prova esplicita, perché è un difetto di conformità.

## Passo 7 ⏸ rimandato — Conformità

- **Manifesto dati** di un'applicazione ad ambito `utente`: dichiarazione esplicita di chi vede che cosa.
- **Informativa**: il paragrafo del §5 della storia — i dati non sono visibili ai colleghi
  nell'applicazione, ma appartengono al titolare dell'account, che può ottenerli per i propri obblighi.
  Scrivere il testo e portarlo alla revisione legale (voce in `docs/_REVISIONE-LEGALE.md`).
- Il rilevatore dei segnali privacy segnalerà il campo nuovo: rispondere con la classificazione, non
  silenziarlo.

## Verifica finale

```bash
cd services && mvn -B test -pl core
cd .. && ./run-tests.sh backend tooling
```

(Nessuna area della conformità in questa storia: manifesti e informativa vanno col filtro, passo 7.)

## Trappole note

1. **Un secondo `@TenantId` non esiste**: chi ci prova perde mezza giornata. Il filtro di sessione è la
   via.
2. **Filtro acceso dove non deve**: la purga e l'esportazione dell'account lascerebbero dati indietro. È il
   difetto più grave possibile qui, e va provato invece che sperato.
3. **Filtro spento dove deve stare acceso**: l'ambito diventa una promessa falsa. Per questo l'elenco dei
   percorsi esenti è chiuso e scritto in un solo posto.
4. **Il campo senza il filtro è una promessa di riservatezza non mantenuta**: è l'unico modo in cui questa
   storia può fallire davvero. Per questo la guardia è a **due** punti di arresto e non a uno, e per questo
   il messaggio d'errore rimanda a questo piano invece di dire soltanto «valore non valido». Se qualcuno
   togliesse la guardia per «sbloccare» un'applicazione, otterrebbe dati personali visibili a chi non deve
   vederli, senza che nulla diventi rosso.
5. **La quota resta dell'account** anche ad ambito `utente`: è controintuitivo e va scritto nel testo
   mostrato all'utente, altrimenti arriverà come segnalazione.
