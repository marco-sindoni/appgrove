# InsightGrove — estensioni della console di amministrazione

**Applicazione**: 20 — InsightGrove (`insights`) · **Stato del documento**: 🟡 bozza d'autore
**Ultimo aggiornamento**: 2026-08-03
**Documento capofila**: [application-description.md](application-description.md)

## 1. Esito

**B — Servono le estensioni descritte qui sotto.**

Poche, e tutte di **diagnosi**. La ragione è una sola e vale per tutta l'app: InsightGrove è l'unica applicazione
della suite che **non produce i propri dati**. Li riceve dal bus di eventi, uno per uno, da fonti che l'account ha
collegato. Ne discende che la frase che l'assistenza sentirà più spesso non è «non riesco a fare una cosa», ma
**«i miei numeri sono fermi a martedì»** — una domanda che non si risolve guardando l'account, ma guardando la
coda, il ritardo delle fonti e gli scarti. Serve inoltre una deroga a tempo sul tetto delle domande, perché il
primo mese di un cliente — ripopolamento dello storico più prova del copilota — è atipico per costruzione.

Nulla di tutto questo dà accesso ai numeri del cliente: si guardano stati, conteggi e metadati. E su un punto
questa app è **più severa** della media del catalogo: **le domande poste al copilota non sono visibili alla
console**, in nessuna forma (§6).

## 2. Parametri di configurazione per account

| Parametro | Che cosa governa | Valore predefinito | Chi può cambiarlo | Perché non è nell'app |
|---|---|---|---|---|
| `ritardo_atteso_per_fonte` | la soglia oltre la quale una fonte silente è considerata in ritardo e i suoi numeri vengono marcati incompleti (storia 0010) | il valore dichiarato dalla fonte | amministratore di piattaforma | è un parametro **della sorgente**, non una preferenza del cliente: se lo decidesse lui, potrebbe alzarlo per far sparire il contrassegno di incompletezza — cioè spegnere l'allarme invece del problema |
| `sospensione_ingestione` | interruttore che ferma la scrittura dei fatti in arrivo per un singolo account | spento | amministratore di piattaforma | è una misura d'emergenza: serve quando una fonte difettosa sta pubblicando fatti sbagliati e continuare peggiora il danno. Fermare significa **numeri fermi**, non numeri sbagliati: è il male minore, ma va comunicato al cliente |
| `tetto_domande_in_deroga` | tetto alternativo temporaneo sulla metrica `questions` (§3) | assente | amministratore di piattaforma | è assistenza, non listino |

Nessun altro parametro per account. Metriche, cruscotti, avvisi, rapporti e collegamento delle fonti sono
**interamente nelle mani del cliente**: sono il prodotto.

## 3. Quote e deroghe

- **Metrica governata**: `questions` (domande al copilota), natura `flow` — consumo su una finestra che si azzera
  ogni mese, non una giacenza.
- **Serve una deroga manuale?** **Sì**, per due casi soltanto, entrambi prevedibili.
  Il primo è **l'ingresso**: il cliente collega tre fonti, ripopola lo storico e passa il pomeriggio a chiedere al
  copilota che cosa sa fare. Le venti domande del piano gratuito finiscono prima che abbia capito se il prodotto
  gli serve; bloccarlo in quel momento è il modo migliore per perderlo.
  Il secondo è **un guasto nostro**: domande che hanno consumato una unità e sono finite in errore per un difetto
  del servizio. Restituirle non è una cortesia, è una correzione.
- **Forma della deroga**: tetto alternativo sulle domande **con data di scadenza obbligatoria** (proposta: non
  oltre 30 giorni). Alla scadenza il tetto torna quello del piano, senza che nessuno debba ricordarsene.
- **Tracciamento**: ogni deroga porta chi l'ha concessa, quando, fino a quando, con quale tetto e **perché** —
  motivo scritto obbligatorio.
- **Limite**: una deroga non è uno sconto e non cambia l'abbonamento. Se un cliente ha stabilmente bisogno di più
  domande, passa di piano: una deroga rinnovata tre volte è un errore di piano travestito, e la console deve
  renderlo visibile.
- **Nota propria della metrica a flusso**: alla scadenza della deroga non resta nulla «sopra il tetto» da
  risolvere — è il vantaggio di una metrica a flusso rispetto a una a giacenza. Il consumo semplicemente riparte
  dal tetto del piano alla finestra successiva.

## 4. Viste operative e diagnostiche

| Vista | Cosa mostra | A che domanda risponde | Dati esposti |
|---|---|---|---|
| **Salute delle fonti** | per account: fonti collegate, stato, momento dell'ultimo fatto ricevuto, ritardo atteso, ritardo effettivo, numero di fatti nelle ultime 24 ore | «Perché i miei numeri sono fermi a martedì?» | metadati: identificativo dell'app sorgente, stati, momenti, conteggi. **Nessun valore, nessuna dimensione, nessuna etichetta** |
| **Coda dei fatti in ingresso** | arretrato complessivo e per account, età del messaggio più vecchio, ritmo di consumo, messaggi finiti nella coda degli scarti | «C'è un accumulo? Il consumo è fermo?» | conteggi e stati tecnici |
| **Fatti scartati** | per account e per fonte: numero di scarti per **motivo** (malformato, account sconosciuto, fonte non collegata, chiave di dimensione non dichiarata, doppione), con il momento e l'identificativo di correlazione | «Perché la fonte pubblica ma il numero non si muove?» | il **motivo** dello scarto e i suoi metadati; **mai il contenuto del fatto scartato** |
| **Ripopolamento dello storico** | per account: richieste di ripopolamento, fonte, stato (in corso, concluso, fallito), avanzamento, momento | «Il caricamento iniziale è finito o è morto a metà?» | stati, conteggi e momenti |
| **Consumo delle domande** | per account: consumo del periodo su tetto del piano, deroghe attive e scadute, numero di domande in errore | «È al limite? Vale la pena proporgli il piano superiore?» | conteggi **aggregati per account** |
| **Uso dell'app** | per account: fonti collegate, metriche pubblicate, cruscotti, avvisi attivi, rapporti programmati, ultima apertura del modulo | «Il cliente la sta usando o l'ha attivata e abbandonata?» | conteggi aggregati |

**Divieto di impersonificazione.** Nessuna di queste viste mostra un numero del cliente, una definizione di
metrica, un'etichetta di dimensione, il contenuto di un cruscotto o un valore di soglia di un avviso. La domanda
«che numero vede il cliente?» va sempre riformulata in «che cosa è arrivato, quando, e che cosa è stato
scartato»: la prima non è rispondibile senza guardare i suoi dati, la seconda sì ed è quella che risolve
davvero il problema.

**Divieto proprio di questa app.** Nessuna vista aggrega **per persona**. Un pannello che mostrasse «domande poste
da Anna» sarebbe visibilità sull'attività di una lavoratrice esercitata per giunta da un soggetto esterno
all'account: è la stessa materia che tiene fuori dal prodotto gli indicatori per persona
([application-description.md](application-description.md) §2.3, punto 3).

## 5. Azioni di supporto

| Azione | Quando serve | Reversibile? | Tracciamento | Rischi |
|---|---|---|---|---|
| **Concedere una deroga sulle domande** | ingresso del cliente; domande consumate per un guasto nostro | sì (si revoca) | operatore, motivo, tetto, scadenza | trasformare un problema di piano in consuetudine: la vista del §4 lo rende visibile |
| **Rimettere in lavorazione i fatti scartati** | uno scarto è stato causato da un difetto nostro (per esempio una chiave di dimensione dichiarata male) | sì | operatore, motivo, fonte, finestra temporale, numero di messaggi | doppia scrittura: la rilavorazione **deve** passare dalla stessa chiave di idempotenza della storia 0007, mai da una scorciatoia |
| **Richiedere un ripopolamento dello storico** | il caricamento iniziale è fallito a metà e il cliente non sa rilanciarlo | sì | operatore, motivo, fonte, periodo | carico sulla fonte e sulla coda: richiede conferma esplicita e si esegue una fonte per volta |
| **Sospendere l'ingestione di un account** | una fonte difettosa sta pubblicando fatti sbagliati | sì | operatore, motivo, momento | i numeri del cliente si fermano: misura d'emergenza, mai prassi, e va comunicata prima, non dopo |
| **Forzare il ricalcolo dei valori di un periodo** | una correzione di definizione o una rilavorazione ha lasciato valori vecchi in cache | sì | operatore, motivo, metrica, periodo | costo di calcolo; nessun rischio sui dati, perché il ricalcolo è deterministico sui fatti già scritti |

**Regole comuni.** Ogni azione richiede un motivo scritto; le azioni che toccano il flusso dei dati richiedono
conferma esplicita e non sono mai automatiche; nessuna azione dà accesso ai contenuti dell'account. In
particolare — ed è il divieto più importante di questo documento — **nessuna azione della console pubblica una
definizione di metrica, crea o modifica un avviso, un cruscotto o un rapporto, e nessuna collega o scollega una
fonte**. Scollegare una fonte cancella fisicamente lo storico ricevuto (storia 0008): è un atto del cliente, e
resta suo. La console può rimettere in moto ciò che il cliente ha già disposto, mai disporre al posto suo.

## 6. Dati esposti alla console

| Dato | Genere | Contiene dati personali? | Perché serve |
|---|---|---|---|
| stato e ritardo delle fonti collegate, per account | metadato | no | rispondere a «i numeri sono fermi» |
| conteggio dei fatti ricevuti per fonte e finestra temporale | metrica | no | capire se la sorgente pubblica davvero |
| conteggio degli scarti per **motivo**, con correlazione | metadato | no | diagnosi della causa senza guardare il contenuto |
| arretrato ed età dei messaggi in coda | metrica tecnica | no | accorgersi di un accumulo prima del cliente |
| consumo delle domande su tetto, per account | metrica **aggregata per account** | no | diagnosi delle quote e proposta di piano |
| numero di cruscotti, metriche, avvisi e rapporti per account | metrica | no | capire se l'app è usata o abbandonata |
| deroghe concesse, con operatore e motivo | metadato | no (identifica un nostro operatore, già coperto) | responsabilità delle decisioni di assistenza |

**Ciò che NON è esposto, per scelta esplicita:**

- **il testo delle domande poste al copilota** — mai, in nessuna vista, in nessun registro, nemmeno troncato.
  È il testo libero dell'app, può nominare persone, ed è il dato che il cliente si aspetta che nessuno legga
  ([application-description.md](application-description.md) §6.4). Se un cliente segnala «il copilota mi ha
  risposto male», la strada è chiedergli **l'identificativo della domanda** e diagnosticare sul piano prodotto e
  sulla traccia del calcolo, non sul testo;
- **i valori delle metriche, le etichette di dimensione e le soglie degli avvisi** — sono i dati economici del
  cliente ed è ciò che sta comprando;
- **il contenuto dei fatti scartati** — di uno scarto serve il motivo, non il carico.

**Verifica obbligatoria.** Nessuna riga della tabella qui sopra contiene dati personali del cliente: è una
proprietà voluta e va **provata**, non assunta. Una prova deve verificare che le proiezioni esposte alla console
non contengano testo di domande, etichette, valori né identificativi di persona. Se un giorno servisse esporne
uno, quella voce va aggiunta al manifesto dei dati dell'app con la finalità «assistenza tecnica»: l'accesso
amministrativo è un trattamento come gli altri.

## 7. Punti aperti

- **La durata massima della deroga sulle domande (proposta: 30 giorni)** e il tetto massimo concedibile non sono
  dati rilevati: sono valori ragionevoli scelti qui. Li chiude lo sviluppatore insieme al listino (fermata di
  escalation, [application-description.md](application-description.md) §5).
- **Restituire le domande consumate da un guasto nostro va automatizzato o resta manuale?** L'automatismo è
  giusto ma richiede di distinguere con certezza l'errore nostro da quello del cliente, e quella certezza oggi
  non c'è. Proposta: **manuale, con la vista che elenca le domande in errore** perché l'operatore le veda senza
  cercarle. Chiude: **sviluppatore**.
- **La sospensione dell'ingestione lascia il cliente con numeri fermi**, che è il difetto peggiore per questa app.
  Serve decidere se accompagnarla con un contrassegno visibile nell'interfaccia del cliente («ricezione sospesa
  dall'assistenza») invece che con una semplice comunicazione. Raccomandazione: **sì, contrassegno visibile** —
  un numero fermo senza spiegazione è esattamente ciò che l'app promette di non fare. Chiude: **sviluppatore**.
- **Se e come esporre alla console la diagnosi del copilota** (piani rifiutati, motivi del «non lo so») senza
  toccare il testo delle domande: sarebbe utile per capire dove il modello sbaglia, ma il confine è sottile e va
  disegnato con cura. Chiude: **sviluppatore**.
