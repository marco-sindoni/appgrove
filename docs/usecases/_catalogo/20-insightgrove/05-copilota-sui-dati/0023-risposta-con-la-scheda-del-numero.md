# 0023 — Risposta con la scheda del numero

**Applicazione**: 20 — InsightGrove (`insights`) · **Epica**: 05 — Copilota sui dati
**Storia**: `0023` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0016`, `0022`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che ha appena ricevuto una risposta dal copilota
> voglio sapere da dove viene quel numero prima di usarlo in una decisione
> così da poterlo controllare in un clic invece di doverci credere sulla parola.

**Contesto.** La differenza fra un assistente generico e questa app sta tutta qui. Un assistente risponde
«42.300 €»; questa app risponde «42.300 €, da 118 fatti di due fonti, aggiornato alle 06:15, completo, con la
definizione `fatturato_emesso` versione 3 — ecco le prime dieci fatture». La ricevuta esiste già (storia 0016):
questa storia la porta **dentro la conversazione**, e stabilisce che una risposta senza ricevuta non esce
dall'applicazione.

## 2. Requisiti funzionali

1. **RF-1** — Ogni risposta numerica del copilota è accompagnata da: il piano eseguito in forma leggibile, il
   valore con la sua unità, il **grado di completezza** e l'accesso alla **scheda del numero**.
2. **RF-2** — Se il valore è **parziale**, il copilota lo dice **nella prima frase** della risposta, prima del
   numero, non in coda.
3. **RF-3** — La risposta contiene i **rimandi** alla riga d'origine (fino a dieci, storia 0011): dalla chat si
   arriva alla fattura.
4. **RF-4** — Il testo della risposta è generato dal modello **a partire dal risultato già calcolato**: il
   modello mette in parole un numero che ha ricevuto, e **non può alterarlo**. Il numero mostrato è quello del
   calcolo, inserito nel testo in modo verificabile.
5. **RF-5** — Quando la domanda ha prodotto un confronto, la risposta dichiara che cosa è stato confrontato con
   che cosa, comprese le regole del confronto parziale su periodi in corso (storia 0015).
6. **RF-6** — Una risposta si può **rieseguire**: lo stesso piano ricalcolato mostra il valore di allora e quello
   di adesso, con che cosa è cambiato.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** La traccia associata alla risposta si legge con `tenant_id` dal
  gettone verificato; una risposta di un altro account non è raggiungibile.
- **RT-2 — Interfaccia di programmazione (§2).** La risposta della rotta `POST /api/insights/v1/domande` include
  il riferimento alla traccia; errori in `application/problem+json`; definizione OpenAPI aggiornata nello stesso
  commit.
- **RT-4 — Modulo frontend (§3, §5).** La sezione Copilota mostra la risposta con il piano e il contrassegno di
  completezza accanto al numero; la scheda si apre nel pannello laterale già costruito dalla storia 0016; solo
  token del sistema di design; tema chiaro e scuro; controllo automatico di accessibilità sulla conversazione.
- **RT-5 — Cinque lingue (§4).** Le etichette e i testi di contorno esistono in `en, it, fr, es, de`; il testo
  generato è nella lingua della domanda.
- **RT-6 — Varchi e ruoli (§6).** La risposta rispetta la classe di riservatezza della metrica (storia 0025).
- **RT-8 — Dati personali (§10).** Nessuna voce nuova oltre a `domanda.testo` (storia 0022); la risposta
  generata **non** viene conservata come testo se non insieme alla domanda, ed è soggetta alla stessa
  conservazione e alla stessa cancellazione.
- **RT-14 — Registrazione eventi (§14).** «Risposta prodotta», «risposta rieseguita» con `tenant_id`, `app_id`,
  `user_id`, identificativo della traccia; **mai** il testo della risposta.
- **RT-11 — Prove (§11).** Prova che il numero mostrato nel testo coincide **esattamente** con il valore
  calcolato: è il collaudo che impedisce al modello di arrotondare o riscrivere una cifra.

## 4. Criteri di accettazione

**CA-1 — La risposta porta la ricevuta**
- **Dato** la domanda «quanto ho fatturato a luglio»
- **Quando** il copilota risponde
- **Allora** la risposta mostra il numero, l'unità, il piano eseguito, il grado di completezza, l'accesso alla
  scheda e fino a dieci rimandi alla riga d'origine

**CA-2 — Il parziale si dice per primo**
- **Dato** una fonte richiesta silente da sei giorni
- **Quando** il copilota risponde a una domanda che la coinvolge
- **Allora** la prima frase della risposta dice che il dato è incompleto e perché, e solo dopo compare il numero

**CA-3 — Il modello non altera il numero**
- **Dato** un calcolo che ha prodotto `42.317,55 €`
- **Quando** il copilota mette in parole il risultato
- **Allora** la cifra mostrata è `42.317,55 €`; una prova automatica verifica la coincidenza esatta fra valore
  calcolato e valore mostrato

**CA-4 — Dalla chat alla fattura**
- **Dato** una risposta su «crediti scaduti»
- **Quando** l'utente clicca un rimando
- **Allora** atterra sulla schermata della fattura corrispondente nell'app sorgente

**CA-5 — Riesecuzione**
- **Dato** una risposta di ieri e dodici fatti arrivati nel frattempo
- **Quando** l'utente chiede di rieseguirla
- **Allora** vede il valore di ieri, quello di oggi e «12 fatti arrivati dopo la prima risposta»

**CA-6 — Isolamento fra account**
- **Dato** una traccia dell'account `B`
- **Quando** un utente di `A` prova ad aprirla dal proprio copilota
- **Allora** riceve `404`

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno le aree toccate; l'intera suite prima del commit);
- [ ] prove di **unità** sulla coincidenza esatta fra valore calcolato e valore mostrato, e sull'ordine delle
      frasi quando il valore è parziale; prove di **integrazione** sulla risposta completa;
- [ ] prova di **isolamento fra account** sulla traccia associata alla risposta;
- [ ] **prova end-to-end**: *coprire ora* — il percorso `[J-INSIGHTS]` include «dalla risposta del copilota alla
      riga d'origine»; registro di copertura aggiornato;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati** verificato: la risposta segue la sorte della domanda in esportazione e
      cancellazione;
- [ ] **registro delle decisioni** compilato, con la regola «il modello mette in parole un numero che non può
      cambiare» e il collaudo che la sorveglia;
- [ ] contratto degli **strumenti conversazionali**: `spiega_numero` restituisce la stessa scheda mostrata qui
      (storia 0031);
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0016` | la scheda del numero esiste già: qui si porta nella conversazione |
| storia `0022` | serve il piano e il suo risultato |
| storia `0011` | i rimandi alla riga d'origine |

## 7. Fuori ambito

- il rifiuto e il «non lo so»: storia 0024;
- la riservatezza: storia 0025;
- la generazione di grafici dentro la conversazione: rimandata, perché nessuna fonte l'ha indicata come
  richiesta prioritaria e i riquadri del cruscotto la coprono.

## 8. Punti aperti

- **Il testo generato va conservato?** Conservarlo aiuta a spiegare una risposta contestata; non conservarlo
  riduce la superficie di dati. Raccomandazione: **conservare il piano e il risultato sempre, il testo generato
  solo per la durata della conversazione**, perché il testo si può rigenerare dal piano. Chiude:
  **sviluppatore**.
