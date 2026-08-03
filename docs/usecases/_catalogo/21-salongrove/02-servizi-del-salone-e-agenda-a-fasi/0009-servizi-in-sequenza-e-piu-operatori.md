# 0009 — Servizi in sequenza e più operatori

**Applicazione**: 21 — SalonGrove (`salone`) · **Epica**: 02 — Servizi del salone e agenda a fasi
**Storia**: `0009` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0007`, `0008`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come chi sta alla reception
> voglio prenotare «colore e taglio» come un appuntamento solo, sapendo che il colore lo fa Sara, lo shampoo
> l'assistente e il taglio Marco
> così da non dover incastrare a mano tre righe di agenda e da poter dire alla cliente un'ora d'arrivo sola.

**Contesto.** È la seconda differenza fra un'agenda generica e un'agenda per saloni: nel beauty il singolo
appuntamento è quasi sempre **più servizi in fila**, spesso con **persone diverse**. Le fonti di settore descrivono
proprio la vista unica di operatori, postazioni e cabine come il modo per evitare le sovrapposizioni (§2.5 della
descrizione). Senza questa storia, un colore con taglio si prenota come due appuntamenti che qualcuno deve
ricordarsi di tenere attaccati — e quando si sposta il primo, il secondo resta indietro.

## 2. Requisiti funzionali

1. **RF-1** — Un appuntamento può contenere **più servizi in sequenza**, con un ordine, e la durata complessiva è
   la somma delle durate tenendo conto delle fasi e delle varianti.
2. **RF-2** — Ogni servizio della sequenza può avere un **operatore diverso**, scelto a mano o proposto fra quelli
   che erogano quel servizio e sono liberi in quei minuti.
3. **RF-3** — La ricerca degli spazi liberi propone solo gli orari in cui **tutta la sequenza** sta in piedi: se
   manca l'operatore del terzo servizio, quell'orario non compare.
4. **RF-4** — Spostare o disdire l'appuntamento agisce su **tutta la sequenza**; togliere un singolo servizio
   dalla sequenza ricalcola gli orari di quelli che seguono e chiede conferma se il risultato sposta la fine.
5. **RF-5** — È ammesso un **intervallo voluto** fra due servizi della sequenza (la cliente vuole un caffè), che
   però lascia le risorse libere e non le blocca.
6. **RF-6** — L'agenda mostra la sequenza come **una cliente sola** che attraversa più colonne, non come tre
   clienti distinti.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Sequenza, proposta degli operatori e calcolo filtrano per `tenant_id`
  dal token verificato.
- **RT-2 — Interfaccia di programmazione (§2).** La creazione della prenotazione accetta una **lista** di servizi
  con operatore facoltativo; la disponibilità accetta la stessa lista e restituisce solo gli orari interamente
  soddisfacibili; errori in `problem+json`; OpenAPI aggiornata.
- **RT-3 — Persistenza (§8).** Il legame prenotazione → servizi in sequenza porta `tenant_id`, ordine, operatore
  scelto e riferimento alle varianti; i segmenti della storia `0007` si generano per l'intera sequenza in **una
  sola transazione**: o entra tutta o non entra niente.
- **RT-4 — Concorrenza.** La scrittura di una sequenza è atomica rispetto al vincolo di non sovrapposizione: se
  uno solo dei segmenti confligge, l'intera sequenza è rifiutata e nessuna occupazione parziale resta.
- **RT-5 — Modulo frontend (§3, §5).** Nella prenotazione, la sequenza è una lista ordinata con l'operatore
  accanto a ciascun servizio e l'orario di fine che si aggiorna; in agenda, un tratto che collega i pezzi della
  stessa cliente. Solo token del sistema di design, tema chiaro e scuro.
- **RT-6 — Cinque lingue (§4).** Etichette, messaggi di conflitto e testo della conferma di ricalcolo in tutte e
  cinque le lingue.
- **RT-7 — Dati personali (§10).** Nessun dato personale nuovo: la sequenza lega entità che esistono già.
- **RT-8 — Registrazione eventi (§14).** `sequenza prenotata`, `sequenza respinta per conflitto` con `tenant_id`,
  `app_id`, `user_id` e correlazione.

## 4. Criteri di accettazione

**CA-1 — Colore e taglio con due persone**
- **Dato** un colore (Sara) e un taglio (Marco), entrambi disponibili alle 14:00
- **Quando** si prenota la sequenza
- **Allora** nasce **un** appuntamento che occupa Sara nelle fasi del colore e Marco nei minuti del taglio, e la
  cliente ha una sola ora d'arrivo

**CA-2 — Nessun orario se manca un pezzo**
- **Dato** che Marco è occupato in tutto il pomeriggio
- **Quando** si cercano gli spazi liberi per la sequenza colore + taglio
- **Allora** il pomeriggio non compare fra le proposte, nemmeno per la parte del colore

**CA-3 — Tutto o niente**
- **Dato** due richieste simultanee che confliggono sul solo terzo servizio della sequenza
- **Quando** arrivano insieme
- **Allora** una sola sequenza entra per intero, e dell'altra **non resta nessun segmento**

**CA-4 — Togliere un servizio ricalcola**
- **Dato** una sequenza di tre servizi
- **Quando** si toglie il primo
- **Allora** il programma mostra il nuovo orario di inizio e di fine e chiede conferma prima di applicarlo

**CA-5 — Isolamento fra account**
- **Dato** due account con operatori omonimi
- **Quando** un utente del primo prenota una sequenza forzando l'identificativo di un operatore dell'altro
- **Allora** la richiesta è rifiutata e nessun dato dell'altro account trapela nel messaggio

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (suite intera prima del commit);
- [ ] prove di **unità** sul calcolo della sequenza e di **integrazione** sull'atomicità sotto concorrenza;
- [ ] prova di **isolamento fra account** su disponibilità e prenotazione della sequenza;
- [ ] **prova end-to-end**: *coprire ora* — il percorso `[J-SALONGROVE]` (storia `0030`) prenota una sequenza a
      due operatori; registro [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) aggiornato;
- [ ] **traduzioni** in tutte e cinque le lingue;
- [ ] **manifesto dei dati**: nessuna voce nuova, e il fatto è dichiarato;
- [ ] **registro delle decisioni**: atomicità della sequenza, intervallo voluto che non blocca risorse, conferma
      esplicita sul ricalcolo;
- [ ] avvio locale invariato; il salone di prova contiene almeno una sequenza a due operatori.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0007` | i segmenti sono la base su cui la sequenza si scrive |
| storia `0008` | le varianti cambiano le durate dei singoli servizi della sequenza |

## 7. Fuori ambito

- la prenotazione di una sequenza **dalla pagina pubblica**: in prima versione la pagina pubblica resta a un
  servizio per volta (è di BookGrove), perché una sequenza scelta da chi non conosce il salone produce
  combinazioni che il salone non vuole. Rimandato, e il motivo è questo;
- l'attribuzione del venduto agli operatori della sequenza: storia `0023`, che ne ha bisogno ma è un'altra cosa;
- l'assistente che «passa» un pezzo di lavoro a un altro a metà: fuori ambito, non ho elementi per dire se serva.

## 8. Punti aperti

Nessuno.
