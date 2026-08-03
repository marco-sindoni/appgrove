# 0012 — Creazione del preventivo e delle righe

**Applicazione**: 06 — QuoteGrove (`preventivi`) · **Epica**: 03 — Redazione dell'offerta
**Storia**: `0012` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0006`, `0008`, `0009`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come addetto che prepara le offerte
> voglio comporre un preventivo scegliendo il cliente e aggiungendo righe dal catalogo o scritte a mano
> così da avere in cinque minuti un documento completo invece di un foglio di calcolo da ricopiare.

**Contesto.** È la storia centrale dell'applicazione: tutto ciò che è stato costruito finora — destinatari,
catalogo, listini — serve a questo momento. La ricerca di mercato dice che la richiesta numero uno del segmento è
«fare e mandare un preventivo in pochi minuti partendo da un modello» (§2.5 della descrizione dell'applicazione):
questa storia consegna la prima metà di quella frase.

## 2. Requisiti funzionali

1. **RF-1** — Si crea un preventivo scegliendo il destinatario; il numero progressivo per account e anno è
   assegnato dall'app e non si modifica.
2. **RF-2** — Si aggiungono righe **dal catalogo** (con descrizione, unità, prezzo e aliquota precompilati dal
   listino risolto) oppure **libere** (scritte a mano, senza legame con il catalogo).
3. **RF-3** — Le righe si riordinano, si duplicano e si cancellano; l'ordine è quello che il cliente vedrà.
4. **RF-4** — Si possono inserire righe **descrittive senza prezzo** (un titolo di sezione, una nota): fanno parte
   del documento ma non del totale.
5. **RF-5** — Un preventivo si può duplicare per crearne uno nuovo: è il modo in cui il segmento riusa il lavoro
   fatto.
6. **RF-6** — Un preventivo in stato diverso da `bozza` o `in_revisione` **non si modifica**: l'app propone di
   emettere una nuova versione (storia `0015`).

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura e scrittura di `preventivo` e `riga_preventivo` filtra per
  `tenant_id` preso dal token verificato; destinatario e voci di catalogo citati devono appartenere allo stesso
  account, altrimenti la richiesta è respinta.
- **RT-2 — Interfaccia di programmazione (§2).** `POST /api/preventivi/v1/preventivi`,
  `PUT /api/preventivi/v1/preventivi/{id}/righe`, corpo validato, errori in `problem+json`, OpenAPI aggiornata.
- **RT-3 — Persistenza (§8).** Migrazione `V7__righe.sql`: tabella `riga_preventivo` con `tenant_id`, UUID
  versione 7, colonne di controllo, cancellazione logica, colonna d'ordine.
- **RT-4 — Modulo frontend (§3, §5).** Schermata di composizione nella sezione **Preventivi**: righe modificabili
  in linea, riordino, totale che si aggiorna mentre si scrive; solo token del sistema di design; tema chiaro e
  scuro; controllo automatico di accessibilità.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe visibili in `en, it, fr, es, de`.
- **RT-6 — Varchi e quota (§6, §7).** **La creazione non consuma quota**: la metrica `preventivi_inviati` si
  consuma all'invio (storia `0017`). Con abbonamento non attivo la risorsa risponde `402`.
- **RT-7 — Dati personali (§10).** Nessun campo di persona nuovo, ma le descrizioni di riga sono **testo libero**:
  l'interfaccia mostra l'avviso di non inserirvi dati sensibili.
- **RT-8 — Registrazione eventi (§14).** `preventivo creato`, `righe aggiornate`, `preventivo duplicato` con
  `tenant_id`, `app_id`, `user_id` e correlazione — senza il contenuto delle righe.

## 4. Criteri di accettazione

**CA-1 — Preventivo completo in pochi passi**
- **Dato** un destinatario e un catalogo popolati · **Quando** si crea un preventivo e si aggiungono tre righe dal
  catalogo · **Allora** il documento ha numero progressivo, righe con prezzi risolti dal listino e un totale

**CA-2 — Riga libera e riga descrittiva**
- **Dato** un preventivo in bozza · **Quando** si aggiunge una riga scritta a mano con prezzo e una riga di solo
  testo · **Allora** la prima entra nel totale, la seconda no, ed entrambe compaiono nell'ordine scelto

**CA-3 — Documento non modificabile**
- **Dato** un preventivo in stato `inviato` · **Quando** si tenta di cambiarne una riga · **Allora** `409` in
  `problem+json` con l'indicazione di emettere una nuova versione, e nulla cambia

**CA-4 — Isolamento fra account**
- **Dato** un preventivo dell'account `A` · **Quando** un utente di `B` ne chiede il dettaglio o tenta di
  modificarlo · **Allora** riceve la stessa risposta che riceverebbe per un documento inesistente

**CA-5 — Duplicazione**
- **Dato** un preventivo accettato · **Quando** lo si duplica · **Allora** nasce un documento nuovo in `bozza`,
  con numero nuovo, righe copiate e nessun legame di stato con l'originale

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh`;
- [ ] prove di **unità** sulla macchina a stati e di **integrazione** sulle risorse, con database effimero;
- [ ] prova di **isolamento fra account** su preventivo e righe;
- [ ] **prova end-to-end**: **coperta ora in parte** — è il primo passo del percorso `[J-PREVENTIVI]` creato dalla
      storia `0029`; qui si scrive la prova di interfaccia, il percorso completo arriva lì, e il registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) si aggiorna nella `0029`;
- [ ] **traduzioni** in tutte e cinque le lingue;
- [ ] **manifesto dei dati**: nessuna voce nuova, ma il rischio del testo libero è ribadito;
- [ ] **registro delle decisioni** compilato (righe libere e descrittive, immutabilità fuori dalla bozza,
      duplicazione senza legame di stato);
- [ ] avvio locale invariato.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0006` | serve un destinatario |
| storie `0008`, `0009` | le righe prendono da catalogo e listino |

## 7. Fuori ambito

- il calcolo dei totali con sconti e imposte: storia `0013`;
- i testi standard e le condizioni: storia `0014`;
- l'invio: storia `0017`.

## 8. Punti aperti

Nessuno.
