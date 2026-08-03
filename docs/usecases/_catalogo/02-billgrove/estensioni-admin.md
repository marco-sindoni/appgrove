# BillGrove — estensioni della console di amministrazione

**Applicazione**: 02 — BillGrove (`billing`) · **Stato del documento**: 🟡 bozza d'autore
**Ultimo aggiornamento**: 2026-08-03
**Documento capofila**: [application-description.md](application-description.md)

## 1. Esito

**B — Servono le estensioni descritte qui sotto.**

Le viste comuni della console — account, utenti, abilitazioni, fatturazione, richieste di assistenza — bastano per
quasi tutto, ma tre cose di BillGrove non sono governabili con quelle. Primo: la **numerazione progressiva**, che è
l'unica parte dell'app dove un difetto diventa un problema fiscale del cliente, e su cui chi risponde
all'assistenza deve poter vedere lo stato dei contatori senza entrare nell'account. Secondo: le **lavorazioni
programmate e i canali esterni** (solleciti, fatture ricorrenti, trasmissioni), che falliscono in silenzio e di cui
serve conoscere l'arretrato. Terzo: il **blocco decennale** sui documenti emessi, che rende BillGrove l'unica app
in cui una richiesta di cancellazione non può essere soddisfatta per intero — e chi la gestisce deve vederlo prima
di rispondere all'interessato, non dopo.

## 2. Parametri di configurazione per account

| Parametro | Che cosa governa | Valore predefinito | Chi può cambiarlo | Perché non è nell'app |
|---|---|---|---|---|
| `dunning.enabled` | Interruttore generale dei solleciti automatici per l'account | acceso se il cliente lo ha attivato | amministratore di piattaforma | Serve a **fermare** in emergenza gli invii di un account che sta mandando messaggi sbagliati ai propri clienti, senza aspettare che il cliente se ne accorga. Non è una funzione del cliente: è un freno di sicurezza |
| `transmission.channel.status` | Sospensione della trasmissione verso il canale esterno per un account | attivo | amministratore di piattaforma | Se un canale esterno restituisce errori a raffica per un account, sospenderlo evita di accumulare tentativi falliti. Il cliente non ha modo di saperlo prima di noi |
| `retention.legalHoldNotice` | Testo aggiuntivo mostrato al cliente sulla conservazione decennale, per giurisdizione | vuoto | amministratore di piattaforma | Dipende dalla giurisdizione e cambia per legge: non è una preferenza del cliente |

Nessun altro parametro per account: la configurazione dell'app — sezionali, listini, regole di sollecito, dati di
intestazione — è interamente nelle mani del cliente, ed è giusto che sia così.

## 3. Quote e deroghe

- **Metrica governata**: `documenti` (natura `flow`).
- **Serve una deroga manuale?** **Sì.** Il caso è preciso e ricorrente: un cliente che migra da un altro prodotto
  emette nel primo mese molto più del suo ritmo normale, perché sta recuperando l'arretrato. Senza deroga,
  l'esperienza del primo mese è un blocco — cioè esattamente il contrario di quello che serve a trattenere chi ha
  appena cambiato prodotto.
- **Forma della deroga**: tetto alternativo sulla metrica `documenti` con **data di scadenza obbligatoria**. Mai
  una sospensione indefinita del blocco: una deroga senza scadenza diventa un piano diverso concesso di nascosto.
- **Tracciamento**: ogni deroga porta chi l'ha concessa, quando, fino a quando, con quale tetto e perché.
- **Limite**: una deroga non è uno sconto e non cambia l'abbonamento. Se il cliente ha bisogno stabilmente di più
  documenti, passa di piano; se la stessa deroga viene chiesta due volte, la risposta corretta è il cambio di piano,
  e la console deve renderlo evidente mostrando le deroghe precedenti.

## 4. Viste operative e diagnostiche

| Vista | Cosa mostra | A che domanda risponde | Dati esposti |
|---|---|---|---|
| Stato dei contatori di numerazione | Per account: elenco dei sezionali con anno e ultimo numero assegnato, e segnalazione di eventuali discontinuità | «Il cliente dice che gli manca un numero: è successo davvero?» | Metadati: nome del sezionale, anno, ultimo numero, conteggio. **Nessun documento, nessun cliente, nessun importo** |
| Lavorazioni programmate | Code di solleciti e di fatture ricorrenti: arretrato, ultima esecuzione, ultimi errori per codice | «C'è un accumulo? Da quando?» | Conteggi e codici di errore |
| Stato del canale di trasmissione | Per account: canale configurato (tipo, non credenziali), esito e orario dell'ultima trasmissione, numero di fallimenti recenti per codice | «Perché il cliente dice che le fatture non partono più?» | Metadati: stato, orario, codice di errore. **Nessun destinatario, nessun contenuto** |
| Blocco di conservazione | Per una richiesta di cancellazione: quanti documenti emessi dell'account riguardano l'interessato e non sono cancellabili, con la data fino a cui la conservazione vale | «Che cosa posso davvero cancellare, e che cosa devo spiegare all'interessato?» | Conteggi e date. **Nessun contenuto di documento** |
| Consumo della metrica | Andamento del consumo di `documenti` per account, con le deroghe attive e passate | «Questo cliente sta per sbattere sul tetto?» | Conteggi |

**Divieto di impersonificazione.** Nessuna di queste viste mostra documenti, clienti, importi o testi
dell'account. Le domande dell'assistenza che sembrano richiedere di «vedere quello che vede il cliente» —
«controllate voi se la fattura è giusta» — vanno riformulate come diagnostica su metadati, oppure risolte chiedendo
al cliente di condividere lui il documento. Non esiste un modo, in questa console, di entrare in un account.

## 5. Azioni di supporto

| Azione | Quando serve | Reversibile? | Tracciamento | Rischi |
|---|---|---|---|---|
| Concedere una deroga di quota a termine | Migrazione iniziale di un cliente nuovo | sì (revocabile, e comunque scade) | riga di controllo con operatore, motivo, tetto e scadenza | Diventa uno sconto occulto se ripetuta: la console mostra le deroghe precedenti proprio per questo |
| Ripetere una trasmissione fallita | Il cliente segnala documenti non arrivati e la traccia mostra errori del canale | sì | riga di controllo con operatore e motivo | **Doppio invio**: l'azione è ammessa solo su trasmissioni in stato fallito, mai su quelle riuscite |
| Sospendere il canale di trasmissione di un account | Il canale restituisce errori a raffica | sì | riga di controllo | Il cliente non riesce più a trasmettere: va avvisato, e la sospensione va mostrata dentro l'app |
| Sospendere i solleciti automatici di un account | L'account sta mandando messaggi sbagliati ai propri clienti | sì | riga di controllo con motivo | È un'intromissione in una scelta del cliente: si fa solo su segnalazione o per danno in corso |
| Ripetere una generazione di bozze ricorrenti fallita | La lavorazione programmata è andata in errore per un account | sì | riga di controllo | Nessuno: la generazione è ripetibile senza danno (storia `0020`) |

**Azioni espressamente NON previste**, e non per dimenticanza:

- **modificare o cancellare un documento emesso**: nessun amministratore può farlo, per nessun motivo. È il presidio
  della storia `0026` e vale anche per noi;
- **cambiare un contatore di numerazione**: si può guardare, non si può toccare. Se un contatore fosse davvero
  sbagliato, la correzione è un intervento straordinario con tracciamento proprio, non un pulsante;
- **emettere o trasmettere un documento per conto del cliente**: sarebbe impersonificazione, ed è vietata.

**Regole comuni.** Ogni azione richiede un motivo scritto; le azioni con effetti verso l'esterno — la ripetizione di
una trasmissione — richiedono una conferma esplicita e non sono mai automatiche; nessuna azione dà accesso ai
contenuti dell'account.

## 6. Dati esposti alla console

| Dato | Genere | Contiene dati personali? | Perché serve |
|---|---|---|---|
| Conteggio dei documenti emessi per account e per periodo | metrica | no | Diagnosi delle quote e valutazione del piano |
| Stato dei contatori per sezionale e anno | metadato | no | Diagnosi della classe di difetti più grave dell'app |
| Arretrato ed esiti delle lavorazioni programmate | metrica | no | Rilevare accumuli prima che lo faccia il cliente |
| Esito, orario e codice di errore delle trasmissioni | metadato | **no**, purché il destinatario **non** venga esposto | Rispondere a «perché non parte» |
| Tipo di canale configurato | metadato | no | Capire con che cosa sta parlando l'account |
| Numero di documenti non cancellabili collegati a una richiesta di cancellazione | conteggio | **indirettamente sì**: il conteggio è riferito a un interessato identificato dalla richiesta | Rispondere correttamente all'interessato senza aprire i documenti |
| Deroghe di quota concesse, con operatore e motivo | metadato | no (identifica un operatore interno, non un cliente) | Evitare che la deroga diventi uno sconto occulto |

**Verifica obbligatoria.** L'ultima riga contiene un dato riferito a una persona identificata: va dichiarata nel
manifesto dei dati dell'app come trattamento della console, con finalità «gestione delle richieste degli
interessati» e base giuridica «obbligo di legge». L'accesso amministrativo è un trattamento come gli altri. Le
credenziali dei canali esterni **non** sono mai esposte alla console, in nessuna forma, nemmeno oscurate.

## 7. Punti aperti

- **Chi può concedere una deroga di quota** e fino a quale tetto: è una decisione di esercizio e di prodotto, non
  tecnica. La proposta è che la possa concedere solo un amministratore di piattaforma, con scadenza obbligatoria,
  ma la soglia oltre la quale serve un secondo assenso non la decide un agente.
- **Se la sospensione dei solleciti di un account debba essere possibile senza il consenso del cliente**: è
  un'intromissione in una sua scelta. La proposta è di ammetterla solo per danno in corso, con avviso immediato al
  cliente; va confermata dallo sviluppatore.
- **Come si corregge un contatore di numerazione davvero sbagliato**: qui è dichiarato che non si tocca, ma il caso
  può presentarsi. Serve un intervento straordinario definito, con tracciamento e assenso del cliente. Lo chiude lo
  sviluppatore, se e quando il caso si presenterà.
