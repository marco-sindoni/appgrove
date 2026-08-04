# 0005 — Avvio locale e dati di prova

**Applicazione**: 20 — InsightGrove (`insights`) · **Epica**: 01 — Fondamenta
**Storia**: `0005` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0001`, `0002`, `0003`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come sviluppatore che riprende il lavoro su questa app
> voglio poterla avviare in locale e trovarla già piena di numeri inventati, di due account diversi
> così da poter vedere e collaudare cruscotti, indicatori e copilota **prima** che esista una sola app sorgente
> vera.

**Contesto.** InsightGrove ha un problema che nessun'altra app del catalogo ha: **non produce i propri dati**.
Finché non esistono BillGrove, CashGrove e StockGrove che pubblicano fatti, l'app è vuota e non si può né vedere
né collaudare. Non si può nemmeno aspettare: la nota del catalogo dice che InsightGrove va costruita dopo 3-4
app, ma le storie vanno scritte e provate comunque. La soluzione è un **generatore di fatti inventati** che
simula quello che le fonti pubblicheranno, con due account distinti, così che ogni collaudo di isolamento abbia
materia vera su cui girare.

## 2. Requisiti funzionali

1. **RF-1** — Esiste un comando di sviluppo che popola il magazzino con fatti inventati per **due account
   diversi**, su almeno tre app d'origine simulate (fatturazione, incassi, magazzino) e almeno tredici mesi di
   storico, così che i confronti con l'anno precedente abbiano dati.
2. **RF-2** — I fatti generati sono **deterministici**: lo stesso comando produce gli stessi numeri, così che i
   collaudi possano asserire valori attesi.
3. **RF-3** — I dati generati sono **palesemente inventati**: nomi di fantasia, nessun indirizzo reale, valori
   arrotondati e riconoscibili come finti. Mai dati veri, mai dati verosimili al punto da sembrare di un cliente.
4. **RF-4** — Il generatore produce anche i casi che rompono le cose: un mese con un buco in una fonte, una
   fonte silente da due settimane, un fatto corretto da un fatto successivo con la stessa chiave di idempotenza.
5. **RF-5** — Il generatore **passa dallo stesso percorso di ingresso** dei fatti veri (l'evento, storia 0007):
   non scrive direttamente nel database. Altrimenti collauderebbe una strada che in produzione non esiste.

## 3. Requisiti tecnici

- **RT-15 — Avvio locale (§15).** `./dev.sh services` mostra `insights` con la sua porta e il suo schema;
  `./app-start.sh` la avvia senza modifiche manuali agli script; il comando di popolamento è un sottocomando di
  `dev`, non uno script sparso.
- **RT-1 — Isolamento fra account (§1).** I due account generati sono la base di tutte le prove di isolamento
  delle storie successive: i loro dati devono essere **distinguibili a occhio** (ordini di grandezza diversi), in
  modo che una fuga si veda subito.
- **RT-11 — Prove (§11).** I dati di prova sono deterministici e inventati; gli indirizzi di posta elettronica
  eventualmente generati usano il dominio `*.test`; nessuna attesa a tempo nei collaudi che li usano.
- **RT-8 — Dati personali (§10).** Nessun dato personale nuovo: i dati generati sono di persone che non
  esistono. Va comunque scritto nel registro delle decisioni che il generatore **non deve mai essere eseguibile
  in un ambiente diverso da quello locale**.
- **RT-14 — Registrazione eventi (§14).** Il generatore registra quanti fatti ha prodotto per account e per
  fonte, senza contenuti.

## 4. Criteri di accettazione

**CA-1 — L'app si avvia e si popola**
- **Dato** un repository pulito
- **Quando** si esegue `./app-start.sh` e poi il comando di popolamento
- **Allora** entrando in InsightGrove con un utente del primo account si vedono numeri, e con un utente del
  secondo se ne vedono altri, diversi

**CA-2 — Il popolamento è ripetibile**
- **Dato** un magazzino già popolato
- **Quando** si esegue di nuovo il comando di popolamento
- **Allora** i fatti restano quelli (l'idempotenza li riconosce) e i numeri mostrati non cambiano

**CA-3 — I casi difficili ci sono**
- **Dato** il magazzino popolato
- **Quando** si guarda lo stato delle fonti
- **Allora** almeno una fonte risulta silente oltre il proprio ritardo atteso e almeno un periodo risulta
  incompleto

**CA-4 — Il generatore non scavalca il percorso vero**
- **Dato** il ramo di questa storia
- **Quando** si guarda il codice del generatore
- **Allora** produce eventi e li immette nella coda locale, e **non** contiene alcuna scrittura diretta sulla
  tabella `fatto`

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno le aree toccate; l'intera suite prima del commit);
- [ ] prova di **integrazione** che esegue il generatore e verifica il conteggio dei fatti per account;
- [ ] prova di **isolamento fra account**: i due account generati sono usati dalle prove di isolamento;
- [ ] **prova end-to-end**: *rimando* alla storia 0034; voce `da-coprire` nel registro di copertura;
- [ ] **traduzioni**: non applicabile (comando di sviluppo, non superficie utente);
- [ ] **manifesto dei dati**: nessuna modifica;
- [ ] **registro delle decisioni** compilato, con il vincolo «solo in locale» e il perché il generatore passa
      dalla coda;
- [ ] documentazione di sviluppo aggiornata con il comando di popolamento.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0001` | serve il servizio e la coda |
| storia `0002` | serve il magazzino dove i fatti atterrano |
| storia `0003` | serve l'interfaccia dove vederli |
| storia `0007` (a valle) | il generatore usa il percorso di ingresso vero: finché non esiste, immette direttamente nella coda e la storia 0007 lo riallinea |

## 7. Fuori ambito

- i dati di prova per il collaudo end-to-end del percorso `[J-INSIGHTS]`: sono della storia 0034, che li vuole
  minimi e mirati, non l'archivio completo che questa storia produce;
- il ripopolamento dello storico da una fonte vera: storia 0009.

## 8. Punti aperti

- **Il generatore serve anche in ambiente di prova?** Riempire l'ambiente di prova di numeri finti aiuta le
  dimostrazioni e insieme confonde chi ci fa collaudi veri. Raccomandazione: **solo in locale**, e le
  dimostrazioni si fanno con un account locale. Chiude: **sviluppatore**.
