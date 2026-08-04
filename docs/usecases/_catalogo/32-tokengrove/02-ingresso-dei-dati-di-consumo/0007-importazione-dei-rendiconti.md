# 0007 — Importazione dei rendiconti del fornitore

**Applicazione**: 32 — TokenGrove (`spesa_modelli`) · **Epica**: 02 — Ingresso dei dati di consumo
**Storia**: `0007` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0006`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come responsabile tecnico che ha appena collegato la propria chiave
> voglio che TokenGrove vada a prendersi da solo il consumo e il costo, anche quello dei mesi scorsi
> così da avere una serie storica da guardare fin dal primo giorno, invece di dover aspettare un mese.

**Contesto.** Collegata la fonte, qualcuno deve andare a leggerla. Le interfacce dei due fornitori principali hanno
forme simili e limiti precisi che vanno rispettati, non aggirati: il consumo si legge a intervalli di minuto, ora o
giorno; il **costo solo a giorno**; i risultati sono paginati; e Anthropic dichiara che l'interrogazione sostenibile
è **una al minuto** per organizzazione (§2.6, fonte 1). Chi scrive questa storia scrive di fatto un adattatore per
fornitore: farlo bene una volta evita di riscriverlo tre volte.

## 2. Requisiti funzionali

1. **RF-1** — Per ogni fonte attiva esiste una lavorazione periodica che recupera i nuovi dati di consumo e di
   costo dall'ultimo istante importato in poi, mantenendo un cursore per fonte.
2. **RF-2** — Al primo collegamento la fonte recupera lo **storico** fino alla profondità configurata dalla
   piattaforma (predefinita 90 giorni), a lotti, senza bloccare il resto dell'app e mostrando l'avanzamento.
3. **RF-3** — Il recupero rispetta i limiti di interrogazione del fornitore: al massimo una chiamata al minuto per
   fonte, con attesa progressiva e ripresa dal cursore in caso di rifiuto per eccesso di richieste.
4. **RF-4** — Ogni riga importata diventa una `misura` con origine «rendiconto», il suo identificativo esterno, il
   modello, i conteggi e — dove il fornitore lo dà — l'importo dichiarato. Le righe di costo giornaliere diventano
   invece `rendiconto`, che è la verità di fatturazione.
5. **RF-5** — Una lavorazione interrotta a metà riprende da dove era arrivata e **non duplica**: rieseguirla sullo
   stesso intervallo produce lo stesso risultato.
6. **RF-6** — L'utente può chiedere una ripetizione manuale del recupero su un intervallo di giorni, entro i limiti
   della propria quota.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** La lavorazione opera per fonte e quindi per `tenant_id`; ogni scrittura
  porta il `tenant_id` della fonte. Nessuna interrogazione senza filtro.
- **RT-2 — Interfaccia di programmazione (§2).** Rotta `POST /api/spesa_modelli/v1/fonti/{id}/importazioni` per la
  ripetizione manuale, `GET .../importazioni` per lo stato; errori in `problem+json`; definizione OpenAPI
  aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione sullo schema `app_spesa_modelli`: tabella `importazione` con
  `tenant_id`, fonte, intervallo, cursore, stato, conteggi, colonne di controllo e cancellazione logica.
- **RT-4 — Adattatore per fornitore.** Un'implementazione per fornitore dietro un'unica interfaccia, con la
  mappatura dei nomi dei campi in un solo punto: le differenze fra fornitori non si spargono nel codice. È la
  contromisura al rischio R5 del documento capofila.
- **RT-5 — Varchi e quota (§6, §7).** Ogni riga registrata prenota una unità della metrica `misure_registrate`
  (natura a consumo); a quota esaurita la lavorazione si ferma, lascia il cursore dov'è e segnala all'account
  perché si è fermata — non perde silenziosamente i dati.
- **RT-6 — Esposizione conversazionale (§12).** Nessuno strumento nuovo: lo stato dell'importazione è compreso in
  `stato_fonti` (storia `0006`).
- **RT-7 — Dati personali (§10).** Nessun dato personale nuovo: dal rendiconto arrivano modelli, conteggi,
  identificativi di chiave e di spazio di lavoro. Se un fornitore restituisse un identificativo di utente finale,
  la voce va aggiunta al manifesto **prima** di memorizzarlo.
- **RT-8 — Registrazione eventi (§14).** Eventi «importazione avviata», «importazione conclusa con N righe»,
  «importazione fermata per quota», «limite di interrogazione del fornitore raggiunto» con `tenant_id`, `app_id`,
  `user_id` e identificativo di correlazione, senza dati personali.

## 4. Criteri di accettazione

**CA-1 — Recupero dello storico al primo collegamento**
- **Dato** una fonte appena collegata e novanta giorni di dati sul fornitore simulato
- **Quando** parte il recupero iniziale
- **Allora** al termine l'account ha le misure dei novanta giorni, l'avanzamento è stato visibile durante il
  recupero e il cursore punta all'ultimo istante importato

**CA-2 — Ripresa senza duplicati**
- **Dato** un recupero interrotto a metà dell'intervallo
- **Quando** la lavorazione riparte
- **Allora** riprende dal cursore e il numero totale di misure è identico a quello di un recupero eseguito senza
  interruzioni

**CA-3 — Rispetto del limite di interrogazione**
- **Dato** un fornitore che risponde «troppe richieste» alla seconda chiamata nello stesso minuto
- **Quando** la lavorazione incontra il rifiuto
- **Allora** attende con intervallo crescente, non perde il cursore e conclude senza perdere righe

**CA-4 — Quota esaurita durante l'importazione**
- **Dato** un account che raggiunge il tetto della metrica a metà del recupero
- **Quando** la lavorazione tenta di registrare la riga successiva
- **Allora** si ferma, lascia il cursore all'ultima riga registrata e l'account vede un avviso che spiega cosa è
  successo e come rimediare; nessuna riga viene persa in silenzio

**CA-5 — Isolamento fra account**
- **Dato** due account con una fonte ciascuno sullo stesso fornitore
- **Quando** entrambe le lavorazioni girano
- **Allora** nessuna misura di un account finisce nell'altro, nemmeno a parità di identificativo esterno

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno backend; l'intera suite prima del commit);
- [ ] prove di **unità** sull'adattatore e sulla gestione del cursore, e di **integrazione** sull'importazione
      completa con fornitore simulato e migrazioni vere;
- [ ] prova di **isolamento fra account** sulle misure importate;
- [ ] **prova end-to-end**: **coprire ora**, estendendo `[J-SPESA-MODELLI]` con il passo «collegata la fonte,
      compaiono i numeri», e aggiornare il registro di copertura
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml);
- [ ] **traduzioni** presenti in tutte e cinque le lingue per gli stati dell'importazione;
- [ ] **manifesto dei dati**: nessuna voce nuova, dichiarato esplicitamente;
- [ ] **registro delle decisioni** compilato, in particolare sulla scelta di un adattatore per fornitore;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storia `0006` | Serve una fonte collegata e verificata |
| Storia `0004` | Serve la prenotazione della quota |

## 7. Fuori ambito

- la deduplica fra righe di origini diverse (rendiconto e invio che raccontano la stessa chiamata): è della storia
  `0010`;
- il confronto fra ciò che abbiamo importato e ciò che il fornitore dichiara: è della storia `0011`;
- il calcolo del costo delle righe che arrivano senza importo: è dell'epica 03.

## 8. Punti aperti

- **La profondità del recupero iniziale rispetto ai piani.** Novanta giorni di storico su un piano che ne conserva
  trenta significa importare dati che verranno cancellati. La proposta è limitare il recupero iniziale alla
  profondità di conservazione del piano, e dirlo prima di partire; ma toglie al piano gratuito proprio la
  possibilità di vedere il trimestre. È una decisione di prodotto legata al listino: la chiude lo sviluppatore.
