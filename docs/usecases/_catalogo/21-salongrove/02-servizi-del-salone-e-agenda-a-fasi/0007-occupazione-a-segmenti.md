# 0007 — Occupazione a segmenti

**Applicazione**: 21 — SalonGrove (`salone`) · **Epica**: 02 — Servizi del salone e agenda a fasi
**Storia**: `0007` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0006`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come chi sta alla reception di un salone con tre poltrone
> voglio che l'agenda mi proponga di mettere la signora Bianchi alle 15:20, mentre la signora Rossi è in posa
> così da fare cinque clienti in un pomeriggio invece di tre, senza rischiare di sovrapporre due persone sulla
> stessa poltrona.

**Contesto.** È il cuore tecnico del verticale, e l'unica storia che **cambia il motore** invece di aggiungergli
qualcosa accanto. Oggi una prenotazione è un intervallo continuo e il vincolo di non sovrapposizione sta nel
database (BookGrove, storia `0014`). Con i servizi a fasi una prenotazione diventa una **sequenza di segmenti di
occupazione**, uno per risorsa e per fase, e il vincolo va spostato lì. Farlo bene è la differenza fra un'agenda
che riempie la giornata e il difetto più imbarazzante possibile per un'app di prenotazioni: due persone sulla
stessa poltrona.

## 2. Requisiti funzionali

1. **RF-1** — Confermare una prenotazione di un servizio a fasi genera i **segmenti di occupazione**: uno per
   ciascuna coppia (fase, risorsa impegnata), con inizio e fine calcolati dalle durate.
2. **RF-2** — Il calcolo degli spazi liberi considera **liberi** i minuti in cui una risorsa non ha segmenti:
   l'operatore che ha una posa in corso risulta disponibile in quei minuti per un altro servizio.
3. **RF-3** — Una risorsa non può avere **due segmenti sovrapposti**: il vincolo vive nel database, non nel
   programma applicativo, e il conflitto si risolve dando torto a chi arriva secondo con un messaggio chiaro.
4. **RF-4** — L'agenda mostra la prenotazione come **un solo appuntamento** con dentro le sue fasi, non come tre
   appuntamenti separati: chi guarda deve vedere una cliente, non tre righe.
5. **RF-5** — Spostare o disdire una prenotazione muove o toglie **tutti** i suoi segmenti insieme: non esistono
   segmenti orfani, mai.
6. **RF-6** — Un salone può **spegnere l'intercalazione** per un operatore che non la vuole: in quel caso i suoi
   minuti di posa restano occupati come oggi. È l'interruttore che evita di imporre un modo di lavorare.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Segmenti e calcolo della disponibilità filtrano per `tenant_id` dal
  token verificato; **anche il calcolo eseguito per la pagina pubblica** resta legato all'account risolto dal
  server, mai a un identificativo che arrivi dalla richiesta.
- **RT-2 — Persistenza (§8).** Tabella `segmento_occupazione` con `tenant_id`, riferimento alla prenotazione e
  alla risorsa, inizio e fine in tempo universale coordinato, e **vincolo di esclusione nel database** che impedisce
  la sovrapposizione per la stessa risorsa. Chi tenta di aggirare il vincolo trova un errore, non un
  comportamento.
- **RT-3 — Interfaccia di programmazione (§2).** Le rotte esistenti di disponibilità e prenotazione restituiscono
  e accettano le fasi; errori di conflitto in `problem+json` con un codice riconoscibile che il frontend possa
  tradurre in un messaggio utile; OpenAPI aggiornata.
- **RT-4 — Fuso orario.** Ogni istante in tempo universale coordinato, con il fuso della sede: è il vincolo
  permanente dichiarato da BookGrove, e qui pesa il doppio perché un errore di un'ora moltiplicato per tre
  segmenti produce agende incomprensibili.
- **RT-5 — Modulo frontend (§3, §5).** L'agenda disegna l'appuntamento con le fasi tratteggiate dove l'operatore è
  libero; la ricerca di uno spazio libero mostra le proposte «dentro la posa» distinguendole visivamente. Solo
  token del sistema di design.
- **RT-6 — Cinque lingue (§4).** Messaggi di conflitto, etichette delle fasi in agenda e il testo
  dell'interruttore per operatore in tutte e cinque le lingue.
- **RT-7 — Dati personali (§10).** Nessun dato personale nuovo: un segmento è tempo e risorsa.
- **RT-8 — Registrazione eventi (§14).** `segmenti generati`, `conflitto di occupazione respinto` con
  `tenant_id`, `app_id`, `user_id` e correlazione — mai il nome del cliente.

## 4. Criteri di accettazione

**CA-1 — L'operatore è libero durante la posa**
- **Dato** un colore prenotato alle 14:00 (applicazione 20′, posa 35′, finitura 25′) con l'operatrice Sara
- **Quando** si cercano gli spazi liberi di Sara per un taglio da 30 minuti
- **Allora** fra le proposte compare uno spazio dentro i minuti di posa, e **non** compare uno spazio dentro
  l'applicazione o la finitura

**CA-2 — La postazione resta occupata**
- **Dato** lo stesso colore, che tiene la poltrona 1 per tutti gli 80 minuti
- **Quando** si cerca uno spazio per un servizio che richiede la poltrona 1
- **Allora** nessuna proposta cade dentro quegli 80 minuti

**CA-3 — Doppia prenotazione impossibile**
- **Dato** due richieste simultanee per lo stesso minuto sulla stessa poltrona
- **Quando** arrivano insieme, una dal banco e una dalla pagina pubblica
- **Allora** una sola riesce; l'altra riceve un errore di conflitto con un messaggio che invita a scegliere un
  altro orario, e **nessuna occupazione parziale resta** nel database

**CA-4 — Spostamento coerente**
- **Dato** un colore prenotato con tre segmenti
- **Quando** lo si sposta di un'ora
- **Allora** tutti e tre i segmenti si spostano insieme, e nessun segmento resta all'orario vecchio

**CA-5 — Intercalazione spenta**
- **Dato** un operatore per cui l'intercalazione è spenta
- **Quando** si cercano i suoi spazi liberi durante una posa
- **Allora** non ne compare nessuno: i minuti di posa risultano occupati

**CA-6 — Isolamento fra account**
- **Dato** due account con poltrone che portano lo stesso nome
- **Quando** un utente del primo chiede la disponibilità forzando l'identificativo della risorsa dell'altro
- **Allora** non ottiene nessuna informazione sull'agenda altrui

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (suite intera prima del commit: la storia tocca il motore);
- [ ] prove di **unità** sul calcolo dei segmenti e sull'incrocio con le regole di disponibilità;
- [ ] prova di **integrazione** che verifica il vincolo di non sovrapposizione **nel database**, con due
      transazioni concorrenti;
- [ ] prova di **isolamento fra account** su disponibilità e prenotazione;
- [ ] **prova end-to-end**: *coprire ora* — il percorso `[J-SALONGROVE]` (storia `0030`) prevede espressamente il
      passo «prenoto un secondo cliente dentro la posa del primo», e il registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) va aggiornato di conseguenza;
- [ ] **traduzioni** in tutte e cinque le lingue;
- [ ] **manifesto dei dati**: nessuna voce nuova;
- [ ] **registro delle decisioni**: vincolo nel database e non nel codice, comportamento del conflitto, spostamento
      atomico, interruttore per operatore;
- [ ] avvio locale invariato.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0006` | senza fasi non ci sono segmenti |
| calcolo degli spazi liberi di BookGrove (storia `0010` di quell'app) | è il motore che questa storia estende |

## 7. Fuori ambito

- la prenotazione dalla pagina pubblica: resta quella di BookGrove, che qui eredita il calcolo nuovo senza
  saperlo;
- l'ottimizzazione automatica dell'agenda («riempi tu i buchi»): non è in questa stesura, e sarebbe una storia a
  sé con un rischio suo — un'agenda che si riordina da sola spaventa chi la usa;
- la lista d'attesa: è di BookGrove.

## 8. Punti aperti

**L'intercalazione cambia il senso della mancata presentazione.** Se durante la posa della signora Rossi c'è la
signora Bianchi e la Rossi finisce prima, qualcuno aspetta. Il modello non lo sa e non lo può sapere: le durate
sono stime. Proposta: mostrare in agenda un indicatore di **rischio di attesa** quando due appuntamenti
condividono un operatore in minuti adiacenti, senza impedire niente. È una funzione di comodo e non ho elementi
per dire quanto serva: la lascio come punto aperto invece di inventarne i dettagli.
