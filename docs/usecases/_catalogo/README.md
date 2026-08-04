# Catalogo applicazioni — indice del drill-down

Il documento [`appgrove-catalogo-applicazioni.md`](appgrove-catalogo-applicazioni.md) valuta **60 idee di
applicazione** per il marketplace: dice *cosa* potrebbe essere costruito. Questa cartella contiene lo strato
che manca fra quella valutazione e la skill [`new-application`](../../../.claude/skills/new-application/SKILL.md):
epiche, storie utente, disegno dell'interfaccia e le risposte che la skill pretende prima di generare.

Prodotto dalla change [`0086-catalogo-app-drill-down`](../../../changes/0086-catalogo-app-drill-down/).

## Come si usa

Una cartella applicazione è pronta per essere data in pasto alla skill:

```
/new-application
```
e come descrizione il contenuto di `NN-<slug>/application-description.md`, che porta già identificativo,
modello utente, porta, metrica di quota, listino proposto e classificazione dei dati personali proposta.

> **Listino e dati personali sono proposte, non decisioni.** Restano fermate di escalation dello
> sviluppatore, e la skill li fa confermare comunque. Lo stesso vale per ogni punto marcato «da confermare»
> nelle singole descrizioni.

## Le diciassette applicazioni

| # | Applicazione | Identificativo | Epiche | Storie | Cartella |
|---|---|---|---|---|---|
| 02 | BillGrove — fatturazione e documenti commerciali | `billing` | 6 | 31 | [02-billgrove](02-billgrove/) |
| 03 | CashGrove — incasso crediti e flusso di cassa | `crediti` | 6 | 31 | [03-cashgrove](03-cashgrove/) |
| 04 | LeadGrove — clienti e trattative di vendita | `sales` | 7 | 37 | [04-leadgrove](04-leadgrove/) |
| 06 | QuoteGrove — preventivi e proposte | `preventivi` | 6 | 30 | [06-quotegrove](06-quotegrove/) |
| 07 | BookGrove — prenotazioni e agenda | `prenotazioni` | 7 | 34 | [07-bookgrove](07-bookgrove/) |
| 08 | SpendGrove — note spese e ricevute | `notespese` | 6 | 31 | [08-spendgrove](08-spendgrove/) |
| 12 | DeskGrove Support — assistenza clienti | `helpdesk` | 7 | 37 | [12-deskgrove-support](12-deskgrove-support/) |
| 13 | FlowGrove — progetti, attività e ore | `progetti` | 6 | 31 | [13-flowgrove](13-flowgrove/) |
| 14 | StockGrove — magazzino e inventario | `magazzino` | 7 | 37 | [14-stockgrove](14-stockgrove/) |
| 16 | ReachGrove — comunicazioni verso i clienti | `campaigns` | 7 | 37 | [16-reachgrove](16-reachgrove/) |
| 17 | RepGrove — recensioni e reputazione | `recensioni` | 6 | 31 | [17-repgrove](17-repgrove/) |
| 19 | SubGrove — abbonamenti dei clienti del cliente | `abbonati` | 7 | 35 | [19-subgrove](19-subgrove/) |
| 20 | InsightGrove — indicatori e copilota sui dati | `insights` | 7 | 35 | [20-insightgrove](20-insightgrove/) |
| 21 | SalonGrove — gestione salone (verticale) | *nessuno* — verticale di `prenotazioni` | 7 | 32 | [21-salongrove](21-salongrove/) |
| 31 | AuditGrove — registro delle azioni degli agenti | `agentaudit` | 7 | 37 | [31-auditgrove](31-auditgrove/) |
| 32 | TokenGrove — spesa per i modelli linguistici | `spesa_modelli` | 7 | 35 | [32-tokengrove](32-tokengrove/) |
| 33 | RenewGrove — rapporto col cliente e abbandono | `fidelizzazione` | 6 | 32 | [33-renewgrove](33-renewgrove/) |

**Totale: 17 applicazioni, 113 epiche, 573 storie**, ciascuna con artefatto navigabile ed estensioni della
console di amministrazione.

Nessun identificativo collide con un altro né con le app reali già nel repository (`fatture`, `crm`).
SalonGrove è l'unica che conclude di **non** essere un'applicazione nuova: è il verticale beauty di
BookGrove, e questa conclusione è marcata come da confermare.

## Le tre raccomandate dal catalogo

La sezione 5 del catalogo indica **31 AuditGrove**, **32 TokenGrove** e **33 RenewGrove** come le tre
scommesse da attaccare per prime: buyer diversi, canali diversi, profili di rischio diversi, così che il
fallimento di una non trascini le altre. Fra le tre, TokenGrove ha la complessità più bassa ed è la
candidata naturale al primo build.

## Quello che non c'è, e perché

**Quarantatré applicazioni del catalogo non hanno una cartella qui**, per due ragioni diverse:

- **venti sono escluse per peso normativo** — vedi [`_escluse/README.md`](_escluse/), che ne porta il
  criterio e la motivazione una per una. Due di esse (01 InvoiceGrove e 05 ChatGrove) erano già state
  scritte per intero prima della decisione e sono conservate lì, **fuori dal piano di costruzione**;
- **ventitré non sono state scritte** — 24 FieldGrove, 25 BuildGrove, 26 EstateGrove, 28 ProGrove, 30 MoveGrove,
  34 BackupGrove, 36 VendorGrove, 37 FleetGrove, 38 AssetGrove IT, 39 SpendGrove SaaS, 40 MaintGrove,
  41 RentGrove, 43 PimGrove, 45 OnboardGrove, 47 RefGrove, 48 ProcureGrove, 50 QualityGrove,
  51 WarrantyGrove, 53 DeskGrove Spaces, 54 BudgetGrove, 55 SyncGrove, 56 IncidentGrove, 59 SolarGrove.
  Non è una bocciatura: è una scelta di sequenza. Scrivere le storie di tutte prima di averne costruita
  una significa scrivere molto su un metodo non ancora messo alla prova. Aggiungerne una costa un
  passaggio del kit d'autore.

## Il kit d'autore

[`_kit/`](_kit/) contiene il metro con cui queste cartelle sono state scritte, e con cui si scrivono le
prossime: guida operativa, digest dei vincoli di piattaforma, modelli dei tre documenti e guscio dell'artefatto
navigabile. **Chi aggiunge un'applicazione parte da lì**, non da una cartella esistente copiata a mano.

## Attenzione: due numerazioni diverse

Le storie di questa cartella usano una numerazione **locale all'applicazione** (`0001`, `0002`, …) e **non**
consumano i numeri assoluti degli use case del repository (`docs/usecases/<area>/NNNN-*.md`). Quando una di
queste applicazioni verrà davvero costruita, i suoi use case reali riceveranno numeri assoluti dalla skill
[`new-usecase`](../../../.claude/skills/new-usecase/SKILL.md): qui c'è il **materiale di partenza**, non il
catalogo ufficiale.

## Punti che tornano allo sviluppatore

Tre decisioni trasversali sono emerse scrivendo queste applicazioni e vivono in
[`docs/_BACKLOG.md`](../../_BACKLOG.md), non qui — due delle tre vanno chiuse **prima** di scaffoldare la
prima applicazione:

1. **superfici pubbliche senza autenticazione** — chi dice qual è il conto quando chi guarda la pagina non è
   un utente (riguarda 06, 07, 19);
2. **autenticazione di una macchina** — come si ricava il conto quando il chiamante è un agente o un
   servizio e non una persona (riguarda 31, e ogni futura ricezione di dati da sistemi del cliente);
3. **«prospetto sì, classifica no»** — se promuovere a invariante il divieto di aggregati per persona,
   oggi rispettato per convenzione da 08, 13 e 21.

Ogni cartella ha inoltre la propria sezione «Rischi e punti aperti».
