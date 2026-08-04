# 0035 — Esportazione e cancellazione dei dati personali

**Applicazione**: 20 — InsightGrove (`insights`) · **Epica**: 07 — Esposizione conversazionale e prove end-to-end
**Storia**: `0035` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0002`, `0008`, `0026`, `0028`, `0034`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come persona i cui dati sono finiti dentro InsightGrove — un utente dell'account che ha posto domande, oppure
> un cliente il cui nome compare come etichetta di una scomposizione
> voglio ottenere copia di ciò che l'app sa di me e poterne chiedere la cancellazione
> così da esercitare i miei diritti anche sull'applicazione che, di tutte, ne trattiene di meno e se ne accorge
> di meno.

**Contesto.** È la storia che rende vera la classificazione del §6 della [descrizione](../application-description.md),
e chiude il contratto dati dell'applicazione. Il rischio qui è particolare e va detto: InsightGrove tratta **pochi**
dati personali — un fatto è un numero — e proprio per questo è l'app in cui è più facile dimenticarne uno. I punti
in cui il dato personale entra sono quattro, tutti laterali: le **etichette di dimensione** (se è stata scelta la
via A del §6.1: la ragione sociale di una ditta individuale è il nome di una persona), il **testo delle domande**
poste al copilota, i **destinatari** di avvisi e rapporti, e le colonne `created_by` / `updated_by` sparse
ovunque. Un manifesto che dimentica una tabella è il difetto di conformità più probabile di un'app nuova
([PRINCIPI-APPGROVE.md](../../_kit/PRINCIPI-APPGROVE.md) §10): qui il presidio non può essere la memoria di chi
scrive, deve essere una prova.

## 2. Requisiti funzionali

1. **RF-1** — Il servizio implementa `InsightsDataContract` (`AppDataContract`) con `appId()`, `manifest()`,
   `exportData(scope)` e `purgeData(scope)`, per i due ambiti previsti: **un singolo interessato** e **l'intero
   account**.
2. **RF-2** — L'esportazione copre **tutte** le tabelle con dati riferibili a persone: `etichetta_dimensione` (se
   via A), `domanda`, `piano_di_interrogazione`, `avviso`, `scatto_avviso`, `rapporto_programmato`,
   `esecuzione_rapporto`, `esportazione`, `bozza_di_azione`, più le colonne di controllo `created_by` /
   `updated_by` di `fonte`, `definizione_metrica`, `cruscotto`, `riquadro` e `previsione`. Il risultato è
   leggibile da una persona, non un dispersivo scarico tecnico.
3. **RF-3** — La cancellazione è **fisica**, comprese le righe già marcate `deleted_at`: sostituire un'etichetta
   con un codice non è cancellare, e una domanda «anonimizzata» che conserva le parole resta un dato personale
   perché il testo può nominare qualcuno (§6.3 della descrizione).
4. **RF-4** — Ogni tabella presente in `exportData` è presente anche in `purgeData`, e una prova lo verifica per
   **confronto automatico fra i due elenchi**, non a occhio. La tabella `fatto` non contiene dati personali per
   contratto, ma è **dato dell'account** e rientra nella cancellazione con ambito account.
5. **RF-5** — La **revoca di una fonte** (storia 0008) è una cancellazione parziale che l'app sa fare da sola:
   cancella fisicamente i fatti e le etichette provenienti da quella fonte, e le metriche che dipendevano solo da
   essa smettono di produrre valori — **non producono valori sbagliati**. Questa storia verifica che le due
   strade, revoca e purga, usino lo **stesso** codice di cancellazione.
6. **RF-6** — Esportazione e cancellazione restano accessibili **anche con app disabilitata o abbonamento
   scaduto**: sono diritti, non funzioni del piano. Ogni cancellazione lascia una riga di prova nel registro delle
   purghe della piattaforma — che cosa, quando, su quale richiesta — senza il dato cancellato.

## 3. Requisiti tecnici

- **RT-8 — Dati personali (§10).** Manifesto `docs/compliance/manifests/insights.yaml` completo, con ogni voce in
  **italiano e inglese** (posizione, interessati, categoria del dato, finalità, base giuridica, conservazione);
  ogni campo Java annotato `@PersonalData` — un campo annotato e non dichiarato fa fallire la compilazione. I
  controlli di parità delle lingue e di freschezza del registro dei trattamenti girano nell'area `compliance` di
  `run-tests.sh`. Conservazioni proposte: domande **12 mesi**, etichette **finché la fonte resta collegata** (poi
  cancellazione entro 30 giorni), destinatari **finché l'avviso o il rapporto esistono**.
- **RT-1 — Isolamento fra account (§1).** L'ambito «account» opera solo sul `tenant_id` del gettone verificato;
  l'ambito «interessato» opera sull'identificativo dell'utente **dentro** quell'account. Nessuna delle due strade
  accetta un `tenant_id` dal corpo della richiesta.
- **RT-2 — Interfaccia di programmazione (§2).** Nessuna rotta pubblica nuova dell'app: la richiesta arriva dalla
  sezione «I miei dati» della piattaforma e questa app espone l'implementazione del contratto. Errori in
  `application/problem+json`.
- **RT-3 — Persistenza (§8).** Nessuna tabella nuova. La cancellazione fisica agisce anche sulle righe con
  `deleted_at` valorizzato; i **file** delle esportazioni prodotte (storia 0027) vanno rimossi dall'archivio a
  oggetti, non solo la loro riga.
- **RT-6 — Varchi (§6, §13).** Nessun `402` su questa strada: i diritti dell'interessato restano accessibili con
  abbonamento `canceled` o app disabilitata.
- **RT-7 — Esposizione conversazionale (§12).** **Nessuno strumento**: né esportare né cancellare si comandano da
  una chat. È una cancellazione di dati, cioè il caso che la regola di sicurezza di piattaforma cita per nome fra
  gli effetti irreversibili, e il posto giusto è la sezione «I miei dati» con l'identità verificata.
- **RT-11 — Prove (§11).** Prova di **completezza**: per ogni tabella con almeno un campo annotato
  `@PersonalData` deve esistere la voce corrispondente in esportazione **e** in cancellazione, altrimenti la prova
  è rossa. È la prova che impedisce alla prossima storia di dimenticarsi una tabella.
- **RT-14 — Registrazione eventi (§14).** «Esportazione dell'interessato prodotta», «cancellazione eseguita» con
  `tenant_id`, `app_id`, `user_id`, ambito e numero di righe per tabella; **mai** i dati cancellati né il testo
  delle domande.

## 4. Criteri di accettazione

**CA-1 — L'esportazione è completa**
- **Dato** un utente dell'account con 34 domande poste, due avvisi di cui è destinatario, un rapporto programmato
  e tre esportazioni prodotte
- **Quando** si esporta il suo ambito
- **Allora** il risultato contiene righe da tutte le tabelle che lo riguardano, e nessuna tabella dichiarata nel
  manifesto risulta assente

**CA-2 — La cancellazione è fisica**
- **Dato** lo stesso utente
- **Quando** si esegue la cancellazione del suo ambito
- **Allora** nessuna riga con il suo identificativo resta nel database — comprese quelle con `deleted_at`
  valorizzato — nessun testo di domanda sopravvive, e i file delle sue esportazioni non sono più nell'archivio

**CA-3 — Elenchi disallineati fanno rosso**
- **Dato** una tabella nuova con un campo annotato `@PersonalData`, aggiunta a `exportData` ma non a `purgeData`
- **Quando** si eseguono le prove
- **Allora** la prova di completezza fallisce e dice quale tabella manca

**CA-4 — La revoca di una fonte cancella davvero**
- **Dato** una fonte collegata con 12.480 fatti e 214 etichette
- **Quando** l'account la revoca
- **Allora** fatti ed etichette spariscono fisicamente, le metriche che dipendevano solo da quella fonte passano a
  «non calcolabile» invece di produrre numeri più piccoli, e resta una riga di prova

**CA-5 — Il diritto non dipende dall'abbonamento**
- **Dato** un account con abbonamento `canceled`
- **Quando** un suo utente chiede l'esportazione dei propri dati
- **Allora** la ottiene, senza `402`

**CA-6 — Isolamento fra account**
- **Dato** due account con utenti omonimi e con le stesse etichette di dimensione
- **Quando** si esporta l'ambito di un utente di `A`
- **Allora** nessuna riga di `B` compare nel risultato, e la cancellazione non tocca nulla di `B`

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (aree `backend` e `compliance`; l'intera suite prima del commit);
- [ ] prove di **unità** sulla composizione dell'esportazione e di **integrazione** sulla cancellazione, con
      database effimero, migrazioni vere e archivio dei file simulato;
- [ ] prova di **completezza** per confronto automatico fra l'elenco delle tabelle annotate, `exportData` e
      `purgeData`;
- [ ] prova di **isolamento fra account** su entrambi gli ambiti;
- [ ] **prova end-to-end**: *rimando* — il percorso `[J-INSIGHTS]` (storia 0034) non cancella dati, perché una
      prova che distrugge i propri dati di partenza è fragile; la copertura resta alle prove di integrazione, con
      motivo registrato in [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) e in
      `decisions.json`;
- [ ] **traduzioni**: nessuna stringa nuova nel modulo; il manifesto è in italiano e inglese (sono due elenchi
      diversi: cinque lingue per l'interfaccia, due per il manifesto);
- [ ] **manifesto dei dati** completo e verde ai controlli di parità e freschezza;
- [ ] **registro delle decisioni** compilato: elenco delle tabelle, scelta fra via (A) e via (B) per le etichette,
      conservazioni, motivo per cui non esiste alcuno strumento conversazionale;
- [ ] documentazione aggiornata dove le conservazioni sono descritte;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0002` | annotazioni, colonne di controllo e predisposizione del manifesto |
| storia `0008` | la revoca di una fonte è già una cancellazione fisica: qui si verifica che usi lo stesso codice |
| storia `0026` | il testo delle domande è il dato personale più delicato dell'app |
| storia `0028` | i destinatari di rapporti e avvisi |
| storia `0034` | il registro di copertura, dove va scritta la risposta «rimando» di questa storia |
| piattaforma — sezione «I miei dati» e registro delle purghe | l'app implementa il contratto, non l'interfaccia della richiesta |

## 7. Fuori ambito

- l'interfaccia con cui l'interessato fa la richiesta e la verifica della sua identità: sono di piattaforma;
- l'**esportazione operativa** dei numeri, che serve al lavoro e non ai diritti: storia 0027;
- la cancellazione dei dati **dentro le app sorgenti**: ogni app risponde dei propri; InsightGrove cancella ciò
  che ha ricevuto, non chiede alle altre di cancellare;
- la chiusura dell'account (`tenant.offboarded`): la purga per account è invocata dalla piattaforma, che possiede
  quel percorso.

## 8. Punti aperti

- **Via (A) o via (B) sulle etichette di dimensione** — con nomi leggibili o con soli codici. È il punto aperto
  numero 2 della descrizione (§11) e **va chiuso prima della storia 0006**, non qui: se la risposta fosse (B),
  questa storia perde una tabella e l'app diventa quasi priva di dati personali. Chiude: **sviluppatore**.
- **Dodici mesi per le domande e trenta giorni dopo la revoca per le etichette** sono proposte prudenti, non dati
  rilevati. Chiude: **sviluppatore**, con la revisione legale pre-go-live.
- **Che cosa fare delle etichette di un cliente cancellato nell'app d'origine?** La strada pulita è che l'app
  sorgente pubblichi la cancellazione e InsightGrove la applichi — ma è materia del contratto degli eventi di
  dominio, che non esiste (§11, punto 11 della descrizione). Nel frattempo la revoca della fonte è l'unico
  strumento a grana grossa. Chiude: **piattaforma**.
