# 0012 — Salute e ritardo delle fonti

**Applicazione**: 32 — TokenGrove (`spesa_modelli`) · **Epica**: 02 — Ingresso dei dati di consumo
**Storia**: `0012` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0007`, `0009`, `0011`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come responsabile tecnico che sta per prendere una decisione guardando un numero
> voglio sapere se quel numero è fresco e completo, o se una fonte è ferma da tre giorni
> così da non tagliare un modello per un'impennata che in realtà era solo un buco nei dati.

**Contesto.** Questa app dipende da servizi che non controlliamo e da codice che non abbiamo scritto: un dato che
manca è indistinguibile da una spesa che non c'è stata. Il ritardo dichiarato dai fornitori nella documentazione è
un'indicazione, non un impegno — Anthropic dichiara «tipicamente entro 5 minuti, con ritardi occasionalmente più
lunghi» (§2.6, fonte 1) e OpenAI non lo dichiara affatto. La conclusione operativa è che il ritardo va **misurato
sul campo**, non letto sulla documentazione.

## 2. Requisiti funzionali

1. **RF-1** — Ogni fonte espone un semaforo a tre stati — in salute, in ritardo, ferma — con la regola di ciascuno
   scritta e visibile, non implicita.
2. **RF-2** — Per ogni fonte si misura e si mostra il **ritardo osservato**: la differenza tipica fra l'istante di
   una chiamata e l'istante in cui il suo dato è arrivato a noi, calcolata sugli ultimi giorni.
3. **RF-3** — La scheda di una fonte mostra: ultima importazione riuscita, ultimo errore con la sua causa in
   parole comprensibili, misure ricevute e respinte nell'ultima ora, duplicati riconosciuti, scarto di
   riconciliazione corrente.
4. **RF-4** — Ogni schermata che mostra un totale porta l'indicazione «aggiornato a…» e, se una fonte è ferma o in
   ritardo, un'avvertenza che dice **quali** dati potrebbero mancare.
5. **RF-5** — Una fonte ferma da più di un periodo dichiarato genera un avviso all'account: non si aspetta che sia
   il cliente ad accorgersene.
6. **RF-6** — Un buco nei dati (un intervallo senza alcuna misura dove prima ce n'erano) è riconosciuto e mostrato
   come tale, distinto da un periodo di consumo effettivamente nullo.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Lo stato di salute è per fonte e quindi per `tenant_id` preso dal gettone
  verificato; nessuna aggregazione fra account nell'app (l'aggregazione di piattaforma vive nella console di
  amministrazione, su metadati).
- **RT-2 — Interfaccia di programmazione (§2).** Rotta `GET /api/spesa_modelli/v1/fonti/{id}/salute` e riepilogo
  in `GET /api/spesa_modelli/v1/fonti`; errori in `problem+json`; definizione OpenAPI aggiornata nello stesso
  commit.
- **RT-3 — Persistenza (§8).** Il ritardo osservato si conserva come statistica per fonte e giorno, non si
  ricalcola su tutta la storia a ogni lettura.
- **RT-4 — Modulo frontend (§3, §5).** Semaforo nella sezione «Fonti» e riga «aggiornato a…» in testa a ogni
  schermata di spesa; solo token del sistema di design; funziona in tema chiaro e scuro. Il colore rosso del
  semaforo è quello funzionale del sistema di design, non il colore-categoria dell'app.
- **RT-5 — Cinque lingue (§4).** Le cause degli errori sono tradotte in `en, it, fr, es, de`: un messaggio del
  fornitore in inglese va mappato su un testo nostro comprensibile, non mostrato così com'è.
- **RT-6 — Esposizione conversazionale (§12).** Lo strumento `stato_fonti() → elenco delle fonti con stato,
  ritardo osservato e scarto` è **completato** qui, marcato lettura. È lo strumento che un assistente deve
  chiamare **prima** di rispondere a una domanda sulla spesa, per non rispondere con numeri incompleti: il
  contratto lo dichiara esplicitamente nella descrizione.
- **RT-7 — Dati personali (§10).** Nessun dato personale nuovo. Le cause di errore mostrate non riportano mai il
  corpo delle risposte del fornitore, che potrebbe contenere qualunque cosa.
- **RT-8 — Registrazione eventi (§14).** Eventi «fonte passata in ritardo», «fonte ferma», «buco rilevato» con
  `tenant_id`, `app_id` e identificativo di correlazione.

## 4. Criteri di accettazione

**CA-1 — Semaforo e ritardo osservato**
- **Dato** una fonte che riceve dati con un ritardo tipico di sei minuti
- **Quando** si apre la sua scheda
- **Allora** il semaforo è in salute e il ritardo osservato mostra circa sei minuti, calcolato sui dati veri e non
  su un valore scritto nel codice

**CA-2 — Fonte ferma**
- **Dato** una fonte la cui ultima importazione riuscita risale a tre giorni fa
- **Quando** si apre la panoramica
- **Allora** il semaforo è rosso, compare l'avvertenza che dice quali dati potrebbero mancare, e l'account ha
  ricevuto l'avviso senza averlo chiesto

**CA-3 — Buco distinto dal consumo nullo**
- **Dato** un account che ha consumato tutti i giorni tranne martedì, in cui la fonte era ferma
- **Quando** guarda il grafico della settimana
- **Allora** martedì risulta «dato mancante» e non «zero euro speso»

**CA-4 — Causa comprensibile**
- **Dato** una fonte che riceve dal fornitore un rifiuto per credenziale scaduta
- **Quando** si apre la sua scheda in una qualunque delle cinque lingue
- **Allora** la causa è espressa in quella lingua e dice cosa fare, senza riportare il messaggio grezzo del
  fornitore

**CA-5 — Isolamento fra account**
- **Dato** due account con fonti sullo stesso fornitore, una in salute e una ferma
- **Quando** ciascuno guarda le proprie fonti
- **Allora** ognuno vede solo lo stato delle proprie

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (backend e frontend; l'intera suite prima del commit);
- [ ] prove di **unità** sul calcolo del ritardo osservato e sul riconoscimento del buco, e di **integrazione**
      sulla rotta della salute;
- [ ] prova di **isolamento fra account** sullo stato delle fonti;
- [ ] **prova end-to-end**: **coprire ora**, estendendo `[J-SPESA-MODELLI]` con il passo «fonte ferma: la
      panoramica avverte», e aggiornare il registro di copertura
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml);
- [ ] **traduzioni** presenti in tutte e cinque le lingue, cause di errore comprese;
- [ ] **manifesto dei dati**: nessuna modifica;
- [ ] **registro delle decisioni** compilato, in particolare sulla scelta di misurare il ritardo invece di
      dichiararlo;
- [ ] contratto degli **strumenti conversazionali** aggiornato per `stato_fonti`;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storie `0007` e `0009` | Servono le due origini di cui misurare la salute |
| Storia `0011` | Lo scarto di riconciliazione è una delle informazioni della scheda |

## 7. Fuori ambito

- la vista di piattaforma sullo stato delle fonti di **tutti** gli account: è nella console di amministrazione
  ([estensioni-admin.md](../estensioni-admin.md) §4);
- il ripristino automatico di una fonte con credenziale scaduta: non esiste, perché richiede una credenziale nuova
  che solo il cliente può dare.

## 8. Punti aperti

Nessuno.
