# 0011 — Imposte e valuta

**Applicazione**: 06 — QuoteGrove (`preventivi`) · **Epica**: 02 — Anagrafica, catalogo e listini
**Storia**: `0011` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0008`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come addetto che prepara offerte anche a clienti esteri
> voglio dichiarare l'aliquota giusta su ogni riga, le esenzioni con la loro motivazione e la valuta del documento
> così da non dover correggere il preventivo quando diventa fattura, e da non promettere un totale sbagliato.

**Contesto.** Il preventivo non è un documento fiscale, ma il numero che il cliente legge in fondo è quello su cui
deciderà: se l'imposta è sbagliata, il totale è sbagliato. Serve inoltre che il documento porti già le
informazioni che la fatturazione (catalogo 02) si aspetterà di trovare nell'evento della storia `0025`: aliquota
per riga e motivazione dell'esenzione sono esattamente quelle.

## 2. Requisiti funzionali

1. **RF-1** — Ogni riga porta una **aliquota** presa dalla voce di catalogo e sovrascrivibile a mano.
2. **RF-2** — L'account gestisce il proprio elenco di aliquote, con descrizione e percentuale, e ne indica una
   predefinita.
3. **RF-3** — Una riga può essere **esente**, e in quel caso la motivazione dell'esenzione è **obbligatoria**:
   un'esenzione senza motivo è un errore, non una scelta.
4. **RF-4** — Il documento ha **una sola valuta**, dichiarata alla creazione e non modificabile dopo l'invio.
5. **RF-5** — Il riepilogo mostra l'imponibile e l'imposta **suddivisi per aliquota**, non solo il totale.
6. **RF-6** — L'app **non converte** fra valute e lo dice: se il listino è in una valuta e il documento in
   un'altra, avvisa invece di applicare un cambio inventato.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** L'elenco delle aliquote è per account, letto dal token verificato.
- **RT-2 — Interfaccia di programmazione (§2).** `/api/preventivi/v1/impostazioni/aliquote`; il campo valuta è
  parte del preventivo; errori in `problem+json`; OpenAPI aggiornata.
- **RT-3 — Persistenza (§8).** Migrazione `V6__imposte_valuta.sql`: tabella `aliquota` e colonne su riga e
  documento; importi memorizzati in **unità minime intere** con la valuta accanto, mai in numeri a virgola mobile.
- **RT-4 — Modulo frontend (§3, §5).** Riepilogo per aliquota nella schermata del preventivo; formattazione degli
  importi secondo la lingua dell'interfaccia; solo token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe visibili in `en, it, fr, es, de`; la formattazione dei numeri
  segue la lingua, il **simbolo** segue la valuta del documento.
- **RT-6 — Dati personali (§10).** Nessun dato personale nuovo.
- **RT-7 — Registrazione eventi (§14).** `aliquota modificata`, `esenzione applicata` con gli identificativi
  d'obbligo, senza dati personali.

## 4. Criteri di accettazione

**CA-1 — Riepilogo per aliquota**
- **Dato** un preventivo con due righe al 22 % e una al 10 % · **Quando** si apre il riepilogo · **Allora** si
  leggono due imponibili distinti con le rispettive imposte, e il totale è la loro somma

**CA-2 — Esenzione senza motivo**
- **Dato** una riga marcata esente e nessuna motivazione · **Quando** si salva · **Allora** `400` in
  `problem+json` che indica il campo mancante, e nulla viene salvato

**CA-3 — Valuta immutabile dopo l'invio**
- **Dato** un preventivo già inviato in euro · **Quando** si tenta di cambiarne la valuta · **Allora** l'app
  rifiuta e propone invece di emettere una nuova versione

**CA-4 — Nessuna conversione inventata**
- **Dato** un listino in franchi svizzeri e un documento in euro · **Quando** si aggiunge una voce · **Allora**
  l'app avvisa che le valute non coincidono e chiede il prezzo, invece di convertirlo da sé

**CA-5 — Arrotondamenti**
- **Dato** tre righe con importi che generano decimali · **Quando** si calcola il totale · **Allora** la somma dei
  riepiloghi per aliquota è **esattamente** il totale mostrato: nessuna differenza di un centesimo

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh`;
- [ ] prove di **unità** sugli arrotondamenti e sulle esenzioni, di **integrazione** sulla risorsa;
- [ ] prova di **isolamento fra account** sull'elenco delle aliquote;
- [ ] **prova end-to-end**: rimando alla storia `0029`;
- [ ] **traduzioni** in tutte e cinque le lingue;
- [ ] **manifesto dei dati**: nessuna voce nuova;
- [ ] **registro delle decisioni** compilato (rappresentazione degli importi, regola di arrotondamento, rifiuto
      esplicito della conversione di valuta);
- [ ] avvio locale invariato.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0008` | l'aliquota predefinita sta sulla voce di catalogo |

## 7. Fuori ambito

- le regole fiscali per giurisdizione (inversione contabile, operazioni fuori campo): sono di competenza della
  fatturazione (catalogo 02), che è il posto in cui sbagliarle costa;
- la conversione automatica fra valute: esclusa per scelta, vedi RF-6.

## 8. Punti aperti

Nessuno.
