# 0027 — Cruscotto della spesa

**Applicazione**: 08 — SpendGrove (`notespese`) · **Epica**: 05 — Riconciliazione e uscita verso la contabilità
**Storia**: `0027` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0016`, `0020`, `0024`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare
> voglio vedere in una schermata quanto abbiamo speso nel periodo, in che cosa, chi ha speso e cosa richiede la mia
> attenzione
> così da accorgermi di uno scostamento a luglio invece che a gennaio dell'anno dopo.

**Contesto.** Tutte le storie precedenti producono dati; questa li restituisce in forma di risposta. È l'ultima
dell'epica perché ha bisogno che i numeri siano completi: senza il rimborso registrato non si sa cosa è pagato,
senza la qualificazione dell'imposta non si sa quanto si recupera, senza la valutazione fiscale non si sa cosa è a
rischio. Ha anche un limite dichiarato: **non è un cruscotto sulle persone**. Misura la spesa dell'azienda, non il
comportamento dei collaboratori, e la differenza non è di sfumatura (descrizione, §6, punto aperto n. 6).

## 2. Requisiti funzionali

1. **RF-1** — Il cruscotto mostra, per un periodo scelto: totale speso, totale rimborsato, totale ancora da
   liquidare, imposta recuperabile e importo a rischio di indeducibilità.
2. **RF-2** — Il dettaglio si raggruppa per **categoria**, per **collaboratore**, per **centro di costo** e per
   **trasferta**, con il confronto rispetto al periodo precedente.
3. **RF-3** — Ogni numero è **navigabile**: si arriva dall'aggregato all'elenco delle spese che lo compongono, in
   un clic.
4. **RF-4** — Il riquadro «richiede attenzione» elenca le cose da fare: ricevute da rivedere, note da approvare,
   note approvate da liquidare da troppo tempo, movimenti orfani, spese a rischio.
5. **RF-5** — Il cruscotto si esporta come tabella per il periodo mostrato, con gli stessi numeri che si vedono a
   schermo.
6. **RF-6** — Chi ha ruolo `sostiene` vede il cruscotto **delle proprie** spese; chi `approva`, quello dei suoi
   assegnati; chi `amministra`, quello dell'account. Non esiste una vista che confronti i collaboratori fra loro in
   una classifica.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni aggregazione filtra per `tenant_id` preso dal token verificato, e
  **dentro** l'account applica il filtro di visibilità per ruolo prima di aggregare: un totale calcolato su dati che
  l'utente non potrebbe vedere è una fuga di informazione anche se il dettaglio resta nascosto.
- **RT-2 — Interfaccia di programmazione (§2).** `GET /api/notespese/v1/cruscotto?periodo=&raggruppamento=` e
  `GET /api/notespese/v1/cruscotto/export`; errori in `application/problem+json`; definizione OpenAPI aggiornata.
- **RT-3 — Persistenza (§8).** Nessuna tabella nuova: si aggrega su quelle esistenti, con gli indici che servono
  (periodo, categoria, collaboratore). Se i tempi di risposta non reggono, si valuta una vista materializzata —
  ma **non prima di aver misurato**: una micro-impresa ha centinaia di righe, non milioni.
- **RT-4 — Modulo frontend (§3, §5).** Sezione *Panoramica*: gli indicatori, il riquadro «richiede attenzione», i
  raggruppamenti. Al massimo quattro indicatori in testa: se sono sette, non sono indicatori. Solo token del
  sistema di design; tema chiaro e scuro; i grafici, se ci sono, restano leggibili senza colore.
- **RT-5 — Cinque lingue (§4).** Etichette, unità e formati numerici passano dallo spazio-nomi `notespese` e sono
  presenti in `en, it, fr, es, de`.
- **RT-6 — Varchi e quota (§6, §7).** Nessun consumo. Con abbonamento `canceled` risponde `402`.
- **RT-7 — Esposizione conversazionale (§12).** La storia dichiara
  `riepilogo_spese(periodo, raggruppamento) → totali`, marcato **lettura**: è la domanda per cui il livello
  conversazionale vale davvero («quanto abbiamo speso in trasferte a luglio?»). Lo strumento restituisce **solo**
  ciò che il ruolo di chi chiede può vedere. Dipendenza: UC 0061-0063.
- **RT-8 — Dati personali (§10).** Nessun dato nuovo, ma un **uso** nuovo: aggregare le spese per collaboratore
  produce un profilo di comportamento. Va scritto nel manifesto che il raggruppamento per collaboratore esiste per
  la finalità amministrativa (attribuire i costi e rimborsare), **non** per valutare le persone, e che non esistono
  classifiche né indicatori individuali di performance. È la linea che tiene l'app fuori dal controllo a distanza
  dei lavoratori (descrizione, §6).
- **RT-9 — Registrazione eventi (§14).** L'evento `cruscotto esportato` porta `tenant_id`, `app_id`, `user_id`,
  identificativo di correlazione e periodo — nessun numero, nessun nome.

## 4. Criteri di accettazione

**CA-1 — Numeri coerenti**
- **Dato** un mese con spese confermate per 2.410 € e rimborsi registrati per 1.900 €
- **Quando** si apre il cruscotto di quel mese
- **Allora** i totali mostrano 2.410 € speso, 1.900 € rimborsato e 510 € da liquidare

**CA-2 — Navigazione dal numero alla riga**
- **Dato** il raggruppamento per categoria con 640 € su *Vitto*
- **Quando** si tocca quel valore
- **Allora** si apre l'elenco delle spese che lo compongono, filtrato, e la loro somma è 640 €

**CA-3 — Visibilità per ruolo**
- **Dato** un collaboratore con ruolo `sostiene` · **Quando** apre il cruscotto
- **Allora** vede solo i propri numeri, e il raggruppamento per collaboratore mostra soltanto sé stesso

**CA-4 — Richiede attenzione**
- **Dato** tre ricevute da rivedere, una nota da approvare e due movimenti orfani
- **Quando** si apre la panoramica
- **Allora** il riquadro elenca le tre voci con i conteggi e ciascuna porta al suo elenco

**CA-5 — Nessuna classifica**
- **Dato** il cruscotto in tutte le sue viste
- **Quando** lo si esamina
- **Allora** non esiste alcun ordinamento dei collaboratori per spesa presentato come graduatoria né alcun
  indicatore individuale di comportamento

**CA-6 — Isolamento fra account**
- **Dato** due account · **Quando** l'uno apre il cruscotto
- **Allora** nessun valore dell'altro contribuisce a nessun totale

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno backend e frontend; l'intera suite prima del commit);
- [ ] prove di **unità** sulle aggregazioni e sui confronti di periodo; di **integrazione** sulle rotte con database
      effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** **e** di visibilità per ruolo **sugli aggregati**, non solo sui dettagli;
- [ ] **prova end-to-end**: *coprire ora* il passo «apro la panoramica e i numeri tornano» nel percorso
      `[J-NOTESPESE]`; registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) aggiornato;
- [ ] **traduzioni** presenti in tutte e cinque le lingue, con i formati numerici corretti;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese con la finalità del raggruppamento per collaboratore e
      il limite dichiarato;
- [ ] **registro delle decisioni** compilato, con la scelta di non produrre classifiche individuali;
- [ ] contratto dello strumento `riepilogo_spese` dichiarato, marcato lettura e con il filtro di ruolo applicato;
- [ ] controllo automatico di **accessibilità** verde sulla panoramica;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| `0016` | Senza il rimborso registrato non si sa cosa è pagato |
| `0020` | L'importo a rischio viene dalla valutazione fiscale |
| `0024` | L'imposta recuperabile viene dalla qualificazione |

## 7. Fuori ambito

- Previsioni e budget: è BudgetGrove (catalogo 54). Qui si guarda il consuntivo.
- Indicatori di produttività o di comportamento dei collaboratori: **esclusi per scelta**, non per mancanza di
  tempo.
- Grafici elaborati: la micro-impresa ha bisogno di quattro numeri giusti, non di un cruscotto da sala di
  controllo.

## 8. Punti aperti

- **Confronto con il periodo precedente quando i periodi sono disomogenei** (un mese con una trasferta lunga e uno
  senza): il confronto rischia di suggerire conclusioni sbagliate. Serve decidere se mostrarlo sempre, e con quale
  avvertenza.
