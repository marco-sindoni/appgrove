# 0031 — Percorso end-to-end del pacchetto

**Applicazione**: 21 — SalonGrove (`salone`) · **Epica**: 07 — Esposizione conversazionale e prove
**Storia**: `0031` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0005`, `0020`, `0022`, `0025`, `0030`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come sviluppatore di piattaforma
> voglio una seconda prova automatica che segua un pacchetto prepagato dalla vendita all'esaurimento, con i punti
> fedeltà e la provvigione che ne discendono
> così da sapere che il denaro incassato in anticipo e consumato nel tempo torna sempre, che è la cosa che un
> salone non perdona.

**Contesto.** Il pacchetto è la parte del verticale in cui un errore si scopre **mesi dopo** — «avevo dieci sedute,
me ne risultano sei e ne ho fatte tre» — ed è quella con una conseguenza fiscale a valle: le due specie di buono
corrispettivo, monouso e multiuso, sono rilevanti in momenti diversi (§2.3 punto 4 della
[descrizione](../application-description.md), storia `0020`). Merita un percorso proprio, separato da quello del
salone (storia `0030`), perché copre una catena diversa e più lunga nel tempo: vendita → utilizzo → residuo →
esaurimento o scadenza, con punti e provvigione agganciati.

## 2. Requisiti funzionali

1. **RF-1** — Esiste `tools/platform-e2e/journeys/J-SALONGROVE-PKG.spec.ts` e **ogni** prova porta l'etichetta in
   testa al titolo: `test('[J-SALONGROVE-PKG] …')`.
2. **RF-2** — Il percorso principale, in sequenza: vendita di un pacchetto **a sedute determinate** (dieci sedute
   dello stesso servizio, specie «monouso»); verifica che la specie e la data di vendita siano registrate;
   appuntamento eseguito e conto chiuso pagando **con il pacchetto**; verifica che il residuo scenda di una seduta,
   che l'utilizzo sia registrato con la sua data, che i punti fedeltà maturino secondo la regola e che la
   provvigione dell'operatore che ha eseguito maturi come atteso; ripetizione fino all'esaurimento; verifica dello
   stato finale `esaurito`.
3. **RF-3** — Un secondo percorso copre il **pacchetto a valore** (specie «multiuso»): vendita di un credito,
   utilizzo parziale su un conto con due righe, verifica del valore residuo e del fatto che la specie resti
   distinta da quella a sedute.
4. **RF-4** — Un terzo percorso copre **i casi limite**: utilizzo di un pacchetto **scaduto** (rifiutato, con il
   messaggio che dice quando è scaduto); tentativo di scalare più sedute di quante ne restino (rifiutato);
   annullamento di un pacchetto **con una seduta già usata** (rifiutato, perché l'annullamento è ammesso solo a
   pacchetto intatto); spesa di punti fedeltà oltre il saldo (rifiutata).
5. **RF-5** — Una prova **negativa** verifica che il residuo mostrato a schermo, quello restituito
   dall'interfaccia di programmazione e quello ricalcolato dagli utilizzi coincidano sempre: è il difetto che in
   questa parte del dominio costa di più, e che una prova per pezzi non vedrebbe.
6. **RF-6** — Il registro [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) è aggiornato
   con il percorso, i test e le storie coperte, e il controllo automatico dell'area `tooling` è verde.

## 3. Requisiti tecnici

- **RT-1 — Prove (§11).** Playwright senza finestra, sullo stack locale reale; niente attese a tempo; accesso
  programmatico; dati di prova deterministici e **inventati** (storia `0005`). Le date di scadenza si costruiscono
  relative all'istante della prova, mai fisse nel calendario, o la prova morirà da sola fra un anno.
- **RT-2 — Isolamento fra account (§1).** Il percorso verifica che un pacchetto di un salone non sia utilizzabile
  né visibile dall'altro, anche forzando l'indirizzo della pagina di dettaglio.
- **RT-3 — Coerenza del residuo.** La verifica dell'RF-5 confronta tre sorgenti (schermata, rotta, somma degli
  utilizzi): è lo stesso principio già affermato per la giacenza di magazzino, dove la verità è la somma dei
  movimenti e la colonna è una comodità di lettura (§4 della descrizione).
- **RT-4 — Cinque lingue (§4).** Almeno un passaggio gira in una lingua diversa dall'italiano, con la verifica dei
  formati di data e di importo di quella lingua.
- **RT-5 — Dati personali (§10).** Nessun dato reale; il percorso non cancella nulla — la cancellazione di un
  cliente **con pacchetto aperto** è un caso delicato e ha la sua prova nella storia `0032`.
- **RT-6 — Registro di copertura.** Le voci dichiarano applicazione, percorso, test e storie coperte, e onorano i
  rimandi delle storie `0020`, `0022` e `0025`.

## 4. Criteri di accettazione

**CA-1 — La seduta si scala e tutto matura**
- **Dato** un pacchetto da dieci sedute venduto a 300 € e un appuntamento eseguito
- **Quando** il percorso chiude il conto pagando con il pacchetto
- **Allora** il residuo passa a nove, l'utilizzo è registrato con la data, i punti fedeltà maturano secondo la
  regola e la provvigione dell'operatore è quella attesa, calcolata a mano nella prova

**CA-2 — Fino all'esaurimento**
- **Dato** lo stesso pacchetto · **Quando** il percorso consuma le dieci sedute
- **Allora** lo stato finale è `esaurito` e un ulteriore utilizzo viene rifiutato

**CA-3 — Le due specie restano distinte**
- **Dato** un pacchetto a sedute e uno a valore
- **Quando** si leggono entrambi
- **Allora** ciascuno dichiara la propria specie, e quello a valore scala un importo mentre l'altro scala sedute

**CA-4 — I casi limite sono rifiutati con un motivo**
- **Dato** un pacchetto scaduto, uno con due sedute residue e uno con una seduta già usata
- **Quando** il percorso tenta rispettivamente l'utilizzo, lo scarico di tre sedute e l'annullamento
- **Allora** ogni tentativo è rifiutato con un messaggio che dice il perché, e nessuno stato cambia

**CA-5 — Il residuo è sempre lo stesso numero**
- **Dato** un pacchetto con utilizzi sparsi nel tempo
- **Quando** si confrontano schermata, rotta e somma degli utilizzi
- **Allora** i tre valori coincidono

**CA-6 — Registro coerente e percorso stabile**
- **Dato** il registro aggiornato
- **Quando** si esegue `./run-tests.sh tooling` e si ripete il percorso dieci volte
- **Allora** il controllo di copertura è verde e il percorso non fallisce mai in modo casuale

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (in particolare `platform` e `tooling`);
- [ ] il percorso è **stabile**: eseguito dieci volte di seguito non fallisce mai in modo casuale;
- [ ] prova di **isolamento fra account** sui pacchetti, anche dall'interfaccia;
- [ ] **prova end-to-end**: **coperta ora** — onora i rimandi delle storie `0020`, `0022` e `0025`;
- [ ] **traduzioni**: almeno un passaggio in una lingua diversa dall'italiano, con i formati di data verificati;
- [ ] **manifesto dei dati**: nessuna voce nuova;
- [ ] **registro delle decisioni**: casi limite coperti e casi lasciati alle prove di integrazione, con il motivo;
- [ ] registro di copertura [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) aggiornato e
      verde;
- [ ] avvio locale invariato.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0005` | i dati di prova, che comprendono un pacchetto già venduto e uno prossimo alla scadenza |
| storia `0020` | è la catena che questo percorso verifica |
| storia `0022` | i punti fedeltà maturano nello stesso momento e vanno verificati insieme |
| storia `0025` | la provvigione maturata sull'utilizzo è la parte che nessuna prova per pezzi vede |
| storia `0030` | riusa l'impianto del percorso principale: accesso, dati, convenzioni di attesa |

## 7. Fuori ambito

- la **cancellazione** di un cliente con pacchetto aperto: storia `0032`, dove sta il caso e la sua prova;
- il **rimborso** di un pacchetto non consumato in denaro: appgrove non muove denaro, e la decisione commerciale è
  del salone;
- la **rilevanza fiscale** delle due specie di buono: l'app registra la distinzione, non calcola imposte (app 01 e
  02).

## 8. Punti aperti

Nessuno.
