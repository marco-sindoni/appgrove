# SalonGrove — estensioni della console di amministrazione

**Applicazione**: 21 — SalonGrove (`salone`) · **Stato del documento**: 🟡 bozza d'autore
**Ultimo aggiornamento**: 2026-08-03
**Documento capofila**: [application-description.md](application-description.md)

> ⚠️ **Nota sulla via scelta al §0 della descrizione.** Sotto la via **(b)** — SalonGrove come verticale di 07
> BookGrove — la console non vede una applicazione nuova: vede l'account di `prenotazioni` con un piano in più.
> Tutto ciò che segue vale ugualmente, ma va **aggiunto alla scheda di quell'app** invece che a una scheda nuova, e
> la metrica governata resta quella di BookGrove (`risorse_prenotabili`, che qui chiamo `postazioni`).

## 1. Esito

**B — Servono le estensioni descritte qui sotto.**

Tre cose che la console comune non può dare, e sono tutte piccole. La prima: la quota è **a giacenza sulle
postazioni**, e il salone che arriva da un altro programma tiene aperte per qualche giorno più postazioni di quante
il piano ne consenta, mentre porta dentro l'agenda vecchia — serve una deroga a tempo, che non è uno sconto. La
seconda: la giacenza di magazzino è la **somma dei movimenti** e la colonna che la riporta è una comodità di
lettura; il giorno in cui le due si discostano bisogna accorgersene dalla console, perché il cliente se ne
accorgerà come «il magazzino sbaglia» e senza uno strumento la diagnosi è una caccia. La terza: le **fotografie del
trattamento** sono la funzione con il profilo di rischio più alto dell'applicazione, e devono poter essere spente
per un account senza toccare il codice e senza attendere un rilascio.

Nulla di tutto questo dà accesso ai contenuti: si guardano stati, conteggi e metadati.

## 2. Parametri di configurazione per account

| Parametro | Che cosa governa | Valore predefinito | Chi può cambiarlo | Perché non è nell'app |
|---|---|---|---|---|
| `fotografie_trattamento_attive` | interruttore che spegne per un singolo account il caricamento e la visualizzazione delle fotografie del trattamento (storia `0013`) | acceso, se il piano la comprende | amministratore di piattaforma | è una misura di tutela, non una preferenza: serve quando emerge un difetto sulla conservazione delle immagini, o quando un cliente segnala un uso improprio. Se la governasse il cliente sarebbe una funzione, e nessuno la spegnerebbe nel momento giusto. Spegnerla **non cancella** nulla: rende le immagini non caricabili e non visibili finché il punto non è risolto |
| `sospensione_eventi_conto_chiuso` | interruttore che ferma la pubblicazione degli eventi di conto chiuso verso le app 01, 02 e 03 (storia `0019`) | spento | amministratore di piattaforma | misura di emergenza: se un difetto sta producendo conti sbagliati, continuare a spingerli a valle moltiplica il danno in documenti che qualcuno dovrà stornare |

Nessun altro parametro per account. Servizi, fasi, dosi previste, soglie di riordino, regole di provvigione e
regole dei punti sono **interamente nelle mani del cliente**: è una scelta di prodotto, perché un salone che deve
chiedere all'assistenza per cambiare una dose torna al quaderno ([application-description.md](application-description.md) §2.5).

## 3. Quote e deroghe

- **Metrica governata**: `postazioni` (poltrone, cabine, lettini e macchinari prenotabili), natura `stock` — un
  tetto su ciò che esiste ora, non un consumo che si azzera a fine mese.
- **Serve una deroga manuale?** **Sì**, e per due casi. Il primo, frequente: **la migrazione**. Il salone porta
  dentro l'agenda e l'anagrafica dal programma precedente e per qualche giorno tiene aperte tutte le postazioni
  vecchie insieme a quelle nuove; bloccarlo mentre sta trasferendo il proprio lavoro è il modo migliore per
  perderlo. Il secondo, più raro: un errore nostro nel conteggio delle postazioni aperte (storia `0004`), che
  lascia occupate postazioni già chiuse.
- **Forma della deroga**: tetto alternativo sulle postazioni **con data di scadenza obbligatoria** (proposta: non
  oltre 30 giorni). Alla scadenza il tetto torna quello del piano, senza che nessuno debba ricordarsene, e torna a
  valere il blocco del passaggio a un piano inferiore.
- **Tracciamento**: ogni deroga porta chi l'ha concessa, quando, fino a quando, con quale tetto e **perché**,
  motivo scritto obbligatorio.
- **Limite**: una deroga non è uno sconto e non cambia l'abbonamento. Se il salone ha stabilmente bisogno di più
  postazioni, passa di piano; una deroga rinnovata tre volte è un errore di piano travestito, e la console deve
  renderlo visibile.
- **Vincolo proprio della metrica a giacenza**: alla scadenza l'account può trovarsi **sopra** il tetto. Non si
  chiude una postazione al posto del cliente: si blocca l'apertura di postazioni nuove e lo si avvisa, perché
  scegliere quale poltrona spegnere è una decisione sua. La console mostra l'elenco degli account in questo stato:
  è una situazione che si risolve parlando, non aspettando.

## 4. Viste operative e diagnostiche

| Vista | Cosa mostra | A che domanda risponde | Dati esposti |
|---|---|---|---|
| **Coerenza fra giacenza e movimenti** | per account: numero di prodotti in cui la giacenza registrata si discosta dalla somma dei movimenti, con l'entità dello scostamento e la data del movimento più vecchio interessato | «Il cliente dice che il magazzino sbaglia: sbaglia davvero, e da quando?» | **conteggi e differenze numeriche**, mai il nome del prodotto, mai la marca, mai il servizio |
| **Occupazione delle postazioni** | per account: postazioni aperte su tetto del piano, deroghe attive e scadute, account sopra il tetto | «È al limite? È il caso di proporgli il piano superiore?» | conteggi |
| **Eventi di conto chiuso in uscita** | per account: eventi pubblicati, in attesa e falliti verso le app 01, 02 e 03, con momento, esito e codice di errore | «Ho chiuso i conti di luglio e in fatturazione non è arrivato niente: dove si sono fermati?» | metadati: tipo, momento, esito, codice, identificativo di correlazione. **Nessun importo, nessun nome** |
| **Cassetta degli eventi in uscita** | arretrato complessivo e per account, età del messaggio più vecchio, ultimi errori di pubblicazione | «C'è un accumulo? La pubblicazione è ferma?» | conteggi e stati tecnici |
| **Uso dell'app** | per account: appuntamenti della settimana, conti chiusi, schede tecniche compilate, prodotti a magazzino, prospetti chiusi nell'ultimo mese | «Il salone sta usando davvero l'app o l'ha attivata e abbandonata?» | conteggi **aggregati per account**, mai per operatore |
| **Stato delle fotografie** | per account: se la funzione è accesa, numero di immagini conservate, numero di consensi revocati non ancora onorati | «La revoca di un consenso è stata eseguita?» | conteggi. **Nessuna immagine è visibile dalla console, in nessun caso** |

**Divieto di impersonificazione.** Nessuna di queste viste mostra un cliente, una scheda tecnica, una formula, una
fotografia, un conto o una nota. Se durante l'assistenza servisse guardare un dato, la strada non è entrare
nell'account: è chiedere al salone di esportarlo (storie `0014` e `0032`) e allegarlo alla richiesta.

**Divieto proprio di questa applicazione.** La vista «Uso dell'app» aggrega **per account, mai per operatore**. Una
console di piattaforma che mostrasse «conti chiusi da Sara» sarebbe la stessa classifica di persone che
l'applicazione rifiuta di essere (§6 della descrizione, avviso sul lavoro; storia `0026`) — e il fatto che a
guardarla sia chi amministra appgrove invece del titolare non la rende meno una sorveglianza.

**Un divieto in più, proprio del beauty.** La vista sulla coerenza del magazzino guarda **conteggi**, non il
contenuto: sapere *quale* prodotto si sta consumando in un salone è un'informazione commerciale del cliente, e non
ci serve per rispondere a una segnalazione.

## 5. Azioni di supporto

| Azione | Quando serve | Reversibile? | Tracciamento | Rischi |
|---|---|---|---|---|
| **Concedere una deroga sulle postazioni** | migrazione da un altro programma; errore nostro nel conteggio | sì (si revoca) | operatore, motivo, tetto, scadenza | trasformare un problema di piano in una consuetudine: la vista del §4 lo rende visibile |
| **Ripetere la pubblicazione di un evento di conto chiuso** | un guasto nostro ha impedito l'arrivo dell'evento alle app a valle | sì | operatore, motivo, evento, momento | doppio documento a valle se la ripetizione non è idempotente: si ripubblica **lo stesso evento** con lo stesso identificativo, mai uno nuovo, e serve conferma esplicita |
| **Spegnere le fotografie del trattamento per un account** | difetto sulla conservazione delle immagini, segnalazione di uso improprio, revoca di consenso non onorata | sì | operatore, motivo, momento | il salone perde una funzione che sta usando: va comunicato, non lasciato scoprire |
| **Sospendere gli eventi in uscita di un account** | un difetto sta producendo conti sbagliati e continuare peggiora il danno a valle | sì | operatore, motivo, momento | ferma la catena verso la fatturazione: misura di emergenza, mai prassi |

**Regole comuni.** Ogni azione richiede un motivo scritto; le azioni con effetti verso l'esterno o verso un'altra
app richiedono una conferma esplicita e non sono mai automatiche; nessuna azione dà accesso ai contenuti
dell'account.

**Il divieto che conta più di tutti, in questa applicazione.** Nessuna azione della console **chiude un conto,
rettifica una giacenza, chiude un prospetto di provvigioni, scrive una scheda tecnica o cancella una fotografia**.
La console può ripetere ciò che il cliente ha già disposto, mai disporre qualcosa al posto suo — e sui prospetti
vale un divieto assoluto, perché sono la base di quanto viene pagato a una persona. Una correzione, se serve, la
fa il salone con la rettifica (storie `0019` e `0025`); la cancellazione di dati personali passa dagli strumenti
dei diritti dell'interessato (storia `0032`), non da un pulsante di assistenza.

## 6. Dati esposti alla console

| Dato | Genere | Contiene dati personali? | Perché serve |
|---|---|---|---|
| postazioni aperte su tetto, per account | metrica | no | diagnosi delle quote e proposta di piano |
| conteggi d'uso per account (appuntamenti, conti chiusi, schede, prodotti, prospetti) | metrica **aggregata** | no | capire se l'app è usata o abbandonata |
| scostamenti fra giacenza e somma dei movimenti, per account | metrica | no | diagnosi della classe di segnalazione «il magazzino sbaglia» |
| esito degli eventi di conto chiuso, con codice di errore e correlazione | metadato | no | rispondere a «in fatturazione non è arrivato niente» |
| stato dell'interruttore delle fotografie, numero di immagini, revoche non onorate | metadato | no (conteggi, mai immagini) | verificare che una revoca sia stata eseguita |
| deroghe concesse, con operatore e motivo | metadato | no (identifica un nostro operatore, già coperto) | responsabilità delle decisioni di assistenza |

**Verifica obbligatoria.** Nessuna riga di questa tabella contiene dati personali dei clienti del salone né di chi
vi lavora: è una proprietà voluta e va **provata**, non assunta — una prova deve verificare che le proiezioni
esposte alla console non contengano identificativi di cliente o di operatore, nomi, formule, note, importi
individuali né riferimenti a immagini. Se un giorno servisse esporre un identificativo (per esempio per
diagnosticare una postazione che non si libera), quella voce va aggiunta al manifesto dei dati con la finalità
«assistenza tecnica»: l'accesso amministrativo è un trattamento come gli altri.

## 7. Punti aperti

- **La durata massima della deroga sulle postazioni (proposta: 30 giorni)** e il tetto massimo concedibile non
  sono dati rilevati: sono valori ragionevoli scelti qui. Li chiude lo sviluppatore insieme al listino (fermata di
  escalation, [application-description.md](application-description.md) §5).
- **Se l'interruttore delle fotografie debba nascere spento** per tutti gli account fino a una revisione legale
  della funzione. È coerente con la prudenza del §6 della descrizione, ma renderebbe la funzione invisibile a chi
  ha comprato il piano che la comprende. È una decisione di prodotto e di rischio insieme, e la chiude lo
  sviluppatore.
- **La soglia oltre la quale uno scostamento fra giacenza e movimenti va segnalato** non è determinabile a priori:
  un salone che rettifica spesso ha scostamenti fisiologici. Va osservata sui primi clienti e corretta.
- **Sotto la via (b)** queste estensioni si aggiungono alla scheda di 07 BookGrove: chi le implementa deve
  decidere se mostrarle sempre o solo agli account che hanno il piano verticale acceso. Proposta: solo a quelli,
  perché una console piena di riquadri vuoti smette di essere letta.
