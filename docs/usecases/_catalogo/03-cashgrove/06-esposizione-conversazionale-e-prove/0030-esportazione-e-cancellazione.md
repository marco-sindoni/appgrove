# 0030 — Esportazione e cancellazione

**Applicazione**: 03 — CashGrove (`crediti`) · **Epica**: 06 — Esposizione conversazionale e prove end-to-end
**Storia**: `0030` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: tutte le storie che introducono una tabella (`0002`, `0008`, `0009`, `0010`, `0011`, `0012`, `0013`, `0014`, `0015`, `0016`, `0017`, `0018`, `0019`, `0020`, `0021`, `0022`, `0025`, `0026`, `0027`, `0029`)
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come responsabile della conformità della piattaforma
> voglio che l'esportazione e la cancellazione dei dati di CashGrove coprano **tutte** le tabelle, senza dimenticarne
> nessuna
> così da poter rispondere a una richiesta di un interessato senza scoprire, mesi dopo, che una tabella era rimasta
> fuori.

**Contesto.** Ogni storia precedente ha fatto la sua parte: ha aggiunto le proprie voci al manifesto e le proprie
tabelle al contratto dati. Questa storia chiude il cerchio e verifica che il conto torni — perché il difetto di
conformità più probabile in un'app nuova è **dimenticare una tabella**, e in CashGrove le tabelle sono più di venti.
C'è poi un problema che nessuna storia precedente poteva risolvere da sola: i dati riguardano persone che **non sono
nostri utenti né utenti del cliente**. Il debitore non ha mai scelto di stare nel sistema, e può chiedere la
cancellazione mentre il creditore ha bisogno di conservare la prova dell'attività di recupero per difendere un diritto.
Questa storia mette il conflitto sul tavolo; non lo decide.

## 2. Requisiti funzionali

1. **RF-1** — Il contratto `CreditiDataContract` implementa `appId()`, `manifest()`, `exportData(ambito)` e
   `purgeData(ambito)` coprendo **tutte** le tabelle dello schema `app_crediti` che contengono dati riferibili a una
   persona.
2. **RF-2** — L'esportazione produce un insieme leggibile e completo, organizzato per entità, con i riferimenti fra
   entità comprensibili senza conoscere lo schema.
3. **RF-3** — La cancellazione è **fisica**: le righe spariscono. Sostituire i nomi con dei codici non è cancellare e
   non è ammesso come esito.
4. **RF-4** — La cancellazione lascia una riga di prova nel registro delle purghe: cosa è stato cancellato, quando, per
   quale richiesta — senza contenere i dati cancellati.
5. **RF-5** — Un controllo automatico confronta l'elenco delle tabelle dello schema con quelle coperte dal contratto e
   **fa fallire la compilazione** se una tabella nuova non è stata classificata (coperta oppure esplicitamente esclusa
   con motivo).
6. **RF-6** — Esportazione e cancellazione restano accessibili anche quando l'app è disabilitata o l'abbonamento è
   scaduto.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Esportazione e cancellazione operano su un ambito che comprende sempre
  l'account; nessuna operazione può attraversare due account, nemmeno per un debitore che compare in entrambi.
- **RT-2 — Interfaccia di programmazione (§2).** L'app non espone rotte proprie per i diritti dell'interessato: risponde
  al contratto che la piattaforma invoca. Errori in `application/problem+json`.
- **RT-3 — Persistenza (§8).** Nessuna tabella nuova. La cancellazione fisica deve rispettare l'ordine delle dipendenze
  logiche e restare una operazione unica: una purga interrotta a metà è peggio di una purga non fatta.
- **RT-4 — Modulo frontend (§3, §5).** Nessuna schermata nuova nel modulo: i diritti dell'interessato si esercitano
  dalla sezione «I miei dati» della piattaforma. Il modulo aggiunge solo, nella scheda del debitore, l'indicazione di
  quali dati lo riguardano e per quanto si conservano.
- **RT-5 — Cinque lingue (§4).** Le poche stringhe aggiunte passano dallo spazio-nomi `crediti` e sono presenti in
  `en, it, fr, es, de`. Il **manifesto** invece vuole due sole lingue, italiano e inglese: sono due elenchi diversi e
  non vanno confusi.
- **RT-6 — Varchi e quota (§6, §7).** I diritti dell'interessato non consumano quota e **non** passano dal varco
  dell'abbonamento: restano accessibili con `canceled`.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento: esportare o cancellare dati personali su richiesta di
  un assistente non è un vantaggio, è un rischio. Esclusione deliberata, annotata nel contratto.
- **RT-8 — Dati personali (§10).** È l'oggetto della storia. Elenco delle tabelle che devono comparire in `exportData`
  e `purgeData`: `debitore`, `credito`, `incasso`, `imputazione`, `cambio_stato_credito`, `importazione`,
  `sequenza_solleciti`, `passo_sollecito`, `modello_messaggio`, `invio_programmato`, `mittente_posta`, `canale_breve`,
  `sospensione`, **`sollecito_inviato`** (la più facile da dimenticare, e quella che contiene destinatario e testo),
  `promessa_di_pagamento`, `contestazione`, `addebito_di_mora`, `messa_in_mora`, `gettone_pubblico`,
  `segnalazione_debitore`, `punteggio_di_rischio`, `previsione_incassi`, `esportazione`, `bozza_operazione`. Esclusa
  con motivo: `tasso_mora`, che è un dato di legge comune e non contiene persone.
- **RT-9 — Registrazione eventi (§14).** Gli eventi «esportazione eseguita» e «purga eseguita» sono registrati con
  `tenant_id`, `app_id`, ambito e identificativo di correlazione, **senza** i dati trattati.

## 4. Criteri di accettazione

**CA-1 — Nessuna tabella dimenticata**
- **Dato** lo schema `app_crediti` con tutte le sue tabelle
- **Quando** il controllo automatico confronta schema e contratto
- **Allora** ogni tabella risulta coperta o esplicitamente esclusa con motivo; una tabella nuova non classificata fa
  fallire la compilazione

**CA-2 — Esportazione completa**
- **Dato** un account con dati in tutte le entità
- **Quando** si esegue l'esportazione
- **Allora** l'insieme prodotto contiene almeno una voce per ciascuna entità popolata, compresi i solleciti inviati con
  il loro testo

**CA-3 — Cancellazione fisica**
- **Dato** una purga eseguita sull'ambito di un debitore
- **Quando** si interroga direttamente il database
- **Allora** le righe non esistono più — non sono presenti con i campi sostituiti da codici

**CA-4 — Prova della purga**
- **Dato** una purga eseguita · **Quando** si consulta il registro delle purghe · **Allora** c'è una riga con ambito,
  istante e riferimento della richiesta, e nessun dato personale

**CA-5 — Accessibile senza abbonamento**
- **Dato** un account con abbonamento `canceled` · **Quando** si esercita l'esportazione · **Allora** funziona

**CA-6 — Isolamento fra account**
- **Dato** lo stesso debitore presente in due account · **Quando** si purga l'ambito di uno · **Allora** i dati
  dell'altro restano intatti

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (backend e compliance);
- [ ] prove di **unità** sul controllo di copertura schema↔contratto, di **integrazione** su esportazione e purga con
      database effimero popolato in tutte le entità;
- [ ] prova di **isolamento fra account** sulla purga;
- [ ] **prova end-to-end**: *nessun impatto sul percorso applicativo* — i diritti dell'interessato hanno il proprio
      percorso di piattaforma; qui si verifica con prove di integrazione;
- [ ] **traduzioni** presenti in tutte e cinque le lingue per le poche stringhe aggiunte;
- [ ] **manifesto dei dati** riletto **integralmente** in italiano e inglese contro le tabelle esistenti: è la verifica
      finale, non una formalità;
- [ ] **registro delle decisioni** compilato, in particolare sulla soluzione data al conflitto fra cancellazione e prova;
- [ ] contratto degli **strumenti conversazionali**: esclusione deliberata, annotata;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| tutte le storie che introducono una tabella | Non si può verificare la copertura finché le tabelle non ci sono tutte |

## 7. Fuori ambito

- L'interfaccia con cui l'interessato presenta la richiesta: è di piattaforma.
- La cancellazione dei dati alla chiusura dell'account: è il percorso di piattaforma, che invoca lo stesso contratto.
- La conservazione delle copie di sicurezza: è di piattaforma e ha regole proprie.

## 8. Punti aperti

🛑 **Conflitto fra il diritto alla cancellazione del debitore e la conservazione della prova dell'attività di
recupero.** Il debitore può chiedere la cancellazione; il creditore ha un interesse legittimo a conservare i solleciti
inviati e la messa in mora per difendere un diritto, e la conservazione dei documenti commerciali ha durate proprie. Le
tre vie possibili — cancellare tutto, conservare il solo minimo probatorio per una durata dichiarata, sospendere la
cancellazione limitatamente al contenzioso in corso — hanno conseguenze diverse e **non le sceglie un agente**. È il
punto aperto n. 4 del documento capofila §11: **decide lo sviluppatore**, con la revisione legale pre-go-live.
