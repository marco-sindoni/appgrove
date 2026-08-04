# 0029 — Confronto del costo fra modelli

**Applicazione**: 32 — TokenGrove (`spesa_modelli`) · **Epica**: 06 — Cruscotti, confronti e rapporti
**Storia**: `0029` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0013`, `0016`, `0028`
**Ultimo aggiornamento**: 2026-08-04

## 1. Narrazione

> Come responsabile tecnico che ha scelto un modello sei mesi fa e non ha più rivisto la scelta
> voglio sapere quanto sarebbe costato lo stesso lavoro, già fatto, su un modello diverso
> così da decidere se cambiare con un numero in mano invece che con una sensazione.

**Contesto.** Il listino dei fornitori cambia in continuazione e i modelli si moltiplicano: un modello che sei mesi
fa era la scelta ragionevole oggi può costare il triplo di uno equivalente. La domanda che il cliente si fa è
sempre la stessa — «se avessi usato quell'altro, quanto avrei speso?» — e oggi per rispondere apre un foglio di
calcolo e moltiplica a mano i conteggi per il prezzo di listino, sbagliando quasi sempre la parte servita da cache
e la scrittura in cache, che sono voci con prezzi diversi. Qui il conto lo fa l'app, con i conteggi **veri** già
misurati e il catalogo dei prezzi **datato** (storia `0013`), compresi gli eventuali prezzi negoziati dell'account
(storia `0016`).

**Il limite da dire per primo.** Questo è un confronto **di costo**, non **di qualità**: nessuno può sapere se il
modello alternativo avrebbe risposto bene, quanti segni di testo avrebbe prodotto in uscita, quante volte si sarebbe
dovuto riprovare. La schermata lo dichiara sopra al risultato, non in una nota in fondo. La valutazione della
qualità delle risposte è dichiarata fuori ambito dall'app (§1 del documento capofila) e resta il mestiere di altri
prodotti.

## 2. Requisiti funzionali

1. **RF-1** — Scelto un periodo e un insieme di misure (tutte, oppure filtrate per modello, etichetta o fonte), si
   sceglie uno o più **modelli alternativi** dal catalogo dei prezzi e si ottiene il costo che quelle stesse misure
   avrebbero avuto con i loro conteggi, ai prezzi validi nel periodo.
2. **RF-2** — Il risultato è una tavola: modello, costo effettivo o simulato, differenza in euro e in percentuale
   rispetto a quello usato davvero, e il dettaglio per voce di prezzo (ingresso, uscita, ingresso servito da cache,
   scrittura in cache).
3. **RF-3** — Il confronto usa i **prezzi validi nel periodo simulato**, non quelli di oggi: simulare marzo con i
   prezzi di agosto darebbe una risposta a una domanda che nessuno ha fatto. La versione di catalogo usata è
   dichiarata nel risultato.
4. **RF-4** — Se un modello alternativo non ha prezzo per una parte del periodo, il confronto **non estrapola**: lo
   dichiara, mostra il risultato sulla parte coperta e dice quale parte manca (coerente con la storia `0015`).
5. **RF-5** — Sopra il risultato compare l'avvertenza che il confronto riguarda solo il costo, con l'elenco esplicito
   di ciò che non tiene conto: qualità, lunghezza diversa delle risposte, ritentativi, finestra di contesto
   differente.
6. **RF-6** — È possibile anche il confronto **per unità**: costo medio per chiamata e per mille segni di testo, che
   è il numero che si porta in una riunione quando i volumi dei due periodi non sono uguali.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** La simulazione legge le misure del solo `tenant_id` del gettone
  verificato; i prezzi negoziati usati sono quelli dell'account, mai quelli di un altro.
- **RT-2 — Interfaccia di programmazione (§2).** Rotta `POST /api/spesa_modelli/v1/confronto-modelli` (corpo:
  periodo, filtro, modelli alternativi, per unità sì/no) — è una lettura con parametri complessi, non una
  modifica: **non scrive nulla** ed è idempotente. Errori in `problem+json` che distinguono «modello sconosciuto»
  da «prezzo mancante nel periodo»; definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Nessuna scrittura sulle misure (§ documento capofila, §4).** La simulazione **non tocca** il costo
  congelato delle righe e non produce righe nuove: è un calcolo effimero. Rifare i conti sul serio è un'altra cosa
  e ha la sua storia (`0017`), che è esplicita, tracciata e produce righe nuove.
- **RT-4 — Prestazioni.** Il calcolo si serve dalla sintesi giornaliera per modello (storia `0028`) quando il
  filtro lo consente, e scende sulle misure solo quando il filtro è per etichetta. Un periodo lungo con molte
  misure produce una risposta in tempo utile o dichiara di essere troncato: mai un'attesa senza spiegazione.
- **RT-5 — Modulo frontend (§3, §5).** Sezione «Confronto» del modulo `spesa_modelli`: scelta del periodo e del
  filtro, scelta dei modelli alternativi, tavola del risultato con la differenza in evidenza. Solo token del
  sistema di design; tema chiaro e scuro; controllo automatico di accessibilità.
- **RT-6 — Cinque lingue (§4).** Tutte le stringhe in `en, it, fr, es, de`, compresa l'avvertenza di RF-5, che è il
  testo che non va accorciato in traduzione.
- **RT-7 — Varchi e quota (§6, §7).** Il confronto **non consuma** la metrica `misure_registrate`. Ha un limite di
  frequenza proprio, perché è la lettura più costosa dell'app.
- **RT-8 — Esposizione conversazionale (§12).** Strumento `confronta_costo_modelli(periodo, modelli[], per_unita?)
  → tavola comparativa`, marcato **lettura** (storia `0032`): è la funzione dell'app che in chat funziona meglio
  che in una schermata, perché la domanda arriva già formulata a parole.
- **RT-9 — Dati personali (§10).** Nessun dato personale nuovo. Se il filtro è per etichetta, il risultato non
  ripete l'etichetta nei registri applicativi.
- **RT-10 — Registrazione eventi (§14).** Evento «confronto eseguito» con `tenant_id`, `app_id`, `user_id`,
  periodo, numero di modelli confrontati e identificativo di correlazione, **senza** importi né etichette.

## 4. Criteri di accettazione

**CA-1 — Confronto su un periodo con prezzi coperti**
- **Dato** un mese di misure su un modello, e un modello alternativo con prezzo valido per tutto il mese
- **Quando** si chiede il confronto
- **Allora** la tavola mostra costo effettivo, costo simulato, differenza in euro e in percentuale, e il dettaglio
  per voce di prezzo

**CA-2 — I prezzi sono quelli del periodo**
- **Dato** un catalogo prezzi pubblicato dopo la fine del periodo simulato, con prezzi diversi
- **Quando** si esegue il confronto su quel periodo
- **Allora** il risultato usa i prezzi validi nel periodo e dichiara la versione di catalogo impiegata

**CA-3 — Prezzo mancante: si dichiara, non si estrapola**
- **Dato** un modello alternativo il cui prezzo comincia a metà del periodo scelto
- **Quando** si esegue il confronto
- **Allora** il risultato copre solo la parte con prezzo, dichiara quale parte manca, e non presenta un totale come
  se fosse completo

**CA-4 — L'avvertenza c'è ed è sopra il risultato**
- **Dato** la schermata del confronto in una qualunque delle cinque lingue
- **Quando** si guarda il risultato
- **Allora** l'avvertenza «questo è un confronto di costo, non di qualità» con il suo elenco precede la tavola

**CA-5 — La simulazione non cambia nulla**
- **Dato** un confronto eseguito dieci volte sullo stesso periodo
- **Quando** si rileggono le misure e i totali della panoramica
- **Allora** costi congelati, numero di righe e totali sono invariati

**CA-6 — Isolamento fra account**
- **Dato** l'account `A` con un prezzo negoziato su un modello e l'account `B` senza
- **Quando** entrambi confrontano lo stesso modello
- **Allora** `A` vede il proprio prezzo negoziato e `B` il prezzo pubblico; nessuno dei due vede i prezzi dell'altro

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (backend e frontend; l'intera suite prima del commit);
- [ ] prove di **unità** sulla simulazione (quattro voci di prezzo, prezzo negoziato, prezzo mancante, confronto per
      unità) e di **integrazione** sulla rotta;
- [ ] prova di **isolamento fra account**, con verifica esplicita che i prezzi negoziati non attraversino gli
      account;
- [ ] prova che il confronto **non produce scritture**: conteggio delle righe invariato prima e dopo;
- [ ] **prova end-to-end**: **coprire ora**, estendendo `[J-SPESA-MODELLI]` con il passo «confronto il modello usato
      con un'alternativa e vedo la differenza», e aggiornare il registro di copertura
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml);
- [ ] **traduzioni** presenti in tutte e cinque le lingue, con revisione mirata dell'avvertenza;
- [ ] controllo automatico di **accessibilità** sulla schermata del confronto;
- [ ] **manifesto dei dati**: nessuna modifica;
- [ ] **registro delle decisioni** compilato, in particolare sull'uso dei prezzi del periodo e sul rifiuto di
      estrapolare i prezzi mancanti;
- [ ] contratto degli **strumenti conversazionali** dichiarato per `confronta_costo_modelli`;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storia `0013` | Serve il catalogo dei prezzi datato, con i prezzi validi nel periodo simulato |
| Storia `0016` | I prezzi negoziati dell'account devono entrare nella simulazione, altrimenti il confronto è falso |
| Storia `0028` | La sintesi giornaliera per modello è la sorgente da cui la simulazione legge quando può |

## 7. Fuori ambito

- **qualunque giudizio sulla qualità** delle risposte: è dichiarato fuori ambito dall'app (§1 del documento
  capofila) e nessuna parte di questa storia lo introduce, neppure come stima;
- la **raccomandazione automatica** di cambiare modello: qui si mostra il numero, la scelta è del cliente. Una
  raccomandazione richiederebbe di conoscere la qualità, che non conosciamo;
- il ricalcolo effettivo dei costi storici con un altro prezzo: è la storia `0017`, ed è un'operazione di natura
  completamente diversa;
- il confronto con modelli di fornitori che l'account non usa e per cui non ha una fonte: si può fare — il catalogo
  dei prezzi è comune — ma il risultato dichiara che si tratta di un fornitore non collegato.

## 8. Punti aperti

- **Se stimare i conteggi in uscita del modello alternativo.** Un modello diverso produce risposte di lunghezza
  diversa, e usare i conteggi del modello originale sottostima o sovrastima. Esistono correttivi (rapporto medio
  osservato fra modelli), ma sono stime su stime e renderebbero il numero meno difendibile. Proposta: **no**,
  si usano i conteggi veri e si dichiara il limite. La conferma lo sviluppatore.
