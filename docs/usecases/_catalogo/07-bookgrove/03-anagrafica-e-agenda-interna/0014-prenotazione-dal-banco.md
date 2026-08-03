# 0014 — Prenotazione dal banco

**Applicazione**: 07 — BookGrove (`prenotazioni`) · **Epica**: 03 — Anagrafica dei clienti e agenda interna
**Storia**: `0014` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0011`, `0013`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come chi risponde al telefono mentre lavora
> voglio prendere un appuntamento in pochi secondi, senza uscire dall'agenda
> così da non far aspettare né la persona al telefono né quella che ho davanti.

**Contesto.** È l'atto centrale dell'applicazione visto dal lato dell'attività, ed è il primo punto in cui si
scrive nell'agenda. Qui va risolto il problema che rende imbarazzante un'app di prenotazioni: la **doppia
prenotazione**. Due persone che scelgono lo stesso spazio nello stesso istante — una dal banco, una dalla pagina
pubblica — devono ottenere una che vince e una che riceve un messaggio comprensibile, mai due appuntamenti
sovrapposti. Il presidio non può stare nel programma applicativo: deve stare nel database.

## 2. Requisiti funzionali

1. **RF-1** — Dall'agenda si crea un appuntamento scegliendo cliente, servizio, risorsa e orario; il cliente si
   cerca o si crea al volo con i soli campi obbligatori.
2. **RF-2** — Gli orari proposti vengono dal motore della storia `0010`; si può però **forzare** un orario non
   disponibile, perché il banco ha sempre l'ultima parola — con un avviso esplicito e la traccia di chi ha
   forzato.
3. **RF-3** — Due prenotazioni sulla stessa risorsa non possono sovrapporsi: il secondo tentativo riceve un
   messaggio che dice che lo spazio è appena stato preso, e nulla viene scritto.
4. **RF-4** — La durata proposta viene dal servizio ma è **modificabile** sul singolo appuntamento, perché una
   stessa prestazione non dura sempre uguale.
5. **RF-5** — Si modifica un appuntamento esistente: orario, risorsa, servizio, note; ogni modifica lascia una
   traccia con chi e quando.
6. **RF-6** — L'appuntamento creato dal banco nasce già `confermata`: chi lo ha preso era al telefono con la
   persona.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura e scrittura delle `prenotazione` filtra per `tenant_id`
  preso dal token verificato; il cliente, il servizio e la risorsa indicati devono appartenere allo stesso
  account, altrimenti la richiesta è rifiutata.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `POST /api/prenotazioni/v1/prenotazioni` e
  `PUT /api/prenotazioni/v1/prenotazioni/{id}`; corpo validato; errori in `problem+json` con codici stabili per
  «spazio non più disponibile» e «entità di un altro account»; OpenAPI aggiornata.
- **RT-3 — Persistenza (§8).** Migrazione `V7__prenotazioni.sql`: tabella `prenotazione` con `tenant_id`, UUID
  versione 7, colonne di controllo e cancellazione logica; **vincolo di non sovrapposizione nel database** su
  (risorsa, intervallo) per gli stati che occupano tempo — è il presidio contro la doppia prenotazione, e non
  basta un controllo nel programma; tabella `evento_prenotazione` per la traccia delle modifiche.
- **RT-4 — Modulo frontend (§3, §5).** Creazione dall'agenda, con selezione dell'orario dal calcolo della
  disponibilità e l'opzione di forzatura ben distinta; moduli con React Hook Form e Zod; solo token del sistema di
  design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Interfaccia e messaggi di errore in `en, it, fr, es, de`.
- **RT-6 — Varchi e quota (§6, §7).** La creazione di una prenotazione **non consuma quota**: la metrica
  `risorse_prenotabili` è a giacenza sulle risorse, e questa è una proprietà voluta (§3 e §11 della descrizione).
  Con abbonamento non attivo risponde `402`.
- **RT-7 — Dati personali (§10).** Voci nuove nel manifesto in italiano e inglese: `prenotazione.note_cliente` e
  il collegamento fra cliente e servizio prenotato, con il regime deciso dalla storia `0012`; campi annotati
  `@PersonalData`; tabelle `prenotazione` ed `evento_prenotazione` aggiunte a `exportData` e `purgeData`.
- **RT-8 — Registrazione eventi (§14).** `prenotazione creata`, `prenotazione modificata`, `sovrapposizione
  respinta`, `orario forzato` con `tenant_id`, `app_id`, `user_id` e correlazione — **mai il nome del cliente né
  il nome del servizio**.

## 4. Criteri di accettazione

**CA-1 — Appuntamento in pochi passi**
- **Dato** un cliente esistente e uno spazio libero alle 15
- **Quando** si crea l'appuntamento dall'agenda
- **Allora** compare nella colonna della risorsa, in stato `confermata`, con la durata del servizio

**CA-2 — Doppia prenotazione impossibile**
- **Dato** due richieste che arrivano nello stesso istante sullo stesso spazio
- **Quando** vengono elaborate
- **Allora** una riesce e l'altra riceve l'errore stabile «spazio non più disponibile», e in agenda c'è un solo
  appuntamento

**CA-3 — Forzatura consapevole**
- **Dato** uno spazio fuori orario · **Quando** si forza l'appuntamento · **Allora** viene creato, l'avviso è
  stato mostrato prima e resta la traccia di chi ha forzato

**CA-4 — Durata modificata**
- **Dato** un servizio da 40 minuti · **Quando** si porta la durata a 70 su questo appuntamento
- **Allora** il blocco occupa 70 minuti e gli spazi successivi si aggiornano

**CA-5 — Entità di un altro account**
- **Dato** l'identificativo di un cliente di un altro account · **Quando** si prova a usarlo · **Allora** la
  richiesta è rifiutata e nulla viene creato

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend`, `frontend` e `compliance`);
- [ ] prove di **unità** sulla validazione dell'intervallo e di **integrazione** sulla creazione, compresa una
      prova di **concorrenza** che esercita il vincolo del database;
- [ ] prova di **isolamento fra account** su creazione e modifica;
- [ ] **prova end-to-end**: **coperta ora** — è il passo centrale del percorso `[J-BOOKGROVE]`, con il registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) aggiornato dalla storia `0033`;
- [ ] **traduzioni** in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese, con le tabelle in esportazione e cancellazione;
- [ ] **registro delle decisioni** compilato: il vincolo di non sovrapposizione nel database e la possibilità di
      forzare;
- [ ] avvio locale invariato; i dati di prova comprendono prenotazioni in stati diversi;
- [ ] documentazione aggiornata.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0011` | serve il cliente |
| storia `0013` | si crea dall'agenda |
| storia `0010` | serve sapere quali orari proporre |

## 7. Fuori ambito

- disdetta, spostamento e mancata presentazione: storia `0015`;
- la prenotazione fatta dal cliente finale: storia `0017`;
- l'appuntamento ricorrente (ogni due settimane, stesso orario): rimandato, non richiesto dalla scheda di
  catalogo e con casi limite non banali sulle eccezioni.

## 8. Punti aperti

**Fino a che punto si può forzare.** Forzare un orario fuori disponibilità è utile; forzare una **sovrapposizione**
sulla stessa risorsa no, perché la risorsa non si sdoppia. La proposta è: si può forzare l'orario, **non** la
sovrapposizione. Se un verticale avesse bisogno del contrario (una sala che accoglie più persone insieme), lo
risolve la capienza, che è un punto aperto della storia `0007`.
