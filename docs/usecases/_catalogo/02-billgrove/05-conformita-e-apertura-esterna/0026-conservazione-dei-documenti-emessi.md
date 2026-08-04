# 0026 — Conservazione dei documenti emessi

**Applicazione**: 02 — BillGrove (`billing`) · **Epica**: 05 — Conformità e apertura verso l'esterno
**Storia**: `0026` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0012`, `0024`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che sa di dover tenere le fatture per dieci anni
> voglio essere sicuro che quello che ho emesso non si possa perdere, cambiare o cancellare per sbaglio
> così da non trovarmi senza documenti in caso di controllo, e da sapere che cosa è successo a ciascuno di essi.

**Contesto.** La legge impone la conservazione dei documenti per dieci anni dalla data di emissione, con
un'operazione da eseguire almeno una volta l'anno (§2.3 della descrizione). Questa storia **non** costruisce un
sistema di conservazione accreditato — è un servizio che si acquista — ma fa le due cose che dipendono da noi: rende
i documenti emessi **immodificabili e non cancellabili** dentro l'app, e prepara il passaggio verso un conservatore.
È anche la storia che risolve, sul piano tecnico, il conflitto fra cancellazione e obbligo fiscale sollevato nel §6
della descrizione.

## 2. Requisiti funzionali

1. **RF-1** — Un documento emesso non è cancellabile, né logicamente né fisicamente, per l'intero periodo di
   conservazione, da nessuna funzione dell'app.
2. **RF-2** — Esiste un registro immutabile che, per ogni documento emesso, riporta l'impronta del contenuto al
   momento dell'emissione.
3. **RF-3** — L'app rileva e segnala qualunque discordanza fra il documento e la sua impronta.
4. **RF-4** — Una richiesta di cancellazione dei dati di un interessato cancella l'**anagrafica** del cliente ma
   **non** i documenti emessi che lo riguardano; l'esito riporta esplicitamente che cosa non è stato cancellato e
   perché.
5. **RF-5** — È possibile produrre il pacchetto dei documenti di un periodo, con il relativo registro delle
   impronte, da consegnare a un conservatore.
6. **RF-6** — L'app mostra al titolare, in modo comprensibile, fino a quando ciascun documento va conservato.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Registro delle impronte e pacchetti di conservazione filtrano per
  `tenant_id` preso dal token verificato.
- **RT-2 — Interfaccia di programmazione (§2).** `GET /api/billing/v1/retention/{documentId}` (stato e scadenza
  della conservazione) e `POST /api/billing/v1/retention/packages` (pacchetto di periodo); errori in
  `application/problem+json`; definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V13__retention.sql` sullo schema `app_billing`: tabella
  `document_seal` con `tenant_id`, identificativo del documento, impronta, data e colonne di controllo. Il vincolo
  di non cancellabilità va imposto **anche a livello di base dati**, non solo nel codice applicativo: un blocco che
  vive in un solo punto è un blocco che prima o poi si aggira.
- **RT-4 — Modulo frontend (§3, §5).** Sulla scheda del documento, l'indicazione della conservazione con la data
  fino a cui vale; nella sezione «I miei dati», la spiegazione di che cosa la cancellazione non può togliere. Solo
  token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Le spiegazioni sulla conservazione e sui limiti della cancellazione passano dallo
  spazio-nomi `billing` e sono presenti in `en, it, fr, es, de`. Sono i testi che devono essere più chiari
  dell'app intera: se non si capiscono, la persona pensa che stiamo tenendo i suoi dati per comodità nostra.
- **RT-6 — Varchi e quota (§6, §13).** Nessun consumo di quota. **I diritti dell'interessato restano accessibili
  anche con app disabilitata o abbonamento scaduto**: è una regola di piattaforma e qui è particolarmente sensibile.
- **RT-7 — Esposizione conversazionale (§12).** **Nessuno strumento di scrittura**: nulla che riguardi
  conservazione o cancellazione passa dalla chat. Va dichiarato.
- **RT-8 — Dati personali (§10).** È la storia che **implementa** la posizione proposta nel §6 della descrizione:
  `purgeData` cancella fisicamente `customer` e le tabelle collegate, ma **non** `document`, che resta coperto
  dall'obbligo di legge; ogni purga lascia una riga nel registro delle purghe che dichiara che cosa è rimasto e per
  quale base giuridica. **Sostituire i nomi con dei codici non è cancellare**: qui non si pseudonimizza, si dichiara
  una eccezione motivata. La posizione va **confermata dallo sviluppatore con la revisione legale** prima
  dell'implementazione.
- **RT-9 — Registrazione eventi (§14).** Gli eventi `impronta registrata`, `discordanza rilevata`, `pacchetto di
  conservazione prodotto` e `cancellazione parziale eseguita` sono registrati con `tenant_id`, `app_id`, `user_id` e
  identificativo di correlazione, senza dati personali.

## 4. Criteri di accettazione

**CA-1 — Documento non cancellabile**
- **Dato** una fattura emessa
- **Quando** una qualunque funzione dell'app tenta di cancellarla, compresa la purga dei dati
- **Allora** la cancellazione è impedita e l'esito lo dichiara

**CA-2 — Impronta e discordanza**
- **Dato** una fattura emessa con la sua impronta registrata
- **Quando** si verifica l'integrità del documento
- **Allora** la verifica passa; se il contenuto fosse alterato fuori dall'app, la verifica segnala la discordanza

**CA-3 — Cancellazione parziale dell'interessato**
- **Dato** un cliente persona fisica con due fatture emesse
- **Quando** si esegue una richiesta di cancellazione dei suoi dati
- **Allora** l'anagrafica è cancellata fisicamente, le fatture restano, e l'esito elenca che cosa è rimasto e per
  quale motivo di legge

**CA-4 — Pacchetto di conservazione**
- **Dato** un anno di documenti emessi
- **Quando** si chiede il pacchetto del periodo
- **Allora** si ottiene l'insieme dei documenti con il registro delle impronte

**CA-5 — Diritti sempre accessibili**
- **Dato** un account con abbonamento `canceled`
- **Quando** chiede l'esportazione dei propri dati · **Allora** la ottiene

**CA-6 — Isolamento fra account**
- **Dato** due account con documenti emessi
- **Quando** un utente di `A` chiede un pacchetto, anche forzando l'identificativo di `B`
- **Allora** il pacchetto contiene solo documenti di `A`

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend`, `frontend`, `compliance`; l'intera suite prima del
      commit);
- [ ] prove di **unità** sul calcolo dell'impronta e di **integrazione** sulla purga parziale, con database
      effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** su impronte e pacchetti;
- [ ] **prova end-to-end**: *coprire ora* — passo «tenta di cancellare una fattura emessa e verifica che sia
      impedito» del percorso `[J-BILLING]`; registro di copertura aggiornato;
- [ ] **traduzioni** presenti in tutte e cinque le lingue, con particolare cura ai testi sulla cancellazione;
- [ ] **manifesto dei dati** aggiornato: conservazione decennale e base giuridica dell'obbligo di legge dichiarate
      su tutte le voci del documento;
- [ ] **registro delle decisioni** compilato, con annotata l'eccezione alla cancellazione fisica e la sua
      motivazione;
- [ ] contratto degli **strumenti conversazionali**: nessuno, con la motivazione scritta;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata: il comportamento della cancellazione cambia rispetto allo standard di piattaforma e
      va descritto dove la piattaforma lo descrive.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0012` | Si conserva ciò che è stato emesso |
| storia `0024` | Il pacchetto di conservazione contiene la forma canonica |
| Punto aperto 3 del §11 della descrizione | La posizione sulla cancellazione va confermata prima di implementare |

## 7. Fuori ambito

- la conservazione **accreditata** presso un conservatore: è un servizio da acquistare, e il fornitore va dichiarato
  come responsabile esterno del trattamento (storia `0025` per il meccanismo);
- la marcatura temporale e la firma digitale: fanno parte del servizio di conservazione, non di BillGrove;
- la cancellazione dei documenti alla scadenza dei dieci anni: rimandata, ma va tracciata — sarà necessaria.

## 8. Punti aperti

**Fermata di escalation.** La posizione secondo cui la cancellazione di un interessato non tocca i documenti emessi
è una **proposta**, non una decisione dell'agente: è il punto 3 del §11 della descrizione dell'applicazione e ha
conseguenze legali. Va confermata dallo sviluppatore con la revisione legale prima che una riga di codice la
implementi. Resta aperto anche che cosa fare **alla scadenza** dei dieci anni: cancellare in automatico è
rischioso, non cancellare mai è contrario al principio di limitazione della conservazione.
