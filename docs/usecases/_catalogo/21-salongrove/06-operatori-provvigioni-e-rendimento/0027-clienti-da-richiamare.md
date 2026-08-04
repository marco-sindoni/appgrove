# 0027 — Clienti da richiamare

**Applicazione**: 21 — SalonGrove (`salone`) · **Epica**: 06 — Operatori, provvigioni e rendimento
**Storia**: `0027` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0010`, `0019`, `0026`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che si accorge tardi quando una cliente abituale smette di venire
> voglio un elenco di chi non torna da più tempo del solito, con l'ultimo servizio fatto
> così da poter fare quattro telefonate mirate invece di una campagna a tutti.

**Contesto.** Il colore si rifà ogni cinque-sei settimane, il taglio ogni sei-otto: nel beauty l'assenza ha un
**ritmo atteso**, e una cliente che salta due giri non è un caso, è un cliente perso. Oggi il salone se ne accorge
per caso, mesi dopo. Questa storia produce **l'elenco**, e si ferma lì: l'invio è di chi possiede il canale (§1 e
§10 della [descrizione](../application-description.md) — SalonGrove non fa campagne, e la scheda di catalogo
chiedeva un `send_winback` che qui **diventa deliberatamente una lettura**, §7).

⚠️ **Perché l'invio resta fuori.** Mandare per sbaglio un messaggio a duecento clienti non si annulla, costa denaro
al salone e credibilità con i suoi clienti. Un elenco si guarda prima di usarlo; una spedizione no.

## 2. Requisiti funzionali

1. **RF-1** — L'elenco mostra i clienti il cui **ultimo servizio** è più vecchio di una soglia in giorni, con:
   ultima data, ultimo servizio, operatore che l'aveva eseguito, valore medio dei conti.
2. **RF-2** — La soglia è **per servizio**, con un valore predefinito sensato per ciascuno (proposta: due volte
   l'intervallo tipico di ripetizione osservato per quel cliente, e in mancanza di storia una soglia unica
   dell'account): la funzione deve **funzionare a vuoto**, senza configurazione (§2.5 della descrizione).
3. **RF-3** — Dall'elenco si escludono: chi ha un appuntamento **futuro** già preso, chi ha chiesto di non essere
   contattato, chi è stato messo in elenco e già richiamato entro un numero di giorni impostabile.
4. **RF-4** — Ogni voce si può **marcare come gestita** con un esito («richiamata», «non risponde», «non
   interessata»), e la marcatura la toglie dall'elenco per il periodo scelto.
5. **RF-5** — L'elenco si **esporta** come tabella con i soli dati necessari a richiamare, e l'esportazione porta a
   schermo l'avvertenza che l'invio, il consenso e la base giuridica del contatto sono responsabilità del salone.
6. **RF-6** — L'applicazione **non invia niente**: nessun messaggio, nessuna posta, nessuna consegna a un canale.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** L'elenco si calcola filtrando per `tenant_id` preso dal token verificato;
  un identificativo di cliente di un altro account non è raggiungibile in nessun modo.
- **RT-2 — Interfaccia di programmazione (§2).** `GET /api/<app>/v1/clienti-da-richiamare?giorni=&servizio=`,
  `POST /api/<app>/v1/clienti-da-richiamare/{cliente}/esito`, `GET /api/<app>/v1/clienti-da-richiamare/export`;
  corpo validato; errori in `application/problem+json`; **nessuna rotta di invio**; OpenAPI aggiornata.
- **RT-3 — Persistenza (§8).** Migrazione sullo schema dell'app: tabella `richiamo_esito` (cliente, data, esito,
  chi) con `tenant_id`, UUID versione 7, colonne di controllo e cancellazione logica. L'elenco in sé è una
  **lettura derivata**: non si materializza, così non si disallinea.
- **RT-4 — Modulo frontend (§3, §5).** Sezione *Andamento*, riquadro *Da richiamare*: elenco corto, ordinato per
  quanto manca dall'ultimo servizio, con l'azione di marcatura a un tocco. Solo token del sistema di design; tema
  chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Etichette, esiti, avvertenza sull'esportazione e testi delle soglie presenti in
  `en, it, fr, es, de`.
- **RT-6 — Varchi e quota (§6, §7).** Nessun consumo di quota. Sezione accesa dal piano `sede`; a piano inferiore
  `402`.
- **RT-7 — Dati personali (§10).** Nessun campo nuovo sul cliente, ma un **uso nuovo**: selezionare le persone in
  base al comportamento d'acquisto. Va scritto nel manifesto in italiano e inglese — voce `richiamo_esito`
  (interessato = il cliente del salone; finalità «ricontattare chi non torna»; base «legittimo interesse del
  salone alla relazione con il proprio cliente», valutata dal salone come titolare; durata proposta 12 mesi) — e va
  dichiarato che l'elenco **rispetta la volontà di non essere contattati** già registrata sull'anagrafica. Campi
  annotati `@PersonalData`; tabella in esportazione e cancellazione (storie `0014` e `0032`).
- **RT-8 — Esposizione conversazionale (§12).** `clienti_da_richiamare(giorni_di_assenza, servizio?) → elenco
  minimizzato`, marcato **lettura**: nome, ultima data, ultimo servizio. Nessun recapito nel risultato e
  **nessuno strumento di invio**, né qui né altrove (storie `0028` e `0029`).
- **RT-9 — Registrazione eventi (§14).** `elenco richiami consultato`, `elenco richiami esportato` con `tenant_id`,
  `app_id`, `user_id`, correlazione e **conteggio** — mai i nomi delle persone in elenco.

## 4. Criteri di accettazione

**CA-1 — Chi manca da troppo tempo**
- **Dato** una cliente il cui ultimo colore risale a 110 giorni fa, con soglia a 84 giorni
- **Quando** si apre l'elenco
- **Allora** compare con l'ultima data, l'ultimo servizio e l'operatore che l'aveva eseguita

**CA-2 — Chi ha già un appuntamento non compare**
- **Dato** la stessa cliente, con un appuntamento fra dieci giorni
- **Quando** si apre l'elenco
- **Allora** non compare

**CA-3 — Chi non vuole essere contattato non compare**
- **Dato** un cliente che ha chiesto di non essere contattato
- **Quando** si apre l'elenco e quando lo si esporta
- **Allora** non compare in nessuno dei due, ed è una regola provata, non una consuetudine

**CA-4 — La marcatura toglie dall'elenco**
- **Dato** una cliente in elenco · **Quando** la si marca «richiamata»
- **Allora** esce dall'elenco per il periodo impostato e resta la traccia dell'esito con la data

**CA-5 — Nessun invio, da nessuna parte**
- **Dato** l'applicazione completa
- **Quando** si cercano rotte, azioni di interfaccia e strumenti conversazionali che spediscano un messaggio
- **Allora** non ne esiste nessuno, e l'esportazione mostra l'avvertenza sulle responsabilità del salone

**CA-6 — Isolamento fra account**
- **Dato** due account con clienti inattivi
- **Quando** l'uno apre l'elenco
- **Allora** vede solo i propri, anche forzando l'identificativo di un cliente dell'altro

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend` e `frontend`; l'intera suite prima del commit);
- [ ] prove di **unità** sul calcolo delle soglie e sulle esclusioni; di **integrazione** sulle rotte con database
      effimero e migrazioni vere;
- [ ] prova **negativa** che non esista alcuna via d'invio, compresa l'assenza di uno strumento conversazionale di
      scrittura;
- [ ] prova di **isolamento fra account**;
- [ ] **prova end-to-end**: *rimando* — passo facoltativo del percorso `[J-SALONGROVE]` (storia `0030`), che apre
      l'elenco e verifica un'esclusione; registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) aggiornato con la risposta;
- [ ] **traduzioni** in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese, con la finalità e il rispetto della volontà di non
      essere contattati;
- [ ] **registro delle decisioni**: soglie predefinite, esclusioni, rifiuto dell'invio e sua motivazione;
- [ ] avvio locale invariato; il salone di prova ha almeno una cliente inattiva e una esclusa.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0010` | l'ultimo servizio e la sua data vengono dalla scheda tecnica e dallo storico |
| storia `0019` | il valore medio dei conti viene dai conti chiusi |
| storia `0026` | condivide la sezione *Andamento* e il suo varco di piano |
| preferenze di contatto dell'anagrafica (app 07 BookGrove) | l'esclusione di chi non vuole essere contattato poggia su un dato che vive là |

## 7. Fuori ambito

- l'**invio** dei messaggi: è dell'app che possiede il canale (16 ReachGrove, o il canale dei promemoria già usato
  da BookGrove);
- la **campagna** con testi, segmenti e misurazione: non è questa applicazione e non lo diventa;
- la **previsione** di quando un cliente tornerà: sarebbe una profilazione, con tutto ciò che comporta; qui si
  guarda un fatto passato, non si prevede un comportamento.

## 8. Punti aperti

**La soglia adattiva è una profilazione leggera.** Calcolare la soglia dal ritmo osservato del singolo cliente è
più utile di una soglia unica, ma è un trattamento che deduce un comportamento dalla storia della persona. La
proposta è di ammetterla perché produce **solo un ordinamento interno di un elenco che una persona legge**, senza
decisioni automatizzate. Va confermato insieme al manifesto.

**La base giuridica del contatto è del salone, non nostra.** Noi produciamo un elenco; chi telefona o scrive è il
salone, che è titolare del trattamento verso i suoi clienti. L'avvertenza a schermo lo dice, ma è una scelta che
merita una riga nelle condizioni d'uso.
