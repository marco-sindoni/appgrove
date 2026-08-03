# 0012 — Modelli di messaggio

**Applicazione**: 03 — CashGrove (`crediti`) · **Epica**: 03 — Solleciti automatici
**Storia**: `0012` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0011`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare
> voglio che il testo del sollecito sia il mio, con i numeri giusti inseriti da soli, e nella lingua del cliente
> così da non riscriverlo ogni volta e da non mandare mai un messaggio con l'importo sbagliato.

**Contesto.** La sequenza dice *quando*; questa storia dice *cosa*. È il punto in cui l'app tocca la reputazione del
cliente: un sollecito con l'importo sbagliato o il nome di un altro fa più danno del ritardo che voleva risolvere. È
anche il punto in cui entrano due vincoli di condotta rilevati nell'analisi: il messaggio va **solo** al debitore, e
l'oggetto non deve rivelare a chi passa davanti allo schermo che si tratta di un mancato pagamento
([documento capofila](../application-description.md) §2.3, punto 4).

## 2. Requisiti funzionali

1. **RF-1** — Esistono modelli di messaggio con oggetto e corpo, associati a un tono (cortese, fermo, formale) e a una
   lingua fra quelle previste dai debitori.
2. **RF-2** — Il corpo ammette segnaposto da un elenco **chiuso**: denominazione del debitore, numero del documento,
   data di scadenza, importo residuo, giorni di ritardo, denominazione del creditore, elenco dei documenti scaduti.
3. **RF-3** — Un segnaposto sconosciuto o malformato impedisce il salvataggio del modello, indicando quale e dove.
4. **RF-4** — L'anteprima mostra il messaggio compilato su un credito reale scelto dall'utente, così come lo riceverà
   il debitore.
5. **RF-5** — Al momento dell'invio si sceglie il modello nella **lingua preferita del debitore**; se manca, si usa
   quella predefinita dell'account, e la scheda del credito lo dichiara.
6. **RF-6** — L'account nasce con un corredo di modelli già pronti per i tre toni e per le cinque lingue, così che
   nessuno debba scrivere un testo per cominciare.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura e scrittura dell'entità `ModelloMessaggio` filtra per
  `tenant_id` preso dal token verificato. L'anteprima compila su un credito dello **stesso** account e su nessun altro.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `GET`, `POST`, `PATCH`, `DELETE /api/crediti/v1/modelli` e
  `POST /api/crediti/v1/modelli/{id}/anteprima`; corpo validato; errori in `application/problem+json`; definizione
  OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione per la tabella `modello_messaggio` sullo schema `app_crediti`, con
  `tenant_id`, chiave UUID versione 7, colonne di controllo e cancellazione logica.
- **RT-4 — Modulo frontend (§3, §5).** Editor del modello con elenco dei segnaposto disponibili e anteprima affiancata;
  solo token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Attenzione alla distinzione che questa storia rende evidente: le stringhe
  dell'**interfaccia** passano dallo spazio-nomi `crediti` e sono presenti in `en, it, fr, es, de`; i **modelli di
  messaggio** sono invece contenuti del cliente, scritti da lui, in un elenco di lingue che può differire. Il corredo
  iniziale copre le stesse cinque lingue perché è materiale nostro.
- **RT-6 — Varchi e quota (§6, §7).** I modelli non consumano quota; modificarli richiede ruolo `owner` o `admin`.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento in questa storia. La compilazione del messaggio è
  però la base di `prepara_sollecito` (storia `0029`), che restituisce **esattamente** ciò che l'anteprima mostra.
- **RT-8 — Dati personali (§10).** Il modello non contiene dati personali; il messaggio **compilato** sì. Il manifesto
  registra che l'anteprima produce un testo con dati personali che non viene conservato, e che il testo effettivamente
  inviato viene invece conservato nel registro dei solleciti (storia `0017`).
- **RT-9 — Registrazione eventi (§14).** «Modello creato» e «modello modificato» sono registrati con `tenant_id`,
  `app_id`, `user_id` e identificativo di correlazione. **Il testo compilato non finisce mai nei registri**: contiene
  nome e importo.
- **RT-10 — Condotta verso il debitore.** L'oggetto del messaggio predefinito non contiene parole che rivelino il
  mancato pagamento a chi legge la notifica sullo schermo di un telefono; nessun modello prevede destinatari diversi
  dal debitore, e l'app non offre un campo «per conoscenza».

## 4. Criteri di accettazione

**CA-1 — Anteprima corretta**
- **Dato** un modello con i segnaposto del numero di documento e dell'importo residuo, e un credito da 1.200 € scaduto
  da 12 giorni
- **Quando** si chiede l'anteprima su quel credito
- **Allora** il testo mostra il numero giusto, «1.200,00 €» formattato secondo la lingua del modello e «12 giorni»

**CA-2 — Segnaposto sconosciuto**
- **Dato** un modello che contiene un segnaposto inesistente · **Quando** si tenta di salvarlo · **Allora** la richiesta
  è respinta con `400`, indicando il segnaposto e la sua posizione nel testo

**CA-3 — Lingua del debitore**
- **Dato** un debitore con lingua preferita francese e un modello francese disponibile
- **Quando** si prepara il sollecito
- **Allora** viene scelto il modello francese

**CA-4 — Lingua mancante**
- **Dato** un debitore con lingua preferita portoghese e nessun modello portoghese
- **Quando** si prepara il sollecito
- **Allora** viene usato il modello nella lingua predefinita dell'account e la scheda del credito dichiara la
  sostituzione

**CA-5 — Nessun destinatario in copia**
- **Dato** l'editor del modello · **Quando** si cerca un campo per aggiungere un destinatario in copia · **Allora** non
  esiste, e una nota spiega che il sollecito va solo al debitore

**CA-6 — Isolamento fra account**
- **Dato** due account `A` e `B` · **Quando** un utente di `A` chiede l'anteprima di un proprio modello su un credito
  di `B` · **Allora** riceve l'errore di risorsa non trovata

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (backend, frontend, compliance);
- [ ] prove di **unità** sulla sostituzione dei segnaposto e sulla validazione, di **integrazione** sull'anteprima;
- [ ] prova di **isolamento fra account** sui modelli e sull'anteprima;
- [ ] **prova end-to-end**: *rimando* alla storia `0031`;
- [ ] **traduzioni** dell'interfaccia presenti in tutte e cinque le lingue, e corredo di modelli iniziali nelle stesse
      cinque lingue;
- [ ] **manifesto dei dati** aggiornato con la nota sul testo compilato;
- [ ] **registro delle decisioni** compilato, in particolare sull'elenco chiuso dei segnaposto e sull'assenza del campo
      «per conoscenza»;
- [ ] contratto degli **strumenti conversazionali**: nessuna aggiunta in questa storia;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0011` | Il modello si aggancia a un passo della sequenza |

## 7. Fuori ambito

- La generazione automatica del testo con un modello linguistico: rimandata. Sarebbe l'unico punto dell'app esposto alla
  commoditizzazione descritta dal catalogo (§8): il valore sta nel flusso, non nella scrittura.
- L'invio vero: storia `0014`.
- I modelli per i canali brevi, che hanno vincoli di lunghezza e approvazione propri: storia `0015`.

## 8. Punti aperti

Nessuno.
