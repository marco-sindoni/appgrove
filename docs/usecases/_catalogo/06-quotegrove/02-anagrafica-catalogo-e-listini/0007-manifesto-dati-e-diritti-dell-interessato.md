# 0007 — Manifesto dei dati e diritti dell'interessato

**Applicazione**: 06 — QuoteGrove (`preventivi`) · **Epica**: 02 — Anagrafica, catalogo e listini
**Storia**: `0007` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0006`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che riceve dal proprio cliente la richiesta di sapere quali dati abbiamo su di lui
> voglio poter esportare tutto quello che QuoteGrove sa di quella persona, e poterlo cancellare davvero
> così da rispondere alla richiesta in giorni e non in settimane, e senza dimenticare una tabella.

**Contesto.** La storia `0006` ha portato dentro l'app i primi dati di persone. Da quel momento l'app ha un
obbligo che non si può rimandare: dichiarare cosa tratta e saperlo restituire e cancellare. Si fa adesso, con
poche tabelle, perché è il momento in cui costa meno: ogni storia successiva aggiungerà le proprie voci a un
impianto che già esiste. Farlo alla fine significa rileggere quindici migrazioni per capire dove sono finiti i
dati delle persone — ed è così che se ne dimentica una.

## 2. Requisiti funzionali

1. **RF-1** — Esiste il manifesto `docs/compliance/manifests/preventivi.yaml` con una voce per ogni campo che
   riguarda una persona, ciascuna con luogo, interessati, categoria di dato, finalità, base giuridica e durata di
   conservazione, **in italiano e in inglese**.
2. **RF-2** — Esiste `PreventiviDataContract` che implementa `AppDataContract` con `appId()`, `exportData(scope)`,
   `purgeData(scope)` e `manifest()`.
3. **RF-3** — `exportData` restituisce, per un interessato o per un intero account, tutte le righe di tutte le
   tabelle che lo riguardano, in un formato leggibile e con i riferimenti fra le entità mantenuti.
4. **RF-4** — `purgeData` esegue una cancellazione **fisica** e lascia una riga di prova nel registro delle
   purghe: sostituire il nome con un codice non è cancellare.
5. **RF-5** — Il controllo automatico che fa fallire la compilazione quando un campo annotato `@PersonalData` non
   è dichiarato nel manifesto è verde, e resta verde.
6. **RF-6** — L'esportazione e la cancellazione restano accessibili anche quando l'app è disabilitata o
   l'abbonamento è scaduto.

## 3. Requisiti tecnici

- **RT-1 — Dati personali (§10).** Manifesto obbligatoriamente bilingue; ogni tabella con dati di persone deve
  comparire **sia** in `exportData` **sia** in `purgeData`; i dati stanno a riposo solo in regioni europee;
  nessun tracciamento dentro l'app.
- **RT-2 — Isolamento fra account (§1).** Esportazione e cancellazione operano sempre dentro un account, letto
  dal token verificato: una richiesta non può mai attraversare due account.
- **RT-3 — Interfaccia di programmazione (§2).** Il contratto è invocato dalla piattaforma, non da una rotta
  pubblica dell'app; gli errori escono in `problem+json`.
- **RT-4 — Abbonamento (§13).** I diritti dell'interessato restano accessibili in ogni stato dell'abbonamento.
- **RT-5 — Registrazione eventi (§14).** `esportazione eseguita`, `purga eseguita` con `tenant_id`, `app_id`,
  `user_id`, correlazione e **conteggi** — mai i contenuti esportati.
- **RT-6 — Prove (§11).** Prova di integrazione che crea dati in due account, esporta per uno e verifica che
  l'altro sia intatto; prova che dopo la purga la riga non esiste più nel database, non che è «vuota».

## 4. Criteri di accettazione

**CA-1 — Esportazione completa**
- **Dato** un destinatario con due preventivi, tre invii e una prova di accettazione
- **Quando** si esporta ciò che lo riguarda
- **Allora** l'esito contiene righe da **tutte** le tabelle che lo citano, e nessuna tabella dichiarata nel
  manifesto resta fuori

**CA-2 — Cancellazione fisica**
- **Dato** lo stesso destinatario · **Quando** si esegue la purga · **Allora** le righe non esistono più nel
  database, resta una riga nel registro delle purghe, e nessun dato di altri interessati è stato toccato

**CA-3 — Campo annotato e non dichiarato**
- **Dato** un campo nuovo annotato `@PersonalData` e non aggiunto al manifesto · **Quando** si compila
- **Allora** la compilazione fallisce con un messaggio che indica il campo

**CA-4 — Manifesto monolingue**
- **Dato** una voce del manifesto con il solo testo italiano · **Quando** gira il controllo di conformità
- **Allora** la suite è rossa e indica la voce incompleta

**CA-5 — Diritti accessibili con abbonamento scaduto**
- **Dato** un account in stato `canceled` · **Quando** chiede l'esportazione dei propri dati · **Allora**
  l'esportazione funziona, anche se l'app risponde `402` a tutto il resto

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (backend + compliance);
- [ ] prove di **unità** sul contratto e di **integrazione** su esportazione e purga con database effimero;
- [ ] prova di **isolamento fra account** su entrambe le operazioni;
- [ ] **prova end-to-end**: nessun impatto sulla superficie dell'app; il percorso di piattaforma sui diritti
      dell'interessato esiste già ed è di piattaforma;
- [ ] **traduzioni**: non applicabile all'interfaccia; il manifesto è bilingue per obbligo;
- [ ] **manifesto dei dati** completo per le tabelle esistenti a oggi;
- [ ] **registro delle decisioni** compilato: **ogni** base giuridica e **ogni** durata di conservazione scelta,
      con il perché e con l'indicazione che sono state approvate dallo sviluppatore;
- [ ] avvio locale invariato.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0006` | senza dati di persone non c'è niente da dichiarare |
| approvazione dello sviluppatore su basi giuridiche e durate | è una fermata di escalation: nessun agente compila un manifesto da solo |

## 7. Fuori ambito

- le voci relative a entità che non esistono ancora (versioni, prove, eventi): le aggiunge la storia che le crea,
  ciascuna nel proprio commit;
- l'informativa verso il destinatario sulla pagina pubblica: storia `0018`.

## 8. Punti aperti

**Il conflitto fra cancellazione e prova.** La prova dell'accettazione (storia `0019`) è insieme un dato personale
e la prova di un contratto: se l'interessato ne chiede la cancellazione, quale diritto prevale non lo decide
questa applicazione. Qui la prova viene esportata e cancellata come tutto il resto; l'eventuale eccezione la
stabilisce lo sviluppatore con revisione legale (punto 4 dei rischi della descrizione dell'applicazione).
