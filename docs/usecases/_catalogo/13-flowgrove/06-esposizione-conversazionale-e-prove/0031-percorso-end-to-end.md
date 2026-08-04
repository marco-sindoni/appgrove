# 0031 — Percorso end-to-end

**Applicazione**: 13 — FlowGrove (`progetti`) · **Epica**: 06 — Esposizione conversazionale e prove
**Storia**: `0031` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0005`, `0006`, `0011`, `0017`, `0020`, `0022`, `0026`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come sviluppatore di piattaforma
> voglio una prova automatica che percorra tutta la catena di FlowGrove — dal progetto alla riga fatturabile —
> sullo stack locale reale
> così da sapere che l'anello fra il preventivo e la fattura regge davvero, e non solo che ogni pezzo funziona da
> solo.

**Contesto.** Le prove di unità e di integrazione dicono che i pezzi funzionano; solo il percorso end-to-end dice
che una persona riesce a fare il suo lavoro. Per questa app il percorso non è un adempimento: la ragione d'essere
di FlowGrove è **la catena** — attività, ore dichiarate, tariffa, periodo chiuso, lotto consegnato, margine — e
una catena si prova solo percorrendola tutta. Quattordici storie precedenti hanno risposto «coprire ora» alla
domanda di copertura rimandando **a questa**: qui i rimandi si onorano, invece di sparire
([PRINCIPI-APPGROVE.md](../../_kit/PRINCIPI-APPGROVE.md) §11).

## 2. Requisiti funzionali

1. **RF-1** — Esiste `tools/platform-e2e/journeys/J-PROGETTI.spec.ts` e **ogni** prova porta l'etichetta in testa
   al titolo: `test('[J-PROGETTI] …')`.
2. **RF-2** — Il percorso principale, in sequenza: accesso; creazione di un progetto con budget in ore e in
   importo; creazione di due attività; assegnazione a un collaboratore con scadenza; spostamento di stato dalla
   lavagna **con l'alternativa da tastiera** (il trascinamento è instabile in una prova automatica);
   dichiarazione delle ore dalla schermata «Le mie attività» e una settimana dal foglio ore; verifica
   dell'importo maturato con la tariffa attesa; chiusura del periodo; composizione e consegna del lotto
   fatturabile; verifica del margine di commessa atteso.
3. **RF-3** — Un secondo percorso copre **lo sforamento**: ore che superano la soglia del budget, avviso mostrato
   **prima** dello sforamento, e verifica che l'avanzamento del progetto lo riporti.
4. **RF-4** — Un terzo percorso copre **i blocchi**: tentativo di consegna su un periodo ancora aperto (`409`),
   modifica di una riga di ore in un periodo chiuso (rifiutata, con la rettifica tracciata come unica strada), e
   rifiuto per **quota dei posti esaurita** all'aggiunta di una persona oltre il tetto del piano, con il messaggio
   che dice quante persone vanno rimosse.
5. **RF-5** — Una prova **negativa** verifica il confine di prodotto: nessuna schermata e nessuna rotta
   raggiungibile dal percorso restituisce un aggregato economico raggruppato **per persona** fuori dal contesto
   di un progetto e di un periodo (storia 0026, §6 della descrizione). È l'unico modo per impedire che la
   sorveglianza rientri da una finestra.
6. **RF-6** — Il registro [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) è aggiornato
   con le voci di questa applicazione — percorso, test e storie coperte — e il controllo automatico che lo
   sorveglia (area `tooling`) è verde.

## 3. Requisiti tecnici

- **RT-1 — Prove (§11).** Playwright senza finestra, sullo stack locale reale; **niente attese a tempo** — ogni
  attesa è su una condizione osservabile; accesso programmatico; dati di prova deterministici e **inventati**,
  quelli della storia `0005` (nomi di fantasia, indirizzi `*.test`).
- **RT-2 — Isolamento fra account (§1).** Il percorso usa i due account dei dati di prova e verifica **anche
  dall'interfaccia** che l'uno non veda progetti, attività e ore dell'altro.
- **RT-3 — Cinque lingue (§4).** Almeno un passaggio del percorso gira in una lingua diversa dall'italiano, per
  scoprire le stringhe non tradotte che le prove di unità non vedono; gli importi e le durate si verificano nel
  formato di quella lingua.
- **RT-4 — Registro di copertura.** Le voci nuove dichiarano applicazione, percorso e test, e chiudono i rimandi
  delle storie precedenti: registro incoerente significa suite rossa.
- **RT-5 — Dati personali (§10).** Nessun dato reale in nessun punto della prova: è un requisito, non una
  raccomandazione. Il percorso non cancella dati (storia 0030) perché una prova che distrugge i propri dati di
  partenza è fragile.
- **RT-6 — Esposizione conversazionale (§12).** Nessuno strumento in questa storia, e nessuna prova del livello
  conversazionale: non esiste ancora un livello da percorrere (casi d'uso 0061-0063).

## 4. Criteri di accettazione

**CA-1 — La catena regge**
- **Dato** lo stack locale avviato con i dati di prova
- **Quando** si esegue `./run-tests.sh platform`
- **Allora** il percorso `[J-PROGETTI]` completa progetto → attività → assegnazione → ore → periodo chiuso →
  lotto consegnato, e il margine finale è quello atteso, calcolato a mano nel codice della prova

**CA-2 — Lo sforamento si vede prima**
- **Dato** un progetto con budget di 40 ore
- **Quando** il percorso dichiara ore fino a superare la soglia d'avviso
- **Allora** l'interfaccia mostra l'avviso **prima** dello sforamento e l'avanzamento lo riporta

**CA-3 — I blocchi bloccano**
- **Dato** un periodo ancora aperto e un account al tetto dei posti
- **Quando** il percorso tenta la consegna e poi l'aggiunta di una persona in più
- **Allora** vede rispettivamente il messaggio «chiudi prima il periodo» e il messaggio di quota con il rimedio, e
  nulla viene creato né consegnato

**CA-4 — Nessuna classifica di persone**
- **Dato** il percorso completo eseguito
- **Quando** la prova negativa interroga le schermate e le rotte di rendicontazione
- **Allora** non esiste alcun aggregato economico per persona fuori dal contesto di progetto e periodo

**CA-5 — Isolamento visibile dall'interfaccia**
- **Dato** i due account dei dati di prova
- **Quando** il percorso accede con l'uno e cerca i progetti dell'altro
- **Allora** non ne trova nessuno, nemmeno forzando l'indirizzo di una pagina di dettaglio

**CA-6 — Registro coerente e percorso stabile**
- **Dato** il registro aggiornato
- **Quando** si esegue `./run-tests.sh tooling` e si ripete il percorso dieci volte
- **Allora** il controllo di copertura è verde e il percorso non fallisce mai in modo casuale

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (in particolare `platform` e `tooling`; l'intera suite prima del
      commit);
- [ ] il percorso è **stabile**: eseguito dieci volte di seguito non fallisce mai in modo casuale;
- [ ] prova di **isolamento fra account** anche dall'interfaccia;
- [ ] **prova end-to-end**: **coperta ora** — è questa la storia che onora i rimandi delle storie `0003`, `0004`,
      `0005`, `0006`, `0007`, `0009`, `0011`, `0012`, `0013`, `0017`, `0018`, `0019`, `0020`, `0021`, `0022`,
      `0024`, `0025` e `0026`;
- [ ] **traduzioni**: almeno un passaggio del percorso in una lingua diversa dall'italiano;
- [ ] **manifesto dei dati**: nessuna voce nuova — la prova non introduce dati, usa quelli inventati;
- [ ] **registro delle decisioni** compilato: quali percorsi si è scelto di coprire e perché proprio quelli, quali
      no;
- [ ] registro di copertura [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) aggiornato e
      verde;
- [ ] avvio locale invariato (`./dev.sh services`, `./app-start.sh`).

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0005` | i dati di prova deterministici e inventati, e i due account |
| storia `0006` | la creazione del progetto è il primo passo |
| storia `0011` | lo spostamento di stato, eseguito con l'alternativa da tastiera |
| storia `0017` | la dichiarazione delle ore è il passo centrale |
| storia `0020` | la chiusura del periodo precede la consegna |
| storia `0022` | la consegna del lotto è l'ultimo passo e la verifica decisiva |
| storia `0026` | il margine atteso è ciò che il percorso controlla alla fine |

## 7. Fuori ambito

- il percorso che parte da un **preventivo accettato** in 06 QuoteGrove (storia 0023): dipenderebbe da un'app che
  non esiste ancora; il percorso crea il progetto a mano e il rimando resta scritto;
- lo **scarico di file** (esportazioni della storia 0027 e allegati della storia 0015): fragile da verificare in
  una prova automatica, e coperto dalle prove di integrazione;
- la **cancellazione dei dati** (storia 0030), per non distruggere i dati di partenza del percorso;
- le prove del **livello conversazionale**: non esiste ancora un livello da percorrere;
- gli **avvisi di ritardo** (storia 0016), che dipendono da una lavorazione programmata: attenderla renderebbe il
  percorso lento e instabile.

## 8. Punti aperti

Nessuno.
