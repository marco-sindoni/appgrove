# 0030 — Percorso end-to-end del destinatario

**Applicazione**: 06 — QuoteGrove (`preventivi`) · **Epica**: 06 — Esposizione conversazionale e prove
**Storia**: `0030` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0019`, `0025`, `0029`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come sviluppatore di piattaforma
> voglio una prova automatica che percorra la parte del cliente: apre il collegamento, legge l'offerta, accetta
> così da sapere che la superficie più esposta dell'applicazione funziona e non perde colpi.

**Contesto.** La pagina pubblica è l'unica parte di appgrove che una persona **fuori** dall'account può
raggiungere, e l'accettazione è l'atto su cui poggia il valore probatorio di tutto (storia `0019`). È anche il
punto in cui un errore costa di più: un collegamento che non funziona è un'offerta persa; un collegamento che
funziona troppo è una violazione di dati. Questa storia chiude il catalogo dell'applicazione con la prova che
quella parte regge, e completa il registro di copertura.

## 2. Requisiti funzionali

1. **RF-1** — Il percorso, in sequenza: partendo da un preventivo inviato, si apre il collegamento riservato senza
   alcun accesso, si verifica che il documento sia quello giusto e nella lingua giusta, si accetta con nome e
   presa visione, si verifica che il preventivo risulti `accettato` nel backoffice con la sua prova.
2. **RF-2** — Un secondo percorso copre il rifiuto con motivo, e verifica che il documento non sia più accettabile.
3. **RF-3** — Un terzo percorso copre i collegamenti che non devono funzionare: scaduto, revocato, manomesso, di
   un altro account — tutti portano alla **stessa** pagina neutra.
4. **RF-4** — Il percorso verifica che dall'accettazione nasca il messaggio in uscita `preventivo.accettato`
   (storia `0025`).
5. **RF-5** — Il registro [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) è completato
   con le voci di questa applicazione, e il controllo automatico è verde.

## 3. Requisiti tecnici

- **RT-1 — Prove (§11).** Playwright senza finestra sullo stack locale reale; niente attese a tempo; dati
  inventati (`*.test`); il percorso porta l'etichetta `[J-PREVENTIVI]` in testa al titolo di ogni test, come
  quello interno.
- **RT-2 — Isolamento fra account (§1).** Il caso «collegamento di un altro account» è parte del percorso, non
  una prova a parte: è il modo in cui l'isolamento si vede davvero.
- **RT-3 — Cinque lingue (§4).** Il percorso verifica che la pagina pubblica sia resa nella lingua del
  destinatario, non in quella di chi ha inviato.
- **RT-4 — Accessibilità.** Controllo automatico di accessibilità sulla pagina pubblica: è la schermata che
  raggiungono le persone di cui non sappiamo nulla, ed è quella dove l'accessibilità conta di più.
- **RT-5 — Dati personali (§10).** Nessun dato reale; il percorso verifica anche che l'informativa breve sia
  presente sulla pagina (storia `0018`).
- **RT-6 — Registro di copertura.** Le voci dell'applicazione sono completate qui: percorso interno (storia
  `0029`) e percorso del destinatario, con i rimandi dichiarati dalle storie che hanno risposto «rimando».

## 4. Criteri di accettazione

**CA-1 — Il cliente accetta**
- **Dato** un preventivo inviato · **Quando** il percorso apre il collegamento, legge e accetta · **Allora** il
  backoffice mostra lo stato `accettato` e la prova con nome, momento e impronta della versione

**CA-2 — Il cliente rifiuta**
- **Dato** un altro preventivo inviato · **Quando** il percorso rifiuta indicando un motivo · **Allora** il
  documento è `rifiutato`, il motivo è visibile e il collegamento non accetta più

**CA-3 — Collegamenti che non devono funzionare**
- **Dato** un collegamento scaduto, uno revocato, uno manomesso e uno di un altro account · **Quando** il percorso
  li apre uno per uno · **Allora** vede la stessa pagina neutra in tutti e quattro i casi

**CA-4 — L'evento nasce**
- **Dato** l'accettazione del primo percorso · **Quando** si guarda la coda dei messaggi in uscita · **Allora**
  esiste `preventivo.accettato` con il documento congelato

**CA-5 — Registro completo e verde**
- **Dato** il registro aggiornato con tutte le voci dell'applicazione · **Quando** si esegue `./run-tests.sh`
- **Allora** l'intera suite, compreso il controllo di copertura, è verde

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` completo;
- [ ] il percorso è stabile su esecuzioni ripetute;
- [ ] prova di **isolamento fra account** dentro il percorso (collegamento di un altro account);
- [ ] **prova end-to-end**: **coperta ora** — onora i rimandi delle storie `0015`, `0018`-`0021`, `0023` e `0024`;
- [ ] **traduzioni**: la pagina pubblica è verificata nella lingua del destinatario;
- [ ] controllo automatico di **accessibilità** sulla pagina pubblica;
- [ ] **manifesto dei dati**: nessuna voce nuova;
- [ ] **registro delle decisioni** compilato (percorsi scelti, casi negativi coperti);
- [ ] registro di copertura [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) completo per
      l'applicazione e verde;
- [ ] avvio locale invariato.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0019` | l'accettazione è il cuore del percorso |
| storia `0025` | il percorso verifica che l'evento nasca |
| storia `0029` | condivide l'impianto e l'etichetta del percorso |

## 7. Fuori ambito

- le prove di carico sulla pagina pubblica: utili ma di un'altra natura, e nessuno le ha chieste;
- le prove del livello conversazionale: non esiste ancora un livello da percorrere.

## 8. Punti aperti

Nessuno.
