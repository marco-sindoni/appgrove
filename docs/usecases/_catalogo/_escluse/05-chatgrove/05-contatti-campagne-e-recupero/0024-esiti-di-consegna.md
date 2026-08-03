# 0024 — Esiti di consegna

**Applicazione**: 05 — ChatGrove (`chat_commerce`) · **Epica**: 05 — Contatti, campagne e recupero
**Storia**: `0024` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0007`, `0023`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare di un negozio
> voglio sapere se i miei messaggi sono arrivati e se sono stati letti
> così da capire se è valsa la pena, e da accorgermi se qualcosa si è rotto.

**Contesto.** Un invio senza esiti è un salto nel buio: il negozio non sa se il problema è il messaggio o il
canale. Gli esiti servono anche a noi, perché un tasso di fallimento improvviso è il primo segnale che la
connessione ha un problema — è la stessa informazione che serve alla console di amministrazione per rispondere
a «perché non arrivano più i miei messaggi?».

## 2. Requisiti funzionali

1. **RF-1** — Il canale notifica i cambi di stato dei messaggi inviati (inviato, consegnato, letto, fallito
   con motivo) e l'app li registra sul messaggio corrispondente.
2. **RF-2** — La notifica di stato è **idempotente** e rispetta l'ordine: un esito più arretrato non
   sovrascrive uno più avanzato arrivato prima.
3. **RF-3** — La scheda della campagna mostra i conteggi per esito e la percentuale di consegna e di lettura.
4. **RF-4** — L'esito per singolo destinatario è consultabile e filtrabile per stato, così da poter richiamare
   chi non ha ricevuto.
5. **RF-5** — I messaggi falliti riportano il motivo in parole comprensibili, non il codice del fornitore.
6. **RF-6** — La pagina d'atterraggio segnala se la percentuale di fallimento delle ultime 24 ore supera una
   soglia: è il sintomo di un problema di connessione.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** L'esito si associa all'account tramite la **connessione** che ha
  ricevuto la notifica, mai tramite un identificativo presente nel corpo; ogni lettura filtra per `tenant_id`
  preso dal token verificato.
- **RT-2 — Interfaccia di programmazione (§2).** Le notifiche di stato arrivano sulla stessa rotta di
  ricezione della storia `0007`, con la stessa verifica di firma. Rotte di lettura
  `GET /api/chat_commerce/v1/campaigns/{id}/deliveries`, paginate e filtrabili; errori in
  `application/problem+json`; definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V17__esiti.sql`: colonne di stato e di motivo su `message` e
  `campaign_delivery`. L'aggiornamento è idempotente e monotono: si applica solo se lo stato in arrivo è più
  avanzato di quello registrato.
- **RT-4 — Dati personali (§10).** Nessun campo nuovo che riguardi una persona oltre a quelli già dichiarati:
  gli esiti sono metadati sui messaggi. Il motivo del fallimento **non** deve contenere il numero di telefono.
- **RT-5 — Modulo frontend (§3, §4, §5).** Scheda della campagna con conteggi e elenco filtrabile; avviso
  sulla pagina d'atterraggio. Tutte le stringhe, **compresi i motivi di fallimento tradotti**, in
  `en, it, fr, es, de`.
- **RT-6 — Registrazione eventi (§14).** `esito ricevuto` (con lo stato), `soglia di fallimento superata` con
  `tenant_id`, `app_id` e identificativo di correlazione, senza numeri né contenuti.

## 4. Criteri di accettazione

**CA-1 — Gli esiti arrivano**
- **Dato** una campagna con 112 messaggi inviati
- **Quando** il canale notifica 100 consegne e 60 letture
- **Allora** la scheda della campagna mostra 100 consegnati e 60 letti, con le percentuali

**CA-2 — Ordine rispettato**
- **Dato** un messaggio già segnato «letto»
- **Quando** arriva in ritardo la notifica «consegnato» dello stesso messaggio
- **Allora** lo stato resta «letto»

**CA-3 — Ripetizione**
- **Dato** la stessa notifica ricevuta tre volte · **Quando** viene elaborata · **Allora** i conteggi non
  cambiano dopo la prima

**CA-4 — Motivo comprensibile**
- **Dato** un messaggio fallito perché il numero non è attivo sul canale
- **Quando** si apre l'elenco degli esiti · **Allora** si legge una spiegazione in lingua, non un codice

**CA-5 — Allarme di fallimento**
- **Dato** una percentuale di fallimenti oltre la soglia nelle ultime 24 ore
- **Quando** si apre la pagina d'atterraggio · **Allora** compare l'avviso con il rimando alla connessione

**CA-6 — Isolamento fra account**
- **Dato** due account · **Quando** arriva un esito per un messaggio di `A` · **Allora** i conteggi di `B` non
  cambiano e un utente di `B` non vede quell'esito

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (backend e frontend; l'intera suite prima del commit);
- [ ] prove di **unità** sulla monotonia degli stati e di **integrazione** sulla ricezione degli esiti con il
      canale simulato;
- [ ] prova di **isolamento fra account** sugli esiti;
- [ ] **prova end-to-end**: *rimando* alla storia `0029`;
- [ ] **traduzioni** presenti in tutte e cinque le lingue, compresi i motivi di fallimento;
- [ ] **manifesto dei dati**: verificato che i motivi di fallimento non contengano numeri di telefono;
- [ ] **registro delle decisioni** compilato, con la regola della monotonia degli stati;
- [ ] contratto degli **strumenti conversazionali**: gli esiti sono esposti in **lettura** dentro il riepilogo
      della campagna;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| `0007` | Riusa la rotta di ricezione e la verifica della firma |
| `0023` | Gli esiti si aggregano per campagna |

## 7. Fuori ambito

- il punteggio di qualità del numero fornito dal canale: se il fornitore lo espone, è una storia a sé; qui si
  usa il tasso di fallimento come sintomo;
- le statistiche di vendita per campagna (quanti hanno poi comprato): utile, ma richiede di legare campagna e
  ordine; è una storia futura.

## 8. Punti aperti

- **Quali esiti il fornitore renda davvero disponibili** dipende dal rivenditore e dalle impostazioni di
  riservatezza del destinatario (la conferma di lettura può essere disattivata dall'utente finale). L'app deve
  reggere il caso in cui la lettura non arrivi mai: proposto di mostrare «non disponibile» invece di zero, per
  non far credere al negozio che nessuno legga i suoi messaggi.
