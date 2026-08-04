# 0020 — Richiesta di nulla osta

**Applicazione**: 31 — AuditGrove (`agentaudit`) · **Epica**: 04 — Regole e approvazione umana
**Storia**: `0020` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0019`, `0008`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come agente automatico che sta per compiere un'azione che potrebbe fare danni
> voglio poter chiedere il permesso prima di agire e ricevere una risposta chiara
> così da non essere io a decidere se un'azione irreversibile è opportuna, e da lasciare traccia di aver chiesto.

**Contesto.** È la storia in cui il registro smette di guardare e comincia a governare, e va capito bene **come**
lo fa, perché è la scelta che definisce il prodotto.

AuditGrove **non intercetta**: non sta in mezzo al traffico degli agenti del cliente (§1 della descrizione
dell'applicazione). Non può quindi fermare niente per forza propria. Quello che può fare è essere **interpellato**:
l'agente, prima di compiere un'azione che la regola classifica come delicata, chiede un **nulla osta** e aspetta la
risposta. È l'agente che obbedisce.

Sembra debole, e in parte lo è — un agente che non chiede resta invisibile, e le contromisure a quel limite sono
la numerazione di sequenza (storia 0011) e la riconciliazione (storia 0023). Ma è anche l'unico modello che non
richiede al cliente di rifare la propria architettura per adottarci, ed è il modello che funziona anche quando gli
agenti sono cinque, costruiti con strumenti diversi, da persone diverse. La completezza si compra con
l'intercettazione, e si paga con l'adozione.

## 2. Requisiti funzionali

1. **RF-1** — Una sorgente autenticata può chiedere un nulla osta su `POST /api/agentaudit/v1/clearances`,
   dichiarando: strumento, classe di effetto attesa, forma e impronte dei parametri, identificativo di chi ha
   chiesto l'azione, identificativo dell'agente.
2. **RF-2** — La risposta è immediata e vale una fra tre: **concesso**, **negato**, **in attesa**. Porta sempre un
   identificativo del nulla osta e, quando pertinente, una scadenza.
3. **RF-3** — L'esito è determinato dalla **regola vigente** per quello strumento (storia 0019): `consenti` →
   concesso; `nega` → negato; `richiedi approvazione` → in attesa, e nasce una richiesta per una persona.
4. **RF-4** — Un nulla osta `in attesa` si risolve in due modi, entrambi offerti: **interrogazione periodica** di
   `GET /api/agentaudit/v1/clearances/{id}`, oppure **attesa lunga della risposta** entro un limite di tempo, per
   gli agenti che possono restare fermi.
5. **RF-5** — La richiesta è **idempotente**: la sorgente fornisce una chiave di deduplicazione, e ripetere la
   stessa richiesta con la stessa chiave restituisce lo stesso nulla osta invece di crearne un altro.
6. **RF-6** — Ogni richiesta di nulla osta e ogni sua risoluzione sono **righe del registro**, incatenate come
   tutte le altre; la richiesta consuma una unità della metrica `actions`.
7. **RF-7** — Se il servizio non è in grado di rispondere — guasto interno, regola non risolvibile — la risposta
   è **non concesso**: chi non sa non procede.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Il `tenant_id` si ricava dalla credenziale verificata della sorgente
  (storia 0006) e **mai** dal corpo della richiesta: una sorgente non può chiedere un nulla osta per conto di un
  altro account nemmeno dichiarandolo. Ogni lettura dello stato di un nulla osta filtra per `tenant_id`. La
  credenziale non umana della sorgente è un punto che eccede questa storia e resta assunto, non deciso (§11,
  punto 7 della descrizione dell'applicazione).
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `POST /api/agentaudit/v1/clearances` e
  `GET /api/agentaudit/v1/clearances/{id}`; corpo validato — schema stretto, valori ammessi dichiarati; errori in
  `application/problem+json`; definizione OpenAPI aggiornata nello stesso commit. L'attesa lunga della risposta ha
  un limite di tempo dichiarato nella definizione, oltre il quale si risponde `in attesa` e si invita a
  riprovare.
- **RT-3 — Persistenza (§8).** Migrazione `V<N>__nulla_osta.sql` sullo schema `app_agentaudit`: tabella
  `clearances` con `tenant_id`, chiave primaria UUID versione 7, colonne di controllo, stato, strumento, classe di
  effetto, impronte dei parametri, chiave di deduplicazione con vincolo di unicità per account, scadenza,
  riferimento alla regola applicata e alla sua versione. **La versione della regola si registra nel nulla osta**,
  non si ricava dopo: è la differenza fra sapere e ricostruire.
- **RT-4 — Modulo frontend (§3, §5).** Nessuna schermata in questa storia: la coda delle approvazioni e la
  decisione sono la storia 0021. Qui si costruisce la rotta che gli agenti chiamano.
- **RT-5 — Cinque lingue (§4).** Nessun testo visibile introdotto. I messaggi di errore restituiti alla sorgente
  sono destinati a un programma, non a una persona, e restano in forma tecnica secondo il formato degli errori di
  piattaforma.
- **RT-6 — Varchi e quota (§6, §7).** Prima di registrare la richiesta il servizio prenota una unità della metrica
  `actions` (natura `flow`); a quota esaurita risponde `429` con l'indicazione del rimedio. **Il comportamento a
  quota esaurita è delicato e va deciso, non subito**: rifiutare il nulla osta significa che l'agente non ottiene
  risposta e — per la regola RF-7 — non procede, cioè la quota esaurita blocca il lavoro del cliente. La banda di
  cortesia e il conteggio dei rifiuti della storia 0004 valgono anche qui. Con abbonamento non attivo risponde
  `402`.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento nuovo esposto: chiedere un nulla osta è ciò che
  fa un agente attraverso questa rotta, non un'operazione da comandare in chat. `elenca_approvazioni_in_attesa`
  (lettura) arriva con la storia 0034; `nega_azione` (scrittura, con conferma umana obbligatoria) con la 0035.
  **`approva_azione` non esiste e non esisterà**: il motivo è al §7 della descrizione dell'applicazione. Il
  contratto vive dentro il servizio; il server conversazionale è di piattaforma e non ancora implementato
  (UC 0061-0063).
- **RT-8 — Dati personali (§10).** Introduce l'**identificativo di chi ha chiesto l'azione**, fornito dalla
  sorgente: è un dato che riguarda una persona anche quando è un codice. Va dichiarato nel manifesto
  `docs/compliance/manifests/agentaudit.yaml` in italiano e inglese, il campo va annotato `@PersonalData`, e la
  tabella dei nulla osta va aggiunta a `exportData` e `purgeData` del contratto dati dell'app. **Nessun valore di
  parametro viene conservato**: solo forma e impronte, come stabilito dalla storia 0009.
- **RT-9 — Registrazione eventi (§14).** Gli eventi `nulla osta richiesto`, `nulla osta concesso automaticamente`,
  `nulla osta negato automaticamente`, `nulla osta in attesa` e `nulla osta respinto per quota` sono registrati con
  `tenant_id`, `app_id`, `user_id` e identificativo di correlazione, senza dati personali.

## 4. Criteri di accettazione

**CA-1 — Consenso automatico**
- **Dato** uno strumento con regola vigente `consenti`
- **Quando** una sorgente chiede il nulla osta
- **Allora** riceve `concesso` immediatamente, con l'identificativo del nulla osta, e nel registro compaiono la
  richiesta e la concessione con il riferimento alla versione di regola applicata

**CA-2 — Attesa di una persona**
- **Dato** uno strumento con regola vigente `richiedi approvazione`
- **Quando** una sorgente chiede il nulla osta
- **Allora** riceve `in attesa` con identificativo e scadenza, nasce una richiesta visibile nella coda delle
  approvazioni, e l'interrogazione successiva restituisce lo stesso stato finché una persona non decide

**CA-3 — Ripetere non duplica**
- **Dato** una richiesta già inviata con una certa chiave di deduplicazione
- **Quando** la sorgente ripete esattamente la stessa richiesta con la stessa chiave
- **Allora** riceve lo stesso identificativo di nulla osta e lo stesso stato, non viene creata una seconda
  richiesta, e non viene consumata una seconda unità di quota

**CA-4 — Chi non sa non procede**
- **Dato** un guasto interno che impedisce di risolvere la regola vigente
- **Quando** una sorgente chiede il nulla osta
- **Allora** la risposta è **non concesso** con un errore che lo dichiara, e nel registro resta traccia sia della
  richiesta sia dell'impossibilità di deciderla

**CA-5 — Isolamento fra account**
- **Dato** due account `A` e `B`, ciascuno con le proprie sorgenti e i propri nulla osta
- **Quando** una sorgente di `A` chiede un nulla osta dichiarando nel corpo l'identificativo dell'account `B`
- **Allora** il nulla osta nasce comunque sotto `A`, ricavato dalla credenziale verificata, e nessuna
  informazione sull'account `B` viene restituita

**CA-6 — Quota esaurita**
- **Dato** un account che ha raggiunto il tetto della metrica `actions` ed esaurito la banda di cortesia
- **Quando** una sorgente chiede un nulla osta
- **Allora** riceve `429` con un messaggio che spiega come rimediare, nessun nulla osta viene creato, e il rifiuto
  viene conteggiato secondo quanto stabilito dalla storia 0004

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno le aree toccate; l'intera suite prima del commit);
- [ ] prove di **unità** sulla risoluzione della regola in esito e sulla deduplicazione, e di **integrazione**
      sulla rotta con attesa lunga e interrogazione periodica, con database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** sulla richiesta e sulla lettura dello stato, compreso il tentativo di
      dichiarare un altro account nel corpo;
- [ ] **prova end-to-end**: risposta «rimando» — la superficie utente del nulla osta è la coda delle approvazioni,
      che è la storia 0021 ed è la storia proprietaria del passo di percorso; il registro di copertura
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) riceve la voce `da-coprire` con
      questo motivo;
- [ ] **traduzioni**: nessun testo visibile introdotto, e il fatto è dichiarato;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese con l'identificativo del richiedente, campo
      annotato, e tabella dei nulla osta presente in esportazione e cancellazione;
- [ ] **registro delle decisioni** `changes/NNNN-*/decisions.json` compilato, con **tre voci obbligatorie**: la
      postura «chi non sa non procede», la registrazione della versione di regola dentro il nulla osta, e il fatto
      che la richiesta consuma quota;
- [ ] contratto degli **strumenti conversazionali**: nessuno nuovo; il divieto permanente su `approva_azione` è
      dichiarato qui perché è qui che nasce l'oggetto approvabile;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0019` | L'esito del nulla osta è determinato dalla regola vigente: senza regole non c'è cosa applicare |
| storia `0008` | La richiesta è una riga del registro e usa la stessa macchina di accodamento e incatenamento |
| storia `0006` | Serve la credenziale della sorgente da cui ricavare l'account: è il punto aperto 7 della descrizione dell'applicazione, qui assunto e non deciso |

## 7. Fuori ambito

- la coda delle approvazioni e la decisione di una persona: storia 0021;
- la scadenza del nulla osta non deciso e il suo esito: storia 0022;
- il confronto fra ciò che è stato concesso e ciò che l'agente ha poi fatto: storia 0023;
- la libreria o l'adattatore che il cliente installa per far chiedere i nulla osta ai propri agenti: è materiale
  di supporto, non parte del servizio, e non è in questa storia.

## 8. Punti aperti

- ⚠️ **La postura in caso di errore dovrebbe essere una scelta del cliente, per classe di effetto.** «Chi non sa
  non procede» è la postura giusta per una cancellazione o un pagamento; per uno strumento di lettura classificato
  `richiedi approvazione` per prudenza, bloccare il lavoro perché il nostro servizio ha un guasto è
  sproporzionato. La proposta è: postura rigida per le classi di effetto distruttive, configurabile per le altre.
  Non è una decisione da prendere qui, perché determina quanto il nostro guasto diventa un guasto del cliente.
  Chi chiude: sviluppatore.
- **Quota esaurita e blocco del lavoro.** Per la combinazione di RF-7 e del `429`, un account che finisce la quota
  vede i propri agenti fermarsi sulle azioni delicate. È coerente e va detto in modo esplicito nel listino, perché
  è una conseguenza che nessuno legge finché non accade. Chi chiude: sviluppatore (§5 della descrizione
  dell'applicazione).
- **Quanto può durare l'attesa lunga della risposta.** Un agente che aspetta trenta secondi è accettabile; uno che
  aspetta dieci minuti tiene occupata una connessione e probabilmente ha un problema di progetto suo. Il limite va
  fissato e dichiarato. Chi chiude: sviluppatore.
