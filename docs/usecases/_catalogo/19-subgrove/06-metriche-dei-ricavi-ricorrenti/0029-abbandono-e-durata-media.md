# 0029 — Abbandono e durata media

**Applicazione**: 19 — SubGrove (`abbonati`) · **Epica**: 06 — Metriche dei ricavi ricorrenti
**Storia**: `0029` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0028`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare di una palestra che ogni settembre riparte da capo
> voglio sapere quanti iscritti perdo, quanto restano in media e quanto vale in media un abbonato
> così da capire se conviene di più cercarne di nuovi o tenermi quelli che ho.

**Contesto.** La scomposizione della storia `0028` dice cosa è successo il mese scorso; questa storia dice **con
che ritmo** succede. Sono tre numeri soli, e vanno tenuti tre: **quanti se ne vanno** (tasso di abbandono, sia sul
numero di abbonati sia sul ricavo), **quanto restano** (durata media del rapporto) e **quanto vale in media un
abbonato** per tutta la sua permanenza. Il terzo si ricava dai primi due e dal canone medio, ed è il numero che
risponde alla domanda pratica: quanto ha senso spendere per acquisire un iscritto.

C'è una trappola da disinnescare in partenza, ed è la ragione per cui questa storia ha un requisito che le altre
non hanno. Il segmento a cui l'app si rivolge ha **numeri piccoli**: su venti abbonati, uno che se ne va fa il 5%,
due fanno il 10%, e una percentuale calcolata su venti oscilla in modo che non significa nulla. Mostrarla con due
decimali sarebbe una bugia con l'aria della precisione. L'app deve quindi sapere **quando tacere**: sotto una
soglia di numerosità mostra i conteggi e non le percentuali, e lo dice.

## 2. Requisiti funzionali

1. **RF-1** — L'app calcola, su finestre mobili di 3, 6 e 12 mesi: il **tasso di abbandono degli abbonati**
   (quanti cessano rispetto a quanti c'erano) e il **tasso di abbandono del ricavo** (quanto ricavo se ne va
   rispetto a quanto ce n'era).
2. **RF-2** — L'app calcola la **durata media del rapporto** sugli abbonamenti cessati nella finestra, e il
   **valore medio per abbonato** come canone medio mensile moltiplicato per la durata media.
3. **RF-3** — Sotto una **soglia di numerosità** l'app mostra i conteggi assoluti e **non** le percentuali, con una
   riga che spiega perché: «con pochi abbonati una percentuale non dice nulla».
4. **RF-4** — Ogni numero dichiara **su cosa è calcolato**: quale finestra, quanti abbonamenti, quale definizione.
   Un numero senza il suo denominatore non si mostra.
5. **RF-5** — L'abbandono distingue quello **volontario** (l'abbonato ha disdetto) da quello **involontario** (il
   rapporto è finito dopo la catena dei solleciti e la sospensione): sono due problemi diversi e si affrontano in
   due modi diversi (§2.5 della [descrizione](../application-description.md)).
6. **RF-6** — I motivi di cessazione raccolti dalla storia `0024` e dal ciclo di vita si mostrano come conteggi
   accanto all'abbandono, senza pretendere di spiegarlo.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Tutti i calcoli filtrano per `tenant_id` preso dal token verificato; le
  finestre mobili non attraversano mai account diversi.
- **RT-2 — Interfaccia di programmazione (§2).** Rotta `GET /api/abbonati/v1/metriche/abbandono?finestra=3|6|12`,
  che restituisce i numeri **con** i rispettivi denominatori e la marcatura «numerosità insufficiente»; errori in
  `application/problem+json`; definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** **Nessuna tabella nuova**: i numeri si derivano dalle istantanee (`0027`), dai
  movimenti (`0028`) e dalle transizioni della macchina a stati (`0011`). Se il calcolo diventasse pesante, la
  risposta giusta è un'istantanea in più, non una tabella parallela.
- **RT-4 — Modulo frontend (§3, §5).** Nella sezione *Andamento*, un riquadro con i tre numeri, il selettore della
  finestra e la riga che dice su quanti abbonamenti sono calcolati; solo token del sistema di design; tema chiaro e
  scuro.
- **RT-5 — Cinque lingue (§4).** «tasso di abbandono», «durata media», «valore medio per abbonato» e la riga sulla
  numerosità insufficiente in `en, it, fr, es, de`, con i termini spiegati alla prima occorrenza.
- **RT-6 — Varchi e quota (§6, §7).** Lettura: non consuma la metrica `abbonamenti_attivi`. Con abbonamento di
  piattaforma `canceled` risponde `402`; in `past_due` resta accessibile.
- **RT-7 — Esposizione conversazionale (§12).** I numeri di questa storia entrano nel risultato dello strumento di
  lettura `metriche_ricorrenti`, **con** il loro denominatore e con la marcatura di numerosità insufficiente: un
  assistente che leggesse «abbandono 10%» senza sapere che è calcolato su venti abbonati direbbe una sciocchezza
  con sicurezza. Contratto raccolto nella storia `0031`.
- **RT-8 — Dati personali (§10).** **Nessun dato personale nuovo**: sono aggregati. I motivi di cessazione si
  mostrano come conteggi, mai come elenco di persone con il loro motivo accanto.
- **RT-9 — Registrazione eventi (§14).** `metriche di abbandono lette (finestra)`, con `tenant_id`, `app_id`,
  `user_id` e identificativo di correlazione, senza numeri riferibili a persone.
- **RT-10 — Prove (§11).** Unità sulle due definizioni di abbandono, sulla durata media e sulla soglia di
  numerosità, con casi limite: zero abbonati, un solo abbonato, tutti cessati nella finestra.

## 4. Criteri di accettazione

**CA-1 — Numerosità insufficiente**
- **Dato** un account con dodici abbonamenti attivi e uno cessato nella finestra
- **Quando** si aprono le metriche di abbandono
- **Allora** si vedono i conteggi («1 cessato su 12»), **non** una percentuale, e la riga che spiega perché

**CA-2 — Due tassi, due significati**
- **Dato** un account che perde un solo abbonato, ma del piano più caro
- **Quando** si leggono i due tassi di abbandono
- **Allora** quello sul numero di abbonati è basso e quello sul ricavo è alto, e la differenza è spiegata a schermo

**CA-3 — Volontario contro involontario**
- **Dato** un abbonamento cessato per disdetta e uno cessato dopo la catena dei solleciti
- **Quando** si legge l'abbandono
- **Allora** i due compaiono in voci distinte, con i rispettivi conteggi

**CA-4 — Ogni numero porta il suo denominatore**
- **Dato** una qualunque finestra · **Quando** si legge un numero
- **Allora** accanto c'è su quanti abbonamenti e su quale periodo è calcolato

**CA-5 — Casi limite**
- **Dato** un account senza alcun abbonamento cessato · **Quando** si aprono le metriche
- **Allora** non compare alcuna divisione per zero e il riquadro dice «non ci sono ancora cessazioni da misurare»

**CA-6 — Isolamento fra account**
- **Dato** due account · **Quando** uno legge le proprie metriche · **Allora** vede solo i propri numeri

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend`, `frontend`);
- [ ] prove di **unità** sulle definizioni e sui casi limite; **integrazione** sulla rotta con database effimero;
- [ ] prova di **isolamento fra account**;
- [ ] **prova end-to-end**: *nessun impatto* — sono letture derivate; la sezione *Andamento* è già attraversata dal
      percorso `[J-ABBONATI]` della storia `0033` e il registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) non cambia;
- [ ] **traduzioni** presenti in tutte e cinque le lingue, con i termini spiegati;
- [ ] **manifesto dei dati**: nessuna voce nuova;
- [ ] **registro delle decisioni** compilato: definizioni scelte, soglia di numerosità e sua motivazione,
      distinzione volontario/involontario;
- [ ] contratto dello strumento di lettura aggiornato con i denominatori;
- [ ] controllo di accessibilità verde sul riquadro.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0028` | i movimenti di abbandono si contano lì |
| storia `0027` | le finestre mobili si appoggiano alle istantanee |
| storia `0022` | l'abbandono involontario è la coda della sospensione automatica |

## 7. Fuori ambito

- la **previsione** dei mesi futuri: storia `0030`;
- la spiegazione **causale** dell'abbandono («se ne vanno perché il corso del giovedì è pieno»): l'app conta, non
  interpreta; le indagini di soddisfazione sono mestiere di **16 ReachGrove** e **17 RepGrove**;
- le azioni di trattenimento (sconto a chi sta per andarsene): fuori dal nucleo, e nel percorso di disdetta sono
  **vietate** (storia `0024`);
- il confronto con medie di settore: non esiste una fonte che io possa citare, e inventarla sarebbe peggio che
  tacere.

## 8. Punti aperti

**Dove sta la soglia di numerosità.** Sotto quanti abbonamenti una percentuale smette di significare qualcosa non
è una domanda con una risposta unica: dipende da quanto è raro l'evento. **Proposta**: una soglia dichiarata,
uguale per tutti, scelta sul lato prudente, scritta a schermo e cambiabile in un punto solo del codice. Chiude: lo
sviluppatore.

**Quale definizione di durata media.** Calcolarla sui soli abbonamenti **cessati** è semplice e onesto, ma sovrastima
l'abbandono nei primi mesi di vita dell'app (chi è ancora dentro non è contato). L'alternativa — stimarla come
inverso del tasso di abbandono — è più diffusa ma diventa assurda quando l'abbandono è vicino a zero. **Proposta**:
usare la definizione semplice, dichiararla a schermo e non mostrare la stima. Chiude: lo sviluppatore.
