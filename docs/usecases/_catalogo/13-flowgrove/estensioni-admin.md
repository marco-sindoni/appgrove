# FlowGrove — estensioni della console di amministrazione

**Applicazione**: 13 — FlowGrove (`progetti`) · **Stato del documento**: 🟡 bozza d'autore
**Ultimo aggiornamento**: 2026-08-03
**Documento capofila**: [application-description.md](application-description.md)

## 1. Esito

**B — Servono le estensioni descritte qui sotto.**

Tre cose che la console comune non può dare, e sono tutte piccole. La prima: la quota di FlowGrove è **a giacenza
sui posti**, e un cliente che entra facendo salire dentro tutta la squadra il primo giorno urta il tetto prima
ancora di aver capito quale piano gli serve — serve una deroga a tempo, che non è uno sconto. La seconda:
FlowGrove è **un anello di una catena fra app** (06 QuoteGrove a monte, 08 SpendGrove di lato, 02 BillGrove a
valle) e la frase che l'assistenza sentirà più spesso è «le ore non sono arrivate in fattura» — una domanda a cui
la console oggi non sa rispondere, perché non vede gli eventi di suite. La terza discende dalla seconda: quando
una consegna fallisce per un guasto nostro, bisogna poterla ripetere senza chiedere al cliente di rifare tutto.

Nulla di tutto questo dà accesso ai contenuti: si guardano stati, conteggi e metadati.

## 2. Parametri di configurazione per account

| Parametro | Che cosa governa | Valore predefinito | Chi può cambiarlo | Perché non è nell'app |
|---|---|---|---|---|
| `sospensione_consegne` | interruttore che ferma le consegne dei lotti fatturabili di un singolo account verso 02 BillGrove | spento | amministratore di piattaforma | è una misura di emergenza: serve se un difetto sta producendo lotti sbagliati e continuare peggiorerebbe il danno a valle. Se la governasse il cliente, sarebbe una funzione — e nessuno la userebbe al momento giusto |
| `finestra_riapertura_periodo` | per quanti giorni dopo la chiusura un periodo resta riapribile dal cliente (storia 0020) | 7 giorni | amministratore di piattaforma | è un limite di integrità del consuntivo, non una preferenza: se lo decidesse il cliente, «chiuso» smetterebbe di voler dire qualcosa |

Nessun altro parametro per account. La configurazione dell'app — progetti, stati, tariffe, budget, modelli — è
interamente nelle mani del cliente, ed è una scelta di prodotto: FlowGrove è venduta come strumento leggero e
senza configurazione obbligatoria ([application-description.md](application-description.md) §2.5).

## 3. Quote e deroghe

- **Metrica governata**: `seats` (posti occupati), natura `stock` — un tetto su ciò che esiste ora, non un
  consumo che si azzera a fine mese.
- **Serve una deroga manuale?** **Sì**, e per un caso solo, ma frequente: **l'ingresso**. Il titolare attiva
  FlowGrove e fa entrare le sei persone della squadra prima di aver deciso il piano; il piano gratuito ne regge
  tre. Bloccarlo nel momento esatto in cui sta valutando il prodotto è il modo migliore per perderlo. Secondo
  caso, più raro: un errore nostro nel conteggio dei posti occupati (storia 0004) che lascia occupati posti di
  persone già rimosse.
- **Forma della deroga**: tetto alternativo sui posti **con data di scadenza obbligatoria** (proposta: non oltre
  30 giorni). Alla scadenza il tetto torna quello del piano, senza bisogno di ricordarsene — e il blocco del
  passaggio a un piano inferiore torna a valere.
- **Tracciamento**: ogni deroga porta chi l'ha concessa, quando, fino a quando, con quale tetto e **perché**,
  motivo scritto obbligatorio.
- **Limite**: una deroga non è uno sconto e non cambia l'abbonamento. Se il cliente ha stabilmente bisogno di più
  posti, passa di piano; una deroga rinnovata tre volte è un errore di piano travestito, e la console deve
  renderlo visibile.
- **Vincolo proprio della metrica a giacenza**: alla scadenza della deroga l'account può trovarsi **sopra** il
  tetto. La regola di piattaforma non consente di espellere nessuno da soli: si blocca la creazione di nuovi posti
  e si avvisa il cliente, che sceglie chi rimuovere o cambia piano. La console deve mostrare l'elenco degli
  account in questo stato, perché è una situazione che va risolta parlando, non aspettando.

## 4. Viste operative e diagnostiche

| Vista | Cosa mostra | A che domanda risponde | Dati esposti |
|---|---|---|---|
| **Eventi di suite in ingresso** | per account: eventi ricevuti da 06 QuoteGrove (preventivo accettato) e 08 SpendGrove (spesa approvata con riferimento di commessa) negli ultimi 30 giorni, con esito — accolto, scartato, in errore — e codice di errore | «Perché il progetto non è nato dal preventivo che ho accettato?» | metadati: tipo di evento, momento, esito, codice di errore, identificativo di correlazione. **Nessun contenuto**: né importi, né nome del cliente, né righe |
| **Consegne verso la fatturazione** | per account: lotti consegnati, in attesa e falliti, con periodo, numero di righe, momento ed esito | «Le ore di luglio non sono arrivate in fattura: dove si sono fermate?» | conteggi, stati e momenti. Nessun importo, nessun nome di persona |
| **Occupazione dei posti** | per account: posti occupati su tetto del piano, deroghe attive e scadute, account sopra il tetto | «È al limite? È il caso di proporgli il piano superiore?» | conteggi |
| **Cassetta degli eventi in uscita** | arretrato complessivo e per account, età del messaggio più vecchio, ultimi errori di pubblicazione | «C'è un accumulo? La pubblicazione è ferma?» | conteggi e stati tecnici |
| **Uso dell'app** | per account: progetti attivi, attività aperte, righe di ore dichiarate nell'ultima settimana | «Il cliente sta usando davvero l'app o l'ha attivata e abbandonata?» | conteggi **aggregati per account**, mai per persona |

**Divieto di impersonificazione.** Nessuna di queste viste mostra un progetto, un'attività, una riga di ore, un
commento o un allegato. Se durante l'assistenza servisse guardare un dato, la strada non è entrare nell'account:
è chiedere al cliente di esportarlo (storia 0027) e allegarlo alla richiesta.

**Divieto proprio di questa app.** La vista «Uso dell'app» aggrega **per account**, mai per persona: una console
di piattaforma che mostrasse «ore dichiarate da Anna» sarebbe esattamente la sorveglianza che l'app rifiuta di
essere ([application-description.md](application-description.md) §6). Vale anche per chi amministra la
piattaforma, non solo per il titolare del cliente.

## 5. Azioni di supporto

| Azione | Quando serve | Reversibile? | Tracciamento | Rischi |
|---|---|---|---|---|
| **Concedere una deroga sui posti** | ingresso di una squadra intera prima della scelta del piano; errore nostro nel conteggio | sì (si revoca) | operatore, motivo, tetto, scadenza | trasformare un problema di piano in una consuetudine: la vista del §4 lo rende visibile |
| **Ripetere una consegna fallita** | un guasto nostro ha impedito la pubblicazione del lotto verso 02 BillGrove | sì | operatore, motivo, lotto, momento | doppia fatturazione a valle se non è idempotente: la ripetizione **riusa lo stesso lotto**, non ne compone un altro (storia 0022, RF-4), e richiede conferma esplicita |
| **Ripetere un evento in ingresso scartato** | un preventivo accettato non ha generato la proposta di progetto per un difetto nostro | sì | operatore, motivo, evento | creare una seconda proposta di progetto: la ripetizione è idempotente sull'identificativo dell'evento |
| **Sospendere le consegne di un account** | un difetto sta producendo lotti sbagliati e continuare peggiora il danno a valle | sì | operatore, motivo, momento | ferma il lavoro del cliente a fine mese, che è il momento peggiore: misura di emergenza, mai prassi, e va comunicata |

**Regole comuni.** Ogni azione richiede un motivo scritto; le azioni con effetti verso l'esterno o verso un'altra
app richiedono una conferma esplicita e non sono mai automatiche; nessuna azione dà accesso ai contenuti
dell'account. In particolare: **nessuna azione della console compone un lotto, chiude un periodo o scrive una riga
di ore**. La console può ripetere ciò che il cliente ha già disposto, mai disporre qualcosa al posto suo — e sulle
ore vale un divieto assoluto, perché sono la dichiarazione di una persona sul proprio lavoro.

## 6. Dati esposti alla console

| Dato | Genere | Contiene dati personali? | Perché serve |
|---|---|---|---|
| posti occupati su tetto, per account | metrica | no | diagnosi delle quote e proposta di piano |
| numero di progetti e attività per stato, per account | metrica | no | capire se l'app è usata o abbandonata |
| numero di righe di ore per account e settimana | metrica **aggregata** | no | capire se il foglio ore viene compilato: è l'indicatore che dice se l'app sta funzionando davvero |
| esito degli eventi di suite con codice di errore e correlazione | metadato | no | diagnosi dei mancati arrivi lungo la catena |
| stato dei lotti consegnati (periodo, righe, esito) | metadato | no | rispondere a «le ore non sono arrivate in fattura» |
| deroghe concesse, con operatore e motivo | metadato | no (identifica un nostro operatore, già coperto) | responsabilità delle decisioni di assistenza |

**Verifica obbligatoria.** Nessuna riga di questa tabella contiene dati personali del cliente: è una proprietà
voluta e va **provata**, non assunta — una prova deve verificare che le proiezioni esposte alla console non
contengano `user_id`, nomi, note né descrizioni. Se in futuro servisse esporre un identificativo di persona (per
esempio per diagnosticare un posto occupato che non si libera), quella voce va aggiunta al manifesto dei dati
dell'app con la finalità «assistenza tecnica»: l'accesso amministrativo è un trattamento come gli altri.

## 7. Punti aperti

- **La durata massima della deroga sui posti (proposta: 30 giorni)** e il tetto massimo concedibile non sono dati
  rilevati: sono valori ragionevoli scelti qui. Li chiude lo sviluppatore insieme al listino (fermata di
  escalation, [application-description.md](application-description.md) §5).
- **Il valore predefinito della finestra di riapertura di un periodo (proposta: 7 giorni)** dipende da come i
  clienti chiudono davvero il mese: nessuna fonte consultata dà un riferimento per il segmento micro. Va
  osservato sui primi clienti e corretto.
- **Se la console debba poter vedere l'elenco degli account sopra il tetto dopo la scadenza di una deroga** è qui
  proposto di sì, perché è una situazione che si risolve parlando. Se lo sviluppatore preferisce che il sistema
  gestisca la cosa da solo (avviso al cliente e blocco silenzioso), la vista si toglie — ma allora l'avviso al
  cliente deve essere molto esplicito.
