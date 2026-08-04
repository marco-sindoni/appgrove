# 0021 — Lettera di messa in mora

**Applicazione**: 03 — CashGrove (`crediti`) · **Epica**: 04 — Esiti e recupero
**Storia**: `0021` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0020`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che ha esaurito la via bonaria
> voglio ottenere in un clic la bozza di una lettera di costituzione in mora, con tutti i numeri giusti
> così da poterla far controllare e spedire, invece di copiare un modello trovato in rete e sbagliare gli importi.

**Contesto.** Quando la sequenza si esaurisce, il credito va in `in_escalation` e il titolare si trova davanti al passo
formale: la lettera che mette il debitore in mora, interrompe la prescrizione e prepara l'eventuale azione legale.
Oggi la scrive a mano o la fa scrivere, con costi e tempi sproporzionati per una fattura da mille euro. L'app ha già
tutto quello che serve — anagrafica, documenti, importi, mora calcolata, storico dei solleciti — e può comporla. Ma non
può spedirla da sola: è un atto con effetti giuridici, e il confine è netto.

## 2. Requisiti funzionali

1. **RF-1** — Da un credito in stato `scaduto` o `in_escalation` l'utente genera la **bozza** di una lettera di messa in
   mora, in formato adatto alla stampa e alla firma.
2. **RF-2** — La bozza contiene: dati del creditore e del debitore, elenco dei documenti scaduti con importi e scadenze,
   totale in linea capitale, interessi di mora calcolati con il dettaglio, forfait, riepilogo dei solleciti già inviati
   con le date, termine per il pagamento e avvertenza sulle conseguenze.
3. **RF-3** — La bozza è modificabile prima di essere prodotta: il testo è un modello come gli altri, con segnaposto
   verificati.
4. **RF-4** — L'app **non spedisce** la messa in mora: la produce, la mette a disposizione e registra che è stata
   generata. Chi la spedisce e come (raccomandata, posta elettronica certificata, tramite legale) è fuori dall'app.
5. **RF-5** — L'utente registra a mano la data di spedizione e il mezzo, così che il dato entri nella cronologia e nei
   conteggi.
6. **RF-6** — Ogni generazione lascia una riga nel registro, con l'istante, l'autore e il documento prodotto: la
   messa in mora è un atto, non una stampa.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** La generazione e il registro filtrano per `tenant_id` preso dal token
  verificato; i dati del creditore vengono dal profilo dell'account, non dalla richiesta.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `POST /api/crediti/v1/crediti/{id}/messa-in-mora` (genera la
  bozza) e `POST /api/crediti/v1/messe-in-mora/{id}/spedizione` (registra la spedizione avvenuta); errori in
  `application/problem+json`; definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione per la tabella `messa_in_mora` sullo schema `app_crediti` (credito o gruppo di
  crediti, istante di generazione, autore, totali congelati, data e mezzo di spedizione dichiarati) con `tenant_id`,
  chiave UUID versione 7, colonne di controllo e cancellazione logica. I totali sono **congelati** al momento della
  generazione: una lettera spedita non cambia perché il credito è cambiato dopo.
- **RT-4 — Modulo frontend (§3, §5).** Azione «prepara messa in mora» dalla scheda del credito, anteprima del documento,
  registrazione della spedizione; solo token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Le stringhe dell'interfaccia passano dallo spazio-nomi `crediti` e sono presenti in
  `en, it, fr, es, de`. Il **testo della lettera** è invece contenuto del cliente, nella lingua del debitore, e non si
  traduce automaticamente: una lettera con effetti giuridici tradotta da un programma è un rischio, non una comodità.
- **RT-6 — Varchi e quota (§6, §7).** Non consuma quota. Richiede ruolo `owner` o `admin`: è un atto formale
  dell'impresa.
- **RT-7 — Esposizione conversazionale (§12).** `prepara_messa_in_mora(credito) → bozza` è dichiarato nella storia
  `0029` come **scrittura con conferma umana obbligatoria**. Non esiste e non esisterà uno strumento che spedisca:
  l'app non spedisce nemmeno dall'interfaccia.
- **RT-8 — Dati personali (§10).** Il documento prodotto contiene dati personali del debitore ed è il documento più
  sensibile che l'app genera. Va nel manifesto come voce propria, con finalità «esercizio di un diritto» e base
  «legittimo interesse, difesa di un diritto in giudizio»; tabella presente in `exportData` e `purgeData`.
- **RT-9 — Registrazione eventi (§14).** Gli eventi «messa in mora generata» e «spedizione registrata» sono registrati
  con `tenant_id`, `app_id`, `user_id` e identificativo di correlazione, senza contenuti né importi.

## 4. Criteri di accettazione

**CA-1 — Generazione**
- **Dato** un credito in `in_escalation` da 3.000 €, con tre solleciti inviati e mora calcolata
- **Quando** l'utente genera la bozza
- **Allora** ottiene un documento con capitale, interessi, forfait, elenco dei tre solleciti con le loro date e termine
  per il pagamento

**CA-2 — Nessuna spedizione automatica**
- **Dato** una bozza generata · **Quando** si cerca un modo di spedirla dall'app · **Allora** non esiste, e
  l'interfaccia spiega che la spedizione è fuori dall'app e perché

**CA-3 — Totali congelati**
- **Dato** una messa in mora generata ieri · **Quando** oggi arriva un incasso parziale · **Allora** il documento
  generato ieri resta identico, e la scheda mostra che i suoi totali sono anteriori all'incasso

**CA-4 — Registrazione della spedizione**
- **Dato** una messa in mora generata · **Quando** l'utente registra «spedita il 12/09 per raccomandata» · **Allora** il
  dato compare nella cronologia del credito

**CA-5 — Ruolo insufficiente**
- **Dato** un utente con ruolo `member` · **Quando** tenta di generare la messa in mora · **Allora** riceve `403`

**CA-6 — Isolamento fra account**
- **Dato** due account `A` e `B` · **Quando** un utente di `A` tenta di generare la messa in mora su un credito di `B` ·
  **Allora** riceve l'errore di risorsa non trovata

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (backend, frontend, compliance);
- [ ] prove di **unità** sul congelamento dei totali e di **integrazione** sulla generazione del documento;
- [ ] prova di **isolamento fra account** e **matrice dei ruoli** sulle rotte introdotte;
- [ ] **prova end-to-end**: *coprire ora* — la generazione della bozza è l'atto finale del percorso `[J-CREDITI]`; voce
      registrata nel registro di copertura con proprietaria la storia `0031`;
- [ ] **traduzioni** dell'interfaccia presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato con `messa_in_mora`, presente in esportazione e cancellazione;
- [ ] **registro delle decisioni** compilato, in particolare sulla scelta di non spedire e sul congelamento dei totali;
- [ ] contratto degli **strumenti conversazionali**: funzione predisposta, contratto dichiarato in `0029`;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0020` | Senza il calcolo della mora la lettera è incompleta |
| storia `0017` | Il riepilogo dei solleciti già inviati viene dal registro |

## 7. Fuori ambito

- **La spedizione**, in qualunque forma: raccomandata, posta certificata, invio tramite legale. È un effetto
  irreversibile verso l'esterno con valore giuridico, e l'app si ferma prima.
- Il decreto ingiuntivo e ogni atto processuale: fuori dal perimetro dichiarato nel documento capofila §1.
- La validazione legale del testo del modello: è materia della revisione legale pre-go-live, non di questa storia.

## 8. Punti aperti

**Il testo del modello di messa in mora fornito con l'app** deve essere rivisto da un legale prima di essere messo a
disposizione dei clienti: un modello sbagliato distribuito a molti è un danno moltiplicato. Va nel registro della
revisione legale pre-go-live. **Decide lo sviluppatore** se fornire un modello o lasciare il campo vuoto con
l'istruzione di farselo scrivere.
