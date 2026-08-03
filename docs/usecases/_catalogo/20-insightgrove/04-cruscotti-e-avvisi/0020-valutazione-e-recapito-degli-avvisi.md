# 0020 — Valutazione e recapito degli avvisi

**Applicazione**: 20 — InsightGrove (`insights`) · **Epica**: 04 — Cruscotti e avvisi
**Storia**: `0020` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0016`, `0019`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che ha impostato un avviso sui crediti scaduti
> voglio che mi arrivi quando serve, e **non** mi arrivi quando il numero su cui si basa è incompleto
> così da poter dare peso a quello che ricevo, invece di imparare a ignorarlo.

**Contesto.** Qui sta il problema difficile degli avvisi, e non è la spedizione. Un avviso che suona su un numero
calcolato mentre una fonte è silente sta segnalando un guasto dei dati travestito da fatto aziendale — «il
fatturato è crollato» quando in realtà la fatturazione ha smesso di parlare. È l'errore plausibile del §2.5 della
[descrizione](../application-description.md), nella sua forma più dannosa: arriva in casella, sembra un allarme
vero, e chi lo riceve agisce. La regola di questa storia è quindi netta: **su un numero incompleto un avviso non
suona — segnala il guasto**.

## 2. Requisiti funzionali

1. **RF-1** — Gli avvisi attivi vengono valutati con la cadenza del proprio periodo (giornaliera, settimanale o
   mensile), a un'ora dichiarata e nel fuso orario dell'account.
2. **RF-2** — La valutazione calcola il valore con lo stesso motore dei cruscotti (storia 0015) e ne ottiene
   anche il **grado di completezza** (storia 0016).
3. **RF-3** — Se il valore è `completo` e la condizione è soddisfatta, l'avviso **scatta** e viene recapitato ai
   destinatari.
4. **RF-4** — Se il valore è `parziale` o `non calcolabile`, l'avviso **non scatta**: viene registrato come «non
   valutabile» con il motivo, e ai soli `owner` e `admin` arriva — al massimo una volta per episodio — un
   messaggio di **guasto del dato**, che è una cosa diversa da un allarme aziendale e va scritto in modo che non
   si confondano.
5. **RF-5** — Il messaggio recapitato contiene: metrica, periodo, valore rilevato, soglia, il momento
   dell'ultimo dato e un collegamento che porta alla **scheda del numero** dentro il backoffice.
6. **RF-6** — Il recapito è **idempotente**: un tentativo ripetuto per la stessa valutazione non produce un
   secondo messaggio.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** La valutazione gira per account, e il calcolo legge i fatti con
  `WHERE tenant_id = :tid`; nessuna valutazione può leggere dati di un account diverso da quello dell'avviso.
- **RT-2 — Interfaccia di programmazione (§2).** Nessuna rotta pubblica nuova: la valutazione è una lavorazione
  programmata. Il collegamento nel messaggio porta a una schermata del backoffice, non a una risorsa pubblica.
- **RT-3 — Persistenza (§8).** Tabella `scatto_avviso` con `tenant_id`, avviso, momento, valore rilevato, soglia,
  esito (scattato, non valutabile, recapitato, fallito) e riferimento alla traccia del calcolo.
- **RT-5 — Cinque lingue (§4).** Il messaggio recapitato è nella lingua dell'account, fra `en, it, fr, es, de`.
- **RT-6 — Varchi e ruoli (§6).** Un avviso su metrica economica non viene recapitato a un destinatario interno
  che non avrebbe il ruolo per vedere quella metrica: il recapito **non è una scorciatoia** al controllo di
  ruolo. L'unica eccezione è l'indirizzo esterno confermato esplicitamente (storia 0019).
  Con abbonamento `canceled` la valutazione si ferma; con `past_due` continua. **Nessun consumo di quota**.
- **RT-8 — Dati personali (§10).** Il messaggio è recapitato a indirizzi già dichiarati nel manifesto; il
  contenuto è un numero e un periodo, **nessun dato personale**.
- **RT-14 — Registrazione eventi (§14).** «Avviso valutato», «avviso scattato», «avviso non valutabile»,
  «recapito fallito» con `tenant_id`, `app_id`, identificativo dell'avviso e identificativo di correlazione;
  **mai** gli indirizzi né il contenuto del messaggio.
- **RT-11 — Prove (§11).** Prove con **tempo controllato** e servizio di posta simulato; nessuna attesa reale.

## 4. Criteri di accettazione

**CA-1 — L'avviso scatta**
- **Dato** un avviso «crediti scaduti maggiori di 10.000 €» e un valore completo di 12.400 €
- **Quando** arriva l'ora di valutazione
- **Allora** l'avviso scatta, il messaggio parte verso i destinatari e contiene valore, soglia, periodo, momento
  dell'ultimo dato e il collegamento alla scheda del numero

**CA-2 — Su numero incompleto non suona**
- **Dato** lo stesso avviso, e la fonte incassi silente da sei giorni
- **Quando** arriva l'ora di valutazione
- **Allora** l'avviso **non** scatta; viene registrato «non valutabile — fonte incassi silente dal …»; ai soli
  `owner` e `admin` arriva un messaggio di guasto del dato, distinto per titolo e testo da un allarme aziendale

**CA-3 — Il guasto si segnala una volta sola**
- **Dato** una fonte silente da dieci giorni con lo stesso avviso valutato ogni giorno
- **Quando** passano i giorni
- **Allora** il messaggio di guasto è stato mandato una volta sola; quando la fonte torna in linea, l'episodio si
  chiude

**CA-4 — Il recapito non scavalca il ruolo**
- **Dato** un avviso su metrica economica con fra i destinatari un utente `member` dell'account
- **Quando** l'avviso scatta
- **Allora** quel destinatario **non** riceve il messaggio, e l'interfaccia dell'avviso segnala il destinatario
  non recapitabile con il motivo

**CA-5 — Idempotenza del recapito**
- **Dato** una valutazione già recapitata
- **Quando** la lavorazione viene rieseguita per un guasto
- **Allora** non parte un secondo messaggio

**CA-6 — Isolamento fra account**
- **Dato** due account con avvisi sulla stessa metrica e soglia
- **Quando** entrambi vengono valutati
- **Allora** ciascuno usa i propri fatti, e nessun destinatario riceve il valore dell'altro account

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno le aree toccate; l'intera suite prima del commit);
- [ ] prove di **unità** sulla decisione «scatta / non valutabile» al variare della completezza, e di
      **integrazione** sulla lavorazione programmata con tempo controllato e posta simulata;
- [ ] prova di **isolamento fra account** e **matrice dei ruoli** sul recapito;
- [ ] **prova end-to-end**: *coprire ora* — il percorso `[J-INSIGHTS]` include «con una fonte silente l'avviso
      non scatta»; registro di copertura aggiornato;
- [ ] **traduzioni** presenti in tutte e cinque le lingue per il messaggio e per il messaggio di guasto;
- [ ] **manifesto dei dati**: nessuna voce nuova oltre a quelle della storia 0019;
- [ ] **registro delle decisioni** compilato, con la regola «su numero incompleto non si suona» e il perché;
- [ ] contratto degli **strumenti conversazionali**: nessuna funzione nuova;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0016` | il grado di completezza è ciò che decide se l'avviso può scattare |
| storia `0019` | serve la definizione dell'avviso |
| storia `0010` | lo stato di salute delle fonti alimenta la completezza |

## 7. Fuori ambito

- il registro degli scatti come schermata e la sospensione manuale: storia 0021;
- canali di recapito diversi dalla posta elettronica.

## 8. Punti aperti

- **A che ora si valuta?** Un'ora fissa (per esempio le 07:00 nel fuso dell'account) è semplice e prevedibile.
  Chiude: **sviluppatore**.
- **Il messaggio di guasto del dato è di InsightGrove o di piattaforma?** Una fonte silente riguarda anche
  l'app sorgente, e mandare due messaggi diversi per lo stesso guasto è confusionario. Oggi non esiste un
  meccanismo di piattaforma. Chiude: **piattaforma**, quando esisterà; nel frattempo lo manda questa app.
