# 0029 — Percorso end-to-end interno

**Applicazione**: 06 — QuoteGrove (`preventivi`) · **Epica**: 06 — Esposizione conversazionale e prove
**Storia**: `0029` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0017`, `0026`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come sviluppatore di piattaforma
> voglio una prova automatica che percorra tutto il lavoro di chi vende, dalla creazione all'invio, sullo stack
> locale reale
> così da sapere che la catena regge davvero, e non solo che ogni pezzo funziona da solo.

**Contesto.** Le prove di unità e di integrazione dicono che i pezzi funzionano; solo il percorso end-to-end dice
che una persona riesce a fare il suo lavoro. Questa storia crea il percorso `[J-PREVENTIVI]` e raccoglie i
rimandi lasciati dalle storie precedenti, che qui vengono onorati invece di essere dimenticati.

## 2. Requisiti funzionali

1. **RF-1** — Esiste `tools/platform-e2e/journeys/J-PREVENTIVI.spec.ts` e ogni prova porta l'etichetta in testa al
   titolo: `test('[J-PREVENTIVI] …')`.
2. **RF-2** — Il percorso principale, in sequenza: accesso, creazione di un destinatario, creazione di un
   preventivo con due righe dal catalogo, verifica del totale calcolato, generazione dell'anteprima, invio,
   verifica che lo stato sia `inviato`, verifica che il consumo di quota sia aumentato di uno.
3. **RF-3** — Un secondo percorso copre il blocco: sconto sopra la soglia di approvazione, tentativo di invio,
   rifiuto con il messaggio giusto, approvazione, invio riuscito.
4. **RF-4** — Un terzo percorso copre la quota esaurita: tentativo di invio, `429` mostrato all'utente in modo
   comprensibile, nessun documento inviato.
5. **RF-5** — Il registro [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) è aggiornato
   con le voci di questa applicazione, e il controllo automatico che lo sorveglia è verde.

## 3. Requisiti tecnici

- **RT-1 — Prove (§11).** Playwright senza finestra, sullo stack locale reale; **niente attese a tempo**; accesso
  programmatico; dati di prova deterministici e **inventati** (indirizzi `*.test`), quelli della storia `0005`.
- **RT-2 — Isolamento fra account (§1).** Il percorso usa i due account dei dati di prova e verifica anche
  dall'interfaccia che l'uno non veda i documenti dell'altro.
- **RT-3 — Cinque lingue (§4).** Almeno un passaggio del percorso gira in una lingua diversa dall'italiano, per
  scoprire le stringhe non tradotte che le prove di unità non vedono.
- **RT-4 — Registro di copertura.** Le voci nuove dichiarano l'applicazione, il percorso e i suoi test: registro
  incoerente significa suite rossa.
- **RT-5 — Dati personali (§10).** Nessun dato reale in nessun punto della prova: è un requisito, non una
  raccomandazione.

## 4. Criteri di accettazione

**CA-1 — Il percorso principale è verde**
- **Dato** lo stack locale avviato con i dati di prova · **Quando** si esegue `./run-tests.sh platform`
- **Allora** il percorso `[J-PREVENTIVI]` completa creazione, calcolo, anteprima e invio, e verifica il consumo di
  quota

**CA-2 — Il blocco per approvazione**
- **Dato** un preventivo scontato sopra soglia · **Quando** il percorso tenta l'invio · **Allora** vede il
  messaggio di approvazione mancante, approva con un utente autorizzato e completa l'invio

**CA-3 — La quota esaurita si vede**
- **Dato** l'account dei dati di prova con quota esaurita · **Quando** il percorso tenta l'invio · **Allora**
  l'interfaccia mostra il messaggio di quota con il rimedio, e nessun documento risulta inviato

**CA-4 — Registro coerente**
- **Dato** il registro aggiornato · **Quando** si esegue `./run-tests.sh tooling` · **Allora** il controllo di
  copertura è verde

**CA-5 — Nessuna attesa a tempo**
- **Dato** il codice della prova · **Quando** lo si esamina · **Allora** non contiene pause fisse: ogni attesa è
  su una condizione osservabile

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (in particolare `platform` e `tooling`);
- [ ] il percorso è stabile: eseguito dieci volte di seguito non fallisce mai in modo casuale;
- [ ] prova di **isolamento fra account** anche dall'interfaccia;
- [ ] **prova end-to-end**: **coperta ora** — è questa la storia che onora i rimandi delle storie `0003`, `0006`,
      `0008`-`0014`, `0017` e `0026`;
- [ ] **traduzioni**: almeno un passaggio in una lingua diversa dall'italiano;
- [ ] **manifesto dei dati**: nessuna voce nuova;
- [ ] **registro delle decisioni** compilato (percorsi scelti e perché proprio quelli);
- [ ] registro di copertura [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) aggiornato e
      verde;
- [ ] avvio locale invariato.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0017` | l'invio è il punto d'arrivo del percorso |
| storia `0026` | la Panoramica è ciò che il percorso verifica alla fine |
| storia `0005` | i dati di prova deterministici |

## 7. Fuori ambito

- il percorso del destinatario, che parte da fuori e non ha accesso: storia `0030`;
- le prove del livello conversazionale: non esiste ancora un livello da percorrere.

## 8. Punti aperti

Nessuno.
