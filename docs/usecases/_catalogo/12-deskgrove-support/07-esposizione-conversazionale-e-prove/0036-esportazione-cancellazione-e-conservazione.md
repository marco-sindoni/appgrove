# 0036 — Esportazione, cancellazione e conservazione dei dati

**Applicazione**: 12 — DeskGrove Support (`helpdesk`) · **Epica**: 07 — Esposizione conversazionale e prove end-to-end
**Storia**: `0036` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0002`, `0016`, `0027`, `0032`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare di un'azienda che usa DeskGrove per assistere i propri clienti
> voglio poter esportare o cancellare i dati di **un singolo mio cliente** quando me lo chiede, e decidere per
> quanto tempo conservo le conversazioni
> così da poter onorare da solo un obbligo che è mio, senza dover aprire una richiesta ad appgrove e senza
> aspettare.

**Contesto.** Su questa applicazione appgrove non è titolare del trattamento: è **responsabile**, e tratta per
conto dell'azienda cliente i dati dei clienti finali di quest'ultima. La conseguenza pratica è che i diritti delle
persone non arrivano a noi ma **al cliente**, e il cliente deve poterli onorare **dentro l'app**. È una differenza
che nessun'altra applicazione del catalogo ha con questa intensità, e va costruita, non dichiarata.

La stessa asimmetria vale per la conservazione: non esiste un termine di legge per «una conversazione di
assistenza», il criterio è la minimizzazione e a fissare il termine è il titolare. Perciò la durata **non può
essere una costante nel codice**: è un parametro dell'account. È anche l'unica leva che tiene sotto controllo due
problemi insieme — l'accumulo di testo libero che può contenere di tutto (§6 del documento capofila) e la crescita
dell'archivio degli allegati, che nessuna metrica di quota limita.

## 2. Requisiti funzionali

1. **RF-1** — Il contratto dati dell'app espone l'esportazione e la cancellazione di **tutto** l'account, con
   l'elenco completo delle tabelle: richiedenti, richieste, messaggi, allegati e i file nell'archivio, operatori,
   indagini di soddisfazione, risposte predefinite, bozze.
2. **RF-2** — Esiste, in più, l'**esportazione per singolo richiedente**: l'azienda cliente esporta tutto ciò che
   riguarda una persona, in un formato leggibile, per consegnarlo a chi lo ha chiesto.
3. **RF-3** — Esiste la **cancellazione per singolo richiedente**: rimuove fisicamente i dati di quella persona —
   anagrafica, messaggi, allegati, voto e commento — e lascia una riga di prova nel registro delle purghe.
4. **RF-4** — La cancellazione di un richiedente **non** distrugge le misure aggregate già calcolate (quante
   richieste, quanto tempo di risposta): quelle non identificano nessuno e servono al cliente.
5. **RF-5** — La **durata di conservazione** è un parametro dell'account, con valori distinti per le conversazioni,
   per gli allegati e per l'indirizzo di rete raccolto dal modulo pubblico; ogni valore ha un massimo consentito e
   un valore predefinito prudente.
6. **RF-6** — Una lavorazione periodica cancella ciò che ha superato la propria durata, in modo tracciato e
   ripetibile, senza toccare le richieste ancora aperte.
7. **RF-7** — Esportazione e cancellazione restano accessibili **anche** quando l'app è disabilitata o
   l'abbonamento è scaduto.

## 3. Requisiti tecnici

- **RT-1 — Dati personali (§10).** L'app implementa il contratto `AppDataContract` come `HelpdeskDataContract`, con
  `appId()`, `exportData(scope)`, `purgeData(scope)` e `manifest()`. **Ogni** tabella che contiene dati personali
  compare in entrambi: dimenticarne una è il difetto di conformità più probabile in un'app nuova, e qui la
  candidata a essere dimenticata è la più insidiosa — i **file** nell'archivio degli allegati, che non sono righe di
  tabella. La cancellazione è **fisica**: sostituire il nome del richiedente con un codice **non è cancellare**.
- **RT-2 — Isolamento fra account (§1).** Esportazione e cancellazione operano sempre e solo sull'account del token
  verificato; l'identificativo del richiedente da cancellare si risolve dentro quell'account e mai globalmente.
- **RT-3 — Interfaccia di programmazione (§2).** Rotte
  `POST /api/helpdesk/v1/requesters/{id}/export`, `POST /api/helpdesk/v1/requesters/{id}/erase`,
  `GET|PUT /api/helpdesk/v1/settings/retention`; corpo validato; errori in `application/problem+json`; definizione
  OpenAPI aggiornata nello stesso commit.
- **RT-4 — Persistenza (§8).** Migrazione sullo schema `app_helpdesk` per i parametri di conservazione dell'account.
  Qui la **cancellazione logica non basta**: la purga rimuove le righe davvero, e la traccia resta nel registro
  delle purghe, non nella tabella cancellata.
- **RT-5 — Varchi (§6, §13).** I diritti delle persone restano accessibili anche con abbonamento non attivo: non
  sono una funzione commerciale. La modifica dei parametri di conservazione richiede invece il ruolo di titolare
  dell'account o di amministratore.
- **RT-6 — Modulo frontend e cinque lingue (§3, §4, §5).** Una sezione «Conservazione e dati» dentro le
  Impostazioni del modulo, con l'avvertenza esplicita che la cancellazione è definitiva; conferma con
  digitazione del nome del richiedente per le operazioni irreversibili; solo token del sistema di design; tutte le
  stringhe in `en, it, fr, es, de`.
- **RT-7 — Esposizione conversazionale (§12).** **Nessuno strumento**: la cancellazione di dati è un effetto
  irreversibile e non si comanda da una chat. La regola del catalogo la elenca esplicitamente fra gli effetti che
  richiedono conferma umana; qui si va oltre e non la si espone affatto, perché il percorso deliberato
  dell'interfaccia è di per sé il presidio.
- **RT-8 — Registrazione eventi (§14).** «Esportazione richiesta», «cancellazione eseguita», «purga periodica
  conclusa» si registrano con `tenant_id`, `app_id`, `user_id`, identificativo di correlazione e conteggi; mai nomi,
  indirizzi o contenuti.

## 4. Criteri di accettazione

**CA-1 — L'esportazione contiene tutto**
- **Dato** un account con richieste, messaggi, allegati, operatori, voti di soddisfazione e risposte predefinite
- **Quando** si esporta l'intero account
- **Allora** l'esportazione contiene ogni tabella dichiarata nel manifesto, compresi i file degli allegati, e non
  manca nessuna delle tabelle presenti nel manifesto stesso

**CA-2 — La cancellazione per persona è fisica**
- **Dato** un richiedente con tre richieste, dodici messaggi, due allegati e un voto di soddisfazione
- **Quando** l'azienda cliente esegue la cancellazione per quel richiedente
- **Allora** nessuna di quelle righe esiste più nel database, i file non esistono più nell'archivio, e nel registro
  delle purghe resta una riga con chi, quando e quante righe

**CA-3 — Le misure aggregate sopravvivono**
- **Dato** lo stesso account dopo la cancellazione
- **Quando** si apre il cruscotto del servizio sul periodo interessato
- **Allora** i conteggi e i tempi medi non cambiano: non identificano nessuno

**CA-4 — La conservazione è del cliente**
- **Dato** un account che imposta la conservazione delle conversazioni a dodici mesi
- **Quando** la lavorazione periodica gira
- **Allora** le richieste chiuse da più di dodici mesi vengono cancellate, quelle ancora aperte no, e il conteggio
  di ciò che è stato cancellato è tracciato

**CA-5 — Isolamento fra account**
- **Dato** due account `A` e `B` con un richiedente omonimo e lo stesso indirizzo di posta
- **Quando** `A` esegue la cancellazione per il proprio richiedente
- **Allora** i dati di `B` restano intatti: l'identità del richiedente vive dentro l'account, non fra gli account

**CA-6 — I diritti non si bloccano**
- **Dato** un account con abbonamento disdetto
- **Quando** si chiede l'esportazione dei dati
- **Allora** l'operazione riesce

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend`, `frontend` e `compliance`; l'intera suite prima del
      commit);
- [ ] prove di **unità** sul calcolo di ciò che è scaduto, e di **integrazione** su esportazione e purga con
      database effimero, migrazioni vere e archivio simulato;
- [ ] prova di **isolamento fra account** su esportazione, cancellazione e parametri di conservazione;
- [ ] prova che **ogni** tabella del manifesto compare sia in esportazione sia in cancellazione — è il controllo che
      impedisce il difetto più probabile;
- [ ] **prova end-to-end**: la storia `0037` include nel percorso `[J-HELPDESK]` il passo della cancellazione per
      richiedente e possiede la voce del registro di copertura;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati** completo, in italiano e inglese, e coerente con il codice: nessun campo annotato
      mancante, nessuna voce senza campo;
- [ ] **registro delle decisioni** compilato, con annotati i valori predefiniti e massimi di conservazione e il
      perché la durata è un parametro dell'account e non una costante;
- [ ] contratto degli **strumenti conversazionali**: nessuno strumento esposto, e il motivo è scritto;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storia `0002` di questa app | Le tabelle portanti e le prime voci del manifesto nascono lì |
| Storia `0016` di questa app | Gli allegati sono file, non righe: la cancellazione deve raggiungere anche l'archivio |
| Storia `0027` di questa app | Il commento dell'indagine di soddisfazione è testo libero di una persona esterna e va cancellato con lei |
| Storia `0032` di questa app | I gettoni dei collegamenti di stato riguardano una persona: scadono e vanno revocati con la cancellazione |
| Lavorazione periodica di conservazione della piattaforma (UC 0035) | La purga dell'app deve essere coerente con la conservazione di piattaforma, non un secondo meccanismo parallelo |

## 7. Fuori ambito

- **L'informativa da mostrare al cliente finale** sul modulo di contatto: il testo lo scrive il **titolare**, cioè
  l'azienda cliente. L'app deve permettere di inserirlo — è la storia `0013` — non scriverlo al posto suo.
- **Il contratto di nomina a responsabile del trattamento** fra appgrove e l'azienda cliente: è un documento, non
  codice, e appartiene alla revisione legale.
- **La cancellazione dell'intero account**: è un percorso di piattaforma, non dell'app.

## 8. Punti aperti

- ⚠️ **I valori predefiniti e massimi di conservazione** (proposta del documento capofila: 24 mesi per le
  conversazioni, 12 per gli allegati, 30 giorni per l'indirizzo di rete) sono **proposte**, non decisioni: toccano
  insieme la conformità, il costo di archiviazione e l'aspettativa del cliente. Li conferma lo sviluppatore, e la
  revisione legale pre-go-live li rivede.
- ⚠️ **Serve una valutazione d'impatto sulla protezione dei dati?** Gli elementi che la rendono probabile ci sono
  tutti: trattamento su larga scala di dati di terzi, categorie particolari non escludibili perché il canale è
  aperto al pubblico, ruolo di responsabile del trattamento. Non lo decide un agente: va portato alla revisione
  legale.
- **Che cosa succede ai dati alla fine del contratto**: il responsabile del trattamento deve cancellarli o
  restituirli su scelta del titolare. Oggi la piattaforma ha un percorso di chiusura dell'account; se la scelta
  «restituisci invece di cancellare» debba essere offerta è una decisione di prodotto aperta.
- **La conservazione come parametro del piano** (§5 del documento capofila): la proposta è che il piano superiore
  consenta durate più lunghe. È l'unico modo trovato per governare la crescita dell'archivio senza introdurre una
  seconda metrica di quota, ma è una decisione di listino e quindi dello sviluppatore.
