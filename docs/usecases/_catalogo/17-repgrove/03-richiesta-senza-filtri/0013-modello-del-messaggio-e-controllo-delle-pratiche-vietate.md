# 0013 — Modello del messaggio e controllo delle pratiche vietate

**Applicazione**: 17 — RepGrove (`recensioni`) · **Epica**: 03 — Richiesta di recensione senza filtri
**Storia**: `0013` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0012`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che scrive il messaggio con cui chiede la recensione
> voglio poterlo scrivere con parole mie, ma essere fermato se sto scrivendo qualcosa di vietato
> così da non prendere una sanzione per una frase che mi sembrava gentile.

**Contesto.** Il testo dell'invito è il punto in cui un cliente in buona fede si mette nei guai. «Lasciaci una
recensione e la prossima volta ti offriamo il caffè» sembra ospitalità: è un incentivo, vietato da Google, vietato
da Trustpilot e sanzionato in Italia da 500 a 5.000 euro (descrizione §2.3). «Se ti sei trovato bene con Marco,
scrivi il suo nome» sembra un complimento a un dipendente: Google vieta espressamente di chiedere che la
recensione contenga contenuti specifici, compreso il nome di un membro del personale.

Questa storia mette un controllo **prima** che il testo venga usato, non dopo. Il controllo non è un
suggerimento: **respinge** il modello e non lo rende utilizzabile, spiegando quale regola violerebbe. Ed è un
presidio onesto solo se ammette di essere imperfetto: nessun controllo automatico su un testo libero è completo, e
l'app deve dirlo invece di far credere il contrario.

## 2. Requisiti funzionali

1. **RF-1** — Per ogni sede esiste un modello di messaggio per lingua, con oggetto e corpo, campi variabili
   consentiti (nome del cliente, nome della sede, collegamento alla recensione) e anteprima.
2. **RF-2** — L'app parte con un modello **predefinito conforme** per ognuna delle cinque lingue: la maggior parte
   dei clienti non lo cambierà, e il valore predefinito è la cosa che li protegge di più.
3. **RF-3** — Alla richiesta di approvazione, il modello passa un **controllo delle pratiche vietate** che
   respinge il testo quando riconosce: promesse di vantaggi (sconto, omaggio, buono, premio, concorso, punti);
   richieste di contenuto specifico (citare una persona, un prodotto, una parola); richieste di un voto
   determinato («cinque stelle»); condizioni («se ti sei trovato bene…»); rimandi a un canale privato in
   alternativa alla recensione.
4. **RF-4** — Il messaggio di rifiuto dice **quale frase** ha fatto scattare il controllo, **quale regola**
   violerebbe (con il riferimento alla piattaforma o alla legge) e **come si riscrive**. Un rifiuto senza rimedio
   è un rifiuto inutile.
5. **RF-5** — Il controllo dichiara i propri limiti nell'interfaccia: riconosce le formulazioni comuni nelle
   cinque lingue, non tutte; la responsabilità del testo resta del cliente. Nessuna promessa di conformità
   garantita.
6. **RF-6** — Solo un modello in stato `approvato` può essere usato per inviare (storia 0014). Un modello
   respinto resta modificabile e conserva l'esito del controllo.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura e scrittura di `modello_di_messaggio` filtra per
  `tenant_id` preso dal token verificato.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `GET|PUT /api/recensioni/v1/sedi/{id}/modelli/{lingua}` e
  `POST /api/recensioni/v1/modelli/{id}/approva`; il rifiuto esce come `422` in `application/problem+json` con
  l'elenco strutturato dei riscontri (frase, regola violata, suggerimento); definizione OpenAPI aggiornata nello
  stesso commit.
- **RT-3 — Persistenza (§8).** Tabella `modello_di_messaggio` (storia 0002) con lo stato e l'esito dell'ultimo
  controllo; la storia degli esiti si conserva, perché è la prova di aver respinto (descrizione §11, rischi).
- **RT-4 — Modulo frontend (§3, §5).** Sezione *Impostazioni* → «Messaggio di invito»: modifica per lingua,
  anteprima con dati finti, riquadro dell'esito del controllo con le frasi evidenziate. Solo token del sistema di
  design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Doppio senso: l'interfaccia è in cinque lingue **e** i modelli si scrivono in
  cinque lingue. Il modello predefinito conforme esiste per tutte e cinque; le regole del controllo coprono tutte
  e cinque, e dove non coprono lo dicono.
- **RT-6 — Varchi e quota (§6, §7).** Approvare un modello richiede ruolo `admin` o `owner`; `402` con
  abbonamento non attivo. Nessun consumo di quota.
- **RT-7 — Esposizione conversazionale (§12).** Uno strumento può **leggere** il modello e **proporne** una
  riscrittura come bozza, ma l'approvazione resta umana: è la stessa regola della risposta pubblica (storia 0019).
  Uno strumento che approvasse un modello da solo aggirerebbe l'unico presidio della storia.
- **RT-8 — Dati personali (§10).** Nessun dato personale nuovo: il modello contiene campi variabili, non persone.
  L'anteprima usa dati **inventati**.
- **RT-9 — Registrazione eventi (§14).** `modello approvato`, `modello respinto` con le regole violate (i codici,
  non le frasi), con `tenant_id`, `app_id`, `user_id` e identificativo di correlazione. Le frasi del cliente non
  finiscono nei registri.

## 4. Criteri di accettazione

**CA-1 — Il modello predefinito passa**
- **Dato** una sede appena creata
- **Quando** si guarda il modello predefinito in ognuna delle cinque lingue
- **Allora** è già in stato `approvato` e utilizzabile senza modifiche

**CA-2 — Incentivo respinto**
- **Dato** un modello che contiene «lasciaci una recensione e ricevi il 10% di sconto»
- **Quando** si chiede l'approvazione
- **Allora** la richiesta è respinta con `422`, il messaggio indica la frase, cita il divieto di incentivi delle
  piattaforme e propone una riformulazione, e il modello resta non utilizzabile

**CA-3 — Richiesta di contenuto specifico respinta**
- **Dato** un modello che contiene «scrivi che ti ha servito Marco»
- **Quando** si chiede l'approvazione
- **Allora** è respinto, con il riferimento al divieto di chiedere contenuti specifici

**CA-4 — Filtro travestito respinto**
- **Dato** un modello che contiene «se ti sei trovato bene lascia una recensione, altrimenti scrivici in privato»
- **Quando** si chiede l'approvazione
- **Allora** è respinto, con il riferimento al divieto di sollecitazione selettiva

**CA-5 — Un modello non approvato non invia**
- **Dato** un modello in stato `respinto`
- **Quando** si tenta di inviare un invito che lo userebbe
- **Allora** l'invio è rifiutato con un messaggio che rimanda alla schermata del modello

**CA-6 — Isolamento fra account**
- **Dato** due account con modelli diversi
- **Quando** un utente di `A` chiede il modello di `B`
- **Allora** riceve `404`, anche forzando l'identificativo dell'account nella richiesta

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno le aree toccate; l'intera suite prima del commit);
- [ ] prove di **unità** sul controllo, con una raccolta di frasi vietate e frasi lecite **nelle cinque lingue**,
      e verifica dei falsi positivi più probabili («la ringraziamo per la sua visita» non deve essere respinta);
- [ ] prova di **isolamento fra account** sui modelli;
- [ ] **prova end-to-end**: *coprire ora* il passo «il modello con un incentivo viene respinto» nel percorso
      `[J-RECENSIONI]`, e registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) aggiornato;
- [ ] **traduzioni** presenti in tutte e cinque le lingue, modelli predefiniti compresi;
- [ ] **manifesto dei dati**: nessuna voce nuova;
- [ ] **registro delle decisioni** compilato, con l'elenco delle regole del controllo e le fonti di ciascuna;
- [ ] contratto degli **strumenti conversazionali**: riscrittura assistita come **bozza**, approvazione umana.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0012` | la regola di equità decide **chi**, il modello decide **cosa gli si dice**: la seconda ha senso dopo la prima |

## 7. Fuori ambito

- l'invio — storia 0014;
- il testo del **sollecito**, che riusa lo stesso modello con una variante — storia 0015;
- la generazione assistita del testo dell'invito: si può fare, ma passa dallo stesso controllo. Se servisse una
  funzione dedicata, è una storia nuova.

## 8. Punti aperti

- **Il controllo automatico è imperfetto per costruzione.** Riconosce le formulazioni comuni, non tutte. Il modo
  in cui questo limite viene dichiarato all'utente è una scelta di prodotto e di responsabilità: la mia proposta
  è dirlo in una riga sotto il riquadro dell'esito, senza enfasi e senza scuse.
- **Falsi positivi.** Un controllo troppo severo che respinge testi leciti fa disattivare il prodotto nella testa
  del cliente. La raccolta di prova delle frasi lecite conta quanto quella delle frasi vietate.
- **Se un cliente insistesse** modificando il testo dopo l'approvazione attraverso un'altra via: non esiste
  un'altra via, ma la console di amministrazione tiene traccia dei modelli respinti proprio per accorgersi di chi
  ci prova ([estensioni-admin.md](../estensioni-admin.md)).
</content>
