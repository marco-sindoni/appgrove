# 0030 — Percorso end-to-end del salone

**Applicazione**: 21 — SalonGrove (`salone`) · **Epica**: 07 — Esposizione conversazionale e prove
**Storia**: `0030` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0005`, `0006`, `0007`, `0009`, `0010`, `0017`, `0019`, `0023`, `0026`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come sviluppatore di piattaforma
> voglio una prova automatica che percorra tutta la catena di SalonGrove — dal servizio a fasi al conto chiuso —
> sullo stack locale reale
> così da sapere che la catena regge davvero, e non solo che ogni pezzo funziona da solo.

**Contesto.** Le prove di unità e di integrazione dicono che i pezzi funzionano; solo il percorso end-to-end dice
che una persona riesce a fare il suo lavoro. Per questa applicazione il percorso non è un adempimento: la ragione
d'essere di SalonGrove è **la catena** — servizio a fasi, poltrona occupata mentre l'operatore è libero, scheda
tecnica, conto chiuso, magazzino scaricato, provvigione maturata — e una catena si prova solo percorrendola tutta
([PRINCIPI-APPGROVE.md](../../_kit/PRINCIPI-APPGROVE.md) §11). Diciassette storie precedenti hanno risposto
«coprire ora» o «rimando» alla domanda di copertura rimandando **a questa**: qui i rimandi si onorano, invece di
sparire.

## 2. Requisiti funzionali

1. **RF-1** — Esiste `tools/platform-e2e/journeys/J-SALONGROVE.spec.ts` e **ogni** prova porta l'etichetta in testa
   al titolo: `test('[J-SALONGROVE] …')`.
2. **RF-2** — Il percorso principale, in sequenza: accesso; definizione di un servizio a fasi (applicazione 20′,
   posa 35′, finitura 25′) con l'indicazione di quale fase impegna l'operatore e quale solo la postazione;
   prenotazione di quel servizio; **prenotazione di un secondo cliente sullo stesso operatore durante la posa**,
   che deve riuscire; esecuzione; compilazione della scheda tecnica con formula e prodotti; apertura del conto già
   compilato; attribuzione delle righe a due operatori diversi; chiusura con modo d'incasso dichiarato; verifica
   dei **tre effetti** — giacenza di cabina scesa della dose prevista, punti maturati, provvigione maturata secondo
   la regola; apertura del cruscotto e verifica di scontrino medio e riempimento attesi.
3. **RF-3** — Un secondo percorso copre **i blocchi**: sovrapposizione rifiutata sulla stessa postazione durante la
   posa; tentativo di chiudere un conto con una riga priva di operatore (rifiutato); tentativo di riaprire un conto
   chiuso (rifiutato, con la rettifica come unica strada); rifiuto per **quota di postazioni esaurita**
   all'apertura di una postazione oltre il tetto del piano, con il messaggio che dice come rimediare.
4. **RF-4** — Un terzo percorso copre **la rettifica**: conto chiuso con una riga sbagliata, rettifica registrata,
   verifica che totale del giorno, provvigione maturata e giacenza si correggano di conseguenza e che restino
   visibili sia il conto sia la rettifica.
5. **RF-5** — Due prove **negative** verificano i confini di prodotto, e sono la parte di questa storia che non si
   può togliere:
   - nessuna schermata, rotta o esportazione raggiunta dal percorso produce un aggregato **raggruppato per
     operatore** o un ordinamento di persone (storia `0026`);
   - accanto al campo di **nota tecnica libera** compare l'avviso che vieta di scrivervi informazioni sulla salute
     del cliente, e nel modello dati non esiste alcun campo per allergie, patologie, farmaci o gravidanza
     (storia `0012`).
6. **RF-6** — Il registro [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) è aggiornato
   con le voci di questa applicazione — percorso, test e storie coperte — e il controllo automatico che lo
   sorveglia (area `tooling`) è verde.

## 3. Requisiti tecnici

- **RT-1 — Prove (§11).** Playwright senza finestra, sullo stack locale reale; **niente attese a tempo** — ogni
  attesa è su una condizione osservabile; accesso programmatico; dati di prova deterministici e **inventati**,
  quelli della storia `0005` (nomi di fantasia, indirizzi `*.test`).
- **RT-2 — Isolamento fra account (§1).** Il percorso usa i due saloni dei dati di prova e verifica **anche
  dall'interfaccia** che l'uno non veda clienti, schede tecniche, conti e giacenze dell'altro, nemmeno forzando
  l'indirizzo di una pagina di dettaglio.
- **RT-3 — Cinque lingue (§4).** Almeno un passaggio del percorso gira in una lingua diversa dall'italiano, per
  scoprire le stringhe non tradotte che le prove di unità non vedono; gli importi e le durate si verificano nel
  formato di quella lingua.
- **RT-4 — Registro di copertura.** Le voci nuove dichiarano applicazione, percorso e test, e chiudono i rimandi
  delle storie precedenti: registro incoerente significa suite rossa.
- **RT-5 — Dati personali (§10).** Nessun dato reale in nessun punto della prova: è un requisito, non una
  raccomandazione. Il percorso **non** cancella dati (storia `0032`), perché una prova che distrugge i propri dati
  di partenza è fragile; le fotografie del trattamento restano fuori dal percorso principale (storia `0013`), dove
  il caricamento di un file rende la prova instabile senza aggiungere copertura utile.
- **RT-6 — Esposizione conversazionale (§12).** Nessuna prova del livello conversazionale: non esiste ancora un
  livello da percorrere (casi d'uso 0061-0063).

## 4. Criteri di accettazione

**CA-1 — La catena regge**
- **Dato** lo stack locale avviato con i dati di prova
- **Quando** si esegue `./run-tests.sh platform`
- **Allora** il percorso `[J-SALONGROVE]` completa servizio a fasi → doppia prenotazione durante la posa → scheda
  tecnica → conto chiuso, e i tre effetti hanno i valori attesi, calcolati a mano nel codice della prova

**CA-2 — La poltrona è occupata, l'operatore no**
- **Dato** un colore prenotato alle 9:00 con posa dalle 9:20 alle 9:55
- **Quando** il percorso prenota un taglio con lo stesso operatore alle 9:25
- **Allora** la prenotazione riesce, e una seconda prenotazione sulla **stessa postazione** nello stesso intervallo
  viene rifiutata

**CA-3 — I blocchi bloccano**
- **Dato** un conto con una riga senza operatore e un account al tetto delle postazioni
- **Quando** il percorso tenta la chiusura e poi l'apertura di una postazione in più
- **Allora** vede rispettivamente il messaggio sulla riga incompleta e il messaggio di quota con il rimedio, e nulla
  viene chiuso né creato

**CA-4 — La rettifica corregge tutto**
- **Dato** un conto chiuso con una riga sbagliata di 20 €
- **Quando** il percorso registra la rettifica
- **Allora** totale del giorno, provvigione maturata e giacenza risultano corretti, e restano visibili sia il conto
  sia la rettifica

**CA-5 — I confini di prodotto tengono**
- **Dato** il percorso completo eseguito
- **Quando** le prove negative interrogano cruscotto, esportazioni, rotte di rendicontazione e scheda tecnica
- **Allora** non esiste alcun aggregato per operatore né alcun ordinamento di persone, l'avviso sulla nota libera è
  presente, e nessun campo per dati sulla salute esiste nel modello

**CA-6 — Registro coerente e percorso stabile**
- **Dato** il registro aggiornato
- **Quando** si esegue `./run-tests.sh tooling` e si ripete il percorso dieci volte
- **Allora** il controllo di copertura è verde e il percorso non fallisce mai in modo casuale

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (in particolare `platform` e `tooling`; l'intera suite prima del commit);
- [ ] il percorso è **stabile**: eseguito dieci volte di seguito non fallisce mai in modo casuale;
- [ ] prova di **isolamento fra account** anche dall'interfaccia;
- [ ] **prova end-to-end**: **coperta ora** — è questa la storia che onora i rimandi delle storie `0003`, `0004`,
      `0005`, `0006`, `0007`, `0008`, `0009`, `0010`, `0011`, `0012`, `0013`, `0015`, `0016`, `0017`, `0018`,
      `0019`, `0021`, `0023`, `0024`, `0026` e `0027`;
- [ ] **traduzioni**: almeno un passaggio del percorso in una lingua diversa dall'italiano;
- [ ] **manifesto dei dati**: nessuna voce nuova — la prova non introduce dati, usa quelli inventati;
- [ ] **registro delle decisioni**: quali percorsi si è scelto di coprire e perché proprio quelli, quali no;
- [ ] registro di copertura [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) aggiornato e
      verde;
- [ ] avvio locale invariato (`./dev.sh services`, `./app-start.sh`).

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0005` | i dati di prova deterministici e inventati, e i due saloni |
| storia `0006` | il servizio a fasi è il primo passo del percorso |
| storia `0007` | la doppia prenotazione durante la posa è la verifica che distingue questa app da un'agenda |
| storia `0009` | la sequenza a più operatori dà le righe da attribuire |
| storia `0010` | la scheda tecnica è il passo di mezzo |
| storia `0017` | lo scarico di cabina è uno dei tre effetti verificati |
| storia `0019` | la chiusura del conto è il passo decisivo |
| storia `0023` | l'attribuzione è ciò che rende verificabile la provvigione maturata |
| storia `0026` | il cruscotto e la prova negativa sulla classifica |

## 7. Fuori ambito

- il percorso del **pacchetto prepagato** e della fedeltà: storia `0031`, che ha una catena propria e merita una
  prova propria;
- la **cancellazione dei dati** e l'esportazione: storia `0032`, per non distruggere i dati di partenza;
- il **caricamento delle fotografie**: fragile in una prova automatica, coperto dalle prove di integrazione
  (storia `0013`);
- le prove del **livello conversazionale**: non esiste ancora un livello da percorrere;
- la **lista di riordino** (storia `0018`), che è una lettura derivata già coperta dalle prove di integrazione.

## 8. Punti aperti

Nessuno.
