# 0024 — Il «non lo so»

**Applicazione**: 20 — InsightGrove (`insights`) · **Epica**: 05 — Copilota sui dati
**Storia**: `0024` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0022`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che fa una domanda a cui l'app non può rispondere
> voglio sentirmi dire «non lo so, e questo è il motivo»
> così da poter cercare la risposta altrove, invece di portarmi via un numero inventato.

**Contesto.** È la storia che rende onesto il copilota, ed è quella che più facilmente non viene scritta perché
non «aggiunge funzioni». Il materiale del 2026 è concorde nel dire che il pericolo degli assistenti analitici non
è sbagliare, è **sbagliare in modo plausibile** (§2.5 della [descrizione](../application-description.md)). Un
sistema che risponde sempre è un sistema che, quando non sa, inventa. Questa storia elenca i casi in cui non si
risponde e stabilisce **come** non si risponde: mai con un ripiego, sempre con il motivo e con ciò che invece si
può chiedere.

## 2. Requisiti funzionali

1. **RF-1** — Il copilota rifiuta e dice perché in questi casi, ciascuno con un messaggio proprio:
   - la domanda non si traduce in alcuna metrica del catalogo;
   - la metrica esiste ma richiede una fonte non collegata;
   - la dimensione o il filtro chiesti non sono ammessi dalla metrica;
   - il periodo non è rappresentabile o non esistono fatti in quel periodo;
   - il ruolo di chi chiede non consente di vedere la metrica (storia 0025);
   - la domanda non riguarda i dati dell'account (per esempio chiede un'opinione, o un fatto del mondo).
2. **RF-2** — Ogni rifiuto propone **che cosa si può chiedere invece**: le due o tre metriche più vicine del
   catalogo dell'account, oppure l'azione che sbloccherebbe la risposta (collegare una fonte, chiedere a chi
   amministra).
3. **RF-3** — Un rifiuto **non ripiega mai** su una metrica diversa da quella chiesta senza dirlo: se l'utente
   chiede il margine e c'è solo il fatturato, non riceve il fatturato spacciato per risposta.
4. **RF-4** — Il copilota **non interpreta e non consiglia**: non dice «dovresti alzare i prezzi». Riporta
   numeri, li scompone e dichiara che cosa non sa. È una scelta di prodotto, non una limitazione tecnica.
5. **RF-5** — Un rifiuto **non consuma quota** se il piano non è stato eseguito e il modello non è stato
   interpellato per formulare la risposta; consuma quota se il modello è stato interpellato (storia 0026).
   In entrambi i casi l'utente sa se ha consumato.
6. **RF-6** — I rifiuti sono contati per motivo: sapere che il 40 % dei rifiuti riguarda una fonte non collegata
   è un'informazione di prodotto, e resta **aggregata per account**, mai fra account.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** I suggerimenti alternativi vengono dal catalogo dell'account
  chiamante, letto con `tenant_id` dal gettone verificato.
- **RT-2 — Interfaccia di programmazione (§2).** Il rifiuto è una risposta legittima della rotta
  `POST /api/insights/v1/domande` — `200` con esito «non risposto» e motivo strutturato — **non** un errore
  tecnico. Un `500` al posto di un «non lo so» è il difetto che questa storia esiste per evitare. Definizione
  OpenAPI aggiornata nello stesso commit.
- **RT-4 — Modulo frontend (§3, §5).** Il rifiuto si presenta come un messaggio con il motivo e le alternative
  cliccabili; solo token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Tutti i messaggi di rifiuto esistono in `en, it, fr, es, de`.
- **RT-6 — Varchi e quota (§6, §7).** Il rifiuto per ruolo insufficiente è il quarto varco (`403` nella
  risorsa, messaggio esplicito nella conversazione); il rifiuto per quota esaurita è il quinto (`429`).
- **RT-8 — Dati personali (§10).** Nessuna voce nuova: il motivo del rifiuto è un codice, non il testo della
  domanda.
- **RT-14 — Registrazione eventi (§14).** «Domanda non risposta» con `tenant_id`, `app_id`, `user_id`, il
  **codice** del motivo e l'identificativo di correlazione; **mai** il testo della domanda.

## 4. Criteri di accettazione

**CA-1 — Fuori catalogo**
- **Dato** un catalogo senza alcuna metrica sul costo del personale
- **Quando** l'utente chiede «quanto mi costa il personale»
- **Allora** riceve «non lo so: nel tuo catalogo non c'è un indicatore sul costo del personale», con l'elenco
  delle metriche economiche disponibili

**CA-2 — Fonte non collegata**
- **Dato** la metrica «valore di magazzino» e la fonte magazzino non collegata
- **Quando** l'utente chiede «quanto vale il mio magazzino»
- **Allora** riceve «l'indicatore esiste ma richiede la fonte magazzino, che non è collegata», con il pulsante
  che porta alla sezione Fonti

**CA-3 — Nessun ripiego**
- **Dato** una domanda sul margine, e un catalogo che ha solo il fatturato
- **Quando** viene elaborata
- **Allora** la risposta **non** contiene il fatturato come se fosse la risposta: contiene il rifiuto e, come
  suggerimento esplicito, «posso dirti il fatturato»

**CA-4 — Domanda fuori dominio**
- **Dato** la domanda «conviene aprire una seconda sede?»
- **Quando** viene elaborata
- **Allora** il copilota risponde che non dà consigli, e propone gli indicatori che aiuterebbero a decidere

**CA-5 — Nessun errore tecnico**
- **Dato** una qualunque delle sei condizioni di rifiuto
- **Quando** la richiesta viene elaborata
- **Allora** la risorsa risponde `200` con esito «non risposto» e motivo strutturato; nessun `500` compare nei
  registri

**CA-6 — Isolamento fra account**
- **Dato** due account con cataloghi diversi
- **Quando** entrambi ricevono un rifiuto per metrica inesistente
- **Allora** i suggerimenti alternativi vengono ciascuno dal proprio catalogo

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno le aree toccate; l'intera suite prima del commit);
- [ ] prove di **unità** con un caso per **ciascuno** dei sei motivi di rifiuto, con modello simulato;
- [ ] prova di **isolamento fra account** sui suggerimenti alternativi;
- [ ] **prova end-to-end**: *coprire ora* — il percorso `[J-INSIGHTS]` include «chiedi una cosa che non c'è e
      ricevi un non lo so»; registro di copertura aggiornato;
- [ ] **traduzioni** presenti in tutte e cinque le lingue per tutti i messaggi di rifiuto;
- [ ] **manifesto dei dati**: nessuna voce nuova;
- [ ] **registro delle decisioni** compilato, con l'elenco dei sei motivi, la regola del non ripiego e la scelta
      di non dare consigli;
- [ ] contratto degli **strumenti conversazionali**: `interroga_metrica` restituisce un rifiuto motivato con gli
      stessi codici (storia 0031);
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0022` | il rifiuto nasce dalla validazione del piano |
| storia `0012` | i suggerimenti alternativi vengono dal catalogo |

## 7. Fuori ambito

- il rifiuto specifico per riservatezza, che ha una regola propria: storia 0025;
- la misura di quanto spesso il copilota rifiuta rispetto ai concorrenti: non ho dati e non li invento.

## 8. Punti aperti

- **Il conteggio dei rifiuti per motivo si può guardare da fuori dell'account?** Sarebbe utilissimo per capire
  che cosa manca al catalogo, e sarebbe **uso secondario dei dati dei clienti**, vietato
  ([PRINCIPI-APPGROVE.md](../../_kit/PRINCIPI-APPGROVE.md) §10). Il conteggio resta per account. Se servisse una
  statistica di prodotto, va progettata come dato aggregato non riconducibile e decisa dallo sviluppatore.
  Chiude: **sviluppatore**.
