# 0024 — Qualificazione dell'imposta sul valore aggiunto

**Applicazione**: 08 — SpendGrove (`notespese`) · **Epica**: 05 — Riconciliazione e uscita verso la contabilità
**Storia**: `0024` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0008`, `0010`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che vorrebbe recuperare l'imposta sulle spese aziendali
> voglio che l'app mi dica, riga per riga, se l'imposta è recuperabile e perché no quando non lo è
> così da sapere quando conviene chiedere la fattura invece dello scontrino, mentre sono ancora davanti alla cassa.

**Contesto.** Il catalogo indica fra i casi d'uso principali il «tracciamento IVA detraibile», ed è una funzione che
si può fare bene o male. La regola di fondo è netta: **il diritto alla detrazione si esercita solo con una fattura
intestata** al soggetto passivo; con un documento commerciale — quello che tutti chiamano scontrino — l'imposta non
si detrae, mentre il costo resta deducibile se inerente (descrizione, §2.3, fonte 7). C'è poi tutto il capitolo
delle indetraibilità parziali, che varia per categoria e per situazione. L'app **non fa consulenza fiscale**:
qualifica, spiega e mette il commercialista in condizione di decidere.

## 2. Requisiti funzionali

1. **RF-1** — Ogni spesa dichiara il **tipo di documento**: documento commerciale (scontrino), fattura intestata
   all'azienda, ricevuta fiscale, nessun documento.
2. **RF-2** — L'app calcola la qualificazione: imposta **recuperabile**, **non recuperabile**, oppure **da
   verificare**, con il motivo in una riga comprensibile («con lo scontrino l'imposta non si detrae: serve la
   fattura intestata»).
3. **RF-3** — Ogni categoria porta una percentuale di indetraibilità predefinita, configurabile dall'account, che
   l'app applica e mostra: il valore predefinito è **zero** e va scelto dal cliente con il suo consulente.
4. **RF-4** — Una spesa può avere più aliquote (un conto con cibo e bevande a percentuali diverse): si registrano
   più voci d'imposta, ciascuna con imponibile, aliquota, imposta e quota indetraibile.
5. **RF-5** — In revisione, quando il tipo di documento è uno scontrino, l'app **suggerisce** di chiedere la
   fattura, con un testo che spiega quanto si sta perdendo: è il momento in cui il suggerimento serve.
6. **RF-6** — L'app **non** produce liquidazioni, registri o dichiarazioni: qualifica il dato e lo consegna
   (storia `0025`).

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Voci d'imposta e configurazioni di categoria filtrano per `tenant_id`
  preso dal token verificato.
- **RT-2 — Interfaccia di programmazione (§2).** Le voci d'imposta sono parte della rappresentazione della spesa
  (`GET|PATCH /api/notespese/v1/spese/{id}`); la configurazione sta su `PATCH /api/notespese/v1/categorie/{id}`;
  errori in `application/problem+json` con `422` quando la somma delle voci non torna con il totale; definizione
  OpenAPI aggiornata.
- **RT-3 — Persistenza (§8).** Migrazione `V21__voci_imposta.sql`: tabella `voce_imposta` con `tenant_id`, chiave
  UUID versione 7, riferimento logico alla spesa, imponibile, aliquota, imposta, quota indetraibile, motivo,
  colonne di controllo e cancellazione logica; colonna `tipo_documento` sulla spesa (già prevista dalla storia
  `0002`, qui valorizzata e usata).
- **RT-4 — Modulo frontend (§3, §5).** Nella schermata di revisione, il blocco delle voci d'imposta con il calcolo
  visibile e il suggerimento sulla fattura; in *Impostazioni*, l'indetraibilità per categoria. Solo token del
  sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Testi delle qualificazioni e dei motivi passano dallo spazio-nomi `notespese` e
  sono presenti in `en, it, fr, es, de`, e **dichiarano la giurisdizione** a cui si riferiscono: la regola italiana
  non vale altrove.
- **RT-6 — Varchi e quota (§6, §7).** Nessun consumo nuovo: la qualificazione avviene dentro la revisione.
- **RT-7 — Esposizione conversazionale (§12).** La qualificazione entra nella lettura `verifica_deducibilita`
  (storia `0020`) come secondo genere di rischio: «imposta non recuperabile perché manca la fattura». Nessuno
  strumento di scrittura.
- **RT-8 — Dati personali (§10).** Le voci d'imposta non contengono dati personali in sé, ma sono legate a una
  spesa che sì: la tabella `voce_imposta` entra comunque in `exportData` e `purgeData`, perché lasciarla fuori
  significherebbe che la cancellazione di un collaboratore lascia indietro dei frammenti. Voce nel manifesto in
  italiano e inglese.
- **RT-9 — Registrazione eventi (§14).** Gli eventi `imposta qualificata`, `voce d'imposta incoerente` portano
  `tenant_id`, `app_id`, `user_id`, identificativo di correlazione e il codice della qualificazione — non gli
  importi.

## 4. Criteri di accettazione

**CA-1 — Scontrino: imposta non recuperabile**
- **Dato** una spesa con tipo di documento «documento commerciale»
- **Quando** viene qualificata
- **Allora** l'imposta risulta non recuperabile, il motivo è mostrato in una riga comprensibile e il costo resta
  indicato come deducibile se inerente

**CA-2 — Fattura intestata: imposta recuperabile**
- **Dato** la stessa spesa con tipo di documento «fattura intestata all'azienda»
- **Quando** viene qualificata
- **Allora** l'imposta risulta recuperabile, salvo la quota di indetraibilità configurata per la categoria

**CA-3 — Indetraibilità parziale**
- **Dato** la categoria Rappresentanza configurata al 50% di indetraibilità e una spesa con 22,00 € di imposta
- **Quando** viene qualificata
- **Allora** 11,00 € risultano recuperabili e 11,00 € no, con il motivo della quota indetraibile

**CA-4 — Più aliquote**
- **Dato** una spesa con due voci: 100 € al 10% e 50 € al 22%
- **Quando** si salva
- **Allora** la somma di imponibili e imposte deve tornare con il totale della spesa, altrimenti la risposta è
  `422` con l'indicazione dello scarto

**CA-5 — Suggerimento al momento giusto**
- **Dato** una spesa da 240 € con scontrino · **Quando** l'utente la rivede
- **Allora** compare il suggerimento di chiedere la fattura, con l'indicazione dell'imposta che si sta perdendo

**CA-6 — Isolamento fra account**
- **Dato** due account con configurazioni di indetraibilità diverse sulla stessa categoria
- **Quando** ciascuno qualifica una spesa identica
- **Allora** ognuno ottiene il risultato della **propria** configurazione

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno backend e frontend; l'intera suite prima del commit);
- [ ] prove di **unità** sulla qualificazione (matrice tipo di documento × categoria × indetraibilità) e sulla
      quadratura delle voci; di **integrazione** sulla spesa con più voci, con database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** su voci d'imposta e configurazioni;
- [ ] **prova end-to-end**: *coprire ora* il passo «lo scontrino mi dice che l'imposta non si recupera» nel percorso
      `[J-NOTESPESE]`; registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) aggiornato;
- [ ] **traduzioni** presenti in tutte e cinque le lingue, con la giurisdizione dichiarata;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese, con la tabella presente in esportazione e
      cancellazione;
- [ ] **registro delle decisioni** compilato, con la scelta di predefinire l'indetraibilità a zero e il perché;
- [ ] contratto degli **strumenti conversazionali**: la qualificazione entra in `verifica_deducibilita`;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| `0008` | La qualificazione si mostra nella schermata di revisione |
| `0010` | L'indetraibilità predefinita è una proprietà della categoria |

## 7. Fuori ambito

- Liquidazioni, registri e dichiarazioni: non è un software di contabilità (descrizione, §1).
- Le regole di detraibilità di giurisdizioni diverse dall'Italia: la struttura le regge come configurazione, i
  contenuti no (punto aperto n. 3 della descrizione).
- L'imposta sugli acquisti dall'estero e i regimi speciali: fuori portata per il ciclo della nota spese di una
  micro-impresa.

## 8. Punti aperti

- 🛑 **Fino a che punto l'app può spingersi nel qualificare** senza fare consulenza fiscale. La proposta è: dire la
  regola generale, mostrare il calcolo, non decidere i casi dubbi (che restano «da verificare»). Ma il confine va
  confermato da chi si assume il rischio — è la stessa decisione della storia `0020`.
- **Valori predefiniti di indetraibilità per categoria**: fornirli sarebbe comodo e rischioso insieme. Oggi zero, e
  la scelta è del cliente con il suo consulente. Da confermare.
