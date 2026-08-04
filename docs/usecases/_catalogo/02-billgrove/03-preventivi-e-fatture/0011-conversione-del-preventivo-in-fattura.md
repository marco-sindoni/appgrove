# 0011 — Conversione del preventivo in fattura

**Applicazione**: 02 — BillGrove (`billing`) · **Epica**: 03 — Preventivi e fatture
**Storia**: `0011` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0010`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che ha appena ricevuto il «sì» del cliente
> voglio trasformare il preventivo accettato in una fattura con un gesto solo
> così da non ribattere righe, quantità e prezzi già concordati, e da avere sempre sott'occhio da quale offerta
> nasce quella fattura.

**Contesto.** È il punto di giunzione della catena del documento contabile e il momento in cui la suite mostra il
proprio valore: il dato non si ricopia. È anche una funzione piccola, che vale la pena tenere separata dalla
creazione della fattura perché ha regole proprie — che cosa si può convertire, che cosa succede se il preventivo
cambia dopo, che cosa resta legato a che cosa.

## 2. Requisiti funzionali

1. **RF-1** — Da un preventivo in stato `accettato` si può generare una **bozza** di fattura che ne ricopia
   cliente, righe, prezzi, sconti e aliquote.
2. **RF-2** — La fattura generata resta legata al preventivo d'origine, e il legame è visibile da entrambi i lati.
3. **RF-3** — La bozza generata è **modificabile** prima dell'emissione: la conversione è un punto di partenza, non
   un vincolo.
4. **RF-4** — Un preventivo già convertito non si converte una seconda volta, a meno di una conferma esplicita che
   dichiari che si tratta di una fatturazione parziale o ripetuta.
5. **RF-5** — Se il preventivo è in uno stato diverso da `accettato`, la conversione è rifiutata con un messaggio
   che spiega quale stato serve.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Preventivo di partenza e fattura generata appartengono allo stesso
  `tenant_id`, preso dal token verificato; convertire un preventivo di un altro account risponde `404`, non `403`:
  quel preventivo, per chi chiede, non esiste.
- **RT-2 — Interfaccia di programmazione (§2).** `POST /api/billing/v1/quotes/{id}/convert`; corpo validato; errori
  in `application/problem+json`; definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Nessuna tabella nuova: si valorizza il riferimento al documento d'origine già
  previsto dalla storia `0002`. L'operazione è **transazionale**: o nasce la fattura e il preventivo risulta
  convertito, o non succede niente.
- **RT-4 — Modulo frontend (§3, §5).** Azione «Converti in fattura» sul preventivo accettato, che porta alla bozza
  appena creata. Solo token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe visibili passano dallo spazio-nomi `billing` e sono presenti in
  `en, it, fr, es, de`.
- **RT-6 — Varchi e quota (§6, §7).** La conversione produce una **bozza**: non consuma quota, perché la quota si
  prenota all'emissione (storia `0012`). Va detto nel messaggio dell'interfaccia, altrimenti l'utente si aspetta di
  aver già consumato.
- **RT-7 — Esposizione conversazionale (§12).** Strumento dichiarato:
  `converti_preventivo_in_fattura(id_preventivo) → bozza di fattura`, marcato **scrittura**; produce una bozza e
  richiede conferma umana. Non emette e non invia nulla. Dipendenza dichiarata: UC 0061-0063.
- **RT-8 — Dati personali (§10).** Nessun dato personale nuovo: la fattura eredita il riferimento al cliente già
  dichiarato.
- **RT-9 — Registrazione eventi (§14).** L'evento `preventivo convertito in fattura` è registrato con `tenant_id`,
  `app_id`, `user_id`, identificativo di correlazione e i due identificativi dei documenti, senza dati personali.

## 4. Criteri di accettazione

**CA-1 — Conversione riuscita**
- **Dato** un preventivo `accettato` con tre righe
- **Quando** si chiede la conversione
- **Allora** nasce una fattura in stato `bozza` con le stesse tre righe, gli stessi prezzi e il legame al
  preventivo, e il preventivo risulta convertito

**CA-2 — Stato sbagliato**
- **Dato** un preventivo in stato `inviato` · **Quando** si chiede la conversione
- **Allora** la risposta è `409` con l'indicazione che serve lo stato `accettato`, e nulla viene creato

**CA-3 — Seconda conversione**
- **Dato** un preventivo già convertito · **Quando** si chiede una seconda conversione senza conferma esplicita
- **Allora** la risposta è `409`; con la conferma esplicita nasce una seconda bozza, e il preventivo risulta legato
  a due fatture

**CA-4 — La bozza resta modificabile**
- **Dato** la fattura appena generata · **Quando** si cambia la quantità di una riga
- **Allora** la modifica è accettata e il preventivo d'origine resta invariato

**CA-5 — Isolamento fra account**
- **Dato** un preventivo dell'account `B` · **Quando** un utente di `A` ne chiede la conversione
- **Allora** riceve `404`

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend` e `frontend`; l'intera suite prima del commit);
- [ ] prove di **unità** sulla copia delle righe e sulle regole di stato, di **integrazione** sulla rotta di
      conversione con database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** sulla conversione di un preventivo altrui;
- [ ] **prova end-to-end**: *coprire ora* — passo «converti il preventivo accettato» del percorso `[J-BILLING]`;
      registro di copertura aggiornato;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati**: nessuna voce nuova, dichiarato;
- [ ] **registro delle decisioni** compilato;
- [ ] contratto degli **strumenti conversazionali** dichiarato per `converti_preventivo_in_fattura`;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0010` | Serve un preventivo accettato da convertire |

## 7. Fuori ambito

- l'emissione e la numerazione della fattura generata: storia `0012`;
- la conversione di un documento di trasporto in fattura differita: storia `0015`;
- la fatturazione parziale a stati di avanzamento: rimandata, non è un bisogno rilevato nel segmento micro.

## 8. Punti aperti

Nessuno.
