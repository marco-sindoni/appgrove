# 0012 — Manifesto dati e diritti dell'interessato

**Applicazione**: 07 — BookGrove (`prenotazioni`) · **Epica**: 03 — Anagrafica dei clienti e agenda interna
**Storia**: `0012` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0011`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che riceve dal proprio cliente la richiesta di cancellare i suoi dati
> voglio poterlo fare davvero, in un colpo solo, e poter mostrare cosa è stato cancellato
> così da rispondere entro i termini senza dover cercare a mano in cinque tabelle.

**Contesto.** La piattaforma genera il registro dei trattamenti e gli strumenti di esportazione e cancellazione
**dal manifesto dei dati**: un campo non dichiarato è un campo che l'esportazione dimentica e la cancellazione
lascia indietro. Questa storia chiude il contratto dati dell'applicazione mentre le tabelle con dati personali
sono ancora poche, così che le epiche successive lo estendano invece di doverlo ricostruire.

> 🛑 **Questa storia non parte finché non è chiusa la decisione sull'articolo 9** descritta al §6 della
> descrizione dell'applicazione: se il collegamento fra una persona e il nome del servizio prenotato debba essere
> trattato come dato relativo alla salute per tutti, per nessuno, o solo per gli account che dichiarano di erogare
> prestazioni sanitarie. Le tre vie hanno conseguenze diverse su base giuridica, valutazione d'impatto, durata di
> conservazione e condizioni d'uso. **La decisione è dello sviluppatore.**

## 2. Requisiti funzionali

1. **RF-1** — Il manifesto `docs/compliance/manifests/prenotazioni.yaml` dichiara **tutte** le voci esistenti a
   questo punto, in italiano e inglese, con dove vive il dato, di chi è, che dato è, a cosa serve, perché è lecito
   e per quanto si tiene.
2. **RF-2** — Il contratto dati dell'app esporta, per un dato interessato, tutte le sue informazioni da tutte le
   tabelle che lo riguardano.
3. **RF-3** — La cancellazione è **fisica**: le righe spariscono. Sostituire il nome con un codice non è
   cancellare.
4. **RF-4** — La cancellazione di un cliente lascia coerente l'agenda passata: gli intervalli già trascorsi
   restano occupati **senza intestatario**, quelli futuri vengono rimossi.
5. **RF-5** — Esportazione e cancellazione restano accessibili anche quando l'app è disabilitata o l'abbonamento
   è scaduto.
6. **RF-6** — Ogni cancellazione lascia una riga di prova nel registro delle purghe: chi, quando, quante righe,
   su quali tabelle — senza contenere i dati cancellati.

## 3. Requisiti tecnici

- **RT-1 — Dati personali (§10).** Manifesto obbligatoriamente bilingue; ogni campo Java annotato
  `@PersonalData` deve comparire nel manifesto, altrimenti la compilazione fallisce; il contratto
  `PrenotazioniDataContract` implementa `appId()`, `exportData(scope)`, `purgeData(scope)`, `manifest()`.
  **Ogni** tabella con dati personali compare in entrambi: a oggi `cliente`, `risorsa`, `chiusura` (per il campo
  motivo).
- **RT-2 — Isolamento fra account (§1).** Esportazione e cancellazione agiscono su un solo `tenant_id`, preso dal
  token verificato; un ambito che arrivasse dalla richiesta non può allargare il perimetro.
- **RT-3 — Interfaccia di programmazione (§2).** Le rotte dei diritti dell'interessato sono quelle di
  piattaforma: l'app fornisce l'implementazione del contratto, non rotte proprie. Errori in `problem+json`.
- **RT-4 — Persistenza (§8).** La cancellazione fisica supera la cancellazione logica: `deleted_at` serve al
  ciclo di vita applicativo, non ai diritti dell'interessato.
- **RT-5 — Cinque lingue (§4) e due lingue (§10).** Attenzione a non confondere i due elenchi: l'**interfaccia**
  vuole cinque lingue, il **manifesto** ne vuole due, italiano e inglese.
- **RT-6 — Registrazione eventi (§14).** `esportazione richiesta`, `purga eseguita` con `tenant_id`, `app_id`,
  `user_id`, correlazione e conteggi — **mai i dati cancellati**.
- **RT-7 — Prove (§11).** L'area `compliance` di `./run-tests.sh` verifica la parità delle due lingue del
  manifesto; il controllo `@PersonalData`↔manifesto gira nei test backend; una prova di integrazione verifica che
  dopo la purga non resti nessuna riga riferita all'interessato in nessuna delle tabelle dichiarate.

## 4. Criteri di accettazione

**CA-1 — Manifesto completo e bilingue**
- **Dato** il manifesto dell'app · **Quando** si esegue `./run-tests.sh compliance` · **Allora** è verde: ogni
  voce ha italiano e inglese e nessun campo annotato manca

**CA-2 — Esportazione completa**
- **Dato** un cliente con prenotazioni, note e una voce in lista d'attesa
- **Quando** si esporta il suo dato
- **Allora** l'esportazione contiene tutto ciò che lo riguarda, da tutte le tabelle dichiarate

**CA-3 — Cancellazione fisica**
- **Dato** lo stesso cliente · **Quando** si esegue la purga · **Allora** nessuna riga riferita a lui resta in
  nessuna tabella, e una verifica diretta sul database lo conferma

**CA-4 — L'agenda passata resta coerente**
- **Dato** un cliente con due appuntamenti, uno passato e uno futuro · **Quando** si esegue la purga
- **Allora** l'intervallo passato resta occupato ma senza intestatario, e quello futuro è rimosso

**CA-5 — Diritti sempre accessibili**
- **Dato** un account con abbonamento `canceled` · **Quando** chiede l'esportazione · **Allora** la ottiene lo
  stesso

**CA-6 — Prova della purga**
- **Dato** una purga eseguita · **Quando** si guarda il registro delle purghe · **Allora** c'è la riga con chi,
  quando e i conteggi, e non contiene nessun dato personale

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend` e `compliance`);
- [ ] prove di **unità** sull'ambito della purga e di **integrazione** sull'esportazione e sulla cancellazione;
- [ ] prova di **isolamento fra account**: la purga di un account non tocca l'altro;
- [ ] **prova end-to-end**: *nessun impatto* sulla superficie applicativa dell'app; i diritti dell'interessato
      hanno il proprio percorso di piattaforma, e il registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) lo dichiara;
- [ ] **traduzioni**: non applicabile all'interfaccia; il manifesto è in italiano e inglese;
- [ ] **manifesto dei dati** completo per tutte le tabelle esistenti;
- [ ] **registro delle decisioni** compilato: **la decisione sull'articolo 9 e la sua motivazione** — è la voce
      più importante di tutta l'applicazione — più il comportamento della purga sull'agenda passata;
- [ ] avvio locale invariato;
- [ ] documentazione di conformità aggiornata.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0011` | serve la tabella dei clienti |
| **decisione dello sviluppatore sull'articolo 9** | cambia base giuridica, durate e condizioni d'uso |

## 7. Fuori ambito

- le voci di manifesto delle epiche successive (promemoria, eventi della pagina pubblica, collegamenti ai
  calendari): le aggiunge ciascuna storia che introduce il dato, come vuole la regola;
- la valutazione d'impatto sulla protezione dei dati, se la decisione sull'articolo 9 la rende necessaria: è un
  documento, non codice.

## 8. Punti aperti

**Durata di conservazione.** I 24 mesi proposti al §6 della descrizione non vengono da una norma: non ho trovato
un obbligo di conservazione delle prenotazioni. Vanno confermati, ed è ragionevole che il valore sia
**configurabile per account**, perché uno studio e un parrucchiere hanno esigenze diverse. Chi lo chiude: lo
sviluppatore, con il supporto legale.
