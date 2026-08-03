# 0026 — Indicatori di conversione

**Applicazione**: 06 — QuoteGrove (`preventivi`) · **Epica**: 05 — Esito, acconti e catena del documento
**Storia**: `0026` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0024`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che vuole capire se sto migliorando
> voglio vedere quanti preventivi si trasformano in lavori, quanto ci mette il cliente a rispondere e quanto vale
> ciò che è in ballo
> così da decidere se il problema è quanto offro o come offro.

**Contesto.** Le storie precedenti hanno prodotto i dati: stati, tempi, esiti, motivi. Questa storia li mette in
quattro numeri sulla Panoramica, che è la pagina d'atterraggio del modulo. Vale una regola di onestà: **si mostra
il dato del cliente, non lo si confronta con medie di settore inventate** — nessuna fonte affidabile sul tasso di
accettazione nel segmento micro è stata trovata (§2.7 della descrizione dell'applicazione).

## 2. Requisiti funzionali

1. **RF-1** — La Panoramica mostra quattro indicatori per un periodo scelto (mese in corso, trimestre, anno):
   **tasso di accettazione** (vinti su chiusi), **valore in trattativa** (somma dei preventivi inviati e non
   ancora chiusi), **tempo medio di risposta** (dall'invio alla prima risposta del cliente), **valore vinto**.
2. **RF-2** — Ogni indicatore dice **su quanti documenti** è calcolato: una percentuale su tre preventivi non è
   una percentuale, e va detto.
3. **RF-3** — I motivi della perdita sono mostrati come distribuzione, ordinata per frequenza.
4. **RF-4** — Da ogni indicatore si arriva all'elenco dei documenti che lo compongono: un numero da cui non si può
   scendere è un numero di cui non ci si fida.
5. **RF-5** — Gli indicatori si esportano in formato tabellare.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni aggregazione filtra per `tenant_id` preso dal token verificato: un
  indicatore è per definizione una lettura di molte righe, ed è il posto in cui il filtro si dimentica più
  facilmente. Prova di isolamento obbligatoria su ciascuna aggregazione.
- **RT-2 — Interfaccia di programmazione (§2).** `GET /api/preventivi/v1/indicatori?periodo=`; errori in
  `problem+json`; OpenAPI aggiornata.
- **RT-3 — Persistenza (§8).** Nessuna tabella nuova: si calcola sulle esistenti. Se le prestazioni lo
  richiederanno, si valuterà una vista materializzata — ma non prima di averne bisogno.
- **RT-4 — Modulo frontend (§3, §5).** Riga di indicatori sulla Panoramica, con selettore di periodo; da due a
  quattro numeri, non di più; solo token del sistema di design; tema chiaro e scuro; le eventuali
  rappresentazioni grafiche restano leggibili anche senza colore.
- **RT-5 — Cinque lingue (§4).** Le etichette in `en, it, fr, es, de`; percentuali e importi formattati secondo la
  lingua.
- **RT-6 — Dati personali (§10).** Nessun dato personale nuovo: gli indicatori sono aggregati. L'esportazione
  tabellare **non** deve contenere nomi di destinatari se non richiesti esplicitamente.
- **RT-7 — Registrazione eventi (§14).** `indicatori richiesti` con gli identificativi d'obbligo e il periodo.

## 4. Criteri di accettazione

**CA-1 — I quattro numeri**
- **Dato** un account con dieci preventivi chiusi, sei vinti · **Quando** apre la Panoramica sul mese
- **Allora** legge un tasso di accettazione del 60 %, con l'indicazione «su 10 preventivi chiusi»

**CA-2 — Campione piccolo dichiarato**
- **Dato** un account con due soli preventivi chiusi · **Quando** guarda il tasso · **Allora** l'app mostra il
  numero ma avverte che il campione è troppo piccolo per trarne conclusioni

**CA-3 — Si scende al dettaglio**
- **Dato** il valore in trattativa · **Quando** ci si clicca sopra · **Allora** si apre l'elenco dei preventivi
  che lo compongono, e la somma dei loro totali coincide con l'indicatore

**CA-4 — Isolamento fra account**
- **Dato** due account con volumi diversi · **Quando** ciascuno guarda i propri indicatori · **Allora** i numeri
  sono calcolati solo sui propri documenti

**CA-5 — Nessun confronto inventato**
- **Dato** la schermata degli indicatori · **Quando** la si esamina · **Allora** non compare nessuna «media di
  settore» né alcun confronto con dati che non siano del cliente

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh`;
- [ ] prove di **unità** sulle formule e di **integrazione** sull'aggregazione;
- [ ] prova di **isolamento fra account** su **ogni** aggregazione;
- [ ] **prova end-to-end**: rimando alla storia `0029`, che verifica la Panoramica dopo l'invio;
- [ ] **traduzioni** in tutte e cinque le lingue;
- [ ] **manifesto dei dati**: nessuna voce nuova;
- [ ] **registro delle decisioni** compilato (formule scelte, soglia del campione piccolo, rifiuto dei confronti
      di settore);
- [ ] avvio locale invariato.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0024` | gli esiti sono la materia prima degli indicatori |

## 7. Fuori ambito

- previsioni e proiezioni: sono del CRM (catalogo 04) e richiedono dati che qui non ci sono;
- il confronto con altri clienti di appgrove: sarebbe un uso secondario dei dati dei clienti, **vietato** dai
  principi di piattaforma.

## 8. Punti aperti

Nessuno.
