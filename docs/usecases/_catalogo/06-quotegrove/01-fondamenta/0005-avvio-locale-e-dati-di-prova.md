# 0005 — Avvio locale e dati di prova

**Applicazione**: 06 — QuoteGrove (`preventivi`) · **Epica**: 01 — Fondamenta
**Storia**: `0005` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0001`, `0002`, `0003`, `0004`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come sviluppatore che riprende il lavoro su QuoteGrove
> voglio avviare l'app in locale e trovarci dentro dati di prova credibili in un minuto
> così da poter provare una funzione senza prima passare mezz'ora a inventare preventivi a mano.

**Contesto.** Le quattro storie precedenti hanno costruito un'app corretta e vuota. Un'app vuota non si prova: chi
la apre non vede né la forma di un elenco, né una barra di quota che si muove, né un caso limite. Questa storia
chiude le fondamenta con un insieme di dati inventati, caricabile e ricaricabile a comando, che serve a chi
sviluppa, alle dimostrazioni e — con lo stesso vocabolario — alle prove automatiche delle storie successive.

## 2. Requisiti funzionali

1. **RF-1** — Esiste un comando che carica su un account locale un insieme di dati di prova: due account distinti,
   qualche destinatario, un catalogo minimo, un listino e una decina di preventivi negli stati principali.
2. **RF-2** — I dati sono **inventati e riconoscibili come tali**: nomi di fantasia, indirizzi di posta sul
   dominio `*.test`, importi tondi. Nessun dato reale, nemmeno «preso da un cliente vero e modificato».
3. **RF-3** — Il caricamento è ripetibile: eseguirlo due volte non duplica e non lascia lo schema a metà.
4. **RF-4** — L'insieme comprende almeno un caso limite utile: un preventivo scaduto, uno con quota già esaurita
   per il suo account, uno con sconto oltre la soglia di approvazione.
5. **RF-5** — La documentazione di sviluppo dice in tre righe come si carica e come si azzera.

## 3. Requisiti tecnici

- **RT-1 — Avvio locale (§15).** `./dev.sh services` mostra l'app con la sua porta e il suo schema;
  `./app-start.sh` la avvia senza modifiche manuali agli script; `dev migrate` applica le migrazioni.
- **RT-2 — Isolamento fra account (§1).** L'insieme crea **due** account proprio per rendere dimostrabile
  l'isolamento a occhio, oltre che nelle prove.
- **RT-3 — Dati personali (§10).** I dati di prova contengono nomi e recapiti **inventati**: non sono dati
  personali di persone reali e non entrano nel manifesto. Il divieto di usare dati veri è parte della storia.
- **RT-4 — Prove (§11).** I dati di prova sono deterministici e usati dalle prove end-to-end delle storie `0029`
  e `0030`: gli identificativi non cambiano fra due caricamenti.
- **RT-5 — Registrazione eventi (§14).** Il caricamento registra quante entità ha creato, senza contenuti.

## 4. Criteri di accettazione

**CA-1 — Un minuto e l'app è viva**
- **Dato** uno stack locale appena avviato · **Quando** si esegue il comando di caricamento · **Allora** l'elenco
  dei preventivi mostra documenti negli stati `bozza`, `inviato`, `visto`, `accettato`, `rifiutato` e `scaduto`

**CA-2 — Ripetibile**
- **Dato** i dati già caricati · **Quando** si esegue di nuovo il comando · **Allora** il risultato è identico e
  non ci sono duplicati

**CA-3 — Due account separati**
- **Dato** i dati caricati · **Quando** si entra con l'utente del secondo account · **Allora** si vedono solo i
  suoi preventivi, con numerazione che riparte da 1

**CA-4 — Niente dati veri**
- **Dato** l'insieme dei dati di prova · **Quando** lo si ispeziona · **Allora** tutti gli indirizzi di posta sono
  su dominio `*.test` e nessun nome corrisponde a una persona o a un'azienda reale

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh`;
- [ ] prova di **integrazione** che carica l'insieme su database effimero e verifica i conteggi;
- [ ] prova di **isolamento fra account** con i due account dell'insieme;
- [ ] **prova end-to-end**: nessun impatto diretto, ma l'insieme è il presupposto delle storie `0029` e `0030`;
- [ ] **traduzioni**: non applicabile;
- [ ] **manifesto dei dati**: nessuna voce nuova (dati inventati);
- [ ] **registro delle decisioni** compilato (composizione dell'insieme e casi limite scelti);
- [ ] `./dev.sh services` e l'avvio locale funzionano senza passi manuali;
- [ ] documentazione di sviluppo aggiornata.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storie `0001`-`0004` | l'insieme popola ciò che quelle storie hanno creato |

## 7. Fuori ambito

- i dati di prova delle entità che ancora non esistono (versioni, prove di accettazione): ogni epica successiva
  estende l'insieme con le proprie.

## 8. Punti aperti

Nessuno.
