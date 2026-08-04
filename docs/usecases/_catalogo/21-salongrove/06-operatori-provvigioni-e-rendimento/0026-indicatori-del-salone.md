# 0026 — Indicatori del salone

**Applicazione**: 21 — SalonGrove (`salone`) · **Epica**: 06 — Operatori, provvigioni e rendimento
**Storia**: `0026` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0007`, `0017`, `0019`, `0025`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che vuole sapere se il salone sta andando
> voglio quattro numeri veri sul mese — quanto sono piene le poltrone, quanto vale uno scontrino, quanto costa il
> prodotto, quanto pesa la rivendita
> così da accorgermi a luglio di uno scostamento, invece che a gennaio dell'anno dopo.

**Contesto.** Tutte le epiche precedenti producono dati; questa li restituisce come risposta. È l'ultima del gruppo
economico perché ha bisogno che i numeri siano completi: senza il consumo di cabina (storia `0017`) non c'è
margine, senza il conto chiuso (storia `0019`) non c'è incasso, senza l'occupazione a segmenti (storia `0007`) il
riempimento è finto. È anche la storia in cui il piano `sede` trova la sua ragione (§5 della
[descrizione](../application-description.md)).

🛑 **Questa è la storia più delicata dell'epica, e il motivo non è tecnico.** Misurare il rendimento delle persone
tocca il confine del **controllo dell'attività lavorativa**. Il confine lo tracciamo qui e lo diciamo per esteso:

- **le provvigioni sono un fatto retributivo** e stanno per persona, nel prospetto della storia `0025`: si paga una
  persona, non un salone;
- **gli indicatori sono un fatto d'impresa** e stanno **per salone, per servizio, per postazione e per periodo**.
  **Non esiste una vista che ordini gli operatori per prodotto, per scontrino medio, per riempimento o per
  qualunque altra misura, né una che li affianchi per confrontarli.**

Non è una scelta isolata: è la stessa che hanno preso le altre applicazioni di questo catalogo che toccano il tema
— **08 SpendGrove** (storia `0027`: «non esiste alcun ordinamento dei collaboratori per spesa presentato come
graduatoria né alcun indicatore individuale di comportamento») e **13 FlowGrove** (§6 della sua descrizione: «le
viste aggregate sono per commessa, non per persona; nessuna classifica di produttività, nessun punteggio, nessun
indicatore *ore per dipendente*»). SalonGrove **non se ne discosta**: qui il dato per persona esiste soltanto dove
serve a calcolare quanto le spetta. È lo stesso confine che ha portato a escludere dal catalogo l'app 11
ShiftGrove (§6 e §10 della descrizione, punto aperto n. 6).

## 2. Requisiti funzionali

1. **RF-1** — Il cruscotto mostra, per un periodo scelto: **riempimento** delle postazioni (minuti occupati su
   minuti disponibili), **incasso** e **scontrino medio**, **costo del prodotto di cabina** e **margine sui
   servizi**, **peso della rivendita** sull'incasso.
2. **RF-2** — Il dettaglio si raggruppa per **servizio**, per **postazione**, per **giorno della settimana** e per
   **fascia oraria**, con il confronto rispetto al periodo precedente.
3. **RF-3** — Ogni numero è **navigabile**: dall'aggregato si arriva all'elenco dei conti o degli appuntamenti che
   lo compongono, in un tocco.
4. **RF-4** — **Nessun raggruppamento per operatore.** Le rotte non accettano l'operatore come chiave di
   raggruppamento, l'interfaccia non lo offre e nessuna esportazione lo produce. L'unico dato intestato a una
   persona resta il prospetto delle provvigioni (storia `0025`), che si apre uno per uno.
5. **RF-5** — Il cruscotto si esporta come tabella per il periodo mostrato, con gli stessi numeri che si vedono a
   schermo e con lo stesso limite.
6. **RF-6** — Gli indicatori sono visibili a chi **amministra** l'account. Un operatore non vede il cruscotto: non
   per riservatezza dei numeri d'impresa, ma perché non è la sua schermata — se il salone vuole condividerli, lo fa
   parlando, non con un tabellone.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni aggregazione filtra per `tenant_id` preso dal token verificato, e il
  filtro si applica **prima** di aggregare.
- **RT-2 — Interfaccia di programmazione (§2).** `GET /api/<app>/v1/indicatori?periodo=&raggruppamento=` e
  `GET /api/<app>/v1/indicatori/export`; il parametro `raggruppamento` accetta **solo** `servizio`, `postazione`,
  `giorno`, `fascia_oraria` — un valore `operatore` risponde `400` con un messaggio che dice perché, non `404`:
  chi lo tenta deve leggere il motivo. Errori in `application/problem+json`; OpenAPI aggiornata.
- **RT-3 — Persistenza (§8).** Nessuna tabella nuova: si aggrega su quelle esistenti con gli indici che servono
  (periodo, servizio, postazione). Se i tempi di risposta non reggono si valuta una vista materializzata, **ma non
  prima di aver misurato**: un salone ha migliaia di righe l'anno, non milioni.
- **RT-4 — Modulo frontend (§3, §5).** Sezione *Andamento*: al massimo quattro indicatori in testa — se sono sette
  non sono indicatori — più i raggruppamenti. I grafici restano leggibili senza colore. Solo token del sistema di
  design; tema chiaro e scuro; controllo automatico di accessibilità verde.
- **RT-5 — Cinque lingue (§4).** Etichette, unità di misura e formati numerici passano dallo spazio-nomi dell'app e
  sono presenti in `en, it, fr, es, de`.
- **RT-6 — Varchi e quota (§6, §7).** Nessun consumo di quota. La sezione è accesa dal piano `sede`; a piano
  inferiore risponde `402` e l'interfaccia spiega che cosa contiene, senza mostrarne i numeri.
- **RT-7 — Dati personali (§10).** **Nessun campo nuovo**, ma va scritto nel manifesto in italiano e inglese un
  **limite d'uso**: gli aggregati economici del verticale hanno finalità di gestione dell'impresa e **non** sono
  raggruppabili per operatore; il dato per persona esiste solo nel prospetto delle provvigioni, per la finalità
  retributiva. È la riga che tiene l'app fuori dal controllo a distanza dell'attività lavorativa (§6 della
  descrizione, avviso sul lavoro).
- **RT-8 — Esposizione conversazionale (§12).** Nessuno strumento nuovo in questa storia:
  `provvigioni_periodo` (storia `0025`) resta l'unico che tocca una persona, e non ordina né confronta. Uno
  strumento di riepilogo degli indicatori si potrà aggiungere, ma con lo **stesso** divieto di raggruppamento, ed è
  dichiarato nel contratto (storia `0028`).
- **RT-9 — Registrazione eventi (§14).** `indicatori esportati` con `tenant_id`, `app_id`, `user_id`, correlazione
  e periodo — nessun numero, nessun nome.

## 4. Criteri di accettazione

**CA-1 — I numeri tornano**
- **Dato** un mese con 8.400 € di incasso su 210 conti chiusi e 620 € di prodotto di cabina consumato
- **Quando** si apre il cruscotto di quel mese
- **Allora** scontrino medio 40 €, costo del prodotto 620 €, margine sui servizi coerente con i due valori

**CA-2 — Il riempimento tiene conto delle fasi**
- **Dato** una giornata con un colore le cui fasi lasciano l'operatore libero durante la posa (storia `0007`)
- **Quando** si legge il riempimento delle postazioni
- **Allora** i minuti della posa risultano occupati per la postazione e liberi per l'operatore, come nell'agenda

**CA-3 — Dal numero alla riga**
- **Dato** il raggruppamento per servizio con 2.100 € sul colore
- **Quando** si tocca quel valore
- **Allora** si apre l'elenco dei conti che lo compongono, filtrato, e la loro somma è 2.100 €

**CA-4 — Nessuna classifica di persone**
- **Dato** il cruscotto in tutte le sue viste, le sue esportazioni e le sue rotte
- **Quando** si tenta il raggruppamento per operatore, anche interrogando direttamente l'interfaccia di
  programmazione
- **Allora** la richiesta è respinta con `400` e un messaggio che spiega il motivo, e nessuna vista mostra un
  ordinamento di persone

**CA-5 — Piano insufficiente**
- **Dato** un account sul piano `salone`
- **Quando** apre la sezione *Andamento*
- **Allora** vede la descrizione di che cosa contiene e l'invito a cambiare piano, e la rotta risponde `402`

**CA-6 — Isolamento fra account**
- **Dato** due account con conti nello stesso mese
- **Quando** l'uno apre il cruscotto
- **Allora** nessun valore dell'altro contribuisce ad alcun totale

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend` e `frontend`; l'intera suite prima del commit);
- [ ] prove di **unità** sulle aggregazioni, sul riempimento a segmenti e sui confronti di periodo; di
      **integrazione** sulle rotte con database effimero e migrazioni vere;
- [ ] prova **negativa** che il raggruppamento per operatore sia rifiutato su tutte le rotte e assente da ogni
      esportazione: è il presidio del confine, e se non è una prova non esiste;
- [ ] prova di **isolamento fra account** sugli aggregati;
- [ ] **prova end-to-end**: *coprire ora* — il percorso `[J-SALONGROVE]` (storia `0030`) apre il cruscotto, verifica
      i numeri attesi e contiene la prova negativa sulla classifica; registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) aggiornato;
- [ ] **traduzioni** in tutte e cinque le lingue, con i formati numerici corretti;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese con il limite d'uso degli aggregati;
- [ ] **registro delle decisioni**: elenco dei raggruppamenti ammessi, rifiuto esplicito del raggruppamento per
      operatore e sua motivazione, coerenza dichiarata con 08 SpendGrove e 13 FlowGrove;
- [ ] controllo automatico di **accessibilità** verde sulla sezione;
- [ ] avvio locale invariato.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0007` | il riempimento si calcola sull'occupazione a segmenti, altrimenti è un numero falso |
| storia `0017` | il costo del prodotto di cabina è ciò che trasforma l'incasso in margine |
| storia `0019` | l'incasso e lo scontrino medio vengono dai conti chiusi |
| storia `0025` | è la storia con cui questa deve restare coerente: il per-persona sta là, e solo là |

## 7. Fuori ambito

- **qualunque indicatore per persona**: escluso per scelta, non per mancanza di tempo, e la prova negativa lo
  sorveglia;
- le **previsioni** e i budget: qui si guarda il consuntivo;
- il **confronto con altri saloni** (dati di mercato): richiederebbe di usare i dati dei clienti per una finalità
  secondaria, che la piattaforma vieta (§10 dei principi);
- i **grafici elaborati**: un salone ha bisogno di quattro numeri giusti, non di una sala di controllo.

## 8. Punti aperti

**È il punto aperto n. 6 della descrizione, e questa storia lo chiude solo per la parte tecnica.** La proposta —
indicatori per salone e per servizio, dato per persona solo dove serve al calcolo di quanto spetta — va confermata
dallo sviluppatore **prima** di implementare, perché la prima richiesta che arriverà da un titolare sarà «fammi
vedere chi vende di più» e la risposta deve essere già scritta. Se la si volesse concedere, non sarebbe una
schermata in più: sarebbe una finalità nuova, con base giuridica, informativa e verifica legale proprie — e, in
Italia, con la disciplina sul controllo a distanza dell'attività lavorativa da esaminare.

**Il confronto fra periodi disomogenei** (un mese con due settimane di chiusura estiva e uno pieno) suggerisce
conclusioni sbagliate. Va deciso se mostrarlo sempre e con quale avvertenza.
