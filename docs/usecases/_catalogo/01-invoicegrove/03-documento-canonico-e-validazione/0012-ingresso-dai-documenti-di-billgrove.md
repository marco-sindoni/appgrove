# 0012 — Ingresso dei documenti dall'app di fatturazione

**Applicazione**: 01 — InvoiceGrove (`einvoicing`) · **Epica**: 03 — Documento canonico e validazione
**Storia**: `0012` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0011`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che usa già l'app di fatturazione della suite
> voglio che le fatture che emetto lì arrivino da sole in InvoiceGrove pronte per gli adempimenti
> così da non reinserire nulla e da non avere due verità sullo stesso documento.

**Contesto.** È la storia che realizza la posizione presa nella descrizione dell'applicazione §10 rispetto alla
nota del catalogo §6: InvoiceGrove **non crea fatture**, è lo strato di conformità di chi le crea. La porta
d'ingresso principale è quindi questa, non un modulo di inserimento. Il vincolo di piattaforma è netto: **un'app
non chiama un'altra app**, l'unica via è asincrona a eventi
([PRINCIPI-APPGROVE.md](../../_kit/PRINCIPI-APPGROVE.md) §2). Va fatta subito dopo il modello canonico perché
definisce il contratto con cui i documenti entreranno per sempre.

## 2. Requisiti funzionali

1. **RF-1** — Il servizio consuma un evento «documento emesso» pubblicato dall'app di fatturazione della
   piattaforma e ne ricava un `CanonicalDocument` in stato `bozza`.
2. **RF-2** — Il consumo è **idempotente**: lo stesso evento consegnato due volte produce un solo documento, e la
   seconda consegna non cambia nulla.
3. **RF-3** — Se l'evento non contiene tutto ciò che serve alla giurisdizione di destinazione, il documento entra
   comunque in stato `bozza` e viene marcato **incompleto**, con l'elenco di cosa manca; non viene scartato in
   silenzio.
4. **RF-4** — La controparte dell'evento viene riconciliata con l'anagrafica locale per identificativo fiscale; se
   non esiste, viene creata in bozza e marcata «da completare col recapito».
5. **RF-5** — L'utente vede in panoramica quanti documenti sono arrivati e quanti sono incompleti, con un percorso
   diretto per completarli.
6. **RF-6** — Un evento che non si riesce a interpretare finisce in una coda di documenti non elaborati, con il
   motivo, e non viene perso.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Il `tenant_id` del documento viene dal **carico dell'evento firmato
  dalla piattaforma**, non dal corpo di una richiesta utente; ogni lettura successiva filtra per `tenant_id` preso
  dal token verificato. Prova di isolamento: un evento di un account non deve mai produrre un documento in un
  altro.
- **RT-2 — Interfaccia di programmazione (§2).** Nessuna rotta pubblica di creazione: si entra **solo** per
  eventi. Rotta di sola lettura `GET /api/einvoicing/v1/inbound-issues` per la coda dei non elaborati; errori in
  `application/problem+json`; definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V9__inbound_events.sql`: tabella di deduplica degli eventi consumati
  (chiave dell'evento, istante, esito) e tabella dei non elaborati, entrambe con `tenant_id`, chiave UUID versione
  7 e colonne di controllo.
- **RT-4 — Modulo frontend (§3, §5).** Riquadro «Arrivati da completare» nella panoramica e filtro «incompleti»
  nell'elenco dei documenti. Solo token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** I messaggi «manca il recapito della controparte», «manca la natura
  dell'operazione» e simili sono i testi più letti dell'app: dallo spazio-nomi `einvoicing`, presenti in
  `en, it, fr, es, de`.
- **RT-6 — Varchi e quota (§6, §7).** ⚠️ L'ingresso di un documento **non** consuma la metrica `documenti`: la
  quota si consuma alla **trasmissione**, che è dove sta il costo verso il fornitore. Contarla all'ingresso
  significherebbe far pagare al cliente le bozze che non spedirà. Se l'account non ha abilitazione attiva, gli
  eventi vengono comunque accolti e conservati, ma i documenti restano inerti: si perderebbero altrimenti dei
  documenti fiscali per una questione di abbonamento.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento nuovo: `list_documents` della storia `0011`
  copre già la lettura, con l'aggiunta del filtro «incompleti».
- **RT-8 — Dati personali (§10).** Il carico dell'evento **contiene dati personali** (controparte, descrizioni di
  riga) e crea controparti in anagrafica: le voci del manifesto esistono già dalle storie `0008` e `0011`, ma va
  aggiunta la **provenienza** nella colonna «dove vive il dato» del manifesto. Le tabelle nuove di questa storia
  (deduplica, non elaborati) **contengono carichi con dati personali** e vanno quindi in `exportData` e
  `purgeData`: è la dimenticanza più probabile di questa storia.
- **RT-9 — Registrazione eventi (§14).** Gli eventi `documento acquisito`, `evento duplicato ignorato`,
  `evento non elaborabile` sono registrati con `tenant_id`, `app_id`, `user_id` (quello dell'evento originale) e
  identificativo di correlazione, **senza** il carico e senza denominazioni.

## 4. Criteri di accettazione

**CA-1 — Documento acquisito**
- **Dato** un account abilitato e un evento «documento emesso» valido dall'app di fatturazione
- **Quando** l'evento viene consumato
- **Allora** esiste un `CanonicalDocument` in stato `bozza` con numero, data, righe e totali corrispondenti

**CA-2 — Consegna doppia**
- **Dato** lo stesso evento consegnato due volte
- **Quando** entrambi i consumi sono completati
- **Allora** esiste **un solo** documento e la seconda consegna è registrata come duplicato ignorato

**CA-3 — Documento incompleto**
- **Dato** un evento la cui controparte non ha recapito elettronico
- **Quando** viene consumato
- **Allora** il documento è creato, marcato incompleto, e l'elenco di cosa manca indica il recapito

**CA-4 — Evento non interpretabile**
- **Dato** un evento con un carico malformato
- **Quando** viene consumato
- **Allora** finisce nella coda dei non elaborati con il motivo, e nulla viene perso

**CA-5 — Isolamento fra account**
- **Dato** due account `A` e `B`
- **Quando** arriva un evento per `A`
- **Allora** il documento esiste solo in `A`, e nessun utente di `B` lo vede in alcun modo

**CA-6 — Abbonamento non attivo**
- **Dato** un account con abbonamento `canceled`
- **Quando** arriva un evento «documento emesso»
- **Allora** il documento viene comunque conservato, resta inerte, e l'utente che riattiva lo ritrova

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (backend, frontend, compliance);
- [ ] prove di **unità** sulla mappatura evento → documento canonico e sull'idempotenza, di **integrazione** sul
      consumo con database effimero;
- [ ] prova di **isolamento fra account** sul consumo degli eventi;
- [ ] **prova end-to-end**: *coprire ora, in parte* — il percorso `[J-EINVOICING]` (storia `0030`) parte da qui, e
      il registro di copertura va aggiornato con la voce dell'ingresso a eventi;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato: le tabelle di deduplica e dei non elaborati contengono carichi con dati
      personali e devono comparire in esportazione e cancellazione;
- [ ] **registro delle decisioni** compilato, con la scelta «la quota si consuma alla trasmissione, non
      all'ingresso» e il motivo;
- [ ] contratto degli **strumenti conversazionali**: nessuno nuovo, e il motivo è scritto.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| `0011` | Serve il modello canonico in cui far entrare i documenti |
| App di fatturazione della piattaforma (repo: `fatture`; catalogo: BillGrove 02) | Deve pubblicare l'evento «documento emesso». **Se non lo pubblica, questa storia non è realizzabile**: vedi punti aperti |

## 7. Fuori ambito

- L'importazione da file e l'inserimento manuale: storia `0013`.
- La **restituzione** dello stato all'app di fatturazione («la tua fattura è stata accettata»): è un evento in
  uscita, rimandato alla storia `0019`, che è la proprietaria del ciclo di vita.
- L'ingresso da gestionali di terze parti: rimandato; nella prima versione la via è l'importazione da file
  (storia `0013`).

## 8. Punti aperti

- 🛑 **L'evento «documento emesso» oggi non esiste.** L'app di fatturazione reale del repository (`fatture`) non
  pubblica un evento con questo contratto, e definirlo significa **modificare un'altra app**: non è lavoro di
  InvoiceGrove e non va anticipato qui. È una dipendenza di piattaforma da concordare con lo sviluppatore, ed è
  il punto su cui questa storia si ferma per primo.
- **Chi possiede il contratto dell'evento.** Se lo possiede la sorgente, ogni cambiamento della sorgente rompe
  InvoiceGrove; se lo possiede una definizione condivisa, serve un luogo dove metterla. È una decisione di
  architettura di piattaforma, non di questa app.
- **Cosa succede se il documento viene modificato nella sorgente dopo essere entrato qui.** La proposta è che una
  fattura già trasmessa non si modifichi mai (è anche la regola fiscale), ma il caso della bozza modificata va
  deciso.
