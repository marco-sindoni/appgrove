# RenewGrove — estensioni della console di amministrazione

**Applicazione**: 33 — RenewGrove (`fidelizzazione`) · **Stato del documento**: 🟡 bozza d'autore
**Ultimo aggiornamento**: 2026-08-03
**Documento capofila**: [application-description.md](application-description.md)

## 1. Esito

**B — Servono le estensioni descritte qui sotto.**

RenewGrove ha due caratteristiche che nessuna vista comune della console copre. Primo: **non ha dati propri** — vive
interamente di segnali pubblicati da altre applicazioni (§4.2 del documento capofila), quindi un flusso che si
interrompe la rende silenziosamente inutile senza che nulla diventi rosso; il cliente se ne accorge quando smette di
vedere avvisi, cioè quando pensa di non avere problemi. Secondo: **produce giudizi su persone e aziende** con una
versione datata di un modello di punteggio; quando arriva una richiesta di spiegazione o un reclamo, chi risponde
dalla piattaforma deve poter sapere *quale versione del modello era viva quel giorno per quell'account*, senza mai
guardare i contenuti dell'account.

Le estensioni sono quindi **tutte di diagnosi**, tranne una deroga di quota. Non c'è alcun pannello di
configurazione del prodotto: la taratura del punteggio è del cliente e resta del cliente (storia `0016`).

## 2. Parametri di configurazione per account

| Parametro | Che cosa governa | Valore predefinito | Chi può cambiarlo | Perché non è nell'app |
|---|---|---|---|---|
| `ritardo_massimo_atteso_per_fonte` | Dopo quanto tempo senza segnali una fonte è marcata «in silenzio» e i punteggi che ne dipendono portano l'avvertenza (storia `0011`) | 7 giorni | amministratore di piattaforma | Dipende dal ritmo con cui l'**applicazione d'origine** pubblica, che è un fatto di piattaforma e non del cliente. Un cliente che lo alzasse per «togliere l'avviso fastidioso» spegnerebbe proprio il segnale che serve a lui |
| `finestra_minima_di_valutazione` | Il minimo della finestra di osservazione che il cliente può dichiarare prima di un intervento (storia `0024`) | 60 giorni | amministratore di piattaforma | È il presidio contro la finestra scelta per far vincere il rendiconto. Se fosse nell'app, chi vuole un bel numero la porterebbe a sette giorni e il rendiconto smetterebbe di significare qualcosa |

Nessun altro parametro per account: il modello di punteggio, i piani di intervento, le soglie delle fasce e i tetti
delle offerte sono **interamente nelle mani del cliente**, ed è una scelta di prodotto, non una dimenticanza — un
punteggio tarato da noi sarebbe un punteggio che il cliente non può contraddire (§6 del documento capofila).

## 3. Quote e deroghe

- **Metrica governata**: `rapporti_sorvegliati` (natura `stock`).
- **Serve una deroga manuale?** **Sì**, per un caso preciso e ricorrente: la **prima importazione**. Un cliente che
  collega SubGrove e BillGrove insieme si vede nascere in poche ore tutti i rapporti sorvegliabili del proprio
  archivio, e supera il tetto del piano prima di aver capito quali gli interessano davvero. Bloccarlo al primo
  giorno significa perderlo al primo giorno.
- **Forma della deroga**: tetto alternativo **con data di scadenza obbligatoria** (proposta: 30 giorni). Alla
  scadenza il tetto torna quello del piano e il cliente deve aver archiviato i rapporti in eccesso; l'app glielo
  dice per tempo e gli mostra quali sono i candidati naturali (rapporti senza segnali recenti).
- **Tracciamento**: ogni deroga porta chi l'ha concessa, quando, fino a quando, con quale tetto e perché.
- **Limite**: una deroga non è uno sconto e non cambia l'abbonamento. Se il cliente ha stabilmente bisogno di
  sorvegliare più rapporti, passa di piano. Una deroga rinnovata due volte è un segnale commerciale, non una
  soluzione tecnica.

## 4. Viste operative e diagnostiche

| Vista | Cosa mostra | A che domanda risponde | Dati esposti |
|---|---|---|---|
| **Salute delle fonti, per account** | Per ogni account e ogni fonte collegata: stato (collegata/sospesa/scollegata), momento dell'ultimo segnale ricevuto, ritardo rispetto all'atteso, conteggio dei segnali scartati nelle ultime 24 ore e motivo aggregato dello scarto | «Il cliente dice che non gli compare più niente: la fonte ha smesso di pubblicare, o siamo noi che scartiamo?» | Metadati e conteggi: stato, orari, codici di scarto. **Nessun contenuto di segnale**, nessuna etichetta di rapporto |
| **Coda di scarto** | Volume dei segnali finiti nella coda di scarto, per app d'origine e per motivo (schema non valido, tipo non dichiarato, account sconosciuto, campo vietato) | «C'è un'applicazione che pubblica male?» — è la vista che scopre un difetto di una **fonte**, non di RenewGrove | Conteggi per motivo e per app d'origine. **Mai il carico rifiutato**, che potrebbe contenere proprio il campo vietato che ha causato lo scarto |
| **Lavorazione giornaliera dei punteggi** | Ultima esecuzione, durata, account elaborati, giorni eventualmente saltati e recuperati, errori | «I punteggi sono fermi a ieri l'altro?» — un ricalcolo che non gira è il guasto che il cliente scopre per ultimo e nel modo peggiore | Conteggi e orari |
| **Versione del modello di punteggio viva, per account** | Quale versione del modello è viva oggi per quell'account, da quando, e la serie delle versioni precedenti con le date di validità. **Non** i pesi: solo l'identificativo di versione e le date | «Un cliente finale contesta un giudizio del 14 marzo: con quale versione del modello era stato calcolato?» È il presupposto per rispondere a una richiesta di spiegazione (§6, sentenza C-634/21) | Identificativo e date. I pesi sono configurazione del cliente e **non** vanno esposti alla console |
| **Volume degli interventi confermati** | Per account e per periodo: quanti interventi sono stati confermati e quanti sono usciti come richiesta di comunicazione verso un'altra app | «Un account sta usando RenewGrove per contattare in massa?» — è la sorveglianza sull'abuso, non sul contenuto | Conteggi. **Nessun destinatario, nessun testo** |

Le viste comuni (account, abilitazioni, fatturazione, richieste di assistenza) restano quelle e bastano per tutto il
resto.

## 5. Azioni di supporto

| Azione | Quando serve | Reversibile? | Tracciamento | Rischi |
|---|---|---|---|---|
| **Concedere una deroga di quota a termine** | Prima importazione (§3) | sì (si revoca, e scade da sola) | Riga di controllo con operatore, motivo, tetto e scadenza | Il cliente si abitua al tetto alto e la scadenza diventa una brutta sorpresa: la scadenza va comunicata dall'app, non solo registrata |
| **Ripetere il ripopolamento dello storico di una fonte** | Il cliente ha collegato una fonte e lo storico non è arrivato | sì (le chiavi di idempotenza impediscono i doppioni) | Riga di controllo con operatore, account, fonte, finestra richiesta | Nessuno se l'idempotenza tiene; se non tenesse, doppioni che falsano i punteggi — è il motivo per cui la storia `0007` la richiede con una prova |
| **Sospendere una fonte per un account** | Una fonte pubblica male e sta inquinando i punteggi | sì | Riga di controllo con motivo | I punteggi diventano «non calcolabili» e il cliente vede un'app rotta: va accompagnata da una comunicazione, mai fatta in silenzio |
| **Rimettere in coda i segnali scartati per un account** | Difetto della fonte risolto | sì | Riga di controllo con conteggio | Se il difetto non era davvero risolto, si riempie di nuovo la coda di scarto |

**Regole comuni.** Ogni azione richiede un motivo scritto. Le azioni **irreversibili** o con effetti verso l'esterno
richiedono conferma esplicita e non sono mai automatiche. **Nessuna azione dà accesso ai contenuti dell'account**, e
in questa app la regola ha un'estensione che va scritta: **chi amministra la piattaforma non vede i punteggi, non
vede le etichette dei rapporti e non vede i testi degli interventi**. Vede che i segnali arrivano, quanti sono e se
il calcolo gira.

🛑 **Azioni esplicitamente escluse**, e va detto perché qualcuno le chiederà:

- **far partire un intervento per conto del cliente** — sarebbe un effetto verso una persona che non è nostro
  utente, deciso da chi non ha alcun rapporto con lei. Non esiste motivo di assistenza che lo giustifichi;
- **modificare i pesi del modello di punteggio di un account** — cambierebbe i giudizi su persone senza che il
  titolare del trattamento lo sappia. Se i pesi sono sbagliati, li cambia il cliente (storia `0016`);
- **marcare un segnale come non pertinente per conto del cliente** — è una contestazione, e la contestazione ha un
  autore per costruzione (storia `0015`). Un'operazione di questo genere firmata dalla piattaforma renderebbe
  falsa la traccia.

## 6. Dati esposti alla console

| Dato | Genere | Contiene dati personali? | Perché serve |
|---|---|---|---|
| Conteggio dei rapporti sorvegliati per account | metrica | no | Diagnosi delle quote e delle deroghe |
| Stato, ultimo segnale e ritardo per fonte collegata | metadato | no | Diagnosi del flusso di segnali |
| Conteggio dei segnali ricevuti e scartati, per app d'origine e motivo | metrica | no | Trovare la fonte che pubblica male |
| Identificativo e date di validità della versione del modello di punteggio | metadato | no | Rispondere a una richiesta di spiegazione senza guardare i giudizi |
| Conteggio degli interventi confermati e consegnati, per periodo | metrica | no | Sorveglianza sull'abuso, per volume |
| Stato ed esito dell'ultima lavorazione giornaliera di ricalcolo | metadato | no | Accorgersi di un arresto silenzioso |

**Verifica obbligatoria.** Nessuna riga di questa tabella contiene dati personali, ed è una proprietà da **collaudare,
non da dichiarare**: la storia `0031` chiede una prova che l'interfaccia amministrativa dell'app non esponga
etichette di rapporto, valori di punteggio, contributi, testi di intervento né destinatari. Se un giorno servisse
una vista che li contiene, va dichiarata nel manifesto dei dati e motivata: l'accesso amministrativo è un trattamento
come gli altri.

## 7. Punti aperti

- **La vista sul volume degli interventi confermati è sorveglianza sull'abuso o sull'uso?** Il confine è sottile: un
  conteggio alto può essere un cliente che usa bene il prodotto. Serve una soglia dichiarata sopra la quale ci si
  guarda, altrimenti la vista diventa un'abitudine a osservare i clienti. **Chi lo chiude**: sviluppatore.
- **Conservazione dei metadati di diagnosi** (conteggi di scarto, esecuzioni della lavorazione): per quanto si
  tengono? Non contengono dati personali, ma occupano e non servono all'infinito. **Chi lo chiude**: sviluppatore,
  con la scelta generale di piattaforma sui registri.
- **Se la piattaforma decidesse che RenewGrove diventa un'epica di SubGrove** (punto aperto n. 1 del documento
  capofila), tutte le viste di questo documento andrebbero riportate sotto l'app `abbonati`, e la vista sulla
  versione del modello di punteggio sarebbe una novità per quell'app, che oggi non produce giudizi. **Chi lo
  chiude**: sviluppatore — direzione di prodotto.
