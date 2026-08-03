# 0022 — Documento in valuta estera

**Applicazione**: 02 — BillGrove (`billing`) · **Epica**: 05 — Conformità e apertura verso l'esterno
**Storia**: `0022` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0012`, `0013`, `0021`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come consulente che ha due clienti in Svizzera e uno nel Regno Unito
> voglio emettere la fattura nella loro valuta e vedere comunque i miei totali nella mia
> così da farmi pagare senza costringerli a conversioni, e da continuare a sapere quanto ho fatturato davvero.

**Contesto.** La scheda di catalogo elenca il multi-valuta fra i casi d'uso principali. È una funzione piccola ma
insidiosa: il cambio va **congelato** sul documento al momento dell'emissione, altrimenti i riepiloghi cambiano
retroattivamente ogni volta che il cambio si muove — e un documento emesso non deve cambiare mai (storia `0012`).
Va dopo il report perché il report deve saper sommare documenti in valute diverse.

## 2. Requisiti funzionali

1. **RF-1** — L'account dichiara la propria **valuta di conto**; ogni documento può essere emesso in una valuta
   diversa.
2. **RF-2** — Il documento porta valuta e **tasso di cambio**, congelati al momento dell'emissione insieme alla
   data a cui il tasso si riferisce.
3. **RF-3** — Righe, riepiloghi e totali sono espressi nella valuta del documento; accanto compare il
   controvalore nella valuta di conto.
4. **RF-4** — Il tasso si può inserire a mano; se non viene inserito, l'emissione viene rifiutata con la
   spiegazione (nessun tasso «di default» inventato dal sistema).
5. **RF-5** — Gli incassi si registrano nella valuta del documento; il report riporta tutto alla valuta di conto
   usando il cambio congelato del documento.
6. **RF-6** — Un documento in valuta estera è riconoscibile a colpo d'occhio negli elenchi.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** La valuta di conto è per `tenant_id` preso dal token verificato; nessun
  parametro di valuta arriva dal corpo della richiesta per conto di un altro account.
- **RT-2 — Interfaccia di programmazione (§2).** I campi `currency` e `exchangeRate` entrano negli oggetti di
  trasferimento del documento; errori in `application/problem+json` (`409` se manca il tasso all'emissione);
  definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione sullo schema `app_billing`: colonne di valuta, tasso e data del tasso su
  `document` e su `payment`, con le colonne di controllo consuete. Gli importi si conservano come valori decimali
  esatti, mai come numeri a virgola mobile.
- **RT-4 — Modulo frontend (§3, §5).** Scelta della valuta sul documento, campo del tasso con la data, controvalore
  mostrato accanto al totale. Solo token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe visibili passano dallo spazio-nomi `billing` e sono presenti in
  `en, it, fr, es, de`; i simboli di valuta e la formattazione seguono la lingua scelta.
- **RT-6 — Varchi e quota (§6).** Nessun consumo di quota aggiuntivo: un documento in valuta estera è un documento
  come gli altri.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento proprio; valuta e controvalore compaiono nei
  risultati di `leggi_documento` e `riepilogo_incassi` (epica 06), che devono dichiarare **in quale valuta** sono
  i numeri che restituiscono: un importo senza valuta, in una chat, è un errore che si propaga.
- **RT-8 — Dati personali (§10).** Nessun dato personale nuovo.
- **RT-9 — Registrazione eventi (§14).** L'evento `documento emesso in valuta estera` è registrato con `tenant_id`,
  `app_id`, `user_id`, identificativo di correlazione e codice della valuta.

## 4. Criteri di accettazione

**CA-1 — Emissione in valuta estera**
- **Dato** un account con valuta di conto euro e un cliente svizzero
- **Quando** si emette una fattura in franchi con tasso indicato
- **Allora** il documento riporta i totali in franchi, il controvalore in euro e il tasso congelato con la sua data

**CA-2 — Tasso mancante**
- **Dato** una bozza in valuta estera senza tasso · **Quando** si tenta di emetterla
- **Allora** la risposta è `409` con la spiegazione, e nulla viene emesso

**CA-3 — Il cambio non cambia dopo**
- **Dato** una fattura emessa con tasso 0,95 e un cambio odierno diverso
- **Quando** si riapre il documento · **Allora** i valori sono quelli di allora

**CA-4 — Report in valuta di conto**
- **Dato** un mese con una fattura in euro e una in franchi
- **Quando** si apre il riepilogo
- **Allora** il fatturato è espresso nella valuta di conto usando il cambio congelato di ciascun documento

**CA-5 — Isolamento fra account**
- **Dato** due account con valute di conto diverse
- **Quando** ciascuno apre il proprio riepilogo
- **Allora** ognuno vede i propri importi nella propria valuta di conto

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend` e `frontend`; l'intera suite prima del commit);
- [ ] prove di **unità** sulla conversione e sull'arrotondamento, di **integrazione** sull'emissione in valuta e sul
      report misto, con database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** sulla valuta di conto;
- [ ] **prova end-to-end**: *rimando* — il percorso `[J-BILLING]` lavora nella valuta di conto; il caso in valuta
      estera è coperto dalle prove di integrazione. Motivo: tenere il percorso corto. Proprietaria: storia `0031`;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati**: nessuna voce nuova, dichiarato;
- [ ] **registro delle decisioni** compilato, con annotata la scelta di congelare il tasso e di non inventarne uno;
- [ ] contratto degli **strumenti conversazionali**: nessuno proprio, ma i risultati dichiarano la valuta;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0012` | Il tasso si congela all'emissione |
| storia `0013` | I riepiloghi per aliquota vanno espressi in entrambe le valute |
| storia `0021` | Il report deve saper sommare valute diverse |

## 7. Fuori ambito

- il recupero automatico dei tassi da una fonte esterna: rimandato, perché introdurrebbe una dipendenza esterna e
  la questione di **quale** fonte sia quella valida per il fisco. Il tasso si inserisce a mano;
- la contabilizzazione delle differenze di cambio all'incasso: è materia contabile, non di BillGrove;
- i listini in valuta diversa: la valuta del listino (storia `0008`) deve coincidere con quella del documento,
  altrimenti l'emissione è rifiutata.

## 8. Punti aperti

Quale fonte del tasso di cambio sia quella accettabile per il fisco è una domanda normativa che non ho potuto
chiudere con le ricerche fatte (§2.7 della descrizione). Finché resta aperta, l'inserimento a mano è la scelta
prudente: l'utente sa quale tasso deve usare, il sistema non lo indovina.
