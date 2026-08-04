# 0030 — Esportazione e cancellazione

**Applicazione**: 13 — FlowGrove (`progetti`) · **Epica**: 06 — Esposizione conversazionale e prove
**Storia**: `0030` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0002`, `0017`, `0022`, `0029`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come collaboratore di un'azienda che usa FlowGrove
> voglio poter ottenere copia di tutto ciò che l'app sa di me e chiederne la cancellazione
> così da esercitare i miei diritti anche su uno strumento che registra come passo le mie giornate di lavoro.

**Contesto.** È la storia che rende vera la classificazione dei dati personali della descrizione
([application-description.md](../application-description.md) §6). FlowGrove è la prima app della suite che tratta
dati **dei lavoratori del cliente** e non dei clienti del cliente: le righe di ore, lette insieme, raccontano la
giornata lavorativa di una persona. La storia 0002 ha predisposto le annotazioni e le tabelle; qui si chiude il
contratto dati e si affronta il nodo che questa app ha e le altre no: **cancellare le ore di una persona cambia il
consuntivo di una commessa già fatturata**. Un manifesto che dimentica una tabella è il difetto di conformità più
probabile di un'app nuova ([PRINCIPI-APPGROVE.md](../../_kit/PRINCIPI-APPGROVE.md) §10), e qui le tabelle con dati
di persone sono nove.

## 2. Requisiti funzionali

1. **RF-1** — Il servizio implementa `ProgettiDataContract` (`AppDataContract`) con `appId()`, `manifest()`,
   `exportData(scope)` e `purgeData(scope)`, per i due ambiti previsti: **un singolo interessato** e **l'intero
   account**.
2. **RF-2** — L'esportazione copre **tutte** le tabelle con dati di persone: `project` (referente), `task`,
   `assignment`, `time_entry`, `comment`, `attachment` (riga e file nell'archivio), `project_cost`,
   `billable_batch_line`, `notification`, `seat_usage`, `period_closure`, `tool_draft`, più le colonne di
   controllo `created_by`/`updated_by` di ogni tabella. Il risultato è leggibile da una persona, non un dispersivo
   scarico tecnico.
3. **RF-3** — La cancellazione è **fisica**: sostituire il nome con un codice non è cancellare. Ogni tabella
   presente in `exportData` è presente anche in `purgeData`, e una prova lo verifica per **confronto automatico
   fra i due elenchi**, non a occhio.
4. **RF-4** — Sulle ore già **consegnate** alla fatturazione (storia 0022) la cancellazione dell'interessato
   elimina le righe individuali e conserva **il solo totale aggregato del lotto**, senza riferimento alla persona:
   il consuntivo di una commessa già fatturata non cambia importo. La regola è scritta, non implicita, e va
   validata (§8).
5. **RF-5** — Esportazione e cancellazione restano accessibili **anche quando l'app è disabilitata o
   l'abbonamento è scaduto**: sono diritti, non funzioni del piano.
6. **RF-6** — Ogni cancellazione lascia una riga di prova nel registro delle purghe della piattaforma: che cosa è
   stato cancellato, quando, su quale richiesta — senza il dato cancellato.

## 3. Requisiti tecnici

- **RT-1 — Dati personali (§10).** Manifesto `docs/compliance/manifests/progetti.yaml` completo, con ogni voce in
  **italiano e inglese** (posizione, interessati, categoria, finalità, base giuridica, conservazione); ogni campo
  Java annotato `@PersonalData` — un campo annotato e non dichiarato fa fallire la compilazione. Il controllo di
  parità delle lingue e la freschezza del registro dei trattamenti girano nell'area `compliance` di
  `run-tests.sh`.
- **RT-2 — Isolamento fra account (§1).** L'ambito «account» opera solo sul `tenant_id` del token verificato;
  l'ambito «interessato» opera sull'identificativo dell'utente dentro quell'account. Nessuna delle due strade
  accetta un `tenant_id` dal corpo della richiesta.
- **RT-3 — Interfaccia di programmazione (§2).** Nessuna rotta pubblica nuova dell'app: le richieste arrivano
  dalla piattaforma («I miei dati») e questa app espone l'implementazione del contratto. Errori in
  `application/problem+json`.
- **RT-4 — Persistenza (§8).** Nessuna tabella nuova. La cancellazione fisica agisce anche sulle righe già
  marcate `deleted_at`: la cancellazione logica non è cancellazione. I file degli allegati vanno rimossi
  dall'archivio, non solo la loro riga.
- **RT-5 — Varchi (§6, §13).** I diritti dell'interessato restano accessibili con abbonamento `canceled` o app
  disabilitata: nessun `402` su questa strada.
- **RT-6 — Esposizione conversazionale (§12).** **Nessuno strumento**: né esportare né cancellare si comandano da
  una chat. È una cancellazione di dati, cioè il caso che la regola di sicurezza di piattaforma cita per nome fra
  gli effetti irreversibili, e il posto giusto per farla è la sezione «I miei dati» con l'identità verificata.
- **RT-7 — Registrazione eventi (§14).** «Esportazione prodotta», «cancellazione eseguita» con `tenant_id`,
  `app_id`, `user_id`, ambito, numero di righe per tabella e correlazione; **mai** i dati cancellati.
- **RT-8 — Prove (§11).** Prova di **completezza**: per ogni tabella con almeno un campo annotato
  `@PersonalData`, deve esistere la voce corrispondente in esportazione **e** in cancellazione, altrimenti la
  prova è rossa. È la prova che impedisce alla prossima storia di dimenticarsi una tabella.

## 4. Criteri di accettazione

**CA-1 — L'esportazione è completa**
- **Dato** un collaboratore con attività assegnate, 40 righe di ore, 3 commenti, 1 allegato e 2 avvisi
- **Quando** si esporta il suo ambito
- **Allora** il risultato contiene righe da tutte le tabelle che lo riguardano, e nessuna tabella dichiarata nel
  manifesto risulta assente

**CA-2 — La cancellazione è fisica**
- **Dato** lo stesso collaboratore
- **Quando** si esegue la cancellazione del suo ambito
- **Allora** nessuna riga con il suo identificativo resta nel database — comprese quelle con `deleted_at`
  valorizzato — e il file dell'allegato non è più nell'archivio

**CA-3 — Le ore già fatturate**
- **Dato** un lotto consegnato che conteneva 120 ore, di cui 30 di quel collaboratore
- **Quando** si esegue la cancellazione
- **Allora** le 30 righe individuali spariscono, il totale del lotto resta 120 ore senza riferimento a nessuna
  persona, e l'operazione è registrata

**CA-4 — Il diritto non dipende dall'abbonamento**
- **Dato** un account con abbonamento `canceled`
- **Quando** un suo utente chiede l'esportazione
- **Allora** la ottiene, senza `402`

**CA-5 — Elenchi disallineati**
- **Dato** una tabella nuova con un campo annotato `@PersonalData` aggiunta a `exportData` ma non a `purgeData`
- **Quando** si eseguono le prove
- **Allora** la prova di completezza fallisce e dice quale tabella manca

**CA-6 — Isolamento fra account**
- **Dato** due account `A` e `B` con collaboratori omonimi
- **Quando** si esporta l'ambito di un collaboratore di `A`
- **Allora** nessuna riga di `B` compare nel risultato

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (aree `backend` e `compliance`);
- [ ] prove di **unità** sulla composizione dell'esportazione e di **integrazione** sulla cancellazione, con
      database effimero, migrazioni vere e archivio dei file simulato;
- [ ] prova di **isolamento fra account** su entrambi gli ambiti;
- [ ] **prova end-to-end**: rimando — il percorso `[J-PROGETTI]` (storia 0031) non cancella dati, perché una
      prova che distrugge i propri dati di partenza è fragile; la copertura resta alle prove di integrazione e il
      motivo è registrato in [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) e in
      `decisions.json`;
- [ ] **traduzioni**: nessuna stringa nuova nel modulo; il manifesto è in italiano e inglese (sono due elenchi
      diversi: cinque lingue per l'interfaccia, due per il manifesto);
- [ ] **manifesto dei dati** completo e verde ai controlli di parità e freschezza;
- [ ] **registro delle decisioni** compilato: elenco delle tabelle, regola sulle ore già fatturate, motivo per cui
      non c'è alcuno strumento conversazionale;
- [ ] documentazione aggiornata dove la conservazione è descritta;
- [ ] avvio locale invariato (`./dev.sh services`).

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0002` | annotazioni, colonne di controllo e manifesto predisposti |
| storia `0017` | le righe di ore, che sono il dato più delicato dell'app |
| storia `0022` | i lotti consegnati, da cui nasce il conflitto fra cancellazione e traccia contabile |
| storia `0029` | la tabella delle bozze conversazionali entra negli elenchi |
| piattaforma — sezione «I miei dati» e registro delle purghe | l'app implementa il contratto, non l'interfaccia della richiesta |

## 7. Fuori ambito

- l'interfaccia con cui l'interessato fa la richiesta e la verifica della sua identità: sono di piattaforma;
- l'esportazione **operativa** dei rapporti (consuntivo, ore, calendario), che è un'altra cosa e serve al lavoro,
  non ai diritti: storia `0027`;
- la definizione delle **durate di conservazione** definitive: si veda il punto aperto.

## 8. Punti aperti

- **Per quanto si conservano le righe di ore.** La durata utile all'app è 24 mesi, ma le ore che diventano
  fattura hanno una vita contabile più lunga **in capo al cliente titolare**, e la qualificazione del foglio ore
  rispetto all'articolo 4 dello Statuto dei lavoratori le fonti la lasciano caso per caso
  ([application-description.md](../application-description.md) §2.7). **Questa storia non si chiude senza la
  risposta**: la chiude lo sviluppatore con la revisione legale pre-go-live.
- **La conciliazione fra diritto alla cancellazione e traccia contabile** (RF-4) è qui proposta nella forma «resta
  il totale aggregato, spariscono le righe individuali». È una scelta ragionevole ma non è mia: tocca un obbligo
  del cliente titolare. Va validata insieme al punto precedente.
