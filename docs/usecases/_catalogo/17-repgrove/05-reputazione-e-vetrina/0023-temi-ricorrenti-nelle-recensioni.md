# 0023 — Temi ricorrenti nelle recensioni

**Applicazione**: 17 — RepGrove (`recensioni`) · **Epica**: 05 — Reputazione e vetrina
**Storia**: `0023` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0017`, `0022`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che legge le recensioni una per una e si ricorda solo l'ultima
> voglio sapere di cosa parlano davvero i miei clienti — l'attesa, la pulizia, il prezzo, una persona del mio
> personale — e se il tema è positivo o negativo
> così da capire su cosa intervenire invece di reagire all'ultimo che si è lamentato.

**Contesto.** È la funzione che il catalogo chiama «report di sentiment», e va fatta in modo utile a chi ha
quaranta recensioni, non quattromila. Il valore per una micro-impresa non è un punteggio di sentimento fra meno
uno e più uno: è la frase «negli ultimi tre mesi sei persone hanno parlato dell'attesa, cinque in modo negativo».
Il numero astratto non fa cambiare niente; il tema con le recensioni sotto sì.

Due limiti dichiarati fin da subito. Il primo: questa storia **dipende dalla conservazione del testo**, che è un
punto aperto (storia 0010); se il testo non si può conservare, la funzione non esiste. Il secondo: **le persone
nominate nelle recensioni sono persone**. Un tema «Marco» che raccoglie le recensioni che citano un dipendente è
una valutazione automatica su un lavoratore, e questo è un terreno delicato: va guardato prima, non dopo.

## 2. Requisiti funzionali

1. **RF-1** — L'app estrae dai testi delle recensioni di una sede i temi ricorrenti, con: nome del tema, quante
   recensioni lo toccano, quante in modo positivo e quante negativo, e il periodo.
2. **RF-2** — Ogni tema è **navigabile**: si apre e si vedono le recensioni che lo compongono. Un tema che non
   porta alle sue recensioni non è verificabile, e quindi non è utile.
3. **RF-3** — I temi si calcolano su un periodo scelto e si confrontano con il periodo precedente: quello che
   conta è ciò che sta cambiando.
4. **RF-4** — L'app mostra i temi solo quando ci sono abbastanza recensioni con testo perché il risultato
   significhi qualcosa (proposta: 10 nel periodo); sotto quella soglia dice quante ne mancano.
5. **RF-5** — I temi che corrispondono a **persone** (nomi di dipendenti) sono trattati a parte: non compaiono
   nella vista dei temi per difetto, e la vista dedicata — se ci sarà — richiede una decisione esplicita
   (vedi i punti aperti). Nessuna classifica del personale, in nessun caso: le quote e le classifiche sul
   personale sono anche una pratica vietata dalle piattaforme in fase di raccolta (descrizione §1, rifiuto 5), e
   sarebbe incoerente rifiutarle da un lato e ricostruirle dall'altro.
6. **RF-6** — Se la conservazione del testo è disattivata (storia 0010), la funzione è spenta con una spiegazione,
   e nessuna schermata va in errore.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** L'estrazione dei temi opera per account; nessun modello, nessun
  vocabolario e nessuna statistica sono condivisi fra account.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `GET /api/recensioni/v1/sedi/{id}/temi?periodo=` e
  `GET /api/recensioni/v1/temi/{id}/recensioni`; errori in `application/problem+json` con un codice per «testo non
  conservato»; definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** I temi si calcolano periodicamente e si conservano come risultato per periodo, con
  il riferimento alle recensioni che li compongono; migrazione `V10__temi.sql`. Il risultato non sopravvive alle
  recensioni da cui deriva.
- **RT-4 — Modulo frontend (§3, §5).** *Panoramica* → «Di cosa parlano»: elenco dei temi con conteggi e segno,
  variazione rispetto al periodo precedente, apertura sulle recensioni. Solo token del sistema di design; tema
  chiaro e scuro; il segno positivo/negativo si legge **anche senza colore**.
- **RT-5 — Cinque lingue (§4).** L'interfaccia in `en, it, fr, es, de`. I **temi** nascono da testi in lingue
  diverse: vanno raggruppati per significato, non per parola, e il nome del tema va mostrato nella lingua
  dell'interfaccia. Dove l'app non sa raggruppare fra lingue diverse, lo dice.
- **RT-6 — Varchi e quota (§6, §7).** Nessun consumo di quota; `402` con abbonamento `canceled`. Se l'estrazione
  ha un costo variabile, il presidio è un limite tecnico di frequenza, non una seconda metrica.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento dedicato nella prima versione: la domanda «di
  cosa si lamentano i miei clienti?» si può servire con `elenca_recensioni` e la lettura dei testi. Se in seguito
  si aggiungesse `temi_ricorrenti`, sarebbe di sola lettura.
- **RT-8 — Dati personali (§10).** Il testo delle recensioni è già dichiarato (storia 0009) e può contenere
  **categorie particolari** (descrizione §6): un'analisi automatica su quel testo è un trattamento ulteriore e va
  valutato nella valutazione d'impatto, non aggiunto di straforo. Se l'estrazione passa da un fornitore esterno,
  quel fornitore va dichiarato.
- **RT-9 — Registrazione eventi (§14).** `temi calcolati: n temi su m recensioni`, con `tenant_id`, `app_id` e
  identificativo di correlazione, senza testi e senza nomi di temi che siano nomi di persona.

## 4. Criteri di accettazione

**CA-1 — I temi compaiono**
- **Dato** una sede con venti recensioni con testo, di cui sei parlano dell'attesa (cinque negative)
- **Quando** si apre «Di cosa parlano»
- **Allora** il tema dell'attesa compare con sei ricorrenze, cinque negative, e la variazione sul periodo
  precedente

**CA-2 — Il tema porta alle recensioni**
- **Dato** un tema mostrato
- **Quando** lo si apre
- **Allora** si vedono le recensioni che lo compongono, con il testo e l'attribuzione

**CA-3 — Sotto la soglia**
- **Dato** una sede con quattro recensioni con testo nel periodo
- **Quando** si apre la sezione
- **Allora** l'app non mostra temi e dice quante recensioni servono ancora

**CA-4 — Nessuna classifica del personale**
- **Dato** recensioni che citano ripetutamente i nomi di due dipendenti
- **Quando** si aprono i temi
- **Allora** non compare nessuna vista che confronti le persone né nessun conteggio per dipendente

**CA-5 — Testo non conservato**
- **Dato** la conservazione del testo disattivata
- **Quando** si apre la sezione
- **Allora** la funzione è spenta con una spiegazione, e nessuna schermata va in errore

**CA-6 — Isolamento fra account**
- **Dato** due account con recensioni simili
- **Quando** si calcolano i temi per `A`
- **Allora** il risultato deriva solo dalle recensioni di `A`

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno le aree toccate; l'intera suite prima del commit);
- [ ] prove di **unità** sull'aggregazione dei temi e sulla soglia; di **integrazione** sulla lavorazione con
      estrattore **simulato**;
- [ ] prova di **isolamento fra account** sul calcolo dei temi;
- [ ] **prova end-to-end**: *rimando* — funzione secondaria che richiede un volume di dati non compatibile con un
      percorso end-to-end veloce; coperta a livello di integrazione. Voce motivata nel registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml);
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato con l'eventuale fornitore dell'estrazione e con la finalità di analisi;
- [ ] **registro delle decisioni** compilato, con la scelta di tenere fuori i temi che sono persone;
- [ ] controllo automatico di **accessibilità** verde, compresa la leggibilità del segno senza colore.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0017` | servono le recensioni con il testo |
| storia `0010` | se il testo non si conserva, questa storia non esiste |
| **valutazione d'impatto** (descrizione §6) | l'analisi automatica di testi che possono contenere dati sulla salute è un trattamento da valutare prima |

## 7. Fuori ambito

- il punteggio di sentimento come numero unico: non serve a chi deve decidere cosa fare;
- il confronto dei temi con i concorrenti (descrizione §11.3);
- la valutazione del personale, in qualunque forma.

## 8. Punti aperti

- **I temi che sono persone.** Un titolare vuole sapere se i clienti parlano bene di chi lavora con lui, ed è una
  domanda legittima. Ma un'analisi automatica sistematica su un lavoratore ha implicazioni che superano questa
  storia (informativa al lavoratore, uso nel rapporto di lavoro, rappresentanze sindacali). **Non la decido**: la
  prima versione tiene i nomi fuori, e la valutazione spetta allo sviluppatore con un parere.
- **Estrazione con fornitore esterno o in casa**: la prima è migliore e fa uscire i testi; la seconda è più povera
  e non fa uscire niente. La scelta va fatta insieme a quella della storia 0018, non separatamente.
- **Raggruppamento fra lingue diverse**: se non si riesce, i temi vanno separati per lingua e va detto.
</content>
